package it.zerozero.athome_app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.customtabs.CustomTabsIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.bmx280.Bmx280;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.ht16k33.Ht16k33;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.android.things.pio.Gpio;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {

    private TextView textViewIP;
    private Button buttonPerActiv;
    private Button buttonBrowser;
    private com.google.android.things.contrib.driver.button.Button buttonB;
    private Runnable updateSecond;
    private Handler updateSecondHandler;
    private Handler serverHandler;
    private WifiManager wifiManager;
    private AlphanumericDisplay display;
    private Bmx280 sensor;
    private Apa102 ledStrip;
    private int[] ledColorsAr = new int[RainbowHat.LEDSTRIP_LENGTH];
    public final int WARM_WHITE = Color.rgb(72, 36, 6);
    private Gpio ledRed;
    private Gpio ledGreen;
    private Gpio ledBlue;
    protected long updateSecondSeconds = 0;
    private static Thread serverThread;
    private NsdHelper nsdHelper;
    private String displayIpAddress = "init.more";

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        textViewIP = findViewById(R.id.textViewIP);
        textViewIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    display.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    Log.i("Temp", String.valueOf(sensor.readTemperature()));
                    Log.i("Press", String.valueOf(sensor.readPressure()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        updateSecondHandler = new Handler();
        updateSecond = new Runnable() {
            @Override
            public void run() {
                updateSecondSeconds++;

                try {
                    if (ledBlue != null) {
                        ledBlue.setValue(!ledBlue.getValue());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (updateSecondSeconds % 60 == 0) {
                    RunningDash runningDash = new RunningDash(display, ledStrip, ledColorsAr);
                    runningDash.execute();
                    Log.i("updateSecondSeconds", String.valueOf(updateSecondSeconds));
                }

                try {
                    // String ipStr = android.text.format.Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
                    // textViewIP.setText("WiFi IP: " + ipStr);
                    textViewIP.setText("");
                    ArrayList<String> allIntfIPAddr = getIPAddresses(true);
                    for (String ifip : allIntfIPAddr) {
                        textViewIP.append(ifip + "\r\n");
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Unable to get IP addresses", Toast.LENGTH_SHORT).show();
                    textViewIP.setText("???");
                }

                boolean goStartServer = false;
                if (serverThread != null) {
                    if (!serverThread.isAlive()) goStartServer = true;
                }
                else goStartServer = true;

                if (goStartServer) {
                    serverThread = new Thread(new TCPServer.ServerThread(serverHandler, 19881));
                    serverThread.start();
                    Toast.makeText(MainActivity.this, "serverThread started", Toast.LENGTH_SHORT).show();
                }

                updateSecondHandler.postDelayed(this, 1000);
            }
        };

        serverHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                Gson gson = new Gson();
                Bundle bundle = msg.getData();
                String arrayString = bundle.getString("receivedStr");
                LedStripCommands receivedLedStripCommands = gson.fromJson(arrayString, LedStripCommands.class);

                if (receivedLedStripCommands != null && ledStrip != null) {
                    try {
                        ledStrip.write(receivedLedStripCommands.getLedColorsAr());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        buttonPerActiv = findViewById(R.id.buttonPerActiv);
        buttonPerActiv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent perActivIntent = new Intent(getApplicationContext(), PeripheralActivity0.class);
                startActivity(perActivIntent);
            }
        });
        buttonBrowser = findViewById(R.id.buttonBrowser);
        buttonBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "http://www.google.com";
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(000000);
                builder.setShowTitle(true);

                CustomTabsIntent customTabsIntent = builder.build();
                try {
                    customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());

    }

    @Override
    protected void onResume() {
        super.onResume();

        updateSecondHandler.post(updateSecond);

        try {
            ledBlue = RainbowHat.openLedBlue();
            ledGreen = RainbowHat.openLedGreen();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            display = RainbowHat.openDisplay();
            display.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX);
            display.display("o  o");
            display.setEnabled(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ledStrip = RainbowHat.openLedStrip();
            ledStrip.setBrightness(15);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            sensor = RainbowHat.openSensor();
            sensor.setTemperatureOversampling(Bmx280.OVERSAMPLING_1X);
            sensor.setPressureOversampling(Bmx280.OVERSAMPLING_1X);
        } catch (IOException e) {
            e.printStackTrace();
        }

        LedStripCommands ledStripCommands = new LedStripCommands();
        try {
            ledStrip.write(ledStripCommands.getLedColorsAr());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            buttonB = RainbowHat.openButtonB();
            buttonB.setOnButtonEventListener(new com.google.android.things.contrib.driver.button.Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(com.google.android.things.contrib.driver.button.Button button, boolean pressed) {
                    if(pressed) {
                        ScrollIp scrollIp = new ScrollIp();
                        scrollIp.execute();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        nsdHelper = new NsdHelper(getApplicationContext());
        nsdHelper.initializeRegistrationListener();
        nsdHelper.registerService(19881);
    }

    @Override
    protected void onPause() {
        super.onPause();

        updateSecondHandler.removeCallbacks(updateSecond);

        try {
            if (ledBlue != null) {
                ledBlue.setValue(false);
                ledBlue.close();
            }
            if (ledGreen != null) {
                ledGreen.setValue(false);
                ledGreen.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        try {
            display.clear();
            display.setEnabled(false);
            display.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (ledStrip != null) {
            try {
                for (int led = 0; led < RainbowHat.LEDSTRIP_LENGTH; led++) {
                    ledColorsAr[led] = Color.BLACK;
                }
                ledStrip.write(ledColorsAr);
                ledStrip.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                ledStrip = null;
            }
        }

        try {
            sensor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            buttonB.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            buttonB = null;
        }

        nsdHelper.tearDown();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (display != null) {
            try {
                display.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                display = null;
            }
        }

        try {
            ledBlue.close();
            ledGreen.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ledBlue = null;
            ledGreen = null;
        }
    }

    public native String stringFromJNI();

    public ArrayList<String> getIPAddresses(boolean useIPv4) {
        ArrayList<String> ipAddressList = new ArrayList<>();
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        String sIfName = intf.getDisplayName();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        displayIpAddress = sAddr;
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                ipAddressList.add(String.format("%s has IP v4 %s", sIfName, sAddr));
                        } else {
                            if (!isIPv4) {
                                ipAddressList.add("[IP v6 not supported]");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ipAddressList;
    }

    public static class RunningDash extends AsyncTask {

        private AlphanumericDisplay mDisplay;
        private Apa102 mLedStrip;
        private int[] mLedArray;

        public RunningDash(AlphanumericDisplay mDisplay, Apa102 mLedStrip, int[] mLedArray) {
            this.mDisplay = mDisplay;
            this.mLedStrip = mLedStrip;
            this.mLedArray = mLedArray;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mDisplay != null) {
                try {
                    mDisplay.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            /**
            try {
                ledStrip.setDirection(Apa102.Direction.REVERSED);
                for (int led = 0; led < RainbowHat.LEDSTRIP_LENGTH; led++) {
                    ledColorsAr[led] = WARM_WHITE;
                    for (int blackLed = 0; blackLed < led; blackLed++) {
                        ledColorsAr[blackLed] = Color.BLACK;
                    }
                    ledStrip.write(ledColorsAr);
                    Thread.sleep(50);
                }
                for (int led = 0; led < RainbowHat.LEDSTRIP_LENGTH; led++) {
                    ledColorsAr[led] = Color.BLACK;
                }
                ledStrip.write(ledColorsAr);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                ledStrip.setDirection(Apa102.Direction.NORMAL);
            }
            */

            if(mDisplay != null) {
                try {
                    mDisplay.display("-   ");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mDisplay.display(" -  ");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mDisplay.display("  - ");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mDisplay.display("   -");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mDisplay.display("    ");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                for (int led = 0; led < RainbowHat.LEDSTRIP_LENGTH; led++) {
                    mLedArray[led] = Color.BLACK;
                }
                mLedStrip.write(mLedArray);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if (mDisplay != null) {
                try {
                    mDisplay.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public class ScrollIp extends AsyncTask {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(ledGreen != null) {
                try {
                    ledGreen.setValue(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            if (displayIpAddress != null) {
                Log.i("displayIpAddress", displayIpAddress);
                String[] octet = displayIpAddress.split(".");
                for(String o : octet) {
                    Log.i("octet", o);
                }
            }
            else {
                Log.i("displayIpAddress", "null");
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if(ledGreen != null) {
                try {
                    ledGreen.setValue(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
