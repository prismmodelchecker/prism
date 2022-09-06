//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package prism;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Stack;
import java.util.Vector;

import parser.Values;
import parser.ast.Expression;
import parser.ast.ForLoop;
import parser.type.Type;
import parser.type.TypeInt;

public class Preprocessor
{
	private final static char DELIMITER = '#';
	private final static boolean IGNORE_COMMENTS = true;

	// prism
	private Prism prism;

	// files
	private File modelFile;

	// preprocessing (PP) stuff
	private int numPPExprs;
	private String ppExprStrings[];
	private String ppExprs[];
	private int ppExprLines[];
	private String lastString;

	// interpreter stuff
	private int pc; // program counter
	private Stack<Object> stack; // control flow stack
	private String output; // output string
	private boolean outputEnabled; // output enabling flag
	private Vector<String> varNames; // variable names
	private Vector<Type> varTypes; // variable types
	private Vector<Integer> varScopes; // variable scopes
	private Values values; // variable values
	private int paramCounter; // how many paramaters found so far

	private String params[];

	// constructor

	public Preprocessor(Prism p, File mf)
	{
		prism = p;
		modelFile = mf;
	}

	public void setParameters(String args[])
	{
		params = args;
	}

	// main method: do preprocessing

	public String preprocess() throws PrismException
	{
		// see how many preprocessing expressions there are
		countPPExprs();
		// and bail out if none
		if (numPPExprs == 0)
			return null;

		// do preprocessing
		storePPExprs();
		interpret();

		return output;
	}

	// count the number of preprocessing expressions

	private void countPPExprs() throws PrismException
	{
		String s, s2;
		int i, count, lineNum = 0;

		numPPExprs = 0;
		try (BufferedReader in = new BufferedReader(new FileReader(modelFile))) {
			// read lines one by one
			s = in.readLine();
			lineNum++;
			while (s != null) {
				// strip any comments
				i = (IGNORE_COMMENTS) ? s.indexOf("//") : -1;
				s2 = (i != -1) ? s.substring(0, i) : s;
				// count delimiters
				count = 0;
				i = -1;
				while ((i = s2.indexOf(DELIMITER, i + 1)) != -1)
					count++;
				if (count % 2 != 0)
					throw new PrismException("Unterminated preprocessing expression at line " + lineNum);
				numPPExprs += (count / 2);
				// read next line
				s = in.readLine();
				lineNum++;
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + modelFile + "\"");
		}
	}

	// store the preprocessing expressions and related info

	private void storePPExprs() throws PrismException
	{
		BufferedReader in;
		String s, s1, s2, s3, text, ss[];
		int i, j, n, count, lineNum = 0;

		// allocate arrays
		ppExprStrings = new String[numPPExprs];
		ppExprs = new String[numPPExprs];
		ppExprLines = new int[numPPExprs];

		try {
			count = 0;
			text = "";
			// open file for reading
			in = new BufferedReader(new FileReader(modelFile));
			// read lines one by one
			s = in.readLine();
			lineNum++;
			while (s != null) {
				// split into non-comment(s1)/comment(s2)
				i = (IGNORE_COMMENTS) ? s.indexOf("//") : -1;
				s1 = (i != -1) ? s.substring(0, i) : s;
				s2 = (i != -1) ? s.substring(i) : "";
				// if there are delimiters no delimiters, move on
				i = s1.indexOf(DELIMITER);
				if (i == -1) {
					text += s1;
					text += s2;
					text += "\n";
				} else {
					// strip off stuff before first and after last delimiter
					j = s1.lastIndexOf(DELIMITER);
					s3 = s1.substring(i, j + 1);
					s2 = s1.substring(j + 1) + s2;
					s1 = s1.substring(0, i);
					// add trailing space so that split() catches any trailing empty pairs of delimiters
					s3 += " ";
					// go through delimiters
					ss = s3.split("" + DELIMITER);
					n = (ss.length - 1) / 2;
					// add first part of line to text
					// (unless this line contains just one pp expr and white space)
					if (!(n == 1 && s1.trim().length() == 0 && s2.trim().length() == 0))
						text += s1;
					for (i = 0; i < n; i++) {
						text += ss[2 * i];
						ppExprStrings[count] = text;
						ppExprs[count] = ss[2 * i + 1];
						ppExprLines[count] = lineNum;
						count++;
						text = "";
					}
					// add last part of line to text
					// (unless this line contains just one pp expr and white space)
					if (!(n == 1 && s1.trim().length() == 0 && s2.trim().length() == 0))
						text += s2 + "\n";
				}
				// read next line
				s = in.readLine();
				lineNum++;
			}
			lastString = text;
			// close file
			in.close();
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + modelFile + "\"");
		}
	}

	// go through the preprocessing expressions, interpreting them

	private void interpret() throws PrismException
	{
		String s;

		// initialise interpreter
		output = "";
		outputEnabled = true;
		pc = 0;
		stack = new Stack<Object>();
		varNames = new Vector<String>();
		varTypes = new Vector<Type>();
		varScopes = new Vector<Integer>();
		values = new Values();
		paramCounter = 0;

		// main control flow loop
		try {
			while (pc < numPPExprs) {

				// add text preceding this preprocessing expression to output
				if (outputEnabled)
					output += ppExprStrings[pc];

				// process current preprocessing expression
				s = ppExprs[pc].trim();

				// parameter
				if (s.indexOf("param int ") == 0) {
					s = s.substring(10).trim();
					interpretConstant(s);
				} else if (s.indexOf("const ") == 0) {
					// old notation - backwards compatability
					s = s.substring(6).trim();
					interpretConstant(s);
				}
				// for loops
				else if (s.indexOf("for ") == 0) {
					s = s.substring(4).trim();
					interpretForLoop(s, "");
				} else if (s.indexOf("& ") == 0) {
					s = s.substring(2).trim();
					interpretForLoop(s, "&");
				} else if (s.indexOf("| ") == 0) {
					s = s.substring(2).trim();
					interpretForLoop(s, "|");
				} else if (s.indexOf("* ") == 0) {
					s = s.substring(2).trim();
					interpretForLoop(s, "*");
				} else if (s.indexOf("+ ") == 0) {
					s = s.substring(2).trim();
					interpretForLoop(s, "+");
				} else if (s.indexOf(", ") == 0) {
					s = s.substring(2).trim();
					interpretForLoop(s, ",");
				} else if (s.indexOf("; ") == 0) {
					s = s.substring(2).trim();
					interpretForLoop(s, ";");
				}
				// end
				else if (s.equals("end")) {
					interpretEnd();
				}
				// empty expression
				else if (s.length() == 0) {
					// move to next statement
					pc++;
				}
				// anything else, i.e. arbitrary expression
				else {
					interpretExpression(s);
				}
			}
			// add final piece of text to output
			if (outputEnabled)
				output += lastString;
		} catch (PrismException e) {
			throw new PrismException(e.getMessage() + " (preprocessing expression \"" + ppExprs[pc] + "\" at line " + ppExprLines[pc] + ")");
		}
	}

	private void interpretConstant(String s) throws PrismException
	{
		int i;
		String name;
		Expression expr;

		// if not currently outputting, just skip this
		if (!outputEnabled) {
			pc++;
			return;
		}

		// get constant name - terminated by "=", white space or end of expression
		i = s.indexOf('=');
		if (i == -1)
			i = s.indexOf(' ');
		if (i == -1)
			i = s.indexOf('\t');
		if (i == -1)
			i = s.length();
		name = s.substring(0, i).trim();
		// check name is valid identifier
		if (!name.matches("[_a-zA-Z]([_a-zA-Z0-9])*"))
			throw new PrismException("Invalid constant name \"" + name + "\"");
		// and that it doesn't already exist
		if (varNames.contains(name))
			throw new PrismException("Duplicated variable/constant \"" + name + "\"");
		// see if a value if defined for it
		expr = null;
		s = s.substring(i).trim();
		if (s.length() > 0) {
			if (s.charAt(0) != '=') {
				throw new PrismException("Syntax error in constant definition");
			}
			// if so, get expression for value definition
			s = s.substring(1).trim();
			// parse expression, do some checks
			expr = Prism.parseSingleExpressionString(s);
			expr = (Expression) expr.findAllVars(varNames, varTypes);
			expr.typeCheck();
			expr.semanticCheck();
		}
		// set up new variable in interpreter
		varNames.add(name);
		varTypes.add(TypeInt.getInstance());
		varScopes.add(stack.size());
		if (expr != null) {
			values.addValue(name, new Integer(expr.evaluateInt(null, values)));
		} else {
			if (params.length <= paramCounter + 1)
				throw new PrismException("No value provided for undefined preprocessor constant \"" + name + "\"");
			values.addValue(name, new Integer(Integer.parseInt(params[++paramCounter])));
		}
		// move to next statement
		pc++;
	}

	private void interpretForLoop(String s, String between) throws PrismException
	{
		ForLoop fl;

		// if not currently outputting, just stick a dummy for loop on the stack and move on
		if (!outputEnabled) {
			stack.push("Dummy for loop");
			pc++;
			return;
		}

		// parse for loop, do some checks
		fl = prism.parseForLoopString(s);
		if (varNames.contains(fl.getLHS()))
			throw new PrismException("Duplicated variable/constant \"" + fl.getLHS() + "\"");
		fl = (ForLoop) fl.findAllVars(varNames, varTypes);
		fl.typeCheck();
		fl.semanticCheck();
		// set up more info and then put on stack
		fl.setPC(pc + 1);
		fl.setBetween(between);
		stack.push(fl);
		// set up new variable in interpreter
		varNames.add(fl.getLHS());
		varTypes.add(TypeInt.getInstance());
		varScopes.add(stack.size());
		values.addValue(fl.getLHS(), new Integer(fl.getFrom().evaluateInt(null, values)));
		// if for loop trivially not satisfied, set output flag to false
		if (fl.getFrom().evaluateInt(null, values) > fl.getTo().evaluateInt(null, values)) {
			outputEnabled = false;
		}

		// move to next statement
		pc++;
	}

	private void interpretEnd() throws PrismException
	{
		int i, j;
		ForLoop fl;

		// make sure there is something to end
		if (stack.empty())
			throw new PrismException("Surplus \"end\" statement");

		// end of for loop
		if (stack.peek() instanceof ForLoop) {
			fl = (ForLoop) stack.peek();
			// remove variables that will become out of scope (except loop counter)
			i = stack.size();
			j = 0;
			while (j < varNames.size()) {
				if (varScopes.get(j) >= i && !varNames.get(j).equals(fl.getLHS())) {
					varNames.removeElementAt(j);
					varTypes.removeElementAt(j);
					varScopes.removeElementAt(j);
					values.removeValue(j);
				} else {
					j++;
				}
			}
			// if not outputting (which means that the condition for the for loop was initially false)
			// the end for loop
			if (!outputEnabled) {
				// reset flag
				outputEnabled = true;
				// remove loop counter variable
				j = varNames.indexOf(fl.getLHS());
				varNames.removeElementAt(j);
				varTypes.removeElementAt(j);
				varScopes.removeElementAt(j);
				values.removeValue(j);
				// remove for loop from stack
				stack.pop();
				// move to next statement
				pc++;
			}
			// otherwise increment to see if we have finished yet
			else {
				// increment for loop
				i = values.getIntValueOf(fl.getLHS());
				i += fl.getStep().evaluateInt(null, values);
				// if loop is not finished...
				if (i <= fl.getTo().evaluateInt(null, values)) {
					// update value of loop counter
					values.setValue(fl.getLHS(), new Integer(i));
					// add "between" character to text
					output += fl.getBetween();
					// go back to start of loop
					pc = fl.getPC();
				}
				// if loop is finished...
				else {
					// remove loop counter variable
					j = varNames.indexOf(fl.getLHS());
					varNames.removeElementAt(j);
					varTypes.removeElementAt(j);
					varScopes.removeElementAt(j);
					values.removeValue(j);
					// remove for loop from stack
					stack.pop();
					// move to next statement
					pc++;
				}
			}
		}

		// if not currently outputting, just pop the stack and move on
		else if (stack.peek() instanceof String && !outputEnabled) {
			stack.pop();
			pc++;
		}

		else {
			throw new PrismException("Preprocessor stack error");
		}
	}

	private void interpretExpression(String s) throws PrismException
	{
		Expression expr;

		// if not currently outputting, just skip this
		if (!outputEnabled) {
			pc++;
			return;
		}

		// parse expression, do some checks
		expr = Prism.parseSingleExpressionString(s);
		expr = (Expression) expr.findAllVars(varNames, varTypes);
		expr.typeCheck();
		expr.semanticCheck();
		// add 
		output += "" + expr.evaluate(null, values);
		// move to next statement
		pc++;
	}

	public static void main(String[] args)
	{
		if (args.length < 1)
			return;
		Prism p = new Prism(new PrismFileLog("stdout"));
		try {
			Preprocessor pp = new Preprocessor(p, new File(args[0]));
			pp.setParameters(args);
			String s = pp.preprocess();
			if (s == null) {
				System.out.println("Error: No preprocessing information.");
			} else {
				System.out.print(s);
			}
		} catch (PrismException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
}

//------------------------------------------------------------------------------
