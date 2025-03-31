package com.fijicoffee.servicereport

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Image
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private val selectedImages = mutableListOf<Uri>()
    private lateinit var imageAdapter: ImagePreviewAdapter
    private val calendar = Calendar.getInstance()

    private val getContent = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.size + selectedImages.size <= 10) {
            selectedImages.addAll(uris)
            imageAdapter.notifyDataSetChanged()
        } else {
            Toast.makeText(this, "Maximum 10 images allowed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupDropdowns()
        setupDateTimePicker()
        setupImageUpload()
        setupSubmitButton()
    }

    private fun setupDropdowns() {
        // Service Type Dropdown
        val serviceTypes = arrayOf("Servicing", "Breakdown")
        setupDropdown(findViewById(R.id.serviceTypeDropdown), serviceTypes)

        // Technician Name Dropdown
        val technicians = arrayOf("Sergio Ruiz", "Neco", "Avinesh", "Hari")
        setupDropdown(findViewById(R.id.technicianNameDropdown), technicians)

        // Machine Type Dropdown
        val machineTypes = arrayOf("Frankee A300", "Frankee A400", "S700 Coffee Machine", "Dalla Corte 'Studio'")
        setupDropdown(findViewById(R.id.machineTypeDropdown), machineTypes)

        // Fault Type Dropdown
        val faultTypes = arrayOf("User", "Machine")
        setupDropdown(findViewById(R.id.faultTypeDropdown), faultTypes)
    }

    private fun setupDropdown(autoCompleteTextView: AutoCompleteTextView, items: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        autoCompleteTextView.setAdapter(adapter)
    }

    private fun setupDateTimePicker() {
        val dateTimeInput = findViewById<TextInputEditText>(R.id.serviceDateInput)
        dateTimeInput.setOnClickListener {
            showDateTimePicker { selectedDateTime ->
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                dateTimeInput.setText(formatter.format(selectedDateTime.time))
            }
        }
    }

    private fun showDateTimePicker(callback: (Calendar) -> Unit) {
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)

            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                callback(calendar)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setupImageUpload() {
        val recyclerView = findViewById<RecyclerView>(R.id.imagePreviewRecyclerView)
        imageAdapter = ImagePreviewAdapter(selectedImages) { position ->
            selectedImages.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
        }
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = imageAdapter

        findViewById<MaterialButton>(R.id.uploadImagesButton).setOnClickListener {
            if (checkAndRequestPermissions()) {
                getContent.launch("image/*")
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val notGrantedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGrantedPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGrantedPermissions.toTypedArray(), 1)
            return false
        }
        return true
    }

    private fun setupSubmitButton() {
        findViewById<MaterialButton>(R.id.submitReportButton).setOnClickListener {
            if (validateForm()) {
                generatePDF()
            }
        }
    }

    private fun validateForm(): Boolean {
        // Add validation logic here
        return true
    }

    private fun generatePDF() {
        try {
            val pdfFile = File(getExternalFilesDir(null), "service_report.pdf")
            val writer = PdfWriter(FileOutputStream(pdfFile))
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            // Add content to PDF
            document.add(Paragraph("Fiji Coffee - Machine Service Report"))
            // Add more content...

            document.close()

            // Share the PDF
            val uri = Uri.fromFile(pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "application/pdf"
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(shareIntent, "Share Service Report"))

        } catch (e: Exception) {
            Toast.makeText(this, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
