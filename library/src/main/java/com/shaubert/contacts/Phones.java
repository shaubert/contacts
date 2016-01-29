package com.shaubert.contacts;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class Phones {

    public static String formatInternationalPhone(String phone) {
        Phonenumber.PhoneNumber phoneNumber = parseInternationalPhone(phone);
        if (phoneNumber == null) {
            return phone;
        }

        return PhoneNumberUtil.getInstance().format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
    }

    public static String appendPlusIfNeeded(String phone) {
        if (TextUtils.isEmpty(phone)) return phone;

        if (!phone.contains("+")) {
            phone = "+" + phone.trim();
        }

        return phone;
    }

    public static Phonenumber.PhoneNumber parseInternationalPhone(String phone) {
        phone = appendPlusIfNeeded(phone);
        if (TextUtils.isEmpty(phone)) return null;

        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        try {
            return util.parse(phone, "");
        } catch (NumberParseException ignored) {
        }

        return null;
    }

    public static Phonenumber.PhoneNumber parseLocalPhone(String phone, Context context) {
        String[] possibleRegions = {
                getRegionFromSim(context),
                getRegionFromNetwork(context)
        };

        return parseLocalPhone(phone, possibleRegions);
    }

    public static Phonenumber.PhoneNumber parseLocalPhone(String phone, String[] possibleRegions) {
        for (String region : possibleRegions) {
            if (!TextUtils.isEmpty(region)) {
                try {
                    Phonenumber.PhoneNumber number = PhoneNumberUtil.getInstance().parse(phone, region);
                    if (PhoneNumberUtil.getInstance().isValidNumber(number)) {
                        return number;
                    }
                } catch (NumberParseException ignored) {
                }
            }
        }

        return null;
    }

    public static String getRegionFromSim(Context context) {
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return manager.getSimCountryIso();
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String getRegionFromNetwork(Context context) {
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return manager.getSimCountryIso();
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String formatE164(Phonenumber.PhoneNumber phoneNumber) {
        return PhoneNumberUtil.getInstance().format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    public static String leaveOnlyDigits(String phone) {
        if (TextUtils.isEmpty(phone)) return phone;
        return PhoneNumberUtil.normalizeDigitsOnly(phone);
    }

    public static String leaveOnlyDigitsAndPlus(String phone) {
        if (TextUtils.isEmpty(phone)) return phone;
        boolean hasPlus = phone.startsWith("+");
        String digitsOnly = PhoneNumberUtil.normalizeDigitsOnly(phone);
        return hasPlus ? ("+" + digitsOnly) : digitsOnly;
    }
}
