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

package userinterface;

//Java Packages
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;

import prism.Prism;
//Prism Packages
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;
import userinterface.util.GUIComputationEvent;
import userinterface.util.GUIEvent;
import userinterface.util.GUIEventHandler;
import userinterface.util.GUIException;
import userinterface.util.GUIExitEvent;
import userinterface.util.PresentationMetalTheme;

/**  PRISM Graphical User Interface
 *  This class is the top level class for the PRISM GUI. It acts as a container
 *  for GUIPlugin objects.  The method getPluginArray() is defined to allow any
 *  class that implements this interface to be added to the user interface.  The aim
 *  of this is to make transferral between different versions of the system easier.
 *  As a top level class, GUIPrism contains some utility methods, that can be used
 *  via the GUIPlugin class.  This class is also a container of a Prism object,
 *  which acts as the link between the user interface and the underlying PRISM code.
 */
public class GUIPrism extends JFrame
{
	//CONSTANTS
	//defaults
	/** The default width of the main window */
	public static final int DEFAULT_WINDOW_WIDTH = 1024;
	/** The default height of the main window */
	public static final int DEFAULT_WINDOW_HEIGHT = 640;
	/** The minimum width of the main window */
	public static final int MINIMUM_WINDOW_WIDTH = 10;
	/** The minimum height of the main window. */
	public static final int MINIMUM_WINDOW_HEIGHT = 10;

	//STATIC MEMBERS

	private static GUIPrismSplash splash;
	private static GUIPrism gui;
	private boolean doExit;
	private static GUIClipboard clipboardPlugin;

	//STATIC METHODS

	/** The entry point of the program from the command line.
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		try {
			//Show the splash screen
			splash = new GUIPrismSplash("images/splash.png");
			splash.display();
			gui = new GUIPrism();
			gui.setVisible(true);
			EventQueue.invokeLater(new GUIPrism.SplashScreenCloser());
			gui.passCLArgs(args);
		} catch (GUIException e) {
			System.err.println("Error: Could not load the PRISM GUI: " + e.getMessage());
			System.exit(1);
		} catch (PrismException e) {
			System.err.println("Error: " + e.getMessage());
			System.exit(1);
		} catch (jdd.JDD.CuddOutOfMemoryException e) {
			System.err.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Define this method to add plugins to the GUI. Returns an ArrayList of Plugin objects.
	 * @param g The instance of GUIPrism to associate with the GUIPlugin objects.
	 * @return An ArrayList of GUIPlugins to be included in the user interface
	 */
	public static ArrayList<GUIPlugin> getPluginArray(GUIPrism g)
	{
		// Plugins
		userinterface.GUIFileMenu fileMenu;
		userinterface.model.GUIMultiModel model;
		userinterface.properties.GUIMultiProperties props;
		userinterface.simulator.GUISimulator sim;
		userinterface.log.GUILog log;
		userinterface.GUINetwork nw;
		// Create
		fileMenu = new userinterface.GUIFileMenu(g);
		clipboardPlugin = new GUIClipboard(g);
		model = new userinterface.model.GUIMultiModel(g);
		sim = new userinterface.simulator.GUISimulator(g);
		props = new userinterface.properties.GUIMultiProperties(g, sim);
		log = new userinterface.log.GUILog(g);
		nw = new userinterface.GUINetwork(g);
		// Add to list
		ArrayList<GUIPlugin> plugs = new ArrayList<GUIPlugin>();
		plugs.add(fileMenu);
		plugs.add(clipboardPlugin);
		plugs.add(model);
		plugs.add(props);
		plugs.add(sim);
		plugs.add(log);
		plugs.add(nw);
		// Make some plugins aware of others
		sim.setGUIMultiModel(model);
		// Return list
		return plugs;
	}

	public static GUIPrism getGUI()
	{
		return gui;
	}

	//ATTRIBUTES
	//properties
	private Prism prism;
	private PrismLog theLog;

	//gui components
	private ArrayList plugs;
	private JTabbedPane theTabs;
	private GUIPlugin logPlug;
	private GUIEventHandler eventHandle;
	private GUIOptionsDialog options;
	private JFileChooser choose;
	private JProgressBar progress;
	private GUITaskBar taskbar;
	private String taskbarText = "";
	private Action prismOptions;
	private Action fontIncrease;
	private Action fontDecrease;

	//CONSTRUCTORS AND INITIALISATION METHODS

	/** Creates a new instance of GUIPrism.  By calling setupResources(), setupPrism()
	 * and then initComponents().
	 * @throws GUIException Thrown if there is an error in initialising the user interface.
	 */
	public GUIPrism() throws GUIException, PrismException
	{
		super();
		setupResources();
		setupPrism();
		initComponents();
		prism.getSettings().notifySettingsListeners();
	}

	/**
	 *  Sets the URL for images properly, so that they are loaded from the
	 *  correct directory.  This also loads in all of the resouces for
	 *  internationalization support.  Also sets up the file chooser and event handler.
	 *  Throws a GUIException if there is a problem with the resources.
	 */
	private void setupResources() throws GUIException
	{
		try {
			MetalTheme theme = new PresentationMetalTheme(0);
			MetalLookAndFeel.setCurrentTheme(theme);
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			//UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			throw new GUIException("Failed to Initialise:\nLook and Feel Invalid");
		}

		// Create new file chooser which starts in current directory
		choose = new JFileChooser();
		File currentDir = new File(".");
		// If current directory is the bin directory, go up one level (mainly for Windows version)
		try {
			currentDir = currentDir.getCanonicalFile();
			if (currentDir.getName().equals("bin"))
				currentDir = currentDir.getParentFile();
		} catch (IOException e) {
			currentDir = new File(".");
		}
		choose.setCurrentDirectory(currentDir);

		logPlug = null;
		eventHandle = new GUIEventHandler(this);

		//Load resources here
		/********************/
		//This will provide for internationalisation support, if necessary
		//optimism zone
	}

	private void setupPrism() throws PrismException
	{
		theLog = new userinterface.log.GUIWindowLog();
		prism = new Prism(theLog);
		prism.loadUserSettingsFile();
		prism.initialise();
	}

	/**
	 *  This initialises all of the graphical user interface componenets.
	 *  The structure of the user interface should be
	 *  <UL>
	 *	  <LI>Menu Bar at the Top
	 *	  <LI>Toolbar just below
	 *	  <LI>Tabs with all of the pluggable "screens"
	 *	  <LI>Status Bar at the bottom
	 *  </UL>
	 */
	private void initComponents() throws GUIException
	{
		JMenuBar menuBar = new JMenuBar();
		JPanel toolPanel = new JPanel();
		toolPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		options = new GUIOptionsDialog(this);
		//options.addPanel(new GUIPrismOptionsPanel(prism));
		JPanel thePanel = new JPanel(); // panel to store tabs
		theTabs = new JTabbedPane();
		theTabs.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				clipboardPlugin.pluginChanged(getFocussedPlugin());
			}
		});
		//Setup pluggable screens in here
		plugs = getPluginArray(this);
		for (int i = 0; i < plugs.size(); i++) {
			GUIPlugin plug = (GUIPlugin) plugs.get(i);
			if (plug.displaysTab()) {
				theTabs.addTab(plug.getTabText(), plug);
				theTabs.setEnabledAt(theTabs.getComponentCount() - 1, plug.isEnabled());
			}
			if (plug.getMenu() != null) {
				menuBar.add(plug.getMenu());
			}
			if (plug.getToolBar() != null) {
				toolPanel.add(plug.getToolBar());
			}
			if (plug.getOptions() != null) {
				options.addPanel(plug.getOptions());
			}
			if (plug instanceof userinterface.log.GUILog) {
				logPlug = (userinterface.log.GUILog) plug;
			}
			prism.getSettings().addSettingsListener(plug);
		}
		theTabs.setTabPlacement(JTabbedPane.BOTTOM);
		thePanel.setLayout(new BorderLayout());
		thePanel.setPreferredSize(new Dimension(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT));
		thePanel.setMinimumSize(new Dimension(MINIMUM_WINDOW_WIDTH, MINIMUM_WINDOW_HEIGHT));
		thePanel.add(theTabs, BorderLayout.CENTER);

		JMenu optionsMenu = new JMenu("Options");
		menuBar.add(optionsMenu);

		Action tabSwapper = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				nextTab();
			}
		};
		tabSwapper.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		prismOptions = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				options.show();
			}
		};
		prismOptions.putValue(Action.LONG_DESCRIPTION, "Brings up an option dialog for setting PRISM and user interface parameters.");
		prismOptions.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		prismOptions.putValue(Action.NAME, "Options");
		prismOptions.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallOptions.png"));

		optionsMenu.add(prismOptions);
		optionsMenu.setMnemonic('O');

		fontIncrease = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				adjustFont(1);
			}
		};
		fontIncrease.putValue(Action.LONG_DESCRIPTION, "Increase the application font size.");
		fontIncrease.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
		fontIncrease.putValue(Action.NAME, "Increase font size");
		fontIncrease.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFontIncrease.png"));
		fontIncrease.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK));

		optionsMenu.add(fontIncrease);
		optionsMenu.setMnemonic('I');

		fontDecrease = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				adjustFont(-1);
			}
		};
		fontDecrease.putValue(Action.LONG_DESCRIPTION, "Decrease the application font size.");
		fontDecrease.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		fontDecrease.putValue(Action.NAME, "Decrease font size");
		fontDecrease.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFontDecrease.png"));
		fontDecrease.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		optionsMenu.add(fontDecrease);
		optionsMenu.setMnemonic('D');

		JPanel bottomPanel = new JPanel();
		{
			progress = new JProgressBar(0, 100);
			progress.setBorder(null);
			taskbar = new GUITaskBar();
			taskbar.setText("Welcome to PRISM...");
			bottomPanel.setBorder(new javax.swing.border.EtchedBorder());
			bottomPanel.setLayout(new BorderLayout());
			bottomPanel.add(progress, BorderLayout.EAST);
			bottomPanel.add(taskbar, BorderLayout.CENTER);
		}

		setJMenuBar(menuBar);
		setTitle(Prism.getToolName() + " " + Prism.getVersion());
		setDefaultCloseOperation(javax.swing.JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new java.awt.event.WindowAdapter()
		{
			public void windowClosing(java.awt.event.WindowEvent evt)
			{
				exit();
			}
		});
		getContentPane().add(toolPanel, java.awt.BorderLayout.NORTH);
		getContentPane().add(thePanel, java.awt.BorderLayout.CENTER);
		getContentPane().add(bottomPanel, java.awt.BorderLayout.SOUTH);
		setIconImage(GUIPrism.getIconFromImage("smallPrism.png").getImage());
		getContentPane().setSize(new java.awt.Dimension(800, 600));
		pack();
	}

	public void passCLArgs(String args[])
	{
		// just before we get started, pass any command-line args to all plugins
		// we first remove the -javamaxmem/-javastack arguments, if present
		List<String> argsCopy = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-javamaxmem") || args[i].equals("-javastack")) {
				// ignore argument and subsequent value
				i++;
			} else {
				argsCopy.add(args[i]);
			}
		}
		for (int i = 0; i < plugs.size(); i++) {
			GUIPlugin plug = (GUIPlugin) plugs.get(i);
			plug.takeCLArgs(argsCopy.toArray(new String[0]));
		}
	}

	/**
	 *  Adjust the main font for the GUI (+1 = one size up, -1 = one size down)
	 */
	public void adjustFont(int adjust)
	{
		Object[] objs = UIManager.getLookAndFeel().getDefaults().keySet().toArray();
		for (int i = 0; i < objs.length; i++) {
			if (objs[i].toString().toUpperCase().indexOf(".FONT") != -1) {
				Font font = UIManager.getFont(objs[i]);
				font = font.deriveFont((float) (font.getSize() + adjust));
				UIManager.put(objs[i], new FontUIResource(font));
			}
		}
		SwingUtilities.updateComponentTreeUI(this);
		SwingUtilities.updateComponentTreeUI(choose);
		SwingUtilities.updateComponentTreeUI(options);
		repaint();
	}

	//EXITERS
	public void exit()
	{
		doExit = true;
		notifyEventListeners(new GUIExitEvent(GUIExitEvent.REQUEST_EXIT));

		// Don't bug user to save defaults on exit
		/*if (prism.getSettings().isModified()) {
		    
		    String[] selection =
		    {"Yes", "No", "Cancel"};
		    int selectionNo = -1;
		    
		    selectionNo = JOptionPane.showOptionDialog(this, "Prism settings have been modified. Do you wish to save them?", "Save Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, selection, selection[0]);
		    switch(selectionNo)
		    {
		        case 0: 
		        {
		            try
		            {
		                prism.getSettings().saveSettingsFile(); break;
		            }
		            catch(PrismException e)
		            {
		                errorDialog("Error: Could not save settings",e.getMessage());
		            }
		        }
		        case 1: {break;}
		        case 2: doExit = false;
		        default: doExit = false;
		    }
		}*/

		if (doExit)
			System.exit(0);
	}

	//ACCESS METHODS

	/** Utility access method to access the functionality contained within 'prism'.
	 * @return The Prism object contained within this class.
	 */
	public Prism getPrism()
	{
		return prism;
	}

	/** Utility access method to acquire information stored within 'options'.
	 * @return The GUIOptionsDialog object contained within this class.
	 */
	public GUIOptionsDialog getOptions()
	{
		return options;
	}

	/** Utility Access method access the event handler.
	 * @return The event handler.
	 */
	public GUIEventHandler getEventHandler()
	{
		return eventHandle;
	}

	/** Utility access method which takes in a string filename and attempts to retrieve
	 * the file from the images directory.
	 * @return An ImageIcon
	 * @param file The name of the file to be loaded.
	 */
	public static ImageIcon getIconFromImage(String file)
	{
		URL url = GUIPrism.class.getClassLoader().getResource("images/" + file);
		if (url == null) {
			System.out.println("Warning: Failed to load icon file \"" + file + "\"");
			return null;
		}
		return new ImageIcon(url);
	}

	/** Utility access method for the file chooser used in the user interface.
	 * @return The file chooser for the PRISM GUI.
	 */
	public JFileChooser getChooser()
	{
		return choose;
	}

	/** Access method to return the GUIPlugin object that is currently viewable on the
	 * screen.
	 * @return The foccused GUIPlugin object.
	 */
	public GUIPlugin getFocussedPlugin()
	{
		return (GUIPlugin) theTabs.getComponentAt(theTabs.getSelectedIndex());
	}

	/** Access utility method for the PrismLog.
	 * @return The PrismLog contained within this class.
	 */
	public PrismLog getLog()
	{
		return theLog;
	}

	//UPDATE METHODS

	/** Utility method to notify the event handler of a GUIEvent.
	 * @param e The GUIEvent to be handled.
	 */
	public void notifyEventListeners(GUIEvent e)
	{
		getEventHandler().notifyListeners(e);
	}

	/** Responsible for passing on user interface events to the options panel.
	 * @param e A GUIEvent.
	 */
	public boolean processGUIEvent(GUIEvent e)
	{
		if (e instanceof GUIComputationEvent) {
			if (e.getID() == GUIComputationEvent.COMPUTATION_START) {
				prismOptions.setEnabled(false);
			} else if (e.getID() == GUIComputationEvent.COMPUTATION_DONE) {
				prismOptions.setEnabled(true);
				appendWarningsNoteToTaskBarText(prism.getMainLog());
			} else if (e.getID() == GUIComputationEvent.COMPUTATION_ERROR) {
				prismOptions.setEnabled(true);
			}
		} else if (e instanceof GUIExitEvent) {
			if (e.getID() == GUIExitEvent.CANCEL_EXIT) {
				doExit = false;
			}
		}
		return false;
	}

	/** Update method that takes the GUIPlugin object 'tab' and either disables its
	 * userinterface, or enables it according to 'enable'
	 * @param tab The GUIPlugin object to change
	 * @param enable Should it be enabled?
	 */
	public void enableTab(GUIPlugin tab, boolean enable)
	{
		for (int i = 0; i < theTabs.getComponentCount(); i++) {
			Component c = theTabs.getComponentAt(i);
			if (c instanceof GUIPlugin) {
				GUIPlugin pl = (GUIPlugin) c;

				if (pl == tab) {
					theTabs.setEnabledAt(i, enable);
					break;
				}
			}
		}
	}

	/** Moves the view to the next GUIPlugin component which has a tab.  If the last one
	 * has been reached, this sets the view back to the first one.
	 */
	public void nextTab()
	{
		theTabs.setSelectedIndex((theTabs.getSelectedIndex() + 1) % theTabs.getComponentCount());
	}

	/** Moves the view to the given GUIPlugin object.
	 * @param tab The GUIPlugin object to view.
	 */
	public void showTab(GUIPlugin tab)
	{
		for (int i = 0; i < theTabs.getComponentCount(); i++) {
			Component c = theTabs.getComponentAt(i);
			if (c == tab) {
				theTabs.setSelectedIndex(i);
				break;
			}
		}
	}

	/** This shows the GUIPlugin associated with the log. */
	public void showLogTab()
	{
		if (logPlug != null) {
			showTab(logPlug);
		}
	}

	/** Utility update method to set the text on the taskbar to 'message'
	 * @param message The message for the taskbar.
	 */
	public void setTaskBarText(String message)
	{
		taskbar.setText(message);
		// Store message to in case we want to append a (single) warning later
		taskbarText = message;
	}

	/** Utility update method to append a note about warnings to the taskbar
	 * @param log PrismLog to check for warnings
	 */
	public void appendWarningsNoteToTaskBarText(PrismLog log)
	{
		int numWarnings = log.getNumberOfWarnings();
		String message = null;
		if (numWarnings == 1)
			message = "[ There was 1 warning - see log for details ]";
		else if (numWarnings > 1)
			message = "[ There were " + numWarnings + " warnings - see log for details ]";
		if (message != null) {
			String taskbarTextNew = taskbarText;
			if (taskbarTextNew == null)
				taskbarTextNew = "";
			if (taskbarTextNew.length() > 0)
				taskbarTextNew += "  ";
			taskbarTextNew += message;
			taskbar.setText(taskbarTextNew);
		}
	}

	/** Utility update method to set the JProgressBar to an indeterminate state. */
	public void startProgress()
	{
		progress.setIndeterminate(true);
	}

	/** Utility update method to set the state of the JProgressBar to stopped. */
	public void stopProgress()
	{
		progress.setIndeterminate(false);
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

	//THREADS

	/** Thread to close the splash screen
	 */
	private static final class SplashScreenCloser implements Runnable
	{
		public void run()
		{
			splash.dispose();
		}
	}

	public static GUIClipboard getClipboardPlugin()
	{
		return clipboardPlugin;
	}
}
