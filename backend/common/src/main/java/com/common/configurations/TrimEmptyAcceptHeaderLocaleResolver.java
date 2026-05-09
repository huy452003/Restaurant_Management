package com.common.configurations;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

public class TrimEmptyAcceptHeaderLocaleResolver extends AcceptHeaderLocaleResolver {

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String raw = request.getHeader("Accept-Language");
        String trimmed = raw != null ? raw.trim() : "";
        if (!StringUtils.hasText(trimmed)) {
            Locale def = getDefaultLocale();
            return def != null ? def : Locale.ENGLISH;
        }
        return super.resolveLocale(request);
    }
}
