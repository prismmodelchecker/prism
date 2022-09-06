//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Vincent Nimal <vincent.nimal@comlab.ox.ac.uk> (University of Oxford)
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

import java.util.Date;

import cern.jet.random.Exponential;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

/**
 * Random number generator for the simulator.
 * Currently, uses the Colt library.
 */
public class RandomNumberGenerator
{
	private MersenneTwister random;
	private Uniform uniform;
	private Exponential exponential;

	/**
	 * Create a new random number generator (seeded, by default, with the current time).
	 */
	public RandomNumberGenerator()
	{
		random = new MersenneTwister(new Date());
		uniform = new Uniform(random);
		// Create exponential generator (rate 1.0 but this is ignored from now on)
		exponential = new Exponential(1.0, random);
	}

	/**
	 * Pick a (uniformly distributed) random integer in the range [0,...,n-1].
	 */
	public int randomUnifInt(int n)
	{
		return uniform.nextIntFromTo(0, n - 1);
	}

	/**
	 * Pick a (uniformly distributed) random double in the range (0,1).
	 */
	public double randomUnifDouble()
	{
		return random.nextDouble();
	}

	/**
	 * Pick a (uniformly distributed) random double in range (0,x).
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
		return exponential.nextDouble(x);
		//return (-Math.log(random.nextDouble())) / x;
	}
}
