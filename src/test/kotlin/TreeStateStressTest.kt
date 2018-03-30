import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.util.*

/**
 * Created by haipham on 30/3/18.
 */

class TreeStateStressTest {
  val keyCount = 10000
  var alphabets: List<String>? = null
  var keys: MutableList<String>? = null
  var keyValues: MutableMap<String, Int>? = null
  var maxLength = 0

  @BeforeMethod
  fun setUp() {
    val letters = "a b c d e f g h i j k l m n o p q r s t u v w x y z"
    alphabets = letters.split(" ")
    keys = mutableListOf()
    keyValues = mutableMapOf()
    val rand = Random()

    for (i in 0 until keyCount) {
      val length = Math.max(1, 30)

      if (length > maxLength) {
        maxLength = length
      }

      val key = (0 until length).joinToString(".") {
        alphabets!![rand.nextInt(alphabets!!.size)]
      }

      val value = rand.nextInt(1000000)
      keys!!.add(key)
      keyValues!![key] = value
    }
  }

  @Test
  fun test_updateStateWithKeyValues_shouldWork() {
    /// Setup
    var state = TreeState.empty<Int>()

    /// When
    state = state.updateValues(keyValues!!)

    /// Then
    for (key in keys!!) {
      val value = keyValues!![key]!!
      val stateValue = state.valueAt(key).value!!
      Assert.assertEquals(stateValue, value)
    }

    Assert.assertEquals(state.nestingLevel, maxLength)
    state = state.removeValues(keys!!)
    Assert.assertTrue(state.isEmpty)
    print(state)
  }
}