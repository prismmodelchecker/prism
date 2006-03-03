//==============================================================================
//
//	Copyright (c) 2004-2005, Andrew Hinton
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

/**
 *
 * @author  Andrew Hinton
 */
public class SimulationInformation
{
    Values initialState;
    double approx;
    double confidence;
    int noIterations;
    int maxPathLength;
	int autoIndex;
	boolean distributed;
    
    public SimulationInformation
    (Values initialState,
    double approx,
    double confidence,
    int noIterations,
    int maxPathLength)
    {
        this.initialState = initialState;
        this.approx = approx;
        this.confidence = confidence;
        this.noIterations = noIterations;
        this.maxPathLength = maxPathLength;

		this.autoIndex = 0;
		
		this.distributed = false;
    }

	public int getAutoIndex()
	{
		return autoIndex;
	}

	public void setAutoIndex(int autoIndex)
	{
		this.autoIndex = autoIndex;
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
    
    /**
     * Getter for property approx.
     * @return Value of property approx.
     */
    public double getApprox()
    {
        return approx;
    }
    
    /**
     * Setter for property approx.
     * @param approx New value of property approx.
     */
    public void setApprox(double approx)
    {
        this.approx = approx;
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
    
    /**
     * Getter for property noIterations.
     * @return Value of property noIterations.
     */
    public int getNoIterations()
    {
        return noIterations;
    }
    
    /**
     * Setter for property noIterations.
     * @param noIterations New value of property noIterations.
     */
    public void setNoIterations(int noIterations)
    {
        this.noIterations = noIterations;
    }
    
    /**
     * Getter for property maxPathLength.
     * @return Value of property maxPathLength.
     */
    public int getMaxPathLength()
    {
        return maxPathLength;
    }
    
    /**
     * Setter for property maxPathLength.
     * @param maxPathLength New value of property maxPathLength.
     */
    public void setMaxPathLength(int maxPathLength)
    {
        this.maxPathLength = maxPathLength;
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
	
}
