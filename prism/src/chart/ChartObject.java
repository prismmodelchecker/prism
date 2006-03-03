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
import java.awt.geom.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import settings.*;
/**
 *
 * @author  Andrew Hinton
 */
public abstract class ChartObject extends Rectangle2D.Double implements MouseListener, MouseMotionListener, KeyListener
{
    private static ArrayList objects;
    protected MultiGraphModel theModel;
    
    static
    {
        objects = new ArrayList();
    }
    
    public static void deSelectAll()
    {
        for(int i = 0; i < objects.size(); i++)
            ((ChartObject)objects.get(i)).setSelected(false);
    }
    
    private boolean selected;
    
    /** Creates a new instance of ChartObject */
    public ChartObject(MultiGraphModel theModel, double x, double y, double w, double h)
    {
        super(x,y,w,h);
        this.theModel = theModel;
        selected = false;
        objects.add(this);
    }
    
    public abstract boolean isRenderable();
    
    public abstract boolean isRenderableWhenSelected();
    
    public abstract Color getSelectionColour();
    
    public abstract void updatePosition(double width, double height) throws SettingException;
    
    public abstract void resize(Resizer sizer, double horiz, double vert) throws SettingException;
    
    public boolean isSelected()
    {
		//IF YOU WANNA MOVE STUFF AROUND DECOMMENT THIS
        //return selected;

		return false;
    }
    
    public void setSelected(boolean select)
    {
		//IF YOU WANNA MOVE STUFF AROUND DECOMMENT THIS
        //this.selected = select;
    }
    
    /**This should be overridden by the subclass */
    public void render(Graphics2D g2)
    {
		//IF YOU WANNA MOVE STUFF AROUND DECOMMENT THIS
        /*if(selected)
        {
            if(!isRenderableWhenSelected()) return;
            else
            {
                g2.setColor(getSelectionColour());
                g2.draw(this);
            }
        }
        else*/
        {
            if(!isRenderable()) return;
            else
            {
                g2.setColor(Color.black);
                g2.draw(this);
            }
        }
        
    }
    
    public void mouseClicked(MouseEvent e)
    {
    }
    
    public void mouseEntered(MouseEvent e)
    {
    }
    
    public void mouseExited(MouseEvent e)
    {
    }
    
    public void mousePressed(MouseEvent e)
    {
		//IF YOU WANNA MOVE STUFF AROUND DECOMMENT THIS
        /*
        Rectangle2D.Double click = new Rectangle2D.Double(e.getX()-2, e.getY()-2, 4, 4);
        if(click.intersects(this))
        {
            deSelectAll();
            setSelected(true);
        }*/
    }
    
    public void mouseReleased(MouseEvent e)
    {
    }
    
    public void mouseDragged(MouseEvent e)
    {
    }
    
    public void mouseMoved(MouseEvent e)
    {
    }
    
    public void keyPressed(KeyEvent e)
    {
    }
    
    public void keyReleased(KeyEvent e)
    {
    }
    
    public void keyTyped(KeyEvent e)
    {
    }
    
    
}
