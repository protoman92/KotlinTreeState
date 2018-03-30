import org.testng.Assert
import org.testng.annotations.Test

/**
 * Created by haipham on 30/3/18.
 */
class TreeStateTest {
  @Test
  fun test_stateConstruction_shouldWork() {
    /// Setup
    val state = TreeState.empty<Int>()

    /// When & Then
    Assert.assertTrue(state.isEmpty)
  }
}