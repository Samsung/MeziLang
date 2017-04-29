package mezic.compiler.type;

import java.util.LinkedList;

import mezic.compiler.CompileException;
import mezic.parser.LangUnitNode;

public class TraverseStackNode implements java.io.Serializable {

  private static final long serialVersionUID = 3825986649168198387L;

  private LangUnitNode node;

  private int current_child_idx;

  public TraverseStackNode(LangUnitNode node) {
    this.node = node;
    this.current_child_idx = 0;
  }

  public LangUnitNode getNode() {
    return this.node;
  }

  public TraverseStackNode getNextChild() throws CompileException {
    int num_child = node.jjtGetNumChildren();

    if (current_child_idx < num_child) {
      return allocNode((LangUnitNode) node.getChildren(current_child_idx++));
    } else {
      return null;
    }
  }

  public int schedulingNextChildTraverse(int index) {
    current_child_idx = index;
    return current_child_idx;
  }

  public int schedulingPassNextChildTraverse(int offset) {
    current_child_idx += offset;
    return current_child_idx;
  }

  public int schedulingEndChildTraverse() {
    current_child_idx = node.jjtGetNumChildren();
    return current_child_idx;
  }

  public static TraverseStackNode allocNode(LangUnitNode node) throws CompileException {
    TraverseStackNode frame = null;

    if (node == null) {
      throw new CompileException("allFrame node is inavalid");
    }

    frame = new TraverseStackNode(node);

    return frame;
  }

  public static void push(LinkedList<TraverseStackNode> stack, TraverseStackNode node) throws CompileException {
    if (stack == null) {
      throw new CompileException("frame stack is not initialized");
    }

    stack.push(node);
  }

  public static TraverseStackNode pop(LinkedList<TraverseStackNode> stack) throws CompileException {
    if (stack == null) {
      throw new CompileException("frame stack is not initialized");
    }

    if (stack.size() == 0) {
      return null;
    }

    return (TraverseStackNode) stack.pop();
  }

  public static TraverseStackNode getTop(LinkedList<TraverseStackNode> stack) throws CompileException {
    if (stack == null) {
      throw new CompileException("frame stack is not initialized");
    }

    if (stack.size() == 0) {
      return null;
    }

    return stack.get(0);
  }

  public static void dump_stack(LinkedList<TraverseStackNode> stack, String prefix) {
    int size = stack.size();
    System.out.println(prefix + "lasting traverse stack size (" + size + ")");
    for (int i = 0; i < size; i++) {
      System.out.println(prefix + "(" + (size - i - 1) + ") " + stack.get(i));
    }
  }

  public String toString() {
    return node.toString() + " child idx(" + current_child_idx + "/" + node.jjtGetNumChildren() + ")";
  }

}
