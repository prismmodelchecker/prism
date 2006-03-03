//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker
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

package prism;

import java.io.*;
import java.util.Vector;

import jdd.*;

// stuff for computing (S)CCs - (strongly) conected components

public class SCCComputer
{
	// logs
	private PrismLog mainLog;
	private PrismLog techLog;

	// model info
	private StochModel model;
	private JDDNode trans01;
	private JDDNode reach;
	private JDDVars allDDRowVars;
	private JDDVars allDDColVars;
	
	// stuff for SCCs
	private Vector vectSCCs;
	private JDDNode notInSCCs;
	// stuff for BSCCs
	private Vector vectBSCCs;
	private JDDNode notInBSCCs;

	// constructor - set some defaults

	public SCCComputer(PrismLog log1, PrismLog log2, Model m) throws PrismException
	{
		// initialise
		mainLog = log1;
		techLog = log2;
		if (!(m instanceof StochModel)) {	
			throw new PrismException("Error: Wrong model type passed to SCCComputer.");
		}
		model = (StochModel)m;
		trans01 = model.getTrans01();
		reach = model.getReach();
		allDDRowVars = model.getAllDDRowVars();
		allDDColVars = model.getAllDDColVars();
	}

	// get methods
	
	public Vector getVectBSCCs() { return vectBSCCs; }
	
	public JDDNode getNotInBSCCs() { return notInBSCCs; }

	// bottom strongly connected components (bscc) computation
	// (using algorithm of xie/beerel'99)
	
	public void computeBSCCs()
	{
		JDDNode v, s, back, scc, out;
		int i, n;
		
		// vector to be filled with SCCs
		vectSCCs = new Vector();
		// BDD of non-SCC states (initially zero BDD)
		notInSCCs = JDD.Constant(0);
		
		// first compute all sccs
		// (using algorithm of xie/beerel'99)
		JDD.Ref(reach);
		v = reach;
		while (!v.equals(JDD.ZERO)) {
			s = pickRandomState(v);
			back = computeBackwardSet(s, v);
			computeSCCsRec(s, back);
			JDD.Ref(reach);
			v = JDD.And(v, JDD.And(reach, JDD.Not(JDD.Or(s, back))));
		}
		JDD.Deref(v);
		
		// print out sccs
		n = vectSCCs.size();
		mainLog.print(" SCCs: " + n);
//		for (i = 0; i < n; i++) {
//			mainLog.print(i + ": ");
//			JDD.PrintVector((JDDNode)vectSCCs.elementAt(i), allDDRowVars);
//		}
//		mainLog.print("States not in SCCs:\nx: ");
//		JDD.PrintVector(notInSCCs, allDDRowVars);
		
		// now check which ones are bsccs and keep them
		vectBSCCs = new Vector();
		notInBSCCs = notInSCCs;
		n = vectSCCs.size();
		for (i = 0; i < n; i++) {
			scc = (JDDNode)vectSCCs.elementAt(i);
			JDD.Ref(trans01);
			JDD.Ref(scc);
			out = JDD.And(trans01, scc);
			JDD.Ref(scc);
			out = JDD.And(out, JDD.Not(JDD.PermuteVariables(scc, allDDRowVars, allDDColVars)));
			if (out.equals(JDD.ZERO)) {
				vectBSCCs.addElement(scc);
			}
			else {
				JDD.Ref(scc);
				notInBSCCs = JDD.Or(scc, notInBSCCs);
				JDD.Deref(scc);
			}
			JDD.Deref(out);
		}
		
		// print out bsccs
		n = vectBSCCs.size();
		mainLog.print(" BSCCs: " + n);
//		for (i = 0; i < n; i++) {
//			mainLog.print(i + ": ");
//			JDD.PrintVector((JDDNode)vectBSCCs.elementAt(i), allDDRowVars);
//		}
//		mainLog.print("States not in BSCCs:\nx: ");
//		JDD.PrintVector(notInBSCCs, allDDRowVars);
		mainLog.println(" Transient states: " + (int)JDD.GetNumMinterms(notInBSCCs, allDDRowVars.n()));
	}
	
	// pick a random (actually the first) state from set (set not empty)
	
	public JDDNode pickRandomState(JDDNode set)
	{
		int i, n;
		JDDNode tmp, tmp2, res, var;
		
		JDD.Ref(set);
		tmp = set;
		res = JDD.Constant(1);
		n = allDDRowVars.n();
		for (i = 0; i < n; i++) {
			JDD.Ref(allDDRowVars.getVar(i));
			var = allDDRowVars.getVar(i);
			JDD.Ref(var);
			JDD.Ref(tmp);
			tmp2 = JDD.And(tmp, JDD.Not(var));
			if (!tmp2.equals(JDD.ZERO)) {
				JDD.Deref(tmp);
				tmp = tmp2;
				JDD.Ref(var);
				res = JDD.And(res, JDD.Not(var));
			}
			else {
				JDD.Deref(tmp2);
				JDD.Ref(var);
				tmp = JDD.And(tmp, var);
				JDD.Ref(var);
				res = JDD.And(res, var);
			}
			JDD.Deref(var);
		}
		
		JDD.Deref(tmp);
		
		return res;
	}
	
	// find backward set of state s restricted to set v
	
	public JDDNode computeBackwardSet(JDDNode s, JDDNode v)
	{
		JDDNode back, tmp;
		boolean done = false;
		
		// do one step of loop first (s not nec. in forward set of s)
		JDD.Ref(s);
		JDD.Ref(trans01);
		back = JDD.ThereExists(JDD.And(JDD.PermuteVariables(s, allDDRowVars, allDDColVars), trans01), allDDColVars);

		// fixpoint
		while (!done) {
			JDD.Ref(back);
			tmp = JDD.PermuteVariables(back, allDDRowVars, allDDColVars);
			JDD.Ref(trans01);
			tmp = JDD.And(tmp, trans01);
			tmp = JDD.ThereExists(tmp, allDDColVars);
			JDD.Ref(back);
			tmp = JDD.Or(tmp, back);
			JDD.Ref(v);
			tmp = JDD.And(v, tmp);
			
			if (tmp.equals(back)) {
				done = true;
			}
			
			JDD.Deref(back);
			back = tmp;
		}
		
		return back;
	}
	
	// find forward set of state s restricted to set v
	
	public JDDNode computeForwardSet(JDDNode s, JDDNode v)
	{
		JDDNode forw, v2, tmp;
		boolean done = false;
		
		// do one step of loop first (s not nec. in forward set of s)
		JDD.Ref(s);
		JDD.Ref(trans01);
		forw = JDD.ThereExists(JDD.And(s, trans01), allDDRowVars);
		
		// transpose a copy of v
		JDD.Ref(v);
		v2 = JDD.PermuteVariables(v, allDDRowVars, allDDColVars);
		
		// fixpoint
		while (!done) {
			JDD.Ref(forw);
			tmp = JDD.PermuteVariables(forw, allDDColVars, allDDRowVars);
			JDD.Ref(trans01);
			tmp = JDD.And(tmp, trans01);
			tmp = JDD.ThereExists(tmp, allDDRowVars);
			JDD.Ref(forw);
			tmp = JDD.Or(tmp, forw);
			JDD.Ref(v2);
			tmp = JDD.And(v2, tmp);
			
			if (tmp.equals(forw)) {
				done = true;
			}
			
			JDD.Deref(forw);
			forw = tmp;
		}
		
		// tidy up
		JDD.Deref(v2);
		
		return JDD.PermuteVariables(forw, allDDColVars, allDDRowVars);
	}
	
	// compute fmd (finite maximum distance) predecessors of set w
	
	public JDDNode computeFMDPred(JDDNode w, JDDNode u)
	{
		JDDNode pred, front, bound, x, y;
		
		pred = JDD.Constant(0);
		JDD.Ref(w);
		front = w;
		JDD.Ref(u);
		bound = u;
		
		while (!front.equals(JDD.ZERO)) {
			JDD.Ref(trans01);
			JDD.Ref(bound);
			x = JDD.ThereExists(JDD.And(JDD.PermuteVariables(front, allDDRowVars, allDDColVars), JDD.And(trans01, bound)), allDDColVars);
			JDD.Ref(bound);
			JDD.Ref(trans01);
			JDD.Ref(bound);
			y = JDD.ThereExists(JDD.And(JDD.PermuteVariables(bound, allDDRowVars, allDDColVars), JDD.And(trans01, bound)), allDDColVars);
			JDD.Ref(reach);
			front = JDD.And(x, JDD.And(reach, JDD.Not(y)));
			JDD.Ref(front);
			pred = JDD.Or(pred, front);
			JDD.Ref(reach);
			JDD.Ref(front);
			bound = JDD.And(bound, JDD.And(reach, JDD.Not(front)));
		}
		
		JDD.Deref(front);
		JDD.Deref(bound);
		
		return pred;
	}
	
	// recursively compute SCCs in back
	// (store SCCs in first vector,
	//  store states not in an SCC in first element of second vector)
	
	public void computeSCCsRec(JDDNode s, JDDNode back)
	{
		JDDNode forw, x, r, y, ip, s2, back2, tmp;
		
		forw = computeForwardSet(s, back);
		if (forw.equals(JDD.ZERO)) {
			JDD.Ref(s);
			notInSCCs = JDD.Or(notInSCCs, s);
		}
		else {
			JDD.Ref(forw);
			vectSCCs.addElement(forw);
		}
		JDD.Ref(forw);
		JDD.Ref(s);
		x = JDD.Or(forw, s);
		JDD.Ref(back);
		JDD.Ref(x);
		JDD.Ref(reach);
		r = JDD.And(back, JDD.And(reach, JDD.Not(x)));
		y = computeFMDPred(x, r);
		JDD.Ref(y);
		notInSCCs = JDD.Or(notInSCCs, y);
		JDD.Ref(y);
		JDD.Ref(reach);
		r = JDD.And(r, JDD.And(reach, JDD.Not(y)));
		JDD.Ref(x);
		JDD.Ref(y);
		tmp = JDD.Or(y, x);
		ip = computeBackwardSet(tmp, r);
		JDD.Deref(tmp);
		
		while (!r.equals(JDD.ZERO)) {
			s2 = pickRandomState(ip);
			back2 = computeBackwardSet(s2, r);
			computeSCCsRec(s2, back2);
			JDD.Ref(reach);
			JDD.Ref(s2);
			JDD.Ref(back2);
			r = JDD.And(r, JDD.And(reach, JDD.Not(JDD.Or(s2, back2))));
			JDD.Ref(reach);
			JDD.Ref(s2);
			JDD.Ref(back2);
			ip = JDD.And(ip, JDD.And(reach, JDD.Not(JDD.Or(s2, back2))));
			JDD.Deref(s2);
			JDD.Deref(back2);
		}
		
		JDD.Deref(forw);
		JDD.Deref(x);
		JDD.Deref(r);
		JDD.Deref(y);
		JDD.Deref(ip);
	}
}
