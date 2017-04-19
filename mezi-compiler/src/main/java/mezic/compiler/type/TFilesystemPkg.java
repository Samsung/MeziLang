package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;

public class TFilesystemPkg extends AbsType implements AbsPkgType {

  private static final long serialVersionUID = -7816842048350387495L;

  public TFilesystemPkg(String name) throws CompileException {
    super(name);

    this.form = AbsType.FORM_PKG;
  }

  @Override // override Type.getMthdDscStr
  public String getMthdDscStr() throws CompileException {
    throw new CompileException("This method is not supported");
  }

  @Override // override Type.getLocalChildVariable
  public Container getLocalChildVariable(String name) throws CompileException {
    throw new CompileException("Package can not have local child variable");
  }

  @Override // override TypePackage.getChildClass
  public AbsClassType getChildClass(String name) throws CompileException {
    throw new CompileException("This method should not be used.");
    // return null;
  }

  @Override // override TypePackage.getChildPackage
  public AbsPkgType getChildPackage(String full_name, CompilerLoader cpLoader) throws CompileException {

    AbsPkgType pkg_type = (AbsPkgType) cpLoader.findPackage(full_name);
    return pkg_type;
  }

  @Override
  public String toString() {
    return "FileSystemPkg(" + getName() + ")";
  }

}
