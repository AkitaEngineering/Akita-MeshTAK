package com.atakmap.android.ipc;

import android.content.IntentFilter;

public final class AtakBroadcast {

    private AtakBroadcast() {
    }

    public static class DocumentedIntentFilter extends IntentFilter {
        public void addAction(String action, String documentation) {
            super.addAction(action);
        }
    }
}
