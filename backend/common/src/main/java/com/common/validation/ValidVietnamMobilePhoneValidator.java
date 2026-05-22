package com.common.validation;

import com.common.utils.VietnamMobilePhoneUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidVietnamMobilePhoneValidator
    implements ConstraintValidator<ValidVietnamMobilePhone, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return VietnamMobilePhoneUtils.isValid(value);
  }
}
