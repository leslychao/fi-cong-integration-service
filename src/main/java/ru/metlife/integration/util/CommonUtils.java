package ru.metlife.integration.util;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonUtils {

  public static Logger LOGGER = LoggerFactory.getLogger(CommonUtils.class);

  private static final Pattern VALID_EMAIL_PATTERN = Pattern
      .compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
  private static final Pattern EMAIL_CC_PATTERN = Pattern.compile("(.*<)?(\\S+?)>?\\s*$");

  public static String getStringCellValue(Map<String, Object> sheetAsMap,
      String fieldName,
      String... defaultValue) {
    return getStringCellValue(sheetAsMap.get(fieldName), defaultValue);
  }

  public static String getStringCellValue(Object cellValue, String... defaultValue) {
    return ofNullable(cellValue)
        .map(String::valueOf)
        .map(String::trim)
        .orElseGet(() -> {
          if (isNull(defaultValue)) {
            return null;
          }
          return defaultValue.length == 1 ? defaultValue[0] : EMPTY;
        });
  }

  public static void createDirectoryIfNotExists(File dir) throws IOException {
    createDirectoryIfNotExists(dir.getAbsolutePath());
  }

  public static void createDirectoryIfNotExists(String pathToDir) throws IOException {
    File dir = new File(pathToDir);
    if (!dir.isDirectory()) {
      LOGGER.warn("Creating directory {}", dir);
      if (dir.mkdirs()) {
        LOGGER.warn("Directory successfully created {}", dir);
      } else {
        throw new IOException("Failed to create directory " + dir);
      }
    }
    LOGGER.warn("Directory already exists {}", dir);
  }

  public static boolean deleteIfExists(File file) {
    return file.exists() && !file.delete();
  }

  public static boolean renameTo(File from, File to, boolean deleteDestination) {
    if (deleteDestination) {
      deleteIfExists(to);
    }
    return from.renameTo(to);
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

  public static int getOrderIndependentHash(Object... objects) {
    if (isNull(objects)) {
      return 0;
    }
    return Arrays.stream(objects)
        .map(String::valueOf)
        .mapToInt(Object::hashCode)
        .reduce(0, (left, right) -> left ^ right);
  }
}
