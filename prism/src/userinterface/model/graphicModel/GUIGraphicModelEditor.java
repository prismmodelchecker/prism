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


import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;

import prism.*;
import parser.type.*;
import userinterface.util.*;
import userinterface.model.*;
import userinterface.model.computation.*;

public class GUIGraphicModelEditor extends GUIModelEditor implements SelectionListener
{
    public static final JLabel NO_MODULES = new JLabel("Empty Graphic Model", JLabel.CENTER);
    
    private GUIMultiModelHandler handler;
    private GUITextModelEditor textEditor;
    private GUIMultiModelTree tree;
    private PropertyTableModel properties;
    
    private boolean busy = false;
    
    private ModulesPanel modPanel;
    
    private JSplitPane splitter;
    //private ModuleDrawingPane drawingPane;
    //private ModuleModel current = null;
    //private JLabel moduleLabel;
    
    //model editor attributes
    //private ArrayList modules;
    //private ModuleDrawingPane renderer;
    
    /** Creates a new instance of GUIGraphicModelEditor */
    public GUIGraphicModelEditor(GUIMultiModelHandler handler, GUIMultiModelTree tree, PropertyTableModel properties)
    {
        ////System.out.println("Constructing GUIGraphicModelEditor");
        this.handler = handler;
        this.properties = properties;
        initComponents();
        this.tree = tree;
        //modules = new ArrayList();
        tree.setGraphicModelEditor(this);
    }
    
    public String getParseText()
    {
        String str =  tree.getParseText() + "\n" + textEditor.getParseText();
        ////System.out.println("str = \n\n"+str);
        return str;
    }
    
    public void newModel()
    {
        ////System.out.println("newModel called");
        //moduleLabel.setText("");
        tree.setGraphicModelEditor(this);
        tree.newTree(true);
        //drawingPane.setModel(null);
        textEditor.newModel();
        
        properties.setOwners(new ArrayList());
        //modules = new ArrayList();
        modPanel.newModel();
    }
    
    public String toString()
    {
        return "GUIGRAPHICMODELEDITOR IS ME";
    }
    
    public void undo()
    {        
               
    }
    
    public void redo()
    {
    	
    }
    
    public void copy()
    {
        
        modPanel.getModuleModel(modPanel.getSelectedIndex()).copy();
        
    }
    
    public void cut()
    {
        modPanel.getModuleModel(modPanel.getSelectedIndex()).cut();
    }
    
    public void paste()
    {
        modPanel.getModuleModel(modPanel.getSelectedIndex()).paste();
    }
    
    public void delete()
    {
        modPanel.getModuleModel(modPanel.getSelectedIndex()).deleteSelected();
    }
    
    public void selectAll()
    {
    }
    
    public void load(File f) throws PrismException, IOException, FileNotFoundException
    {
    }
    
    public void save(File f) throws PrismException, IOException, FileNotFoundException
    {
    }
    
    public void setAutolayout(boolean val)
    {
        modPanel.setAutolayout(val);
    }
    
    public boolean isAutolayout()
    {
        return modPanel.isAutolayout();
    }
    
    public void initComponents()
    {
        ////System.out.println("initComponents() of GUIGraphicModelEditor");
        splitter = new JSplitPane();
        {
            textEditor = new GUITextModelEditor("", handler);
            
            JPanel editorPanel = new JPanel();
            editorPanel.setLayout(new BorderLayout());
            //JScrollPane drawingScroller = new JScrollPane();
            //drawingPane = new ModuleDrawingPane(null);
            //drawingPane.setLayout(new BorderLayout());
            //drawingPane.add(NO_MODULES);
            
            //moduleLabel = new JLabel("");
            
            //drawingScroller.setViewportView(drawingPane);
            //drawingScroller.setColumnHeaderView(moduleLabel);
            //drawingPane.doSize();
            modPanel = new ModulesPanel(handler.getGUIPlugin().getGUI(), this);
            
            editorPanel.add(modPanel, BorderLayout.CENTER);
            editorPanel.add(new GraphicToolBar(this), BorderLayout.NORTH);
            
            splitter.setTopComponent(editorPanel);
            splitter.setBottomComponent(textEditor);
            splitter.setOrientation(JSplitPane.VERTICAL_SPLIT);
            splitter.setOneTouchExpandable(true);
            //System.out.println("The size of the panel is "+modPanel.getHeight());
            splitter.setResizeWeight(1.0);
            splitter.setDividerSize(8);
            
        }
        setLayout(new BorderLayout());
        add(splitter);
        
        //splitter.setDividerLocation(0.5);
        //drawingPane.doSize();
    }
    
    public void initialSplitterPosition(int height)
    {
        //System.out.println("The size of the panel is "+height);
        int position = (int)(height*0.85);
        splitter.setDividerLocation(position);
    }
    
    public void requestNewModule()
    {
        tree.a_addModule();
    }
    
    public void addNewModule(GUIMultiModelTree.ModuleNode mn)
    {
        ModuleModel m = new ModuleModel(handler, mn);
        m.addSelectionListener(this);
        modPanel.addModule(m);
        mn.setModel(m);
        
    }
    
    public void switchModuleView(GUIMultiModelTree.ModuleNode mn)
    {
        ////System.out.println("switch actually called");
        ModuleModel selected = getModuleModel(mn);
        /*if(current != null)
        {
            selected.setMode(current.getMode());
            selected.setZoom(current.getZoom());
            selected.setSnap(current.isSnap());
            selected.setShowGrid(current.isShowGrid());
            selected.setShowLongLabels(current.isShowLongLabels());
        }
        current = selected;
        if(selected != null)
        {
            moduleLabel.setText(mn.getName());
            drawingPane.setModel(selected);
        }*/
    }
    
    public void notifyChangeTo(GUIMultiModelTree.ModuleNode n)
    {
        
        
        modPanel.notifyNewName(getModuleModel(n), n.getName());
    }
    
    public void removeModule(GUIMultiModelTree.ModuleNode mn)
    {
	//System.out.println("removing module");
        //modules.remove(mn);
        //if(drawingPane.getModel() == getModuleModel(mn)) drawingPane.setModel(null);
        modPanel.removeModule(getModuleModel(mn));
    }
    
    public ModuleModel getModuleModel(GUIMultiModelTree.ModuleNode mn)
    {
        for(int i = 0; i < getNumModules(); i++)
        {
            ModuleModel mm = getModuleModel(i);
            if(mm.getCorrespondingModuleNode() == mn)
                return mm;
        }
        return null;
    }
    
    public ModuleModel getModuleModel(int i)
    {
        return modPanel.getModuleModel(i);
    }
    
    public int getNumModules()
    {
        return modPanel.getNumModules();
    }
    
    public void nextModule()
    {
        modPanel.next();
    }
    
    public void lastModule()
    {
        modPanel.previous();
    }
    
    //Updates
    
    public void loadAllExceptModules(String name, String type, ArrayList globals, ArrayList constants, String systemInformation) throws PrismException
    {
        //System.out.println("loadAllExceptModules called "+type);
        
        textEditor.setText(systemInformation);
        if(type.equals("mdp"))
            tree.a_setModelType(ModelType.MDP);
        else if(type.equals("dtmc"))
            tree.a_setModelType(ModelType.DTMC);
        else if(type.equals("ctmc"))
            tree.a_setModelType(ModelType.CTMC);
        
        for(int i = 0; i < constants.size(); i++)
        {
            LoadGraphicModelThread.Constant cons = (LoadGraphicModelThread.Constant)constants.get(i);
            if(cons instanceof LoadGraphicModelThread.BooleanConstant)
                tree.a_addBooleanConstant(cons.name, cons.value);
            else if(cons instanceof LoadGraphicModelThread.IntegerConstant)
                tree.a_addIntegerConstant(cons.name, cons.value);
            else if(cons instanceof LoadGraphicModelThread.DoubleConstant)
                tree.a_addDoubleConstant(cons.name, cons.value);
        
        }
        
        for(int i = 0; i < globals.size(); i++)
        {
            LoadGraphicModelThread.Variable vars = (LoadGraphicModelThread.Variable)globals.get(i);
            if(vars instanceof LoadGraphicModelThread.IntegerVariable)
                tree.a_addIntegerGlobal(vars.name, ((LoadGraphicModelThread.IntegerVariable)vars).min, ((LoadGraphicModelThread.IntegerVariable)vars).max, ((LoadGraphicModelThread.IntegerVariable)vars).init);
            else if(vars instanceof LoadGraphicModelThread.BooleanVariable)
                tree.a_addBooleanGlobal(vars.name, ((LoadGraphicModelThread.BooleanVariable)vars).init);
        }
        
        
        
    }
    
    public GUIMultiModelTree.ModuleNode requestNewModule(String name)
    {
        return tree.a_requestNewModule(name);
    }
    
    public void addBooleanVariable(GUIMultiModelTree.ModuleNode node, LoadGraphicModelThread.BooleanVariable var) throws PrismException
    {
        tree.a_addLocalBoolean(node, var);
    }
    
    public void addIntegerVariable(GUIMultiModelTree.ModuleNode node, LoadGraphicModelThread.IntegerVariable var) throws PrismException
    {
        tree.a_addLocalInteger(node, var);
    }
    
    public void setModified()
    {
        if(!isBusy())handler.hasModified(true);
    }
    
    public void zoomIn()
    {
        for(int i = 0; i < getNumModules(); i++)
            getModuleModel(i).setZoom(getModuleModel(i).getZoom()*1.1);
    }
    
    public void zoomOut()
    {
        for(int i = 0; i < getNumModules(); i++)
            getModuleModel(i).setZoom(getModuleModel(i).getZoom()/1.1);
    }
    
    public void showGrid(boolean b)
    {
        for(int i = 0; i < getNumModules(); i++)
            getModuleModel(i).setShowGrid(b);
    }
    
    public void snapToGrid(boolean b)
    {
        for(int i = 0; i < getNumModules(); i++)
            getModuleModel(i).setSnap(b);
    }
    
    public void showLongLabels(boolean b)
    {
        for(int i = 0; i < getNumModules(); i++)
            getModuleModel(i).setShowLongLabels(b);
    }
    
    public void setMode(int mode)
    {
        for(int i = 0; i < getNumModules(); i++)
            getModuleModel(i).setMode(mode);
    }
    
    //Access
    
    
    public ModelType getModelType()
    {
        return tree.getModelType();
    }
    
    public ArrayList getEditableConstantNames()
    {
        return tree.getEditableConstantNames();
    }
    
    public ArrayList getEditableConstantValues()
    {
        return tree.getEditableConstantValues();
    }
    
    public ArrayList<Type> getEditableConstantTypes()
    {
        return tree.getEditableConstantTypes();
    }
    
    public ArrayList getEditableGlobalNames()
    {
        return tree.getEditableGlobalNames();
    }
    
    public ArrayList getEditableGlobalMins()
    {
        return tree.getEditableGlobalMins();
    }
    
    public ArrayList getEditableGlobalMaxs()
    {
        return tree.getEditableGlobalMaxs();
    }
    
    public ArrayList getEditableGlobalInits()
    {
        return tree.getEditableGlobalInits();
    }
    
    public ArrayList<Type> getEditableGlobalTypes()
    {
        return tree.getEditableGlobalTypes();
    }
    
    public String getTextEditorString()
    {
        return textEditor.getTextString();
    }
    
    public ModuleModel[] getModuleArray()
    {
        ModuleModel[] mms = new ModuleModel[getNumModules()];
        for(int i = 0; i < mms.length; i++)
            mms[i] = getModuleModel(i);
        return mms;
    }
    
    public ArrayList getVariableNames(ModuleModel m)
    {
        return tree.getVariableNames(m.getCorrespondingModuleNode());
    }
    
    public ArrayList<Type> getVariableTypes(ModuleModel m)
    {
        return tree.getVariableTypes(m.getCorrespondingModuleNode());
    }
    
    public ArrayList getVariableMins(ModuleModel m)
    {
        return tree.getVariableMins(m.getCorrespondingModuleNode());
    }
    
    public ArrayList getVariableMaxs(ModuleModel m)
    {
        return tree.getVariableMaxs(m.getCorrespondingModuleNode());
    }
    
    public ArrayList getVariableInits(ModuleModel m)
    {
        return tree.getVariableInits(m.getCorrespondingModuleNode());
    }
    
    public void selectionPerformed(SelectionEvent e)
    {
        ////System.out.println("SelectionPerformed detected where it should be");
        properties.setOwners(e.getSelectedItems());
        
    }    
    
    /**
     * Getter for property busy.
     * @return Value of property busy.
     */
    public boolean isBusy()
    {
        return busy;
    }    
    
    /**
     * Setter for property busy.
     * @param busy New value of property busy.
     */
    public void setBusy(boolean busy)
    {
        this.busy = busy;
        handler.setBusy(busy);
    } 
    
   
}
