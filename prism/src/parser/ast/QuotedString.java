//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Joachim Klein <joachim.klein@automata.tools> (formerly TU Dresden)
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

package parser.ast;

import java.util.regex.Matcher;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

/**
 * A quoted string ("text"), double quotes and backslash are quoted/unquoted.
 */
public class QuotedString extends ASTElement
{

	/** The unquoted text */
	private String text;
	/** Flag: do we need to quote on output? */
	private boolean needsEscaping;

	/** Constructor for the text (without quotes) */
	public QuotedString(String text)
	{
		this.text = text;
		needsEscaping = needsEscaping(text);
	}

	/** Constructor for the text (without quotes) and info about escaping */
	private QuotedString(String text, boolean needsEscaping)
	{
		this.text = text;
		this.needsEscaping = needsEscaping;
	}

	/** Get the unquoted text */
	public String getText()
	{
		return text;
	}

	/** Get the quoted string */
	public String getQuoted() {
		if (!needsEscaping) {
			return "\"" + text + "\"";
		}
		// first, replace all \ -> \\
		String result= text.replaceAll("[\\\\]", Matcher.quoteReplacement("\\\\"));
		// first, replace all " -> \"
		result = result.replaceAll("[\"]", Matcher.quoteReplacement("\\\""));
		// finally, add outer "
		return "\"" + result + "\"";
	}

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public String toString()
	{
		return getQuoted();
	}

	@Override
	public QuotedString deepCopy()
	{
		return new QuotedString(text, needsEscaping);
	}

	/** Does the unquoted string need quoting, i.e. does it contain " or \ characters?*/
	private static boolean needsEscaping(String s)
	{
		// does the string contain " or \ ?
		return (s.indexOf('"')!=-1 || s.indexOf('\\')!=-1);
	}

	/** Static constructor, from quoted string */
	public static QuotedString fromQuoted(String quotedString) throws PrismLangException
	{
		return new QuotedString(unquote(quotedString));
	}

	/** Unquote a quoted string */
	public static String unquote(String s) throws PrismLangException
	{
		if (!s.startsWith("\"")) {
			throw new PrismLangException("String not quoted: " + s);
		}
		if (!s.endsWith("\"")) {
			throw new PrismLangException("String does not end with quote: " + s);
		}
		if (s.length() < 2) {
			throw new PrismLangException("Invalid quoted string: " + s);
		}

		// remove outer quotes
		s = s.substring(1, s.length()-1);
		int pos = s.indexOf('\\'); // find first '\'
		if (pos == -1) {
			// nothing to unquote
			if (s.indexOf('"') != -1) {
				throw new PrismLangException("Unquoted \" in string: \"" + s+ "\"");
			}
			return s;
		}

		StringBuffer sb = new StringBuffer();
		int last_pos = 0;
		do {
			String n = s.substring(last_pos, pos); // substring that doesn't need to be quoted
			if (n.indexOf('"') != -1) {
				throw new PrismLangException("Unquoted \" in string: \"" + s+ "\"");
			}
			sb.append(n);
			if (pos+1 >= s.length()) {
				throw new PrismLangException("Quoted string ends with \\ character: \"" + s +"\"");
			}
			sb.append(s.charAt(pos+1));  // the quoted character
			last_pos = pos+2;                 // skip quote character
			pos = s.indexOf('\\', last_pos);  // find next '\'
		} while (pos != -1);

		if (last_pos < s.length()) {
			String n = s.substring(last_pos); // remaining
			if (n.indexOf('"') != -1) {
				throw new PrismLangException("Unquoted \" in string: \"" + s+ "\"");
			}
			sb.append(n);
		}

		return sb.toString();
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (needsEscaping ? 1231 : 1237);
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof QuotedString))
			return false;
		QuotedString other = (QuotedString) obj;
		if (needsEscaping != other.needsEscaping)
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

	/** small test routine, reads lines from System.in and unquotes / quotes */
	public static void main(String[] args) throws Exception
	{
		java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
		String line;
		while ((line = r.readLine()) != null) {
			System.out.print(line + " => ");
			System.out.flush();
			try {
				String unquoted = unquote(line);
				System.out.println(unquoted + " => " + new QuotedString(unquoted));
			} catch (PrismLangException e) {
				System.out.println();
				System.out.println(e);
			}
		}
	}
}
