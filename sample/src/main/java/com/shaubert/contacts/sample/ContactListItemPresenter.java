package com.shaubert.contacts.sample;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.shaubert.contacts.Contact;
import com.shaubert.ui.remoteimageview.RemoteImageView;

public class ContactListItemPresenter {
    private RemoteImageView image;
    private TextView name;
    private TextView details;
    private View view;
    private Contact data;

    public ContactListItemPresenter(LayoutInflater inflater, ViewGroup parent) {
        view = inflater.inflate(R.layout.contact_list_item, parent, false);
        image = (RemoteImageView) view.findViewById(R.id.image);
        name = (TextView) view.findViewById(R.id.name);
        details = (TextView) view.findViewById(R.id.details);
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
