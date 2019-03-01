package it.zerozero.athome_app;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.net.InetAddress;
import java.util.Locale;

public class NsdHelper {

    Context mContext;
    NsdManager mNsdManager;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.RegistrationListener mRegistrationListener;
    public String mServiceName = "ATHomeApp";
    public final String NSD_SERVICE_TYPE =  "_athomeapp._tcp";

    public NsdHelper(Context context) {
        mContext = context;
        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
    }

    public void registerService(int inPort) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setPort(inPort);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(NSD_SERVICE_TYPE);

        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public void startDiscovery() {
        mNsdManager.discoverServices(NSD_SERVICE_TYPE, mNsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public void tearDown() {
        mNsdManager.unregisterService(mRegistrationListener);
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {
                Log.d("NsdHelper", "Service registration failed.");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {

            }

            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                mServiceName = nsdServiceInfo.getServiceName();
                Log.d("NsdHelper", "Service registered with name " + mServiceName);
                Log.i("NsdHelper", "port " + String.valueOf(nsdServiceInfo.getPort()));
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
                Log.d("NsdHelper", "Service unregistered.");
            }
        };

    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {

            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d("NsdHelper", "Service resolved.");
                InetAddress host = serviceInfo.getHost();
                Log.d("NsdHelper", host.getHostAddress());
            }
        };
    }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {

            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d("NsdHelper", "Service discovery started.");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d("NsdHelper", "Service discovery stopped.");
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d("NsdHelper", String.format(Locale.ITALIAN, "service found %s %s %s", String.valueOf(serviceInfo.getPort()), serviceInfo.getServiceName(), serviceInfo.getServiceType()));
                if (!serviceInfo.getServiceType().equals(NSD_SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d("NsdHelper", "Unknown Service Type: " + serviceInfo.getServiceType());
                } else if (serviceInfo.getServiceName().equals(mServiceName)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d("NsdHelper", "Same machine: " + mServiceName);
                } else if (serviceInfo.getServiceName().contains(mServiceName)){
                    mNsdManager.resolveService(serviceInfo, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {

            }
        };
    }

}
