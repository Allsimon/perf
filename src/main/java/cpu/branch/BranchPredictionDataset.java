package cpu.branch;

import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.util.Random;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.State;

@State(Benchmark)
public class BranchPredictionDataset {

  static final int MIN_VALUE = 500;
  final int[] input;
  final int[] sortedInput;
  final long expectedOutput;

  /**
   * Generate arrays with values included in [-1000; 1000]
   */
  public BranchPredictionDataset() {
    Random rnd = new Random(0);
    input = IntStream.generate(() -> rnd.nextInt() % 1_000)
        .limit(30_000)
        .toArray();
    sortedInput = IntStream.of(input).sorted().toArray();
    expectedOutput = IntStream.of(input).filter(i -> i >= MIN_VALUE).sum();
  }
}
