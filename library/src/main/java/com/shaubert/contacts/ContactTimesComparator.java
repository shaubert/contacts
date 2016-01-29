package com.shaubert.contacts;

import java.util.Comparator;

public class ContactTimesComparator implements Comparator<Contact> {
    @Override
    public int compare(Contact lhs, Contact rhs) {
        return compare(rhs.getTimesContacted(), lhs.getTimesContacted());
    }

    int compare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }
}
