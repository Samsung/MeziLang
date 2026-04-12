package mezic.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import mezic.util.PathUtil;

public class JarFileCache {

  @SuppressWarnings("unused")
  private String name;
  private ZipFile jar_file;
  private HashSet<String> pkg_set = new HashSet<>();
  private final boolean jmodFile;

  public JarFileCache(String name) throws FileNotFoundException, IOException {
    this.name = name;
    this.jmodFile = name.endsWith(".jmod");
    jar_file = new ZipFile(new File(name));

    /*
     * jar_file.stream().forEach( (entry)->pkg_set.add( PathUtil.getPkgName(
     * entry.getName() ) ) );
     */
    jar_file.stream().forEach((entry) -> {
      String entryName = entry.getName();
      if (jmodFile) {
        if (!entryName.startsWith("classes/")) {
          return;
        }
        entryName = entryName.substring("classes/".length());
      }

      PathUtil.getSubPkgNames(entryName, "/").stream().forEach((sub_pkg) -> {
        /* out.println(sub_pkg); */ pkg_set.add(sub_pkg);
      });
    });

  }

  public InputStream getClassInputStream(String class_path) throws IOException {
    if (jmodFile) {
      class_path = "classes/" + class_path;
    }

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
