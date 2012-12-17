package prism;

import sun.awt.image.OffScreenImage;

/**
 * Enumeration that represents one of the operators used in multi-objective
 * verification. Note that strict inequalities are missing as these are not
 * allowed in current implementation of multi-objective.
 */
public enum Operator {
	P_MAX(0), R_MAX(3), P_MIN(5), R_MIN(8), P_GE(2), R_GE(4), P_LE(7), R_LE(9);
	   
	private int intValue = -1;
	
	private Operator(int i)
	{
		this.intValue = i;
	}
	
   	/**
   	 * Returns {@code true} if op is one {@link Operator.P_MIN},
   	 * {@link Operator.R_MIN}, {@link Operator.P_LE}, or {@link Operator.R_LE}.
   	 */
	public static boolean isMinOrLe(Operator op)
	{
		switch (op)
		{
			case P_MIN:
			case R_MIN:
			case P_LE:
			case R_LE:
				return true;
			default:
				return false;
		}
	}
	
	/**
	 * Returns a number representing the current operator. These numbers
	 * are used in the C code.
	 */
	public int toNumber()
	{
		return this.intValue;
	}
}
