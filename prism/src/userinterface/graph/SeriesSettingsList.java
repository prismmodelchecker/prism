//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
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

import javax.swing.*;

/**
 * Representation of an axis of a Graph.
 * The settings are propagated to the JFreeChart library.
 */
@SuppressWarnings("serial")
public class SeriesSettingsList extends AbstractListModel implements Observer
{
	private Graph graph;
	
	private HashMap<Integer, Graph.SeriesKey> seriesKeys;
	
	public SeriesSettingsList(Graph graph)
	{
		this.graph = graph;
		this.seriesKeys = new HashMap<Integer, Graph.SeriesKey>();
	}	

	public Object getElementAt(int index) 
	{
		synchronized (graph.getSeriesLock())
		{
			return graph.getGraphSeries(seriesKeys.get(index));
		}		
	}
	
	public Graph.SeriesKey getKeyAt(int index)
	{
		synchronized (graph.getSeriesLock())
		{
			return seriesKeys.get(index);
		}
	}

	public int getSize() 
	{
		return seriesKeys.size();
	}
	
	public void updateSeriesList()
	{
		synchronized (graph.getSeriesLock())
		{
			for (Map.Entry<Integer, Graph.SeriesKey> entry : seriesKeys.entrySet())
			{			
				SeriesSettings series = graph.getGraphSeries(entry.getValue());
				if (series != null)
					series.deleteObserver(this);
			}
			
			seriesKeys.clear();
			
			for (Graph.SeriesKey key: graph.getAllSeriesKeys())
			{
				seriesKeys.put(graph.getJFreeChartIndex(key), key);
				graph.getGraphSeries(key).updateSeries();
				graph.getGraphSeries(key).addObserver(this);				
			}
		}
		
		fireContentsChanged(this, 0, this.getSize());		
	}
	
	public void update(Observable o, Object arg) 
	{		
		fireContentsChanged(this, 0, this.getSize());
	}
}
