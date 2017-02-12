package cc.mallet.types;

public class PolyaUrnDirichletSamplerBuilder extends StandardArgsDirichletBuilder {

	String samplerClassName = "cc.mallet.types.PolyaUrnDirichlet";
		
	@Override
	protected String getSparseDirichletSamplerClassName() {
		return samplerClassName;
	}

}
