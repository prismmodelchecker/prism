//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

import java.util.Observable;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import settings.BooleanSetting;
import settings.ColorSetting;
import settings.Setting;
import settings.SettingDisplay;
import settings.SettingException;
import settings.SettingOwner;

import java.awt.*;

import javax.swing.UIManager;

/**
 * Representation of the display settings of a Graph.
 * The settings are propagated to the JFreeChart library.
 */
public class DisplaySettings extends Observable implements SettingOwner
{
	/* Display for settings. */
	private SettingDisplay display;
	
	/** Our graph object. */
	private Graph graph;
	
	/** JFreeChart representation of graphs. */
	private JFreeChart chart;
	
	/** XYPlot of this JFreeChart */
	private XYPlot plot;
	
	private BooleanSetting antiAlias;	
	private ColorSetting backgroundColor;	
	
	public DisplaySettings(Graph graph)
	{
		this.graph = graph;
		this.chart = graph.getChart();
		this.plot = chart.getXYPlot();
		
		antiAlias = new BooleanSetting("anti-aliasing", new Boolean(true), "Should the graph be rendered using anti-aliasing?", this, false);
		Color defaultColor = Color.white; 
		
		//Color defaultColor =  UIManager.getColor("Panel.background");
		
		//if (chart.getBackgroundPaint() instanceof Color)
		//	defaultColor = ((Color)chart.getBackgroundPaint());
		
		backgroundColor = new ColorSetting("background colour", defaultColor, "The background colour of the graph panel", this, false);
		
		updateDisplay();
		setChanged();
		notifyObservers();
	}	
	
	public SettingDisplay getDisplay() 
	{
		return display;
	}

	public int getNumSettings() 
	{
		return 2;
	}

	public Setting getSetting(int index) 
	{
		switch(index)
		{
			case 0: return antiAlias;
			case 1: return backgroundColor;
			default: return null;			
		}
	}

	public String getSettingOwnerClassName() {
		return "Display";		
	}

	public int getSettingOwnerID() {
		return prism.PropertyConstants.GRAPH_DISPLAY;
	}

	public String getSettingOwnerName() 
	{
		if (graph != null && graph.getName() != null)
			return graph.getName();
			
		return "";
	}

	public void notifySettingChanged(Setting setting) {
		updateDisplay();
		setChanged();		
		notifyObservers(this);	
	}

	public void setDisplay(SettingDisplay display) {
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
	 * Getter for property antiAlias.
	 * @return Value of property antiAlias.
	 */
	public boolean isAntiAliased()
	{
		return antiAlias.getBooleanValue();
	}
	
	/**
	 * Setter for property antiAlias.
	 * @param value Value of property antiAlias.
	 */
	public void setAntiAliased(boolean value)
	{
		try
		{
			antiAlias.setValue(new Boolean(value));
			updateDisplay();
			setChanged();
			notifyObservers(this);
		}
		catch (SettingException e)
		{
			// Shouldn't happen.
		}
	}
	
	/**
	 * Getter for property backgroundColor.
	 * @return Value of property backgroundColor.
	 */
	public Color getBackgroundColor()
	{
		return backgroundColor.getColorValue();
	}
	
	/**
	 * Setter for property backgroundColor.
	 * @param background Value of property backgroundColor.
	 */
	public void setBackgroundColor(Color background)
	{
		try
		{
			backgroundColor.setValue(background);
			updateDisplay();
			setChanged();
			notifyObservers(this);
		}
		catch (SettingException e)
		{
			// Shouldn't happen.
		}
	}
	
	private void updateDisplay()
	{
		/* Draw anti-aliased?. */
		if (isAntiAliased() != this.chart.getAntiAlias())
		{		
			this.chart.setAntiAlias(isAntiAliased());
		}
		
		/* Background changed? */
		if (!(this.chart.getBackgroundPaint() instanceof Color) || !backgroundColor.getColorValue().equals(this.chart.getBackgroundPaint()))
		{
			this.chart.setBackgroundPaint(backgroundColor.getColorValue());
		}
	}
}
