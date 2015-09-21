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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;

import org.w3c.dom.Element;

import parser.PrismParser;

/**
 * Base class for classes that build/store a set of biological reactions and then convert to PRISM.
 */
public abstract class Reactions2Prism extends PrismLanguageTranslator
{
	// Log for output of warnings, messages
	protected PrismLog mainLog = null;

	// Reactions set definition
	protected String compartmentName, speciesId, initialAmountString;
	protected double compartmentSize;
	protected ArrayList<Species> speciesList;
	protected ArrayList<Parameter> parameterList;
	protected ArrayList<Reaction> reactionList;

	// Config

	/** Maximum amount of each species (unless some initial amount is higher). */
	protected int maxAmount = 100;

	// Optional PRISM code header/footer
	protected String prismCodeHeader;
	protected String prismCodeFooter;

	// Constructors

	public Reactions2Prism()
	{
		this(new PrismFileLog("stdout"));
	}

	public Reactions2Prism(PrismLog mainLog)
	{
		this.mainLog = mainLog;
	}

	public void setMaxAmount(int maxAmount)
	{
		this.maxAmount = maxAmount;
	}

	/**
	 * Print the currently loaded reaction set model (for testing purposes).
	 */
	protected void printModel(PrintStream out)
	{
		int i, n;
		Reaction reaction;

		out.println(speciesList.size() + " species: " + speciesList);
		if (parameterList.size() > 0)
			out.println(parameterList.size() + " parameters: " + parameterList);
		n = reactionList.size();
		out.println(n + " reactions:");
		for (i = 0; i < n; i++) {
			reaction = reactionList.get(i);
			out.print(" * " + reaction);
		}
	}

	/**
	 * Process the currently loaded reaction set model, convert to PRISM code, export to an OutputStream. 
	 */
	protected void convertToPRISMCode(PrintStream out) throws PrismException
	{
		StringBuilder sb = convertToPRISMCode();
		try {
			OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(out), "utf-8");
			writer.append(sb);
			writer.flush();
		} catch (IOException e) {
			throw new PrismException("Error writing to output stream");
		}
	}

	/**
	 * Process the currently loaded reaction set model, convert to PRISM code, export as file. 
	 */
	protected void convertToPRISMCode(File file) throws PrismException
	{
		StringBuilder sb = convertToPRISMCode();
		//OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(outputStream), "utf-8");
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.append(sb);
			writer.close();
		} catch (IOException e) {
			throw new PrismException("Error writing to file \"" + file + "\"");
		}
	}

	/**
	 * Process the currently loaded reaction set model, convert to PRISM code, return as StringBuilder. 
	 */
	protected StringBuilder convertToPRISMCode() throws PrismException
	{
		processModel();
		return generatePRISMCode();
	}

	/**
	 * Do some processing of the reaction set model in preparation for conversion to PRISM code.
	 */
	private void processModel() throws PrismException
	{
		int j, k, m;
		String s, s2;
		HashSet<String> modulesNames;
		HashSet<String> prismIdents;

		// Check species ids are unique
		ArrayList<String> speciesIDs = new ArrayList<String>();
		for (Species species : speciesList) {
			if (speciesIDs.contains(species.id))
				throw new PrismException("Duplicate species id \"" + species.id + "\"");
			speciesIDs.add(species.id);
		}
		// Check parameter names are unique
		ArrayList<String> paramNames = new ArrayList<String>();
		for (Parameter parameter : parameterList) {
			if (paramNames.contains(parameter.name))
				throw new PrismException("Duplicate parameter name \"" + parameter.name + "\"");
			paramNames.add(parameter.name);
		}
		// Check reaction ids are unique
		ArrayList<String> reactionIDs = new ArrayList<String>();
		for (Reaction reaction : reactionList) {
			if (reactionIDs.contains(reaction.id))
				throw new PrismException("Duplicate reaction id \"" + reaction.id + "\"");
			reactionIDs.add(reaction.id);
		}
		
		// Look at initial amounts for all species
		// If any exceed MAX_AMOUNT, increase it accordingly
		for (Species species : speciesList) {
			if (species.init > maxAmount)
				maxAmount = (int) species.init;
		}

		// Generate unique and valid PRISM identifier (module and variable name) for each species
		modulesNames = new HashSet<String>();
		prismIdents = new HashSet<String>();
		for (Species species : speciesList) {
			s = species.id;
			s2 = convertToValidPrismIdent(s);
			if (!s.equals(s2))
				mainLog.printWarning("Converted species id \"" + s + "\" to \"" + s2 + "\" (invalid PRISM identifier)");
			if (!modulesNames.add(s2)) {
				j = 2;
				while (!modulesNames.add(s2 + "_" + j))
					j++;
				s2 = s2 + "_" + j;
				mainLog.printWarning("Converted species id \"" + s + "\" to \"" + s2 + "\" (duplicate PRISM identifiers)");
			}
			species.prismName = s2;
			prismIdents.add(s2);
		}

		// Generate unique and valid PRISM constant name for model parameter
		for (Parameter parameter : parameterList) {
			s = parameter.name;
			s2 = convertToValidPrismIdent(s);
			if (!s.equals(s2))
				mainLog.printWarning("Converted parameter id \"" + s + "\" to \"" + s2 + "\" (invalid PRISM identifier)");
			if (!modulesNames.add(s2)) {
				j = 2;
				while (!prismIdents.add(s2 + "_" + j))
					j++;
				s2 = s2 + "_" + j;
				mainLog.printWarning("Converted parameter id \"" + s + "\" to \"" + s2 + "\" (duplicate PRISM identifiers)");
			}
			parameter.prismName = s2;
			prismIdents.add(s2);
		}

		// Generate unique and valid PRISM constant name for each reaction parameter
		for (Reaction reaction : reactionList) {
			m = reaction.parameters.size();
			for (j = 0; j < m; j++) {
				s = reaction.parameters.get(j).name;
				s2 = convertToValidPrismIdent(s);
				if (!s.equals(s2))
					mainLog.printWarning("Converted parameter id \"" + s + "\" to \"" + s2 + "\" (invalid PRISM identifier)");
				if (!prismIdents.add(s2)) {
					k = 2;
					while (!prismIdents.add(s2 + "_" + k))
						k++;
					s2 = s2 + "_" + k;
					mainLog.printWarning("Converted parameter id \"" + s + "\" to \"" + s2 + "\" (duplicate PRISM identifiers)");
				}
				reaction.parameters.get(j).prismName = s2;
			}
		}
	}

	/**
	 * Generate PRISM code for the (already processed) reaction set model.
	 */
	private StringBuilder generatePRISMCode() throws PrismException
	{
		int i, i2, n, n2, before, after;
		Species species;
		Reaction reaction;
		Parameter parameter;
		String s2;
		ArrayList<String> renameFrom = new ArrayList<String>();
		ArrayList<String> renameTo = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();

		// Header
		if (prismCodeHeader != null)
			sb.append(prismCodeHeader);
		sb.append("ctmc\n");
		sb.append("\nconst int MAX_AMOUNT = " + maxAmount + ";\n");

		// If required, add a constant for compartment size
		if (compartmentName != null) {
			sb.append("\n// Compartment size\n");
			sb.append("const double " + compartmentName + " = " + compartmentSize + ";\n");
		}

		// Generate constant definition for each (model and reaction) parameter
		n = parameterList.size();
		if (n > 0)
			sb.append("\n// Model parameters\n");
		for (i = 0; i < n; i++) {
			parameter = parameterList.get(i);
			sb.append("const double " + parameter.prismName);
			if (parameter.value != null && parameter.value.length() > 0)
				sb.append(" = " + parameter.value);
			sb.append("; // " + parameter.name + "\n");
		}
		n = reactionList.size();
		for (i = 0; i < n; i++) {
			reaction = reactionList.get(i);
			n2 = reaction.parameters.size();
			if (n2 > 0)
				sb.append("\n// Parameters for reaction " + reaction.id + "\n");
			for (i2 = 0; i2 < n2; i2++) {
				parameter = reaction.parameters.get(i2);
				sb.append("const double " + parameter.prismName);
				if (parameter.value != null && parameter.value.length() > 0)
					sb.append(" = " + parameter.value);
				sb.append("; // " + parameter.name + "\n");
			}
		}

		// Generate module for each species (except those with boundaryCondition=true)
		n = speciesList.size();
		for (i = 0; i < n; i++) {
			species = speciesList.get(i);
			if (species.boundaryCondition)
				continue;
			sb.append("\n// Species " + species + "\n");
			sb.append("const int " + species.prismName + "_MAX = MAX_AMOUNT;\n");
			sb.append("module " + species.prismName + "\n");

			// Generate variable representing the amount of this species
			sb.append("\t\n\t" + species.prismName + " : [0.." + species.prismName + "_MAX]");
			sb.append(" init " + (int) species.init + "; // Initial amount " + (int) species.init + "\n\t\n");
			//			sb.append(" init " + (int)Math.round(scaleFactor*species.init) + "; // Initial amount " + species.init + "\n\t\n");

			// Generate a command for each reaction that involves this species
			n2 = reactionList.size();
			for (i2 = 0; i2 < n2; i2++) {
				reaction = reactionList.get(i2);
				if (reaction.isSpeciesInvolved(species.id)) {
					// Forwards
					sb.append("\t// " + reaction.id);
					if (reaction.name.length() > 0)
						sb.append(" (" + reaction.name + ")");
					sb.append("\n");
					sb.append("\t[" + reaction.id + "] ");
					before = reaction.before(species.id);
					after = reaction.after(species.id);
					if (before > 0)
						sb.append(species.prismName + " > " + (before - 1));
					if (after - before > 0) {
						if (before > 0)
							sb.append(" &");
						sb.append(" " + species.prismName + " <= " + species.prismName + "_MAX-" + (after - before));
					}
					sb.append(" -> (" + species.prismName + "'=" + species.prismName);
					if (after - before > 0)
						sb.append("+" + (after - before));
					if (after - before < 0)
						sb.append((after - before));
					sb.append(");\n");
					// Maybe also backwards
					if (reaction.reversible) {
						sb.append("\t// " + reaction.id);
						if (reaction.name.length() > 0)
							sb.append(" (" + reaction.name + ")");
						sb.append(" (reverse)\n");
						sb.append("\t[" + reaction.id + "_rev] ");
						before = reaction.after(species.id);
						after = reaction.before(species.id);
						if (before > 0)
							sb.append(species.prismName + " > " + (before - 1));
						if (after - before > 0) {
							if (before > 0)
								sb.append(" &");
							sb.append(" " + species.prismName + " <= " + species.prismName + "_MAX-" + (after - before));
						}
						sb.append(" -> (" + species.prismName + "'=" + species.prismName);
						if (after - before > 0)
							sb.append("+" + (after - before));
						if (after - before < 0)
							sb.append((after - before));
						sb.append(");\n");
					}
				}
			}

			// Generate the end of this module definition
			sb.append("\t\nendmodule\n");
		}

		// Generate dummy module to store reaction rates
		sb.append("\n// Reaction rates\nmodule reaction_rates\n\n");
		n = reactionList.size();
		for (i = 0; i < n; i++) {
			reaction = reactionList.get(i);
			// Build info about renames (to unique PRISM idents)
			renameFrom.clear();
			renameTo.clear();
			n2 = speciesList.size();
			for (i2 = 0; i2 < n2; i2++) {
				species = speciesList.get(i2);
				if (!species.id.equals(species.prismName)) {
					renameFrom.add(species.id);
					renameTo.add(species.prismName);
				}
			}
			n2 = reaction.parameters.size();
			for (i2 = 0; i2 < n2; i2++) {
				parameter = reaction.parameters.get(i2);
				if (!parameter.name.equals(parameter.prismName)) {
					renameFrom.add(parameter.name);
					renameTo.add(parameter.prismName);
				}
			}
			n2 = parameterList.size();
			for (i2 = 0; i2 < n2; i2++) {
				parameter = parameterList.get(i2);
				if (!parameter.name.equals(parameter.prismName)) {
					renameFrom.add(parameter.name);
					renameTo.add(parameter.prismName);
				}
			}
			// Generate code
			// Forwards
			sb.append("\t// " + reaction.id);
			if (reaction.name.length() > 0)
				sb.append(" (" + reaction.name + ")");
			sb.append(": " + reaction.reactionString());
			sb.append("\n");
			if (reaction.kineticLawString != null)
				s2 = reaction.kineticLawString;
			else
				s2 = MathML2Prism.convert(reaction.kineticLaw, renameFrom, renameTo);
			sb.append("\t[" + reaction.id + "] " + s2 + " > 0 -> " + s2 + " : true;\n");
			// Maybe also backwards
			if (reaction.reversible) {
				sb.append("\t// " + reaction.id);
				if (reaction.name.length() > 0)
					sb.append(" (" + reaction.name + ")");
				sb.append(": " + reaction.reactionString());
				sb.append(" (reverse)\n");
				if (reaction.kineticLawReverseString != null)
					s2 = reaction.kineticLawReverseString;
				else
					s2 = MathML2Prism.convert(reaction.kineticLawReverse, renameFrom, renameTo);
				sb.append("\t[" + reaction.id + "_rev] " + s2 + " > 0 -> " + s2 + " : true;\n");
			}
		}
		sb.append("\nendmodule\n");

		// Generate a reward structure for each species
		sb.append("\n// Reward structures (one per species)\n\n");
		n = speciesList.size();
		for (i = 0; i < n; i++) {
			species = speciesList.get(i);
			if (species.boundaryCondition)
				continue;
			sb.append("// Reward " + (i + 1) + ": " + species + "\nrewards \"" + species.prismName + "\" true : " + species.prismName + "; endrewards\n");
		}

		// Footer
		if (prismCodeFooter != null)
			sb.append(prismCodeFooter);

		return sb;
	}

	// Check whether a given string is a valid PRISM language identifier

	protected static boolean isValidPrismIdent(String s)
	{
		if (!s.matches("[_a-zA-Z_][_a-zA-Z0-9]*"))
			return false;
		if (PrismParser.isKeyword(s))
			return false;
		return true;
	}

	// Convert a string to a valid PRISM language identifier (by removing invalid characters)

	protected static String convertToValidPrismIdent(String s)
	{
		String s2;
		if (!s.matches("[_a-zA-Z_][_a-zA-Z0-9]*"))
			s2 = s.replaceAll("[^_a-zA-Z0-9]", "");
		else
			s2 = s;
		if (PrismParser.isKeyword(s2))
			s2 = "_" + s2;
		return s2;
	}

	// Classes to store info about a set of reactions

	class Species
	{
		public String id;
		public String name;
		public double init;
		public String prismName;
		public boolean boundaryCondition;

		public Species(String id, String name, double init)
		{
			this.id = id;
			this.name = name;
			this.init = init;
			this.prismName = null;
			this.boundaryCondition = false;
		}

		public String toString()
		{
			return id + (name != null && name.length() > 0 ? (" (" + name + ")") : "");
		}
	}

	class Parameter
	{
		public String name;
		public String value;
		public String prismName;

		public Parameter(String name, String value)
		{
			this.name = name;
			this.value = value;
			this.prismName = null;
		}

		public String toString()
		{
			return name + "=" + value;
		}
	}

	class Reaction
	{
		public String id;
		public String name;
		public ArrayList<String> reactants;
		public ArrayList<Integer> reactantStoichs;
		public ArrayList<String> products;
		public ArrayList<Integer> productStoichs;
		public boolean reversible;
		public Element kineticLaw;
		public String kineticLawString;
		public Element kineticLawReverse;
		public String kineticLawReverseString;
		public ArrayList<Parameter> parameters;

		public Reaction(String id, String name)
		{
			this.id = id;
			this.name = name;
			reactants = new ArrayList<String>();
			reactantStoichs = new ArrayList<Integer>();
			products = new ArrayList<String>();
			productStoichs = new ArrayList<Integer>();
			reversible = false;
			kineticLaw = null;
			kineticLawString = null;
			kineticLawReverse = null;
			kineticLawReverseString = null;
			parameters = new ArrayList<Parameter>();
		}

		public void addReactant(String reactant)
		{
			addReactant(reactant, 1);
		}

		public void addReactant(String reactant, int stoich)
		{
			int i = reactants.indexOf(reactant);
			if (i == -1) {
				reactants.add(reactant);
				reactantStoichs.add(stoich);
			} else {
				reactantStoichs.set(i, reactantStoichs.get(i) + stoich);
			}
		}

		public void addProduct(String product)
		{
			addProduct(product, 1);
		}

		public void addProduct(String product, int stoich)
		{
			int i = products.indexOf(product);
			if (i == -1) {
				products.add(product);
				productStoichs.add(stoich);
			} else {
				productStoichs.set(i, productStoichs.get(i) + stoich);
			}
		}

		public void setReversible(boolean reversible)
		{
			this.reversible = reversible;
		}

		public void setKineticLaw(Element kineticLaw)
		{
			this.kineticLaw = kineticLaw;
			this.kineticLawString = null;
		}

		public void setKineticLawString(String kineticLawString)
		{
			this.kineticLawString = kineticLawString;
			this.kineticLaw = null;
		}

		public void setKineticLawReverse(Element kineticLawReverse)
		{
			this.kineticLawReverse = kineticLawReverse;
			this.kineticLawReverseString = null;
		}

		public void setKineticLawReverseString(String kineticLawReverseString)
		{
			this.kineticLawReverseString = kineticLawReverseString;
			this.kineticLawReverse = null;
		}

		public void addParameter(String name, String value)
		{
			parameters.add(new Parameter(name, value));
		}

		public boolean isSpeciesInvolved(String species)
		{
			return reactants.contains(species) || products.contains(species);
		}

		public int before(String species)
		{
			int i = reactants.indexOf(species);
			if (i == -1)
				return 0;
			return reactantStoichs.get(i);
		}

		public int after(String species)
		{
			int i = products.indexOf(species);
			if (i == -1)
				return 0;
			return productStoichs.get(i);
		}

		public String reactionString()
		{
			String s = PrismUtils.joinString(reactants, "+");
			s += " -> " + PrismUtils.joinString(products, "+");
			return s;
		}

		public String toString()
		{
			String s = "";
			s += id;
			if (name.length() > 0)
				s += " (" + name + ")";
			s += ":\n";
			s += "    Reactants: " + reactants + "\n";
			s += "    Reactants stoichiometry: " + productStoichs + "\n";
			s += "    Products: " + products + "\n";
			s += "    Products stoichiometry: " + productStoichs + "\n";
			s += "    Kinetic law: " + ((kineticLawString != null) ? kineticLawString : kineticLaw) + "\n";
			if (reversible) {
				s += "    Reverse kinetic law: " + ((kineticLawReverseString != null) ? kineticLawReverseString : kineticLawReverse) + "\n";
			}
			s += "    Parameters: " + parameters + "\n";
			return s;
		}
	}
}
