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

package jltl2ba;

/**
 * Helper to determine syntactic fragments of LTL formulas.
 * <br>
 * Each object stores information about membership in the
 * "safety", "guarantee/co-safety", "obligation", "recurrence" and "persistence"
 * fragments of LTL.
 */
public class LTLFragments
{
	private boolean syntacticSafety;
	private boolean syntacticGuarantee;
	private boolean syntacticObligation;
	private boolean syntacticRecurrence;
	private boolean syntacticPersistence;

	/**
	 * Constructor for basic formula (true, false, atomic proposition),
	 * contained in all fragments.
	 */
	private LTLFragments()
	{
		syntacticSafety = true;
		syntacticGuarantee = true;
		syntacticObligation = true;
		syntacticRecurrence = true;
		syntacticPersistence = true;
	}

	/** Copy constructor */
	private LTLFragments(LTLFragments other)
	{
		syntacticSafety = other.syntacticSafety;
		syntacticGuarantee = other.syntacticGuarantee;
		syntacticObligation = other.syntacticObligation;
		syntacticRecurrence = other.syntacticRecurrence;
		syntacticPersistence = other.syntacticPersistence;
	}

	/** Do we belong to the syntactic safety fragment? */
	public boolean isSyntacticSafety()
	{
		return syntacticSafety;
	}

	/** Do we belong to the syntactic guarantee (co-safety) fragment? */
	public boolean isSyntacticGuarantee()
	{
		return syntacticGuarantee;
	}

	/** Do we belong to the syntactic obligation fragment? */
	public boolean isSyntacticObligation()
	{
		return syntacticObligation;
	}

	/** Do we belong to the syntactic recurrence fragment? */
	public boolean isSyntacticRecurrence()
	{
		return syntacticRecurrence;
	}

	/** Do we belong to the syntactic persistence fragment? */
	public boolean isSyntacticPersistence()
	{
		return syntacticPersistence;
	}

	@Override
	public String toString()
	{
		String s = "[";
		boolean first = true;

		if (isSyntacticSafety()) {
			if (!first) s+=",";
			first = false;
			s+="syntactic safety";
		}

		if (isSyntacticGuarantee()) {
			if (!first) s+=",";
			first = false;
			s+="syntactic guarantee";
		}

		if (isSyntacticObligation()) {
			if (!first) s+=",";
			first = false;
			s+="syntactic obligation";
		}

		if (isSyntacticRecurrence()) {
			if (!first) s+=",";
			first = false;
			s+="syntactic recurrence";
		}

		if (isSyntacticPersistence()) {
			if (!first) s+=",";
			first = false;
			s+="syntactic persistence";
		}

		return s+"]";
	}

	/**
	 * Apply the Boolean AND operator (i.e., corresponding to this & other),
	 * returning the result.
	 */
	private LTLFragments and(LTLFragments other)
	{
		LTLFragments result = new LTLFragments(this);

		result.syntacticSafety &= other.syntacticSafety;
		result.syntacticGuarantee &= other.syntacticGuarantee;
		result.syntacticObligation &= other.syntacticObligation;
		result.syntacticRecurrence &= other.syntacticRecurrence;
		result.syntacticPersistence &= other.syntacticPersistence;

		return result;
	}

	/**
	 * Apply the Boolean OR operator (i.e., corresponding to this | other),
	 * returning the result.
	 */
	private LTLFragments or(LTLFragments other)
	{
		LTLFragments result = new LTLFragments(this);

		result.syntacticSafety &= other.syntacticSafety;
		result.syntacticGuarantee &= other.syntacticGuarantee;
		result.syntacticObligation &= other.syntacticObligation;
		result.syntacticRecurrence &= other.syntacticRecurrence;
		result.syntacticPersistence &= other.syntacticPersistence;

		return result;
	}

	/**
	 * Apply the Boolean NOT operator (i.e., corresponding to !this),
	 * returning the result.
	 */
	private LTLFragments not()
	{
		LTLFragments result = new LTLFragments();

		// take opposites
		result.syntacticSafety = this.syntacticGuarantee;
		result.syntacticGuarantee = this.syntacticSafety;

		// obligation stays the same
		result.syntacticObligation = this.syntacticObligation;

		// take opposites
		result.syntacticRecurrence = this.syntacticPersistence;
		result.syntacticPersistence = this.syntacticRecurrence;

		return result;
	}

	/**
	 * Apply the temporal "nextstep" operator (i.e., corresponding to X this),
	 * returning the result.
	 */
	private LTLFragments nextstep()
	{
		return new LTLFragments(this);  // just a copy
	}

	/**
	 * Apply the temporal "eventually" operator (i.e., corresponding to F this),
	 * returning the result.
	 */
	private LTLFragments eventually()
	{
		LTLFragments result = new LTLFragments(this);  // copy

		result.syntacticSafety = false;
		// syntacticGuarantee inherited
		result.syntacticObligation = syntacticGuarantee;
		result.syntacticRecurrence = syntacticGuarantee;
		// syntacticPersistence inherited

		return result;
	}

	/**
	 * Apply the temporal "always" operator (i.e., corresponding to G this),
	 * returning the result.
	 */
	private LTLFragments always()
	{
		LTLFragments result = new LTLFragments(this);  // copy

		// syntacticSafety inherited
		result.syntacticGuarantee = false;
		result.syntacticObligation = syntacticSafety;
		// syntacticRecurrence inherited
		result.syntacticPersistence = syntacticSafety;

		return result;
	}

	/**
	 * Apply the temporal "until" operator (i.e., corresponding to this U other),
	 * returning the result.
	 */
	private LTLFragments until(LTLFragments other)
	{
		LTLFragments result = new LTLFragments();
		
		result.syntacticSafety = false;
		result.syntacticGuarantee = this.syntacticGuarantee & other.syntacticGuarantee;
		result.syntacticObligation = this.syntacticObligation & other.syntacticGuarantee;
		result.syntacticRecurrence = this.syntacticRecurrence & other.syntacticGuarantee;
		result.syntacticPersistence = this.syntacticPersistence & other.syntacticPersistence;

		return result;
	}

	/**
	 * Apply the temporal "release" operator (i.e., corresponding to this R other),
	 * returning the result.
	 */
	private LTLFragments release(LTLFragments other)
	{
		LTLFragments result = new LTLFragments();

		result.syntacticSafety = this.syntacticSafety & other.syntacticSafety;
		result.syntacticGuarantee = false;
		result.syntacticObligation = this.syntacticObligation & other.syntacticSafety;
		result.syntacticRecurrence = this.syntacticRecurrence & other.syntacticRecurrence;
		result.syntacticPersistence = this.syntacticPersistence & other.syntacticSafety;

		return result;
	}

	/**
	 * Apply the Boolean EQUIV operator (i.e., corresponding to this &lt;-&gt; other),
	 * returning the result.
	 */
	private LTLFragments equiv(LTLFragments other)
	{
		LTLFragments left = and(other);
		LTLFragments right = not().and(other.not());

		return left.or(right);
	}

	/**
	 * Apply the Boolean IMPLIES operator (i.e., corresponding to this -&gt; other),
	 * returning the result.
	 */
	private LTLFragments implies(LTLFragments other)
	{
		LTLFragments result = not().or(other);
		return result;
	}

	/**
	 * Analyse a given LTL formula and return the syntactic LTL fragments it belongs to.
	 * @param root the root of the LTL formula
	 * @return the LTLFragments it belongs to
	 */
	public static LTLFragments analyse(SimpleLTL root)
	{
		switch (root.kind) {
		case TRUE:
		case FALSE:
			return new LTLFragments(); // all true
		case AP:
			return new LTLFragments(); // all true
		case NOT:
			return analyse(root.left).not();

		case AND:
			return analyse(root.left).and(analyse(root.right));
		case OR:
			return analyse(root.left).or(analyse(root.right));

		case FINALLY:
			return analyse(root.left).eventually();
		case GLOBALLY:
			return analyse(root.left).always();
		case NEXT:
			return analyse(root.left).nextstep();
		case UNTIL:
			return analyse(root.left).until(analyse(root.right));
		case RELEASE:
			return analyse(root.left).release(analyse(root.right));

		case EQUIV:
			return analyse(root.left).equiv(analyse(root.right));
		case IMPLIES:
			return analyse(root.left).implies(analyse(root.right));
		}
		throw new UnsupportedOperationException();
	}
}
