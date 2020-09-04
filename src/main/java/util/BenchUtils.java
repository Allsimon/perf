package util;

import java.util.function.Supplier;

public class BenchUtils {

  public static <T> T throwsIfDifferent(Supplier<T> supplier, T expectedValue) {
    T value = supplier.get();
    if (!value.equals(expectedValue)) {
      throw new RuntimeException("Received " + value + ", expected " + expectedValue);
    }
    return value;
  }

  public static <T> T throwsIfDifferent(T value, T expectedValue) {
    if (!value.equals(expectedValue)) {
      throw new RuntimeException("Received " + value + ", expected " + expectedValue);
    }
    return value;
  }
}
