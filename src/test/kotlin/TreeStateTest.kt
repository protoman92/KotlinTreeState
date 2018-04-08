import javafx.beans.binding.Bindings.valueAt
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

  @Test
  fun test_nestingLevel_shouldWork() {
    /// Setup
    var state1 = TreeState.empty<Int>()
    val state2 = TreeState.empty<Int>()

    /// When
    state1 = state1.updateValue("a.b.c", 10)

    /// Then
    Assert.assertEquals(state1.nestingLevel, 3)
    Assert.assertEquals(state2.nestingLevel, 1)
  }

  @Test
  fun test_updateValue_shouldWork() {
    /// Setup
    val path = "a.b.c"
    var state = TreeState.empty<Int>()

    /// When & Then
    state = state.updateValue(path, 1)
    Assert.assertEquals(state.valueAt(path).value, 1)
    state = state.removeValue(path)
    Assert.assertNull(state.valueAt(path).value)
    Assert.assertNull(state.valueAt("").value)
    state = state.clear().updateValue("", 10)
    Assert.assertTrue(state.isEmpty)
  }

  @Test
  fun test_updateChildState_shouldWork() {
    /// Setup
    val path = "a.b.c.d"
    val statePath = "a.b"
    val subPath = "c.d"
    var state1 = TreeState.empty<Int>()
    var state2 = TreeState.empty<Int>()

    /// When & Then
    state2 = state2.updateValue(subPath, 1)
    state1 = state1.updateChildState(statePath, state2)
    Assert.assertEquals(state1.valueAt(path).value, 1)

    Assert.assertEquals(state1.childStateAt(statePath)
      .flatMap { it.valueAt(subPath) }.value, 1)

    state1.removeChildState(statePath)
    Assert.assertEquals(state1.valueAt(path).value, 1)
    Assert.assertNotNull(state1.childStateAt(statePath).value)
    Assert.assertNull(state1.childStateAt("").value)
    state1 = state1.updateChildState("", state2)
    Assert.assertEquals(state1.valueAt(path).value, 1)
  }

  @Test
  fun test_clearState_shouldWork() {
    /// Setup
    var state = TreeState.empty<Int>()

    /// When
    state = state.updateValue("a.b.c", 1).clear()

    /// Then
    Assert.assertTrue(state.isEmpty)
  }

  @Test
  fun test_stateValueImmutability_shouldWork() {
    /// Setup
    val path = "a.b.c"
    val state1 = TreeState.empty<Int>()
    val extraValues = mutableMapOf("a" to 0, "b" to 1, "c" to 2)

    /// When
    val state2 = state1.updateValue(path, 1)
    val state3 = state1.updateValues(extraValues)
    extraValues["a"] = 10

    /// Then
    Assert.assertNull(state1.valueAt(path).value)
    Assert.assertEquals(state2.valueAt(path).value, 1)
    Assert.assertEquals(state3.valueAt("a").value, 0)
  }

  @Test
  fun test_stateChildStateImmutability_shouldWork() {
    /// Setup
    val path = "a.b.c.d"
    val statePath = "a.b"
    val subPath = "c.d"
    val state1 = TreeState.empty<Int>()

    /// When
    val state2 = TreeState.empty<Int>().updateValue(subPath, 1)
    val state3 = state1.updateChildState(statePath, state2)
    val state4 = state2.removeValue(subPath)

    /// Then
    Assert.assertEquals(state2.valueAt(subPath).value, 1)
    Assert.assertEquals(state3.valueAt(path).value, 1)
    Assert.assertTrue(state4.isEmpty)
  }

  @Test
  fun test_accessKeyValues_shouldWork() {
    /// Setup
    var state = TreeState.empty<Int>()
    val keys = listOf("a.b", "a.b.c", "a.b.c.d")

    state = state
      .updateValue("a.b", 0)
      .updateValue("a.b.c", 1)
      .updateValue("a.b.c.d", 2)

    /// When
    val keyValues = state.keyValues

    /// Then
    keys.forEach { Assert.assertEquals(keyValues[it], state.valueAt(it).value) }
  }
}