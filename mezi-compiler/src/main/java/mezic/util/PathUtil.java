package mezic.util;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mezic.compiler.CompileException;

public class PathUtil {

  private static String getAbsolutePath(String[] contTokens) {
    String excel_path = "";

    if (contTokens.length == 5) {
      excel_path = contTokens[1];

    } else if (contTokens.length == 6) {
      excel_path = contTokens[1] + ":" + contTokens[2];
    } else {
      new CompileException("Invalid Container Name Token Len(" + contTokens.length + ")");
    }

    File cont = new File(excel_path);

    return cont.getAbsolutePath();

    // return excel_path;
  }

  private static String getAbsoluteDir(String[] contTokens) {
    String absPath = getAbsolutePath(contTokens);

    if (absPath == null) {
      new CompileException("Invalid Container Name Token Len(" + contTokens.length + ")");
    }

    File f = new File(absPath);

    File p = f.getParentFile();

    return p.getAbsolutePath();
  }

  private static boolean isRelativeExcelContainerPath(String path) {

    return (!path.startsWith("\\") && !path.startsWith("/"));
    /*
    if (!path.startsWith("\\") && !path.startsWith("/"))
      return true;
    else
      return false;
      */
  }

  public static String excelContainerNameProcessing(String target_cont_name, String self_cont_name)
      throws CompileException {
    if (target_cont_name == null && self_cont_name == null) {
      new CompileException("Invalid Container Name Target(" + target_cont_name + ") Self(" + self_cont_name + ")");
    }

    if (target_cont_name.startsWith("excel:") && self_cont_name.startsWith("excel:")) {
      String[] targetContNameTokens = Util.getTokens(target_cont_name, ":");
      String[] selfContNameTokens = Util.getTokens(self_cont_name, ":");

      if (targetContNameTokens == null && selfContNameTokens == null) {
        new CompileException("Invalid Container Name Target(" + target_cont_name + ") Self(" + self_cont_name + ")");
      }

      if (targetContNameTokens.length == 4) {
        String excel_path = "";
        excel_path = getAbsolutePath(selfContNameTokens);
        return targetContNameTokens[0] + ":" + excel_path + ":" + targetContNameTokens[1] + ":"
            + targetContNameTokens[2] + ":" + targetContNameTokens[3];
      } else if (targetContNameTokens.length == 5 && isRelativeExcelContainerPath(targetContNameTokens[1])) {
        String excel_dir = getAbsoluteDir(selfContNameTokens);

        return targetContNameTokens[0] + ":" + excel_dir + "/" + targetContNameTokens[1] + ":" + targetContNameTokens[2]
            + ":" + targetContNameTokens[3] + ":" + targetContNameTokens[4];
      } else {
        new CompileException("Invalid Container Name Target(" + target_cont_name + ")");
      }

    }

    return target_cont_name;
  }

  public static String getPkgName(String tu_name) {
    return getPkgName(tu_name, "/");
  }

  public static String getPkgName(String tu_name, String delimeter) {
    String[] tks = Util.getTokens(tu_name, delimeter);

    if (tks.length >= 2) {
      StringBuffer buf = new StringBuffer();
      int size = tks.length - 1;
      for (int i = 0; i < size; i++) {
        if (i == 0) {
          buf.append(tks[i]);
        } else {
          buf.append(delimeter + tks[i]);
        }
      }
      return buf.toString();
    } else {
      return "";
    }
  }

  public static List<String> getSubPkgNames(String tu_name, String delimeter) {
    String[] tks = Util.getTokens(tu_name, delimeter);

    List<String> list = new ArrayList<>();

    if (tks.length >= 2) {
      StringBuffer buf = new StringBuffer();
      int size = tks.length - 1;
      for (int i = 0; i < size; i++) {
        if (i == 0) {
          buf.append(tks[i]);
        } else {
          buf.append(delimeter + tks[i]);
        }

        list.add(buf.toString());
      }

    } else {
      // no pkg name
    }

    return list;
  }

  public static String getSimpleName(String full_name) {
    String[] tks = Util.getTokens(full_name, "/");

    if (tks == null) {
      return null;
    } else {
      return tks[tks.length - 1];
    }
  }

  public static boolean clearFilesOnPath(String path) {
    File dir = new File(path);

    if (!dir.exists()) {
      System.out.println(path + " does not exists");
      return false;
    }

    if (!dir.isDirectory()) {
      System.out.println(path + " is not directory");
      return false;
    }

    File[] files = dir.listFiles();

    System.out.println("deleting " + path + " (" + files.length + " files)");

    for (int i = 0; i < files.length; i++) {
      if (files[i].isFile()) {
        // System.out.println("deleting " + files[i] );
        if (!files[i].delete()) {
          return false;
        }
      }
    }

    return true;
  }

  public static boolean listPath(String path) {
    File dir = new File(path);

    if (!dir.exists()) {
      System.out.println(path + " does not exists");
      return false;
    }

    if (!dir.isDirectory()) {
      System.out.println(path + " is not directory");
      return false;
    }

    File[] files = dir.listFiles();

    System.out.println("# of files: " + files.length);

    for (int i = 0; i < files.length; i++) {
      if (files[i].isFile()) {
        System.out.println("(" + i + ")" + files[i]);
      }
    }

    return true;
  }

  public static String mtdDscToFileName(String dsc) {
    int size = dsc.length();
    StringBuffer buf = new StringBuffer();

    char c = 0;

    for (int i = 0; i < size; i++) {
      c = dsc.charAt(i);

      if (Character.isAlphabetic(c)) {
        buf.append(c);
      } else {
        buf.append('_');
      }
    }

    return buf.toString();
  }


  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void executeMain(String[] classes) {
    Arrays.stream(classes).forEach(strClass -> {
      try {
        Class testClass = Class.forName(strClass);
        Method meth = testClass.getMethod("main", String[].class);
        meth.invoke(null, (Object) null);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });

  }

}
