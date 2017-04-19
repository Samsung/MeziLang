package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import org.objectweb.asm.Opcodes;

public class OpObjectArray extends OpObject {

  private static final long serialVersionUID = -7568242921822615219L;

  public OpObjectArray(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "ObjectArrayOp";
  }

  @Override
  public Container map_create(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    AbsType map_type = lval.getType();
    Debug.assertion(map_type != null, "map_type should be initialized");

    AbsType map_ele_type = ((TMapType) map_type).getElementType();
    Debug.assertion(map_ele_type != null, "map_type should be initialized");

    if (map_ele_type.isName(TPrimitiveClass.NAME_INT)) {
      Debug.println_info("NEWARRAY T_INT");
      opinfo.mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_CHAR)) {
      Debug.println_info("NEWARRAY T_CHAR");
      opinfo.mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_CHAR);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_SHORT)) {
      Debug.println_info("NEWARRAY T_SHORT");
      opinfo.mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_SHORT);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_BYTE)) {
      Debug.println_info("NEWARRAY T_BYTE");
      opinfo.mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_BOOL)) {
      Debug.println_info("NEWARRAY T_BOOLEAN");
      opinfo.mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_LONG)) {
      Debug.println_info("NEWARRAY T_LONG");
      opinfo.mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_FLOAT)) {
      Debug.println_info("NEWARRAY T_FLOAT");
      opinfo.mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_DOUBLE)) {
      Debug.println_info("NEWARRAY T_DOUBLE");
      opinfo.mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE);
    } else {
      if (map_ele_type instanceof TMapType) {
        Debug.println_info("Opcodes.ANEWARRAY " + map_ele_type.getMthdDscStr());
        opinfo.mv.visitTypeInsn(Opcodes.ANEWARRAY, map_ele_type.getMthdDscStr());
      } else {
        Debug.println_info("Opcodes.ANEWARRAY " + map_ele_type.getName());
        opinfo.mv.visitTypeInsn(Opcodes.ANEWARRAY, map_ele_type.getName());
      }
    }

    Container anony_map = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    anony_map.initializeType(map_type);
    anony_map.setAssigned(true);

    return anony_map;
  }

  @Override
  public Container map_access(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    AbsType map_type = lval.getType();
    Debug.assertion(map_type != null, "map_type should be initialized");

    AbsType map_ele_type = ((TMapType) map_type).getElementType();
    Debug.assertion(map_ele_type != null, "map_type should be initialized");

    if (map_ele_type.isName(TPrimitiveClass.NAME_INT)) {
      Debug.println_info("IALOAD");
      opinfo.mv.visitInsn(Opcodes.IALOAD);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_BOOL)) {
      Debug.println_info("BALOAD");
      opinfo.mv.visitInsn(Opcodes.BALOAD);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_CHAR)) {
      Debug.println_info("CALOAD");
      opinfo.mv.visitInsn(Opcodes.CALOAD);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_SHORT)) {
      Debug.println_info("SALOAD");
      opinfo.mv.visitInsn(Opcodes.SALOAD);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_BYTE)) {
      Debug.println_info("BALOAD");
      opinfo.mv.visitInsn(Opcodes.BALOAD);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_LONG)) {
      Debug.println_info("LALOAD");
      opinfo.mv.visitInsn(Opcodes.LALOAD);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_FLOAT)) {
      Debug.println_info("FALOAD");
      opinfo.mv.visitInsn(Opcodes.FALOAD);
    } else if (map_ele_type.isName(TPrimitiveClass.NAME_DOUBLE)) {
      Debug.println_info("DALOAD");
      opinfo.mv.visitInsn(Opcodes.DALOAD);
    } else {
      Debug.println_info("AALOAD");
      opinfo.mv.visitInsn(Opcodes.AALOAD);
    }

    Container map_ele_cont = new Container("map_element", Container.FORM_MAPELEMENT_VAR, true, false);
    map_ele_cont.initializeType(map_ele_type);
    map_ele_cont.initOwnerContainer(lval);

    Container opstack_ele = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    opstack_ele.initializeType(map_ele_type);
    opstack_ele.setAssigned(true);
    opstack_ele.initOwnerContainer(map_ele_cont);

    return opstack_ele;
  }

  @Override
  public AbsType get_return_type_of_single_op(int opcode, AbsType lval_type, TContext top_context)
      throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public AbsType get_return_type_of_nonassign_dualchild_op(int opcode, AbsType lval_type, AbsType rval_type,
      TContext top_context) throws CompileException {
    Debug.assertion(lval_type instanceof TMapType, "lval_type should be instance of TMapType");

    AbsType map_ele_type = ((TMapType) lval_type).getElementType();
    Debug.assertion(map_ele_type != null, "map_ele_type should be valid");

    return map_ele_type;
  }

  @Override
  public boolean is_compatible_type(AbsType tgttype) throws CompileException {
    if (tgttype.isName("java/lang/Object")) {
      return true;
    }

    return false;
  }

  @Override
  public AbsType get_converting_type(AbsType tgttype) throws CompileException {
    if (tgttype.isName("java/lang/String")) {
      return (AbsType) cpLoader.findClassFull("java/lang/String");
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container lval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName("java/lang/String")) {
      Debug.println_info("Array ->java/lang/String");
      return string_type_convert(lval, opinfo);
    } else {
      throw new CompileException("Not Supported target type");
    }
  }

}
