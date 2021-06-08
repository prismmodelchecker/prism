//==============================================================================
//	
//	Copyright (c) 2021-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

package parser;

import java.util.HashMap;

import parser.ast.ASTElement;
import prism.PrismLangException;

/**
 * Helper class to keep track of a set of identifiers that have been used.
 */
public class IdentUsage
{
	// Are these identifiers used in (double) quotes?
	private boolean quoted = false;
	
	// Where identifiers were declared (AST element)
	private HashMap<String, ASTElement> identDecls;
	// Uses of identifiers (e.g. "constant")
	private HashMap<String, String> identUses;
	// Location of identifiers (e.g. "the model")
	private HashMap<String, String> identLocs;

	public IdentUsage()
	{
		this(false);
	}
	
	/**
	 * @param quoted Are these identifiers used in (double) quotes?
	 */
	public IdentUsage(boolean quoted)
	{
		this.quoted = quoted;
		identDecls = new HashMap<>();
		identUses = new HashMap<>();
		identLocs = new HashMap<>();
	}
	
	public void clear()
	{
		identDecls.clear();
		identUses.clear();
		identLocs.clear();
	}
	
	/**
	 * Check if an identifier is already used somewhere
	 * and throw an exception if it is. Otherwise, add it to the list.
	 * @param ident The name of the (new) identifier
	 * @param decl Where the identifier is declared in the model
	 * @param use Optionally, the identifier's usage (e.g. "constant")
	 * @param loc Optionally, the identifier's location (e.g. "the model")
	 */
	public void checkAndAddIdentifier(String ident, ASTElement decl, String use, String loc) throws PrismLangException
	{
		checkIdent(ident, decl, use);
		identDecls.put(ident, decl);
		identUses.put(ident, use);
		identLocs.put(ident, loc);
	}
	
	/**
	 * Check if an identifier is already used somewhere
	 * @param ident The name of the identifier to check
	 */
	public boolean isIdentUsed(String ident)
	{
		return identDecls.containsKey(ident);
	}

	/**
	 * Check if an identifier is already used somewhere
	 * and throw an exception if it is.
	 * @param ident The name of the identifier to check
	 * @param decl Where the identifier is declared in the model (for the error message)
	 * @param use Optionally, the identifier's usage (e.g. "constant")
	 */
	public void checkIdent(String ident, ASTElement decl, String use) throws PrismLangException
	{
		ASTElement existing = identDecls.get(ident);
		if (existing != null) {
			// Construct error message if identifier exists already
			String identStr = use != null ? ("Name of " + use) : "Identifier";
			String qu = quoted ? "\"" : "";
			identStr += " " + qu + ident + qu;
			String existingUse = identUses.get(ident);
			if (existingUse == null) {
				existingUse = "";
			} else {
				if (existingUse.length() > 0 && "aeiou".contains("" + existingUse.charAt(0))) {
					existingUse = " for an " + existingUse;
				} else {
					existingUse = " for a " + existingUse;
				}
			}
			String existingLoc = identLocs.get(ident);
			existingLoc = (existingLoc == null) ? "" : " in " + existingLoc;
			throw new PrismLangException(identStr + " is already used" + existingUse + existingLoc, decl);
		}
	}

	@SuppressWarnings("unchecked")
	public IdentUsage deepCopy()
	{
		IdentUsage ret = new IdentUsage(quoted);
		ret.identDecls = (HashMap<String, ASTElement>) identDecls.clone();
		return ret;
	}
}
