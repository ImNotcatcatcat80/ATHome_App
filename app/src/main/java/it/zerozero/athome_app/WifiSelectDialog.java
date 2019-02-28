package it.zerozero.athome_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class WifiSelectDialog extends android.app.DialogFragment {

    private View fragmentView;
    private static List<ScanResult> networks;
    private static String[] ssidArray;
    WifiDialogInterface listener;

    static WifiSelectDialog newInstance(String title, List<ScanResult> wifiNetworks){
        WifiSelectDialog fragment = new WifiSelectDialog();
        networks = wifiNetworks;
        ssidArray = new String[networks.size()];
        for(int n = 0; n <= networks.size() - 1; n++ ) {
            ssidArray[n] = networks.get(n).SSID;
        }
        Bundle args = new Bundle();
        args.putString("title", title);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String title = getArguments().getString("title");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
                .setItems(ssidArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                listener.onSsidSelected(ssidArray[i]);
            }
        })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        try {
            listener = (WifiDialogInterface) getContext();
        }
        catch (ClassCastException cce) {
            throw  new ClassCastException("Activity must implement WifiDialogInterface");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        Dialog dialog = builder.create();
        return dialog;
    }

    public interface WifiDialogInterface {
        public void onSsidSelected(String ssid);
    }

}
