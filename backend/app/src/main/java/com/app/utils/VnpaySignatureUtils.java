package com.app.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Tiện ích HMAC-SHA512 và chuỗi ký theo tài liệu VNPAY (sort key bắt đầu bằng {@code vnp_}, bỏ hash).
 */
public final class VnpaySignatureUtils {

    private static final String HMAC_SHA512 = "HmacSHA512";

    private VnpaySignatureUtils() {
    }

    /**
     * Gom tham số request thành map một giá trị (dùng cho IPN/Return query).
     */
    public static TreeMap<String, String> toSortedVnpParams(HttpServletRequest request) {
        // Đọc toàn bộ query/form vào TreeMap để ký / verify giống thứ tự VNPAY.
        TreeMap<String, String> sorted = new TreeMap<>();
        for (String name : Collections.list(request.getParameterNames())) {
            String value = request.getParameter(name);
            if (value != null) {
                sorted.put(name, value);
            }
        }
        return sorted;
    }

    /**
     * Tạo chuỗi dữ liệu ký: sort key alphabetically, ghép {@code key=value&} (bỏ {@code vnp_SecureHash},
     * {@code vnp_SecureHashType}), chỉ field tên bắt đầu {@code vnp_}, bỏ giá trị rỗng.
     */
    public static String buildSignData(Map<String, String> params) {
        // Chuỗi ký: lọc vnp_* (trừ hash), sort tên field, nối key=value&...
        List<String> fieldNames = new ArrayList<>();
        for (String key : params.keySet()) {
            if (key != null
                && key.startsWith("vnp_")
                && !"vnp_SecureHash".equals(key)
                && !"vnp_SecureHashType".equals(key)) {
                String value = params.get(key);
                if (StringUtils.hasText(value)) {
                    fieldNames.add(key);
                }
            }
        }
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String k = fieldNames.get(i);
            String v = params.get(k);
            if (i > 0) {
                hashData.append('&');
            }
            hashData.append(k).append('=').append(v);
        }
        return hashData.toString();
    }

    /**
     * HMAC-SHA512 (hex không prefix), so khớp VNPAY thường dùng chữ thường hoặc hoa — nên so sánh không phân biệt hoa thường.
     */
    public static String hmacSha512Hex(String secretKey, String signData) {
        // Mã hóa chuỗi ký bằng HMAC-SHA512, hex thường dùng so khớp vnp_SecureHash (không phân biệt hoa thường).
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA512));
            byte[] raw = mac.doFinal(signData.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA512 failed", e);
        }
    }

    /**
     * Kiểm tra {@code vnp_SecureHash} trên bản đồ tham số đầy đủ (đã có hash từ query).
     */
    public static boolean verifySecureHash(Map<String, String> params, String hashSecret) {
        // So sánh vnp_SecureHash trong request với hash tính lại từ các tham số còn lại.
        String received = params.get("vnp_SecureHash");
        if (!StringUtils.hasText(received)) {
            return false;
        }
        String signData = buildSignData(params);
        String computed = hmacSha512Hex(hashSecret, signData);
        return secureHashEquals(computed, received);
    }

    private static boolean secureHashEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equalsIgnoreCase(b.trim());
    }

    /**
     * Ghép query string UTF-8 (dùng sau khi đã có đủ tham số kể cả {@code vnp_SecureHash}).
     */
    public static String toUrlQuery(TreeMap<String, String> sortedParams) {
        // Encode application/x-www-form-urlencoded cho redirect URL.
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : sortedParams.entrySet()) {
            if (b.length() > 0) {
                b.append('&');
            }
            b.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            b.append('=');
            b.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return b.toString();
    }

    /**
     * Gắn {@code vnp_SecureHash} rồi trả URL redirect đầy đủ tới cổng thanh toán VNPAY.
     */
    public static String buildPaymentRedirectUrl(String basePaymentUrl, TreeMap<String, String> vnpParamsNoHash, String hashSecret) {
        // Thêm vnp_SecureHash rồi ghép URL đầy đủ tới sandbox/production pay.
        if (basePaymentUrl.contains("?")) {
            throw new IllegalArgumentException("vnpay.payment-url must be base URL without query string");
        }
        String signData = buildSignData(vnpParamsNoHash);
        String secureHash = hmacSha512Hex(hashSecret, signData);
        TreeMap<String, String> all = new TreeMap<>(vnpParamsNoHash);
        all.put("vnp_SecureHash", secureHash);
        return basePaymentUrl + "?" + toUrlQuery(all);
    }
}
