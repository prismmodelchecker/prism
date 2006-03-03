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


public class ExplorationTesting
{
	public static void main(String[]args)
	{
		try
		{
			//PRISM initialisation stuff
			Prism p = new Prism(null, null);

			//Parse model
			ModulesFile mf = p.parseModelFile(new File(args[0]));

			//Define constants

			Values constants = new Values();

			ConstantList modConstants = mf.getConstantList();
			if(modConstants.getNumUndefined() > 0)
				System.out.println("Please enter values for the following undefined model constants:"); 
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); 
			String input;
			if(modConstants != null)
			{
				for(int i = 0; i < modConstants.getNumUndefined(); i++)
				{
					
					String var = (String)modConstants.getUndefinedConstants().elementAt(i);
					
					boolean valid = true;
					do
					{
						System.out.print(var+": ");
						input = br.readLine();
						if(input.equals("true"))
						{
							constants.addValue(var, new Integer(1));
						}
						else if(input.equals("false"))
						{
							constants.addValue(var, new Integer(0));
						}
						else
						{
							try
							{
								constants.addValue(var, new Integer(input));
								valid = true;
							}
							catch(NumberFormatException e)
							{
								valid = false;
							}
						}
					} while(!valid);
				}
			}
			System.out.println("Constants done...");


			
			//Dummy PropertiesFile
			PropertiesFile pf = new PropertiesFile(mf);

			//Obtain the default initial state
			Values v = mf.getInitialValues();

			//Setup the simulator engine API
			SimulatorEngine engine = new SimulatorEngine();
		
			//Load in state v
			engine.startNewPath(mf, pf, v);

			//At this point, the updates set has already been calculated, this can be queried:
			int numUpdates = engine.getNumUpdates();
			System.out.println("Number of updates: "+numUpdates);
			System.out.println();
			System.out.println("Module\tAction\tProb\tAssignments");
			System.out.println("==============================================================");
			for(int i = 0; i < numUpdates; i++)
			{
				System.out.println(engine.getModuleNameOfUpdate(i)+"\t"+
					engine.getActionLabelOfUpdate(i)+"\t"+
					engine.getProbabilityOfUpdate(i)+"\t"+
					engine.getAssignmentDescriptionOfUpdate(i));
				//Note there are many, many more querying methods should you require them
			}

			//Try another set of values
			while(true)
			{
				System.out.println();
				System.out.println("Try another set of values: ");
				v = new Values();
				//simple for loop to read some values from the command line.
				
				for(int i = 0; i < mf.getVarNames().size(); i++)
				{
					String var = (String)mf.getVarNames().elementAt(i);
					
					boolean valid = true;
					do
					{
						System.out.print(var+": ");
						input = br.readLine();
						if(input.equals("true"))
						{
							v.addValue(var, new Integer(1));
						}
						else if(input.equals("false"))
						{
							v.addValue(var, new Integer(0));
						}
						else
						{
							try
							{
								v.addValue(var, new Integer(input));
								valid = true;
							}
							catch(NumberFormatException e)
							{
								valid = false;
							}
						}
					} while(!valid);
				}

				//Load in state v
				engine.restartNewPath(v);

				//At this point, the updates set has already been calculated, this can be queried:
				numUpdates = engine.getNumUpdates();
				System.out.println("Number of updates: "+numUpdates);
				System.out.println();
				System.out.println("Module\tAction\tProb\tAssignments");
				System.out.println("==============================================================");
				for(int i = 0; i < numUpdates; i++)
				{
					System.out.println(engine.getModuleNameOfUpdate(i)+"\t"+
						engine.getActionLabelOfUpdate(i)+"\t"+
						engine.getProbabilityOfUpdate(i)+"\t"+
						engine.getAssignmentDescriptionOfUpdate(i));
					//Note there are many, many more querying methods should you require them
				}

			}

		}
		catch(Exception e)
		{
			System.out.println("Error: "+e.getMessage());
			e.printStackTrace();
		}
	}


}
