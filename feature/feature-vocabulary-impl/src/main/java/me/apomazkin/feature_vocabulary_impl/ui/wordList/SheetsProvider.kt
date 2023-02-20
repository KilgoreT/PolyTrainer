package me.apomazkin.feature_vocabulary_impl.ui.wordList

import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.core_db_api.entity.*
import me.apomazkin.feature_vocabulary_impl.ui.wordList.builder.CellSeal
import me.apomazkin.feature_vocabulary_impl.ui.wordList.builder.RowLine
import me.apomazkin.testsheets.builder.SheetDoc
import me.apomazkin.testsheets.builder.SheetPage
import java.util.*

class SheetsProvider(
    accountCredential: GoogleAccountCredential,
) {
    private val transport = NetHttpTransport()
    val jsonFactory = JacksonFactory()

    private val sheetsApi: Sheets

    init {

        sheetsApi = Sheets
            .Builder(
                transport,
                jsonFactory,
                accountCredential
            )
            .setApplicationName("SheetTEst")
            .build()

    }

    suspend fun createSpreadsheet(title: String, dump: Dump) {

        val languagePageBuilder = SheetPage.Builder()
            .title("Language")
        dump.languages.forEach { languageDump: LanguageDump ->
            languagePageBuilder
                .addLine(
                    RowLine.Builder()
                        .addCell(CellSeal.CellLong(languageDump.id))
                        .addCell(CellSeal.CellLong(languageDump.numericCode.toLong()))
                        .addCell(CellSeal.CellString(languageDump.code))
                        .addCell(CellSeal.CellString(languageDump.name))
                        .addCell(CellSeal.CellDate(languageDump.addDate))
                        .addCell(CellSeal.CellDate(languageDump.changeDate))
                        .build()
                )
        }

        val wordPageBuilder = SheetPage.Builder()
            .title("Word")
        dump.words.forEach { word: WordDump ->
            wordPageBuilder
                .addLine(
                    RowLine.Builder()
                        .addCell(CellSeal.CellLong(word.id))
                        .addCell(CellSeal.CellLong(word.langId))
                        .addCell(CellSeal.CellString(word.word))
                        .addCell(CellSeal.CellDate(word.addDate))
                        .addCell(CellSeal.CellDate(word.changeDate))
                        .build()
                )

        }
        val definitionPageBuilder = SheetPage.Builder()
            .title("Definition")
        dump.definitions.forEach { definitionDump: DefinitionDump ->
            definitionPageBuilder
                .addLine(
                    RowLine.Builder()
                        .addCell(CellSeal.CellLong(definitionDump.id))
                        .addCell(CellSeal.CellLong(definitionDump.wordId))
                        .addCell(CellSeal.CellString(definitionDump.definition))
                        .addCell(CellSeal.CellString(definitionDump.wordClass))
                        .addCell(CellSeal.CellLong(definitionDump.options))
                        .build()
                )
        }
        val hintPageBuilder = SheetPage.Builder()
            .title("Hint")
        dump.hints.forEach { hintDump: HintDump ->
            hintPageBuilder
                .addLine(
                    RowLine.Builder()
                        .addCell(CellSeal.CellLong(hintDump.id))
                        .addCell(CellSeal.CellLong(hintDump.definitionId))
                        .addCell(CellSeal.CellString(hintDump.value))
                        .addCell(CellSeal.CellDate(hintDump.addDate))
                        .addCell(CellSeal.CellDate(hintDump.changeDate))
                        .build()
                )
        }
        val samplePageBuilder = SheetPage.Builder()
            .title("Sample")
        dump.samples.forEach { sampleDump: SampleDump ->
            samplePageBuilder
                .addLine(
                    RowLine.Builder()
                        .addCell(CellSeal.CellLong(sampleDump.id))
                        .addCell(CellSeal.CellLong(sampleDump.definitionId))
                        .addCell(CellSeal.CellString(sampleDump.value))
                        .addCell(CellSeal.CellString(sampleDump.source))
                        .addCell(CellSeal.CellDate(sampleDump.addDate))
                        .addCell(CellSeal.CellDate(sampleDump.changeDate))
                        .build()
                )
        }
        val writePageBuilder = SheetPage.Builder()
            .title("Write")
        dump.writes.forEach { writeQuizDump ->
            writePageBuilder
                .addLine(
                    RowLine.Builder()
                        .addCell(CellSeal.CellLong(writeQuizDump.id))
                        .addCell(CellSeal.CellLong(writeQuizDump.langId))
                        .addCell(CellSeal.CellLong(writeQuizDump.definitionId))
                        .addCell(CellSeal.CellLong(writeQuizDump.grade.toLong()))
                        .addCell(CellSeal.CellLong(writeQuizDump.score.toLong()))
                        .addCell(CellSeal.CellDate(writeQuizDump.addDate))
                        .addCell(CellSeal.CellDate(writeQuizDump.lastSelectDate))
                        .build()
                )
        }

        val builder = SheetDoc.Builder()
            .title(title)
            .addSheetPage(languagePageBuilder.build())
            .addSheetPage(wordPageBuilder.build())
            .addSheetPage(definitionPageBuilder.build())
            .addSheetPage(hintPageBuilder.build())
            .addSheetPage(samplePageBuilder.build())
            .addSheetPage(writePageBuilder.build())
            .build()
        withContext(Dispatchers.IO) {
            sheetsApi.spreadsheets().create(builder.get()).execute()
        }
    }

    suspend fun create() {

        val celldata1 = CellData()
        val ext1 = ExtendedValue()
        ext1.stringValue = "Zhopa"
        celldata1.userEnteredValue = ext1

        val celldata2 = CellData()
        val ext2 = ExtendedValue()
        ext2.stringValue = "Piska"
        celldata2.userEnteredValue = ext2


        val rowData = RowData()
        val listCellData = mutableListOf<CellData>()
        listCellData.add(celldata1)
        listCellData.add(celldata2)
        rowData.setValues(listCellData)

        val gridData = GridData()
        gridData.rowData = listOf(rowData)

        val sheet = Sheet()
        val sheetProperties = SheetProperties()
        sheetProperties.title = "TITLEZHOPA"
        sheet.properties = sheetProperties
        sheet.data = listOf(gridData)

        val spreadSheet = Spreadsheet()
        val sp = SpreadsheetProperties()
        sp.title = "Szhopa"
        spreadSheet.properties = sp
        spreadSheet.sheets = listOf(sheet)

        Log.d("###", "SheetsProvider / 68 / create: ")

        withContext(Dispatchers.IO) {
            val fff = sheetsApi.spreadsheets().create(spreadSheet).execute()
        }

    }

    suspend fun getDataFromSheet(fileId: String): Dump {
        val langDump = parseLangDump(fileId)
        val wordDump = parseWordDump(fileId)
        val definitionDump = parseDefinitionDump(fileId)
        val hintDump = parseHintDump(fileId)
        val sampleDump = parseSampleDump(fileId)
        val writeQuizDump = parseWriteDump(fileId)
        return Dump(
            languages = langDump,
            words = wordDump,
            definitions = definitionDump,
            hints = hintDump,
            samples = sampleDump,
            writes = writeQuizDump,
        )
    }

    private suspend fun parseLangDump(fileId: String): List<LanguageDump> {
        return sheetsApi.spreadsheets().values().getListOfLine(fileId, LanguageDump.RANGE)
            .map { line ->
                LanguageDump(
                    id = line[0].toLong(),
                    numericCode = line[1].toInt(),
                    code = line[2],
                    name = line[3],
                    addDate = Date(line[4].toLong()),
                    changeDate = line.getOrNull(5)?.let { Date(it.toLong()) }
                )
            }
    }

    private suspend fun parseWordDump(fileId: String): List<WordDump> {
        return sheetsApi.spreadsheets().values().getListOfLine(fileId, WordDump.RANGE)
            .map { line ->
                WordDump(
                    id = line[0].toLong(),
                    langId = line[1].toLong(),
                    word = line[2],
                    addDate = line.getOrNull(3)?.let { Date(it.toLong()) },
                    changeDate = line.getOrNull(4)?.let { Date(it.toLong()) }
                )
            }
    }

    private suspend fun parseDefinitionDump(fileId: String): List<DefinitionDump> {
        return sheetsApi.spreadsheets().values().getListOfLine(fileId, DefinitionDump.RANGE)
            .map { line ->
                DefinitionDump(
                    id = line[0].toLong(),
                    wordId = line[1].toLong(),
                    definition = line[2],
                    wordClass = line[3],
                    options = line[4].toLong()
                )
            }
    }

    private suspend fun parseHintDump(fileId: String): List<HintDump> {
        return sheetsApi.spreadsheets().values().getListOfLine(fileId, HintDump.RANGE)
            .map { line ->
                HintDump(
                    id = line[0].toLong(),
                    definitionId = line[1].toLong(),
                    value = line[2],
                    addDate = Date(line[3].toLong()),
                    changeDate = line.getOrNull(4)?.let { Date(it.toLong()) }
                )
            }
    }

    private suspend fun parseSampleDump(fileId: String): List<SampleDump> {
        return sheetsApi.spreadsheets().values().getListOfLine(fileId, SampleDump.RANGE)
            .map { line ->
                SampleDump(
                    id = line[0].toLong(),
                    definitionId = line[1].toLong(),
                    value = line[2],
                    source = line[4],
                    addDate = Date(line[4].toLong()),
                    changeDate = line.getOrNull(5)?.let { Date(it.toLong()) }
                )
            }
    }

    private suspend fun parseWriteDump(fileId: String): List<WriteQuizDump> {
        return sheetsApi.spreadsheets().values().getListOfLine(fileId, WriteQuizDump.RANGE)
            .map { line ->
                WriteQuizDump(
                    id = line[0].toLong(),
                    langId = line[1].toLong(),
                    definitionId = line[2].toLong(),
                    grade = line[3].toInt(),
                    score = line[4].toInt(),
                    addDate = Date(line[5].toLong()),
                    lastSelectDate = line.getOrNull(6)?.let { Date(it.toLong()) }
                )
            }
    }
}

suspend fun Sheets.Spreadsheets.Values.getListOfLine(
    fileId: String,
    range: String
): List<List<String>> {
    return withContext(Dispatchers.IO) {
        get(fileId, range)
            .execute()
            .values.toList()
            .getOrNull(2)
            ?.let { value ->
                (value as ArrayList<ArrayList<String>>)
            } ?: emptyList()
    }
//    return get(fileId, range)
//        .execute()
//        .values.toList()
//        .getOrNull(2)
//        ?.let { value ->
//            (value as ArrayList<ArrayList<String>>)
//        } ?: emptyList()
}