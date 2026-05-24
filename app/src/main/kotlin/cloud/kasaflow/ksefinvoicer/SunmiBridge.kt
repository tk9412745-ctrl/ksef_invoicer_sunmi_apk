package cloud.kasaflow.ksefinvoicer

import android.app.Activity
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import cloud.kasaflow.ksefinvoicer.printer.SunmiPrinterManager
import org.json.JSONObject

/**
 * JS interface exposed jako `window.SunmiBridge` w naszej web-app.
 *
 * Metody @JavascriptInterface sa CALLABLE z JavaScriptu. Pamietaj:
 *   - Wszystkie metody musza byc thread-safe (wolane z JS thread, NIE main thread)
 *   - Zwracane wartosci tylko prymitywy + String (JSON serializowane)
 *   - JS wolanie: `window.SunmiBridge.scanBarcode()`, `window.SunmiBridge.printReceipt(text)`
 */
class SunmiBridge(
    private val activity: Activity,
    private val webView: WebView,
    private val printerManager: SunmiPrinterManager,
) {
    companion object { private const val TAG = "SunmiBridge" }

    /** Info o urzadzeniu — pokazujemy w bannerze "Sunmi POS aktywne · V2s (xxx)". */
    @JavascriptInterface
    fun getDeviceInfo(): String {
        val info = JSONObject().apply {
            put("model", Build.MODEL)            // np. "V2s" lub "V2-PRO"
            put("manufacturer", Build.MANUFACTURER) // "SUNMI"
            put("serial", try { Build.SERIAL } catch (e: Exception) { "unknown" })
            put("androidVersion", Build.VERSION.RELEASE)
            put("apkVersion", "0.1.0-alpha")
        }
        return info.toString()
    }

    /**
     * Aktywuje sprzetowy laser scanner.
     *
     * Sunmi V2s ma trigger na boku — naciskasz fizyczny przycisk -> scanner skanuje.
     * Tutaj wywoluje broadcastIntent ze action SCAN_NOW.
     * BroadcastReceiver odpowie i SunmiScannerReceiver wyemituje result do JS.
     *
     * Alternatywnie mozemy uzyc broadcast `com.sunmi.scanner.Scan_start` zeby
     * software-trigger (bez fizycznego nacisniecia).
     */
    @JavascriptInterface
    fun scanBarcode() {
        Log.i(TAG, "scanBarcode() called from JS")
        val intent = android.content.Intent("com.sunmi.scanner.Scan_start")
        activity.sendBroadcast(intent)
    }

    /**
     * Drukuje paragon na wbudowanej drukarce termalnej Sunmi V2s (58mm).
     * @param text Plain text z paragonem (juz sformatowany do 32 znakow szerokosci).
     */
    @JavascriptInterface
    fun printReceipt(text: String) {
        Log.i(TAG, "printReceipt() called from JS, length=${text.length}")
        printerManager.printPlainText(text)
    }

    /**
     * Drukuje PDF lub bitmape przekazana jako base64.
     * @param base64 Base64-encoded image (PNG/JPG) szerokosci 384px (58mm) lub 576px (80mm).
     */
    @JavascriptInterface
    fun printImage(base64: String) {
        Log.i(TAG, "printImage() called from JS, length=${base64.length}")
        printerManager.printBase64Image(base64)
    }

    /**
     * Forward scan result z SunmiScannerReceiver → JS callback.
     * Wolane Z MAIN THREAD (BroadcastReceiver lapie tam).
     */
    fun forwardScanToJs(code: String) {
        val js = "if (window.onSunmiBarcodeScanned) " +
            "window.onSunmiBarcodeScanned(${JSONObject.quote(code)})"
        webView.post {
            webView.evaluateJavascript(js, null)
        }
    }
}
