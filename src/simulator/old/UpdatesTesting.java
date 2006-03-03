//==============================================================================
//	
//	Copyright (c) 2004-2005, Andrew Hinton
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

package simulator.old;

import java.util.*;
import java.io.*;
import parser.*;
import prism.*;

/**	
 */
public class UpdatesTesting
{
	private Prism p;
	private Expression e;
	private SimulatorEngine s;

	public static void main(String[]args)
	{
		new UpdatesTesting();

	}

	public UpdatesTesting()
	{
		PrismFileLog ppp = new PrismFileLog("stdout");
		p = new Prism(ppp,ppp);
		p.initialise();

		s = new SimulatorEngine(p);

		//Setup the test log

		PrintWriter testLog = null;
		try
		{
			File f = new File("../testing/update_testlog_1.log");
			testLog = new PrintWriter(new FileWriter(f), true);
			
			//testLog.println("Test the printwriter");
		}
		catch(IOException e)
		{
			System.err.println("Error in opening test log file: \n"+e.getMessage());
		}

		Model builtModel = null;

		//for each test case
		for(int i = 0; i < getNumTestCases(); i++)
		{
			s.initialiseModel();

			//create ModulesFile for this test case
			ModulesFile mf = null;
			try
			{
				File f = new File(getTestFile(i));
				System.err.println("Loading..."+f.toString());
				mf = p.parseModelFile(f);
				//System.err.println(mf.toString());
			}
			catch(Exception e)
			{
				testLog.println(getTestFile(i)+"\t"+"Parsing Error: "+e.getMessage());
				continue;
			}
			Values constants = new Values();
			addConstantsForTestFile(i, constants);

			try
			{
				mf.setUndefinedConstants(constants);
				Values v = mf.getConstantValues();
			}
			catch(Exception e)
			{
				testLog.println(getTestFile(i)+"\t"+"Invalid Constants Provided: "+e.getMessage());
				continue;
			}
			
			try
			{
				if(builtModel != null)
					builtModel.clear();
				builtModel = p.buildModel(mf);
			}
			catch(PrismException e)
			{
				testLog.println(getTestFile(i)+"\t"+"Build Error "+e.getMessage());
				continue;
			}
			
			StateList sl = builtModel.getReachableStates();
			
			PrismFileLog pfl = new PrismFileLog("../testing/tempStates.txt");
			sl.print(pfl);
			pfl.close();
			File stateFile = new File("../testing/tempStates.txt");
			File transFile = new File("../testing/tempTrans.txt");
			try
			{
				p.exportToFile(builtModel, true, Prism.EXPORT_PLAIN, transFile);
			}
			catch(Exception e)
			{
				testLog.println(getTestFile(i)+"\t"+"Error when exporting transitions: "+e.getMessage());
			}
			ArrayList states = new ArrayList();

			try
			{
				//testLog.println(getTestFile(i)+"\t"+"Before test states and transitions");
				computeStatesAndTransitions(states, stateFile, transFile, builtModel.getNumVars());
				//testLog.println(getTestFile(i)+"\t"+"Computed test states and transitions");
			}
			catch(Exception e)
			{
				testLog.println(getTestFile(i)+"\t"+"Error when computing test states and transitions: "+e.getMessage());
				continue;
			}

			int countErrors = 0;
			int countTests = 0;

			String[]varNames = new String[mf.getVarNames().size()];
			for(int j = 0; j < varNames.length; j++)
			{
				varNames[j] = (String)mf.getVarNames().elementAt(j);
				//System.out.println("A var: "+varNames[j]);
			}

			s.loadModulesFile(mf);
			

			for(int j = 0; j < states.size(); j++)
			{
				if(j%10 == 0)
				{
					System.err.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
					System.err.print("Iteration: "+j);
				}
				
				//System.out.println("Iteration "+j+": ");
				State currentTestState = (State)states.get(j);
				//System.out.println(currentTestState.toString());
				Values test = new Values();
				for(int k = 0; k < varNames.length; k++)
				{
					int type = ((Integer)mf.getVarTypes().elementAt(k)).intValue();
					Object obj;

					if(type == Expression.BOOLEAN)
					{
						obj = (currentTestState.varValues[k] == 0) ? new Boolean(false) : new Boolean(true);
					}
					else
					{
						obj = new Integer(currentTestState.varValues[k]);
					}
					test.addValue(varNames[k], obj);
				}
				try
				{
					if(j == 0)
						s.startNewPath(mf.getConstantValues(),  test);
					else
						s.startNewPath(test);
					//System.err.println(s.modelToString());
					//System.err.println(s.pathToString());
					boolean result = currentTestState.compareUpdates(s, states);
					if(!result)
					{
						//Produce error report

						countErrors++;

						PrintWriter pw = new PrintWriter(new FileWriter(new File("../testing/errorReports/error_"+i+"_"+countErrors+".txt")));

						pw.println("Error report for "+getTestFile(i));
						pw.println("===================================================================");
						pw.println();
						//pw.println(s.modelToString());

						int numUpdates = s.getNumUpdates();
						pw.println("Actual Results");
						pw.println();

						for(int ii = 0; ii < numUpdates; ii++)
						{
							int[] vars = new int[builtModel.getNumVars()];
							pw.print(s.getProbabilityOfUpdate(ii)+"\t");
							pw.print(s.getDistributionIndexOfUpdate(ii)+"\t");
							double prob = s.getProbabilityOfUpdate(ii);
							int dist = s.getDistributionIndexOfUpdate(ii);
							for(int jj = 0; jj < builtModel.getNumVars(); jj++)
							{
								vars[jj] = s.getResultOfUpdate(ii,jj);
								//pw.print(s.getResultOfUpdate(i,j));
							}
							State ssss = null;
							for(int jj = 0; jj < states.size(); jj++)
							{
								ssss = (State)states.get(jj);
								if(ssss.equals(vars)) 
								{
									pw.println("   "+ssss.index);
									break;
								}
							}
							
						}
						pw.println("Expected results");
						pw.println(currentTestState.toString());
						pw.println();						
						pw.flush();
						pw.close();
					}

				}
				catch(Exception e)
				{
					testLog.println(getTestFile(i)+"\t"+"Error in starting new path");
				}
				countTests++;
			}

			testLog.println(getTestFile(i)+"\tno. tests: "+countTests+"\tfailed: "+countErrors);

		}
	}


	public void computeStatesAndTransitions(ArrayList states, File stateFile, File transFile, int numVars) throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(stateFile));

		int ff= 0;
		while(br.ready())
		{
			String line = br.readLine();
			//System.err.println("line = "+line);
			//find the number
			int numEnd = 0;
			while(line.charAt(numEnd) != ':')
				numEnd++;
			int index = Integer.parseInt(line.substring(0,numEnd));
			//System.err.println("step1");
			State st = new State(index, numVars);


			//System.err.println("numEnd = "+line.charAt(numEnd));
			int curr = numEnd+2; //start where the variables start
			int startNum = numEnd+2;

			int varIndex = 0;
			//System.err.println("step2");
			while(line.charAt(curr)!= ')')
			{
				//System.err.println("step "+line.charAt(curr));
				if(line.charAt(curr) == ',')
				{
					//System.err.println("step3");
					int num = Integer.parseInt(line.substring(startNum, curr));
					startNum = curr+1;
					st.varValues[varIndex++] = num;
				}
				curr++;
			}
			int num = Integer.parseInt(line.substring(startNum, curr));
			startNum = curr+1;
			st.varValues[varIndex++] = num;

			states.add(st);
		}

		br = new BufferedReader(new FileReader(transFile));
		
		br.readLine(); //ignore the first line header info

		while(br.ready())
		{
			String line = br.readLine();
			StringTokenizer tokens = new StringTokenizer(line, " ");
			if(tokens.countTokens() == 4) //nondeterministic
			{
				int fromIndex = Integer.parseInt(tokens.nextToken());
				int distIndex = Integer.parseInt(tokens.nextToken());
				int toIndex = Integer.parseInt(tokens.nextToken());
				double probability = Double.parseDouble(tokens.nextToken());

				State fromState = (State)states.get(fromIndex);
				State toState = (State)states.get(toIndex);
				Transition tran = new Transition(toState, distIndex, probability);

				fromState.addTransition(tran);
			}
			else if(tokens.countTokens() == 3) //dtmc (ctmc)
			{
				int fromIndex = Integer.parseInt(tokens.nextToken());
				int distIndex = -1;
				int toIndex = Integer.parseInt(tokens.nextToken());
				double probability = Double.parseDouble(tokens.nextToken());

				State fromState = (State)states.get(fromIndex);
				State toState = (State)states.get(toIndex);
				Transition tran = new Transition(toState, distIndex, probability);

				fromState.addTransition(tran);
			}
			else throw new Exception("Wrong number of tokens in transition file");
		}
		
	}

	class State
	{
		int index;
		int[] varValues;

		ArrayList transitions;

		public State(int index, int numVarValues)
		{
			this.index = index;
			varValues = new int[numVarValues];

			transitions = new ArrayList();
		}

		public boolean equals(int[]test)
		{
			for(int i = 0; i < varValues.length; i++)
				if(varValues[i] != test[i]) return false;
			return true;
		}

		public boolean compareUpdates(SimulatorEngine s, ArrayList states)
		{
			int numUpdates = s.getNumUpdates();

			int[] distribution = new int[100];
			for(int i = 0; i < distribution.length; i++)
			{
				distribution[i] = -1;
			}

			for(int i = 0; i < numUpdates; i++)
			{
				int[] vars = new int[varValues.length];
				//System.err.print(s.getProbabilityOfUpdate(i)+"\t");
				//System.err.print(s.getDistributionIndexOfUpdate(i)+"\t");
				double prob = s.getProbabilityOfUpdate(i);
				int dist = s.getDistributionIndexOfUpdate(i);
				for(int j = 0; j < varValues.length; j++)
				{
					vars[j] = s.getResultOfUpdate(i,j);
					//System.err.print(s.getResultOfUpdate(i,j));
				}
				State ssss = null;
				for(int j = 0; j < states.size(); j++)
				{
					ssss = (State)states.get(j);
					if(ssss.equals(vars)) 
					{
						//System.err.print("   "+ssss.index);
						break;
					}
				}
				//System.err.println();
				if(ssss != null)
				{
					for(int j = 0; j < getNumTransitions(); j++)
					{
						Transition t = getTransition(j);
						if(t.toState.index == ssss.index)
						{
							if(t.probability > prob-0.0000001 && t.probability < prob+0.0000001)
							{
								t.checked = true;
								/*if(distribution[t.distributionIndex] == -1)
								{
									distribution[t.distributionIndex] = dist;
								}
								else
								{
									//if we have already assigned check
									//that it is the same
									if(distribution[t.distributionIndex]
										!= dist) return false;
								}*/
							}
							else return false;
						}
					}
				}
			}

			//all transitions should have been checked
			for(int i = 0; i < getNumTransitions(); i++)
			{
				if(!getTransition(i).checked) return false;
			}
			//System.err.println();
			//System.err.println(this.toString());
			//System.err.println();
			return true;
		}

		public Transition getTransition(int i)
		{
			return (Transition)transitions.get(i);
		}

		public void addTransition(Transition t)
		{
			transitions.add(t);
		}

		public int getNumTransitions()
		{
			return transitions.size();
		}

		public String toString()
		{
			String returner = ""+index+":(";
			for(int i = 0; i < varValues.length; i++)
			{
				returner+=""+varValues[i];
				if(i != varValues.length-1) returner+=","; 
			}
			returner+=")";
			for(int i = 0; i < transitions.size(); i++)
				returner+="\n\t"+getTransition(i).toString();
			
			return returner;
		}
	}

	class Transition
	{
		State toState;
		int distributionIndex;
		double probability;
		boolean checked;

		public Transition(State toState, int distributionIndex, double probability)
		{
			this.toState = toState;
			this.distributionIndex = distributionIndex;
			this.probability = probability;
		}

		public String toString()
		{
			String returner = distributionIndex >= 0 ? "["+distributionIndex+"]" : "";
			returner+=" "+probability+": "+toState.index;
			return returner;
		}
	}


	//====================================================================
	//	Test Cases
	//====================================================================

	public String getTestFile(int i )
	{
		String returner = "../examples/";
		switch(i)
		{
			/*case 0: return returner+"dice/two_dice.nm";
			case 1: return returner+"leader/asynchronous/leader4.nm";
			case 2: return returner+"leader/asynchronous/leader5.nm";
			case 3: return returner+"firewire/abst/deadline.nm";
			case 4: return returner+"firewire/abst/deadline.nm";
			case 5: return returner+"lrabin/lrabin34.nm";	 */
			//case 0: return returner+"/cluster/cluster.sm";
			//case 0: return returner+"mutual/mutual3.nm";
			case 0: return returner+"cyclin.sm";
			case 1: return returner+"phil/phil4.nm";
			case 2: return returner+"rabin/rabin3.nm";

			case 9: return returner+"lrabin/lrabin35.nm";
			case 10: return returner+"lrabin/lrabin36.nm";
			case 11: return returner+"lrabin/lrabin38.nm";
			case 12: return returner+"lrabin/lrabin44.nm";
			case 13: return returner+"lrabin/lrabin45.nm";
			case 14: return returner+"lrabin/lrabin46.nm";
			case 15: return returner+"lrabin/lrabin48.nm";
			case 16: return returner+"mutual/mutual10.nm";
			case 17: return returner+"mutual/mutual4.nm";
			case 18: return returner+"mutual/mutual5.nm";
			case 19: return returner+"mutual/mutual8.nm";
			
			case 20: return returner+"phil/phil15.nm";
			case 21: return returner+"phil/phil2.nm";
			case 22: return returner+"phil/phil20.nm";
			case 24: return returner+"phil/phil25.nm";
			case 25: return returner+"phil/phil3.nm";
			case 26: return returner+"phil/phil30.nm";
			case 27: return returner+"phil/phil35.nm";
			case 28: return returner+"phil/phil4.nm";
			case 29: return returner+"phil/phil5.nm";
			case 30: return returner+"phil/phil6.nm";
			case 31: return returner+"phil/phil7.nm";
			case 32: return returner+"phil/phil8.nm";
			case 33: return returner+"phil/phil9.nm";
			
			case 35: return returner+"rabin/rabin12.nm";
			case 36: return returner+"rabin/rabin3.nm";
			case 37: return returner+"rabin/rabin4.nm";
			case 38: return returner+"rabin/rabin5.nm";
			case 39: return returner+"rabin/rabin6.nm";
			case 40: return returner+"rabin/rabin8.nm";
			case 41: return returner+"leader/asynchronous/leader6.nm";
			case 42: return returner+"leader/asynchronous/leader7.nm";


		}
		return null;
	}

	public void addConstantsForTestFile(int i, Values values)
	{
		switch(i)
		{
			case 0: values.addValue("N", new Integer(2)); return;
			case 1: return;
			case 2: return;
			case 3: values.addValue("deadline", new Integer(10)); return; //eg
			case 4: return;
			case 5: return;
			case 6: return;
			case 7:	 return;
			case 8: return;
			case 9: return;
			case 10: return;
			case 11: return;
			case 12: return;
			case 13: return;
			case 14: return;
			case 15: return;
			case 16: return;
			case 17: return;
			case 18: return;
			case 19: return;
			case 20: return;
			case 21: return;
			case 22: return;
			case 23: return;
			case 24: return;
			case 25: return;
			case 26: return;
			case 27: return;
			case 28: return;
			case 29: return;
			case 30: return;
			case 31: return;
			case 32: return;
			case 33: return;
			case 34: return;
			case 35: return;
			case 36: return;
			case 37: return;
			case 38: return;
			case 39: return;
			case 40: return;
			case 41: return;
            case 42: return;

		}
	}

	public int getNumTestCases()
	{
		return 1;
	}


}
