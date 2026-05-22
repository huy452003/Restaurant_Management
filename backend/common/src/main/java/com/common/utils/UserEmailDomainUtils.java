package com.common.utils;

import java.util.Locale;
import java.util.Set;

public final class UserEmailDomainUtils {

    private static final Set<String> ALLOWED_DOMAINS = Set.of(
        "gmail.com",
        "outlook.com",
        "outlook.com.vn"
    );

    private UserEmailDomainUtils() {}

    public static boolean isAllowedDomain(String email) {
        if (email == null || email.isBlank()) {
            return true;
        }
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return false;
        }
        String domain = email.substring(at + 1).trim().toLowerCase(Locale.ROOT);
        return ALLOWED_DOMAINS.contains(domain);
    }
}
