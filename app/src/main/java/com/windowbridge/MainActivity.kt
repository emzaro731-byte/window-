package com.windowbridge

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var fileInfo: TextView

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
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 48, 28, 28)
            setBackgroundColor(0xFF070B14.toInt())
        }
        val title = TextView(this).apply {
            text = "WINDOWBRIDGE"
            textSize = 27f
            setTextColor(0xFFEAF2FF.toInt())
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Windows app manager for Android"
            textSize = 15f
            setTextColor(0xFF91A7C7.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 28)
        }
        status = TextView(this).apply {
            text = "Ready to import a Windows installer"
            textSize = 16f
            setTextColor(0xFFB8D8FF.toInt())
            setPadding(0, 12, 0, 12)
        }
        fileInfo = TextView(this).apply {
            text = "No installer selected"
            textSize = 14f
            setTextColor(0xFFD3DCEC.toInt())
            setPadding(0, 8, 0, 20)
        }
        val choose = Button(this).apply {
            text = "Choose Windows installer"
            setOnClickListener {
                val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                startActivityForResult(picker, REQUEST_PICK_FILE)
            }
        }
        val note = TextView(this).apply {
            text = "This starter can receive or select files. Running Windows software requires a compatible runtime, which is not bundled yet."
            textSize = 13f
            setTextColor(0xFF91A7C7.toInt())
            setPadding(0, 28, 0, 0)
        }
        root.addView(title, matchWrap())
        root.addView(subtitle, matchWrap())
        root.addView(status, matchWrap())
        root.addView(fileInfo, matchWrap())
        root.addView(choose, matchWrap())
        root.addView(note, matchWrap())
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
    }

    @Deprecated("Kept compatible with the simple document-picker flow")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_FILE && resultCode == RESULT_OK) {
            data?.data?.let { showFile(it) }
        }
    }

    private fun handleIncoming(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { showFile(it) }
        }
    }

    private fun showFile(uri: Uri) {
        val name = uri.lastPathSegment ?: uri.toString()
        fileInfo.text = "Selected file:\n$name"
        status.text = "Installer received — runtime integration required"
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    companion object {
        private const val REQUEST_PICK_FILE = 42
    }
}
