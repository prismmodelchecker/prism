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


public class Generalized {

	public int init_size;
	public Vector<GState> g_init;
	public Vector<Integer> _final;
	public GState gstates;
	
	private GState gstack;
	private GState gremoved;
	private GScc scc_stack;
	private int gstate_id;
	private int gstate_count;
	private int gtrans_count;
	private int rank;
	private int scc_id;
	private MyBitSet fin;
	private MyBitSet bad_scc;
	
	public static class GScc {
		public GState gstate;
		public int rank;
		public int theta;
		public GScc nxt;
		
		public GScc() {}
	}

	public static class GState {
		public int id;
		public int incoming;
		public MyBitSet nodes_set;
		public GTrans trans;
		public GState nxt;
		public GState prv;
		
		public GState()
		{
			nodes_set = new MyBitSet();
		}
		
		public void free()
		{
			if (trans.nxt != null)
				trans.nxt.free(trans, true);
			nodes_set = null;
		}
	}

	public static class GTrans {
		public MyBitSet pos;
		public MyBitSet neg;
		public MyBitSet _final;
		public GState to;
		public GTrans nxt;
		
		public GTrans()
		{
			pos = new MyBitSet();
			neg = new MyBitSet();
			_final = new MyBitSet();
			nxt = null;
			to = null;
		}
		
		public GTrans(MyBitSet _pos, MyBitSet _neg, MyBitSet _fin)
		{
			pos = (MyBitSet) _pos.clone();
			neg = (MyBitSet) _neg.clone();
			_final = (MyBitSet) _fin.clone();
			nxt = null;
			to = null;
		}
		
		public GTrans clone()
		{
			GTrans rv = new GTrans(pos, neg, _final);
			rv.to = to;
			rv.nxt = nxt;
			return rv;
		}
		
		public void copyTo(GTrans g)
		{
			g.pos = (MyBitSet) pos.clone();
			g.neg = (MyBitSet) neg.clone();
			g._final = (MyBitSet) _final.clone();
			g.to = to;
		}
		
		public void free(GTrans sentinel, boolean fly)
		{
			if (this != sentinel) {
				nxt.free(sentinel, fly);
				if (fly)
					to.incoming--;
			}
		}
	}

	/* generates a generalized Buchi automaton from the alternating automaton */
	public Generalized(Alternating a)
	{
		Alternating.ATrans t;
		GState s;
		
		init_size = 0;
		gstate_id = 1;
		gstate_count = 0;
		gtrans_count = 0;

		fin = new MyBitSet();
		bad_scc = new MyBitSet();
		_final = a.final_set.IntegerList();

		gstack = new GState();
		gstack.nxt = gstack;
		gremoved = new GState();
		gremoved.nxt = gremoved;
		gstates = new GState();
		gstates.nxt = gstates;
		gstates.prv = gstates;

		for (t = a.transition.get(0); t != null; t = t.nxt) {	/* puts initial states in the stack */
			s = new GState();
			s.id = t.to.isEmpty() ? 0 : gstate_id++;
			s.incoming = 1;
			s.nodes_set.or(t.to);
			s.trans = new GTrans();
			s.trans.nxt = s.trans;
			s.nxt = gstack.nxt;
			gstack.nxt = s;
			init_size++;
		}

		if (init_size != 0)
			g_init = new Vector<GState>();
		init_size = 0;
		for (s = gstack.nxt; s != gstack; s = s.nxt) {
			g_init.add(s);
			init_size++;
		}
		while (gstack.nxt != gstack) {	/* solves all states in the stack until it is empty */
			s = gstack.nxt;
			gstack.nxt = gstack.nxt.nxt;
			if (s.incoming == 0) {
				s.free();
				continue;
			}
			make_gtrans(a, s);
		}

		retarget_all_gtrans();
		gstack = null;
		
		// System.out.println("Generalized Buchi automaton before simplification:");
		// print(System.out, a.sym_table);

		simplify_gscc(a);
		simplifyGTrans();
		simplify_gscc(a);
		while (simplifyGStates() != 0) {	/* simplifies as much as possible */
			simplify_gscc(a);
			simplifyGTrans();
			simplify_gscc(a);
		}
		// System.out.println("Generalized Buchi automaton after simplification:");
		// print(System.out, a.sym_table);
	}
	
	/** Get the highest ID of a GState */
	public int getGStateID()
	{
		return gstate_id;
	}

	/*is the transition final for i ? */
	private boolean isFinal(Alternating a, MyBitSet from, Alternating.ATrans at, int i)
	{
		Alternating.ATrans t;
		boolean in_to;
		if (!at.to.get(i))
		    return true;
		in_to = at.to.get(i);
		at.to.clear(i);
		for (t = a.transition.get(i); t != null; t = t.nxt)
			if (at.to.containsAll(t.to) &&
			    at.pos.containsAll(t.pos) &&
			    at.neg.containsAll(t.neg)) {
				if (in_to)
					at.to.set(i);
				return true;
			}
		if (in_to)
			at.to.set(i);
		return false;
	}
	
	/* finds the corresponding state, or creates it */
	private GState findGState(MyBitSet set, GState s)
	{
		if (set.equals(s.nodes_set))
			return s;	/* same state */

		s = gstack.nxt;	/* in the stack */
		gstack.nodes_set = set;
		while (!set.equals(s.nodes_set))
			s = s.nxt;
		if (s != gstack)
			return s;

		s = gstates.nxt;	/* in the solved states */
		gstates.nodes_set = set;
		while (!set.equals(s.nodes_set))
			s = s.nxt;
		if (s != gstates)
			return s;

		s = gremoved.nxt;	/* in the removed states */
		gremoved.nodes_set = set;
		while (!set.equals(s.nodes_set))
			s = s.nxt;
		if (s != gremoved)
			return s;

		s = new GState();
		s.id = set.isEmpty() ? 0 : gstate_id++;
		s.incoming = 0;
		s.nodes_set = (MyBitSet) set.clone();
		s.trans = new GTrans();
		s.trans.nxt = s.trans;
		s.nxt = gstack.nxt;
		gstack.nxt = s;
		return s;
	}

	/* returns 1 if the transitions are identical */
	private boolean same_gtrans(GState a, GTrans s, GState b, GTrans t, boolean use_scc)
	{
		if ((s.to != t.to) ||
			!s.pos.equals(t.pos) ||
			!s.neg.equals(t.neg))
			return false;	/* transitions differ */
		if (s._final.equals(t._final))
			return true;	/* same transitions exactly */
		/* next we check whether acceptance conditions may be ignored */
		if (use_scc && ( bad_scc.get(a.incoming)
						|| bad_scc.get(b.incoming)
						|| (a.incoming != s.to.incoming)
						|| (b.incoming != t.to.incoming)))
			return true;
		
		return false;
		
		/* below is the old test to check whether acceptance conditions may be ignored */
		//if (!use_scc)
		//	return 0;	/* transitions differ */
		// if ((a.incoming == b.incoming) && (a.incoming == s.to.incoming))
		// 	return 0;	/* same scc: acceptance conditions must be taken into account */
		/* if scc(a)=scc(b)>scc(s->to) then acceptance conditions need not be taken into account */
		/* if scc(a)>scc(b) and scc(a) is non-trivial then all_gtrans_match(a,b,use_scc) will fail */
		/* if scc(a) is trivial then acceptance conditions of transitions from a need not be taken into account */
		// return 1;	/* same transitions up to acceptance conditions */
		
	}
	
	/* decides if the states are equivalent */
	private int allGTransMatch(GState a, GState b, boolean use_scc)
	{
		GTrans s, t;
		for (s = a.trans.nxt; s != a.trans; s = s.nxt) {
			/* all transitions from a appear in b */
			s.copyTo(b.trans);
			t = b.trans.nxt;
			while (!same_gtrans(a, s, b, t, use_scc))
				t = t.nxt;
			if (t == b.trans)
				return 0;
		}
		for (t = b.trans.nxt; t != b.trans; t = t.nxt) {
			/* all transitions from b appear in a */
			t.copyTo(a.trans);
			s = a.trans.nxt;
			while (!same_gtrans(a, s, b, t, use_scc))
				s = s.nxt;
			if (s == a.trans)
				return 0;
		}
		return 1;
	}

	/* creates all the transitions from a state */
	private void make_gtrans(Alternating a, GState s)
	{
		int i, state_trans = 0, trans_exist = 1;
		Vector<Integer> list;
		GState s1;
		Alternating.ATrans t1;
		Alternating.AProd prod = new Alternating.AProd();	/* initialization */
		prod.nxt = prod;
		prod.prv = prod;
		prod.prod = new Alternating.ATrans();
		prod.trans = prod.prod;
		prod.trans.nxt = prod.prod;
		list = s.nodes_set.IntegerList();

		for (i = 0; i < list.size(); i++) {
			Alternating.AProd p = new Alternating.AProd();
			p.astate = list.get(i);
			p.trans = a.transition.get(list.get(i));
			if (p.trans == null)
				trans_exist = 0;
			p.prod = Alternating.do_merge_atrans(prod.nxt.prod, p.trans);
			p.nxt = prod.nxt;
			p.prv = prod;
			p.nxt.prv = p;
			p.prv.nxt = p;
		}

		while (trans_exist != 0) {	/* calculates all the transitions */
			Alternating.AProd p = prod.nxt;
			t1 = p.prod;
			if (t1 != null) {	/* solves the current transition */
				GTrans trans, t2;
				fin.clear();
				for (i = 0; i < _final.size(); i++)
					if (isFinal(a, s.nodes_set, t1, _final.get(i)))
						fin.set(_final.get(i));
				for (t2 = s.trans.nxt; t2 != s.trans;) {
					if (t2.to.nodes_set.containsAll(t1.to) && 
						t2.pos.containsAll(t1.pos) && 
						t2.neg.containsAll(t1.neg) &&
						fin.equals(t2._final)) {	/* t2 is redundant */
						GTrans free = t2.nxt;
						t2.to.incoming--;
						t2.to = free.to;
						t2.pos = (MyBitSet) free.pos.clone();
						t2.neg = (MyBitSet) free.neg.clone();
						t2._final = (MyBitSet) free._final.clone();
						t2.nxt = free.nxt;
						if (free == s.trans)
							s.trans = t2;
						free = null;
						state_trans--;
					} else if (t1.to.containsAll(t2.to.nodes_set) && t1.pos.containsAll(t2.pos) && t1.neg.containsAll(t2.neg) && t2._final.equals(fin)) {	/* t1 is redundant */
						break;
					} else {
						t2 = t2.nxt;
					}
				}
				if (t2 == s.trans) {	/* adds the transition */
					trans = new GTrans();
					trans.to = findGState(t1.to, s);
					trans.to.incoming++;
					trans.pos = (MyBitSet) t1.pos.clone();
					trans.neg = (MyBitSet) t1.neg.clone();
					trans._final = (MyBitSet) fin.clone();
					trans.nxt = s.trans.nxt;
					s.trans.nxt = trans;
					state_trans++;
				}
			}
			if (p.trans == null)
				break;
			while (p.trans.nxt == null)	/* calculates the next transition */
				p = p.nxt;
			if (p == prod)
				break;
			p.trans = p.trans.nxt;
			p.prod = Alternating.do_merge_atrans(p.nxt.prod, p.trans);
			p = p.prv;
			while (p != prod) {
				p.trans = a.transition.get(p.astate);
				p.prod = Alternating.do_merge_atrans(p.nxt.prod, p.trans);
				p = p.prv;
			}
		}

		list.clear();
		while (prod.nxt != prod) {
			Alternating.AProd p = prod.nxt;
			prod.nxt = p.nxt;
			p.prod = null;
			p = null;
		}
		prod.prod = null;
		prod = null;

		if (s.trans == s.trans.nxt) {	/* s has no transitions */
			s.trans.nxt.free(s.trans, true);
			s.trans = null;
			s.prv = null;
			s.nxt = gremoved.nxt;
			gremoved.nxt = s;
			for (s1 = gremoved.nxt; s1 != gremoved; s1 = s1.nxt)
				if (s1.prv == s)
					s1.prv = null;
			return;
		}

		gstates.trans = s.trans;
		s1 = gstates.nxt;
		while (allGTransMatch(s, s1, false) == 0)
			s1 = s1.nxt;
		if (s1 != gstates) {	/* s and s1 are equivalent */
			s.trans.nxt.free(s.trans, true);
			s.trans = null;
			s.prv = s1;
			s.nxt = gremoved.nxt;
			gremoved.nxt = s;
			for (s1 = gremoved.nxt; s1 != gremoved; s1 = s1.nxt)
				if (s1.prv == s)
					s1.prv = s.prv;
			return;
		}

		s.nxt = gstates.nxt;	/* adds the current state to 'gstates' */
		s.prv = gstates;
		s.nxt.prv = s;
		gstates.nxt = s;
		gtrans_count += state_trans;
		gstate_count++;
	}

	/* redirects transitions before removing a state from the automaton */
	private void retarget_all_gtrans()
	{
		GState s;
		GTrans t;
		int i;
		for (i = 0; i < init_size; i++)
			if (g_init.get(i) != null && (g_init.get(i).trans == null))	/* g_init[i] has been removed */
				g_init.set(i, g_init.get(i).prv);
		for (s = gstates.nxt; s != gstates; s = s.nxt)
			for (t = s.trans.nxt; t != s.trans;)
				if (t.to.trans == null) {	/* t->to has been removed */
					t.to = t.to.prv;
					if (t.to == null) {	/* t->to has no transitions */
						GTrans free = t.nxt;
						t.to = free.to;
						t.pos = (MyBitSet) free.pos.clone();
						t.neg = (MyBitSet) free.neg.clone();
						t._final = (MyBitSet) free._final.clone();
						t.nxt = free.nxt;
						if (free == s.trans)
							s.trans = t;
						free = null;
					} else
						t = t.nxt;
				} else
					t = t.nxt;
		while (gremoved.nxt != gremoved) {	/* clean the 'removed' list */
			s = gremoved.nxt;
			gremoved.nxt = gremoved.nxt.nxt;
			if (s.nodes_set != null)
					s.nodes_set = null;
			s = null;
		}
	}

	/* removes a state */
	private GState removeGState(GState s, GState s1)
	{
		GState prv = s.prv;
		s.prv.nxt = s.nxt;
		s.nxt.prv = s.prv;
		s.trans.nxt.free(s.trans, false);
		s.trans = null;
		s.nodes_set = null;
		s.nxt = gremoved.nxt;
		gremoved.nxt = s;
		s.prv = s1;
		for (s1 = gremoved.nxt; s1 != gremoved; s1 = s1.nxt)
			if (s1.prv == s)
				s1.prv = s.prv;
		return prv;
	}

	/* simplifies the transitions */
	private int simplifyGTrans()
	{
		int changed = 0;
		GState s;
		GTrans t;
		GTrans t1;

		for (s = gstates.nxt; s != gstates; s = s.nxt) {
			t = s.trans.nxt;
			while (t != s.trans) {	// tries to remove t
				t.copyTo(s.trans);
				t1 = s.trans.nxt;
				while (!((t != t1)
						&& (t1.to == t.to)
						&& t.pos.containsAll(t1.pos)
						&& t.neg.containsAll(t1.neg)
						&& (t1._final.containsAll(t._final) ||
							(s.incoming != t.to.incoming) || bad_scc.get(s.incoming)))
						)
					t1 = t1.nxt;
			
			if (t1 != s.trans) {
					GTrans free = t.nxt;
					t.to = free.to;
					t.pos = (MyBitSet) free.pos.clone();
					t.neg = (MyBitSet) free.neg.clone();
					t._final = (MyBitSet) free._final.clone();
					t.nxt = free.nxt;
					if (free == s.trans)
						s.trans = t;
					free = null;
					changed++;
				} else
					t = t.nxt;
			}
		}
		return changed;
	}

	/* eliminates redundant states */
	private int simplifyGStates()
	{
		int changed = 0;
		GState a, b;

		for (a = gstates.nxt; a != gstates; a = a.nxt) {
			if (a.trans == a.trans.nxt) {	/* a has no transitions */
				a = removeGState(a, null);
				changed++;
				continue;
			}
			gstates.trans = a.trans;
			b = a.nxt;
			while (allGTransMatch(a, b, true) == 0)	/* = and not == */
				b = b.nxt;
			if (b != gstates) {	/* a and b are equivalent */
				/* if scc(a)>scc(b) and scc(a) is non-trivial then all_gtrans_match(a,b,use_scc) must fail */
				if (a.incoming > b.incoming) // scc(a) is trivial
					a = removeGState(a, b);
				else	// either scc(a)=scc(b) or scc(b) is trivial
					removeGState(b, a);
				changed++;
			}
		}
		retarget_all_gtrans();

		return changed;
	}

	private int gdfs(GState s)
	{
		GTrans t;
		GScc c;
		GScc scc = new GScc();
		scc.gstate = s;
		scc.rank = rank;
		scc.theta = rank++;
		scc.nxt = scc_stack;
		scc_stack = scc;

		s.incoming = 1;

		for (t = s.trans.nxt; t != s.trans; t = t.nxt) {
			if (t.to.incoming == 0) {
				int result = gdfs(t.to);
				scc.theta = scc.theta < result ? scc.theta : result;
			} else {
				for (c = scc_stack.nxt; c != null; c = c.nxt) {
					if (c.gstate == t.to) {
						scc.theta = scc.theta < c.rank ? scc.theta : c.rank;
						break;
					}
				}
			}
		}
		if (scc.rank == scc.theta) {
			while (scc_stack != scc) {
				scc_stack.gstate.incoming = scc_id;
				scc_stack = scc_stack.nxt;
			}
			scc.gstate.incoming = scc_id++;
			scc_stack = scc.nxt;
		}
		return scc.theta;
	}

	private void simplify_gscc(Alternating a)
	{
		GState s;
		GTrans t;
		int i;
		Vector<MyBitSet> scc_final;
		
		rank = 1;
		scc_stack = null;
		scc_id = 1;

		if (gstates == gstates.nxt)	return;

		for (s = gstates.nxt; s != gstates; s = s.nxt) {
			s.incoming = 0;	/* state color = white */
		}

		for (i = 0; i < init_size; i++) {
			if ((g_init.get(i) != null) && g_init.get(i).incoming == 0) {
				gdfs(g_init.get(i));
			}
		}

		scc_final = new Vector<MyBitSet>();
		for (i = 0; i < scc_id; i++) {
			scc_final.add(new MyBitSet());
		}
		for (s = gstates.nxt; s != gstates; s = s.nxt) {
			if (s.incoming == 0) {
				s = removeGState(s, null);
			}
			else {
				for (t = s.trans.nxt; t != s.trans; t = t.nxt) {
					if (t.to.incoming == s.incoming) {
						scc_final.get(s.incoming).or(t._final);
					}
				}
			}
		}
		bad_scc.clear();

		for (i = 0; i < scc_id; i++) {
			if (!scc_final.get(i).containsAll(a.final_set)) {
				bad_scc.set(i);
			}
		}
		scc_final.clear();
	}
	
	private void reverse_print_generalized(PrintStream out, GState s, APSet symtab)
	{				/* dumps the generalized Buchi automaton */
		GTrans t;
		if (s == gstates)
			return;

		reverse_print_generalized(out, s.nxt, symtab);	/* begins with the last state */

		out.format("state %d (", s.id);
		s.nodes_set.print(out);
		out.format(") : %d", s.incoming);
		out.println();
		for (t = s.trans.nxt; t != s.trans; t = t.nxt) {
			if (t.pos.isEmpty() && t.neg.isEmpty())
				out.print("1");
			t.pos.print(out, symtab, true);
			if (!t.pos.isEmpty() && !t.neg.isEmpty())
				out.print(" & ");
			t.neg.print(out, symtab, false);
			out.format(" -> %d : ", t.to.id);
			t._final.print(out);
			out.println();
		}
	}

	/* prints intial states and calls 'reverse_print' */
	public void print(PrintStream out, APSet symtab)
	{
		int i;
		out.println("init :");
		for (i = 0; i < init_size; i++)
			if (g_init.get(i) != null)
				out.println(g_init.get(i).id);
		reverse_print_generalized(out, gstates.nxt, symtab);
	}
}
