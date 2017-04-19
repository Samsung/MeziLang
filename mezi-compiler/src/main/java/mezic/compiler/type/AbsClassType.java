package mezic.compiler.type;

import java.util.List;

import mezic.compiler.CompileException;

public interface AbsClassType {

  public final String CONSTRUCTOR_NAME = "<init>";

  public List<AbsFuncType> getLocalFunctions() throws CompileException;

  public void getLocalFunctions(List<AbsFuncType> list) throws CompileException;

  public AbsFuncType getLocalFunction(String name, AbsTypeList paratype_list) throws CompileException;

  public AbsFuncType getLocalFunctionPoly(String name, AbsTypeList paratype_list, boolean is_polymorphic)
      throws CompileException;

  public AbsFuncType getLocalVarArgFunction(String name, AbsTypeList paratype_list) throws CompileException;

  public AbsFuncType getLocalVarArgFunctionPoly(String name, AbsTypeList paratype_list, boolean is_polymorphic)
      throws CompileException;

  public AbsFuncType getLocalApplyFunction(String name, AbsTypeList paratype_list) throws CompileException;

  public AbsFuncType getLocalConstructor(AbsTypeList paratype_list) throws CompileException;

  public AbsFuncType getLocalConstructorPoly(AbsTypeList paratype_list, boolean is_polymorphic) throws CompileException;

  public AbsFuncType[] getLocalConstructors() throws CompileException;

  public AbsClassType getSuperClass() throws CompileException;

  public List<AbsClassType> getInterfaceList() throws CompileException;

  public boolean isDescendentOf(AbsClassType ancestor) throws CompileException;

  public boolean isInterface() throws CompileException;

  public boolean isFunctionalInterface() throws CompileException;
}
