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
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var library: LinearLayout
    private var selectedInstaller: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
        handleIncoming(intent)
        openInstallerDetailsFromShortcut()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(32), dp(22), dp(28))
            setBackgroundColor(0xFF070B14.toInt())
        }

        root.addView(label("WINDOWBRIDGE", 27f, 0xFFEAF2FF.toInt(), true), matchWrap())
        root.addView(label("WINDOWS APP LIBRARY • v0.6.0", 12f, 0xFF79AFFF.toInt(), true).apply {
            setPadding(0, dp(8), 0, dp(24))
        }, matchWrap())

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(0xFF111C2D.toInt())
        }
        hero.addView(label("Manage Windows installers", 18f, 0xFFEAF2FF.toInt(), false), matchWrap())
        hero.addView(label(
            "Import a Windows .EXE or .MSI, choose a compatibility profile, and manage installers here. A standalone Windows engine is not bundled yet.",
            14f, 0xFFB5C6DF.toInt(), false
        ).apply { setPadding(0, dp(10), 0, dp(16)) }, matchWrap())
        hero.addView(Button(this).apply {
            text = "＋  IMPORT INSTALLER"
            setOnClickListener { openPicker() }
        }, matchWrap())
        hero.addView(Button(this).apply {
            text = "COMPATIBILITY PROFILE: ${currentProfileLabel()}"
            setOnClickListener { chooseCompatibilityProfile() }
        }, matchWrap())
        hero.addView(Button(this).apply {
            text = "RUNTIME STATUS / SETUP"
            setOnClickListener { showStandaloneRuntimeStatus() }
        }.apply { setPadding(0, dp(4), 0, dp(4)) }, matchWrap())
        root.addView(hero, matchWrap())

        status = label("Ready. No installer selected.", 14f, 0xFF8FC5FF.toInt(), false).apply {
            setPadding(0, dp(18), 0, dp(12))
        }
        root.addView(status, matchWrap())
        root.addView(label(runtimeSummary(), 13f, 0xFF9BE7C4.toInt(), false).apply {
            setPadding(0, 0, 0, dp(10))
        }, matchWrap())
        root.addView(label("MY INSTALLERS", 13f, 0xFF91A7C7.toInt(), true).apply {
            setPadding(0, dp(8), 0, dp(8))
        }, matchWrap())

        library = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(library, matchWrap())

        root.addView(label(
            "STANDALONE ENGINE: NOT INSTALLED\nThe Auto profile chooses a suggested setup, but WindowBridge cannot run Windows programs until a compatible engine is bundled. No installer is executed by this app yet.",
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

    private fun currentProfile(): String =
        getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_PROFILE, PROFILE_AUTO) ?: PROFILE_AUTO

    private fun currentProfileLabel(): String = when (currentProfile()) {
        PROFILE_LIGHT -> "LIGHTWEIGHT"
        PROFILE_GAMES -> "PC & GAMES"
        else -> "AUTO"
    }

    private fun chooseCompatibilityProfile() {
        val options = arrayOf(
            "Auto — choose settings based on the program",
            "Lightweight — older / simpler Windows apps",
            "PC & games — demanding apps and games"
        )
        val values = arrayOf(PROFILE_AUTO, PROFILE_LIGHT, PROFILE_GAMES)
        val selected = values.indexOf(currentProfile()).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Compatibility profile")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_PROFILE, values[which]).apply()
                status.text = "Profile selected: ${options[which].substringBefore(" —")}. This is a preference only; no Windows engine is bundled yet."
                render()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStandaloneRuntimeStatus() {
        val runtimeDir = File(filesDir, "windows-runtime")
        val marker = File(runtimeDir, "runtime-ready")
        val ready = marker.exists()
        AlertDialog.Builder(this)
            .setTitle(if (ready) "Windows engine detected" else "Standalone Windows engine not installed")
            .setMessage(
                if (ready) "A runtime marker exists in app storage. Actual program compatibility still depends on the runtime components and device."
                else "Your selected profile is saved, but this version does not include Wine, CPU translation, or graphics runtime components. Therefore it cannot yet install or run .EXE/.MSI files by itself. The profile selector is preparation only; it does not install an engine or use Winlator automatically."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun installOrRunWindowsApp(file: File) {
        val marker = File(File(filesDir, "windows-runtime"), "runtime-ready")
        if (!marker.exists()) {
            AlertDialog.Builder(this)
                .setTitle("Windows engine required")
                .setMessage(
                    "${file.name} is saved, but WindowBridge does not yet contain its own Windows compatibility engine. The ${currentProfileLabel()} profile is only a preference and cannot execute the installer. To meet your request to work without Winlator, Wine and the required CPU/graphics components must first be integrated into this project."
                )
                .setPositiveButton("Runtime status") { _, _ -> showStandaloneRuntimeStatus() }
                .setNegativeButton("Close", null)
                .show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Runtime integration incomplete")
            .setMessage("A runtime marker was found, but automatic installation and launching are not enabled until the runtime adapter is implemented and validated.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun openRuntimeGuide() {
        val url = Uri.parse("https://github.com/brunodev85/winlator/releases")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url))
        } catch (_: Exception) {
            Toast.makeText(this, "Open the official Winlator releases page in your browser.", Toast.LENGTH_LONG).show()
        }
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
            card.addView(label(String.format("%.2f MB • Saved privately", sizeMb), 12f, 0xFF91A7C7.toInt(), false).apply {
                setPadding(0, dp(6), 0, dp(10))
            }, matchWrap())

            card.addView(Button(this).apply {
                text = "ADD HOME-SCREEN SHORTCUT"
                setOnClickListener { createHomeShortcut(file) }
            }, matchWrap())

            card.addView(Button(this).apply {
                text = "INSTALL / RUN WINDOWS APP"
                setOnClickListener { installOrRunWindowsApp(file) }
            }, matchWrap())
            card.addView(Button(this).apply {
                text = "OPTIONAL: SEND TO WINLATOR"
                setOnClickListener { sendToWinlator(file) }
            }, matchWrap())
            card.addView(Button(this).apply {
                text = "SHARE WITH ANOTHER APP"
                setOnClickListener { shareInstaller(file) }
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
            library.addView(card, matchWrap().apply { bottomMargin = dp(10) })
        }
    }


    private fun createHomeShortcut(file: File) {
        if (android.os.Build.VERSION.SDK_INT < 26) {
            Toast.makeText(this, "Home-screen shortcuts require Android 8 or newer.", Toast.LENGTH_LONG).show()
            return
        }
        val manager = getSystemService(android.content.pm.ShortcutManager::class.java)
        if (manager == null || !manager.isRequestPinShortcutSupported) {
            Toast.makeText(this, "Your launcher does not allow pinned shortcuts.", Toast.LENGTH_LONG).show()
            return
        }
        val launch = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_SHORTCUT_FILE, file.name)
        }
        val shortcut = android.content.pm.ShortcutInfo.Builder(this, "installer_" + file.name.hashCode())
            .setShortLabel(file.name.take(20))
            .setLongLabel("Open ${file.name} in WindowBridge")
            .setIntent(launch)
            .setIcon(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.sym_def_app_icon))
            .build()
        try {
            manager.requestPinShortcut(shortcut, null)
            Toast.makeText(this, "Shortcut request sent to your launcher.", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Could not create shortcut on this launcher.", Toast.LENGTH_LONG).show()
        }
    }

    private fun openInstallerDetailsFromShortcut() {
        val name = intent?.getStringExtra(EXTRA_SHORTCUT_FILE) ?: return
        val file = File(File(filesDir, "installers"), name)
        if (file.exists()) {
            selectedInstaller = file
            status.text = "Shortcut opened: ${file.name}. Choose Install / Run Windows App below."
            showDetails(file)
        } else {
            status.text = "That installer is no longer in your library."
        }
    }

    private fun runtimeSummary(): String = if (File(File(filesDir, "windows-runtime"), "runtime-ready").exists()) {
        "STANDALONE ENGINE: Runtime marker detected; execution adapter still needs validation."
    } else {
        "STANDALONE ENGINE: Not installed. Profiles are saved, but Windows programs cannot run yet."
    }

    private fun openInstallerInRuntime(file: File) {
        val launch = packageManager.getLaunchIntentForPackage("com.winlator")
        if (launch == null) {
            AlertDialog.Builder(this)
                .setTitle("Winlator not detected")
                .setMessage("Install Winlator from its official releases page, open it once, then return here. In Winlator, create a container and use its file browser to locate/import the installer. Windows apps are not guaranteed to work on every Android device.")
                .setNegativeButton("Close", null)
                .setPositiveButton("Get Winlator") { _, _ -> openRuntimeGuide() }
                .show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Open ${file.name} in Winlator")
            .setMessage("WindowBridge cannot force Winlator to execute a private installer directly. You can open Winlator now, then use its file browser to locate or import the installer. If direct sharing is supported by your Winlator build, use the sharing button instead.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Open Winlator") { _, _ ->
                try { startActivity(launch) } catch (_: Exception) { shareInstaller(file) }
            }
            .setNeutralButton("Try direct handoff") { _, _ -> sendToWinlator(file) }
            .show()
    }

    private fun sendToWinlator(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
            val handoff = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newUri(contentResolver, file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.winlator")
            }
            startActivity(handoff)
            status.text = "Sent to Winlator. If it does not import the file, use Share with another app or open the installer from Winlator’s file browser."
        } catch (_: Exception) {
            status.text = "Winlator could not accept a direct installer handoff. Showing compatible apps instead."
            shareInstaller(file)
        }
    }

    private fun shareInstaller(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newUri(contentResolver, file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            selectedInstaller = file
            startActivity(Intent.createChooser(share, "Send installer to a compatible app"))
            status.text = "Choose an app that can accept this installer."
        } catch (e: Exception) {
            status.text = "Could not share installer: ${e.message ?: "unknown error"}"
            Toast.makeText(this, "Sharing failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDetails(file: File) {
        selectedInstaller = file
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setMessage("Size: ${file.length()} bytes\nSaved privately inside WindowBridge.\n\nUse “Share with compatible runtime” to send a copy to another app. Window execution is handled by an external runtime, not inside WindowBridge.")
            .setPositiveButton("Share") { _, _ -> shareInstaller(file) }
            .setNegativeButton("Close", null)
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
        private const val EXTRA_SHORTCUT_FILE = "com.windowbridge.EXTRA_SHORTCUT_FILE"
        private const val PREFS = "windowbridge_settings"
        private const val KEY_PROFILE = "compatibility_profile"
        private const val PROFILE_AUTO = "auto"
        private const val PROFILE_LIGHT = "lightweight"
        private const val PROFILE_GAMES = "games"
    }
}
