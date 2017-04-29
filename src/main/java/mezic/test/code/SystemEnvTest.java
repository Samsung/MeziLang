package mezic.test.code;

import static java.lang.System.out;

import java.util.Map;
import java.util.Properties;

public class SystemEnvTest {

  public static void main(String[] args) {

    Map<String, String> env_map = System.getenv();

    out.println("Env Variables");
    env_map.forEach((key, val) -> out.println("[" + key + "] " + val));

    Properties ps = System.getProperties();

    out.println("\n\nProperties");
    ps.forEach((key, val) -> out.println("[" + key + "] " + val));

  }

}
