//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Charles Harley <cd.harley@talk21.com> (University of Edinburgh)
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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.StyleContext;
import javax.swing.text.Utilities;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/** Pepa model editor kit for the text model editor. Defines the syntax
 * highlighting that the model editor should use.
 */
class PepaEditorKit extends DefaultEditorKit
{
	
	private PepaContext preferences;
	private GUIMultiModelHandler handler;
	
	/** Creates a new instance of the Pepa editor kit.
	 * 
	 * @param handler The GUI handler for this component.
	 */
	public PepaEditorKit(GUIMultiModelHandler handler)
	{
		this.handler = handler;
	}
	
	public PepaContext getStylePreferences()
	{
		if (preferences == null)
		{
			preferences = new PepaContext(handler);
		}
		return preferences;
	}
	
	public void setStylePreferences(PepaContext prefs)
	{
		preferences = prefs;
	}
	
	public String getContentType()
	{
		return "text/pepa";
	}
	
	public Document createDefaultDocument()
	{
		return new PlainDocument();
	}
	
	public final ViewFactory getViewFactory()
	{
		return getStylePreferences();
	}
	
}

class PepaContext extends StyleContext implements ViewFactory
{

	public static final String COMMENT_D = "Single Line Comment";
	
	private GUIMultiModelHandler handler;
	
	/** Creates a new instance of PepaContext.
	 * 
	 * @param handler The GUI handler for this component. 
	 */
	public PepaContext(GUIMultiModelHandler handler)
	{
		this.handler = handler;
	}
	
	public View create(Element elem)
	{
		return new PepaView(elem, handler);
	}
	
}

class PepaView extends PlainView
{
	
	static final userinterface.model.Style PLAIN_S = new userinterface.model.Style(Color.black, Font.PLAIN);
	private Matcher match;
	private Pattern pattern;
	private GUIMultiModelHandler handler;
	
	public PepaView(Element elem, GUIMultiModelHandler handler)
	{
		super(elem);
		this.handler = handler;
		pattern = Pattern.compile("%.*");
	}
	
	protected int drawUnselectedText(Graphics g, int x, int y, int p0, int p1) throws BadLocationException
	{
		int stLine = findStartOfLine(p0, getDocument());
		int enLine = findEndOfLine(p1, getDocument());

		if (g instanceof Graphics2D) {
			Graphics2D g2d = (Graphics2D)g;
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
        
		try
		{
			g.setColor(Color.green);
			Document doc = getDocument();
			Segment segment = getLineBuffer();
			
			
			//String s = doc.getText(p0, p1-p0);
			String s = doc.getText(stLine, enLine-stLine);
			userinterface.model.Style[] styles = highlight(s, (p0-stLine), (p1-p0));
			int currStart = 0;
			int currEnd = 0;
			Color last = null;
			String fname = handler.getPepaEditorFontFast().getName();
			int fsize = handler.getPepaEditorFontFast().getSize();
			
			for(int curr = 0; curr < styles.length; curr++)
			{
				
				userinterface.model.Style c = styles[curr];
				
				g.setColor(c.c);
				g.setFont(new Font(fname, c.style, fsize));
				Segment segm = getLineBuffer();
				doc.getText(p0+curr, 1, segm);
				x = Utilities.drawTabbedText(segm, x, y, g, this, p0+curr);
				
			}
			g.setColor(Color.black);
			g.setFont(new Font(fname, Font.PLAIN, fsize));
		}
		catch(BadLocationException ex)
		{
			//System.out.println("ex = "+ex);
			//ex.printStackTrace();
		}
		return x;
	}
	
	protected int drawSelectedText(Graphics g, int x, int y, int p0, int p1) throws BadLocationException
	{
		int stLine = findStartOfLine(p0, getDocument());
		int enLine = findEndOfLine(p1, getDocument());

		if (g instanceof Graphics2D) {
			Graphics2D g2d = (Graphics2D)g;
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
        
		try
		{
			g.setColor(Color.green);
			Document doc = getDocument();
			Segment segment = getLineBuffer();
			
			
			//String s = doc.getText(p0, p1-p0);
			String s = doc.getText(stLine, enLine-stLine);
			userinterface.model.Style[] styles = highlight(s, (p0-stLine), (p1-p0));
			int currStart = 0;
			int currEnd = 0;
			Color last = null;
			String fname = handler.getPepaEditorFontFast().getName();
			int fsize = handler.getPepaEditorFontFast().getSize();
			
			for(int curr = 0; curr < styles.length; curr++)
			{
				
				userinterface.model.Style c = styles[curr];
				
				g.setColor(c.c);
				g.setFont(new Font(fname, c.style, fsize));
				Segment segm = getLineBuffer();
				doc.getText(p0+curr, 1, segm);
				x = Utilities.drawTabbedText(segm, x, y, g, this, p0+curr);
				
			}
			g.setColor(Color.black);
			g.setFont(new Font(fname, Font.PLAIN, fsize));
		}
		catch(BadLocationException ex)
		{
			//System.out.println("ex = "+ex);
			//ex.printStackTrace();
		}
		return x;
	}
	
	private synchronized userinterface.model.Style[] highlight(String s, int offset, int length)
	{
		userinterface.model.Style[] styles = new userinterface.model.Style[s.length()];
		for(int i = 0; i < styles.length; i++)
			styles[i] = PLAIN_S;
		
		match = pattern.matcher(s);
		
		
		int starter = 0;
		int end = 0;
		boolean contain = match.find();
		while(contain)
		{
			starter = match.start();
			end = match.end();
			
			for(int j = starter; j < end; j++)
			{
				styles[j] = handler.getPepaEditorCommentFast();
			}
			contain = match.find();
		}
		
		//System.out.println("styles.length = "+styles.length);
		//System.out.println("ret.length = "+length);
		//System.out.println("offset = "+offset);
		userinterface.model.Style[]ret = new userinterface.model.Style[length];
		for(int i = 0; i < ret.length; i++)
		{
			ret[i] = styles[i+offset];
		}
		
		return ret;
	}
	
	
	private synchronized int findStartOfLine(int p0, Document d)
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
		if(!(!s.equals("\n") && index >= -1)) index--;//botch of the century, an alternative good code
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
	}
	
}
