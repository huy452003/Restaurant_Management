package com.app.services;

import java.io.IOException;

import org.springframework.http.ResponseEntity;

import com.common.models.payment.PaymentCreateRequestModel;
import com.common.models.payment.VnpayCheckoutResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface VnpayPaymentService {

    VnpayCheckoutResponse initiateVnpay(PaymentCreateRequestModel request, HttpServletRequest httpRequest);

    ResponseEntity<String> handleIpn(HttpServletRequest request);

    void handleReturn(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
