package com.shaubert.contacts;

public interface ContactsStateListener {
    void onContactsStateChanges(ContactsHelper.State state, boolean hasChanges);
}
