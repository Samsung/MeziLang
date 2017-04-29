package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;

public interface AbsPkgType {

  public AbsClassType getChildClass(String name) throws CompileException;

  public AbsPkgType getChildPackage(String name, CompilerLoader cpLoader) throws CompileException;

}
