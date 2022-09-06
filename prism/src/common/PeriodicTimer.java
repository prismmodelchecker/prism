//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package common;

/**
 * This class keeps time, returning {@code true} for the method {@code triggered()}
 * when another period of {@code period} milliseconds has elapsed.
 * <br>
 * The time is only checked when {@code triggered} is called, so if there
 * can be long delays between calls to that method, intermediate periods are ignored.
 * <br>
 * This class is thus most helpful for "print some update at least after 5 seconds" style
 * jobs.
 */
public class PeriodicTimer
{
	/** The period */
	long period;
	/** The time when the timer was started */
	long start;
	/** The time when the last period was detected */
	long startOfCurrentPeriod;

	/** Constructor, provide the period in milliseconds, e.g., 5000 = 5 seconds */
	public PeriodicTimer(long periodMillis)
	{
		this.period = periodMillis;
		this.start = -1;
		this.startOfCurrentPeriod = -1;
	}

	/** Start the timer */
	public void start()
	{
		start = System.currentTimeMillis();
		startOfCurrentPeriod = start;
	}

	/**
	 * Returns true if at least 'period' milliseconds have
	 * elapsed since the last time that this method has
	 * returned {@code true}.
	 */
	public boolean triggered()
	{
		long now = System.currentTimeMillis();
		if (now - startOfCurrentPeriod > period) {
			startOfCurrentPeriod = now;
			return true;
		}
		return false;
	}

	/** Get the number of milliseconds since {@code start()} was called */
	public long elapsedMillisTotal()
	{
		long now = System.currentTimeMillis();
		return now - start;
	}

	/**
	 * Get the number of milliseconds since {@code triggered()} has returned {@code true}
	 * the last time
	 */
	public long elapsedMillisInThisPeriod()
	{
		long now = System.currentTimeMillis();
		return now - startOfCurrentPeriod;
	}

}
