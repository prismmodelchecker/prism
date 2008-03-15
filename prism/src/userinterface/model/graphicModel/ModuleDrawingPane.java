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

//Java Packages

import javax.swing.*;
import javax.swing.JPanel;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.print.*;
import userinterface.*;


/** This class extends JPanel to provide a view for a ModuleModel.  It implements
 * Observer in order to "observe" a ModuleModel, and also contains the listener
 * methods which call the update methods in the model, so it also acts as a
 * controller.  This method also contains the painting method paint(Graphics) which
 * draws the module diagram to the graphics canvas.
 */
public class ModuleDrawingPane extends JPanel implements Observer, MouseListener, MouseWheelListener, FocusListener, MouseMotionListener, Printable, Scrollable, KeyListener
{
    //Constants
    public static final int EDGE = 90;
    
    //Attributes
    private ModuleModel theModel;
    private Color gridColor, selectionColour, zoomColour;
    private double minX = 200, minY = 200;
    private double maxX, maxY;
    private double borderOffsetX =0, borderOffsetY = 0;
    private double printZoom;
    
    private BufferedImage bi;
    
    private Rectangle rect;
    private GraphicModuleContainer container;
    
    Cursor zoom, mouse, cross, crossZoom, mouseAndMove, mouseAndTransition;
    
    //Constructors
    
    /** Constructs a new ModuleDrawingPane with the given ModuleModel as a model. */
    public ModuleDrawingPane(ModuleModel theModel)
    {
        // create a cursor for 'image' with the hotspot at 0, 0.
        zoom = Toolkit.getDefaultToolkit().createCustomCursor(GUIPrism.getIconFromImage("mouseZoom.png").getImage(), new Point(0, 0), "ZoomMouse");
        mouse = Toolkit.getDefaultToolkit().createCustomCursor(GUIPrism.getIconFromImage("mouse.png").getImage(), new Point(0, 0), "NormalMouse");
        cross = Toolkit.getDefaultToolkit().createCustomCursor(GUIPrism.getIconFromImage("mouseCross.png").getImage(), new Point(6, 6), "Cross");
        crossZoom = Toolkit.getDefaultToolkit().createCustomCursor(GUIPrism.getIconFromImage("mouseCrossZoom.png").getImage(), new Point(6, 6), "CrossZoom");
        mouseAndMove = Toolkit.getDefaultToolkit().createCustomCursor(GUIPrism.getIconFromImage("mouseAndMove.png").getImage(), new Point(0, 0), "MouseAndMove");
        mouseAndTransition = Toolkit.getDefaultToolkit().createCustomCursor(GUIPrism.getIconFromImage("mouseAndTransition.png").getImage(), new Point(0,0), "MouseAndTransition");
        // set this cursor on a component
        //comp.setCursor(curs);
        this.theModel = theModel;
        gridColor = new Color(240,240,240); //light Gray
        selectionColour = new Color(255, 0, 0); // deep red
        zoomColour = new Color(0, 0, 255); // deep blue
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);
        addFocusListener(this);
        maxX = 200;
        maxY = 200;
        //setPreferredSize(new Dimension((int)maxX, (int)maxY));
        printZoom = 1;
    }
    
    
    public boolean isFocusable()
    {
        return true;
    }
    public void setContainer(GraphicModuleContainer container)
    {
        this.container = container;
    }
    
    //Access methods
    
    /** Returns the model associated with this view. */
    public ModuleModel getModel()
    {
        return theModel;
    }
    
    
    /** Method used for printing.*/
    public int print(Graphics g, PageFormat pf, int page) throws PrinterException
    {
        if(page>=1) return Printable.NO_SUCH_PAGE;
        Graphics2D g2 = (Graphics2D)g;
        //setSize(new Dimension((int)(pf.getWidth ()-(pf.getImageableX ()*2)), (int)(pf.getHeight ()-(pf.getImageableY ()*2))));
        g2.translate(pf.getImageableX()+2, pf.getImageableY()+2);
        double w = this.getWidth();
        double h = this.getHeight();
        double wzoom = w / (pf.getWidth()-(pf.getImageableX()*2));
        double hzoom = h /(pf.getHeight()-(pf.getImageableY()*2));
        printZoom = 1/Math.max(wzoom, hzoom);
        paintComponent(g2, true);
        return Printable.PAGE_EXISTS;
    }
    
    //Update methods
    
    /** This method is required to implement the Observer interface.  It is called when
     * an update is completed in the model.
     */
    public void update(java.util.Observable observable, Object obj)
    {
        if(obj instanceof Rectangle)
        {
            rect = (Rectangle)obj;
            //repaint(5, 5, 5, 5);
            repaint(rect);
        }
        else
        {
            repaint();
        }
    }
    
    /*public void update()
    {
        repaint();
    }*/
    
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        
        paintComponent(g, false);
    }
    
    public void setModel(ModuleModel mm)
    {
        this.theModel = mm;
        repaint();
    }
    
    public void doSize(double zoom)
    {
        setPreferredSize(new Dimension((int)(maxX*zoom), (int)(maxY*zoom)));
        theModel.notifyDimensions((int)(maxX*zoom), (int)(maxY*zoom));
        
        if(container != null)container.doPack();
        this.validate();
        theModel.notifyViewingArea(getViewableWidth(), getViewableHeight());
        //theModel.setScrOffsetX(getViewOffsetX());
        //theModel.setScrOffsetY(getViewOffsetY());
    }
    
    public int getViewOffsetX()
    {
        if(container != null)
            return container.getViewOffsetX();
        else
            return 0;
    }
    
    public int getViewOffsetY()
    {
        if(container != null)
            return container.getViewOffsetY();
        else
            return 0;
    }
    
    public int getViewableWidth()
    {
        if(container != null)
            return container.getViewableWidth();
        else
            return 0;
    }
    
    public int getViewableHeight()
    {
        if(container != null)
            return container.getViewableHeight();
        else
            return 0;
    }
    
    boolean doNotRender = false;
    /** This method contains all of the painting required to draw the module diagram to
     * the screen.
     */
    //int recurseCounter = 0;
    
    public void paintComponent(Graphics g, boolean isPrinting)
    {
        
        Rectangle clipBounds = (Rectangle)g.getClipBounds().clone();
        
        
        
        
        
        if(clipBounds == null) 
        {
            clipBounds = new Rectangle(0,0,getWidth(), getHeight());
        }
        
            
            
            while(clipBounds.x%2 != 0)
            {
                clipBounds.x--;
                clipBounds.width++;
            }
            
            while(clipBounds.y%2 != 0)
            {
                clipBounds.y--;
                clipBounds.height++;
            }
        
        clipBounds.x /= theModel.getZoom();
            clipBounds.y /= theModel.getZoom();
            clipBounds.width /= theModel.getZoom();
            clipBounds.height /= theModel.getZoom();
            clipBounds.width += 10;
            clipBounds.height += 10;
        
        //g.setClip(clipBounds);
        //PREAMBLE
        //========
        
        Graphics2D g2 = (Graphics2D)g;
        
        
        
        //Set up appropriate zoom and transforms
        AffineTransform tx = g2.getTransform();
        double zoom;
        if(isPrinting)
        {
            zoom = printZoom;
        }
        else
        {
            zoom = theModel.getZoom();
        }
        tx.scale(zoom, zoom);
        g2.setTransform(tx);
        
        
        //If there is no model, paint a grey panel
        if(theModel == null)
        {
            g2.setColor(Color.lightGray);
            //g2.fill(clipBounds);
            return;
        }
        
        
        //DOES THE JPANEL NEED TO BE RESIZED?
        //===================================
        
        //Default minimums are the taken from the scrollpanes current total area (accounting for zoom)
        double maxX = ((container.getMaxX()/zoom)-EDGE);
        double maxY = ((container.getMaxY()/zoom)-EDGE);
        
        //Find whether any state is outside of the current default minimum area, if so change to accomodate
        for(int i = 0; i < theModel.getNumStates(); i++)
        {
            State st = theModel.getState(i);
            if(st.getX() > maxX) maxX = st.getX();
            if(st.getY() > maxY) maxY = st.getY();
        }
        
        //Find out whether maxX and maxY differ from the current size
        
        boolean changeSize = false;
        maxX += EDGE;
        maxY += EDGE;
        if((int)(maxX*zoom) != getWidth())
        {
            changeSize = true;
        }
        if((int)(maxY*zoom) != getHeight())
        {
            changeSize = true;
        }
        
        //set the maxX and maxY attribute, so that other classes can query this for the size
        this.maxX = maxX;
        this.maxY = maxY;
        
        
        if(changeSize)
        {
            doNotRender = true;
            doSize(zoom);
            doNotRender = false;
            if((container != null && (theModel.zoomAreaChanged()|| theModel.zoomChanged())))
            {
                return;
            }
            
            
        }
        else if(container != null && theModel.zoomChanged())
        {
            doNotRender = true;
            container.centreScrollTo(theModel.getCentreX(), theModel.getCentreY());
            doNotRender = false;
            repaint();
            return;
            
        }
        else if(container != null && theModel.zoomAreaChanged())
        {
            doNotRender = true;
            container.cornerScrollTo(theModel.getCornerX(), theModel.getCornerY());
            doNotRender = false;
            repaint();
            return;
        }
        if(doNotRender)
        {
            return;
        }
        
		
        //RENDERING
        //=========
        
        
        boolean longLabels = theModel.isShowLongLabels();
        
        //Blank Background
        double frameWidth = getWidth();
        double frameHeight = getHeight();
        g2.setColor(Color.white);
        //g2.fill(new Rectangle2D.Double(0,0,frameWidth/zoom, frameHeight/zoom));  //??
        g2.fill(clipBounds);
        
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if(theModel.isShowGrid())
        {
            //Draw Grid
            double gridWidth = theModel.getGridWidth();
            float[] dashPattern2 =
            { 1, 2 };
            //g2.setStroke(new BasicStroke(1.0F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0F, dashPattern2, 0));
            g2.setColor(gridColor);
            /*if(clipBounds.width < getWidth())
            {
                g2.setColor(Color.blue);
             }*/
            for(double i = -borderOffsetX; i < frameWidth/zoom; i=i+gridWidth)
            {
                Line2D lin = new Line2D.Double(i,clipBounds.y,i,clipBounds.y+clipBounds.height);
                if(lin.intersects(clipBounds))g2.draw(lin);
            }
            for(double i = -borderOffsetY; i < frameHeight/zoom; i=i+gridWidth)
            {
                Line2D lin = new Line2D.Double(clipBounds.x, i, clipBounds.x+clipBounds.width, i);
                if(lin.intersects(clipBounds))g2.draw(lin);
            }
            
            g2.setStroke(new BasicStroke());
        }
        
        //Draw Transitions
        for(int i = 0; i < theModel.getNumTransitions(); i++)
            theModel.getTransition(i).render(g2, longLabels);
        
        
        //Draw States
        for(int i = 0; i < theModel.getNumStates(); i++)
        {
            State st = theModel.getState(i);
            st.render(g2, i, longLabels);
            
        }
        
        //Draw drawing of a transition
        if(theModel.isDrawingTrans())
        {
            g2.setColor(Color.gray);
            double fromX = theModel.getTempFromState().getX()+15;
            double fromY = theModel.getTempFromState().getY()+15;
            for(int i = 0; i < theModel.getNumTempProbNails(); i++)
            {
                double toX = theModel.getTempProbNail(i).getX();
                double toY = theModel.getTempProbNail(i).getY();
                g2.draw(new Line2D.Double(fromX, fromY, toX, toY));
                fromX = toX;
                fromY = toY;
            }
            g2.setColor(Color.green);
            g2.draw(new Line2D.Double(theModel.getTranDrawX1(), theModel.getTranDrawY1(), theModel.getTranDrawX2(), theModel.getTranDrawY2()));
        }
        
        //Draw drawing of a probablistic transition
        if(theModel.isDrawingProbTrans())
        {
            if(theModel.getTempProbFrom() != null) //if we have a from state
            {
                g2.setColor(Color.gray);
                double fromX = theModel.getTempProbFrom().getX()+15;
                double fromY = theModel.getTempProbFrom().getY()+15;
                double probDecX;
                double probDecY;
                if(!theModel.isTempProbNailDown()) //if there is no nail down
                {
                    double mix = fromX;
                    double miy = fromY;
                    double max = fromX;
                    double may = fromY;
                    for(int i = 0; i < theModel.getTempProbTo().size(); i++)
                    {
                        State to = (State)(theModel.getTempProbTo().get(i));
                        mix = Math.min(to.getX()+15, mix);
                        max = Math.max(to.getX()+15, max);
                        miy = Math.min(to.getY()+15, miy);
                        may = Math.max(to.getY()+15, may);
                    }
                    mix = Math.min(theModel.getTranDrawX2(), mix);
                    max = Math.max(theModel.getTranDrawX2(), max);
                    miy = Math.min(theModel.getTranDrawY2(), miy);
                    may = Math.max(theModel.getTranDrawY2(), may);
                    probDecX = (mix+max)/2;
                    probDecY = (miy+may)/2;
                }
                else
                {
                    //double fromX = theModel.getTempProbFrom().getX()+15;
                    //double fromY = theModel.getTempProbFrom().getY()+15;
                    for(int i = 0; i < theModel.getNumTempProbNails(); i++)
                    {
                        double toX = theModel.getTempProbNail(i).getX();
                        double toY = theModel.getTempProbNail(i).getY();
                        g2.draw(new Line2D.Double(fromX, fromY, toX, toY));
                        fromX = toX;
                        fromY = toY;
                    }
                    probDecX = theModel.getLastProbNail().getX();
                    probDecY = theModel.getLastProbNail().getY();
                }
                if(theModel.getTempProbTo().size() == 0) g2.setColor(Color.green);
                g2.draw(new Line2D.Double(fromX, fromY, probDecX, probDecY));
                
                for(int i = 0; i < theModel.getTempProbTo().size();i++)
                {
                    State toState = (State)(theModel.getTempProbTo().get(i));
                    
                    int counter = 0;
                    ////System.out.println("i =   "+i);
                    for(int j = 0; j < theModel.getTempProbTo().size(); j++)
                    {
                        State otherState = (State)(theModel.getTempProbTo().get(j));
                        if(otherState == toState)
                        {
                            counter++;
                            
                        }
                    }
                    ////System.out.println("counter   = " + counter);
                    if(counter == 1)
                    {
                        double probToX =toState.getX()+15;
                        double probToY = toState.getY()+15;
                        if(!(theModel.getTempProbFrom() == toState))
                            g2.draw(new Line2D.Double(probDecX, probDecY, probToX, probToY));
                        else
                        {
                            g2.draw(new Line2D.Double(probDecX,  probDecY,  ((probDecX+probToX)/2)+30,  ((probDecY+probToY)/2)+30));
                            g2.draw(new Line2D.Double(((probDecX+probToX)/2)+30,  ((probDecY+probToY)/2)+30, probToX, probToY));
                        }
                    }
                    else
                    {
                        for(int j = 0; j < counter; j++)
                        {
                            double offset = (-(((counter-1)*30)/2))+(j*30);
                            double x1 = probDecX;
                            double y1 = probDecY;
                            double x2 = toState.getX()+15;
                            double y2 = toState.getY()+15;
                            
                            double nailx = ((x1+x2)/2)+((Math.sin((Math.PI/2.0)-(Math.atan(((x1-x2)/2)/((y1-y2)/2)))))*((offset)));
                            double naily = ((y1+y2)/2)-((Math.cos((Math.PI/2.0)-(Math.atan(((x1-x2)/2)/((y1-y2)/2)))))*((offset)));
                            
                            g2.draw(new Line2D.Double(probDecX, probDecY, nailx, naily));
                            g2.draw(new Line2D.Double(nailx, naily, x2, y2));
                            
                        }
                    }
                }
                
                g2.setColor(Color.green);
                g2.draw(new Line2D.Double(probDecX, probDecY, theModel.getTranDrawX2(), theModel.getTranDrawY2()));
            }
        }
        
        //Drawing of extra branches
        if(theModel.isDrawingExtraBranches())
        {
            if(theModel.getTempProbFrom() != null) //if we have a from state
            {
                g2.setColor(Color.gray);
                double fromX = theModel.getTempProbFrom().getX()+3;
                double fromY = theModel.getTempProbFrom().getY()+3;
                double probDecX  = theModel.getTempProbFrom().getX()+3;;
                double probDecY = theModel.getTempProbFrom().getY()+3;;
                //if(!theModel.isTempProbNailDown()) //if there is no nail down
                /*{
                    double mix = fromX;
                    double miy = fromY;
                    double max = fromX;
                    double may = fromY;
                    for(int i = 0; i < theModel.getTempProbTo().size(); i++)
                    {
                        State to = (State)(theModel.getTempProbTo().get(i));
                        mix = Math.min(to.getX()+15, mix);
                        max = Math.max(to.getX()+15, max);
                        miy = Math.min(to.getY()+15, miy);
                        may = Math.max(to.getY()+15, may);
                    }
                    mix = Math.min(theModel.getTranDrawX2(), mix);
                    max = Math.max(theModel.getTranDrawX2(), max);
                    miy = Math.min(theModel.getTranDrawY2(), miy);
                    may = Math.max(theModel.getTranDrawY2(), may);
                    probDecX = (mix+max)/2;
                    probDecY = (miy+may)/2;
                }*/
                /*else
                {
                    //double fromX = theModel.getTempProbFrom().getX()+15;
                    //double fromY = theModel.getTempProbFrom().getY()+15;
                    for(int i = 0; i < theModel.getNumTempProbNails(); i++)
                    {
                        double toX = theModel.getTempProbNail(i).getX();
                        double toY = theModel.getTempProbNail(i).getY();
                        g2.draw(new Line2D.Double(fromX, fromY, toX, toY));
                        fromX = toX;
                        fromY = toY;
                    }
                    probDecX = theModel.getLastProbNail().getX();
                    probDecY = theModel.getLastProbNail().getY();
                }*/
                /*if(theModel.getTempProbTo().size() == 0) g2.setColor(Color.green);
                g2.draw(new Line2D.Double(fromX, fromY, probDecX, probDecY));*/
                
                for(int i = 0; i < theModel.getTempProbTo().size();i++)
                {
                    State toState = (State)(theModel.getTempProbTo().get(i));
                    
                    int counter = 0;
                    ////System.out.println("i =   "+i);
                    for(int j = 0; j < theModel.getTempProbTo().size(); j++)
                    {
                        State otherState = (State)(theModel.getTempProbTo().get(j));
                        if(otherState == toState)
                        {
                            counter++;
                        }
                    }
                    ////System.out.println("counter   = " + counter);
                    if(counter == 1)
                    {
                        double probToX =toState.getX()+15;
                        double probToY = toState.getY()+15;
                        if(!(theModel.getTempProbFrom() == toState))
                            g2.draw(new Line2D.Double(probDecX, probDecY, probToX, probToY));
                        else
                        {
                            g2.draw(new Line2D.Double(probDecX,  probDecY,  ((probDecX+probToX)/2)+30,  ((probDecY+probToY)/2)+30));
                            g2.draw(new Line2D.Double(((probDecX+probToX)/2)+30,  ((probDecY+probToY)/2)+30, probToX, probToY));
                        }
                    }
                    else
                    {
                        for(int j = 0; j < counter; j++)
                        {
                            double offset = (-(((counter-1)*30)/2))+(j*30);
                            double x1 = probDecX;
                            double y1 = probDecY;
                            double x2 = toState.getX()+15;
                            double y2 = toState.getY()+15;
                            
                            double nailx = ((x1+x2)/2)+((Math.sin((Math.PI/2.0)-(Math.atan(((x1-x2)/2)/((y1-y2)/2)))))*((offset)));
                            double naily = ((y1+y2)/2)-((Math.cos((Math.PI/2.0)-(Math.atan(((x1-x2)/2)/((y1-y2)/2)))))*((offset)));
                            
                            g2.draw(new Line2D.Double(probDecX, probDecY, nailx, naily));
                            g2.draw(new Line2D.Double(nailx, naily, x2, y2));
                            
                        }
                    }
                }
                
                g2.setColor(Color.green);
                g2.draw(new Line2D.Double(probDecX, probDecY, theModel.getTranDrawX2(), theModel.getTranDrawY2()));
            }
        }
        
        //Draw Selection
        if(theModel.isItSelecting())
        {
            //////System.out.println("Drawing box");
            float[] dashPattern =
            { 6, 2 };
            g2.setStroke(new BasicStroke(1.0F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0F, dashPattern, 0));
            g2.setColor(selectionColour);
            
            double selX;
            double selY;
            double selWidth;
            double selHeight;
            double modSelStartX = theModel.getSelStartX();
            double modSelStartY = theModel.getSelStartY();
            double modSelEndX = theModel.getSelEndX();
            double modSelEndY = theModel.getSelEndY();
            if(modSelStartX < modSelEndX)
            {
                selX = modSelStartX;
                selWidth = modSelEndX - modSelStartX;
            }
            else
            {
                selX = modSelEndX;
                selWidth = modSelStartX - modSelEndX;
            }
            if(modSelStartY < modSelEndY)
            {
                selY = modSelStartY;
                selHeight = modSelEndY - modSelStartY;
            }
            else
            {
                selY = modSelEndY;
                selHeight = modSelStartY - modSelEndY;
            }
            float alpha = 1/16.0f;
            g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, alpha));
            g2.fill(new Rectangle2D.Double(selX, selY, selWidth, selHeight));
            alpha = 1;
            g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, alpha));
            g2.draw(new Rectangle2D.Double(selX, selY, selWidth, selHeight));
            g2.setStroke(new BasicStroke());
        }
        
        //Draw Zoom Selection
        if(theModel.isZoomSelecting())
        {
            //////System.out.println("Drawing box");
            float[] dashPattern =
            { 6, 2 };
            g2.setStroke(new BasicStroke(1.0F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0F, dashPattern, 0));
            g2.setColor(zoomColour);
            
            double selX;
            double selY;
            double selWidth;
            double selHeight;
            double modSelStartX = theModel.getZoomStartX();
            double modSelStartY = theModel.getZoomStartY();
            double modSelEndX = theModel.getZoomEndX();
            double modSelEndY = theModel.getZoomEndY();
            if(modSelStartX < modSelEndX)
            {
                selX = modSelStartX;
                selWidth = modSelEndX - modSelStartX;
            }
            else
            {
                selX = modSelEndX;
                selWidth = modSelStartX - modSelEndX;
            }
            if(modSelStartY < modSelEndY)
            {
                selY = modSelStartY;
                selHeight = modSelEndY - modSelStartY;
            }
            else
            {
                selY = modSelEndY;
                selHeight = modSelStartY - modSelEndY;
            }
            float alpha = 1/16.0f;
            g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, alpha));
            g2.fill(new Rectangle2D.Double(selX, selY, selWidth, selHeight));
            alpha = 1;
            g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, alpha));
            g2.draw(new Rectangle2D.Double(selX, selY, selWidth, selHeight));
            g2.setStroke(new BasicStroke());
        }
        
        /*if(clipBounds.width < getWidth())
        {
        g2.setColor(new Color((int)(Math.random()*255.0), (int)(Math.random()*255.0), (int)(Math.random()*255.0)));
        float alpha = 1/16.0f;
            g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, alpha));
        g2.fill(clipBounds);
        }*/
        /*
        g2.setColor(Color.red);
        if(theModel.getLastMouseBox() != null)
            g2.draw(theModel.getLastMouseBox());*/
        
    }
    
    //Listener methods
    
    /** Listener method called when the mouse is dragged.  It calls the appropriate
     * response method in the model.
     */
    public void mouseDragged(java.awt.event.MouseEvent mouseEvent)
    {
        if(theModel == null) return;
        int fixX = mouseEvent.getX();
        int fixY = mouseEvent.getY();
        
        if(fixX < 0) fixX = 0;
        if(fixY < 0) fixY = 0;
        if(fixX > getWidth()) fixX = getWidth();
        if(fixY > getHeight()) fixY = getHeight();
        
        
        if(theModel.isItSelecting())
        {
            theModel.setSelEndX(fixX);
            theModel.setSelEndY(fixY);
        }
        else if(theModel.isZoomSelecting())
        {
            theModel.setZoomEndX(fixX);
            theModel.setZoomEndY(fixY);
        }
        else if(theModel.isItMoving())
        {
            theModel.setMoveX(fixX);
            theModel.setMoveY(fixY);
        }
        else if(theModel.isDrawingTrans())
        {
            theModel.setTranDrawX2(fixX);
            theModel.setTranDrawY2(fixY);
        }
        else if(theModel.isDrawingExtraBranches())
        {
            theModel.setTranDrawX2(fixX);
            theModel.setTranDrawY2(fixY);
        }
        ////System.out.println("START DRAG PROCESSING");
        theModel.processDrag();
        ////System.out.println("END DRAG PROCESSING");
        setAppropriateCursor();
    }
    
    /** Listener method called when the mouse is moved. It calls the appropriate
     * response method in the model.
     */
    public void mouseMoved(java.awt.event.MouseEvent mouseEvent)
    {
        if(theModel == null) return;
        int fixX = mouseEvent.getX();
        int fixY = mouseEvent.getY();
        
        if(fixX < 0) fixX = 0;
        if(fixY < 0) fixY = 0;
        if(fixX > getWidth()) fixX = getWidth();
        if(fixY > getHeight()) fixY = getHeight();
        theModel.setTranDrawX2(fixX);
        theModel.setTranDrawY2(fixY);
        if(theModel.isDrawingTrans() || theModel.isDrawingProbTrans() || theModel.isZoomSelecting() || theModel.isDrawingExtraBranches())
        {
            theModel.processDrag();
        }
        
        theModel.processMouseMoved(mouseEvent.getX(), mouseEvent.getY());
       
        
        setAppropriateCursor();
        
    }
    
    /** Called when the mouse is clicked. It calls the appropriate
     * response method in the model - be it a single or double click, or with the Right
     * mouse button.
     */
    public void mouseClicked(MouseEvent e)
    {
        ////System.out.println("mouseClicked");
        requestFocusInWindow();
        if(theModel == null) return;
        if(e.isPopupTrigger())
        {
            theModel.setRightClick();
            theModel.processRightClick(e.getX(), e.getY(),e);
        }
        else if(e.getButton() == MouseEvent.BUTTON1)
        {
            if(e.getClickCount() >=2) //if double clicked
            {
                theModel.processDoubleClick(e.getX(), e.getY());
            }
            else
            {
                theModel.processSingleClick(e.getX(), e.getY());
            }
        }
        //setAppropriateCursor();
    }
    
    /** Only needed to implement the MouseMotionListener interface. */
    public void mouseEntered(MouseEvent e)
    {
        setAppropriateCursor();
    }
    
    /** Only needed to implement the MouseMotionListener interface. */
    public void mouseExited(MouseEvent e)
    {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
    
    /** Listener method called when a mousePressed event is created.  It calls the appropriate
     * response method in the model.
     */
    public void mousePressed(MouseEvent e)
    {
        requestFocusInWindow();
        if(theModel == null) return;
        
        if(e.isPopupTrigger())
        {
            theModel.setRightClick();
            theModel.processRightClick(e.getX(),    e.getY(),e);
        }
        else if(e.getButton() == MouseEvent.BUTTON1)
            theModel.processMouseDown(e.getX(), e.getY());
       // setAppropriateCursor();
    }
    
    /** Listener method called when a mouseReleased event is created.  It calls the appropriate
     * response method in the model.
     */
    public void mouseReleased(MouseEvent e)
    {
        requestFocusInWindow();
        if(theModel == null) return;
        if(e.isPopupTrigger())
        {theModel.processRightClick(e.getX(),   e.getY(),e);}
        else if(e.getButton() == MouseEvent.BUTTON1)
            theModel.processMouseUp(e.getX(), e.getY());
        setAppropriateCursor();
    }
    
    public Dimension getPreferredScrollableViewportSize()
    {
        return new Dimension(20,20);
    }
    
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        return 20;
    }
    
    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }
    
    public boolean getScrollableTracksViewportWidth()
    {
        return false;
    }
    
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        return 40;
    }
    
    public void keyPressed(KeyEvent e)
    {
        ////System.out.println("keypressed");
        if(e.getKeyCode() == KeyEvent.VK_CONTROL)
        {
            theModel.processControlDown();
        }
        else if(e.getKeyCode() == KeyEvent.VK_DELETE)
        {
            theModel.deleteSelected();
        }
    }
    
    public void keyReleased(KeyEvent e)
    {
        ////System.out.println("keyreleased");
        if(e.getKeyCode() == KeyEvent.VK_CONTROL)
        {
            theModel.processControlUp();
        }
    }
    
    public void keyTyped(KeyEvent e)
    {
    }
    
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        requestFocusInWindow();
        if(theModel == null) return;
        //System.out.println("Scroll amount = "+e.getScrollAmount());
        if(e.getWheelRotation() > 0) //scroll down
        {
            theModel.processScrollWheel(e.getX(), e.getY(), false);
        }
        else if(e.getWheelRotation()< 0) //scroll up
        {
            theModel.processScrollWheel(e.getX(), e.getY(), true);
        }
    }
    
    public void focusGained(FocusEvent e)
    {
        //System.out.println("Focus Gained");
    }
    
    
    public void focusLost(FocusEvent e)
    {
        theModel.processControlUp();
        //System.out.println("Focus Lost");
    }
    
    
    public void setAppropriateCursor()
    {
        if(theModel.getMode() == ModuleModel.EDIT)
        {
            if(theModel.isItSelecting())
            {
                ////System.out.println("setting cross");
                setCursor(cross);
            }
            else if(theModel.isItMoving())
            {
                ////System.out.println("setting move");
                setCursor(new Cursor(Cursor.MOVE_CURSOR));
            }
            else if(theModel.isOverMovable())
            {
                setCursor(mouseAndMove);
            }
            else if(theModel.isOverTransition())
            {
                setCursor(mouseAndTransition);
            }
            else
            {
                ////System.out.println("setting normal");
                setCursor(mouse);
            }
        }
        else if(theModel.getMode() == ModuleModel.ZOOM)
        {
            if(theModel.isZoomSelecting())
            {
                setCursor(crossZoom);
            }
            else
            {
                setCursor(zoom);
            }
        }
        else
        {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }
    
    
}
