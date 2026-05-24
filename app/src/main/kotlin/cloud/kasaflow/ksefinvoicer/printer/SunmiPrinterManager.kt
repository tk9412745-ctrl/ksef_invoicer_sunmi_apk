package cloud.kasaflow.ksefinvoicer.printer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Base64
import android.util.Log
import woyou.aidlservice.jiuiv5.ISunmiPrinterService

/**
 * Zarzadza polaczeniem z Sunmi Printer Service (AIDL).
 *
 * Sunmi exposuje system service `com.sunmi.printerservice` (V2s/V3) lub
 * `woyou.aidlservice.jiuiv5` (starsze) — service binduje sie przez AIDL.
 *
 * UWAGA: Te AIDL files (`ISunmiPrinterService.aidl`, `ICallback.aidl`) musza
 * byc w `app/src/main/aidl/woyou/aidlservice/jiuiv5/`. Patrz README.md.
 *
 * Stub mode: jezeli urzadzenie nie ma Sunmi (emulator/laptop), service nie
 * zbindowuje sie — logujemy WARN i printujemy do Log.i zeby przetestowac
 * format bez fizycznej drukarki.
 */
class SunmiPrinterManager(private val ctx: Context) {

    companion object {
        private const val TAG = "SunmiPrinter"
        private const val SUNMI_PACKAGE = "woyou.aidlservice.jiuiv5"
        private const val SUNMI_SERVICE = "woyou.aidlservice.jiuiv5.IWoyouService"
    }

    @Volatile
    private var printer: ISunmiPrinterService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            printer = ISunmiPrinterService.Stub.asInterface(service)
            Log.i(TAG, "Sunmi Printer Service connected")
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            printer = null
            Log.w(TAG, "Sunmi Printer Service disconnected")
        }
    }

    init {
        bind()
    }

    private fun bind() {
        try {
            val intent = Intent().apply {
                setPackage(SUNMI_PACKAGE)
                action = SUNMI_SERVICE
            }
            val ok = ctx.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (!ok) Log.w(TAG, "bindService returned false — czy to Sunmi? (emulator OK)")
        } catch (e: Exception) {
            Log.e(TAG, "bind exception (stub mode)", e)
        }
    }

    fun disconnect() {
        try { ctx.unbindService(connection) } catch (_: Exception) {}
    }

    /**
     * Drukuje paragon tekstowy.
     *
     * Sunmi Printer SDK ma funkcje `printText(text, callback)` ktora obsluguje:
     * - Auto-wrap przy 32 znakach (58mm) / 48 znakach (80mm)
     * - Polskie znaki UTF-8 (firmware V2s + supports it)
     * - `\n` jako line break
     * Po wszystkim — `cutPaper(callback)` przeciagnie do cut-line.
     */
    fun printPlainText(text: String) {
        val p = printer
        if (p == null) {
            Log.w(TAG, "STUB print (no Sunmi):\n$text")
            return
        }
        try {
            // Center align + normal font
            p.setAlignment(0, null)          // 0=lewo, 1=srodek, 2=prawo
            p.setFontSize(24f, null)         // default ~24, V2s native
            p.printText(text, null)
            p.lineWrap(3, null)              // 3 puste linie pod tekstem
            p.cutPaper(null)                 // jezeli drukarka wspiera (V2s — TAK)
        } catch (e: Exception) {
            Log.e(TAG, "printPlainText failed", e)
        }
    }

    /** Drukuje base64-encoded bitmap (PNG/JPG). Uzywane do QR/logo. */
    fun printBase64Image(base64: String) {
        val p = printer ?: run {
            Log.w(TAG, "STUB printImage (no Sunmi), bytes=${base64.length}")
            return
        }
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) {
                p.printBitmap(bmp, null)
                p.lineWrap(3, null)
                p.cutPaper(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "printBase64Image failed", e)
        }
    }
}
