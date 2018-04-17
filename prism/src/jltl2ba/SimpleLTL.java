/* Written by Denis Oddoux, LIAFA, France                                 *
 * Copyright (c) 2001  Denis Oddoux                                       *
 * Modified by Paul Gastin, LSV, France                                   *
 * Copyright (c) 2007  Paul Gastin                                        *
 * Ported by Carlos Bederian, FaMAF, Argentina                            *
 * Copyright (c) 2007  Carlos Bederian                                    *
 *                                                                        *
 * This program is free software; you can redistribute it and/or modify   *
 * it under the terms of the GNU General Public License as published by   *
 * the Free Software Foundation; either version 2 of the License, or      *
 * (at your option) any later version.                                    *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program; if not, write to the Free Software            *
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA*
 *                                                                        *
 * Based on the translation algorithm by Gastin and Oddoux,               *
 * presented at the 13th International Conference on Computer Aided       *
 * Verification, CAV 2001, Paris, France.                                 *
 * Proceedings - LNCS 2102, pp. 53-65                                     *
 *                                                                        */

package jltl2ba;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import jltl2dstar.APMonom;
import jltl2dstar.NBA;
import prism.PrismException;

public class SimpleLTL {

	public enum LTLType { 
		FALSE, TRUE,
		AP,
		NOT, NEXT, 
		OR, AND, EQUIV, IMPLIES,
		UNTIL, RELEASE,
		GLOBALLY, FINALLY
	};

	public SimpleLTL left;
	public SimpleLTL right;
	public LTLType kind;
	public String ap;

	public SimpleLTL(boolean v)
	{
		left = null;
		right = null;
		kind = v ? LTLType.TRUE : LTLType.FALSE;
		ap = null;
	}

	public SimpleLTL(String prop)
	{
		left = null;
		right = null;
		kind = LTLType.AP;
		ap = prop;
	}

	public SimpleLTL(LTLType type, SimpleLTL lft)
	{
		switch (type) {
		case NOT:
		case NEXT:
		case GLOBALLY:
		case FINALLY:
			left = lft;
			right = null;
			kind = type;
			ap = null;
			break;
		default:
		//	throw new PrismException("Trying to build invalid SimpleLTL");
		}
	}

	public SimpleLTL(LTLType type, SimpleLTL lft, SimpleLTL rgt)
	{
		switch (type) {
		case AND:
		case OR:
		case IMPLIES:
		case EQUIV:
		case UNTIL:
		case RELEASE:
			left = lft;
			right = rgt;
			kind = type;
			ap = null;
			break;
		default:
		//	throw new PrismException("Trying to build invalid SimpleLTL");
		}
	}

	public SimpleLTL(LTLType type, SimpleLTL lft, SimpleLTL rgt, String prop)
	{
		kind = type;
		left = lft;
		right = rgt;
		ap = null;
		if (prop != null) { ap = prop; }
	}

	public boolean equals(Object o)
	{
		if (o instanceof SimpleLTL) {
			SimpleLTL other = (SimpleLTL) o;
			if (kind == other.kind) {
				switch (kind) {
				case FALSE:case TRUE:
					return true;
				case NOT:case NEXT:case GLOBALLY:case FINALLY:
					return left.equals(other.left);
				case OR:case AND:case EQUIV:case IMPLIES:case UNTIL:case RELEASE:
					return (left.equals(other.left) && right.equals(other.right));
				case AP:
					return ap.equals(other.ap);
				default:
					return false;
				}
			}
			else return false;
		}
		else return false;
	}

	public APSet getAPs()
	{
		APSet rv;

		switch (kind) {
		case NOT:case NEXT:
		case GLOBALLY:case FINALLY:
			rv = left.getAPs();
			break;
		case OR:case AND:
		case EQUIV:case IMPLIES:
		case UNTIL:case RELEASE:
			rv = left.getAPs();
			for (String s : right.getAPs())
				rv.addAP(s);
			break;
			// terminals
		case FALSE:case TRUE:
			rv = new APSet();
			break;
		case AP:
			rv = new APSet();
			rv.addAP(ap);
			break;
		default:
			rv = new APSet();
		break;
		}
		return rv;
	}

	public SimpleLTL clone()
	{
		SimpleLTL rv = new SimpleLTL(kind,
				left != null ? left.clone() : null,
						right != null ? right.clone() : null,
								ap);
		return rv;
	}

	private boolean implies(SimpleLTL b)
	{
		return
		(this.equals(b) ||
				b.kind == LTLType.TRUE ||
				kind == LTLType.FALSE ||
				(b.kind == LTLType.AND && this.implies(b.left) && this.implies(b.right)) ||
				(kind == LTLType.OR && left.implies(b) && right.implies(b)) ||
				(kind == LTLType.AND && (left.implies(b) || right.implies(b))) ||
				(b.kind == LTLType.OR && (this.implies(b.left) || this.implies(b.right))) ||
				(b.kind == LTLType.UNTIL && this.implies(b.right)) ||
				(kind == LTLType.RELEASE && right.implies(b)) ||
				(kind == LTLType.UNTIL && left.implies(b) && right.implies(b)) ||
				(b.kind == LTLType.RELEASE && this.implies(b.left) && this.implies(b.right)) ||
				((kind == LTLType.UNTIL || kind == LTLType.RELEASE) && kind == b.kind &&
						left.implies(b.left) && right.implies(b.right)));
	}

	/**
	 * Construct simplified PNF form.
	 *
	 * Currently, this requires that the SimpleLTL formula is a tree, i.e., that
	 * subtrees are not shared. If this is not the case,
	 * an {@code IllegalArgumentException} is thrown.
	 */
	public SimpleLTL simplify()
	{
		if (!this.isTree()) {
			throw new IllegalArgumentException("Implementation error: SimpleLTL.simplify() requires that the formula is a tree, not a DAG");
		}
		return this.simplified();
	}
	
	private SimpleLTL simplified()
	{
		SimpleLTL tmp, tmp2, a, b;
		SimpleLTL rv = this;
		
		switch (kind) {
		case AND:
		case OR:
		case IMPLIES:
		case EQUIV:
		case UNTIL:
		case RELEASE:
			right = right.simplified();
			left = left.simplified();
			break;

		case NOT:
		case NEXT:
		case FINALLY:
		case GLOBALLY:
			left = left.simplified();
			break;

		default:
			// do nothing
		}

		switch (kind) {
		case NOT:
			tmp = this.pushNegation();
			if (tmp.kind != LTLType.NOT)
				rv = tmp.simplified();
			else rv = tmp;
			break;
			
		case FINALLY:
			if (left.kind == LTLType.TRUE || left.kind == LTLType.FALSE) {
				rv = left;
				break;
			}
			else if (left.kind == LTLType.UNTIL) {
				if (left.left.kind == LTLType.TRUE) {
					rv = left;
					break;
				}
				left = left.right;
				/* fall thru */
			}
			tmp = new SimpleLTL(LTLType.UNTIL, new SimpleLTL(true), left);
			rv = tmp.simplified();
			break;

		case GLOBALLY:
			if (left.kind == LTLType.FALSE || left.kind == LTLType.TRUE) {
				rv = left;
				break;
			}
			else if (left.kind == LTLType.RELEASE) {
				if (left.left.kind == LTLType.FALSE) {
					rv = left;
					break;
				}
				left = left.right;	/* [] (p V q) = [] q */
				/* fall thru */
			}
			tmp = new SimpleLTL(LTLType.RELEASE, new SimpleLTL(false), left);
			rv = tmp.simplified();
			break;

		case UNTIL:
			if (right.kind == LTLType.TRUE
					|| right.kind == LTLType.FALSE
					|| left.kind == LTLType.FALSE) {
				rv = right;
				break;
			}
			if (left.implies(right)) {
				rv = right;
				break;
			}
			/* (p U q) U p = (q U p) */
			if (left.kind == LTLType.UNTIL && left.left.equals(right)) {	
				left = left.right;
				break;
			}
			if (right.kind == LTLType.UNTIL && left.implies(right.left)) {
				rv = right;
				break;
			}
			/* X p U X q == X (p U q) */
			if (right.kind == LTLType.NEXT && left.kind == LTLType.NEXT) {
				rv = new SimpleLTL(LTLType.NEXT, 
						new SimpleLTL(LTLType.UNTIL, left.left, right.left));
				break;
			}

			/* F X p == X F p */
			if (left.kind == LTLType.TRUE && right.kind == LTLType.NEXT) {
				rv = new SimpleLTL(LTLType.NEXT,
						new SimpleLTL(LTLType.UNTIL, new SimpleLTL(true), right.left));
				break;
			}

			/* F G F p == G F p */
			if (left.kind == LTLType.TRUE &&
					right.kind == LTLType.RELEASE &&
					right.left.kind == LTLType.FALSE &&
					right.right.kind == LTLType.UNTIL &&
					right.right.left.kind == LTLType.TRUE) {
				rv = right;
				break;
			}

			if (left.kind != LTLType.TRUE) {
				tmp = new SimpleLTL(LTLType.NOT, right.clone());
				if (tmp.pushNegation().implies(left))
					left = new SimpleLTL(true); 
				break;
			}	    
			break;

		case RELEASE:
			if (right.kind == LTLType.FALSE
					|| right.kind == LTLType.TRUE
					|| left.kind == LTLType.TRUE) {
				rv = right;
				break;
			}
			/* p V p = p */
			if (right.implies(left)) {	
				rv = right;
				break;
			}
			/* F V (p V q) == F V q */
			if (left.kind == LTLType.FALSE && right.kind == LTLType.RELEASE) {
				right = right.right;
				break;
			}

			/* G X p == X G p */
			if (left.kind == LTLType.FALSE && right.kind == LTLType.NEXT) {
				rv = new SimpleLTL(LTLType.NEXT,
						new SimpleLTL(LTLType.RELEASE,
								new SimpleLTL(false), right.left));
				break;
			}
			/* G F G p == F G p */
			if (left.kind == LTLType.FALSE &&
					right.kind == LTLType.UNTIL &&
					right.left.kind == LTLType.TRUE &&
					right.right.kind == LTLType.RELEASE &&
					right.right.left.kind == LTLType.FALSE) {
				rv = right;
				break;
			}

			if (right.kind == LTLType.RELEASE
					&& right.left.implies(left)) {
				rv = right;
				break;
			}

			if (left.kind != LTLType.FALSE) {
				tmp = new SimpleLTL(LTLType.NOT, right.clone());
				if (left.implies(tmp.pushNegation()))
					left = new SimpleLTL(false);
				break;
			}
			break;

		case NEXT:
			if (left.kind == LTLType.TRUE || left.kind == LTLType.FALSE) {
				rv = left;
				break;
			}

			/* X G F p == G F p */
			if (left.kind == LTLType.RELEASE &&
					left.left.kind == LTLType.FALSE &&
					left.right.kind == LTLType.UNTIL &&
					left.right.left.kind == LTLType.TRUE) {
				rv = left;
				break;
			}
			/* X F G p == F G p */
			if (left.kind == LTLType.UNTIL &&
					left.left.kind == LTLType.TRUE &&
					left.right.kind == LTLType.RELEASE &&
					left.right.left.kind == LTLType.FALSE) {
				rv = left;
				break;
			}
			break;
		case IMPLIES:
			if (left.implies(right)) {
				rv = new SimpleLTL(true);
				break;
			}

			tmp = new SimpleLTL(LTLType.NOT, left);
			tmp = new SimpleLTL(LTLType.OR, tmp.pushNegation(), right);
			rv = tmp.rewrite();
			break;

		case EQUIV:
			if (left.implies(right) &&
					right.implies(left)) {
				rv = new SimpleLTL(true);
				break;
			}

			a = new SimpleLTL(LTLType.AND, left.clone(), right.clone());
			tmp = new SimpleLTL(LTLType.NOT, left);
			tmp2 = new SimpleLTL(LTLType.NOT, right);
			b = new SimpleLTL(LTLType.AND, tmp.pushNegation(), tmp2.pushNegation());
			rv = new SimpleLTL(LTLType.OR, a.rewrite(), b.rewrite());
			rv = rv.rewrite();
			break;

		case AND:
			/* p && (q U p) = p */
			if (right.kind == LTLType.UNTIL
					&& right.right.equals(left)) {
				rv = left;
				break;
			}
			if (left.kind == LTLType.UNTIL
					&& left.right.equals(right)) {
				rv = right;
				break;
			}

			/* p && (q V p) == q V p */
			if (right.kind == LTLType.RELEASE
					&& right.right.equals(left)) {
				rv = right;
				break;
			}
			if (left.kind == LTLType.RELEASE
					&& left.right.equals(right)) {
				rv = left;
				break;
			}

			/* (p U q) && (r U q) = (p && r) U q */
			if (right.kind == LTLType.UNTIL
					&& left.kind == LTLType.UNTIL
					&& right.right.equals(left.right)) {
				rv = new SimpleLTL(LTLType.UNTIL,
						new SimpleLTL(LTLType.AND, left.left, right.left),
						left.right);
				break;
			}

			/* (p V q) && (p V r) = p V (q && r) */
			if (right.kind == LTLType.RELEASE
					&& left.kind == LTLType.RELEASE
					&& right.left.equals(left.left)) {
				rv = new SimpleLTL(LTLType.RELEASE,
						right.left,
						new SimpleLTL(LTLType.AND, left.right, right.right)); 
				break;
			}

			/* X p && X q == X (p && q) */
			if (right.kind == LTLType.NEXT && left.kind == LTLType.NEXT) {
				rv = new SimpleLTL(LTLType.NEXT,
						new SimpleLTL(LTLType.AND, left.left, right.left));
				break;
			}

			/* (p V q) && (r U q) == p V q */
			if (right.kind == LTLType.UNTIL
					&& left.kind == LTLType.RELEASE
					&& left.right.equals(right.right)) {
				rv = left;
				break;
			}

			if (left.equals(right)	/* (p && p) == p */
					||right.kind == LTLType.FALSE	/* (p && F) == F */
					|| left.kind == LTLType.TRUE	/* (T && p) == p */
					|| right.implies(left)) {	/* NEW */
				rv = right;
				break;
			}
			if (right.kind == LTLType.TRUE	/* (p && T) == p */
					|| left.kind == LTLType.FALSE	/* (F && p) == F */
					|| left.implies(right)) {	/* NEW */
				rv = left;
				break;
			}

			/* F G p && F G q == F G (p && q) */
			if (left.kind == LTLType.UNTIL &&
					left.left.kind == LTLType.TRUE &&
					left.right.kind == LTLType.RELEASE &&
					left.right.left.kind == LTLType.FALSE &&
					right.kind == LTLType.UNTIL &&
					right.left.kind == LTLType.TRUE &&
					right.right.kind == LTLType.RELEASE &&
					right.right.left.kind == LTLType.FALSE) {
				rv = new SimpleLTL(LTLType.UNTIL, 
						new SimpleLTL(true),
						new SimpleLTL(LTLType.RELEASE, 
								new SimpleLTL(false),
								new SimpleLTL(LTLType.AND,
										left.right.right,
										right.right.right)));
				break;
			}

			tmp = new SimpleLTL(LTLType.NOT, right.clone());
			if (left.implies(tmp.pushNegation())) {
				rv = new SimpleLTL(false);
				break;
			}
			tmp = new SimpleLTL(LTLType.NOT, left.clone());
			if (right.implies(tmp.pushNegation())) {
				rv = new SimpleLTL(false);
				break;
			}
			break;

		case OR:
			/* p || (q U p) == q U p */
			if (right.kind == LTLType.UNTIL
					&& right.right.equals(left)) {
				rv = right;
				break;
			}

			/* p || (q V p) == p */
			if (right.kind == LTLType.RELEASE
					&& right.right.equals(left)) {
				rv = left;
				break;
			}

			/* (p U q) || (p U r) = p U (q || r) */
			if (right.kind == LTLType.UNTIL
					&& left.kind == LTLType.UNTIL
					&& right.left.equals(left.left)) {
				rv = new SimpleLTL(LTLType.UNTIL,
						right.left,
						new SimpleLTL(LTLType.OR, left.right, right.right));
				break;
			}

			if (left.equals(right)	/* (p || p) == p */
					||right.kind == LTLType.FALSE	/* (p || F) == p */
					|| left.kind == LTLType.TRUE	/* (T || p) == T */
					|| right.implies(left)) {
				rv = left;
				break;
			}
			if (right.kind == LTLType.TRUE	/* (p || T) == T */
					|| left.kind == LTLType.FALSE	/* (F || p) == p */
					|| left.implies(right)) {
				rv = right;
				break;
			}

			/* (p V q) || (r V q) = (p || r) V q */
			if (right.kind == LTLType.RELEASE
					&& left.kind == LTLType.RELEASE
					&& left.right.equals(right.right)) {
				rv = new SimpleLTL(LTLType.RELEASE,
						new SimpleLTL(LTLType.OR, left.left, right.left),
						right.right);
				break;
			}

			/* (p V q) || (r U q) == r U q */
			if (right.kind == LTLType.UNTIL
					&& left.kind == LTLType.RELEASE
					&& left.right.equals(right.right)) {
				rv = right;
				break;
			}

			/* G F p || G F q == G F (p || q) */
			if (left.kind == LTLType.RELEASE &&
					left.left.kind == LTLType.FALSE &&
					left.right.kind == LTLType.UNTIL &&
					left.right.left.kind == LTLType.TRUE &&
					right.kind == LTLType.RELEASE &&
					right.left.kind == LTLType.FALSE &&
					right.right.kind == LTLType.UNTIL &&
					right.right.left.kind == LTLType.TRUE) {
				rv = new SimpleLTL(LTLType.RELEASE,
						new SimpleLTL(false),
						new SimpleLTL(LTLType.UNTIL,
								new SimpleLTL(true),
								new SimpleLTL(LTLType.OR,left.right.right,right.right.right)));
				break;
			}

			tmp = new SimpleLTL(LTLType.NOT, right.clone());
			if (tmp.pushNegation().implies(left)) {
				rv = new SimpleLTL(true);
				break;
			}
			tmp = new SimpleLTL(LTLType.NOT, left.clone());
			if (tmp.pushNegation().implies(right)) {
				rv = new SimpleLTL(true);
				break;
			}
			break;
		}
		return rv;
	}

	/**
	 * Returns an equivalent SimpleLTL formula with a
	 * basic set of operators:
	 *   AP, TRUE, FALSE, AND, OR, NOT, UNTIL, FINALLY, GLOBALLY, NEXT
	 */
	public SimpleLTL toBasicOperators() {
		switch (kind) {
		case AP:
		case TRUE:
		case FALSE:
			return this;
		case AND:
		case OR:
		case UNTIL:
			return new SimpleLTL(kind, left.toBasicOperators(), right.toBasicOperators());
		case FINALLY:
		case GLOBALLY:
		case NEXT:
		case NOT:
			return new SimpleLTL(kind, left.toBasicOperators());
		case EQUIV: {
			SimpleLTL newLeft = left.toBasicOperators();
			SimpleLTL newRight = right.toBasicOperators();
			SimpleLTL bothTrue = new SimpleLTL(LTLType.AND, newLeft, newRight);
			SimpleLTL bothFalse = new SimpleLTL(LTLType.AND,
			                                    new SimpleLTL(LTLType.NOT, newLeft),
			                                    new SimpleLTL(LTLType.NOT, newRight));
			return new SimpleLTL(LTLType.OR, bothTrue, bothFalse);
		}
		case IMPLIES: {
			SimpleLTL newLeft = new SimpleLTL(LTLType.NOT, left.toBasicOperators());
			return new SimpleLTL(LTLType.OR, newLeft, right.toBasicOperators());
		}
		case RELEASE: {
			SimpleLTL newLeft = new SimpleLTL(LTLType.NOT, left.toBasicOperators());
			SimpleLTL newRight = new SimpleLTL(LTLType.NOT, right.toBasicOperators());
			return new SimpleLTL(LTLType.UNTIL, newLeft, newRight);
		}
		}
		throw new UnsupportedOperationException("Unknown operator in SimpleLTL");
	}

	public SimpleLTL negate() {
		return new SimpleLTL(LTLType.NOT, this);
	}

	public SimpleLTL pushNegation()
	{	
		SimpleLTL m;
		boolean pushBothOperands = false;

		if (kind != LTLType.NOT)
			return this;
			// throw new PrismException("No NOT to push!");

		switch (left.kind) {
		case TRUE:
			left = null;
			kind = LTLType.FALSE;
			break;
		case FALSE:
			left = null;
			kind = LTLType.TRUE;
			break;
		case NOT:
			m = left.left;
			left = m.left;
			right = m.right;
			kind = m.kind;
			if (kind == LTLType.AP) { ap = m.ap; }
			break;
		case RELEASE:
			kind = LTLType.UNTIL;
			pushBothOperands = true;
			break;
		case UNTIL:
			kind = LTLType.RELEASE;
			pushBothOperands = true;
			break;
		case NEXT:
			kind = LTLType.NEXT;
			left.kind = LTLType.NOT;
			left = left.pushNegation();
			break;
		case AND:
			kind = LTLType.OR;
			pushBothOperands = true;
			break;
		case OR:
			kind = LTLType.AND;
			pushBothOperands = true;
			break;
		case IMPLIES:
			kind = LTLType.AND;
			m = new SimpleLTL(LTLType.NOT, left.right);
			right = m.pushNegation();
			left = left.left;
			break;
		case EQUIV:
			kind = LTLType.OR;
			m = new SimpleLTL(LTLType.NOT, left.left.clone());
			right = new SimpleLTL(LTLType.AND, m.pushNegation(), left.right.clone());
			left.kind = LTLType.AND;
			m = new SimpleLTL(LTLType.NOT, left.right);
			left.right = m.pushNegation();
			break;
		case FINALLY:
			kind = LTLType.GLOBALLY;
			left.kind = LTLType.NOT;
			left = left.pushNegation();
			break;
		case GLOBALLY:
			kind = LTLType.FINALLY;
			left.kind = LTLType.NOT;
			left = left.pushNegation();
			break;
		case AP:
			return this;
		}
		if (pushBothOperands) {
			m = new SimpleLTL(LTLType.NOT, left.right);
			right = m.pushNegation();
			left.kind = LTLType.NOT;
			left.right = null;
			left = left.pushNegation();
		}
		return this.rewrite();
	}
	
	public SimpleLTL rewrite()
	{
		return this.rightLinked().canonical();
	}

	public SimpleLTL rightLinked()
	{
		SimpleLTL tmp;
		SimpleLTL n = this;

		if (n.kind == LTLType.AND || n.kind == LTLType.OR) {
			while (n.left != null && n.left.kind == n.kind) {
				tmp = n.left;
				n.left = tmp.right;
				tmp.right = n;
				n = tmp;
			}
		}
		if (n.left != null)
			n.left = n.left.rightLinked();
		if (n.right != null)
			n.right = n.right.rightLinked();

		return n;
	}

	public SimpleLTL canonical()
	{
		return this;	// No caches here, yet

		/* SimpleLTL m;		// assumes input is right_linked

		if ((m = in_cache(n)) != ZN)
			return m;

		n->rgt = canonical(n->rgt);
		n->lft = canonical(n->lft);

		return cached(n);
		 */
	}

	public int countNodes()
	{
		switch (kind) {
		case AND:
		case OR:
		case IMPLIES:
		case EQUIV:
		case UNTIL:
		case RELEASE:
			return left.countNodes() + right.countNodes() + 1;
		case NEXT:
		case FINALLY:
		case GLOBALLY:
		case NOT:
			return left.countNodes() + 1;
		default:
			return 1;
		}
	}

	public int countPredicates()
	{
		switch (kind) {
		case AND:
		case OR:
		case IMPLIES:
		case EQUIV:
		case UNTIL:
		case RELEASE:
			return left.countPredicates() + right.countPredicates();
		case NEXT:
		case FINALLY:
		case GLOBALLY:
		case NOT:
			return left.countPredicates();
		case AP:
			return 1;
		default:
			return 0;
		}
	}

	public String toString()
	{
		String rv = "";

		switch (kind) {
		case OR:
			rv = "(" + left.toString() + " || " + right.toString() + ")";
			break;
		case AND:
			rv = "(" + left.toString() + " && " + right.toString() + ")";
			break;
		case UNTIL:
			rv = "(" + left.toString() + " U " + right.toString() + ")";
			break;
		case RELEASE:
			rv = "(" + left.toString() + " V " + right.toString() + ")";
			break;
		case IMPLIES:
			rv = "(" + left.toString() + " -> " + right.toString() + ")";
			break;
		case EQUIV:
			rv = "(" + left.toString() + " <-> " + right.toString() + ")";
			break;
		case NEXT:
			rv = "X " + left.toString();
			break;
		case FINALLY:
			rv = "F " + left.toString();
			break;
		case GLOBALLY:
			rv = "G " + left.toString();
			break;
		case NOT:
			rv = "! " + left.toString();
			break;
		case FALSE:
			rv = "false";
			break;
		case TRUE:
			rv = "true";
			break;
		case AP:
			rv = ap;
			break;
		default:
			rv = null;
		}
		return rv;
	}

	// ltl2dstar stuff
	public boolean isCoSafe()
	{
		switch(kind) {
		case RELEASE:
		case GLOBALLY:
			return false;
		default:
			if (left != null && !left.isCoSafe()) {
				return false;
			}
		if (right != null && !right.isCoSafe()) {
			return false;
		}
		return true;
		}
	}

	public boolean hasNextStep()
	{
		if (kind == LTLType.NEXT) {
			return true;
		}
		if (left != null && left.hasNextStep()) {
			return true;
		}
		if (right != null && right.hasNextStep()) {
			return true;
		}
		return false;
	}

	/**
	 * Returns true if this SimpleLTL structure is a tree, i.e., not a
	 * directed acyclical graph (DAG) where sharing of subtrees is allowed.
	 */
	public boolean isTree()
	{
		// use an IdentityHashMap with (formula,formula) pairs indicating that formula
		// has already been seen (due to lack of IdentityHashSet)
		IdentityHashMap<SimpleLTL, SimpleLTL> seen = new IdentityHashMap<SimpleLTL, SimpleLTL>();
		return isTree(seen);
	}

	/**
	 * Recursive function for checking if this SimpleLTL structure is a tree, i.e., not a
	 * directed acyclical graph (DAG) where sharing of subtrees is allowed.
	 *
	 * The already seen SimpleLTL subtrees are tracked in {@code seen}.
	 */
	private boolean isTree(IdentityHashMap<SimpleLTL,SimpleLTL> seen)
	{
		if (seen.containsKey(this)) {
			return false;
		}
		seen.put(this, this);

		if (left != null && !left.isTree(seen)) {
			// left child exists and is not a tree
			return false;
		}
		if (right != null && !right.isTree(seen)) {
			// right child exists and is not a tree
			return false;
		}

		return true;
	}

	/**
	 * Transform LTL formula by extend each<br>
	 * X phi<br>
	 * node in the syntax tree to<br>
	 * X (label & phi).
	 * <br>
	 * The LTL formula has to consist only of basic operators (see toBasicOperators()).
	 * @param label the label of the atomic proposition
	 * @return the transformed LTL formula
	 */
	public SimpleLTL extendNextStepWithAP(String label)
	{
		switch (kind) {
		case TRUE:
		case FALSE:
		case AP:
			return this.clone();
		case NOT: {
			SimpleLTL newLeft = left.extendNextStepWithAP(label);
			return new SimpleLTL(kind, newLeft);
		}
		case NEXT: {
			SimpleLTL newLeft = left.extendNextStepWithAP(label);
			// transform X phi to X ("label" & phi)
			return new SimpleLTL(LTLType.NEXT,
					new SimpleLTL(LTLType.AND, new SimpleLTL(label), newLeft));
		}
		case OR:
		case AND:
		case RELEASE:
		case UNTIL: {
			SimpleLTL newLeft = left.extendNextStepWithAP(label);
			SimpleLTL newRight = right.extendNextStepWithAP(label);
			return new SimpleLTL(kind, newLeft, newRight);
		}
		case FINALLY:
		case GLOBALLY: {
			SimpleLTL newLeft = left.extendNextStepWithAP(label);
			return new SimpleLTL(kind, newLeft);
		}
		case EQUIV:
		case IMPLIES:
			throw new UnsupportedOperationException("Extending Next operator with AP not supported for " + kind.toString() + " operator, only for basic operators: " + this);
		}
		throw new UnsupportedOperationException();
	}

	public SimpleLTL toDNF() throws PrismException
	{
		switch (kind) {
		case TRUE:
			return new SimpleLTL(true);
		case FALSE:
			return new SimpleLTL(false);
		case NOT:
			return new SimpleLTL(LTLType.NOT, left.toDNF());
		case AP:
			return new SimpleLTL(ap);
		case OR:
			return new SimpleLTL(LTLType.OR, left.toDNF(), right.toDNF());
		case AND:
			SimpleLTL l = left.toDNF();
			SimpleLTL r = right.toDNF();

			if (l.kind == LTLType.OR) {
				SimpleLTL a, b;
				a = l.left;
				b = l.right;

				if (r.kind == LTLType.OR) {
					SimpleLTL c, d;
					c=r.left;
					d=r.right;

					SimpleLTL a_c = new SimpleLTL(LTLType.AND, a, c);
					SimpleLTL b_c = new SimpleLTL(LTLType.AND, b, c);
					SimpleLTL a_d = new SimpleLTL(LTLType.AND, a, d);
					SimpleLTL b_d = new SimpleLTL(LTLType.AND, b, d);

					return new SimpleLTL(LTLType.OR,
							(new SimpleLTL(LTLType.OR, a_c, b_c)).toDNF(),
							(new SimpleLTL(LTLType.OR, a_d, b_d)).toDNF());	  
				} else {
					SimpleLTL a_c = new SimpleLTL(LTLType.AND, a, r);
					SimpleLTL b_c = new SimpleLTL(LTLType.AND, b, r);

					return new SimpleLTL(LTLType.OR, a_c.toDNF(), b_c.toDNF());
				}
			} else if (r.kind == LTLType.OR) {
				SimpleLTL a, b;
				a = r.left;
				b = r.right;

				SimpleLTL a_c = new SimpleLTL(LTLType.AND, l, a);
				SimpleLTL b_c = new SimpleLTL(LTLType.AND, l, b);

				return new SimpleLTL(LTLType.OR, a_c.toDNF(), b_c.toDNF());
			} else {
				return new SimpleLTL(LTLType.AND, l, r);
			}
		default:
			throw new PrismException("Illegal operator for DNF!");
		}
	}

	/** Returns an APMonom representing the formula rooted at
	 * this node. Formula has to be in DNF. */
	public APMonom toMonom(APSet apset) throws PrismException
	{
		APMonom result = new APMonom(true);

		switch (kind) {
		case AND: {
			APMonom l = left.toMonom(apset);
			APMonom r = right.toMonom(apset);

			result = l.and(r);
			return result;
		}
		case NOT:
			switch (left.kind) {
			case AP:
				result.setValue(apset.indexOf(left.ap), false);
				return result;
			case FALSE:
				return new APMonom(true);
			case TRUE:
				return new APMonom(false);
			default:
				throw new PrismException("Formula not in DNF!");
			}	  
		case AP:
			result.setValue(apset.indexOf(ap), true);
			return result;
		case FALSE:
			return new APMonom(false);
		case TRUE:
			return new APMonom(true);
		default:
			throw new PrismException("Formula not in DNF!");
		}
	}

	/**
	 * Renames the atomic propositions apparing in the formula that
	 * start with {@code prefixFrom}. For these, the prefix is replaced
	 * by prefixTo. For example, with prefixFrom = "L" and prefixTo = "p",
	 * "L3" will be renamed to "p3", but "T2" will be left as-is.
	 */
	public void renameAP(String prefixFrom, String prefixTo)
	{
		switch (kind) {
		case AP:
			if (ap.startsWith(prefixFrom)) {
				ap = prefixTo + ap.substring(prefixFrom.length());
			}
			return;
		case AND:
		case OR:
		case EQUIV:
		case IMPLIES:
		case RELEASE:
		case UNTIL:
			left.renameAP(prefixFrom, prefixTo);
			right.renameAP(prefixFrom, prefixTo);
			return;
		case FINALLY:
		case GLOBALLY:
		case NEXT:
		case NOT:
			left.renameAP(prefixFrom, prefixTo);
			return;
		case TRUE:
		case FALSE:
			return;
		}
		throw new UnsupportedOperationException("Unknown operator in SimpleLTL formula: "+this);
	}
	
	/**
	 * Render this LTL formula in LBT syntax, i.e., in prefix notation.
	 */
	public String toStringLBT()
	{
		String rv = "";

		switch (kind) {
		case OR:
			rv = "| " + left.toStringLBT() + " " + right.toStringLBT();
			break;
		case AND:
			rv = "& " + left.toStringLBT() + " " + right.toStringLBT();
			break;
		case UNTIL:
			rv = "U " + left.toStringLBT() + " " + right.toStringLBT();
			break;
		case RELEASE:
			rv = "V " + left.toStringLBT() + " " + right.toStringLBT();
			break;
		case IMPLIES:
			rv = "i " + left.toStringLBT() + " " + right.toStringLBT();
			break;
		case EQUIV:
			rv = "e " + left.toStringLBT() + " " + right.toStringLBT();
			break;
		case NEXT:
			rv = "X " + left.toStringLBT();
			break;
		case FINALLY:
			rv = "F " + left.toStringLBT();
			break;
		case GLOBALLY:
			rv = "G " + left.toStringLBT();
			break;
		case NOT:
			rv = "! " + left.toStringLBT();
			break;
		case FALSE:
			rv = "f";
			break;
		case TRUE:
			rv = "t";
			break;
		case AP:
			rv = ap;
			break;
		default:
			rv = null;
		}
		return rv;
	}

	/**
	 * Render this LTL formula in Spin syntax.
	 */
	public String toStringSpin()
	{
		String rv = "";

		switch (kind) {
		case OR:
			rv = "(" + left.toStringSpin() + ") || (" + right.toStringSpin() + ")";
			break;
		case AND:
			rv = "(" + left.toStringSpin() + ") && (" + right.toStringSpin() + ")";
			break;
		case UNTIL:
			rv = "(" + left.toStringSpin() + ") U (" + right.toStringSpin() + ")";
			break;
		case RELEASE:
			rv = "(" + left.toStringSpin() + ") V (" + right.toStringSpin() + ")";
			break;
		case IMPLIES:
			rv = "(" + left.toStringSpin() + ") -> (" + right.toStringSpin() + ")";
			break;
		case EQUIV:
			rv = "(" + left.toStringSpin() + ") <-> (" + right.toStringSpin() + ")";
			break;
		case NEXT:
			rv = "X (" + left.toStringSpin() + ")";
			break;
		case FINALLY:
			rv = "<> (" + left.toStringSpin() + ")";
			break;
		case GLOBALLY:
			rv = "[] (" + left.toStringSpin() + ")";
			break;
		case NOT:
			rv = "! (" + left.toStringSpin() + ")";
			break;
		case FALSE:
			rv = "false";
			break;
		case TRUE:
			rv = "true";
			break;
		case AP:
			rv = ap;
			break;
		default:
			rv = null;
		}
		return rv;
	}

	
	/**
	 * Render this LTL formula in Spot syntax.
	 */
	public String toStringSpot()
	{
		String rv = "";

		switch (kind) {
		case OR:
			rv = "(" + left.toStringSpot() + ") | (" + right.toStringSpot() + ")";
			break;
		case AND:
			rv = "(" + left.toStringSpot() + ") & (" + right.toStringSpot() + ")";
			break;
		case UNTIL:
			rv = "(" + left.toStringSpot() + ") U (" + right.toStringSpot() + ")";
			break;
		case RELEASE:
			rv = "(" + left.toStringSpot() + ") R (" + right.toStringSpot() + ")";
			break;
		case IMPLIES:
			rv = "(" + left.toStringSpot() + ") -> (" + right.toStringSpot() + ")";
			break;
		case EQUIV:
			rv = "(" + left.toStringSpot() + ") <-> (" + right.toStringSpot() + ")";
			break;
		case NEXT:
			rv = "X (" + left.toStringSpot() + ")";
			break;
		case FINALLY:
			rv = "F (" + left.toStringSpot() + ")";
			break;
		case GLOBALLY:
			rv = "G (" + left.toStringSpot() + ")";
			break;
		case NOT:
			rv = "! (" + left.toStringSpot() + ")";
			break;
		case FALSE:
			rv = "false";
			break;
		case TRUE:
			rv = "true";
			break;
		case AP:
			rv = ap;
			break;
		default:
			rv = null;
		}
		return rv;
	}

	/** Parse a formula in LBT (prefix format) */
	public static SimpleLTL parseFormulaLBT(String formula) throws Exception
	{
		formula=formula.trim();  // remove leading, trailing spaces
		String[] split = formula.split("[ ]+");
		List<String> formulaList = new ArrayList<String>();
		for (String s : split) formulaList.add(s);

		// set up operator -> SimpleLTL.LTLType mapping for the standard operators
		Map<String, SimpleLTL.LTLType> unaryOps  = new HashMap<String, SimpleLTL.LTLType>();
		Map<String, SimpleLTL.LTLType> binaryOps = new HashMap<String, SimpleLTL.LTLType>();
		unaryOps.put("!", SimpleLTL.LTLType.NOT);
		unaryOps.put("F", SimpleLTL.LTLType.FINALLY);
		unaryOps.put("G", SimpleLTL.LTLType.GLOBALLY);
		unaryOps.put("X", SimpleLTL.LTLType.NEXT);
		binaryOps.put("|", SimpleLTL.LTLType.OR);
		binaryOps.put("&", SimpleLTL.LTLType.AND);
		binaryOps.put("i", SimpleLTL.LTLType.IMPLIES);
		binaryOps.put("e", SimpleLTL.LTLType.EQUIV);
		binaryOps.put("U", SimpleLTL.LTLType.UNTIL);
		binaryOps.put("V", SimpleLTL.LTLType.RELEASE);

		SimpleLTL result = parseFormulaLBT(formulaList, unaryOps, binaryOps);

		if (formulaList.size()>0) {
			String remainingFormula = "";
			for (String op : formulaList) remainingFormula += " "+op;
			throw new RuntimeException("Malformed formula, extra information after end of formula: "+remainingFormula);
		}
		return result;
	}

	/**
	 * Parse a formula in LBT format, where the formula is already split into a list of operators / APs
	 * @param formulaList sequence of operators / APs
	 * @param unaryOps map for the standard unary ops
	 * @param binaryOps map for the standard binary ops
	 * @throws RuntimeException on parse error
	 */
	private static SimpleLTL parseFormulaLBT(List<String> formulaList,
	                                         Map<String, SimpleLTL.LTLType> unaryOps,
	                                         Map<String, SimpleLTL.LTLType> binaryOps) throws RuntimeException
	{
		if (formulaList.size() == 0) {
			throw new RuntimeException("Malformed formula, premature ending");
		}
		String current = formulaList.get(0);
		formulaList.remove(0);

		if (current.equals("t")) {
			return new SimpleLTL(true);
		} else if (current.equals("f")) {
			return new SimpleLTL(false);
		} else if (unaryOps.containsKey(current)) {
			// standard unary op
			SimpleLTL operand = parseFormulaLBT(formulaList, unaryOps, binaryOps);
			return new SimpleLTL(unaryOps.get(current), operand);
		} else if (binaryOps.containsKey(current)) {
			// standard binary op
			SimpleLTL operand1 = parseFormulaLBT(formulaList, unaryOps, binaryOps);
			SimpleLTL operand2 = parseFormulaLBT(formulaList, unaryOps, binaryOps);
			return new SimpleLTL(binaryOps.get(current), operand1, operand2);
		} else if (current.equals("W")) {
			// a W b == !(a&!b U !a&!b)
			SimpleLTL operand1 = parseFormulaLBT(formulaList, unaryOps, binaryOps);
			SimpleLTL operand2 = parseFormulaLBT(formulaList, unaryOps, binaryOps);

			SimpleLTL aAndNotb =
			   new SimpleLTL(SimpleLTL.LTLType.AND, operand1.clone(),
			     new SimpleLTL(SimpleLTL.LTLType.NOT, operand2.clone()));
			
			SimpleLTL NotaAndNotb =
			   new SimpleLTL(SimpleLTL.LTLType.AND,
			     new SimpleLTL(SimpleLTL.LTLType.NOT, operand1.clone()),
			     new SimpleLTL(SimpleLTL.LTLType.NOT, operand2.clone()));

			return new SimpleLTL(SimpleLTL.LTLType.NOT,
			   new SimpleLTL(SimpleLTL.LTLType.UNTIL, aAndNotb, NotaAndNotb));
		} else if (current.equals("^")) {
			// a xor b == !(a equiv b)
			SimpleLTL operand1 = parseFormulaLBT(formulaList, unaryOps, binaryOps);
			SimpleLTL operand2 = parseFormulaLBT(formulaList, unaryOps, binaryOps);

			return new SimpleLTL(SimpleLTL.LTLType.NOT,
			    new SimpleLTL(SimpleLTL.LTLType.EQUIV, operand1, operand2));
		} else if (current.equals("M") || current.equals("B")) {
			throw new RuntimeException("Operator "+current+" currently not supported.");
		} else if (current.matches("[a-zA-Z].*")) {
			// atomic proposition
			return new SimpleLTL(current);
		} else {
			throw new RuntimeException("Illegal/unsupported operator: "+current);
		}
	}

	public NBA toNBA(APSet apset) throws PrismException
	{
		Alternating a = new Alternating(this, apset);
		// a.print(System.out);
		Generalized g = new Generalized(a);
		// g.print(System.out, apset);
		Buchi b = new Buchi(g);
		// b.print_spin(System.out, apset);
		NBA nba = b.toNBA(apset);
		// nba.print(System.out);

		// jltl2ba should never produce disjoint NBA,
		// i.e., where some states are not reachable
		// from the intial state, so we want to fail
		// later if the NBA is discovered to be disjoint:
		nba.setFailIfDisjoint(true);

		return nba;
	}
	
	public NBA toNBA() throws PrismException
	{
		return this.toNBA(new APSet());
	}

	/** Print a DOT representation of the syntax tree of this SimpleLTL formula */
	public void toDot(PrintStream out) {
		IdentityHashMap<SimpleLTL, String> map = new IdentityHashMap<SimpleLTL, String>();

		out.println("digraph {");
		toDot(out, map);
		out.println("}");
	}

	/**
	 * Print a DOT representation of the syntax tree of this SimpleLTL formula.
	 *
	 * @param out the output print stream
	 * @param seen a map storing an identifier for each subformula that has already been seen / printed
	 */
	private String toDot(PrintStream out, IdentityHashMap<SimpleLTL, String> seen)
	{
		String id = seen.get(this);
		if (id != null) {
			return id;
		}

		id = Integer.toString(seen.size());
		seen.put(this, id);
		out.println(id + " [label=\""+toStringLBT()+"\"]");

		switch (kind) {
		case AND:
		case EQUIV:
		case OR:
		case UNTIL:
		case RELEASE:
		case IMPLIES: {
			String leftID = left.toDot(out, seen);
			String rightID = right.toDot(out, seen);
			out.println(id + " -> " + leftID);
			out.println(id + " -> " + rightID);
			break;
		}
		case FINALLY:
		case GLOBALLY:
		case NEXT:
		case NOT: {
			String leftID = left.toDot(out, seen);
			out.println(id + " -> " + leftID);
			break;
		}

		case AP:
		case TRUE:
		case FALSE:
			break;

		default:
			break;
		}

		return id;
	}

}
