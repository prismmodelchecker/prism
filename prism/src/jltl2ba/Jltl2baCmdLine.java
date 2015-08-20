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

package jltl2ba;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintStream;

import jltl2dstar.NBA;

/**
 * Command-line interface to the jltl2ba LTL->NBA translator.
 */
public class Jltl2baCmdLine {

	public int run(String formula, PrintStream output) throws Exception {
		formula = formula.trim();
		SimpleLTL ltl = SimpleLTL.parseFormulaLBT(formula);
		ltl = ltl.simplify();
		
		NBA nba = ltl.toNBA();
		// nba.print_lbtt(output);
		nba.print_hoa(output);
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

			Jltl2baCmdLine jltl2ba = new Jltl2baCmdLine();
			rv = jltl2ba.run(formula, output);
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
