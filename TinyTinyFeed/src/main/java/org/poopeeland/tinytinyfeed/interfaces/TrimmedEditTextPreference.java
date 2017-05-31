package org.poopeeland.tinytinyfeed.interfaces;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Extends {@link EditTextPreference} to trim the input.
 * Created by setdemr on 26/09/2016.
 */
public class TrimmedEditTextPreference extends EditTextPreference {

    public TrimmedEditTextPreference(final Context context, final AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TrimmedEditTextPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public TrimmedEditTextPreference(final Context context) {
        super(context);
    }

    @Override
    public void setText(final String text) {
        super.setText(text.trim());
    }
}
