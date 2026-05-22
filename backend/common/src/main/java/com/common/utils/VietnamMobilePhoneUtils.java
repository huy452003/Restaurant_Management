package com.common.utils;

import java.util.regex.Pattern;

public final class VietnamMobilePhoneUtils {

  /** 11 chữ số: 84 + đầu số di động VN (3, 5, 7, 8, 9) + 8 số. */
  private static final Pattern VIETNAM_MOBILE_11 =
      Pattern.compile("^84[35789][0-9]{8}$");

  private VietnamMobilePhoneUtils() {}

  public static String digitsOnly(String phone) {
    if (phone == null) {
      return "";
    }
    return phone.replaceAll("\\D", "");
  }

  /** Chuẩn hóa lưu/API: 11 số dạng 84xxxxxxxxx (từ 0xxxxxxxxx hoặc 84...). */
  public static String normalize(String phone) {
    String digits = digitsOnly(phone);
    if (digits.startsWith("84") && digits.length() >= 11) {
      return digits.substring(0, 11);
    }
    if (digits.startsWith("84") && digits.length() > 2) {
      return "84" + digits.substring(2, Math.min(digits.length(), 11));
    }
    if (digits.length() == 10 && digits.startsWith("0")) {
      return "84" + digits.substring(1);
    }
    if (digits.length() == 9) {
      return "84" + digits;
    }
    return digits;
  }

  public static boolean isValid(String phone) {
    if (phone == null || phone.isBlank()) {
      return true;
    }
    return VIETNAM_MOBILE_11.matcher(normalize(phone)).matches();
  }
}
