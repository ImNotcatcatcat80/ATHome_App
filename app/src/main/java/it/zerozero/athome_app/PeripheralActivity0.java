package it.zerozero.athome_app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.IOException;
import java.util.List;

import android.util.Log;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PeripheralActivity0 extends Activity {
    private static final String TAG = PeripheralActivity0.class.getSimpleName();

    // LED configuration.
    private static final int NUM_LEDS = 7;
    private static final int LED_BRIGHTNESS = 14; // 0 ... 31
    private static final Apa102.Mode LED_MODE = Apa102.Mode.BGR;
    private static final String SPI_BUS = "BUS NAME";
    private Apa102 mLedstrip;
    private int[] mLedColors = new int[RainbowHat.LEDSTRIP_LENGTH];
    private EditText editText0;
    private Button buttonDisplay;
    private TextView textViewBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peripheral0);
        editText0 = findViewById(R.id.editText0);
        editText0.setText("<click to check I2C bus>");
        editText0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText0.setText("");
                ScanI2CAsyncTask scanI2CAsyncTask = new ScanI2CAsyncTask();
                scanI2CAsyncTask.execute();
            }
        });
        buttonDisplay = findViewById(R.id.buttonDisplay);
        buttonDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText0.setText("");
                StartDisplayAsyncTask startDisplayAsyncTask = new StartDisplayAsyncTask();
                startDisplayAsyncTask.execute();
            }
        });
        textViewBottom = findViewById(R.id.textViewBottom);
        setupLedStrip();
        /**
        float[] hsv = {1f, 1f, 1f};
        for (int i = 0; i < mLedColors.length; i++) { // Assigns gradient colors.
            hsv[0] = i * 360.f / mLedColors.length;
            mLedColors[i] = Color.HSVToColor(0, hsv);
        }
        try {
            mLedstrip.write(mLedColors);
        } catch (IOException e) {
            Log.e(TAG, "Error setting LED colors", e);
        }
         */
        try {
            for (int led = 0; led < RainbowHat.LEDSTRIP_LENGTH; led++) {
                mLedColors[led] = Color.rgb(72, 36, 6);
            }
            mLedstrip.write(mLedColors);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        destroyLedStrip();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyLedStrip();
    }

    private void setupLedStrip() {
        mLedColors = new int[NUM_LEDS];
        try {
            Log.d(TAG, "Initializing LED strip");
            mLedstrip = RainbowHat.openLedStrip();
            mLedstrip.setBrightness(LED_BRIGHTNESS);
        } catch (IOException e) {
            Log.e(TAG, "Error initializing LED strip", e);
        }
    }

    private void destroyLedStrip() {
        if (mLedstrip != null) {
            try {
                for (int led = 0; led < RainbowHat.LEDSTRIP_LENGTH; led++) {
                    mLedColors[led] = Color.BLACK;
                }
                mLedstrip.write(mLedColors);
                mLedstrip.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception closing LED strip", e);
            } finally {
                mLedstrip = null;
            }
        }
    }

    class ScanI2CAsyncTask extends AsyncTask {

        I2cDevice device;

        @Override
        protected Object doInBackground(Object[] objects) {
            PeripheralManager pManager = PeripheralManager.getInstance();
            List<String> deviceList = pManager.getI2cBusList();
            Log.i("deviceList length", String.valueOf(deviceList.size()));
            Log.i("device", deviceList.get(0));
            try {
                device = pManager.openI2cDevice(deviceList.get(0), 0x70);
                publishProgress("success opening HT16K33 device at 0x70");
                Log.i("i2cDevice", "success opening HT16K33 device at 0x70");
                try {
                    device.readRegByte(0x0);
                    Log.i("ScanI2CAsyncTask", "<< SUCCESS >> reading 1 Byte of data");
                    publishProgress("<< SUCCESS >> reading 1 Byte of data");
                } catch (IOException inE) {
                    Log.i("ScanI2CAsyncTask", "failure reading data");
                    publishProgress("failure reading data");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    device.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                device = null;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
            editText0.append(String.format("%s", values[0]));
            editText0.append("\r\n");
        }
    }

    class StartDisplayAsyncTask extends AsyncTask {

        I2cDevice i2cDevice;

        @Override
        protected Object doInBackground(Object[] objects) {
            PeripheralManager peripheralManager = PeripheralManager.getInstance();
            List<String> deviceList = peripheralManager.getI2cBusList();
            Log.i("deviceList length", String.valueOf(deviceList.size()));
            Log.i("device", deviceList.get(0));
            try {
                i2cDevice = peripheralManager.openI2cDevice(deviceList.get(0), 0x3c);
                publishProgress("success opening I2C device at 0x3c");
                Log.i("i2cDevice", "success opening I2C device at 0x3c");
                try {
                    i2cDevice.readRegByte(0x0);
                    Log.i("ScanI2CAsyncTask", "<< SUCCESS >> reading 1 Byte of data");
                    publishProgress("<< SUCCESS >> reading 1 Byte of data");

                } catch (IOException inE) {
                    Log.i("ScanI2CAsyncTask", "failure reading data");
                    publishProgress("failure reading data");
                }
            } catch (IOException e) {
                e.printStackTrace();
                publishProgress("error opening address 0x3C");
            } finally {
                try {
                    i2cDevice.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                i2cDevice = null;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
            editText0.append(String.format("%s", values[0]));
            editText0.append("\r\n");
        }
    }

}
