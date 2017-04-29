package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;

public class TContextPkg extends TContext implements AbsPkgType {

  private static final long serialVersionUID = -4547423754249668290L;

  public TContextPkg(String pkg_name, CompilerLoader cpLoader) throws CompileException {
    super(null, AbsType.FORM_PKG, pkg_name, null, cpLoader); // Package does not
                                                             // have LvalOp
  }

  public TContext getChildContextTu(String tu_name) throws CompileException {
    int idx = getChildContextIndex(tu_name, AbsType.FORM_TU, 0);

    if (idx == -1) {
      return null;
    }

    return (TContextPkg) getChildContext(idx);
  }

  @Override
  public AbsPkgType getChildPackage(String full_name, CompilerLoader cpLoader) throws CompileException {
    // search from building context
    int idx = getChildContextIndex(full_name, AbsType.FORM_PKG, 0);

    if (idx != -1) {
      return (AbsPkgType) getChildContext(idx);
    }

    return null;
  }

  @Override
  public AbsClassType getChildClass(String full_name) throws CompileException {
    int len = childcontext_list.size();

    TContext tu_ctx = null;
    TContextClass class_ctx = null;
    for (int i = 0; i < len; i++) {
      tu_ctx = childcontext_list.get(i);

      class_ctx = (TContextClass) tu_ctx.getLocalChildContext(AbsType.FORM_CLASS, full_name);

      if (class_ctx != null) {
        return class_ctx;
      }
    }

    return null;
  }

}
