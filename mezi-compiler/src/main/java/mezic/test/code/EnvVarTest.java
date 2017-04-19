package mezic.test.code;

import java.util.Map;

public class EnvVarTest {

  @SuppressWarnings("rawtypes")
  public static void main(String[] args) {

    for (Map.Entry entry : System.getProperties().entrySet()) {
      System.out.println(entry.getKey() + "=" + entry.getValue());
    }

    /*
     * for (Map.Entry entry: System.getenv().entrySet()) System.out.println(
     * entry.getKey() + "=" + entry.getValue() );
     */

  }

}
