package com.shaubert.contacts.sample;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.i18n.phonenumbers.Phonenumber;
import com.shaubert.contacts.Contact;
import com.shaubert.contacts.Phones;
import com.shaubert.ui.remoteimageview.RemoteImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContactListItemPresenter {
    private RemoteImageView image;
    private TextView name;
    private TextView details;
    private View view;
    private Contact data;

    private String[] possibleRegions;

    public ContactListItemPresenter(LayoutInflater inflater, ViewGroup parent) {
        view = inflater.inflate(R.layout.contact_list_item, parent, false);
        image = (RemoteImageView) view.findViewById(R.id.image);
        name = (TextView) view.findViewById(R.id.name);
        details = (TextView) view.findViewById(R.id.details);

        List<String> regions = new ArrayList<String>(Arrays.asList(Phones.getPossibleRegions(parent.getContext())));
        regions.add("ru");
        possibleRegions = regions.toArray(new String[regions.size()]);
    }

    public View getView() {
        return view;
    }

    public Contact getData() {
        return data;
    }

    public void refresh() {
        if (data != null) {
            view.setVisibility(View.VISIBLE);

            String imageUrl = data.getImageUri() != null ? data.getImageUri().toString() : null;
            image.setImageUrl(imageUrl);

            name.setText(data.getName());

            String phone = data.getPhone();
            if (!TextUtils.isEmpty(phone)) {
                Phonenumber.PhoneNumber phoneNumber = Phones.parsePhone(phone, possibleRegions);
                if (phoneNumber != null) {
                    phone = Phones.formatInternationalPhone(phoneNumber);
                }

                details.setText(phone);
                details.setVisibility(View.VISIBLE);
            } else {
                details.setVisibility(View.GONE);
            }
        } else {
            view.setVisibility(View.GONE);
        }
    }

    public void swapData(Contact data) {
        this.data = data;
        refresh();
    }
}
