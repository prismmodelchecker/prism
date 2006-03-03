//==============================================================================
//
//	Copyright (c) 2002-2005, Andrew Hinton
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

import java.awt.*;
import java.awt.font.TextLayout;
import javax.swing.*;
import javax.swing.event.*;
import java.io.Serializable;
import java.util.EventListener;
import java.lang.*;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/*import userinterface.util.*;
import userinterface.GUIPrism;
import prism.Prism;*/
import settings.*;

/** This graph is the model for the MultiGraphicView component.  It contains all of
 * the data required for the drawing of the graph.  The controller is the
 * MultiGraphOptions class.
 */
public class MultiGraphModel extends Observable implements SettingOwner, ListModel, EntityResolver
{
	
	//Display significant figures
	private int sigFigs;
	
	//Properties
	private MultipleLineStringSetting graphTitle;
	private FontColorSetting titleFont;
	private BooleanSetting legendVisible;
	private ChoiceSetting legendPosition;
	private DoubleSetting legendPositionX;
	private DoubleSetting legendPositionY;
	private FontColorSetting legendFont;
	private BooleanSetting autoborder;
	private HorizontalGraphBorder topBorder;
	private HorizontalGraphBorder bottomBorder;
	private VerticalGraphBorder rightBorder;
	private VerticalGraphBorder leftBorder;
	
	//Series information
	private ArrayList graphs; //Contains GraphLists
	private int numberOfGraphs;
	private SeriesList seriesList; //Used by the options
	
	//Axis information
	private AxisOwner xAxis, yAxis;
	
	//Layout information
	private FourBorders borders;
	private int sortBy;
	
	//View Reference
	private MultiGraphView canvas;
	
	
	
	//Constants
	
	private static final int SORTBYX = 0;
	private static final int SORTBYY = 1;
	//legend position
	public static final int LEFT = 0;
	public static final int RIGHT = 1;
	public static final int BOTTOM = 2;
	public static final int MANUAL = 3;
	
	//Constructors
	
	/** Constructs a new MultiGraphModel.  It sets the values to common sense values for
	 * an empty graph.
	 */
	public MultiGraphModel()
	{
		graphs = new ArrayList();
		
		graphTitle = new MultipleLineStringSetting("title", "New Graph", "The main title heading for the chart.", this, false);
		titleFont  = new FontColorSetting("title font", new FontColorPair(new Font("SansSerif", Font.PLAIN, 14), Color.black), "The font for the chart's title", this, false);
		legendVisible = new BooleanSetting("legend visible?", new Boolean(true), "Should the legend, which displays all of the series headings, be displayed?", this, false);
		String [] choices =
		{"Left", "Right", "Bottom", "Manual"};
		legendPosition = new ChoiceSetting("legend position", choices, choices[1], "The position of the legend", this, false);
		legendPositionX = new DoubleSetting("legend x", new Double(50.0), "The x position of the legend as a percentage of the total chart width.", this, false, new RangeConstraint("0.0,100.0"));
		legendPositionY = new DoubleSetting("legend y", new Double(50.0), "The y position of the legend as a percentage of the total chart width.", this, false, new RangeConstraint("0.0,100.0"));
		legendPositionX.setEnabled(false);
		legendPositionY.setEnabled(false);
		legendFont = new FontColorSetting("legend font", new FontColorPair(new Font("SansSerif", Font.PLAIN, 11), Color.black), "The font for the legend", this, false);
		autoborder = new BooleanSetting("autoborder", new Boolean(true), "Should the border sizes be calculated automatically?", this, false);
		
		numberOfGraphs = 0;
		
		bottomBorder = new HorizontalGraphBorder(this,HorizontalGraphBorder.BOTTOM,100);
		topBorder = new HorizontalGraphBorder(this,HorizontalGraphBorder.TOP, 100);
		leftBorder = new VerticalGraphBorder(this,VerticalGraphBorder.LEFT, 100);
		rightBorder = new VerticalGraphBorder(this,VerticalGraphBorder.RIGHT, 100);
		bottomBorder.getOffsetProperty().setEnabled(false);
		topBorder.getOffsetProperty().setEnabled(false);
		leftBorder.getOffsetProperty().setEnabled(false);
		rightBorder.getOffsetProperty().setEnabled(false);
		topBorder.setOtherHorizontalGraphBorder(bottomBorder);
		topBorder.setRightVerticalGraphBorder(rightBorder);
		topBorder.setLeftVerticalGraphBorder(leftBorder);
		bottomBorder.setOtherHorizontalGraphBorder(topBorder);
		bottomBorder.setRightVerticalGraphBorder(rightBorder);
		bottomBorder.setLeftVerticalGraphBorder(leftBorder);
		leftBorder.setOtherVerticalGraphBorder(rightBorder);
		leftBorder.setTopHorizontalGraphBorder(topBorder);
		leftBorder.setBottomVerticalGraphBorder(bottomBorder);
		rightBorder.setOtherVerticalGraphBorder(leftBorder);
		rightBorder.setTopHorizontalGraphBorder(topBorder);
		rightBorder.setBottomVerticalGraphBorder(bottomBorder);
		borders = new FourBorders(this, topBorder, bottomBorder, leftBorder, rightBorder);
		
		xAxis = new AxisOwner("X");
		yAxis = new AxisOwner("Y");
		
		canvas = null;
		sigFigs = 8;
		
		seriesList = new SeriesList(this);
		
	}
	
	//Access Methods
	
	
	public int getSigFigs()
	{
		return sigFigs;
	}
	
	/** The model contains a copy of its view purely for the reason of passing it to the
	 * MultiGraphOptions object.  This is accessed when the dialog requires a Panel to
	 * send to the printer.
	 * @return A MultiGraphView intended for printing.
	 */
	public MultiGraphView getPrintCanvas()
	{
		return canvas;
	}
	/** Access method which returns an ArrayList of GraphLists, which represent a series
	 * of data on the chart.
	 * @return a collection of graph series.
	 */
	public ArrayList getGraphs()
	{
		return graphs;
	}
	/** Access method to return the colour of the label for the x-axis.
	 * @return the colour of the label for the x-axis
	 */
	public Color getXLabelColour()
	{
		return xAxis.numberFont.getFontColorValue().c;
	}
	
	public Font getXLabelFont()
	{
		return xAxis.numberFont.getFontColorValue().f;
	}
	
	/** Access method to return the colour of the label for the y-axis
	 * @return the colour of the label for the y-axis
	 */
	public Color getYLabelColour()
	{
		return yAxis.numberFont.getFontColorValue().c;
	}
	
	public Font getYLabelFont()
	{
		return yAxis.numberFont.getFontColorValue().f;
	}
	
	public Font getXTitleFont()
	{
		return xAxis.headingFont.getFontColorValue().f;
	}
	
	public Color getXTitleColour()
	{
		return xAxis.headingFont.getFontColorValue().c;
	}
	
	public Font getYTitleFont()
	{
		return yAxis.headingFont.getFontColorValue().f;
	}
	
	public Color getYTitleColour()
	{
		return yAxis.headingFont.getFontColorValue().c;
	}
	
	
	
	/** Access method to return the colour of the vertical major gridlines.
	 * @return the colour of the vertical major gridlines.
	 */
	public Color getXMajorColour()
	{
		return xAxis.majorColour.getColorValue();
	}
	
	/** Access method to return the colour of the horizonal major gridlines
	 * @return the colour of the horizonal major gridlines
	 */
	public Color getYMajorColour()
	{
		return yAxis.majorColour.getColorValue();
	}
	
	/** Access method to return the colour of the vertical minor gridlines
	 * @return the colour of the vertical minor gridlines
	 */
	public Color getXMinorColour()
	{
		return xAxis.minorColour.getColorValue();
	}
	
	/** Access method to return the colour of the horizontal minor gridlines
	 * @return the colour of the horizontal minor gridlines
	 */
	public Color getYMinorColour()
	{
		return yAxis.minorColour.getColorValue();
	}
	
	/** Access method to say whether to show major x gridlines
	 * @return flag to say whether to show major x gridlines
	 */
	public boolean isXShowMajor()
	{
		return xAxis.showMajor.getBooleanValue();
	}
	
	/** Access method to say whether to show major y gridlines
	 * @return flag to say whether to show major y gridlines
	 */
	public boolean isYShowMajor()
	{
		return yAxis.showMajor.getBooleanValue();
	}
	
	/** Access method to say whether to show minor y gridlines
	 * @return flag to say whether to show minor y gridlines
	 */
	public boolean isYShowMinor()
	{
		return yAxis.showMinor.getBooleanValue();
	}
	
	/** Access method to say whether to show minor x gridlines
	 * @return flag to say whether to show minor x gridlines
	 */
	public boolean isXShowMinor()
	{
		return xAxis.showMinor.getBooleanValue();
	}
	
	/** Access method to return the x position of the legend, if it is not docked.
	 * @return the xposition of the legend.
	 */
	public double getLegendPositionX()
	{
		return legendPositionX.getDoubleValue();
	}
	
	/** Access method to return the y position of the legend, if it is not docked.
	 * @return the y position of the legend.
	 */
	public double getLegendPositionY()
	{
		return legendPositionY.getDoubleValue();
	}
	
	/** Access method to return the font for the graph's title.
	 * @return the font for the graph's title
	 */
	public Font getTitleFont()
	{
		return titleFont.getFontColorValue().f;
	}
	
	/** Access method to return the colour for the graph's title.
	 * @return the colour for the graph's title.
	 */
	public Color getTitleColour()
	{
		return titleFont.getFontColorValue().c;
	}
	
	/** Access method to return the font for the legend's title.
	 * @return the font for the legend's title
	 */
	public Font getLegendFont()
	{
		return legendFont.getFontColorValue().f;
	}
	
	/** Access method to return the colour for the legend's title.
	 * @return the colour for the legend's title.
	 */
	public Color getLegendColour()
	{
		return legendFont.getFontColorValue().c;
	}
	
	/** Access method to state whether the legend should be visible or not
	 * @return flag to state whether the legend should be visible or not.
	 */
	public boolean isLegendVisible()
	{
		return legendVisible.getBooleanValue();
	}
	
	/** Access method to return the legend's position index.  This is only useful if the
	 * legend is docked.
	 * @return the legend's position index:
	 * <ul>
	 *	<li>0: LEFT
	 *	<li>1: RIGHT
	 *	<li>2: BOTTOM
	 * </ul>
	 */
	public int getLegendPosition()
	{
		String pos = legendPosition.getStringValue();
		
		if(pos.equals("Left")) return 0;
		else if(pos.equals("Right")) return 1;
		else return 2;
	}
	
	/** Access method to return the size of the top border.
	 * @return the size of the top border.
	 */
	//	public int getBorderSizeTop()
	//	{
	//	return borderSizeTop;
	//	}
	
	/** Access method to return the size of the bottom border
	 * @return the size of the bottom border
	 */
	//	public int getBorderSizeBottom()
	//	{
	//	return borderSizeBottom;
	//	}
	
	public HorizontalGraphBorder getTopBorder()
	{
		return topBorder;
	}
	
	public HorizontalGraphBorder getBottomBorder()
	{
		return bottomBorder;
	}
	
	public VerticalGraphBorder getRightBorder()
	{
		return rightBorder;
	}
	
	public VerticalGraphBorder getLeftBorder()
	{
		return leftBorder;
	}
	
	/** Access method to return the size of the left border
	 * @return the size of the left border
	 */
	public double getBorderSizeLeft()
	{
		return leftBorder.getOffset();
	}
	
	/** Access method to return the size of the right border
	 * @return the size of the right border
	 */
	public double getBorderSizeRight()
	{
		return rightBorder.getOffset();
	}
	/** Access method to return the minimum x value allowed on the axis.  This is only
	 * useful when the x-axis is not logarithmic.
	 * @return the minimum x value allowed on the axis.
	 */
	public double getMinimumX()
	{
		if(isLogarithmicX()) return Math.pow(getLogarithmicBaseX(), xAxis.minimumPower.getIntegerValue());
		else
			return getMinX();
	}
	
	/** Access method to return the minimum y value allowed on the axis.  This is only
	 * useful when the y-axis is not logarithmic.
	 * @return the minimum y value allowed on the axis.
	 */
	public double getMinumumY()
	{
		if(isLogarithmicY()) return Math.pow(getLogarithmicBaseY(), yAxis.minimumPower.getIntegerValue());
		else
			return getMinY();
	}
	/** Access method to return a graph series at the specified index.
	 * @param graphNumber the index of the graph
	 * @return the graph series.
	 */
	public GraphList getGraphPoints(int graphNumber)
	{
		return (GraphList)graphs.get(graphNumber);
	}
	
	/** Access method to return the maximum x value allowed on the axis.  This is only
	 * useful when the x-axis is not logarithmic.
	 * @return the maximum x value allowed on the axis.
	 */
	public double getMaxX()
	{
		//if(isLogarithmicX()) return Math.pow(getLogarithmicBaseX(), xAxis.maximumPower.getValue());
		//else
		return xAxis.maxValue.getDoubleValue();
	}
	
	/** Access method to return the maximum y value allowed on the axis.  This is only
	 * useful when the y-axis is not logarithmic.
	 * @return maximum y value allowed on the axis.
	 */
	public double getMaxY()
	{
		//if(isLogarithmicY()) return Math.pow(getLogarithmicBaseY(), yAxis.maximumPower.getValue());
		//else
		return yAxis.maxValue.getDoubleValue();
	}
	
	/** Access method to return the minimum x value allowed on the axis.  This is only
	 * useful when the x-axis is not logarithmic.
	 * @return the maximum x value allowed on the axis.
	 */
	public double getMinX()
	{
		return xAxis.minValue.getDoubleValue();
	}
	
	/** Access method to return the minimum y value allowed on the axis.  This is only
	 * useful when the y-axis is not logarithmic.
	 * @return maximum y value allowed on the axis.
	 */
	public double getMinY()
	{
		return yAxis.minValue.getDoubleValue();
	}
	
	/** Access method to return the heading for the x-axis.
	 * @return the heading for the x-axis.
	 */
	public String getXTitle()
	{
		return xAxis.heading.getStringValue().toString();
	}
	
	/** Access method to return the heading for the y-axis.
	 * @return the heading for the y-axis.
	 */
	public String getYTitle()
	{
		return yAxis.heading.getStringValue().toString();
	}
	
	/** Access method to return the interval in pixels between major gridlines on the
	 * x-axis.
	 * @return the major x-gridline interval.
	 */
	public double getMajorXInterval()
	{
		return xAxis.majorGridInterval.getDoubleValue();
	}
	
	/** Access method to return the interval in pixels between major gridlines on the
	 * y-axis.
	 * @return the major y-gridline interval.
	 */
	public double getMajorYInterval()
	{
		return yAxis.majorGridInterval.getDoubleValue();
	}
	
	/** Access method to return the interval in pixels between minor gridlines on the
	 * x-axis.
	 * @return the minor x-gridline interval.
	 */
	public double getMinorXInterval()
	{
		return xAxis.minorGridInterval.getDoubleValue();
	}
	
	/** Access method to return the interval in pixels between minor gridlines on the
	 * y-axis.
	 * @return the minor y-gridline interval.
	 */
	public double getMinorYInterval()
	{
		return yAxis.minorGridInterval.getDoubleValue();
	}
	
	/** Access method to return the title of the chart.
	 * @return the title of the chart
	 */
	public String getGraphTitle()
	{
		return graphTitle.getStringValue();
	}
	
	/** Access method return the number of data series' on the chart
	 * @return the number of data series' on the chart
	 */
	public int getNoGraphs()
	{
		return numberOfGraphs;
	}
	
	/** Access method to say whether the x-axis should be drawn as a logarithmic scale.
	 * @return a flag stating whether the x-axis is logarithmic
	 */
	public boolean isLogarithmicX()
	{
		return xAxis.logarithmic.getCurrentIndex() == 1;
	}
	
	/** Access method to say whether the y-axis should be drawn as a logarithmic scale.
	 * @return a flag stating whether the y-axis is logarithmic
	 */
	public boolean isLogarithmicY()
	{
		return yAxis.logarithmic.getCurrentIndex() == 1;
	}
	
	/** Access method to return the base to which logarithmic scales on the x-axisshould be set.
	 * Notice that this is an integer value (so it cannot handle base e).   This method
	 * is only useful if the x-axis is set to a logarithmic scale.
	 * @return the x-axis logarithmic base
	 */
	public double getLogarithmicBaseX()
	{
		return (int)xAxis.logBase.getDoubleValue();
	}
	
	/** Access method to return the base to which logarithmic scales on the y-axis should be set.
	 * Notice that this is an integer value (so it cannot handle base e).  This method
	 * is only useful if the y-axis is set to a logarithmic scale.
	 * @return the y-axis logarithmic base
	 */
	public double getLogarithmicBaseY()
	{
		return (int)yAxis.logBase.getDoubleValue();
	}
	
	/** Access method to state whether the legend is docked.  If this value is false the
	 * legend should be free to be positioned anywhere on the screen.
	 * @return a flag to say whether the legend is docked.
	 */
	public boolean isLegendDocked()
	{
		return legendPosition.getCurrentIndex() != MANUAL;
	}
	
	/** Access method to state whether this model is working out borders automatically.
	 * @return a flag stating whether autoborder is turned on.
	 */
	public boolean isAutoBorder()
	{
		return autoborder.getBooleanValue();
	}
	
	public String toString()
	{
		return this.graphTitle.getStringValue().toString();
	}
	
	public FourBorders getBorders()
	{
		return borders;
	}
	
	
	
	
	//Update methods
	
	public void changed()
	{
		setChanged();
		notifyObservers();
		if(canvas != null)
			canvas.repaint();
	}
	
	public void setSigFigs(int i)
	{
		sigFigs = i;
	}
	
	/** Sets whether to show major gridlines on the x-axis.
	 * @param b Flag stating whether x-major-gridlines should be shown
	 */
	public void setXShowMajor(boolean b) throws SettingException
	{
		xAxis.showMajor.setValue(new Boolean(b));
		setChanged();
	}
	
	/** Sets whether to show minor gridlines on the x-axis.
	 * @param b Flag stating whether x-minor-gridlines should be shown
	 */
	public void setXShowMinor(boolean b) throws SettingException
	{
		xAxis.showMinor.setValue(new Boolean(b));
		setChanged();
	}
	
	/** Sets whether to show major gridlines on the y-axis.
	 * @param b Flag stating whether y-major-gridlines should be shown
	 */
	public void setYShowMajor(boolean b) throws SettingException
	{
		yAxis.showMajor.setValue(new Boolean(b));
		setChanged();
		
	}
	
	/** Sets whether to show minor gridlines on the -axis.
	 * @param b Flag stating whether y-minor-gridlines should be shown
	 */
	public void setYShowMinor(boolean b) throws SettingException
	{
		yAxis.showMinor.setValue(new Boolean(b));
		
	}
	
	/** Sets the model to use autoborder. */
	public void setAutoBorder(boolean b) throws SettingException
	{
		autoborder.setValue(new Boolean(b));
		setChanged();
		
	}
	/** Sets the x coordinate of the legend, if it is not docked. */
	public void setLegendPositionX(double i) throws SettingException
	{
		legendPositionX.setValue(new Double(i));
		setChanged();
		
	}
	
	/** Sets the y coordinate of the legend, if it is not docked. */
	public void setLegendPositionY(double i) throws SettingException
	{
		legendPositionY.setValue(new Double(i));
		setChanged();
		
	}
	
	/** Sets the font of the chart title. */
	public void setTitleFont(Font i) throws SettingException
	{
		titleFont.setValue(new FontColorPair(i, titleFont.getFontColorValue().c));
		
		
		setChanged();
		
	}
	
	/** Sets the colour of the chart title. */
	public void setTitleColour(Color i) throws SettingException
	{
		titleFont.setValue(new FontColorPair(titleFont.getFontColorValue().f, i));
		setChanged();
		
	}
	
	/** Sets whether the legned should be visible on the screen. */
	public void setLegendVisible(boolean b) throws SettingException
	{
		legendVisible.setValue(new Boolean(b));
		setChanged();
		
	}
	
	/** Sets the position of the legend if it is docked.
	 * @param i This value describes the new position of the legend:
	 * <ul>
	 *	<li>0: LEFT
	 *	<li>1: RIGHT
	 *	<li>2: BOTTOM
	 * </ul>
	 */
	public void setLegendPosition(int i) throws SettingException
	{
		
		legendPosition.setSelectedIndex(i);
		setChanged();
		
	}
	
	/** Sets whether the legend should be docked.
	 * public void setLegendDocked(boolean b)
	 * {
	 * legendDocked = b;
	 * setChanged();
	 *
	 * }*/
	
	/** Sets the colour of x-major-gridlines. */
	public void setXMajorColour(Color c) throws SettingException
	{
		xAxis.majorColour.setValue(c);
		setChanged();
		
	}
	
	/** Sets the colour of x-minor-gridlines. */
	public void setXMinorColour(Color c) throws SettingException
	{
		xAxis.minorColour.setValue(c);
		setChanged();
		
	}
	
	/** Sets the colour of y-major-gridlines. */
	public void setYMajorColour(Color c) throws SettingException
	{
		yAxis.majorColour.setValue(c);
		
	}
	
	/** Sets the colour of y-minor-gridlines. */
	public void setYMinorColour(Color c) throws SettingException
	{
		yAxis.minorColour.setValue(c);
		
	}
	
	/** Sets the colour of the axis label on the x-axis. */
		/*public void setXLabelColour(Color c)
		 {
		 xLabelColour = c;setChanged();
		 
		 }*/
	
	/** The model needs to store a copy of its view in order for the Options Dialog to
	 * print it.  This is a method to set this up.
	 * @param v This should be a pointer to the same MultiGraphView which observes this model.
	 */
	public void setCanvas(MultiGraphView v)
	{
		canvas = v;
		try
		{
		Graphics2D g2 = (Graphics2D)canvas.getGraphics();
		if (g2 == null) return; //this can happen during export of the graph to png/jpeg/etc.
		if(isAutoBorder())setBordersUp(getLegendPosition(), canvas.getLegendWidth(g2), canvas.getLegendHeight(), canvas.getTitleHeight(g2));
		}
		catch(SettingException e)
		{
			gop.errorDialog(e.getMessage(), "Error");
		}
	}
	
	/** Sets the colour of the axis label on the y-axis. */
		/*public void setYLabelColour(Color c)
		 {
		 yLabelColour = c;
		 setChanged();
		 
		 }*/
	
	/** This method is intended to be called when autoborders are turned on.  It sets
	 * the border width values automatically according to the position and size of the
	 * legend.
	 * @param position The location index of the legend:
	 * <ul>
	 *	<li>0: LEFT
	 *	<li>1: RIGHT
	 *	<li>2: BOTTOM
	 * <ul>
	 * @param legendWidth The width in pixels of the legend
	 * @param legendHeight The Height in pixels of the legend
	 */
	public void setBordersUp(int position, double legendWidth, double legendHeight, double titleHeight) throws SettingException
	{
		//System.out.println("set borders up");
		int top = (int) (titleHeight+20);
		if(isLegendDocked() && isLegendVisible())
		{
			//System.out.println("sbu1");
			if(position == LEFT)
			{
				//System.out.println("sbu2");
				//		borderSizeRight = 10;
				//		borderSizeBottom = 35;
				//		borderSizeTop = 35;
				if(topBorder.getOffset() != 35)topBorder.setOffset(top);
				if(bottomBorder.getOffset() != 35)bottomBorder.setOffset(35);
				if(rightBorder.getOffset() != 35)rightBorder.setOffset(10);
				int left =  10 + (int)legendWidth +25;
				if(leftBorder.getOffset() != left) leftBorder.setOffset(left);
				//		borderSizeLeft = 75 + 10 + (int)legendWidth;
			}
			else if(position == RIGHT)
			{
				//System.out.println("sbu3");
				//		borderSizeRight = 10 + (int)legendWidth;
				//		borderSizeTop = 35;
				//		borderSizeBottom = 35;
				topBorder.setOffset(top);
				bottomBorder.setOffset(35);
				int right = 10 + (int)legendWidth+25;
				rightBorder.setOffset(right);
				leftBorder.setOffset(70);
				//		borderSizeLeft = 70;
			}
			else if(position == BOTTOM)
			{
				//System.out.println("sbu4");
				//		borderSizeRight = 10;
				//		borderSizeTop = 35;
				//		borderSizeBottom = 30 + 10 + (int)legendHeight;
				topBorder.setOffset(top);
				bottomBorder.setOffset(30 + 10 + (int)legendHeight);
				rightBorder.setOffset(10);
				leftBorder.setOffset(50);
				//		borderSizeLeft = 50;
			}
		}
		else
		{
			//System.out.println("sbu5");
			//		borderSizeRight = 10;
			//		borderSizeBottom = 35;
			//		borderSizeLeft = 70;
			//		borderSizeTop = 35;
			topBorder.setOffset(top);
			bottomBorder.setOffset(35);
			rightBorder.setOffset(10);
			leftBorder.setOffset(70);
		}
		setChanged();
	}
	/** Sets up a new data series with a given title, colour and shape.  It also returns
	 * the index of this graph in the graph array so that additions to this graph are
	 * made easier.
	 * @param title the title of the series
	 * @param colour the colour of the series
	 * @param sh the shape of the series.  The following constants should be used:
	 * <ul>
	 *	<li> GraphList.CIRCLE
	 *	<li> GraphList.SQUARE
	 *	<li> GraphList.TRIANGLE
	 *	<li> GraphList.RECTANGLE_H
	 *	<li> GraphList.RECTANGLE_V
	 *	<li> GraphList.NONE
	 * </ul>
	 * @return the index of this series in the graph array.
	 */
	public int addGraph(String title, Color colour, int sh) throws SettingException
	{
		GraphList gl = new GraphList(this, title, colour, sh);
		graphs.add(gl);
		
		numberOfGraphs++;setChanged();
		notifyObservers(null);
		
		workOutAndSetXScales();
		workOutAndSetYScales();
		
		seriesList.setGraphsFromModel(this);
		if(gop != null)tellModelAboutGraphOptionsPanel(gop);
		fireContentsChanged(this, 0, getSize());
		return numberOfGraphs-1;
	}
	/** Sets up a new data series with random values for the colour and shape but with a
	 * given title.  It also returns the index of this graph in the graph array so that additions to this graph are
	 * made easier.
	 * @param title the title of the series
	 * @return the index of this series in the graph array.
	 */
	int sh_counter = 0;
	//static Color[] colors =
	//{Color.red, Color.blue, Color.green, Color.orange, Color.magenta};
	public int addGraph(String title) throws SettingException
	{
		int color_ref = sh_counter%GraphList.DEFAULT_COLORS.length;
		int shape_ref = (sh_counter+(sh_counter/10))%5;
		sh_counter++;
		Color c = GraphList.DEFAULT_COLORS[color_ref];
		int sh = shape_ref;
		return addGraph(title, c, sh);
	}
	
	public void removeGraph(GraphList gl)
	{
		for(int i = 0; i < graphs.size(); i++)
		{
			GraphList gggg = (GraphList)graphs.get(i);
			if(gggg == gl)
			{
				graphs.remove(i);
				break;
			}
		}
		//graphs.remove(gl); //why this doesn't work, I don't know... oh dear ^ hackz above ^
		numberOfGraphs--;
		fireContentsChanged(this, 0, getSize());
		setChanged();
		notifyObservers(null);
	}
	
	public void swapGraphs(int g1, int g2)
	{
		Object first = graphs.get(g1);
		graphs.set(g1, graphs.get(g2));
		graphs.set(g2, first);
		setChanged();
		notifyObservers(null);
	}
	
	/** Sets whether the x-axis should use a logarithmic scale. */
	public void setLogarithmicX(boolean isLogarithmic) throws SettingException
	{
		try
		{
			if(isLogarithmic)
			{
				xAxis.logarithmic.setSelectedIndex(1);
			}
			else
			{
				xAxis.logarithmic.setSelectedIndex(0);
			}
		}
		catch(SettingException e)
		{}
		setChanged();
		
	}
	
	/** Sets whether the y-axis should use a logarithmic scale. */
	public void setLogarithmicY(boolean isLogarithmic) throws SettingException
	{
		try
		{
			if(isLogarithmic)
			{
				yAxis.logarithmic.setSelectedIndex(1);
			}
			else
			{
				yAxis.logarithmic.setSelectedIndex(0);
			}
		}
		catch(SettingException e)
		{}
		setChanged();
		
	}
	
	/** Sets the logarithmic base for the x-axis. */
	public void setLogarithmicBaseX(int logBase) throws SettingException
	{
		xAxis.logBase.setValue(new Integer(logBase));
		setChanged();
	}
	
	/** Sets the logarithmic base for the y-axis */
	public void setLogarithmicBaseY(int logBase) throws SettingException
	{
		yAxis.logBase.setValue(new Integer(logBase));
		setChanged();
	}
	
	public synchronized void addPoint(int graphIndex, GraphPoint p, boolean sortAfter) throws SettingException
	{
		addPoint(graphIndex, p, sortAfter, true, true);
	}
	
	/** Adds a GraphPoint object to the series at an index.  A flag is needed to state
	 * whether the internal data structure should be sorted after the insertation of
	 * this point.  It is recommended that this value is false for all additions of a
	 * multiple insert except for the last.  It is always recommended that the last
	 * insert to the graph has this value as true.  Single inserts should have this
	 * value as true.  The importance of this sort is so that the drawing methods know
	 * which order to draw lines between the points.
	 * @param graphIndex The index of the series this point should be added to.
	 * @param p the GraphPoint object containing the data about the point to be added.
	 * @param sortAfter should the internal data structure be sorted after this insert.
	 *
	 *	Note 17/12/04
	 *	autoX and autoY are now irrelevent because there is now an auto-scale option
	 *	for each axis which handles this
	 */
	public synchronized void addPoint(int graphIndex, GraphPoint p, boolean sortAfter, boolean autoX, boolean autoY) throws SettingException
	{
		((GraphList)graphs.get(graphIndex)).add(p);
		if(sortAfter)((GraphList)graphs.get(graphIndex)).sortPoints(sortBy);
		
		//System.out.println("Before workOutXAndY");
		workOutAndSetXScales();
		workOutAndSetYScales();
		//System.out.println("After workOutXAndY");
		
		setChanged();
		notifyObservers(null);
		//System.out.println("After notifyObservers");
		if(canvas!=null)canvas.repaint();
	}
	
	
	/** Removes a given GraphPoint object from the data series at the given index.
	 * @param graphIndex the index of the series that this point should be deleted from.
	 * @param p the GraphPoint object to be removed.
	 */
	public void removePoint(int graphIndex, GraphPoint p) throws SettingException
	{
		((GraphList)graphs.get(graphIndex)).remove(p);
		workOutAndSetXScales();
		workOutAndSetYScales();
		//sortPoints(sortBy);
		setChanged();
		notifyObservers(null);
	}
	
	/** Removes all points from the specified data series.
	 * @param graphIndex the index of the graph to have its points removed.
	 */
	public void removeAllPoints(int graphIndex) throws SettingException
	{
		((GraphList)graphs.get(graphIndex)).clear();
		workOutAndSetXScales();
		workOutAndSetYScales();
		//graphs.set(graphIndex, null);
		changed();
	}
	
	/** Sets the maximum value on the x-axis. */
	public void setMaxX(double x) throws SettingException
	{
		xAxis.maxValue.setValue(new Double(x));
		setChanged();
		
	}
	
	/** Sets the maximum value on the y-axis. */
	public void setMaxY(double y) throws SettingException
	{
		yAxis.maxValue.setValue(new Double(y));
		setChanged();
		
	}
	
	/** Sets the maximum value on the x-axis. */
	public void setMinX(double x) throws SettingException
	{
		xAxis.minValue.setValue(new Double(x));
		setChanged();
		
	}
	
	/** Sets the maximum value on the y-axis. */
	public void setMinY(double y) throws SettingException
	{
		yAxis.minValue.setValue(new Double(y));
		setChanged();
		
	}
	
	/** For logarithmic scales, this sets the x-axis minimum power. */
	public void setMinimumPowerX(int x) throws SettingException
	{
		xAxis.minimumPower.setValue(new Integer(x));
		setChanged();
	}
	
	/** For logarithmic scales, this sets the y-axis minimum power. */
	public void setMinimumPowerY(int y) throws SettingException
	{
		yAxis.minimumPower.setValue(new Integer(y));
		setChanged();
	}
	
	/** This sets the value of the maximum x-coordinate according the values stored in
	 * the available data in the chart.  It rounds up to the nearest factor of ten.
	 * If the scale is normal, this rounds up to the nearest factor of ten.  If the
	 * sacle is logarithmic, this rounds up to the nearest factor of the base.
	 *
	 * An addition to this method also works out the appropriate minimum power for
	 * logarithmic scales
	 */
	public void workOutAndSetXScales() throws SettingException
	{
		//System.out.println("workOutAndSetXScales called");
		AxisOwner axis = xAxis;
		if(axis.autoScale.getBooleanValue())
		{
			//get previous value to look for changes
			double previousMax = getMaxX();
			double previousMin = getMinX();
			double previousMinor = getMinorXInterval();
			double previousMajor = getMajorXInterval();
			double previousLogBase = getLogarithmicBaseX();
			double previousMinPow = xAxis.getMinimumPower().getIntegerValue();
			double previousMaxPow = xAxis.getMaximumPower().getIntegerValue();
			
			double UNDEFINED = Double.POSITIVE_INFINITY;
			//declare new values
			double
			newMax = UNDEFINED,
			newMin = UNDEFINED,
			newMinor = UNDEFINED,
			newMajor = UNDEFINED,
			newLogBase = UNDEFINED,
			newMinPow = UNDEFINED,
			newMaxPow = UNDEFINED;
			
			
			
			//start by searching for the maximum and minimum values
			
			double max = 0;	  //this is the smallest that max can b
			double min = Double.POSITIVE_INFINITY; //need to find the smallest value for sorting out log scales
			double minPositive = Double.POSITIVE_INFINITY; //need to find the smallest positive value for the minimum log scale
			double curr = 0;
			boolean validPointFound = false;
			boolean anyPositive = false;
			
			for(int i = 0; i < graphs.size(); i++)//for each series
			{
				GraphList points = (GraphList)graphs.get(i);
				for(int j = 0; j < points.size(); j++)//for each point
				{
					curr = (points.getPoint(j)).getXCoord();
					if(curr == Double.NEGATIVE_INFINITY) continue;
					if(curr > 0) anyPositive = true;
					validPointFound = true;
					if(curr > max) max = curr;
					if(curr < min)min = curr;
					if(curr > 0 && curr < minPositive) minPositive = curr;
					
				}
			}
			if(!validPointFound)//if no points were found
			{
				newMax = 1.0;
				newMin = 0;
				newMajor = newMax / 10.0;
				newMinor = newMax / 50.0;
				newLogBase = 10;
				newMinPow = 0;
				newMaxPow = 1;
			}
			else //if at least one point was found
			{
				
				//sort out max for normal scale
				int exponent = (int)Math.ceil(log(10, max));
				double maxVal = Math.pow(10, exponent);
				double n = maxVal;
				if((maxVal/10.00)>0.0000001) //if maxX is positive
				{
					int counter = 0;
					boolean set = false;
					for(double i = 0; i <= n; i=i+(maxVal/10.0))
					{
						if(i > max)
						{
							newMax = i;
							newMajor = newMax / counter;
							newMinor = newMax / (counter*5);
							set = true;
							break;
						}
						
						counter++;
					}
					if(!set)
					{
						newMax = max;
						newMajor = newMax / 10;
						newMinor = newMax / 50;
					}
				}
				
				//sort out min for normal scale
				if(min >= 0.0)	//don't use minimum values greater than 0 for normal scale
				{
					newMin = 0.0;
				}
				else
				{
					double minVal = 0;
					
					double decr = Math.pow(10, exponent)/10;
					
					
					if((decr)>0.0000001)
					{
						while(minVal > min)
						{
							minVal -= decr;
						}
						
						newMin = minVal;
					}
					else
					{
						newMin = -newMax;
					}
				}
				
				//System.out.println("Doing logarithmic stuff");
				//newLogBase = 10;
				newLogBase = previousLogBase; //no change now
				if(anyPositive)
				{
					if(max > 0)
					{
						
						exponent = (int)Math.ceil(log(newLogBase, max));
						//yAxis.maxValue.setValue(Math.pow(getLogarithmicBaseY(), exponent)); //not really needed
						newMaxPow = exponent;
						
						//sort out minimum power
						if(minPositive != Double.POSITIVE_INFINITY)
						{
							exponent = (int)Math.ceil(log(newLogBase, minPositive));
							newMinPow = exponent-1;
						}
						else
						{
							newMinPow = newMaxPow-1;
						}
					}
					else
					{
						newMaxPow = 1;
						newMinPow = 0;
					}
				}
				else
				{
					
					newMaxPow = 1;
					newMaxPow = 0;
				}
				
				
				// System.out.println("Getting as far as the setCHanged notifyObservers()");
				
				
			}
			
			//System.out.println("previousMax = "+previousMax);
			//System.out.println("newMax = "+newMax);
			//System.out.println("previousMinPow = "+previousMinPow);
			//System.out.println("newMinPow = "+newMinPow);
			//System.out.println("previousMaxPow = "+previousMaxPow);
			//System.out.println("newMaxPow = "+newMaxPow);
			
			if(newMax == UNDEFINED)
				newMax = 1.0;
			if(newMin == UNDEFINED)
				newMin = 0;
			if(newMajor == UNDEFINED)
				newMajor = newMax / 10.0;
			if(newMinor == UNDEFINED)
				newMinor = newMax / 50.0;
			
			
			
			
			
			//test for changes, if there were any notify observers
			boolean changeMade = false;
			axis.activated = false;
			if(previousMax != newMax)
			{
				//System.out.println("maxdiff");
				changeMade = true;
				axis.getMaxValue().setValue(new Double(newMax));
			}
			if(previousMin != newMin)
			{
				//System.out.println("mindiff");
				changeMade = true;
				axis.getMinValue().setValue(new Double(newMin));
			}
			if(previousMajor != newMajor)
			{
				//System.out.println("majordiff");
				changeMade = true;
				axis.getMajorGridInterval().setValue(new Double(newMajor));
			}
			if(previousMinor != newMinor)
			{
				//System.out.println("minordiff");
				changeMade = true;
				axis.getMinorGridInterval().setValue(new Double(newMinor));
			}
			if(previousLogBase != newLogBase)
			{
				//System.out.println("logbasediff");
				changeMade = true;
				axis.getLogBase().setValue(new Double(newLogBase));
			}
			if(previousMinPow != newMinPow)
			{
				//System.out.println("minpowdiff");
				changeMade = true;
				axis.getMinimumPower().setValue(new Integer((int)newMinPow));
			}
			if(previousMaxPow != newMaxPow)
			{
				//System.out.println("maxpowdiff");
				changeMade = true;
				axis.getMaximumPower().setValue(new Integer((int)newMaxPow));
			}
			axis.activated = true;
			if(changeMade)
			{
				//setChanged();
				//notifyObservers(null);
			}
		}
		
		
		////System.out.println("workOutAndSetXScales done");
	}
	
	/** This sets the value of the maximum y-coordinate according the values stored in
	 * the available data in the chart.  It rounds up to the nearest factor of ten.
	 * If the scale is normal, this rounds up to the nearest factor of ten.  If the
	 * sacle is logarithmic, this rounds up to the nearest factor of the base.
	 * An addition to this method also works out the appropriate minimum power for
	 * logarithmic scales
	 */
	public void workOutAndSetYScales() throws SettingException
	{
		AxisOwner axis = yAxis;
		if(axis.autoScale.getBooleanValue())
		{
			//get previous value to look for changes
			double previousMax = getMaxY();
			double previousMin = getMinY();
			double previousMinor = getMinorYInterval();
			double previousMajor = getMajorYInterval();
			double previousLogBase = getLogarithmicBaseY();
			double previousMinPow = yAxis.getMinimumPower().getIntegerValue();
			double previousMaxPow = yAxis.getMaximumPower().getIntegerValue();
			
			double UNDEFINED = Double.POSITIVE_INFINITY;
			//declare new values
			double
			newMax = UNDEFINED,
			newMin = UNDEFINED,
			newMinor = UNDEFINED,
			newMajor = UNDEFINED,
			newLogBase = UNDEFINED,
			newMinPow = UNDEFINED,
			newMaxPow = UNDEFINED;
			
			
			
			//start by searching for the maximum and minimum values
			
			double max = 0;	  //this is the smallest that max can b
			double min = Double.POSITIVE_INFINITY; //need to find the smallest value for sorting out log scales
			double minPositive = Double.POSITIVE_INFINITY; //need to find the smallest positive value for the minimum log scale
			double curr = 0;
			boolean validPointFound = false;
			boolean anyPositive = false;
			
			for(int i = 0; i < graphs.size(); i++)//for each series
			{
				GraphList points = (GraphList)graphs.get(i);
				for(int j = 0; j < points.size(); j++)//for each point
				{
					curr = (points.getPoint(j)).getYCoord();
					if(curr == Double.NEGATIVE_INFINITY) continue;
					if(curr > 0) anyPositive = true;
					validPointFound = true;
					if(curr > max) max = curr;
					if(curr < min)min = curr;
					if(curr > 0 && curr < minPositive) minPositive = curr;
					
				}
			}
			if(!validPointFound)//if no points were found
			{
				newMax = 1.0;
				newMin = 0;
				newMajor = newMax / 10.0;
				newMinor = newMax / 50.0;
				newLogBase = 10;
				newMinPow = 0;
				newMaxPow = 1;
			}
			else //if at least one point was found
			{
				
				//sort out max for normal scale
				int exponent = (int)Math.ceil(log(10, max));
				double maxVal = Math.pow(10, exponent);
				double n = maxVal;
				if((maxVal/10.00)>0.0000001) //if maxX is positive
				{
					int counter = 0;
					boolean set = false;
					for(double i = 0; i <= n; i=i+(maxVal/10.0))
					{
						if(i > max)
						{
							newMax = i;
							newMajor = newMax / counter;
							newMinor = newMax / (counter*5);
							set = true;
							break;
						}
						
						counter++;
					}
					if(!set)
					{
						newMax = max;
						newMajor = newMax / 10;
						newMinor = newMax / 50;
					}
				}
				
				//sort out min for normal scale
				if(min >= 0.0)	//don't use minimum values greater than 0 for normal scale
				{
					newMin = 0.0;
				}
				else
				{
					double minVal = 0;
					
					double decr = Math.pow(10, exponent)/10;
					
					
					if((decr)>0.0000001)
					{
						while(minVal > min)
						{
							minVal -= decr;
						}
						
						newMin = minVal;
					}
					else
					{
						newMin = -newMax;
					}
				}
				
				//System.out.println("Doing logarithmic stuff");
				//newLogBase = 10;
				newLogBase = previousLogBase;
				if(anyPositive)
				{
					if(max > 0)
					{
						
						exponent = (int)Math.ceil(log(newLogBase, max));
						//yAxis.maxValue.setValue(Math.pow(getLogarithmicBaseY(), exponent)); //not really needed
						newMaxPow = exponent;
						
						//sort out minimum power
						if(minPositive != Double.POSITIVE_INFINITY)
						{
							exponent = (int)Math.ceil(log(newLogBase, minPositive));
							newMinPow = exponent-1;
						}
						else
						{
							newMinPow = newMaxPow-1;
						}
					}
					else
					{
						newMaxPow = 1;
						newMinPow = 0;
					}
				}
				else
				{
					
					newMaxPow = 1;
					newMaxPow = 0;
				}
				
				
				// System.out.println("Getting as far as the setCHanged notifyObservers()");
				
				
			}
			
			if(newMax == UNDEFINED)
				newMax = 1.0;
			if(newMin == UNDEFINED)
				newMin = 0;
			if(newMajor == UNDEFINED)
				newMajor = newMax / 10.0;
			if(newMinor == UNDEFINED)
				newMinor = newMax / 50.0;
			
			//test for changes, if there were any notify observers
			boolean changeMade = false;
			axis.activated = false;
			if(previousMax != newMax)
			{
				changeMade = true;
				axis.getMaxValue().setValue(new Double(newMax));
			}
			if(previousMin != newMin)
			{
				changeMade = true;
				axis.getMinValue().setValue(new Double(newMin));
			}
			if(previousMajor != newMajor)
			{
				changeMade = true;
				axis.getMajorGridInterval().setValue(new Double(newMajor));
			}
			if(previousMinor != newMinor)
			{
				changeMade = true;
				axis.getMinorGridInterval().setValue(new Double(newMinor));
			}
			if(previousLogBase != newLogBase)
			{
				changeMade = true;
				axis.getLogBase().setValue(new Double(newLogBase));
			}
			if(previousMinPow != newMinPow)
			{
				changeMade = true;
				axis.getMinimumPower().setValue(new Integer((int)newMinPow));
			}
			if(previousMaxPow != newMaxPow)
			{
				changeMade = true;
				axis.getMaximumPower().setValue(new Integer((int)newMaxPow));
			}
			axis.activated = true;
			if(changeMade)
			{
				//setChanged();
				//notifyObservers(null);
			}
		}
		
		
	}
	
	/** Sets the axis heading for the x-axis. */
	public void setXTitle(String x) throws SettingException
	{
		try
		{
			xAxis.heading.setValue(x);
		}
		catch(Exception e)
		{}
		setChanged();
		
	}
	
	/** Sets the axis heading for the y-axis. */
	public void setYTitle(String y) throws SettingException
	{
		try
		{
			yAxis.heading.setValue(y);
		}
		catch(Exception e)
		{}
		setChanged();
		
	}
	
	/** Sets the title of the chart to the given string. */
	public void setGraphTitle(String t) throws SettingException
	{
		try
		{
			graphTitle.setValue(t);
		}
		catch(SettingException e)
		{
		}
		setChanged();
		notifyObservers(null);
		
	}
	
	
	public GraphOptionsPanel gop;
	public void tellModelAboutGraphOptionsPanel(GraphOptionsPanel gop)
	{
		this.gop = gop;
		//		graphTitle.addObserver(gop);
		//
		//		for(int i = 0; i < getNoGraphs(); i++)
		//			getGraphPoints(i).tellSeriesHeadingAboutGraphOptionsPanel(gop);
	}
	
	
	
	/** Sets the interval in pixels between minor x gridlines. */
	public void setMinorXInterval(double c) throws SettingException
	{
		xAxis.minorGridInterval.setValue(new Double(c));
		setChanged();
		
	}
	
	/** Sets the interval in pixels between minor y gridlines. */
	public void setMinorYInterval(double c) throws SettingException
	{
		yAxis.minorGridInterval.setValue(new Double(c));
		setChanged();
		
	}
	
	/** Sets the interval in pixels between major x gridlines. */
	public void setMajorXInterval(double c) throws SettingException
	{
		xAxis.majorGridInterval.setValue(new Double(c));
		setChanged();
	}
	
	
	
	/** Sets the interval in pixels between major y gridlines. */
	public void setMajorYInterval(double c) throws SettingException
	{
		yAxis.majorGridInterval.setValue(new Double(c));
		setChanged();
		
	}
	
	/** Sets the size of the top border. */
	public void setBorderSizeTop(int s) throws SettingException
	{
		topBorder.setOffset(s);setChanged();
	}
	
	/** Sets the size of the bottom border. */
	public void setBorderSizeBottom(int s) throws SettingException
	{
		bottomBorder.setOffset(s);setChanged();
	}
	
	/** Sets the size of the left border. */
	public void setBorderSizeLeft(int s) throws SettingException
	{
		leftBorder.setOffset(s);setChanged();
	}
	
	/** Sets the size of the right border. */
	public void setBorderSizeRight(int s) throws SettingException
	{
		rightBorder.setOffset(s);setChanged();
	}
	
	
	// Other
	
	
	/** Static method used to calculate logs to a given base.
	 * @param base The logarithmic base to be used.
	 * @param x the value of which to calculate the log
	 * @return the log of the given value to the given base.
	 */
	public static double log(double base, double x)
	{
		return Math.log(x) / Math.log(base);
	}
	
	//Property Owner Stuff
	
	//One for each type of owner
	public int getSettingOwnerID()
	{
		return prism.PropertyConstants.GRAPH;
	}
	//One for each owner
	//This will be displayed when only
	//this owner is being displayed.
	public String getSettingOwnerName()
	{
		return graphTitle.getStringValue();
	}
	//ONe for each type of property collection
	//When only one owner is being displayed,
	//we see the result of this method, and then
	//the result of getDescriptor.
	//If there is more than one owner being
	//displayed, we see the number of
	//owners, then the result of this method
	//followed by an "s".
	public String getSettingOwnerClassName()
	{
		return "Graph";
	}
	
	public int getNumSettings()
	{
		return 12;
	}
	
	public Setting getSetting(int index)
	{
		switch(index)
		{
			case 0: return graphTitle;
			case 1: return titleFont;
			case 2: return legendVisible;
			case 3: return legendPosition;
			case 4: return legendPositionX;
			case 5: return legendPositionY;
			case 6: return legendFont;
			case 7: return autoborder;
			case 8: return topBorder.getOffsetProperty();
			case 9: return bottomBorder.getOffsetProperty();
			case 10: return leftBorder.getOffsetProperty();
			case 11: return rightBorder.getOffsetProperty();
			default: return null;
		}
	}
	
	public void registerObserver(Observer obs)
	{
		//		graphTitle.addObserver(obs);
		//		titleFont.addObserver(obs);
	}
	
	public int compareTo(Object o)
	{
		if(o instanceof SettingOwner)
		{
			SettingOwner po = (SettingOwner) o;
			if(getSettingOwnerID() < po.getSettingOwnerID() )return -1;
			else if(getSettingOwnerID() > po.getSettingOwnerID() ) return 1;
			else return 0;
		}
		else return 0;
	}
	
	int lastAutoBorderValue = -1; //-1 = undetermined, 0 = false, 1 = true
	int lastLegendPosition = -1; //-1 = undetermined
   /* public void update(Observable o, Object arg)
	{
//
//		if(autoborder == null) //things haven't been assignged
//			return;
//		//System.out.println("Calling main update, should sort out the autoborder stuff");
//		if(autoborder != null && autoborder.getBooleanValue())
//		{
//			//if(lastAutoBorderValue == -1 || lastAutoBorderValue == 1)
//			{
//				//System.out.println("telling em to disable");
//				bottomBorder.getOffsetProperty().setEnabled(false);
//				topBorder.getOffsetProperty().setEnabled(false);
//				leftBorder.getOffsetProperty().setEnabled(false);
//				rightBorder.getOffsetProperty().setEnabled(false);
//				lastAutoBorderValue = 0;
//			}
//		}
//		else
//		{
//			//if(lastAutoBorderValue == -1 || lastAutoBorderValue == 0)
//			{
//				bottomBorder.getOffsetProperty().setEnabled(true);
//				topBorder.getOffsetProperty().setEnabled(true);
//				leftBorder.getOffsetProperty().setEnabled(true);
//				rightBorder.getOffsetProperty().setEnabled(true);
//				lastAutoBorderValue = 0;
//			}
//
//		}
//
//		if(legendPosition == null) return;
//		if(legendVisible.getBooleanValue())
//		{
//			legendPosition.setEnabled(true);
//			switch(legendPosition.getCurrentIndex())
//			{
//				case LEFT:
//				case RIGHT:
//				case BOTTOM:
//
//
//					legendPositionX.setEnabled(false);
//					legendPositionY.setEnabled(false);
//					lastLegendPosition = legendPosition.getCurrentIndex();
//
//					break;
//
//				case MANUAL:
//
//					legendPositionX.setEnabled(true);
//					legendPositionY.setEnabled(true);
//					lastLegendPosition = legendPosition.getCurrentIndex();
//
//			}
//		}
//		else
//		{
//			legendPositionX.setEnabled(false);
//			legendPositionY.setEnabled(false);
//			legendPosition.setEnabled(false);
//		}
	
	
	
	
//	setChanged();
//	notifyObservers(null);
	}*/
	
	/**
	 * Getter for property xAxis.
	 * @return Value of property xAxis.
	 */
	public chart.MultiGraphModel.AxisOwner getXAxis()
	{
		return xAxis;
	}
	
	/**
	 * Setter for property xAxis.
	 * @param xAxis New value of property xAxis.
	 */
	public void setXAxis(chart.MultiGraphModel.AxisOwner xAxis)
	{
		this.xAxis = xAxis;
	}
	
	/**
	 * Getter for property yAxis.
	 * @return Value of property yAxis.
	 */
	public chart.MultiGraphModel.AxisOwner getYAxis()
	{
		return yAxis;
	}
	
	/**
	 * Setter for property yAxis.
	 * @param yAxis New value of property yAxis.
	 */
	public void setYAxis(chart.MultiGraphModel.AxisOwner yAxis)
	{
		this.yAxis = yAxis;
	}
	
	/**
	 * Getter for property seriesList.
	 * @return Value of property seriesList.
	 */
	public chart.SeriesList getSeriesList()
	{
		return seriesList;
	}
	
	/**
	 * Setter for property seriesList.
	 * @param seriesList New value of property seriesList.
	 */
	public void setSeriesList(chart.SeriesList seriesList)
	{
		this.seriesList = seriesList;
	}
	
	public void doChange() throws SettingException
	{
		//System.out.println("Do change called");
		workOutAndSetXScales();
		workOutAndSetYScales();
		setChanged();
		notifyObservers(null);
	}
	
	//To make this a ListModel
	protected EventListenerList listenerList = new EventListenerList();
	public void addListDataListener(javax.swing.event.ListDataListener l)
	{
		listenerList.add(ListDataListener.class, l);
	}
	
	public Object getElementAt(int index)
	{
		return graphs.get(index);
	}
	
	public int getSize()
	{
		return graphs.size();
	}
	
	public void removeListDataListener(javax.swing.event.ListDataListener l)
	{
		listenerList.remove(ListDataListener.class, l);
	}
	
	/**
	 * <code>AbstractListModel</code> subclasses must call this method
	 * <b>after</b>
	 * one or more elements of the list change.  The changed elements
	 * are specified by the closed interval index0, index1 -- the endpoints
	 * are included.  Note that
	 * index0 need not be less than or equal to index1.
	 *
	 * @param source the <code>ListModel</code> that changed, typically "this"
	 * @param index0 one end of the new interval
	 * @param index1 the other end of the new interval
	 * @see EventListenerList
	 * @see DefaultListModel
	 */
	protected void fireContentsChanged(Object source, int index0, int index1)
	{
		Object[] listeners = listenerList.getListenerList();
		ListDataEvent e = null;
		
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ListDataListener.class)
			{
				if (e == null)
				{
					e = new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED, index0, index1);
				}
				((ListDataListener)listeners[i+1]).contentsChanged(e);
			}
		}
	}
	
	
	/**
	 * <code>AbstractListModel</code> subclasses must call this method
	 * <b>after</b>
	 * one or more elements are added to the model.  The new elements
	 * are specified by a closed interval index0, index1 -- the enpoints
	 * are included.  Note that
	 * index0 need not be less than or equal to index1.
	 *
	 * @param source the <code>ListModel</code> that changed, typically "this"
	 * @param index0 one end of the new interval
	 * @param index1 the other end of the new interval
	 * @see EventListenerList
	 * @see DefaultListModel
	 */
	protected void fireIntervalAdded(Object source, int index0, int index1)
	{
		Object[] listeners = listenerList.getListenerList();
		ListDataEvent e = null;
		
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ListDataListener.class)
			{
				if (e == null)
				{
					e = new ListDataEvent(source, ListDataEvent.INTERVAL_ADDED, index0, index1);
				}
				((ListDataListener)listeners[i+1]).intervalAdded(e);
			}
		}
	}
	
	
	/**
	 * <code>AbstractListModel</code> subclasses must call this method
	 * <b>after</b> one or more elements are removed from the model.
	 * The new elements
	 * are specified by a closed interval index0, index1, i.e. the
	 * range that includes both index0 and index1.  Note that
	 * index0 need not be less than or equal to index1.
	 *
	 * @param source the ListModel that changed, typically "this"
	 * @param index0 one end of the new interval
	 * @param index1 the other end of the new interval
	 * @see EventListenerList
	 * @see DefaultListModel
	 */
	protected void fireIntervalRemoved(Object source, int index0, int index1)
	{
		Object[] listeners = listenerList.getListenerList();
		ListDataEvent e = null;
		
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ListDataListener.class)
			{
				if (e == null)
				{
					e = new ListDataEvent(source, ListDataEvent.INTERVAL_REMOVED, index0, index1);
				}
				((ListDataListener)listeners[i+1]).intervalRemoved(e);
			}
		}
	}
	
	public class AxisOwner implements SettingOwner, Observer
	{
		
		private String name;
		
		SingleLineStringSetting heading;
		FontColorSetting headingFont;
		FontColorSetting numberFont;
		BooleanSetting showMajor;
		BooleanSetting showMinor;
		ColorSetting majorColour;
		ColorSetting minorColour;
		ChoiceSetting logarithmic;
		BooleanSetting autoScale;
		DoubleSetting minValue;
		DoubleSetting maxValue;
		DoubleSetting majorGridInterval;
		DoubleSetting minorGridInterval;
		IntegerSetting minimumPower;
		IntegerSetting maximumPower;
		DoubleSetting logBase;
		ChoiceSetting logStyle;
		
		boolean activated = false;
		
		public AxisOwner(String name)
		{
			this.name = name;
			
			heading = new SingleLineStringSetting("heading", name, "The heading for this axis", this, false);
			headingFont = new FontColorSetting("heading font", new FontColorPair(new Font("SansSerif", Font.PLAIN, 12), Color.black), "The font for this axis' heading.", this, false);
			numberFont = new FontColorSetting("numbering font", new FontColorPair(new Font("SansSerif", Font.PLAIN, 12), Color.black), "The font used to number the axis.", this, false);
			showMajor = new BooleanSetting("show major gridlines", new Boolean(true), "Should the major gridlines be visible", this, false);
			showMinor = new BooleanSetting("show minor gridlines", new Boolean(true), "Should the minor gridlines be visible", this, false);
			majorColour = new ColorSetting("major gridline colour", new Color(199,244,255), "The colour of the major gridlines", this, false);
			minorColour = new ColorSetting("minor gridline colour", new Color(240,240,240), "The colour of the minor gridlines", this, false);
			String[] choices =
			{"Normal", "Logarithmic"};
			logarithmic = new ChoiceSetting("scale type", choices, choices[0], "Should the scale be normal, or logarithmic", this, false);
			autoScale = new BooleanSetting("auto-scale", new Boolean(true), "When set to true, all minimum values, maximum values, grid intervals, maximum logarithmic powers and minimum logarithmic powers are automatically set and maintained when the data changes.", this, false);
			minValue = new DoubleSetting("minimum value", new Double(0.0), "The minimum value for the axis", this, false);
			maxValue = new DoubleSetting("maximum value", new Double(1.0), "The maximum value for the axis", this, false);
			majorGridInterval = new DoubleSetting("major gridline interval", new Double(0.2), "The interval between major gridlines", this, false, new RangeConstraint(0, Double.POSITIVE_INFINITY, false, true));
			minorGridInterval = new DoubleSetting("minor gridline interval", new Double(0.05), "The inerval between minor gridlines", this, false, new RangeConstraint(0, Double.POSITIVE_INFINITY, false, true));
			logBase = new DoubleSetting("log base", new Double(10), "The base for the logarithmic scale", this, false, new RangeConstraint("2,"));
			
			minimumPower = new IntegerSetting("minimum power", new Integer("0"), "The minimum logarithmic power that should be displayed on the scale", this, false);
			maximumPower = new IntegerSetting("maximum power", new Integer("1"), "The maximum logarithmic power that should be displayed on the scale", this, false);
			String[] choices2 =
			{"Values", "Base and exponent"};
			logStyle = new ChoiceSetting("logarithmic number style", choices2, choices2[1], "Should the style of the logarithmic scale show the actual values, or the base with the exponent.", this, false);
			
			minValue.addConstraint(new NumericConstraint()
			{
				public void checkValueDouble(double d) throws SettingException
				{
					if(activated && d >= maxValue.getDoubleValue()) throw new SettingException("Minimum value should be < Maximum value");
				}
				
				public void checkValueInteger(int i) throws SettingException
				{
					if(activated && i >= maxValue.getDoubleValue()) throw new SettingException("Minimum value should be < Maximum value");
				}
			});
			maxValue.addConstraint(new NumericConstraint()
			{
				public void checkValueDouble(double d) throws SettingException
				{
					if(activated && d <= minValue.getDoubleValue()) throw new SettingException("Maximum value should be > Minimum value");
				}
				
				public void checkValueInteger(int i) throws SettingException
				{
					if(activated && i <= minValue.getDoubleValue()) throw new SettingException("Maximum value should be > Minimum value");
				}
			});
			minimumPower.addConstraint(new NumericConstraint()
			{
				public void checkValueDouble(double d) throws SettingException
				{
					if(activated && d >= maximumPower.getIntegerValue()) throw new SettingException("Minimum power should be < Maximum power");
				}
				
				public void checkValueInteger(int i) throws SettingException
				{
					if(activated && i >= maximumPower.getIntegerValue()) throw new SettingException("Minimum power should be < Maximum power");
				}
			});
			maximumPower.addConstraint(new NumericConstraint()
			{
				public void checkValueDouble(double d) throws SettingException
				{
					if(activated && d <= minimumPower.getIntegerValue()) throw new SettingException("Maximum power should be > Minimum power");
				}
				
				public void checkValueInteger(int i) throws SettingException
				{
					if(activated && i <= minimumPower.getIntegerValue()) throw new SettingException("Maximum power should be > Minimum power");
				}
			});
			
			
			doEnables();
			
			//logarithmic.addObserver(this);
			//autoScale.addObserver(this);
			//logBase.addObserver(this);
			
			activated = true;
		}
		
		public int compareTo(Object o)
		{
			if(o instanceof SettingOwner)
			{
				SettingOwner po = (SettingOwner) o;
				if(getSettingOwnerID() < po.getSettingOwnerID() )return -1;
				else if(getSettingOwnerID() > po.getSettingOwnerID()) return 1;
				else return 0;
			}
			else return 0;
		}
		
		public String getClassDescriptor()
		{
			return "Axis";
		}
		
		public String getDescriptor()
		{
			return name;
		}
		
		public int getNumSettings()
		{
			return 17;
		}
		
		public String getSettingOwnerName()
		{
			return heading.getStringValue();
		}
		
		public Setting getSetting(int index)
		{
			switch(index)
			{
				case 0: return heading;
				case 1:return headingFont;
				case 2:return numberFont;
				case 3:return showMajor;
				case 4:return showMinor;
				case 5:return majorColour;
				case 6:return minorColour;
				case 7:return logarithmic;
				case 8:return autoScale;
				case 9:return minValue;
				case 10:return maxValue;
				case 11:return majorGridInterval;
				case 12:return minorGridInterval;
				case 13:return minimumPower;
				case 14:return maximumPower;
				case 15:return logBase;
				case 16:return logStyle;
				default: return null;
				
				
				
			}
		}
		
		public int getSettingOwnerID()
		{
			return prism.PropertyConstants.AXIS;
		}
		
		public String getSettingOwnerClassName()
		{
			return "Axis";
		}
		
		public void registerObserver(Observer obs)
		{
		}
		
		public void doEnables()
		{
			minimumPower.setEnabled(!autoScale.getBooleanValue() && logarithmic.getCurrentIndex()==1);
			maximumPower.setEnabled(!autoScale.getBooleanValue() && logarithmic.getCurrentIndex()==1);
			logBase.setEnabled(logarithmic.getCurrentIndex()==1);
			logStyle.setEnabled(logarithmic.getCurrentIndex()==1);
			minValue.setEnabled(!autoScale.getBooleanValue() && logarithmic.getCurrentIndex()==0);
			maxValue.setEnabled(!autoScale.getBooleanValue() && logarithmic.getCurrentIndex()==0);
			majorGridInterval.setEnabled(!autoScale.getBooleanValue() && logarithmic.getCurrentIndex()==0);
			minorGridInterval.setEnabled(!autoScale.getBooleanValue() && logarithmic.getCurrentIndex()==0);
		}
		
		//private int lastScaleValue = -1; //-1 = undefined, 0 = normal, 1 = logarithmic
		
		public void notifySettingChanged(Setting setting)
		{
			try
			{
				/*if(setting == logBase)
				{
					workOutAndSetXScales();
					workOutAndSetYScales();
				 
					setChanged();
					notifyObservers(null);
					return;
				}*/
				/*if(autoScale.getBooleanValue())
				{*/
				doEnables();
				
				//if(lastScaleValue == 0 || lastScaleValue == 1)
				//{
				workOutAndSetXScales();
				workOutAndSetYScales();
				//}
				//else
				//{
				
				//}
				
				//lastScaleValue = -1;
				
				/*}
				else
				{
					if(logarithmic.getCurrentIndex() == 0)//normal
					{
						if(lastScaleValue == -1 || lastScaleValue == 1)
						{
							logBase.setEnabled(false);
							logStyle.setEnabled(false);
							minimumPower.setEnabled(false);
							maximumPower.setEnabled(false);
							minValue.setEnabled(true);
							maxValue.setEnabled(true);
							majorGridInterval.setEnabled(true);
							minorGridInterval.setEnabled(true);
							lastScaleValue = 0;
						}
					}
					else//logarithmic
					{
						if(lastScaleValue == -1 || lastScaleValue == 0)
						{
							logBase.setEnabled(true);
							logStyle.setEnabled(true);
							minimumPower.setEnabled(true);
							maximumPower.setEnabled(true);
							minValue.setEnabled(false);
							maxValue.setEnabled(false);
							majorGridInterval.setEnabled(false);
							minorGridInterval.setEnabled(false);
							lastScaleValue = 1;
						}
					}*/
				//}
				
				//if(activated)
				//{
				//now check for dodgy updates to scales
					/*if(minimumPower.getIntegerValue() >= maximumPower.getIntegerValue())
					{
						double v1 = minimumPower.getIntegerValue();
						double v2 = maximumPower.getIntegerValue();
						minimumPower.setValue(new Integer((int)(maximumPower.getIntegerValue()-1.0)));
						//JFrame f = GUIPrism.getGUI();
					 
						//JOptionPane.showMessageDialog(f,
						//	"Maximum power <= Minimum power "+v1+" "+v2+"\nMinimum power has been adjusted",
						//	"Invalid Input", JOptionPane.ERROR_MESSAGE);
						if(gop!=null) gop.errorDialog("Maximum power <= Minimum power "+v1+" "+v2+"\nMinimum power has been adjusted", "Invalid Input");
					}*/
				
					/*if(minValue.getDoubleValue() >= maxValue.getDoubleValue())
					{
						minValue.setValue(new Double(maxValue.getDoubleValue()-1));
					 
										/*JFrame f = GUIPrism.getGUI();
					 
										JOptionPane.showMessageDialog(f,
												"Maximum value <= Minimum value\nMinimum value has been adjusted",
												"Invalid Input", JOptionPane.ERROR_MESSAGE);*//*
						if(gop!=null) gop.errorDialog("Maximum value <= Minimum value\nMinimum value has been adjusted", "Invalid Input");
					}*/
				
					/*if(majorGridInterval.getDoubleValue() <= 0)
					{
						majorGridInterval.setValue(new Double(1));
					 
										/*JFrame f = GUIPrism.getGUI();
					 
										JOptionPane.showMessageDialog(f,
												"Negative grid interval entered\nValue has been adjusted to 1",
												"Invalid Input", JOptionPane.ERROR_MESSAGE);*//*
						if(gop!=null) gop.errorDialog("Negative grid interval entered\nValue has been adjusted to 1", "Invalid Input");
					}*/
					/*
					if(minorGridInterval.getDoubleValue() <=0)
					{
						minorGridInterval.setValue(new Double(1));
					 
										/*JFrame f = GUIPrism.getGUI();
					 
										JOptionPane.showMessageDialog(f,
												"Negative grid interval entered\nValue has been adjusted to 1",
												"Invalid Input", JOptionPane.ERROR_MESSAGE);*//*
						if(gop!=null) gop.errorDialog("Negative grid interval entered\nValue has been adjusted to 1", "Invalid Input");
					}*/
				
				
				//}
			}
			catch(SettingException e)
			{
				if(gop!=null) gop.errorDialog(e.getMessage(), "Setting Error");
			}
			
			setChanged();
			notifyObservers(null);
		}
		
		
		public void update(Observable o, Object arg)
		{
			//System.out.println("Axis recognised change in something");
			
			
		}
		
		/**
		 * Getter for property heading.
		 * @return Value of property heading.
		 */
		public SingleLineStringSetting getHeading()
		{
			return heading;
		}
		
		
		
		/**
		 * Getter for property headingFont.
		 * @return Value of property headingFont.
		 */
		public FontColorSetting getHeadingFont()
		{
			return headingFont;
		}
		
		
		
		/**
		 * Getter for property numberFont.
		 * @return Value of property numberFont.
		 */
		public FontColorSetting getNumberFont()
		{
			return numberFont;
		}
		
		
		
		/**
		 * Getter for property showMajor.
		 * @return Value of property showMajor.
		 */
		public BooleanSetting getShowMajor()
		{
			return showMajor;
		}
		
		
		
		/**
		 * Getter for property showMinor.
		 * @return Value of property showMinor.
		 */
		public BooleanSetting getShowMinor()
		{
			return showMinor;
		}
		
		
		
		/**
		 * Getter for property majorColour.
		 * @return Value of property majorColour.
		 */
		public ColorSetting getMajorColour()
		{
			return majorColour;
		}
		
		
		
		/**
		 * Getter for property minorColour.
		 * @return Value of property minorColour.
		 */
		public ColorSetting getMinorColour()
		{
			return minorColour;
		}
		
		
		
		/**
		 * Getter for property logarithmic.
		 * @return Value of property logarithmic.
		 */
		public ChoiceSetting getLogarithmic()
		{
			return logarithmic;
		}
		
		public ChoiceSetting getLogStyle()
		{
			return logStyle;
		}
		
		
		
		/**
		 * Getter for property minValue.
		 * @return Value of property minValue.
		 */
		public DoubleSetting getMinValue()
		{
			return minValue;
		}
		
		
		
		/**
		 * Getter for property maxValue.
		 * @return Value of property maxValue.
		 */
		public DoubleSetting getMaxValue()
		{
			return maxValue;
		}
		
		
		
		/**
		 * Getter for property majorGridInterval.
		 * @return Value of property majorGridInterval.
		 */
		public DoubleSetting getMajorGridInterval()
		{
			return majorGridInterval;
		}
		
		
		
		/**
		 * Getter for property minorGridInterval.
		 * @return Value of property minorGridInterval.
		 */
		public DoubleSetting getMinorGridInterval()
		{
			return minorGridInterval;
		}
		
		
		
		/**
		 * Getter for property logBase.
		 * @return Value of property logBase.
		 */
		public DoubleSetting getLogBase()
		{
			return logBase;
		}
		
		
		
		/**
		 * Getter for property minimumPower.
		 * @return Value of property minimumPower.
		 */
		public IntegerSetting getMinimumPower()
		{
			return minimumPower;
		}
		
		/**
		 * Getter for property maximumPower.
		 * @return Value of property maximumPower.
		 */
		public IntegerSetting getMaximumPower()
		{
			return maximumPower;
		}
		
		SettingDisplay disp;
		
		public SettingDisplay getDisplay()
		{
			return disp;
		}
		
		public void setDisplay(SettingDisplay display)
		{
			disp = display; //TODO sort this out
		}
		
	}
	
	// matlab exporter
	
	public void exportToMatlab(File f)throws IOException
	{
		PrintWriter out = new PrintWriter(new FileWriter(f));
		
		out.println("%=========================================");
		out.println("%Generated by PRISM Chart Package");
		out.println("%=========================================");
		out.println();
		
		//Seriesdata
		
		for(int i = 0; i < getNoGraphs(); i++)
		{
			GraphList gs = getGraphPoints(i);
			StringBuffer x = new StringBuffer("x"+i+" = [");
			StringBuffer y = new StringBuffer("y"+i+" = [");
			for(int j = 0; j < gs.size(); j++)
			{
				GraphPoint p = gs.getPoint(j);
				x.append(p.getXCoord()+" ");
				y.append(p.getYCoord()+" ");
			}
			x.append("];");
			y.append("];");
			out.println(x.toString());
			out.println(y.toString());
		}
		
		//Create a figure
		out.println();
		out.println("figure1 = figure('Color', [1 1 1], 'PaperPosition',[0.6345 6.345 20.3 15.23],'PaperSize',[20.98 29.68]);");
		
		//Create axes
		boolean xLog =
		(getXAxis().getLogarithmic().getCurrentIndex() == 1);
		boolean yLog =
		(getYAxis().getLogarithmic().getCurrentIndex() == 1);
		out.println();
		if(xLog && yLog)
			out.println("axes1 = axes('Parent',figure1, 'XScale','log','YScale','log');");
		else if(xLog)
			out.println("axes1 = axes('Parent',figure1, 'XScale','log');");
		else if(yLog)
			out.println("axes1 = axes('Parent',figure1, 'YScale','log');");
		else
			out.println("axes1 = axes('Parent',figure1);");
		
		out.println("xlabel(axes1,'"+getXAxis().heading.getStringValue()+"');");
		out.println("ylabel(axes1,'"+getYAxis().heading.getStringValue()+"');");
		out.println("box(axes1, 'on');");
		out.println("hold(axes1, 'all');");
		
		//Graph title
		out.println();
		String title = "";
		StringTokenizer st = new StringTokenizer(getGraphTitle(), "\n");
		
		int num = st.countTokens();
		for(int i = 0; i < num; i++)
		{
			title += "'"+ st.nextToken()+"'";
			if(i < num - 1) title += ", char(10),";
		}
		
		out.println("title(axes1,["+title+"])");
		
		//Sort out logarithmic scales
		String scaleType = "plot";
		if(getNoGraphs() > 0)
		{
			if(xLog && yLog)
				scaleType = "loglog";
			else if(xLog)
				scaleType = "semilogx";
			else if(yLog)
				scaleType = "semilogy";
		}
		
		//Create plots
		for(int i = 0; i < getNoGraphs(); i++)
		{
			GraphList gl = getGraphPoints(i);
			String marker = "'";
			if(gl.isDrawPoint())
			{
				switch(gl.getPointIndex())
				{
					case GraphList.CIRCLE: marker += "o";break;
					case GraphList.SQUARE: marker += "s";break;
					case GraphList.TRIANGLE: marker += "^";break;
					case GraphList.RECTANGLE_H: marker += "d"; break;
					case GraphList.RECTANGLE_V: marker += "x"; break;
				}
			}
			if(gl.isShowLines())
			{
				switch(gl.getLineStyle())
				{
					case 0: marker += "-";break;
					case 1: marker += "--";break;
					case 2: marker += "-.";break;
				}
			}
			marker+="'";
			out.println("plot"+i+" = "+scaleType+"(x"+i+", y"+i+", "+marker+", 'Parent', axes1);");
		}
		
		//Create legend
		String seriesNames = "";
		for(int i = 0; i < getNoGraphs(); i++)
		{
			seriesNames+="'"+getGraphPoints(i).getGraphTitle()+"'";
			if(i < getNoGraphs() - 1) seriesNames+=", ";
		}
		
		//Determine location
		
		String loc = "";
		switch(legendPosition.getCurrentIndex())
		{
			case 0: loc = "'WestOutside'";break;
			case 1: loc = "'EastOutside'";break;
			case 2: loc = "'SouthOutside'";break;
			case 3: loc = "'EastOutside'";break;
		}
		
		if(legendVisible.getBooleanValue())
			out.println("legend1 = legend(axes1,{"+seriesNames+"},'Location', "+loc+");");
		
		out.flush();
		out.close();
	}
	
	// saving methods
	
	public MultiGraphModel load(File f) throws Exception
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(true);
			factory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(this);
			Document doc = builder.parse(f);
			Element chartFormat = doc.getDocumentElement();
			
			//graph title property
			setGraphTitle(chartFormat.getAttribute("graphTitle"));
			
			//titleFont property
			String titleFontName = chartFormat.getAttribute("titleFontName");
			String titleFontSize = chartFormat.getAttribute("titleFontSize");
			String titleFontStyle = chartFormat.getAttribute("titleFontStyle");
			int style, size = 0;
			try
			{
				size = Integer.parseInt(titleFontSize);
				style = Integer.parseInt(titleFontStyle);
			}
			catch(NumberFormatException e)
			{
				size = 14;
				style = Font.PLAIN;
				
				//throw new Exception ("a titleFont property has invalid value which should be an integer.");
			}
			if(size <=0 ) size = 12;
			if(titleFontName.equals("")) titleFontName = "SansSerif";
			
			Font titleFont = new Font(titleFontName, style, size);
			setTitleFont(titleFont);
			String titleFontColourR = chartFormat.getAttribute("titleFontColourR");
			String titleFontColourG = chartFormat.getAttribute("titleFontColourG");
			String titleFontColourB = chartFormat.getAttribute("titleFontColourB");
			int r,g,b = 0;
			try
			{
				r = Integer.parseInt(titleFontColourR);
				g = Integer.parseInt(titleFontColourG);
				b = Integer.parseInt(titleFontColourB);
				
				if(r > 255) r = 255;
				if(r < 0) r = 0;
				if(g > 255) g = 255;
				if(g < 0) g = 0;
				if(b > 255) b = 255;
				if(b < 0) b = 0;
			}
			catch(NumberFormatException e)
			{
				r = 0;
				g = 0;
				b = 0;
				//throw new Exception ("a titleFontColour property has invalid value which should be an integer.");
			}
			Color titleFontColour = new Color(r,g,b);
			setTitleColour(titleFontColour);
			
			
			//legend visible property
			boolean legendVisible = ("true").equals(chartFormat.getAttribute("legendVisible"));
			setLegendVisible(legendVisible);
			
			//Legend position property
			String legendPosition = chartFormat.getAttribute("legendPosition");
			int pos = 0;
			if(legendPosition.equals("left")) pos = 0;
			else if(legendPosition.equals("bottom")) pos = 1;
			else if(legendPosition.equals("right")) pos = 2;
			else pos = 3;
			setLegendPosition(pos);
			
			//Legend position x and y properties
			String legendPositionX = chartFormat.getAttribute("legendPositionX");
			String legendPositionY = chartFormat.getAttribute("legendPositionY");
			
			double legX, legY = 0.0;
			try
			{
				legX = Double.parseDouble(legendPositionX);
				legY = Double.parseDouble(legendPositionY);
			}
			catch(NumberFormatException e)
			{
				legX = 50.0;
				legY = 50.0;
				//throw new Exception ("a legendPosition property has invalid value which should be a real number.");
			}
			setLegendPositionX(legX);
			setLegendPositionY(legY);
			
			//legendFont property
			String legendFontName = chartFormat.getAttribute("legendFontName");
			String legendFontSize = chartFormat.getAttribute("legendFontSize");
			String legendFontStyle = chartFormat.getAttribute("legendFontStyle");
			style=0;
			size = 0;
			try
			{
				size = Integer.parseInt(legendFontSize);
				style = Integer.parseInt(legendFontStyle);
			}
			catch(NumberFormatException e)
			{
				size = 11;
				style = Font.PLAIN;
				
				//throw new Exception ("a titleFont property has invalid value which should be an integer.");
			}
			if(size <=0 ) size = 11;
			if(legendFontName.equals("")) legendFontName = "SansSerif";
			
			Font legendFont = new Font(legendFontName, style, size);
			this.legendFont.setValue(new FontColorPair(legendFont, this.legendFont.getFontColorValue().c));
			//this.legendFont.getFontColorValue().f = legendFont;
			String legendFontColourR = chartFormat.getAttribute("legendFontColourR");
			String legendFontColourG = chartFormat.getAttribute("legendFontColourG");
			String legendFontColourB = chartFormat.getAttribute("legendFontColourB");
			r=0;
			g=0;
			b = 0;
			try
			{
				r = Integer.parseInt(legendFontColourR);
				g = Integer.parseInt(legendFontColourG);
				b = Integer.parseInt(legendFontColourB);
				
				if(r > 255) r = 255;
				if(r < 0) r = 0;
				if(g > 255) g = 255;
				if(g < 0) g = 0;
				if(b > 255) b = 255;
				if(b < 0) b = 0;
			}
			catch(NumberFormatException e)
			{
				r = 0;
				g = 0;
				b = 0;
				//throw new Exception ("a titleFontColour property has invalid value which should be an integer.");
			}
			Color legendFontColour = new Color(r,g,b);
			this.legendFont.getFontColorValue().c = legendFontColour;
			//autoborder property
			boolean autoborder = (chartFormat.getAttribute("autoborder")).equals("true");
			setAutoBorder(autoborder);
			
			NodeList rootChildren = chartFormat.getChildNodes();
			
			Element layout = (Element)rootChildren.item(0);
			Element xAxis  = (Element)rootChildren.item(1);
			Element yAxis  = (Element)rootChildren.item(2);
			
			//Layout element
			
			String topOffset = layout.getAttribute("topOffset");
			String bottomOffset = layout.getAttribute("bottomOffset");
			String leftOffset = layout.getAttribute("leftOffset");
			String rightOffset = layout.getAttribute("rightOffset");
			double top,bottom,left,right = 0.0;
			try
			{
				top = Double.parseDouble(topOffset);
				bottom = Double.parseDouble(topOffset);
				left = Double.parseDouble(leftOffset);
				right = Double.parseDouble(rightOffset);
			}
			catch(NumberFormatException e)
			{
				top = bottom = left = right = 100;
				//throw new Exception ("a layout element offset property has invalid value which should be a real number.");
			}
			setBorderSizeTop((int)top);
			setBorderSizeBottom((int)bottom);
			setBorderSizeLeft((int)left);
			setBorderSizeRight((int)right);
			
			//X-axis element
			AxisOwner ao = getXAxis();
			
			ao.activated = false;
			
			//axis heading property
			String heading = xAxis.getAttribute("heading");
			ao.getHeading().setValue(heading);
			
			//axis headingFont property
			String headingFontName = xAxis.getAttribute("headingFontName");
			String headingFontSize = xAxis.getAttribute("headingFontSize");
			String headingFontStyle = xAxis.getAttribute("headingFontStyle");
			size = style = 0;
			try
			{
				size = Integer.parseInt(headingFontSize);
				style = Integer.parseInt(headingFontStyle);
			}
			catch(NumberFormatException e)
			{
				size = 12;
				style = Font.PLAIN;
				//throw new Exception ("an x-axis headingFont property has invalid value which should be an integer.");
			}
			if(size <=0 ) size = 12;
			if(headingFontName.equals("")) headingFontName = "SansSerif";
			ao.getHeadingFont().setValue(new FontColorPair(new Font(headingFontName, style, size), ao.getHeadingFont().getFontColorValue().c));
			//ao.getHeadingFont().getFontColorPair().f = new Font(headingFontName, style, size);
			String headingFontColourR = xAxis.getAttribute("headingFontColourR");
			String headingFontColourG = xAxis.getAttribute("headingFontColourG");
			String headingFontColourB = xAxis.getAttribute("headingFontColourB");
			r = g = b = 0;
			try
			{
				r = Integer.parseInt(headingFontColourR);
				g = Integer.parseInt(headingFontColourG);
				b = Integer.parseInt(headingFontColourB);
				
				if(r > 255) r = 255;
				if(r < 0) r = 0;
				if(g > 255) g = 255;
				if(g < 0) g = 0;
				if(b > 255) b = 255;
				if(b < 0) b = 0;
			}
			catch(NumberFormatException e)
			{
				r = 0;
				g = 0;
				b = 0;
				//throw new Exception ("an x-axis headingFontColour property has invalid value which should be an integer.");
			}
			ao.getHeadingFont().setValue(new FontColorPair(ao.getHeadingFont().getFontColorValue().f, new Color(r,g,b)));
			//ao.getHeadingFont().getFontColorPair().c = new Color(r,g,b);
			String numberFontName = xAxis.getAttribute("numberFontName");
			String numberFontSize = xAxis.getAttribute("numberFontSize");
			String numberFontStyle = xAxis.getAttribute("numberFontStyle");
			size = style = 0;
			try
			{
				size = Integer.parseInt(numberFontSize);
				style = Integer.parseInt(numberFontStyle);
			}
			catch(NumberFormatException e)
			{
				size = 11;
				style = Font.PLAIN;
				//throw new Exception ("an x-axis numberFont property has invalid value which should be an integer.");
			}
			if(size <=0 ) size = 12;
			if(numberFontName.equals("")) numberFontName = "SansSerif";
			ao.getNumberFont().setValue(new FontColorPair(new Font(numberFontName, style, size), ao.getNumberFont().getFontColorValue().c));
			//ao.getNumberFont().getFontColorPair().f = new Font(numberFontName, style, size);
			String numberFontColourR = xAxis.getAttribute("numberFontColourR");
			String numberFontColourG = xAxis.getAttribute("numberFontColourG");
			String numberFontColourB = xAxis.getAttribute("numberFontColourB");
			r = g = b = 0;
			try
			{
				r = Integer.parseInt(numberFontColourR);
				g = Integer.parseInt(numberFontColourG);
				b = Integer.parseInt(numberFontColourB);
				
				if(r > 255) r = 255;
				if(r < 0) r = 0;
				if(g > 255) g = 255;
				if(g < 0) g = 0;
				if(b > 255) b = 255;
				if(b < 0) b = 0;
			}
			catch(NumberFormatException e)
			{
				r = 0;
				g = 0;
				b = 0;
				//throw new Exception ("an x-axis numberFontColour property has invalid value which should be an integer.");
			}
			ao.getNumberFont().setValue(new FontColorPair(ao.getNumberFont().getFontColorValue().f, new Color(r,g,b)));
			//ao.getNumberFont().getFontColorPair().c = new Color(r,g,b);
			
			//axis show major property
			ao.getShowMajor().setValue(new Boolean(("true").equals(xAxis.getAttribute("showMajor"))));
			
			//axis show minor property
			ao.getShowMinor().setValue(new Boolean(("true").equals(xAxis.getAttribute("showMinor"))));
			
			//axis major colour property
			String majorColourR = xAxis.getAttribute("majorColourR");
			String majorColourG = xAxis.getAttribute("majorColourG");
			String majorColourB = xAxis.getAttribute("majorColourB");
			r = g = b = 0;
			try
			{
				r = Integer.parseInt(majorColourR);
				g = Integer.parseInt(majorColourG);
				b = Integer.parseInt(majorColourB);
				
				if(r > 255) r = 255;
				if(r < 0) r = 0;
				if(g > 255) g = 255;
				if(g < 0) g = 0;
				if(b > 255) b = 255;
				if(b < 0) b = 0;
			}
			catch(NumberFormatException e)
			{
				r = 0;
				g = 0;
				b = 0;
				//throw new Exception ("an x-axis majorColour property has invalid value which should be an integer.");
			}
			ao.getMajorColour().setValue(new Color(r,g,b));
			
			//axis minor colour property
			String minorColourR = xAxis.getAttribute("minorColourR");
			String minorColourG = xAxis.getAttribute("minorColourG");
			String minorColourB = xAxis.getAttribute("minorColourB");
			r = g = b = 0;
			try
			{
				r = Integer.parseInt(minorColourR);
				g = Integer.parseInt(minorColourG);
				b = Integer.parseInt(minorColourB);
				
				if(r > 255) r = 255;
				if(r < 0) r = 0;
				if(g > 255) g = 255;
				if(g < 0) g = 0;
				if(b > 255) b = 255;
				if(b < 0) b = 0;
			}
			catch(NumberFormatException e)
			{
				r = 0;
				g = 0;
				b = 0;
				//throw new Exception ("an x-axis minorColour property has invalid value which should be an integer.");
			}
			ao.getMinorColour().setValue(new Color(r,g,b));
			
			//axis isLogarihmic property
			boolean logarithmic = ("true").equals(xAxis.getAttribute("logarithmic"));
			if(logarithmic)ao.getLogarithmic().setSelectedIndex(1);
			else ao.getLogarithmic().setSelectedIndex(0);
			
			//axis autoScale property
			boolean autoScale = ("true").equals(xAxis.getAttribute("autoscale"));
			ao.autoScale.setValue(new Boolean(autoScale));
			
			//axis minValue property
			String minValue = xAxis.getAttribute("minValue");
			String maxValue = xAxis.getAttribute("maxValue");
			String majorGridInterval = xAxis.getAttribute("majorGridInterval");
			String minorGridInterval = xAxis.getAttribute("minorGridInterval");
			String logBase = xAxis.getAttribute("logBase");
			String minimumPower = xAxis.getAttribute("minimumPower");
			String maximumPower = xAxis.getAttribute("maximumPower");
			double min,max,major,minor,log,power,maxpower;
			try
			{
				min = Double.parseDouble(minValue);
			}
			catch(NumberFormatException e)
			{
				min = 0;
			}
			try
			{
				max = Double.parseDouble(maxValue);
			}
			catch(NumberFormatException e)
			{
				if(min < 1)
				{
					max = 1.0;
				}
				else
				{
					max = min + 1;
				}
			}
			try
			{
				major = Double.parseDouble(majorGridInterval);
			}
			catch(NumberFormatException e)
			{
				major = max / 5;
			}
			try
			{
				minor = Double.parseDouble(minorGridInterval);
			}
			catch(NumberFormatException e)
			{
				minor = major / 10;
			}
			try
			{
				log = Double.parseDouble(logBase);
			}
			catch(NumberFormatException e)
			{
				log = 10;
			}
			try
			{
				power = Double.parseDouble(minimumPower);
			}
			catch(NumberFormatException e)
			{
				power = 0;
			}
			try
			{
				maxpower = Double.parseDouble(maximumPower);
			}
			catch(NumberFormatException e)
			{
				if(power < 2)
					maxpower = 2;
				else
					maxpower = power+1;
				//throw new Exception ("an x-axis property has invalid value which should be a real number.");
			}
			
			ao.getMinValue().setValue(new Double(min));//, false);
			ao.getMaxValue().setValue(new Double(max));
			ao.getMajorGridInterval().setValue(new Double(major));
			ao.getMinorGridInterval().setValue(new Double(minor));
			ao.getLogBase().setValue(new Double(log));
			ao.getMinimumPower().setValue(new Integer((int)power));
			ao.getMaximumPower().setValue(new Integer((int)maxpower));
			
			String loggyStyle = xAxis.getAttribute("logStyle");
			int lStyle;
			try
			{
				lStyle = Integer.parseInt(loggyStyle);
			}
			catch(NumberFormatException e)
			{
				lStyle = 0;
			}
			
			ao.logStyle.setSelectedIndex(lStyle);
			
			ao.activated = true;
			
			/////////////////////////////////////////////////////////////
			//Y-axis element
			/////////////////////////////////////////////////////////////
			ao = getYAxis();
			
			ao.activated = false;
			
			//axis heading property
			heading = yAxis.getAttribute("heading");
			ao.getHeading().setValue(heading);
			
			//axis headingFont property
			headingFontName = yAxis.getAttribute("headingFontName");
			headingFontSize = yAxis.getAttribute("headingFontSize");
			headingFontStyle = yAxis.getAttribute("headingFontStyle");
			size = style = 0;
			try
			{
				size = Integer.parseInt(headingFontSize);
				style = Integer.parseInt(headingFontStyle);
			}
			catch(NumberFormatException e)
			{
				size = 12;
				style = Font.PLAIN;
				//throw new Exception ("an x-axis headingFont property has invalid value which should be an integer.");
			}
			if(size <=0 ) size = 12;
			if(headingFontName.equals("")) headingFontName = "SansSerif";
			ao.getHeadingFont().setValue(new FontColorPair(new Font(headingFontName, style, size), ao.getHeadingFont().getFontColorValue().c));
			//ao.getHeadingFont().getFontColorPair().f = new Font(headingFontName, style, size);
			headingFontColourR = yAxis.getAttribute("headingFontColourR");
			headingFontColourG = yAxis.getAttribute("headingFontColourG");
			headingFontColourB = yAxis.getAttribute("headingFontColourB");
			r = g = b = 0;
			try
			{
				r = Integer.parseInt(headingFontColourR);
				g = Integer.parseInt(headingFontColourG);
				b = Integer.parseInt(headingFontColourB);
				
				if(r > 255) r = 255;
				if(r < 0) r = 0;
				if(g > 255) g = 255;
				if(g < 0) g = 0;
				if(b > 255) b = 255;
				if(b < 0) b = 0;
			}
			catch(NumberFormatException e)
			{
				r = 0;
				g = 0;
				b = 0;
				//throw new Exception ("an x-axis headingFontColour property has invalid value which should be an integer.");
			}
			ao.getHeadingFont().setValue(new FontColorPair(ao.getHeadingFont().getFontColorValue().f, new Color(r,g,b)));
			//ao.getHeadingFont().getFontColorPair().c = new Color(r,g,b);
			numberFontName = yAxis.getAttribute("numberFontName");
			numberFontSize = yAxis.getAttribute("numberFontSize");
			numberFontStyle = yAxis.getAttribute("numberFontStyle");
			size = style = 0;
			try
			{
				size = Integer.parseInt(numberFontSize);
				style = Integer.parseInt(numberFontStyle);
			}
			catch(NumberFormatException e)
			{
				size = 11;
				style = Font.PLAIN;
				//throw new Exception ("an x-axis numberFont property has invalid value which should be an integer.");
			}
			if(size <=0 ) size = 12;
			if(numberFontName.equals("")) numberFontName = "SansSerif";
			ao.getNumberFont().setValue(new FontColorPair(new Font(numberFontName, style, size), ao.getHeadingFont().getFontColorValue().c));
			//ao.getNumberFont().getFontColorPair().f = new Font(numberFontName, style, size);
			numberFontColourR = yAxis.getAttribute("numberFontColourR");
			numberFontColourG = yAxis.getAttribute("numberFontColourG");
			numberFontColourB = yAxis.getAttribute("numberFontColourB");
			r = g = b = 0;
			try
			{
				r = Integer.parseInt(numberFontColourR);
				g = Integer.parseInt(numberFontColourG);
				b = Integer.parseInt(numberFontColourB);
				
				if(r > 255) r = 255;
				if(r < 0) r = 0;
				if(g > 255) g = 255;
				if(g < 0) g = 0;
				if(b > 255) b = 255;
				if(b < 0) b = 0;
			}
			catch(NumberFormatException e)
			{
				r = 0;
				g = 0;
				b = 0;
				//throw new Exception ("an x-axis numberFontColour property has invalid value which should be an integer.");
			}
			ao.getNumberFont().setValue(new FontColorPair(ao.getNumberFont().getFontColorValue().f, new Color(r,g,b)));
			//ao.getNumberFont().getFontColorPair().c = new Color(r,g,b);
			
			//axis show major property
			ao.getShowMajor().setValue(new Boolean(("true").equals(yAxis.getAttribute("showMajor"))));
			
			//axis show minor property
			ao.getShowMinor().setValue(new Boolean(("true").equals(yAxis.getAttribute("showMinor"))));
			
			//axis major colour property
			majorColourR = yAxis.getAttribute("majorColourR");
			majorColourG = yAxis.getAttribute("majorColourG");
			majorColourB = yAxis.getAttribute("majorColourB");
			r = g = b = 0;
			try
			{
				r = Integer.parseInt(majorColourR);
				g = Integer.parseInt(majorColourG);
				b = Integer.parseInt(majorColourB);
				
				if(r > 255) r = 255;
				if(r < 0) r = 0;
				if(g > 255) g = 255;
				if(g < 0) g = 0;
				if(b > 255) b = 255;
				if(b < 0) b = 0;
			}
			catch(NumberFormatException e)
			{
				r = 0;
				g = 0;
				b = 0;
				//throw new Exception ("an x-axis majorColour property has invalid value which should be an integer.");
			}
			
			ao.getMajorColour().setValue(new Color(r,g,b));
			
			//axis minor colour property
			minorColourR = yAxis.getAttribute("minorColourR");
			minorColourG = yAxis.getAttribute("minorColourG");
			minorColourB = yAxis.getAttribute("minorColourB");
			r = g = b = 0;
			try
			{
				r = Integer.parseInt(minorColourR);
				g = Integer.parseInt(minorColourG);
				b = Integer.parseInt(minorColourB);
				
				if(r > 255) r = 255;
				if(r < 0) r = 0;
				if(g > 255) g = 255;
				if(g < 0) g = 0;
				if(b > 255) b = 255;
				if(b < 0) b = 0;
			}
			catch(NumberFormatException e)
			{
				r = 0;
				g = 0;
				b = 0;
				//throw new Exception ("an x-axis minorColour property has invalid value which should be an integer.");
			}
			ao.getMinorColour().setValue(new Color(r,g,b));
			
			//axis isLogarihmic property
			logarithmic = ("true").equals(yAxis.getAttribute("logarithmic"));
			if(logarithmic)ao.getLogarithmic().setSelectedIndex(1);
			else ao.getLogarithmic().setSelectedIndex(0);
			
			//axis autoScale property
			autoScale = ("true").equals(yAxis.getAttribute("autoscale"));
			ao.autoScale.setValue(new Boolean(autoScale));
			
			//axis minValue property
			minValue = yAxis.getAttribute("minValue");
			maxValue = yAxis.getAttribute("maxValue");
			majorGridInterval = yAxis.getAttribute("majorGridInterval");
			minorGridInterval = yAxis.getAttribute("minorGridInterval");
			logBase = yAxis.getAttribute("logBase");
			minimumPower = yAxis.getAttribute("minimumPower");
			maximumPower = yAxis.getAttribute("maximumPower");
			
			try
			{
				min = Double.parseDouble(minValue);
			}
			catch(NumberFormatException e)
			{
				min = 0;
			}
			try
			{
				max = Double.parseDouble(maxValue);
			}
			catch(NumberFormatException e)
			{
				if(min < 1)
				{
					max = 1.0;
				}
				else
				{
					max = min + 1;
				}
			}
			try
			{
				major = Double.parseDouble(majorGridInterval);
			}
			catch(NumberFormatException e)
			{
				major = max / 5;
			}
			try
			{
				minor = Double.parseDouble(minorGridInterval);
			}
			catch(NumberFormatException e)
			{
				minor = major / 10;
			}
			try
			{
				log = Double.parseDouble(logBase);
			}
			catch(NumberFormatException e)
			{
				log = 10;
			}
			try
			{
				power = Double.parseDouble(minimumPower);
			}
			catch(NumberFormatException e)
			{
				power = 0;
			}
			try
			{
				maxpower = Double.parseDouble(maximumPower);
			}
			catch(NumberFormatException e)
			{
				if(power < 2)
					maxpower = 2;
				else
					maxpower = power+1;
				//throw new Exception ("an x-axis property has invalid value which should be a real number.");
			}
			ao.getMinValue().setValue(new Double(min));
			ao.getMaxValue().setValue(new Double(max));
			ao.getMajorGridInterval().setValue(new Double(major));
			ao.getMinorGridInterval().setValue(new Double(minor));
			ao.getLogBase().setValue(new Double(log));
			ao.getMinimumPower().setValue(new Integer((int)power));
			ao.getMaximumPower().setValue(new Integer((int)maxpower));
			
			loggyStyle = yAxis.getAttribute("logStyle");
			
			try
			{
				lStyle = Integer.parseInt(loggyStyle);
			}
			catch(NumberFormatException e)
			{
				lStyle = 0;
			}
			
			ao.logStyle.setSelectedIndex(lStyle);
			
			ao.activated = true;
			
			//Series (also known as Graphs)
			for(int i = 3; i < rootChildren.getLength(); i++)
			{
				Element graph = (Element)rootChildren.item(i);
				
				String seriesHeading = graph.getAttribute("seriesHeading");
				String seriesColourR = graph.getAttribute("seriesColourR");
				String seriesColourG = graph.getAttribute("seriesColourG");
				String seriesColourB = graph.getAttribute("seriesColourB");
				r = g = b = 0;
				try
				{
					r = Integer.parseInt(seriesColourR);
					g = Integer.parseInt(seriesColourG);
					b = Integer.parseInt(seriesColourB);
					
					if(r > 255) r = 255;
					if(r < 0) r = 0;
					if(g > 255) g = 255;
					if(g < 0) g = 0;
					if(b > 255) b = 255;
					if(b < 0) b = 0;
				}
				catch(NumberFormatException e)
				{
					throw new Exception(seriesHeading+" has a seriesColour property with an invalid value which should be a real number.");
				}
				Color col = new Color(r,g,b);
				String seriesShape = graph.getAttribute("seriesShape");
				int shape = 0;
				if(seriesShape.equals("circle")) shape = GraphList.CIRCLE;
				else if(seriesShape.equals("square")) shape = GraphList.SQUARE;
				else if(seriesShape.equals("triangle")) shape = GraphList.TRIANGLE;
				else if(seriesShape.equals("rectangle_h")) shape = GraphList.RECTANGLE_H;
				else if(seriesShape.equals("rectangle_v")) shape = GraphList.RECTANGLE_V;
				else if(seriesShape.equals("none")) shape = GraphList.NONE;
				else shape = GraphList.NONE;
				
				int index = addGraph(seriesHeading, col, shape);
				GraphList gl = getGraphPoints(index);
				gl.setDrawPoint(graph.getAttribute("showPoints").equals("true"));
				gl.setShowLines(graph.getAttribute("showLines").equals("true"));
				double width = 1;
				try
				{
					width = Double.parseDouble(graph.getAttribute("lineWidth"));
					if(width <= 0 ) width = 1.0;
				}
				catch(NumberFormatException e)
				{
					throw new Exception(seriesHeading+" has a lineWidth property with an invalid value which should be a real number.");
				}
				gl.setLineWidth(width);
				String lineStyle = graph.getAttribute("lineStyle");
				try
				{
					if(lineStyle.equals("normal")) gl.setLineStyle(0);
					else if(lineStyle.equals("dashed")) gl.setLineStyle(1);
					else gl.setLineStyle(2);
				}
				catch(SettingException e)
				{
					//Shouldn't ever happen
					throw new Exception("An unexpected exception has occured when setting lineStyle of "+seriesHeading);
				}
				
				
				NodeList graphChildren = graph.getChildNodes();
				
				for(int j = 0; j < graphChildren.getLength(); j++)
				{
					Element point = (Element)graphChildren.item(j);
					double x, y = 0;
					try
					{
						x = Double.parseDouble(point.getAttribute("x"));
						y = Double.parseDouble(point.getAttribute("y"));
						addPoint(index, new GraphPoint(x,y,this), false);
					}
					catch(NumberFormatException e)
					{
						// throw new Exception (seriesHeading+" has an invalid x or y value which should be a real number.");
					}
				}
			}
			
			setChanged();
			notifyObservers();
			return this;
		}
		catch(Exception e)
		{
			throw new Exception("Error in loading chart: "+e);
		}
	}
	
	public void save(File f) throws Exception
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			
			Element chartFormat = doc.createElement("chartFormat");
			
			chartFormat.setAttribute("versionString", "");//Prism.getVersion());
			chartFormat.setAttribute("graphTitle", getGraphTitle());
			Font ff = getTitleFont();
			chartFormat.setAttribute("titleFontName", ff.getName());
			chartFormat.setAttribute("titleFontSize", ""+ff.getSize());
			chartFormat.setAttribute("titleFontStyle", ""+ff.getStyle());
			Color c = getTitleColour();
			chartFormat.setAttribute("titleFontColourR", ""+c.getRed());
			chartFormat.setAttribute("titleFontColourG", ""+c.getGreen());
			chartFormat.setAttribute("titleFontColourB", ""+c.getBlue());
			chartFormat.setAttribute("legendVisible", ""+isLegendVisible());
			chartFormat.setAttribute("legendFontName", ""+ this.legendFont.getFontColorValue().f.getName());
			chartFormat.setAttribute("legendFontSize", ""+this.legendFont.getFontColorValue().f.getSize());
			chartFormat.setAttribute("legendFontStyle", ""+this.legendFont.getFontColorValue().f.getStyle());
			c = legendFont.getFontColorValue().c;
			chartFormat.setAttribute("legendFontColourR", ""+c.getRed());
			chartFormat.setAttribute("legendFontColourG", ""+c.getGreen());
			chartFormat.setAttribute("legendFontColourB", ""+c.getBlue());
			switch(getLegendPosition())
			{
				case 0: chartFormat.setAttribute("legendPosition", "left"); break;
				case 1: chartFormat.setAttribute("legendPosition", "bottom"); break;
				case 2: chartFormat.setAttribute("legendPosition", "right"); break;
				default:	chartFormat.setAttribute("legendPosition", "manual");
			}
			chartFormat.setAttribute("legendPositionX", ""+getLegendPositionX());
			chartFormat.setAttribute("legendPositionY", ""+getLegendPositionY());
			chartFormat.setAttribute("autoborder", ""+isAutoBorder());
			
			
			//Layout
			{
				Element layout = doc.createElement("layout");
				
				layout.setAttribute("topOffset", ""+getTopBorder().getOffset());
				layout.setAttribute("bottomOffset", ""+getBottomBorder().getOffset());
				layout.setAttribute("leftOffset", ""+getLeftBorder().getOffset());
				layout.setAttribute("rightOffset", ""+getRightBorder().getOffset());
				
				chartFormat.appendChild(layout);
			}
			
			//X-Axis
			{
				AxisOwner ao = getXAxis();
				
				Element axis = doc.createElement("axis");
				
				axis.setAttribute("heading", ao.getHeading().toString());
				ff = ao.getHeadingFont().getFontColorValue().f;
				axis.setAttribute("headingFontName", ff.getName());
				axis.setAttribute("headingFontSize", ""+ff.getSize());
				axis.setAttribute("headingFontStyle", ""+ff.getStyle());
				Color cc = ao.getHeadingFont().getFontColorValue().c;
				axis.setAttribute("headingFontColourR", ""+cc.getRed());
				axis.setAttribute("headingFontColourG", ""+cc.getGreen());
				axis.setAttribute("headingFontColourB", ""+cc.getBlue());
				ff = ao.getNumberFont().getFontColorValue().f;
				axis.setAttribute("numberFontName", ff.getName());
				axis.setAttribute("numberFontSize", ""+ff.getSize());
				axis.setAttribute("numberFontStyle", ""+ff.getStyle());
				cc = ao.getNumberFont().getFontColorValue().c;
				axis.setAttribute("numberFontColourR", ""+cc.getRed());
				axis.setAttribute("numberFontColourG", ""+cc.getGreen());
				axis.setAttribute("numberFontColourB", ""+cc.getBlue());
				axis.setAttribute("showMajor", ""+ao.getShowMajor().getBooleanValue());
				axis.setAttribute("showMinor", ""+ao.getShowMinor().getBooleanValue());
				cc = ao.getMajorColour().getColorValue();
				axis.setAttribute("majorColourR", ""+cc.getRed());
				axis.setAttribute("majorColourG", ""+cc.getGreen() );
				axis.setAttribute("majorColourB", ""+cc.getBlue() );
				cc = ao.getMinorColour().getColorValue();
				axis.setAttribute("minorColourR", ""+cc.getRed() );
				axis.setAttribute("minorColourG", ""+cc.getGreen() );
				axis.setAttribute("minorColourB", ""+cc.getBlue() );
				axis.setAttribute("logarithmic", ""+(ao.getLogarithmic().getCurrentIndex() == 1));
				axis.setAttribute("autoscale", ""+ao.autoScale.getBooleanValue());
				axis.setAttribute("minValue", ""+ao.getMinValue().getValue());
				axis.setAttribute("maxValue", ""+ao.getMaxValue().getValue());
				axis.setAttribute("majorGridInterval", ""+ao.getMajorGridInterval().getValue());
				axis.setAttribute("minorGridInterval", ""+ao.getMinorGridInterval().getValue());
				axis.setAttribute("logBase", ""+ao.getLogBase().getValue());
				axis.setAttribute("logStyle", ""+ao.logStyle.getCurrentIndex());
				axis.setAttribute("minimumPower", ""+ao.getMinimumPower().getValue());
				axis.setAttribute("maximumPower", ""+ao.getMaximumPower().getValue());
				
				chartFormat.appendChild(axis);
			}
			//Y-Axis
			{
				AxisOwner ao = getYAxis();
				
				Element axis = doc.createElement("axis");
				
				axis.setAttribute("heading", ao.getHeading().toString());
				ff = ao.getHeadingFont().getFontColorValue().f;
				axis.setAttribute("headingFontName", ff.getName());
				axis.setAttribute("headingFontSize", ""+ff.getSize());
				axis.setAttribute("headingFontStyle", ""+ff.getStyle());
				Color cc = ao.getHeadingFont().getFontColorValue().c;
				axis.setAttribute("headingFontColourR", ""+cc.getRed());
				axis.setAttribute("headingFontColourG", ""+cc.getGreen());
				axis.setAttribute("headingFontColourB", ""+cc.getBlue());
				ff = ao.getNumberFont().getFontColorValue().f;
				axis.setAttribute("numberFontName", ff.getName());
				axis.setAttribute("numberFontSize", ""+ff.getSize());
				axis.setAttribute("numberFontStyle", ""+ff.getStyle());
				cc = ao.getNumberFont().getFontColorValue().c;
				axis.setAttribute("numberFontColourR", ""+cc.getRed());
				axis.setAttribute("numberFontColourG", ""+cc.getGreen());
				axis.setAttribute("numberFontColourB", ""+cc.getBlue());
				axis.setAttribute("showMajor", ""+ao.getShowMajor().getBooleanValue());
				axis.setAttribute("showMinor", ""+ao.getShowMinor().getBooleanValue());
				cc = ao.getMajorColour().getColorValue();
				axis.setAttribute("majorColourR", ""+cc.getRed());
				axis.setAttribute("majorColourG", ""+cc.getGreen() );
				axis.setAttribute("majorColourB", ""+cc.getBlue() );
				cc = ao.getMinorColour().getColorValue();
				axis.setAttribute("minorColourR", ""+cc.getRed() );
				axis.setAttribute("minorColourG", ""+cc.getGreen() );
				axis.setAttribute("minorColourB", ""+cc.getBlue() );
				axis.setAttribute("logarithmic", ""+(ao.getLogarithmic().getCurrentIndex() == 1));
				axis.setAttribute("autoscale", ""+ao.autoScale.getBooleanValue());
				axis.setAttribute("minValue", ""+ao.getMinValue().getValue());
				axis.setAttribute("maxValue", ""+ao.getMaxValue().getValue());
				axis.setAttribute("majorGridInterval", ""+ao.getMajorGridInterval().getValue());
				axis.setAttribute("minorGridInterval", ""+ao.getMinorGridInterval().getValue());
				axis.setAttribute("logBase", ""+ao.getLogBase().getValue());
				axis.setAttribute("logStyle", ""+ao.logStyle.getCurrentIndex());
				axis.setAttribute("minimumPower", ""+ao.getMinimumPower().getValue());
				axis.setAttribute("maximumPower", ""+ao.getMaximumPower().getValue());
				
				chartFormat.appendChild(axis);
			}
			
			//Series (also known as Graphs)
			
			for(int i = 0; i < getNoGraphs(); i++)
			{
				GraphList gl = getGraphPoints(i);
				
				Element graph = doc.createElement("graph");
				
				graph.setAttribute("seriesHeading", gl.getGraphTitle());
				Color cc = gl.getGraphColour();
				graph.setAttribute("seriesColourR", ""+cc.getRed());
				graph.setAttribute("seriesColourG", ""+cc.getGreen());
				graph.setAttribute("seriesColourB", ""+cc.getBlue());
				graph.setAttribute("showPoints", ""+gl.isDrawPoint());
				switch(gl.getPointIndex())
				{
					case GraphList.CIRCLE: graph.setAttribute("seriesShape", "circle"); break;
					case GraphList.RECTANGLE_H: graph.setAttribute("seriesShape", "rectangle_h"); break;
					case GraphList.RECTANGLE_V: graph.setAttribute("seriesShape", "rectangle_v"); break;
					case GraphList.SQUARE: graph.setAttribute("seriesShape", "square"); break;
					case GraphList.TRIANGLE: graph.setAttribute("seriesShape", "triangle"); break;
					default: graph.setAttribute("seriesShape", "none"); break;
				}
				graph.setAttribute("showLines", ""+gl.isShowLines());
				graph.setAttribute("lineWidth", ""+gl.getLineWidth());
				switch(gl.getLineStyle())
				{
					case 0: graph.setAttribute("lineStyle", "normal"); break;
					case 1: graph.setAttribute("lineStyle", "dashed"); break;
					default: graph.setAttribute("lineStyle", "dotDashed");
				}
				
				//Points for the series
				for(int j = 0; j < gl.size(); j++)
				{
					GraphPoint gp = gl.getPoint(j);
					
					Element point = doc.createElement("point");
					
					point.setAttribute("x", ""+gp.getXCoord());
					point.setAttribute("y", ""+gp.getYCoord());
					
					graph.appendChild(point);
				}
				
				chartFormat.appendChild(graph);
			}
			
			
			doc.appendChild(chartFormat);
			//File writing
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty("doctype-system", "chartformat.dtd");
			t.setOutputProperty("indent", "yes");
			t.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(f)));
		}
		
		catch(DOMException e)
		{
			throw new Exception("Problem saving graph: DOM Exception: "+e);
		}
		catch(ParserConfigurationException e)
		{
			throw new Exception("Problem saving graph: Parser Exception: "+e);
		}
		catch(TransformerConfigurationException e)
		{
			throw new Exception("Problem saving graph: Error in creating XML: "+e);
		}
		catch(FileNotFoundException e)
		{
			throw new Exception("Problem saving graph: File Not Found: "+e);
		}
		catch(TransformerException e)
		{
			throw new Exception("Problem saving graph: Transformer Exception: "+e);
		}
	}
	
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
	{
		InputSource inputSource = null;
		
		// override the resolve method for the dtd
		if (systemId.endsWith("dtd"))
		{
			// get appropriate dtd from classpath
			InputStream inputStream = ClassLoader.getSystemResourceAsStream("dtds/chartformat.dtd");
			if (inputStream != null) inputSource = new InputSource(inputStream);
		}
		
		return inputSource;
	}
	
	//Settings interface
	
		/*public int getNumSettings()
		{
			return 12;
		}
		 
		public Setting getSetting(int index)
		{
			switch(index)
				{
						case 0: return graphTitle;
						case 1: return titleFont;
						case 2: return legendVisible;
						case 3: return legendPosition;
						case 4: return legendPositionX;
						case 5: return legendPositionY;
						case 6: return legendFont;
						case 7: return autoborder;
						case 8: return topBorder.getOffsetProperty();
						case 9: return bottomBorder.getOffsetProperty();
						case 10: return leftBorder.getOffsetProperty();
						case 11: return rightBorder.getOffsetProperty();
						default: return null;
				}
		}
		 
		public String getSettingOwnerClassName()
		{
			return "Graph";
		}
		 
		public int getSettingOwnerID()
		{
			return PropertyConstants.GRAPH;
		}
		 
		public String getSettingOwnerName()
		{
			return graphTitle.getProperty().toString();
		}*/
	
	public void notifySettingChanged(Setting setting)
	{
		System.out.println("notifySettingChanged in MultiGraphModle");
		if(autoborder == null) //things haven't been assignged
			return;
		//System.out.println("Calling main update, should sort out the autoborder stuff");
		if(autoborder != null && autoborder.getBooleanValue())
		{
			//if(lastAutoBorderValue == -1 || lastAutoBorderValue == 1)
			{
				//System.out.println("telling em to disable");
				bottomBorder.getOffsetProperty().setEnabled(false);
				topBorder.getOffsetProperty().setEnabled(false);
				leftBorder.getOffsetProperty().setEnabled(false);
				rightBorder.getOffsetProperty().setEnabled(false);
				lastAutoBorderValue = 0;
				try
				{
					Graphics2D g2 = (Graphics2D)canvas.getGraphics();
					if (g2 != null) setBordersUp(getLegendPosition(), canvas.getLegendWidth(g2), canvas.getLegendHeight(), canvas.getTitleHeight(g2));
				}
				catch(SettingException ee)
				{
					gop.errorDialog(ee.getMessage(), "Error");
				}
			}
		}
		else
		{
			//if(lastAutoBorderValue == -1 || lastAutoBorderValue == 0)
			{
				bottomBorder.getOffsetProperty().setEnabled(true);
				topBorder.getOffsetProperty().setEnabled(true);
				leftBorder.getOffsetProperty().setEnabled(true);
				rightBorder.getOffsetProperty().setEnabled(true);
				lastAutoBorderValue = 0;
			}
			
		}
		
		if(legendPosition == null) return;
		if(legendVisible.getBooleanValue())
		{
			legendPosition.setEnabled(true);
			switch(legendPosition.getCurrentIndex())
			{
				case LEFT:
				case RIGHT:
				case BOTTOM:
					
					
					legendPositionX.setEnabled(false);
					legendPositionY.setEnabled(false);
					lastLegendPosition = legendPosition.getCurrentIndex();
					
					break;
					
				case MANUAL:
					
					legendPositionX.setEnabled(true);
					legendPositionY.setEnabled(true);
					lastLegendPosition = legendPosition.getCurrentIndex();
					
			}
		}
		else
		{
			legendPositionX.setEnabled(false);
			legendPositionY.setEnabled(false);
			legendPosition.setEnabled(false);
		}
		
		if(setting == graphTitle)
		{
			//System.out.println("should be calling graphTitel stuf");
			gop.doListTitles();
		}
		setChanged();
		notifyObservers(null);
	}
	
	SettingDisplay disp;
	
	public SettingDisplay getDisplay()
	{
		return disp;
	}
	
	
	public void setDisplay(SettingDisplay display)
	{
		disp = display;
	}
	
}
