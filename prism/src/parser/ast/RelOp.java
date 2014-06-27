package parser.ast;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class to represent a relational operator (or similar) found in a P/R/S operator.
 */
public enum RelOp {

	GT, GEQ, MIN, LEQ, LT, MAX, EQ;

	protected static Map<RelOp, String> symbols;
	static {
		symbols = new HashMap<RelOp, String>();
		symbols.put(RelOp.GT, ">");
		symbols.put(RelOp.GEQ, ">=");
		symbols.put(RelOp.MIN, "min=");
		symbols.put(RelOp.LT, "<");
		symbols.put(RelOp.LEQ, "<=");
		symbols.put(RelOp.MAX, "max=");
		symbols.put(RelOp.EQ, "=");
	}

	@Override
	public String toString()
	{
		return symbols.get(this);
	}

	/**
	 * Returns true if this corresponds to a lower bound (i.e. >, >=).
	 * NB: "min=?" does not return true for this.
	 */
	public boolean isLowerBound()
	{
		switch (this) {
		case GT:
		case GEQ:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Returns true if this corresponds to an upper bound (i.e. <, <=).
	 * NB: "max=?" does not return true for this.
	 */
	public boolean isUpperBound()
	{
		switch (this) {
		case LT:
		case LEQ:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Returns true if this is a strict bound (i.e. < or >).
	 */
	public boolean isStrict()
	{
		switch (this) {
		case GT:
		case LT:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Returns true if this corresponds to minimum (min=?).
	 */
	public boolean isMin()
	{
		switch (this) {
		case MIN:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Returns true if this corresponds to maximum (max=?).
	 */
	public boolean isMax()
	{
		switch (this) {
		case MAX:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Returns the RelOp object corresponding to a (string) symbol,
	 * e.g. parseSymbol("<=") returns RelOp.LEQ. Returns null if invalid.
	 * @param symbol The symbol to look up
	 * @return
	 */
	public static RelOp parseSymbol(String symbol)
	{
		Iterator<Entry<RelOp, String>> it = symbols.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<RelOp, String> e = it.next();
			if (e.getValue().equals(symbol))
				return e.getKey();
		}
		return null;
	}
}
