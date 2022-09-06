/*
 * This file is part of a Java port of the program ltl2dstar
 * (http://www.ltl2dstar.de/) for PRISM (http://www.prismmodelchecker.org/)
 * Copyright (C) 2005-2007 Joachim Klein <j.klein@ltl2dstar.de>
 * Copyright (c) 2007 Carlos Bederian
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as 
 *  published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jltl2dstar;

import prism.PrismException;
import java.util.Iterator;

import jltl2ba.APElement;
import jltl2ba.APSet;
import jltl2ba.MyBitSet;

/**
 * Class representing a subset of 2^AP where AP is the set of 
 * atomic propositions (APSet). It stores two bits per AP: 
 * One bit to determine if the value of this AP is set, 
 * the second bit to store the value.<br>
 * Additionally, the APMonom can have the special values
 * TRUE or FALSE.<br>
 */
public class APMonom {

	private MyBitSet bits_set;
	private MyBitSet bits_value;
	private boolean booleanValue;

	public APMonom()
	{
		bits_set = new MyBitSet();
		bits_value = new MyBitSet();
		booleanValue = true;
	}

	public APMonom(boolean b) {
		bits_set = new MyBitSet();
		bits_value = new MyBitSet();
		booleanValue = b;
	}

	public APMonom(MyBitSet set, MyBitSet value)
	{
		bits_set = set;
		bits_value = value;
	}

	/**
	 * Is the AP set?
	 * @param index index of AP
	 * @return <b>true</b> if AP is set
	 */
	public boolean isSet(int index) throws PrismException {
		if (!isNormal()) {
			// TRUE / FALSE -> the bit is not set
			return false;
		}
		return bits_set.get(index);
	}

	/**
	 * Gets the value for this AP. You can't get the value if the AP is not set.
	 * @param index index of AP
	 * @return <b>true</b> if AP is true
	 */
	public boolean getValue(int index) throws PrismException {
		if (!isNormal()) {
			throw new PrismException("Can't get AP, is either TRUE/FALSE!");
		}

		if (!bits_set.get(index)) {
			throw new PrismException("Can't get value: AP not set!");
		}

		return bits_value.get(index);
	}

	/**
	 * Sets the value for this AP. Implicitly, it also sets the AP to 'set'.
	 * @param index index of AP
	 * @param value value of AP
	 */
	public void setValue(int index, boolean value) {
		bits_set.set(index, true);
		bits_value.set(index, value);
	}


	/**
	 * Perform a logical AND operation of this APMonom with a single AP.
	 * @param index index index of AP
	 * @param value value of AP
	 */
	public void andAP(int index, boolean value) {
		if (isFalse()) {return;}

		if (!isTrue()) {
			if (bits_set.get(index) && bits_value.get(index) != value) {
				// contradiction
				booleanValue = false;
				bits_set.clear();
				bits_value.clear();
				return;
			}
		}
		setValue(index, value);
	}


	/**
	 * Unsets this AP.
	 * @param index index of AP
	 */
	public void unset(int index) {
		bits_value.set(index,false);
		bits_set.set(index, false);
	}


	/**
	 * Checks if this APMonom is equivalent to TRUE.
	 * @return <b>true</b> if this APMonom is TRUE
	 */
	public boolean isTrue() {
		return (!isNormal() && booleanValue);
	}

	/**
	 * Checks if this APMonom is equivalent to FALSE.
	 * @return <b>true</b> if this APMonom is FALSE
	 */
	public boolean isFalse() {
		return (!isNormal() && !booleanValue);
	}

	/**
	 * Checks if this APMonom is a normal APMonon (not equivalent to TRUE or FALSE).
	 * @return <b>true</b> if this APMonom is normal (not TRUE/FALSE).
	 */
	public boolean isNormal() {
		return !bits_set.isEmpty();
	}

	/**
	 * Provides access to the underlying MyBitSet representing the
	 * value (AP occurs in positive or negative form).
	 * @return the SimpleBitSet of the values
	 */
	public MyBitSet getValueBits() {
		return bits_value;
	}

	/**
	 * Provides access to the underlying MyBitSet representing the
	 * bits that are set (AP occurs).
	 * @return the SimpleBitSet of the occuring APs
	 */
	public MyBitSet getSetBits() {
		return bits_set;
	}

	/** Checks to see if the MyBitSet representation is normalized. */
	public boolean isNormalized() {
		if (isTrue() || isFalse()) {
			return true;
		}
		MyBitSet tmp = getValueBits();
		tmp.andNot(bits_set);
		return tmp.isEmpty();
	}

	/**
	 * Output APMonom
	 */
	public String toString()
	{
		String rv = "";
		if (isTrue()) {
			return "true";
		} else if (isFalse()) {
			return "false";
		} else {
			for (int i = 0; i < bits_set.size(); i++) {
				if (bits_set.get(i)) {
					if (bits_value.get(i)) {
						rv = rv + "+" + i;
					} else {
						rv = rv + "-" + i;
					}
				}
			}
		}
		return rv;
	}

	/**
	 * Performs an intersection check.
	 * @param m1 the first APMonom
	 * @param m2 the second APMonom
	 * @return <b>true</b> if the intersection of <i>m1</i> and <i>m2</i> is empty.
	 * 
	 * FIXME: I'm not sure if this does what it says it does, but ltl2dstar does it this way.
	 */
	public boolean isIntersectionEmpty(APMonom m2) {
		// check if there are contradicting values 
		MyBitSet set_in_both = getSetBits();
		set_in_both.and(m2.getSetBits());

		MyBitSet maskedm1 = getValueBits();
		maskedm1.and(set_in_both);

		MyBitSet maskedm2 = m2.getValueBits();
		maskedm2.and(set_in_both);

		return ! maskedm1.equals(maskedm2);
	}

	/**
	 * Perform logical conjunction with other APMonom.
	 * @param other the other APMonom
	 */
	public APMonom and(APMonom other) {
		if (this.isFalse() || other.isFalse()) {
			return new APMonom(false);
		}

		if (this.isTrue()) {return other;}
		if (other.isTrue()) {return this;}

		// both are not TRUE/FALSE:

		if (this.isIntersectionEmpty(other)) {
			//  return APMonom equivalent to false
			return new APMonom(false);
		}

		// both Monoms are not contradicting...
		MyBitSet result_set = this.getSetBits();
		result_set.or(other.getSetBits());

		MyBitSet result_value = this.getValueBits();
		result_value.or(other.getValueBits());

		return new APMonom(result_set, result_value);
	}

	/**
	 * Perform 'minus' operation (equal to *this & !other).
	 * @param other the other APMonom
	 */
	APMonom andNot(APMonom other) {
		if (this.isFalse()) {
			// false & anything == false
			return new APMonom(false);
		}

		if (other.isFalse()) {
			// *this & !(false) == *this & true == *this
			return this;
		}

		if (other.isTrue()) {
			// *this & !(true) == *this & false == false
			return new APMonom(false);
		}

		// the result will be false, if there are two set bits
		// with equal value
		MyBitSet set_in_both= getSetBits();
		set_in_both.and(other.getSetBits());

		MyBitSet maskedm1 = getValueBits();
		maskedm1.and(set_in_both);

		MyBitSet maskedm2 = other.getValueBits();
		maskedm2.and(set_in_both);
		maskedm2.flip(0,maskedm2.size());

		if (!maskedm1.equals(maskedm2)) {
			// return false;
			return new APMonom(false);
		}
		MyBitSet result_set = getSetBits();
		result_set.or(other.getSetBits());

		MyBitSet result_value = getValueBits();
		result_value.andNot(other.getValueBits());

		return new APMonom(result_set, result_value);
	}


	/**
	 * Checks for equality.
	 * @param other the other APMonom
	 * @return <b>true</b> if this and the other APMonom are equal
	 */
	public boolean equals(APMonom other) {
		return (isNormal() && other.isNormal() &&
				getValueBits().equals(other.getValueBits()) && 
				getSetBits().equals(other.getSetBits()) ||
				isTrue() && other.isTrue() ||
				isFalse() && other.isFalse());
	}

	public boolean equals(Object other) {
		return ((other instanceof APMonom) && this.equals((APMonom) other));
	}

	public Iterator<APElement> APElementIterator(APSet s)
	{
		return (Iterator<APElement>) new APMonom2APElements(s, this);  
	}

	public void setFromPosNeg(MyBitSet pos, MyBitSet neg) throws PrismException
	{
		int sz = pos.size() > neg.size() ? pos.size() : neg.size();

		if (pos.intersects(neg)) {
			throw new PrismException("MyBitSet contradiction");
		}
		if ((pos.cardinality() == sz)
				|| (pos.cardinality() == 0 && neg.cardinality() == 0)) {
			bits_set = new MyBitSet(sz);
			bits_value = new MyBitSet(sz);
			booleanValue = true;
		}
		else if (neg.cardinality() == sz) {
			bits_set = new MyBitSet(sz);
			bits_value = new MyBitSet(sz);
			booleanValue = false;
		}
		else {
			if (pos.size() == sz) {
				bits_set = (MyBitSet) pos.clone();
				bits_set.or(neg);  
				bits_value = (MyBitSet) pos.clone();
			}
			else {
				bits_set = (MyBitSet) neg.clone();
				bits_set.or(pos);  
				bits_value = new MyBitSet(sz); 
				bits_value.or(pos);
			}
		}
	}
}
