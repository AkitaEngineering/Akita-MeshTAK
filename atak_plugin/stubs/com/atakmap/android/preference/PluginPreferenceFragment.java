package com.atakmap.android.preference;

import android.content.Context;
import android.preference.PreferenceFragment;

public class PluginPreferenceFragment extends PreferenceFragment {

    public PluginPreferenceFragment(Context context, int resourceId) {
    }

    protected String getSubTitle(String title, String subtitle) {
        return title + "\n" + subtitle;
    }

    public String getSubTitle() {
        return "";
    }
}
