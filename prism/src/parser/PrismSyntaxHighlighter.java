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

package parser;

import java.util.ArrayList;
import java.io.*;
import prism.Prism;

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
	private static final ArrayList puncReplaceTo_ECHO , puncReplaceFrom_ECHO;
	private static final ArrayList puncReplaceTo_HTML, puncReplaceFrom_HTML;
	private static final ArrayList puncReplaceTo_LATEX, puncReplaceFrom_LATEX;
	private static final ArrayList puncReplaceTo_LATEXMATHS, puncReplaceFrom_LATEXMATHS;
	private static final ArrayList puncReplaceTo_PRISMGUI, puncReplaceFrom_PRISMGUI;
	
	static {
		puncReplaceFrom_ECHO = new ArrayList(); puncReplaceTo_ECHO = new ArrayList();
		puncReplaceFrom_HTML = new ArrayList(); puncReplaceTo_HTML = new ArrayList();
		puncReplaceFrom_HTML.add("&"); puncReplaceTo_HTML.add("&amp;");
		puncReplaceFrom_HTML.add("<"); puncReplaceTo_HTML.add("&lt;");
		puncReplaceFrom_HTML.add(">"); puncReplaceTo_HTML.add("&gt;");
		puncReplaceFrom_LATEX = new ArrayList(); puncReplaceTo_LATEX = new ArrayList();
		puncReplaceFrom_LATEX.add("&"); puncReplaceTo_LATEX.add("\\\\&");
		puncReplaceFrom_LATEX.add("_"); puncReplaceTo_LATEX.add("\\\\_");
		puncReplaceFrom_LATEX.add("%"); puncReplaceTo_LATEX.add("\\\\%");
		puncReplaceFrom_LATEX.add(">="); puncReplaceTo_LATEX.add("\\$\\\\geq\\$");
		puncReplaceFrom_LATEX.add("<="); puncReplaceTo_LATEX.add("\\$\\\\leq\\$");
		puncReplaceFrom_LATEX.add("->"); puncReplaceTo_LATEX.add("\\$\\\\rightarrow\\$");
		puncReplaceFrom_LATEX.add("=>"); puncReplaceTo_LATEX.add("\\$\\\\Rightarrow\\$");
		puncReplaceFrom_LATEX.add("\\{"); puncReplaceTo_LATEX.add("\\\\{");
		puncReplaceFrom_LATEX.add("\\}"); puncReplaceTo_LATEX.add("\\\\}");
		puncReplaceFrom_LATEXMATHS = new ArrayList(); puncReplaceTo_LATEXMATHS = new ArrayList();
		puncReplaceFrom_LATEXMATHS.add("&"); puncReplaceTo_LATEXMATHS.add("\\\\&");
		puncReplaceFrom_LATEXMATHS.add("_"); puncReplaceTo_LATEXMATHS.add("\\\\_");
		puncReplaceFrom_LATEXMATHS.add("%"); puncReplaceTo_LATEXMATHS.add("\\\\%");
		puncReplaceFrom_LATEXMATHS.add(">="); puncReplaceTo_LATEXMATHS.add("\\\\geq");
		puncReplaceFrom_LATEXMATHS.add("<="); puncReplaceTo_LATEXMATHS.add("\\\\leq");
		puncReplaceFrom_LATEXMATHS.add("->"); puncReplaceTo_LATEXMATHS.add("\\\\rightarrow");
		puncReplaceFrom_LATEXMATHS.add("=>"); puncReplaceTo_LATEXMATHS.add("\\\\Rightarrow");
		puncReplaceFrom_LATEXMATHS.add("\\{"); puncReplaceTo_LATEXMATHS.add("\\\\{");
		puncReplaceFrom_LATEXMATHS.add("\\}"); puncReplaceTo_LATEXMATHS.add("\\\\}");
		puncReplaceFrom_PRISMGUI = new ArrayList(); puncReplaceTo_PRISMGUI = new ArrayList();
	}
	
	// resulting output
	private static StringBuffer resStringBuffer;;
	private static boolean resNewLine;
	private static boolean resStart;
	private static int resTypeArray[];
	private static int resCharCount;
	
	// public methods
	
	public static String echoFile(File file) throws FileNotFoundException, ParseException
	{
		resStringBuffer = new StringBuffer();
		highlight(new FileInputStream(file), ECHO);
		return resStringBuffer.toString();
	}
	
	public static String echoFile(InputStream stream) throws ParseException
	{
		resStringBuffer = new StringBuffer();
		highlight(stream, ECHO);
		return resStringBuffer.toString();
	}
	
	public static String lineToHtml(String line) throws ParseException
	{
		resStringBuffer = new StringBuffer();
		highlight(new ByteArrayInputStream(line.getBytes()), HTML);
		return resStringBuffer.toString();
	}
	
	public static String fileToHtml(File file, boolean hf) throws FileNotFoundException, ParseException
	{
		resStringBuffer = new StringBuffer();
		if (hf) resStringBuffer.append(htmlFileHeader(file.getName()));
		highlight(new FileInputStream(file), HTML);
		if (hf) resStringBuffer.append(htmlFileFooter()); 
		return resStringBuffer.toString();
	}
	
	public static String fileToHtml(InputStream stream, boolean hf) throws ParseException
	{
		resStringBuffer = new StringBuffer();
		if (hf) resStringBuffer.append(htmlFileHeader("PRISM Code")); 
		highlight(stream, HTML);
		if (hf) resStringBuffer.append(htmlFileFooter()); 
		return resStringBuffer.toString();
	}

	public static String fileToLatex(File file, boolean hf) throws FileNotFoundException, ParseException
	{
		resStringBuffer = new StringBuffer();
		resNewLine = true;
		resStart = true;
		if (hf) resStringBuffer.append(latexFileHeader(file.getName()));
		highlight(new FileInputStream(file), LATEX);
		if (hf) resStringBuffer.append(latexFileFooter()); 
		return resStringBuffer.toString();
	}
	
	public static String fileToLatex(InputStream stream, boolean hf) throws ParseException
	{
		resStringBuffer = new StringBuffer();
		resNewLine = true;
		resStart = true;
		if (hf) resStringBuffer.append(latexFileHeader("PRISM Code")); 
		highlight(stream, LATEX);
		if (hf) resStringBuffer.append(latexFileFooter()); 
		return resStringBuffer.toString();
	}
	
	public static int[] lineForPrismGUI(String line) throws ParseException
	{
		resTypeArray = new int[line.length()];
		resCharCount = 0;
		highlight(new ByteArrayInputStream(line.getBytes()), PRISMGUI);
		return resTypeArray;
	}

	// generate file headers/footers
	
	private static String htmlFileHeader(String title)
	{
		String s = "";
		
		s += "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 //EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" + "\n";
		
		s += "<html>" + "\n";
		s += "<head>" + "\n";
		s += "<title>" + "\n";
		s += title + "\n";
		s += "</title>" + "\n";
		s += "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" + "\n";
		s += "<!-- Style sheet \"prism.css\" can be found in the \"etc\" directory of the PRISM distribution -->" + "\n";
		s += "<link type=\"text/css\" rel=\"stylesheet\" href=\"prism.css\">" + "\n";
		s += "</head>" + "\n";
		s += "<body text=\"#000000\" bgcolor=\"#ffffff\" link=\"0000ff\" alink=\"ff0000\" vlink=\"000099\">" + "\n";
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
		
		s += "\\documentclass{article}" + "\n";
		s += "\\begin{document}" + "\n";
		s += "\\input{prism.tex} % Input file \"prism.tex\" can be found in the \"etc\" directory of the PRISM distribution" + "\n";
		s += "\\scriptsize" + "\n";
		s += "\\noindent" + "\n";
		
		return s;
	}
	
	private static String latexFileFooter()
	{
		String s = "";
		
		s += "\\end{document}" + "\n";
		
		return s;
	}

	// multi-purpose highlighting code
	
	public static void highlight(InputStream stream, int oType) throws ParseException
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
				try {
					first = t = tokenManager.getNextToken();
					while (t != null && t.kind != PrismParserConstants.EOF) {
						t.next = tokenManager.getNextToken();
						t = t.next;
					}
				}
				catch (TokenMgrError e) {
					throw new ParseException(e.getMessage());
				}
				
				// do a first pass through the tokens to do some preprocessing
				// namely, sort out properties labels
				/*t = first;
				while (t != null && t.kind != PrismParserConstants.EOF) {
					if (t.kind == PrismParserConstants.DQUOTE) {
						// todo (can't remember what was wrong with properties labels anyway)
					}
					t = t.next;
				}*/
				
				// go through tokens to do syntax highlighting
				t = first;
				while (!done) {
					// see if we're at the end
					if (t == null) { done = true; continue; }
					if (t.kind == PrismParserConstants.EOF) { done = true; }
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
			throw new ParseException("Concurrency error in parser");
		}
	}

	private static void output(String s, int tType, int oType)
	{
		int i, n;
		
		// deal with new lines for latex
		if (oType == LATEX) if (resNewLine) {
			if (!resStart) {
				resStringBuffer.append("$} \\\\\n");
			}
			else {
				resStart = false;
			}
			if (tType != EOF) resStringBuffer.append("\\mbox{$");
			resNewLine = false;
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
			case LATEX: resStringBuffer.append("\\prismcomment{"+s.substring(0,s.length()-1)+"}"); resNewLine = true; break;
			case PRISMGUI: n = s.length(); for (i=0;i<n;i++) resTypeArray[resCharCount++]=COMMENT; break;
			}
			break;
		
		case WHITESPACE:
			// ignore carriage returns
			if ("\r".equals(s)) break;
			switch(oType) {
			case ECHO: resStringBuffer.append(s); break;
			case HTML: resStringBuffer.append(s); break;
			case LATEX: if ("\n".equals(s)) resNewLine = true;
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
		int i, j, n;
		String s1, s2, snew;
		ArrayList to, from;
		
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
			s1 = (String)from.get(i);
			s2 = (String)to.get(i);
			snew = snew.replaceAll(s1, s2);
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
			s1 = (String)puncReplaceFrom[oType].get(i);
			j = 0;
			while (j != -1) {
				j = sb.indexOf(s1, j);
				if (j != -1) {
					s2 = (String)puncReplaceTo[oType].get(i);
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
		int type;
		
		if (args.length == 0) {
			System.out.println("Error: First argument must be output type");
			System.exit(1);
		}
		try {
			if (args[0].equals("echo")) {
				if (args.length > 1) {
					System.out.print(echoFile(new File(args[1])));
				} else {
					System.out.print(echoFile(System.in));
				}
			} else if (args[0].equals("html")) {
				if (args.length > 1) {
					System.out.print(fileToHtml(new File(args[1]), true));
				} else {
					System.out.print(fileToHtml(System.in, false));
				}
			} else if (args[0].equals("latex")) {
				if (args.length > 1) {
					System.out.print(fileToLatex(new File(args[1]), true));
				} else {
					System.out.print(fileToLatex(System.in, false));
				}
			} else {
				System.out.println("Error: Type must be \"echo\", \"html\" or \"latex\"");
				System.exit(1);
			}
		}
		catch (FileNotFoundException e) {
			System.out.println("Error: Could not load file \""+ args[1] + "\"");
			System.exit(1);
		}
		catch (ParseException e) {
			System.out.println("Error: " + e.getShortMessage());
			System.exit(1);
		}
	}
}
