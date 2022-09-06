//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
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

import java.util.function.Function;
import java.util.function.Supplier;

import prism.PrismLog;

/**
 * Stop watch for keeping track of the runtime of some computation,
 * optionally printing the elapsed time to the log after the computation
 * has stopped.
 * <br>
 * Example usage:
 * <pre>
 * StopWatch timer = new StopWatch(log);
 * timer.start("model checking");
 * // ... do the actual computation ...
 * timer.stop();
 * </pre>
 * would result in the output
 * <pre>
 * Time for model checking 42.00 seconds.
 * </pre>
 * <br>
 * Stopping via
 * <pre>
 * timer.stop("result was xyz");
 * </pre>
 * results in
 * <pre>
 * Time for model checking 42.00 seconds, result was xyz.
 * </pre>
 */
public class StopWatch
{
	/** The (optional) log */
	protected PrismLog log;

	/** An (optional) task description */
	protected String taskDescription;

	/** For storing the time */
	protected long time = 0;

	/** Is watch running? */
	protected boolean running = false;

	/** Constructor, no log and no output */
	public StopWatch()
	{
	}

	/** Constructor, stores log for output */
	public StopWatch(PrismLog log)
	{
		this.log = log;
	}

	/** Start the stop watch (without task description) */
	public StopWatch start()
	{
		return start(null);
	}

	/** Start the stop watch, store task description (may be {@code null}) */
	public StopWatch start(String taskDescription)
	{
		this.taskDescription = taskDescription;
		running = true;
		time = System.currentTimeMillis();
		return this;
	}

	/**
	 * Stop the stop watch.
	 * If a task description and a log was given, output elapsed time.
	 * @return elapsed time in milliseconds
	 */
	public long stop()
	{
		return stop(null);
	}

	/**
	 * Stop the stop watch, optionally taking extra text for output.
	 * If a log and a task description / extra text was given, output
	 * elapsed time.
	 * <br>
	 * Extra text is output as "... xx.yy seconds, extra-text."
	 * @param extraText extra text to output (optional, ignored if {@code null})
	 */
	public long stop(String extraText)
	{
		time = System.currentTimeMillis() - time;
		running = false;
		if (log != null) {
			if (taskDescription != null) {
				log.print("Time for " + taskDescription + ": " + elapsedSeconds() + " seconds");
				if (extraText != null) {
					log.print(", " + extraText);
				}
				log.println(".");
			} else if (extraText != null) {
				log.println("Time: " + elapsedSeconds() + " seconds, " + extraText + ".");
			}
		}
		return time;
	}

	/** Get the number of elapsed milliseconds (fixed value after having called stop). */
	public long elapsedMillis()
	{
		return running ? System.currentTimeMillis() - time : time;
	}

	/** Get the number of elapsed seconds (fixed value after having called stop). */
	public double elapsedSeconds()
	{
		return elapsedMillis() / 1000.0;
	}

	/**
	 * Stop the execution time of a task.
	 *
	 * @return time in milliseconds
	 **/
	public long run(Runnable task)
	{
		return run(task, null, null);
	}

	/**
	 * Stop the execution time of a task.
	 *
	 * @param taskDescription description or {@code null})
	 * @param extraText text or {@code null}
	 * @return time in milliseconds
	 **/
	public long run(Runnable task, String taskDescription, String extraText)
	{
		start(taskDescription);
		task.run();
		return stop(extraText);
	}

	/**
	 * Stop the execution time of a task and return the result.
	 * Time is available via {@code elapsedMillis()} and {@code elapsedSeconds()}.
	 *
	 * @return task result
	 **/
	public <T> T run(Supplier<T> task)
	{
		return run(task, null, (String) null);
	}

	/**
	 * Stop the execution time of a task and return the result.
	 * Time is available via {@code elapsedMillis()} and {@code elapsedSeconds()}.
	 *
	 * @param taskDescription description or {@code null}
	 * @param extraText text or {@code null}
	 * @return task result
	 **/
	public <T> T run(Supplier<T> task, String taskDescription, String extraText)
	{
		start(taskDescription);
		T result = task.get();
		stop(extraText);
		return result;
	}

	/**
	 * Stop the execution time of a task and return the result.
	 * Time is available via {@code elapsedMillis()} and {@code elapsedSeconds()}.
	 *
	 * @param taskDescription description or {@code null}
	 * @param resultDescription function that provides a description or {@code null}
	 * @return task result
	 **/
	public <T> T run(Supplier<T> task, String taskDescription, Function<? super T, String> resultDescription)
	{
		start(taskDescription);
		T result = task.get();
		stop(resultDescription.apply(result));
		return result;
	}
}
