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

package userinterface;
import java.awt.*;
import prism.*;
import java.net.URL;

public class GUIPrismSplash extends Frame 
{
	private String filename;
	private MediaTracker mt;
	private Image image;
	
	public GUIPrismSplash(String filename) 
	{
		this.filename = filename;
	}
	
	public void display() 
	{
		mt = new MediaTracker(this);
		
		URL imageURL = GUIPrismSplash.class.getClassLoader().getResource(filename);
		if (imageURL == null) 
		{
			System.out.println("Warning: Failed to load icon file \"" + filename + "\"");
		}
		
		
		image = Toolkit.getDefaultToolkit().getImage(imageURL);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle frame = getBounds();
		setLocation((screen.width - frame.width)/2, (screen.height - frame.height)/2);
		
		mt.addImage(image, 0);
		try 
		{
			mt.waitForID(0);
		}
		catch(InterruptedException ie) 
		{
			System.out.println("Error in media tracker");
		}
		
		SplashWindow splashWindow = new SplashWindow(this,image);
	}
	
	//Thanks to http://www.javapractices.com/Topic149.cjp for the SplashWindow class
	private class SplashWindow extends Window 
	{
		SplashWindow(Frame aParent, Image aImage) 
		{
			super(aParent);
			fImage = aImage;
			setSize(fImage.getWidth(null), fImage.getHeight(null));
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			Rectangle window = getBounds();
			setLocation((screen.width - window.width) / 2, (screen.height - window.height) / 2);
			setVisible(true);
		}
		public void paint(Graphics graphics) 
		{
			if (fImage != null) 
			{
				graphics.drawImage(fImage,0,0,this);
				Font theFont = new Font ("monospaced", Font.BOLD, 10);
				graphics.setFont(theFont);
				int x = (int)(getBounds().width - theFont.getSize2D()*(Prism.getVersion().length()+1)*(5.0/8.0) - 10);
				int y = (int)(getBounds().height - theFont.getSize2D() - 10);
				graphics.drawString(Prism.getVersion() + " ", x, y);
			}
		}
		private Image fImage;
	}
}
