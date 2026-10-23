package me.apomazkin.core_network_impl

import kotlinx.coroutines.test.runTest
import me.apomazkin.core_network_api.entity.ComponentNodeApiEntity
import me.apomazkin.core_network_api.entity.ComponentOptionNodeApiEntity
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.LookupRequestApiEntity
import me.apomazkin.core_network_api.entity.LookupResult
import me.apomazkin.core_network_api.entity.TokenApiEntity
import me.apomazkin.core_network_api.entity.WordStatus
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Слой сети против поддельного сервера: запрос (WFT-19…23, WFT-78), отказ
 * (WFT-63, WFT-05), ответ по контракту 0.3.0 (разделы 5.1–5.7). Адреса
 * строк спеки в именах — `WFT-NN_k` (точка в имени метода JVM запрещена;
 * скрипт покрытия понимает оба разделителя).
 */
class WordsApiImplTest {

    private val server = MockWebServer()
    private val logLines = mutableListOf<String>()
    private val logger = object : LexemeLogger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logLines += "$tag $message"
        }
    }

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    // === Ш1: запрос ===

    @Test
    fun `WFT-20_1 WFT-20_3 request carries tokens in order with repeats confidence language dictionary languages and bearer header`() = runTest {
        server.enqueue(MockResponse(code = 200, body = EMPTY_RESPONSE))
        val texts = listOf("Las", "nubes", "corrían", "por", "el", "cielo.", "El", "perro", "duerme", "en", "el", "jardín.")

        api().lookup(requestOf(tokens = texts.map { token(it, 0.97f, "es") }), runId = 7)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/words/lookup", recorded.url.encodedPath)
        assertEquals("Bearer $ACCESS_KEY", recorded.headers["Authorization"])
        assertEquals("7", recorded.headers["X-Run-Id"])
        val body = recorded.body?.utf8() ?: ""
        assertTrue(body, body.contains("\"learningLanguage\":\"es\""))
        assertTrue(body, body.contains("\"translationLanguage\":\"ru\""))
        val sent = Regex("\"text\":\"([^\"]+)\"").findAll(body).map { it.groupValues[1] }.toList()
        assertEquals(texts, sent)
        assertTrue(body, body.contains("\"confidence\":0.97"))
        assertTrue(body, body.contains("\"language\":\"es\""))
    }

    @Test
    fun `WFT-20_2 token without language is sent as null`() = runTest {
        server.enqueue(MockResponse(code = 200, body = EMPTY_RESPONSE))

        api().lookup(requestOf(token("accorc", 0.42f, null)), runId = 1)

        val body = server.takeRequest().body?.utf8() ?: ""
        assertTrue(body, body.contains("\"language\":null"))
    }

    @Test
    fun `WFT-20_4 WFT-78_1 WFT-78_2 WFT-78_3 the tree goes as in the contract - built-in keys own option without key and the dependent component inside the option`() = runTest {
        server.enqueue(MockResponse(code = 200, body = EMPTY_RESPONSE))

        api().lookup(requestOf(token("perro", 1f, "es"), components = contractTree()), runId = 1)

        val body = server.takeRequest().body?.utf8() ?: ""
        val expectedTree = """"components":[""" +
            """{"id":1,"key":"translation","name":"Перевод","template":"text","core":true,"multiple":false},""" +
            """{"id":2,"key":"part_of_speech","name":"Часть речи","template":"choice","core":false,"multiple":false,"options":[""" +
            """{"id":21,"key":"noun","label":"существительное","children":[""" +
            """{"id":5,"key":null,"name":"Род","template":"choice","core":false,"multiple":false,"options":[""" +
            """{"id":51,"key":null,"label":"м.р."},{"id":52,"key":null,"label":"ж.р."}]}]},""" +
            """{"id":22,"key":"verb","label":"глагол"}]},""" +
            """{"id":3,"key":"example","name":"Пример","template":"captioned_text","core":false,"multiple":true},""" +
            """{"id":4,"key":null,"name":"Толкование","template":"text","core":false,"multiple":false}]"""
        assertTrue(body, body.contains(expectedTree))
    }

    @Test
    fun `WFT-78_7 a dictionary without components sends no components field`() = runTest {
        server.enqueue(MockResponse(code = 200, body = EMPTY_RESPONSE))

        api().lookup(requestOf(token("perro"), components = emptyList()), runId = 1)

        val body = server.takeRequest().body?.utf8() ?: ""
        assertFalse(body, body.contains("components"))
    }

    @Test
    fun `WFT-19_1 empty access key sends nothing and is denied as no access key`() = runTest {
        server.enqueue(MockResponse(code = 200, body = EMPTY_RESPONSE))

        val result = api(accessKey = "").lookup(requestOf(token("perro")), runId = 1)

        assertEquals(LookupResult.Denied(DenyReason.NO_ACCESS_KEY), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `WFT-21_1 WFT-21_2 http base url including loopback is refused without a connection`() = runTest {
        server.enqueue(MockResponse(code = 200, body = EMPTY_RESPONSE))

        val result = api(requireHttps = true).lookup(requestOf(token("perro")), runId = 1)

        assertTrue(result.toString(), result is LookupResult.Unavailable)
        assertEquals(0, server.requestCount)
    }

    // === Ш1: ожидание ===

    @Test
    fun `WFT-22_1 silent server past the call timeout is unavailable`() = runTest {
        server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.Stall).build())

        val result = api(callTimeout = 300.milliseconds).lookup(requestOf(token("perro")), runId = 1)

        assertTrue(result.toString(), result is LookupResult.Unavailable)
    }

    @Test
    fun `WFT-22_2 answer after the call timeout is dropped as unavailable`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .headersDelay(900, TimeUnit.MILLISECONDS)
                .body(EMPTY_RESPONSE)
                .build(),
        )

        val result = api(callTimeout = 300.milliseconds).lookup(requestOf(token("perro")), runId = 1)

        assertTrue(result.toString(), result is LookupResult.Unavailable)
    }

    @Test
    fun `WFT-22_3 read and write timeouts equal the call timeout so a slow server is awaited the full 180 seconds`() {
        val options = NetworkOptions(baseUrl = "https://api.lexeme.pro/", accessKey = ACCESS_KEY, callTimeout = 180.seconds)

        val client = LexemeServiceFactory.httpClient(options, logger)

        assertEquals(180_000, client.callTimeoutMillis)
        assertEquals(180_000, client.readTimeoutMillis)
        assertEquals(180_000, client.writeTimeoutMillis)
        assertEquals(10_000, client.connectTimeoutMillis)
    }

    @Test
    fun `WFT-22_3 answer within the call timeout is a success`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .headersDelay(200, TimeUnit.MILLISECONDS)
                .body(EMPTY_RESPONSE)
                .build(),
        )

        val result = api(callTimeout = 2.seconds).lookup(requestOf(token("perro")), runId = 1)

        assertTrue(result.toString(), result is LookupResult.Success)
    }

    // === Ш1: сервер недоступен или не по контракту ===

    @Test
    fun `WFT-23_1 refused connection is unavailable`() = runTest {
        val url = server.url("/").toString()
        server.close()

        val result = api(baseUrl = url).lookup(requestOf(token("perro")), runId = 1)

        assertTrue(result.toString(), result is LookupResult.Unavailable)
    }

    @Test
    fun `WFT-23_2 status 500 is unavailable`() = runTest {
        server.enqueue(MockResponse(code = 500, body = "boom"))

        val result = api().lookup(requestOf(token("perro")), runId = 1)

        assertTrue(result.toString(), result is LookupResult.Unavailable)
    }

    @Test
    fun `WFT-23_3 status 200 with a body off the schema is unavailable`() = runTest {
        server.enqueue(MockResponse(code = 200, body = """{"hello":"world"}"""))

        val result = api().lookup(requestOf(token("perro")), runId = 1)

        assertTrue(result.toString(), result is LookupResult.Unavailable)
    }

    @Test
    fun `WFT-23_4 status 400 invalid_request is our bug and is unavailable`() = runTest {
        server.enqueue(MockResponse(code = 400, body = errorBody("invalid_request")))

        val result = api().lookup(requestOf(token("perro")), runId = 1)

        assertTrue(result.toString(), result is LookupResult.Unavailable)
    }

    @Test
    fun `WFT-23_5 status 503 with a body off the error schema is unavailable`() = runTest {
        server.enqueue(MockResponse(code = 503, body = "<html>Service Unavailable</html>"))

        val result = api().lookup(requestOf(token("perro")), runId = 1)

        assertTrue(result.toString(), result is LookupResult.Unavailable)
    }

    // === В1, В2: отказ ===

    @Test
    fun `WFT-63_1 status 401 is denied as unauthorized`() = runTest {
        server.enqueue(MockResponse(code = 401, body = errorBody("unauthorized")))

        val result = api().lookup(requestOf(token("perro")), runId = 1)

        assertEquals(LookupResult.Denied(DenyReason.UNAUTHORIZED), result)
    }

    @Test
    fun `WFT-63_2 status 429 is denied as rate limited`() = runTest {
        server.enqueue(MockResponse(code = 429, body = errorBody("rate_limited")))

        val result = api().lookup(requestOf(token("perro")), runId = 1)

        assertEquals(LookupResult.Denied(DenyReason.RATE_LIMITED), result)
    }

    @Test
    fun `WFT-63_3 status 413 is denied as request too large`() = runTest {
        server.enqueue(MockResponse(code = 413, body = errorBody("request_too_large")))

        val result = api().lookup(requestOf(token("perro")), runId = 1)

        assertEquals(LookupResult.Denied(DenyReason.REQUEST_TOO_LARGE), result)
    }

    @Test
    fun `WFT-63_4 status 400 unsupported_language is denied as unsupported language`() = runTest {
        server.enqueue(MockResponse(code = 400, body = errorBody("unsupported_language")))

        val result = api().lookup(requestOf(token("perro")), runId = 1)

        assertEquals(LookupResult.Denied(DenyReason.UNSUPPORTED_LANGUAGE), result)
    }

    @Test
    fun `WFT-63_5 status 503 llm_unavailable is denied as llm unavailable`() = runTest {
        server.enqueue(MockResponse(code = 503, body = errorBody("llm_unavailable")))

        val result = api().lookup(requestOf(token("perro")), runId = 1)

        assertEquals(LookupResult.Denied(DenyReason.LLM_UNAVAILABLE), result)
    }

    // === Инвариант: ключ не утекает ===

    @Test
    fun `WFT-05_1 access key never appears in the request log`() = runTest {
        server.enqueue(MockResponse(code = 401, body = errorBody("unauthorized")))

        api().lookup(requestOf(token("perro")), runId = 1)

        assertFalse(logLines.isEmpty())
        assertTrue(logLines.toString(), logLines.none { it.contains(ACCESS_KEY) })
    }

    // === Ш8: ответ по контракту 0.3.0 (разделы 5.1–5.6) ===

    @Test
    fun `WFT-49_1 WFT-50_1 contract response gives words in response order with attribution and recommendations`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_v0_1_10_es.json")))

        val result = api().lookup(requestOf(token("corrían")), runId = 1) as LookupResult.Success

        assertEquals("Wiktionary, CC BY-SA 4.0", result.attribution)
        assertEquals(listOf("corrían", "perro", "zarpó", "accorc", "llave"), result.words.map { it.text })
        val recommendation = result.recommendations.single()
        assertEquals("Род", recommendation.name)
        assertEquals("choice", recommendation.template)
        assertEquals(listOf("м.р.", "ж.р."), recommendation.options)
    }

    @Test
    fun `WFT-90_1 WFT-87_2 corrian keeps the selection order and the component matches of every lexeme`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_v0_1_10_es.json")))

        val result = api().lookup(requestOf(token("corrían")), runId = 1) as LookupResult.Success

        val corrian = result.words.first { it.text == "corrían" }
        assertEquals(WordStatus.FOUND, corrian.status)
        assertTrue(corrian.remarks.isEmpty())
        val correr = corrian.lemmas.single()
        assertEquals("correr", correr.text)
        assertEquals("third-person plural imperfect indicative", correr.formNote)
        assertEquals(listOf("ruwiktionary/1.verb.3", "ruwiktionary/1.verb.1", "ruwiktionary/1.verb.5"), correr.lexemes.map { it.key })
        val first = correr.lexemes.first()
        assertEquals(listOf(1L, 2L, 4L), first.componentMatches.map { it.componentId })
        assertEquals(listOf(null, 22L, null), first.componentMatches.map { it.optionId })
        assertEquals(listOf("translations", null, "gloss"), first.componentMatches.map { it.field })
        assertEquals("to flow", first.gloss)
    }

    @Test
    fun `WFT-87_1 WFT-81_1 perro carries grammar pronunciation examples and the option matches`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_v0_1_10_es.json")))

        val result = api().lookup(requestOf(token("perro")), runId = 1) as LookupResult.Success

        val perro = result.words.first { it.text == "perro" }.lemmas.single()
        assertEquals(listOf("/ˈpero/"), perro.pronunciations)
        val noun = perro.lexemes.first()
        assertEquals(mapOf("gender" to "masculine"), noun.grammar)
        assertEquals(listOf("perro callejero"), noun.examples)
        assertEquals(listOf(1L, 2L, 5L, 3L, 4L), noun.componentMatches.map { it.componentId })
        assertEquals(51L, noun.componentMatches[2].optionId)
        assertEquals(listOf("разг."), perro.lexemes[1].notes)
        assertNull(perro.lexemes[2].gloss)
    }

    @Test
    fun `WFT-91_1 WFT-81_3 zarpo carries two no_translation remarks level synonyms and antonyms`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_v0_1_10_es.json")))

        val result = api().lookup(requestOf(token("zarpó")), runId = 1) as LookupResult.Success

        val zarpo = result.words.first { it.text == "zarpó" }
        assertEquals(listOf("no_translation", "no_translation"), zarpo.remarks.map { it.code })
        assertEquals(listOf("enwiktionary/1.verb.1", "enwiktionary/2.verb.1"), zarpo.remarks.map { it.lexemeKey })
        assertTrue(zarpo.remarks.all { it.lemma == "zarpar" && it.componentId == null })
        val embark = zarpo.lemmas.single().lexemes.first()
        assertTrue(embark.translations.isEmpty())
        assertEquals("B1", embark.level)
        assertEquals(listOf("salir"), embark.synonyms)
        assertEquals(listOf("atracar"), embark.antonyms)
        assertEquals(mapOf("transitivity" to "intransitive"), embark.grammar)
    }

    @Test
    fun `WFT-50_2 WFT-81_2 WFT-81_4 not found and error words have no lemmas and empty grammar or pronunciations parse as empty`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_v0_1_10_es.json")))

        val result = api().lookup(requestOf(token("accorc")), runId = 1) as LookupResult.Success

        val accorc = result.words.first { it.text == "accorc" }
        val llave = result.words.first { it.text == "llave" }
        assertEquals(WordStatus.NOT_FOUND, accorc.status)
        assertEquals(WordStatus.ERROR, llave.status)
        assertTrue(accorc.lemmas.isEmpty() && llave.lemmas.isEmpty())
        val correr = result.words.first { it.text == "corrían" }.lemmas.single()
        assertTrue(correr.pronunciations.isEmpty())
        assertTrue(correr.lexemes.first().grammar.isEmpty())
        assertNull(correr.lexemes.first().level)
    }

    @Test
    fun `WFT-88_1 a component_unclear remark carries the component id`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"attribution":"Wiktionary, CC BY-SA 4.0","recommendations":[],"words":[{"text":"perro","status":"found","remarks":[{"code":"component_unclear","componentId":7}],"lemmas":[]}]}""",
            ),
        )

        val result = api().lookup(requestOf(token("perro")), runId = 1) as LookupResult.Success

        val remark = result.words.single().remarks.single()
        assertEquals("component_unclear", remark.code)
        assertEquals(7L, remark.componentId)
        assertNull(remark.lemma)
    }

    @Test
    fun `WFT-90_8 an unknown remark code and an unknown recommendation template stay strings`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"attribution":"Wiktionary, CC BY-SA 4.0","recommendations":[{"code":"component_recommended","name":"Картинка","template":"image"}],"words":[{"text":"perro","status":"found","remarks":[{"code":"new_code"}],"lemmas":[]}]}""",
            ),
        )

        val result = api().lookup(requestOf(token("perro")), runId = 1) as LookupResult.Success

        assertEquals("new_code", result.words.single().remarks.single().code)
        assertEquals("image", result.recommendations.single().template)
        assertTrue(result.recommendations.single().options.isEmpty())
    }

    // === Ш8: ответы прода v0.1.9 (IS532) — лишние поля пропускаются, новых нет ===

    @Test
    fun `WFT-50_3 old recorded response without lemmas parses with empty lemmas and remarks`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_sample.json")))

        val result = api().lookup(requestOf(token("corrían")), runId = 1) as LookupResult.Success

        assertTrue(result.words.all { it.lemmas.isEmpty() && it.remarks.isEmpty() })
        assertEquals(4, result.words.size)
        assertTrue(result.recommendations.isEmpty())
    }

    @Test
    fun `WFT-76_2 recorded running gives lemmas run with a form note and running without`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_v0_1_9_en.json")))

        val result = api().lookup(requestOf(token("running")), runId = 1) as LookupResult.Success

        val running = result.words.first { it.text == "running" }
        assertEquals(listOf("run", "running"), running.lemmas.map { it.text })
        assertEquals("present participle and gerund", running.lemmas[0].formNote)
        assertNull(running.lemmas[1].formNote)
        assertEquals(71, running.lemmas[0].lexemes.size)
        assertEquals(35, running.lemmas[1].lexemes.size)
    }

    @Test
    fun `WFT-71_1 WFT-72_1 recorded run carries 130 lexemes with key source and part of speech key and empty matches`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_v0_1_9_en.json")))

        val result = api().lookup(requestOf(token("run")), runId = 1) as LookupResult.Success

        val run = result.words.first { it.text == "run" }.lemmas.first { it.text == "run" }
        assertEquals(130, run.lexemes.size)
        assertEquals(71, run.lexemes.count { it.partOfSpeechKey == "verb" })
        assertEquals(setOf("ruwiktionary", "enwiktionary"), run.lexemes.map { it.source }.toSet())
        assertEquals(130, run.lexemes.map { it.key }.toSet().size)
        assertEquals(listOf("бежать, бегать"), run.lexemes.first().translations)
        assertTrue(run.lexemes.all { it.componentMatches.isEmpty() })
    }

    @Test
    fun `WFT-76_1 recorded corrian gives lemma correr with a form note and 27 lexemes`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_v0_1_9_es.json")))

        val result = api().lookup(requestOf(token("corrían")), runId = 1) as LookupResult.Success

        val correr = result.words.first { it.text == "corrían" }.lemmas.single()
        assertEquals("correr", correr.text)
        assertEquals("third-person plural imperfect indicative", correr.formNote)
        assertEquals(27, correr.lexemes.size)
        assertEquals(15, correr.lexemes.count { it.source == "ruwiktionary" })
    }

    @Test
    fun `unknown source part of speech key and extra fields keep the lexeme and the response stays a success`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"attribution":"Wiktionary, CC BY-SA 4.0","served":"cache","words":[{"text":"run","status":"found","lemmas":[{"text":"run","formNote":null,"lexemes":[{"key":"cambridge/1","partOfSpeechKey":"gerund","source":"cambridge","translations":["бег"],"level":"B1","extra":1}]}]}]}""",
            ),
        )

        val result = api().lookup(requestOf(token("run")), runId = 1)

        val success = result as LookupResult.Success
        val lexeme = success.words.single().lemmas.single().lexemes.single()
        assertEquals("cambridge", lexeme.source)
        assertEquals("gerund", lexeme.partOfSpeechKey)
        assertEquals(listOf("бег"), lexeme.translations)
        assertEquals("B1", lexeme.level)
        assertNull(lexeme.gloss)
        assertTrue(lexeme.examples.isEmpty())
    }

    @Test
    fun `lexeme without key or source is off the schema and makes the response unavailable`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"attribution":"x","words":[{"text":"run","status":"found","lemmas":[{"text":"run","lexemes":[{"translations":["бег"]}]}]}]}""",
            ),
        )

        val result = api().lookup(requestOf(token("run")), runId = 1)

        assertTrue(result is LookupResult.Unavailable)
    }

    // === обвязка ===

    private fun api(
        baseUrl: String = server.url("/").toString(),
        accessKey: String = ACCESS_KEY,
        callTimeout: kotlin.time.Duration = 5.seconds,
        requireHttps: Boolean = false,
    ): WordsApiImpl {
        val options = NetworkOptions(
            baseUrl = baseUrl,
            accessKey = accessKey,
            callTimeout = callTimeout,
            requireHttps = requireHttps,
        )
        return WordsApiImpl(
            service = LexemeServiceFactory.create(options, logger),
            options = options,
        )
    }

    private fun requestOf(
        vararg tokens: TokenApiEntity,
        components: List<ComponentNodeApiEntity> = contractTree(),
    ) = requestOf(tokens = tokens.toList(), components = components)

    private fun requestOf(
        tokens: List<TokenApiEntity>,
        components: List<ComponentNodeApiEntity> = contractTree(),
    ) = LookupRequestApiEntity(
        learningLanguage = "es",
        translationLanguage = "ru",
        tokens = tokens,
        components = components,
    )

    private fun token(text: String, confidence: Float = 0.9f, language: String? = "es") =
        TokenApiEntity(text = text, confidence = confidence, language = language)

    /** Дерево словаря из контракта: «Перевод», «Часть речи» с «Родом» под «существительное», «Пример», «Толкование». */
    private fun contractTree(): List<ComponentNodeApiEntity> = listOf(
        component(1L, "translation", "Перевод", "text", core = true),
        component(
            2L, "part_of_speech", "Часть речи", "choice",
            options = listOf(
                ComponentOptionNodeApiEntity(
                    id = 21L, key = "noun", label = "существительное",
                    children = listOf(
                        component(
                            5L, null, "Род", "choice",
                            options = listOf(
                                ComponentOptionNodeApiEntity(51L, null, "м.р.", emptyList()),
                                ComponentOptionNodeApiEntity(52L, null, "ж.р.", emptyList()),
                            ),
                        ),
                    ),
                ),
                ComponentOptionNodeApiEntity(22L, "verb", "глагол", emptyList()),
            ),
        ),
        component(3L, "example", "Пример", "captioned_text", multiple = true),
        component(4L, null, "Толкование", "text"),
    )

    private fun component(
        id: Long,
        key: String?,
        name: String,
        template: String,
        core: Boolean = false,
        multiple: Boolean = false,
        options: List<ComponentOptionNodeApiEntity> = emptyList(),
    ) = ComponentNodeApiEntity(
        id = id,
        key = key,
        name = name,
        template = template,
        core = core,
        multiple = multiple,
        options = options,
        children = emptyList(),
    )

    private fun errorBody(code: String) = """{"code":"$code","message":"test"}"""

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "no fixture $name" }
            .bufferedReader()
            .use { it.readText() }

    private companion object {
        const val ACCESS_KEY = "test-access-key-0123456789abcdefghijklmnopqrstuvwxyz"
        const val EMPTY_RESPONSE = """{"attribution":"Wiktionary, CC BY-SA 4.0","words":[],"recommendations":[]}"""
    }
}
