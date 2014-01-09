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

package parser;

import java.util.ArrayList;
import java.io.*;

import prism.Prism;
import prism.PrismLangException;

public class PrismSyntaxHighlighter
{
	// token types
	public static final int PUNCTUATION = 0;
	public static final int COMMENT = 1;
	public static final int WHITESPACE = 2;
	public static final int KEYWORD = 3;
	public static final int NUMERIC = 4;
	public static final int IDENTIFIER = 5;
	public static final int PREPROC = 6;
	public static final int EOF = 100;
	
	// output types
	private static final int ECHO = 0; // for testing mainly
	private static final int HTML = 1;
	private static final int LATEX = 2;
	private static final int PRISMGUI = 3;
	
	// punctation replacements
	private static final ArrayList<String> puncReplaceTo_ECHO , puncReplaceFrom_ECHO;
	private static final ArrayList<String> puncReplaceTo_HTML, puncReplaceFrom_HTML;
	private static final ArrayList<String> puncReplaceTo_LATEX, puncReplaceFrom_LATEX;
	private static final ArrayList<String> puncReplaceTo_LATEXMATHS, puncReplaceFrom_LATEXMATHS;
	private static final ArrayList<String> puncReplaceTo_PRISMGUI, puncReplaceFrom_PRISMGUI;
	private static int quoteAlternator = 1;
	
	static {
		puncReplaceFrom_ECHO = new ArrayList<String>(); puncReplaceTo_ECHO = new ArrayList<String>();
		puncReplaceFrom_HTML = new ArrayList<String>(); puncReplaceTo_HTML = new ArrayList<String>();
		puncReplaceFrom_HTML.add("&"); puncReplaceTo_HTML.add("&amp;");
		puncReplaceFrom_HTML.add("<"); puncReplaceTo_HTML.add("&lt;");
		puncReplaceFrom_HTML.add(">"); puncReplaceTo_HTML.add("&gt;");
		puncReplaceFrom_LATEX = new ArrayList<String>(); puncReplaceTo_LATEX = new ArrayList<String>();
		puncReplaceFrom_LATEX.add("&"); puncReplaceTo_LATEX.add("\\\\&");
		puncReplaceFrom_LATEX.add("_"); puncReplaceTo_LATEX.add("\\\\_");
		puncReplaceFrom_LATEX.add("%"); puncReplaceTo_LATEX.add("\\\\%");
		puncReplaceFrom_LATEX.add(">="); puncReplaceTo_LATEX.add("\\${\\\\geq}\\$");
		puncReplaceFrom_LATEX.add("<="); puncReplaceTo_LATEX.add("\\${\\\\leq}\\$");
		puncReplaceFrom_LATEX.add("->"); puncReplaceTo_LATEX.add("\\$\\\\rightarrow\\$");
		puncReplaceFrom_LATEX.add("=>"); puncReplaceTo_LATEX.add("\\$\\\\Rightarrow\\$");
		puncReplaceFrom_LATEX.add("\\{"); puncReplaceTo_LATEX.add("\\\\{");
		puncReplaceFrom_LATEX.add("\\}"); puncReplaceTo_LATEX.add("\\\\}");
		puncReplaceFrom_LATEX.add("="); puncReplaceTo_LATEX.add("\\${=}\\$");
		puncReplaceFrom_LATEX.add(">"); puncReplaceTo_LATEX.add("\\${>}\\$");
		puncReplaceFrom_LATEX.add("<"); puncReplaceTo_LATEX.add("\\${<}\\$");
		puncReplaceFrom_LATEX.add("#"); puncReplaceTo_LATEX.add("\\\\#");
		puncReplaceFrom_LATEXMATHS = new ArrayList<String>(); puncReplaceTo_LATEXMATHS = new ArrayList<String>();
		puncReplaceFrom_LATEXMATHS.add("&"); puncReplaceTo_LATEXMATHS.add("\\\\&");
		puncReplaceFrom_LATEXMATHS.add("_"); puncReplaceTo_LATEXMATHS.add("\\\\_");
		puncReplaceFrom_LATEXMATHS.add("%"); puncReplaceTo_LATEXMATHS.add("\\\\%");
		puncReplaceFrom_LATEXMATHS.add(">="); puncReplaceTo_LATEXMATHS.add("{\\\\geq}");
		puncReplaceFrom_LATEXMATHS.add("<="); puncReplaceTo_LATEXMATHS.add("{\\\\leq}");
		puncReplaceFrom_LATEXMATHS.add("->"); puncReplaceTo_LATEXMATHS.add("\\\\rightarrow");
		puncReplaceFrom_LATEXMATHS.add("=>"); puncReplaceTo_LATEXMATHS.add("\\\\Rightarrow");
		puncReplaceFrom_LATEXMATHS.add("\\{"); puncReplaceTo_LATEXMATHS.add("\\\\{");
		puncReplaceFrom_LATEXMATHS.add("\\}"); puncReplaceTo_LATEXMATHS.add("\\\\}");
		puncReplaceFrom_LATEXMATHS.add("="); puncReplaceTo_LATEXMATHS.add("{=}");
		puncReplaceFrom_LATEXMATHS.add(">"); puncReplaceTo_LATEXMATHS.add("{>}");
		puncReplaceFrom_LATEXMATHS.add("<"); puncReplaceTo_LATEXMATHS.add("{<}");
		puncReplaceFrom_PRISMGUI = new ArrayList<String>(); puncReplaceTo_PRISMGUI = new ArrayList<String>();
	}
	
	// resulting output
	private static StringBuffer resStringBuffer;;
	private static int resNewLine;
	private static boolean resStart;
	private static int resTypeArray[];
	private static int resCharCount;
	
	// public methods
	
	public static String echoFile(File file) throws FileNotFoundException, PrismLangException
	{
		resStringBuffer = new StringBuffer();
		highlight(new FileInputStream(file), ECHO);
		return resStringBuffer.toString();
	}
	
	public static String echoFile(InputStream stream) throws PrismLangException
	{
		resStringBuffer = new StringBuffer();
		highlight(stream, ECHO);
		return resStringBuffer.toString();
	}
	
	public static String lineToHtml(String line) throws PrismLangException
	{
		resStringBuffer = new StringBuffer();
		highlight(new ByteArrayInputStream(line.getBytes()), HTML);
		return resStringBuffer.toString();
	}
	
	public static String fileToHtml(File file, boolean hf, String cssLoc) throws FileNotFoundException, PrismLangException
	{
		resStringBuffer = new StringBuffer();
		if (hf) resStringBuffer.append(htmlFileHeader(file.getName(), cssLoc));
		highlight(new FileInputStream(file), HTML);
		if (hf) resStringBuffer.append(htmlFileFooter()); 
		return resStringBuffer.toString();
	}
	
	public static String fileToHtml(InputStream stream, boolean hf, String cssLoc) throws PrismLangException
	{
		resStringBuffer = new StringBuffer();
		if (hf) resStringBuffer.append(htmlFileHeader("PRISM Code", cssLoc)); 
		highlight(stream, HTML);
		if (hf) resStringBuffer.append(htmlFileFooter()); 
		return resStringBuffer.toString();
	}

	public static String fileToLatex(File file, boolean hf) throws FileNotFoundException, PrismLangException
	{
		resStringBuffer = new StringBuffer();
		resNewLine = 1;
		resStart = true;
		if (hf) resStringBuffer.append(latexFileHeader(file.getName()));
		highlight(new FileInputStream(file), LATEX);
		if (hf) resStringBuffer.append(latexFileFooter()); 
		return resStringBuffer.toString();
	}
	
	public static String fileToLatex(InputStream stream, boolean hf) throws PrismLangException
	{
		resStringBuffer = new StringBuffer();
		resNewLine = 1;
		resStart = true;
		if (hf) resStringBuffer.append(latexFileHeader("PRISM Code")); 
		highlight(stream, LATEX);
		if (hf) resStringBuffer.append(latexFileFooter()); 
		return resStringBuffer.toString();
	}
	
	public static int[] lineForPrismGUI(String line) throws PrismLangException
	{
		resTypeArray = new int[line.length()];
		resCharCount = 0;
		highlight(new ByteArrayInputStream(line.getBytes()), PRISMGUI);
		return resTypeArray;
	}

	// generate file headers/footers
	
	private static String htmlFileHeader(String title, String cssLoc)
	{
		String s = "";
		s += "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n";
		s += "\"http://www.w3.org/TR/html4/loose.dtd\">\n";
		
		s += "<html>" + "\n";
		s += "<head>" + "\n";
		s += "<title>" + "\n";
		s += title + "\n";
		s += "</title>" + "\n";
		s += "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" + "\n";
		s += "<!-- Style sheet \"prism.css\" can be found in the \"etc\" directory of the PRISM distribution -->" + "\n";
		s += "<link type=\"text/css\" rel=\"stylesheet\" href=\"" + cssLoc + "\">" + "\n";
		s += "</head>" + "\n";
		s += "<body text=\"#000000\" bgcolor=\"#ffffff\">" + "\n";
		s += "<pre>" + "\n";
		
		return s;
	}
	
	private static String htmlFileFooter()
	{
		String s = "";
		
		s += "</pre>" + "\n";
		s += "</body>" + "\n";
		s += "</html>" + "\n";
		
		return s;
	}

	private static String latexFileHeader(String title)
	{
		String s = "";
		
		s += "\\centering" + "\n";
		s += "{\\scriptsize" + "\n";
		s += "\\fbox{\\begin{minipage}{6in}" + "\n";
		s += "\\begin{tabbing}" + "\n";
		//s += "\\quad \\=\\quad \\=\\quad \\=\\quad \\=\\quad \\=\\quad \\=\\quad \\=\\quad \\=\\quad\\=\\quad \\=\\quad \\=\\quad \\kill" + "\n";
		
		return s;
	}
	
	private static String latexFileFooter()
	{
		String s = "";
		
		s += "\\end{tabbing}" + "\n";
		s += "\\end{minipage}}}" + "\n";
		
		return s;
	}

	// multi-purpose highlighting code
	
	public static void highlight(InputStream stream, int oType) throws PrismLangException
	{
		PrismParser prismParser;
		PrismParserTokenManager tokenManager;
		Token first, t, st;
		boolean done = false;
		
		try {
			// obtain exclusive acces to the prism parser
			// (don't forget to release it afterwards)
			prismParser = Prism.getPrismParser();
			try {
				// restart parser and get its token manager
				PrismParser.ReInit(stream);
				tokenManager = prismParser.token_source;
				
				// get stream of tokens from token manager and put in a linked list
				first = t = tokenManager.getNextToken();
				while (t != null && t.kind != PrismParserConstants.EOF) {
					t.next = tokenManager.getNextToken();
					t = t.next;
				}
				
				// go through tokens to do syntax highlighting
				t = first;
				while (!done) {
					// see if we're at the end
					if (t == null) { done = true; continue; }
					if (t.kind == PrismParserConstants.EOF) { done = true; }
					// see if there was a lexical error
					if (t.kind == PrismParserConstants.LEXICAL_ERROR) {
						String s = "Lexical error (\""+t.image+"\", line "+t.beginLine+", column "+t.beginColumn+")";
						throw new PrismLangException(s);
					}
					// if there are preceding special tokens
					if (t.specialToken != null) {
						// go back to start of special tokens
						st = t.specialToken;
						while (st.specialToken != null) st = st.specialToken;
						// go through special tokens
						while (st != null) {
							// output
							if (st.kind == PrismParserConstants.COMMENT) {
								output(st.image, COMMENT, oType);
							} else {
								output(st.image, WHITESPACE, oType);
							}
							// next special token
							st = st.next;
						}
					}
					// output
					if (t.kind == PrismParserConstants.EOF) {
						output(t.image, EOF, oType);
						continue;
					}
					else if (t.kind > PrismParserConstants.COMMENT && t.kind < PrismParserConstants.NOT)
						output(t.image, KEYWORD, oType);
					else if (t.kind == PrismParserConstants.REG_INT || t.kind == PrismParserConstants.REG_DOUBLE)
						output(t.image, NUMERIC, oType);
					else if (t.kind == PrismParserConstants.REG_IDENT || t.kind == PrismParserConstants.REG_IDENTPRIME)
						output(t.image, IDENTIFIER, oType);
					else if (t.kind == PrismParserConstants.PREPROC)
						output(t.image, PREPROC, oType);
					else
						output(t.image, PUNCTUATION, oType);
					// next token
					t = t.next;
				}
			}
			finally {
				// release prism parser
				Prism.releasePrismParser();
			}
		}
		catch (InterruptedException e) {
			throw new PrismLangException("Concurrency error in parser");
		}
	}

	private static void output(String s, int tType, int oType)
	{
		int i, n;
		
		// deal with new lines for latex
		// (beacuse we have to precede each new line with "\mbox{")
		if (oType == LATEX) if (resNewLine > 0) {
			// if this next token is another new line, don't output anything yet
			if (!(tType == WHITESPACE && ("\r".equals(s) || "\n".equals(s)))) {
				// trim trailing new lines
				if (tType == EOF) resNewLine = 1;
				// output code for new line(s)
				for (i = 0; i < resNewLine; i++) {
					if (!resStart) {
						resStringBuffer.append("$}");
						if (tType != EOF) resStringBuffer.append(" \\\\");
						resStringBuffer.append("\n");
					}
					if (tType != EOF) resStringBuffer.append("\\mbox{$");
					resStart = false;
				}
				resNewLine = 0;
			}
		}
		
		// substitute any punctuation with special code as necessary
		s = replacePunc(s, tType, oType);
		
		switch (tType) {
		
		case PUNCTUATION:
			switch(oType) {
			case ECHO: resStringBuffer.append(s); break;
			case HTML: resStringBuffer.append(s); break;
			case LATEX: resStringBuffer.append(s); break;
			case PRISMGUI: n = s.length(); for (i=0;i<n;i++) resTypeArray[resCharCount++]=PUNCTUATION; break;
			}
			break;
		
		case COMMENT:
			// strip any nasty carriage returns
			s = s.replaceAll("\r", "");
			switch (oType) {
			case ECHO: resStringBuffer.append(s); break;
			case HTML: resStringBuffer.append("<span class=\"prismcomment\">"+s.substring(0,s.length()-1)+"</span>"+"\n"); break;
			case LATEX: resStringBuffer.append("\\prismcomment{"+s.substring(0,s.length()-1)+"}"); resNewLine++; break;
			case PRISMGUI: n = s.length(); for (i=0;i<n;i++) resTypeArray[resCharCount++]=COMMENT; break;
			}
			break;
		
		case WHITESPACE:
			// ignore carriage returns
			if ("\r".equals(s)) break;
			switch(oType) {
			case ECHO: resStringBuffer.append(s); break;
			case HTML: resStringBuffer.append(s); break;
			case LATEX: if ("\n".equals(s)) resNewLine++;
			            else if (" ".equals(s)) resStringBuffer.append(" \\; ");
			            else if ("\t".equals(s)) resStringBuffer.append("\\prismtab");
			            else resStringBuffer.append(s);
			            break;
			case PRISMGUI: n = s.length(); for (i=0;i<n;i++) resTypeArray[resCharCount++]=WHITESPACE; break;
			}
			break;
		
		case KEYWORD:
			switch (oType) {
			case ECHO: resStringBuffer.append(s); break;
			case HTML: resStringBuffer.append("<span class=\"prismkeyword\">"+s+"</span>"); break;
			case LATEX: resStringBuffer.append("\\prismkeyword{"+s+"}"); break;
			case PRISMGUI: n = s.length(); for (i=0;i<n;i++) resTypeArray[resCharCount++]=KEYWORD; break;
			}
			break;
		
		
		case NUMERIC:
			switch (oType) {
			case ECHO: resStringBuffer.append(s); break;
			case HTML: resStringBuffer.append("<span class=\"prismnum\">"+s+"</span>"); break;
			case LATEX: resStringBuffer.append(s); break;
			case PRISMGUI: n = s.length(); for (i=0;i<n;i++) resTypeArray[resCharCount++]=NUMERIC; break;
			}
			break;
		
		case IDENTIFIER:
			switch (oType) {
			case ECHO: resStringBuffer.append(s); break;
			case HTML: resStringBuffer.append("<span class=\"prismident\">"+s+"</span>"); break;
			case LATEX: resStringBuffer.append("\\prismident{"+s+"}"); break;
			case PRISMGUI: n = s.length(); for (i=0;i<n;i++) resTypeArray[resCharCount++]=IDENTIFIER; break;
			}
			break;
		
		case PREPROC:
			switch (oType) {
			case ECHO: resStringBuffer.append(s); break;
			case HTML: resStringBuffer.append("<span class=\"prismpreproc\">"+s+"</span>"); break;
			case LATEX: resStringBuffer.append("\\prismpreproc{"+s+"}"); break;
			case PRISMGUI: n = s.length(); for (i=0;i<n;i++) resTypeArray[resCharCount++]=PREPROC; break;
			}
			break;
		
		case EOF:
			switch (oType) {
			case ECHO: break;
			case HTML: break;
			case LATEX:
				// close \mbox if there was no trailing new line
				if (resStringBuffer.length() > 0)
					if (resStringBuffer.charAt(resStringBuffer.length()-1)  != '\n')
						resStringBuffer.append("$}");
				break;
			case PRISMGUI: break;
			}
			break;
		}
	}
	
	// substitute any punctuation with special code as necessary
	
	public static String replacePunc(String s, int tType, int oType)
	{
		int i, n;
		String s1, s2, snew;
		ArrayList<String> to, from;
		
		switch (oType) {
			case ECHO: to = puncReplaceTo_ECHO; from = puncReplaceFrom_ECHO; break;
			case HTML: to = puncReplaceTo_HTML; from = puncReplaceFrom_HTML; break;
			case LATEX: if (tType == COMMENT) {
			              to = puncReplaceTo_LATEX; from = puncReplaceFrom_LATEX;
			            } else {
			              to = puncReplaceTo_LATEXMATHS; from = puncReplaceFrom_LATEXMATHS;
			            } break;
			case PRISMGUI: to = puncReplaceTo_PRISMGUI; from = puncReplaceFrom_PRISMGUI; break;
			default: to = puncReplaceTo_ECHO; from = puncReplaceFrom_ECHO; break;
		}
		
		snew = s;
		n = from.size();
		for (i = 0; i < n; i++) {
			s1 = from.get(i);
			s2 = to.get(i);
			snew = snew.replaceAll(s1, s2);
		}
		
		if (oType == LATEX && s.contains("\"")) {
			snew = snew.replaceAll("\"", (quoteAlternator==1)?"\\\\mbox{``}":"\\\\mbox{''}");
			quoteAlternator = 3 - quoteAlternator;
		}
		
		return snew;
	}
	
	// old version - worked on buffer not string; also: no regexes
	/*public static void replacePunc(StringBuffer sb, int oType)
	{
		int i, j, n;
		String s1, s2;
		
		n = puncReplaceFrom[oType].size();
		for (i = 0; i < n; i++) {
			s1 = puncReplaceFrom[oType].get(i);
			j = 0;
			while (j != -1) {
				j = sb.indexOf(s1, j);
				if (j != -1) {
					s2 = puncReplaceTo[oType].get(i);
					sb.replace(j, j+s1.length(), s2);
					j = j+s2.length();
				}
			}
		}
	}*/

	// main method - for command-line execution
	// arguments:
	// 0 = output type
	//     echo, html
	// 1 = filename
	//     if provided, argument is name of file to use, header/footer added
	//     if not, read from stdin, no header/footer added
	
	public static void main(String args[])
	{
		// Default setting
		String cssLoc = "prism.css";
		
		// Put arguments into a list, parsing any switches as we go
		ArrayList<String> argsList = new ArrayList<String>();
		int i = 0;
		while (i < args.length) {
			if (args[i].matches("-.*")) {
				String sw = args[i].substring(1);
				if (sw.equals("css")) {
					if (args.length < i + 2)  {
						System.out.print("Error: Missing argument for switch " + args[i]);
						System.exit(1);
					}
					cssLoc = args[i+1];
					i += 2;
				}
				else {
					System.out.print("Error: Unknown switch " + args[i]);
					System.exit(1);
				}
			} else {
				argsList.add(args[i]);
				i++;
			}
		}
		
		// Process non-switch arguments
		if (argsList.size() == 0) {
			System.out.println("Error: First argument must be output type");
			System.exit(1);
		}
		try {
			if (argsList.get(0).equals("echo")) {
				if (argsList.size() > 1) {
					System.out.print(echoFile(new File(argsList.get(1))));
				} else {
					System.out.print(echoFile(System.in));
				}
			} else if (argsList.get(0).equals("html")) {
				if (argsList.size() > 1) {
					System.out.print(fileToHtml(new File(argsList.get(1)), true, cssLoc));
				} else {
					System.out.print(fileToHtml(System.in, false, cssLoc));
				}
			} else if (argsList.get(0).equals("latex")) {
				if (argsList.size() > 1) {
					System.out.print(fileToLatex(new File(argsList.get(1)), true));
				} else {
					System.out.print(fileToLatex(System.in, false));
				}
			} else {
				System.out.println("Error: Type must be \"echo\", \"html\" or \"latex\"");
				System.exit(1);
			}
		}
		catch (FileNotFoundException e) {
			System.out.println("Error: Could not load file \""+ argsList.get(1) + "\"");
			System.exit(1);
		}
		catch (PrismLangException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}
}
