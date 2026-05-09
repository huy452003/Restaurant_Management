package com.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình VNPAY (sandbox hoặc production) — bind từ application.properties / biến môi trường.
 * Không dùng {@code @Validated} ở đây để app vẫn chạy khi chưa cấu hình VNPAY; kiểm tra đủ field tại runtime khi gọi init.
 */
@ConfigurationProperties(prefix = "vnpay")
public class VnpayProperties {

    private String tmnCode = "";
    private String hashSecret = "";
    private String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    /** vnp_ReturnUrl: URL công khai trùng đăng ký trên Merchant Admin. */
    private String returnUrl = "";
    /** Base URL cho IPN — phải khớp URL server VNPAY gọi (thường cùng host với return, path khác). */
    private String ipnUrl = "";
    /** Optional: redirect browser tới frontend sau return (vd http://localhost:5173). */
    private String frontendRedirectBase = "";

    public String getTmnCode() {
        return tmnCode;
    }

    public void setTmnCode(String tmnCode) {
        this.tmnCode = tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public void setHashSecret(String hashSecret) {
        this.hashSecret = hashSecret;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getIpnUrl() {
        return ipnUrl;
    }

    public void setIpnUrl(String ipnUrl) {
        this.ipnUrl = ipnUrl;
    }

    public String getFrontendRedirectBase() {
        return frontendRedirectBase;
    }

    public void setFrontendRedirectBase(String frontendRedirectBase) {
        this.frontendRedirectBase = frontendRedirectBase;
    }
}
