//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package jltl2dstar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.BitSet;

import acceptance.AcceptanceRabin;
import automata.DA;
import jltl2ba.SimpleLTL;

/**
 * Command-line interface to the jltl2dstar LTL->DRA translator.
 */
public class Jltl2dstarCmdLine {

	public int run(String formula, PrintStream output) throws Exception {
		SimpleLTL ltl = SimpleLTL.parseFormulaLBT(formula);
		ltl = ltl.simplify();

		DA<BitSet, AcceptanceRabin> dra = LTL2Rabin.ltl2rabin(ltl);
		dra.printHOA(output);
		//DA.printLtl2dstar(dra, output);
		return 0;
	}
	
	public static void main(String[] args) {
		int rv = 0;
		int outfile_pos = 1;
		BufferedReader r = null;
		try {
			String formula;
			if (args.length == 0) {
				usage("");
			}
			if (args[0].equals("--ltl")) {
				if (args.length <= 1)
					usage("Missing formula");
				formula = args[1];
				outfile_pos++;
			} else {
				if (args[0].equals("-")) {
					r = new BufferedReader(new InputStreamReader(System.in));
				} else {
					r = new BufferedReader(new FileReader(args[0]));
				}
			}

			if (outfile_pos >= args.length) {
				usage("Missing output file");
			}
			PrintStream output;
			String outfile = args[outfile_pos];
			if (outfile.equals("-")) {
				output = System.out;
			} else {
				output = new PrintStream(outfile);
			}

			formula = r.readLine();
			r.close();

			Jltl2dstarCmdLine jltl2dstar = new Jltl2dstarCmdLine();
			rv = jltl2dstar.run(formula, output);
			output.close();
		} catch (Exception e) {
			System.err.println(e.toString());
			rv = 1;
		}
		
		if (rv != 0) {
			System.exit(rv);
		}
	}

	private static void usage(String error)
	{
		if (!error.isEmpty()) {
			System.err.println("Error: " + error);
			System.err.println();
		}
		System.err.println("Usage:");
		System.err.println("  --ltl formula out-file");
		System.err.println(" or");
		System.err.println("  in-file out-file");
		System.err.println("\n  Filename of '-' stands for standard input/output");
		System.exit(1);
	}
}
