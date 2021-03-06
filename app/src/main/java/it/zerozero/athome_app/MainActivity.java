/**
 * ----------------------------------------------------------------------------------------------------*
     ___   .___________. __    __    ______   .___  ___.  _______          ___      .______   .______
    /   \  |           ||  |  |  |  /  __  \  |   \/   | |   ____|        /   \     |   _  \  |   _  \
   /  ^  \ `---|  |----`|  |__|  | |  |  |  | |  \  /  | |  |__          /  ^  \    |  |_)  | |  |_)  |
  /  /_\  \    |  |     |   __   | |  |  |  | |  |\/|  | |   __|        /  /_\  \   |   ___/  |   ___/
 /  _____  \   |  |     |  |  |  | |  `--'  | |  |  |  | |  |____      /  _____  \  |  |      |  |
/__/     \__\  |__|     |__|  |__|  \______/  |__|  |__| |_______|____/__/     \__\ | _|      | _|
                                                                |______|
 * -----------------------------------------------------------------------------------------------------*
 Useless and weird, but I like it. Built on Android Things 1.03/1.09
 David Girardello, 2017 - 2019
*/

package it.zerozero.athome_app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.bmx280.Bmx280;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.ht16k33.Ht16k33;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
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


public class MainActivity extends Activity implements WifiSelectDialog.WifiDialogInterface, WifiPassDialog.WifiPassInterface {

    private TextView textViewIP;
    private TextView textViewBottom;
    private Button buttonPerActiv;
    private Button buttonWifi;
    private com.google.android.things.contrib.driver.button.Button buttonA, buttonB, buttonC;
    private Runnable updateSecond;
    private Handler updateSecondHandler;
    private Handler serverHandler;
    private String wifiSsid;
    private WifiManager wifiManager;
    private WifiConfiguration wifiConfig;
    private BroadcastReceiver wifiScanReceiver;
    private AlphanumericDisplay display;
    private ButtonInputDriver keyA, keyS, keyD, keyF, keyG, keyH, keyJ, keyK;
    private ButtonInputDriver keyQ, keyW, keyR, keyT, keyY;
    private Bmx280 sensor;
    private Apa102 ledStrip;
    private int[] ledColorsAr = new int[RainbowHat.LEDSTRIP_LENGTH];
    public final int WARM_WHITE = Color.rgb(72, 36, 6);
    private Gpio ledRed, ledGreen, ledBlue;
    private Speaker pwmBuzzer;
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
        if (wifiManager != null) {
            wifiManager.startScan();
        }
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean scanSuccess = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (scanSuccess) {
                    Log.d("Wifi scan", "scan successful.");
                }
                else {
                    Log.e("Wifi scan", "scan failed.");
                }
            }
        };
        IntentFilter intentFilterWifiScan = new IntentFilter();
        intentFilterWifiScan.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getApplicationContext().registerReceiver(wifiScanReceiver, intentFilterWifiScan);
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
                    if (ledRed != null) {
                        ledRed.setValue(!ledRed.getValue());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (updateSecondSeconds % 60 == 0) {
                    RunningDash runningDash = new RunningDash(display, ledStrip, ledColorsAr);
                    runningDash.execute();
                    Log.i("updateSecondSeconds", String.valueOf(updateSecondSeconds));
                    long hours = updateSecondSeconds / 3600;
                    long minutes = (updateSecondSeconds % 3600) / 60;
                    textViewBottom.setText(String.format(Locale.ITALIAN, "Uptime %d hours %d minutes", hours, minutes));
                }

                try {
                    // String ipStr = android.text.format.Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
                    // textViewIP.setText("WiFi IP: " + ipStr);
                    textViewIP.setText("");
                    ArrayList<String> allIntfIPAddr = getIPAddresses(true, true);
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
        buttonWifi = findViewById(R.id.buttonWifi);
        buttonWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiManager.setWifiEnabled(true);

                // Remove old configurations
                List<WifiConfiguration> oldConfigs = wifiManager.getConfiguredNetworks();
                for( WifiConfiguration i : oldConfigs ) {
                    wifiManager.removeNetwork(i.networkId);
                }

                List<ScanResult> wifiNetworks = wifiManager.getScanResults();
                for(ScanResult r : wifiNetworks) {
                    Log.d("scanResult", r.SSID);
                }
                WifiSelectDialog wifiSelectDialog = WifiSelectDialog.newInstance("Select SSID", wifiNetworks);
                wifiSelectDialog.show(getFragmentManager(), "Select Wifi network");
            }
        });
        textViewBottom = (TextView) findViewById(R.id.sample_text);
        textViewBottom.setText(stringFromJNI());
        textViewBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if(wifiInfo.isConnected()) {
                    textViewBottom.setText("Wifi connected");
                }
                else {
                    textViewBottom.setText("Wifi NOT connected");
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        updateSecondHandler.post(updateSecond);

        try {
            ledRed = RainbowHat.openLedRed();
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
            pwmBuzzer = RainbowHat.openPiezo();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            buttonA = RainbowHat.openButtonA();
            buttonA.setOnButtonEventListener(new com.google.android.things.contrib.driver.button.Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(com.google.android.things.contrib.driver.button.Button button, boolean pressed) {
                    if(pressed) {
                        try {
                            String ipString = getIPAddresses(true, false).get(0);
                            Log.i("ipString", ipString);
                            String[] chunks = ipString.split("\\.");
                            String ip = "IP";
                            ScrollText scrollIp = new ScrollText(1000, ip, chunks[0], chunks[1], chunks[2], chunks[3]);
                            scrollIp.execute();
                        } catch (Exception e) {
                            e.printStackTrace();
                            ScrollText scrollIpErr = new ScrollText(1000, "IP", "Err");
                            scrollIpErr.execute();
                        }
                    }
                }
            });
            buttonB = RainbowHat.openButtonB();
            buttonB.setOnButtonEventListener(new com.google.android.things.contrib.driver.button.Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(com.google.android.things.contrib.driver.button.Button button, boolean pressed) {
                    if(pressed) {
                        ScrollText scrollTxt = new ScrollText(1000,"AT", "HOME", "APP", "/", "//", "///", "////");
                        scrollTxt.execute();
                    }
                }
            });
            buttonC = RainbowHat.openButtonC();
            buttonC.setOnButtonEventListener(new com.google.android.things.contrib.driver.button.Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(com.google.android.things.contrib.driver.button.Button button, boolean pressed) {
                    if(pressed) {
                        try {
                            pwmBuzzer.play(220);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        try {
                            pwmBuzzer.stop();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            keyA = RainbowHat.createButtonInputDriver("key_A", KeyEvent.KEYCODE_A);
            keyA.register();
            keyQ = RainbowHat.createButtonInputDriver("key_Q", KeyEvent.KEYCODE_Q);
            keyQ.register();
            keyS = RainbowHat.createButtonInputDriver("key_S", KeyEvent.KEYCODE_S);
            keyS.register();
            keyW = RainbowHat.createButtonInputDriver("key_W", KeyEvent.KEYCODE_W);
            keyW.register();
            keyD = RainbowHat.createButtonInputDriver("key_D", KeyEvent.KEYCODE_D);
            keyD.register();
            keyF = RainbowHat.createButtonInputDriver("key_F", KeyEvent.KEYCODE_F);
            keyF.register();
            keyR = RainbowHat.createButtonInputDriver("key_R", KeyEvent.KEYCODE_R);
            keyR.register();
            keyG = RainbowHat.createButtonInputDriver("key_G", KeyEvent.KEYCODE_G);
            keyG.register();
            keyT = RainbowHat.createButtonInputDriver("key_T", KeyEvent.KEYCODE_T);
            keyT.register();
            keyH = RainbowHat.createButtonInputDriver("key_H", KeyEvent.KEYCODE_H);
            keyH.register();
            keyY = RainbowHat.createButtonInputDriver("key_Y", KeyEvent.KEYCODE_Y);
            keyY.register();
            keyJ = RainbowHat.createButtonInputDriver("key_J", KeyEvent.KEYCODE_J);
            keyJ.register();
            keyK = RainbowHat.createButtonInputDriver("key_K", KeyEvent.KEYCODE_K);
            keyK.register();
        } catch (IOException ioe) {
            ioe.printStackTrace();
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
            if (ledRed != null) {
                ledRed.setValue(false);
                ledRed.close();
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
            pwmBuzzer.stop();
            pwmBuzzer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pwmBuzzer = null;
        }

        try {
            buttonA.close();
            buttonB.close();
            buttonC.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            buttonA = null;
            buttonB = null;
            buttonC = null;
        }

        try {
            keyA.unregister();
            keyQ.unregister();
            keyS.unregister();
            keyW.unregister();
            keyD.unregister();
            keyF.unregister();
            keyR.unregister();
            keyG.unregister();
            keyT.unregister();
            keyH.unregister();
            keyY.unregister();
            keyJ.unregister();
            keyK.unregister();
        } catch (Exception e) {
            e.printStackTrace();
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
            ledRed.close();
            ledGreen.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ledRed = null;
            ledGreen = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            if(keyCode == KeyEvent.KEYCODE_A) {
                pwmBuzzer.play(523.251);
            }
            if(keyCode == KeyEvent.KEYCODE_Q) {
                pwmBuzzer.play(554.365);
            }
            if(keyCode == KeyEvent.KEYCODE_S) {
                pwmBuzzer.play(587.33);
            }
            if(keyCode == KeyEvent.KEYCODE_W) {
                pwmBuzzer.play(622.254);
            }
            if(keyCode == KeyEvent.KEYCODE_D) {
                pwmBuzzer.play(659.255);
            }
            if(keyCode == KeyEvent.KEYCODE_F) {
                pwmBuzzer.play(698.456);
            }
            if(keyCode == KeyEvent.KEYCODE_R) {
                pwmBuzzer.play(739.989);
            }
            if(keyCode == KeyEvent.KEYCODE_G) {
                pwmBuzzer.play(783.991);
            }
            if(keyCode == KeyEvent.KEYCODE_T) {
                pwmBuzzer.play(830.609);
            }
            if(keyCode == KeyEvent.KEYCODE_H) {
                pwmBuzzer.play(880.0);
            }
            if(keyCode == KeyEvent.KEYCODE_Y) {
                pwmBuzzer.play(932.328);
            }
            if(keyCode == KeyEvent.KEYCODE_J) {
                pwmBuzzer.play(987.767);
            }
            if(keyCode == KeyEvent.KEYCODE_K) {
                pwmBuzzer.play(1046.5);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        try {
            pwmBuzzer.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.onKeyUp(keyCode, event);
    }

    public native String stringFromJNI();

    public ArrayList<String> getIPAddresses(boolean useIPv4, boolean descriptive) {
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
                                if (descriptive) {
                                    ipAddressList.add(String.format("%s has IP v4 %s", sIfName, sAddr));
                                } else {
                                    ipAddressList.add(sAddr);
                                }
                        } else {
                            if (!isIPv4) {
                                ipAddressList.add("ERR IPv6");
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

    @Override
    public void onSsidSelected(String ssid) {
        wifiSsid = ssid;
        textViewBottom.setText(String.format("Selected SSID: %s", ssid));
        wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid); // "\"" + ssid + "\"";
        WifiPassDialog wifiPassDialog = WifiPassDialog.newInstance(String.format("Pass for %s", ssid), ssid);
        wifiPassDialog.show(getFragmentManager(), "Wifi Passkey");
    }

    @Override
    public void onPassOk(String pass) {
        if(wifiConfig != null) {
            wifiConfig.preSharedKey = String.format("\"%s\"", pass); // "\"" + pass + "\"";
            wifiManager.addNetwork(wifiConfig);
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for( WifiConfiguration c : list ) {
                Log.d("Wifi conf list len", String.valueOf(list.size()));
                if(c.SSID != null && c.SSID.equals("\"" + wifiSsid + "\"")) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(c.networkId, true);
                    wifiManager.reconnect();
                    break;
                }
            }
        }
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

    public class ScrollText extends AsyncTask {

        List<String> text2show;
        long delay = -1;

        public ScrollText(Long dly) {
            delay = dly;
            if (delay < 10) {
                delay = 10;
            }
            text2show = new ArrayList<>();
        }

        public ScrollText(long dly, String... textsGroup) {
            delay = dly;
            text2show = new ArrayList<>();
            for(String t : textsGroup) {
                if (t != null) {
                    text2show.add(t);
                }
            }
        }

        public void addText(String newTxt) {
            if(delay < 10) {
                text2show = new ArrayList<>();
            }
            text2show.add(newTxt);
        }

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
            if (text2show != null && display != null) {
                for(String s : text2show) {
                    try {
                        display.display(s);
                        Thread.sleep(delay);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            else {
                Log.i("ScrollText", "something is null.");
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
            if(display != null) {
                try {
                    display.clear();
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }
}
