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
 * TODO implement completely
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
public class DagFunction extends Function {
	private DagFunctionFactory dagFactory;
	private DagOperator num;
	private DagOperator den;
	int type;
	final static int NORMAL = 0;
	final static int INF = 1;
	final static int MINF = 2;
	final static int NAN = 3;
	
	public DagFunction(FunctionFactory factory, DagOperator num, DagOperator den) {
		super(factory);
		dagFactory = (DagFunctionFactory) factory;
		this.num = num;
		this.den = den;
		this.type = NORMAL;
	}
	
	public DagFunction(FunctionFactory factory, int type) {
		super(factory);
		this.type = type;
		num = null;
		den = null;
	}	

	public DagOperator getNum() {
		return num;
	}
	
	public DagOperator getDen() {
		return den;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DagFunction)) {
			return false;
		}
		DagFunction other = (DagFunction) obj;
		if (this.type != NORMAL) {
			return this.type == other.type;
		}
		if (other.type != NORMAL) {
			return false;
		}
		
		BigRational thisRat = new BigRational(this.num.getCValue(), this.den.getCValue());
		BigRational otherRat = new BigRational(other.num.getCValue(), other.den.getCValue());
		return thisRat.equals(otherRat);
	}
	
	@Override
	public int hashCode() {
		if (type != NORMAL) {
			return type;
		}
		BigRational thisRat = new BigRational(num.getCValue(), den.getCValue());
		return thisRat.hashCode();
	}

	
	@Override
	public Function add(Function other) {
		return dagFactory.add(this, (DagFunction) other);
	}

	@Override
	public Function negate() {
		return dagFactory.negate(this);
	}
	
	@Override
	public Function subtract(Function other) {
		return dagFactory.subtract(this, (DagFunction) other);
	}

	@Override
	public Function multiply(Function other) {
		return dagFactory.multiply(this, (DagFunction) other);
	}

	@Override
	public Function divide(Function other) {
		return dagFactory.divide(this, (DagFunction) other);
	}

	@Override
	public Function star() {
		return dagFactory.star(this);
	}

	@Override
	public Function toConstraint() {
		return dagFactory.toConstraint(this);
	}

	@Override
	public BigRational evaluate(Point point, boolean cancel) {
		BigRational result = dagFactory.evaluate(this, point, cancel);
		return result;
	}

	@Override
	public BigRational asBigRational() {
		switch (type) {
		case NORMAL:
			return dagFactory.asBigRational(this);
		case NAN:
			return BigRational.NAN;
		case INF:
			return BigRational.INF;
		case MINF:
			return BigRational.MINF;
		}
		throw new RuntimeException("Illegal type");
	}

	@Override
	public boolean isNaN() {
		return type == NAN;
	}

	@Override
	public boolean isInf() {
		return type == INF;
	}

	@Override
	public boolean isMInf() {
		return type == MINF;
	}

	@Override
	public boolean isOne() {
		return dagFactory.isOne(this);
	}

	@Override
	public boolean isZero() {
		return dagFactory.isZero(this);
	}
	
	@Override
	public String toString() {
		return dagFactory.toString(this);
	}

	public int getType() {
		return type;
	}

	@Override
	public boolean isConstant()
	{
		if (type != NORMAL)
			return true;

		return dagFactory.isConstant(num) && dagFactory.isConstant(den);
	}
}
