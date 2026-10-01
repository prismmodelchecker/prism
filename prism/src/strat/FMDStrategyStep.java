//==============================================================================
//	
//	Copyright (c) 2022-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

package strat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import explicit.ConstructInducedModel;
import explicit.ConstructStrategyProduct;
import explicit.Model;
import explicit.NondetModel;
import parser.State;
import prism.PrismException;
import prism.PrismLog;

/**
 * Class to store finite-memory deterministic (FMD) strategies
 * giving a different choice for each state across k steps of execution.
 * So the memory is simply the number of elapsed steps (up to k).
 * Optionally, a memoryless "tail" strategy gives the choices after k steps;
 * otherwise, choices after k steps are arbitrary.
 */
public class FMDStrategyStep<Value> extends StrategyExplicit<Value>
{
	// Model size
	private int numStates;
	// Max number of steps considered
	private int k;
	// Choices for each state, for each step 0...k-1
	private ArrayList<StepChoices> choices;
	// Optional memoryless strategy for after k steps
	private MDStrategy<Value> tail = null;
	
	/**
	 * Create a blank FMDStrategyStep for a specified model and maximum step count.
	 */
	public FMDStrategyStep(NondetModel<Value> model, int k)
	{
		super(model);
		numStates = model.getNumStates();
		this.k = k;
		choices = new ArrayList<>(numStates);
		for (int s = 0; s < numStates; s++) {
			choices.add(new StepChoicesArray(k));
		}
	}

	/**
	 * Set the choice to be taken in state s at step i,
	 * specified by the index of the choice in the corresponding model.
	 */
	public void setStepChoice(int s, int i, int ch)
	{
		choices.get(s).setChoiceForStep(i, ch);
	}
	
	/**
	 * Set the choices to be taken in all states at step i,
	 * specified by the indices of the choices in the corresponding model.
	 */
	public void setStepChoices(int i, int ch[])
	{
		for (int s = 0; s < numStates; s++) {
			choices.get(s).setChoiceForStep(i, ch[s]);
		}
	}
	
	/**
	 * Get the maximum number of steps k for which choices are stored.
	 */
	public int getNumSteps()
	{
		return k;
	}

	/**
	 * Set the choices to be taken in all states at steps offset...offset+k'-1
	 * to be those of another FMDStrategyStep {@code other} (with step bound k')
	 * at steps 0...k'-1.
	 */
	public void setStepChoicesFrom(FMDStrategyStep<?> other, int offset)
	{
		for (int s = 0; s < numStates; s++) {
			for (int i = 0; i < other.k; i++) {
				choices.get(s).setChoiceForStep(offset + i, other.getChoiceIndex(s, i));
			}
		}
	}

	/**
	 * Set a (memoryless) strategy for the choices to be taken after k steps.
	 */
	public void setTail(MDStrategy<Value> tail)
	{
		this.tail = tail;
	}

	@Override
	public Memory memory()
	{
		return Memory.FINITE;
	}
	
	@Override
	public Object getChoiceAction(int s, int m)
	{
		// After k steps, use the tail strategy, if present
		if (m >= k && tail != null) {
			return tail.getChoiceAction(s);
		}
		int c = getChoiceIndex(s, m);
		return c >= 0 ? model.getAction(s, c) : Strategy.UNDEFINED;
	}
	
	@Override
	public int getChoiceIndex(int s, int m)
	{
		// After k steps, use the tail strategy, if present
		if (m >= k) {
			return tail != null ? tail.getChoiceIndex(s) : -1;
		}
		return choices.get(s).getChoiceForStep(m);
	}

	@Override
	public UndefinedReason whyUndefined(int s, int m)
	{
		// After k steps, use the tail strategy, if present; otherwise, any choice is fine
		if (m >= k) {
			return tail != null ? tail.whyUndefined(s, -1) : UndefinedReason.ARBITRARY;
		}
		switch (getChoiceIndex(s, m)) {
		case -1:
			return UndefinedReason.UNKNOWN;
		case -2:
			return UndefinedReason.ARBITRARY;
		case -3:
			return UndefinedReason.UNREACHABLE;
		default:
			return null;
		}
	}

	@Override
	public int getMemorySize()
	{
		return k + 1;
	}
	
	@Override
	public int getInitialMemory(int sInit)
	{
		// Step count initially zero
		return 0;
	}
	
	@Override
	public int getUpdatedMemory(int m, Object action, int sNext)
	{
		// Step count increases by 1 each time (up to k)
		return m >= k ? k : m + 1;
	}

	@Override
	public explicit.Model<Value> constructInducedModel(StrategyExportOptions options) throws PrismException
	{
		ConstructStrategyProduct csp = new ConstructStrategyProduct(options);
		Model<Value> prodModel = csp.constructProductModel(model, this);
		return prodModel;
	}

	@Override
	public void exportActions(PrismLog out, StrategyExportOptions options)
	{
		List<State> states = model.getStatesList();
		boolean showStates = options.getShowStates() && states != null;
		for (int s = 0; s < numStates; s++) {
			for (int m = 0; m < (tail != null ? k + 1 : k); m++) {
				if (isChoiceDefined(s, m)) {
					out.println((showStates ? states.get(s) : s) + "," + m + "=" + getChoiceActionString(s, m));
				}
			}
		}
	}

	@Override
	public void exportIndices(PrismLog out, StrategyExportOptions options)
	{
		for (int s = 0; s < numStates; s++) {
			for (int m = 0; m < (tail != null ? k + 1 : k); m++) {
				if (isChoiceDefined(s, m)) {
					out.println(s + "," + m + "=" + getChoiceIndex(s, m));
				}
			}
		}
	}

	@Override
	public void clear()
	{
		choices = null;
	}

	@Override
	public String toString()
	{
		return "[" + IntStream.range(0, getNumStates())
				.mapToObj(s -> IntStream.range(0, tail != null ? k + 1 : k)
				.mapToObj(m -> s + "," + m + "=" + getChoiceActionString(s, m))
				.collect(Collectors.joining(",")))
				.collect(Collectors.joining(",")) + "]";
	}

	// Classes to store the choice for each step (0...k-1) in one state
	
	/**
	 * Abstract class to store the choice for each step (0...k-1) in one state
	 */
	abstract class StepChoices
	{
		/**
		 * Set the choice for a given step
		 */
		abstract void setChoiceForStep(int step, int ch);
		
		/**
		 * Get the choice for a given step
		 */
		abstract int getChoiceForStep(int step);
	}
	
	/**
	 * Simple implementation of {@link StepChoices},
	 * just storing choice indices in an array of size k
	 */
	class StepChoicesArray extends StepChoices
	{
		int stepChoices[];
		
		StepChoicesArray(int stepBound)
		{
			stepChoices = new int[stepBound];
			// All unknown (-1) initially
			for (int i = 0; i < stepBound; i++) {
				stepChoices[i] = -1;
			}
		}
		
		@Override
		void setChoiceForStep(int step, int ch)
		{
			stepChoices[step] = ch;
		}
		
		@Override
		int getChoiceForStep(int step)
		{
			return stepChoices[step];
		}
	}
}
