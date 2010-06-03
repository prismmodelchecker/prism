//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vojtech Forejt <forejt@fi.muni.cz> (Masaryk University)
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

import prism.PrismException;

public final class FoxGlynn
{
	// constructor parameters
	private double underflow, overflow, accuracy;
	private double q_tmax;

	// returned values
	private int left, right;
	private double totalWeight;
	private double[] weights;

	public FoxGlynn(double qtmax, double uf, double of, double acc) throws PrismException
	{
		q_tmax = qtmax;
		underflow = uf;
		overflow = of;
		accuracy = acc;
		run();
	}

	public final double[] getWeights()
	{
		return weights;
	}

	public final int getLeftTruncationPoint()
	{
		return left;
	}

	public final int getRightTruncationPoint()
	{
		return right;
	}

	public final double getTotalWeight()
	{
		return totalWeight;
	}

	private final void run() throws PrismException
	{
		if (q_tmax == 0.0) {
			throw new PrismException("Overflow: TA parameter qtmax = time * maxExitRate = 0.");
		}
		else if (q_tmax < 400)
		{ //here naive approach should have better performance than Fox Glynn
			final double expcoef = Math.exp(-q_tmax); //the "e^-lambda" part of p.m.f. of Poisson dist.
			int k; //denotes that we work with event "k steps occur"
			double lastval; //(probability that exactly k events occur)/expcoef
			double accum; //(probability that 0 to k events occur)/expcoef
			double desval = (1-(accuracy/2.0)) / expcoef; //value that we want to accumulate in accum before we stop
			java.util.Vector<Double> w = new java.util.Vector<Double>(); //stores weights computed so far.
			
			//k=0 is simple
			lastval = 1;
			accum = lastval;
			w.add(lastval * expcoef);
			
			//add further steps until you have accumulated enough
			k = 1;
			do {
				lastval *= q_tmax / k; // invariant: lastval = q_tmax^k / k!
				accum += lastval;
				w.add(lastval * expcoef);
				k++;
			} while (accum < desval);

			//store all data
			this.left=0;
			this.right=k-1;
			this.weights = new double[k];

			for(int i = 0; i < w.size(); i++)
			{
				this.weights[i] = w.get(i);			
			}

			//we return actual weights, so no reweighting should be done
			this.totalWeight = 1.0;
		}
		else
		{ //use actual Fox Glynn for q_tmax>400
			if (accuracy < 1e-10) {
				throw new PrismException("Overflow: Accuracy is smaller than Fox Glynn can handle (must be at least 1e-10).");
			}
			final double factor = 1e+10; //factor from the paper, it has no real explanation there
			final int m = (int) q_tmax; //mode
			//run FINDER to get left, right and weight[m]
			{
				final double sqrtpi = 1.7724538509055160; //square root of PI
				final double sqrt2 = 1.4142135623730950; //square root of 2
				final double sqrtq = Math.sqrt(q_tmax);
				final double aq = (1.0 + 1.0/q_tmax) * Math.exp(0.0625) * sqrt2; //a_\lambda from the paper			
				final double bq = (1.0 + 1.0/q_tmax) * Math.exp(0.125/q_tmax); //b_\lambda from the paper

				//use Corollary 1 to find right truncation point
				final double lower_k_1 = 1.0 / (2.0*sqrt2*q_tmax); //lower bound on k from Corollary 1
				final double upper_k_1 = sqrtq / (2.0*sqrt2); //upper bound on k from Corollary 1
				double k;

				//justification for increment is in the paper:
				//"increase k through the positive integers greater than 3"
				for(k=lower_k_1; k <= upper_k_1;
					k=(k==lower_k_1)? k+4 : k+1 )
				{
					double dkl = 1.0/(1 - Math.exp(-(2.0/9.0)*(k*sqrt2*sqrtq+1.5))); //d(k,\lambda) from the paper
					double res = aq*dkl*Math.exp(-k*k/2.0)/(k*sqrt2*sqrtpi); //right hand side of the equation in Corollary 1
					if (res <= accuracy/2.0)
					{
						break;
					}
				}

				if (k>upper_k_1)
					k=upper_k_1;

				this.right = (int) Math.ceil(m+k*sqrt2*sqrtq + 1.5); 

				//use Corollary 2 to find left truncation point
				//NOTE: the original implementation used some upper bound on k,
				//      however, I didn't find it in the paper and I think it is not needed
				final double lower_k_2 = 1.0/(sqrt2*sqrtq); //lower bound on k from Corollary 2

				double res;
				k=lower_k_2;
				do
				{
					res = bq*Math.exp(-k*k/2.0)/(k*sqrt2*sqrtpi); //right hand side of the equation in Corollary 2
					k++;			
				}
				while (res > accuracy/2.0);
				
				this.left = (int) (m - k*sqrtq - 1.5);
				
				//According to the paper, we should check underflow of lower bound.
				//However, it seems that for no reasonable values this can happen.
				//And neither the original implementation checked it
				
				double wm = overflow / (factor*(this.right - this.left));

				this.weights = new double[this.right-this.left+1];
				this.weights[m-this.left] = wm;
			}
			//end of FINDER

			//compute weights
			//(at this point this.left, this.right and this.weight[m] is known)
			
			//Down from m
			for(int j=m; j>this.left; j--)
				this.weights[j-1-this.left] = (j/q_tmax)*this.weights[j-this.left];
			//Up from m
			for(int j=m; j<this.right; j++)
				this.weights[j+1-this.left] = (q_tmax/(j+1))*this.weights[j-this.left];

			//Compute totalWeight (i.e. W in the paper)
			//instead of summing from left to right, start from smallest
			//and go to highest weights to prevent roundoff
			this.totalWeight = 0.0;
			int s = this.left;
			int t = this.right;
			while (s<t)
			{
				if(this.weights[s - this.left] <= this.weights[t - this.left])
				{
					this.totalWeight += this.weights[s-this.left];
					s++;
				}
				else
				{
					this.totalWeight += this.weights[t-this.left];
					t--;
				}
			}
			this.totalWeight += this.weights[s-this.left];
		}
	}

	public static void test()
	{
		double[] weights;
		double totalWeight = 0.0;
		int left, right;

		FoxGlynn w = null;
		try {
			// q = maxDiagRate, time = time parameter (a U<time b)
			double q = 1, time = 1;
			w = new FoxGlynn(q * time, 1.0e-300, 1.0e+300, 1.0e-6);
		} catch (PrismException e) {
			// ...
		}
		weights = w.getWeights();
		left = w.getLeftTruncationPoint();
		right = w.getRightTruncationPoint();
		totalWeight = w.getTotalWeight();
		w = null;
	}

}
