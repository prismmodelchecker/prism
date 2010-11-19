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
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import userinterface.*;
import java.beans.*;

public class ModulesPanel extends JPanel implements MouseListener, ComponentListener
{
    private ArrayList containers;
    private ArrayList showChecks;
    private ArrayList dockChecks;
    private GUIPrism gui;
    private GUIGraphicModelEditor gme;
    
    private JDesktopPane dp;
    private JScrollPane scr;
    
    private JTabbedPane tp;
    
    private boolean autolayout;
    
    private JPopupMenu popup;
    private JMenu show, dock, layout;
    
    private JCheckBoxMenuItem autol;
    
    private Action tile, cascade, addModule;
    
    
    
    /** Creates a new instance of ModulesPanel */
    public ModulesPanel(GUIPrism gui, GUIGraphicModelEditor gme)
    {
        containers = new ArrayList();
        showChecks = new ArrayList();
        dockChecks = new ArrayList();
        this.gui = gui;
        this.gme = gme;
        
        autolayout = true;
        this.addComponentListener(this);
        
        initComponents();
        
        //tp = new JTabbedPane();
    }
    
    public void newModel()
    {
        dp.removeAll();
        
        containers = new ArrayList();
        showChecks = new ArrayList();
        dockChecks = new ArrayList();
        
        autolayout = true;
        
        repaint();
    }
    
    public void addModule(ModuleModel m)
    {
        ModuleDrawingPane dp = new ModuleDrawingPane(m);
        m.addObserver(dp);
        
        GraphicModuleContainer gmc = new GraphicModuleContainer(gui, m, dp, this);
        
        containers.add(gmc);
        
	
        
        JCheckBoxMenuItem mod = new JCheckBoxMenuItem(gmc.getName());
        mod.addActionListener(new ActionListener()
        {
            
            public void actionPerformed(ActionEvent e)
            {
                JCheckBoxMenuItem jcbmi = (JCheckBoxMenuItem)e.getSource();
                if(jcbmi.isSelected())
                {
                    a_show(getModuleContainer(jcbmi.getText()));
                }
                else
                {
                    a_hide(getModuleContainer(jcbmi.getText()));
                }
            }
        }
        );
        mod.setSelected(true);
        showChecks.add(mod);
        
        JCheckBoxMenuItem docker = new JCheckBoxMenuItem(gmc.getName());
        docker.addActionListener(new ActionListener()
        {
            
            public void actionPerformed(ActionEvent e)
            {
                JCheckBoxMenuItem jcbmi = (JCheckBoxMenuItem)e.getSource();
                if(jcbmi.isSelected())
                {
                    a_dock(getModuleContainer(jcbmi.getText()));
                }
                else
                {
                    a_undock(getModuleContainer(jcbmi.getText()));
                }
            }
        }
        );
        docker.setSelected(true);
        dockChecks.add(docker);
        
        show.add(mod, containers.size()-1);
        dock.add(docker, containers.size()-1);
        
        autoLayout();
    }
    
    public void removeModule(ModuleModel m)
    {
        GraphicModuleContainer gmc = getModuleContainer(m);
	JCheckBoxMenuItem dockItem = getModuleDockCheck(m);
	JCheckBoxMenuItem showItem = getModuleShowCheck(m);
	
	gmc.getInternalFrame().setVisible(false);
	
	containers.remove(gmc);
	dockChecks.remove(dockItem);
	showChecks.remove(showItem);
	show.remove(showItem);
	dock.remove(dockItem);
	
    }
    
    public ModuleModel getModuleModel(int i)
    {
        return ((GraphicModuleContainer)containers.get(i)).getModuleModel();
    }
    
    public GraphicModuleContainer getModuleContainer(String name)
    {
        GraphicModuleContainer gmc = null;
        
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer curr = (GraphicModuleContainer)containers.get(i);
            if(curr.getName().equals(name))
            {
                gmc = curr;
                break;
            }
        }
        return gmc;
    }
    
    public GraphicModuleContainer getModuleContainer(ModuleModel mm)
    {
        GraphicModuleContainer gmc = null;
        
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer curr = (GraphicModuleContainer)containers.get(i);
            if(curr.getModuleModel() == mm)
            {
                gmc = curr;
                break;
            }
        }
        return gmc;
    }
    
    public JCheckBoxMenuItem getModuleDockCheck(ModuleModel mm)
    {
        JCheckBoxMenuItem dock = null;
        
        for(int i = 0; i < dockChecks.size(); i++)
        {
            JCheckBoxMenuItem curr = (JCheckBoxMenuItem)dockChecks.get(i);
            if(curr.getText().equals(mm.getModuleName()))
            {
                dock = curr;
                break;
            }
        }
        return dock;
    }
    
    public JCheckBoxMenuItem getModuleShowCheck(ModuleModel mm)
    {
        JCheckBoxMenuItem show = null;
        
        for(int i = 0; i < showChecks.size(); i++)
        {
            JCheckBoxMenuItem curr = (JCheckBoxMenuItem)showChecks.get(i);
            if(curr.getText().equals(mm.getModuleName()))
            {
                show = curr;
                break;
            }
        }
        return show;
    }
    
    public GraphicModuleContainer getModuleContainer(int index)
    {
        return (GraphicModuleContainer)containers.get(index);
    }
    
    public int getNumModules()
    {
        return containers.size();
    }
    
    public void next()
    {
        //System.out.println("next called");
        JInternalFrame fr = dp.getSelectedFrame();
        JInternalFrame next = null;
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer cont = (GraphicModuleContainer)containers.get(i);
            if(cont.isDocked())
            {
                if(cont.getInternalFrame() == fr)
                {
                    //now find the next
                    i++;
                    if(i == containers.size()) i = 0;
                    while(true)
                    {
                        cont = (GraphicModuleContainer)containers.get(i);
                       if(cont.isDocked())
                       {
                           next = cont.getInternalFrame();
                           try
                           {
                                next.setSelected(true);
                           }
                           catch(PropertyVetoException e)
                           {}
                           //System.out.println("setting next");
                           return;
                       }
                       i++;
                        if(i == containers.size()) i = 0;
                    }
                }
            }
        }
    }
    
    public void previous()
    {
        //System.out.println("backwards called");
        JInternalFrame fr = dp.getSelectedFrame();
        JInternalFrame next = null;
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer cont = (GraphicModuleContainer)containers.get(i);
            if(cont.isDocked())
            {
                if(cont.getInternalFrame() == fr)
                {
                    //now find the previous
                    i--;
                    if(i == -1) i = containers.size()-1;
                    while(true)
                    {
                        cont = (GraphicModuleContainer)containers.get(i);
                       if(cont.isDocked())
                       {
                           next = cont.getInternalFrame();
                           try
                           {
                           next.setSelected(true);
                           }
                           catch(PropertyVetoException e)
                           {}
                           return;
                       }
                       i--;
                        if(i == -1) i = containers.size()-1;
                    }
                }
            }
        }
    }
    
    public void dock(JInternalFrame jif)
    {
        dp.add(jif);
        dp.repaint();
    }
    
    public void undock(JInternalFrame jif)
    {
        dp.remove(jif);
        dp.repaint();
        revalidate();
    }
    
    public void initComponents()
    {
        scr = new JScrollPane();
        ////System.out.println("doing initcompontnet");
        dp = new JDesktopPane();
        dp.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        //dp.addMouseListener(this);
        scr.addMouseListener(this);
        dp.setBackground(new Color(200,200,255));
        
        setLayout(new BorderLayout());
        scr.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scr.setViewportView(dp);
        
        add(scr, BorderLayout.CENTER);
        
        popup = new JPopupMenu();
        {
            
            addModule = new AbstractAction()
            {
                public void actionPerformed(ActionEvent e)
                {
                    a_addModule();
                }
            };
            addModule.putValue(Action.LONG_DESCRIPTION,  "Adds a new module to the model");
            addModule.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
            addModule.putValue(Action.NAME, "Add Module");
            addModule.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallNewModule.png"));
            show = new JMenu("Show");
            {
                //no modules first
                show.add(new JSeparator());
                Action showAll = new AbstractAction()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        a_showAll();
                    }
                };
                showAll.putValue(Action.LONG_DESCRIPTION, "Shows all graphical modules");
                showAll.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
                showAll.putValue(Action.NAME, "All");
                show.add(showAll);
                
                Action hideAll = new AbstractAction()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        a_hideAll();
                    }
                };
                hideAll.putValue(Action.LONG_DESCRIPTION, "Hides all graphical modules");
                hideAll.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
                hideAll.putValue(Action.NAME, "None");
                show.add(hideAll);
            }
            show.setIcon(GUIPrism.getIconFromImage("smallView.png"));
            dock = new JMenu("Dock");
            {
                //no modules first
                dock.add(new JSeparator());
                Action dockAll = new AbstractAction()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        a_dockAll();
                    }
                };
                dockAll.putValue(Action.LONG_DESCRIPTION, "Docks all graphical modules");
                dockAll.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
                dockAll.putValue(Action.NAME, "All");
                dock.add(dockAll);
                
                Action undockAll = new AbstractAction()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        a_undockAll();
                    }
                };
                undockAll.putValue(Action.LONG_DESCRIPTION, "Undocks all graphical modules");
                undockAll.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
                undockAll.putValue(Action.NAME, "None");
                dock.add(undockAll);
            }
            dock.setIcon(GUIPrism.getIconFromImage("smallDocking.png"));
            layout = new JMenu("Layout");
            {
                tile = new AbstractAction()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        a_tile();
                    }
                };
                tile.putValue(Action.LONG_DESCRIPTION, "Tiles all docked graphical modules");
                tile.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
                tile.putValue(Action.NAME, "Tile");
                tile.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallTile.png"));
                layout.add(tile);
                
                cascade = new AbstractAction()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        a_cascade();
                    }
                };
                cascade.putValue(Action.LONG_DESCRIPTION, "Cascades all docked graphical modules");
                cascade.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
                cascade.putValue(Action.NAME, "Cascade");
                cascade.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCascade.png"));
                layout.add(cascade);
                
                layout.add(new JSeparator());
                autol = new JCheckBoxMenuItem("    Auto Layout");
                
                autol.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        JCheckBoxMenuItem source = (JCheckBoxMenuItem)e.getSource();
                        if(source.isSelected())
                        {
                            tile.setEnabled(false);
                            cascade.setEnabled(false);
                            autolayout = true;
                            
                            autoLayout();
                        }
                        else
                        {
                            tile.setEnabled(true);
                            cascade.setEnabled(true);
                            autolayout = false;
                        }
                    }
                });
                layout.add(autol);
                
                autolayout = true;
                tile.setEnabled(false);
                cascade.setEnabled(false);
                autol.setSelected(true);
                
            }
            layout.setIcon(GUIPrism.getIconFromImage("smallLayout.png"));
        }
        popup.add(addModule);
        popup.addSeparator();
        popup.add(show);
        popup.add(dock);
        popup.add(layout);
    }
    
    public void autoLayout()
    {
        if(autolayout)
        {
            a_tile();
        }
    }
    
    public void ensureFocusIsViewable()
    {
        ////System.out.println("ensurefocusisviewbalse");
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer gmc = (GraphicModuleContainer)containers.get(i);
            if(gmc.isSelectedModule())
            {
                if(gmc.isVisible()) return;
            }
            
        }
        
        //nothing valid is selected, select the first valid module
        
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer gmc = (GraphicModuleContainer)containers.get(i);
            if(gmc.isVisible())
            {
                gmc.setSelected();
                break;
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
        if(e.isPopupTrigger())
        {
            mousePopup(e);
        }
    }
    
    public void mouseReleased(MouseEvent e)
    {
        if(e.isPopupTrigger())
        {
            mousePopup(e);
        }
    }
    
    public void mousePopup(MouseEvent e)
    {
        //System.out.println("e.getComponent = "+e.getComponent().toString());
        popup.show(this, e.getX(), e.getY());
    }
    
    public void componentHidden(ComponentEvent e)
    {
        if(autolayout)
        {
            autoLayout();
        }
    }
    
    public void componentMoved(ComponentEvent e)
    {
        if(autolayout)
        {
            ////System.out.println("moved");
            if(e.getSource() != this) //see whether they have dropped one window on top of another
            {
                int index = -1;;
                //Find out which one is being moved
                for(int i = 0 ; i < containers.size(); i++)
                {
                    GraphicModuleContainer gme = (GraphicModuleContainer)containers.get(i);
                    if(!gme.isDocked()) continue;
                    if(gme.getInternalFrame() == e.getSource())
                    {
                        index = i;
                        break;
                    }
                }
                ////System.out.println("index = "+index);
                
                //Discover which frame the indexed frame is being dragged over
                int swapIndex = -1;
                
                Rectangle rect = e.getComponent().getBounds();
                for(int i = 0; i < containers.size(); i++)
                {
                    GraphicModuleContainer gme2 = (GraphicModuleContainer)containers.get(i);
                    if(!gme2.isDocked()) continue;
                    if(i != index)
                    {
                        if(rect.intersects(gme2.getInternalFrame().getBounds()))
                        {
                            swapIndex = i;
                            break;
                        }
                    }
                }
                
                if(index != swapIndex && index != -1 && swapIndex!= -1)
                {
                    //Swap the frames
                    Object temp = containers.get(index);
                    containers.set(index, containers.get(swapIndex));
                    containers.set(swapIndex, temp);
                }
            }
            autoLayout();
        }
    }
    
    public void componentResized(ComponentEvent e)
    {
        
        if(autolayout)
        {
            autoLayout();
            //maximised components resize appropriately
            if(e.getComponent() == this)
            {
                ////System.out.println("frame resize");
                dp.setPreferredSize(new Dimension(scr.getViewport().getWidth(), scr.getViewport().getHeight()));
                for(int i = 0; i < containers.size(); i++)
                {
                    GraphicModuleContainer gme = (GraphicModuleContainer)containers.get(i);
                    if(!gme.isDocked()) continue;
                    if(gme.getInternalFrame().isMaximum())
                    {
                        gme.getInternalFrame().setPreferredSize(new Dimension(dp.getWidth(), dp.getHeight()));
                    }
                }
            }
            else
            {
                for(int i = 0; i < containers.size(); i++)
                {
                    GraphicModuleContainer gme = (GraphicModuleContainer)containers.get(i);
                    if(!gme.isDocked()) continue;
                    if(gme.getInternalFrame().isMaximum())
                    {
                        dp.setPreferredSize(new Dimension(scr.getViewport().getWidth(), scr.getViewport().getHeight()));
                        gme.getInternalFrame().setPreferredSize(new Dimension(dp.getWidth(), dp.getHeight()));
                        if(gme.getInternalFrame() == e.getComponent()) a_maximiseAll();
                        break;
                    }
                    else
                    {
                        if(gme.getInternalFrame() == e.getComponent()) a_normalAll();
                    }
                }
            }
            autoLayout();
        }
    }
    
    public void componentShown(ComponentEvent e)
    {
        if(autolayout)
        {
            autoLayout();
        }
    }
    
    public void desktopLoseFocus()
    {
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer gmc = (GraphicModuleContainer)containers.get(i);
            if(gmc.isDocked())
            {
                try
                {
                    gmc.getInternalFrame().setSelected(false);
                }
                catch(PropertyVetoException e)
                {
                    ////System.out.println("vetoexception");
                }
            }
        }
    }
    
    public int getSelectedIndex()
    {
        int index = -1;
        
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer gmc = (GraphicModuleContainer)containers.get(i);
            if(gmc.isSelectedModule()) 
            {
                index = i;
                break;
            }
        }
        return  index;
    }
    
    public void notifyNewName(ModuleModel m, String name)
    {
        getModuleContainer(m).setTitle(name);
    }
    
    public void a_showAll()
    {
        ////System.out.println("show all");
        
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer gmc = (GraphicModuleContainer)containers.get(i);
            gmc.setVisible(true);
        }
        
        for(int i = 0; i < containers.size(); i++)
        {
            JCheckBoxMenuItem jcmi = (JCheckBoxMenuItem)showChecks.get(i);
            jcmi.setSelected(true);
        }
    }
    
    public void a_hideAll()
    {
        ////System.out.println("hide all");
        
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer gmc = (GraphicModuleContainer)containers.get(i);
            gmc.setVisible(false);
            
        }
        
        for(int i = 0; i < containers.size(); i++)
        {
            JCheckBoxMenuItem jcmi = (JCheckBoxMenuItem)showChecks.get(i);
            jcmi.setSelected(false);
        }
    }
    
    public void a_show(GraphicModuleContainer gmc)
    {
        ////System.out.println("show "+gmc.getName());
        gmc.setVisible(true);
    }
    
    public void a_hide(GraphicModuleContainer gmc)
    {
        gmc.setVisible(false);
        
        for(int i = 0; i < containers.size(); i++)
        {
            JCheckBoxMenuItem jcmi = (JCheckBoxMenuItem)showChecks.get(i);
            
            if(jcmi.getText().equals(gmc.getName()))
            {
                jcmi.setSelected(false);
            }
        }
        ensureFocusIsViewable();
    }
    
    public void a_dock(GraphicModuleContainer gmc)
    {
        gmc.setDocked(true);
        gmc.setVisible(true);
        
        for(int i = 0; i < containers.size(); i++)
        {
            JCheckBoxMenuItem jcmi = (JCheckBoxMenuItem)showChecks.get(i);
            
            if(jcmi.getText().equals(gmc.getName()))
            {
                jcmi.setSelected(true);
            }
        }
    }
    
    public void a_undock(GraphicModuleContainer gmc)
    {
        gmc.setDocked(false);
        gmc.setVisible(true);
    }
    
    public void a_dockAll()
    {
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer gmc = (GraphicModuleContainer)containers.get(i);
            gmc.setDocked(true);
            
        }
        
        for(int i = 0; i < containers.size(); i++)
        {
            JCheckBoxMenuItem jcmi = (JCheckBoxMenuItem)dockChecks.get(i);
            jcmi.setSelected(true);
        }
        
        a_showAll();
    }
    
    public void a_undockAll()
    {
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer gmc = (GraphicModuleContainer)containers.get(i);
            gmc.setDocked(false);
            
        }
        ////System.out.println("before for loop");
        for(int i = 0; i < dockChecks.size(); i++)
        {
            ////System.out.println("unchecking "+i);
            JCheckBoxMenuItem jcmi = (JCheckBoxMenuItem)dockChecks.get(i);
            jcmi.setSelected(false);
        }
        
        a_showAll();
    }
    
    public void a_tile()
    {
        for(int i = 0; i < containers.size(); i++)
        {
            
            GraphicModuleContainer gmc = (GraphicModuleContainer)containers.get(i);
            if(gmc.isDocked() && gmc.getInternalFrame().isMaximum())
            {
                return;
            }
        }
        
        ////System.out.println("doing minmi");
        int maxWidth = dp.getWidth();
        int border = 7;
        
        int currX = 0;
        int currY = 0;
        
        int maxHeightSoFar = 0;
        
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer gmc = (GraphicModuleContainer)containers.get(i);
            if(!gmc.isDocked() || !gmc.getInternalFrame().isVisible()) continue;
            if((currX + border + border + gmc.getInternalFrame().getWidth()) > dp.getWidth())
            {
                currX = 0;
                currY = currY + border + maxHeightSoFar;
                maxHeightSoFar = 0;
            }
            
            gmc.getInternalFrame().setLocation(currX+border,currY+border);
            currX+=(gmc.getInternalFrame().getWidth()+border);
            if(gmc.getInternalFrame().getHeight() > maxHeightSoFar) maxHeightSoFar = gmc.getInternalFrame().getHeight();
            
            
        }
        
        ////System.out.println("setting preferred size to: "+(currY+maxHeightSoFar+border+border));
        dp.setPreferredSize(new Dimension(getWidth(), currY+maxHeightSoFar+border+border));
        //p.getViewport().setPreferredSize(new Dimension(getContentPane().getWidth(), currY+maxHeightSoFar+border+border));
        revalidate();
    }
    
    public void a_cascade()
    {
        for(int i = 0; i < containers.size(); i++)
        {
            
            GraphicModuleContainer gmc = (GraphicModuleContainer)containers.get(i);
            if(gmc.isDocked() && gmc.getInternalFrame().isMaximum())
            {
                return;
            }
        }
        
        ////System.out.println("doing minmi");
        int maxWidth = dp.getWidth();
        int border = 21;
        
        int currX = 0;
        int currY = 0;
        
        int currBaseY = 0;
        int maxHeightSoFar = 0;
        
        int previousSelection = getSelectedIndex();
        
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer gmc = (GraphicModuleContainer)containers.get(i);
            if(!gmc.isDocked() || !gmc.getInternalFrame().isVisible()) continue;
            if((currX + border + border + gmc.getInternalFrame().getWidth()) > dp.getWidth())
            {
                currX = 0;
                currY = border + maxHeightSoFar;
                maxHeightSoFar = 0;
            }
            
            gmc.getInternalFrame().setLocation(currX+border,currY+border);
            currX+=border;
            currY+=border;
            if(currY + gmc.getInternalFrame().getHeight() > maxHeightSoFar) maxHeightSoFar = currY + gmc.getInternalFrame().getHeight();
            gmc.setSelected();
            
        }
        //reselect the original
        if(previousSelection > -1) getModuleContainer(previousSelection).setSelected();
        
        ////System.out.println("setting preferred size to: "+(currY+maxHeightSoFar+border+border));
        dp.setPreferredSize(new Dimension(getWidth(), currY+maxHeightSoFar+border+border));
        //p.getViewport().setPreferredSize(new Dimension(getContentPane().getWidth(), currY+maxHeightSoFar+border+border));
        revalidate();
    }
    
    public void a_maximiseAll()
    {
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer gmc = getModuleContainer(i);
            if(gmc.isDocked() && !gmc.getInternalFrame().isMaximum())
            {
                try
                {
                    gmc.getInternalFrame().setMaximum(true);
                }
                catch(PropertyVetoException e)
                {
                    
                }
            }
        }
    }
    
    public void a_normalAll()
    {
        for(int i = 0; i < containers.size(); i++)
        {
            GraphicModuleContainer gmc = getModuleContainer(i);
            if(gmc.isDocked() && gmc.getInternalFrame().isMaximum())
            {
                try
                {
                    gmc.getInternalFrame().setMaximum(false);
                }
                catch(PropertyVetoException e)
                {
                    
                }
            }
        }
    }
    
    public void a_addModule()
    {
        //System.out.println("ADD MODULE");
        gme.requestNewModule();
    }
    
    /** Getter for property autolayout.
     * @return Value of property autolayout.
     *
     */
    public boolean isAutolayout()
    {
        return autolayout;
    }
    
    /** Setter for property autolayout.
     * @param autolayout New value of property autolayout.
     *
     */
    public void setAutolayout(boolean autolayout)
    {
        this.autolayout = autolayout;
        
        autol.setSelected(autolayout);
        autoLayout();
    }
    
}
