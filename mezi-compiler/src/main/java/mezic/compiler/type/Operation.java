package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;
import mezic.compiler.Reduction;
import mezic.parser.LangUnitNode;
import mezic.parser.ParserConstants;

public class Operation extends Reduction {

  private static final long serialVersionUID = 1982486570969317763L;

  private int opcode;

  private LangUnitNode opnode;

  @SuppressWarnings("unused")
  private TContext top_context;

  public Operation(int opCode, LangUnitNode opNode, TContext top_context) throws CompileException {
    if (!isValidOpcode(opCode)) {
      throw new CompileException("Invalid opcode(" + opCode + ")");
    }
    if (opNode == null) {
      throw new CompileException("Invalid opnode(" + opNode + ")");
    }
    if (top_context == null) {
      throw new CompileException("Invalid context(" + top_context + ")");
    }

    this.opcode = opCode;
    this.opnode = opNode;
    this.top_context = top_context;
  }

  private boolean isValidOpcode(int opCode) {
    switch (opCode) {
    /* asignment operation */
    case ParserConstants.ASSIGN:
    case ParserConstants.MULTIPLY_ASSIGN:
    case ParserConstants.DIVISION_ASSIGN:
    case ParserConstants.REST_ASSIGN:
    case ParserConstants.PLUS_ASSIGN:
    case ParserConstants.MINUS_ASSIGN:
    case ParserConstants.SHIFT_LEFT_ASSIGN:
    case ParserConstants.SHIFT_RIGHT_ASSIGN:
    case ParserConstants.AND_ASSIGN:
    case ParserConstants.EXCLU_OR_ASSIGN:
    case ParserConstants.INCLU_OR_ASSIGN:

      /* fundamental operation */
    case ParserConstants.PLUS:
    case ParserConstants.MINUS:
    case ParserConstants.MULTIPLY:
    case ParserConstants.DIVISION:
    case ParserConstants.REST:

    case ParserConstants.INCLU_OR:
    case ParserConstants.EXCLU_OR:
    case ParserConstants.AND:

    case ParserConstants.SHIFT_LEFT:
    case ParserConstants.SHIFT_RIGHT:

      /* relative operation */
    case ParserConstants.EQUAL:
    case ParserConstants.NOT_EQUAL:
    case ParserConstants.SMALLER:
    case ParserConstants.BIGGER:
    case ParserConstants.SMALLER_OR_EQUAL:
    case ParserConstants.BIGGER_OR_EQUAL:

    case ParserConstants.LOG_AND:
    case ParserConstants.LOG_OR:

      /* unary operation */
    case ParserConstants.UNARY_NOT:
    case ParserConstants.UNARY_INVERSE:

      /* etc */
    case ParserConstants.GET:

      return true;
    default:
      return false;
    }
  }

  public int getOpCode() {
    return opcode;
  }

  public String getOpCodeName() {
    return ParserConstants.tokenImage[opcode];
  }

  public LangUnitNode getOpNode() {
    return opnode;
  }

  public String toString() {
    return "Operation(" + getOpCodeName() + ":" + getOpNode() + ")";
  }

  private boolean isObjectOpType(AbsType type) throws CompileException {
    return type.op().getOpString().equals("ObjectOp");
  }

  public void nonassign_dualchild_op_rval_conversion(AbsType lval_type, AbsType rval_type, CompilerLoader cpLoader)
      throws CompileException {
    int num_opnode_child = opnode.jjtGetNumChildren();
    Debug.assertion(num_opnode_child == 2 || num_opnode_child == 3,
        "opnode child num should be 2, but " + opnode.jjtGetNumChildren());

    if (isObjectOpType(lval_type)) { // operator overload case
      // do not rvalue type conversion .. (polymorphism will be applied)
      return;
    }

    // primitive type
    switch (opcode) {
    case ParserConstants.GET:
    case ParserConstants.SHIFT_LEFT:
    case ParserConstants.SHIFT_RIGHT:
    case ParserConstants.SHIFT_LEFT_ASSIGN:
    case ParserConstants.SHIFT_RIGHT_ASSIGN: {
      // op rvalue type should be 'int' for primitive operation
      if (!rval_type.isName(TPrimitiveClass.NAME_INT)) {
        throw new CompileException(
            "rvalue type(" + rval_type + ") should be int for operator " + ParserConstants.tokenImage[opcode]);
      }
      break;
    }
    default: {
      if (cpLoader.isCompatibleClass(rval_type, lval_type)) {
        // do nothing
      } else if (cpLoader.isConvertibleClass(rval_type, lval_type)) {

        LangUnitNode rval_node = null;

        switch (num_opnode_child) {
        case 2:
          rval_node = opnode.getChildren(1);
          break;
        case 3:
          rval_node = opnode.getChildren(2);
          break;
        default:
          throw new CompileException("invalid opnode child num");
        }

        Debug.assertion(rval_node != null, "rval_node should be valid");

        AbsType conv_type = rval_type.op().get_converting_type(lval_type);
        Debug.assertion(conv_type != null, "conv_type should be valid");

        rval_node.setConvertingType(conv_type);

      } else {
        throw new CompileException("lvalue type(" + lval_type + ") operator " + ParserConstants.tokenImage[opcode]
            + " is not defined for rvalue type(" + rval_type + ")");
      }

    }
    } // switch

    return;
  }

  public Container non_assign_op(Container lvalue, Container rvalue, OpInfo opinfo, CompilerLoader cpLoader)
      throws CompileException {
    Container op_rslt = null;

    switch (opcode) {
    case ParserConstants.PLUS: // '+'
      op_rslt = lvalue.op().plus(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.MINUS: // '-'
      op_rslt = lvalue.op().minus(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.MULTIPLY: // '*'
      op_rslt = lvalue.op().multiply(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.DIVISION: // '/'
      op_rslt = lvalue.op().division(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.REST: // '%'
      op_rslt = lvalue.op().rest(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.INCLU_OR: // '|'
      op_rslt = lvalue.op().inc_or(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.EXCLU_OR: // '^'
      op_rslt = lvalue.op().exc_or(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.AND: // '&'
      op_rslt = lvalue.op().and(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.EQUAL: // '=='
      op_rslt = lvalue.op().equal(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.NOT_EQUAL: // '!='
      op_rslt = lvalue.op().not_equal(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.SMALLER: // '<'
      op_rslt = lvalue.op().smaller(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.BIGGER: // '>'
      op_rslt = lvalue.op().bigger(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.SMALLER_OR_EQUAL: // '<='
      op_rslt = lvalue.op().smaller_equal(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.BIGGER_OR_EQUAL: // '>='
      op_rslt = lvalue.op().bigger_equal(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.LOG_AND: // 'AND'
      op_rslt = lvalue.op().logical_and(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.LOG_OR: // 'OR'
      op_rslt = lvalue.op().logical_or(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.SHIFT_LEFT: // '<<'
      op_rslt = lvalue.op().shfit_left(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.SHIFT_RIGHT: // '>>'
      op_rslt = lvalue.op().shfit_right(lvalue, rvalue, opinfo);
      break;
    case ParserConstants.GET:
      op_rslt = lvalue.op().map_access(lvalue, rvalue, opinfo);
      break;
    default:
      throw new CompileException("Not Supported " + toString(), getOpNode());
    }

    return op_rslt;
  }

  public static String opOverloadingFuncName(int opcode) throws CompileException {
    String func_name = null;
    switch (opcode) {
    case ParserConstants.PLUS:
      func_name = "+";
      break;
    case ParserConstants.MINUS:
      func_name = "-";
      break;
    case ParserConstants.MULTIPLY:
      func_name = "*";
      break;
    case ParserConstants.DIVISION:
      func_name = "/";
      break;
    case ParserConstants.REST:
      func_name = "%";
      break;
    case ParserConstants.INCLU_OR:
      func_name = "|";
      break;
    case ParserConstants.EXCLU_OR:
      func_name = "^";
      break;
    case ParserConstants.AND:
      func_name = "&";
      break;
    case ParserConstants.EQUAL:
      func_name = "==";
      break;
    case ParserConstants.NOT_EQUAL:
      func_name = "!=";
      break;
    case ParserConstants.SMALLER:
      func_name = "<";
      break;
    case ParserConstants.BIGGER:
      func_name = ">";
      break;
    case ParserConstants.SMALLER_OR_EQUAL:
      func_name = "<=";
      break;
    case ParserConstants.BIGGER_OR_EQUAL:
      func_name = ">=";
      break;
    case ParserConstants.LOG_AND:
      func_name = "&&";
      break;
    case ParserConstants.LOG_OR:
      func_name = "||";
      break;
    case ParserConstants.SHIFT_LEFT:
      func_name = "<<";
      break;
    case ParserConstants.SHIFT_RIGHT:
      func_name = ">>";
      break;
    case ParserConstants.GET:
      func_name = "[]";
      break;
    case ParserConstants.SET:
      func_name = "[]=";
      break;

    case ParserConstants.MULTIPLY_ASSIGN:
      func_name = "*=";
      break;
    case ParserConstants.DIVISION_ASSIGN:
      func_name = "/=";
      break;
    case ParserConstants.REST_ASSIGN:
      func_name = "%=";
      break;
    case ParserConstants.PLUS_ASSIGN:
      func_name = "+=";
      break;
    case ParserConstants.MINUS_ASSIGN:
      func_name = "-=";
      break;
    case ParserConstants.SHIFT_LEFT_ASSIGN:
      func_name = "<<=";
      break;
    case ParserConstants.SHIFT_RIGHT_ASSIGN:
      func_name = ">>=";
      break;
    case ParserConstants.AND_ASSIGN:
      func_name = "&=";
      break;
    case ParserConstants.EXCLU_OR_ASSIGN:
      func_name = "^=";
      break;
    case ParserConstants.INCLU_OR_ASSIGN:
      func_name = "|=";
      break;

    default:
      throw new CompileException("Not Supported opcode " + opcode);
    }

    return func_name;
  }

  public Container assign_op(Container lvalue, Container rvalue, OpInfo opinfo, CompilerLoader cpLoader)
      throws CompileException {
    Container retCont = null;

    try {
      switch (opcode) {
      case ParserConstants.ASSIGN: // '='

        Debug.assertion(!lvalue.isForm(Container.FORM_OPSTACK_VAR),
            "l_value should not be opstack var(" + lvalue + ")");

        Debug.assertion(opnode != null, "opnode should be valid");
        LangUnitNode assign_node = (LangUnitNode) opnode.jjtGetParent();
        Debug.assertion(assign_node != null, "assign_node should be valid");

        LangUnitNode assign_parent_node = (LangUnitNode) assign_node.jjtGetParent();
        Debug.assertion(assign_parent_node != null, "assign_parent_node should be valid");

        if (assign_parent_node.requiringOpStackRvalue()) {
          Debug.println_dbg("assign_rvalue_dup");
          lvalue.op().assign_rvalue_dup(lvalue, rvalue, opinfo);
          retCont = lvalue.op().assign(lvalue, rvalue, opinfo);
        } else {
          lvalue.op().assign(lvalue, rvalue, opinfo);
          retCont = lvalue;
        }

        break;

      case ParserConstants.MULTIPLY_ASSIGN: // '*='
        retCont = lvalue.op().multiply_assign(lvalue, rvalue, opinfo);
        break;

      case ParserConstants.DIVISION_ASSIGN: // '/='
        retCont = lvalue.op().division_assign(lvalue, rvalue, opinfo);
        break;

      case ParserConstants.REST_ASSIGN: // '%='
        retCont = lvalue.op().rest_assign(lvalue, rvalue, opinfo);
        break;

      case ParserConstants.PLUS_ASSIGN: // '+='
        retCont = lvalue.op().plus_assign(lvalue, rvalue, opinfo);
        break;

      case ParserConstants.MINUS_ASSIGN: // '-='
        retCont = lvalue.op().minus_assign(lvalue, rvalue, opinfo);
        break;

      case ParserConstants.SHIFT_LEFT_ASSIGN: // '<<='
        retCont = lvalue.op().shift_left_assign(lvalue, rvalue, opinfo);
        break;

      case ParserConstants.SHIFT_RIGHT_ASSIGN: // '>>='
        retCont = lvalue.op().shift_right_assign(lvalue, rvalue, opinfo);
        break;

      case ParserConstants.AND_ASSIGN: // '&='
        retCont = lvalue.op().and_assign_assign(lvalue, rvalue, opinfo);
        break;

      case ParserConstants.EXCLU_OR_ASSIGN: // '^='
        retCont = lvalue.op().exclusive_or_assign(lvalue, rvalue, opinfo);
        break;

      case ParserConstants.INCLU_OR_ASSIGN: // '|='
        retCont = lvalue.op().inclusive_or_assign(lvalue, rvalue, opinfo);
        break;

      default:
        throw new CompileException("Not Supported " + toString(), getOpNode());
      }

    } catch (CompileException e) {
      e.printStackTrace();

      throw new CompileException(e.getMessage(), getOpNode());
    }

    return retCont;
  }

  public static boolean is_comparable(int opcode) {
    switch (opcode) {
    case ParserConstants.EQUAL: // '=='
    case ParserConstants.NOT_EQUAL: // '!='
    case ParserConstants.SMALLER: // '<'
    case ParserConstants.BIGGER: // '>'
    case ParserConstants.SMALLER_OR_EQUAL: // '<='
    case ParserConstants.BIGGER_OR_EQUAL: // '>='
    case ParserConstants.LOG_AND: // 'AND'
    case ParserConstants.LOG_OR: // 'OR'
      return true;
    default:
      return false;
    }
  }

  public static boolean is_equality(int opcode) {
    switch (opcode) {
    case ParserConstants.EQUAL: // '=='
    case ParserConstants.NOT_EQUAL: // '!='
      return true;
    default:
      return false;
    }
  }

  public int reduceType() {
    return Reduction.OPERATION;
  }

}
