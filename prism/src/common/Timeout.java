//==============================================================================
//	
//	Copyright (c) 2015-
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
 * Simple timeout mechanism.
 * After the specified number of seconds have elapsed (measured by
 * System.currentTimeMillis()), a Runnable is invoked.
 * Checks once per second.
 */
public class Timeout implements Runnable
{
	/** The time after which the timeout is expired. */
	private final long timeoutTime;
	/** The Runnable to call after the timeout has expired. */
	private final Runnable timeoutRunnable;

	/**
	 * Set a timeout.
	 * Starts a (daemon) thread which checks if the timeout has elapsed.
	 */
	public static void setTimeout(int timeoutInSeconds, Runnable timeoutRunnable) {
		Timeout t = new Timeout(timeoutInSeconds, timeoutRunnable);
		Thread timeoutThread = new Thread(t);
		// the thread should be a daemon thread, because otherwise
		// the VM will not exit before the timeout is expired...
		timeoutThread.setDaemon(true);
		timeoutThread.start();
	}

	/**
	 * Constructor.
	 */
	private Timeout(int timeoutInSeconds, Runnable timeoutRunnable)
	{
		timeoutTime = System.currentTimeMillis() + timeoutInSeconds*1000;
		this.timeoutRunnable = timeoutRunnable;
	}

	@Override
	public void run()
	{
		while (true) {
			long current = System.currentTimeMillis();
			if (current > timeoutTime) {
				// Timeout is expired, run Runnable
				timeoutRunnable.run();
				return;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
	}
}
