//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Vincent Nimal <vincent.nimal@comlab.ox.ac.uk> (University of Oxford)
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

package simulator.method;

import parser.ast.Expression;
import prism.PrismException;
import simulator.sampler.Sampler;

/**
 * Classes to control simulation-based (approximate/statistical) model checking.
 */
public abstract class SimulationMethod implements Cloneable
{
	/**
	 * Get the (short) name of this method.
	 */
	public abstract String getName();
	
	/**
	 * Get the (full) name of this method.
	 */
	public abstract String getFullName();
	
	/**
	 * Reset the status of this method (but not values of any parameters that have
	 * been set). Typically called if this instance is going to be re-used for
	 * another run of approximate verification. 
	 */
	public abstract void reset();

	/**
	 * Compute the missing parameter (typically there are multiple parameters,
	 * one of which can be left free) *before* simulation. This is not always
	 * possible - sometimes the parameter cannot be determined until afterwards.
	 * This method may get called multiple times.
	 * @throws PrismException if the parameters set so far are invalid.
	 */
	public abstract void computeMissingParameterBeforeSim() throws PrismException;

	/**
	 * Set the Expression (property) that simulation is going to be used to approximate.
	 * It will be either an ExpressionProb or ExpressionReward object.
	 * All constants should have already been defined and replaced.
	 * @throws PrismException if property is inappropriate somehow for this method.
	 */
	public abstract void setExpression(Expression expr) throws PrismException;
	
	/**
	 * Compute the missing parameter (typically there are multiple parameters,
	 * one of which can be left free) *after* simulation. (Sometimes the parameter
	 * cannot be determined before simulation.)
	 * This method may get called multiple times. 
	 */
	public abstract void computeMissingParameterAfterSim();

	/**
	 * Get the missing (computed) parameter. If it has not already been computed and
	 * this can be done *before* simulation, calling this method will trigger its computation.
	 * If it can only be done *after*, an exception is thrown.
	 * @return the computed missing parameter (as an Integer or Double object)
	 * @throws PrismException if missing parameter is not and cannot be computed yet
	 * or if the parameters set so far are invalid.
	 */
	public abstract Object getMissingParameter() throws PrismException;

	/**
	 * Get the parameters of the simulation (including the computed one) as a string.
	 */
	public abstract String getParametersString();

	/**
	 * Determine whether or not simulation should stop now, based on the stopping
	 * criteria of this method, and the current state of simulation (the number of
	 * iterations so far and the corresponding Sampler object).
	 * Note: This method may continue being called after 'true' is returned,
	 * e.g. if multiple properties are being simulated simultaneously.
	 * @param iters The number of iterations (samples) done so far
	 * @param sampler The Sampler object for this simulation
	 * @return true if the simulation should stop, false otherwise
	 */
	public abstract boolean shouldStopNow(int iters, Sampler sampler);

	/**
	 * Get an indication of progress so far for simulation, i.e. an approximate value
	 * for the percentage of work (samples) done. The value is a multiple of 10 in the range [0,100].
	 * This estimate may not be linear (e.g. for CI/ACI where 'iterations' is computed).
	 * It is assumed that this method is called *after* the call to shouldStopNow(...).
	 * Note: The iteration count may exceed that dictated by this method,
	 * e.g. if multiple properties are being simulated simultaneously.
	 * TODO: check methods for this
	 * @param iters The number of iterations (samples) done so far
	 * @param sampler The Sampler object for this simulation
	 */
	public abstract int getProgress(int iters, Sampler sampler);

	/**
	 * Get the (approximate) result for the property that simulation is being used to approximate.
	 * This should be a Boolean/Double for bounded/quantitative properties, respectively.
	 * @param sampler The Sampler object for this simulation
	 * @throws PrismException if we can't get a result for some reason.
	 */
	public abstract Object getResult(Sampler sampler) throws PrismException;
	
	/**
	 * Get an explanation for the result of the simulation as a string.
	 * @param sampler The Sampler object for this simulation (e.g. to get mean)
	 * @throws PrismException if we can't get a result for some reason.
	 */
	public abstract String getResultExplanation(Sampler sampler) throws PrismException;
	
	@Override
	public abstract SimulationMethod clone();
	
}
