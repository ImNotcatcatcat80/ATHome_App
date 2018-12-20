package it.zerozero.athome_app;

import android.graphics.Color;
import android.util.Log;

public class LedStripCommands {

    public final int LEDSTRIP_LENGTH = 7;
    private int[] ledColorsAr = new int[LEDSTRIP_LENGTH];

    public LedStripCommands() {
        for (int i = 0; i < LEDSTRIP_LENGTH; i++) {
            if (i % 2 == 0) {
                ledColorsAr[i] = Color.YELLOW;
            }
            else {
                ledColorsAr[i] = Color.BLACK;
            }
        }
    }

    public int[] getLedColorsAr() {
        return ledColorsAr;
    }

    public void setLedColorsAr(int[] ledColorsAr) {
        this.ledColorsAr = ledColorsAr;
        for (int r = 0; r < ledColorsAr.length; r++) {
            Log.i("ledColorsAr", String.format("at [%d] == %s", r, String.valueOf(ledColorsAr[r])));
        }
    }
}
