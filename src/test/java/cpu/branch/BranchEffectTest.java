package cpu.branch;

import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BranchEffectTest {

  @Test
  void checkSameResults() {
    BranchEffect cut = new BranchEffect();
    cut.setup();
    Stream<Supplier<String>> functionStream = of(cut::branchToUppercase, cut::branchlessToUppercase);

    // This method assumes that the STD is correct
    String oracle = cut.stdToUppercase();

    assertThat(functionStream.map(Supplier::get)).containsOnly(oracle);
  }
}
