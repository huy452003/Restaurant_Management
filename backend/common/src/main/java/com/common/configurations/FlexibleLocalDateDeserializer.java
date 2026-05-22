package com.common.configurations;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/** Hỗ trợ dd-MM-yyyy (API) và yyyy-MM-dd (Redis / ISO). */
public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

  private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

  @Override
  public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String text = p.getValueAsString();
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(text, DD_MM_YYYY);
    } catch (DateTimeParseException ignored) {
      // fall through
    }
    try {
      return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
    } catch (DateTimeParseException e) {
      throw ctxt.weirdStringException(text, LocalDate.class, "Expected dd-MM-yyyy or yyyy-MM-dd");
    }
  }
}
