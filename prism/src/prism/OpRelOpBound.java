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

	public boolean isProbabilistic()
	{
		return "P".equals(op);
	}

	public boolean isReward()
	{
		return "R".equals(op);
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

	public boolean isQualitative()
	{
		return !isNumeric() && op.equals("P") && (bound == 0 || bound == 1);
	}

	public boolean isTriviallyTrue()
	{
		if (!isNumeric() && op.equals("P")) {
			// >=0
			if (bound == 0 && relOp == RelOp.GEQ)
				return true;
			// <=1
			if (bound == 1 && relOp == RelOp.LEQ)
				return true;
		}
		return false;
	}

	public boolean isTriviallyFalse()
	{
		if (!isNumeric() && op.equals("P")) {
			// <0
			if (bound == 0 && relOp == RelOp.LT)
				return true;
			// >1
			if (bound == 1 && relOp == RelOp.GT)
				return true;
		}
		return false;
	}

	public MinMax getMinMax(ModelType modelType) throws PrismException
	{
		MinMax minMax = MinMax.blank();
		if (modelType.nondeterministic()) {
			if (relOp == RelOp.EQ && isNumeric()) {
				throw new PrismException("Can't use \"" + op + "=?\" for nondeterministic models; use e.g. \"" + op + "min=?\" or \"" + op + "max=?\"");
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

	public String relOpBoundString()
	{
		return relOp.toString() + bound;
	}

	@Override
	public String toString()
	{
		return op + relOp.toString() + bound;
	}
}
