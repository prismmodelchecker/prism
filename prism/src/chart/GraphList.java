//==============================================================================
//	
//	Copyright (c) 2002-2004, Andrew Hinton
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

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
//import userinterface.util.*;
import settings.*;

/** Overrides ArrayList to gives each graph some identifying features.  Its data is
 * an array of GraphPoints describing a series of data.
 */
public class GraphList extends ArrayList implements SettingOwner
{
    //Properties
    private SingleLineStringSetting seriesHeading;
    //private DataProperty chartData;
    private ColorSetting seriesColour;
    private BooleanSetting showPoints;
    private ChoiceSetting seriesShape;
    private BooleanSetting showLines;
    private DoubleSetting lineWidth;
    private ChoiceSetting lineStyle;
    private SeriesDataSetting dataProp; 
    
    //Editor
    
    private GraphListEditor listEditor;
    
    //The Model
    
    private MultiGraphModel theModel;
   
    //Constant for the shape of the points on this data series
    
    /** shape index constant */    
    public static final int CIRCLE = 0;
    /** shape index constant */    
    public static final int SQUARE = 1;
    /** shape index constant */    
    public static final int TRIANGLE = 2;
    /** shape index constant */    
    public static final int RECTANGLE_H = 3;
    /** shape index constant */    
    public static final int RECTANGLE_V = 4;
    /** shape index constant */    
    public static final int NONE = 5;
    private static final int[] x = {3,0,6};//for triangles
    private static final int[] y = {0,6,6};//for triangles
    private static final Shape[] shapes = {new Ellipse2D.Double(0,0,4,4),new Rectangle2D.Double(0,0,4,4), new Polygon(x,y,3), new Rectangle2D.Double(0,0,7,2),new Rectangle2D.Double(0,0,2,7), new Rectangle2D.Double(0,0,0,0)};
    
    public static final Color[] DEFAULT_COLORS = {
        new Color(0,0,180),		//blue 
        new Color(0,150,0),		//green
        new Color(255,0,0),		//red
        new Color(0,180,180),	//cyan
        new Color(180,0,180),	//magenta 
        new Color(180,180,0),	//yellow 
        new Color(0,0,0),   	//black 
        new Color(255,150,150),	//pink 
        new Color(150,150,150),	//grey 
        new Color(255,150,0)};	//orange 

    //Constructor
    
    /** Creates a new GraphList with the given title, colour and shape.
     * @param title the title of this data series
     * @param colour the colour of this data series
     * @param point the shape index of the points in this data series.  See shape index constants.
     *
     */    
    public GraphList(MultiGraphModel theModel, String title, Color colour, int point)
    {
        super();
        this.theModel = theModel;
        
        seriesHeading = new SingleLineStringSetting("heading", title, "The heading for this series, as displayed in the legend.", this, true);
        seriesColour = new ColorSetting("colour", colour, "The colour for all lines and points in this series.", this, true);
        showPoints = new BooleanSetting("show points", new Boolean(true), "Should points be displayed for this series?", this, true);
        String[] choices = { "Circle", "Square", "Triangle", "Horizontal Rectangle", "Vertical Rectangle", "None" };
        seriesShape = new ChoiceSetting("point shape", choices, choices[point], "The shape of points for this series.", this, true);
        showLines = new BooleanSetting("show lines", new Boolean(true), "Should lines be displayed for this series?", this, true);
        lineWidth = new DoubleSetting("line width",  new Double(1.0), "The line width for this series.", this, true, new RangeConstraint("0.0,"));
        String [] styles = { "---------", "- - - - -", "- -- - --" };
        lineStyle = new ChoiceSetting("line style", styles, styles[0], "The line style for this series.", this, true);
        //dataProp = new SeriesDataSetting("series data", this, "the data for this series.", this, false);
        dataProp = new SeriesDataSetting("series data", this, "Click to view/edit the data for this series.", this, true);
        
        listEditor = new GraphListEditor(this, theModel.getXAxis(), theModel.getYAxis());
    }
    
    //Access methods
    
    public GraphListEditor getEditor()
    {
        return listEditor;
    }

	public MultiGraphModel getTheModel()
	{
		return theModel;
	}
    
    
    /** Access method to return the title of this data series. */    
    public String getGraphTitle()
    {
        return seriesHeading.getStringValue();
    }
    
    /** Access method to return the colour of this data series. */    
    public Color getGraphColour()
    {
        return seriesColour.getColorValue();
    }
    
    /** Access method to return the shape of points of this data series. */    
    public Shape getPointShape()
    {
        if(!isDrawPoint()) return shapes[NONE];
        else return shapes[getPointIndex()];
    }
    
    /** The shape of the points for this data series can also be described by an index.
     * This returns the index of the shape being used.  see shape constants.
     */    
    public int getPointIndex()
    {
        return seriesShape.getCurrentIndex();
    }
    
    /** Access method to state whether this graph should draw its points. */    
    public boolean isDrawPoint()
    {
        return showPoints.getBooleanValue();
    }
    
    public double getLineWidth()
    {
        return lineWidth.getDoubleValue();
    }
    
    public int getLineStyle()
    {
        return lineStyle.getCurrentIndex();
    }
    
    /** Access method to state whether this graph should join its points together in the
     * display.
     */    
    public boolean isShowLines()
    {
        return showLines.getBooleanValue();
    }
    
    /** Sets whether the points should be drawn to the screen. */    
    public void setDrawPoint(boolean b) throws SettingException
    {
        showPoints.setValue(new Boolean(b));
    }
    
    /** Sets whether the lines which join the points should be drawn. */    
    public void setShowLines(boolean b) throws SettingException
    {
        showLines.setValue(new Boolean(b));
    }
    
    /**Wrapper method, to get rid of irritating casting!*/
    public GraphPoint getPoint(int index)
    {
        return (GraphPoint)super.get(index);
    }
    
    //Update methods
    
    /** Sets the series title to the given string. */    
    public void setGraphTitle(String str)
    {
        try
        {
        seriesHeading.setValue(str);
        }
        catch(SettingException e)
        {
        
        }
    }
    
    /** Sets the series colour to the given colour. */    
    public void setGraphColor(Color col) throws SettingException
    {
        seriesColour.setValue(col);
    }
    
    public void tellSeriesHeadingAboutGraphOptionsPanel(GraphOptionsPanel gop)
    {
        //seriesHeading.addObserver(gop);
    }
    
    /** Sets the graph point shape to the given shape index. */    
    public void setShape(int shp)
    {
        try
        {
            seriesShape.setSelectedIndex(shp);
        }
        catch(SettingException e)
        {
        
        }
    }
    
    public void setLineWidth(double lw) throws SettingException
    {
        lineWidth.setValue(new Double(lw));
    }
    
    public void setLineStyle(int i) throws SettingException
    {
        lineStyle.setSelectedIndex(i);
    }
    
    /** Using selection sort, (due to the items mostly appearing in order), this method
     * sorts points by a given axis.  The integer supplied defines which axis the
     * values should be sorted by.
     * @param sortType which axis the values should be sorted by:
     * <ul>
     *    <li>0 HORIZONTAL (x)
     *    <li>1 VERTICAL (y)
     * </ul>
     */    
    public void sortPoints(int sortType)
    {
        // Using Selection sort because most the time will be inserted in order
        for(int i = 0; i < size()-1; i++)
        {
            int k = i;
            for(int j = i+1; j<size(); j++)
            {
                double compj;
                if(sortType == 0) compj = (getPoint(j)).getXCoord();
                else compj = (getPoint(j)).getYCoord();
                
                double compk;
                if(sortType == 0) compk = (getPoint(k)).getXCoord();
                else compk = (getPoint(k)).getYCoord();
                
                if(compj<compk) k = j;
            }
            
            Object o = getPoint(i);
            Object o2 = getPoint(k);
            if(o != o2)
            {
                set(i,o2);
                set(k,o);
            }
        }
    }
    
    /** Using selection sort, (due to the items mostly appearing in order), this method
     * sorts points by the x(horizontal) axis.
     */    
    public void sortPoints()
    {
        sortPoints(0);
    }
    
    public int compareTo(Object o)
    {
        if(o instanceof SettingOwner)
        {
            SettingOwner po = (SettingOwner) o;
            if(getSettingOwnerID() < po.getSettingOwnerID())return -1;
            else if(getSettingOwnerID() > po.getSettingOwnerID()) return 1;
            else return 0;
        }
        else return 0;
    }
    
    
    
    public void doChange() throws SettingException
    {
        theModel.doChange();
    }
    
    public int getNumSettings()
    {
        return 8;
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
            case 7: return dataProp;
            default: return null;
        }
    }
    
    public String getSettingOwnerClassName()
    {
        return "Series";
    }
    
    public int getSettingOwnerID()
    {
        return prism.PropertyConstants.SERIES;
    }
    
    public String getSettingOwnerName()
    {
        return seriesHeading.getStringValue();
    }
    
    public void notifySettingChanged(Setting setting)
    {
		theModel.changed();
		theModel.gop.doListTitles();
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
    
