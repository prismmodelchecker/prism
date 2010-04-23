//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package simulator;

import java.util.Random;

public class RandomNumberGenerator
{
	private Random random; 
	
	public RandomNumberGenerator()
	{
		random = new Random();
	}
	
	/**
	 * Pick a (uniformly) random integer in range [0,...,n-1].
	 */
	public int randomUnifInt(int n)
	{
		return random.nextInt(n);
	}
	
	/**
	 * Pick a (uniformly) random double in range [0,1).
	 */
	public double randomUnifDouble()
	{
		return random.nextDouble();
	}
	
	/**
	 * Pick a (uniformly) random double in range [0,x).
	 */
	public double randomUnifDouble(double x)
	{
		return x * random.nextDouble();
	}
	
	/**
	 * Pick a random double according to exponential distribution with rate x.
	 */
	public double randomExpDouble(double x)
	{
		double y;
		// Pick (non-zero) random y in (0,1)
		while ((y = random.nextDouble()) == 0);
		// Sample exponential
		return (-Math.log(y)) / x;
	}
}
