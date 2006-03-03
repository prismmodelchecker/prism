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

public class HorizontalGraphBorder extends ChartObject
{
    
    public static final int TOP = 0;
    public static final int BOTTOM = 1;
    
    private int position; //TOP or BOTTOM
    private DoubleSetting offset;
    private HorizontalGraphBorder other;
    private VerticalGraphBorder left;
    private VerticalGraphBorder right;
    
    private double currentHeight;
    
    /** Creates a new instance of HorizontalGraphBorder */
    public HorizontalGraphBorder(MultiGraphModel theModel, int position, int offset)
    {
        super(theModel, 0, 0, 100, 0.5);
        this.position = position;
        switch(position)
        {
            case TOP:
            {
                this.offset = new DoubleSetting("top border size", new java.lang.Double(offset), "The actual size of the top border", theModel, false );
                break;
            }
            case BOTTOM:
            {
                this.offset = new DoubleSetting("bottom border size", new java.lang.Double(offset), "The actual size of the bottom border", theModel, false);

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
    
    public void updatePosition(double width, double height) throws SettingException
    {
        currentHeight = height;
        if(left != null && right != null && other != null)
        {
            switch(position)
            {
                case TOP:
                    super.y = offset.getDoubleValue();
                    left.y = y;
                    right.y = y;
                    if(y > other.y)
                    {
                        y = other.y - 1;
                        offset.setValue(new java.lang.Double(y));
                    }
                    break;
                case BOTTOM:
                    super.y = height - offset.getDoubleValue();
                    left.height = y - left.y;
                    right.height = y - right.y;
                    
            }
        }
    }
    
    public boolean isRenderableWhenSelected()
    {
        return true;
    }
    
    public void setOtherHorizontalGraphBorder(HorizontalGraphBorder other)
    {
        this.other = other;
    }
    
    public void setLeftVerticalGraphBorder(VerticalGraphBorder left)
    {
        this.left = left;
    }
    
    public void setRightVerticalGraphBorder(VerticalGraphBorder right)
    {
        this.right = right;
    }
    
    public void setOffset(double offs) throws SettingException
    {
        if(!(position == TOP && offs+10 > other.y))
            this.offset.setValue(new java.lang.Double(offs)); 
    }
    
    public void mouseDragged(MouseEvent e)
    {
	try
	{
        if(isSelected())
            switch(position)
            {
                case TOP:
                    theModel.setAutoBorder(false);
                    setOffset(e.getY());
                    break;
                case BOTTOM:
                    theModel.setAutoBorder(false);
                    setOffset(currentHeight-e.getY());
                    
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
