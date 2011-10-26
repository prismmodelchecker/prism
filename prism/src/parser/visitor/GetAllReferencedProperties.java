package parser.visitor;

import java.util.Vector;

import parser.ast.ExpressionFormula;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProp;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import prism.PrismLangException;

public class GetAllReferencedProperties extends ASTTraverse
{
	private Vector<String> v;
	private ModulesFile mf;
	private PropertiesFile pf;
	
	
	public GetAllReferencedProperties(Vector<String> v, ModulesFile mf, PropertiesFile pf)
	{
		this.v = v;
		this.mf = mf;
		this.pf = pf;
	}
	
	public void visitPost(ExpressionProp e) throws PrismLangException
	{
		if (!v.contains(e.getName())) {
			v.addElement(e.getName());
		}
	}
		
	public void visitPost(ExpressionLabel e) throws PrismLangException
	{
		String name;
		Property prop = null;
		// See if identifier corresponds to a property
		name = e.getName();
		if (mf != null) {
			prop = mf.getPropertyByName(name);
		}
		if (prop == null && pf != null) {
			prop = pf.getPropertyObjectByName(name);
		}
		if (prop != null) {
			// If so, add the name
			v.addElement(e.getName());
		}
	}
}
