//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
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

package userinterface.log;

import java.io.*;
import javax.swing.*;
import javax.swing.text.BadLocationException;

import prism.*;

public class GUIWindowLog implements PrismLog
{
	// text area swing object where all text will be echoed
	private JTextArea textArea;
	// update thread
	private GUIWindowLogUpdater updater;
	// buffer
	private String buffer;
	// clear flag
	private boolean clearFlag;

	public GUIWindowLog()
	{
		buffer = "";
		clearFlag = false;
		textArea = null;
		updater = null;
	}

	public GUIWindowLog(JTextArea ta)
	{
		buffer = "";
		clearFlag = false;
		open(ta);
	}

	public void open(JTextArea ta)
	{
		textArea = ta;
		updater = new GUIWindowLogUpdater(this, textArea);
		updater.start();
	}

	public boolean ready()
	{
		return (textArea != null);
	}

	public void print(boolean b)
	{
		addToBuffer("" + b);
	}

	public void print(char c)
	{
		addToBuffer("" + c);
	}

	public void print(double d)
	{
		addToBuffer("" + d);
	}

	public void print(float f)
	{
		addToBuffer("" + f);
	}

	public void print(int i)
	{
		addToBuffer("" + i);
	}

	public void print(long l)
	{
		addToBuffer("" + l);
	}

	public void print(Object obj)
	{
		addToBuffer("" + obj);
	}

	public void print(String s)
	{
		addToBuffer(s);
	}

	public void println()
	{
		addToBuffer("\n");
	}

	public void println(boolean b)
	{
		addToBuffer("" + b + "\n");
	}

	public void println(char c)
	{
		addToBuffer("" + c + "\n");
	}

	public void println(double d)
	{
		addToBuffer("" + d + "\n");
	}

	public void println(float f)
	{
		addToBuffer("" + f + "\n");
	}

	public void println(int i)
	{
		addToBuffer("" + i + "\n");
	}

	public void println(long l)
	{
		addToBuffer("" + l + "\n");
	}

	public void println(Object obj)
	{
		addToBuffer("" + obj + "\n");
	}

	public void println(String s)
	{
		addToBuffer(s + "\n");
	}

	public int getFilePointer()
	{
		return -1;
	}

	public void flush()
	{
	}

	public void clear()
	{
		setClearFlag();
	}

	public void close()
	{
	}

	public int getMaxTextLength()
	{
		return updater.getMaxTextLength();
	}

	public void setMaxTextLength(int i)
	{
		updater.setMaxTextLength(i);
	}

	// add string to buffer
	
	public synchronized void addToBuffer(String s)
	{
		buffer += s;
	}

	// remove contents of buffer
	
	public synchronized String takeBuffer()
	{
		String s = buffer;
		buffer = "";
		return s;
	}

	// set clear flag
	
	public synchronized void setClearFlag()
	{
		clearFlag = true;
	}

	// get (and reset) clear flag
	
	public synchronized boolean getClearFlag()
	{
		boolean b = clearFlag;
		clearFlag = false;
		return b;
	}

	// thread which periodically checks for any pending input to log and adds it to window
	
	class GUIWindowLogUpdater extends Thread
	{
		private GUIWindowLog gwl;
		private JTextArea textArea;
		private String text;
		private int textLen = 0;
		private int maxTextLen = 10000;
		private int updateDelay = 1000;
		
		public GUIWindowLogUpdater(GUIWindowLog gwl, JTextArea textArea)
		{
			super(); 
			this.gwl = gwl;
			this.textArea = textArea;
			text = "";
		}

		public int getMaxTextLength()
		{
			return maxTextLen;
		}

		public void setMaxTextLength(int i)
		{
			maxTextLen  = i;
		}

		public void run()
		{
			String s;
			int newStart, i;
			
			while (true) {
			
				// sleep
				try { Thread.sleep(updateDelay); } catch (InterruptedException e) {}
				
				// clear log if asked to
				if (gwl.getClearFlag()) {
					// remove (but don't display) and input pending
					gwl.takeBuffer();
					// clear log
					text = "";
					textLen += 0;
					// update text area
					SwingUtilities.invokeLater(new SetWindowLogText(textArea, text));
				}
				
				// get any new input to log
				s = gwl.takeBuffer();
				// if there actually is any...
				if (s.length() > 0) {
					// add to text
					text += s;
					textLen += s.length();
					// truncate text if necessary
					if (textLen > maxTextLen) {
						// trim to max text length
						newStart = textLen - maxTextLen;
						// remove any partial line at start (i.e. cut to next new line)
						i = text.indexOf('\n', newStart);
						if (i != -1) newStart = i + 1;
						text = text.substring(newStart);
						textLen = text.length();
					}
					// update text area
					SwingUtilities.invokeLater(new SetWindowLogText(textArea, text));
				}
			}
		}
	}

	// inner class to actually write to the text area
	// must be done as a thread because code is called using invokeLater
	
	private class SetWindowLogText extends Thread
	{
		private JTextArea textArea;
		private String s;
		
		public SetWindowLogText(JTextArea textArea, String s)
		{
			super();
			this.textArea = textArea;
			this.s = s;
		}
		
		public void run()
		{
			try {
				// replace all current contents with string s
				textArea.replaceRange(s, 0, textArea.getLineEndOffset(textArea.getLineCount()-1));
			}
			catch (BadLocationException e) {}
		}
	}

	// receive notification of settings info
	
	public void notifySettings(PrismSettings settings)
	{
		textArea.setFont(settings.getFontColorPair(PrismSettings.LOG_FONT).f);
		//textArea.setSelectionColor(settings.getColor(PrismSettings.LOG_SELECTION_COLOUR));
		textArea.setBackground(settings.getColor(PrismSettings.LOG_BG_COLOUR));
		setMaxTextLength(settings.getInteger(PrismSettings.LOG_BUFFER_LENGTH));
	}
}

//------------------------------------------------------------------------------
