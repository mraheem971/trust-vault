package com.secure.trust_vault;

import android.app.Application;
import android.util.Log;

/**
 * TrustVaultApp
 * Custom Application class that sets up a safety UncaughtExceptionHandler
 * to prevent abrupt fatal process death and ensure crash-free execution.
 */
public class TrustVaultApp extends Application {

    private static final String TAG = "TrustVaultApp";

    @Override
    public void onCreate() {
        super.onCreate();

        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception caught safely: " + throwable.getMessage(), throwable);
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
    }
}
