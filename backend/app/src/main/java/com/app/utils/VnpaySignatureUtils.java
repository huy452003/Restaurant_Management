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
     * <p>
     * Khớp sample Java trên tài liệu VNPAY (pay.html): tên tham số giữ nguyên, <strong>giá trị</strong> phải
     * {@code URLEncoder.encode} (application/x-www-form-urlencoded) trước khi HMAC — không dùng chuỗi thô.
     */
    public static String buildSignData(Map<String, String> params) {
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
            hashData.append(k).append('=').append(encodeVnpValue(v));
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
     * Ghép query string (dùng cho IPN/return echo).
     */
    public static String toUrlQuery(TreeMap<String, String> sortedParams) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : sortedParams.entrySet()) {
            if (b.length() > 0) {
                b.append('&');
            }
            b.append(encodeVnpValue(e.getKey()));
            b.append('=');
            b.append(encodeVnpValue(e.getValue()));
        }
        return b.toString();
    }

    /**
     * Ghép URL redirect — khớp servlet mẫu VNPAY (pay.html): hashData chỉ encode value;
     * query encode cả key/value; {@code vnp_SecureHash} nối thô ở cuối (không encode).
     */
    public static String buildPaymentRedirectUrl(String basePaymentUrl, TreeMap<String, String> vnpParamsNoHash, String hashSecret) {
        if (basePaymentUrl.contains("?")) {
            throw new IllegalArgumentException("vnpay.payment-url must be base URL without query string");
        }
        List<String> fieldNames = new ArrayList<>();
        for (String key : vnpParamsNoHash.keySet()) {
            if (key != null && key.startsWith("vnp_") && StringUtils.hasText(vnpParamsNoHash.get(key))) {
                fieldNames.add(key);
            }
        }
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnpParamsNoHash.get(fieldName);
            if (i > 0) {
                hashData.append('&');
                query.append('&');
            }
            hashData.append(fieldName).append('=').append(encodeVnpValue(fieldValue));
            query.append(encodeVnpValue(fieldName)).append('=').append(encodeVnpValue(fieldValue));
        }

        String signData = hashData.toString();
        String secureHash = hmacSha512Hex(hashSecret, signData);
        String queryUrl = query + "&vnp_SecureHash=" + secureHash;
        return basePaymentUrl + "?" + queryUrl;
    }

    /**
     * Khớp servlet mẫu VNPAY (pay.html): {@link StandardCharsets#US_ASCII}, giữ {@code +} cho khoảng trắng.
     */
    private static String encodeVnpValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
