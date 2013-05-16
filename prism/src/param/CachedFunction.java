//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

package param;

/**
 * Function representation using cached functions.
 *
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * @see CachedFunctionFactory
 */
final class CachedFunction extends Function {
	/** unique number of cached function this function represents */
	private int number;
	/** factory this function belongs to */
	private CachedFunctionFactory factory;
	
	/**
	 * Constructs a new cached function.
	 * 
	 * @param factory function factory this function belongs to
	 * @param number unique number of cached function this function represents
	 */
	CachedFunction(CachedFunctionFactory factory, int number)
	{
		super(factory);
		this.factory = factory;
		this.number = number;
	}
	
	/**
	 * Returns the unique number of the cached function this function represents. 
	 * 
	 * @return unique number
	 */
	int getNumber()
	{
		return number;
	}
	
	@Override
	public String toString()
	{
		return factory.getFunction(number).toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CachedFunction)) {
			return false;
		}
		CachedFunction function = (CachedFunction) obj;
		return number == function.number;
	}
	
	@Override
	public int hashCode() {
		return number;
	}
	
	@Override
	public Function add(Function other) {
		return factory.add(this,  other);
	}

	@Override
	public Function negate() {
		return factory.negate(this);
	}

	@Override
	public Function multiply(Function other) {
		return factory.multiply(this, other);
	}

	@Override
	public Function divide(Function other) {
		return factory.divide(this, other);
	}

	@Override
	public Function star() {
		return factory.star(this);
	}

	@Override
	public Function toConstraint() {
		return factory.toConstraint(this);
	}

	@Override
	public BigRational evaluate(Point point, boolean cancel) {
		return factory.evaluate(this, point, cancel);
	}

	@Override
	public BigRational evaluate(Point point) {
		return factory.evaluate(this, point);
	}

	@Override
	public boolean check(Point point, boolean strict) {
		return factory.check(this, point, strict);
	}

	
	@Override
	public BigRational asBigRational() {
		return factory.asBigRational(this);
	}

	@Override
	public boolean isNaN() {
		return factory.isNaN(this);
	}

	@Override
	public boolean isInf() {
		return factory.isInf(this);
	}

	@Override
	public boolean isMInf() {
		return factory.isMInf(this);
	}

	@Override
	public boolean isOne() {
		return factory.isOne(this);
	}

	@Override
	public boolean isZero() {
		return factory.isZero(this);
	}
}
