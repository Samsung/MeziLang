package mezic.java.lang;

import mezic.compiler.Container;
import mezic.compiler.CompileException;
import mezic.compiler.type.OpLvalue;

public interface OpOverload {

	public OpLvalue getOp(Container lvalue) throws CompileException;

}
