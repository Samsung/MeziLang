package mezic.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import mezic.util.PathUtil;

public class JarFileCache {

  @SuppressWarnings("unused")
  private String name;
  private JarFile jar_file;
  private HashSet<String> pkg_set = new HashSet<>();

  public JarFileCache(String name) throws FileNotFoundException, IOException {
    this.name = name;
    jar_file = new JarFile(new File(name));

    /*
     * jar_file.stream().forEach( (entry)->pkg_set.add( PathUtil.getPkgName(
     * entry.getName() ) ) );
     */
    jar_file.stream().forEach((entry) -> {
      PathUtil.getSubPkgNames(entry.getName(), "/").stream().forEach((sub_pkg) -> {
        /* out.println(sub_pkg); */ pkg_set.add(sub_pkg);
      });
    });

  }

  public InputStream getClassInputStream(String class_path) throws IOException {
    ZipEntry zip_entry = jar_file.getEntry(class_path);

    if (zip_entry == null) {
      return null;
    }

    return jar_file.getInputStream(zip_entry);
  }

  public boolean isValidPkg(String pkg_path) {
    return pkg_set.contains(pkg_path);
  }

  public static void main(String[] args) throws FileNotFoundException, IOException {
    String path = "C:/Program Files/Java/jre1.8.0_60/lib/rt.jar";
    JarFileCache cache = new JarFileCache(path);
    System.out.println(cache.isValidPkg("java/lang"));
  }
}
