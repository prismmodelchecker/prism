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

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import parser.PrismParser;
import prism.Prism;

public class SBML2Prism implements EntityResolver
{
	private PrismLog mainLog = null;
	private static PrismParser prismParser;
	private String compartmentName, speciesId, initialAmountString;
	private double compartmentSize;
	private ArrayList<Species> speciesList;
	private ArrayList<Parameter> parameterList;
	private ArrayList<Reaction> reactionList;
	private int maxAmount;
	
	// Calling point: e.g. java -cp classes prism.SBML2Prism myfile.sbml 100
	//
	// (100 denotes (integer) maximum for species population sizes, default is 100)
	// (also used to compute amounts from (real-valued) concentrations)
	
	public static void main(String args[])
	{
		PrismLog errLog = new PrismPrintStreamLog(System.err);
		try {
			if (args.length < 1) {
				System.err.println("Usage: java -cp classes prism.SBML2Prism <sbml_file> [max_amount]");
				System.exit(1);
			}
			SBML2Prism sbml2prism = new SBML2Prism(errLog);
			sbml2prism.load(new File(args[0]), (args.length>1)?args[1]:"100");
		}
		catch (PrismException e) {
			errLog.println("Error: "+e.getMessage()+".");
		}
	}
	
	public SBML2Prism()
	{
		this(new PrismFileLog("stdout"));
	}
	
	public SBML2Prism(PrismLog mainLog)
	{
		this.mainLog = mainLog;
	}
	
	// Main method: load SBML file, process and sent resulting PRISM file to stdout
	
	public void load(File f, String maxAmount) throws PrismException
	{
		try {
			// obtain exclusive acces to the prism parser
			// (don't forget to release it afterwards)
			prismParser = Prism.getPrismParser();

			// translate
			try {
				try { this.maxAmount = Integer.parseInt(maxAmount); }
				catch (NumberFormatException e) { throw new PrismException("Invalid max amount \""+maxAmount+"\""); }
				Document doc = parseSBML(f);
				extractModelFromSBML(doc);
				checkSBMLVersion(doc);
				//printModel(System.err);
				processModel();
				printPRISMModel(f);
			}
			finally {
				// release prism parser
				Prism.releasePrismParser();
			}
		}
		catch (InterruptedException e) {
			throw new PrismException("Concurrency error in parser");
		}
	}
	
	// Parse the SBML file and return an XML Document object
	
	private Document parseSBML(File f) throws PrismException
	{
		DocumentBuilderFactory factory;
		DocumentBuilder builder;
		Document doc = null;
		
		// Create XML parser
		factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringElementContentWhitespace(true);
		try {
			builder = factory.newDocumentBuilder();
			builder.setEntityResolver(this);
			builder.setErrorHandler(new ErrorHandler() {
				public void fatalError(SAXParseException e) throws SAXException { throw e; }
				public void error(SAXParseException e) throws SAXException { throw e; }
				public void warning(SAXParseException e) {}
			});
		}
		catch (ParserConfigurationException e) {
			throw new PrismException("Couldn't create XML parser");
		}
		
		// Parse
		try {
			doc = builder.parse(f);
		}
		catch (IOException e) {
			throw new PrismException("Couldn't load file \""+f.getPath()+"\": " + e.getMessage());
		}
		catch (SAXException e) {
			throw new PrismException("Invalid XML file:\n" + e.getMessage());
		}
		
		return doc;
	}

	// Function used by parseSBML() above to find the SBML DTD
	// (this currently unused since we do not validate the SBML file when reading)
	// (and since the DTD specified in the SBML files is not local)
	// (if validation is enabled, put the DTD file "sbml.dtd" in PRISM's "dtds" directory)
	
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
	{
		InputSource inputSource = null;
		
		// override the resolve method for the dtd
		if (systemId.endsWith("dtd"))
		{
			// get appropriate dtd from classpath
			InputStream inputStream = this.getClass().getResourceAsStream("dtds/sbml.dtd");
			if (inputStream != null) inputSource = new InputSource(inputStream);
		}
		
		return inputSource;
	}

	// Check that we can handle whatever level/version this file is

	private void checkSBMLVersion(Document doc) throws PrismException
	{
		String level, s;
		
		level = doc.getDocumentElement().getAttribute("level");
		if (!("2".equals(level))) {
			s = "The translator only handles Level 2 SBML files - this is a Level "+level+" file";
			throw new PrismException(s);
		}
	}
	
	// Extract the information about the model from the parsed SBML file
	
	private void extractModelFromSBML(Document doc) throws PrismException
	{
		Element e, e_model, e_list;
		Element e_comp, e_species, e_parameter, e_reaction, e_kinetics, e_mathml;
		NodeList nodes, nodes2;
		Node node = null;
		Species species;
		Parameter parameter;
		Reaction reaction;
		int i, j, k, n, m;
		double d;
		String s, s2;
		boolean found;
		
		// Get "model" element of SBML file
		nodes = doc.getDocumentElement().getElementsByTagName("model");
		e_model = (Element)nodes.item(0);
		
		// Process compartment info (if present)
		// Just need to extract the size in case used
		e_list = (Element)e_model.getElementsByTagName("listOfCompartments").item(0);
		nodes = e_list.getElementsByTagName("compartment");
		n = nodes.getLength();
		// Make sure there is at most one
		if (n > 1)
			throw new PrismException("Only one compartment is permitted");
		// And if present, store size info
		compartmentName = null;
		if (n == 1) {
			e_comp = (Element)nodes.item(0);
			if (!"".equals(e_comp.getAttribute("size"))) {
				compartmentName = e_comp.getAttribute("id");
				if ("".equals(compartmentName))
					throw new PrismException("Missing compartment name");
				compartmentSize = Double.parseDouble(e_comp.getAttribute("size"));
			}
		}
		
		// Process list of species
		speciesList = new ArrayList<Species>();
		e_list = (Element)e_model.getElementsByTagName("listOfSpecies").item(0);
		nodes = e_list.getElementsByTagName("species");
		n = nodes.getLength();
		for (i = 0; i < n; i++) {
			e_species = (Element)nodes.item(i);
			speciesId = e_species.getAttribute("id");
			initialAmountString = e_species.getAttribute("initialAmount");
			if ("".equals(initialAmountString))
				throw new PrismException("Missing initial amount for species " + speciesId);
			try {
				d = Double.parseDouble(initialAmountString);
			}
			catch (NumberFormatException nfe) {
				String msg = "Badly formatted initialAmount \"" + initialAmountString + "\" for species " + speciesId;
				throw new PrismException(msg);
			}
			species = new Species(speciesId, e_species.getAttribute("name"), d);
			s = e_species.getAttribute("boundaryCondition");
			if (s.equals("true")) species.boundaryCondition = true;
			speciesList.add(species);
		}
		
		// Process list of parameters (if present)
		parameterList = new ArrayList<Parameter>();
		// Look at direct children only (there might be listOfParameters nodes lower in the tree)
		nodes = e_model.getChildNodes();
		n = nodes.getLength();
		found = false;
		for (i = 0; i < n; i++) {
			node = nodes.item(i);
			if ("listOfParameters".equals(node.getNodeName())) { found = true; break; }
		}
		if (found) {
			e_list = (Element)node;
			nodes = e_list.getElementsByTagName("parameter");
			n = nodes.getLength();
			for (i = 0; i < n; i++) {
				e_parameter = (Element)nodes.item(i);
				parameter = new Parameter(e_parameter.getAttribute("id"), e_parameter.getAttribute("value"));
				parameterList.add(parameter);
			}
		}
		
		// Process list of reactions
		reactionList = new ArrayList<Reaction>();
		e_list = (Element)e_model.getElementsByTagName("listOfReactions").item(0);
		nodes = e_list.getElementsByTagName("reaction");
		n = nodes.getLength();
		for (i = 0; i < n; i++) {
			
			// Process a single reaction...
			e_reaction = (Element)nodes.item(i);
			reaction = new Reaction(e_reaction.getAttribute("id"), e_reaction.getAttribute("name"));
			
			// Reactant list
			e_list = (Element)e_reaction.getElementsByTagName("listOfReactants").item(0);
			if (e_list != null) {
				nodes2 = e_list.getElementsByTagName("speciesReference");
				m = nodes2.getLength();
				for (j = 0; j < m; j++) {
					e = (Element)nodes2.item(j);
					// Get species name of product
					s = e.getAttribute("species");
					// Get stoichiometry if present
					s2 = e.getAttribute("stoichiometry");
					k = 1;
					if (s2.length() > 0) try { k = Integer.parseInt(s2); }
					catch (NumberFormatException ex) { throw new PrismException("Invalid stoichiometry value \""+s2+"\""); }
					// Add reactant to reaction
					reaction.addReactant(s, k);
				}
			}
			
			// Product list
			e_list = (Element)e_reaction.getElementsByTagName("listOfProducts").item(0);
			if (e_list != null) {
				nodes2 = e_list.getElementsByTagName("speciesReference");
				m = nodes2.getLength();
				for (j = 0; j < m; j++) {
					e = (Element)nodes2.item(j);
					// Get species name of product
					s = e.getAttribute("species");
					// Get stoichiometry if present
					s2 = e.getAttribute("stoichiometry");
					k = 1;
					if (s2.length() > 0) try { k = Integer.parseInt(s2); }
					catch (NumberFormatException ex) { throw new PrismException("Invalid stoichiometry value \""+s2+"\""); }
					// Add product to reaction
					reaction.addProduct(s, k);
				}
			}
			
			// Kinetic law
			e_kinetics = (Element)e_reaction.getElementsByTagName("kineticLaw").item(0);
			e_mathml = (Element)e_kinetics.getElementsByTagName("math").item(0);
			reaction.setKineticLaw(e_mathml);
			e_list = (Element)e_kinetics.getElementsByTagName("listOfParameters").item(0);
			if (e_list != null) {
				nodes2 = e_list.getElementsByTagName("parameter");
				m = nodes2.getLength();
				for (j = 0; j < m; j++) {
					e = (Element)nodes2.item(j);
					reaction.addParameter(e.getAttribute("id"), e.getAttribute("value"));
				}
			}
			
			// Add reaction to list
			reactionList.add(reaction);
		}
	}
	
	// Print model
	
	private void printModel(PrintStream out)
	{
		int i, n;
		Reaction reaction;
		
		out.println(speciesList.size() + " species: "+speciesList);
		if (parameterList.size() > 0) out.println(parameterList.size() + " parameters: "+parameterList);
		n = reactionList.size();
		out.println(n + " reactions:");
		for (i = 0; i < n; i++) {
			reaction = reactionList.get(i);
			out.print(" * "+reaction);
		}
	}
	
	// Process model
	
	private void processModel()
	{
		int i, j, k, n, m;
		String s, s2;
		Species species;
		Reaction reaction;
		Parameter parameter;
		HashSet<String> modulesNames;
		HashSet<String> prismIdents;

		// Look at initial amounts for all species
		// If any exceed MAX_AMOUNT, increase it accordingly
		n = speciesList.size();
		for (i = 0; i < n; i++) {
			species = speciesList.get(i);
			if (species.init > maxAmount) maxAmount = (int)species.init;
		}
		
		// Generate unique and valid PRISM identifier (module and variable name) for each species
		modulesNames = new HashSet<String>();
		prismIdents = new HashSet<String>();
		n = speciesList.size();
		for (i = 0; i < n; i++) {
			species = speciesList.get(i);
			s = species.id;
			s2 = convertToValidPrismIdent(s);
			if (!s.equals(s2)) mainLog.printWarning("Converted species id \""+s+"\" to \""+s2+"\" (invalid PRISM identifier)");
			if (!modulesNames.add(s2)) {
				j = 2;
				while (!modulesNames.add(s2+"_"+j)) j++;
				s2 = s2+"_"+j;
				mainLog.printWarning("Converted species id \""+s+"\" to \""+s2+"\" (duplicate PRISM identifiers)");
			}
			species.prismName = s2;
			prismIdents.add(s2);
		}
		
		// Generate unique and valid PRISM constant name for model parameter
		n = parameterList.size();
		for (i = 0; i < n; i++) {
			parameter = parameterList.get(i);
			s = parameter.name;
			s2 = convertToValidPrismIdent(s);
			if (!s.equals(s2)) mainLog.printWarning("Converted parameter id \""+s+"\" to \""+s2+"\" (invalid PRISM identifier)");
			if (!modulesNames.add(s2)) {
				j = 2;
				while (!prismIdents.add(s2+"_"+j)) j++;
				s2 = s2+"_"+j;
				mainLog.printWarning("Converted parameter id \""+s+"\" to \""+s2+"\" (duplicate PRISM identifiers)");
			}
			parameter.prismName = s2;
			prismIdents.add(s2);
		}
		
		// Generate unique and valid PRISM constant name for each reaction parameter
		n = reactionList.size();
		for (i = 0; i < n; i++) {
			reaction = reactionList.get(i);
			m = reaction.parameters.size();
			for (j = 0; j < m; j++) {
				s = reaction.parameters.get(j).name;
				s2 = convertToValidPrismIdent(s);
				if (!s.equals(s2)) mainLog.printWarning("Converted parameter id \""+s+"\" to \""+s2+"\" (invalid PRISM identifier)");
				if (!prismIdents.add(s2)) {
					k = 2;
					while (!prismIdents.add(s2+"_"+k)) k++;
					s2 = s2+"_"+k;
					mainLog.printWarning("Converted parameter id \""+s+"\" to \""+s2+"\" (duplicate PRISM identifiers)");
				}
				reaction.parameters.get(j).prismName = s2;
			}
		}
	}
	
	// Generate and print PRISM code
	
	private void printPRISMModel(File f) throws PrismException
	{
		int i, i2, n, n2, before, after;
		Species species;
		Reaction reaction;
		Parameter parameter;
		String s = "", s2;
		ArrayList<String> renameFrom = new ArrayList<String>();
		ArrayList<String> renameTo = new ArrayList<String>();
		
		// Header
		s += "// File generated by automatic SBML-to-PRISM conversion\n";
		s += "// Original SBML file: " + f.getPath() + "\n\n"; 
		s += "ctmc\n";
		s += "\nconst int MAX_AMOUNT = " + maxAmount + ";\n";
		
		// If required, add a constant for compartment size
		if (compartmentName != null) {
			s += "\n// Compartment size\n";
			s += "const double " + compartmentName + " = " + compartmentSize + ";\n";
		}
		
		// Generate constant definition for each (model and reaction) parameter
		n = parameterList.size();
		if (n > 0) s += "\n// Model parameters\n";
		for (i = 0; i < n; i++) {
			parameter = parameterList.get(i);
			s += "const double " + parameter.prismName;
			if (parameter.value != null && parameter.value.length()>0) s += " = " + parameter.value;
			s += "; // "+parameter.name+"\n";
		}
		n = reactionList.size();
		for (i = 0; i < n; i++) {
			reaction = reactionList.get(i);
			n2 = reaction.parameters.size();
			if (n2 > 0) s += "\n// Parameters for reaction " + reaction.id + "\n";
			for (i2 = 0; i2 < n2; i2++) {
				parameter = reaction.parameters.get(i2);
				s += "const double " + parameter.prismName;
				if (parameter.value != null && parameter.value.length()>0) s += " = " + parameter.value;
				s += "; // "+parameter.name+"\n";
			}
		}
		
		// Generate module for each species (except those with boundaryCondition=true)
		n = speciesList.size();
		for (i = 0; i < n; i++) {
			species = speciesList.get(i);
			if (species.boundaryCondition) continue;
			s += "\n// Species " + species + "\n";
			s += "const int "+species.prismName+"_MAX = MAX_AMOUNT;\n";
			s += "module " + species.prismName + "\n";
			
			// Generate variable representing the amount of this species
			s += "\t\n\t" + species.prismName + " : [0.."+species.prismName+"_MAX]";
			s += " init " + (int)species.init + "; // Initial amount " + (int)species.init + "\n\t\n";
//			s += " init " + (int)Math.round(scaleFactor*species.init) + "; // Initial amount " + species.init + "\n\t\n";
			
			// Generate a command for each reaction that involves this species
			n2 = reactionList.size();
			for (i2 = 0; i2 < n2; i2++) {
				reaction = reactionList.get(i2);
				if (reaction.isSpeciesInvolved(species.id)) {
					s += "\t// " + reaction.id;
					if (reaction.name.length() > 0) s += " (" + reaction.name + ")";
					s += "\n";
					s += "\t[" + reaction.id + "] " ;
					before = reaction.before(species.id);
					after = reaction.after(species.id);
					if (before > 0) s += species.prismName + " > "+(before-1);
					if (after-before > 0) {
						if (before > 0) s += " &";
						s += " " + species.prismName + " <= "+species.prismName + "_MAX-" + (after-before);
					}
					s += " -> (" + species.prismName + "'=" + species.prismName;
					if (after-before > 0) s += "+" + (after-before);
					if (after-before < 0) s += (after-before);
					s += ");\n";
				}
			}
			
			// Generate the end of this module definition
			s += "\t\nendmodule\n";
		}
		
		// Generate dummy module to store reaction rates
		s += "\n// Reaction rates\nmodule reaction_rates\n\n";
		n = reactionList.size();
		for (i = 0; i < n; i++) {
			reaction = reactionList.get(i);
			// Build info about renames (to unique PRISM idents)
			renameFrom.clear();
			renameTo.clear();
			n2 = speciesList.size();
			for (i2 = 0; i2 < n2; i2++) {
				species = speciesList.get(i2);
				if (!species.id.equals(species.prismName)) { renameFrom.add(species.id); renameTo.add(species.prismName); }
			}
			n2 = reaction.parameters.size();
			for (i2 = 0; i2 < n2; i2++) {
				parameter = reaction.parameters.get(i2);
				if (!parameter.name.equals(parameter.prismName)) { renameFrom.add(parameter.name); renameTo.add(parameter.prismName); }
			}
			n2 = parameterList.size();
			for (i2 = 0; i2 < n2; i2++) {
				parameter = parameterList.get(i2);
				if (!parameter.name.equals(parameter.prismName)) { renameFrom.add(parameter.name); renameTo.add(parameter.prismName); }
			}
			// Generate code
			s += "\t// " + reaction.id;
			if (reaction.name.length() > 0) s += " (" + reaction.name + ")";
			s += "\n";
			s2 = MathML2Prism.convert(reaction.kineticLaw, renameFrom, renameTo);
			s += "\t[" + reaction.id + "] " + s2 + " > 0 -> " + s2 + " : true;\n";
		}
		s += "\nendmodule\n";
		
		// Generate a reward structure for each species
		s += "\n// Reward structures (one per species)\n\n";
		n = speciesList.size();
		for (i = 0; i < n; i++) {
			species = speciesList.get(i);
			if (species.boundaryCondition) continue;
			s += "// " + (i+1) + "\nrewards \"" + species.prismName + "\" true : " + species.prismName + "; endrewards\n";
		}
		
		System.out.print(s);
	}
	
	// Check whether a given string is a valid PRISM language identifier
	
	private static boolean isValidPrismIdent(String s)
	{
		if (!s.matches("[_a-zA-Z_][_a-zA-Z0-9]*")) return false;
		if (prismParser.isKeyword(s)) return false;
		return true;
	}
	
	// Convert a string to a valid PRISM language identifier (by removing invalid characters)
	
	private static String convertToValidPrismIdent(String s)
	{
		String s2;
		if (!s.matches("[_a-zA-Z_][_a-zA-Z0-9]*")) s2 = s.replaceAll("[^_a-zA-Z0-9]", ""); else s2 = s;
		if (prismParser.isKeyword(s2)) s2 = "_"+s2;
		return s2;
	}
	
	// Classes to store info from an SBML file
	
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
		public String toString() { return id+(name.length()>0 ? (" ("+name+")") : ""); }
	}

	class Parameter
	{
		public String name;
		public String value;
		public String prismName;
		public Parameter(String name, String value) { this.name = name; this.value = value; this.prismName = null; }
		public String toString() { return name+"="+value; }
	}

	class Reaction
	{
		public String id;
		public String name;
		public ArrayList<String> reactants;
		public ArrayList<Integer> reactantStoichs;
		public ArrayList<String> products;
		public ArrayList<Integer> productStoichs;
		public Element kineticLaw;
		public ArrayList<Parameter> parameters;
		public Reaction(String id, String name)
		{
			this.id = id;
			this.name = name;
			reactants = new ArrayList<String>();
			reactantStoichs = new ArrayList<Integer>();
			products = new ArrayList<String>();
			productStoichs = new ArrayList<Integer>();
			kineticLaw = null;
			parameters = new ArrayList<Parameter>();
		}
		public void addReactant(String reactant) { addReactant(reactant, 1); }
		public void addReactant(String reactant, int stoich) { reactants.add(reactant); reactantStoichs.add(stoich); }
		public void addProduct(String product) { addProduct(product, 1); }
		public void addProduct(String product, int stoich) { products.add(product); productStoichs.add(stoich); }
		public void setKineticLaw(Element kineticLaw) { this.kineticLaw = kineticLaw; }
		public void addParameter(String name, String value) { parameters.add(new Parameter(name, value)); }
		public boolean isSpeciesInvolved(String species) { return reactants.contains(species) || products.contains(species); }
		public int before(String species)
		{
			int i = reactants.indexOf(species);
			if (i == -1) return 0;
			return reactantStoichs.get(i);
		}
		public int after(String species)
		{
			int i = products.indexOf(species);
			if (i == -1) return 0;
			return productStoichs.get(i);
		}
		public String toString()
		{
			String s = "";
			s += id;
			if (name.length() > 0) s+= " ("+name+")";
			s += ":\n";
			s += "    Reactants: " + reactants+"\n";
			s += "    Reactants stoichiometry: " + productStoichs+"\n";
			s += "    Products: " + products+"\n";
			s += "    Products stoichiometry: " + productStoichs+"\n";
			s += "    Kinetic law: " + kineticLaw+"\n";
			s += "    Parameters: " + parameters+"\n";
			return s;
		}
	}
}
