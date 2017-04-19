package mezic.compiler;

import java.util.LinkedList;

import mezic.parser.Token;

public class TokenList implements java.io.Serializable {

  private static final long serialVersionUID = -5987475651885447350L;
  private LinkedList<Token> list = null;

  public TokenList() {
    list = new LinkedList<Token>();
  }

  public void add(Token token) {
    list.add(token);
  }

  public Token get(int idx) {
    return list.get(idx);
  }

  public int getLen() {
    return list.size();
  }

  public int append(TokenList append_list) {
    int len = append_list.getLen();
    Token t;

    int i = 0;
    for (; i < len; i++) {
      t = append_list.get(i);

      if (t != null) {
        add(t);
      }
    }

    return i;
  }

  public String toString() {
    int len = getLen();

    Token ele = null;

    StringBuffer buf = new StringBuffer();

    for (int i = 0; i < len; i++) {
      ele = list.get(i);
      buf.append("-");
      buf.append(ele.toString());
    }

    return buf.toString();
  }
}
