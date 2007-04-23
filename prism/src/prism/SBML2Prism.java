//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class SBML2Prism implements EntityResolver
{
	private ArrayList<Species> speciesList;
	private ArrayList<Reaction> reactionList;
	private int scaleFactor;
	
	// Calling point: e.g. java -cp classes prism.SBML2Prism myfile.sbml 100
	// (100 denotes (integer) scale factor used to compute population sizes from (real-valued) concentrations, default is 10)
	
	public static void main(String args[])
	{
		try {
			if (args.length < 1) {
				System.err.println("Usage: java -cp classes prism.SBML2Prism <sbml_file> [scale_factor]");
				System.exit(1);
			}
			new SBML2Prism().load(new File(args[0]), (args.length>1)?args[1]:"10");
		}
		catch (PrismException e) {
			System.err.println("Error: "+e.getMessage());
		}
	}
	
	// Main method: load SBML file, process and sent resulting PRISM file to stdout
	
	public void load(File f, String scaleFactor) throws PrismException
	{
		try { this.scaleFactor = Integer.parseInt(scaleFactor); }
		catch (NumberFormatException e) { throw new PrismException("Invalid scale factor \""+scaleFactor+"\""); }
		Document doc = parseSBML(f);
		extractModelFromSBML(doc);
		printModel(System.err);
		processModel();
		printPRISMModel(f);
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
			InputStream inputStream = ClassLoader.getSystemResourceAsStream("dtds/sbml.dtd");
			if (inputStream != null) inputSource = new InputSource(inputStream);
		}
		
		return inputSource;
	}

	// Extract the information about the model from the parsed SBML file
	
	private void extractModelFromSBML(Document doc) throws PrismException
	{
		Element e, e_model, e_list, e_species, e_reaction, e_kinetics, e_mathml, e_params;
		NodeList nodes, nodes2;
		Species species;
		Reaction reaction;
		int i, j, n, m;
		double d;
		String s;
		
		// Get "model" element of SBML file
		nodes = doc.getDocumentElement().getElementsByTagName("model");
		e_model = (Element)nodes.item(0);
		
		// Process list of species
		speciesList = new ArrayList<Species>();
		e_list = (Element)e_model.getElementsByTagName("listOfSpecies").item(0);
		nodes = e_list.getElementsByTagName("species");
		n = nodes.getLength();
		for (i = 0; i < n; i++) {
			e_species = (Element)nodes.item(i);
			d = Double.parseDouble(e_species.getAttribute("initialAmount"));
			species = new Species(e_species.getAttribute("id"), e_species.getAttribute("name"), d);
			speciesList.add(species);
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
					s = e.getAttribute("species");
					reaction.addReactant(s);
				}
			}
			
			// Product list
			e_list = (Element)e_reaction.getElementsByTagName("listOfProducts").item(0);
			if (e_list != null) {
				nodes2 = e_list.getElementsByTagName("speciesReference");
				m = nodes2.getLength();
				for (j = 0; j < m; j++) {
					e = (Element)nodes2.item(j);
					s = e.getAttribute("species");
					reaction.addProduct(s);
				}
			}
			
			// Kinetic law
			e_kinetics = (Element)e_reaction.getElementsByTagName("kineticLaw").item(0);
			e_mathml = (Element)e_kinetics.getElementsByTagName("math").item(0);
			reaction.setKineticLaw(e_mathml);
			e_list = (Element)e_kinetics.getElementsByTagName("listOfParameters").item(0);
			nodes2 = e_list.getElementsByTagName("parameter");
			m = nodes2.getLength();
			for (j = 0; j < m; j++) {
				e = (Element)nodes2.item(j);
				reaction.addParameter(e.getAttribute("id"), e.getAttribute("value"));
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
		HashSet<String> modulesNames;
		HashSet<String> prismIdents;
		
		// Generate unique and valid PRISM identifier (module and variable name) for each species
		modulesNames = new HashSet<String>();
		prismIdents = new HashSet<String>();
		n = speciesList.size();
		for (i = 0; i < n; i++) {
			species = speciesList.get(i);
			s = species.id;
			s2 = convertToValidPrismIdent(s);
			if (!s.equals(s2)) System.err.println("Warning: Converted species id \""+s+"\" to \""+s2+"\" (invalid PRISM identifier)");
			if (!modulesNames.add(s2)) {
				j = 2;
				while (!modulesNames.add(s2+"_"+j)) j++;
				s2 = s2+"_"+j;
				System.err.println("Warning: Converted species id \""+s+"\" to \""+s2+"\" (duplicate PRISM identifiers)");
			}
			species.prismName = s2;
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
				if (!s.equals(s2)) System.err.println("Warning: Converted parameter id \""+s+"\" to \""+s2+"\" (invalid PRISM identifier)");
				if (!prismIdents.add(s2)) {
					k = 2;
					while (!prismIdents.add(s2+"_"+k)) k++;
					s2 = s2+"_"+k;
					System.err.println("Warning: Converted parameter id \""+s+"\" to \""+s2+"\" (duplicate PRISM identifiers)");
				}
				reaction.parameters.get(j).prismName = s2;
			}
		}
	}
	
	// Generate and print PRISM code
	
	private void printPRISMModel(File f) throws PrismException
	{
		int i, i2, n, n2;
		Species species;
		Reaction reaction;
		Parameter parameter;
		String s = "";
		
		// Header
		s += "// File generated by automatic SBML-to-PRISM conversion\n";
		s += "// Original SBML file: " + f.getPath() + "\n\n"; 
		s += "ctmc\n";
		
		// 
		s += "\nconst int SCALE_FACTOR = " + scaleFactor + ";\n";
		
		// Generate constant definition for each (reaction) parameter
		n = reactionList.size();
		for (i = 0; i < n; i++) {
			reaction = reactionList.get(i);
			n2 = reaction.parameters.size();
			if (n2 > 0) s += "\n// Parameters for reaction " + reaction.id + "\n";
			for (i2 = 0; i2 < n2; i2++) {
				parameter = reaction.parameters.get(i2);
				s += "const double " + parameter.prismName + " = " + parameter.value + "; // "+parameter.name+"\n";
			}
		}
		
		// Generate module for each species
		n = speciesList.size();
		for (i = 0; i < n; i++) {
			species = speciesList.get(i);
			s += "\n// Species " + species + "\n";
			s += "module " + species.prismName + "\n";
			
			// Generate variable representing the amount of this species
			s += "\t\n\t" + species.prismName + " : [0.." + scaleFactor + "]";
			s += " init " + (int)Math.round(scaleFactor*species.init) + "; // Initial amount " + species.init + "\n\t\n";
			
			// Generate a command for each reaction that involves this species as a reactant
			n2 = reactionList.size();
			for (i2 = 0; i2 < n2; i2++) {
				reaction = reactionList.get(i2);
				if (reaction.reactants.contains(species.id)) {
					s += "\t// " + reaction.id;
					if (reaction.name.length() > 0) s += " (" + reaction.name + ")";
					s += "\n";
					s += "\t[" + reaction.id + "] " ;
					s += species.prismName + " > 0 -> (" + species.prismName + "'=" + species.prismName + "-1);\n";
				}
			}
			
			// Generate a command for each reaction that involves this species as a product
			n2 = reactionList.size();
			for (i2 = 0; i2 < n2; i2++) {
				reaction = reactionList.get(i2);
				if (reaction.products.contains(species.id)) {
					s += "\t// " + reaction.id;
					if (reaction.name.length() > 0) s += " (" + reaction.name + ")";
					s += "\n";
					s += "\t[" + reaction.id + "] " ;
					s += species.prismName + " < " + scaleFactor + " -> (" + species.prismName + "'=" + species.prismName + "+1);\n";
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
			s += "\t// " + reaction.id;
			if (reaction.name.length() > 0) s += " (" + reaction.name + ")";
			s += "\n";
			s += "\t[" + reaction.id + "] " + MathML2Prism.convert(reaction.kineticLaw) + " > 0 -> " + MathML2Prism.convert(reaction.kineticLaw) + " : true;\n";
			System.err.println(MathML2Prism.convert(reaction.kineticLaw));
		}
		s += "\nendmodule\n";
		
		// Generate a reward structure for each species
		s += "\n// Reward structures (one per species)\n\n";
		n = speciesList.size();
		for (i = 0; i < n; i++) {
			species = speciesList.get(i);
			s += "// " + (i+1) + "\nrewards \"" + species.prismName + "\" true : " + species.prismName + "; endrewards\n";
		}
		
		System.out.print(s);
	}
	
	// Check whether a given string is a valid PRISM language identifier
	
	private static boolean isValidPrismIdent(String s)
	{
		return s.matches("[_a-zA-Z_][_a-zA-Z0-9]*");
	}
	
	// Convert a string to a valid PRISM language identifier (by removing invalid characters)
	
	private static String convertToValidPrismIdent(String s)
	{
		String s2;
		if (isValidPrismIdent(s)) return s;
		s2 = s.replaceAll("[^_a-zA-Z0-9]", "");
		return s2;
	}
	
	// Classes to store info from an SBML file
	
	class Species
	{
		public String id;
		public String name;
		public double init;
		public String prismName;
		public Species(String id, String name, double init)
		{
			this.id = id;
			this.name = name;
			this.init = init;
			this.prismName = null;
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
		public ArrayList<String> products;
		public Element kineticLaw;
		public ArrayList<Parameter> parameters;
		public Reaction(String id, String name)
		{
			this.id = id;
			this.name = name;
			reactants = new ArrayList<String>();
			products = new ArrayList<String>();
			kineticLaw = null;
			parameters = new ArrayList<Parameter>();
		}
		public void addReactant(String reactant) { reactants.add(reactant); }
		public void addProduct(String product) { products.add(product); }
		public void setKineticLaw(Element kineticLaw) { this.kineticLaw = kineticLaw; }
		public void addParameter(String name, String value) { parameters.add(new Parameter(name, value)); }
		public String toString()
		{
			String s = "";
			s += id;
			if (name.length() > 0) s+= " ("+name+")";
			s += ":\n";
			s += "    Reactants: " + reactants+"\n";
			s += "    Products: " + products+"\n";
			s += "    Kinetic law: " + kineticLaw+"\n";
			s += "    Parameters: " + parameters+"\n";
			return s;
		}
	}
}
