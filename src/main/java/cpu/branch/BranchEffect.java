package cpu.branch;

import static java.lang.String.valueOf;

import java.util.Random;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * This method implements a really simple toUppercase functions: this implementation completely ignores locales.
 *
 * | char | value |
 * |------|-------|
 * | @    | 64    |
 * | A    | 65    |
 * | B    | 66    |
 * | …    | …     |
 * | Z    | 90    |
 * | [    | 91    |
 * | …    | …     |
 * | `    | 96    |
 * | a    | 97    |
 * | b    | 98    |
 * | …    | …     |
 * | z    | 122   |
 */
@Fork(1)
@State(Scope.Benchmark)
@Warmup(iterations = 3, batchSize = 2, time = 2)
@Measurement(iterations = 3, batchSize = 2, time = 2)
public class BranchEffect {

  private static final int CHAR_BEFORE_A = 'a' - 1; // 96
  private static final int CHAR_AFTER_Z = 'z' + 1; // 123
  // Offset to remove in order to go from a lowercase letter to an uppercase letter
  private static final int OFFSET = 'a' - 'A'; // 32

  private String input;

  @Setup
  public void setup() {
    input = new Random(0).ints(32, 126)
        .limit(100_000)
        .mapToObj(i -> (char) i)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
  }

  @Benchmark
  public String branchToUppercase() {
    char[] chars = input.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char currentChar = chars[i];
      if (currentChar > CHAR_BEFORE_A && currentChar < CHAR_AFTER_Z) {
        chars[i] = (char) (currentChar - OFFSET);
      }
    }
    return valueOf(chars);
  }

  @Benchmark
  public String branchlessToUppercase() {
    char[] chars = input.toCharArray();
    int bitToRightShift = 15;
    for (int i = 0; i < chars.length; i++) {
      int charValue = chars[i];
      chars[i] -= (((CHAR_BEFORE_A - charValue) & (charValue - CHAR_AFTER_Z)) >> bitToRightShift) & OFFSET;
    }
    return valueOf(chars);
  }

  @Benchmark
  public String stdToUppercase() {
    return input.toUpperCase();
  }
}
