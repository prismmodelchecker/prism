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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Random;

/**
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * TODO complete once needed
 */
class DagFunctionFactory extends FunctionFactory {
	private class Number extends DagOperator {
		private BigInteger number;
		Number(BigInteger number) {
			super(number);
			this.number = number;
		}
		BigInteger getNumber() {
			return number;
		}
		
		@Override
		public String toString() {
			return number.toString();
		}
	}
	
	private class Variable extends DagOperator {
		private int variable;
		Variable(int variable) {
			super(randomPosition.getDimension(variable).getNum());
			this.variable = variable;
		}
		int getVariable() {
			return variable;
		}
		
		@Override
		public String toString() {
			return parameterNames[variable];
		}
	}

	private class Negate extends DagOperator {
		private DagOperator what;
		Negate(DagOperator what) {
			super(what.getCValue().negate());
			this.what = what;
		}
		DagOperator getWhat() {
			return what;
		}
		
		@Override
		public String toString() {
			return "-(" + what.toString() + ")";
		}
	}

	private class Add extends DagOperator {
		private DagOperator op1;
		private DagOperator op2;
		Add(DagOperator op1, DagOperator op2) {
			super(op1.getCValue().add(op2.getCValue()));
			this.op1 = op1;
			this.op2 = op2;
		}
		DagOperator getOp1() {
			return op1;
		}
		DagOperator getOp2() {
			return op2;
		}
		
		@Override
		public String toString() {
			return "(" + op1.toString() + "+" + op2.toString() + ")";
		}
	}
	
	private class Multiply extends DagOperator {
		private DagOperator op1;
		private DagOperator op2;
		Multiply(DagOperator op1, DagOperator op2) {
			super(op1.getCValue().multiply(op2.getCValue()));

			this.op1 = op1;
			this.op2 = op2;
		}
		DagOperator getOp1() {
			return op1;
		}
		DagOperator getOp2() {
			return op2;
		}
		
		@Override
		public String toString() {
			return "(" + op1.toString() + "*" + op2.toString() + ")";
		}
	}
	
	private Point randomPosition;
	private HashMap<DagOperator,DagOperator> polynomials;
	private DagOperator zeroOp;
	private DagOperator oneOp;
	private DagFunction[] parameters;
	private DagFunction zero;
	private DagFunction one;
	private DagFunction nan;
	private DagFunction inf;
	private DagFunction minf;
	private HashMap<DagFunction,DagFunction> functions;
//	private boolean negateToInner;
	
	public DagFunctionFactory(String[] parameterNames, BigRational[] lowerBounds, BigRational[] upperBounds, double maxProbWrong, boolean negateToInner) {
		super(parameterNames, lowerBounds, upperBounds);
		Random random = new Random();
		BigRational[] randomPosArr = new BigRational[parameterNames.length];
		int numRandomBits = (int) Math.ceil(Math.log(parameterNames.length / maxProbWrong) / Math.log(2));
		for (int dim = 0; dim < parameterNames.length; dim++) {
			BigInteger num = new BigInteger(numRandomBits, random);
			randomPosArr[dim] = new BigRational(num, BigInteger.ONE);
		}
		randomPosition = new Point(randomPosArr);

		polynomials = new HashMap<DagOperator,DagOperator>();
		functions = new HashMap<DagFunction,DagFunction>();
		zeroOp = new Number(BigInteger.ZERO);
		polynomials.put(zeroOp,zeroOp);
		oneOp = new Number(BigInteger.ONE);
		polynomials.put(oneOp,oneOp);
		zero = new DagFunction(this, zeroOp, oneOp);
		functions.put(zero,zero);
		one = new DagFunction(this, oneOp, oneOp);
		functions.put(one,one);
		nan = new DagFunction(this, DagFunction.NAN);
		functions.put(nan,nan);
		inf = new DagFunction(this, DagFunction.INF);
		functions.put(inf,inf);
		minf = new DagFunction(this, DagFunction.MINF);
		functions.put(minf,minf);
		parameters = new DagFunction[parameterNames.length];
		for (int varNr = 0; varNr < parameterNames.length; varNr++) {
			DagOperator paramOp = new Variable(varNr);
			polynomials.put(paramOp,paramOp);
			parameters[varNr] = new DagFunction(this, paramOp, oneOp);
			functions.put(parameters[varNr],parameters[varNr]);
		}
//		this.negateToInner = negateToInner;
	}
	
	@Override
	public Function getZero() {
		return zero;
	}
	
	@Override
	public Function getOne() {
		return one;
	}

	@Override
	public Function getNaN() {
		return nan;
	}

	@Override
	public Function getInf() {
		return inf;
	}

	@Override
	public Function getMInf() {
		return minf;
	}
	
	private DagOperator makeUnique(DagOperator op) {
		DagOperator foundOp = polynomials.get(op);
		if (foundOp == null) {
			foundOp = op;
			polynomials.put(foundOp, foundOp);
		}
		return foundOp;
	}
	
	private DagFunction makeUnique(DagFunction fn) {
		DagFunction foundFn = functions.get(fn);
		if (foundFn == null) {
			foundFn = fn;
			functions.put(foundFn, foundFn);
		}
		return foundFn;
	}
	
	@Override
	public Function fromBigRational(BigRational bigRat) {
		if (bigRat.isSpecial()) {
			if (bigRat.isNaN()) {
				return getNaN();
			} else if (bigRat.isInf()) {
				return getInf();
			} else if (bigRat.isMInf()) {
				return getMInf();
			} else {
				throw new RuntimeException("Unknown special value");
			}
		}
		// normal:
		bigRat = bigRat.cancel();
		DagOperator num = new Number(bigRat.getNum());
		num = makeUnique(num);
		DagOperator den = new Number(bigRat.getDen());
		den = makeUnique(den);
		DagFunction result = new DagFunction(this,  num, den);
		return makeUnique(result);
	}

	@Override
	public Function getVar(int var) {
		return parameters[var];
	}

	private DagOperator opMultiply(DagOperator op1, DagOperator op2) {
		return makeUnique(new Multiply(op1, op2));
	}
	
	private DagOperator opAdd(DagOperator op1, DagOperator op2) {
		return makeUnique(new Add(op1, op2));
	}
	
	private DagOperator opNegate(DagOperator op) {
		return makeUnique(new Negate(op));
	}
	
	public Function add(DagFunction op1, DagFunction op2) {
		DagOperator num = opAdd(opMultiply(op1.getNum(), op2.getDen()),
				opMultiply(op1.getDen(), op2.getNum()));
		DagOperator den = opMultiply(op1.getDen(), op2.getDen());
		return makeUnique(new DagFunction(this, num,  den));
	}

	public DagFunction negate(DagFunction dagFunction) {
		DagOperator neg = opNegate(dagFunction.getNum());
		return makeUnique(new DagFunction(this, neg, dagFunction.getDen()));
	}
	
	public Function subtract(DagFunction op1, DagFunction op2) {
		DagFunction negOther = negate(op2);
		return add(op1, negOther);
	}

	public Function multiply(DagFunction op1, DagFunction op2) {
		DagOperator num = opMultiply(op1.getNum(), op2.getNum());
		DagOperator den = opMultiply(op1.getDen(), op2.getDen());
		return makeUnique(new DagFunction(this, num, den));
	}

	public Function divide(DagFunction op1, DagFunction op2) {
		DagOperator num = opMultiply(op1.getNum(), op2.getDen());
		DagOperator den = opMultiply(op1.getDen(), op2.getNum());
		return makeUnique(new DagFunction(this, num, den));
	}

	public Function star(DagFunction op) {
		DagOperator num = op.getDen();
		DagOperator den = opAdd(op.getDen(), opNegate(op.getNum()));
		return makeUnique(new DagFunction(this, num, den));
	}

	// TODO fix following
	public Function toConstraint(DagFunction op) {
		DagOperator num = op.getNum();
		DagOperator den = oneOp;
		return makeUnique(new DagFunction(this, num, den));
	}

	// TODO use cache for faster evaluation
	private BigRational evaluate(DagOperator op, Point point) {
		if (op instanceof Number) {
			Number opNum = (Number) op;
			return new BigRational(opNum.getNumber());
		} else if (op instanceof Variable) {
			Variable opVar = (Variable) op;
			return point.getDimension(opVar.getVariable());
		} else if (op instanceof Negate) {
			Negate opNegate = (Negate) op;
			return evaluate(opNegate.getWhat(), point).negate();
		} else if (op instanceof Add) {
			Add opAdd = (Add) op;
			return evaluate(opAdd.getOp1(), point).add(evaluate(opAdd.getOp2(), point));
		} else if (op instanceof Multiply) {
			Multiply opMultiply = (Multiply) op;
			return evaluate(opMultiply.getOp1(), point).multiply(evaluate(opMultiply.getOp2(), point));
		} else {
			throw new RuntimeException("invalid operator");
		}
	}
	
	public BigRational evaluate(DagFunction op, Point point, boolean cancel) {
		if (op.getType() == DagFunction.NAN) {
			return BigRational.NAN;
		}
		return evaluate(op.getNum(), point).divide(evaluate(op.getDen(), point));
	}

	public BigRational asBigRational(DagFunction op) {
		BigRational[] point = new BigRational[parameterNames.length];
		for (int i = 0; i < parameterNames.length; i++) {
			point[i] = BigRational.ZERO;
		}
		return evaluate(op, new Point(point), true);
	}

	public boolean isOne(DagFunction op) {
		return op == one;
	}

	public boolean isZero(DagFunction op) {
		return op == zero;
	}

	/**
	 * Returns true if the tree rooted in {@code op}
	 * is guaranteed to represent a constant value.
	 */
	public boolean isConstant(DagOperator op) {
		if (op instanceof Number) {
			return true;
		} else if (op instanceof Variable) {
			return false;
		} else if (op instanceof Negate) {
			Negate opNegate = (Negate) op;
			return isConstant(opNegate.getWhat());
		} else if (op instanceof Add) {
			Add opAdd = (Add) op;
			return isConstant(opAdd.getOp1()) && isConstant(opAdd.getOp2());
		} else if (op instanceof Multiply) {
			Multiply opMultiply = (Multiply) op;
			return isConstant(opMultiply.getOp1()) && isConstant(opMultiply.getOp2());
		} else {
			throw new RuntimeException("invalid operator");
		}
	}

	public String toString(DagFunction op) {
		return "(" + op.getNum().toString() + ")/(" + op.getDen().toString() + ")";
	}
}
