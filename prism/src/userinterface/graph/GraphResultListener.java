//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

import param.BigRational;
import parser.*;
import prism.*;

import org.jfree.data.xy.*;

// TODO: When either the graph or the resultset seizes to exist, then so should this listener.

/**
 * This class is responsible for mapping output of a ResultsCollection to the input of a GraphModel. Used to be called Displayable Data.
 */
public class GraphResultListener implements ResultListener
{
	// A graph.
	private Graph graph;
	// The name of the series.
	private Graph.SeriesKey seriesKey;		
	// The constant on the x-axis.
	private String rangeConstant;
	// The other constants.
	private Values otherValues;	
		
	/** 
	 * Creates a new instance of GraphResultListener 
	 * It presumes that the seriesKey is returned by a call of {@link userinterface.graph.Graph#addSeries graph.addSeries(...)}. 
	 * @param graph The graph to notify when new results are found.
	 * @param seriesKey The key of the series this listener represents.
	 * @param rangeConstant The ranging constant (x-axis value) (required to identify the series from the results). 
	 * @param otherValues Values of all other constants of this series (required to identify the series from the results). 
	 */
	public GraphResultListener(Graph graph, Graph.SeriesKey seriesKey, String rangeConstant, Values otherValues)
	{
		this.graph = graph;
		this.seriesKey = seriesKey;
		this.rangeConstant = rangeConstant;
		this.otherValues = otherValues;		
	}	
	
	
	public void notifyResult(ResultsCollection resultsCollection, Values values, Object result)
	{
		Object xObj = isInSeries(values);
		
		/* This is a result of our series, xObj is our x-coordinate. */
		if(xObj != null)
		{
			double x,y;
			
			// Get x coordinate
			if(xObj instanceof Integer) {	
				x = ((Integer)xObj).intValue(); // Use integer value.  	
			} else if(xObj instanceof Double) {
				x = ((Double)xObj).doubleValue(); // Use double value.
			} else if(xObj instanceof BigRational) {
				x = ((BigRational)xObj).doubleValue(); // Use double value.
			} else return; // Cancel if non integer/double			
			
			// Cancel if x = +/- infinity or NaN
			if (x == Double.POSITIVE_INFINITY || x == Double.NEGATIVE_INFINITY || Double.isNaN(x)) 
				return;
						
			// Add point to graph (if of valid type) 
			if (result instanceof Double) {
				y = ((Double) result).doubleValue();
				graph.addPointToSeries(seriesKey, new XYDataItem(x, y));
			} else if (result instanceof Integer) {
				y = ((Integer) result).intValue();
				graph.addPointToSeries(seriesKey, new XYDataItem(x, y));
			} else if (result instanceof BigRational) {
				y = ((BigRational) result).doubleValue();
				graph.addPointToSeries(seriesKey, new XYDataItem(x, y));
			} else if (result instanceof Interval) {
				Interval interval = (Interval) result;
				if (interval.lower instanceof Double) {
					y = ((Double) interval.lower).doubleValue();
					graph.addPointToSeries(seriesKey, new XYDataItem(x, y));
					y = ((Double) interval.upper).doubleValue();
					graph.addPointToSeries(seriesKey.next, new XYDataItem(x, y));
				} else if (result instanceof Integer) {
					y = ((Integer) interval.lower).intValue();
					graph.addPointToSeries(seriesKey, new XYDataItem(x, y));
					y = ((Integer) interval.upper).intValue();
					graph.addPointToSeries(seriesKey.next, new XYDataItem(x, y));
				}
			}
		}
	}
	
	/**	
	 *  Looks at the values and sees whether it matches otherValues, apart
	 *	from one which should match 'rangeConstant'. If so this method returns
	 *	the value of the rangeConstant (x-axis). If not this returns null. 
	 **/
	private Object isInSeries(Values v)
	{
		for(int i = 0; i < otherValues.getNumValues(); i++)
		{
			String name = otherValues.getName(i);
			Object value = otherValues.getValue(i);
			if(!name.equals(rangeConstant))
			{
				try
				{
					Object compare = v.getValueOf(name);
					if(compare.equals(value))
						continue;
					else throw new PrismException("value not same");
				}
				catch(PrismException e)
				{
					return null;
				}
			}
		}
		try
		{
			Object value = v.getValueOf(rangeConstant);
			return value;
		}
		catch(PrismException e)
		{
			return null;
		}
	}
	
}
