//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

package chart;
import java.awt.*;
import java.awt.event.*;
import settings.*;
/**
 *
 * @author  Andrew Hinton
 */
public class FourBorders extends ChartObject
{
    private HorizontalGraphBorder top,bottom;
    private VerticalGraphBorder left,right;
    private Resizer topLeft, topRight, bottomLeft, bottomRight, topR, bottomR, leftR, rightR;
    
    private double width, height;
    
    /** Creates a new instance of FourBorders */
    public FourBorders(MultiGraphModel theModel, HorizontalGraphBorder top, HorizontalGraphBorder bottom, VerticalGraphBorder left, VerticalGraphBorder right)
    {
        super(theModel, left.x, top.y, right.x-left.x, bottom.y - top.y);
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
        
        topLeft = new Resizer(super.x, super.y, 4, this, Resizer.BOTH_RESIZE);
        topRight = new Resizer(super.x+super.width, super.y, 4, this, Resizer.BOTH_RESIZE);
        bottomLeft = new Resizer(super.x, super.y+super.height, 4, this, Resizer.BOTH_RESIZE);
        bottomRight = new Resizer(super.x+super.width, super.y+super.height, 4, this, Resizer.BOTH_RESIZE);
        topR = new Resizer(super.x + (super.width/2.0), super.y, 4, this, Resizer.VERTICAL_RESIZE);
        bottomR = new Resizer(super.x +(super.width/2.0), super.y+super.height, 4, this,  Resizer.VERTICAL_RESIZE);
        leftR = new Resizer(super.x, super.y+(super.height/2), 4, this, Resizer.HORIZONTAL_RESIZE);
        rightR = new Resizer(super.x+super.width, super.y+(super.height/2), 4, this, Resizer.HORIZONTAL_RESIZE);
        width = 100;
        height = 100;
    }
    
    public java.awt.Color getSelectionColour()
    {
        return Color.green;
    }
    
    public boolean isRenderable()
    {
        return false;
    }
    
    public boolean isRenderableWhenSelected()
    {
        return true;
    }
    
    public void render(Graphics2D g2)
    {
        super.render(g2);
        if(isSelected())
        {
            g2.setColor(getSelectionColour());
            g2.draw(this);
            
            topLeft.render(g2);
            topRight.render(g2);
            bottomLeft.render(g2);
            bottomRight.render(g2);
            topR.render(g2);
            bottomR.render(g2);
            leftR.render(g2);
            rightR.render(g2);
        }
    }
    
    public void updatePosition(double width, double height)
    {
        super.x = left.x;
        super.y = top.y;
        super.width = right.x-left.x;
        super.height = bottom.y-top.y;
        
        topLeft.updatePosition(super.x, super.y);
        topRight.updatePosition(super.x+super.width, super.y);
        bottomLeft.updatePosition(super.x, super.y+super.height);
        bottomRight.updatePosition(super.x+super.width, super.y+super.height);
        topR.updatePosition(super.x+(super.width/2.0), super.y);
        bottomR.updatePosition(super.x+(super.width/2.0), super.y+super.height);
        leftR.updatePosition(super.x, super.y+(super.height/2));
        rightR.updatePosition(super.x+super.width, super.y+(super.height/2));
        
        this.width = width;
        this.height = height;
    }
    
    public void resize(Resizer sizer, double horiz, double vert) throws SettingException
    {
        if(sizer == bottomRight)
        {
            theModel.setAutoBorder(false);
            if(shiftDown)
            {
                double xChange = right.getOffset() - (width-horiz);
                double yChange = bottom.getOffset() - (height-vert);
                double maxChange = Math.min(xChange, yChange);
                right.setOffset(right.getOffset() - maxChange);
                bottom.setOffset(bottom.getOffset() - maxChange);
            }
            else
            {
            right.setOffset(width-horiz);
            bottom.setOffset(height-vert);
            }
        }
        else if(sizer == bottomLeft)
        {
            if(shiftDown)
            {
                double xChange = left.getOffset() - (horiz);
                double yChange = bottom.getOffset() - (height-vert);
                double maxChange = Math.min(xChange, yChange);
                left.setOffset(left.getOffset() - maxChange);
                bottom.setOffset(bottom.getOffset() - maxChange);
            }
            else
            {
            theModel.setAutoBorder(false);
            left.setOffset(horiz);
            bottom.setOffset(height-vert);
            }
        }
        else if(sizer == topLeft)
        {
            if(shiftDown)
            {
                double xChange = left.getOffset() - (horiz);
                double yChange = top.getOffset() - (vert);
                double maxChange = Math.min(xChange, yChange);
                left.setOffset(left.getOffset() - maxChange);
                top.setOffset(top.getOffset() - maxChange);
            }
            else
            {
            theModel.setAutoBorder(false);
            left.setOffset(horiz);
            top.setOffset(vert);
            }
        }
        else if(sizer == topRight)
        {
            if(shiftDown)
            {
                double xChange = right.getOffset() - (width-horiz);
                double yChange = top.getOffset() - (vert);
                double maxChange = Math.min(xChange, yChange);
                right.setOffset(right.getOffset() - maxChange);
                top.setOffset(top.getOffset() - maxChange);
            }
            else
            {
            theModel.setAutoBorder(false);
            right.setOffset(width-horiz);
            top.setOffset(vert);
            }
        }
        else if(sizer == topR)
        {
            theModel.setAutoBorder(false);
            top.setOffset(vert);
        }
        else if(sizer == bottomR)
        {
            theModel.setAutoBorder(false);
            bottom.setOffset(height - vert);
        }
        else if(sizer == leftR)
        {
            theModel.setAutoBorder(false);
            left.setOffset(horiz);
        }
        else if(sizer == rightR)
        {
            theModel.setAutoBorder(false);
            right.setOffset(width-horiz);
        }
    }
    
    int lastX,lastY;
    public void mousePressed(MouseEvent e)
    {
        lastX = e.getX();
        lastY = e.getY();
        Resizer.deSelectAll();
        
        topLeft.mousePressed(e);
        topRight.mousePressed(e);
        bottomLeft.mousePressed(e);
        bottomRight.mousePressed(e);
        topR.mousePressed(e);
        bottomR.mousePressed(e);
        leftR.mousePressed(e);
        rightR.mousePressed(e);
        
        if(!topLeft.isSelected() &&
        !topRight.isSelected() &&
        !bottomLeft.isSelected() &&
        !bottomRight.isSelected() &&
        !topR.isSelected() &&
        !bottomR.isSelected() &&
        !leftR.isSelected() &&
        !rightR.isSelected())
            super.mousePressed(e);
        else setSelected(true);
    }
    
    public void mouseDragged(MouseEvent e)
    {
        if(isSelected())
        {
            if(topLeft.isSelected() ||
            topRight.isSelected() ||
            bottomLeft.isSelected() ||
            bottomRight.isSelected() ||
            topR.isSelected() ||
            bottomR.isSelected() ||
            leftR.isSelected() ||
            rightR.isSelected())
            {
                topLeft.mouseDragged(e);
                topRight.mouseDragged(e);
                bottomLeft.mouseDragged(e);
                bottomRight.mouseDragged(e);
                topR.mouseDragged(e);
                bottomR.mouseDragged(e);
                leftR.mouseDragged(e);
                rightR.mouseDragged(e);
            }
            else //move the whole thing
            {
		try
		{
                theModel.setAutoBorder(false);
                int changeX = e.getX() - lastX;
                int changeY = e.getY() - lastY;
                top.setOffset(top.getOffset()+changeY);
                bottom.setOffset(bottom.getOffset()-changeY);
                right.setOffset(right.getOffset()-changeX);
                left.setOffset(left.getOffset()+changeX);
		}
		catch(SettingException eee)
		{//do nothing
		}
            }
        }
        lastX = e.getX();
        lastY = e.getY();
    }
    
    private boolean shiftDown = false;
    public void keyPressed(KeyEvent e)
    {
        if(e.getKeyCode() == KeyEvent.VK_SHIFT)
        {
            shiftDown = true;
        }
    }
    
    public void keyReleased(KeyEvent e)
    {
        if(e.getKeyCode() == KeyEvent.VK_SHIFT)
        {
            shiftDown = false;
        }
    }
    
}
