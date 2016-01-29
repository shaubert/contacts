package com.shaubert.contacts.sample;

import android.content.Intent;

public abstract class ContactsEvent {
    public static final String NAME = ContactsEvent.class.getName();

    public static Intent create() {
        return new Intent(NAME);
    }

    private ContactsEvent() {
    }
}
