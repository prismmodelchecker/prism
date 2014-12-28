package prism;

import parser.ast.RelOp;
import explicit.MinMax;

/**
 * Class to represent info (operator, relational operator, bound, etc.) found in a P/R/S operator.
 */
public class OpRelOpBound
{
	protected String op;
	protected RelOp relOp;
	protected boolean numeric;
	protected double bound;
	
	public OpRelOpBound(String op, RelOp relOp, Double boundObject)
	{
		this.op = op;
		this.relOp = relOp;
		numeric = (boundObject == null);
		if (boundObject != null)
			bound = boundObject.doubleValue();
	}
	
	public RelOp getRelOp()
	{
		return relOp;
	}
	
	public boolean isNumeric()
	{
		return numeric; 
	}
	
	public double getBound()
	{
		return bound;
	}
	
	public MinMax getMinMax(ModelType modelType) throws PrismException
	{
		MinMax minMax = MinMax.blank();
		if (modelType.nondeterministic()) {
			if (relOp == RelOp.EQ && isNumeric()) {
				throw new PrismException("Can't use \""+op+"=?\" for nondeterministic models; use e.g. \""+op+"min=?\" or \""+op+"max=?\"");
			}
			if (modelType == ModelType.MDP || modelType == ModelType.CTMDP) {
				minMax = (relOp.isLowerBound() || relOp.isMin()) ? MinMax.min() : MinMax.max();
			} else {
				throw new PrismException("Don't know how to model check " + getTypeOfOperator() + " properties for " + modelType + "s");
			}
		}
		return minMax;
	}
	
	public String getTypeOfOperator()
	{
		String s = "";
		s += op + relOp;
		s += isNumeric() ? "?" : "p"; // TODO: always "p"?
		return s;
	}
	
	@Override
	public String toString()
	{
		return relOp.toString() + bound;
	}
}
