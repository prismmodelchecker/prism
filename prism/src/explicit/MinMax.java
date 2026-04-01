//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit;

/**
 * Class to store info about types of probabilities that are to be computed
 * (typically how to quantify over strategies, e.g. "min" or "max"). 
 */
public class MinMax
{
	/** How players are quantified over? **/
	protected int numPlayers;
	/** Is quantification over (epistemic) uncertainty included? */
	protected boolean unc;

	/**
	 * Create a blank MinMax object
	 */
	public MinMax()
	{
	}

	/**
	 * Copy constructor
	 */
	public MinMax(MinMax other)
	{
		this.numPlayers = other.numPlayers;
		this.unc = other.unc;
		this.min = other.min;
		this.min1 = other.min1;
		this.min2 = other.min2;
		this.minUnc = other.minUnc;
	}

	// Info about quantification over a single class of strategies/adversaries

	/** Min (true) or max (false) over strategies? */
	protected boolean min;
	
	public MinMax setMin(boolean min)
	{
		numPlayers = 1;
		this.min = min;
		return this;
	}
	
	public boolean isMin()
	{
		return min;
	}
	
	public boolean isMax()
	{
		return !min;
	}
	
	// Info about quantification over a two classes of strategies (e.g. for 2-player games)

	/** Min (true) or max (false) over strategies for player 1? */
	protected boolean min1;
	/** Min (true) or max (false) over strategies for player 2? */
	protected boolean min2;
	
	public MinMax setMinMin(boolean min1, boolean min2)
	{
		numPlayers = 2;
		this.min1 = min1;
		this.min2 = min2;
		return this;
	}
	
	public boolean isMin1()
	{
		return min1;
	}
	
	public boolean isMin2()
	{
		return min2;
	}
	
	// Additional info about quantification over uncertainty

	/** Min (true) or max (false) over epistemic uncertainty? */
	protected boolean minUnc;
	
	public MinMax setMinUnc(boolean minUnc)
	{
		unc = true;
		this.minUnc = minUnc;
		return this;
	}
	
	public boolean isMinUnc()
	{
		return minUnc;
	}
	
	public boolean isMaxUnc()
	{
		return !minUnc;
	}
	
	// Create a new instance by applying some operation
	
	public MinMax negate()
	{
		MinMax neg = new MinMax();
		if (numPlayers == 1) {
			neg.setMin(!isMin());
		} else if (numPlayers == 2) {
			neg.setMinMin(!isMin1(), !isMin2());
		}
		if (unc) {
			neg.setMinUnc(!isMinUnc());
		}
		return neg;
	}
	
	// Utility methods to create instances of this class
	
	public static MinMax blank()
	{
		return new MinMax();
	}
	
	public static MinMax min()
	{
		return new MinMax().setMin(true);
	}
	
	public static MinMax max()
	{
		return new MinMax().setMin(false);
	}

	@Override
	public String toString()
	{
		String s = "";
		if (numPlayers == 1) {
			s += min ? "min" : "max";
		} else if (numPlayers == 2) {
			s += min1 ? "min" : "max";
			s += min2 ? "min" : "max";
		}
		if (unc) {
			s += minUnc ? "min" : "max";
		}
		return s;
	}
}
