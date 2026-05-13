package com.app.services.imp;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
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
import com.common.entities.OrderEntity;
import com.common.entities.PaymentEntity;
import com.common.enums.PaymentMethod;
import com.common.enums.PaymentStatus;
import com.common.models.payment.PaymentCreateRequestModel;
import com.common.models.payment.PaymentModel;
import com.common.models.payment.VnpayCheckoutResponse;
import com.common.models.payment.VnpayInitRequestModel;
import com.common.repositories.OrderRepository;
import com.common.repositories.PaymentRepository;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class VnpayPaymentServiceImpl implements VnpayPaymentService {
    @Autowired
    private VnpayProperties vnpayProperties;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private LoggingService log;
    @Autowired
    private ModelMapper modelMapper;

    private static final String VNP_RSP_SUCCESS = "00";
    private static final String VNP_VERSION = "2.1.0";
    private static final String VNP_COMMAND_PAY = "pay";
    private static final String VNP_CURR_VND = "VND";
    private static final String VNP_LOCALE_VN = "vn";
    private static final String VNP_ORDER_TYPE_OTHER = "other";
    private static final String ZONE_ID_VN = "Asia/Ho_Chi_Minh";
    private static final DateTimeFormatter VNP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private LogContext getLogContext(String methodName, List<Integer> paymentIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(paymentIds)
            .build();
    }

    @Override
    @Transactional
    public VnpayCheckoutResponse initiateVnpay(VnpayInitRequestModel request, HttpServletRequest httpRequest) {
        // Luồng: tra đơn theo orderNumber → amount còn lại → cashier = user đăng nhập → create VNPAY → ký URL.
        LogContext logContext = getLogContext("initiateVnpay", Collections.emptyList());

        requireVnpayConfigured(logContext);

        OrderEntity order = resolveOrderFromOrderNumber(request.getOrderNumber(), logContext);

        PaymentCreateRequestModel createPayload = PaymentCreateRequestModel.builder()
            .orderNumber(order.getOrderNumber())
            .paymentMethod(PaymentMethod.VNPAY)
            .build();

        PaymentModel created = paymentService.create(createPayload);

        PaymentEntity payment = resolvePayment(created.getId(), logContext);

        ZonedDateTime now = ZonedDateTime.now(java.time.ZoneId.of(ZONE_ID_VN));
        ZonedDateTime expire = now.plusMinutes(15);

        TreeMap<String, String> vnp = new TreeMap<>();
        vnp.put("vnp_Version", VNP_VERSION);
        vnp.put("vnp_Command", VNP_COMMAND_PAY);
        vnp.put("vnp_TmnCode", vnpayProperties.getTmnCode());
        vnp.put("vnp_Locale", VNP_LOCALE_VN);
        vnp.put("vnp_CurrCode", VNP_CURR_VND);
        vnp.put("vnp_TxnRef", String.valueOf(payment.getId()));
        vnp.put("vnp_OrderInfo", truncate(buildOrderInfoLabel(payment), 255));
        vnp.put("vnp_OrderType", VNP_ORDER_TYPE_OTHER);
        vnp.put("vnp_Amount", toVnpAmountVnd(payment.getAmount()));
        vnp.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl());
        vnp.put("vnp_IpAddr", resolveClientIp(httpRequest));
        vnp.put("vnp_CreateDate", now.format(VNP_DATE_FORMAT));
        vnp.put("vnp_ExpireDate", expire.format(VNP_DATE_FORMAT));

        String paymentUrl = VnpaySignatureUtils.buildPaymentRedirectUrl(
            vnpayProperties.getPaymentUrl(),
            vnp,
            vnpayProperties.getHashSecret()
        );

        log.logInfo("VNPAY init ok, paymentId=" + payment.getId(), logContext);
        return new VnpayCheckoutResponse(paymentUrl, toPaymentModel(payment));
    }

    @Override
    @Transactional
    public ResponseEntity<String> handleIpn(HttpServletRequest request) {
        // Luồng: verify hash → tra payment theo vnp_TxnRef → khớp vnp_Amount → theo ResponseCode gọi complete hoặc failed.
        LogContext logContext = getLogContext("handleIpn", Collections.emptyList());

        TreeMap<String, String> params = VnpaySignatureUtils.toSortedVnpParams(request);
        log.logInfo("VNPAY IPN received, keys=" + params.keySet(), logContext);

        if (!VnpaySignatureUtils.verifySecureHash(params, vnpayProperties.getHashSecret())) {
            log.logWarn("VNPAY IPN invalid signature", logContext);
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

        // quăng ra null để trả về bodyJson error không throw exception
        PaymentEntity payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.logWarn("VNPAY IPN payment not found id=" + paymentId, logContext);
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
            log.logWarn("VNPAY IPN amount mismatch paymentId=" + paymentId, logContext);
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
            log.logInfo("VNPAY IPN completed paymentId=" + paymentId, logContext);
            return bodyJson("00", "Confirm successfully");
        }

        paymentService.markFailedFromGateway(paymentId);
        log.logInfo("VNPAY IPN marked FAILED paymentId=" + paymentId + " rspCode=" + rspCode, logContext);
        return bodyJson("00", "Confirm successfully");
    }

    @Override
    @Transactional
    public void handleReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Luồng: verify hash → đồng bộ trạng thái (dự phòng IPN) → redirect frontend hoặc hiển thị HTML kết quả.
        LogContext logContext = getLogContext("handleReturn", Collections.emptyList());

        TreeMap<String, String> params = VnpaySignatureUtils.toSortedVnpParams(request);

        if (!VnpaySignatureUtils.verifySecureHash(params, vnpayProperties.getHashSecret())) {
            log.logWarn("VNPAY return invalid signature", logContext);
            writeSimpleHtml(response, false, null, "Invalid signature");
            return;
        }

        String txnRef = params.get("vnp_TxnRef");
        String rsp = params.get("vnp_ResponseCode");
        if (!StringUtils.hasText(txnRef)) {
            writeSimpleHtml(response, false, null, "Missing transaction reference");
            return;
        }

        int paymentId;
        try {
            paymentId = Integer.parseInt(txnRef.trim());
        } catch (NumberFormatException e) {
            writeSimpleHtml(response, false, null, "Invalid transaction reference");
            return;
        }

        PaymentEntity payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null || payment.getPaymentMethod() != PaymentMethod.VNPAY) {
            writeSimpleHtml(response, false, paymentId, "Payment not found");
            return;
        }

        String vnpAmountStr = params.get("vnp_Amount");
        if (StringUtils.hasText(vnpAmountStr)) {
            try {
                long wire = Long.parseLong(vnpAmountStr.trim());
                long ex = payment.getAmount().setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
                if (wire != ex) {
                    writeSimpleHtml(response, false, paymentId, "Amount mismatch");
                    return;
                }
            } catch (NumberFormatException ignored) {
                writeSimpleHtml(response, false, paymentId, "Invalid amount");
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
            log.logError("VNPAY return sync failed paymentId=" + paymentId, e, logContext);
        }

        boolean ok = VNP_RSP_SUCCESS.equals(rsp);
        String frontendBase = vnpayProperties.getFrontendRedirectBase();
        if (StringUtils.hasText(frontendBase)) {
            String sep = frontendBase.contains("?") ? "&" : "?";
            String target = frontendBase + sep + "paymentId=" + paymentId + "&vnp_ResponseCode=" + (rsp != null ? rsp : "");
            response.sendRedirect(target);
            return;
        }

        writeSimpleHtml(response, ok, paymentId, ok ? "Payment successful" : ("Failed, error: " + (rsp != null ? rsp : "unknown")));
    }

    // private method

    private PaymentModel toPaymentModel(PaymentEntity entity) {
        PaymentModel paymentModel = modelMapper.map(entity, PaymentModel.class);
        if (entity.getOrder() != null) {
            paymentModel.setOrderNumber(entity.getOrder().getOrderNumber());
        }
        if (entity.getCashier() != null) {
            paymentModel.setCashierFullname(entity.getCashier().getFullname());
        }
        return paymentModel;
    }

    private OrderEntity resolveOrderFromOrderNumber(String orderNumber, LogContext logContext) {
        return orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Order not found with orderNumber: " + orderNumber,
                Collections.singletonList(orderNumber),
                "OrderModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
    }

    private PaymentEntity resolvePayment(Integer paymentId, LogContext logContext) {
        return paymentRepository.findById(paymentId).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Payment not found with id: " + paymentId,
                Collections.singletonList(paymentId),
                "PaymentModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
    }

    private static String buildOrderInfoLabel(PaymentEntity entity) {
        // Nội dung hiển thị trên trang VNPAY (giới hạn 255 ký tự).
        Integer orderId = entity.getOrderId() != null ? entity.getOrderId() : (entity.getOrder() != null ? entity.getOrder().getId() : null);
        if (orderId != null) {
            return "Thanh toan don hang #" + orderId + ", ma giao dich " + entity.getId();
        }
        return "Thanh toan ma giao dich " + entity.getId();
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

    private void requireVnpayConfigured(LogContext logContext) {
        // Fail nhanh nếu thiếu cấu hình cốt lõi trước khi build URL.
        if (
            !StringUtils.hasText(vnpayProperties.getTmnCode()) || 
            !StringUtils.hasText(vnpayProperties.getHashSecret())
        ) {
            log.logWarn("VNPAY tmn-code or hash-secret is not configured", logContext);
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                """
                VNPAY is not configured: check vnpay.tmn-code
                , vnpay.hash-secret, (application.properties or environment variables)
                """,
                Collections.emptyList(),
                "VnpayProperties"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        if (!StringUtils.hasText(vnpayProperties.getReturnUrl())) {
            log.logWarn("Missing vnpay.return-url", logContext);
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Missing vnpay.return-url — must match the URL registered on Merchant VNPAY (ReturnUrl).",
                Collections.emptyList(),
                "VnpayProperties"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
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
}
