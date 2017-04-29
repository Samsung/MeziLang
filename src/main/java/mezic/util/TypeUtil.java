package mezic.util;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.type.AbsClassType;
import mezic.compiler.type.AbsType;
import mezic.compiler.type.TPrimitiveClass;

public class TypeUtil {

  public static boolean isPositiveInteger(String val) {
    int len = val.length();

    if (len <= 0) {
      return false;
    }

    int i = 0;

    if (val.startsWith("0x") || val.startsWith("0X")) {
      i = 2;
    }

    char c;

    for (; i < len; i++) {
      c = val.charAt(i);
      if (!Character.isDigit(c) && !(c >= 'a' && c <= 'f') && !(c >= 'A' && c <= 'F')) {
        return false;
      }
    }

    return true;
  }

  public static BigInteger parsePositiveInteger(String val) throws CompileException {

    // 'int' value range: -2147483648 ~ 2147483647
    // 0x0 ~ 0xffff,ffff

    BigInteger big_int = null;

    if (val.startsWith("0x") || val.startsWith("0X")) {

      // hexa form can have 1(non-zero) msb
      String num_parts = val.substring(2);
      big_int = new BigInteger(num_parts, 16);

      if (big_int.compareTo(new BigInteger("0", 16)) == -1) {
        throw new CompileException("(" + val + ") is too small as an integer literal");
      }

      if (big_int.compareTo(new BigInteger("ffffffff", 16)) == 1) {
        throw new CompileException("(" + val + ") is too big as an integer literal");
      }

      return big_int;
    } else {
      big_int = new BigInteger(val);
      if (big_int.compareTo(new BigInteger("0")) == -1) {
        throw new CompileException("(" + val + ") is too small as an integer literal");
      }

      if (big_int.compareTo(new BigInteger("2147483647")) == 1) {
        throw new CompileException("(" + val + ") is too big as an integer literal");
      }
      return big_int;
    }
  }

  public static boolean isPositiveShort(String val) {
    int len = val.length();

    if (len <= 0) {
      return false;
    }

    int i = 0;

    if (val.startsWith("0x") || val.startsWith("0X")) {
      i = 2;
    }

    char c;

    for (; i < len; i++) {
      c = val.charAt(i);
      if (!Character.isDigit(c) && !(c >= 'a' && c <= 'f') && !(c >= 'A' && c <= 'F')) {

        if (!(i == (len - 1) && (c == 's' || c == 'S'))) {
          return false;
        }
      }
    }

    return true;
  }

  public static BigInteger parsePositiveShort(String val) throws CompileException {

    BigInteger big_int = null;
    String num_parts;

    if (val.startsWith("0x") || val.startsWith("0X")) {
      num_parts = val.substring(2, val.length() - 1);
      big_int = new BigInteger(num_parts, 16);

      if (big_int.compareTo(new BigInteger("0", 16)) == -1) {
        throw new CompileException("(" + val + ") is too small as an short literal");
      }

      if (big_int.compareTo(new BigInteger("ffff", 16)) == 1) {
        throw new CompileException("(" + val + ") is too big as an short literal");
      }

      return big_int;

    } else {
      num_parts = val.substring(0, val.length() - 1);
      big_int = new BigInteger(num_parts, 10);

      if (big_int.compareTo(new BigInteger("0")) == -1) {
        throw new CompileException("(" + val + ") is too small as an short literal");
      }

      if (big_int.compareTo(new BigInteger("32767")) == 1) {
        throw new CompileException("(" + val + ") is too big as an short literal");
      }
      return big_int;
    }

  }

  public static boolean isCharacter(String val) {
    int len = val.length();

    if (len != 1) {
      return false;
    }
    return true;
  }

  public static char parseCharacter(String val) throws CompileException {
    return val.charAt(0);
  }

  public static boolean isPositiveByte(String val) {
    int len = val.length();

    if (len <= 0) {
      return false;
    }

    int i = 0;

    if (val.startsWith("0x") || val.startsWith("0X")) {
      i = 2;
    }

    char c;

    for (; i < len; i++) {
      c = val.charAt(i);
      if (!Character.isDigit(c) && !(c >= 'a' && c <= 'f') && !(c >= 'A' && c <= 'F')) {

        if (!(i == (len - 1) && (c == 'b' || c == 'B'))) {
          return false;
        }
      }
    }

    return true;
  }

  public static BigInteger parsePositiveByte(String val) throws CompileException {

    BigInteger big_int = null;
    String num_parts;

    if (val.startsWith("0x") || val.startsWith("0X")) {
      num_parts = val.substring(2, val.length() - 1);
      big_int = new BigInteger(num_parts, 16);

      if (big_int.compareTo(new BigInteger("0", 16)) == -1) {
        throw new CompileException("(" + val + ") is too small as an short literal");
      }

      if (big_int.compareTo(new BigInteger("ff", 16)) == 1) {
        throw new CompileException("(" + val + ") is too big as an short literal");
      }

      return big_int;

    } else {
      num_parts = val.substring(0, val.length() - 1);
      big_int = new BigInteger(num_parts, 10);

      if (big_int.compareTo(new BigInteger("0")) == -1) {
        throw new CompileException("(" + val + ") is too small as an short literal");
      }

      if (big_int.compareTo(new BigInteger("127")) == 1) {
        throw new CompileException("(" + val + ") is too big as an short literal");
      }
      return big_int;
    }

  }

  public static boolean isPositiveLongInteger(String val) {
    int len = val.length();

    if (len <= 0) {
      return false;
    }

    int i = 0;

    if (val.startsWith("0x") || val.startsWith("0X")) {
      i = 2;
    }

    char c;

    for (; i < len; i++) {
      c = val.charAt(i);
      if (!Character.isDigit(c) && !(c >= 'a' && c <= 'f') && !(c >= 'A' && c <= 'F')) {

        if (!(i == (len - 1) && (c == 'l' || c == 'L'))) {
          return false;
        }
      }
    }

    return true;
  }

  public static BigInteger parsePositiveLongInteger(String val) throws CompileException {

    // long value range: -9223372036854775808 ~ 9223372036854775807
    // 0x0 ~ 0xffff,ffff,ffff,ffff

    BigInteger big_int = null;
    String num_parts;

    if (val.startsWith("0x") || val.startsWith("0X")) {
      num_parts = val.substring(2, val.length() - 1);
      big_int = new BigInteger(num_parts, 16);

      if (big_int.compareTo(new BigInteger("0", 16)) == -1) {
        throw new CompileException("(" + val + ") is too small as an integer literal");
      }

      if (big_int.compareTo(new BigInteger("ffffffffffffffff", 16)) == 1) {
        throw new CompileException("(" + val + ") is too big as an integer literal");
      }

      return big_int;

    } else {
      num_parts = val.substring(0, val.length() - 1);
      big_int = new BigInteger(num_parts, 10);

      if (big_int.compareTo(new BigInteger("0")) == -1) {
        throw new CompileException("(" + val + ") is too small as an integer literal");
      }

      if (big_int.compareTo(new BigInteger("9223372036854775807")) == 1) {
        throw new CompileException("(" + val + ") is too big as an integer literal");
      }
      return big_int;
    }
  }

  public static boolean isPositiveFloat(String val) {
    int len = val.length();

    if (len <= 0) {
      return false;
    }

    int i = 0;

    if (val.startsWith("0x") || val.startsWith("0X")) {
      return false;
    }

    if (!val.endsWith("f") && !val.endsWith("F")) {
      return false;
    }

    char c;

    for (; i < len; i++) {
      c = val.charAt(i);
      if (!Character.isDigit(c) && c != '.') {

        if (!(i == (len - 1) && (c == 'f' || c == 'F'))) {
          return false;
        }
      }
    }

    return true;
  }

  public static float parsePositiveFloat(String val) throws CompileException {

    float float_val = 0.0f;

    try {

      float_val = Float.parseFloat(val);

    } catch (NumberFormatException e) {
      throw new CompileException("Invalid float number (" + val + ")");
    }

    return float_val;
  }

  public static boolean isPositiveDouble(String val) {
    int len = val.length();

    if (len <= 0) {
      return false;
    }

    int i = 0;

    if (val.startsWith("0x") || val.startsWith("0X")) {
      return false;
    }

    if (!val.endsWith("d") && !val.endsWith("D")) {
      return false;
    }

    char c;

    for (; i < len; i++) {
      c = val.charAt(i);
      if (!Character.isDigit(c) && c != '.') {

        if (!(i == (len - 1) && (c == 'd' || c == 'D'))) {
          return false;
        }
      }
    }

    return true;
  }

  public static double parsePositiveDouble(String val) throws CompileException {

    double double_val = 0.0d;

    try {

      double_val = Double.parseDouble(val);

    } catch (NumberFormatException e) {
      throw new CompileException("Invalid float number (" + val + ")");
    }

    return double_val;
  }

  public static String getArgTypeDsc(String desc) {
    int st_idx = desc.indexOf("(");
    int end_idx = desc.indexOf(")");

    // System.out.println("desc:"+desc+", st_idx:"+st_idx + ",
    // end_idx:"+end_idx);

    if ((end_idx - st_idx) == 1) {
      return "";
    }

    if (st_idx == -1 || end_idx < 1 || (end_idx - 1) < (st_idx + 1)) {
      return null;
    }

    return desc.substring(st_idx + 1, end_idx);
  }

  public static String getRetTypeDsc(String desc) {
    int end_idx = desc.indexOf(")");

    if (end_idx == -1) {
      return null;
    }

    return desc.substring(end_idx + 1);
  }

  public static String dsc2name(String desc) {
    int len = desc.length();
    int idx = 0;
    String array_postfix = "";

    for (;;) {
      if (idx >= len) {
        System.err.println("invalid dsc2name desc(" + desc + ") idx >= len");
        return null;
      }
      if (desc.charAt(idx) == '[') {
        array_postfix += "[]";
      } else {
        break;
      }
      idx++;
    }

    String substr = desc.substring(idx, len);

    // System.out.println("substr="+substr +", idx="+idx +", len="+len);
    // primitive description change
    if (substr.equals("I")) {
      return TPrimitiveClass.NAME_INT + array_postfix;
    } else if (substr.equals("Z")) {
      return TPrimitiveClass.NAME_BOOL + array_postfix;
    } else if (substr.equals("V")) {
      return TPrimitiveClass.NAME_VOID + array_postfix;
    } else if (substr.equals("J")) {
      return TPrimitiveClass.NAME_LONG + array_postfix;
    } else if (substr.equals("C")) {
      return TPrimitiveClass.NAME_CHAR + array_postfix;
    } else if (substr.equals("F")) {
      return TPrimitiveClass.NAME_FLOAT + array_postfix;
    } else if (substr.equals("D")) {
      return TPrimitiveClass.NAME_DOUBLE + array_postfix;
    } else if (substr.equals("B")) {
      return TPrimitiveClass.NAME_BYTE + array_postfix;
    }

    //boolean is_array = false;

    int st_idx = desc.indexOf("L");
    int end_idx = desc.indexOf(";");

    if (st_idx != idx) {
      System.err.println("invalid dsc2name desc(" + desc + ") st_idx != idx");
      return null;
    }
    if ((end_idx - st_idx) == 1) {
      System.err.println("invalid dsc2name desc(" + desc + ") (end_idx - st_idx) == 1");
      return null;
    }

    if (st_idx == -1 || end_idx < 1 || (end_idx - 1) < (st_idx + 1)) {
      return null;
    }

    String name = desc.substring(st_idx + 1, end_idx);

    return name + array_postfix;
  }

  public static int get_map_dimension(String name) {
    int st_idx = name.indexOf("[]");
    if (st_idx == -1) {
      return 0;
    }

    int len = name.length();
    int dimension = 0;

    for (; st_idx < len;) {
      if (name.substring(st_idx, len).startsWith("[]")) {
        dimension++;
        st_idx += 2;
      } else {
        break;
      }
    }
    return dimension;
  }

  public static String get_type_name(String map_name) {
    int st_idx = map_name.indexOf("[]");
    if (st_idx == -1) {
      return map_name;
    } else {
      return map_name.substring(0, st_idx);
    }
  }

  public static String[] splitArgTypeDsc(String desc) throws CompileException {

    Pattern pattern = Pattern.compile("\\[*L[^;]+;|\\[[ZBCSIFDJ]|[ZBCSIFDJ]");
    // Regex for desc \[*L[^;]+;|\[[ZBCSIFDJ]|[ZBCSIFDJ]
    Matcher matcher = pattern.matcher(desc);
    LinkedList<String> listMatches = new LinkedList<String>();
    int counter = 0;

    while (matcher.find()) {
      listMatches.add(matcher.group());
      counter += 1;
      if (counter > CompilerLoader.MAX_ARG_SIZE) {
        throw new CompileException("Maximum argument size error(" + desc + ")");
      }
    }

    String[] arrMatches = new String[counter];

    for (int i = 0; i < counter; i++) {
      arrMatches[i] = listMatches.get(i);
    }

    return arrMatches;
  }

  public static void main(String[] args) {
    try {
      String[] strs = splitArgTypeDsc("[B");

      for (int i = 0; i < strs.length; i++) {
        System.out.println(strs[i]);
      }

    } catch (CompileException e) {
      e.printStackTrace();
    }
  }

  public static boolean isEqualClass(AbsClassType src, AbsClassType tgt) throws CompileException {

    return ((AbsType) src).getName().equals(((AbsType) tgt).getName());
  }

  public static boolean isDescendentOfClass(AbsClassType decendent, AbsClassType ancestor, int depth)
      throws CompileException {

    if (depth > CompilerLoader.MAX_INHERITANCE_LEVEL) {
      throw new CompileException(
          "Maximum Inheritance level(" + CompilerLoader.MAX_INHERITANCE_LEVEL + ") is exceeded:" + depth);
    }

    AbsClassType super_class = decendent.getSuperClass();

    if (super_class == null) {
      return false;
    }

    // if( super_class.equlaClass(ancestor)) return true;
    if (((AbsType) super_class).equals(ancestor)) {
      return true;
    }

    List<AbsClassType> if_list = decendent.getInterfaceList();

    if (if_list.stream().anyMatch((iftype) -> ((iftype)).equals(ancestor))) {
      return true;
    }

    return isDescendentOfClass(super_class, ancestor, depth + 1);
  }

}
