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

import java.util.Vector;
import java.io.PrintStream;

import jltl2ba.SimpleLTL;
import prism.PrismException;

public class Alternating {
	public int astate_count;
	public int atrans_count;
	private Vector<SimpleLTL> done;
	public Vector<ATrans> transition;
	public MyBitSet final_set;
	public APSet sym_table;

	public static class ATrans {
		public MyBitSet to; // nodes
		public MyBitSet pos; // syms
		public MyBitSet neg; // syms
		public ATrans nxt;

		public ATrans() {
			to = new MyBitSet();
			pos = new MyBitSet();
			neg = new MyBitSet();
			nxt = null;
		}

		public ATrans(MyBitSet _to, MyBitSet _pos, MyBitSet _neg) {
			to = (MyBitSet) _to.clone();
			pos = (MyBitSet) _pos.clone();
			neg = (MyBitSet) _neg.clone();
			nxt = null;
		}

		public ATrans clone() {
			ATrans rv = new ATrans(to, pos, neg);
			rv.nxt = nxt;
			return rv;
		}
	}
	
	public static ATrans do_merge_atrans(ATrans first, ATrans second) {
		if (first == null || second == null)
			return null;

		ATrans rv = first.clone();
		rv.nxt = null;

		rv.to.or(second.to);
		rv.pos.or(second.pos);
		rv.neg.or(second.neg);

		if (rv.pos.intersects(rv.neg))
			rv = null;
		return rv;
	}

	public static class AProd {
		public int astate;
		public ATrans prod;
		public ATrans trans;
		public AProd nxt;
		public AProd prv;

		public AProd() {
			;
		}
	}

	public Alternating(SimpleLTL formula, APSet apset) throws PrismException {
		astate_count = 0;
		atrans_count = 0;
		final_set = new MyBitSet();
		transition = new Vector<ATrans>();
		done = new Vector<SimpleLTL>();
		sym_table = apset;
		SimpleLTL p = formula.clone();
		// System.out.println("formula: " + p.toString());
		p = p.simplify();
		// System.out.println("after simplification: " + p.toString());

		done.add(null); // Reserve 0
		transition.add(null);
		transition.set(0, _boolean(p));
		// System.out.println("Alternating automaton before simplification");
		// print(System.out);
		simplifyAStates();
		// System.out.println("Alternating automaton after simplification");
		// print(System.out);
		done.clear();
	}

	/* computes the transitions to boolean nodes -> next & init */
	private ATrans _boolean(SimpleLTL p) {
		ATrans t1, t2, lft, rgt, result = null;

		switch (p.kind) {
		case TRUE:
			result = new ATrans();
			break;
		case FALSE:
			break;
		case AND:
			lft = _boolean(p.left);
			rgt = _boolean(p.right);
			for (t1 = lft; t1 != null; t1 = t1.nxt) {
				for (t2 = rgt; t2 != null; t2 = t2.nxt) {
					ATrans tmp = do_merge_atrans(t1, t2);
					if (tmp != null) {
						tmp.nxt = result;
						result = tmp;
					}
				}
			}
			break;
		case OR:
			lft = _boolean(p.left);
			for (t1 = lft; t1 != null; t1 = t1.nxt) {
				ATrans tmp = t1.clone();
				tmp.nxt = result;
				result = tmp;
			}
			rgt = _boolean(p.right);
			for (t1 = rgt; t1 != null; t1 = t1.nxt) {
				ATrans tmp = t1.clone();
				tmp.nxt = result;
				result = tmp;
			}
			break;
		default:
			buildAlternating(p);
			result = new ATrans();
			result.to.set(done.indexOf(p));
		}
		return result;
	}

	private int getSymID(String ap) {
		return sym_table.addAP(ap);
	}

	/* builds an alternating automaton for p */
	private ATrans buildAlternating(SimpleLTL p) {
		ATrans t1, t2, t = null;

		if (done.contains(p))
			return transition.get(done.indexOf(p));

		switch (p.kind) {

		case TRUE:
			t = new ATrans();
			break;

		case FALSE:
			break;

		case AP:
			t = new ATrans();
			t.pos.set(getSymID(p.ap));
			break;

		case NOT:
			t = new ATrans();
			t.neg.set(getSymID(p.left.ap)); // Should be in negated normal form
			break;

		case NEXT:
			t = _boolean(p.left);
			break;

		case UNTIL: /* p U q <-> q || (p && X (p U q)) */
			for (t2 = buildAlternating(p.right); t2 != null; t2 = t2.nxt) {
				ATrans tmp = t2.clone(); /* q */
				tmp.nxt = t;
				t = tmp;
			}
			for (t1 = buildAlternating(p.left); t1 != null; t1 = t1.nxt) {
				ATrans tmp = t1.clone(); /* p */
				tmp.to.set(done.size()); /* X (p U q) */
				tmp.nxt = t;
				t = tmp;
			}
			final_set.set(done.size());
			break;

		case RELEASE: /* p V q <-> (p && q) || (q && X (p V q)) */
			for (t1 = buildAlternating(p.right); t1 != null; t1 = t1.nxt) {
				ATrans tmp;

				for (t2 = buildAlternating(p.left); t2 != null; t2 = t2.nxt) {
					tmp = do_merge_atrans(t1, t2); /* p && q */
					if (tmp != null) {
						tmp.nxt = t;
						t = tmp;
					}
				}

				tmp = t1.clone(); /* q */
				tmp.to.set(done.size()); /* X (p V q) */
				tmp.nxt = t;
				t = tmp;
			}
			break;

		case AND:
			for (t1 = buildAlternating(p.left); t1 != null; t1 = t1.nxt) {
				for (t2 = buildAlternating(p.right); t2 != null; t2 = t2.nxt) {
					ATrans tmp = do_merge_atrans(t1, t2);
					if (tmp != null) {
						tmp.nxt = t;
						t = tmp;
					}
				}
			}
			break;

		case OR:
			for (t1 = buildAlternating(p.left); t1 != null; t1 = t1.nxt) {
				ATrans tmp = t1.clone();
				tmp.nxt = t;
				t = tmp;
			}
			for (t1 = buildAlternating(p.right); t1 != null; t1 = t1.nxt) {
				ATrans tmp = t1.clone();
				tmp.nxt = t;
				t = tmp;
			}
			break;

		default:
			break;
		}
		transition.add(t);
		done.add(p);
		return t;
	}

	private ATrans simplifyATrans(ATrans trans) {
		ATrans t, father = null;
		ATrans _trans = trans;
		for (t = _trans; t != null;) {
			ATrans t1;
			for (t1 = _trans; t1 != null; t1 = t1.nxt) {
				if ((t1 != t) && t.to.containsAll(t1.to)
						&& t.pos.containsAll(t1.pos)
						&& t.neg.containsAll(t1.neg))
					break;
			}
			if (t1 != null) {
				if (father != null)
					father.nxt = t.nxt;
				else
					_trans = t.nxt;
				if (father != null)
					t = father.nxt;
				else
					t = _trans;
				continue;
			}
			atrans_count++;
			father = t;
			t = t.nxt;
		}
		return _trans;
	}

	/* simplifies the alternating automaton */
	private void simplifyAStates() {
		ATrans t;
		int i;
		MyBitSet acc = new MyBitSet(); /* no state is accessible initially */

		for (t = transition.get(0); t != null; t = t.nxt, i = 0)
			acc.or(t.to); /* all initial states are accessible */

		for (i = transition.size() - 1; i > 0; i--) {
			if (!acc.get(i)) { // frees unaccessible states
				done.set(i, null);
				transition.set(i, null);
				continue;
			}
			astate_count++;
			transition.set(i, simplifyATrans(transition.get(i)));
			for (t = transition.get(i); t != null; t = t.nxt)
				acc.or(t.to);
		}
	}

	/* dumps the alternating automaton */
	public void print(PrintStream out) {
		int i;
		ATrans t;

		out.print("init :\n");
		for (t = transition.get(0); t != null; t = t.nxt) {
			t.to.print(out);
			out.println();
		}

		for (i = done.size() - 1; i > 0; i--) {
			if (done.get(i) == null)
				continue;
			out.format("state %d : ", i);
			out.print(done.get(i).toString());
			out.println();
			for (t = transition.get(i); t != null; t = t.nxt) {
				if (t.pos.isEmpty() && t.neg.isEmpty())
					out.print("1");
				t.pos.print(out, sym_table, true);
				if (!t.pos.isEmpty() && !t.neg.isEmpty())
					out.print(" & ");
				t.neg.print(out, sym_table, false);
				out.print(" -> ");
				t.to.print(out);
				out.println();
			}
		}
	}
}