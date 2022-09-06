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

import java.util.*;

import org.jfree.data.xy.*;
import org.jfree.data.general.*;

public class PrismXYSeries extends XYSeries
{
	/** Do not allow negative and zero values on the x axis. */
	private boolean logXAxis;
	
	/** Do not allow negative and zero values on the y axis. */
	private boolean logYAxis;
	
	/** When in logarithmic mode for either axis, we might have to discard values temporarily. */
	private XYSeries discardedItems;
	
	/**
	 * A XYSeries that is always sorted, and in which it is allowed to add the same x value twice.
	 * Additionally there are constructs in this class to prevent it from returning negative or zero values
	 * when 
	 * 
	 * @param key
	 */
	public PrismXYSeries(Comparable key) 
	{
		super(key, true, false);
		
		this.logXAxis = false;
		this.logYAxis = false;
		
		discardedItems = new XYSeries("dummy", true, false);
	}
	
	/**
	 * Checks validity of XYDataItem. We do NOT allow positive or negative
	 * infinity on x-axis, on the y-axis we allow them but replace them by NaN.
	 * Validity means this is a value we should store, otherwise we can discard it.
	 * @param item The item to check, may be changed.
	 * @return True if valid, false otherwise.
	 */	
	private boolean checkValidity(XYDataItem item)
	{
		if (Double.isInfinite(item.getX().doubleValue()) ||
			Double.isNaN(item.getX().doubleValue()))
			return false;
			
		if (Double.isInfinite(item.getY().doubleValue()))
			item.setY(Double.NaN);
		
		return true;		
	}
	
	public void setLogarithmicDomainAxis(boolean logXAxis) 
	{
		if (this.logXAxis != logXAxis)
		{
			this.logXAxis = logXAxis;
		}
		
		checkData();
	}

	public void setLogarithmicRangeAxis(boolean logYAxis) 
	{
		if (this.logYAxis != logYAxis)
		{
			this.logYAxis = logYAxis;
		}
		
		checkData();
	}
	
	@Override
	public void add(double x, double y) {
		add(new Double(x), new Double(y), true);
	}

	@Override
	public void add(double x, double y, boolean notify) {
		add(new Double(x), new Double(y), notify);
	}
	
	@Override
	public void add(double x, Number y) {
		add(new Double(x), y);
	}

	@Override
	public void add(double x, Number y, boolean notify) {
		add(new Double(x), y, notify);
	}

	@Override
	public void add(Number x, Number y) {
		add(x, y, true);
	}
	
	@Override
	public void add(Number x, Number y, boolean notify) {
		XYDataItem item = new XYDataItem(x, y);
        add(item, notify);
	}

	@Override
	public void add(XYDataItem item) {
		add(item, true);
	}
	
	/**
	 * This method is the one that implements some different
	 * aspects to the XYSeries class.
	 */
	public void add(XYDataItem item, boolean notify) 
	{
		if (checkValidity(item))
		{	
			if (checkDataItem(item))
			{	
				/* Check in discarded dataset. */
				if (discardedItems.indexOf(item.getX()) >= 0)
				{					
					throw new SeriesException("X-value already exists.");
				}
				super.add(item, notify);			
			}
			else
			{
				/* Check in main dataset. */
				if (super.indexOf(item.getX()) >= 0)
					throw new SeriesException("X-value already exists.");
				
				discardedItems.add(item);
			}
		}
	}

	/**
	 *  @return A copy of the overwritten data item, or <code>null</code> if no 
     *  	    item was overwritten.
	 */
	@Override
	public XYDataItem addOrUpdate(Number x, Number y) {
		
		XYDataItem item = new XYDataItem(x,y);
		XYDataItem result = null;
		
		/** If this is a valid update. */
		if (checkValidity(item))
		{			
			int indexD = discardedItems.indexOf(item.getX());
			int indexS = super.indexOf(item.getX());
			
			/* If in discarded items, then remove and return this. */ 
			if (indexD >= 0)
			{
				result = discardedItems.remove(indexD);
			}
			
			/* If in main items, then remove and return this. (Should not be both in discarded and main items) */ 
			if (indexS >= 0)
			{
				result = super.remove(indexS);
			}
			
			this.add(item, true);
			
			return null;
		}
		/* This is not a valid data item! */
		else
		{
			return null;
		}		
	}

	@Override
	public void update(int index, Number y) 
	{
		this.updateByIndex(index, y);
	}

	@Override
	public void update(Number x, Number y) 
	{
		XYDataItem item = new XYDataItem(x,y);
		XYDataItem result = null;
		
		/** If this is a valid update. */
		if (checkValidity(item))
		{			
			int indexD = discardedItems.indexOf(item.getX());
			int indexS = super.indexOf(item.getX());
			
			/* If in discarded items, then remove and return this. */ 
			if (indexD >= 0)
			{
				result = discardedItems.remove(indexD);
			}
			
			/* If in main items, then remove and return this. (Should not be both in discarded and main items) */ 
			if (indexS >= 0)
			{
				result = super.remove(indexS);
			}
			
			if (result != null)
				this.add(item, true);
			else
				throw new SeriesException("No observation for x = " + x);
		}			
	}

	@Override
	public void updateByIndex(int index, Number y) {
		XYDataItem existing = getDataItem(index);
		XYDataItem item = new XYDataItem(existing.getX(), y);
		
		if (checkValidity(item))
		{
			if (checkDataItem(item))
				super.update(index, y);
			else
			{
				super.remove(index);
				discardedItems.add(item);
			}				
		}
	}

	private void checkData()
	{
		boolean changed = false;
		
		/** Check whether we can reintroduce some data we discarded earlier. */
		
		int d = 0;
		
		while (d < discardedItems.getItemCount())
		{
			XYDataItem dataItem = discardedItems.getDataItem(d);
			
			if (checkDataItem(dataItem))
			{
				/* Should be reintroduced. */
				this.discardedItems.remove(d);
				super.add(dataItem);
				changed = true;
			}
			else
			{
				d++;
			}
		}
		
		d = 0;
		
		/** Check whether we have to discard some data we discarded earlier. */
		while (d < super.getItemCount())
		{
			XYDataItem dataItem = super.getDataItem(d);
			
			if (!checkDataItem(dataItem))
			{
				/* Should be discarded. */
				super.remove(d);
				this.discardedItems.add(dataItem);
				changed = true;
			}			
			else
			{
				d++;
			}
		}
		
		if (changed && super.getNotify())
		{					
			fireSeriesChanged();
		}
	}
	@Override
	public XYDataItem getDataItem(int index) 
	{
		XYDataItem item = super.getDataItem(index);
	
		if (this.logYAxis && item.getY().doubleValue() <= 0)
			return new XYDataItem(item.getX(), Double.NaN);
		else
			return item;
	}

	@Override
	public List getItems() 
	{
		List items = new LinkedList();
		
		for (int i = 0; i < getItemCount(); i++)
		{
			XYDataItem item = super.getDataItem(i);
			
			if (this.logYAxis && item.getY().doubleValue() <= 0)
				items.add(new XYDataItem(item.getX(), Double.NaN));
			else
				items.add(new XYDataItem(item.getX(), item.getY()));
		}
		
		return items;
	}

	@Override
	public Number getY(int index) 
	{
		Number res = super.getY(index);
		
		if (this.logYAxis && res.doubleValue() <= 0)
			return Double.NaN;
		else 
			return res;
	}
	
	@Override
	public XYDataItem remove(Number x) 
	{
		XYDataItem result = null;
		
		int indexD = discardedItems.indexOf(x);
		int indexS = super.indexOf(x);
		
		/* If in discarded items, then remove and return this. */ 
		if (indexD >= 0)
		{
			result = discardedItems.remove(indexD);			
		}
		
		/* If in main items, then remove and return this. (Should not be both in discarded and main items) */ 
		if (indexS >= 0)
		{
			result = super.remove(indexS);			
		}
		
		return result;
	}

	/** 
	 * Check whether with the current settings this data item should be in the discarded
	 * set or in the main dataset
	 * @param item the XYDataItem
	 * @return true if it should be in the main set, false otherwise
	 */
	private boolean checkDataItem(XYDataItem item)
	{
		if (this.logXAxis && item.getX().doubleValue() <= 0)
			return false; // If logaritmic x axis and negative or zero x value, then discard for now.
		if (this.logYAxis && item.getY().doubleValue() <= 0)
			return true;  // If logaritmic y axis and negative or zero y value, then do not discard, simply return NaN.
		else 
			return true;  // Nothing wrong	
	}
}
