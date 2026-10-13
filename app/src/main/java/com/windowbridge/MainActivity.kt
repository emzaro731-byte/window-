package com.windowbridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.IOException

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var library: LinearLayout
    private var selectedInstaller: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
        handleIncoming(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(32), dp(22), dp(28))
            setBackgroundColor(0xFF070B14.toInt())
        }

        root.addView(label("WINDOWBRIDGE", 27f, 0xFFEAF2FF.toInt(), true), matchWrap())
        root.addView(label("YOUR WINDOWS APP LIBRARY", 12f, 0xFF79AFFF.toInt(), true).apply {
            setPadding(0, dp(8), 0, dp(24))
        }, matchWrap())

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(0xFF111C2D.toInt())
        }
        hero.addView(label("Bring an installer into your library", 18f, 0xFFEAF2FF.toInt(), false), matchWrap())
        hero.addView(label("Select an .EXE or .MSI file stored on your phone. WindowBridge will keep a private copy so it remains in the library.", 14f, 0xFFB5C6DF.toInt(), false).apply {
            setPadding(0, dp(10), 0, dp(16))
        }, matchWrap())
        hero.addView(Button(this).apply {
            text = "＋  IMPORT INSTALLER"
            setOnClickListener { openPicker() }
        }, matchWrap())
        root.addView(hero, matchWrap())

        status = label("Ready. No installer selected.", 14f, 0xFF8FC5FF.toInt(), false).apply {
            setPadding(0, dp(18), 0, dp(12))
        }
        root.addView(status, matchWrap())
        root.addView(label("MY INSTALLERS", 13f, 0xFF91A7C7.toInt(), true).apply {
            setPadding(0, dp(8), 0, dp(8))
        }, matchWrap())

        library = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(library, matchWrap())

        root.addView(label(
            "Runtime status: NOT INSTALLED\nThis version imports and stores installers only. Windows programs cannot run until a compatible Wine/CPU-translation runtime is integrated. Importing a file does not install or execute it.",
            12f, 0xFFFFD28A.toInt(), false
        ).apply { setPadding(0, dp(22), 0, 0) }, matchWrap())

        setContentView(ScrollView(this).apply { addView(root) })
        refreshLibrary()
    }

    private fun openPicker() {
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(picker, REQUEST_PICK_FILE)
    }

    @Deprecated("Document picker callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_FILE && resultCode == RESULT_OK) {
            data?.data?.let { importInstaller(it) }
        }
    }

    private fun handleIncoming(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW || intent?.action == Intent.ACTION_SEND) {
            val uri = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            if (uri != null) importInstaller(uri)
        }
    }

    private fun importInstaller(uri: Uri) {
        val originalName = queryDisplayName(uri) ?: uri.lastPathSegment ?: "windows-installer"
        val safeName = originalName.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
        val extension = safeName.substringAfterLast('.', "").lowercase()
        if (extension !in setOf("exe", "msi")) {
            status.text = "Unsupported file type: $safeName"
            Toast.makeText(this, "Choose a Windows .exe or .msi installer", Toast.LENGTH_LONG).show()
            return
        }

        val folder = File(filesDir, "installers").apply { mkdirs() }
        var target = File(folder, safeName)
        if (target.exists()) target = File(folder, "${target.nameWithoutExtension}-${System.currentTimeMillis()}.$extension")

        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) throw IOException("Android could not open this file")
                target.outputStream().use { output -> input.copyTo(output) }
            }
            selectedInstaller = target
            status.text = "Imported successfully: ${target.name}"
            refreshLibrary()
            Toast.makeText(this, "Installer saved to WindowBridge", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            status.text = "Import failed: ${e.message ?: "could not read file"}"
            target.delete()
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (_: Exception) { null }
    }

    private fun refreshLibrary() {
        if (!::library.isInitialized) return
        library.removeAllViews()
        val folder = File(filesDir, "installers")
        val files = folder.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
        if (files.isEmpty()) {
            library.addView(label("Your library is empty. Import an installer to get started.", 14f, 0xFF91A7C7.toInt(), false).apply {
                setPadding(0, dp(12), 0, dp(12))
            }, matchWrap())
            return
        }
        files.forEach { file ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                setBackgroundColor(0xFF0D1523.toInt())
            }
            card.addView(label(file.name, 15f, 0xFFEAF2FF.toInt(), false), matchWrap())
            val sizeMb = file.length().toDouble() / (1024.0 * 1024.0)
            card.addView(label(String.format("%.2f MB • Saved on this device", sizeMb), 12f, 0xFF91A7C7.toInt(), false).apply {
                setPadding(0, dp(6), 0, dp(10))
            }, matchWrap())
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(Button(this@MainActivity).apply {
                text = "Details"
                setOnClickListener { showDetails(file) }
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
            actions.addView(Button(this@MainActivity).apply {
                text = "Delete"
                setOnClickListener { confirmDelete(file) }
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
            card.addView(actions, matchWrap())
            val params = matchWrap().apply { bottomMargin = dp(10) }
            library.addView(card, params)
        }
    }

    private fun showDetails(file: File) {
        selectedInstaller = file
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setMessage("Size: ${file.length()} bytes\nSaved privately inside WindowBridge.\n\nRunning this installer is not available yet because the Windows compatibility runtime has not been integrated.")
            .setPositiveButton("OK", null)
            .setNeutralButton("Remove") { _, _ -> confirmDelete(file) }
            .show()
    }

    private fun confirmDelete(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete installer?")
            .setMessage("Remove ${file.name} from the WindowBridge library?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                if (file.delete()) {
                    if (selectedInstaller == file) selectedInstaller = null
                    status.text = "Installer removed."
                    refreshLibrary()
                } else {
                    Toast.makeText(this, "Could not delete file", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun label(textValue: String, size: Float, color: Int, centered: Boolean): TextView =
        TextView(this).apply {
            text = textValue
            textSize = size
            setTextColor(color)
            if (centered) gravity = Gravity.CENTER
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    companion object {
        private const val REQUEST_PICK_FILE = 42
    }
}
