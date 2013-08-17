package fr.gabuzomeu.canadeche;

import android.app.Activity;
import android.os.Bundle;


/**
 * Created by yann on 31/07/13.
 */
public class SettingsActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }





}





