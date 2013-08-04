package fr.gabuzomeu.canadeche;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.apache.http.cookie.Cookie;

import fr.gabuzomeu.canadeche.authenticators.DlfpAuthenticator;

/**
 * Created by yann on 31/07/13.
 */
public class SettingsFragment extends PreferenceFragment {

    private static String TAG = "CanadecheSettingsFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource( R.xml.preferences);
        addPreferencesFromResource( R.xml.boardconfig_linuxfr);
        addPreferencesFromResource( R.xml.boardconfig_gabuzomeu);
        addPreferencesFromResource( R.xml.boardconfig_euromussels);
        addPreferencesFromResource( R.xml.boardconfig_see);

        Preference button = (Preference)findPreference("login_linuxfr_button");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
            AlertDialog dial = loginDialog( getActivity(), "Log DLFP");
            dial.show();
                return true;
            }
        });



    }

    public AlertDialog loginDialog(Context c, String message) {


        Log.d(TAG, "in dialog");
        LayoutInflater factory = LayoutInflater.from(c);
        final View textEntryView = factory.inflate(R.layout.authentication, null);
        final AlertDialog.Builder failAlert = new AlertDialog.Builder(c);
        failAlert.setTitle("Login/ Register Failed");
        failAlert.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cancelled
            }
        });
        AlertDialog.Builder alert = new AlertDialog.Builder(c);
        alert.setTitle("Login/ Register");
        alert.setMessage(message);
        alert.setView(textEntryView);
        alert.setPositiveButton("Login", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    final EditText usernameInput = (EditText) textEntryView.findViewById(R.id.userNameEditText);
                    final EditText passwordInput = (EditText) textEntryView.findViewById(R.id.passwordEditText);
                    ///ff.login(usernameInput.getText().toString(), passwordInput.getText().toString());
                    Cookie cookie = DlfpAuthenticator.getAuthCookie( usernameInput.getText().toString(), passwordInput.getText().toString() );
                    if( cookie != null){
                        Log.d( TAG, "COOKIE OK : " + cookie.getValue());
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences( getActivity());
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString( "boardconfig_linuxfr_edittext_boardcookie", "linuxfr.org_session=" + cookie.getValue());
                        editor.commit();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        return alert.create();
    }

}
