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
    val identifier = "a.b.c"
    var state = TreeState.empty<Int>()

    /// When & Then
    state = state.updateValue(identifier, 1)
    Assert.assertEquals(state.valueAt(identifier).value, 1)
    state = state.removeValue(identifier)
    Assert.assertNull(state.valueAt(identifier).value)
    Assert.assertNull(state.valueAt("").value)
    state = state.clear().updateValue("", 10)
    Assert.assertTrue(state.isEmpty)
  }

  @Test
  fun test_updateChildState_shouldWork() {
    /// Setup
    val identifier = "a.b.c.d"
    val stateId = "a.b"
    val subId = "c.d"
    var state1 = TreeState.empty<Int>()
    var state2 = TreeState.empty<Int>()

    /// When & Then
    state2 = state2.updateValue(subId, 1)
    state1 = state1.updateChildState(stateId, state2)
    Assert.assertEquals(state1.valueAt(identifier).value, 1)
    Assert.assertEquals(state1.childStateAt(stateId).value?.valueAt(subId)?.value, 1)
    state1.removeChildState(stateId)
    Assert.assertEquals(state1.valueAt(identifier).value, 1)
    Assert.assertNotNull(state1.childStateAt(stateId).value)
    Assert.assertNull(state1.childStateAt("").value)
    state1 = state1.updateChildState("", state2)
    Assert.assertEquals(state1.valueAt(identifier).value, 1)
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
    val identifier = "a.b.c"
    val state1 = TreeState.empty<Int>()
    val extraValues = mutableMapOf("a" to 0, "b" to 1, "c" to 2)

    /// When
    val state2 = state1.updateValue(identifier, 1)
    val state3 = state1.updateValues(extraValues)
    extraValues["a"] = 10

    /// Then
    Assert.assertNull(state1.valueAt(identifier).value)
    Assert.assertEquals(state2.valueAt(identifier).value, 1)
    Assert.assertEquals(state3.valueAt("a").value, 0)
  }

  @Test
  fun test_stateChildStateImmutability_shouldWork() {
    /// Setup
    val identifier = "a.b.c.d"
    val stateId = "a.b"
    val subId = "c.d"
    val state1 = TreeState.empty<Int>()

    /// When
    val state2 = TreeState.empty<Int>().updateValue(subId, 1)
    val state3 = state1.updateChildState(stateId, state2)
    val state4 = state2.removeValue(subId)

    /// Then
    Assert.assertEquals(state2.valueAt(subId).value, 1)
    Assert.assertEquals(state3.valueAt(identifier).value, 1)
    Assert.assertTrue(state4.isEmpty)
  }
}