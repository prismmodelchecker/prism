//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Vincent Nimal <vincent.nimal@comlab.ox.ac.uk> (University of Oxford)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package userinterface;

import parser.*;
import prism.PrismSettings;
import simulator.method.*;

public class SimulationInformation
{
	// PRISM settings object
	private PrismSettings settings;

	// Simulation method
	public enum Method { CI, ACI, APMC, SPRT };
	private Method method;
	// Unknown variable
	public enum Unknown { WIDTH, CONFIDENCE, NUM_SAMPLES };
	private Unknown unknown;

	// Settings
	private Values initialState;


	private double width;
	private double confidence;
	private int numSamples;
	private long maxPathLength;

	private boolean distributed;
	private boolean maxRewardGiven;

	/**
	 * Create a new SimulationInformation object.
	 * Default values for simulator settings are extracted from PrismSettings.
	 * PrismSettings object is retained for other settings needed later. 
	 */
	public SimulationInformation(PrismSettings settings)
	{
		this.settings = settings;

		this.initialState = null;

		this.method = Method.CI;
		this.unknown = Unknown.WIDTH;
		this.width = settings.getDouble(PrismSettings.SIMULATOR_DEFAULT_WIDTH);
		this.confidence = settings.getDouble(PrismSettings.SIMULATOR_DEFAULT_CONFIDENCE);
		this.numSamples = settings.getInteger(PrismSettings.SIMULATOR_DEFAULT_NUM_SAMPLES);
		this.maxPathLength = settings.getLong(PrismSettings.SIMULATOR_DEFAULT_MAX_PATH);


		this.distributed = false;

		this.maxRewardGiven = false;
	}

	public void setMethod(Method method)
	{
		this.method = method;
	}

	public Method getMethod()
	{
		return method;
	}

	public void setMethodByName(String name)
	{
		setMethod(Method.valueOf(name));
	}

	public String getMethodName()
	{
		return method.toString();
	}

	public void setUnknown(Unknown unknown)
	{
		this.unknown = unknown;
	}

	public void setUnknownByName(String name)
	{
		if (name.equals("Width") || name.equals("Approximation")) {
			setUnknown(Unknown.WIDTH);
		} else if  (name.equals("Confidence")) {
			setUnknown(Unknown.CONFIDENCE);
		} else if (name.equals("Number of samples")) {
			setUnknown(Unknown.NUM_SAMPLES);
		}
	}

	public String getUnknownName()
	{
		switch (unknown) {
		case WIDTH:
			return (method == Method.APMC) ? "Approximation" : "Width";
		case CONFIDENCE:
			return "Confidence";
		case NUM_SAMPLES:
			return "Number of samples";
		}
		return null;
	}

	public Unknown getUnknown()
	{
		return unknown;
	}
	
	/**
	 * Getter for property initialState.
	 * @return Value of property initialState.
	 */
	public parser.Values getInitialState()
	{
		return initialState;
	}

	/**
	 * Setter for property initialState.
	 * @param initialState New value of property initialState.
	 */
	public void setInitialState(parser.Values initialState)
	{
		this.initialState = initialState;
	}

	public double getWidth()
	{
		return width;
	}

	public void setWidth(double width)
	{
		this.width = width;
	}

	/**
	 * Getter for property confidence.
	 * @return Value of property confidence.
	 */
	public double getConfidence()
	{
		return confidence;
	}

	/**
	 * Setter for property confidence.
	 * @param confidence New value of property confidence.
	 */
	public void setConfidence(double confidence)
	{
		this.confidence = confidence;
	}

	public int getNumSamples()
	{
		return numSamples;
	}

	public void setNumSamples(int numSamples)
	{
		this.numSamples = numSamples;
	}

	/**
	 * Getter for property maxPathLength.
	 * @return Value of property maxPathLength.
	 */
	public long getMaxPathLength()
	{
		return maxPathLength;
	}

	/**
	 * Setter for property maxPathLength.
	 * @param maxPathLength New value of property maxPathLength.
	 */
	public void setMaxPathLength(long maxPathLength)
	{
		this.maxPathLength = maxPathLength;
	}

	public void setPropReward(boolean b)
	{
		this.maxRewardGiven = b;
	}

	/**
	 * Getter for property distributed.
	 * @return Value of property distributed.
	 */
	public boolean isDistributed()
	{
		return distributed;
	}

	/**
	 * Setter for property distributed.
	 * @param distributed New value of property distributed.
	 */
	public void setDistributed(boolean distributed)
	{
		this.distributed = distributed;
	}

	/**
	 * create and return the selected method of simulation
	 * @return the method of simulation
	 */
	public SimulationMethod createSimulationMethod()
	{
		// Extract some simulator options, in case needed, from PRISM settings
		double maxReward = settings.getDouble(PrismSettings.SIMULATOR_MAX_REWARD);
		boolean numberToDecideGiven = settings.getBoolean(PrismSettings.SIMULATOR_DECIDE);
		int numberToDecide = settings.getInteger(PrismSettings.SIMULATOR_ITERATIONS_TO_DECIDE);

		switch (method) {
		case CI:
			switch (unknown) {
			case WIDTH:
				return new CIwidth(confidence, numSamples);
			case CONFIDENCE:
				return new CIconfidence(width, numSamples);
			case NUM_SAMPLES:
				if (numberToDecideGiven) {
					return new CIiterations(confidence, width, numberToDecide);
				} else {
					if (maxRewardGiven)
						return new CIiterations(confidence, width, maxReward);
					else
						return new CIiterations(confidence, width);
				}
			default:
				return null;
			}
		case ACI:
			switch (unknown) {
			case WIDTH:
				return new ACIwidth(confidence, numSamples);
			case CONFIDENCE:
				return new ACIconfidence(width, numSamples);
			case NUM_SAMPLES:
				if (numberToDecideGiven) {
					return new ACIiterations(confidence, width, numberToDecide);
				} else {
					if (maxRewardGiven)
						return new ACIiterations(confidence, width, maxReward);
					else
						return new ACIiterations(confidence, width);
				}
			default:
				return null;
			}
		case APMC:
			switch (unknown) {
			case WIDTH:
				return new APMCapproximation(confidence, numSamples);
			case CONFIDENCE:
				return new APMCconfidence(width, numSamples);
			case NUM_SAMPLES:
				return new APMCiterations(confidence, width);
			default:
				return null;
			}
		case SPRT:
			return new SPRTMethod(confidence, confidence, width);
		default:
			return null;
		}
	}
}