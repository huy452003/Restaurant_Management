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
import com.common.models.payment.VnpayInitRequestModel;
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

    private LogContext getLogContext(String methodName) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .build();
    }

    @PostMapping("/init")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ADMIN','CUSTOMER')")
    public ResponseEntity<Response<VnpayCheckoutResponse>> init(
        Locale locale,
        HttpServletRequest httpRequest,
        @RequestBody @Valid VnpayInitRequestModel body
    ) {
        LogContext logContext = getLogContext("init");
        log.logInfo("VNPAY init endpoint called", logContext);
        VnpayCheckoutResponse data = vnpayPaymentService.initiateVnpay(body, httpRequest);
        Response<VnpayCheckoutResponse> response = new Response<>(
            201,
            messageSource.getMessage("response.message.vnpayInitSuccess", null, locale),
            "vnpayCheckoutResponse",
            null,
            data
        );
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/return")
    public void vnpayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LogContext logContext = getLogContext("vnpayReturn");
        log.logInfo("VNPAY return handler", logContext);
        vnpayPaymentService.handleReturn(request, response);
    }

    @RequestMapping(value = "/ipn", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<String> vnpayIpn(HttpServletRequest request) {
        LogContext logContext = getLogContext("vnpayIpn");
        log.logInfo("VNPAY IPN handler", logContext);
        return vnpayPaymentService.handleIpn(request);
    }
}
