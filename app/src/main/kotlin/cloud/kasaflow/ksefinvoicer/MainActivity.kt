package cloud.kasaflow.ksefinvoicer

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import cloud.kasaflow.ksefinvoicer.printer.SunmiPrinterManager
import cloud.kasaflow.ksefinvoicer.scanner.SunmiScannerReceiver

/**
 * Główny ekran apki — WebView ładujący naszą web-app `/m/faktury/quick`.
 *
 * Architektura:
 *   1. WebView ładuje https://invoicer.kasaflow.cloud/m/faktury/quick?app=sunmi-v2s
 *   2. SunmiBridge jest wstrzyknięty jako `window.SunmiBridge` (JavascriptInterface)
 *   3. Web-app detect SunmiBridge → uzywa native scanner/printer zamiast kamery/RawBT
 *   4. SunmiScannerReceiver lapie broadcast od fizycznego trigger lasera → forward do JS
 *
 * Trash-talk od V2s: Android 7.1, stary WebView (cz. 60), brak BarcodeDetector API.
 * Dlatego POTRZEBUJEMY natywnego bridge'a — w czystym Chrome na V2s by sie nie udalo.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var sunmiBridge: SunmiBridge
    private lateinit var scannerReceiver: SunmiScannerReceiver
    private lateinit var printerManager: SunmiPrinterManager

    companion object {
        private const val TAG = "KsefSunmi"
        // BASE_URL parametr — zmieniaj w buildConfig dla dev (np. http://10.0.2.2:8000)
        // 10.0.2.2 to alias dla 127.0.0.1 hosta z perspektywy emulatora Android
        const val WEB_URL = "https://invoicer.kasaflow.cloud/m/faktury/quick?app=sunmi-v2s"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true       // localStorage / sessionStorage
            databaseEnabled = true
            allowFileAccess = false
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            // User-Agent: dopisuj suffix zeby backend wiedzial ze to nasz APK
            userAgentString = "$userAgentString ksef_invoicer_sunmi/0.1.0"
        }
        // Zostan w obrebie WebView dla linkow (zamiast otwierac browser)
        webView.webViewClient = WebViewClient()

        // === Bridge native → JS ===
        printerManager = SunmiPrinterManager(this)
        sunmiBridge = SunmiBridge(this, webView, printerManager)
        webView.addJavascriptInterface(sunmiBridge, "SunmiBridge")

        // === Scanner BroadcastReceiver ===
        scannerReceiver = SunmiScannerReceiver { code ->
            Log.i(TAG, "Scanner laser fired: $code")
            // Forward do JS przez window.onSunmiBarcodeScanned(code)
            sunmiBridge.forwardScanToJs(code)
        }
        val filter = IntentFilter().apply {
            addAction("com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED")
            addAction("com.summi.scanner.ACTION_DATA_CODE_RECEIVED")  // legacy typo
            addAction("nlscan.action.SCANNER_RESULT")                  // Newland scanner
        }
        ContextCompat.registerReceiver(
            this, scannerReceiver, filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Start ładowania
        webView.loadUrl(WEB_URL)
        Log.i(TAG, "Loading $WEB_URL")
    }

    override fun onDestroy() {
        try { unregisterReceiver(scannerReceiver) } catch (e: Exception) { /* ignore */ }
        printerManager.disconnect()
        super.onDestroy()
    }

    /** Back button → cofnij w historii WebView zamiast zamykac apke. */
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
