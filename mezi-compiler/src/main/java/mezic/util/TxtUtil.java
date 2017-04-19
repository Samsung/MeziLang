package mezic.util;

public class TxtUtil {

  public static int lineNumber(int ind, StringBuffer txt) {
    return countChar(0, ind, txt, '\n');
  }

  public static int countChar(int start_index, int end_index, StringBuffer txt, char c) {
    int count = 0;
    for (int i = start_index; i <= end_index; i++) {
      if (txt.charAt(i) == c) {
        count++;
      }
    }

    return count;
  }

  public static int indexOfMatchedNoExpect(String expectString, StringBuffer ret, int expect_idx, String noExp) {
    int pre_diff = noExp.indexOf(expectString);
    int start_indx;
    int noexp_len = noExp.length();
    String noExpMatchString = "";

    if (pre_diff == -1) {
      // System.out.println("no Exp does not contain expectStr");
      return -1;
    }

    start_indx = expect_idx - pre_diff;

    /*
     * System.out.println("expect_idx=["+expect_idx+"]");
     * System.out.println("pre_diff=["+pre_diff+"]");
     * System.out.println("start_indx=["+start_indx+"]");
     * System.out.println("noexp_len=["+noexp_len+"]");
     */

    if (start_indx < 0) {
      // System.out.println("no Exp start_indx is minus("+start_indx+")");
      return -1;
    }

    if ((start_indx + noexp_len) > ret.length()) {
      // System.out.println("no Exp end index is over("+(start_indx +
      // noexp_len)+")");
      return -1;
    }

    noExpMatchString = ret.substring(start_indx, (start_indx + noexp_len));

    // System.out.println("noExpMatchString=["+noExpMatchString+"],
    // noExpectString=["+noExp+"]");

    if (noExpMatchString.compareTo(noExp) == 0) {
      return start_indx; // start index of matched No Expect String
    } else {
      return -1; //
    }
  }

  public static int indexOfMatchedNoExpect(String expectString, String ret, int expect_idx, String noExp) {
    int pre_diff = noExp.indexOf(expectString);
    int start_indx;
    int noexp_len = noExp.length();
    String noExpMatchString = "";

    if (pre_diff == -1) {
      return -1;
    }

    start_indx = expect_idx - pre_diff;

    if (start_indx < 0) {
      return -1;
    }

    if ((start_indx + noexp_len) > ret.length()) {
      return -1;
    }

    noExpMatchString = ret.substring(start_indx, (start_indx + noexp_len));

    if (noExpMatchString.compareTo(noExp) == 0) {
      return start_indx; // start index of matched No Expect String
    } else {
      return -1; //
    }
  }

  // return the index of ch (search from the end of the line )
  // if ( ind ~ end of line) does not have ch, it return -1
  // if this line end with other ch, it return -1
  public static int findEndCharWithinLine(int index, StringBuffer txt, char ch) {
    int line_start_ind = backwardIndexof(index, txt, "\n") + 1;
    int line_end_ind = txt.indexOf("\n", line_start_ind);
    if (line_end_ind == -1) {
      line_end_ind = txt.length() - 1; // last line
    }

    // System.out.println("line_start_ind:"+line_start_ind);
    // System.out.println("line_end_ind:"+line_end_ind);
    // System.out.println("line {"+txt.substring(line_start_ind,
    // line_end_ind)+"}");

    char c;
    int end_ind = line_end_ind - 1;

    while (line_start_ind < end_ind) { // search from end_ind to 'ind'
      c = txt.charAt(end_ind);
      // System.out.print("["+c+"]");

      if (c == ' ' || c == '\t') {
        end_ind--;
      } else if (c == ch) {
        return end_ind;
      } else { // if this line ends with other character
        return -1;
      }
    }
    return -1;
  }

  public static int findStartCharIndexWithinLine(int index, StringBuffer txt, char ch) {
    int line_start_ind = backwardIndexof(index, txt, "\n") + 1;
    int line_end_ind = txt.indexOf("\n", line_start_ind);
    if (line_end_ind == -1) {
      line_end_ind = txt.length() - 1; // last line
    }

    // System.out.println("line ["+txt.substring(line_start_ind,
    // line_end_ind)+"]");

    int ind = line_start_ind;
    char c;

    while (ind < line_end_ind) {
      c = txt.charAt(ind);

      if (c == ' ' || c == '\t') {
        ind++;
        continue;
      } else if (c == ch) {
        return ind;
      } else {
        return -1;
      }

    }
    return -1;
  }

  public static int backwardIndexof(int start_ind, StringBuffer txt, String str) {
    int str_len = str.length();
    int ind = start_ind;

    // System.out.println(str.charAt(str_len - 1));

    while (ind >= (str_len - 1)) {
      if (str.charAt(str_len - 1) == txt.charAt(ind)) {
        // System.out.println("["+txt.substring(ind - str_len + 1, ind+1)+"]");

        if (txt.substring(ind - str_len + 1, ind + 1).compareTo(str) == 0) {
          return (ind - str_len + 1);
        }
      }

      ind--;
    }
    return -1;
  }

  public static int backwardIndexof(int start_ind, String txt, String str) {
    int str_len = str.length();
    int ind = start_ind;

    // System.out.println(str.charAt(str_len - 1));

    if (start_ind < 0) {
      System.out.println("backwardIndexof error:(start_ind=" + start_ind + ")");
      return -1;
    }

    while (ind >= (str_len - 1)) {
      if (str.charAt(str_len - 1) == txt.charAt(ind)) {
        // System.out.println("["+txt.substring(ind - str_len + 1, ind+1)+"]");

        if (txt.substring(ind - str_len + 1, ind + 1).compareTo(str) == 0) {
          return (ind - str_len + 1);
        }
      }

      ind--;
    }
    return -1;
  }

  public static int findNextLineStartIndex(int ind, StringBuffer txt) {
    int next_new_line_ind = txt.indexOf("\n", ind);
    if (next_new_line_ind == -1 || next_new_line_ind == (txt.length() - 1)) {
      // there is no next new line --> last line
      return -1; // return -1
    } else {
      return (next_new_line_ind + 1);
    }
  }

  // return i th token from start_ind
  public static String getToken(int i, String txt, int start_ind, char[] delim) {
    int t_start_ind;
    int t_end_ind;
    int c_ind = start_ind;
    int count = 0;

    while ((t_start_ind = getNextTokenStartIndex(txt, c_ind, delim)) != -1) {
      c_ind = t_start_ind;
      t_end_ind = getTokenEndIndex(txt, c_ind, delim);
      c_ind = t_end_ind + 1;

      if (count == i) {
        return txt.substring(t_start_ind, t_end_ind + 1);
      }
      count++;
    }

    return null;
  }

  public static int getNextTokenStartIndex(String txt, int start_ind, char[] delim) {
    int txt_len = txt.length();
    int c_ind = start_ind;
    char c = 0;
    int i;

    while (c_ind < txt_len) {
      c = txt.charAt(c_ind);

      for (i = 0; i < delim.length; i++) {
        if (c == delim[i]) {
          break;
        }
      }

      if (i == delim.length) {
        return c_ind;
      }

      c_ind++;
    }
    return -1;
  }

  public static int getTokenEndIndex(String txt, int start_ind, char[] delim) {
    int txt_len = txt.length();
    int end_ind = start_ind;
    char c = 0;
    int i = 0;

    while (end_ind < txt_len) {
      c = txt.charAt(end_ind);
      for (i = 0; i < delim.length; i++) {

        if (c == delim[i]) {
          // System.out.println("i="+i);

          if (end_ind == start_ind) {
            return end_ind;
          }

          return (end_ind - 1);
        }
      }

      end_ind++;
    }

    return -1;
  }

  public static int backword_same_char_count(String str, int begin_index, char c) {
    int idx = begin_index;
    int count = 0;

    while (idx >= 0) {
      if (str.charAt(idx) != c) {
        return count;
      }

      count++;
      idx--;
    }
    return count;
  }

  public static String[] splitWithEscape(String str, String esc_ch) {
    int index = -2;

    if (esc_ch.length() != 2) {
      System.err.println("splitWithEscape: esc_ch length should be 2");
      return null;
    }

    for (;;) {
      index = str.indexOf(esc_ch, index + 2); // if search start index is out of
                                              // bound, it will return -1
      if (index < 0) {
        return null;
      }

      if (index > 0 && backword_same_char_count(str, index - 1, '\\') % 2 == 1) {
        continue;
      } else {
        break;
      }
    }

    String[] token = new String[2];

    token[0] = str.substring(0, index);
    token[1] = str.substring(index + 2, str.length());

    return token;
  }

  public static String alternateEscapeSequence(String src, String str_esc_ch, char char_esc_ch) {
    StringBuffer buf = new StringBuffer();
    String target = src;

    // String mid_regex = "[^\\\\]" + regex_esc_ch;

    for (;;) {
      String[] tokens = splitWithEscape(target, str_esc_ch);

      if (tokens == null) {
        // System.out.println("["+target+"]");
        buf.append(target);
        return buf.toString();
      }
      // System.out.println("["+tokens[0]+"]");
      buf.append(tokens[0]);
      buf.append(char_esc_ch);
      target = tokens[1];
    }
  }

  public static String addNewLineWithString(String src, int max_width) {
    char c;
    int len = src.length();
    int cnt = 0;

    if (max_width == 0) {
      return src;
    }

    for (int i = 0; i < len; i++) {
      c = src.charAt(i);
      if (c != '\n' && c != '\r') {
        cnt++;
      }

      if (cnt >= max_width && len > i + 1) {
        src = src.substring(0, i + 1) + "\n" + src.substring(i + 1, len);

        cnt = 0;
        len = src.length();

      }
    }

    return src;
  }

  /*
   * public static String altStrWithChar(String src, String delim, char c) {
   * StringBuffer buf = new StringBuffer(); String target = src;
   *
   * for( ; ; ) { String [] tokens = splitWithDelim(target, delim);
   *
   * if(tokens == null) { //System.out.println("["+target+"]");
   * buf.append(target); return buf.toString(); }
   * //System.out.println("["+tokens[0]+"]"); buf.append(tokens[0]);
   * buf.append(c); target = tokens[1]; } }
   */

  public static void main(String[] args) {
    // String str = "0123456789";
    String str = "012345678901";

    System.out.println("[" + addNewLineWithString(str, 1) + "]");

  }

}
