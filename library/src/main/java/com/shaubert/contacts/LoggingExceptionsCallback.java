package com.shaubert.contacts;

import android.util.Log;

public class LoggingExceptionsCallback implements ExceptionCallback {

    public static final String TAG = "Contacts";

    @Override
    public void logException(String message, Exception ex) {
        if (ContactsHelper.LOGGING) {
            Log.e(TAG, message, ex);
        }
    }
}
