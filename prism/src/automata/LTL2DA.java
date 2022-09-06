//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package automata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import jhoafparser.consumer.HOAIntermediateStoreAndManipulate;
import jhoafparser.parser.HOAFParser;
import jhoafparser.parser.generated.ParseException;
import jhoafparser.transformations.ToStateAcceptance;
import jltl2ba.APSet;
import jltl2ba.SimpleLTL;
import jltl2ba.LTLFragments;
import jltl2dstar.LTL2Rabin;
import parser.Values;
import parser.ast.Expression;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceRabin;
import acceptance.AcceptanceReach;
import acceptance.AcceptanceType;

/**
 * Infrastructure for constructing deterministic automata for LTL formulas.
 */
public class LTL2DA extends PrismComponent
{

	public LTL2DA(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	/**
	 * Convert an LTL formula into a deterministic Rabin automaton.
	 * The LTL formula is represented as a PRISM Expression,
	 * in which atomic propositions are represented by ExpressionLabel objects.
	 * @param ltl the formula
	 * @param constantValues the values of constants, may be {@code null}
	 */
	@SuppressWarnings("unchecked")
	public DA<BitSet, AcceptanceRabin> convertLTLFormulaToDRA(Expression ltl, Values constantValues) throws PrismException
	{
		return (DA<BitSet, AcceptanceRabin>) convertLTLFormulaToDA(ltl, constantValues, AcceptanceType.RABIN);
	}

	/**
	 * Convert an LTL formula into a deterministic automaton.
	 * The LTL formula is represented as a PRISM Expression,
	 * in which atomic propositions are represented by ExpressionLabel objects.
	 * @param ltl the formula
	 * @param constants the values of constants, may be {@code null}
	 * @param allowedAcceptance the AcceptanceTypes that are allowed to be returned
	 */
	public DA<BitSet, ? extends AcceptanceOmega> convertLTLFormulaToDA(Expression ltl, Values constants, AcceptanceType... allowedAcceptance)
			throws PrismException
	{
		DA<BitSet, ? extends AcceptanceOmega> result = null;

		boolean useExternal = useExternal();
		boolean containsTemporalBounds = Expression.containsTemporalTimeBounds(ltl);
		if (containsTemporalBounds) {
			useExternal = false;
		}

		if (!useExternal) {
			try {
				// checking the library first
				result = LTL2RabinLibrary.getDAforLTL(ltl, constants, allowedAcceptance);
				if (result != null) {
					getLog().println("Taking "+result.getAutomataType()+" from library...");
				}
			} catch (Exception e) {
				if (containsTemporalBounds) {
					// there is (currently) no other way to translate LTL with temporal bounds,
					// so treat an exception as a "real" one
					throw e;
				} else {
					// there is the possibility that we might be able to construct
					// an automaton below, just issue a warning
					getLog().println("Warning: Exception during attempt to construct DRA using the LTL2RabinLibrary:");
					getLog().println(" " + e.getMessage());
				}
			}
		}

		if (result == null) {
			if (!containsTemporalBounds) {
				if (useExternal) {
					result = convertLTLFormulaToDAWithExternalTool(ltl, constants, allowedAcceptance);
				} else {
					SimpleLTL simpleLTL = ltl.convertForJltl2ba();

					// don't use LTL2WDBA translation yet
					boolean allowLTL2WDBA = false;
					if (allowLTL2WDBA) {
						LTLFragments fragments = LTLFragments.analyse(simpleLTL);
						mainLog.println(fragments);

						if (fragments.isSyntacticGuarantee() && AcceptanceType.contains(allowedAcceptance, AcceptanceType.REACH)) {
							// a co-safety property
							mainLog.println("Generating DFA for co-safety property...");
							LTL2WDBA ltl2wdba = new LTL2WDBA(this);
							result = ltl2wdba.cosafeltl2dfa(simpleLTL);
						} else if (allowLTL2WDBA && fragments.isSyntacticObligation() && AcceptanceType.contains(allowedAcceptance, AcceptanceType.BUCHI)) {
							// an obligation property
							mainLog.println("Generating DBA for obligation property...");
							LTL2WDBA ltl2wdba = new LTL2WDBA(this);
							result = ltl2wdba.obligation2wdba(simpleLTL);
						}
					}
					if (result == null) {
						// use jltl2dstar LTL2DA
						result = LTL2Rabin.ltl2da(simpleLTL, allowedAcceptance);
					}
				}
			} else {
				throw new PrismNotSupportedException("Could not convert LTL formula to deterministic automaton, formula had time-bounds");
			}
		}

		if (result == null) {
			throw new PrismNotSupportedException("Could not convert LTL formula to deterministic automaton");
		}

		if (!getSettings().getBoolean(PrismSettings.PRISM_NO_DA_SIMPLIFY)) {
			result = DASimplifyAcceptance.simplifyAcceptance(this, result, allowedAcceptance);
		}

		return result;
	}

	public DA<BitSet, ? extends AcceptanceOmega> convertLTLFormulaToDAWithExternalTool(Expression ltl, Values constants, AcceptanceType... allowedAcceptance)
			throws PrismException
	{
		String ltl2daTool = getSettings().getString(PrismSettings.PRISM_LTL2DA_TOOL);

		SimpleLTL ltlFormula = ltl.convertForJltl2ba();

		// switch from the L0, L1, ... APs of PRISM to the
		// safer p0, p1, ... APs for the external tool
		SimpleLTL ltlFormulaSafeAP = ltlFormula.clone();
		ltlFormulaSafeAP.renameAP("L", "p");

		DA<BitSet, ? extends AcceptanceOmega> result = null;

		try {

			String syntax = getSettings().getString(PrismSettings.PRISM_LTL2DA_SYNTAX);
			if (syntax == null || syntax.isEmpty()) {
				throw new PrismException("No LTL syntax option provided");
			}
			String ltlOutput;
			switch (syntax) {
			case "LBT":
				ltlOutput = ltlFormulaSafeAP.toStringLBT();
				break;
			case "Spin":
				ltlOutput = ltlFormulaSafeAP.toStringSpin();
				break;
			case "Spot":
				ltlOutput = ltlFormulaSafeAP.toStringSpot();
				break;
			case "Rabinizer":
				ltlFormulaSafeAP = ltlFormulaSafeAP.toBasicOperators();
				ltlOutput = ltlFormulaSafeAP.toStringSpot();
				break;
			default:
				throw new PrismException("Unknown LTL syntax option \"" + syntax + "\"");
			}

			File ltl_file = File.createTempFile("prism-ltl-external-", ".ltl", null);
			File da_file = File.createTempFile("prism-ltl-external-", ".hoa", null);
			File tool_output = File.createTempFile("prism-ltl-external-", ".output", null);

			FileWriter ltlWriter = new FileWriter(ltl_file);
			ltlWriter.write(ltlOutput);
			ltlWriter.close();

			List<String> arguments = new ArrayList<String>();
			arguments.add(ltl2daTool);

			getLog().print("Calling external LTL->DA tool: ");
			for (String s : arguments) {
				getLog().print(" " + s);
			}
			getLog().println();

			getLog().print("LTL formula (in " + syntax + " syntax):  ");
			getLog().println(ltlOutput);
			getLog().println();

			arguments.add(ltl_file.getAbsolutePath());
			arguments.add(da_file.getAbsolutePath());

			ProcessBuilder builder = new ProcessBuilder(arguments);
			builder.redirectOutput(tool_output);
			builder.redirectErrorStream(true);

			// if we are running under the Nailgun environment, setup the
			// environment to include the environment variables of the Nailgun client
			prism.PrismNG.setupChildProcessEnvironment(builder);

			Process p = builder.start();
			p.getInputStream().close();

			int rv;
			while (true) {
				try {
					rv = p.waitFor();
					break;
				} catch (InterruptedException e) {
				}
			}
			if (rv != 0) {
				throw new PrismException("Call to external LTL->DA tool failed, return value = " + rv + ".\n"
						+ "To investigate, please consult the following files:" + "\n LTL formula:                     " + ltl_file.getAbsolutePath()
						+ "\n Automaton output:                " + da_file.getAbsolutePath() + "\n Tool output (stdout and stderr): "
						+ tool_output.getAbsolutePath() + "\n");
			}

			tool_output.delete();

			try {
				try {
					HOAF2DA consumerDA = new HOAF2DA();

					InputStream input = new FileInputStream(da_file);
					HOAFParser.parseHOA(input, consumerDA);
					result = consumerDA.getDA();
				} catch (HOAF2DA.TransitionBasedAcceptanceException e) {
					// try again, this time transforming to state acceptance
					getLog().println("Automaton with transition-based acceptance, automatically converting to state-based acceptance...");
					HOAF2DA consumerDA = new HOAF2DA();
					HOAIntermediateStoreAndManipulate consumerTransform = new HOAIntermediateStoreAndManipulate(consumerDA, new ToStateAcceptance());

					InputStream input = new FileInputStream(da_file);
					HOAFParser.parseHOA(input, consumerTransform);
					result = consumerDA.getDA();
				}

				if (result == null) {
					throw new PrismException("Could not construct DA");
				}
				checkAPs(ltlFormulaSafeAP, result.getAPList());

				// rename back from safe APs, i.e., p0, p1, ... to L0, L1, ...
				List<String> automatonAPList = result.getAPList();
				for (int i = 0; i < automatonAPList.size(); i++) {
					if (automatonAPList.get(i).startsWith("p")) {
						String renamed = "L" + automatonAPList.get(i).substring("p".length());
						automatonAPList.set(i, renamed);
					}
				}
			} catch (ParseException e) {
				throw new PrismException("Parse error: " + e.getMessage() + ".\n" + "To investigate, please consult the following files:\n"
						+ " LTL formula:        " + ltl_file.getAbsolutePath() + "\n Automaton output: " + da_file.getAbsolutePath() + "\n");
			} catch (PrismException e) {
				throw new PrismException(e.getMessage() + ".\n" + "To investigate, please consult the following files:" + "\n LTL formula: "
						+ ltl_file.getAbsolutePath() + "\n Automaton output: " + da_file.getAbsolutePath() + "\n");
			}

			da_file.delete();
			ltl_file.delete();
		} catch (IOException e) {
			throw new PrismException(e.getMessage());
		}

		if (!getSettings().getBoolean(PrismSettings.PRISM_NO_DA_SIMPLIFY)) {
			result = DASimplifyAcceptance.simplifyAcceptance(this, result, allowedAcceptance);
		}

		AcceptanceOmega acceptance = result.getAcceptance();
		if (AcceptanceType.contains(allowedAcceptance, acceptance.getType())) {
			return result;
		} else if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.GENERIC)) {
			// The specific acceptance type is not allowed, but GENERIC is allowed
			//   -> transform to generic acceptance and switch acceptance condition
			DA.switchAcceptance(result, acceptance.toAcceptanceGeneric());
			return result;
		} else {
			throw new PrismException("The external LTL->DA tool returned an automaton with " + acceptance.getType()
					+ " acceptance, which is not yet supported for model checking this model / property");
		}
	}

	/** Check whether we should use an external LTL->DA tool */
	private boolean useExternal()
	{
		String ltl2da_tool = getSettings().getString(PrismSettings.PRISM_LTL2DA_TOOL);
		if (ltl2da_tool != null && !ltl2da_tool.isEmpty()) {
			return true;
		}
		return false;
	}

	/** Check the atomic propositions of the (externally generated) automaton */
	private void checkAPs(SimpleLTL ltl, List<String> automatonAPs) throws PrismException
	{
		APSet ltlAPs = ltl.getAPs();
		for (String ap : automatonAPs) {
			if (!ltlAPs.hasAP(ap)) {
				throw new PrismException("Generated automaton has extra atomic proposition \"" + ap + "\"");
			}
		}
		// It's fine for the automaton to not have APs that occur in the formula, e.g., for
		// p0 | !p0, the external tool could simplify to 'true' and omit all APs
	}

	/**
	 * Simple test method: convert LTL formula (in LBT format) to HOA/Dot/txt
	 */
	public static void main(String args[])
	{
		try {
			// Usage:
			// * ... 'X p1' 
			// * ... 'X p1' da.hoa 
			// * ... 'X p1' da.hoa hoa 
			// * ... 'X p1' da.dot dot 
			// * ... 'X p1' - hoa 
			// * ... 'X p1' - txt 

			// Convert to Expression (from PRISM format)
			/*String pltl = "P=?[" + ltl + "]";
			PropertiesFile pf = Prism.getPrismParser().parsePropertiesFile(new ModulesFile(), new ByteArrayInputStream(pltl.getBytes()));
			Prism.releasePrismParser();
			Expression expr = pf.getProperty(0);
			expr = ((ExpressionProb) expr).getExpression();
			System.out.println("LTL: " + expr);*/

			// Convert to Expression (from LBT format)
			// String ltl = args[0];
			SimpleLTL sltl = SimpleLTL.parseFormulaLBT(args[0]);
			Expression expr = Expression.createFromJltl2ba(sltl);
			// System.out.println("LBT: " + ltl);
			// System.out.println("LTL: " + expr);

			// Build/export DA
			LTL2DA ltl2da = new LTL2DA(new PrismComponent());
			DA<BitSet, ? extends AcceptanceOmega> da = ltl2da.convertLTLFormulaToDA(expr, null, AcceptanceType.RABIN, AcceptanceType.REACH);
			PrintStream out = (args.length < 2 || "-".equals(args[1])) ? System.out : new PrintStream(args[1]);
			String format = (args.length < 3) ? "hoa" : args[2];
			da.print(out, format);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.print("Error: " + e);
		}
	}
}
