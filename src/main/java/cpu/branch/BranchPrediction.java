package cpu.branch;

import static cpu.branch.BranchPredictionDataset.MIN_VALUE;

import java.util.Arrays;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

@Fork(1)
@Warmup(iterations = 3, batchSize = 2, time = 2)
@Measurement(iterations = 3, batchSize = 2, time = 2)
public class BranchPrediction {

  public static class Basic {

    @Benchmark
    public long unsortedArray(BranchPredictionDataset dataset) {
      return sumOnArray(dataset.input);
    }

    @Benchmark
    public long sortedArray(BranchPredictionDataset dataset) {
      return sumOnArray(dataset.sortedInput);
    }

    private long sumOnArray(int[] array) {
      long sum = 0;
      for (int value : array) {
        if (value >= MIN_VALUE) {
          sum += value;
        }
      }
      return sum;
    }
  }

  public static class NoBranches {

    @Benchmark
    public long branchlessUnsortedArray(BranchPredictionDataset dataset) {
      return branchlessSumOnArray(dataset.input);
    }

    @Benchmark
    public long branchlessSortedArray(BranchPredictionDataset dataset) {
      return branchlessSumOnArray(dataset.sortedInput);
    }

    private long branchlessSumOnArray(int[] array) {
      long sum = 0;
      for (int value : array) {
        // The highest bit in an int is the sign bit: it's value is 1 if and only if it's value is negative
        // So if (value - MIN_VALUE) is negative, after shifting it by 31, it will be 1 otherwise 0
        sum += ~((value - MIN_VALUE) >> 31) & value;
      }
      return sum;
    }
  }

  public static class StreamBranches {

    @Benchmark
    public long streamUnsortedArray(BranchPredictionDataset dataset) {
      return sumStream(dataset.input);
    }

    @Benchmark
    public long streamSortedArray(BranchPredictionDataset dataset) {
      return sumStream(dataset.sortedInput);
    }

    private long sumStream(int[] array) {
      return Arrays.stream(array).filter(i -> i >= MIN_VALUE).sum();
    }
  }
}
