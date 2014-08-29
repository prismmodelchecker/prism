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
import java.util.HashMap;
import java.io.PrintStream;

import jltl2dstar.APMonom;
import jltl2dstar.NBA;

import prism.PrismException;

public class Buchi {

	private int init_size;
	private Vector<Generalized.GState> g_init;
	private Vector<Integer> _final;
	// private Generalized.GState gstates;
	
	private BState bstack;
	private BState bstates;
	private BState bremoved;
	private BScc scc_stack;
	private int accept;
	private int bstate_count;
	private int btrans_count;
	private int rank;
	
	// the highest id used for a BState.id
	private int max_id;

	public static class BState {
		Generalized.GState gstate;
		public int id;
		public int incoming;
		public int _final;
		BTrans trans;
		BState nxt;
		BState prv;
		
		public BState() { ; }
		
		public void free()
		{
			if (trans.nxt != null)
				trans.nxt.free(trans, true);
		}
	}

	public static class BScc {
		BState bstate;
		int rank;
		int theta;
		BScc nxt;
		
		public BScc() { ; }
	}

	public class BTrans {
		public MyBitSet pos;
		public MyBitSet neg;
		public BState to;
		public BTrans nxt;
		
		public BTrans()
		{
			pos = new MyBitSet();
			neg = new MyBitSet();
			nxt = null;
			to = null;
		}
		
		public BTrans(MyBitSet _pos, MyBitSet _neg)
		{
			pos = (MyBitSet) _pos.clone();
			neg = (MyBitSet) _neg.clone();
			nxt = null;
			to = null;
		}
		
		public BTrans clone()
		{
			BTrans rv = new BTrans(pos, neg);
			rv.to = to;
			rv.nxt = nxt;
			return rv;
		}
		
		public void copyTo(BTrans b)
		{
			b.pos = (MyBitSet) pos.clone();
			b.neg = (MyBitSet) neg.clone();
			b.to = to;
		}
		
		public void free(BTrans sentinel, boolean fly)
		{
			if (this != sentinel) {
				nxt.free(sentinel, fly);
				if (fly)
					to.incoming--;
			}
		}
	}
	
	public Buchi(Generalized g)
	{				/* generates a Buchi automaton from the generalized Buchi automaton */
		init_size = g.init_size;
		g_init = g.g_init;
		_final = g._final;
		// gstates = g.gstates;
		max_id = g.getGStateID();
		
		int i;
		BState s = new BState();
		Generalized.GTrans t;
		BTrans t1;
		accept = g._final.size();
		bstate_count = 0;
		btrans_count = 0;

		bstack = new BState();
		bstack.nxt = bstack;
		bremoved = new BState();
		bremoved.nxt = bremoved;
		bstates = new BState();
		bstates.nxt = s;
		bstates.prv = s;

		s.nxt = bstates;	/* creates (unique) inital state */
		s.prv = bstates;
		s.id = -1;
		s.incoming = 1;
		s._final = 0;
		s.gstate = null;
		s.trans = new BTrans();	/* sentinel */
		s.trans.nxt = s.trans;
		for (i = 0; i < init_size; i++) {
			if (g_init.get(i) != null) {
				for (t = g_init.get(i).trans.nxt; t != g_init.get(i).trans; t = t.nxt) {
					int fin = nextFinal(t._final, 0);
					BState to = findBState(t.to, fin, s);
					for (t1 = s.trans.nxt; t1 != s.trans;) {
						if ((to == t1.to) && t1.pos.containsAll(t.pos) && t1.neg.containsAll(t.neg)) {	/* t1 is redundant */
							BTrans free = t1.nxt;
							t1.to.incoming--;
							t1.to = free.to;
							t1.pos = (MyBitSet) free.pos.clone();
							t1.neg = (MyBitSet) free.neg.clone();
							t1.nxt = free.nxt;
							if (free == s.trans)
								s.trans = t1;
							free = null;
						} else if ((t1.to == to) && t.pos.containsAll(t1.pos) && t.neg.containsAll(t1.neg))	/* t is redundant */
							break;
						else
							t1 = t1.nxt;
					}
					if (t1 == s.trans) {
						BTrans trans = new BTrans();
						trans.to = to;
						trans.to.incoming++;
						trans.pos = (MyBitSet) t.pos.clone();
						trans.neg = (MyBitSet) t.neg.clone();
						trans.nxt = s.trans.nxt;
						s.trans.nxt = trans;
					}
				}
			}
		}
		while (bstack.nxt != bstack) {	/* solves all states in the stack until it is empty */
			s = bstack.nxt;
			bstack.nxt = bstack.nxt.nxt;
			if (s.incoming == 0) {
				s.free();
				continue;
			}
			makeBTrans(s);
		}
		retargetAllBTrans();
		
		// System.out.println("Buchi automaton before simplification:");
		// print_spin(System.out, symtab);

		simplifyBTrans();
		simplifyBScc();
		while (simplifyBStates() != 0) {	/* simplifies as much as possible */
			simplifyBTrans();
			simplifyBScc();
		}
		// System.out.println("Buchi automaton after simplification:");
		// print_spin(System.out, symtab);

	}

	private int nextFinal(MyBitSet set, int fin)
	{				/* computes the 'final' value */
		if ((fin != accept) && set.get(_final.get(fin)))
			return nextFinal(set, fin + 1);
		return fin;
	}

	private BState findBState(Generalized.GState state, int _final, BState s)
	{	
		/* finds the corresponding state, or creates it */
		if ((s.gstate == state) && (s._final == _final))
			return s;	/* same state */

		s = bstack.nxt;	/* in the stack */
		bstack.gstate = state;
		bstack._final = _final;
		while (!(s.gstate == state) || !(s._final == _final))
			s = s.nxt;
		if (s != bstack)
			return s;

		s = bstates.nxt;	/* in the solved states */
		bstates.gstate = state;
		bstates._final = _final;
		while (!(s.gstate == state) || !(s._final == _final))
			s = s.nxt;
		if (s != bstates)
			return s;

		s = bremoved.nxt;	/* in the removed states */
		bremoved.gstate = state;
		bremoved._final = _final;
		while (!(s.gstate == state) || !(s._final == _final))
			s = s.nxt;
		if (s != bremoved)
			return s;

		s = new BState();
		s.gstate = state;
		s.id = state.id;
		s.incoming = 0;
		s._final = _final;
		s.trans = new BTrans();
		s.trans.nxt = s.trans;
		s.nxt = bstack.nxt;
		bstack.nxt = s;
		return s;
	}
	
	private boolean sameBTrans(BTrans s, BTrans t)
	{	
		/* returns 1 if the transitions are identical */
		return ((s.to == t.to) &&
			s.pos.equals(t.pos) && s.neg.equals(t.neg));
	}
	
	private boolean allBTransMatch(BState a, BState b)
	{	
		/* decides if the states are equivalent */
		BTrans s, t;
		
		/* the states have to be both final or both non final,
		 * or at least one of them has to be in a trivial SCC
		 * (incoming == -1), as the acceptance condition of
		 * such a state can be modified without changing the
		 * language of the automaton
		 */
		if (((a._final == accept) || (b._final == accept)) &&
		    (a._final + b._final != 2 * accept) /* final condition of a and b differs */
		    && a.incoming >= 0   /* a is not in a trivial SCC */
		    && b.incoming >= 0)  /* b is not in a trivial SCC */
			return false;   /* states can not be matched */

		for (s = a.trans.nxt; s != a.trans; s = s.nxt) {
			/* all transitions from a appear in b */
			s.copyTo(b.trans);
			t = b.trans.nxt;
			while (!sameBTrans(s, t))
				t = t.nxt;
			if (t == b.trans)
				return false;
		}
		for (s = b.trans.nxt; s != b.trans; s = s.nxt) {
			/* all transitions from b appear in a */
			s.copyTo(a.trans);
			t = a.trans.nxt;
			while (!sameBTrans(s, t))
				t = t.nxt;
			if (t == a.trans)
				return false;
		}
		return true;
	}

	private void makeBTrans(BState s)
	{				/* creates all the transitions from a state */
		int state_trans = 0;
		Generalized.GTrans t;
		BTrans t1;
		BState s1;
		if (s.gstate.trans != null)
			for (t = s.gstate.trans.nxt; t != s.gstate.trans;
			     t = t.nxt) {
				int fin = nextFinal(t._final, (s._final == accept) ? 0 : s._final);
				BState to = findBState(t.to, fin, s);

				for (t1 = s.trans.nxt; t1 != s.trans;) {
					if ((to == t1.to) && t1.pos.containsAll(t.pos) && t1.neg.containsAll(t.neg)) {	/* t1 is redundant */
						BTrans free = t1.nxt;
						t1.to.incoming--;
						t1.to = free.to;
						t1.pos = (MyBitSet) free.pos.clone();
						t1.neg = (MyBitSet) free.neg.clone();
						t1.nxt = free.nxt;
						if (free == s.trans)
							s.trans = t1;
						free = null;
						state_trans--;
					} else if ((t1.to == to) && t.pos.containsAll(t1.pos) && t.neg.containsAll(t1.neg))	/* t is redundant */
						break;
					else
						t1 = t1.nxt;
				}
				if (t1 == s.trans) {
					BTrans trans = new BTrans();
					trans.to = to;
					trans.to.incoming++;
					trans.pos = (MyBitSet) t.pos.clone();
					trans.neg = (MyBitSet) t.neg.clone();
					trans.nxt = s.trans.nxt;
					s.trans.nxt = trans;
					state_trans++;
				}
			}

		if (s.trans == s.trans.nxt) {	/* s has no transitions */
			s.trans.nxt.free(s.trans, true);
			s.trans = null;
			s.prv = null;
			s.nxt = bremoved.nxt;
			bremoved.nxt = s;
			for (s1 = bremoved.nxt; s1 != bremoved; s1 = s1.nxt)
				if (s1.prv == s)
					s1.prv = null;
			return;
		}
		bstates.trans = s.trans;
		bstates._final = s._final;
		s1 = bstates.nxt;
		while (!allBTransMatch(s, s1))
			s1 = s1.nxt;
		if (s1 != bstates) {	/* s and s1 are equivalent */
			s.trans.nxt.free(s.trans, true);
			s.trans = null;
			s.prv = s1;
			s.nxt = bremoved.nxt;
			bremoved.nxt = s;
			for (s1 = bremoved.nxt; s1 != bremoved; s1 = s1.nxt)
				if (s1.prv == s)
					s1.prv = s.prv;
			return;
		}
		s.nxt = bstates.nxt;	/* adds the current state to 'bstates' */
		s.prv = bstates;
		s.nxt.prv = s;
		bstates.nxt = s;
		btrans_count += state_trans;
		bstate_count++;
	}
	
	private void retargetAllBTrans()
	{	
		/* redirects transitions before removing a state from the automaton */
		BState s;
		BTrans t;
		for (s = bstates.nxt; s != bstates; s = s.nxt)
			for (t = s.trans.nxt; t != s.trans; t = t.nxt)
				if (t.to.trans == null) {	/* t.to has been removed */
					t.to = t.to.prv;
					if (t.to == null) {	/* t.to has no transitions */
						BTrans free = t.nxt;
						t.to = free.to;
						t.pos = (MyBitSet) free.pos.clone();
						t.neg = (MyBitSet) free.neg.clone();
						t.nxt = free.nxt;
						if (free == s.trans)
							s.trans = t;
						free = null;
					}
				}
		while (bremoved.nxt != bremoved) {	/* clean the 'removed' list */
			s = bremoved.nxt;
			bremoved.nxt = bremoved.nxt.nxt;
			s = null;
		}
	}

	private int simplifyBTrans()
	{				/* simplifies the transitions */
		BState s;
		BTrans t, t1;
		int changed = 0;

		for (s = bstates.nxt; s != bstates; s = s.nxt)
			for (t = s.trans.nxt; t != s.trans;) {
				t1 = s.trans.nxt;
				t.copyTo(s.trans);
				while ((t == t1) || (t.to != t1.to) ||
				       !t.pos.containsAll(t1.pos) ||
				       !t.neg.containsAll(t1.neg))
					t1 = t1.nxt;
				if (t1 != s.trans) {
					BTrans free = t.nxt;
					t.to = free.to;
					t.pos = (MyBitSet) free.pos.clone();
					t.neg = (MyBitSet) free.neg.clone();
					t.nxt = free.nxt;
					if (free == s.trans)
						s.trans = t;
					free = null;
					changed++;
				} else
					t = t.nxt;
			}
		return changed;
	}

	private int bdfs(BState s)
	{
		BTrans t;
		BScc c;
		BScc scc = new BScc();
		scc.bstate = s;
		scc.rank = rank;
		scc.theta = rank++;
		scc.nxt = scc_stack;
		scc_stack = scc;

		s.incoming = 1;

		for (t = s.trans.nxt; t != s.trans; t = t.nxt) {
			if (t.to.incoming == 0) {
				int result = bdfs(t.to);
				scc.theta = scc.theta < result ? scc.theta : result;
			} else {
				for (c = scc_stack.nxt; c != null; c = c.nxt)
					if (c.bstate == t.to) {
						scc.theta = scc.theta < c.rank ? scc.theta : c.rank;
						break;
					}
			}
		}
		if (scc.rank == scc.theta) {
			if (scc_stack == scc) {	/* s is alone in a scc */
				s.incoming = -1;
				for (t = s.trans.nxt; t != s.trans; t = t.nxt)
					if (t.to == s)
						s.incoming = 1;
			}
			scc_stack = scc.nxt;
		}
		return scc.theta;
	}
	
	private BState removeBState(BState s, BState s1)
	{				/* removes a state */
		BState prv = s.prv;
		s.prv.nxt = s.nxt;
		s.nxt.prv = s.prv;
		s.trans.nxt.free(s.trans, false);
		s.trans = null;
		s.nxt = bremoved.nxt;
		bremoved.nxt = s;
		s.prv = s1;
		for (s1 = bremoved.nxt; s1 != bremoved; s1 = s1.nxt)
			if (s1.prv == s)
				s1.prv = s.prv;
		return prv;
	}

	private void simplifyBScc()
	{
		BState s;
		rank = 1;
		scc_stack = null;

		if (bstates == bstates.nxt)
			return;

		for (s = bstates.nxt; s != bstates; s = s.nxt)
			s.incoming = 0;	/* state color = white */

		bdfs(bstates.prv);

		for (s = bstates.nxt; s != bstates; s = s.nxt)
			if (s.incoming == 0)
				removeBState(s, null);
	}

	private int simplifyBStates()
	{	
		/* eliminates redundant states */
		BState s, s1;
		int changed = 0;

		for (s = bstates.nxt; s != bstates; s = s.nxt) {
			if (s.trans == s.trans.nxt) {	/* s has no transitions */
				s = removeBState(s, null);
				changed++;
				continue;
			}
			bstates.trans = s.trans;
			bstates._final = s._final;
			s1 = s.nxt;
			while (!allBTransMatch(s, s1))
				s1 = s1.nxt;
			if (s1 != bstates) {	/* s and s1 are equivalent */
				/* we now want to remove s and replace it by s1 */
				if (s1.incoming == -1) {  /* s1 is in a trivial SCC */
					s1._final = s._final;  /* change the final condition of s1 to that of s */

					/* We may have to update the SCC status of s1
					 * stored in s1->incoming, because we will retarget the incoming
					 * transitions of s to s1.
					 *
					 * If both s1 and s are in trivial SCC, then retargeting
					 * the incoming transitions does not change the status of s1,
					 * it remains in a trivial SCC.
					 *
					 * If s1 was in a trivial SCC, but s was not, then
					 * s1 has to have a transition to s that corresponds to a
					 * self-loop of s (as both states have the same outgoing transitions).
					 * But then, s1 will not remain a trivial SCC after retargeting.
					 * In particular, afterwards the final condition of s1 may not be
					 * changed anymore.
					 *
					 * If both s1 and s are in non-trivial SCC, merging does not
					 * change the SCC status of s1.
					 *
					 * If we are here, s1->incoming==1 and thus s1 forms a trivial SCC.
					 * We therefore can set the status of s1 to that of s,
					 * which correctly handles the first two cases above.
					 */
					s1.incoming = s.incoming;
				}
				s = removeBState(s, s1);
				changed++;
			}
		}
		retargetAllBTrans();

		 /*
		  * As merging equivalent states can change the 'final' attribute of
		  * the remaining state, it is possible that now there are two
		  * different states with the same id and final values.
		  * This would lead to multiply-defined labels in the generated neverclaim.
		  * We iterate over all states and assign new ids (previously unassigned)
		  * to these states to disambiguate.
		  * Fix from ltl3ba.
		  */
		for (s = bstates.nxt; s != bstates; s = s.nxt) {          /* For all states s*/
			for (BState s2 = s.nxt; s2 != bstates; s2 = s2.nxt) { /*  and states s2 to the right of s */
				if(s._final == s2._final && s.id == s2.id) {      /* if final and id match */
					s.id = ++max_id;                              /* disambiguate by assigning unused id */
				}
			}
		}

		return changed;
	}

	private void spin_print_sets(PrintStream out, APSet apset, MyBitSet pos, MyBitSet neg)
	{				/* prints the content of a set for spin */
		int i;
		boolean start = true;
		for (i = 0; i < (pos.size() > neg.size() ? pos.size() : neg.size()); i++) {
			if (pos.get(i)) {
				if (!start)
					out.print(" && ");
				out.print(apset.getAP(i));
				start = false;
			}
			if (neg.get(i)) {
				if (!start)
					out.print(" && ");
				out.print("!");
				out.print(apset.getAP(i));
				start = false;
			}
		}
		if (start)
			out.print("1");
	}
	
	
	public void print_spin(PrintStream out, APSet apset)
	{
		BTrans t;
		BState s;
		int accept_all = 0;
		if (bstates.nxt == bstates) {	/* empty automaton */
			out.println("never {");
			out.println("T0_init:");
			out.println("\tfalse;");
			out.println("}");
			return;
		}
		if (bstates.nxt.nxt == bstates && bstates.nxt.id == 0) {	/* true */
			out.println("never {");
			out.println("accept_init:");
			out.println("\tif");
			out.println("\t:: (1) . goto accept_init");
			out.println("\tfi;");
			out.println("}");
			return;
		}

		out.println("never {");
		for (s = bstates.prv; s != bstates; s = s.prv) {
			if (s.id == 0) {	/* accept_all at the end */
				accept_all = 1;
				continue;
			}
			if (s._final == accept)
				out.print("accept_");
			else
				out.format("T%d_", s._final);
			if (s.id == -1)
				out.println("init:");
			else {
				out.format("S%d:", s.id);
				out.println();
			}
			if (s.trans.nxt == s.trans) {
				out.println("\tfalse;");
				continue;
			}
			out.println("\tif");
			for (t = s.trans.nxt; t != s.trans; t = t.nxt) {
				BTrans t1;
				out.print("\t:: (");
				spin_print_sets(out, apset, t.pos, t.neg);
				for (t1 = t; t1.nxt != s.trans;)
					if (t1.nxt.to.id == t.to.id &&
					    t1.nxt.to._final == t.to._final) {
						out.print(") || (");
						spin_print_sets(out, apset, t1.nxt.pos, t1.nxt.neg);
						t1.nxt = t1.nxt.nxt;
					} else
						t1 = t1.nxt;
				out.print(") . goto ");
				if (t.to._final == accept)
					out.print("accept_");
				else
					out.format("T%d_", t.to._final);
				if (t.to.id == 0)
					out.println("all");
				else if (t.to.id == -1)
					out.println("init");
				else {
					out.format("S%d", t.to.id);
					out.println();
				}
			}
			out.println("\tfi;");
		}
		if (accept_all != 0) {
			out.println("accept_all:");
			out.println("\tskip");
		}
		out.println("}");
	}
	
	private static class LTL2BAState {
		public int index;
		public int fin;
		
		public LTL2BAState(int index_, int fin_) { index = index_; fin = fin_; }
		public boolean equals(Object o) { return (o instanceof LTL2BAState) && this.equals((LTL2BAState) o); }
		public boolean equals(LTL2BAState s) { return (index == s.index) && (fin == s.fin); }
		public int hashCode() { return index * 31 + fin; }
	}
	
	public NBA toNBA(APSet apset) throws PrismException
	{
		NBA nba = new NBA(apset);
		HashMap<LTL2BAState,Integer> map = new HashMap<LTL2BAState,Integer>();
		int stateindex;
		boolean accept_all = false;
		
		BTrans t;
		BState s;
		
		if (bstates.nxt == bstates) {	/* empty automaton */
			stateindex = nba.nba_i_newState();
			nba.nba_i_setStartState(stateindex);
			nba.nba_i_setFinal(stateindex, false);
			return nba;
		}
		
		if (bstates.nxt.nxt == bstates && bstates.nxt.id == 0) {	/* singleton */
			stateindex = nba.nba_i_newState();
			nba.nba_i_setStartState(stateindex);
			nba.nba_i_setFinal(stateindex, true);
			nba.nba_i_addEdge(stateindex, new APMonom(true), stateindex);
			return nba;
		}
		
		// Map states, set initial and final states
		for (s = bstates.prv; s != bstates; s = s.prv) {
			stateindex = nba.nba_i_newState();
			// System.out.println("Seen ltl2ba state " + s.id + ", mapped to " + stateindex);
			map.put(new LTL2BAState(s.id, s._final), new Integer(stateindex));
			
			if (s.id == -1)
				nba.nba_i_setStartState(stateindex);
			else if (s.id == 0) {	/* accept_all */
				accept_all = true;
				nba.nba_i_setFinal(stateindex, true);
				continue;
			}
			
			if (s._final == accept)
				nba.nba_i_setFinal(stateindex, true);
			else nba.nba_i_setFinal(stateindex, false);
		}
		
		// Copy transitions
		for (s = bstates.prv; s != bstates; s = s.prv) {
			if (s.trans.nxt == s.trans) {
				// no transitions
				continue;
			}
			for (t = s.trans.nxt; t != s.trans; t = t.nxt) {
				BTrans t1;
				APMonom transMonom = new APMonom();
				transMonom.setFromPosNeg(t.pos, t.neg);
				// System.out.println("Seen ltl2ba transition " + s.id + " -|" + transMonom.toString() + "|-> " + t.to.id + ", mapped to " + map.get(new Integer(s.id)) + " -> " + map.get(new Integer(t.to.id)));
				nba.nba_i_addEdge(map.get(new LTL2BAState(s.id, s._final)), transMonom, map.get(new LTL2BAState(t.to.id, t.to._final)));
				for (t1 = t; t1.nxt != s.trans;) {
					if (t1.nxt.to.id == t.to.id && t1.nxt.to._final == t.to._final) {
						transMonom = new APMonom();
						transMonom.setFromPosNeg(t1.nxt.pos, t1.nxt.neg);
						// System.out.println("Seen ltl2ba transition " + s.id + " -|" + transMonom.toString() + "|-> " + t.to.id + ", mapped to " + map.get(new Integer(s.id)) + " -> " + map.get(new Integer(t.to.id)));
						nba.nba_i_addEdge(map.get(new LTL2BAState(s.id, s._final)), transMonom, map.get(new LTL2BAState(t.to.id, t.to._final)));
						t1.nxt = t1.nxt.nxt;
					} 
					else {
						t1 = t1.nxt;
					}
				}
			}
		}
		// FIXME: check if this works
		if (accept_all)
			nba.nba_i_addEdge(map.get(new LTL2BAState(0, accept)), new APMonom(true), map.get(new LTL2BAState(0, accept)));
		
		return nba;
	}
}
