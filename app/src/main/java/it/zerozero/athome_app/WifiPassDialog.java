package it.zerozero.athome_app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.util.List;

public class WifiPassDialog extends DialogFragment {

    private View fragmentView;
    private EditText editTextWifiPassword;
    private static String ssid;
    WifiPassInterface listener;

    static WifiPassDialog newInstance(String title, String selectedSsid){
        WifiPassDialog fragment = new WifiPassDialog();
        ssid = selectedSsid;
        Bundle args = new Bundle();
        args.putString("title", title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String title = getArguments().getString("title");
        LayoutInflater inflater = getActivity().getLayoutInflater();
        fragmentView = inflater.inflate(R.layout.wifi_pass_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setView(R.layout.wifi_pass_dialog);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                listener.onPassOk(editTextWifiPassword.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        try {
            listener = (WifiPassDialog.WifiPassInterface) getContext();
        }
        catch (ClassCastException cce) {
            throw  new ClassCastException("Activity must implement WifiPassInterface");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        Dialog dialog = builder.create();
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        editTextWifiPassword = fragmentView.findViewById(R.id.wifiPass);
    }

    public interface WifiPassInterface {
        void onPassOk(String pass);
    }

}
