package it.zerozero.athome_app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;

public class WifiSelectDialog extends android.app.DialogFragment {

    private View fragmentView;
    private String[] ssids = new String[] {
            "a",
            "bb",
            "ccc"
    };
    private boolean[] checkedSsids = new boolean[] {
            false,
            false,
            true
    };

    static WifiSelectDialog newInstance(String title){
        WifiSelectDialog fragment = new WifiSelectDialog();
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
        builder.setTitle(title);
        builder.setMessage("WiFi selection dialog.");
        builder.setMultiChoiceItems(ssids, checkedSsids, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {

            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        Dialog dialog = builder.create();
        return dialog;
    }
}
