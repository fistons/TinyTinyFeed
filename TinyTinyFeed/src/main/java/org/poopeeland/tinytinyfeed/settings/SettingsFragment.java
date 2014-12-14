package org.poopeeland.tinytinyfeed.settings;


import android.app.Fragment;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;


/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Preference myPref = findPreference(TinyTinyFeedWidget.BG_COLOR_KEY);
        myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {


                return true;
            }
        });
    }

}
