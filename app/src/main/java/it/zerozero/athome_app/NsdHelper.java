package it.zerozero.athome_app;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.net.InetAddress;

public class NsdHelper {

    Context mContext;
    NsdManager mNsdManager;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.RegistrationListener mRegistrationListener;
    String mServiceInfo;
    public final String NSD_SERVICE_NAME = "ATHome_App";
    public final String NSD_SERVICE_TYPE =  "_athomeapp._tcp";

    public NsdHelper(Context context) {
        mContext = context;
        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
    }

    public void registerService(int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(NSD_SERVICE_NAME);
        serviceInfo.setServiceType(NSD_SERVICE_TYPE);
        serviceInfo.setPort(port);

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
                mServiceInfo = nsdServiceInfo.getServiceName();
                Log.d("NsdHelper", "Service registered.");
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
                Log.d("NsdHelper", "Service found.");
                if (!serviceInfo.getServiceType().equals(NSD_SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d("NsdHelper", "Unknown Service Type: " + serviceInfo.getServiceType());
                } else if (serviceInfo.getServiceName().equals(NSD_SERVICE_NAME)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d("NsdHelper", "Same machine: " + NSD_SERVICE_NAME);
                } else if (serviceInfo.getServiceName().contains(NSD_SERVICE_NAME)){
                    mNsdManager.resolveService(serviceInfo, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {

            }
        };
    }

}
