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

package chart;
import java.awt.*;
import java.awt.event.*;
//import userinterface.util.*;
import settings.*;
/**
 *
 * @author  fyjava
 */
public class VerticalGraphBorder extends ChartObject
{

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    
    private int position; //LEFT OR RIGHT
    private DoubleSetting offset;
    private VerticalGraphBorder other;
    private HorizontalGraphBorder top;
    private HorizontalGraphBorder bottom;
    
    private double currentWidth;
    
    /** Creates a new instance of VerticalGraphBorder */
    public VerticalGraphBorder(MultiGraphModel theModel, int position, int offset)
    {
        super(theModel,0,0,0.5,100);
        this.position = position;
        switch(position)
        {
            case LEFT:
            {
                this.offset = new DoubleSetting("left border size", new java.lang.Double(offset), "The actual size of the left border.", theModel, false, new RangeConstraint("0.0,"));
                break;
            }
            case RIGHT:
            {
                this.offset = new DoubleSetting("right border size", new java.lang.Double(offset), "The actual size of the right border.", theModel, false, new RangeConstraint("0.0,"));
            }
        }
    }
    
    public java.awt.Color getSelectionColour()
    {
        return Color.green;
    }
    
    public boolean isRenderable()
    {
        return false;
    }
    
    public double getOffset()
    {
        return offset.getDoubleValue();
    }
    
    public DoubleSetting getOffsetProperty()
    {
        return offset;
    }
    
    public boolean isRenderableWhenSelected()
    {
        return true;
    }
    
    public void setOtherVerticalGraphBorder(VerticalGraphBorder oth)
    {
        this.other = oth;
    }
    
    public void setTopHorizontalGraphBorder(HorizontalGraphBorder to)
    {
        this.top = to;
    }
    
    public void setBottomVerticalGraphBorder(HorizontalGraphBorder bott)
    {
        this.bottom = bott;
    }
    
    public void setOffset(double offs) throws SettingException
    {
        this.offset.setValue(new java.lang.Double(offs));
    }
    
    public void updatePosition(double width, double height)
    {
        currentWidth = width;
        if(top != null && bottom != null && other != null)
        {
            switch(position)
            {
                case LEFT:
                    super.x = offset.getDoubleValue();;
                    top.x = x;
                    bottom.x = x;
                    break;
                case RIGHT:
                    super.x = width - offset.getDoubleValue();
                    top.width = x - top.x;
                    bottom.width = x - bottom.x;
                    
            }
        }
    }  
    
    public void mouseDragged(MouseEvent e)
    {
	try
	{
        if(isSelected())
            switch(position)
            {
                case LEFT:
                    theModel.setAutoBorder(false);
                    setOffset(e.getX());
                    break;
                case RIGHT:
                    theModel.setAutoBorder(false);
                    setOffset(currentWidth-e.getX());
            }
	}
	catch(SettingException eee)
	{ //do nothing
	}
    }
 
    public void resize(Resizer sizer, double horiz, double vert)
    {
    }    
    
}
