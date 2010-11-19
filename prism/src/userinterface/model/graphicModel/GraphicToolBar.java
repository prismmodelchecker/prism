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

import java.awt.event.*;
import javax.swing.*;

import userinterface.GUIPrism;

public class GraphicToolBar extends JToolBar
{
    //CONSTANTS
    
    
    
    //The toolbar either belongs to a graphic model editor, or to an undocked
    //GraphicModuleContainer.
    
    private boolean isDocked; 
    private GUIGraphicModelEditor gme;
    private GraphicModuleContainer mmo;
    
    
    //Must keep track of the internal state of the toolbar in case we need
    //synchronisation
    
    private int mode = ModuleModel.EDIT;
    private boolean snap = true;
    private boolean showGrid = true;
    private boolean longLabels = false;
    
    
    
    /** Creates a new instance of GraphicToolBar if this toolbar is to 
     *  be the main toolbar for the graphic model editor.
     */
    public GraphicToolBar(GUIGraphicModelEditor gme)
    {
        super("PRISM Graphic Tools");
        isDocked = true;
        this.gme = gme;
        initComponents();
    }
    
    /** Create a new instance of GraphicToolBar if this toolbar is to 
     *  belong to an undocked GraphicModuleContainer
     */
    public GraphicToolBar(GraphicModuleContainer mmo)
    {
        super("PRISM Graphic Tools");
        isDocked = false;
        this.mmo = mmo;
        initComponents();
    }
    
    private void initComponents()
    {
        ButtonGroup modeGroup = new ButtonGroup();
        {
            JToggleButton editMode = new JToggleButton();
            editMode.setIcon(GUIPrism.getIconFromImage("smallEdit.png"));
            editMode.setToolTipText("Edit Mode");
            editMode.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if(((JToggleButton)e.getSource()).isSelected())
                    {
                        setMode(ModuleModel.EDIT);
                    }
                }
            });
            editMode.setSelected(true);
            
            JToggleButton zoomMode = new JToggleButton();
            zoomMode.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if(((JToggleButton)e.getSource()).isSelected())
                    {
                        setMode(ModuleModel.ZOOM);
                    }
                }
            });
            zoomMode.setIcon(GUIPrism.getIconFromImage("smallZoom.png"));
            zoomMode.setToolTipText("Zoom Mode");
            
            add(editMode);
            add(zoomMode);
            modeGroup.add(editMode);
            modeGroup.add(zoomMode);
        }
        
        addSeparator();
        
        JToggleButton snapToggle = new JToggleButton();
        snapToggle.setIcon(GUIPrism.getIconFromImage("gridSnapOff.png"));
	snapToggle.setSelected(true);
	snapToggle.setToolTipText("Snap To Grid");
	snapToggle.setSelectedIcon(GUIPrism.getIconFromImage("gridSnapOn.png"));
	snapToggle.addActionListener(new java.awt.event.ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		setSnap(((JToggleButton)e.getSource()).isSelected());    
	    }
	});
        snapToggle.setSelected(true);
        add(snapToggle);
        
        JToggleButton gridToggle = new JToggleButton();
        gridToggle.setIcon(GUIPrism.getIconFromImage("redGrid.png"));
	gridToggle.setSelected(true);
	gridToggle.setToolTipText("Grid On/Off");
	gridToggle.setDisabledSelectedIcon(GUIPrism.getIconFromImage("greenGrid.png"));
	gridToggle.setSelectedIcon(GUIPrism.getIconFromImage("greenGrid.png"));
	gridToggle.addActionListener(new java.awt.event.ActionListener()
	{
	   public void actionPerformed(ActionEvent e)
	    {
		setShowGrid(((JToggleButton)e.getSource()).isSelected());    
	    }
	});
        gridToggle.setSelected(true);
        
        add(gridToggle);
        
        JToggleButton longLabelsToggle = new JToggleButton();
        longLabelsToggle.setIcon(GUIPrism.getIconFromImage("normLabel.png"));
	longLabelsToggle.setToolTipText("Long Labels On/Off");
	longLabelsToggle.setSelectedIcon(GUIPrism.getIconFromImage("longLabel.png"));
	longLabelsToggle.addActionListener(new java.awt.event.ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		setLongLabels(((JToggleButton)e.getSource()).isSelected());
	    }
	});
        
        add(longLabelsToggle);
        
        addSeparator();
        
        
        
        JButton lastModule = new JButton();
        lastModule.setIcon(GUIPrism.getIconFromImage("smallBack.png"));
	lastModule.setToolTipText("Last Module");
	lastModule.addActionListener(new java.awt.event.ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		lastModule();
	    }
	});
        
        add(lastModule);
        
        JButton nextModule = new JButton();
        nextModule.setIcon(GUIPrism.getIconFromImage("smallForward.png"));
	nextModule.setToolTipText("Next Module");
	nextModule.addActionListener(new java.awt.event.ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		nextModule();
	    }
	});
        
        add(nextModule);
        
    }
    
    public void nextModule()
    {
        gme.nextModule(); 
    }
    
    public void lastModule()
    {
        gme.lastModule();
    }
    
    private void setMode(int mode)
    {
        if(mode == ModuleModel.EDIT || mode == ModuleModel.ZOOM)
        {
            if(isDocked) gme.setMode(mode);
            //else mmo.setMode(mode);
            this.mode = mode;
        }
    }
    
    /** Getter for property snap.
     * @return Value of property snap.
     *
     */
    public boolean isSnap()
    {
        return snap;
    }
    
    /** Setter for property snap.
     * @param snap New value of property snap.
     *
     */
    private void setSnap(boolean snap)
    {
        this.snap = snap;
        if(isDocked) gme.snapToGrid(snap);
        //else mmo.snapToGrid(snap);
    }
    
    /** Getter for property showGrid.
     * @return Value of property showGrid.
     *
     */
    public boolean isShowGrid()
    {
        return showGrid;
    }
    
    /** Setter for property showGrid.
     * @param showGrid New value of property showGrid.
     *
     */
    private void setShowGrid(boolean showGrid)
    {
        this.showGrid = showGrid;
        if(isDocked) gme.showGrid(showGrid);
        //else mmo.showGrid(showGrid);
    }
    
    /** Getter for property mode.
     * @return Value of property mode.
     *
     */
    public int getMode()
    {
        return mode;
    }    
   
    /** Getter for property longLabels.
     * @return Value of property longLabels.
     *
     */
    public boolean isLongLabels()
    {
        return longLabels;
    }    
    
    /** Setter for property longLabels.
     * @param longLabels New value of property longLabels.
     *
     */
    public void setLongLabels(boolean longLabels)
    {
        if(isDocked) gme.showLongLabels(longLabels);
        this.longLabels = longLabels;
    }
    
}
