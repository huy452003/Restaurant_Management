package com.app.controllers;

import java.io.IOException;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.app.services.VnpayPaymentService;
import com.common.models.Response;
import com.common.models.payment.PaymentCreateRequestModel;
import com.common.models.payment.VnpayCheckoutResponse;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/payments/vnpay")
public class VnpayPaymentController {

    @Autowired
    private VnpayPaymentService vnpayPaymentService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private LoggingService log;

    private LogContext logContext(String methodName) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .build();
    }

    // --- POST /payments/vnpay/init — Có JWT; tạo Payment VNPAY + trả URL sang sandbox để FE mở redirect. ---

    @PostMapping("/init")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ADMIN')")
    public ResponseEntity<Response<VnpayCheckoutResponse>> init(
        Locale locale,
        HttpServletRequest httpRequest,
        @RequestBody @Valid PaymentCreateRequestModel payment
    ) {
        // Delegating: gọi service khởi tạo, trả paymentUrl + payment snapshot cho FE mở cổng VNPAY.
        LogContext ctx = logContext("init");
        log.logInfo("VNPAY init endpoint called", ctx);
        VnpayCheckoutResponse data = vnpayPaymentService.initiateVnpay(payment, httpRequest);
        Response<VnpayCheckoutResponse> response = new Response<>(
            201,
            messageSource.getMessage("response.message.vnpayInitSuccess", null, locale),
            "vnpayCheckoutResponse",
            null,
            data
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // --- GET /payments/vnpay/return — Public; VNPAY redirect browser về sau khi khách thanh toán (ReturnUrl). ---

    @GetMapping("/return")
    public void vnpayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Không trả JSON: có thể redirect trình duyệt hoặc ghi HTML (xử lý thật trong service.return).
        LogContext ctx = logContext("vnpayReturn");
        log.logInfo("VNPAY return handler", ctx);
        vnpayPaymentService.handleReturn(request, response);
    }

    // --- GET/POST /payments/vnpay/ipn — Public; callback server-to-server (IPN), phải đăng ký URL trên VNPAY và truy cập được từ internet. ---

    @RequestMapping(value = "/ipn", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<String> vnpayIpn(HttpServletRequest request) {
        // Trả body JSON RspCode/Message; VNPAY gọi trực tiếp backend (cần URL công khai HTTPS khi test thật).
        LogContext ctx = logContext("vnpayIpn");
        log.logInfo("VNPAY IPN handler", ctx);
        return vnpayPaymentService.handleIpn(request);
    }
}
