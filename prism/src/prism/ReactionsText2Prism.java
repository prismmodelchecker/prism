//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Class to convert a textual description of a set of reactions into PRISM code.
 */
public class ReactionsText2Prism extends Reactions2Prism
{
	/** Name of the original file */
	protected String parsedFileName;
	
	// Enums

	private enum SectionType {
		SPECIES, PARAMETERS, REACTIONS, RR, R
	};

	// Constructors

	public ReactionsText2Prism()
	{
		super();
	}

	public ReactionsText2Prism(PrismLog mainLog)
	{
		super(mainLog);
	}

	@Override
	public String getName()
	{
		return "reactions";
	}

	@Override
	public void load(File file) throws PrismException
	{
		// Store filename for later use before parsing
		parsedFileName = file.getPath();
		super.load(file);
	}

	@Override
	public void load(InputStream in) throws PrismException
	{
		BufferedReader buf;
		SectionType secType = null;
		String s, s2, ss[], ss2[];
		int i, lineNum = 0;
		Species species;
		Parameter parameter;
		Reaction reaction;
		String reactionId = null, reactionName = null;
		
		// Initialise storage
		speciesList = new ArrayList<Species>();
		parameterList = new ArrayList<Parameter>();
		reactionList = new ArrayList<Reaction>();

		try {
			// Open file for reading
			buf = new BufferedReader(new InputStreamReader(in));
			// Read remaining lines
			s = buf.readLine();
			lineNum++;
			while (s != null) {
				// Strip comments
				s = s.replaceFirst(" *#.*", "");
				// Skip blank lines
				s = s.trim();
				if (s.length() > 0) {

					// Switch mode on section header
					if (s.charAt(0) == '@') {
						s2 = s.substring(1);
						if (s2.equals("species")) {
							secType = SectionType.SPECIES;
						} else if (s2.equals("parameters")) {
							secType = SectionType.PARAMETERS;
						} else if (s2.equals("reactions")) {
							secType = SectionType.REACTIONS;
						} else if (s2.startsWith("rr=")) {
							secType = SectionType.RR;
							// Extract reaction id/name 
							s2 = s2.substring(3);
							i = s2.indexOf(' ');
							reactionId = s2.substring(0, i > 0 ? i : s2.length());
							reactionName = i > 0 ? s2.substring(i + 1).replaceAll("\"", "") : "";
						} else if (s2.startsWith("r=")) {
							secType = SectionType.R;
							// Extract reaction id/name 
							s2 = s2.substring(2);
							i = s2.indexOf(' ');
							reactionId = s2.substring(0, i > 0 ? i : s2.length());
							reactionName = i > 0 ? s2.substring(i + 1).replaceAll("\"", "") : "";
						} else {
							throw new PrismException("@" + s2 + " section not supported");
						}
					} else {
						switch (secType) {

						// Species list
						case SPECIES:
							ss = s.split("=");
							if (ss.length != 2)
								throw new PrismException("invalid species definition \"" + s + "\"");
							// Get id
							String speciesId = ss[0];
							// Get init amount/name
							s2 = ss[1].trim();
							i = s2.indexOf(' ');
							String sInit = s2.substring(0, i > 0 ? i : s2.length());
							String speciesName = i > 0 ? s2.substring(i + 1).replaceAll("\"", "") : null;
							int initialAmount = Integer.parseInt(sInit);
							species = new Species(speciesId, speciesName, initialAmount);
							speciesList.add(species);
							break;

						// Parameter list
						case PARAMETERS:
							ss = s.split("=");
							if (ss.length == 1) {
								// Get id (value undefined)
								String paramId = ss[0];
								parameter = new Parameter(paramId, null);
							} else if (ss.length == 2) {
								// Get id and value
								String paramId = ss[0];
								try {
									Double.parseDouble(ss[1]);
								} catch (NumberFormatException e) {
									throw new PrismException("invalid value \"" + ss[1] + "\" for parameter \"" + paramId + "\"");
								}
								parameter = new Parameter(paramId, ss[1]);
							} else {
								throw new PrismException("invalid parameter definition \"" + s + "\"");
							}
							parameterList.add(parameter);
							break;

						// Reaction
						case RR:
						case R:
							ss = s.split("->");
							if (ss.length != 2)
								throw new PrismException("invalid reaction definition \"" + s + "\"");
							// Create reaction object with earlier info
							reaction = new Reaction(reactionId, reactionName);
							// Get reactants
							ss2 = ss[0].trim().split("\\+");
							for (String reactant : ss2) {
								reaction.addReactant(reactant, 1);
							}
							// Get products
							ss2 = ss[1].trim().split("\\+");
							for (String product : ss2) {
								reaction.addProduct(product, 1);
							}
							// Next line
							s = buf.readLine();
							lineNum++;
							if (s == null)
								throw new PrismException("missing line in reaction definition");
							// Strip comments
							s = s.replaceFirst(" *#.*", "");
							s = s.trim();
							// Get kinetic law
							// Irreversible case
							if (secType == SectionType.R) {
								reaction.setReversible(false);
								reaction.setKineticLawString(s);
							}
							// Irreversible case
							else {
								reaction.setReversible(true);
								ss = s.split("-");
								if (ss.length != 2)
									throw new PrismException("invalid kinetic law \"" + s + "\" for reversible reaction");
								reaction.setKineticLawString(ss[0].trim());
								reaction.setKineticLawReverseString(ss[1].trim());
							}
							reactionList.add(reaction);
							break;

						// Anything else: skip
						default:
						}
					}
				}
				// read next line
				s = buf.readLine();
				lineNum++;
			}

			// close file
			buf.close();
		} catch (IOException e) {
			throw new PrismException("I/O error reading reactions: " + e.getMessage());
		} catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of reactions file");
		} catch (PrismException e) {
			throw new PrismException("Error detected (" + e.getMessage() + ") at line " + lineNum + " of reactions file");
		}
	}

	@Override
	public void translate(PrintStream out) throws PrismException
	{
		// Generate PRISM code
		prismCodeHeader = "// File generated by reactions-to-PRISM conversion\n";
		if (parsedFileName != null) {
			prismCodeHeader += "// Original file: " + parsedFileName + "\n\n";
		}
		convertToPRISMCode(out);
		// Reset filename storage
		parsedFileName = null;
	}
	
	/**
	 * Calling point for command-line script:
	 * e.g. java -cp classes prism.ReactionsText2Prism myfile.txt 100
	 * (100 denotes (integer) maximum for species population sizes, default is 100)
	 */
	public static void main(String args[])
	{
		PrismLog errLog = new PrismPrintStreamLog(System.err);
		try {
			if (args.length < 1) {
				System.err.println("Usage: java -cp classes prism.ReactionsText2Prism <file> [max_amount]");
				System.exit(1);
			}
			ReactionsText2Prism rt2prism = new ReactionsText2Prism(errLog);
			try {
				if (args.length > 1)
					rt2prism.setMaxAmount(Integer.parseInt(args[1]));
			} catch (NumberFormatException e) {
				throw new PrismException("Invalid max amount \"" + args[1] + "\"");
			}
			rt2prism.load(new File(args[0]));
			rt2prism.translate(System.out);
		} catch (PrismException e) {
			errLog.println("Error: " + e.getMessage() + ".");
		}
	}
}
