package com.shaubert.contacts.sample;

import android.app.Application;
import android.graphics.Bitmap;
import android.support.v4.content.LocalBroadcastManager;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.utils.L;
import com.shaubert.contacts.ContactsHelper;
import com.shaubert.contacts.ContactsStateListener;
import com.shaubert.ui.remoteimageview.RemoteImageView;

public class App extends Application {

    private static App APP;

    private ContactsHelper contacts;

    private ImageLoader imageLoader;
    private DisplayImageOptions displayImageOptions;

    public App() {
        APP = this;
    }

    public static App get() {
        return APP;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setupImageLoader();
        setupRemoteImageView();

        ContactsHelper.LOGGING = true;
        contacts = ContactsHelper.get(this);
        contacts.setStateListener(new ContactsStateListener() {
            @Override
            public void onContactsStateChanges(ContactsHelper.State state, boolean hasChanges) {
                LocalBroadcastManager.getInstance(APP).sendBroadcast(ContactsEvent.create());
            }
        });
    }

    private void setupImageLoader() {
        displayImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .considerExifParams(true)
                .build();
        ImageLoaderConfiguration imageLoaderConfiguration = new ImageLoaderConfiguration.Builder(this)
                .diskCacheSize(50 * 1024 * 1024)
                .memoryCacheSizePercentage(30)
                .defaultDisplayImageOptions(displayImageOptions)
                .build();
        L.writeLogs(true);
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(imageLoaderConfiguration);
    }

    private void setupRemoteImageView() {
        new RemoteImageView.Setup()
                .imageLoader(imageLoader)
                .displayImageOptions(displayImageOptions)
                .apply();
    }

    public ContactsHelper getContacts() {
        return contacts;
    }

}
