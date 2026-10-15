package me.apomazkin.core_network_impl

import kotlinx.coroutines.test.runTest
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.LookupRequest
import me.apomazkin.core_network_api.entity.LookupResult
import me.apomazkin.core_network_api.entity.TokenQuery
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
 * Слой сети против поддельного сервера (Ш6: WFT-19…23, В5: WFT-63, WFT-05).
 * Адреса строк спеки в именах — `WFT-NN_k` (точка в имени метода JVM
 * запрещена; скрипт покрытия понимает оба разделителя).
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

    // === Ш6: запрос ===

    @Test
    fun `WFT-20_1 request carries tokens in order with confidence language dictionary languages and bearer header`() = runTest {
        server.enqueue(MockResponse(code = 200, body = EMPTY_RESPONSE))

        api().lookup(requestOf(token("corrían", 0.97f, "es"), token("fuera,", 0.91f, "es")), runId = 7)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/words/lookup", recorded.url.encodedPath)
        assertEquals("Bearer $ACCESS_KEY", recorded.headers["Authorization"])
        assertEquals("7", recorded.headers["X-Run-Id"])
        val body = recorded.body?.utf8() ?: ""
        assertTrue(body, body.contains("\"learningLanguage\":\"es\""))
        assertTrue(body, body.contains("\"translationLanguage\":\"ru\""))
        assertTrue(
                body,
                body.indexOf("\"text\":\"corrían\"") < body.indexOf("\"text\":\"fuera,\""),
        )
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

    // === Ш6: ожидание ===

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

    // === Ш6: сервер недоступен или не по контракту ===

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

    // === В5: отказ ===

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
    fun `WFT-23_2 status 400 invalid_request is our bug and is unavailable`() = runTest {
        server.enqueue(MockResponse(code = 400, body = errorBody("invalid_request")))

        val result = api().lookup(requestOf(token("perro")), runId = 1)

        assertTrue(result.toString(), result is LookupResult.Unavailable)
    }

    // === Инвариант: ключ не утекает ===

    @Test
    fun `WFT-05_1 access key never appears in the request log`() = runTest {
        server.enqueue(MockResponse(code = 401, body = errorBody("unauthorized")))

        api().lookup(requestOf(token("perro")), runId = 1)

        assertFalse(logLines.isEmpty())
        assertTrue(logLines.toString(), logLines.none { it.contains(ACCESS_KEY) })
    }

    // === Ш10 → Ш11: ответ сервера, записанный с прода 2026-10-10 ===

    @Test
    fun `WFP-16_1 recorded response gives words in response order with attribution`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_sample.json")))

        val result = api().lookup(requestOf(token("corrían")), runId = 1) as LookupResult.Success

        assertEquals("Wiktionary, CC BY-SA 4.0", result.attribution)
        assertEquals(listOf("corrían", "fuera", "accorc", "los"), result.words.map { it.text })
    }

    @Test
    fun `WFP-16_7 recorded corrian is found with first candidate correr verb and a form note`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_sample.json")))

        val result = api().lookup(requestOf(token("corrían")), runId = 1) as LookupResult.Success

        val corrian = result.words.first { it.text == "corrían" }
        assertEquals(WordStatus.FOUND, corrian.status)
        assertEquals("correr", corrian.candidates.first().lemma)
        assertEquals("verb", corrian.candidates.first().partOfSpeech)
        assertEquals("third-person plural imperfect indicative", corrian.candidates.first().formNote)
        assertEquals("to run, to jog", corrian.candidates.first().senses.first().gloss)
    }

    @Test
    fun `WFP-16_8 recorded fuera keeps four candidates in article order`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_sample.json")))

        val result = api().lookup(requestOf(token("fuera")), runId = 1) as LookupResult.Success

        val fuera = result.words.first { it.text == "fuera" }
        assertEquals(
                listOf("fuera/adverb", "fuera/interjection", "ser/verb", "ir/verb"),
                fuera.candidates.map { "${it.lemma}/${it.partOfSpeech}" },
        )
        assertNull(fuera.candidates.first().formNote)
    }

    @Test
    fun `WFP-16_3 recorded accorc is not found with no candidates`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_sample.json")))

        val result = api().lookup(requestOf(token("accorc")), runId = 1) as LookupResult.Success

        val accorc = result.words.first { it.text == "accorc" }
        assertEquals(WordStatus.NOT_FOUND, accorc.status)
        assertTrue(accorc.candidates.isEmpty())
    }

    // === Ш10: леммы ответа с лексемами-кандидатами, записано с прода v0.1.9 (IS532) ===

    @Test
    fun `WFT-50_2 old recorded response without lemmas parses with empty lemmas`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_sample.json")))

        val result = api().lookup(requestOf(token("corrían")), runId = 1) as LookupResult.Success

        assertTrue(result.words.all { it.lemmas.isEmpty() })
        assertEquals(4, result.words.size)
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
    fun `WFT-71_1 WFT-72_1 recorded run carries 130 lexemes with key source and part of speech key`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_v0_1_9_en.json")))

        val result = api().lookup(requestOf(token("run")), runId = 1) as LookupResult.Success

        val run = result.words.first { it.text == "run" }.lemmas.first { it.text == "run" }
        assertEquals(130, run.lexemes.size)
        assertEquals(71, run.lexemes.count { it.partOfSpeechKey == "verb" })
        assertEquals(54, run.lexemes.count { it.partOfSpeechKey == "noun" })
        assertEquals(5, run.lexemes.count { it.partOfSpeechKey == "adjective" })
        assertEquals(setOf("ruwiktionary", "enwiktionary"), run.lexemes.map { it.source }.toSet())
        assertEquals(130, run.lexemes.map { it.key }.toSet().size)
        assertEquals(listOf("бежать, бегать"), run.lexemes.first().translations)
        val london = result.words.first { it.text == "london" }.lemmas.single()
        assertEquals("noun", london.lexemes.first { it.source == "ruwiktionary" }.partOfSpeechKey)
    }

    @Test
    fun `WFT-71_11 WFT-71_3 recorded russian lexeme of London has null gloss caption parent key and keeps its example`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_v0_1_9_en.json")))

        val result = api().lookup(requestOf(token("London")), runId = 1) as LookupResult.Success

        val lexeme = result.words.first { it.text == "london" }.lemmas.single().lexemes.first()
        assertEquals("ruwiktionary", lexeme.source)
        assertEquals(listOf("Лондон (город в Великобритании)"), lexeme.translations)
        assertNull(lexeme.gloss)
        assertNull(lexeme.caption)
        assertNull(lexeme.parentKey)
        assertFalse(lexeme.parent)
        assertEquals(listOf("Eventually, she got a job and moved to London."), lexeme.examples)
        assertTrue(lexeme.notes.isEmpty())
    }

    @Test
    fun `WFT-71_8 recorded run has parent lexemes and children pointing at them`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_v0_1_9_en.json")))

        val result = api().lookup(requestOf(token("run")), runId = 1) as LookupResult.Success

        val run = result.words.first { it.text == "run" }.lemmas.first { it.text == "run" }
        val parent = run.lexemes.first { it.parent }
        assertEquals("To move swiftly.", parent.gloss)
        assertNull(parent.parentKey)
        val child = run.lexemes.first { it.parentKey == parent.key }
        assertEquals("enwiktionary", child.source)
        assertTrue(child.gloss!!.startsWith("To move forward quickly upon two feet"))
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
        assertTrue(correr.lexemes.all { it.partOfSpeechKey == "verb" })
    }

    @Test
    fun `WFT-76_3 WFT-73_1 recorded fuera gives three lemmas and amigo ends with the russian adjective`() = runTest {
        server.enqueue(MockResponse(code = 200, body = fixture("lookup_response_v0_1_9_es.json")))

        val result = api().lookup(requestOf(token("fuera")), runId = 1) as LookupResult.Success

        val fuera = result.words.first { it.text == "fuera" }
        assertEquals(listOf("fuera", "ser", "ir"), fuera.lemmas.map { it.text })
        assertEquals(setOf("adverb", "interjection"), fuera.lemmas[0].lexemes.map { it.partOfSpeechKey }.toSet())
        val amigo = result.words.first { it.text == "amigo" }.lemmas.first { it.text == "amigo" }
        assertEquals("adjective", amigo.lexemes.last().partOfSpeechKey)
        assertEquals(listOf("дружественный; дружеский, приятельский"), amigo.lexemes.last().translations)
    }

    @Test
    fun `unknown source part of speech key and extra fields keep the lexeme and the response stays a success`() = runTest {
        server.enqueue(
                MockResponse(
                        code = 200,
                        body = """{"attribution":"Wiktionary, CC BY-SA 4.0","served":"cache","words":[{"text":"run","status":"found","candidates":[],"lemmas":[{"text":"run","formNote":null,"lexemes":[{"key":"cambridge/1","partOfSpeechKey":"gerund","source":"cambridge","translations":["бег"],"level":"B1"}]}]}]}""",
                )
        )

        val result = api().lookup(requestOf(token("run")), runId = 1)

        val success = result as LookupResult.Success
        val lexeme = success.words.single().lemmas.single().lexemes.single()
        assertEquals("cambridge", lexeme.source)
        assertEquals("gerund", lexeme.partOfSpeechKey)
        assertEquals(listOf("бег"), lexeme.translations)
        assertNull(lexeme.gloss)
        assertTrue(lexeme.examples.isEmpty())
    }

    @Test
    fun `lexeme without key or source is off the schema and makes the response unavailable`() = runTest {
        server.enqueue(
                MockResponse(
                        code = 200,
                        body = """{"attribution":"x","words":[{"text":"run","status":"found","candidates":[],"lemmas":[{"text":"run","lexemes":[{"translations":["бег"]}]}]}]}""",
                )
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

    private fun requestOf(vararg tokens: TokenQuery) = LookupRequest(
            learningLanguage = "es",
            translationLanguage = "ru",
            tokens = tokens.toList(),
    )

    private fun token(text: String, confidence: Float = 0.9f, language: String? = "es") =
        TokenQuery(text = text, confidence = confidence, language = language)

    private fun errorBody(code: String) = """{"code":"$code","message":"test"}"""

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "no fixture $name" }
            .bufferedReader()
            .use { it.readText() }

    private companion object {
        const val ACCESS_KEY = "test-access-key-0123456789abcdefghijklmnopqrstuvwxyz"
        const val EMPTY_RESPONSE = """{"attribution":"Wiktionary, CC BY-SA 4.0","words":[]}"""
    }
}
