package com.shaubert.contacts;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Locale;

public class Phones {

    public static String formatInternationalPhone(String phone) {
        return formatInternationalPhone(phone, "");
    }

    public static String formatInternationalPhone(String phone, String defaultRegion) {
        Phonenumber.PhoneNumber phoneNumber = parseInternationalPhone(phone, defaultRegion);
        if (phoneNumber == null) {
            return phone;
        }

        return formatInternationalPhone(phoneNumber);
    }

    public static String formatInternationalPhone(Phonenumber.PhoneNumber phoneNumber) {
        return PhoneNumberUtil.getInstance().format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
    }

    public static String formatE164(Phonenumber.PhoneNumber phoneNumber) {
        return PhoneNumberUtil.getInstance().format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    public static String appendPlusIfNeeded(String phone) {
        if (TextUtils.isEmpty(phone)) return phone;

        if (!phone.contains("+")) {
            phone = "+" + phone.trim();
        }

        return phone;
    }

    public static Phonenumber.PhoneNumber parsePhone(String phone, Context context) {
        return parsePhone(phone, getPossibleRegions(context));
    }

    public static Phonenumber.PhoneNumber parsePhone(String phone, String[] possibleRegions) {
        if (phone == null) return null;

        Phonenumber.PhoneNumber phoneNumber;
        if (phone.startsWith("+")) {
            phoneNumber = parseInternationalPhone(phone);
            if (phoneNumber == null) {
                phoneNumber = parseLocalPhone(phone, possibleRegions);
            }
        } else {
            phoneNumber = parseLocalPhone(phone, possibleRegions);
        }
        return phoneNumber;
    }

    public static Phonenumber.PhoneNumber parseInternationalPhone(String phone) {
        return parseInternationalPhone(phone, "");
    }

    public static Phonenumber.PhoneNumber parseInternationalPhone(String phone, String defaultRegion) {
        phone = appendPlusIfNeeded(phone);
        if (TextUtils.isEmpty(phone)) return null;

        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        try {
            return util.parse(phone, defaultRegion);
        } catch (NumberParseException ignored) {
        }

        return null;
    }

    public static Phonenumber.PhoneNumber parseLocalPhone(String phone, Context context) {
        return parseLocalPhone(phone, getPossibleRegions(context));
    }

    public static String[] getPossibleRegions(Context context) {
        return new String[] {
                getRegionFromSim(context),
                getRegionFromNetwork(context),
                getRegionFromLocale(),
        };
    }

    public static Phonenumber.PhoneNumber parseLocalPhone(String phone, String[] possibleRegions) {
        for (String region : possibleRegions) {
            if (!TextUtils.isEmpty(region)) {
                try {
                    Phonenumber.PhoneNumber number = PhoneNumberUtil.getInstance().parse(phone, region.toUpperCase());
                    if (PhoneNumberUtil.getInstance().isValidNumber(number)) {
                        return number;
                    }
                } catch (NumberParseException ignored) {
                    Log.e("!!!", "!!!", ignored);
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
            return manager.getNetworkCountryIso();
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String getRegionFromLocale() {
        return Locale.getDefault().getCountry();
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
