package com.common.configurations;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.LocaleResolver;

@Configuration
public class LocaleResolverConfig{

    /** Thống nhất với classpath: messages.properties, messages_vi.properties */
    private static final List<Locale> SUPPORTED_UI_LOCALES = Arrays.asList(
        Locale.ENGLISH,
        Locale.forLanguageTag("vi")
    );

    @Bean
    @Primary
    public LocaleResolver localeResolver() {
        TrimEmptyAcceptHeaderLocaleResolver localeResolver = new TrimEmptyAcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        localeResolver.setSupportedLocales(SUPPORTED_UI_LOCALES);
        return localeResolver;
    }

}
