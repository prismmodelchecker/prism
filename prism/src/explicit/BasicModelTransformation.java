//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
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

package explicit;

import java.util.BitSet;
import java.util.function.IntFunction;

import common.IterableBitSet;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.PrismException;

/**
 * Base class for model transformations, providing support for mapping between states.
 */
public class BasicModelTransformation<OM extends Model, TM extends Model> implements ModelTransformation<OM, TM>
{
	public static final IntFunction<Integer> IDENTITY = Integer::valueOf;

	public static final boolean DEFAULT_BOOLEAN = false;
	public static final double DEFAULT_DOUBLE   = Double.NaN;
	public static final int DEFAULT_INTEGER     = -1;

	protected final OM originalModel;
	protected final TM transformedModel;
	protected final IntFunction<Integer> mapToTransformedModel;

	protected final int numberOfStates;

	/** Constructor for a model transformation that maps states one-on-one. */
	public BasicModelTransformation(final OM originalModel, final TM transformedModel)
	{
		this(originalModel, transformedModel, IDENTITY);
	}

	/**
	 * Constructor for a model transformation where the state mapping is given
	 * by a function
	 */
	public BasicModelTransformation(final OM originalModel, final TM transformedModel, final IntFunction<Integer> mapToTransformedModel)
	{
		this.originalModel               = originalModel;
		this.transformedModel            = transformedModel;
		this.numberOfStates              = originalModel.getNumStates();
		this.mapToTransformedModel       = mapToTransformedModel;
	}

	@Override
	public OM getOriginalModel()
	{
		return originalModel;
	}

	@Override
	public TM getTransformedModel()
	{
		return transformedModel;
	}

	@Override
	public StateValues projectToOriginalModel(final StateValues sv) throws PrismException
	{
		if (sv.getType() instanceof TypeBool) {
			assert(sv.getBitSet() != null) : "State values are undefined.";

			final BitSet mapped = projectToOriginalModel(sv.getBitSet());
			return StateValues.createFromBitSet(mapped, originalModel);
		}
		if (sv.getType() instanceof TypeDouble) {
			assert(sv.getDoubleArray() != null) : "State values are undefined.";

			final double[] mapped = projectToOriginalModel(sv.getDoubleArray());
			StateValues res = StateValues.createFromDoubleArray(mapped, originalModel);
			res.setAccuracy(sv.getAccuracy());
			return res;
		}
		if (sv.getType() instanceof TypeInt) {
			assert(sv.getIntArray() != null) : "State values are undefined.";

			final int[] mapped = projectToOriginalModel(sv.getIntArray());
			return StateValues.createFromIntegerArray(mapped, originalModel);
		}
		throw new PrismException("Unsupported type of state values");
	}

	public BitSet projectToOriginalModel(final BitSet values)
	{
		final BitSet result = new BitSet(numberOfStates);

		for (int state = 0; state < numberOfStates; state++) {
			final Integer mappedState = mapToTransformedModel(state);
			final boolean mappedValue = (mappedState == null) ? DEFAULT_BOOLEAN : values.get(mappedState);
			result.set(state, mappedValue);
		}
		return result;
	}

	public double[] projectToOriginalModel(final double[] values)
	{
		final double[] result = new double[numberOfStates];

		for (int state = 0; state < numberOfStates; state++) {
			final Integer mappedState = mapToTransformedModel(state);
			final double mappedValue = (mappedState == null) ? DEFAULT_DOUBLE : values[mappedState];
			result[state] = mappedValue;
		}
		return result;
	}

	public int[] projectToOriginalModel(final int[] values)
	{
		final int[] result = new int[numberOfStates];

		for (int state = 0; state < numberOfStates; state++) {
			final Integer mappedState = mapToTransformedModel(state);
			final int mappedValue = (mappedState == null) ? DEFAULT_INTEGER : values[mappedState];
			result[state] = mappedValue;
		}
		return result;
	}

	public Integer mapToTransformedModel(final int state)
	{
		if (state >= numberOfStates) {
			throw new IndexOutOfBoundsException("State index does not belong to original model.");
		}
		return mapToTransformedModel.apply(state);
	}

	public BitSet mapToTransformedModel(final BitSet states)
	{
		final BitSet result = new BitSet();

		for (int state : new IterableBitSet(states)) {
			final Integer mappedState = mapToTransformedModel(state);
			if (mappedState != null) {
				result.set(mappedState);
			}
		}
		return result;
	}

}
