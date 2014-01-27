//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//  * Frits Dannenberg <frits.dannenberg@cs.ox.ac.uk> (University of Oxford)
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

import java.util.ArrayList;
import prism.PrismException;

/**
 * Class to efficiently compute transient probabilities of a birth process.
 *
 * @author Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
 * @author Frits Dannenberg <frits.dannenberg@cs.ox.ac.uk> (University of Oxford)
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk>  (University of Oxford)
 */
public class BirthProcess
{
	/* uniformisation rate to compute probabilities.
	 * Must be at least as large as largest rate. */
	double unifRate;
	/* precision of computations using Fox-Glynn algorithm */
	double epsilon;
	/* time to compute probabilities for */
	double time;
	/* values used to compute probability to be in a given stage */
	double[] probs;
	/* same as above */
	double[] newProbs;
	/* whether rates added by calculateNextRate will be stored */
	boolean withRateArray;
	ArrayList<Double> jumpRates;
	boolean initialising;
	/* current birth process stage */
	int stageNr;
	/* whether to try to avoid birth process computations in
	 * case all rates are the same
	 */
	boolean avoidBirthComputation; 

	/**
	 * Construct birth process.
	 */
	public BirthProcess()
	{
		stageNr = 0;
		epsilon = 1E-7;
		withRateArray = true;
		initialising = true;
		avoidBirthComputation = true;
	}

	/**
	 * Sets whether rates added by calculateNextRate will be stored.
	 * The default is to do so. Setting it to false allows to save space,
	 * but will not allow rates larger than @a unifRate to be added.
	 *
	 * @param withRateArray whether to store rates in array
	 */
	public void setWithRateArray(boolean withRateArray)
	{
		if (!initialising) {
			throw new IllegalArgumentException("this method might not be called after calculateNextRate");
		}
		this.withRateArray = withRateArray;
	}

	/**
	 * Sets the time to compute probabilities for.
	 * 
	 * @param time time to compute probabilities for
	 */
	public void setTime(double time)
	{
		if (!initialising) {
			throw new IllegalArgumentException("this method might not be called after calculateNextRate");
		}
		if (time < 0.0) {
			throw new IllegalArgumentException("time must be nonnegative");
		}
		this.time = time;
	}

	/**
	 * Sets precision to be used to compute probabilities.
	 * 
	 * @param epsilon precision to be used to compute probabilities
	 */
	public void setEpsilon(double epsilon)
	{
		if (!initialising) {
			throw new IllegalArgumentException("this method might not be called after calculateNextRate");
		}

		this.epsilon = epsilon;
	}

	/**
	 * Chooses whether to try to avoid birth process construction.
	 * In case all rates provided to calculateNextProb are the same, it is
	 * possible to use the Fox-Glynn algorithm to compute probabilities in
	 * the birth process, which is then a Poisson process. If delayBirthComputation
	 * is true, the more expensive computations will only be computed in case
	 * this is necessary. Thus, if calculateNextProb is called with the same
	 * rate all the time, computations are significantly faster. 
	 * 
	 * @param avoidBirthComputation true iff birth process construction shall be delayed
	 */
	public void setAvoidBirthComputation(boolean avoidBirthComputation)
	{
		if (!initialising) {
			throw new IllegalArgumentException("this method might not be called after calculateNextRate");
		}
		this.avoidBirthComputation = avoidBirthComputation;
	}

	/**
	 * Computes probability to reside in next process stage at given time.
	 * If this is the nth call to the process, computes the probability
	 * to reside in the nth stage of the birth process. The rates of the
	 * birth process up to the n-1th stage have been set by previous calls
	 * to the function, the nth rate is set by the current call. The time
	 * probabilities are computed for must have been set previously by
	 * setTime.
	 * 
	 * @param rate leaving rate of current state
	 * @return probability to reside in current stage at given time
	 * @throws PrismException
	 */
	public double calculateNextProb(double rate) throws PrismException
	{
		if (initialising && 0.0 == unifRate && !withRateArray) {
			throw new IllegalArgumentException("unifRate must be set if withRateArray is false");
		}
		if (withRateArray && initialising) {
			jumpRates = new ArrayList<Double>();
		}
		initialising = false;
		if (!withRateArray && rate > unifRate) {
			throw new IllegalArgumentException("cannot use rates larger than initial rate if withRateArray is false");
		}
		if (withRateArray) {
			jumpRates.add(rate);
		}
		boolean recompute = false;
		if (rate > unifRate) {
			if (!avoidBirthComputation) {
				recompute = true;
				unifRate = rate * 1.25 * 1.02;
			} else {
				unifRate = rate;
			}
		}
		if ((jumpRates.size() != 1) && (Math.abs(rate - jumpRates.get(jumpRates.size() - 2)) > 1E-100)) {
			if (avoidBirthComputation) {
				recompute = true;
			}
			avoidBirthComputation = false;
		}
		
		if (null == probs || recompute) {
			initPoisson();
		}
		double result = 0.0;
		if (recompute) {
			for (stageNr = 0; stageNr < jumpRates.size(); stageNr++) {
				result = compNextStageProb(jumpRates.get(stageNr));
			}
		} else {
			if (avoidBirthComputation) {
			    result = (stageNr < probs.length) ? probs[stageNr] : 0.0;
				stageNr++;
			} else {
				result = compNextStageProb(rate);
				stageNr++;
			}
		}

		return result;
	}

	private double compNextStageProb(double rate)
	{
		assert(rate > 0.0);
		double prob = rate / unifRate; // p = r / q
		double omprob = 1.0 - prob; // 1-p
		double result = 0.0;

		double omprobtti = 1.0; // (1-p)^i
		for (int i = 0; i < probs.length; i++) {
			result += omprobtti * probs[i];
			omprobtti *= omprob;
		}

		newProbs[newProbs.length - 1] = 0.0;
		for (int i = probs.length - 1; i >= 1; i--) {
			newProbs[i - 1] = newProbs[i] * omprob + probs[i] * prob;
		}
		double[] temp = probs;
		probs = newProbs;
		newProbs = temp;
		return result;
	}

	/**
	 * Initialises probability vectors by Poisson probabilities.
	 */
	private void initPoisson() throws PrismException
	{
		long left, right;
		double qt = unifRate * time;
		double acc = epsilon / 8.0;
		double[] weights;
		double totalWeight;
		if (unifRate * time == 0.0) {
			left = 0;
			right = 0;
			totalWeight = 1.0;
			weights = new double[1];
			weights[0] = 1.0;
		} else {
			FoxGlynn fg = new FoxGlynn(qt, 1e-300, 1e+300, acc);
			left = fg.getLeftTruncationPoint();
			right = fg.getRightTruncationPoint();
			if (right < 0 || right == Integer.MAX_VALUE) {
				throw new PrismException("Overflow in Fox-Glynn computation (time bound too big?)");
			}
			weights = fg.getWeights();
			totalWeight = fg.getTotalWeight();
		}
		for (long i = left; i <= right; i++) {
			weights[(int) (i - left)] /= totalWeight;
		}
		probs = new double[(int) (right + 1)];
		newProbs = new double[(int) (right + 1)];
		for (long entryNr = left; entryNr <= right; entryNr++) {
			probs[(int) entryNr] = weights[(int) (entryNr - left)];
		}
	}
}
