package com.shaubert.contacts;

import android.text.TextUtils;
import android.util.Pair;

import java.util.StringTokenizer;

public class NamesUtil {

    public static Pair<String, String> getNameSplit(String name) {
        if (TextUtils.isEmpty(name)) {
            return new Pair<>(null, null);
        }

        return splitName(name);
    }

    private static Pair<String, String> splitName(String name) {
        String firstName = null;
        String lastName = null;

        if (!TextUtils.isEmpty(name)) {
            StringTokenizer tokenizer = new StringTokenizer(name);
            firstName = nextNameToken(tokenizer);
            lastName = nextNameToken(tokenizer);
        }

        return new Pair<>(firstName, lastName);
    }

    private static String nextNameToken(StringTokenizer tokenizer) {
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            boolean found = false;
            for (int i = 0; i < token.length(); i++) {
                char ch = token.charAt(i);
                if (Character.isLetter(ch)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                return token;
            }
        }
        return null;
    }

}
