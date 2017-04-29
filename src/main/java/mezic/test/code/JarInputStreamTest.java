package mezic.test.code;

import static java.lang.System.out;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.jar.JarFile;

import mezic.util.PathUtil;

public class JarInputStreamTest {

  public static void main(String[] args) throws IOException {

    HashSet<String> pkg_set = new HashSet<>();
    String path = "C:/Program Files/Java/jre1.8.0_60/lib/rt.jar";

    JarFile jar_file = new JarFile(new File(path));

    /*
     * jar_file.stream().forEach( (entry)->pkg_set.add( PathUtil.getPkgName(
     * entry.getName() ) ) );
     */

    jar_file.stream().forEach((entry) -> {
      PathUtil.getSubPkgNames(entry.getName(), "/").stream().forEach((sub_pkg) -> {
        /* out.println(sub_pkg); */ pkg_set.add(sub_pkg);
      });
    });

    pkg_set.stream().forEach((str) -> out.println(str));

    jar_file.close();

  }

}
