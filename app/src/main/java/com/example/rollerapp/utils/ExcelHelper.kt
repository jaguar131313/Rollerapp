package com.example.rollerapp.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.rollerapp.data.Inspection
import com.example.rollerapp.data.Replacement
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelHelper {

    fun exportInspectionsToExcel(context: Context, inspections: List<Inspection>, filename: String) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Осмотры")

        val header = arrayOf("Опора", "Ролик", "Повреждение", "Запасной", "Ближайший ролик", "Дата осмотра")
        val headerRow = sheet.createRow(0)
        header.forEachIndexed { index, value ->
            headerRow.createCell(index).setCellValue(value)
        }

        inspections.forEachIndexed { index, insp ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(insp.opora.toDouble())
            row.createCell(1).setCellValue(insp.roller)
            row.createCell(2).setCellValue(insp.damage)
            row.createCell(3).setCellValue(if (insp.spare) "Да" else "Нет")
            row.createCell(4).setCellValue(insp.nearestRoller ?: "")
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(insp.timestamp))
            row.createCell(5).setCellValue(date)
        }

        saveWorkbook(context, workbook, filename)
    }

    fun exportReplacementsToExcel(context: Context, replacements: List<Replacement>, filename: String) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Замены")

        val header = arrayOf("Дата замены", "Опора", "Заменённый ролик", "Причина")
        val headerRow = sheet.createRow(0)
        header.forEachIndexed { index, value ->
            headerRow.createCell(index).setCellValue(value)
        }

        replacements.forEachIndexed { index, repl ->
            val row = sheet.createRow(index + 1)
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(repl.timestamp))
            row.createCell(0).setCellValue(date)
            row.createCell(1).setCellValue(repl.opora.toDouble())
            row.createCell(2).setCellValue(repl.roller)
            row.createCell(3).setCellValue(repl.reason)
        }

        saveWorkbook(context, workbook, filename)
    }

    private fun saveWorkbook(context: Context, workbook: XSSFWorkbook, filename: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { workbook.write(it) }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)
            FileOutputStream(file).use { workbook.write(it) }
        }
        workbook.close()
    }
}
