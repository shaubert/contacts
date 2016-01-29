package com.shaubert.contacts.sample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.shaubert.contacts.Contact;
import com.shaubert.ui.adapters.ListAdapter;

public class ContactsAdapter extends ListAdapter<Contact> {

    public ContactsAdapter() {
        setComparable(true);
    }

    @Override
    protected View createNormalView(Contact item, int pos, ViewGroup parent, LayoutInflater inflater) {
        ContactListItemPresenter presenter = new ContactListItemPresenter(inflater, parent);
        View view = presenter.getView();
        view.setTag(presenter);
        return view;
    }

    @Override
    protected void bindNormalView(View view, Contact item, int pos) {
        ContactListItemPresenter presenter = (ContactListItemPresenter) view.getTag();
        presenter.swapData(item);
    }

    @Override
    public long getItemId(int position) {
        return Math.abs(getItem(position).getId().hashCode());
    }
}
