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
public class ExpressionTesting
{
	private Prism p;
	private Expression e;
	private SimulatorEngine s;

	public static void main(String[]args)
	{
		new ExpressionTesting();

	}

	public ExpressionTesting()
	{
		try
		{
			p = new Prism(null, null);
			s = new SimulatorEngine(p);

			int startAt = 0;
			int current;
	
		
			for(current = startAt; current < tests.length; current++)
			{
				e = p.parseSingleExpressionString(tests[current]);
				System.out.print("Test "+(current+1)+"\t"+tests[current]+"\t");
				//s = new SimulatorEngine(p);
				int sim = e.toSimulator(s);
				System.out.print(SimulatorEngine.expressionToString(sim));
				System.out.println("");
				SimulatorEngine.deleteExpression(sim);
			}
		}
		catch(Exception e)
		{
			System.out.println("Error: "+e.getMessage());
		}
	}

	public static String[] tests =
		{
			"(!false)",
			"!(3=4)",
			"!(3!=4)",
			"!(3>4)",
			"!(3<4)",
			"!(3>=4)",
			"!(3<=4)",
			"!(true & true)",
			"!(true & true & true)",
			"!(true | true)",
			"!(true | true | false)",
			"!false",
			"!true",
			"ceil(2.23*2)",
			"ceil(2.23-2.0)",
			"ceil(1/2)",
			"ceil(max(2.0,3.0,4.5))",
			"ceil(2.23)",
			"floor(2.23*2)",
			"floor(2.23-2.0)",
			"floor(1/2)",
			"floor(max(2.0,3.0,4.5))",
			"floor(2.23)",
			"(!true)=(!true)",
			"(!true)=(true=true)",
			"(!true)=(true&true)",
			"(!true)=(true & true & true)",
			"(!true)=(true | true)",
			"(!true)=(true | true | true)",
			"(!true)=true",
			"(!true)=false",
			"(true=true)=(!true)",
			"(true=true)=(true=true)",
			"(true=true)=(true&true)",
			"(true=true)=(true & true & true)",
			"(true=true)=(true | true)",
			"(true=true)=(true | true | true)",
			"(true=true)=true",
			"(true=true)=false",
			"(true & true & true)=(!true)",
			"(true&true&true)=(true=true)",
			"(true & true & true)=(true&true)",
			"(true & true & true)=(true & true & true)",
			"(true & true & true)=(true | true)",
			"(true & true & true)=(true | true | true)",
			"(true & true & true)=true",
			"(true & true & true)=false",
			"(true | true | true)=(!true)",
			"(true | true | true)=(true=true)",
			"(true | true | true)=(true&true)",
			"(true | true | true)=(true & true & true)",
			"(true | true | true)=(true | true)",
			"(true | true | true)=(true | true | true)",
			"(true | true | true)=true",
			"(true | true | true)=false",
			"true=(!true)",
			"true=(true=true)",
			"true=(true&true)",
			"true=(true & true & true)",
			"true=(true | true)",
			"true=(true | true | true)",
			"true=true",
			"true=false",
			"(ceil(2.4))=(ceil(2.4))",
			"(ceil(2.4))=(floor(2.4))",
			"(ceil(2.4))=(2.0*4.0)",
			"(ceil(2.4))=(2.0+4.0)",
			"(ceil(2.4))=(2.0/4.0)",
			"(ceil(2.4))=(2.0-4.0)",
			"(ceil(2.4))=((true) ? (3.0) : (4.0))",
			"(ceil(2.4))=max(2.0, 2.4)",
			"(ceil(2.4))=min(2.0, 2.4)",
			"(ceil(2.4))=3.0",
			"(!true)!=(!true)",
			"(!true)!=(true=true)",
			"(!true)!=(true&true)",
			"(!true)!=(true & true & true)",
			"(!true)!=(true | true)",
			"(!true)!=(true | true | true)",
			"(!true)!=true",
			"(!true)!=false",
			"(true=true)!=(!true)",
			"(true=true)!=(true=true)",
			"(true=true)!=(true&true)",
			"(true=true)!=(true & true & true)",
			"(true=true)!=(true | true)",
			"(true=true)!=(true | true | true)",
			"(true=true)!=true",
			"(true=true)!=false",
			"(true & true & true)!=(!true)",
			"(true&true&true)!=(true=true)",
			"(true & true & true)!=(true&true)",
			"(true & true & true)!=(true & true & true)",
			"(true & true & true)!=(true | true)",
			"(true & true & true)!=(true | true | true)",
			"(true & true & true)!=true",
			"(true & true & true)!=false",
			"(true | true | true)!=(!true)",
			"(true | true | true)!=(true=true)",
			"(true | true | true)!=(true&true)",
			"(true | true | true)!=(true & true & true)",
			"(true | true | true)!=(true | true)",
			"(true | true | true)!=(true | true | true)",
			"(true | true | true)!=true",
			"(true | true | true)!=false",
			"true!=(!true)",
			"true!=(true=true)",
			"true!=(true&true)",
			"true!=(true & true & true)",
			"true!=(true | true)",
			"true!=(true | true | true)",
			"true!=true",
			"true!=false",
			"(ceil(2.4))!=(ceil(2.4))",
			"(ceil(2.4))!=(floor(2.4))",
			"(ceil(2.4))!=(2.0*4.0)",
			"(ceil(2.4))!=(2.0+4.0)",
			"(ceil(2.4))!=(2.0/4.0)",
			"(ceil(2.4))!=(2.0-4.0)",
			"(ceil(2.4))!=((true) ? (3.0) : (4.0))",
			"(ceil(2.4))!=max(2.0, 2.4)",
			"(ceil(2.4))!=min(2.0, 2.4)",
			"(ceil(2.4))!=3.0",
			"(ceil(2.4))>(ceil(2.4))",
			"(ceil(2.4))>(floor(2.4))",
			"(ceil(2.4))>(2.0*4.0)",
			"(ceil(2.4))>(2.0+4.0)",
			"(ceil(2.4))>(2.0/4.0)",
			"(ceil(2.4))>(2.0-4.0)",
			"(ceil(2.4))>((true) ? (3.0) : (4.0))",
			"(ceil(2.4))>max(2.0, 2.4)",
			"(ceil(2.4))>min(2.0, 2.4)",
			"(ceil(2.4))>3.0",
			"(ceil(2.4))<(ceil(2.4))",
			"(ceil(2.4))<(floor(2.4))",
			"(ceil(2.4))<(2.0*4.0)",
			"(ceil(2.4))<(2.0+4.0)",
			"(ceil(2.4))<(2.0/4.0)",
			"(ceil(2.4))<(2.0-4.0)",
			"(ceil(2.4))<((true) ? (3.0) : (4.0))",
			"(ceil(2.4))<max(2.0, 2.4)",
			"(ceil(2.4))<min(2.0, 2.4)",
			"(ceil(2.4))<3.0",
			"(ceil(2.4))>=(ceil(2.4))",
			"(ceil(2.4))>=(floor(2.4))",
			"(ceil(2.4))>=(2.0*4.0)",
			"(ceil(2.4))>=(2.0+4.0)",
			"(ceil(2.4))>=(2.0/4.0)",
			"(ceil(2.4))>=(2.0-4.0)",
			"(ceil(2.4))>=((true) ? (3.0) : (4.0))",
			"(ceil(2.4))>=max(2.0, 2.4)",
			"(ceil(2.4))>=min(2.0, 2.4)",
			"(ceil(2.4))>=3.0",
			"(ceil(2.4))<=(ceil(2.4))",
			"(ceil(2.4))<=(floor(2.4))",
			"(ceil(2.4))<=(2.0*4.0)",
			"(ceil(2.4))<=(2.0+4.0)",
			"(ceil(2.4))<=(2.0/4.0)",
			"(ceil(2.4))<=(2.0-4.0)",
			"(ceil(2.4))<=((true) ? (3.0) : (4.0))",
			"(ceil(2.4))<=max(2.0, 2.4)",
			"(ceil(2.4))<=min(2.0, 2.4)",
			"(ceil(2.4))<=3.0",
			"3.0*ceil(2.4)",
			"3.0*floor(2.4)",
			"3.0*(3.0*4.0)",
			"3.0*(3.0+4.0)",
			"3.0*(3.0/4.0)",
			"3.0*(4.0-3.0)",
			"3.0*((true) ? (3.0) : (4.0))",
			"3.0*max(2.5, 2.6)",
			"3.0*min(2.5, 2.6)",
			"3.0*3.0",
			"3*(3*4)",
			"3*(3+4)",
			"3*(3/4)",
			"3*(4-3)",
			"3*((true) ? (3) : (4))",
			"3*max(3,4)",
			"3*min(3,4)",
			"3*3",
			"3.0+ceil(2.4)",
			"3.0+floor(2.4)",
			"3.0+(3.0*4.0)",
			"3.0+(3.0+4.0)",
			"3.0+(3.0/4.0)",
			"3.0+(4.0-3.0)",
			"3.0+((true) ? (3.0) : (4.0))",
			"3.0+max(2.5, 2.6)",
			"3.0+min(2.5, 2.6)",
			"3.0+3.0",
			"3+(3*4)",
			"3+(3+4)",
			"3+(3/4)",
			"3+(4-3)",
			"3+((true) ? (3) : (4))",
			"3+max(3,4)",
			"3+min(3,4)",
			"3+3"};



}
