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

/**	Test cases for expression building
 */
public class LoadModelTesting
{
	private Prism p;
	private Expression e;
	private SimulatorEngine s;

	public static void main(String[]args)
	{
		new LoadModelTesting();

	}

	public LoadModelTesting()
	{
		try
		{
		p = new Prism(null, null);
		s = new SimulatorEngine(p);

		
		
			ModulesFile m = p.parseModelFile(new File
				("../examples/dice/dice.pm"));
				//("../examples/leader/synchronous/leader3_4.pm"));
			//m.tidyUp();
			//m.setUndefinedConstants(constants);
			System.out.println(m.toString()+"\n\n\n\n\n\n\n");
			long before= System.currentTimeMillis();
			s.loadModulesFile(m);
			Values init = new Values();
			/*
			init.addValue("c", new Integer(1));
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
			init.addValue("p3", new Integer(0));*/
			
			init.addValue("d", new Integer(0));
			init.addValue("s", new Integer(0));
			
			Values constants = new Values();//m.getConstantValues();
			//constants.addValue("c", new Integer(124));
			
			m.setUndefinedConstants(constants);
			Values v = m.getConstantValues();
			System.out.println(v.toString());
			s.startNewPath(m.getConstantValues(), init);
			long timeTaken = System.currentTimeMillis() - before;
			System.out.println("Loading model complete!! Took: "+timeTaken+"ms\n\n");

			System.out.println(SimulatorEngine.modelToString());
			System.out.println(SimulatorEngine.pathToString());
		}
		catch(Exception e)
		{
			System.out.println("Error: "+e.getMessage());
			e.printStackTrace();
		}
	}

	


}
