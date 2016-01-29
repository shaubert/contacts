package com.shaubert.contacts;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.*;

public class ContactsHelper {

    public static final String TAG = ContactsHelper.class.getSimpleName();

    public static boolean LOGGING = true;

    public enum State {
        NOT_INITIALIZED,
        INITIALIZING,
        INITIALIZED,
        ERROR
    }

    public static final int MAX_FREQUENT_CONTACTS_DEFAULT_VALUE = 10;

    private static final String[] CONTACTS_PROJECTION = {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_ID,
            ContactsContract.Contacts.STARRED,
            ContactsContract.Contacts.TIMES_CONTACTED,
    };

    private static final String CONTACTS_WHERE =
            "ifnull(" + ContactsContract.Contacts.HAS_PHONE_NUMBER + ", 0)  == 1";

    private static final String[] PHONES_PROJECTION = {
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY
    };

    private static ContactsHelper instance;

    public static ContactsHelper get(Context context) {
        if (instance == null) {
            instance = new ContactsHelper(context.getApplicationContext());
        }
        return instance;
    }

    private Context appContext;
    private ContactsStateListener stateListener;
    private ExceptionCallback exceptionCallback;
    private int maxFrequentContacts = MAX_FREQUENT_CONTACTS_DEFAULT_VALUE;
    private Handler handler = new Handler();
    private ContentObserver contactsObserver = new ContentObserver(handler) {
        @Override
        public void onChange(boolean selfChange) {
            if (!selfChange) {
                rebuildCache();
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (!selfChange) {
                rebuildCache();
            }
        }
    };

    private Map<String, Contact> contactMap = new HashMap<String, Contact>();
    private Map<String, Contact> phoneMap = new HashMap<String, Contact>();
    private AsyncTask<Void, Void, Boolean> asyncTask;
    private State state = State.NOT_INITIALIZED;

    private ContactsHelper(Context appContext) {
        this.appContext = appContext;
        exceptionCallback = new LoggingExceptionsCallback();

        ContentResolver contentResolver = appContext.getContentResolver();
        contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactsObserver);
        rebuildCache();
    }

    public void setStateListener(ContactsStateListener stateListener) {
        this.stateListener = stateListener;
    }

    public void setExceptionCallback(ExceptionCallback exceptionCallback) {
        this.exceptionCallback = exceptionCallback;
    }

    public void setMaxFrequentContactsCount(int maxFrequentContacts) {
        this.maxFrequentContacts = maxFrequentContacts;
    }

    public boolean isInitialized() {
        return state == State.INITIALIZED;
    }

    public State getState() {
        return state;
    }

    protected void setState(State state, boolean hasChanges) {
        this.state = state;
        if (stateListener != null) {
            stateListener.onContactsStateChanges(state, hasChanges);
        }
    }

    private void logException(String message, Exception ex) {
        if (exceptionCallback != null) {
            exceptionCallback.logException(message, ex);
        }
    }

    public Set<String> getAllPhones() {
        Set<String> result = new HashSet<String>();
        for (Contact contact : contactMap.values()) {
            result.addAll(contact.getPhones());
        }
        return result;
    }

    public List<Contact> getAllContacts() {
        return new ArrayList<>(contactMap.values());
    }

    public void rebuildCache() {
        if (asyncTask != null) {
            asyncTask.cancel(true);
        }

        asyncTask = new AsyncTask<Void, Void, Boolean>() {
            private Map<String, Contact> resultMap = new HashMap<String, Contact>();

            @Override
            protected void onPreExecute() {
                if (state == State.NOT_INITIALIZED
                        || state == State.ERROR) {
                    setState(State.INITIALIZING, false);
                }
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                return buildCache(resultMap);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (Boolean.TRUE.equals(result)) {
                    setContacts(resultMap);
                } else {
                    setState(State.ERROR, false);
                }
            }

        };
        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private Map<String, Contact> buildPhoneMap(Map<String, Contact> contactMap) {
        Map<String, Contact> phoneMap = new HashMap<>();

        for (Contact contact : contactMap.values()) {
            for (String phone : contact.getPhones()) {
                Contact existingMapping = phoneMap.get(phone);
                if (existingMapping == null) {
                    phoneMap.put(phone, contact);
                } else {
                    if (!existingMapping.hasNames() && contact.hasNames()) {
                        existingMapping.setName(contact.getName(NameKind.FULL_NAME));
                        phoneMap.put(phone, contact);
                        continue;
                    }

                    if (existingMapping.getImageUri() == null
                            && contact.getImageUri() != null) {
                        phoneMap.put(phone, contact);
                        continue;
                    }
                }
            }
        }

        return phoneMap;
    }

    private void setContacts(Map<String, Contact> resultMap) {
        Map<String, Contact> resultPhoneMap = buildPhoneMap(resultMap);

        boolean hasChanges = false;
        for (Contact contact : resultMap.values()) {
            if (!hasChanges) {
                Contact oldMapping = contactMap.get(contact.getId());
                if (oldMapping != null) {
                    if (oldMapping.hashCode() != contact.hashCode()) {
                        hasChanges = true;
                    }
                } else {
                    hasChanges = true;
                }
            }
        }

        contactMap = resultMap;
        phoneMap = resultPhoneMap;

        setState(State.INITIALIZED, hasChanges);
    }

    public Contact getContactByPhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return null;
        }

        if (state == State.NOT_INITIALIZED) {
            rebuildCache();
            return null;
        }

        Contact contact = phoneMap.get(phone);
        if (contact != null) {
            return contact;
        }

        Phonenumber.PhoneNumber phoneNumber = Phones.parseInternationalPhone(phone);
        if (phoneNumber != null) {
            PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
            String formattedPhone = phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
            contact = phoneMap.get(formattedPhone);
            if (contact != null) {
                return contact;
            }

            formattedPhone = PhoneNumberUtil.normalizeDigitsOnly(
                    phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
            formattedPhone = Phones.leaveOnlyDigits(formattedPhone);
            contact = phoneMap.get(formattedPhone);
            if (contact != null) {
                return contact;
            }
        }

        return null;
    }

    public Contact refreshContact(Contact contact) {
        Map<String, Contact> resultMap = new HashMap<>(1);
        ContentResolver resolver = appContext.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(getContactUri(contact.getId()), CONTACTS_PROJECTION, null, null, null);
            if (populateContactsMap(cursor, resultMap) && !resultMap.isEmpty()) {
                for (String phone : contact.getPhones()) {
                    phoneMap.remove(phone);
                }
                contactMap.remove(contact.getId());

                Contact updContact = resultMap.values().iterator().next();
                contactMap.put(updContact.getId(), updContact);
                for (String phone : updContact.getPhones()) {
                    phoneMap.put(phone, updContact);
                }
                return updContact;
            }
        } catch (Exception e) {
            logException("failed to get contact", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return contact;
    }

    private boolean buildCache(Map<String, Contact> resultMap) {
        resultMap.clear();

        ContentResolver resolver = appContext.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, CONTACTS_PROJECTION, CONTACTS_WHERE, null, null);
            return populateContactsMap(cursor, resultMap);
        } catch (Exception e) {
            logException("failed to get contacts", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    private boolean populateContactsMap(Cursor contactsCursor, Map<String, Contact> resultMap) {
        if (contactsCursor != null && contactsCursor.moveToFirst()) {
            long startTime = SystemClock.elapsedRealtime();
            ContentResolver resolver = appContext.getContentResolver();

            int idColumn = contactsCursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
            int nameColumn = contactsCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
            int starredColumn = contactsCursor.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED);
            int timesContactedColumn = contactsCursor.getColumnIndexOrThrow(ContactsContract.Contacts.TIMES_CONTACTED);
            int photoIdColumn = contactsCursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_ID);

            do {
                if (Thread.currentThread().isInterrupted()) {
                    return false;
                }

                String name = contactsCursor.getString(nameColumn);
                if (TextUtils.isEmpty(name)) {
                    continue;
                }
                String id = contactsCursor.getString(idColumn);

                Contact contact = new Contact();
                contact.setId(id);
                contact.setName(name);
                if (contactsCursor.getInt(photoIdColumn) > 0) {
                    Uri contactUri = getContactUri(id);
                    contact.setImageUri(contactUri);
                }
                contact.setStarred(contactsCursor.getInt(starredColumn) > 0);
                contact.setTimesContacted(contactsCursor.getInt(timesContactedColumn));

                resultMap.put(id, contact);
            } while (contactsCursor.moveToNext());

            if (Thread.currentThread().isInterrupted()) {
                return false;
            }

            Cursor phoneCursor = null;
            try {
                phoneCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        PHONES_PROJECTION,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                                + " IN (" + TextUtils.join(",", resultMap.keySet()) + ")",
                        null,
                        null);
                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    int contactIdIndex = phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                    int phoneIndex = phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int superPrimaryIndex = phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY);
                    do {
                        if (Thread.currentThread().isInterrupted()) {
                            return false;
                        }

                        String contactId = phoneCursor.getString(contactIdIndex);
                        Contact contact = resultMap.get(contactId);
                        if (contact == null) {
                            continue;
                        }

                        String contactNumber = phoneCursor.getString(phoneIndex);
                        if (!TextUtils.isEmpty(contactNumber)) {
                            String number = prepareContactNumber(contactNumber);
                            if (isNumberValid(number)) {
                                contact.addPhone(number);
                                if (phoneCursor.getInt(superPrimaryIndex) > 0) {
                                    contact.setDefaultPhone(number);
                                }

                            }
                        }
                    } while (phoneCursor.moveToNext());
                }
            } catch (Exception e) {
                logException("failed to get phone numbers from contacts", e);
            } finally {
                if (phoneCursor != null) {
                    phoneCursor.close();
                }
            }

            for (Iterator<Map.Entry<String, Contact>> iterator = resultMap.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<String, Contact> entry = iterator.next();
                if (entry.getValue().getPhones().isEmpty()) {
                    iterator.remove();
                }
            }

            if (LOGGING) {
                Log.d(TAG, "fetch contacts time = " + (SystemClock.elapsedRealtime() - startTime) + "ms");
            }
            markFrequentContacts(resultMap);
            return true;
        } else {
            return contactsCursor != null;
        }
    }

    private void markFrequentContacts(Map<String, Contact> resultMap) {
        List<Contact> contacts = new ArrayList<>(resultMap.values());
        Collections.sort(contacts, new ContactTimesComparator());
        int count = Math.min(contacts.size(), maxFrequentContacts);
        for (int i = 0; i < count; i++) {
            Contact contact = contacts.get(i);
            if (contact.getTimesContacted() > 0) {
                contact.setFrequent(true);
            }
        }
    }

    private Uri getContactUri(String id) {
        return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(id));
    }

    private boolean isNumberValid(String number) {
        if (TextUtils.isEmpty(number)) {
            return false;
        }
        return true;
    }

    public boolean hasImage(Uri contactUri, ContentResolver resolver) {
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = null;
        try {
            cursor = resolver. query(photoUri,
                    new String[] {
                            ContactsContract.CommonDataKinds.Photo.PHOTO
                    }, null, null, null);

            if (cursor == null || !cursor.moveToFirst()) {
                return false;
            }
            return !cursor.isNull(0);
        } catch (Exception ex) {
            logException("failed to check if contact has image", ex);
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String prepareContactNumber(String phone) {
        if (TextUtils.isEmpty(phone)) return phone;
        int ch = phone.charAt(0);
        int digit = Character.digit(ch, 10);
        if (digit != -1 || ch == '+') {
            return Phones.leaveOnlyDigitsAndPlus(phone);
        } else {
            return null;
        }
    }

}
