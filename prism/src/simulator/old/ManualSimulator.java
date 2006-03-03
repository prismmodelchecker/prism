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
public class ManualSimulator
{
	private Prism p;
	private Expression e;
	private SimulatorEngine s;

	public static void main(String[]args)
	{
		if(args.length == 1)
			new ManualSimulator(args[0], null, false);
		else if(args.length == 2)
			new ManualSimulator(args[0], args[1], false);
		else if(args.length == 3)
			new ManualSimulator(args[0], args[1], args[2].equals("-sample"));

		else
		{
			System.out.println("usage: simulator <model_file>");
			System.out.println("       simulator <model_file> <properties_file>");
			System.out.println("       simulator <model_file> <properties_file> -sample");
		}	
	}

	public ManualSimulator(String parameter, String properties, boolean doSampling)
	{
		//initialise prism
		p = new Prism(null, null);
		
		//initialise simulator engine
		s = new SimulatorEngine(p);
		int type = 0;
		
		try
		{
			//Parse model
			ModulesFile m = p.parseModelFile(new File(parameter));
			
			//Load this into simulator engine
			s.loadModulesFile(m);

			//Console reader
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); 
			String input;


			//Sort out the initial state
			System.out.println("Please enter values for the initial state.");

			Values init = new Values();
			for(int i = 0; i < m.getVarNames().size(); i++)
			{
				String var = (String)m.getVarNames().elementAt(i);
					
				boolean valid = true;
				do
				{
					System.out.print(var+": ");
					input = br.readLine();
					if(input.equals("true"))
					{
						init.addValue(var, new Integer(1));
					}
					else if(input.equals("false"))
					{
						init.addValue(var, new Integer(0));
					}
					else
					{
						try
						{
							init.addValue(var, new Integer(input));
							valid = true;
						}
						catch(NumberFormatException e)
						{
							valid = false;
						}
					}
				} while(!valid);
			}
			System.out.println("Initial state done...");
			System.out.println("");
			 

			Values constants = new Values();

			ConstantList modConstants = m.getConstantList();
			if(modConstants.getNumUndefined() > 0)
				System.out.println("Please enter values for the following undefined model constants:"); 

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
			System.out.println("");

			//define the constants
			m.setUndefinedConstants(constants);

			//properties
			PropertiesFile pf = null;
			Values propertyConstants = new Values();
			
			if(properties != null)
			{
				pf = p.parsePropertiesFile(m, new File(properties));

				s.loadProperties(pf);

				Values propConValues = new Values();

				Vector pConstants = pf.getUndefinedConstants();

				if(pConstants != null)
				{
					if(pConstants.size() > 0)
						System.out.println("Please enter values for the following undefined property constants:");
					for(int i = 0; i < pConstants.size(); i++)
					{
					

					
						String var = (String)pConstants.elementAt(i);
					
						boolean valid = true;
						do
						{
							System.out.print(var+": ");
							input = br.readLine();
							if(input.equals("true"))
							{
								propConValues.addValue(var, new Integer(1));
							}
							else if(input.equals("false"))
							{
								propConValues.addValue(var, new Integer(0));
							}
							else
							{
								try
								{
									propConValues.addValue(var, new Integer(input));
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

				pf.setUndefinedConstants(propConValues);

				propertyConstants = pf.getConstantValues();

				s.startNewPath(m.getConstantValues(), propertyConstants, init);

				for(int i = 0; i < pf.getNumProperties(); i++)
				{
					s.addPCTLProperty(pf.getProperty(i));
				}


			}
			else 
				s.startNewPath(m.getConstantValues(), propertyConstants, init);
			

			System.out.println("Simulator loaded successfully...");
			System.out.println("");
			

			if(doSampling)
			{
				s.doSampling();
				System.exit(0);
			}
			
			System.out.println(SimulatorEngine.pathToString());
			SimulatorEngine.printRegisteredPathFormulae();
			SimulatorEngine.printCurrentUpdates();


			

			System.out.print("SIM::> ");
			try 
			{ 
				input = br.readLine(); 
				while(!input.equals("exit"))
				{
					
					if(input.equals("reset"))
					{
						s.initialiseModel();
						s.loadModel(m);
						
						s.startNewPath(m.getConstantValues(), propertyConstants, init);
						if(pf != null)
						{
							for(int i = 0; i < pf.getNumProperties(); i++)
							{
								s.addPCTLProperty(pf.getProperty(i));
							}
						}
						System.out.println(SimulatorEngine.pathToString());
						SimulatorEngine.printRegisteredPathFormulae();
						SimulatorEngine.printCurrentUpdates();
					}
					if(input.equals("backtrack"))
					{
						System.out.print("to step: ");
						input = br.readLine();
						try
						{
							int i = Integer.parseInt(input);
							if(i>=0)//protection
							{
								s.backtrack(i);
								
								System.out.println(SimulatorEngine.pathToString());
								SimulatorEngine.printRegisteredPathFormulae();
								SimulatorEngine.printCurrentUpdates();
							}
						}
						catch(NumberFormatException e)
						{
							System.out.println("Invalid input");
						}

					}
					else if(input.equals("remove"))
					{
						System.out.print("to step: ");
						input = br.readLine();
						try
						{
							int i = Integer.parseInt(input);
							if(i>=0)//protection
							{
								s.removePrecedingStates(i);
								
								System.out.println(SimulatorEngine.pathToString());
								SimulatorEngine.printRegisteredPathFormulae();
								SimulatorEngine.printCurrentUpdates();
							}
						}
						catch(NumberFormatException e)
						{
							System.out.println("Invalid input");
						}
						
					}
					else if(input.equals("auto"))
					{
						System.out.println("how many steps: ");
						input = br.readLine();
						try
						{
							int i = Integer.parseInt(input);
							if(i>=0)
							{
								long cl = System.currentTimeMillis();
								s.automaticChoices(i);
								long ti = System.currentTimeMillis() - cl;
								System.out.println(SimulatorEngine.pathToString());
								SimulatorEngine.printRegisteredPathFormulae();
								SimulatorEngine.printCurrentUpdates();
								//System.out.println(""+i+" steps took: "+ti+"ms");
							}
						}
						catch(NumberFormatException e)
						{
							System.out.println("Invalid input");
						}
					}
					else //manual update
					{
						try
						{
						
							int i = Integer.parseInt(input);

							if(type == ModulesFile.STOCHASTIC)
							{
								System.out.print("time in state: ");
								input = br.readLine();
								try
								{
									double d = Double.parseDouble(input);
									
										s.manualUpdate(i,d);
									
								}
								catch(NumberFormatException e)
								{
									System.out.println("Invalid input");
								}
							}
							else
								s.manualUpdate(i);
							System.out.println();
							System.out.println(SimulatorEngine.pathToString());
							SimulatorEngine.printRegisteredPathFormulae();
							SimulatorEngine.printCurrentUpdates();
						}
						catch(NumberFormatException e)
						{
							System.out.println("Invalid input, enter the update index");
						}
						catch(SimulatorException e)
						{
							System.out.println("Invalid input: "+e.getMessage());
						}
						
					}
					System.out.print("SIM::> ");
					input = br.readLine();
				}
			} 
			catch (IOException ioe) 
			{ 
				System.out.println("IO error trying to read your name!"); 
				System.exit(1); 
			} 
			

		}
		catch(Exception e)
		{
			System.out.println("Error: "+e.getMessage());
			e.printStackTrace();
		}
	}

	


}
