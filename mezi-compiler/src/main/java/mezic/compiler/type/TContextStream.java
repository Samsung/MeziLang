package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;

public class TContextStream extends TContext {

  private static final long serialVersionUID = -823871847356081004L;

  private int stream_type = -1;

  public TContextStream(TContext parent, String stream_name, OpLvalue type_op, CompilerLoader cpLoader)
      throws CompileException {
    super(parent, AbsType.FORM_STREAM, stream_name, type_op, cpLoader);

  }

  public void setIsDefinitionStream(boolean is_def) {
    if (is_def) {
      stream_type = 1;
    } else {
      stream_type = 0;
    }
  }

  public boolean isDefinitionStream() throws CompileException {

    switch (stream_type) {
    case 1:
      return true;
    case 0:
      return false;
    default:
      throw new CompileException("stream_type is not initialized");
    }
  }

  @Override
  public String getMthdDscStr() throws CompileException {

    if (isDefinitionStream()) {
      throw new CompileException("This method should not be called(except for class & function)");
    } else {
      AbsType stream_type = (AbsType) cpLoader.findClassFull(CompilerLoader.LANGSTREAM_CLASS_NAME);
      return stream_type.getMthdDscStr();
    }

  }

}
