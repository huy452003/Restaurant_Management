package com.app.services.imp;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;
import java.util.TreeMap;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.app.config.VnpayProperties;
import com.app.services.PaymentService;
import com.app.services.VnpayPaymentService;
import com.app.utils.VnpaySignatureUtils;
import com.common.entities.PaymentEntity;
import com.common.enums.PaymentMethod;
import com.common.enums.PaymentStatus;
import com.common.models.payment.PaymentCreateRequestModel;
import com.common.models.payment.PaymentModel;
import com.common.models.payment.VnpayCheckoutResponse;
import com.common.repositories.PaymentRepository;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class VnpayPaymentServiceImpl implements VnpayPaymentService {

    private static final String VNP_RSP_SUCCESS = "00";
    private static final String VNP_VERSION = "2.1.0";
    private static final String VNP_COMMAND_PAY = "pay";
    private static final String VNP_CURR_VND = "VND";
    private static final String VNP_LOCALE_VN = "vn";
    private static final String VNP_ORDER_TYPE_OTHER = "other";
    private static final String ZONE_ID_VN = "Asia/Ho_Chi_Minh";
    private static final DateTimeFormatter VNP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private VnpayProperties vnpayProperties;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private LoggingService log;
    @Autowired
    private ModelMapper modelMapper;

    private LogContext logContext(String method) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(method)
            .build();
    }

    // --- Khởi tạo thanh toán: tạo bản ghi PENDING + URL redirect VNPAY. ---

    @Override
    @Transactional
    public VnpayCheckoutResponse initiateVnpay(PaymentCreateRequestModel request, HttpServletRequest httpRequest) {
        // Luồng: validate VNPAY → create payment PENDING → transactionId = id → ghép tham số vnp_* → ký SHA512 → trả URL.
        LogContext ctx = logContext("initiateVnpay");
        requireVnpayConfigured(ctx);

        if (request.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new ValidationExceptionHandle(
                "Chỉ dùng paymentMethod VNPAY cho endpoint /payments/vnpay/init",
                Collections.emptyList(),
                "PaymentModel"
            );
        }

        PaymentModel created = paymentService.create(request);

        PaymentEntity saved = paymentRepository.findById(created.getId()).orElseThrow(() -> new NotFoundExceptionHandle(
            "Payment not found after create: " + created.getId(),
            Collections.singletonList(created.getId()),
            "PaymentModel"
        ));
        saved.setTransactionId(String.valueOf(saved.getId()));
        paymentRepository.saveAndFlush(saved);

        ZonedDateTime now = ZonedDateTime.now(java.time.ZoneId.of(ZONE_ID_VN));
        ZonedDateTime expire = now.plusMinutes(15);

        TreeMap<String, String> vnp = new TreeMap<>();
        vnp.put("vnp_Version", VNP_VERSION);
        vnp.put("vnp_Command", VNP_COMMAND_PAY);
        vnp.put("vnp_TmnCode", vnpayProperties.getTmnCode());
        vnp.put("vnp_Locale", VNP_LOCALE_VN);
        vnp.put("vnp_CurrCode", VNP_CURR_VND);
        vnp.put("vnp_TxnRef", String.valueOf(saved.getId()));
        vnp.put("vnp_OrderInfo", truncate(buildOrderInfoLabel(saved), 255));
        vnp.put("vnp_OrderType", VNP_ORDER_TYPE_OTHER);
        vnp.put("vnp_Amount", toVnpAmountVnd(saved.getAmount()));
        vnp.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl());
        vnp.put("vnp_IpAddr", resolveClientIp(httpRequest));
        vnp.put("vnp_CreateDate", now.format(VNP_DATE_FORMAT));
        vnp.put("vnp_ExpireDate", expire.format(VNP_DATE_FORMAT));

        String paymentUrl = VnpaySignatureUtils.buildPaymentRedirectUrl(
            vnpayProperties.getPaymentUrl(),
            vnp,
            vnpayProperties.getHashSecret()
        );

        PaymentEntity refreshedEntity = paymentRepository.findById(saved.getId())
            .orElseThrow(() -> new NotFoundExceptionHandle(
                "Payment not found after update: " + saved.getId(),
                Collections.singletonList(saved.getId()),
                "PaymentModel"
            ));
        log.logInfo("VNPAY init ok, paymentId=" + saved.getId(), ctx);
        return new VnpayCheckoutResponse(paymentUrl, toPaymentModel(refreshedEntity));
    }

    private PaymentModel toPaymentModel(PaymentEntity pe) {
        // Map entity → API model; set orderId/cashierId từ quan hệ hoặc cột snapshot JPA.
        PaymentModel m = modelMapper.map(pe, PaymentModel.class);
        if (pe.getOrder() != null) {
            m.setOrderId(pe.getOrder().getId());
        } else if (pe.getOrderId() != null) {
            m.setOrderId(pe.getOrderId());
        }
        if (pe.getCashier() != null) {
            m.setCashierId(pe.getCashier().getId());
        } else if (pe.getCashierId() != null) {
            m.setCashierId(pe.getCashierId());
        }
        return m;
    }

    private static String buildOrderInfoLabel(PaymentEntity p) {
        // Nội dung hiển thị trên trang VNPAY (giới hạn 255 ký tự).
        Integer orderId = p.getOrderId() != null ? p.getOrderId() : (p.getOrder() != null ? p.getOrder().getId() : null);
        if (orderId != null) {
            return "Thanh toan don hang #" + orderId + ", ma giao dich " + p.getId();
        }
        return "Thanh toan ma giao dich " + p.getId();
    }

    private static String truncate(String s, int max) {
        // Cắt chuỗi an toàn cho vnp_OrderInfo.
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String toVnpAmountVnd(BigDecimal amount) {
        // Quy đổi VNĐ → số nguyên theo spec (×100) để gửi vnp_Amount.
        long v = amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        return String.valueOf(v);
    }

    private void requireVnpayConfigured(LogContext ctx) {
        // Fail nhanh nếu thiếu cấu hình cốt lõi trước khi build URL.
        if (!StringUtils.hasText(vnpayProperties.getTmnCode())
            || !StringUtils.hasText(vnpayProperties.getHashSecret())) {
            log.logWarn("VNPAY tmn-code / hash-secret chưa cấu hình", ctx);
            throw new ValidationExceptionHandle(
                "VNPAY chưa cấu hình: kiểm tra vnpay.tmn-code, vnpay.hash-secret, vnpay.return-url (application.properties hoặc biến môi trường)",
                Collections.emptyList(),
                "VnpayProperties"
            );
        }
        if (!StringUtils.hasText(vnpayProperties.getReturnUrl())) {
            throw new ValidationExceptionHandle(
                "Thiếu vnpay.return-url — phải trùng URL đăng ký trên Merchant VNPAY (ReturnUrl).",
                Collections.emptyList(),
                "VnpayProperties"
            );
        }
    }

    // --- IPN: VNPAY gọi server-to-server — nguồn xác thực chính để đánh dấu COMPLETED/FAILED. ---

    @Override
    @Transactional
    public ResponseEntity<String> handleIpn(HttpServletRequest request) {
        // Luồng: verify hash → tra payment theo vnp_TxnRef → khớp vnp_Amount → theo ResponseCode gọi complete hoặc failed.
        LogContext ctx = logContext("handleIpn");
        TreeMap<String, String> params = VnpaySignatureUtils.toSortedVnpParams(request);
        log.logInfo("VNPAY IPN received, keys=" + params.keySet(), ctx);

        if (!VnpaySignatureUtils.verifySecureHash(params, vnpayProperties.getHashSecret())) {
            log.logWarn("VNPAY IPN invalid signature", ctx);
            return bodyJson("97", "Invalid signature");
        }

        String txnRef = params.get("vnp_TxnRef");
        String rspCode = params.get("vnp_ResponseCode");
        String vnpAmountStr = params.get("vnp_Amount");

        if (!StringUtils.hasText(txnRef)) {
            return bodyJson("01", "Missing TxnRef");
        }

        int paymentId;
        try {
            paymentId = Integer.parseInt(txnRef.trim());
        } catch (NumberFormatException e) {
            return bodyJson("01", "Invalid TxnRef");
        }

        PaymentEntity payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.logWarn("VNPAY IPN payment not found id=" + paymentId, ctx);
            return bodyJson("01", "Order not found");
        }

        if (payment.getPaymentMethod() != PaymentMethod.VNPAY) {
            return bodyJson("01", "Not VNPAY payment");
        }

        if (!StringUtils.hasText(vnpAmountStr)) {
            return bodyJson("04", "Missing amount");
        }
        long wireAmount;
        try {
            wireAmount = Long.parseLong(vnpAmountStr.trim());
        } catch (NumberFormatException e) {
            return bodyJson("04", "Invalid amount");
        }
        long expectedWire = payment.getAmount().setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        if (wireAmount != expectedWire) {
            log.logWarn("VNPAY IPN amount mismatch paymentId=" + paymentId, ctx);
            return bodyJson("04", "Invalid amount");
        }

        if (Objects.equals(payment.getPaymentStatus(), PaymentStatus.COMPLETED)) {
            return bodyJson("00", "Confirm successfully");
        }

        if (Objects.equals(payment.getPaymentStatus(), PaymentStatus.FAILED)) {
            return bodyJson("00", "Already failed");
        }

        if (!Objects.equals(payment.getPaymentStatus(), PaymentStatus.PENDING)) {
            return bodyJson("02", "Invalid order status");
        }

        if (VNP_RSP_SUCCESS.equals(rspCode)) {
            paymentService.complete(paymentId);
            log.logInfo("VNPAY IPN completed paymentId=" + paymentId, ctx);
            return bodyJson("00", "Confirm successfully");
        }

        paymentService.markFailedFromGateway(paymentId);
        log.logInfo("VNPAY IPN marked FAILED paymentId=" + paymentId + " rspCode=" + rspCode, ctx);
        return bodyJson("00", "Confirm successfully");
    }

    private static ResponseEntity<String> bodyJson(String rspCode, String message) {
        // Phản hồi IPN chuẩn JSON (RspCode/Message) để VNPAY dừng retry khi hợp lệ.
        String json = String.format("{\"RspCode\":\"%s\",\"Message\":\"%s\"}", rspCode, escapeJson(message));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }

    private static String escapeJson(String s) {
        // Escape tối thiểu để JSON IPN không vỡ chuỗi Message.
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // --- Return URL: trình duyệt quay lại — xác thực hash, đồng bộ trạng thái dự phòng nếu IPN chưa tới, redirect FE hoặc HTML tĩnh. ---

    @Override
    @Transactional
    public void handleReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Luồng: verify hash → đồng bộ trạng thái (dự phòng IPN) → redirect frontend hoặc hiển thị HTML kết quả.
        LogContext ctx = logContext("handleReturn");
        TreeMap<String, String> params = VnpaySignatureUtils.toSortedVnpParams(request);

        if (!VnpaySignatureUtils.verifySecureHash(params, vnpayProperties.getHashSecret())) {
            log.logWarn("VNPAY return invalid signature", ctx);
            writeSimpleHtml(response, false, null, "Chu ky khong hop le");
            return;
        }

        String txnRef = params.get("vnp_TxnRef");
        String rsp = params.get("vnp_ResponseCode");
        if (!StringUtils.hasText(txnRef)) {
            writeSimpleHtml(response, false, null, "Thieu ma giao dich");
            return;
        }

        int paymentId;
        try {
            paymentId = Integer.parseInt(txnRef.trim());
        } catch (NumberFormatException e) {
            writeSimpleHtml(response, false, null, "Ma giao dich khong hop le");
            return;
        }

        PaymentEntity payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null || payment.getPaymentMethod() != PaymentMethod.VNPAY) {
            writeSimpleHtml(response, false, paymentId, "Khong tim thay giao dich");
            return;
        }

        String vnpAmountStr = params.get("vnp_Amount");
        if (StringUtils.hasText(vnpAmountStr)) {
            try {
                long wire = Long.parseLong(vnpAmountStr.trim());
                long ex = payment.getAmount().setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
                if (wire != ex) {
                    writeSimpleHtml(response, false, paymentId, "So tien khong khop");
                    return;
                }
            } catch (NumberFormatException ignored) {
                writeSimpleHtml(response, false, paymentId, "So tien khong hop le");
                return;
            }
        }

        try {
            if (VNP_RSP_SUCCESS.equals(rsp)) {
                if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
                    paymentService.complete(paymentId);
                }
            } else if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
                paymentService.markFailedFromGateway(paymentId);
            }
        } catch (Exception e) {
            log.logError("VNPAY return sync failed paymentId=" + paymentId, e, ctx);
        }

        boolean ok = VNP_RSP_SUCCESS.equals(rsp);
        String frontendBase = vnpayProperties.getFrontendRedirectBase();
        if (StringUtils.hasText(frontendBase)) {
            String sep = frontendBase.contains("?") ? "&" : "?";
            String target = frontendBase + sep + "paymentId=" + paymentId + "&vnp_ResponseCode=" + (rsp != null ? rsp : "");
            response.sendRedirect(target);
            return;
        }

        writeSimpleHtml(response, ok, paymentId, ok ? "Thanh toan thanh cong" : ("That bai, ma loi: " + (rsp != null ? rsp : "unknown")));
    }

    private void writeSimpleHtml(HttpServletResponse response, boolean success, Integer paymentId, String message)
        throws IOException {
        // Trang tối giản khi chưa cấu hình frontend redirect sau return.
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        String pid = paymentId != null ? String.valueOf(paymentId) : "-";
        String body = String.format(
            "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>VNPAY</title></head><body>"
                + "<h2>%s</h2><p>Ma thanh toan (payment id): %s</p><p>%s</p></body></html>",
            success ? "Thanh cong" : "Thong bao",
            pid,
            message != null ? escapeHtml(message) : ""
        );
        response.getWriter().write(body);
    }

    private static String escapeHtml(String s) {
        // Tránh XSS nhẹ khi hiển thị thông báo HTML tĩnh.
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String resolveClientIp(HttpServletRequest request) {
        // vnp_IpAddr: ưu tiên X-Forwarded-For khi app sau reverse proxy.
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        String a = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(a)) {
            return "127.0.0.1";
        }
        return a;
    }
}
