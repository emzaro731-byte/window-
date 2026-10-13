package com.windowbridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var library: LinearLayout
    private lateinit var librarySummary: TextView
    private var selectedInstaller: File? = null
    private var searchField: EditText? = null

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
        root.addView(label("WINDOWS APP LIBRARY • v0.9.7", 12f, 0xFF79AFFF.toInt(), true).apply {
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
        hero.addView(Button(this).apply {
            text = "CHECK DEVICE FOR HEAVY APPS"
            setOnClickListener { showDeviceReadiness() }
        }, matchWrap())
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

        searchField = EditText(this).apply {
            hint = "Search saved installers…"
            setSingleLine(true)
            setTextColor(0xFFEAF2FF.toInt())
            setHintTextColor(0xFF8194B2.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(8))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    refreshLibrary()
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        root.addView(searchField, matchWrap())
        root.addView(Button(this).apply {
            text = "CLEAR SEARCH"
            setOnClickListener {
                searchField?.setText("")
                searchField?.clearFocus()
            }
        }, matchWrap())

        root.addView(Button(this).apply {
            text = "FILE TYPE: ${currentTypeLabel()}  ▾"
            setOnClickListener {
                val next = when (currentType()) {
                    TYPE_ALL -> TYPE_EXE
                    TYPE_EXE -> TYPE_MSI
                    else -> TYPE_ALL
                }
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_TYPE, next).apply()
                text = "FILE TYPE: ${currentTypeLabel()}  ▾"
                refreshLibrary()
            }
        }, matchWrap())

        librarySummary = label("0 installers • 0 B used", 12f, 0xFF91A7C7.toInt(), false).apply {
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(librarySummary, matchWrap())
        root.addView(Button(this).apply {
            text = "SCAN FOR DUPLICATE FILES"
            setOnClickListener { scanForDuplicateInstallers() }
        }, matchWrap())

        root.addView(Button(this).apply {
            text = "SORT: ${currentSortLabel()}  ↕"
            setOnClickListener {
                val next = when (currentSort()) {
                    SORT_NEWEST -> SORT_NAME
                    SORT_NAME -> SORT_LARGEST
                    else -> SORT_NEWEST
                }
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_SORT, next).apply()
                refreshLibrary()
                (this as Button).text = "SORT: ${currentSortLabel()}  ↕"
            }
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

    private fun showDeviceReadiness() {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val memory = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memory)
        val stat = android.os.StatFs(filesDir.absolutePath)
        val freeGb = stat.availableBytes / (1024.0 * 1024.0 * 1024.0)
        val totalRamGb = memory.totalMem / (1024.0 * 1024.0 * 1024.0)
        val availableRamGb = memory.availMem / (1024.0 * 1024.0 * 1024.0)
        val abis = android.os.Build.SUPPORTED_ABIS.joinToString()
        val is64Bit = android.os.Process.is64Bit()
        val assessment = when {
            !is64Bit -> "LIMITED: this Android process is 32-bit. Many modern Windows games need a 64-bit runtime."
            totalRamGb < 4.0 -> "LOW MEMORY: lightweight Windows apps are the realistic first target. Heavy games may fail or crash."
            freeGb < 8.0 -> "LOW STORAGE: free space before attempting a large runtime or game. Installers and game data can be very large."
            totalRamGb < 6.0 -> "BASIC / MIXED: some older PC apps may be possible with a compatible engine; demanding games remain uncertain."
            else -> "BETTER STARTING POINT: hardware alone does not guarantee compatibility. A working Wine/CPU/graphics runtime is still required."
        }
        val message = "Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n" +
            "Supported CPU ABIs: $abis\n" +
            "64-bit app process: $is64Bit\n" +
            "Total RAM: %.1f GB\nAvailable RAM now: %.1f GB\nFree internal storage: %.1f GB\n\n%s\n\n".format(
                totalRamGb, availableRamGb, freeGb, assessment
            ) +
            "Important: this is a hardware check only. WindowBridge still does not contain the Windows runtime needed to install or run .EXE/.MSI files. Heavy games can require substantial RAM, storage, compatible graphics, and x86-to-ARM translation."
        AlertDialog.Builder(this)
            .setTitle("Device readiness for PC apps")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showStandaloneRuntimeStatus() {
        AlertDialog.Builder(this)
            .setTitle("Standalone Windows engine not installed")
            .setMessage(
                "WindowBridge does not yet include a verified Windows runtime. A marker file or selected profile is not enough to prove that Wine, CPU translation, graphics/audio components, and the installer launcher are working. The engine must be integrated and tested before .EXE/.MSI files can run here."
            )
            .setPositiveButton("Runtime integration plan") { _, _ -> openRuntimeGuide() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun installOrRunWindowsApp(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Windows engine required")
            .setMessage(
                "${file.name} is saved, but WindowBridge does not yet contain a verified Windows compatibility engine. The ${currentProfileLabel()} profile is only a preference and cannot execute the installer. Wine, CPU translation, graphics/audio components, and a working launcher must be integrated and tested first."
            )
            .setPositiveButton("Runtime status") { _, _ -> showStandaloneRuntimeStatus() }
            .setNegativeButton("Close", null)
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
        val estimatedSize = try {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use {
                if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else -1L
            } ?: -1L
        } catch (_: Exception) { -1L }
        val availableBytes = android.os.StatFs(filesDir.absolutePath).availableBytes
        if (estimatedSize >= 0 && estimatedSize > availableBytes) {
            status.text = "Import stopped: not enough free storage."
            Toast.makeText(this, "This installer is larger than the available internal storage.", Toast.LENGTH_LONG).show()
            return
        }
        var target = File(folder, safeName)
        if (target.exists()) target = File(folder, "${target.nameWithoutExtension}-${System.currentTimeMillis()}.$extension")

        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) throw IOException("Android could not open this file")
                val signature = ByteArray(8)
                var read = 0
                while (read < signature.size) {
                    val count = input.read(signature, read, signature.size - read)
                    if (count <= 0) break
                    read += count
                }
                val validSignature = when (extension) {
                    "exe" -> read >= 2 && signature[0] == 0x4D.toByte() && signature[1] == 0x5A.toByte()
                    "msi" -> read >= 8 && signature.contentEquals(
                        byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(),
                            0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte())
                    )
                    else -> false
                }
                if (!validSignature) {
                    throw IOException("The file extension is .$extension, but its header does not look like a Windows installer. Choose the original, complete file.")
                }
                target.outputStream().use { output ->
                    output.write(signature, 0, read)
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        val remaining = android.os.StatFs(filesDir.absolutePath).availableBytes
                        if (count.toLong() > remaining) {
                            throw IOException("Storage became full while importing. Free space and try again.")
                        }
                        output.write(buffer, 0, count)
                    }
                }
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

    private fun currentType(): String =
        getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_TYPE, TYPE_ALL) ?: TYPE_ALL

    private fun currentTypeLabel(): String = when (currentType()) {
        TYPE_EXE -> "EXE ONLY"
        TYPE_MSI -> "MSI ONLY"
        else -> "ALL INSTALLERS"
    }

    private fun currentSort(): String =
        getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_SORT, SORT_NEWEST) ?: SORT_NEWEST

    private fun currentSortLabel(): String = when (currentSort()) {
        SORT_NAME -> "NAME A–Z"
        SORT_LARGEST -> "LARGEST FIRST"
        else -> "NEWEST FIRST"
    }

    private fun refreshLibrary() {
        if (!::library.isInitialized) return
        library.removeAllViews()
        val folder = File(filesDir, "installers")
        val query = searchField?.text?.toString()?.trim().orEmpty()
        val unsortedFiles = folder.listFiles()?.filter { it.isFile } ?: emptyList()
        val allFiles = when (currentSort()) {
            SORT_NAME -> unsortedFiles.sortedBy { it.name.lowercase() }
            SORT_LARGEST -> unsortedFiles.sortedByDescending { it.length() }
            else -> unsortedFiles.sortedByDescending { it.lastModified() }
        }
        val totalBytes = allFiles.sumOf { it.length() }
        val used = android.text.format.Formatter.formatShortFileSize(this, totalBytes)
        val typedFiles = when (currentType()) {
            TYPE_EXE -> allFiles.filter { it.extension.equals("exe", ignoreCase = true) }
            TYPE_MSI -> allFiles.filter { it.extension.equals("msi", ignoreCase = true) }
            else -> allFiles
        }
        val files = if (query.isBlank()) typedFiles else typedFiles.filter { it.name.contains(query, ignoreCase = true) }
        librarySummary.text = "Showing ${files.size} of ${allFiles.size} installers • ${used} total storage"
        if (files.isEmpty()) {
            val emptyMessage = when {
                allFiles.isEmpty() -> "Your library is empty. Import an installer to get started."
                typedFiles.isEmpty() -> "No ${currentTypeLabel().lowercase()} found. Change the file-type filter to see other installers."
                else -> "No saved installers match “$query”."
            }
            library.addView(label(emptyMessage, 14f, 0xFF91A7C7.toInt(), false).apply {
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
            card.addView(Button(this).apply {
                text = "CALCULATE SHA-256 FINGERPRINT"
                setOnClickListener { calculateSha256(file) }
            }, matchWrap())
            card.addView(Button(this).apply {
                text = "COMPARE WITH PUBLISHER HASH"
                setOnClickListener { compareTrustedHash(file) }
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

    private fun runtimeSummary(): String =
        "STANDALONE ENGINE: Not installed. Profiles are saved, but Windows programs cannot run yet."

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

    private fun compareTrustedHash(file: File) {
        val input = EditText(this).apply {
            hint = "Paste trusted SHA-256 (64 hex characters)"
            setSingleLine(true)
            setTextColor(0xFFEAF2FF.toInt())
            setHintTextColor(0xFF8194B2.toInt())
        }
        AlertDialog.Builder(this)
            .setTitle("Compare publisher fingerprint")
            .setMessage("Get the expected SHA-256 from the software publisher's official website. WindowBridge will compare it with this saved file.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Compare", null)
            .show().also { dialog ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val expected = input.text.toString().trim().lowercase()
                    if (!expected.matches(Regex("[a-f0-9]{64}"))) {
                        input.error = "Enter exactly 64 hexadecimal characters"
                        return@setOnClickListener
                    }
                    dialog.dismiss()
                    status.text = "Comparing publisher fingerprint for ${file.name}…"
                    Thread {
                        val actual = try {
                            val digest = MessageDigest.getInstance("SHA-256")
                            file.inputStream().buffered().use { stream ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    val count = stream.read(buffer)
                                    if (count < 0) break
                                    digest.update(buffer, 0, count)
                                }
                            }
                            digest.digest().joinToString("") { "%02x".format(it) }
                        } catch (e: Exception) {
                            "ERROR: ${e.message ?: "unable to read file"}"
                        }
                        runOnUiThread {
                            val matches = actual == expected
                            status.text = when {
                                actual.startsWith("ERROR:") -> "Could not compare publisher fingerprint."
                                matches -> "SHA-256 MATCH: ${file.name}"
                                else -> "SHA-256 MISMATCH: ${file.name}"
                            }
                            AlertDialog.Builder(this)
                                .setTitle(when {
                                    actual.startsWith("ERROR:") -> "Comparison failed"
                                    matches -> "Fingerprint matches"
                                    else -> "Fingerprint does not match"
                                })
                                .setMessage(when {
                                    actual.startsWith("ERROR:") -> actual.removePrefix("ERROR: ")
                                    matches -> "The file matches the expected SHA-256 value. This confirms file identity only; it does not prove the software is harmless."
                                    else -> "Do not run this file if you expected a match. Check that the expected hash came from the publisher's official source and that you downloaded the correct file.\n\nActual SHA-256:\n$actual"
                                })
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }.start()
                }
            }
    }

    private fun scanForDuplicateInstallers() {
        val folder = File(filesDir, "installers")
        val files = folder.listFiles()?.filter { it.isFile } ?: emptyList()
        if (files.size < 2) {
            AlertDialog.Builder(this)
                .setTitle("Duplicate scan")
                .setMessage("Add at least two installers before scanning for duplicate content.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        status.text = "Scanning ${files.size} installers for duplicate content…"
        Thread {
            val groups = linkedMapOf<String, MutableList<File>>()
            var failureCount = 0
            files.forEach { file ->
                try {
                    val digest = MessageDigest.getInstance("SHA-256")
                    file.inputStream().buffered().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            digest.update(buffer, 0, count)
                        }
                    }
                    val hash = digest.digest().joinToString("") { "%02x".format(it) }
                    groups.getOrPut(hash) { mutableListOf() }.add(file)
                } catch (_: Exception) {
                    failureCount++
                }
            }
            val duplicates = groups.values.filter { it.size > 1 }
            runOnUiThread {
                status.text = "Duplicate scan complete: ${duplicates.size} duplicate group(s)."
                val message = if (duplicates.isEmpty()) {
                    "No identical files were found among ${files.size - failureCount} successfully checked installer(s)."
                } else {
                    duplicates.joinToString("\n\n") { group ->
                        val size = android.text.format.Formatter.formatShortFileSize(this, group.first().length())
                        "Identical content • $size each\n" + group.joinToString("\n") { "• ${it.name}" }
                    } + "\n\nNo files were deleted."
                }
                val finalMessage = if (failureCount > 0) {
                    "$message\n\nCould not check $failureCount file(s)."
                } else message
                AlertDialog.Builder(this)
                    .setTitle("Duplicate scan results")
                    .setMessage(finalMessage)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }.start()
    }

    private fun calculateSha256(file: File) {
        if (!file.isFile) {
            status.text = "Cannot verify: installer file is missing."
            return
        }
        status.text = "Calculating SHA-256 for ${file.name}…"
        Thread {
            val hash = try {
                val digest = MessageDigest.getInstance("SHA-256")
                file.inputStream().buffered().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                "ERROR: ${e.message ?: "could not read file"}"
            }
            runOnUiThread {
                if (hash.startsWith("ERROR:")) {
                    status.text = "SHA-256 check failed."
                    AlertDialog.Builder(this)
                        .setTitle("Could not calculate fingerprint")
                        .setMessage(hash.removePrefix("ERROR: "))
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    status.text = "SHA-256 calculated for ${file.name}"
                    AlertDialog.Builder(this)
                        .setTitle("SHA-256 fingerprint")
                        .setMessage("${file.name}\n\n$hash\n\nCompare this value with a fingerprint from a trusted publisher. A matching hash verifies file identity, not whether the file is safe.")
                        .setPositiveButton("Copy") { _, _ ->
                            val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("SHA-256", hash))
                            Toast.makeText(this, "Fingerprint copied", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Close", null)
                        .show()
                }
            }
        }.start()
    }

    private fun showDetails(file: File) {
        selectedInstaller = file
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setMessage("Size: ${android.text.format.Formatter.formatShortFileSize(this, file.length())}\nModified: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(file.lastModified()))}\nSaved privately inside WindowBridge.\n\nThe file header was checked during import, but that does not prove the installer is safe or compatible. WindowBridge cannot execute it until a real Windows runtime is integrated.")
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
        private const val KEY_SORT = "installer_sort"
        private const val KEY_TYPE = "installer_type_filter"
        private const val TYPE_ALL = "all"
        private const val TYPE_EXE = "exe"
        private const val TYPE_MSI = "msi"
        private const val SORT_NEWEST = "newest"
        private const val SORT_NAME = "name"
        private const val SORT_LARGEST = "largest"
        private const val PROFILE_AUTO = "auto"
        private const val PROFILE_LIGHT = "lightweight"
        private const val PROFILE_GAMES = "games"
    }
}
