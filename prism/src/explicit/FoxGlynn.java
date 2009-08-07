//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Joachim Meyer-Kayser <Joachim.Meyer-Kayser@informatik.uni-erlangen.de> (University of Erlangen)
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
		int m = (int) Math.floor(q_tmax);
		double q = find(m); // get q, left and right

		//Log.entry("Weighter: Right: " + right);
		//Log.entry("Weighter: Left : " + left);

		weights = new double[right - left + 1];
		weights[m - left] = q;
		//Log.logArray("Weights before calculation: ",weights);

		// down
		for (int j = m; j > left; j--) {
			weights[j - 1 - left] = (j / q_tmax) * weights[j - left];
		}

		//up
		if (q_tmax < 400) {

			if (right > 600) {
				throw new PrismException("Overflow: right truncation point > 600.");
			}

			for (int j = m; j < right;) {
				q = q_tmax / (j + 1);

				if (weights[j - left] > underflow / q) {
					weights[j + 1 - left] = q * weights[j - left];
					j++;
				} else {
					right = j;
					//Log.entry("Weighter: Right is now set to " + right);
					computeTotalWeight();
					return;
				}
			}

		} else {
			for (int j = m; j < right; j++) {
				weights[j + 1 - left] = (q_tmax / (j + 1)) * weights[j - left];
			}
		}
		computeTotalWeight();
	}

	private final void computeTotalWeight()
	{
		int l = left;
		int r = right;
		totalWeight = 0.0;

		while (l < r) {
			if (weights[l - left] <= weights[r - left]) {
				totalWeight += weights[l - left];
				++l;
			} else {
				totalWeight += weights[r - left];
				--r;
			}
		}
		totalWeight += weights[l - left];
	}

	private final double find(double m) throws PrismException
	{
		double k;

		if (q_tmax == 0.0)
			throw new PrismException("Overflow: TA parameter qtmax = time * maxExitRate = 0.");

		if (q_tmax < 25.0)
			left = 0;

		if (q_tmax < 400.0) {
			// Find right using Corollary 1 with q_tmax=400
			double sqrt2 = Math.sqrt(2.0);
			double sqrtl = 20;
			double a = 1.0025 * Math.exp(0.0625) * sqrt2;
			double b = 1.0025 * Math.exp(0.125 / 400); //Math.exp (0.0003125)
			double startk = 1.0 / (2.0 * sqrt2 * 400);
			double stopk = sqrtl / (2 * sqrt2);

			for (k = startk; k <= stopk; k += 3.0) {
				double d = 1.0 / (1 - Math.exp((-2.0 / 9.0) * (k * sqrt2 * sqrtl + 1.5)));
				double f = a * d * Math.exp(-0.5 * k * k) / (k * Math.sqrt(2.0 * Math.PI));

				if (f <= accuracy / 2.0)
					break;
			}

			if (k > stopk)
				k = stopk;

			right = (int) Math.ceil(m + k * sqrt2 * sqrtl + 1.5);
		}

		if (q_tmax >= 400.0) {
			// Find right using Corollary 1 using actual q_tmax 
			double sqrt2 = Math.sqrt(2.0);
			double sqrtl = Math.sqrt(q_tmax);
			double a = (1.0 + 1.0 / q_tmax) * Math.exp(0.0625) * sqrt2;
			double b = (1.0 + 1.0 / q_tmax) * Math.exp(0.125 / q_tmax);
			double startk = 1.0 / (2.0 * sqrt2 * q_tmax);
			double stopk = sqrtl / (2 * sqrt2);

			for (k = startk; k <= stopk; k += 3.0) {
				double d = 1.0 / (1 - Math.exp((-2.0 / 9.0) * (k * sqrt2 * sqrtl + 1.5)));
				double f = a * d * Math.exp(-0.5 * k * k) / (k * Math.sqrt(2.0 * Math.PI));

				if (f <= accuracy / 2.0)
					break;
			}

			if (k > stopk)
				k = stopk;

			right = (int) Math.ceil(m + k * sqrt2 * sqrtl + 1.5);
		}

		if (q_tmax >= 25.0) {
			// Find left using Corollary 2 using actual q_tmax 
			double sqrt2 = Math.sqrt(2.0);
			double sqrtl = Math.sqrt(q_tmax);
			double a = (1.0 + 1.0 / q_tmax) * Math.exp(0.0625) * sqrt2;
			double b = (1.0 + 1.0 / q_tmax) * Math.exp(0.125 / q_tmax);
			double startk = 1.0 / (sqrt2 * sqrtl);
			double stopk = (m - 1.5) / (sqrt2 * sqrtl);

			for (k = startk; k <= stopk; k += 3.0) {
				if (b * Math.exp(-0.5 * k * k) / (k * Math.sqrt(2.0 * Math.PI)) <= accuracy / 2.0)
					break;
			}

			if (k > stopk)
				k = stopk;

			left = (int) Math.floor(m - k * sqrtl - 1.5);
		}

		if (left < 0) {
			left = 0;
			System.out.println("Weighter: negative left truncation point found. Ignored.");
		}

		return overflow / (Math.pow(10.0, 10.0) * (right - left));
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
