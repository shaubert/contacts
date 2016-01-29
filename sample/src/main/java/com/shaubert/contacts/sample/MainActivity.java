package com.shaubert.contacts.sample;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import com.shaubert.contacts.ContactsHelper;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 24;

    private ContactsAdapter contactsAdapter;

    private ListView listView;
    private View progressView;
    private View errorView;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshState();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.list);
        progressView = findViewById(R.id.progress);
        errorView = findViewById(R.id.error);

        listView.setVisibility(View.GONE);
        progressView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);

        errorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshContacts();
            }
        });

        contactsAdapter = new ContactsAdapter();
        listView.setAdapter(contactsAdapter);
    }

    private void refreshContacts() {
        if (!checkContactsPermission()) {
            return;
        }

        App.get().getContacts().rebuildCache();
    }

    private boolean checkContactsPermission() {
        int checkResult = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        if (checkResult == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CONTACTS},
                PERMISSIONS_REQUEST_READ_CONTACTS);

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                refreshContacts();
            }
        }
    }

    private void refreshList() {
        contactsAdapter.replaceAll(App.get().getContacts().getAllContacts());
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(ContactsEvent.NAME));
        refreshState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    private void refreshState() {
        ContactsHelper contacts = App.get().getContacts();
        ContactsHelper.State state = contacts.getState();
        switch (state) {
            case NOT_INITIALIZED:
                listView.setVisibility(View.GONE);
                progressView.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.GONE);

                refreshContacts();
                break;
            case INITIALIZING:
                listView.setVisibility(View.GONE);
                progressView.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.GONE);
                break;
            case INITIALIZED:
                listView.setVisibility(View.VISIBLE);
                progressView.setVisibility(View.GONE);
                errorView.setVisibility(View.GONE);

                refreshList();
                break;
            case ERROR:
                listView.setVisibility(View.GONE);
                progressView.setVisibility(View.GONE);
                errorView.setVisibility(View.VISIBLE);
                break;
        }
    }

}
