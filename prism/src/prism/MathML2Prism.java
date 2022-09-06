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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.*;

public class MathML2Prism
{
	// Converts a MathML object to the corresponding PRISM expression (as a string)
	// (optionally with a list of renames for identifiers)
	
	public static String convert(Node node) throws PrismException { return convert(node, null, null); }
	
	public static String convert(Node node, ArrayList<String> renameFrom, ArrayList<String> renameTo) throws PrismException
	{
		String s, nodeName, apply;
		int nodeType, i, n;
		NodeList allChildren;
		ArrayList<Node> children;
		ArrayList<String> translatedChildren;
		
		nodeType = node.getNodeType();
		switch (nodeType) {
		
		// MathML node
		case Node.ELEMENT_NODE:
			nodeName = node.getNodeName();
			
			// Top-level element
			if (nodeName.equals("math")) {
				// return string for first non-text element
				allChildren = node.getChildNodes();
				n = allChildren.getLength();
				children = new ArrayList<Node>();
				for (i = 0; i < n; i++) {
					if (allChildren.item(i).getNodeType() != Node.TEXT_NODE) children.add(allChildren.item(i));
				}
				if (children.size() == 0) throw new PrismException("Empty MathML expression");
				if (children.size() > 1) throw new PrismException("Too many top-level nodes in MathML expression");
				return convert(children.get(0), renameFrom, renameTo);
			}
			
			// Literal
			if (nodeName.equals("cn")) {
				s = node.getFirstChild().getNodeValue().trim();
				return s;
			}
			
			// Identifier
			else if (nodeName.equals("ci")) {
				s = node.getFirstChild().getNodeValue().trim();
				if (renameFrom != null) if (renameFrom.contains(s)) s = renameTo.get(renameFrom.indexOf(s));
				return s;
			}
			
			// Apply operator
			else if (nodeName.equals("apply")) {
				// Get list of non-text nodes
				allChildren = node.getChildNodes();
				n = allChildren.getLength();
				children = new ArrayList<Node>();
				for (i = 0; i < n; i++) {
					if (allChildren.item(i).getNodeType() != Node.TEXT_NODE) children.add(allChildren.item(i));
				}
				n = children.size();
				if (n == 0) throw new PrismException("Empty apply node in MathML expression");
				// Find operator and translate children
				apply = children.get(0).getNodeName();
				translatedChildren = new ArrayList<String>();
				n = children.size() - 1;
				for (i = 0; i < n; i++) {
					translatedChildren.add(convert(children.get(i+1), renameFrom, renameTo));
				}
				
				// Apply "plus"
				if (apply.equals("plus")) {
					s = "(";
					for (i = 0; i < n; i++) {
						if (i > 0) s += "+";
						s += translatedChildren.get(i);
					}
					s += ")";
					return s;
				}
				
				// Apply "minus"
				if (apply.equals("minus")) {
					if (n != 2) throw new PrismException("MathML apply minus operations has "+n+" operands");
					s = "("+translatedChildren.get(0)+"-"+translatedChildren.get(1)+")";
					return s;
				}
				
				// Apply "times"
				if (apply.equals("times")) {
					s = "(";
					for (i = 0; i < n; i++) {
						if (i > 0) s += "*";
						s += translatedChildren.get(i);
					}
					s += ")";
					return s;
				}
				
				// Apply "divide"
				if (apply.equals("divide")) {
					if (n != 2) throw new PrismException("MathML apply divide operations has "+n+" operands");
					s = "("+translatedChildren.get(0)+"/"+translatedChildren.get(1)+")";
					return s;
				}
				
				// Apply "power"
				if (apply.equals("power")) {
					if (n != 2) throw new PrismException("MathML apply power operations has "+n+" operands");
					s = "(func(pow,"+translatedChildren.get(0)+","+translatedChildren.get(1)+"))";
					return s;
				}
				
				throw new PrismException("Unknown MathML apply operator \""+apply+"\"");
			}
			else {
				throw new PrismException("Unknown MathML element \""+nodeName+"\"");
			}
		
		// Ignore CDATA sections + entity references
		case Node.CDATA_SECTION_NODE:
		case Node.ENTITY_REFERENCE_NODE:
			return "";
		
		// Ignore text. For cases where we need it, e.g. "<ci> k4prime </ci>",
		// this is processed by the parent node, e.g. <ci>
		case Node.TEXT_NODE:
			return "";
		
		// Ignore processing instructions
		case Node.PROCESSING_INSTRUCTION_NODE:
			return "";
		}
		
		// Default: return empty string
		return "";
	}
	
	// Additional utility method (currently unused except for testing)
	// which parses a MathML expression from an InputStream before calling the conversion method above
	
	public static String parseAndConvert(InputStream in) throws PrismException
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
			//builder.setEntityResolver(this);
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
			doc = builder.parse(in);
			
			return convert(doc.getDocumentElement());
		}
		catch (IOException e) {
			throw new PrismException("I/O error: "+e.getMessage());
		}
		catch (SAXException e) {
			throw new PrismException("Invalid XML file:\n"+ e.getMessage());
		}
	}

	// Test call function - read MathML expression from stdin, convert, send to stdout
	
	public static void main(String args[])
	{
		try {
			String s = parseAndConvert(System.in);
			System.out.println(s);
		}
		catch (PrismException e) {
			System.err.println("Error: "+e.getMessage());
		}
	}
}
