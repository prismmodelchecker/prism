//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
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

package chart;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.awt.event.*;
import java.util.*;
import javax.print.*;
import javax.print.attribute.*;
import java.awt.print.*;
import java.io.*;
import java.text.*;
import javax.imageio.*;
import java.awt.image.*;
import settings.*;

/** This class extends JPanel to be the view of the graph model.  Its primary method
 * is its paint(Graphics) method which overrides JPanel paint method.  According to
 * the data stored in the model, all calculations about screen positioning are done
 * in this class.
 * <p>
 * This class also provides access for printing using its print method.
 * <p>
 * This class also provides the methods for handling tooltips when a user hovers
 * over a point on the graph.  This is setup using an array of the inner class of HotSpot objects on the
 * screen which are used to detect if the mouse is hovering over a point.
 */

public class MultiGraphView extends javax.swing.JPanel implements Observer, Printable, MouseListener, MouseMotionListener, KeyListener
{
	//Attributes
	
	private MultiGraphModel theModel;
	private ArrayList hotSpots;
	private String currentTip;
	
	//Constants
	
	private static int BORDERSIZE = 100;
	
	//Constructor
	
	/** Sets up a new MultiGraphView with a given MultiGraphModel.  It sets up a new
	 * empty array of hotspots.
	 */
	public MultiGraphView(MultiGraphModel theModel)
	{
		super(false);
		this.theModel = theModel;
		hotSpots = new ArrayList();
		currentTip = null;
		this.setToolTipText("Graph");
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		
	}
	
	//Access methods
	
	/** Overrides JPanel's getToolTipLocation(MouseEvent) method.  According to a given mouse event, this method returns the position that a
	 * tooltip should be positioned relative to this mouse event,
	 */
	public Point getToolTipLocation(MouseEvent e)
	{
		return new Point(e.getX()+10, e.getY()+10);
	}
	
	/** This overrides the getToolTipText(MouseEvent) method in JPanel by providing text
	 * according to the location of the mouse on the panel.  This method works out if
	 * the mouse is over a HotSpot and returns that HotSpot's statistics, or null if
	 * the mouse is not over a HotSpot.
	 */
	public String getToolTipText(MouseEvent e)
	{
		if(isHotArea(e.getX(), e.getY()))
		{
			return currentTip;
		}
		else
		{
			return null;
		}
	}
	
	/** Access method which calculates the width of the legend according to the values
	 * which are stored in it and returns it.
	 * @param g2 the Graphics2D of the current graphics environment is needed to calculate such
	 * factors as zoom, text anti-aliasing etc... which all might have an effect on the
	 * size.
	 * @return
	 */
	public double getLegendWidth(Graphics2D g2)
	{
		double maxStr = 0.0;
		
		g2.setFont(theModel.getLegendFont());
		
		for(int i = 0; i < theModel.getNoGraphs(); i++) // find the longest string in terms of size
		{
			GraphList gr = theModel.getGraphPoints(i);
			double stringWidth = getStringWidth(gr.getGraphTitle(), g2);
			if (stringWidth > maxStr) maxStr = stringWidth;
		}
		//Gap of 5 pixels either size
		//maxStr+=10;
		//25 pixels for coloured line
		//maxStr+=25;
		//Gap of 5 pixels for space between line and text
		//maxStr+=5
		maxStr+=40;
		return maxStr;
	}
	
	public double getTitleHeight(Graphics2D g2)
	{
		StringTokenizer title = new StringTokenizer(theModel.getGraphTitle(), "\n");
		int number = title.countTokens();
		if (number == 0) return 0.0;
		g2.setFont(theModel.getTitleFont());
		String line = title.nextToken();
		double stringHeight = getStringHeight(line, g2);
		
		return number*(3+stringHeight);
	}
	
	/** Access method which calculates and returns the height of the legend according to
	 * the data stored in it.
	 */
	public double getLegendHeight()
	{
		double maxHeight = 0.0;
		//Gap at top
		maxHeight+=5;
		//Each line plus a small gap
		maxHeight+=(theModel.getNoGraphs() * (theModel.getLegendFont().getSize() +8));
		//Gap at bottom
		maxHeight+=5;
		return maxHeight;
	}
	
	/** Method returns a flag stating whether the given x and y co-ordinates lie within
	 * a HotSpot area.
	 */
	public boolean isHotArea(int x, int y)
	{
		Rectangle2D.Double mouse = new Rectangle2D.Double(x,y,1,1);
		boolean test = false;
		for(int i = 0; i < hotSpots.size(); i++)
		{
			if(mouse.intersects((HotSpot)hotSpots.get(i)))
			{
				test = true;
				currentTip = ((HotSpot)hotSpots.get(i)).getText();
				break;
			}
		}
		return test;
	}
	
	/** Returns a clone of this object.  Used for printing purposes. */
	public Object clone()
	{
		return new MultiGraphView(theModel);
	}
	
	
	//Update Methods
	
	/** Needed to implement the Observer interface.  Simply calls the repaint() method.
	 * @param obs not important, but needed to implement the interface
	 * @param o not important, but needed to implement the interface
	 */
	public void update(Observable obs, Object o)
	{
		repaint();
	}
	
	/** Method to print this view to a given file
	 * @param filename the filename of the postscript file to save to.
	 * @deprecated It is recommended that if it is desirable to have a postscript printout, that
	 * the Print to File option is chosen in the print dialog.  This is much more
	 * efficient that this method.
	 */
	public void printPostScript(String filename)
	{
		try
		{
			DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;
			String mimeType = "application/postscript";
			StreamPrintServiceFactory[] factories = StreamPrintServiceFactory.lookupStreamPrintServiceFactories(flavor, mimeType);
			FileOutputStream out = new FileOutputStream(filename);
			if(factories.length == 0) return;
			StreamPrintService service = factories[0].getPrintService(out);
			Doc doc = new SimpleDoc(this, flavor, null);
			DocPrintJob job = service.createPrintJob();
			job.print(doc, new HashPrintRequestAttributeSet());
		}
		catch(FileNotFoundException ex)
		{
			JOptionPane.showMessageDialog(this,ex);
		}
		catch(PrintException ex)
		{
			JOptionPane.showMessageDialog(this,ex);
		}
	}
	/** Method required to print this page.
	 * @param g
	 * @param pf
	 * @param page
	 * @throws PrinterException
	 * @return
	 */
	public int print(Graphics g, PageFormat pf, int page) throws PrinterException
	{
		if(page>=1) return Printable.NO_SUCH_PAGE;
		Graphics2D g2 = (Graphics2D)g;
		setSize(new Dimension((int)(pf.getWidth()-(pf.getImageableX()*2)), (int)(pf.getHeight()-(pf.getImageableY()*2))));
		g2.translate(pf.getImageableX()+2, pf.getImageableY()+2);
		paint(g2);
		return Printable.PAGE_EXISTS;
	}
	
	/** Method which performs all of the drawing functions of the chart. */
	public void paint(Graphics g)
	{
		super.paint(g);
		//System.out.println("PAINTING!!!!!!!!");
		this.requestFocusInWindow();
		Graphics2D g2 = (Graphics2D)g;
		/*if(theModel.isAutoBorder())
		{
		    try
		    {
				//theModel.setBordersUp(theModel.getLegendPosition(), this.getLegendWidth(g2), this.getLegendHeight(), this.getTitleHeight(g2));
		    }
		    catch(SettingException e)
		    {//do nothgin
		    }
		}*/
		double borderSizeLeft = theModel.getBorderSizeLeft();
		double borderSizeRight = theModel.getBorderSizeRight();
		//		int borderSizeTop = theModel.getBorderSizeTop();
		//		int borderSizeBottom = theModel.getBorderSizeBottom();
		HorizontalGraphBorder topBorder = theModel.getTopBorder();
		HorizontalGraphBorder bottomBorder = theModel.getBottomBorder();
		
		
		
		//Set up Graphics 2D object for rendering nicely
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		Font norm = g2.getFont();
		int frameWidth = getWidth();
		int frameHeight = getHeight();
		double graphWidth = frameWidth-(borderSizeLeft+borderSizeRight);
		//		double graphHeight = frameHeight-(borderSizeTop+borderSizeBottom);
		double graphHeight = frameHeight-(topBorder.getOffset()+bottomBorder.getOffset());
		double xFactor, yFactor;
		int baseX = (int)theModel.getLogarithmicBaseX();
		int baseY = (int)theModel.getLogarithmicBaseY();
		double maxX = theModel.getMaxX();
		double maxY = theModel.getMaxY();
		double minX = theModel.getMinX();
		double minY = theModel.getMinY();
		//System.out.println("x("+minX+","+maxX+") y("+minY+","+maxY+")");
		double maxXLog = theModel.getXAxis().getMaximumPower().getIntegerValue();//Math.ceil(theModel.log(baseX, maxX));
		double maxYLog = theModel.getYAxis().getMaximumPower().getIntegerValue();//Math.ceil(theModel.log(baseY, maxY));
		hotSpots = new ArrayList();
		//System.out.println("No tooltips");
		if(!theModel.isLogarithmicX())
		{
			xFactor = ((double)graphWidth)/(maxX-minX);
		}
		else
		{
			xFactor = ((double)graphWidth)/(maxXLog-(MultiGraphModel.log(baseX, theModel.getMinimumX())));
		}
		if(!theModel.isLogarithmicY())
		{
			yFactor = ((double)graphHeight)/(maxY-minY);
		}
		else
		{
			yFactor = ((double)graphHeight)/(maxYLog-(MultiGraphModel.log(baseY, theModel.getMinumumY())));
		}
		
		double yAxisOffset;
		double xAxisOffset;
		
		if(minY <= 0)
		{
			yAxisOffset = (minY*yFactor);
		}
		else
		{
			yAxisOffset = 0;
		}
		
		
		
		if(minX <= 0)
		{
			xAxisOffset = (minX*xFactor);
		}
		else
		{
			xAxisOffset = 0;
		}
		
		if(theModel.isLogarithmicX())
		{
			xAxisOffset = 0;
		}
		if(theModel.isLogarithmicY())
		{
			yAxisOffset = 0;
		}
		
		AffineTransform tx = g2.getTransform();
		g2.setTransform(tx);
		
		//Blank Content
		g2.setColor(Color.white);
		//System.out.println("Painting: This is the transform: "+g2.getTransform().getTranslateX()+", "+g2.getTransform().getTranslateY()+".");
		g2.fillRect(0,0,frameWidth,frameHeight);
		g2.setColor(Color.black);
		
		borderSizeLeft = theModel.getBorderSizeLeft();
		borderSizeRight = theModel.getBorderSizeRight();
		
		
		//Draw minor gridlines
		//Draw X Gridlines and labels
		if(!theModel.isLogarithmicX())
		{
			//minor
			
			g2.setColor(theModel.getXMinorColour());
			Color xMinorColour = g2.getColor();
			//System.out.println ("XMinor in view: Red:"+xMinorColour.getRed ()+" Green:"+xMinorColour.getGreen ()+" Blue:"+xMinorColour.getBlue ()+".");
			double minorXInterval = theModel.getMinorXInterval();
			Line2D l;
			double xPos = 0.0;
			
			for(xPos = 0; xPos<=graphWidth; xPos=xPos+Math.abs(minorXInterval*xFactor))
			{
				l = new Line2D.Double(borderSizeLeft+xPos, topBorder.getOffset(), borderSizeLeft+xPos, topBorder.getOffset()+graphHeight);
				if(theModel.isXShowMinor())g2.draw(l);
			}
		}
		else //if discrete no logarithmic x scale
		{
			g2.setColor(theModel.getXMinorColour());
			
			
			Line2D l;
			
			for(double i = MultiGraphModel.log(baseX, theModel.getMinimumX()); Math.round(i) < Math.round(maxXLog); i++)
			{
				
				
				for(double j = Math.pow(baseX, i); j<=Math.pow(baseX, i+1); j=j+Math.pow(baseX, i))
				{
					
					double offset = theModel.log(baseX, j) * xFactor;
					offset -= theModel.log(baseX, theModel.getMinimumX())*xFactor;
					g2.setColor(theModel.getXMinorColour());
					
					l = new Line2D.Double(borderSizeLeft+offset, topBorder.getOffset(), borderSizeLeft+offset, topBorder.getOffset()+graphHeight);
					if(theModel.isXShowMinor())g2.draw(l);
				}
			}
			
		}
		
		
		//Draw Y Gridlines and labels
		
		if(!theModel.isLogarithmicY())
		{
			g2.setTransform(tx);
			
			//minor
			g2.setColor(theModel.getYMinorColour());
			
			Line2D l;
			double minorYInterval = theModel.getMinorYInterval();
			double yPos = 0.0;
			for(yPos = 0; yPos<=graphHeight; yPos=yPos+Math.abs(minorYInterval*yFactor))
			{
				
				l = new Line2D.Double(borderSizeLeft, frameHeight-bottomBorder.getOffset()-yPos, borderSizeLeft+graphWidth, frameHeight-bottomBorder.getOffset()-yPos);
				
				if(theModel.isYShowMinor())g2.draw(l);
			}
			
		}
		else
		{
			g2.setTransform(tx);
			//Draw GridLine
			g2.setColor(theModel.getYMinorColour());
			
			Line2D l;
			//draw Y lines
			for(double i = MultiGraphModel.log(baseY, theModel.getMinumumY()); Math.round(i) < Math.round(maxYLog); i++)
			{
				
				
				for(double j = Math.pow(baseY, i); j<=Math.pow(baseY, i+1); j=j+Math.pow(baseY, i))
				{
					double offset = theModel.log(baseY, j) * yFactor;
					offset -= theModel.log(baseY, theModel.getMinumumY())*yFactor;
					g2.setColor(theModel.getYMinorColour());
					l = new Line2D.Double(borderSizeLeft, frameHeight-bottomBorder.getOffset()-offset, borderSizeLeft+graphWidth, frameHeight-bottomBorder.getOffset()-offset);
					if(theModel.isYShowMinor())g2.draw(l);
				}
			}
			
		}
		//Draw major gridlines
		
		
		
		//Draw X Gridlines
		if(!theModel.isLogarithmicX())
		{
			//minor
			
			g2.setColor(theModel.getXMinorColour());
			Color xMinorColour = g2.getColor();
			//System.out.println ("XMinor in view: Red:"+xMinorColour.getRed ()+" Green:"+xMinorColour.getGreen ()+" Blue:"+xMinorColour.getBlue ()+".");
			
			Line2D l;
			double xPos = 0.0;
			
			
			//major
			g2.setColor(theModel.getXMajorColour());
			double majorXInterval = theModel.getMajorXInterval();
			
			xPos = 0.0;
			
			for(xPos = 0; xPos<=graphWidth+0.00001; xPos=xPos+Math.abs(majorXInterval*xFactor))
			{
				g2.setColor(theModel.getXMajorColour());
				l = new Line2D.Double(borderSizeLeft+xPos, topBorder.getOffset(), borderSizeLeft+xPos, topBorder.getOffset()+graphHeight);
				if(theModel.isXShowMajor())g2.draw(l);
				
				g2.setColor(Color.black);
				l = new Line2D.Double(borderSizeLeft+xPos, frameHeight-bottomBorder.getOffset()+yAxisOffset, borderSizeLeft+xPos, frameHeight-bottomBorder.getOffset()+yAxisOffset+3);
				g2.draw(l);
			}
			
		}
		else //if discrete no logarithmic x scale
		{
			g2.setColor(theModel.getXMinorColour());
			
			
			Line2D l;
			
			
			for(double i = MultiGraphModel.log(baseX, theModel.getMinimumX()); Math.round(i) < Math.round(maxXLog); i++)
			{
				double set = (i+1) * xFactor;
				set -= theModel.log(baseX, theModel.getMinimumX())*xFactor;
				g2.setColor(theModel.getXMajorColour());
				
				l = new Line2D.Double(borderSizeLeft+set, topBorder.getOffset(), borderSizeLeft+set, topBorder.getOffset()+graphHeight);
				if(theModel.isXShowMajor())g2.draw(l);
				g2.setColor(Color.black);
				l = new Line2D.Double(borderSizeLeft+set, frameHeight-bottomBorder.getOffset()+yAxisOffset, borderSizeLeft+set, frameHeight-bottomBorder.getOffset()+yAxisOffset+3);
				g2.draw(l);
			}
		   
		}
		
		
		//Draw Y Gridlines
		
		if(!theModel.isLogarithmicY())
		{
			g2.setTransform(tx);
			
			//minor
			g2.setColor(theModel.getYMinorColour());
			
			Line2D l;
			
			double yPos = 0.0;
			//major
			g2.setColor(theModel.getYMajorColour());
			double majorYInterval = theModel.getMajorYInterval();
			yPos = 0.0;
			for(yPos = 0; yPos<=graphHeight+0.0001; yPos=yPos+Math.abs(majorYInterval*yFactor))
			{
				g2.setColor(theModel.getYMajorColour());
				l = new Line2D.Double(borderSizeLeft, frameHeight-bottomBorder.getOffset()-yPos, borderSizeLeft+graphWidth, frameHeight-bottomBorder.getOffset()-yPos);
				if(theModel.isYShowMajor())g2.draw(l);
				g2.setColor(Color.black);
				l = new Line2D.Double((borderSizeLeft-xAxisOffset)-3, frameHeight-bottomBorder.getOffset()-yPos, (borderSizeLeft-xAxisOffset), frameHeight-bottomBorder.getOffset()-yPos);
				g2.draw(l);
			}
			
			
		}
		else
		{
			g2.setTransform(tx);
			//Draw GridLine
			g2.setColor(theModel.getYMinorColour());
			
			Line2D l;
			//draw Y lines
			
			for(double i = MultiGraphModel.log(baseY, theModel.getMinumumY()); Math.round(i) < Math.round(maxYLog); i++)
			{
				double set = (i+1) * yFactor;
				set -= theModel.log(baseY, theModel.getMinumumY())*yFactor;
				g2.setColor(theModel.getYMajorColour());
				l = new Line2D.Double(borderSizeLeft, frameHeight-bottomBorder.getOffset()-set, borderSizeLeft+graphWidth, frameHeight-bottomBorder.getOffset()-set);
				if(theModel.isYShowMajor())g2.draw(l);
				g2.setColor(Color.black);
				l = new Line2D.Double((borderSizeLeft-xAxisOffset)-3, frameHeight-bottomBorder.getOffset()-set, (borderSizeLeft-xAxisOffset), frameHeight-bottomBorder.getOffset()-set);
				g2.draw(l);
			}
			
			
			
		}
		
		//Draw Graphs
		g2.setTransform(tx);
		AffineTransform tooler = new AffineTransform();
		
		for(int i = 0; i < theModel.getNoGraphs(); i++)
		{
			GraphList current = theModel.getGraphPoints(i);
			double lastX = 0;
			double lastY = 0;
			boolean firstDone = false;
			boolean drawLine = true;
			
			for(int j = 0; j < current.size(); j++)
			{
				double x = current.getPoint(j).getXCoord();
				double y = current.getPoint(j).getYCoord();
				if(x == GraphPoint.NULL_COORD || y == GraphPoint.NULL_COORD) continue;
				g2.setTransform(tx);
				tooler = new AffineTransform();
				double offsetX, offsetY;
				if(theModel.isLogarithmicX()) 
				{
					if(x > 0)
						offsetX = (xFactor*(MultiGraphModel.log(baseX, x))) - theModel.log(baseX, theModel.getMinimumX())*xFactor;
					else	//ignore this point go on to the next and don't draw a line next time
					{
						drawLine = false;
						continue;
					}
					
						
				}
				else 
				{
					offsetX = xFactor * (x-minX);
				}
				if(theModel.isLogarithmicY())
				{
					if(y > 0)
						offsetY = (yFactor*(MultiGraphModel.log(baseY, y))) - theModel.log(baseY, theModel.getMinumumY())*yFactor;
					else
					{
						drawLine = false;
						continue;
					}
				}
			else offsetY = yFactor * (y-minY);

				double minumumX = theModel.getMinimumX();
				double minimumY = theModel.getMinumumY();
				double maximumX = theModel.getMaxX();
				double maximumY = theModel.getMaxY();
				//if(x >= minumumX && y>=minimumY && x<=maximumX && y<=maximumY)
				{
					g2.translate(borderSizeLeft+offsetX, frameHeight-bottomBorder.getOffset()-offsetY);
					tooler.translate(borderSizeLeft+offsetX, frameHeight-bottomBorder.getOffset()-offsetY);
					g2.setColor(current.getGraphColour());
					Shape s = current.getPointShape();
					Rectangle2D rec= s.getBounds2D();
					g2.translate(-(rec.getWidth()/2), -(rec.getHeight()/2));
					tooler.translate(-(rec.getWidth()/2), -(rec.getHeight()/2));
					g2.fill(s);
					Rectangle2D spot = s.getBounds2D();
					String str = current.getPoint(j).getDescription();
					hotSpots.add(new HotSpot(tooler.getTranslateX(), tooler.getTranslateY(), spot.getWidth(), spot.getHeight(), str));
					
					if(firstDone && drawLine)
					{
						g2.setTransform(tx);
						double x1, y1, x2, y2;
						if(theModel.isLogarithmicX())
						{
							x1 = borderSizeLeft+(xFactor*MultiGraphModel.log(baseX, x))- theModel.log(baseX, theModel.getMinimumX())*xFactor;
							x2 = borderSizeLeft+(xFactor*MultiGraphModel.log(baseX, lastX))- theModel.log(baseX, theModel.getMinimumX())*xFactor;
						}
						else
						{
							x1 = borderSizeLeft+(xFactor*(x-minX));
							x2 = borderSizeLeft+(xFactor*(lastX-minX));
						}
						if(theModel.isLogarithmicY())
						{
							y1 = frameHeight - bottomBorder.getOffset()-(yFactor*MultiGraphModel.log(baseY, y))+ theModel.log(baseY, theModel.getMinumumY())*yFactor;
							y2 = frameHeight - bottomBorder.getOffset()-(yFactor*MultiGraphModel.log(baseY, lastY)) + theModel.log(baseY, theModel.getMinumumY())*yFactor;
						}
						else
						{
							y1 = frameHeight - bottomBorder.getOffset()-(yFactor*(y-minY));
							y2 = frameHeight - bottomBorder.getOffset()-(yFactor*(lastY-minY));
						}
						if(current.isShowLines())
						{
							
							float[] dashPattern1 ={ 2, 2 };
							float[] dashPattern2 ={ 2, 2, 4, 2 };
							//System.out.println("current.getLineStyle = "+current.getLineStyle());
							switch(current.getLineStyle())
							{
								case 0: g2.setStroke(new BasicStroke((float)current.getLineWidth())); break;
								case 1: g2.setStroke(new BasicStroke((float)current.getLineWidth(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0F, dashPattern1, 0)); break;
								case 2: g2.setStroke(new BasicStroke((float)current.getLineWidth(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0F, dashPattern2, 0));
							}
							//g2.setStroke(new BasicStroke((float)current.getLineWidth(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0F, dashPattern2, 0));
							g2.draw(new Line2D.Double(x1,y1,x2,y2));
							
							g2.setStroke(new BasicStroke());
						}
					}
					else
					{
						drawLine = true; //we can carry on drawing the lines
					}
					firstDone = true;
					lastX = x;
					lastY = y;
					
					
				}
			}
			//System.out.println("tooltips yes");
			g2.setTransform(tx);
			

			//Remove borders
			g2.setTransform(tx);
			g2.setColor(Color.white);

			Rectangle2D top = new Rectangle2D.Double(0,0, frameWidth, topBorder.getOffset()-1);
			g2.fill(top);
			Rectangle2D bottom = new Rectangle2D.Double(0, frameHeight-bottomBorder.getOffset()+1, frameWidth, frameHeight);
			g2.fill(bottom);
			Rectangle2D left = new Rectangle2D.Double(0,0, borderSizeLeft-1, frameHeight);
			g2.fill(left);
			Rectangle2D right = new Rectangle2D.Double(frameWidth-borderSizeRight+1, 0, frameWidth, frameHeight);
			g2.fill(right);
		
		
			//Draw Legend
			if(theModel.isLegendVisible())
			{
				if(theModel.isLegendDocked())
				{
					if(theModel.isAutoBorder())
					{
						//theModel.setBordersUp(theModel.getLegendPosition(), this.getLegendWidth(g2), this.getLegendHeight());
					}
					if(theModel.getLegendPosition() == MultiGraphModel.LEFT)
					{
						g2.translate(5, (frameHeight/2)-(getLegendHeight()/2));
					}
					else if(theModel.getLegendPosition() == MultiGraphModel.RIGHT)
					{
						g2.translate(frameWidth-5-getLegendWidth(g2), (frameHeight/2)-(getLegendHeight()/2));
					}
					else if(theModel.getLegendPosition() == MultiGraphModel.BOTTOM)
					{
						g2.translate((frameWidth/2) - (getLegendWidth(g2)/2), frameHeight-5-getLegendHeight());
					}
				}
				else
				{
					double xTrans = frameWidth*(theModel.getLegendPositionX()/100);
					xTrans-= (getLegendWidth(g2)/2);
					double yTrans = frameHeight*(theModel.getLegendPositionY()/100);
					yTrans-= (getLegendHeight()/2);
					g2.translate(xTrans, yTrans);
				}
				
				
				//drawing part
				g2.setColor(Color.white);
				g2.fill(new Rectangle2D.Double(0.0,0.0,getLegendWidth(g2), getLegendHeight()));
				g2.setColor(Color.black);
				g2.draw(new Rectangle2D.Double(0.0,0.0,getLegendWidth(g2), getLegendHeight()));
				AffineTransform legSet = g2.getTransform();
				for(int n = 0; n < theModel.getNoGraphs(); n++)
				{
					
					GraphList gr = theModel.getGraphPoints(n);
					Shape sha = gr.getPointShape();
					g2.setColor(gr.getGraphColour());
					g2.setFont(theModel.getLegendFont());
					float[] dashPattern1 ={ 2, 2 };
					float[] dashPattern2 ={ 2, 2, 4, 2 };
					//System.out.println("current.getLineStyle = "+current.getLineStyle());
					switch(gr.getLineStyle())
					{
						case 0: g2.setStroke(new BasicStroke((float)gr.getLineWidth())); break;
						case 1: g2.setStroke(new BasicStroke((float)gr.getLineWidth(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0F, dashPattern1, 0)); break;
						case 2: g2.setStroke(new BasicStroke((float)gr.getLineWidth(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0F, dashPattern2, 0));
					}
					/*
					if(gr.isShowLines())g2.drawLine(5, 5+(n*20)+10, 30, 5+(n*20)+10);
					g2.setStroke(new BasicStroke());
					g2.setColor(Color.black);
					g2.drawString(gr.getGraphTitle(),35,5+(n*20)+16);
					g2.translate(17.5, 5+(n*20)+10);
					g2.translate(-((sha.getBounds2D().getWidth())/2),-((sha.getBounds2D().getHeight())/2));
					g2.setColor(gr.getGraphColour());
					g2.fill(sha);
					g2.setTransform(legSet);
					*/
					int height = theModel.getLegendFont().getSize()+8 ;

					if(gr.isShowLines())g2.drawLine(5, 5+(n*height)+13, 30, 5+(n*height)+13);
					g2.setStroke(new BasicStroke());
					g2.setColor(theModel.getLegendColour());
					g2.drawString(gr.getGraphTitle(),35,5+(n*height)+16);
					g2.translate(17.5, 5+(n*height)+13);
					g2.translate(-((sha.getBounds2D().getWidth())/2),-((sha.getBounds2D().getHeight())/2));
					g2.setColor(gr.getGraphColour());
					g2.fill(sha);
					g2.setTransform(legSet);
				}
			}
			
		}
		//		}
		
		g2.setTransform(tx);

		//Draw X Ticks
		if(!theModel.isLogarithmicX())
		{
			double majorXInterval = theModel.getMajorXInterval();
			
			double xPos = 0.0;

			Line2D l;
			
			for(xPos = 0; xPos<=graphWidth+0.00001; xPos=xPos+Math.abs(majorXInterval*xFactor))
			{
				g2.setColor(Color.black);
				l = new Line2D.Double(borderSizeLeft+xPos, frameHeight-bottomBorder.getOffset()+yAxisOffset, borderSizeLeft+xPos, frameHeight-bottomBorder.getOffset()+yAxisOffset+3);
				g2.draw(l);
			}
			
		}
		else //if discrete no logarithmic x scale
		{
			
			Line2D l;
			
			
			for(double i = MultiGraphModel.log(baseX, theModel.getMinimumX()); Math.round(i) < Math.round(maxXLog); i++)
			{
				double set = (i+1) * xFactor;
				set -= theModel.log(baseX, theModel.getMinimumX())*xFactor;
				
				g2.setColor(Color.black);
				l = new Line2D.Double(borderSizeLeft+set, frameHeight-bottomBorder.getOffset()+yAxisOffset, borderSizeLeft+set, frameHeight-bottomBorder.getOffset()+yAxisOffset+3);
				g2.draw(l);
			}
		   
		}
		
		
		//Draw Y Ticks
		
		if(!theModel.isLogarithmicY())
		{
			g2.setTransform(tx);
			
			Line2D l;
			
			double yPos = 0.0;
			//major
			
			double majorYInterval = theModel.getMajorYInterval();
			yPos = 0.0;
			for(yPos = 0; yPos<=graphHeight+0.0001; yPos=yPos+Math.abs(majorYInterval*yFactor))
			{
				g2.setColor(Color.black);
				l = new Line2D.Double((borderSizeLeft-xAxisOffset)-3, frameHeight-bottomBorder.getOffset()-yPos, (borderSizeLeft-xAxisOffset), frameHeight-bottomBorder.getOffset()-yPos);
				g2.draw(l);
			}
			
			
		}
		else
		{
			g2.setTransform(tx);
			Line2D l;
			//draw Y lines
			
			for(double i = MultiGraphModel.log(baseY, theModel.getMinumumY()); Math.round(i) < Math.round(maxYLog); i++)
			{
				double set = (i+1) * yFactor;
				set -= theModel.log(baseY, theModel.getMinumumY())*yFactor;
				
				g2.setColor(Color.black);
				l = new Line2D.Double((borderSizeLeft-xAxisOffset)-3, frameHeight-bottomBorder.getOffset()-set, (borderSizeLeft-xAxisOffset), frameHeight-bottomBorder.getOffset()-set);
				g2.draw(l);
			}
			
			
			
		}

		//Draw labels
		
		//Draw X labels
		if(!theModel.isLogarithmicX())
		{
			//x labels
			g2.setColor(theModel.getXLabelColour());
			g2.setFont(theModel.getXLabelFont());
			g2.setTransform(tx);
			for(double i = minX; i<= maxX; i = i + theModel.getMajorXInterval())
			{
				String label = getToSigFigs(i, theModel.getSigFigs());
				double yStringWidth = getStringWidth(label, g2);
				g2.setTransform(tx);
				if(yAxisOffset >= 0.0 || !(i>-0.00000001 && i <0.00000001))
					g2.drawString
						(
						label,
						(int)(borderSizeLeft+(Math.abs((i-minX)*xFactor))-(yStringWidth/2)),
						(int)(frameHeight-bottomBorder.getOffset()+15+yAxisOffset)
						);
			}
		}
		else //if discrete no logarithmic x scale
		{
			//x labels
			g2.setColor(theModel.getXLabelColour());
			g2.setFont(theModel.getXLabelFont());
			g2.setTransform(tx);
			for(double i = MultiGraphModel.log(baseX, theModel.getMinimumX()); i<=maxXLog; i++)
			{
				
				i = Math.round(i);
				String label;
				if(theModel.getXAxis().getLogStyle().getCurrentIndex() == 0)
					label = getToSigFigs(Math.pow(baseX,i), theModel.getSigFigs());
				else
					label = getToSigFigs(baseX, theModel.getSigFigs()) + " ";
				double yStringWidth = getStringWidth(label, g2);
				g2.setTransform(tx);
				g2.drawString(label,
					(int)((borderSizeLeft+((i*xFactor))-(yStringWidth/2))-(theModel.log(baseX, theModel.getMinimumX())*xFactor)),
					(int)(frameHeight-bottomBorder.getOffset()+5+g2.getFont().getSize()+yAxisOffset));
				if(theModel.getXAxis().getLogStyle().getCurrentIndex() == 1)
				{
					//draw the exponent
					Font currFont = g2.getFont();
					Font newFont = new Font(currFont.getName(), currFont.getStyle(), (int)(((double)currFont.getSize())/1.8));

					g2.setFont(newFont);
					label = " "+getToSigFigs(i, theModel.getSigFigs());
					g2.drawString(label, 
						(int)(yStringWidth+(borderSizeLeft+((i*xFactor))-(yStringWidth/2))-(theModel.log(baseX, theModel.getMinimumX())*xFactor)), 
						(int)(frameHeight-bottomBorder.getOffset()+5+g2.getFont().getSize()+yAxisOffset));
					g2.setFont(currFont);
				}
			}
		}
		
		
		//Draw Y Gridlines and labels
		
		if(!theModel.isLogarithmicY())
		{
			
			//y labels
			g2.setColor(theModel.getYLabelColour());
			g2.setFont(theModel.getYLabelFont());
			g2.setTransform(tx);
			for(double i = minY; i<=maxY; i = i + theModel.getMajorYInterval())
			{
				String label = getToSigFigs(i,  theModel.getSigFigs());
				double xStringWidth = getStringWidth(label, g2);
				g2.setTransform(tx);
				if(xAxisOffset >= 0.0 || !(i>-0.00000001 && i <0.00000001))
					g2.drawString
						(
						label,
						(int)(borderSizeLeft-xStringWidth-5-xAxisOffset),
						(int)(frameHeight-bottomBorder.getOffset()-(Math.abs((i-minY)*yFactor))+4)
						);
				
			}
			
		}
		else
		{
			//y labels
			g2.setTransform(tx);
			g2.setFont(theModel.getYLabelFont());
			g2.setColor(theModel.getYLabelColour());
			for(double i = MultiGraphModel.log(baseY, theModel.getMinumumY()); i<=maxYLog; i++)
			{
				i = Math.round(i);
				
				//System.out.println("i = "+i);
				String label;
				if(theModel.getYAxis().getLogStyle().getCurrentIndex() == 0)
					label = getToSigFigs(Math.pow(baseY,i), theModel.getSigFigs());
				else
					label = getToSigFigs(baseY, theModel.getSigFigs()) + " ";
				
				double xStringWidth = getStringWidth(label, g2);   
				
				
				double expWidth = 0;
				if(theModel.getYAxis().getLogStyle().getCurrentIndex() == 1)
				{
					Font currFont = g2.getFont();
					Font newFont = new Font(currFont.getName(), currFont.getStyle(), (int)(((double)currFont.getSize())/1.8));
					   g2.setFont(newFont);
					expWidth = getStringWidth(" "+getToSigFigs(i, theModel.getSigFigs()), g2);
					g2.setFont(currFont);
				}

				g2.setTransform(tx);
				g2.translate((int)(borderSizeLeft-(xStringWidth+expWidth)-5-xAxisOffset), (frameHeight-bottomBorder.getOffset()-((i*yFactor))+4)+(theModel.log(baseY, theModel.getMinumumY())*yFactor));
				
				g2.drawString(label,0,0);
				if(theModel.getYAxis().getLogStyle().getCurrentIndex() == 1)
				{
					//draw the exponent
					Font currFont = g2.getFont();
					int h1 = currFont.getSize();
					Font newFont = new Font(currFont.getName(), currFont.getStyle(), (int)(((double)currFont.getSize())/1.8));
					int h2 = newFont.getSize();

					g2.setFont(newFont);
					label = " "+getToSigFigs(i, theModel.getSigFigs());
					
					g2.drawString(label, (int)xStringWidth, h2-h1);
					g2.setFont(currFont);
				}
			}
		}
		
		//Draw graph title
		g2.setTransform(tx);
		g2.setFont(theModel.getTitleFont());
		g2.setColor(theModel.getTitleColour());
		double stringHeight = getStringHeight(theModel.getGraphTitle(), g2);
		
		StringTokenizer title = new StringTokenizer(theModel.getGraphTitle(), "\n");
		
		int counter = 0;
		int number = title.countTokens()-1;
		while(title.hasMoreTokens())
		{
			String line = title.nextToken();
			double stringWidth = getStringWidth(line, g2);
			
			int y = 
				(int)(topBorder.getOffset()/2) -
				(int)(number*stringHeight)/2 +
				(int)(counter*stringHeight+3);
			
			
			g2.drawString(line,(int)((frameWidth/2)-(stringWidth/2)), y);
			counter++;
		}
		g2.setFont(norm);
		
		//Draw Axis'
		g2.setTransform(tx);
		g2.setColor(Color.black);
		
		Line2D yAxis = new Line2D.Double(borderSizeLeft-xAxisOffset, topBorder.getOffset(), borderSizeLeft-xAxisOffset, frameHeight-bottomBorder.getOffset());
		g2.draw(yAxis);
		
		
		Line2D xAxis = new Line2D.Double(borderSizeLeft, frameHeight-bottomBorder.getOffset()+yAxisOffset, frameWidth-borderSizeRight, frameHeight-bottomBorder.getOffset()+yAxisOffset);
		g2.draw(xAxis);
		
		//Draw axis headings
		
		//y label
		g2.setTransform(tx);
		g2.setFont(theModel.getYTitleFont());
		g2.setColor(theModel.getYTitleColour());
		double yStringWidth = getStringWidth(theModel.getYTitle(), g2);
		g2.translate(15,topBorder.getOffset()+yStringWidth+5);
		g2.rotate(1.5*Math.PI);
		g2.drawString(theModel.getYTitle(), 0,0);
		//x label
		g2.setFont(theModel.getXTitleFont());
		g2.setColor(theModel.getXTitleColour());
		double xStringWidth = getStringWidth(theModel.getXTitle(), g2);
		g2.setTransform(tx);
		g2.translate(frameWidth-borderSizeRight-xStringWidth-5, frameHeight-5);
		g2.drawString(theModel.getXTitle(), 0,0);
		
		g2.setTransform(tx);
		
		//Do selectable objects
		
		//System.out.println("The bottom offset is: "+theModel.getBottomBorder().getOffset());
		try
		{
		theModel.getTopBorder().updatePosition(frameWidth, frameHeight);
		theModel.getBottomBorder().updatePosition(frameWidth, frameHeight);
		theModel.getLeftBorder().updatePosition(frameWidth, frameHeight);
		theModel.getRightBorder().updatePosition(frameWidth, frameHeight);
		theModel.getBorders().updatePosition(frameWidth, frameHeight);
		}
		catch(SettingException eee)
		{
		    //do nothing
		}
		//theModel.getTopBorder().setSelected(true);
		theModel.getTopBorder().render(g2);
		theModel.getBottomBorder().render(g2);
		theModel.getLeftBorder().render(g2);
		theModel.getRightBorder().render(g2);
		theModel.getBorders().render(g2);
		
		theModel.setCanvas(this);
		g2.setTransform(tx);
	}
	
	// print
	
	public void doPrint()
	{ doPrint(null); }
	
	public void doPrint(PrintRequestAttributeSet attributes)
	{
		try
		{
			PrinterJob job = PrinterJob.getPrinterJob();
			MultiGraphView printPage = (MultiGraphView)clone();
			//.getParent ().setSize(new Dimension(595, 842));
			//printPage.setSize(new Dimension(520,750));
			//System.out.println("printPage size = "+printPage.getWidth()+", "+printPage.getHeight());
			job.setPrintable(printPage);
			if (attributes != null)
			{
				if(job.printDialog(attributes)) job.print(attributes);
			} 
			else
			{
				if(job.printDialog()) job.print();
			}
		}
		catch(PrinterException ex)
		{
			System.err.println("Print error:\n"+ ex);
			JOptionPane.showMessageDialog(this, "Print error:\n"+ ex);
		}
		catch(IllegalArgumentException ex)
		{
			System.err.println("Print error:\n"+ ex);
			JOptionPane.showMessageDialog(this, "Print error:\n"+ ex);
		}
	}
	
	// image exports
	
	public void doExportToPNG(File file)
	{
		try
		{
			BufferedImage bi = new BufferedImage(theModel.getPrintCanvas().getWidth(), theModel.getPrintCanvas().getHeight(), BufferedImage.TYPE_3BYTE_BGR ) ;
			Graphics g2 = bi.createGraphics();
			MultiGraphView copy = ((MultiGraphView)(theModel.getPrintCanvas().clone()));
			copy.setSize(new Dimension(theModel.getPrintCanvas().getWidth(), theModel.getPrintCanvas().getHeight()));
			//Graphics g2 = bi.getGraphics ();
			//g2 = (Graphics2D)theModel.getPrintCanvas().getGraphics();
			//g2 = theModel.getPrintCanvas().getGraphics();
			copy.paint(g2);
			ImageIO.write(bi, "PNG", file);
		}
		catch(IOException ex)
		{
			System.err.println("Print error:\n"+ ex);
			JOptionPane.showMessageDialog(this, "Print error:\n"+ ex);
		}
	}
	
	public void doExportToJPEG(File file)
	{
		try
		{
			BufferedImage bi = new BufferedImage(theModel.getPrintCanvas().getWidth(), theModel.getPrintCanvas().getHeight(), BufferedImage.TYPE_3BYTE_BGR ) ;
			Graphics g2 = bi.createGraphics();
			MultiGraphView copy = ((MultiGraphView)(theModel.getPrintCanvas().clone()));
			copy.setSize(new Dimension(theModel.getPrintCanvas().getWidth(), theModel.getPrintCanvas().getHeight()));
			//Graphics g2 = bi.getGraphics ();
			//g2 = (Graphics2D)theModel.getPrintCanvas().getGraphics();
			//g2 = theModel.getPrintCanvas().getGraphics();
			copy.paint(g2);
			ImageIO.write(bi, "JPEG", file);
		}
		catch(IOException ex)
		{
			System.err.println("Print error:\n"+ ex);
			JOptionPane.showMessageDialog(this, "Print error:\n"+ ex);
		}
	}
	
	//Inner Classes
	
	/** Inner class to store data concerning spots on the screen where tooltips should
	 * appear.
	 */
	class HotSpot extends Rectangle2D.Double
	{
		private  String text;
		
		/** Creates a new HotSpot object with the given x coordinate, y coordinate, width,
		 * height and tooltip text.
		 * @param x the x coordinate
		 * @param y the y coordinate
		 * @param w the width
		 * @param h the height
		 * @param text the text to be displayed by the tooltip this area is being used to create.
		 */
		public HotSpot(int x, int y, int w, int h, String text)
		{
			super(x,y,w,h);
			this.text = text;
		}
		
		/** Creates a new HotSpot object with the given x coordinate, y coordinate, width,
		 * height and tooltip text.
		 * @param x the x coordinate
		 * @param y the y coordinate
		 * @param w the width
		 * @param h the height
		 * @param text the text to be displayed by the tooltip this area is being used to create.
		 */
		public HotSpot(double x, double y, double w, double h, String text)
		{
			super(x,y,w,h);
			this.text = text;
		}
		
		/** Returns the text associated with the tooltip this area is representing. */
		public String getText()
		{
			return text;
		}
	}
	
	/** Method which takes in a double value, and a number indicating significant
	 *  figures and returns a string of that number to the amount of significant
	 *  figures stated
	 */
	public static String getToSigFigs(double d, int sigFigs)
	{
		NumberFormat n = new DecimalFormat();
		n.setMaximumFractionDigits(sigFigs);
		return n.format(d);
		
		//System.out.println("This is returned "+d);
	}
	
	/** Method which computes width of a string for a given Graphics2D object
	 */
	public static double getStringWidth(String s, Graphics2D g2)
	{
		// catch special cases...
		// ...TextLayout constructor crashes with null or zero-length string
		if (s == null) return 0;
		if (s.length() == 0) return 0;
		TextLayout layout = new TextLayout(s, g2.getFont(), g2.getFontRenderContext());
		Rectangle r = layout.getOutline(new AffineTransform()).getBounds();
		return r.getWidth();
	}
	
	/** Method which computes height of a string for a given Graphics2D object
	 */
	public static double getStringHeight(String s, Graphics2D g2)
	{
		// catch special cases...
		// ...TextLayout constructor crashes with null or zero-length string
		if (s == null) return 0;
		if (s.length() == 0) return 0;
		TextLayout layout = new TextLayout(s, g2.getFont(), g2.getFontRenderContext());
		Rectangle r = layout.getOutline(new AffineTransform()).getBounds();
		return r.getHeight();
	}
	
	public void mouseClicked(MouseEvent e)
	{
	}
	
	public void mouseEntered(MouseEvent e)
	{
	}
	
	public void mouseExited(MouseEvent e)
	{
	}
	
	public void mousePressed(MouseEvent e)
	{
		ChartObject.deSelectAll();
		
		//theModel.getBottomBorder().mousePressed(e);
		//theModel.getTopBorder().mousePressed(e);
		//theModel.getLeftBorder().mousePressed(e);
		//theModel.getRightBorder().mousePressed(e);
		theModel.getBorders().mousePressed(e);
		repaint();
	}
	
	public void mouseReleased(MouseEvent e)
	{
	}
	
	public void mouseDragged(MouseEvent e)
	{
		//theModel.getTopBorder().mouseDragged(e);
		//theModel.getBottomBorder().mouseDragged(e);
		//theModel.getLeftBorder().mouseDragged(e);
		//theModel.getRightBorder().mouseDragged(e);
		theModel.getBorders().mouseDragged(e);
		
		repaint();
	}
	
	public void mouseMoved(MouseEvent e)
	{
	}
	
	public void keyPressed(KeyEvent e)
	{
		theModel.getBorders().keyPressed(e);
	}
	
	public void keyReleased(KeyEvent e)
	{
		theModel.getBorders().keyReleased(e);
	}
	
	public void keyTyped(KeyEvent e)
	{
	}
	
}

