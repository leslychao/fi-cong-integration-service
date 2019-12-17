package ru.metlife.integration.util;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtils {

  private static final Pattern VALID_EMAIL_PATTERN = Pattern
      .compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
  private static final Pattern EMAIL_CC_PATTERN = Pattern.compile("(.*<)?(\\S+?)>?\\s*$");

  public static String getStringCellValue(Map<String, String> sheetAsMap,
      String cellName,
      String... defaultValue) {
    return getStringCellValue(sheetAsMap.get(cellName), defaultValue);
  }

  public static String getStringCellValue(String cellValue, String... defaultValue) {
    return ofNullable(cellValue)
        .map(String::trim)
        .orElseGet(() -> {
          if (isNull(defaultValue)) {
            return null;
          }
          return defaultValue.length == 1 ? defaultValue[0] : EMPTY;
        });
  }


  public static <T> Predicate<T> andLogFilteredOutValues(Predicate<T> predicate,
      Consumer<T> action) {
    return value -> {
      if (predicate.test(value)) {
        return true;
      }
      action.accept(value);
      return false;
    };
  }

  public static String formatContactString(String contact) {
    Matcher matcher = EMAIL_CC_PATTERN.matcher(contact);
    if (matcher.find()) {
      return matcher.group(2);
    }
    return EMPTY;
  }

  public static boolean isEmailValid(String email) {
    if (isBlank(email)) {
      return false;
    }
    return VALID_EMAIL_PATTERN.matcher(email).matches();
  }

  public static int getOrderIndependentHash(String... objects) {
    if (isNull(objects)) {
      return 0;
    }
    return Arrays.stream(objects)
        .mapToInt(String::hashCode)
        .reduce(0, (left, right) -> left ^ right);
  }

}
