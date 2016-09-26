package org.poopeeland.tinytinyfeed.settings;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Created by setdemr on 26/09/2016.
 */

public class TrimmedEditTextPreference extends EditTextPreference {

    public TrimmedEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TrimmedEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TrimmedEditTextPreference(Context context) {
        super(context);
    }

    @Override
    public void setText(final String text) {
        super.setText(text.trim());
    }
}
