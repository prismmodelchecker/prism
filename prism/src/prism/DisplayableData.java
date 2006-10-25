//==============================================================================
//
//	Copyright (c) 2002-2004, Andrew Hinton, Dave Parker
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

package prism;
import chart.*;
import parser.*;
import java.util.*;
import settings.*;
/**
 *
 * @author  ug60axh
 */
public class DisplayableData
{
	private MultiGraphModel theModel;
	private int seriesIndex;
	
	private String rangeConstant;
	private Values otherValues;
	
	private ArrayList displayedValues;
	private ArrayList displayedGraphPoints;
	
	/** Creates a new instance of DisplayableData */
	public DisplayableData(MultiGraphModel theModel, String rangeConstant, Values otherValues, String seriesName)
	{
		this.theModel = theModel;
		this.rangeConstant = rangeConstant;
		this.otherValues = otherValues;
		this.displayedValues = new ArrayList();
		this.displayedGraphPoints = new ArrayList();
		
		try
		{
			seriesIndex = theModel.addGraph(seriesName);
		}
		catch(SettingException e)
		{
			//do nothing
		}
	}
	
	//constructor for already made graphs
	public DisplayableData(MultiGraphModel theModel, int seriesIndex)
	{
		this.theModel = theModel;
		this.seriesIndex = seriesIndex;
	}
	
	public void notifyResult(Values v, Object result)
	{
		Object obj = shouldAddThis(v);
		if(obj != null)
		{
			double x,y;
			// Get x coordinate
			if(obj instanceof Integer)
			{
				x = ((Integer)obj).intValue();
			}
			else if(obj instanceof Double)
			{
				x = ((Double)obj).doubleValue();
			}
			// Cancel if non integer/double
			else return;
			// Cancel if +/- infinity or NaN
			if (x == Double.POSITIVE_INFINITY || x == Double.NEGATIVE_INFINITY || x != x) return;
			// Get y coordinate
			if(result instanceof Integer)
			{
				y = ((Integer)result).intValue();
			}
			else if(result instanceof Double)
			{
				y = ((Double)result).doubleValue();
			}
			// Cancel if non integer/double
			else return;
			// Cancel if +/- infinity or NaN
			if (y == Double.POSITIVE_INFINITY || y == Double.NEGATIVE_INFINITY || y != y) return;
			// Add point to graph
			try
			{
				GraphPoint existing = getExistingGraphPoint(v);
				if(existing == null)
				{
					GraphPoint gp = new GraphPoint(x,y,theModel);
					displayedValues.add(v);
					displayedGraphPoints.add(gp);
					theModel.addPoint(seriesIndex, gp, false, true, true);
				}
				else
				{
					existing.setXCoord(x);
					existing.setYCoord(y);
					theModel.changed();
				}
			}
			catch(SettingException e)
			{
				//do nothing
			}
		}
	}
	
	public void clear()
	{
		try
		{
			theModel.removeAllPoints(seriesIndex);
		}
		catch(SettingException e)
		{
			//do nothing
		}
	}
	
	/**
	 *	Looks to see if the result has already been set for v -
	 *	if so, the corresponding GraphPoint object/  Returns null
	 *	if there isn't one.
	 */
	public GraphPoint getExistingGraphPoint(Values v)
	{
		for(int i = 0; i < displayedValues.size(); i++)
		{
			Values val = (Values)displayedValues.get(i);
			if(v.equals(val))
				return (GraphPoint)displayedGraphPoints.get(i);
		}
		return null;
	}
	
	/**	Looks at the values and sees whether it matches otherValues, apart
	 *	from one which should match 'rangeConstant'.  If so this method returns
	 *	the value of the rangeConstant.  If not this returns null. */
	public Object shouldAddThis(Values v)
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
