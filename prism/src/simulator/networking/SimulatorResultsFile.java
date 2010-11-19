//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

package simulator.networking;

import java.util.*;
import java.io.*;
import prism.*;

public class SimulatorResultsFile extends Observable
{
	
	private ArrayList results;
	
	/** Creates a new instance of SimulatorResultsFile */
	public SimulatorResultsFile()
	{
		results = new ArrayList();
	}
	
	public void tellObservers()
	{
		setChanged();
		notifyObservers(null);
	}
	
	public synchronized void mergeResultsFile(File resultFile) throws PrismException
	{
		try
		{
			BufferedReader buff = new BufferedReader(new FileReader(resultFile));
			
			int counter = 0;
			while(buff.ready())
			{
				if(counter >= results.size())
				{
					results.add(new SimulatorResult(buff.readLine()));
				}
				else
				{
					((SimulatorResult)results.get(counter)).merge(buff.readLine());
				}
				counter++;
			}
		}
		catch(Exception e)
		{
			throw new PrismException(e.getMessage());
		}
	}
	
	public double getResult(int index)
	{
		if(index < results.size())
		{
			SimulatorResult res = (SimulatorResult)results.get(index);
			return res.result / ((double)res.iterations);
		}
		else return -1.0;
	}
	
	public int getIterations(int index)
	{
		if(index < results.size())
		{
			SimulatorResult res = (SimulatorResult)results.get(index);
			return res.iterations;
		}
		else return -1;
	}
	
	public double getSum(int index)
	{
		if(index < results.size())
		{
			SimulatorResult res = (SimulatorResult)results.get(index);
			return res.result;
		}
		else return -1;
	}
	
	public void reset()
	{
		results = new ArrayList();
		tellObservers();
	}
	
	public String toString()
	{
		String str = "";
		for(int i = 0 ; i < results.size(); i++)
		{
			str += results.get(i).toString()+"\n";
		}
		return str;
	}
	
	public int getNumResults()
	{
		return results.size();
	}
	
	class SimulatorResult
	{
		int index;
		int iterations;
		double result;
		
		public SimulatorResult(int index, int iterations, double result)
		{
			this.index = index;
			this.iterations = iterations;
			this.result = result;
		}
		
		public SimulatorResult(String line) throws Exception
		{
			StringTokenizer tokens = new StringTokenizer(line);
			String token1 = tokens.nextToken();
			String token2 = tokens.nextToken();
			String token3 = tokens.nextToken();
			
			index = Integer.parseInt(token1);
			iterations = Integer.parseInt(token2);
			result = Double.parseDouble(token3);
		}
		
		public String toString()
		{
			return index+"\t"+iterations+"\t"+result;
		}
		
		public void merge(String line) throws PrismException
		{
			StringTokenizer tokens = new StringTokenizer(line);
			String token1 = tokens.nextToken();
			String token2 = tokens.nextToken();
			String token3 = tokens.nextToken();
			
			if(index != Integer.parseInt(token1)) throw new PrismException("Invalid results file, bad merge");
			iterations += Integer.parseInt(token2);
			result += Double.parseDouble(token3);
		}
		
		
	}
	
}
