package com.lifelong.supplierquality

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmap =
                    result.data?.extras?.get("data") as? Bitmap

                bitmap?.let {
                    val output = ByteArrayOutputStream()
                    it.compress(Bitmap.CompressFormat.JPEG, 85, output)

                    val base64 = android.util.Base64.encodeToString(
                        output.toByteArray(),
                        android.util.Base64.NO_WRAP
                    )

                    webView.evaluateJavascript(
                        "addPhoto('data:image/jpeg;base64,$base64')",
                        null
                    )
                }
            }
        }

    private val galleryLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            uri?.let {
                webView.evaluateJavascript(
                    "addPhoto('${it}')",
                    null
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        webView.webViewClient = WebViewClient()

        webView.addJavascriptInterface(
            AppInterface(),
            "Android"
        )

        setContentView(webView)

        webView.loadUrl(
            "file:///android_asset/index.html"
        )
    }

    inner class AppInterface {

        @JavascriptInterface
        fun takePhoto() {
            val intent = Intent(
                MediaStore.ACTION_IMAGE_CAPTURE
            )

            cameraLauncher.launch(intent)
        }

        @JavascriptInterface
        fun openGallery() {
            galleryLauncher.launch("image/*")
        }

        @JavascriptInterface
        fun scanBarcode() {

            val integrator =
                IntentIntegrator(this@MainActivity)

            integrator.setDesiredBarcodeFormats(
                IntentIntegrator.ALL_CODE_TYPES
            )

            integrator.setPrompt(
                "Scan EAN / QR / Barcode"
            )

            integrator.setBeepEnabled(true)
            integrator.setOrientationLocked(false)

            integrator.initiateScan()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        val result =
            IntentIntegrator.parseActivityResult(
                requestCode,
                resultCode,
                data
            )

        if (result != null) {

            result.contents?.let {

                webView.evaluateJavascript(
                    "setBarcode('${it}')",
                    null
                )
            }
        }
    }
}
