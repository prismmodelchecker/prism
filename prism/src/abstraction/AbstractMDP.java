//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package abstraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import explicit.*;

public class AbstractMDP
{
	public int nConcrete;
	public int nAbstract;
	public int nnzConcrete;
	public int nnzAbstract;
	public int mapping[] = null;
	public int initialConcreteState;
	public int initialAbstractState;
	public boolean target[];
	public TreeSet states;
	public List statesList;
	public ArrayList<List> mdp;
	public ArrayList<List> mdpStates;

	protected long timer;
	
	public long getLastTimer()
	{
		return timer;
	}
	
	/* Build the abstract state space */
	
	public void buildAbstractStateSpace(PRISMAbstraction abstr)
	{
		BufferedReader in;
		FileWriter out;
		String s, ss[];
		int i, j, numVars = 0, vars[];
		AbstractState state;
		
		// start timer
		timer = System.currentTimeMillis();

		try {
			states = new TreeSet();
			nConcrete = 0;

			// first pass of states file - build abstract state space

			System.out.println("Computing abstract states...");
			in = new BufferedReader(new FileReader(new File(abstr.filename + ".sta")));
			s = in.readLine();
			if (s != null) {
				s = s.substring(1, s.length() - 1);
				ss = s.split(",");
				numVars = ss.length;
			}
			s = in.readLine();
			while (s != null) {
				s = s.substring(s.indexOf(":") + 2, s.indexOf(")"));
				ss = s.split(",");
				vars = new int[numVars];
				for (j = 0; j < numVars; j++) {
					try {
						vars[j] = Integer.parseInt(ss[j]);
					} catch (NumberFormatException e) {
						if (ss[j].equals("false"))
							vars[j] = 0;
						else {
							if (ss[j].equals("true"))
								vars[j] = 1;
							else
								throw e;
						}
					}
				}
				state = abstr.concreteToAbstract(vars);
				states.add(state);
				nConcrete++;
				s = in.readLine();
			}
			in.close();
			nAbstract = states.size();

			System.out.println("Concrete states: " + nConcrete);
			System.out.println("Abstract states: " + nAbstract);

			// Convert states list to array
			statesList = Arrays.asList(states.toArray());

			// output abstract state space to file
			/*
			 * System.out.println("Sending abstract state space to file..."); out = new FileWriter(new
			 * File(filename+".abs.sta")); s = "(" + abstractVarsString() + ")\n"; out.write(s, 0, s.length()); for (i =
			 * 0; i < nAbstract; i++) { s = ""+i+":"+statesList.get(i)+" \n"; out.write(s, 0, s.length()); }
			 * out.close();
			 */

			// second pass - build mapping from concrete to abstract states and build array of target states
			System.out.println("Building mapping...");
			mapping = new int[nConcrete];
			target = new boolean[nConcrete];
			in = new BufferedReader(new FileReader(new File(abstr.filename + ".sta")));
			s = in.readLine();
			s = in.readLine();
			while (s != null) {
				i = Integer.parseInt(s.substring(0, s.indexOf(":")));
				s = s.substring(s.indexOf(":") + 2, s.indexOf(")"));
				ss = s.split(",");
				vars = new int[numVars];
				for (j = 0; j < numVars; j++) {
					try {
						vars[j] = Integer.parseInt(ss[j]);
					} catch (NumberFormatException e) {
						if (ss[j].equals("false"))
							vars[j] = 0;
						else {
							if (ss[j].equals("true"))
								vars[j] = 1;
							else
								throw e;
						}
					}
				}
				state = abstr.concreteToAbstract(vars);
				j = statesList.indexOf(state);
				mapping[i] = j;
				if (abstr.isInitialConcreteState(vars)) {
					initialConcreteState = i;
					initialAbstractState = j;
				}
				target[i] = abstr.isTargetConcreteState(vars);
				s = in.readLine();
			}
			in.close();
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		}

		// stop timer
		timer = System.currentTimeMillis() - timer;
		System.out.println("Abstract state space built in " + timer / 1000.0 + " seconds.");
	}

	public void buildAbstractMDP(PRISMAbstraction abstr)
	{
		BufferedReader in;
		String s, ss[];
 		int i = 0, j, k, n, iLast, kLast;
		double prob;
		Distribution distr;
		HashSet distrs = null;
		Iterator it1, it2, it3;
		HashMap mdpTmp[];
		Set set;
		List list, list2;
		
		long timer;
		
		// start timer
		timer = System.currentTimeMillis();
		
		try {
			// read transitions file - build mdp
			mdpTmp = new HashMap[nAbstract];
			for (i = 0; i < nAbstract; i++) mdpTmp[i] = new HashMap();
			in = new BufferedReader(new FileReader(new File(abstr.filename+".tra")));
			s = in.readLine();
			nnzConcrete = 0;
			if (s != null) {
				s = s.substring(1,s.length()-1);
				ss = s.split(",");
			}
			iLast = -1;
			kLast = -1;
			distr = null;
			s = in.readLine();
			while (s != null) {
				ss = s.split(" ");
				i = Integer.parseInt(ss[0]);
				k = Integer.parseInt(ss[1]);
				j = Integer.parseInt(ss[2]);
				prob = Double.parseDouble(ss[3]);
				nnzConcrete++;
				if (i != iLast) {
					if (distr != null) {
						distrs.add(distr);
						set = (HashSet)mdpTmp[mapping[iLast]].get(distrs);
						if (set == null) set = new HashSet<Integer>();
						set.add(iLast);
						mdpTmp[mapping[iLast]].put(distrs, set);
					}
					distrs = new HashSet();
					distr = new Distribution();
				}
				else if (k != kLast) {
					if (distr != null) distrs.add(distr);
					distr = new Distribution();
				}
				distr.add(mapping[j], prob);
				iLast = i;
				kLast = k;
				s = in.readLine();
			}
			distrs.add(distr);
			set = (HashSet)mdpTmp[mapping[iLast]].get(distrs);
			if (set == null) set = new HashSet<Integer>();
			set.add(iLast);
			mdpTmp[mapping[iLast]].put(distrs, set);
			in.close();
			
			// build MDP (convert (sets to lists)
			mdp = new ArrayList<List>(nAbstract);
			mdpStates = new ArrayList<List>(nAbstract);
			for (i = 0; i < nAbstract; i++) {
				list = new ArrayList();
				list2 = new ArrayList();
				set = mdpTmp[i].keySet();
				it1 = set.iterator();
				while (it1.hasNext()) {
					distrs = (HashSet)it1.next();
					list.add(distrs);
					list2.add(mdpTmp[i].get(distrs));
				}
				mdp.add(list);
				mdpStates.add(list2);
			}
			
// 			for (i = 0; i < nAbstract; i++) {
//				System.out.println(i+": "+mdp.get(i));
//			}
		}
		catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		}
		
		System.out.println("Concrete MDP: n = " + nConcrete + ", nnz = " + nnzConcrete);
		System.out.println("Initial state: "+initialConcreteState+" (concrete) "+initialAbstractState+" (abstract)");
		
		// check size of abstract mdp
		nnzAbstract = 0;
		j = 0; k = 0;
		for (i = 0; i < nAbstract; i++) {
			list = mdp.get(i);
			n = list.size(); if (n > j) j = n;
			it1 = list.iterator();
			while (it1.hasNext()) {
				distrs = (HashSet)it1.next();
				n = distrs.size(); if (n > k) k = n;
				it2 = distrs.iterator();
				while (it2.hasNext()) {
					distr = (Distribution)it2.next();
					it3 = distr.iterator();
					while (it3.hasNext()) {
						nnzAbstract++;
						it3.next();
					}
				}
			}
		}

		System.out.print("Abstract MDP: n = " + nAbstract + ", nnz = " + nnzAbstract);
		System.out.println(", maxk1 = " + j + ", maxk2 = " + k);
		
		// stop timer
		timer = System.currentTimeMillis() - timer;
		System.out.println("Abstract MDP built in " + timer/1000.0 + " seconds.");
	}

	public void printMapping(PRISMAbstraction abstr)
	{
		BufferedReader in;
		String s, ss[];
		int i, j, numVars = 0, vars[];

		try {
			in = new BufferedReader(new FileReader(new File(abstr.filename + ".sta")));
			s = in.readLine();
			if (s != null) {
				s = s.substring(1, s.length() - 1);
				ss = s.split(",");
				numVars = ss.length;
			}
			s = in.readLine();
			i = 0;
			while (s != null) {
				s = s.substring(s.indexOf(":") + 2, s.indexOf(")"));
				ss = s.split(",");
				vars = new int[numVars];
				for (j = 0; j < numVars; j++) {
					try {
						vars[j] = Integer.parseInt(ss[j]);
					} catch (NumberFormatException e) {
						if (ss[j].equals("false"))
							vars[j] = 0;
						else {
							if (ss[j].equals("true"))
								vars[j] = 1;
							else
								throw e;
						}
					}
				}
				System.out.println(mapping[i] + "\t" + s);
				s = in.readLine();
				i++;
			}
			in.close();
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		}
	}
}
