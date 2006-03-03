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
public class ExploreTesting
{
	private Prism p;
	private Expression e;
	private SimulatorEngine s;

	public static void main(String[]args)
	{
		if(args.length > 0)
		new ExploreTesting(args[0]);
		else
		new ExploreTesting("1");
	}

	public ExploreTesting(String parameter)
	{
		try
		{
		p = new Prism(null, null);
		s = new SimulatorEngine(p);
		int type = 0;
		
		
			ModulesFile m = p.parseModelFile(new File
				//("../examples/dice/dice.pm"));
				//("../examples/leader/synchronous/leader3_4.pm"));
				//("../examples/dice/two_dice.nm"));
				//("../examples/leader/asynchronous/leader4.nm"));
				//("../andrew.pm"));
				("../examples/cluster/cluster.sm"));
				//("../examples/polling/poll3.sm"));
				//("../examples/cyclin.sm"));
				//("../test.sm"));
				//("../examples/molecules/nacl.sm"));
				//("../examples/brp/brp.pm"));
				//("../examples/tandem/tandem.sm"));
			//m.tidyUp();
			//m.setUndefinedConstants(constants);
			//System.out.println(m.toString()+"\n\n\n\n\n\n\n");
			type = m.getType();
			long before= System.currentTimeMillis();
			s.loadModulesFile(m);
			Values init = new Values();
			
			//leader3_4.pm
			/*init.addValue("c", new Integer(1));
			init.addValue("s1", new Integer(0));
			init.addValue("u1", new Boolean(false));
			init.addValue("v1", new Integer(0));
			init.addValue("p1", new Integer(0));
			init.addValue("s2", new Integer(0));
			init.addValue("u2", new Boolean(false));
			init.addValue("v2", new Integer(0));
			init.addValue("p2", new Integer(0));
			init.addValue("s3", new Integer(0));
			init.addValue("u3", new Boolean(false));
			init.addValue("v3", new Integer(0));
			init.addValue("p3", new Integer(0)); */ 
			
			//dice.pm
			//init.addValue("d", new Integer(0));
			//init.addValue("s", new Integer(0));

			//two_dice.nm
			//init.addValue("d1", new Integer(0));
			//init.addValue("d2", new Integer(0));
			//init.addValue("s1", new Integer(0));
			//init.addValue("s2", new Integer(0));

			//leader4.nm
			/*init.addValue("c1", new Integer(0));
			init.addValue("s1", new Integer(0));
			init.addValue("p1", new Integer(0));
			init.addValue("receive1", new Integer(0));
			init.addValue("sent1", new Integer(0));
			init.addValue("c2", new Integer(0));
			init.addValue("s2", new Integer(0));
			init.addValue("p2", new Integer(0));
			init.addValue("receive2", new Integer(0));
			init.addValue("sent2", new Integer(0));
			init.addValue("c3", new Integer(0));
			init.addValue("s3", new Integer(0));
			init.addValue("p3", new Integer(0));
			init.addValue("receive3", new Integer(0));
			init.addValue("sent3", new Integer(0));
			init.addValue("c4", new Integer(0));
			init.addValue("s4", new Integer(0));
			init.addValue("p4", new Integer(0));
			init.addValue("receive4", new Integer(0));
			init.addValue("sent4", new Integer(0));	*/


			//cluster.sm
			init.addValue("left_n", new Integer(parameter));
			init.addValue("left", new Boolean(false));
			init.addValue("right_n", new Integer(parameter));
			init.addValue("right", new Boolean(false));
			init.addValue("line", new Boolean(false));
			init.addValue("line_n", new Boolean(true));
			init.addValue("Toleft", new Boolean(false));
			init.addValue("Toright", new Boolean(false));
			init.addValue("Toleft_n", new Boolean(true));
			init.addValue("Toright_n", new Boolean(true));
			init.addValue("r", new Boolean(false));	 

			//andrew.pm
			//init.addValue("s", new Integer(0));

			//poll23.sm
			//init.addValue("s", new Integer(1));
			//init.addValue("a", new Integer(0));
			//init.addValue("s1", new Integer(0));
			//init.addValue("s2", new Integer(0));
			//init.addValue("s3", new Integer(0));
			//init.addValue("s4", new Integer(0));
			//init.addValue("s5", new Integer(0));
			//init.addValue("s6", new Integer(0));
			//init.addValue("s7", new Integer(0));
			//init.addValue("s8", new Integer(0));
			//init.addValue("s9", new Integer(0));
			//init.addValue("s10", new Integer(0));
			//init.addValue("s11", new Integer(0));
			//init.addValue("s12", new Integer(0));
			//init.addValue("s13", new Integer(0));
			//init.addValue("s14", new Integer(0));
			//init.addValue("s15", new Integer(0));
			//init.addValue("s16", new Integer(0));
			//init.addValue("s17", new Integer(0));
			//init.addValue("s18", new Integer(0));
			//init.addValue("s19", new Integer(0));
			//init.addValue("s20", new Integer(0));
			//init.addValue("s21", new Integer(0));
			//init.addValue("s22", new Integer(0));
			//init.addValue("s23", new Integer(0));  

			//cyclic.sm
			/*init.addValue("cyclin", new Integer(4));
			init.addValue("cyclin_bound", new Integer(0));
			init.addValue("degc", new Integer(0));
			init.addValue("trim", new Integer(0));
			init.addValue("dim", new Integer(0));

			init.addValue("bound1", new Integer(0));
			init.addValue("bound2", new Integer(0));

			init.addValue("cdk", new Integer(2));
			init.addValue("cdk_cat", new Integer(0));

			init.addValue("cdh1", new Integer(2));
			init.addValue("inact", new Integer(0));

			init.addValue("cdc14", new Integer(4));
			
			init.addValue("cki", new Integer(2));  */

			//test.sm
			//init.addValue("x", new Integer(0));

			//nacl.sm
			//init.addValue("na", new Integer(10));
			//init.addValue("cl", new Integer(10));
			//init.addValue("dummy", new Boolean(false));

			//brp.pm
			/*init.addValue("s", new Integer(0));
			init.addValue("srep", new Integer(0));
			init.addValue("nrtr", new Integer(0));
			init.addValue("i", new Integer(0));
			init.addValue("bs", new Boolean(false));
			init.addValue("s_ab", new Boolean(false));
			init.addValue("fs", new Boolean(false));
			init.addValue("ls", new Boolean(false));

			init.addValue("r", new Integer(0));
			init.addValue("rrep", new Integer(0));
			init.addValue("fr", new Boolean(false));
			init.addValue("lr", new Boolean(false));
			init.addValue("br", new Boolean(false));
			init.addValue("r_ab", new Boolean(false));
			init.addValue("recv", new Boolean(false));

			init.addValue("T", new Boolean(false));

			init.addValue("k", new Boolean(false));

			init.addValue("l", new Boolean(false));	*/

			//tandem.sm

			//init.addValue("sc", new Integer(0));
			//init.addValue("ph", new Integer(1));
			//init.addValue("sm", new Integer(0));


			Values constants = new Values();//m.getConstantValues();
			//constants.addValue("c", new Integer(124));

			//constants.addValue("r", new Double(parameter));

			//brp constants
			//constants.addValue("N", new Integer(16));
			//constants.addValue("MAX", new Integer(2));


			//cluster constants
			constants.addValue("N", new Integer(parameter));
			//constants.addValue("T", new Integer(10));

			//tandem constants
			//constants.addValue("c", new Integer(10));
			
			m.setUndefinedConstants(constants);
			Values v = m.getConstantValues();
			System.out.println(v.toString());

			PropertiesFile pf = p.parsePropertiesFile(m, new File
				//("../examples/dice/dice2.pctl"));
				("../examples/cluster/cluster.csl"));
			//("../examples/polling/pollandrew.csl"));
			//("../examples/cyclin.csl"));
			//("../test.csl"));
			//("../examples/molecules/nacl.csl"));
			//("../examples/brp/brp.pctl"));
			//("../examples/tandem/tandem.csl"));
			System.out.println(pf.toString());

			s.loadProperties(pf);

			Values pConstants = new Values();

			//cluster property constants
			pConstants.addValue("T", new Integer(10));

			pf.setUndefinedConstants(pConstants);
			Values pv = pf.getConstantValues();

			s.startNewPath(m.getConstantValues(), init);
			//s.startNewPath(m.getConstantValues(), pf.getConstantValues(), init);
			long timeTaken = System.currentTimeMillis() - before;
			//System.out.println("Loading model complete!! Took: "+timeTaken+"ms\n\n");

			System.out.println(SimulatorEngine.modelToString());
			System.out.println(SimulatorEngine.pathToString());




			//pctl
				
			for(int i = 0; i < pf.getNumProperties(); i++)
			{
				System.out.println("adding property");
				s.addPCTLProperty(pf.getProperty(i));
			}

			//SimulatorEngine.printRegisteredPathFormulae();


			//s.doSampling();
			  
			  
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); 

			String input = null; 

			System.out.print("SIM::> ");
			try 
			{ 
				input = br.readLine(); 
				while(!input.equals("exit"))
				{
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
								//SimulatorEngine.printRegisteredPathFormulae();
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
								//SimulatorEngine.printRegisteredPathFormulae();
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
								//SimulatorEngine.printRegisteredPathFormulae();
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
							//SimulatorEngine.printRegisteredPathFormulae();
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
