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

package prism;

/**
 * Class to display percentage progress meter to a log.
 */
public class ProgressDisplay
{
	/** Log to display progress to */
	private PrismLog mainLog;
	// Config
	/** Minimum delay between updates (milliseconds); */
	private int delay = 3000;
	/** Display progress as multiples of this number */ 
	private int multiple = 2;
	// Current state
	private int lastPercentageDone;
	private long timerProgress;
	
	public ProgressDisplay(PrismLog mainLog)
	{
		this.mainLog = mainLog;
	}
	
	public void start()
	{
		lastPercentageDone = 0;
		timerProgress = System.currentTimeMillis();
		mainLog.print("[");
	}
	
	public boolean ready()
	{
		return System.currentTimeMillis() - timerProgress > delay;
	}
	
	public void update(double percentageDone)
	{
		// Round percentage down to nearest multiple of 'multiple' 
		int percentageDoneRound = (int) Math.floor(percentageDone);
		percentageDoneRound = (percentageDoneRound / multiple) * multiple;
		// Print if new
		if (percentageDoneRound > lastPercentageDone) {
			lastPercentageDone = percentageDoneRound;
			mainLog.print(" " + percentageDoneRound + "%");
			mainLog.flush();
			timerProgress = System.currentTimeMillis();
		}
	}
	
	public void end()
	{
		mainLog.println(" ]");
	}
}
