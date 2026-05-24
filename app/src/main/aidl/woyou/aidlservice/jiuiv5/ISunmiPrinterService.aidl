// ISunmiPrinterService.aidl — Sunmi Printer Service public AIDL interface
// (skrócona wersja — pełen plik ma ~80 metod; trzymamy tylko te których używamy)
// Pełna AIDL: https://github.com/sunmi-OS/SunmiPrinterDemo/blob/master/app/src/main/aidl/woyou/aidlservice/jiuiv5/IWoyouService.aidl
package woyou.aidlservice.jiuiv5;

import android.graphics.Bitmap;
import woyou.aidlservice.jiuiv5.ICallback;

interface ISunmiPrinterService {
    /** Inicjalizuje drukarkę. */
    void printerInit(in ICallback callback);

    /** Drukuje tekst (z aktualnymi ustawieniami formatowania). */
    void printText(String text, in ICallback callback);

    /** Drukuje tekst z konkretnym fontem (V2s wymaga: "Default", "monospace"). */
    void printTextWithFont(String text, String typeface, float fontSize, in ICallback callback);

    /** Ustawia rozmiar fonta dla kolejnych printText. */
    void setFontSize(float fontSize, in ICallback callback);

    /** Wyrównanie: 0=lewo, 1=środek, 2=prawo. */
    void setAlignment(int alignment, in ICallback callback);

    /** Drukuje bitmapę (PNG/JPG po decode). Max szer: 384px (58mm) / 576px (80mm). */
    void printBitmap(in Bitmap bitmap, in ICallback callback);

    /** Drukuje QR code. levelHeight 1-16, errorLevel 0-3. */
    void printQRCode(String data, int modulesize, int errorlevel, in ICallback callback);

    /** Drukuje barcode (EAN13/Code128 etc). symbology: 0..8. */
    void printBarCode(String data, int symbology, int height, int width, int textposition, in ICallback callback);

    /** Pomija n linii (papier do przodu). */
    void lineWrap(int n, in ICallback callback);

    /** Odcina papier (jeśli drukarka wspiera — V2s/V3 TAK). */
    void cutPaper(in ICallback callback);

    /** Pobiera stan drukarki (1=ready, inne=error/papierout). */
    int updatePrinterState();
}
