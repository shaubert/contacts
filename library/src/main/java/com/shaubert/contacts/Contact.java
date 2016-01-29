package com.shaubert.contacts;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Pair;

import java.io.Serializable;
import java.util.*;

public class Contact implements Comparable<Contact>, Parcelable, Serializable {
    private String id;
    private String name;
    private Uri imageUri;
    private Set<String> phones = new HashSet<>();
    private String defaultPhone;
    private boolean starred;
    private boolean frequent;
    private int timesContacted;

    private transient String firstName;
    private transient String lastName;

    public Contact() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return getName(NameKind.FULL_NAME);
    }

    public void setName(String name) {
        this.name = name;

        Pair<String, String> splitName = NamesUtil.getNameSplit(name);
        firstName = splitName.first;
        lastName = splitName.second;
    }

    public String getName(NameKind nameKind) {
        switch (nameKind) {
            case FIRST_NAME:
                return firstName;

            case LAST_NAME:
                return lastName;

            case FULL_NAME:
            default:
                if (!TextUtils.isEmpty(name)) {
                    return name;
                }
                if (!TextUtils.isEmpty(defaultPhone)) {
                    return name = defaultPhone;
                }
                if (phones.size() > 0) {
                    return name = phones.iterator().next();
                }

                return null;
        }
    }

    public boolean hasNames() {
        return !TextUtils.isEmpty(lastName) && !TextUtils.isEmpty(firstName);
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri contactUri) {
        this.imageUri = contactUri;
    }

    public Set<String> getPhones() {
        return Collections.unmodifiableSet(phones);
    }

    public void addPhone(String phone) {
        phones.add(phone);
    }

    public void setPhones(Collection<String> phones) {
        this.phones.clear();
        this.phones.addAll(phones);
    }

    public String getPhone() {
        if (!TextUtils.isEmpty(defaultPhone)) {
            return defaultPhone;
        }
        if (!phones.isEmpty()) {
            return phones.iterator().next();
        }
        return null;
    }

    public String getDefaultPhone() {
        return defaultPhone;
    }

    public void setDefaultPhone(String defaultPhone) {
        this.defaultPhone = defaultPhone;
    }

    public boolean isStarred() {
        return starred;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    public boolean isFrequent() {
        return frequent;
    }

    public void setFrequent(boolean frequent) {
        this.frequent = frequent;
    }

    public int getTimesContacted() {
        return timesContacted;
    }

    public void setTimesContacted(int timesContacted) {
        this.timesContacted = timesContacted;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public int compareTo(Contact another) {
        if (name != null) {
            return name.compareTo(another.getName(NameKind.FULL_NAME));
        }
        return another.name == null ? 0 : -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Contact)) return false;

        Contact contact = (Contact) o;

        if (defaultPhone != null ? !defaultPhone.equals(contact.defaultPhone) : contact.defaultPhone != null)
            return false;
        if (id != null ? !id.equals(contact.id) : contact.id != null) return false;
        if (imageUri != null ? !imageUri.equals(contact.imageUri) : contact.imageUri != null) return false;
        if (name != null ? !name.equals(contact.name) : contact.name != null) return false;
        if (phones != null ? !phones.equals(contact.phones) : contact.phones != null) return false;
        if (starred != contact.starred) return false;
        if (frequent != contact.frequent) return false;
        if (timesContacted != contact.timesContacted) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (imageUri != null ? imageUri.hashCode() : 0);
        result = 31 * result + (phones != null ? phones.hashCode() : 0);
        result = 31 * result + (defaultPhone != null ? defaultPhone.hashCode() : 0);
        result = 31 * result + (starred ? 1 : 0);
        result = 31 * result + (frequent ? 1 : 0);
        result = 31 * result + (timesContacted);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
        dest.writeParcelable(this.imageUri, flags);
        dest.writeStringList(new ArrayList<>(this.phones));
        dest.writeString(defaultPhone);
        dest.writeInt(starred ? 1 : 0);
        dest.writeInt(frequent ? 1 : 0);
        dest.writeInt(timesContacted);
    }

    Contact(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.imageUri = in.readParcelable(Uri.class.getClassLoader());
        this.phones.addAll(in.createStringArrayList());
        this.defaultPhone = in.readString();
        this.starred = in.readInt() > 0;
        this.frequent = in.readInt() > 0;
        this.timesContacted = in.readInt();
    }

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        public Contact createFromParcel(Parcel source) {
            return new Contact(source);
        }

        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };
}
