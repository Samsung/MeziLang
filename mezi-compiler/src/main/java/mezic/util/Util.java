package mezic.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class Util extends Thread {

  public static void print_buf(String str, byte[] buf, int len) {
    int i = 0;
    System.out.print("\n" + str + " size=" + len + " buf=(");

    for (i = 0; i < len; i++) {
      System.out.print(buf[i] + ", ");
    }
    System.out.println(")");
  }

  public static String getByteString(byte[] buf) {
    StringBuffer strBuf = new StringBuffer();

    for (int i = 0; i < buf.length; i++) {
      strBuf.append(String.format(" %02x", buf[i]));
    }
    return strBuf.toString();
  }

  public static String getByteValueString(byte[] buf, int len) {
    int i = 0;
    StringBuffer strBuf = new StringBuffer();

    strBuf.append(new String("("));
    for (i = 0; i < len; i++) {
      strBuf.append(new String(buf[i] + "('" + (char) buf[i] + "'), "));
    }
    strBuf.append(new String(")"));

    return strBuf.toString();
  }

  public static void delay_consume_interrupt(long t) {
    try {
      sleep(t);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

  public static void delay(long t) throws InterruptedException {
    sleep(t);
  }

  /*
   * public static void delay_no_interrupt_hanle(long t) throws
   * InterruptedException { sleep(t); }
   */

  public static int loc_count = 0;

  public static void loc() {
    System.err.println("##" + (loc_count++) + "##");
  }

  public static String[] getTokens(String str, String delim) {
    StringTokenizer st = new StringTokenizer(str, delim);
    int count = st.countTokens();
    String[] tokens = new String[count];
    for (int i = 0; i < count; i++) {
      tokens[i] = st.nextToken();
    }
    return tokens;
  }

  public static String[] splitWithFirst(String str, String delim) {
    int index = str.indexOf(delim);
    if (index == -1) {
      return null;
    }

    String[] token = new String[2];

    token[0] = str.substring(0, index);
    token[1] = str.substring(index + 1, str.length());

    return token;
  }

  public static String[] getStrArray(LinkedList<?> list) {
    String[] str_array = new String[list.size()];

    for (int i = 0; i < str_array.length; i++) {
      str_array[i] = (String) list.get(i);
    }

    return str_array;
  }

  public static String removeNewLine(String str) {
    String tmpStr = str.replaceAll("\n", "");
    tmpStr = tmpStr.replaceAll("\r", "");
    return tmpStr;
  }

  public static String trimChars(String str, String strTrimChars) {
    String trimedStr = str;
    for (int i = 0; i < strTrimChars.length(); i++) {
      trimedStr = trimedStr.replaceAll((new Character(strTrimChars.charAt(i))).toString(), "");
    }
    return trimedStr;
  }

  public static String getPostString(String src, String keyword, int string_length) {
    int startIdx = src.indexOf(keyword) + keyword.length();
    return src.substring(startIdx, startIdx + string_length);
  }

  public static String trimDirName(String dir, String slash) {
    if (dir.endsWith(slash)) {
      return dir;
    } else  {
      return (dir + slash);
    }
  }

  public static int findByteArray(byte[] buf, int start_index, int buf_length, byte[] match) {
    int i = 0;
    int j = 0;

    if (buf == null || match == null) {
      return -1;
    }

    if (buf.length < buf_length) {
      return -1;
    }

    if (buf_length < match.length) {
      return -1;
    }

    if ((start_index + match.length) > buf_length) {
      return -1;
    }

    for (i = start_index; i < (buf_length - match.length + 1); i++) {
      for (j = 0; j < match.length; j++) {
        // System.out.println("buf["+i+"+"+j+"]="+(char)buf[i+j]+"
        // match["+j+"]="+(char)match[j]);
        if (buf[i + j] != match[j]) {
          break;
        }
      }
      if (j == match.length) {
        return i;
      }
    }

    return -1;
  }

  public static int removeByteArray(byte[] buf, int buf_length, int start_index, int end_index) {
    if (buf_length > buf.length || buf_length <= start_index || buf_length <= end_index || end_index < start_index) {
      return -1;
    }

    //int idx = 0;

    byte[] tmpBuf = new byte[buf_length];

    for (int i = (end_index + 1); i < buf_length; i++) {
      tmpBuf[i - (end_index + 1)] = buf[i];
    }

    for (int i = start_index; i < (start_index + (buf_length - end_index - 1)); i++) {
      buf[i] = tmpBuf[i - start_index];
    }

    for (int i = (start_index + (buf_length - end_index - 1)); i < buf_length; i++) {
      buf[i] = 0;
    }

    return buf_length - (end_index - start_index + 1);
  }

  public static byte[] getByteArray(byte[] buf, int buf_length, int start_index, int end_index) {
    if (buf_length > buf.length || buf_length <= start_index || buf_length <= end_index || end_index < start_index) {
      return null;
    }

    int idx = 0;

    int array_length = end_index - start_index + 1;

    byte[] tmp = new byte[array_length];

    for (int i = start_index; i <= end_index; i++) {
      tmp[idx++] = buf[i];
    }

    return tmp;
  }

  public static void testFindByte() {

    // String buf = "01230123456789abcdefghijklmnopqrstuvwxyz";
    String buf = "0123456789";
    byte[] bBuf = buf.getBytes();
    print_buf("bBuf", bBuf, bBuf.length);

    String match1 = "789";
    byte[] bMatch1 = match1.getBytes();
    print_buf("bMatch", bMatch1, bMatch1.length);

    String match2 = "012";
    byte[] bMatch2 = match2.getBytes();
    print_buf("bMatch", bMatch2, bMatch2.length);

    int ind1 = findByteArray(bBuf, 0, bBuf.length, bMatch1);
    System.out.println("index 1=" + ind1);

    int ind2 = findByteArray(bBuf, 0, bBuf.length, bMatch2);
    System.out.println("index 2=" + ind2);

    int len = removeByteArray(bBuf, bBuf.length, ind1, ind2 + bMatch2.length - 1);

    print_buf("buf:", bBuf, bBuf.length);
    System.out.println("len=" + len);
  }

  public static int removeByteArray(byte[] buf, int buf_length, byte[] ba) {
    int bufIdx = 0;
    int bufLen = buf_length;
    int ba_idx = 0;

    while (ba_idx != -1) {
      ba_idx = findByteArray(buf, bufIdx, bufLen, ba);

      if (ba_idx != -1) {
        if ((ba_idx - 1) >= bufIdx) {
          bufLen = Util.removeByteArray(buf, bufLen, ba_idx, ba_idx + ba.length - 1);
        }
      }
    }
    return bufLen;
  }

  public static void setIncreament4Byte(byte[] user_data, int off_set, int init_val) {
    long val = Util.unsigned32(init_val);
    int index = off_set;
    byte[] word;

    while (index + 3 < user_data.length) {
      word = Util.long2byte(val & 0xFFFFFFFFL);
      System.arraycopy(word, 4, user_data, index, 4);
      val++;
      index += 4;
    }
  }

  public static void setIncreament2Byte(byte[] user_data, int off_set, short init_val) {
    long val = Util.unsigned16(init_val);
    int index = off_set;
    byte[] word;

    while (index + 1 < user_data.length) {
      word = Util.long2byte(val & 0xFFFFFFFFL);
      System.arraycopy(word, 6, user_data, index, 2);
      val++;
      index += 2;
    }
  }

  public static boolean increamentWordCheck(byte[] user_data, int off_set, int init_val) {
    long val = Util.unsigned32(init_val);
    int index = off_set;
    byte[] word;

    while (index + 3 < user_data.length) {
      word = Util.long2byte(val & 0xFFFFFFFFL);
      if (word[4] != user_data[index] || word[5] != user_data[index + 1] || word[6] != user_data[index + 2]
          || word[7] != user_data[index + 3]) {
        return false;
      }

      // System.arraycopy(word, 4, user_data, index, 4);
      val++;
      index += 4;
    }

    return true;
  }

  public static short byte2short(byte[] value, int offset) {
    long result = (((long) (value[0 + offset]) & 0xFF) << 8) | ((long) (value[1 + offset]) & 0xFF);
    return (short) (result & 0xFFFFL);
  }

  public static byte[] short2byte(short value) {
    byte [] result = new byte[2];
    result[0] = ((byte) (value >> 8));
    result[1] = ((byte) (value));
    return result;
  }

  public static void short2byte(short value, byte[] buf) {
    if (buf == null || buf.length < 2) {
      System.err.println("Util.short2byte buffer is invalid");
      return;
    }
    buf[0] = ((byte) (value >> 8));
    buf[1] = ((byte) (value));
    return;
  }

  public static byte[] long2byte(long value) {
    byte [] result = new byte[8];
    result[0] = ((byte) (value >> 56));
    result[1] = ((byte) (value >> 48));
    result[2] = ((byte) (value >> 40));
    result[3] = ((byte) (value >> 32));
    result[4] = ((byte) (value >> 24));
    result[5] = ((byte) (value >> 16));
    result[6] = ((byte) (value >> 8));
    result[7] = ((byte) (value));
    return result;
  }

  public static long byte2long(byte[] value, int count) {
    long result = (((long) (value[0 + count]) & 0xFF) << 56) | (((long) (value[1 + count]) & 0xFF) << 48)
        | (((long) (value[2 + count]) & 0xFF) << 40) | (((long) (value[3 + count]) & 0xFF) << 32)
        | (((long) (value[4 + count]) & 0xFF) << 24) | (((long) (value[5 + count]) & 0xFF) << 16)
        | (((long) (value[6 + count]) & 0xFF) << 8) | ((long) (value[7 + count]) & 0xFF);
    return result;
  }

  public static long unsigned32(int n) {
    return n & 0xFFFFFFFFL;
  }

  public static long unsigned16(short s) {
    return s & 0xFFFFL;
  }

  public static long unsigned16(long s) {
    return s & 0xFFFFL;
  }

  public static int unsigned8(byte b) {
    return b & 0xFF;
  }

  public static boolean getBit(byte[] ba, int bit_offset) {
    int byte_idx = bit_offset / 8;
    int bit_remainder = bit_offset % 8;
    byte bit_mask = (byte) ((0x1 << (7 - bit_remainder)) & 0xFF);

    // System.out.println("byte_idx="+byte_idx+"
    // bit_mask="+Long.toHexString((long)bit_mask));

    return ((ba[byte_idx] & bit_mask) != 0);
    /*
    if ((ba[byte_idx] & bit_mask) == 0) {
      return false;
    } else {
      return true;
    }
    */
  }

  public static void setBit(byte[] ba, int bit_offset, boolean on) {
    int byte_idx = bit_offset / 8;
    int bit_remainder = bit_offset % 8;
    byte bit_mask = (byte) ((0x1 << (7 - bit_remainder)) & 0xFF);

    // System.out.println("byte_idx="+byte_idx+"
    // bit_mask="+Long.toHexString((long)bit_mask));

    if (on) {
      ba[byte_idx] |= bit_mask;
    } else {
      ba[byte_idx] &= ~bit_mask;
    }
  }

  public static void parse_hex(String str_hex, byte[] buf, int byte_size) throws Exception {
    if (buf.length != byte_size || (str_hex.length() / 2) != byte_size) {
      throw new Exception("Exception while parsing hex value(" + str_hex + ") byte_size(" + byte_size + ") buf_length("
          + buf.length + ")");
    }

    String val;

    for (int i = 0; i < byte_size; i++) {
      val = str_hex.substring(i * 2, i * 2 + 2);
      // System.out.println("["+val+"]");
      buf[i] = (byte) Integer.parseInt(val, 16);
    }
  }

  public static boolean isHexNum(String hex) {
    char c;
    for (int i = 0; i < hex.length(); i++) {
      c = hex.charAt(i);

      if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
        return false;
      }
    }
    return true;
  }

  public static boolean isDigitNum(String digit) {
    char c;
    for (int i = 0; i < digit.length(); i++) {
      c = digit.charAt(i);

      if (!(c >= '0' && c <= '9')) {
        return false;
      }
    }
    return true;
  }

  public static boolean isEmptyString(String str) {
    char c;
    for (int i = 0; i < str.length(); i++) {
      c = str.charAt(i);

      if (c != ' ' && c != '\t' && c != '\n' && c != 'r') {
        return false;
      }
    }
    return true;
  }

  public static int findIndexStartWith(String[] tokens, String str) {
    for (int i = 0; i < tokens.length; i++) {
      if (tokens[i].startsWith(str)) {
        return i;
      }
    }
    return -1;
  }

  public static int findIndexMatchedWith(String[] tokens, String str) {
    for (int i = 0; i < tokens.length; i++) {
      if (tokens[i].compareTo(str) == 0) {
        return i;
      }
    }
    return -1;
  }

  public static String findValueWithTag(String[] tokens, String tag, String delim) {
    int ind = findIndexStartWith(tokens, tag);

    if (ind == -1) {
      return null;
    }

    String tag_val = tokens[ind];

    String[] token2 = getTokens(tag_val, delim);

    if (token2.length != 2) {
      return null;
    }

    return token2[1];
  }

  public static String convertToFileURL(String filename) {
    // On JDK 1.2 and later, simplify this to:
    // "path = file.toURL().toString()".
    String path = new File(filename).getAbsolutePath();
    if (File.separatorChar != '/') {
      path = path.replace(File.separatorChar, '/');
    }
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    String retVal = "file:" + path;

    return retVal;
  }

  public static void checkColumStrings(String[] colStrs, int length, String hdrname, String exception_msg)
      throws Exception {
    if (colStrs == null || colStrs.length != length) {
      throw new Exception(exception_msg);
    }

    if (length > 0 && hdrname != null && hdrname.compareTo(colStrs[0]) != 0) {
      throw new Exception(exception_msg + ". Invalid Column Hdr(expect:" + hdrname + ", but:" + colStrs[0] + "). ");
    }
  }

  public static String getFileString(String path) throws IOException {
    File file = new File(path);
    StringBuffer strBuf = new StringBuffer();

    BufferedReader buf = null;
    FileReader freader = new FileReader(file);

    buf = new BufferedReader(freader);

    String line;
    while ((line = buf.readLine()) != null) {
      strBuf.append(line + "\n");
    }

    freader.close();

    return strBuf.toString();
  }

  public static void storeFileStr(String str, String path) throws IOException {
    FileOutputStream fout = new FileOutputStream(path, true);

    fout.write(str.getBytes());

    fout.close();
  }

  public void writeByteToFile(byte[] buf, String file_name) throws IOException {
    FileOutputStream fos = new FileOutputStream(file_name);
    fos.write(buf);
    fos.close();
  }

  public byte[] readByteFromFile(byte[] buf, String file_name) throws IOException {
    RandomAccessFile f = new RandomAccessFile(file_name, "r");
    byte[] b = new byte[(int) f.length()];
    f.read(b);
    f.close();
    return b;
  }

  public static int[] integerList2intArr(LinkedList<Integer> list) {
    int[] arr = new int[list.size()];

    for (int i = 0; i < arr.length; i++) {
      arr[i] = list.get(i);
    }

    return arr;
  }

  @SuppressWarnings("unchecked")
  public static <E> E[] list2Arr(Class<E> clazz, LinkedList<E> list) {
    int size = list.size();
    // E[] arr = (E[]) new Object[size];
    E[] arr = (E[]) Array.newInstance(clazz, size);

    for (int i = 0; i < size; i++) {
      arr[i] = list.get(i);
    }

    return arr;
  }

  public static void dump_strarr(String[] arr, String doc) {
    System.out.println(doc);
    for (int i = 0; i < arr.length; i++) {
      System.out.println("(" + i + ") " + arr[i]);
    }
  }

  public static void main(String[] args) {

    // String a = "od -x --skip-bytes 8 --read-bytes 2 --address-radix n /etc/Ld
    // BdInfo";
    // String a = "=aaa";
    // String a = "aaa=";
    String a = "=";

    String[] token = splitWithFirst(a, "=");

    System.out.println("[" + token[0] + "]");
    System.out.println("[" + token[1] + "]");
    System.out.println(token.length);
  }

}
