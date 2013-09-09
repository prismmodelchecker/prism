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

package userinterface.properties;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;

import prism.PrismException;
import userinterface.GUIPlugin;
import userinterface.GUIPrism;
import userinterface.graph.GUIImageExportDialog;
import userinterface.graph.Graph;
import userinterface.graph.GraphException;
import userinterface.graph.GraphOptions;
import userinterface.util.GUIPrismFileFilter;

public class GUIGraphHandler extends JPanel implements MouseListener
{

	private boolean canDelete;

	private JTabbedPane theTabs;
	private JPopupMenu backMenu, graphMenu;

	private java.util.List<Graph> models;
	private java.util.List<GraphOptions> options;

	private GUIPlugin plug;

	private Action graphOptions, zoomIn, zoomOut, zoomDefault;

	private Action printGraph, deleteGraph;
	private Action exportImageJPG, exportImagePNG, exportImageEPS, exportXML, exportMatlab;
	private Action exportOpenDocumentChart, exportOpenDocumentSpreadsheet;
	private Action exportCSV, exportGNUPlot, importXML;

	private JMenu zoomMenu, exportMenu, importMenu;

	private GUIPrismFileFilter imagesFilter[], xmlFilter[], matlabFilter[], OpenDocumentChartFilter[], OpenDocumentSpreadsheetFilter[], CSVFilter[],
			GNUFilter[], DATFilter[];

	private PrintRequestAttributeSet attributes;

	public GUIGraphHandler(JFrame parent, GUIPlugin plug, boolean canDelete)
	{
		this.plug = plug;
		this.canDelete = canDelete;

		this.graphMenu = new JPopupMenu();
		this.backMenu = new JPopupMenu();

		initComponents();

		imagesFilter = new GUIPrismFileFilter[3];
		imagesFilter[0] = new GUIPrismFileFilter("PNG files (*.png)");
		imagesFilter[0].addExtension("png");
		imagesFilter[1] = new GUIPrismFileFilter("JPEG files (*.jpg, *.jpeg)");
		imagesFilter[1].addExtension("jpg");
		imagesFilter[2] = new GUIPrismFileFilter("Encapsulated PostScript files (*.eps)");
		imagesFilter[2].addExtension("eps");

		xmlFilter = new GUIPrismFileFilter[1];
		xmlFilter[0] = new GUIPrismFileFilter("PRISM graph files (*.gra)");
		xmlFilter[0].addExtension("gra");

		matlabFilter = new GUIPrismFileFilter[1];
		matlabFilter[0] = new GUIPrismFileFilter("Matlab files (*.m)");
		matlabFilter[0].addExtension("m");

		OpenDocumentChartFilter = new GUIPrismFileFilter[1];
		OpenDocumentChartFilter[0] = new GUIPrismFileFilter("OpenDocument Chart files (*.odc)");
		OpenDocumentChartFilter[0].addExtension("odc");

		OpenDocumentSpreadsheetFilter = new GUIPrismFileFilter[1];
		OpenDocumentSpreadsheetFilter[0] = new GUIPrismFileFilter("OpenDocument Spreadsheet files (*.ods)");
		OpenDocumentSpreadsheetFilter[0].addExtension("ods");

		CSVFilter = new GUIPrismFileFilter[1];
		CSVFilter[0] = new GUIPrismFileFilter("CSV files (*.csv)");
		CSVFilter[0].addExtension("csv");

		GNUFilter = new GUIPrismFileFilter[1];
		GNUFilter[0] = new GUIPrismFileFilter("GNUPlot files (*.gnu)");
		GNUFilter[0].addExtension("gnu");

		DATFilter = new GUIPrismFileFilter[1];
		DATFilter[0] = new GUIPrismFileFilter("GNUPlot data files (*.dat)");
		DATFilter[0].addExtension("dat");

		attributes = new HashPrintRequestAttributeSet();

		models = new ArrayList<Graph>();
		options = new ArrayList<GraphOptions>();
	}

	private void initComponents()
	{
		theTabs = new JTabbedPane();
		theTabs.addMouseListener(this);

		setLayout(new BorderLayout());
		add(theTabs, BorderLayout.CENTER);
		/*
				importXMLBack = new AbstractAction()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (plug.showOpenFileDialog(xmlFilter, xmlFilter[0]) != JFileChooser.APPROVE_OPTION)
							return;
						try {
							Graph mgm = Graph.load(plug.getChooserFile());
							addGraph(mgm);
						} catch(ChartException ex) {
							plug.error("Could not import PRISM graph file:\n"+ex.getMessage());
						}
					}
				};
				importXMLBack.putValue(Action.NAME, "Import PRISM graph");
				importXMLBack.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
				importXMLBack.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallImport.png"));
				importXMLBack.putValue(Action.LONG_DESCRIPTION, "Imports a saved PRISM graph from a file.");
		*/
		graphOptions = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				GraphOptions graphOptions = options.get(theTabs.getSelectedIndex());
				graphOptions.setVisible(true);
			}
		};

		graphOptions.putValue(Action.NAME, "Graph options");
		graphOptions.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_G));
		graphOptions.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallOptions.png"));
		graphOptions.putValue(Action.LONG_DESCRIPTION, "Displays the options dialog for the graph.");

		zoomIn = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				Graph mgm = models.get(theTabs.getSelectedIndex());
				mgm.zoomInBoth(-1, -1);
			}
		};

		zoomIn.putValue(Action.NAME, "In");
		zoomIn.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
		zoomIn.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPlayerFwd.png"));
		zoomIn.putValue(Action.LONG_DESCRIPTION, "Zoom in on the graph.");

		zoomOut = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				Graph mgm = models.get(theTabs.getSelectedIndex());
				mgm.zoomOutBoth(-1, -1);
			}
		};

		zoomOut.putValue(Action.NAME, "Out");
		zoomOut.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		zoomOut.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPlayerRew.png"));
		zoomOut.putValue(Action.LONG_DESCRIPTION, "Zoom out of the graph.");

		zoomDefault = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				Graph mgm = models.get(theTabs.getSelectedIndex());
				mgm.restoreAutoBounds();
			}
		};

		zoomDefault.putValue(Action.NAME, "Default");
		zoomDefault.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		zoomDefault.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPlayerStart.png"));
		zoomDefault.putValue(Action.LONG_DESCRIPTION, "Set the default zoom for the graph.");

		importXML = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (plug.showOpenFileDialog(xmlFilter, xmlFilter[0]) != JFileChooser.APPROVE_OPTION)
					return;
				try {
					Graph mgm = Graph.load(plug.getChooserFile());
					addGraph(mgm);
				} catch (GraphException ex) {
					plug.error("Could not import PRISM graph file:\n" + ex.getMessage());
				}
			}
		};
		importXML.putValue(Action.NAME, "PRISM graph (*.gra)");
		importXML.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
		importXML.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileGraph.png"));
		importXML.putValue(Action.LONG_DESCRIPTION, "Imports a saved PRISM graph from a file.");

		exportXML = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (plug.showSaveFileDialog(xmlFilter, xmlFilter[0]) != JFileChooser.APPROVE_OPTION)
					return;
				Graph mgm = models.get(theTabs.getSelectedIndex());
				try {
					mgm.save(plug.getChooserFile());
				} catch (PrismException ex) {
					plug.error("Could not export PRISM graph file:\n" + ex.getMessage());
				}
			}
		};
		exportXML.putValue(Action.NAME, "PRISM graph (*.gra)");
		exportXML.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_X));
		exportXML.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileGraph.png"));
		exportXML.putValue(Action.LONG_DESCRIPTION, "Export graph as a PRISM graph file.");

		exportImageJPG = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				GUIImageExportDialog imageDialog = new GUIImageExportDialog(plug.getGUI(), getModel(theTabs.getSelectedIndex()), GUIImageExportDialog.JPEG);

				saveImage(imageDialog);
			}
		};
		exportImageJPG.putValue(Action.NAME, "JPEG Interchange Format (*.jpg, *.jpeg)");
		exportImageJPG.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_J));
		exportImageJPG.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileImage.png"));
		exportImageJPG.putValue(Action.LONG_DESCRIPTION, "Export graph as a JPEG file.");

		exportImagePNG = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				GUIImageExportDialog imageDialog = new GUIImageExportDialog(plug.getGUI(), getModel(theTabs.getSelectedIndex()), GUIImageExportDialog.PNG);

				saveImage(imageDialog);
			}
		};
		exportImagePNG.putValue(Action.NAME, "Portable Network Graphics (*.png)");
		exportImagePNG.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		exportImagePNG.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileImage.png"));
		exportImagePNG.putValue(Action.LONG_DESCRIPTION, "Export graph as a Portable Network Graphics file.");

		exportImageEPS = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				GUIImageExportDialog imageDialog = new GUIImageExportDialog(plug.getGUI(), getModel(theTabs.getSelectedIndex()), GUIImageExportDialog.EPS);

				saveImage(imageDialog);
			}
		};
		exportImageEPS.putValue(Action.NAME, "Encapsulated PostScript (*.eps)");
		exportImageEPS.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_E));
		exportImageEPS.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFilePdf.png"));
		exportImageEPS.putValue(Action.LONG_DESCRIPTION, "Export graph as an Encapsulated PostScript file.");

		exportMatlab = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (plug.showSaveFileDialog(matlabFilter, matlabFilter[0]) != JFileChooser.APPROVE_OPTION)
					return;
				Graph mgm = models.get(theTabs.getSelectedIndex());

				try {
					mgm.exportToMatlab(plug.getChooserFile());
				} catch (IOException ex) {
					plug.error("Could not export Matlab file:\n" + ex.getMessage());
				}
			}
		};
		exportMatlab.putValue(Action.NAME, "Matlab file (*.m)");
		exportMatlab.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		exportMatlab.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileMatlab.png"));
		exportMatlab.putValue(Action.LONG_DESCRIPTION, "Export graph as a Matlab file.");

		exportOpenDocumentChart = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				/*	if (plug.showSaveFileDialog(OpenDocumentChartFilter, OpenDocumentChartFilter[0]) != JFileChooser.APPROVE_OPTION)
						return;
					GraphView mgv = (GraphView)views.get(
						theTabs.getSelectedIndex()
					);
					try {
						mgv.doExportToOpenDocumentChart(plug.getChooserFile());
					} catch(ChartException ex) {
						plug.error("Could not export OpenDocument Chart file:\n" + ex.getMessage());
					}*/
			}
		};
		exportOpenDocumentChart.putValue(Action.NAME, "OpenDocument Chart");
		exportOpenDocumentChart.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		exportOpenDocumentChart.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallExport.png"));
		exportOpenDocumentChart.putValue(Action.LONG_DESCRIPTION, "Export graph as a OpenDocument Chart file.");

		exportOpenDocumentSpreadsheet = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				/*if (plug.showSaveFileDialog(OpenDocumentSpreadsheetFilter, OpenDocumentSpreadsheetFilter[0]) != JFileChooser.APPROVE_OPTION)
					return;
				GraphView mgv = (GraphView)views.get(
					theTabs.getSelectedIndex()
				);
				try {
					mgv.doExportToOpenDocumentSpreadsheet(plug.getChooserFile());
				} catch(ChartException ex) {
					plug.error("Could not export OpenDocument Spreadsheet file:\n" + ex.getMessage());
				}*/
			}
		};
		exportOpenDocumentSpreadsheet.putValue(Action.NAME, "OpenDocument Spreadsheet");
		exportOpenDocumentSpreadsheet.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		exportOpenDocumentSpreadsheet.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallExport.png"));
		exportOpenDocumentSpreadsheet.putValue(Action.LONG_DESCRIPTION, "Export graph as a OpenDocument Spreadsheet file.");

		exportCSV = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				/*if (plug.showSaveFileDialog(CSVFilter, CSVFilter[0]) != JFileChooser.APPROVE_OPTION)
					return;
				GraphModel mgm = models.get(
					theTabs.getSelectedIndex()
				);
				try {
					mgm.exportToCSV(plug.getChooserFile());
				} catch(ChartException ex) {
					plug.error("Could not export CSV file:\n"+ex.getMessage());
				}*/
			}
		};
		exportCSV.putValue(Action.NAME, "CSV file");
		exportCSV.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
		exportCSV.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallExport.png"));
		exportCSV.putValue(Action.LONG_DESCRIPTION, "Export graph as a CSV file.");

		exportGNUPlot = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				Graph mgm = models.get(theTabs.getSelectedIndex());

				if (plug.showSaveFileDialog(GNUFilter, GNUFilter[0]) != JFileChooser.APPROVE_OPTION)
					return;
				File file1 = plug.getChooserFile();

				if (plug.showSaveFileDialog(DATFilter, DATFilter[0]) != JFileChooser.APPROVE_OPTION)
					return;
				File file2 = plug.getChooserFile();

				/*y {	
					//mgm.exportToGNUPlot(file1,file2);
				} catch(ChartException ex) {
					plug.error("Could not export GNU file:\n" + ex.getMessage());
				}*/
			}
		};
		exportGNUPlot.putValue(Action.NAME, "GNUPlot file");
		exportGNUPlot.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_G));
		exportGNUPlot.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallExport.png"));
		exportGNUPlot.putValue(Action.LONG_DESCRIPTION, "Export graph as GNUPlot files.");

		printGraph = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				Graph graph = models.get(theTabs.getSelectedIndex());

				if (!graph.getDisplaySettings().getBackgroundColor().equals(Color.white)) {
					if (plug.questionYesNo("Your graph has a coloured background, this background will show up on the \n"
							+ "printout. Would you like to make the current background colour white?") == 0) {
						graph.getDisplaySettings().setBackgroundColor(Color.white);
					}
				}

				graph.createChartPrintJob();
			}
		};
		printGraph.putValue(Action.NAME, "Print graph");
		printGraph.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		printGraph.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPrint.png"));
		printGraph.putValue(Action.LONG_DESCRIPTION, "Print the graph to a printer or file");

		deleteGraph = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				Graph graph = models.get(theTabs.getSelectedIndex());

				models.remove(theTabs.getSelectedIndex());
				options.remove(theTabs.getSelectedIndex());
				theTabs.remove(graph);
			}
		};
		deleteGraph.putValue(Action.NAME, "Delete graph");
		deleteGraph.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		deleteGraph.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.png"));
		deleteGraph.putValue(Action.LONG_DESCRIPTION, "Deletes the graph.");

		zoomMenu = new JMenu("Zoom");
		zoomMenu.setMnemonic('Z');
		zoomMenu.setIcon(GUIPrism.getIconFromImage("smallView.png"));
		zoomMenu.add(zoomIn);
		zoomMenu.add(zoomOut);
		zoomMenu.add(zoomDefault);

		exportMenu = new JMenu("Export graph");
		exportMenu.setMnemonic('E');
		exportMenu.setIcon(GUIPrism.getIconFromImage("smallExport.png"));
		exportMenu.add(exportXML);
		exportMenu.add(exportImagePNG);
		exportMenu.add(exportImageEPS);
		exportMenu.add(exportImageJPG);

		exportMenu.add(exportMatlab);

		importMenu = new JMenu("Import graph");
		importMenu.setMnemonic('I');
		importMenu.setIcon(GUIPrism.getIconFromImage("smallImport.png"));
		importMenu.add(importXML);

		graphMenu.add(graphOptions);
		graphMenu.add(zoomMenu);
		graphMenu.addSeparator();
		graphMenu.add(printGraph);
		graphMenu.add(deleteGraph);
		graphMenu.addSeparator();
		graphMenu.add(exportMenu);
		graphMenu.add(importMenu);

		/* Tab context menu */
		backMenu.add(importXML);
	}

	public void saveImage(GUIImageExportDialog imageDialog)
	{
		if (!imageDialog.isCancelled()) {
			Graph graph = getModel(theTabs.getSelectedIndex());

			/* If background is not white, and it will show up, then lets warn everyone. */
			if (!graph.getDisplaySettings().getBackgroundColor().equals(Color.white)
					&& (imageDialog.getImageType() != GUIImageExportDialog.PNG || !imageDialog.getAlpha())) {
				if (plug.questionYesNo("Your graph has a coloured background, this background will show up on the \n"
						+ "exported image. Would you like to make the current background colour white?") == 0) {
					graph.getDisplaySettings().setBackgroundColor(Color.white);
				}
			}

			if (imageDialog.getImageType() == GUIImageExportDialog.JPEG) {
				if (plug.showSaveFileDialog(imagesFilter, imagesFilter[1]) != JFileChooser.APPROVE_OPTION)
					return;

				try {
					graph.exportToJPEG(plug.getChooserFile(), imageDialog.getExportWidth(), imageDialog.getExportHeight());
				} catch (GraphException ex) {
					plug.error("Could not export JPEG file:\n" + ex.getMessage());
				} catch (IOException ex) {
					plug.error("Could not export JPEG file:\n" + ex.getMessage());
				}
			} else if (imageDialog.getImageType() == GUIImageExportDialog.PNG) {
				if (plug.showSaveFileDialog(imagesFilter, imagesFilter[0]) != JFileChooser.APPROVE_OPTION)
					return;

				try {
					graph.exportToPNG(plug.getChooserFile(), imageDialog.getExportWidth(), imageDialog.getExportHeight(), imageDialog.getAlpha());
				} catch (GraphException ex) {
					plug.error("Could not export PNG file:\n" + ex.getMessage());
				} catch (IOException ex) {
					plug.error("Could not export PNG file:\n" + ex.getMessage());
				}
			} else if (imageDialog.getImageType() == GUIImageExportDialog.EPS) {
				if (plug.showSaveFileDialog(imagesFilter, imagesFilter[2]) != JFileChooser.APPROVE_OPTION)
					return;

				try {
					graph.exportToEPS(plug.getChooserFile(), imageDialog.getExportWidth(), imageDialog.getExportHeight());
				} catch (GraphException ex) {
					plug.error("Could not export EPS file:\n" + ex.getMessage());
				} catch (IOException ex) {
					plug.error("Could not export EPS file:\n" + ex.getMessage());
				}
			}
		}
	}

	/* We can't keep using the same menu because Swing won't allow
	 * two components on screen with the same reference. Since one
	 * instance of GUIGraphHandler contains N graphs, it needs N
	 * import/export menus.
	 */
	public JMenu getExportMenu()
	{
		JMenu menu = new JMenu("Import/Export..");
		menu.setIcon(GUIPrism.getIconFromImage("Export.png"));
		menu.setMnemonic('I');

		menu.add(new JLabel("Import"));
		menu.add(importXML);
		menu.addSeparator();
		menu.add(new JLabel("Export"));
		menu.add(exportXML);
		menu.add(exportImagePNG);
		menu.add(exportImageEPS);
		menu.add(exportImageJPG);
		menu.add(exportMatlab);
		menu.add(exportOpenDocumentChart);
		menu.add(exportOpenDocumentSpreadsheet);
		menu.add(exportCSV);
		menu.add(exportGNUPlot);

		return menu;
	}

	public Action getPrintGraph()
	{
		return printGraph;
	}

	public Action getDeleteGraph()
	{
		if (canDelete)
			return deleteGraph;
		return null;
	}

	public int addGraph(Graph m)
	{
		String name = "";

		boolean nameNew;
		int counter = 1;

		while (true) {
			name = "Graph " + (counter);
			nameNew = true;

			for (int i = 0; i < theTabs.getComponentCount(); i++) {
				if (theTabs.getTitleAt(i).equals(name))
					nameNew = false;
			}

			if (nameNew)
				return addGraph(m, name);

			counter++;
		}
	}

	public int addGraph(Graph m, String tabName)
	{
		// add the model to the list of models
		models.add(m);

		// make the graph appear as a tab
		theTabs.add(m);
		options.add(new GraphOptions(plug, m, plug.getGUI(), "Options for graph " + tabName));

		// anything that happens to the graph should propagate
		m.addMouseListener(this);

		// get the index of this model in the model list
		int index = models.indexOf(m);

		// increase the graph count and title the tab
		theTabs.setTitleAt(index, tabName);

		// make this new tab the default selection
		theTabs.setSelectedIndex(theTabs.indexOfComponent(m));

		// return the index of the component
		return index;
	}

	public void jumpToGraph(Graph m)
	{
		for (int i = 0; i < models.size(); i++) {
			if (m == models.get(i)) {
				theTabs.setSelectedComponent(m);
				break;
			}
		}
	}

	public Graph getModel(int i)
	{
		return models.get(i);
	}

	public Graph getModel(String tabHeader)
	{
		for (int i = 0; i < theTabs.getComponentCount(); i++) {
			if (theTabs.getTitleAt(i).equals(tabHeader)) {
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

	// User right clicked on a tab
	public void mousePressed(MouseEvent e)
	{
		if (e.isPopupTrigger()) {
			popUpTriggered(e);
		}
	}

	public void mouseClicked(MouseEvent e)
	{
		// Zoom out on double click
		if (e.getClickCount() == 2) {
			if (e.getSource() instanceof Graph) {
				((Graph) e.getSource()).restoreAutoBounds();
			}
		}
	}

	public void mouseReleased(MouseEvent e)
	{
		if (e.isPopupTrigger()) {
			popUpTriggered(e);
		}
	}

	private void popUpTriggered(MouseEvent e)
	{
		if (e.getSource() == theTabs)//just show the background popup
		{
			int index = theTabs.indexAtLocation(e.getX(), e.getY());
			if (index != -1) {
				graphOptions.setEnabled(true);
				zoomMenu.setEnabled(true);

				exportMenu.setEnabled(true);
				importMenu.setEnabled(true);

				printGraph.setEnabled(true);
				deleteGraph.setEnabled(true);

				theTabs.setSelectedIndex(index);

				this.graphMenu.show(theTabs, e.getX(), e.getY());
			} else {
				graphOptions.setEnabled(false);
				zoomMenu.setEnabled(false);

				exportMenu.setEnabled(false);
				importMenu.setEnabled(true);

				printGraph.setEnabled(false);
				deleteGraph.setEnabled(false);

				this.graphMenu.show(theTabs, e.getX(), e.getY());
			}
			return;
		}

		for (int i = 0; i < models.size(); i++) {
			if (e.getSource() == models.get(i)) {
				graphOptions.setEnabled(true);
				zoomMenu.setEnabled(true);

				exportMenu.setEnabled(true);
				importMenu.setEnabled(true);

				printGraph.setEnabled(true);
				deleteGraph.setEnabled(true);

				theTabs.setSelectedIndex(i);
				this.graphMenu.show(models.get(i), e.getX(), e.getY());
				return;
			}
		}
	}

	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		g.clearRect(0, 0, this.getWidth(), this.getHeight());
	}

	// don't implement these for tabs
	//public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

}
