//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

package userinterface.simulator.networking;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import prism.PrismException;
import settings.FileSelector;
import settings.SettingTable;
import simulator.networking.FileSystem;
import simulator.networking.SSHHost;
import simulator.networking.SimulatorNetworkHandler;
import userinterface.GUIPrism;

public class GUINetworkEditor extends JDialog implements TreeSelectionListener, MouseListener, Observer, FileSelector
{
	public static final int CONTINUE = 0;
	public static final int CANCEL = 1;
	
	private SettingTable settingTable;
	private DefaultTreeModel treeModel;
	private SimulatorNetworkHandler networkHandler;
	
	private FileSystem selectedFileSystem = null;
	private SSHHost selectedHost = null;
	
	private JPopupMenu networkPopup, fileSystemPopup, hostPopup;
	private JMenu fileMenu, editMenu;
	private Action newNetwork, open, save, saveAs, close;
	private Action cut, copy, paste, delete;
	
	private ArrayList clipboardHosts;
	private ArrayList clipboardFSs;
	
	private boolean modified = false;
	private File activeFile = null;
	
	private FileNameExtensionFilter netFilter;
	
	
	/** Creates new form GUINetworkEditor */
	public GUINetworkEditor(Frame parent, SimulatorNetworkHandler networkHandler)
	{
		super(parent, true);
		
		setActiveFile(null);
		
		initComponents();
		setLocationRelativeTo(getParent()); // centre
		initPopups();
		theMenu.add(fileMenu);
		theMenu.add(editMenu);
		
		this.networkHandler = networkHandler;
		networkHandler.addObserver(this);
		
		settingTable = new SettingTable(parent);
		bottomPanel.add(settingTable, BorderLayout.CENTER);
		
		treeModel = new DefaultTreeModel(networkHandler);
		networkTree.setModel(treeModel);
		
		networkTree.setCellRenderer(new GUINetworkEditor.NetworkNodeRenderer());
		networkTree.addTreeSelectionListener(this);
		networkTree.addMouseListener(this);
		
		netFilter = new FileNameExtensionFilter("PRISM networks (*.xml)", "xml");
	}
	
	public int doModificationCheck()
	{
		if (!modified) return CONTINUE;
		if (!hasActiveFile())
		{
			String[] selection =
			{"Yes", "No", "Cancel"};
			int selectionNo = -1;
			selectionNo = optionPane("Network profile has been modified.\nDo you wish to save it?", "Question", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, selection, selection[0]);
			switch(selectionNo)
			{
				case 0: return a_saveAs();
				case 1: return CONTINUE;
				case 2: return CANCEL;
				default: return CANCEL;
			}
		}
		else
		{
			String[] selection =
			{"Yes", "No", "Save As...", "Cancel"};
			int selectionNo = -1;
			selectionNo = optionPane("Network profile has been modified.\nDo you wish to save it?", "Question", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, selection, selection[0]);
			switch(selectionNo)
			{
				case 0: return a_save();
				case 1: return CONTINUE;
				case 2: return a_saveAs();
				case 3: return CANCEL;
				default: return CANCEL;
			}
		}
	}
	
	public void setActiveFile(File f)
	{
		activeFile = f;
		if(f == null)
		{
			setTitle("Network Profile <Untitled>");
		}
		else
		{
			setTitle("Network Profile "+f.getName());
		}
	}
	
	public boolean hasActiveFile()
	{
		return activeFile != null;
	}
	
	public void a_new()
	{
		int cont = doModificationCheck();
		if(cont == CONTINUE)
		{
			networkHandler.newNetwork("New Profile");
			treeModel.nodeStructureChanged(networkHandler);
			settingTable.setOwners(new ArrayList());
			modified = false;
		}
	}
	
	public void a_open()
	{
		int cont = doModificationCheck();
		if(cont == CONTINUE)
		{
			if (showOpenFileDialog(netFilter) == JFileChooser.APPROVE_OPTION)
			{
				File file = GUIPrism.getGUI().getChooser().getSelectedFile();
				if (file == null)
				{
					errorDialog("Error: No file selected");
					return;
				}
				// guess model type based on extension
				try
				{
					SimulatorNetworkHandler newHandler = new SimulatorNetworkHandler();
					
					newHandler.loadNetworkFromXML(file);
					modified = false;
					setActiveFile(file);
					networkHandler.deleteObservers();
					this.networkHandler = newHandler;
					networkHandler.addObserver(this);
					treeModel.setRoot(networkHandler);
					treeModel.nodeStructureChanged(networkHandler);
                                        networkTree.revalidate();
                                        
				}
				catch(PrismException ee)
				{
					errorDialog(ee.getMessage());
				}
			}
		}
	}
	
	public int a_save()
	{
		if(!hasActiveFile())
		{
			return a_saveAs();
		}
		else
		{	try
			{
				networkHandler.saveNetworkToXML(activeFile);
				modified = false;
				return CONTINUE;
			}
			catch(PrismException ee)
			{
				errorDialog(ee.getMessage());
				return CANCEL;
			}
		}
	}
	
	public int a_saveAs()
	{
		
		if (showSaveFileDialog(netFilter) != JFileChooser.APPROVE_OPTION)
		{
			return CANCEL;
		}
		// do save
		try
		{
			File file = GUIPrism.getGUI().getChooser().getSelectedFile();
			networkHandler.saveNetworkToXML(file);
			setActiveFile(file);
			modified = false;
			return CONTINUE;
		}
		catch(PrismException ee)
		{
			errorDialog(ee.getMessage());
			return CANCEL;
		}
	}
	
	public void a_close()
	{
		int cont = doModificationCheck();
		if(cont == CONTINUE)
		{
			hide();
		}
	}
	
	public void a_addNetworkCluster()
	{
		networkHandler.addFileSystem("New FileSystem", "~", "~");
		treeModel.nodesWereInserted(networkHandler, new int []
		{ networkHandler.getChildCount() - 1});
		modified = true;
	}
	
	public void a_cut()
	{
		
	}
	
	public void a_copy()
	{
		clipboardHosts = new ArrayList();
		clipboardFSs = new ArrayList();
		
		TreePath[] selectedPaths = networkTree.getSelectionModel().getSelectionPaths();
		
		for(int i = 0; i <selectedPaths.length; i++)
		{
			
			if(selectedPaths[i].getLastPathComponent() instanceof SSHHost)
			{
				SSHHost host = (SSHHost) selectedPaths[i].getLastPathComponent();
				clipboardHosts.add(host);
			}
			else if(selectedPaths[i].getLastPathComponent() instanceof FileSystem)
			{
				FileSystem fs = (FileSystem) selectedPaths[i].getLastPathComponent();
				clipboardFSs.add(fs);
				
				
			}
		}
		
		networkTree.setSelectionInterval(-1, -1);
		
		
	}
	
	public void a_paste()
	{
		if(selectedFileSystem != null)
		{
			if(clipboardHosts != null)
			{
				for(int i = 0; i < clipboardHosts.size(); i++)
				{
					SSHHost host = (SSHHost)clipboardHosts.get(i);
					selectedFileSystem.addHost(host.getHostName(), host.getUserName());
				}
				
				treeModel.nodeStructureChanged(selectedFileSystem);
			}
			
		}
		else if(selectedHost == null) //i.e. it is the network that should be pasted to
		{
			if(clipboardFSs != null)
			{
				int[] indices = new int[clipboardFSs.size()];
				for(int i = 0; i < clipboardFSs.size(); i++)
				{
					FileSystem fs = (FileSystem)clipboardFSs.get(i);
					int index = networkHandler.addFileSystem(fs.getName(), fs.getInputDir(), fs.getOutputDir());
					indices[i] = index;
					FileSystem newFS = networkHandler.getFileSystem(index);
					
					for(int j = 0; j < fs.getNumHosts(); j++)
					{
						SSHHost host = fs.getHost(j);
						newFS.addHost(host.getHostName(), host.getUserName());
						System.out.println("adding new hosts to new filesystem");
					}
				}
				treeModel.nodesWereInserted(networkHandler, indices);
			}
			
		}
		modified = true;
		
	}
	
	public void a_delete()
	{
		// delete hosts first
		TreePath[] selectedPaths = networkTree.getSelectionModel().getSelectionPaths();
		
		
		
		
		ArrayList fss = new ArrayList();
		for(int i = 0; i <selectedPaths.length; i++)
		{
			
			if(selectedPaths[i].getLastPathComponent() instanceof SSHHost)
			{
				SSHHost host = (SSHHost) selectedPaths[i].getLastPathComponent();
				((FileSystem)host.getParent()).deleteHost(host);
				fss.add(host.getParent());
				
			}
		}
		for(int i = 0; i < fss.size(); i++)
		{
			treeModel.nodeStructureChanged((FileSystem)fss.get(i));
		}
		
		ArrayList indices = new ArrayList();
		
		for(int i = 0; i < selectedPaths.length; i++)
		{
			if(selectedPaths[i].getLastPathComponent() instanceof FileSystem)
			{
				indices.add(new Integer(networkHandler.getIndex((FileSystem)selectedPaths[i].getLastPathComponent())));
			}
		}
		
		int[] inds = new int[indices.size()];
		for(int i = 0; i < indices.size(); i++)
		{
			inds[i] = ((Integer)indices.get(i)).intValue();
		}
		Object[]objs = new Object[indices.size()];
		
		int j =0;
		
		for(int i = 0; i < selectedPaths.length; i++)
		{
			if(selectedPaths[i].getLastPathComponent() instanceof FileSystem)
			{
				FileSystem fs = (FileSystem) selectedPaths[i].getLastPathComponent();
				objs[j++] = fs;
				((SimulatorNetworkHandler)fs.getParent()).deleteFileSystem(fs);
				
			}
		}
		
		if(inds.length > 0)
		{
			treeModel.nodesWereRemoved(networkHandler, inds, objs);
		}
		
		//treeModel.nodeStructureChanged(networkHandler);
		
		settingTable.setOwners(new ArrayList());
		modified = true;
	}
	
	public void a_addHost()
	{
		if(selectedFileSystem != null)
		{
			selectedFileSystem.addHost("new_host", "user_name");
			treeModel.nodesWereInserted(selectedFileSystem, new int[]
			{ selectedFileSystem.getChildCount()- 1 });
			modified = true;
		}
	}
	
	public void a_testHosts()
	{
		TreePath[] selectedPaths = networkTree.getSelectionModel().getSelectionPaths();
		
		ArrayList fss = new ArrayList();
		for(int i = 0; i <selectedPaths.length; i++)
		{
			
			if(selectedPaths[i].getLastPathComponent() instanceof SSHHost)
			{
				SSHHost host = (SSHHost) selectedPaths[i].getLastPathComponent();
				host.testHost();
				
			}
		}
	}
	
	public void a_testAllHosts()
	{
		TreePath[] selectedPaths = networkTree.getSelectionModel().getSelectionPaths();
		for(int i = 0; i < selectedPaths.length; i++)
		{
			if(selectedPaths[i].getLastPathComponent() instanceof FileSystem)
			{
				FileSystem fs = (FileSystem) selectedPaths[i].getLastPathComponent();
				for(int j = 0; j < fs.getNumHosts(); j++)
				{
					fs.getHost(j).testHost();
				}
				
			}
		}
	}
	
	public void a_testAll()
	{
		for(int i = 0; i < networkHandler.getNumFileSystems(); i++)
		{
			FileSystem fs = networkHandler.getFileSystem(i);
			for(int j = 0; j < fs.getNumHosts(); j++)
			{
				fs.getHost(j).testHost();
			}
		}
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        javax.swing.JScrollPane jScrollPane1;
        javax.swing.JSplitPane jSplitPane1;

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        networkTree = new javax.swing.JTree();
        bottomPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        theMenu = new javax.swing.JMenuBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        networkTree.setPreferredSize(new java.awt.Dimension(500, 300));
        jScrollPane1.setViewportView(networkTree);

        jSplitPane1.setLeftComponent(jScrollPane1);

        bottomPanel.setLayout(new java.awt.BorderLayout());

        bottomPanel.setPreferredSize(new java.awt.Dimension(500, 300));
        jSplitPane1.setRightComponent(bottomPanel);

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jButton1.setText("Close");
        jButton1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton1ActionPerformed(evt);
            }
        });

        jPanel1.add(jButton1);

        getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);

        setJMenuBar(theMenu);

        pack();
    }//GEN-END:initComponents
	
	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton1ActionPerformed
	{//GEN-HEADEREND:event_jButton1ActionPerformed
		a_close();
	}//GEN-LAST:event_jButton1ActionPerformed
	
	public void initPopups()
	{
		networkPopup = new JPopupMenu();
		
		AbstractAction addFileSystem = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_addNetworkCluster();
			}
		};
		addFileSystem.putValue(Action.LONG_DESCRIPTION, "Adds a new network cluster to the network profile.");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		addFileSystem.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
		addFileSystem.putValue(Action.NAME, "Add Network Cluster");
		addFileSystem.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallAdd.png"));
		
		networkPopup.add(addFileSystem);
		
		AbstractAction testAll = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_testAll();
			}
		};
		testAll.putValue(Action.LONG_DESCRIPTION, "Test All");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		testAll.putValue(Action.NAME, "Test");
		
		networkPopup.addSeparator();
		networkPopup.add(testAll);
		networkPopup.addSeparator();
		
		fileSystemPopup = new JPopupMenu();
		
		AbstractAction addHost = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_addHost();
			}
		};
		addHost.putValue(Action.LONG_DESCRIPTION, "Adds a new host to the network cluster.");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		addHost.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_H));
		addHost.putValue(Action.NAME, "Add Host");
		addHost.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallAdd.png"));
		
		cut = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_cut();
			}
		};
		cut.putValue(Action.LONG_DESCRIPTION, "Cut");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		cut.putValue(Action.NAME, "Cut");
		cut.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
		cut.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCut.png"));
		cut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		
		copy = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_copy();
			}
		};
		copy.putValue(Action.LONG_DESCRIPTION, "Copy");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		copy.putValue(Action.NAME, "Copy");
		copy.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
		copy.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCopy.png"));
		copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		
		paste = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_paste();
			}
		};
		paste.putValue(Action.LONG_DESCRIPTION, "Paste");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		paste.putValue(Action.NAME, "Paste");
		paste.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		paste.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPaste.png"));
		paste.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		
		delete = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_delete();
			}
		};
		delete.putValue(Action.LONG_DESCRIPTION, "Delete");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		delete.putValue(Action.NAME, "Delete");
		delete.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		delete.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.png"));
		delete.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		
		AbstractAction testAllHosts = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_testAllHosts();
			}
		};
		testAllHosts.putValue(Action.LONG_DESCRIPTION, "Test Hosts");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		testAllHosts.putValue(Action.NAME, "Test Hosts");
		
		fileSystemPopup.add(addHost);
		fileSystemPopup.addSeparator();
		fileSystemPopup.add(testAllHosts);
		fileSystemPopup.addSeparator();
		//fileSystemPopup.add(cut);
		fileSystemPopup.add(copy);
		fileSystemPopup.add(paste);
		fileSystemPopup.add(delete);
		
		AbstractAction testHost = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_testHosts();
			}
		};
		testHost.putValue(Action.LONG_DESCRIPTION, "Test");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		testHost.putValue(Action.NAME, "Test");
		
		
		
		
		
		hostPopup = new JPopupMenu();
		
		hostPopup.add(testHost);
		hostPopup.addSeparator();
		//hostPopup.add(cut);
		hostPopup.add(copy);
		//hostPopup.add(paste);
		hostPopup.add(delete);
		
		networkPopup.add(paste);
		
		editMenu = new JMenu("Edit");
		//editMenu.add(cut);
		editMenu.add(copy);
		editMenu.add(paste);
		editMenu.add(delete);
		
		newNetwork = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_new();
			}
		};
		newNetwork.putValue(Action.LONG_DESCRIPTION, "New Profile");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		newNetwork.putValue(Action.NAME, "New Profile");
		newNetwork.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		newNetwork.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallNew.png"));
		
		open = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_open();
			}
		};
		open.putValue(Action.LONG_DESCRIPTION, "Open Profile");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		open.putValue(Action.NAME, "Open Profile");
		open.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		open.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallOpen.png"));
		
		save = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_save();
			}
		};
		save.putValue(Action.LONG_DESCRIPTION, "Save Profile");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		save.putValue(Action.NAME, "Save Profile");
		save.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		save.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSave.png"));
		save.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		
		saveAs = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_saveAs();
			}
		};
		saveAs.putValue(Action.LONG_DESCRIPTION, "Save Profile As...");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		saveAs.putValue(Action.NAME, "Save Profile As...");
		saveAs.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		saveAs.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSaveAs.png"));
		saveAs.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		
		close = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_close();
			}
		};
		close.putValue(Action.LONG_DESCRIPTION, "Close");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		close.putValue(Action.NAME, "Close");
		close.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_L));
		
		fileMenu = new JMenu("File");
		
		fileMenu.add(newNetwork);
		fileMenu.addSeparator();
		fileMenu.add(open);
		fileMenu.add(save);
		fileMenu.add(saveAs);
		fileMenu.addSeparator();
		fileMenu.add(close);
	}
	
	public void valueChanged(TreeSelectionEvent e)
	{
		ArrayList owners = new ArrayList();
		TreePath[] nodes = networkTree.getSelectionPaths();
		if(nodes == null) return;
		for(int i = 0; i < nodes.length; i++)
		{
			owners.add(nodes[i].getLastPathComponent());
		}
		settingTable.setOwners(owners);
		
	}
	
	public void mouseClicked(MouseEvent e)
	{
		TreePath path = networkTree.getClosestPathForLocation(e.getX(), e.getY());
		
		Object node = path.getLastPathComponent();
		
		if(node == null) return;
		else if(node instanceof SimulatorNetworkHandler)
		{
			selectedFileSystem = null;
			selectedHost = null;
		}
		else if(node instanceof FileSystem)
		{
			selectedFileSystem = (FileSystem)node;
			selectedHost = null;
		}
		else if(node instanceof SSHHost)
		{
			selectedHost = (SSHHost) node;
			selectedFileSystem = null;
		}
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
			mousePopupTrigger(e);
	}
	
	public void mouseReleased(MouseEvent e)
	{
		if(e.isPopupTrigger())
			mousePopupTrigger(e);
	}
	
	public void mousePopupTrigger(MouseEvent e)
	{
		TreePath path = networkTree.getClosestPathForLocation(e.getX(), e.getY());
		
		Object node = path.getLastPathComponent();
		
		if(node == null) return;
		else if(node instanceof SimulatorNetworkHandler)
		{
			selectedFileSystem = null;
			selectedHost = null;
			if(!isPathSelected(path))
				networkTree.setSelectionPath(path);
			networkPopup.show(networkTree, e.getX(), e.getY());
		}
		else if(node instanceof FileSystem)
		{
			selectedFileSystem = (FileSystem)node;
			selectedHost = null;
			if(!isPathSelected(path))
				networkTree.setSelectionPath(path);
			fileSystemPopup.show(networkTree,e.getX(), e.getY());
		}
		else if(node instanceof SSHHost)
		{
			selectedHost = (SSHHost) node;
			selectedFileSystem = null;
			if(!isPathSelected(path))
				networkTree.setSelectionPath(path);
			hostPopup.show(networkTree, e.getX(), e.getY());
		}
	}
	
	public boolean isPathSelected(TreePath path)
	{
		return networkTree.getSelectionModel().isPathSelected(path);
	}
	
	public void update(Observable o, Object arg)
	{
		if(arg instanceof SSHHost)
		{
			treeModel.nodeChanged((SSHHost)arg);
		}
		else if(arg instanceof FileSystem)
		{
			treeModel.nodeChanged((FileSystem)arg);
		}
		else if(arg instanceof SimulatorNetworkHandler)
		{
			treeModel.nodeChanged((SimulatorNetworkHandler)arg);
		}
		
		settingTable.refreshGroupNames();
		
		modified = true;
	}
	
	public int optionPane(String message, String title, int buttonType, int messageType, String[]choices, String defa)
	{
		return JOptionPane.showOptionDialog(this, message, title, buttonType, messageType, null, choices, defa);
	}
	
	/** Produces an error dialog box and puts it to the screen with the given message
	 * and default heading
	 * @param errorMessage The error message to be displayed.
	 */
	public void errorDialog(String errorMessage)
	{
		errorDialog("Error", errorMessage);
	}
	
	/** Produces an error dialog box and puts it to the screen with the given message
	 * and a given heading
	 * @param errorHeading The error dialog box's heading
	 * @param errorMessage The error message to be displayed.
	 */
	public void errorDialog(String errorHeading, String errorMessage)
	{
		JOptionPane.showMessageDialog(this, errorMessage, errorHeading, JOptionPane.ERROR_MESSAGE);
		//taskbar.setText(errorHeading);
	}
	
	/** A utility method to show a file opening dialog with the given file filter as a
	 * default.
	 * @param ffs The list of file filters to be used within the filechooser.
	 * @param ff The file filter to be used as the default within the filechooser.
	 * @return An integer which is one of the JFileChooser selection constants.
	 */
	public int showOpenFileDialog(FileFilter ff)
	{
		JFileChooser choose = GUIPrism.getGUI().getChooser();
		choose.resetChoosableFileFilters();
		choose.addChoosableFileFilter(ff);
		choose.setFileFilter(ff);
		choose.setSelectedFile(new File(""));
		return choose.showOpenDialog(this);
	}
	
	/** A utility method to show a file saving dialog with the given file filter as a
	 * default.
	 * @param ffs The list of file filters to be used within the filechooser.
	 * @param ff The file filter to be used as the default within the filechooser.
	 * @return An integer which is one of the JFileChooser selection constants.
	 */
	public int showSaveFileDialog(FileFilter ff)
	{
		JFileChooser choose = GUIPrism.getGUI().getChooser();
		choose.resetChoosableFileFilters();
		choose.addChoosableFileFilter(ff);
		choose.setFileFilter(ff);
		choose.setSelectedFile(new File(""));
		int res = choose.showSaveDialog(this);
		if (res != JFileChooser.APPROVE_OPTION) return res;
		File file = choose.getSelectedFile();
		// check file is non-null
		if (file == null)
		{
			GUIPrism.getGUI().errorDialog("Error: No file selected");
			return JFileChooser.CANCEL_OPTION;
		}
		// check for file overwrite
		if(file.exists())
		{
			int selectionNo = JOptionPane.CANCEL_OPTION;
			selectionNo = optionPane("File exists. Overwrite?", "Confirm Overwrite", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null);
			if (selectionNo != JOptionPane.OK_OPTION) return JFileChooser.CANCEL_OPTION;
		}
		return JFileChooser.APPROVE_OPTION;
	}
	
	public File getFile(Frame parent, File defaultFile)
	{
		modified = false;
		a_new();
		SimulatorNetworkHandler newHandler = new SimulatorNetworkHandler();
		try
		{
			newHandler.loadNetworkFromXML(defaultFile);
			modified = false;
			setActiveFile(defaultFile);
			networkHandler.deleteObservers();
					this.networkHandler = newHandler;
					
			treeModel.setRoot(networkHandler);
			treeModel.nodeStructureChanged(networkHandler);
					networkHandler.addObserver(this);
			
		}
		catch(PrismException e)
		{
			setActiveFile(null);
			modified = true;
			
		}
		
		show();
		return activeFile;
		
	}
	
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTree networkTree;
    private javax.swing.JMenuBar theMenu;
    // End of variables declaration//GEN-END:variables
	
	class NetworkNodeRenderer extends DefaultTreeCellRenderer
	{
		ImageIcon NET = GUIPrism.getIconFromImage("smallNetwork.png");
		ImageIcon SERV = GUIPrism.getIconFromImage("smallServer.png");
		ImageIcon QUESTION_HOST = GUIPrism.getIconFromImage("smallQuestion.png");
		ImageIcon GOOD_HOST = GUIPrism.getIconFromImage("smallHost.png");
		ImageIcon ERROR_HOST = GUIPrism.getIconFromImage("smallError.png");
		ImageIcon RUNNING_HOST = GUIPrism.getIconFromImage("smallClockAnim1.png");
		
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree,value,selected,expanded,leaf,row,hasFocus);
			TreeNode node = (TreeNode)value;
			
			if(node instanceof SimulatorNetworkHandler)
			{
				setIcon(NET);
			}
			else if(node instanceof FileSystem)
			{
				setIcon(SERV);
			}
			else if(node instanceof SSHHost)
			{
				SSHHost host = (SSHHost) node;
				switch(host.getHostState())
				{
					case (SSHHost.READY_UNKNOWN_STATUS): setIcon(QUESTION_HOST);break;
					case (SSHHost.READY_OKAY): setIcon(GOOD_HOST); break;
					case (SSHHost.ERROR): 
					{
						setIcon(ERROR_HOST); 
						setText(getText()+" ("+host.getErrorMessage()+")");
						break;
					} 
					default: setIcon(RUNNING_HOST);break;
				}
				
			}
			return this;
		}
	}
	
	
}
