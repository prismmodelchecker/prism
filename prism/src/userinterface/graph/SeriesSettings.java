//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
//	* Alistair John Strachan <alistair@devzero.co.uk> (University of Edinburgh)
//	* Mike Arthur <mike@mikearthur.co.uk> (University of Edinburgh)
//	* Zak Cohen <zakcohen@gmail.com> (University of Edinburgh)
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

package userinterface.graph;

import java.util.*;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.util.*;
import org.w3c.dom.Element;

import settings.*;

/** 
 * A class representing a series of a Graph. This class should not be used to
 * modify data, use Graph for this. The intention is that this class
 * represents all properties (colour, lines, heading, etc.) other than data.
 */
public class SeriesSettings extends Observable implements SettingOwner
{	
	/** shape index constants */    
    public static final int CIRCLE 		= 0;
    public static final int SQUARE 		= 1;
    public static final int TRIANGLE 	= 2;
    public static final int RECTANGLE_H = 3;
    public static final int RECTANGLE_V = 4;
    public static final int NONE 		= 5;
	
    public static final int SOLID       = 0;
    public static final int DASHED 		= 1;
    public static final int DOT_DASHED 	= 2;
    
    public static final int BLUE 		= 0;
    public static final int GREEN 		= 1;
    public static final int RED	     	= 2;
    public static final int CYAN        = 3;
    public static final int PURPLE      = 4;
    public static final int YELLOW 		= 5;
    public static final int BROWN 		= 5;
    
    public static final Shape[] DEFAULT_SHAPE_SEQUENCE = {
    	DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE[1], // CIRCLE
    	DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE[0], // SQUARE
    	DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE[2], // TRIANGLE
    	DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE[4], // RECTANGLE_H
    	DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE[8], // RECTANGLE_V
    };
    
	// Use slightly smaller point shapes than the JFreeChart standards
	protected static int triPointsX[] = {-2,2,0};
	protected static int triPointsY[] = {-2,-2,2};
	
	/* List of all shapes, including empty shape */
	protected static final Shape[] SHAPES = {
    	new Ellipse2D.Double(-2,-2,4,4),
    	new Rectangle2D.Double(-2,-2,4,4),
    	new Polygon(triPointsX, triPointsY, 3),
    	new Rectangle2D.Double(-2,-1,4,2),
    	new Rectangle2D.Double(-1,-2,2,4),
    	new Rectangle2D.Double(0,0,0,0) // NONE    	
    };	
	
	protected static final Shape[] DEFAULT_SHAPES = {
		SHAPES[CIRCLE],
		SHAPES[SQUARE],
		SHAPES[TRIANGLE],
		SHAPES[RECTANGLE_H],
		SHAPES[RECTANGLE_V] 	
    };   
    
    protected static final Paint[] DEFAULT_PAINTS = {
    	    Color.blue,
    	    new Color(0, 127, 0),
    	    Color.red,
    	    new Color(0, 191, 191),
    	    new Color(191, 0, 191),
    	    new Color(191, 191, 0),
    	    new Color(0.6f, 0.2f, 0f)
    };
    
	/** Graph object. */
	private Graph graph;
	
	/** JFreeChart representation of graphs. */
	private JFreeChart chart;
	
	/** XYPlot of this JFreeChart */
	private XYPlot plot;
	
	/** Key of this series. */
	private Graph.SeriesKey key;
	
	/** The XYItemRenderer for this series. */
	private XYLineAndShapeRenderer renderer;
	
	/** Settings of a series. */
	private SingleLineStringSetting seriesHeading;
    private ColorSetting seriesColour;
    private BooleanSetting showPoints;
    private ChoiceSetting seriesShape;
    private BooleanSetting showLines;
    private DoubleSetting lineWidth;
    private ChoiceSetting lineStyle;
  //  private SeriesDataSetting dataProp;    
   
    /** Display for settings. */
    private SettingDisplay display;
    
    private GraphSeriesIcon icon;
	
	public SeriesSettings(Graph graph, Graph.SeriesKey key)
	{
		this.graph = graph;
		this.key = key;
		this.chart = graph.getChart();
		this.plot = chart.getXYPlot();
			
		/* This should really be checked first. */
		this.renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		
		seriesHeading = new SingleLineStringSetting("heading", "heading", "The heading for this series, as displayed in the legend.", this, true);
        seriesColour = new ColorSetting("colour", Color.black, "The colour for all lines and points in this series.", this, true);
        showPoints = new BooleanSetting("show points", new Boolean(true), "Should points be displayed for this series?", this, true);
        String[] choices = { "Circle", "Square", "Triangle", "Horizontal Rectangle", "Vertical Rectangle", "None" };
        seriesShape = new ChoiceSetting("point shape", choices, choices[0], "The shape of points for this series.", this, true);
        showLines = new BooleanSetting("show lines", new Boolean(true), "Should lines be displayed for this series?", this, true);
        lineWidth = new DoubleSetting("line width",  new Double(1.0), "The line width for this series.", this, true, new RangeConstraint("0.0,"));
        String [] styles = { "---------", "- - - - -", "- -- - --" };
        lineStyle = new ChoiceSetting("line style", styles, styles[0], "The line style for this series.", this, true);
        
        /** JFreeChart is smart enough to choose sensible values for colours, shapes etc. Lets try to use these if we can. */
        synchronized (graph.getSeriesLock())
		{			
			int seriesIndex = graph.getJFreeChartIndex(key);
			
			if (seriesIndex >= 0)
			{		
				/* Set series colour. */
				if (renderer.lookupSeriesPaint(seriesIndex) instanceof Color)
				{
					try
					{ 
						seriesColour.setValue((Color)renderer.getSeriesPaint(seriesIndex)); 
					}
					catch (SettingException e)
					{}					
				}
				
				/* Set series heading. */
				try
				{ 
					seriesHeading.setValue(graph.getXYSeries(key).getKey());
				}
				catch (SettingException e)
				{					
				}	
				
				/* Set showPoints. */
				try
				{ 
					// just do it.
					Boolean pointsVisibleFlag = true;
					showPoints.setValue(new Boolean(pointsVisibleFlag == null || pointsVisibleFlag.booleanValue())); 
				}
				catch (SettingException e)
				{					
				}
				
				/* Set seriesShape. */				
				Shape shape = renderer.lookupSeriesShape(seriesIndex);
				
				try
				{
					boolean foundShape = false;
					
					for (int i = CIRCLE; i < NONE; i++)
					{
						if (ShapeUtilities.equal(shape, SHAPES[i]))
						{
							seriesShape.setSelectedIndex(i);
							foundShape = true;
							break;
						}
					}
					
					if (!foundShape)
						seriesShape.setSelectedIndex(NONE);
				}
				catch (SettingException e)
				{		
					e.printStackTrace();
				}
				
				/* Set showLines. */
				try
				{ 
					Boolean linesVisibleFlag = true;
					showLines.setValue(new Boolean(linesVisibleFlag == null || linesVisibleFlag.booleanValue())); 
				}
				catch (SettingException e)
				{					
				}
			}			
		}
        
        updateSeries();
   	}
	
	public SettingDisplay getDisplay() 
	{
		return display;
	}

	public int getNumSettings() 
	{
		return 7;
	}

	public Setting getSetting(int index) 
	{
		switch(index)
		{
			case 0: return seriesHeading;
	        case 1: return seriesColour;
	        case 2: return showPoints;
	        case 3: return seriesShape;
	        case 4: return showLines;
	        case 5: return lineWidth;
	        case 6: return lineStyle;
	        default: return null;		
		}
	}

	public String getSettingOwnerClassName() {
		return "Series";		
	}

	public int getSettingOwnerID() {
		return prism.PropertyConstants.GRAPH_DISPLAY;
	}

	public String getSettingOwnerName() {
		return seriesHeading.getStringValue();
	}

	public void notifySettingChanged(Setting setting) {
		updateSeries();
		setChanged();		
		notifyObservers(this);	
	}

	public void setDisplay(SettingDisplay display) 
	{
		this.display = display;
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
	
	/**
	 * Getter for property seriesHeading.
	 * @return Value of property seriesHeading.
	 */
	public String getSeriesHeading()
	{
		return seriesHeading.getStringValue();
	}
	
	/**
	 * Setter for property seriesHeading.
	 * @param value Value of property seriesHeading.
	 */
	public void setSeriesHeading(String value)
	{
		try
		{
			seriesHeading.setValue(value);
			updateSeries();
			setChanged();
			notifyObservers(this);
		}
		catch (SettingException e)
		{
			// Shouldn't happen.
		}
	}
	
	/**
	 * Getter for property seriesColour.
	 * @return Value of property seriesColour.
	 */
	public Color getSeriesColour()
	{
		return seriesColour.getColorValue();
	}
	
	/**
	 * Setter for property seriesColour.
	 * @param value Value of property seriesColour.
	 */
	public void setSeriesColour(Color value)
	{
		try
		{
			seriesColour.setValue(value);
			updateSeries();
			setChanged();
			notifyObservers(this);
		}
		catch (SettingException e)
		{
			// Shouldn't happen.
		}
	}
	
	/**
	 * Getter for property showPoints.
	 * @return Value of property showPoints.
	 */
	public boolean showPoints()
	{
		return showPoints.getBooleanValue();
	}
	
	/**
	 * Setter for property showPoints.
	 * @param value Value of property showPoints.
	 */
	public void showPoints(boolean value)
	{
		try
		{
			showPoints.setValue(new Boolean(value));
			updateSeries();
			setChanged();
			notifyObservers(this);
		}
		catch (SettingException e)
		{
			// Shouldn't happen.
		}
	}
	
	/**
	 * Getter for property seriesShape.
	 * @return Value of property seriesShape.
	 */
	public int getSeriesShape()
	{
		return seriesShape.getCurrentIndex();
	}
	
	/**
	 * Setter for property seriesShape.
	 * @param value Value of property seriesShape.
	 */
	public void setSeriesShape(int value) throws SettingException
	{
		seriesShape.setSelectedIndex(new Integer(value));
		updateSeries();
		setChanged();
		notifyObservers(this);		
	}
	
	/**
	 * Getter for property showLines.
	 * @return Value of property showLines.
	 */
	public boolean showLines()
	{
		return showLines.getBooleanValue();
	}
	
	/**
	 * Setter for property showLines.
	 * @param value Value of property showLines.
	 */
	public void showLines(boolean value)
	{
		try
		{
			showLines.setValue(new Boolean(value));
			updateSeries();
			setChanged();
			notifyObservers(this);
		}
		catch (SettingException e)
		{
			// Shouldn't happen.
		}
	}
	
	/**
	 * Getter for property lineWidth.
	 * @return Value of property lineWidth.
	 */
	public double getLineWidth()
	{
		return lineWidth.getDoubleValue();
	}
	
	/**
	 * Setter for property lineWidth.
	 * @param value Value of property lineWidth.
	 */
	public void setLineWidth(double value) throws SettingException
	{
		lineWidth.setValue(new Double(value));
		updateSeries();
		setChanged();
		notifyObservers();					
	}
	
	/**
	 * Getter for property lineStyle.
	 * @return Value of property lineStyle.
	 */
	public int getLineStyle()
	{
		return lineStyle.getCurrentIndex();
	}
	
	/**
	 * Setter for property lineStyle.
	 * @param value Value of property lineStyle (SOLID, DASHED, DOT_DASHED).
	 */
	public void setLineStyle(int value) throws SettingException
	{
		lineStyle.setSelectedIndex(new Integer(value));
		updateSeries();
		setChanged();
		notifyObservers(this);		
	}
	
	public String toString()
	{
		return this.seriesHeading.getStringValue();
	}
	
	public void updateSeries()
	{
		// We don't want series to change while we are updating.
		synchronized (graph.getSeriesLock())
		{			
			int seriesIndex = graph.getJFreeChartIndex(key);
			
			if (seriesIndex >= 0)
			{
				/* Set series colour. */
				if (renderer.getSeriesPaint(seriesIndex) == null || 
				   !renderer.getSeriesPaint(seriesIndex).equals(seriesColour))
				{
					renderer.setSeriesPaint(seriesIndex, seriesColour.getColorValue());
				}				
				
				/* Set series heading. */
				if (!graph.getXYSeries(key).getKey().equals(seriesHeading.getStringValue()))
				{
					graph.changeSeriesName(key, seriesHeading.getStringValue());
					try
					{
						seriesHeading.setValue(graph.getXYSeries(key).getKey());
					}
					catch (SettingException e)
					{}
				}
				
				/* Set showPoints. */
				Boolean pointsVisibleFlag = renderer.getSeriesShapesVisible(seriesIndex);
				
				if (pointsVisibleFlag == null || pointsVisibleFlag.booleanValue() != showPoints.getBooleanValue())
				{
					renderer.setSeriesShapesVisible(seriesIndex, showPoints.getBooleanValue());
				}
								
				/* Set seriesShape. */				
				Shape shape = renderer.getSeriesShape(seriesIndex);
				int shapeIndex = seriesShape.getCurrentIndex();
								
				if (!ShapeUtilities.equal(shape, SHAPES[shapeIndex]))
					renderer.setSeriesShape(seriesIndex, SHAPES[shapeIndex]);
								
				/* Set showLines. */
				Boolean linesVisibleFlag = renderer.getSeriesLinesVisible(seriesIndex);
				if (linesVisibleFlag == null || linesVisibleFlag.booleanValue() != showLines.getBooleanValue())
				{
					renderer.setSeriesLinesVisible(seriesIndex, showLines.getBooleanValue());						
				}
				
				/* Set stroke - hard to check*/
				if (lineStyle.getCurrentIndex() == SOLID) // solid
				{
					BasicStroke newStroke = new BasicStroke((float)lineWidth.getDoubleValue(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f);
					renderer.setSeriesStroke(seriesIndex, newStroke);					
				} 
				else if (lineStyle.getCurrentIndex() == DASHED) // Just dash
				{
					float dash[] = {2.0f, 3.0f};
					BasicStroke newStroke = new BasicStroke((float)lineWidth.getDoubleValue(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dash, 1.0f);
					renderer.setSeriesStroke(seriesIndex, newStroke);				
				}
				else // Funny dash
				{
					float dash[] = {1.0f, 3.0f, 5.0f, 3.0f};
					BasicStroke newStroke = new BasicStroke((float)lineWidth.getDoubleValue(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f);
					renderer.setSeriesStroke(seriesIndex, newStroke);				
				}				
			}
			
			icon = new GraphSeriesIcon(renderer.getSeriesShape(seriesIndex), renderer.getSeriesStroke(seriesIndex), seriesColour.getColorValue(), showLines.getBooleanValue(), showPoints.getBooleanValue());
			icon.setOpaque(false);
			icon.setMinimumSize(new Dimension(20,10));
			icon.setMaximumSize(new Dimension(50,20));
			icon.setPreferredSize(new Dimension(30,10));
		}
	}
	
	public Graph.SeriesKey getSeriesKey()
	{
		return key;
	}
	
	/** Creates a renderer for the icon of this series in the options dialog. */
	public JComponent getIcon()
	{
		return this.icon;
	}
	
	private class GraphSeriesIcon extends JComponent
	{
		private Shape shape;
		private Stroke stroke;
		private Color color;
		
		private boolean showLines;
		private boolean showShapes;
		
		public GraphSeriesIcon(Shape shape, Stroke stroke, Color color, boolean showLines, boolean showShapes) 
		{
			this.shape = shape;
			this.stroke = stroke;
			this.color = color;
			
			this.showLines = showLines;
			this.showShapes = showShapes;
		}

		protected void paintComponent(Graphics g) 
		{
			super.paintComponent(g);
				
			float width = this.getWidth();
			float height = this.getHeight();
			
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			g2d.setColor(color);	
			g2d.setStroke(stroke);
			g2d.translate(width/2, height/2);			
			
			if (showLines)
				g2d.drawLine(-(int)width/2,0, (int)width/2, 0);			
			if (showShapes)
				g2d.fill(shape);
			
			g2d.dispose();
		}
	}
	
	public void save(Element series) throws SettingException
	{
		series.setAttribute("seriesHeading", getSeriesHeading());
		series.setAttribute("lineWidth", "" + getLineWidth());
		
		Color seriesColor = getSeriesColour();
		series.setAttribute("seriesColourR", "" + seriesColor.getRed());
		series.setAttribute("seriesColourG", "" + seriesColor.getGreen());
		series.setAttribute("seriesColourB", "" + seriesColor.getBlue());
		
		series.setAttribute("showPoints", showPoints() ? "true" : "false");
		series.setAttribute("showLines", showLines() ? "true" : "false");
		
		int lineStyle = getLineStyle();
		
		switch (lineStyle)
		{
			case (DASHED) : 
				series.setAttribute("lineStyle", "dashed");
			break;
			case (DOT_DASHED) : 
				series.setAttribute("lineStyle", "dotDashed");
			break;
			default: 
				series.setAttribute("lineStyle", "normal");
		}
		
		int seriesShape = getSeriesShape();
		
		switch (seriesShape)
		{
			case (CIRCLE) : 
				series.setAttribute("seriesShape", "circle");
			break;
			case (SQUARE) : 
				series.setAttribute("seriesShape", "square");
			break;
			case (TRIANGLE) : 
				series.setAttribute("seriesShape", "triangle");
			break;
			case (RECTANGLE_H) : 
				series.setAttribute("seriesShape", "rectangle_h");
			break;
			case (RECTANGLE_V) : 
				series.setAttribute("seriesShape", "rectangle_v");
			break;
			default:
				series.setAttribute("seriesShape", "none");
		}
	}
	
	public void load(Element series) throws SettingException
	{
		setSeriesHeading(series.getAttribute("seriesHeading"));
		
		double lineWidth = Graph.parseDouble(series.getAttribute("lineWidth"));
		
		if (!Double.isNaN(lineWidth))
		{
			setLineWidth(lineWidth);
		}
		
		String seriesColourR = series.getAttribute("seriesColourR");
		String seriesColourG = series.getAttribute("seriesColourG");
		String seriesColourB = series.getAttribute("seriesColourB");
		
		Color seriesColor = Graph.parseColor(seriesColourR, seriesColourG, seriesColourB);

		setSeriesColour(seriesColor);
		showPoints(Graph.parseBoolean(series.getAttribute("showPoints")));
		showLines(Graph.parseBoolean(series.getAttribute("showLines")));
		
		String lineStyle = series.getAttribute("lineStyle");
		
		if (lineStyle.equals("dashed"))
			this.setLineStyle(DASHED);
		else if (lineStyle.equals("dotDashed"))
			this.setLineStyle(DOT_DASHED);	
		else
			this.setLineStyle(SOLID);
		
		String seriesShape = series.getAttribute("seriesShape");
		
		if (seriesShape.equals("circle"))
			this.setSeriesShape(CIRCLE);
		else if (seriesShape.equals("square"))
			this.setSeriesShape(SQUARE);
		else if (seriesShape.equals("triangle"))
			this.setSeriesShape(TRIANGLE);
		else if (seriesShape.equals("rectangle_h"))
			this.setSeriesShape(RECTANGLE_H);
		else if (seriesShape.equals("rectangle_v"))
			this.setSeriesShape(RECTANGLE_V);
		else
			this.setSeriesShape(NONE);
	}
}
