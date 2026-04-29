package com.iris.back.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeFormatters {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private DateTimeFormatters() {
  }

  public static String formatDateTime(LocalDateTime value) {
    return value == null ? null : value.format(DATE_TIME_FORMATTER);
  }
}
