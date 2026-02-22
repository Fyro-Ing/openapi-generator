package org.openapitools.client.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class DataObjectMapper {
  public static String serializeOffsetDateTime(OffsetDateTime value) {
    return value.toString();
  }

  public static String serializeLocalDateTime(LocalDateTime value) {
    return value.toString();
  }

  public static String serializeLocalDate(LocalDate value) {
    return value.toString();
  }

}