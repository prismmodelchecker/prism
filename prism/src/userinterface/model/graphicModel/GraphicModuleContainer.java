//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package userinterface.model.graphicModel;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import javax.swing.border.*;
import userinterface.*;
import java.awt.event.*;
import java.beans.*;

public class GraphicModuleContainer implements FocusListener, AdjustmentListener
{
    public static final int DEFAULT_X_SIZE = 400;
    public static final int DEFAULT_Y_SIZE = 300;
    
    private ModuleModel theModel;
    private ModuleDrawingPane theView;
    private ModulesPanel owner;
    private GUIPrism gui;
    
    private JPanel pan;
    private JScrollPane scroller;
    
    private GraphicModuleContainer thisgmc;
    
    private boolean docked;
    private boolean visible;
    
    private int xSize = DEFAULT_X_SIZE;
    private int ySize = DEFAULT_Y_SIZE;
    
    private int lastInternalX = -1;
    private int lastInternalY = -1;
    
    private int lastExternalX = -1;
    private int lastExternalY = -1;
    
    private JPopupMenu framePopup;
    
    
    
    //The Container is either a JFrame (undocked), or a JInternalFrame (docked)
    private JDialog externalFrame;
    private JInternalFrame internalFrame;
    
    
    /** Creates a new instance of GraphicModule */
    public GraphicModuleContainer(GUIPrism gui, ModuleModel theModel, ModuleDrawingPane theView, ModulesPanel owner)
    {
        this.theModel = theModel;
        this.theView = theView;
        this.owner = owner;
        this.gui = gui;
        
        theModel.setContainer(this);
        
        thisgmc = this;
        
        docked = false;
        
        framePopup = new JPopupMenu();
        
        Action hide = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
            }
        };
        hide.putValue(Action.LONG_DESCRIPTION,  "Hides this graphical module");
        hide.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_H));
        hide.putValue(Action.NAME, "Hide");
        //addModule.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallNewModule.png"));
        
        framePopup.add(hide);
        
        
        setDocked(true);
        setVisible(true);
        
        theView.setContainer(this);
    }
    
    public void cornerScrollTo(int x, int y)
    {
        if(theModel.zoomAreaChanged())
        {
            theModel.doneAreaZoom();
            
            //System.out.println("Zoom in cornerScrollTo is: "+theModel.getZoom());
            //System.out.println("Attempting to zoom to corner: ("+(x/theModel.getZoom())+", "+(y/theModel.getZoom())+")");
            //System.out.println("cornerScrollTo");
            BoundedRangeModel scro = scroller.getVerticalScrollBar().getModel();
            int extent = scro.getExtent();
            int max = scro.getMaximum();
            int min = scro.getMinimum();
            double prop = ((double)y)/((double)theView.getHeight());
            int value = (int)((max*prop));
            if(value+extent > max) value = max - extent;
            if(value < min) value = min;
            
            //////System.out.println("VERTICAL: extent = "+extent+" min = "+min+" max = "+max+" value = "+value+" prop = "+prop);
            //////System.out.println("theView.getHeight() = "  +theView.getHeight());
            scro.setValue(value);
            
            scro = scroller.getHorizontalScrollBar().getModel();
            extent = scro.getExtent();
            max = scro.getMaximum();
            min = scro.getMinimum();
            prop = ((double)x)/((double)theView.getWidth());
            value = (int)((max*prop) );
            if(value+extent > max) value = max - extent;
            if(value < min) value = min;
            
            //////System.out.println("HORIZONTAL: extent = "+extent+" min = "+min+" max = "+max+" value = "+value+" prop = "+prop);
            //////System.out.println("theView.getWidth()= " + theView.getWidth());
            
            scro.setValue(value);
            
        }
    }
    
    
    //sets the position, width and height
    public void setRectangle(java.awt.Rectangle position)
    {
        if(isDocked())
        {
            internalFrame.setLocation(position.x, position.y);
            internalFrame.setSize(position.width, position.height);
        }
    }
    
    public Rectangle getRectangle()
    {
        if(isDocked())
        {
            return new Rectangle(internalFrame.getX(), internalFrame.getY(), internalFrame.getWidth(), internalFrame.getHeight());
        }
        else
        {
            return new Rectangle(0,0,200,200);
        }
    }
    
    
    public void centreScrollTo(int x, int y)
    {
        
        
        if(theModel.zoomChanged())
        {
            theModel.doneZoom();
            
            //System.out.println("centreScrollTo");
            BoundedRangeModel scro = scroller.getVerticalScrollBar().getModel();
            int extent = scro.getExtent();
            int max = scro.getMaximum();
            int min = scro.getMinimum();
            double prop = ((double)y)/((double)theView.getHeight());
            int value = (int)((max*prop) - (extent/2));
            if(value+extent > max) value = max - extent;
            if(value < min) value = min;
            
            //////System.out.println("VERTICAL: extent = "+extent+" min = "+min+" max = "+max+" value = "+value+" prop = "+prop);
            //////System.out.println("theView.getHeight() = "  +theView.getHeight());
            scro.setValue(value);
            
            scro = scroller.getHorizontalScrollBar().getModel();
            extent = scro.getExtent();
            max = scro.getMaximum();
            min = scro.getMinimum();
            prop = ((double)x)/((double)theView.getWidth());
            value = (int)((max*prop) - (extent/2));
            if(value+extent > max) value = max - extent;
            if(value < min) value = min;
            
            //////System.out.println("HORIZONTAL: extent = "+extent+" min = "+min+" max = "+max+" value = "+value+" prop = "+prop);
            //////System.out.println("theView.getWidth()= " + theView.getWidth());
            
            scro.setValue(value);
            
        }
    }
    
    public void setTitle(String str)
    {
        if(docked)
        {
            internalFrame.setTitle(str);
        }
        else
            externalFrame.setTitle(str);
    }
    
    public void setDocked(boolean docked)
    {
        if(docked)
        {
            if(isDocked()) return;
            else //to dock
            {
                if(externalFrame != null)
                {
                    xSize = externalFrame.getWidth();
                    ySize = externalFrame.getHeight();
                    externalFrame.dispose();
                    
                    lastExternalX = externalFrame.getX();
                    lastExternalY = externalFrame.getY();
                }
                externalFrame = null;
                internalFrame = new JInternalFrame(getName(), true, false, true, true);
                internalFrame.getContentPane().setLayout(new BorderLayout());
                
                UIManager.put("ScrollBar.width", new Integer(10));
                
                scroller = new JScrollPane();
                scroller.getVerticalScrollBar().addAdjustmentListener(this);
                scroller.getHorizontalScrollBar().addAdjustmentListener(this);
                pan = new JPanel();
                internalFrame.getContentPane().add(scroller, BorderLayout.CENTER);
                scroller.setViewportView(pan);
                //scroller.getVerticalScrollBar().setVisibleAmount(3);
                
                
                
                pan.setLayout(new GridBagLayout());
                pan.add(theView, new GridBagConstraints());
                
                theView.doSize(theModel.getZoom());
                
                
                internalFrame.pack();
                
                internalFrame.setSize(xSize, ySize);
                
                
                //internalFrame.pack();
                internalFrame.setBorder(new LineBorder(Color.black, 2, false));
                internalFrame.setFrameIcon(GUIPrism.getIconFromImage("smallModule.png"));
                internalFrame.addComponentListener(owner);
                internalFrame.addFocusListener(this);
                //internalFrame.setIconifiable(false);
                internalFrame.addInternalFrameListener((new InternalFrameListener()
                {
                    public void internalFrameActivated(InternalFrameEvent e)
                    {}
                    public void internalFrameClosed(InternalFrameEvent e)
                    {}
                    public void internalFrameClosing(InternalFrameEvent e)
                    {owner.a_hide(thisgmc);}
                    
                    public void internalFrameDeactivated(InternalFrameEvent e)
                    {}
                    
                    public void internalFrameDeiconified(InternalFrameEvent e)
                    {
                        owner.a_show(thisgmc);
                        
                    }
                    
                    public void internalFrameIconified(InternalFrameEvent e)
                    {
                        owner.a_hide(thisgmc);
                    }
                    
                    public void internalFrameOpened(InternalFrameEvent e)
                    {}
                    
                    
                }));
                
                
                
                internalFrame.addMouseListener(new MouseListener()
                {
                    
                    
                    public void mousePressed(MouseEvent e)
                    {
                        //System.out.println("MOUSE PRESSED");
                        if(e.isPopupTrigger())
                        {
                            //System.out.println("e.getComponent = "+e.getComponent().toString());
                            framePopup.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                    
                    public void mouseReleased(MouseEvent e)
                    {
                        //System.out.println("MOUSE RELEASED");
                        if(e.isPopupTrigger())
                        {
                            //System.out.println("e.getComponent = "+e.getComponent().toString());
                            framePopup.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                    
                    public void mouseClicked(MouseEvent e)
                    {
                        //System.out.println("MOUSE CLICKED");
                    }
                    
                    public void mouseEntered(MouseEvent e)
                    {
                        //System.out.println("MOUSE ENTERED");
                    }
                    
                    public void mouseExited(MouseEvent e)
                    {
                        //System.out.println("MOUSE EXITED");
                    }
                });
                
                
                owner.dock(internalFrame);
                
                if(lastInternalX > -1 && lastInternalY > -1)
                {
                    internalFrame.setLocation(lastInternalX, lastInternalY);
                }
                
                this.docked = true;
                
                UIManager.put("ScrollBar.width", new Integer(16));
            }
        }
        else
        {
            if(!isDocked()) return;
            else //to undock
            {
                if(internalFrame != null)
                {
                    xSize = internalFrame.getWidth();
                    ySize = internalFrame.getHeight();
                    
                    lastInternalX = internalFrame.getX();
                    lastInternalY = internalFrame.getY();
                }
                
                
                
                
                externalFrame = new JDialog(gui, getName(), false);
                externalFrame.getContentPane().setLayout(new BorderLayout());
                externalFrame.getContentPane().add(theView, BorderLayout.CENTER);
                externalFrame.getContentPane().setSize(xSize, ySize);
                
                externalFrame.addWindowListener(new WindowListener()
                {
                    public void windowActivated(WindowEvent e)
                    {
                    }
                    
                    public void windowClosed(WindowEvent e)
                    {
                        
                    }
                    
                    public void windowClosing(WindowEvent e)
                    {
                        //////System.out.println("closing");
                        owner.a_hide(thisgmc);
                    }
                    
                    public void windowDeactivated(WindowEvent e)
                    {
                    }
                    
                    public void windowDeiconified(WindowEvent e)
                    {
                    }
                    
                    
                    public void windowIconified(WindowEvent e)
                    {
                    }
                    
                    public void windowOpened(WindowEvent e)
                    {
                    }
                    
                    
                });
                
                externalFrame.addFocusListener(new FocusListener()
                {
                    public void focusGained(FocusEvent e)
                    {
                        owner.desktopLoseFocus();
                    }
                    
                    public void focusLost(FocusEvent e)
                    {
                        owner.ensureFocusIsViewable();
                    }
                    
                    
                });
                externalFrame.addFocusListener(this);
                
                
                //externalFrame.setResizable(false);
                
                UIManager.put("ScrollBar.width", new Integer(10));
                
                scroller = new JScrollPane();
                scroller.getVerticalScrollBar().addAdjustmentListener(this);
                scroller.getHorizontalScrollBar().addAdjustmentListener(this);
                pan = new JPanel();
                externalFrame.getContentPane().add(scroller, BorderLayout.CENTER);
                scroller.setViewportView(pan);
                //scroller.getVerticalScrollBar().setVisibleAmount(3);
                
                
                
                pan.setLayout(new GridBagLayout());
                pan.add(theView, new GridBagConstraints());
                
                
                
                
                
                
                
                
                
                theView.doSize(theModel.getZoom());
                //externalFrame.pack();
                externalFrame.setSize(xSize, ySize);
                
                
                if(lastExternalX > -1 && lastExternalY > -1)
                {
                    externalFrame.setLocation(lastExternalX, lastExternalY);
                }
                else //match the position of the internal frame
                {
                    if(lastInternalX > -1 && lastInternalY > -1 && internalFrame != null)
                    {
                        int posX = internalFrame.getX();
                        int posY = internalFrame.getY();
                        Container comp = internalFrame;
                        
                        //////System.out.println("comp.parent = "+comp.getParent());
                        
                        while(comp.getParent() instanceof Container)
                        {
                            //////System.out.println("par is component");
                            comp = comp.getParent();
                            posX += comp.getX();
                            posY += comp.getY();
                        }
                        
                        
                        externalFrame.setLocation(posX, posY);
                        
                    }
                }
                this.docked = false;
                owner.undock(internalFrame);
                
                if(internalFrame != null)internalFrame.dispose();
                internalFrame = null;
            }
        }
    }
    
    public boolean isDocked()
    {
        return docked;
    }
    
    public void setVisible(boolean visible)
    {
        
        if(isDocked())
        {
            setDocked(true);
            
            //internalFrame.restoreSubcomponentFocus();
            try
            {
                internalFrame.setIcon(false);
            }
            catch(Exception exp)
            {}
            internalFrame.setVisible(visible);
            
        }
        else externalFrame.setVisible(visible);
        
        this.visible = visible;
    }
    
    public boolean isVisible()
    {
        if(docked)
        {
            return internalFrame.isVisible();
        }
        else
        {
            return externalFrame.isVisible();
        }
    }
    
    public String getName()
    {
        return theModel.getModuleName();
    }
    
    public ModuleModel getModuleModel()
    {
        return theModel;
    }
    
    public JInternalFrame getInternalFrame()
    {
        return internalFrame;
    }
    
    public boolean isSelectedModule()
    {
        if(docked)
        {
            return internalFrame.isSelected();
        }
        else
        {
            return externalFrame.isFocusOwner();
        }
    }
    
    public void doPack()
    {
        pan.revalidate();
        pan.repaint();
    }
    
    public void setSelected()
    {
        //////System.out.println("setting selected");
        if(docked)
        {
            try
            {
                internalFrame.setSelected(true);
            }
            catch(PropertyVetoException e)
            {
                //////System.out.println("vetoexception");
            }
        }
        else
        {
            externalFrame.requestFocus();
        }
    }
    
    public double getMaxX()
    {
        return scroller.getViewport().getWidth();
    }
    
    public double getMaxY()
    {
        return scroller.getViewport().getHeight();
    }
    
    public int getViewableWidth()
    {
        return (int)scroller.getViewport().getExtentSize().getWidth();
    }
    
    public int getViewableHeight()
    {
        return (int)scroller.getViewport().getExtentSize().getHeight();
    }
    
    public int getViewOffsetX()
    {
        return (int)scroller.getHorizontalScrollBar().getModel().getValue();
    }
    
    public int getViewOffsetY()
    {
        return (int)scroller.getVerticalScrollBar().getModel().getValue();
    }
    
    public void focusGained(FocusEvent e)
    {
        //        //////System.out.println("focusgained");
        //        //we need to ensure that the panel has the focus to accept key events
        //        if(e.getComponent() instanceof JInternalFrame)
        //        {
        //            ((JInternalFrame)e.getComponent()).getContentPane().getComponent(0).requestFocus();
        //        }
        //        else if(e.getComponent() instanceof JDialog)
        //        {
        //            ((JDialog)e.getComponent()).getContentPane().getComponent(0).requestFocus();
        //        }
    }
    
    public void focusLost(FocusEvent e)
    {
    }
    
    public void adjustmentValueChanged(AdjustmentEvent e)
    {
        theModel.setScrOffsetX(getViewOffsetX());
        theModel.setScrOffsetY(getViewOffsetY());
    }
    
    
}
