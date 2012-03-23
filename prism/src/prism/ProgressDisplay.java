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
 * Class to display progress meter to a log, either as percentages or just a counter.
 */
public class ProgressDisplay
{
	/** Log to display progress to */
	private PrismLog mainLog;
	// Config
	/** Minimum delay between updates (milliseconds); */
	private int delay = 3000;
	/** Display progress as multiples of this number */
	private int percentMultiple = 2;
	// Current state
	private long totalCount;
	private long lastCount;
	private long lastPercentageDone;
	private long timerProgress;
	private boolean first;

	public ProgressDisplay(PrismLog mainLog)
	{
		this.mainLog = mainLog;
	}

	/**
	 * Initialise; start the timer.
	 */
	public void start()
	{
		totalCount = -1; // i.e. not used
		lastCount = 0;
		lastPercentageDone = 0;
		timerProgress = System.currentTimeMillis();
		first = true;
	}

	/**
	 * Set total count expected, thus triggering percentage mode.
	 */
	public void setTotalCount(long totalCount)
	{
		this.totalCount = totalCount;
	}

	/**
	 * Is it time for the next update?
	 */
	public boolean ready()
	{
		return System.currentTimeMillis() - timerProgress > delay;
	}

	/**
	 * Display an update, if it is ready and anything changed.
	 */
	public void updateIfReady(long count)
	{
		if (ready())
			update(count);
	}
	
	/**
	 * Display an update, if anything changed.
	 */
	public void update(long count)
	{
		// Percentage mode
		if (totalCount != -1) {
			// Compute percentage, round down to nearest multiple of 'multiple'
			int percentageDoneRound;
			if (count >= totalCount) {
				percentageDoneRound = 100;
			} else {
				percentageDoneRound = (int) Math.floor((100.0 * count) / totalCount);
				percentageDoneRound = (percentageDoneRound / percentMultiple) * percentMultiple;
			}
			// Print if new
			if (percentageDoneRound > lastPercentageDone) {
				if (first) {
					mainLog.print("[");
					first = false;
				}
				lastPercentageDone = percentageDoneRound;
				mainLog.print(" " + percentageDoneRound + "%");
				mainLog.flush();
				timerProgress = System.currentTimeMillis();
			}
		}
		// Counter mode
		else {
			// Print if new
			if (count > lastCount) {
				lastCount = count;
				mainLog.print(" " + count);
				mainLog.flush();
				timerProgress = System.currentTimeMillis();
			}
		}
	}

	/**
	 * Finish up.
	 */
	public void end()
	{
		end("");
	}

	/**
	 * Finish up, displaying {@code text} first.
	 */
	public void end(String text)
	{
		mainLog.print(text);
		if (totalCount != -1)
			mainLog.print(" ]");
		mainLog.println();
	}
}
