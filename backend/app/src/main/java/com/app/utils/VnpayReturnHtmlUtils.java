package com.app.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.TreeMap;

import org.springframework.util.StringUtils;

import com.common.entities.PaymentEntity;

import jakarta.servlet.http.HttpServletResponse;

public final class VnpayReturnHtmlUtils {

    private VnpayReturnHtmlUtils() {
    }

    public static void write(
        HttpServletResponse response,
        boolean success,
        PaymentEntity payment,
        TreeMap<String, String> vnpParams,
        String homeUrl
    ) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(buildHtml(success, payment, vnpParams, homeUrl));
    }

    private static String buildHtml(
        boolean success,
        PaymentEntity payment,
        TreeMap<String, String> vnpParams,
        String homeUrl
    ) {
        String responseCode = param(vnpParams, "vnp_ResponseCode");
        String title = success ? "Thanh toán thành công" : "Thanh toán chưa hoàn tất";
        String subtitle = success
            ? "Giao dịch VNPAY đã được xác nhận. Bạn có thể đóng trang này và quay lại ứng dụng."
            : describeFailure(responseCode, param(vnpParams, "vnp_Message"));

        Integer paymentId = payment != null ? payment.getId() : parsePaymentId(param(vnpParams, "vnp_TxnRef"));
        String orderNumber = payment != null && payment.getOrder() != null
            ? payment.getOrder().getOrderNumber()
            : null;
        String amount = formatVndAmount(param(vnpParams, "vnp_Amount"));
        String bank = param(vnpParams, "vnp_BankCode");
        String txnNo = param(vnpParams, "vnp_TransactionNo");
        String payDate = formatPayDate(param(vnpParams, "vnp_PayDate"));
        String safeHome = StringUtils.hasText(homeUrl) ? escapeHtml(homeUrl.trim()) : "#";

        String statusClass = success ? "success" : "failure";
        String icon = success ? "&#10003;" : "&#10007;";

        StringBuilder rows = new StringBuilder();
        appendRow(rows, "Mã thanh toán", paymentId != null ? String.valueOf(paymentId) : "—");
        if (StringUtils.hasText(orderNumber)) {
            appendRow(rows, "Mã đơn hàng", orderNumber);
        }
        if (StringUtils.hasText(amount)) {
            appendRow(rows, "Số tiền", amount);
        }
        if (StringUtils.hasText(bank)) {
            appendRow(rows, "Ngân hàng", bank);
        }
        if (StringUtils.hasText(txnNo)) {
            appendRow(rows, "Mã giao dịch VNPAY", txnNo);
        }
        if (StringUtils.hasText(payDate)) {
            appendRow(rows, "Thời gian thanh toán", payDate);
        }
        if (StringUtils.hasText(responseCode)) {
            appendRow(
                rows,
                "Mã phản hồi",
                responseCode + " — " + describeResponseCode(responseCode)
            );
        }

        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>%s — Restaurant</title>
              <style>
                * { box-sizing: border-box; }
                body {
                  margin: 0;
                  min-height: 100vh;
                  font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
                  background: linear-gradient(160deg, #fafaf9 0%%, #f5f5f4 45%%, #e7e5e4 100%%);
                  color: #1c1917;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  padding: 24px;
                }
                .card {
                  width: 100%%;
                  max-width: 440px;
                  background: #fff;
                  border: 1px solid #e7e5e4;
                  border-radius: 20px;
                  box-shadow: 0 20px 50px rgba(28, 25, 23, 0.08);
                  padding: 32px 28px;
                  text-align: center;
                }
                .icon {
                  width: 64px;
                  height: 64px;
                  margin: 0 auto 16px;
                  border-radius: 50%%;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  font-size: 32px;
                  font-weight: 700;
                  line-height: 1;
                }
                .icon.success { background: #ecfdf5; color: #047857; border: 2px solid #a7f3d0; }
                .icon.failure { background: #fef2f2; color: #b91c1c; border: 2px solid #fecaca; }
                h1 {
                  margin: 0 0 8px;
                  font-size: 1.35rem;
                  font-weight: 700;
                  color: #292524;
                }
                .subtitle {
                  margin: 0 0 24px;
                  font-size: 0.9rem;
                  line-height: 1.5;
                  color: #78716c;
                }
                dl {
                  margin: 0 0 24px;
                  text-align: left;
                  border: 1px solid #f5f5f4;
                  border-radius: 12px;
                  overflow: hidden;
                }
                .row {
                  display: flex;
                  justify-content: space-between;
                  gap: 12px;
                  padding: 10px 14px;
                  font-size: 0.875rem;
                  border-top: 1px solid #f5f5f4;
                }
                .row:first-child { border-top: none; }
                .row:nth-child(even) { background: #fafaf9; }
                dt { margin: 0; color: #78716c; font-weight: 500; flex-shrink: 0; }
                dd { margin: 0; color: #1c1917; font-weight: 600; text-align: right; word-break: break-word; }
                .btn {
                  display: inline-block;
                  width: 100%%;
                  padding: 12px 20px;
                  border-radius: 12px;
                  font-size: 0.95rem;
                  font-weight: 600;
                  text-decoration: none;
                  color: #fff;
                  background: #44403c;
                  transition: background 0.15s ease;
                }
                .btn:hover { background: #292524; }
                .hint {
                  margin-top: 16px;
                  font-size: 0.75rem;
                  color: #a8a29e;
                }
              </style>
            </head>
            <body>
              <main class="card">
                <div class="icon %s" aria-hidden="true">%s</div>
                <h1>%s</h1>
                <p class="subtitle">%s</p>
                <dl>%s</dl>
                <a class="btn" href="%s">Quay lại ứng dụng</a>
                <p class="hint">Cửa sổ thanh toán VNPAY có thể đóng an toàn.</p>
              </main>
            </body>
            </html>
            """.formatted(
            escapeHtml(title),
            statusClass,
            icon,
            escapeHtml(title),
            escapeHtml(subtitle),
            rows.toString(),
            safeHome
        );
    }

    private static void appendRow(StringBuilder rows, String label, String value) {
        rows.append("<div class=\"row\"><dt>")
            .append(escapeHtml(label))
            .append("</dt><dd>")
            .append(escapeHtml(value))
            .append("</dd></div>");
    }

    private static String param(TreeMap<String, String> params, String key) {
        if (params == null || key == null) {
            return "";
        }
        String v = params.get(key);
        return v != null ? v.trim() : "";
    }

    private static Integer parsePaymentId(String txnRef) {
        if (!StringUtils.hasText(txnRef)) {
            return null;
        }
        try {
            return Integer.parseInt(txnRef.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatVndAmount(String wireAmount) {
        if (!StringUtils.hasText(wireAmount)) {
            return "";
        }
        try {
            BigDecimal vnd = new BigDecimal(wireAmount.trim()).movePointLeft(2);
            NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
            nf.setMaximumFractionDigits(0);
            nf.setMinimumFractionDigits(0);
            return nf.format(vnd.setScale(0, RoundingMode.HALF_UP)) + " \u20ab";
        } catch (Exception e) {
            return "";
        }
    }

    private static String formatPayDate(String raw) {
        if (!StringUtils.hasText(raw) || raw.length() < 14) {
            return raw != null ? raw : "";
        }
        String d = raw.trim();
        return String.format(
            "%s:%s:%s %s/%s/%s",
            d.substring(8, 10),
            d.substring(10, 12),
            d.substring(12, 14),
            d.substring(6, 8),
            d.substring(4, 6),
            d.substring(0, 4)
        );
    }

    private static String describeResponseCode(String code) {
        if (!StringUtils.hasText(code)) {
            return "Không xác định";
        }
        return switch (code) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Giao dịch bị nghi ngờ";
            case "09" -> "Thẻ chưa đăng ký InternetBanking";
            case "10" -> "Xác thực thông tin thẻ sai quá số lần";
            case "11" -> "Hết hạn chờ thanh toán";
            case "12" -> "Thẻ bị khóa";
            case "13" -> "Sai mật khẩu OTP";
            case "24" -> "Khách hàng hủy giao dịch";
            case "51" -> "Tài khoản không đủ số dư";
            case "65" -> "Vượt hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng bảo trì";
            case "79" -> "Nhập sai mật khẩu quá số lần";
            case "99" -> "Lỗi khác";
            default -> "Giao dịch không thành công";
        };
    }

    private static String describeFailure(String responseCode, String vnpMessage) {
        if (StringUtils.hasText(vnpMessage)) {
            return vnpMessage.trim();
        }
        if ("24".equals(responseCode)) {
            return "Bạn đã hủy thanh toán trên cổng VNPAY. Đơn vẫn có thể thanh toán lại từ ứng dụng.";
        }
        if (StringUtils.hasText(responseCode)) {
            return "Mã lỗi " + responseCode + ": " + describeResponseCode(responseCode) + ".";
        }
        return "Không xác nhận được kết quả. Vui lòng kiểm tra lại trong ứng dụng.";
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
