import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by haipham on 30/3/18.
 */
public final class AccessTest {
  @Test
  public void test_accessKotlinClasses_shouldWork() {
    /// Setup
    System.out.println(TreeStateKt.class);
    TreeStateType<String> state = TreeState.Companion.empty();
    Map<String, String> keyValues = new HashMap<>();

    String[][] kvArrays = new String[][] {
      new String[] { "a.b.c", "1" },
      new String[] { "a.b.c.d", "2" },
      new String[] { "a.b.c.d.e", "3" }
    };

    for (String[] kv : kvArrays) {
      keyValues.put(kv[0], kv[1]);
    }

    /// When
    state = state.updateValues(keyValues);

    /// Then
    for (String[] kv : kvArrays) {
      Assert.assertEquals(state.valueAt(kv[0]).getValue(), kv[1]);
    }
  }
}
