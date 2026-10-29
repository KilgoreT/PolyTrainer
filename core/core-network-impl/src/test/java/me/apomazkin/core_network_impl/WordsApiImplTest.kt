package me.apomazkin.core_network_impl

import kotlinx.coroutines.test.runTest
import me.apomazkin.core_network_api.entity.ComponentNodeApiEntity
import me.apomazkin.core_network_api.entity.ComponentOptionNodeApiEntity
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.LemmaContextApiEntity
import me.apomazkin.core_network_api.entity.LemmasRequestApiEntity
import me.apomazkin.core_network_api.entity.LemmasResult
import me.apomazkin.core_network_api.entity.LexemesRequestApiEntity
import me.apomazkin.core_network_api.entity.LexemesResult
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
 * Слой сети против поддельного сервера: запрос `lexemesFromLemma` (ACC-19,
 * LFL-21…23, LFL-78, LFL-103, LFL-104), отказы по причине (WCL-11.2, WCL-11.6,
 * ACC-05), ответ по контракту 0.5.0 (раздел 2) на записанных ответах прода
 * v0.1.37; операция `lemmasFromTokens` по контракту 0.4.0 (LFT-02, TFP-26).
 * Адреса строк спеки в именах — `LFL-NN_k`, `ACC-NN_k`, `LFT-NN_k`,
 * `TFP-NN_k`, `WCL-NN_k` (точка в имени метода JVM запрещена; скрипт
 * покрытия понимает оба разделителя).
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
    fun `LFL-103_1 request carries the lemma the text and translation of the photo the dictionary languages the tree the path and the headers`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lexemes_response_v0_1_37_tap_es.json")))
        val context = LemmaContextApiEntity(text = "Las nubes corrían por el cielo.", translation = "Облака бежали по небу.")

        api().lexemesFromLemma(lexemesRequestOf("correr", context = context), runId = 7)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/lexemes/from-lemma", recorded.url.encodedPath)
        assertEquals("Bearer $ACCESS_KEY", recorded.headers["Authorization"])
        assertEquals("7", recorded.headers["X-Run-Id"])
        val body = recorded.body?.utf8() ?: ""
        assertTrue(body, body.contains("\"learningLanguage\":\"es\""))
        assertTrue(body, body.contains("\"translationLanguage\":\"ru\""))
        assertTrue(body, body.contains("\"lemma\":\"correr\""))
        assertTrue(body, body.contains("\"context\":{\"text\":\"Las nubes corrían por el cielo.\",\"translation\":\"Облака бежали по небу.\"}"))
        assertFalse(body, body.contains("tokens"))
    }

    @Test
    fun `LFL-104_1 LFL-78_7 request of the card has no context and a dictionary without components sends no components field`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lexemes_response_v0_1_37_card_es.json")))

        api().lexemesFromLemma(lexemesRequestOf("perro", context = null, components = emptyList()), runId = 1)

        val body = server.takeRequest().body?.utf8() ?: ""
        assertTrue(body, body.contains("\"lemma\":\"perro\""))
        assertFalse(body, body.contains("context"))
        assertFalse(body, body.contains("components"))
    }

    @Test
    fun `LFL-78_1 LFL-78_2 LFL-78_3 the tree goes as in the contract - built-in keys own option without key and the dependent component inside the option`() = runTest {
        server.enqueue(MockResponse(code = 200, body = NOT_FOUND_RESPONSE))

        api().lexemesFromLemma(lexemesRequestOf("perro", components = contractTree()), runId = 1)

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
    fun `ACC-19_1 empty access key sends nothing and is denied as no access key`() = runTest {
        server.enqueue(MockResponse(code = 200, body = NOT_FOUND_RESPONSE))

        val result = api(accessKey = "").lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertEquals(LexemesResult.Denied(DenyReason.NO_ACCESS_KEY), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `LFL-21_1 LFL-21_2 http base url including loopback is refused without a connection`() = runTest {
        server.enqueue(MockResponse(code = 200, body = NOT_FOUND_RESPONSE))

        val result = api(requireHttps = true).lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertTrue(result.toString(), result is LexemesResult.Unavailable)
        assertEquals(0, server.requestCount)
    }

    // === Ш1: ожидание ===

    @Test
    fun `LFL-22_1 silent server past the call timeout is unavailable`() = runTest {
        server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.Stall).build())

        val result = api(callTimeout = 300.milliseconds).lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertTrue(result.toString(), result is LexemesResult.Unavailable)
    }

    @Test
    fun `LFL-22_2 answer after the call timeout is dropped as unavailable`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .headersDelay(900, TimeUnit.MILLISECONDS)
                .body(NOT_FOUND_RESPONSE)
                .build(),
        )

        val result = api(callTimeout = 300.milliseconds).lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertTrue(result.toString(), result is LexemesResult.Unavailable)
    }

    @Test
    fun `LFL-22_3 read and write timeouts equal the call timeout so a slow server is awaited the full 180 seconds`() {
        val options = NetworkOptions(baseUrl = "https://api.lexeme.pro/", accessKey = ACCESS_KEY, callTimeout = 180.seconds)

        val client = LexemeServiceFactory.httpClient(options, logger)

        assertEquals(180_000, client.callTimeoutMillis)
        assertEquals(180_000, client.readTimeoutMillis)
        assertEquals(180_000, client.writeTimeoutMillis)
        assertEquals(10_000, client.connectTimeoutMillis)
    }

    @Test
    fun `LFL-22_3 answer within the call timeout is a success`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .headersDelay(200, TimeUnit.MILLISECONDS)
                .body(NOT_FOUND_RESPONSE)
                .build(),
        )

        val result = api(callTimeout = 2.seconds).lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertTrue(result.toString(), result is LexemesResult.Success)
    }

    // === Ш1: сервер недоступен или не по контракту ===

    @Test
    fun `LFL-23_1 refused connection is unavailable`() = runTest {
        val url = server.url("/").toString()
        server.close()

        val result = api(baseUrl = url).lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertTrue(result.toString(), result is LexemesResult.Unavailable)
    }

    @Test
    fun `LFL-23_2 status 500 is unavailable`() = runTest {
        server.enqueue(MockResponse(code = 500, body = "boom"))

        val result = api().lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertTrue(result.toString(), result is LexemesResult.Unavailable)
    }

    @Test
    fun `LFL-23_3 status 200 with a body off the schema is unavailable`() = runTest {
        server.enqueue(MockResponse(code = 200, body = """{"attribution":"x","words":[]}"""))

        val result = api().lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertTrue(result.toString(), result is LexemesResult.Unavailable)
    }

    @Test
    fun `LFL-23_4 status 400 invalid_request is our bug and is unavailable`() = runTest {
        server.enqueue(MockResponse(code = 400, body = errorBody("invalid_request")))

        val result = api().lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertTrue(result.toString(), result is LexemesResult.Unavailable)
    }

    @Test
    fun `LFL-23_5 status 503 with a body off the error schema is unavailable`() = runTest {
        server.enqueue(MockResponse(code = 503, body = "<html>Service Unavailable</html>"))

        val result = api().lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertTrue(result.toString(), result is LexemesResult.Unavailable)
    }

    // === В1, В2: отказ по причине ===

    @Test
    fun `WCL-11_2 status 401 is denied as unauthorized`() = runTest {
        server.enqueue(MockResponse(code = 401, body = errorBody("unauthorized")))

        val result = api().lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertEquals(LexemesResult.Denied(DenyReason.UNAUTHORIZED), result)
    }

    @Test
    fun `WCL-11_2 status 429 is denied as rate limited`() = runTest {
        server.enqueue(MockResponse(code = 429, body = errorBody("rate_limited")))

        val result = api().lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertEquals(LexemesResult.Denied(DenyReason.RATE_LIMITED), result)
    }

    @Test
    fun `WCL-11_2 status 413 is denied as request too large`() = runTest {
        server.enqueue(MockResponse(code = 413, body = errorBody("request_too_large")))

        val result = api().lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertEquals(LexemesResult.Denied(DenyReason.REQUEST_TOO_LARGE), result)
    }

    @Test
    fun `WCL-11_2 status 400 unsupported_language is denied as unsupported language`() = runTest {
        server.enqueue(MockResponse(code = 400, body = errorBody("unsupported_language")))

        val result = api().lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertEquals(LexemesResult.Denied(DenyReason.UNSUPPORTED_LANGUAGE), result)
    }

    @Test
    fun `WCL-11_6 status 503 llm_unavailable is denied as llm unavailable`() = runTest {
        server.enqueue(MockResponse(code = 503, body = errorBody("llm_unavailable")))

        val result = api().lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertEquals(LexemesResult.Denied(DenyReason.LLM_UNAVAILABLE), result)
    }

    // === Инвариант: ключ не утекает ===

    @Test
    fun `ACC-05_1 access key never appears in the request log`() = runTest {
        server.enqueue(MockResponse(code = 401, body = errorBody("unauthorized")))

        api().lexemesFromLemma(lexemesRequestOf("perro"), runId = 1)

        assertFalse(logLines.isEmpty())
        assertTrue(logLines.toString(), logLines.none { it.contains(ACCESS_KEY) })
    }

    // === Ш8: ответы прода v0.1.37 по контракту 0.5.0 (раздел 2) ===

    @Test
    fun `LFL-113_1 LFL-87_2 recorded tap correr gives one lemma with its lexeme marked in context and the matches`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lexemes_response_v0_1_37_tap_es.json")))

        val result = api().lexemesFromLemma(lexemesRequestOf("correr"), runId = 1) as LexemesResult.Success

        assertEquals("Wiktionary, CC BY-SA 4.0", result.attribution)
        assertTrue(result.recommendations.isEmpty())
        assertEquals("correr", result.word.text)
        assertEquals(WordStatus.FOUND, result.word.status)
        assertTrue(result.word.remarks.isEmpty())
        val correr = result.word.lemmas.single()
        assertEquals("correr", correr.text)
        assertNull(correr.formNote)
        assertEquals(listOf("/koˈreɾ/", "[koˈreɾ]", "[ko'rer]"), correr.pronunciations)
        val running = correr.lexemes.single()
        assertEquals("ruwiktionary/1.verb.1", running.key)
        assertTrue(running.inContext)
        assertEquals(listOf("бегать; бежать"), running.translations)
        assertEquals(mapOf("transitivity" to "intransitive"), running.grammar)
        assertEquals(listOf(1L, 2L), running.componentMatches.map { it.componentId })
        assertEquals(listOf(null, 22L), running.componentMatches.map { it.optionId })
        assertEquals(listOf("translations", null), running.componentMatches.map { it.field })
    }

    @Test
    fun `LFL-113_2 LFL-90_1 recorded card perro gives three lexemes in selection order none in context`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lexemes_response_v0_1_37_card_es.json")))

        val result = api().lexemesFromLemma(lexemesRequestOf("perro", context = null), runId = 1) as LexemesResult.Success

        val perro = result.word.lemmas.single()
        assertEquals(listOf("ruwiktionary/1.noun.1", "ruwiktionary/1.noun.2", "ruwiktionary/2.adjective.1"), perro.lexemes.map { it.key })
        assertTrue(perro.lexemes.none { it.inContext })
        assertEquals(listOf("noun", "noun", "adjective"), perro.lexemes.map { it.partOfSpeechKey })
        assertEquals(21L, perro.lexemes[0].componentMatches[1].optionId)
        assertEquals(listOf("разг."), perro.lexemes[2].notes)
        assertEquals(listOf(1L), perro.lexemes[2].componentMatches.map { it.componentId })
        assertEquals(5, perro.pronunciations.size)
    }

    @Test
    fun `LFL-50_2 recorded not found and error words have the status and no lemmas`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lexemes_response_v0_1_37_not_found.json")))
        server.enqueue(MockResponse(code = 200, body = fixture("lexemes_response_v0_1_37_error.json")))

        val notFound = api().lexemesFromLemma(lexemesRequestOf("xyzzyq", context = null), runId = 1) as LexemesResult.Success
        val error = api().lexemesFromLemma(lexemesRequestOf("zarpar"), runId = 2) as LexemesResult.Success

        assertEquals("xyzzyq", notFound.word.text)
        assertEquals(WordStatus.NOT_FOUND, notFound.word.status)
        assertEquals(WordStatus.ERROR, error.word.status)
        assertTrue(notFound.word.lemmas.isEmpty() && error.word.lemmas.isEmpty())
        assertTrue(notFound.word.remarks.isEmpty() && error.word.remarks.isEmpty())
    }

    @Test
    fun `LFL-91_1 LFL-113_2 lexeme without translations carries its gloss the word a no_translation remark and a missing inContext parses as false`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"attribution":"Wiktionary, CC BY-SA 4.0","recommendations":[],"word":{"text":"zarpó","status":"found",""" +
                    """"remarks":[{"code":"no_translation","lemma":"zarpar","lexemeKey":"enwiktionary/1.verb.1"}],""" +
                    """"lemmas":[{"text":"zarpar","formNote":"third-person singular preterite indicative","pronunciations":[],""" +
                    """"lexemes":[{"key":"enwiktionary/1.verb.1","partOfSpeechKey":"verb","source":"enwiktionary","translations":[],""" +
                    """"gloss":"to embark; to set sail","level":"B1","synonyms":["salir"],"antonyms":["atracar"],""" +
                    """"componentMatches":[{"componentId":4,"optionId":null,"field":"gloss"},{"componentId":2,"optionId":22,"field":null}]}]}]}}""",
            ),
        )

        val result = api().lexemesFromLemma(lexemesRequestOf("zarpó"), runId = 1) as LexemesResult.Success

        val remark = result.word.remarks.single()
        assertEquals("no_translation", remark.code)
        assertEquals("zarpar", remark.lemma)
        assertEquals("enwiktionary/1.verb.1", remark.lexemeKey)
        val zarpar = result.word.lemmas.single()
        assertEquals("third-person singular preterite indicative", zarpar.formNote)
        val embark = zarpar.lexemes.single()
        assertEquals("to embark; to set sail", embark.gloss)
        assertTrue(embark.translations.isEmpty())
        assertFalse(embark.inContext)
        assertEquals("B1", embark.level)
        assertEquals(listOf("salir"), embark.synonyms)
        assertEquals(listOf("atracar"), embark.antonyms)
        assertEquals(listOf("gloss", null), embark.componentMatches.map { it.field })
    }

    @Test
    fun `LFL-88_1 a component_unclear remark carries the component id`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"attribution":"Wiktionary, CC BY-SA 4.0","recommendations":[],"word":{"text":"perro","status":"found","remarks":[{"code":"component_unclear","componentId":7}],"lemmas":[]}}""",
            ),
        )

        val result = api().lexemesFromLemma(lexemesRequestOf("perro"), runId = 1) as LexemesResult.Success

        val remark = result.word.remarks.single()
        assertEquals("component_unclear", remark.code)
        assertEquals(7L, remark.componentId)
        assertNull(remark.lemma)
    }

    @Test
    fun `LFL-90_8 an unknown remark code and an unknown recommendation template stay strings`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"attribution":"Wiktionary, CC BY-SA 4.0","recommendations":[{"code":"component_recommended","name":"Картинка","template":"image"}],"word":{"text":"perro","status":"found","remarks":[{"code":"new_code"}],"lemmas":[]}}""",
            ),
        )

        val result = api().lexemesFromLemma(lexemesRequestOf("perro"), runId = 1) as LexemesResult.Success

        assertEquals("new_code", result.word.remarks.single().code)
        assertEquals("image", result.recommendations.single().template)
        assertTrue(result.recommendations.single().options.isEmpty())
    }

    @Test
    fun `unknown source part of speech key and extra fields keep the lexeme and the response stays a success`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"attribution":"Wiktionary, CC BY-SA 4.0","served":"cache","word":{"text":"run","status":"found","lemmas":[{"text":"run","formNote":null,"lexemes":[{"key":"cambridge/1","partOfSpeechKey":"gerund","source":"cambridge","translations":["бег"],"level":"B1","extra":1}]}]}}""",
            ),
        )

        val result = api().lexemesFromLemma(lexemesRequestOf("run"), runId = 1)

        val success = result as LexemesResult.Success
        val lexeme = success.word.lemmas.single().lexemes.single()
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
                body = """{"attribution":"x","word":{"text":"run","status":"found","lemmas":[{"text":"run","lexemes":[{"translations":["бег"]}]}]}}""",
            ),
        )

        val result = api().lexemesFromLemma(lexemesRequestOf("run"), runId = 1)

        assertTrue(result is LexemesResult.Unavailable)
    }

    // === lemmasFromTokens (LFT, IS537): запрос, ожидание, отказы, ответ ===

    @Test
    fun `LFT-02_1 LFT-02_3 lemmas request carries the twelve tokens in order with repeats confidence and language the dictionary languages and the headers and no components`() = runTest {
        server.enqueue(MockResponse(code = 200, body = LEMMAS_RESPONSE))
        val texts = listOf(
            "Las", "nubes", "corrían", "por", "el", "cielo.",
            "El", "perro", "duerme", "en", "el", "jardín.",
        )
        val request = lemmasRequestOf(texts.map { token(it, 0.97f, "es") })

        api().lemmasFromTokens(request, runId = 7)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/lemmas/from-tokens", recorded.url.encodedPath)
        assertEquals("Bearer $ACCESS_KEY", recorded.headers["Authorization"])
        assertEquals("7", recorded.headers["X-Run-Id"])
        val body = recorded.body?.utf8() ?: ""
        assertTrue(body, body.contains("\"learningLanguage\":\"es\""))
        assertTrue(body, body.contains("\"translationLanguage\":\"ru\""))
        val sent = Regex("\"text\":\"([^\"]+)\"").findAll(body).map { it.groupValues[1] }.toList()
        assertEquals(texts, sent)
        assertTrue(body, body.contains("\"confidence\":0.97"))
        assertTrue(body, body.contains("\"language\":\"es\""))
        assertFalse(body, body.contains("components"))
    }

    @Test
    fun `LFT-02_2 lemmas token without language is sent as null`() = runTest {
        server.enqueue(MockResponse(code = 200, body = LEMMAS_RESPONSE))

        api().lemmasFromTokens(lemmasRequestOf(token("cielo.", 0.97f, null)), runId = 1)

        val body = server.takeRequest().body?.utf8() ?: ""
        assertTrue(body, body.contains("\"language\":null"))
    }

    @Test
    fun `lemmas 200 body by the contract gives the text the translation and four lists and a missing list parses as empty`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"text":"Las nubes corrían por el cielo.",""" +
                    """"translation":"Облака бежали по небу.",""" +
                    """"lexical":["nube","correr","cielo"],"function":["el","por"],"unparsed":[]}""",
            ),
        )

        val result = api().lemmasFromTokens(lemmasRequestOf(token("Las")), runId = 1)

        val expected = LemmasResult.Success(
            text = "Las nubes corrían por el cielo.",
            translation = "Облака бежали по небу.",
            lexical = listOf("nube", "correr", "cielo"),
            proper = emptyList(),
            function = listOf("el", "por"),
            unparsed = emptyList(),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `LFL-23_3 lemmas status 200 with a body off the schema is unavailable`() = runTest {
        server.enqueue(MockResponse(code = 200, body = """{"hello":"world"}"""))

        val result = api().lemmasFromTokens(lemmasRequestOf(token("Las")), runId = 1)

        assertTrue(result.toString(), result is LemmasResult.Unavailable)
    }

    @Test
    fun `ACC-19_1 lemmas with an empty access key sends nothing and is denied as no access key`() = runTest {
        server.enqueue(MockResponse(code = 200, body = LEMMAS_RESPONSE))

        val result = api(accessKey = "").lemmasFromTokens(lemmasRequestOf(token("Las")), runId = 1)

        assertEquals(LemmasResult.Denied(DenyReason.NO_ACCESS_KEY), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `LFL-21_1 lemmas over an http base url is refused without a connection`() = runTest {
        server.enqueue(MockResponse(code = 200, body = LEMMAS_RESPONSE))

        val result = api(requireHttps = true)
            .lemmasFromTokens(lemmasRequestOf(token("Las")), runId = 1)

        assertTrue(result.toString(), result is LemmasResult.Unavailable)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `LFL-22_1 lemmas silent server past the call timeout is unavailable`() = runTest {
        server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.Stall).build())

        val result = api(callTimeout = 300.milliseconds)
            .lemmasFromTokens(lemmasRequestOf(token("Las")), runId = 1)

        assertTrue(result.toString(), result is LemmasResult.Unavailable)
    }

    @Test
    fun `LFL-23_2 TFP-26_1 lemmas status 500 and a refused connection are unavailable`() = runTest {
        server.enqueue(MockResponse(code = 500, body = "boom"))
        val url = server.url("/").toString()

        val failed = api(baseUrl = url).lemmasFromTokens(lemmasRequestOf(token("Las")), runId = 1)
        server.close()
        val refused = api(baseUrl = url).lemmasFromTokens(lemmasRequestOf(token("Las")), runId = 2)

        assertTrue(failed.toString(), failed is LemmasResult.Unavailable)
        assertTrue(refused.toString(), refused is LemmasResult.Unavailable)
    }

    @Test
    fun `TFP-26_2 lemmas 503 llm_unavailable is denied as llm unavailable and the retry sends the same tokens with a new run id`() = runTest {
        server.enqueue(MockResponse(code = 503, body = errorBody("llm_unavailable")))
        server.enqueue(MockResponse(code = 200, body = LEMMAS_RESPONSE))
        val request = lemmasRequestOf(token("Las"), token("nubes"))

        val first = api().lemmasFromTokens(request, runId = 1)
        val retry = api().lemmasFromTokens(request, runId = 2)

        assertEquals(LemmasResult.Denied(DenyReason.LLM_UNAVAILABLE), first)
        assertTrue(retry.toString(), retry is LemmasResult.Success)
        val sent = listOf(server.takeRequest(), server.takeRequest())
        assertEquals(sent[0].body?.utf8(), sent[1].body?.utf8())
        assertEquals(listOf("1", "2"), sent.map { it.headers["X-Run-Id"] })
    }

    @Test
    fun `TFP-26_3 lemmas 401 429 413 and 400 unsupported_language are denied by reason`() = runTest {
        server.enqueue(MockResponse(code = 401, body = errorBody("unauthorized")))
        server.enqueue(MockResponse(code = 429, body = errorBody("rate_limited")))
        server.enqueue(MockResponse(code = 413, body = errorBody("request_too_large")))
        server.enqueue(MockResponse(code = 400, body = errorBody("unsupported_language")))
        server.enqueue(MockResponse(code = 400, body = errorBody("invalid_request")))

        val results = (1..5).map {
            api().lemmasFromTokens(lemmasRequestOf(token("Las")), runId = it.toLong())
        }

        assertEquals(LemmasResult.Denied(DenyReason.UNAUTHORIZED), results[0])
        assertEquals(LemmasResult.Denied(DenyReason.RATE_LIMITED), results[1])
        assertEquals(LemmasResult.Denied(DenyReason.REQUEST_TOO_LARGE), results[2])
        assertEquals(LemmasResult.Denied(DenyReason.UNSUPPORTED_LANGUAGE), results[3])
        assertTrue(results[4].toString(), results[4] is LemmasResult.Unavailable)
    }

    @Test
    fun `ACC-05_1 lemmas access key never appears in the request log`() = runTest {
        server.enqueue(MockResponse(code = 401, body = errorBody("unauthorized")))

        api().lemmasFromTokens(lemmasRequestOf(token("Las")), runId = 1)

        assertFalse(logLines.isEmpty())
        assertTrue(logLines.toString(), logLines.none { it.contains(ACCESS_KEY) })
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

    private fun lexemesRequestOf(
        lemma: String,
        context: LemmaContextApiEntity? = LemmaContextApiEntity(text = "El perro duerme.", translation = "Собака спит."),
        components: List<ComponentNodeApiEntity> = contractTree(),
    ) = LexemesRequestApiEntity(
        learningLanguage = "es",
        translationLanguage = "ru",
        lemma = lemma,
        context = context,
        components = components,
    )

    private fun lemmasRequestOf(vararg tokens: TokenApiEntity) = lemmasRequestOf(tokens.toList())

    private fun lemmasRequestOf(tokens: List<TokenApiEntity>) = LemmasRequestApiEntity(
        learningLanguage = "es",
        translationLanguage = "ru",
        tokens = tokens,
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
        const val NOT_FOUND_RESPONSE =
            """{"attribution":"Wiktionary, CC BY-SA 4.0","recommendations":[],"word":{"text":"perro","status":"not_found","remarks":[],"lemmas":[]}}"""
        const val LEMMAS_RESPONSE = """{"text":"Las nubes","translation":"Облака",""" +
            """"lexical":["nube"],"proper":[],"function":["el"],"unparsed":[]}"""
    }
}
