package cloud.kasaflow.ksefinvoicer.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Łapie broadcast z fizycznego lasera Sunmi (lub Newland w starszych V2s).
 *
 * Format intentu zależy od modelu:
 *   - Sunmi V2/V2s/V3 (najczęściej):
 *       action: com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED
 *       extra "data": String — zeskanowany kod
 *       extra "source_byte": ByteArray — raw bytes (przed dekodowaniem)
 *   - Sunmi z literówką (firmware bug): com.summi.scanner.* (dwa M)
 *   - Newland: nlscan.action.SCANNER_RESULT, extra "SCAN_BARCODE1"
 *
 * Trzymamy 3 warianty w manifest <intent-filter> żeby nie zgadywać.
 */
class SunmiScannerReceiver(
    private val onCode: (String) -> Unit
) : BroadcastReceiver() {

    companion object { private const val TAG = "ScannerRx" }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return

        // Sunmi (lub "Summi" literowy)
        val sunmiCode = intent.getStringExtra("data")?.trim()
        if (!sunmiCode.isNullOrEmpty()) {
            Log.i(TAG, "Sunmi scan: $sunmiCode (action=${intent.action})")
            onCode(sunmiCode)
            return
        }

        // Newland
        val newlandCode = intent.getStringExtra("SCAN_BARCODE1")?.trim()
        if (!newlandCode.isNullOrEmpty()) {
            Log.i(TAG, "Newland scan: $newlandCode")
            onCode(newlandCode)
            return
        }

        // Niektóre firmware używają "barcode_string" lub "scanResult"
        val alt = intent.getStringExtra("barcode_string")
            ?: intent.getStringExtra("scanResult")
        if (!alt.isNullOrEmpty()) {
            Log.i(TAG, "Alt scan: $alt")
            onCode(alt.trim())
            return
        }

        Log.w(TAG, "Empty scan broadcast: action=${intent.action}, extras=${intent.extras?.keySet()}")
    }
}
