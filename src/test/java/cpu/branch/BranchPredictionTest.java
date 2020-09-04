package cpu.branch;

import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;

import cpu.branch.BranchPrediction.NoBranches;
import cpu.branch.BranchPrediction.StreamBranches;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BranchPredictionTest {

  @Test
  void checkSameResults() {
    Stream<Function<BranchPredictionDataset, Integer>> functionStream =
        of(new BranchPrediction()::sortedArray, new BranchPrediction()::unsortedArray,
            new NoBranches()::branchlessSortedArray, new NoBranches()::branchlessUnsortedArray,
            new StreamBranches()::streamSortedArray, new StreamBranches()::streamUnsortedArray);

    BranchPredictionDataset dataset = new BranchPredictionDataset();

    assertThat(functionStream.mapToInt(f -> f.apply(dataset)))
        .containsOnly(dataset.expectedOutput);
  }
}
