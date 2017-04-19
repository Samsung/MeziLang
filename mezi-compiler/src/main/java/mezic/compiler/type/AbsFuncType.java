package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;

public interface AbsFuncType {

  public boolean is_constructor();

  public boolean is_static();

  public boolean has_inst_body();

  public boolean is_abstract();

  public int get_access() throws CompileException;

  public boolean is_apply() throws CompileException;

  public AbsType getReturnType(CompilerLoader cpLoader) throws CompileException;

  public FuncSignatureDesc getFuncSignatureDesc() throws CompileException;

  public AbsTypeList getThrowsList() throws CompileException;
}
