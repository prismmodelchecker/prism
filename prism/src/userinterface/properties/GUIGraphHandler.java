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

package userinterface.properties;

import javax.swing.*;
import javax.print.*;
import javax.print.attribute.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import chart.*;
import userinterface.*;
import userinterface.util.*;
import settings.*;
/**
 *
 * @author  ug60axh
 */
 
public class GUIGraphHandler extends JPanel implements MouseListener
{
	private static int counter = 0;
	private JTabbedPane theTabs;
	
	private ArrayList models;
	private ArrayList graphs;
	private ArrayList options;
	
	private int currentSelect;
	
	private JPopupMenu graphMenu, backMenu;
	private JMenu exportMenu;
	private Action graphOptions, importXML, importXMLBack, exportPNG, exportJPEG, exportXML, exportMatlab, printGraph, deleteGraph;
	private JFrame parent;
	private GUIPlugin plug;
	
	private GUIPrismFileFilter imagesFilter[], xmlFilter[], matlabFilter[];
	private PrintRequestAttributeSet attributes;

	/** Creates a new instance of GUIGraphHandler */
	public GUIGraphHandler(JFrame parent, GUIPlugin plug)
	{
		this.parent = parent;
		this.plug = plug;
		initComponents();

		imagesFilter = new GUIPrismFileFilter[2];
		imagesFilter[0] = new GUIPrismFileFilter("PNG files (*.png)");
		imagesFilter[0].addExtension("png");
		imagesFilter[1] = new GUIPrismFileFilter("JPEG files (*.jpg, *.jpeg)");
		imagesFilter[1].addExtension("jpg");
		
		xmlFilter = new GUIPrismFileFilter[1];
		xmlFilter[0] = new GUIPrismFileFilter("PRISM graph files (*.gra)");
		xmlFilter[0].addExtension("gra");

		matlabFilter = new GUIPrismFileFilter[1];
		matlabFilter[0] = new GUIPrismFileFilter("Matlab files (*.m)");
		matlabFilter[0].addExtension("m");

		attributes = new HashPrintRequestAttributeSet();
		newHandler();
	}
	
	private void initComponents()
	{
		theTabs = new JTabbedPane();

		theTabs.addMouseListener(this);
		setLayout(new BorderLayout());
		add(theTabs, BorderLayout.CENTER);

		importXMLBack = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (plug.showOpenFileDialog(xmlFilter, xmlFilter[0]) != JFileChooser.APPROVE_OPTION) return;
				try
				{
					MultiGraphModel mgm = new MultiGraphModel().load(plug.getChooserFile());
					addGraph(mgm);
				}
				catch(Exception ex)
				{
					plug.error("Could not import PRISM graph file:\n"+ex.getMessage());
				}
			}
		};
		importXMLBack.putValue(Action.NAME, "Import PRISM graph");
		importXMLBack.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
		importXMLBack.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallImport.gif"));
		importXMLBack.putValue(Action.LONG_DESCRIPTION, "Imports a saved PRISM graph from a file.");
		
		graphOptions = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				MultiGraphOptions mgo = (MultiGraphOptions)options.get(currentSelect);
				mgo.show();
			}
		};
		graphOptions.putValue(Action.NAME, "Graph options");
		graphOptions.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_G));
		graphOptions.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallOptions.gif"));
		graphOptions.putValue(Action.LONG_DESCRIPTION, "Displays the options dialog for the graph.");

		importXML = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (plug.showOpenFileDialog(xmlFilter, xmlFilter[0]) != JFileChooser.APPROVE_OPTION) return;
				try
				{
					MultiGraphModel mgm = new MultiGraphModel().load(plug.getChooserFile());
					addGraph(mgm);
				}
				catch(Exception ex)
				{
					plug.error("Could not import PRISM graph file:\n"+ex.getMessage());
				}
			}
		};
		importXML.putValue(Action.NAME, "Import PRISM graph");
		importXML.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
		importXML.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallImport.gif"));
		importXML.putValue(Action.LONG_DESCRIPTION, "Imports a saved PRISM graph from a file.");

		exportXML = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (plug.showSaveFileDialog(xmlFilter, xmlFilter[0]) != JFileChooser.APPROVE_OPTION) return;
				MultiGraphModel mgm = (MultiGraphModel)models.get(currentSelect);
				try
				{
					mgm.save(plug.getChooserFile());
				}
				catch(Exception ex)
				{
					plug.error("Could not export PRISM graph file:\n"+ex.getMessage());
				}
			}
		};
		exportXML.putValue(Action.NAME, "PRISM graph");
		exportXML.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_X));
		exportXML.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallExport.gif"));
		exportXML.putValue(Action.LONG_DESCRIPTION, "Export graph as a PRISM graph file.");
		
		exportPNG = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (plug.showSaveFileDialog(imagesFilter, imagesFilter[0]) != JFileChooser.APPROVE_OPTION) return;
				MultiGraphView mgv = (MultiGraphView)graphs.get(currentSelect);
				mgv.doExportToPNG(plug.getChooserFile());
			}
		};
		exportPNG.putValue(Action.NAME, "PNG");
		exportPNG.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		exportPNG.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallExport.gif"));
		exportPNG.putValue(Action.LONG_DESCRIPTION, "Export graph as a PNG file.");
		
		exportJPEG = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (plug.showSaveFileDialog(imagesFilter, imagesFilter[1]) != JFileChooser.APPROVE_OPTION) return;
				MultiGraphView mgv = (MultiGraphView)graphs.get(currentSelect);
				mgv.doExportToJPEG(plug.getChooserFile());
			}
		};
		exportJPEG.putValue(Action.NAME, "JPEG");
		exportJPEG.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_J));
		exportJPEG.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallExport.gif"));
		exportJPEG.putValue(Action.LONG_DESCRIPTION, "Export graph as a JPEG file.");
		
		exportMatlab = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (plug.showSaveFileDialog(matlabFilter, matlabFilter[0]) != JFileChooser.APPROVE_OPTION) return;
				MultiGraphModel mgm = (MultiGraphModel)models.get(currentSelect);
				try
				{
					mgm.exportToMatlab(plug.getChooserFile());
				}
				catch(Exception ex)
				{
					plug.error("Could not export Matlab file:\n"+ex.getMessage());
				}
			}
		};
		exportMatlab.putValue(Action.NAME, "Matlab file");
		exportMatlab.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		exportMatlab.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallExport.gif"));
		exportMatlab.putValue(Action.LONG_DESCRIPTION, "Export graph as a Matlab file.");

		exportMenu = new JMenu("Export graph as");
		exportMenu.setMnemonic('E');
		exportMenu.setIcon(GUIPrism.getIconFromImage("smallExport.gif"));
		exportMenu.add(exportXML);
		exportMenu.add(exportPNG);
		exportMenu.add(exportJPEG);
		exportMenu.add(exportMatlab);

		
		
		printGraph = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				MultiGraphView mgv = (MultiGraphView)graphs.get(currentSelect);
				mgv.doPrint(attributes);
			}
		};
		printGraph.putValue(Action.NAME, "Print graph");
		printGraph.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		printGraph.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPrint.gif"));
		printGraph.putValue(Action.LONG_DESCRIPTION, "Print the graph to a printer or file");
		
		deleteGraph = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				//will have to be more sophisticated to notify ResultsCollections and so on maybe....
				MultiGraphOptions mgo = (MultiGraphOptions)options.get(currentSelect);
				mgo.hide();
				mgo.dispose();
				options.remove(mgo);
				MultiGraphView mgv = (MultiGraphView)graphs.get(currentSelect);
				theTabs.remove(mgv);
				graphs.remove(mgv);
				models.remove(currentSelect);
			}
		};
		deleteGraph.putValue(Action.NAME, "Delete graph");
		deleteGraph.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		deleteGraph.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.gif"));
		deleteGraph.putValue(Action.LONG_DESCRIPTION, "Deletes the graph.");
		
		graphMenu = new JPopupMenu();
		graphMenu.add(graphOptions);
		graphMenu.addSeparator();
		graphMenu.add(printGraph);
		graphMenu.add(exportMenu);
		graphMenu.add(deleteGraph);
		graphMenu.addSeparator();
		graphMenu.add(importXML);

		backMenu = new JPopupMenu();
		backMenu.add(importXMLBack);
	}
	
	//UPDATE METHODS
	
	public void newHandler()
	{
		models = new ArrayList();
		graphs = new ArrayList();
		options = new ArrayList();
		counter = 0;
		theTabs.removeAll();
	}
	
	public int addGraph(String yAxisTitle)
	{
		MultiGraphModel m = new MultiGraphModel();
                int index = -1;
                try
                {
		m.setYTitle(yAxisTitle);
		m.setMaxY(1.0);
		m.setMinorYInterval(0.1);
		m.setMajorYInterval(0.2);
		MultiGraphView  v = new MultiGraphView(m);
		m.addObserver(v);
		
		models.add(m);
		graphs.add(v);
		
		theTabs.add(v);
		v.addMouseListener(this);
		index = models.indexOf(m);
		counter++;
		theTabs.setTitleAt(index, "Graph"+counter);
		
		theTabs.setSelectedIndex(theTabs.indexOfComponent(v));
		MultiGraphOptions o = new MultiGraphOptions(m, parent, "Graph Options for "+theTabs.getTitleAt(index));
		options.add(o);
                }
                catch(SettingException e)
                {
                    plug.error(e.getMessage(), "Chart error");
                }
		return index;
	}

	public int addGraph(MultiGraphModel m)
	{
		MultiGraphView  v = new MultiGraphView(m);
		
		m.setCanvas(v);
		m.addObserver(v);
		m.changed();
		
		models.add(m);
		graphs.add(v);
		
		theTabs.add(v);
		v.addMouseListener(this);
		int index = models.indexOf(m);
		counter++;
		theTabs.setTitleAt(index, "Graph"+counter);
		
		theTabs.setSelectedIndex(theTabs.indexOfComponent(v));
		MultiGraphOptions o = new MultiGraphOptions(m, parent, "Graph Options for "+theTabs.getTitleAt(index));
		options.add(o);
		return index;
	}
	
	public void jumpToGraph(MultiGraphModel m)
	{
		for(int i = 0; i < models.size(); i++)
		{
			if(m == models.get(i))
			{
				theTabs.setSelectedComponent((MultiGraphView)graphs.get(i));
				break;
			}
		}
	}
	
	public void graphIsChanging(MultiGraphModel m)//therefore hide options
	{
		for(int i = 0; i < models.size(); i++)
		{
			if(m == models.get(i))
			{
				//System.out.println("hiding "+i);
				((MultiGraphOptions)options.get(i)).hide();
				break;
			}
		}
	}
	
	//ACCESS METHODS
	
	public MultiGraphModel getModel(int i)
	{
		return (MultiGraphModel)models.get(i);
	}
	
	public MultiGraphModel getModel(String tabHeader)
	{
		for(int i = 0; i < theTabs.getComponentCount(); i++)
		{
			if(theTabs.getTitleAt(i).equals(tabHeader))
			{
				return getModel(i);
			}
		}
		return null;
	}
	
	public int getNumModels()
	{
		return models.size();
	}
	
	public String getGraphName(int i)
	{
		return theTabs.getTitleAt(i);
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
			if(e.getSource() == theTabs)//just show the background popup
			{
				this.backMenu.show(theTabs, e.getX(),e.getY());
				return;
			}
			MultiGraphView v = null;
			for(int i = 0; i < graphs.size(); i++)
			{
				if(e.getSource() == graphs.get(i))
				{
					v = (MultiGraphView)graphs.get(i);
					break;
				}
			}
			
			if(v != null)
			{
				currentSelect = theTabs.indexOfComponent(v);
				MultiGraphOptions mgo = (MultiGraphOptions)options.get(currentSelect);
				graphOptions.setEnabled(!mgo.isVisible());
				this.graphMenu.show(v, e.getX(), e.getY());
			}
		}
	}
	
	public void mouseReleased(MouseEvent e)
	{
        if(e.isPopupTrigger())
		{
			if(e.getSource() == theTabs)//just show the background popup
			{
				this.backMenu.show(theTabs, e.getX(),e.getY());
				return;
			}
			MultiGraphView v = null;
			for(int i = 0; i < graphs.size(); i++)
			{
				if(e.getSource() == graphs.get(i))
				{
					v = (MultiGraphView)graphs.get(i);
					break;
				}
			}
			
			if(v != null)
			{
				currentSelect = theTabs.indexOfComponent(v);
				MultiGraphOptions mgo = (MultiGraphOptions)options.get(currentSelect);
				graphOptions.setEnabled(!mgo.isVisible());
				this.graphMenu.show(v, e.getX(), e.getY());
			}
		}
	}
}
