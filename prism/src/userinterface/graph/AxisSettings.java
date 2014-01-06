//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

import settings.*;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import org.jfree.chart.*;

import org.jfree.chart.plot.*;
import org.jfree.chart.axis.*;
import org.jfree.data.*;
import org.jfree.data.xy.*;

import org.w3c.dom.*;

/**
 * Representation of an axis of a Graph.
 * The settings are propagated to the JFreeChart library.
 */
public class AxisSettings extends Observable implements SettingOwner
{
	private String name;
	private SettingDisplay display;
	
	/** Our graph object. */
	private Graph graph;
	
	/** JFreeChart representation of graphs. */
	private JFreeChart chart;
	
	/** XYPlot of this JFreeChart */
	private XYPlot plot;
	
	/** JFreeChart representation of this axis. */
	private ValueAxis axis;
	
	/** True when this reprents the domain axis. */
	private boolean isDomain;
	
	private SingleLineStringSetting heading;
	private FontColorSetting headingFont;
	private FontColorSetting numberFont;
	
	private BooleanSetting showGrid;
	private ColorSetting gridColour;
	private ChoiceSetting scaleType;
	
	private BooleanSetting autoScale;
	private DoubleSetting minValue;
	private DoubleSetting maxValue;
	private DoubleSetting gridInterval;
	private DoubleSetting minimumPower;
	private DoubleSetting maximumPower;
	
	private DoubleSetting logBase;
	private ChoiceSetting logStyle;
	
	/** Choice for scale. Displays a linear scale */
	public static final int NORMAL_SCALE = 0;
	/** Choice for scale. Displays a logarithmic scale */
	public static final int LOGARITHMIC_SCALE = 1;
	
	/** Choice for display of logarithmic scale. Displays actual values (0.1, 10, 1000, etc.). */
	public static final int VALUES = 0;
	/** Choice for display of logarithmic scale. Displays values in scientific form (10^-1, 10^1, 10^3, etc.). */
	public static final int BASE_AND_EXPONENT = 1;
	
	public NumberFormat valuesFormatter;
		
	boolean activated = false;
	
	public AxisSettings(String name, boolean isDomain, Graph graph)
	{
		this.name = name;
		this.isDomain = isDomain;
		this.graph = graph;
		this.chart = graph.getChart();
		this.plot = chart.getXYPlot();
		this.axis = (isDomain) ? this.plot.getDomainAxis() : this.plot.getRangeAxis();
		
		this.valuesFormatter = NumberFormat.getInstance(Locale.UK);
		
		if (this.valuesFormatter instanceof DecimalFormat)
			((DecimalFormat) this.valuesFormatter).applyPattern("###,###,###,###,###,###,###,###.########################");
				
		/* Initialise all the settings. */
		heading = new SingleLineStringSetting("heading", name, "The heading for this axis", this, true);
		headingFont = new FontColorSetting("heading font", new FontColorPair(new Font("SansSerif", Font.PLAIN, 12), Color.black), "The font for this axis' heading.", this, true);
		numberFont = new FontColorSetting("numbering font", new FontColorPair(new Font("SansSerif", Font.PLAIN, 12), Color.black), "The font used to number the axis.", this, true);
		
		showGrid = new BooleanSetting("show gridlines", new Boolean(true), "Should the gridlines be visible", this, true);
		gridColour = new ColorSetting("gridline colour", new Color(204,204,204), "The colour of the gridlines", this, true);
		
		String[] logarithmicChoices = {"Normal", "Logarithmic"};
		scaleType = new ChoiceSetting("scale type", logarithmicChoices, logarithmicChoices[0], "Should the scale be normal, or logarithmic", this, true);
		autoScale = new BooleanSetting("auto-scale", new Boolean(true), "When set to true, all minimum values, maximum values, grid intervals, maximum logarithmic powers and minimum logarithmic powers are automatically set and maintained when the data changes.", this, true);
		minValue = new DoubleSetting("minimum value", new Double(0.0), "The minimum value for the axis", this, true);
		maxValue = new DoubleSetting("maximum value", new Double(1.0), "The maximum value for the axis", this, true);
		gridInterval = new DoubleSetting("gridline interval", new Double(0.2), "The interval between gridlines", this, false, new RangeConstraint(0, Double.POSITIVE_INFINITY, false, true));
		logBase = new DoubleSetting("log base", new Double(10), "The base for the logarithmic scale", this, false, new RangeConstraint("1,"));
		
		minimumPower = new DoubleSetting("minimum power", new Double("0.0"), "The minimum logarithmic power that should be displayed on the scale", this, true);
		maximumPower = new DoubleSetting("maximum power", new Double("1.0"), "The maximum logarithmic power that should be displayed on the scale", this, true);
		
		String[] logStyleChoices = {"Values", "Base and exponent"};
		logStyle = new ChoiceSetting("logarithmic number style", logStyleChoices, logStyleChoices[1], "Should the style of the logarithmic scale show the actual values, or the base with the exponent.", this, false);
		
		/* Add constraints. */		
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
			
			public void checkValueLong(long i) throws SettingException
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
			
			public void checkValueLong(long i) throws SettingException
			{
				if(activated && i <= maxValue.getDoubleValue()) throw new SettingException("Minimum value should be > Maximum value");
			}
		});
		minimumPower.addConstraint(new NumericConstraint()
		{
			public void checkValueDouble(double d) throws SettingException
			{
				if(activated && d >= maximumPower.getDoubleValue()) throw new SettingException("Minimum power should be < Maximum power");
			}
			
			public void checkValueInteger(int i) throws SettingException
			{
				if(activated && i >= maximumPower.getDoubleValue()) throw new SettingException("Minimum power should be < Maximum power");
			}
			
			public void checkValueLong(long i) throws SettingException
			{
				if(activated && i >= maximumPower.getDoubleValue()) throw new SettingException("Minimum power should be < Maximum power");
			}
		});
		maximumPower.addConstraint(new NumericConstraint()
		{
			public void checkValueDouble(double d) throws SettingException
			{
				if(activated && d <= minimumPower.getDoubleValue()) throw new SettingException("Maximum power should be > Minimum power");
			}
			
			public void checkValueInteger(int i) throws SettingException
			{
				if(activated && i <= minimumPower.getDoubleValue()) throw new SettingException("Maximum power should be > Minimum power");
			}
			
			public void checkValueLong(long i) throws SettingException
			{
				if(activated && i <= minimumPower.getDoubleValue()) throw new SettingException("Maximum power should be > Minimum power");
			}
		});
		
		doEnables();
		display = null;		
		activated = true;
		
		updateAxis();
		setChanged();
		notifyObservers();
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
		return 14;
	}
	
	public String getSettingOwnerName()
	{
		return heading.getStringValue();
	}
	
	public Setting getSetting(int index)
	{
		switch(index)
		{
			case 0 : return heading;
			case 1 : return headingFont;
			case 2 : return numberFont;
			case 3 : return showGrid;
			case 4 : return gridColour;
			case 5 : return scaleType;
			case 6 : return autoScale;
			case 7 : return minValue;
			case 8 : return maxValue;
			case 9: return gridInterval;
			case 10: return minimumPower;
			case 11: return maximumPower;
			case 12: return logBase;
			case 13: return logStyle;
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
	
	public void doEnables()
	{
		minimumPower.setEnabled(!autoScale.getBooleanValue() && scaleType.getCurrentIndex() == LOGARITHMIC_SCALE);
		maximumPower.setEnabled(!autoScale.getBooleanValue() && scaleType.getCurrentIndex() == LOGARITHMIC_SCALE);
		logBase.setEnabled(scaleType.getCurrentIndex()== LOGARITHMIC_SCALE);
		logStyle.setEnabled(scaleType.getCurrentIndex()== LOGARITHMIC_SCALE);
		minValue.setEnabled(!autoScale.getBooleanValue() && scaleType.getCurrentIndex() == NORMAL_SCALE);
		maxValue.setEnabled(!autoScale.getBooleanValue() && scaleType.getCurrentIndex() == NORMAL_SCALE);
		gridInterval.setEnabled(!autoScale.getBooleanValue() && scaleType.getCurrentIndex() == NORMAL_SCALE);
	}	
	
	public void notifySettingChanged(Setting setting)
	{
		doEnables();
		updateAxis();
		setChanged();
		notifyObservers(this);		
	}
	
	/**
	 * Getter for property heading.
	 * @return Value of property heading.
	 */
	public String getHeading()
	{
		return heading.getStringValue();
	}
	
	/**
	 * Setter for property heading.
	 * @param value Value of property heading.
	 */
	public void setHeading(String value)
	{
		try
		{
			heading.setValue(value);
			doEnables();
			updateAxis();
			setChanged();
			notifyObservers(this);
		}
		catch (SettingException e)
		{
			// Shouldn't happen.
		}
	}	
	
	/**
	 * Getter for property headingFont.
	 * @return Value of property headingFont.
	 */
	public FontColorPair getHeadingFont()
	{
		return headingFont.getFontColorValue();
	}
	
	/**
	 * Setter for property headingfont.
	 * @param value Value of property headingfont.
	 */
	public void setHeadingFont(FontColorPair value)
	{
		try
		{
			headingFont.setValue(value);
			doEnables();
			updateAxis();
			setChanged();
			notifyObservers(this);
		}
		catch (SettingException e)
		{
			// Shouldn't happen.
		}
	}
		
	/**
	 * Getter for property numberFont.
	 * @return Value of property numberFont.
	 */
	public FontColorPair getNumberFont()
	{
		return numberFont.getFontColorValue();
	}
	
	/**
	 * Setter for property numberfont.
	 * @param value Value of property numberfont.
	 */
	public void setNumberFont(FontColorPair value)
	{
		try
		{
			numberFont.setValue(value);
			doEnables();
			updateAxis();
			setChanged();
			notifyObservers(this);
		}
		catch (SettingException e)
		{
			// Shouldn't happen.
		}
	}
	
	/**
	 * Getter for property autoScale.
	 * @return Value of property autoScale.
	 */
	public boolean isAutoScale()
	{
		return autoScale.getBooleanValue();
	}
	
	/**
	 * Setter for property autoScale.
	 * @param value Value of property autoScale.
	 */
	public void setAutoScale(boolean value)
	{
		try
		{
			autoScale.setValue(new Boolean(value));
			doEnables();
			updateAxis();
			setChanged();
			notifyObservers(this);
		}
		catch (SettingException e)
		{
			// Shouldn't happen.
		}
	}
	
	/**
	 * Getter for property showGrid.
	 * @return Value of property showGrid.
	 */
	public boolean showGrid()
	{
		return showGrid.getBooleanValue();
	}
	
	
	
	/**
	 * Setter for property showGrid.
	 * @param value Value of property showGrid.
	 */
	public void showGrid(boolean value)
	{
		try
		{
			showGrid.setValue(new Boolean(value));
			doEnables();
			updateAxis();
			setChanged();
			notifyObservers(this);
		}
		catch (SettingException e)
		{
			// Shouldn't happen.
		}
	}
	
	/**
	 * Getter for property gridColour.
	 * @return Value of property gridColour.
	 */
	public Color getGridColour() 
	{
		return gridColour.getColorValue();
	}
	
	/**
	 * Setter for property gridColour.
	 * @param value Value of property gridColour.
	 */
	public void setGridColour(Color value)
	{
		try
		{
			gridColour.setValue(value);
			doEnables();
			updateAxis();
			setChanged();
			notifyObservers(this);
		}
		catch (SettingException e)
		{
			// Shouldn't happen.
		}
	}
		
	/**
	 * Getter for property scaleType.
	 * @return Value of property scaleType (either NORMAL_SCALE, or LOGARITHMIC_SCALE).
	 */
	public int getScaleType()
	{
		return scaleType.getCurrentIndex();
	}
	
	/**
	 * Setter for property scaleType.
	 * @param value Value of property scaleType (either NORMAL_SCALE, or LOGARITHMIC_SCALE).
	 */
	public void setScaleType(int value) throws SettingException
	{		
		scaleType.setSelectedIndex(value);
		doEnables();
		updateAxis();
		setChanged();
		notifyObservers(this);
	}
	
	/**
	 * Checks whether this axis has a logarithmic scale.
	 * @return (getScaleType() == LOGARITHMIC_SCALE).
	 */
	public boolean isLogarithmic()
	{
		return (scaleType.getCurrentIndex() == LOGARITHMIC_SCALE);
	}	
	
	/**
	 * Getter for property logarithmic.
	 * @return Value of property logarithmic (either VALUES, or BASE_AND_EXPONENT).
	 */
	public int getLogStyle()
	{
		return logStyle.getCurrentIndex();
	}
	
	/**
	 * Setter for property logarithmic.
	 * @param value Value of property logarithmic (either VALUES, or BASE_AND_EXPONENT).
	 */
	public void setLogStyle(int value) throws SettingException
	{		
		logStyle.setSelectedIndex(value);
		doEnables();
		updateAxis();
		setChanged();
		notifyObservers(this);
	}
	
	/**
	 * Getter for property minValue.
	 * @return Value of property minValue.
	 */
	public double getMinValue()
	{
		return minValue.getDoubleValue();
	}
	
	/**
	 * Setter for property minValue.
	 * @param value Value of property minValue.
	 */
	public void setMinValue(double value) throws SettingException
	{		
		minValue.setValue(value);
		doEnables();
		updateAxis();
		setChanged();
		notifyObservers(this);
	}
		
	/**
	 * Getter for property maxValue.
	 * @return Value of property maxValue.
	 */
	public double getMaxValue()
	{
		return maxValue.getDoubleValue();
	}
	
	/**
	 * Setter for property maxValue.
	 * @param value Value of property maxValue.
	 */
	public void setMaxValue(double value) throws SettingException
	{		
		maxValue.setValue(value);
		doEnables();
		updateAxis();
		setChanged();
		notifyObservers(this);
	}
		
	/**
	 * Getter for property gridInterval.
	 * @return Value of property gridInterval.
	 */
	public double getGridInterval()
	{
		return gridInterval.getDoubleValue();
	}
	
	/**
	 * Setter for property gridInterval.
	 * @param value Value of property gridInterval.
	 */
	public void setGridInterval(Double value)
	{
		try
		{
			gridInterval.setValue(value);
			doEnables();
			updateAxis();
			setChanged();
			notifyObservers(this);
		}
		catch (SettingException e)
		{
			// Shouldn't happen.
		}
	}
	
	/**
	 * Getter for property logBase.
	 * @return Value of property logBase.
	 */
	public double getLogBase()
	{
		return logBase.getDoubleValue();
	}
	
	/**
	 * Setter for property logBase.
	 * @param value Value of property logBase.
	 */
	public void setLogBase(double value) throws SettingException
	{		
		logBase.setValue(value);
		doEnables();
		updateAxis();
		setChanged();
		notifyObservers(this);
	}
	
	/**
	 * Getter for property minimumPower.
	 * @return Value of property minimumPower.
	 */
	public double getMinimumPower()
	{
		return minimumPower.getDoubleValue();
	}
	
	/**
	 * Setter for property minimumPower.
	 * @param value Value of property minimumPower.
	 */
	public void setMinimumPower(double value) throws SettingException
	{		
		minimumPower.setValue(new Double(value));
		doEnables();
		updateAxis();
		setChanged();
		notifyObservers(this);
	}
	
	/**
	 * Getter for property maximumPower.
	 * @return Value of property maximumPower.
	 */
	public double getMaximumPower()
	{
		return maximumPower.getDoubleValue();
	}
	
	/**
	 * Setter for property maximumPower.
	 * @param value Value of property maximumPower.
	 */
	public void setMaximumPower(double value) throws SettingException
	{		
		maximumPower.setValue(new Double(value));
		doEnables();
		updateAxis();
		setChanged();
		notifyObservers(this);
	}

	public SettingDisplay getDisplay() 
	{		
		return display;
	}

	public void setDisplay(SettingDisplay display) 
	{
		this.display = display;
	}
	
	private void updateAxis()
	{
		/** -- First check whether we still have the right axis object */
		
		/* If we do not have a logarithmic scale, but the axis settings want one, then change the axis. */
		if (axis instanceof NumberAxis && isLogarithmic())
		{
			/** Update xAxis such that other settings can be checked for consistency. */
			PrismLogarithmicAxis newAxis = new PrismLogarithmicAxis(getHeading());
			
			/** We need to discard all negative and zero values should there be any. */
			/* TODO: Do this in a more elegant way. */
			synchronized (graph.getSeriesLock())
			{
				for (Graph.SeriesKey key : graph.getAllSeriesKeys())
				{					
					XYSeries series = graph.getXYSeries(key);
					
					if (series instanceof PrismXYSeries)
					{
						PrismXYSeries prismSeries = (PrismXYSeries)series;
						
						if (isDomain)
							prismSeries.setLogarithmicDomainAxis(true);
						else
							prismSeries.setLogarithmicRangeAxis(true);
					}						
				}
			}
			
			if (isDomain)
			{
				this.plot.setDomainAxis(newAxis);
				axis = this.plot.getDomainAxis();
			}
			else
			{
				this.plot.setRangeAxis(newAxis);
				axis = this.plot.getRangeAxis();
			}				
		}
		
		/* If we have a logarithmic scale, but the axis settings want a normal scale, then change the axis. */
		if (axis instanceof PrismLogarithmicAxis && !isLogarithmic())
		{
			/** Update xAxis such that other settings can be checked for consistency. */
			if (isDomain) 
			{
				this.plot.setDomainAxis(new NumberAxis(getHeading()));
				axis = this.plot.getDomainAxis();
			}
			else
			{
				this.plot.setRangeAxis(new NumberAxis(getHeading()));
				axis = this.plot.getRangeAxis();
			}
			
			/** It could be we discarded some negative and zero values, lets bring them back. */
			synchronized (graph.getSeriesLock())
			{
				for (Graph.SeriesKey key : graph.getAllSeriesKeys())
				{					
					XYSeries series = graph.getXYSeries(key);
					
					if (series instanceof PrismXYSeries)
					{
						PrismXYSeries prismSeries = (PrismXYSeries)series;
						
						if (isDomain)
							prismSeries.setLogarithmicDomainAxis(false);
						else
							prismSeries.setLogarithmicRangeAxis(false);
					}						
				}
			}
		}
		
		/** -- Check done, now look for smaller changes. */		
		
		/* If the heading of the axis does not match the heading set in the settings... */
		if (!(axis.getLabel().equals(getHeading())))
		{
			axis.setLabel(getHeading());
		}
		
		/* Update axis heading font if appropriate */
		if (!(axis.getLabelFont().equals(getHeadingFont().f)))
		{
			axis.setLabelFont(getHeadingFont().f);
		}	
		
		/* Update axis heading colour if appropriate */
		if (!(axis.getLabelPaint().equals(getHeadingFont().c)))
		{
			axis.setLabelPaint(getHeadingFont().c);
		}
		
		/* Update axis numbering font if appropriate */
		if (!(axis.getTickLabelFont().equals(getNumberFont().f)))
		{
			axis.setTickLabelFont(getNumberFont().f);
		}	
		
		/* Update axis numbering colour if appropriate */
		if (!(axis.getTickLabelPaint().equals(getNumberFont().c)))
		{
			axis.setTickLabelPaint(getNumberFont().c);
		}
		
		/* Update gridlines if appropriate. */
		if (isDomain && (plot.isDomainGridlinesVisible() != showGrid.getBooleanValue()))
		{
			plot.setDomainGridlinesVisible(showGrid.getBooleanValue());
		}		
		
		if (!isDomain && (plot.isRangeGridlinesVisible() != showGrid.getBooleanValue()))
		{
			plot.setRangeGridlinesVisible(showGrid.getBooleanValue());
		}
		
		/* Update gridline colour if appropriate. */
		if (isDomain && (!plot.getDomainGridlinePaint().equals(gridColour.getColorValue())))
		{
			plot.setDomainGridlinePaint(gridColour.getColorValue());
		}
		
		if (!isDomain && (!plot.getRangeGridlinePaint().equals(gridColour.getColorValue())))
		{
			plot.setRangeGridlinePaint(gridColour.getColorValue());
		}		
		
		/** Check properties specific to logarithmic axis. */
		if (axis instanceof PrismLogarithmicAxis)
		{
			PrismLogarithmicAxis logAxis = (PrismLogarithmicAxis)axis;
			
			if ((logStyle.getCurrentIndex() == BASE_AND_EXPONENT) != logAxis.isBaseAndExponentFormatOverride())
			{				
				logAxis.setBaseAndExponentFormatOverride(logStyle.getCurrentIndex() == BASE_AND_EXPONENT);
			}
			
			if ((logStyle.getCurrentIndex() == VALUES) && logAxis.getNumberFormatOverride() != this.valuesFormatter)
			{				
				logAxis.setNumberFormatOverride(this.valuesFormatter);
			}
			
			/* Switched from auto to manual? */			
			if (logAxis.isAutoRange() && !autoScale.getBooleanValue())
			{
				Range range = logAxis.getRange();
				logAxis.setAutoRange(false);
				
				try
				{
					this.minimumPower.setValue(logAxis.calculateLog(range.getLowerBound()));
					this.maximumPower.setValue(logAxis.calculateLog(range.getUpperBound()));
				}
				catch (SettingException e)
				{
					// best effort.
				}		
			}
			
			/* Switched from manual to auto? */
			if (!axis.isAutoRange() && autoScale.getBooleanValue())
			{			
				axis.setAutoRange(true);							
			}	
			
			/* If the log base is wrong. */
			if (logBase.getDoubleValue() != logAxis.getBase())
			{
				Range range = axis.getRange();
							
				logAxis.setBase(logBase.getDoubleValue());
				
				try
				{
					this.minimumPower.setValue(logAxis.calculateLog(range.getLowerBound()));				
					this.maximumPower.setValue(logAxis.calculateLog(range.getUpperBound()));
				}
				catch (SettingException e)
				{
					// best effort
				}
				
				if (Math.round(logBase.getDoubleValue()) == logBase.getDoubleValue())
					logAxis.setMinorTickCount((int)logBase.getDoubleValue());
				else
					logAxis.setMinorTickCount(1);
			}
			
			/* If manual, logarithmic, and range does not match our settings, then update */
			if (!axis.isAutoRange())
			{	
				Range range = logAxis.getRange();
								
				if (range.getLowerBound() != logAxis.calculateValue(minimumPower.getDoubleValue()) || range.getUpperBound() != logAxis.calculateValue(maximumPower.getDoubleValue()))
				{
					axis.setRange(logAxis.calculateValue(minimumPower.getDoubleValue()), logAxis.calculateValue(maximumPower.getDoubleValue()));
				}
			}
		}		
		
		/** Check properties specific to numeric axis. */
		if (axis instanceof NumberAxis)
		{
			NumberAxis numAxis = (NumberAxis)axis;
			
			/* Switched from auto to manual? */
			if (axis.isAutoRange() && !autoScale.getBooleanValue())
			{
				Range range = axis.getRange();
				axis.setAutoRange(false);
				axis.setAutoTickUnitSelection(false);										
			
				try
				{
					this.minValue.setValue(range.getLowerBound());
					this.maxValue.setValue(range.getUpperBound());
					this.gridInterval.setValue(numAxis.getTickUnit().getSize());
				}
				catch (SettingException e)
				{
					// best effort.
				}				
			}
			
			/* Switched from manual to auto? */
			if (!axis.isAutoRange() && autoScale.getBooleanValue())
			{			
				axis.setAutoRange(true);
				axis.setAutoTickUnitSelection(true);				
			}
			
			/* If manual, numeric, and range does not match our settings, then update */
			if (!axis.isAutoRange())
			{
				Range range = axis.getRange();
				
				if (range.getLowerBound() != minValue.getDoubleValue() || range.getUpperBound() != maxValue.getDoubleValue())
				{
					axis.setRange(minValue.getDoubleValue(), maxValue.getDoubleValue());
				}
				
				if (gridInterval.getDoubleValue() != numAxis.getTickUnit().getSize())
				{
					// FIXME: With i.e. interval 0.01 it rounds "0.10" to "0.1"
					numAxis.setTickUnit(new NumberTickUnit(gridInterval.getDoubleValue()));
					// Some experimental code to make axis display only odd numbers:
					/*if (axisShouldOnlyShowOdd) numAxis.setNumberFormatOverride(new DecimalFormat()
					{ public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
							return ((int)number % 2 == 0) ? new StringBuffer("") : super.format(number, toAppendTo, pos);
					} });*/
				}
			}
		}
	}
	
	public void save(Element axis) throws SettingException
	{
		axis.setAttribute("heading", getHeading());
		
		Font headingFont = getHeadingFont().f;
		axis.setAttribute("headingFontName", headingFont.getName());
		axis.setAttribute("headingFontSize", "" + headingFont.getSize());
		axis.setAttribute("headingFontStyle", "" + headingFont.getStyle());
		
		Color headingFontColor = getHeadingFont().c;
		axis.setAttribute("headingFontColourR", "" + headingFontColor.getRed());
		axis.setAttribute("headingFontColourG", "" + headingFontColor.getGreen());
		axis.setAttribute("headingFontColourB", "" + headingFontColor.getBlue());
		
		Font numberFont = getNumberFont().f;
		axis.setAttribute("numberFontName", numberFont.getName());
		axis.setAttribute("numberFontSize", "" + numberFont.getSize());
		axis.setAttribute("numberFontStyle", "" + numberFont.getStyle());
		
		Color numberFontColor = getHeadingFont().c;
		axis.setAttribute("numberFontColourR", "" + numberFontColor.getRed());
		axis.setAttribute("numberFontColourG", "" + numberFontColor.getGreen());
		axis.setAttribute("numberFontColourB", "" + numberFontColor.getBlue());
		
		axis.setAttribute("showMajor", showGrid() ? "true" : "false");
		
		Color gridColor = gridColour.getColorValue();		
		axis.setAttribute("majorColourR", "" + gridColor.getRed());
		axis.setAttribute("majorColourG", "" + gridColor.getGreen());
		axis.setAttribute("majorColourB", "" + gridColor.getBlue());
		
		axis.setAttribute("logarithmic", isLogarithmic() ? "true" : "false");
		
		axis.setAttribute("minValue", "" + getMinValue());
		axis.setAttribute("maxValue", "" + getMaxValue());
		
		axis.setAttribute("majorGridInterval", "" + getGridInterval());
		axis.setAttribute("logBase", "" + getLogBase());
		axis.setAttribute("logStyle", "" + getLogStyle());
		
		axis.setAttribute("minimumPower", "" + getMinimumPower());
		axis.setAttribute("maximumPower", "" + getMaximumPower());
		
		axis.setAttribute("autoscale", isAutoScale() ? "true" : "false");
	}
	
	public void load(Element axis) throws SettingException
	{
		setHeading(axis.getAttribute("heading")); 
  
		String headingFontName = axis.getAttribute("headingFontName");
		String headingFontSize = axis.getAttribute("headingFontSize");
		String headingFontStyle = axis.getAttribute("headingFontStyle");
	  
		Font headingFont = Graph.parseFont( headingFontName, headingFontStyle, headingFontSize);
		
		String headingFontColourR = axis.getAttribute("headingFontColourR");
		String headingFontColourG = axis.getAttribute("headingFontColourG");
		String headingFontColourB = axis.getAttribute("headingFontColourB");
		
		Color headingFontColor = Graph.parseColor(headingFontColourR, headingFontColourG, headingFontColourB);
		
		setHeadingFont(new FontColorPair(headingFont, headingFontColor));
				
		String numberFontName = axis.getAttribute("numberFontName"); 
		String numberFontSize = axis.getAttribute("numberFontSize"); 
		String numberFontStyle = axis.getAttribute("numberFontStyle"); 
		
		Font numberFont = Graph.parseFont(numberFontName, numberFontStyle, numberFontSize ); 
		
		String numberFontColourR = axis.getAttribute("numberFontColourR");
		String numberFontColourG = axis.getAttribute("numberFontColourG");
		String numberFontColourB = axis.getAttribute("numberFontColourB");
	  
		Color numberFontColor = Graph.parseColor(numberFontColourR, numberFontColourG, numberFontColourB);
	  
		setNumberFont(new FontColorPair(numberFont, numberFontColor));
		
	   // axis autoScale property 
		boolean autoScale = Graph.parseBoolean(axis.getAttribute("autoscale"));
		setAutoScale(autoScale);
		
		// axis minValue property 
		double minValue = Graph.parseDouble(axis.getAttribute("minValue"));
		double maxValue = Graph.parseDouble(axis.getAttribute("maxValue"));
		double majorGridInterval = Graph.parseDouble(axis.getAttribute("majorGridInterval"));
		
		if (!Double.isNaN(minValue) && !Double.isNaN(maxValue))
		{
			if (minValue > getMaxValue())
			{			
				setMaxValue(maxValue);
				setMinValue(minValue);
			}
			else
			{
				setMinValue(minValue);
				setMaxValue(maxValue);
			}
		}
				
		if (!Double.isNaN(majorGridInterval))
		{
			setGridInterval(majorGridInterval);
		}
		
		String majorGridColourR = axis.getAttribute("majorColourR");
		String majorGridColourG = axis.getAttribute("majorColourG");
		String majorGridColourB = axis.getAttribute("majorColourB");
	  
		Color majorGridColour = Graph.parseColor(majorGridColourR, majorGridColourG, majorGridColourB);
		
		showGrid(Graph.parseBoolean(axis.getAttribute("showMajor")));
	 	setScaleType(Graph.parseBoolean(axis.getAttribute("logarithmic")) ? LOGARITHMIC_SCALE : NORMAL_SCALE);
		
	 	setLogBase(Graph.parseDouble(axis.getAttribute("logBase")));
	 	setLogStyle(Graph.parseInt(axis.getAttribute("logStyle")));
	 	
	 	double minimumPower = Graph.parseDouble(axis.getAttribute("minimumPower"));
		double maximumPower = Graph.parseDouble(axis.getAttribute("maximumPower"));
		
		if (!Double.isNaN(minimumPower) && !Double.isNaN(maximumPower))
		{
			if (minimumPower > getMaximumPower())
			{			
				setMaximumPower(maximumPower);
				setMinimumPower(minimumPower);
			}
			else
			{
				setMinimumPower(minimumPower);
				setMaximumPower(maximumPower);
			}
		}
		
		setAutoScale(autoScale);
	}
}

