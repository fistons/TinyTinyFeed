package org.poopeeland.tinytinyfeed.settings;


import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import org.poopeeland.tinytinyfeed.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment {


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

}
