//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Charles Harley <cd.harley@talk21.com> (University of Edinburgh)
//	* Sebastian Vermehren <seb03@hotmail.com> (University of Edinburgh)
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

package userinterface.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.StyleContext;
import javax.swing.text.Utilities;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import prism.PrismLangException;
import parser.PrismSyntaxHighlighter;

/** Prism model editor kit for the text model editor. Defines the syntax
 * highlighting that the model editor should use.
 */
class PrismEditorKit extends DefaultEditorKit
{
	
	private PrismContext preferences;
	private GUIMultiModelHandler handler;
	
	/** Creates a new instance of the Prism editor kit.
	 * 
	 * @param handler The GUI handler for this component.
	 */
	public PrismEditorKit(GUIMultiModelHandler handler)
	{
		this.handler = handler;
	}

	public PrismContext getStylePreferences()
	{
		if (preferences == null)
		{
			preferences = new PrismContext(handler);
		}
		return preferences;
	}

	public void setStylePreferences(PrismContext preferences)
	{
		this.preferences = preferences;
	}

	public String getContentType()
	{
		return "text/prism";
	}

	public Document createDefaultDocument()
	{
		return new DefaultStyledDocument();
	}

	public final ViewFactory getViewFactory()
	{
		return getStylePreferences();
	}

}

class PrismContext extends StyleContext implements ViewFactory
{

	public static final String KEY_WORD_D = "Prism Keyword";
	public static final String NUMERIC_D = "Numeric";
	public static final String VARIABLE_D = "Variable";
	public static final String COMMENT_D = "Single Line Comment";
	
	private GUIMultiModelHandler handler;
	
	/** Creates a new instance of PrismContext.
	 * 
	 * @param handler The GUI handler for this component. 
	 */
	public PrismContext(GUIMultiModelHandler handler)
	{
		this.handler = handler;
	}

	public View create(Element element)
	{
		return new PrismView(element, handler);
	}

}

class PrismView extends PlainView
{
	
	static final Style PLAIN_S = new Style(Color.black, Font.PLAIN);
	
	private GUIMultiModelHandler handler;

	public PrismView(Element element, GUIMultiModelHandler handler)
	{
		super(element);
		this.handler = handler;
	}

	@Override
	protected float drawUnselectedText(Graphics2D g, float x, float y, int p0, int p1) throws BadLocationException
	{
		int stLine = p0;// findStartOfLine(p0, getDocument());
		int enLine = p1;// findEndOfLine(p1-1, getDocument());
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		try {
			g.setColor(Color.green);
			Document doc = getDocument();
			String s = doc.getText(stLine, enLine-stLine);
			Style[] styles = highlight(s, (p0-stLine), (p1-p0));
			String fname = handler.getPrismEditorFontFast().getName();
			int fsize = handler.getPrismEditorFontFast().getSize();

			for (int curr = 0; curr < styles.length; curr++) {
				Style c = styles[curr];
				g.setColor(c.c);
				g.setFont(new Font(fname, c.style, fsize));
				Segment segm = new Segment();// getLineBuffer();
				doc.getText(p0+curr, 1, segm);
				x = Utilities.drawTabbedText(segm, x, y, g, this, p0+curr);
			}
			g.setColor(Color.black);
			g.setFont(new Font(fname, Font.PLAIN, fsize));
		}
		catch(BadLocationException ex) {
			ex.printStackTrace();
		}
		return x;
	}

	@Override
	protected float drawSelectedText(Graphics2D g, float x, float y,int p0, int p1) throws BadLocationException
	{
		int stLine = p0;// findStartOfLine(p0, getDocument());
		int enLine = p1;// findEndOfLine(p1-1, getDocument());
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		try {
			g.setColor(Color.green);
			Document doc = getDocument();
			String s = doc.getText(stLine, enLine-stLine);
			Style[] styles = highlight(s, (p0-stLine), (p1-p0));
			String fname = handler.getPrismEditorFontFast().getName();
			int fsize = handler.getPrismEditorFontFast().getSize();
			for (int curr = 0; curr < styles.length; curr++) {
				Style c = styles[curr];
				g.setColor(c.c);
				g.setFont(new Font(fname, c.style, fsize));
				Segment segm = new Segment();// getLineBuffer();
				doc.getText(p0+curr, 1, segm);
				x = Utilities.drawTabbedText(segm, x, y, g, this, p0+curr);
			}
			g.setColor(Color.black);
			g.setFont(new Font(fname, Font.PLAIN, fsize));
		}
		catch(BadLocationException ex) {
			ex.printStackTrace();
		}
		return x;
	}

	private synchronized Style[] highlight(String s, int offset, int length)
	{
		int typeArray[];
		int i, n;
		if(!s.endsWith("\n"))
			s += "\n";
		// s = s.substring(0, s.length()-1);

		try
		{
			typeArray = PrismSyntaxHighlighter.lineForPrismGUI(s);
		}
		catch (PrismLangException e)
		{
			n = s.length();
			typeArray = new int[n];
			for (i = 0; i < n; i++)
				typeArray[i] = PrismSyntaxHighlighter.PUNCTUATION;
		}

		Style[] ret = new Style[length];
		for (i = 0; i < length; i++)
		{

			if(i+offset < typeArray.length)
			{
				switch (typeArray[i+offset])
				{
				case PrismSyntaxHighlighter.PUNCTUATION: ret[i] = PLAIN_S; break;
				case PrismSyntaxHighlighter.COMMENT: ret[i] = handler.getPrismEditorCommentFast(); break;
				case PrismSyntaxHighlighter.WHITESPACE: ret[i] = PLAIN_S; break;
				case PrismSyntaxHighlighter.KEYWORD: ret[i] = handler.getPrismEditorKeywordFast(); break;
				case PrismSyntaxHighlighter.NUMERIC: ret[i] = handler.getPrismEditorNumericFast(); break;
				case PrismSyntaxHighlighter.IDENTIFIER: ret[i] = handler.getPrismEditorVariableFast(); break;
				default: ret[i] = PLAIN_S; break;
				}
			}
			else ret[i] = PLAIN_S;

		}

		return ret;
	}

	// Legacy code, kept in just in case the original developers decide they need it.
	/*private synchronized int findStartOfLine(int p0, Document d)
	{
		int index = p0;
		String s = "";
		try
		{
			s = d.getText(index, 1);
		}
		catch(BadLocationException e)
		{
			return 0;
		}
		index--;
		if(!(!s.equals("\n") && index >= -1)) index--;// botch of the
		// century, an
		// alternative good
		// code
		while(!s.equals("\n") && index >= -1)
		{
			try
			{
				s = d.getText(index, 1);
			}
			catch(BadLocationException e)
			{
				return 0;
			}
			index--;
		}
		index+=2;
		return index;
	}

	private synchronized int findEndOfLine(int p1, Document d)
	{
		int index = p1;
		String s = "";
		try
		{
			s = d.getText(index, 1);
		}
		catch(BadLocationException e)
		{
			return d.getLength();
		}
		index++;
		while(!s.equals("\n") && index <= d.getLength())
		{
			try
			{
				s = d.getText(index, 1);
			}
			catch(BadLocationException e)
			{
				return d.getLength()-1;
			}
			index++;
		}
		index--;
		return index;
	}*/

}
