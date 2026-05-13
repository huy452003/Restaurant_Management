package com.common.models.payment;

public record VnpayCheckoutResponse(String paymentUrl, PaymentModel payment) {}