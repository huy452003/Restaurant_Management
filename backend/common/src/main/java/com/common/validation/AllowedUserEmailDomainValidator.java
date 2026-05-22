package com.common.validation;

import com.common.utils.UserEmailDomainUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AllowedUserEmailDomainValidator implements ConstraintValidator<AllowedUserEmailDomain, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return UserEmailDomainUtils.isAllowedDomain(value);
    }
}
