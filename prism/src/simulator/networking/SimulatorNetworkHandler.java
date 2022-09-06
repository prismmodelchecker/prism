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

package simulator.networking;

import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import java.io.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.*;

//import userinterface.util.*;
import settings.*;
import prism.*;
import parser.*;

public class SimulatorNetworkHandler extends Observable implements EntityResolver, SettingOwner, TreeNode, TableModel
{
	public static final String EXECUTABLE = "prismsimulator"; //always linux
	
	public static final int NOT_RUNNING = 0;
	public static final int RUNNING = 1;
	
	public SingleLineStringSetting networkName;
	//public SingleProperty sshDir;
	public SingleLineStringSetting sshCommand;
	public SingleLineStringSetting scpCommand;
	
	private int networkState;
	
	
	private Vector fileSystems;
	
	private File networkFile;
	
	/** Creates a new instance of SimulatorNetworkHandler */
	public SimulatorNetworkHandler()
	{
		fileSystems = new Vector();
		
		this.networkName = new SingleLineStringSetting("network name", "default", "A name for this network profile.  Each network profile is a collection of network clusters.  Each cluster has the characteristic that it has the same filesystem.", this, false);
		
		this.sshCommand = new SingleLineStringSetting("ssh command", "/usr/bin/ssh2", "The location on the network cluster's filesystem of the ssh command", this, true);
		this.scpCommand = new SingleLineStringSetting("scp command", "/usr/bin/scp2", "The location on the network cluster's filesystem of the scp command", this, true);
		
		setState(NOT_RUNNING);
		
	}
	
	public void setState(int state)
	{
		this.networkState = state;
		setChanged();
		notifyObservers();
	}
	
	public void setName(String name) throws SettingException
	{
		networkName.setValue(name);
	}
	
	//Access Methods
	
	public int getState()
	{
		return networkState;
	}
	
	public String getName()
	{
		return networkName.getStringValue();
	}
	
	public String getSSHCommand()
	{
		return sshCommand.getStringValue();
	}
	
	public String getSCPCommand()
	{
		return scpCommand.getStringValue();
	}
	
	public String toString()
	{
		return getSettingOwnerClassName() +" "+getSettingOwnerName();
	}
	
	//SettingOwner methods
	
	public void newNetwork(String name)
	{
		try
		{
			this.networkName.setValue(name);
			this.sshCommand.setValue("/usr/bin/ssh2");
			this.scpCommand.setValue("/usr/bin/scp2");
		}
		catch(SettingException eee)
		{
			//Do nothing
		}
		fileSystems.removeAllElements();
		
		setChanged();
		notifyObservers(null);
	}
	
	public int compareTo(Object o)
	{
		if(o instanceof SettingOwner)
		{
			SettingOwner po = (SettingOwner) o;
			if(getSettingOwnerID() < po.getSettingOwnerID())return -1;
			else if(getSettingOwnerID() > po.getSettingOwnerID()) return 1;
			else return 0;
		}
		else return 0;
	}
	
	private SettingDisplay display;
	
	public SettingDisplay getDisplay()
	{
		return display;
	}
	
	public int getNumSettings()
	{
		return 3;
	}
	
	public Setting getSetting(int index)
	{
		switch(index)
		{
			case 0: return networkName;
			case 1: return sshCommand;
			default: return scpCommand;
		}
	}
	
	public String getSettingOwnerClassName()
	{
		return "Network Profile";
	}
	
	public int getSettingOwnerID()
	{
		return PropertyConstants.NETWORK_PROFILE;
	}
	
	public String getSettingOwnerName()
	{
		return getName();
	}
	
	public void notifySettingChanged(Setting setting)
	{
		notifyChanged(this);
	}
	
	public void setDisplay(SettingDisplay display)
	{
		this.display = display;
	}
	
	public int addFileSystem(String name, String binaryDir,
	String resultsDir)
	{
		FileSystem fs = new FileSystem(this, name, binaryDir, resultsDir);
		fileSystems.add(fs);
		return fileSystems.indexOf(fs);
	}
	
	public void deleteFileSystem(FileSystem fs)
	{
		fileSystems.remove(fs);
		setChanged();
		notifyObservers(null);
	}
	
	public FileSystem getFileSystem(int index)
	{
		return (FileSystem)fileSystems.get(index);
	}
	
	public int getNumFileSystems()
	{
		return fileSystems.size();
	}
	
	public SimulatorResultsFile getResultsFile()
	{
		return srf;
	}
	
	public void removeAll()
	{
		fileSystems.clear();
	}
	
	private File simBinary;
	private long maxPathLength;
	private int noIterations;
	private boolean feedback, resultsFeedback;
	private SimulatorResultsFile srf = new SimulatorResultsFile();
	private Values modelConstants;
	private ArrayList propertyConstantRanges;
	private ResultsCollection resultsCollection;
	
	public void doTesting()
	{
		Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					setState(RUNNING);
					
					//start the test threads
					for(int i = 0; i < getNumHosts(); i++)
					{
						getHost(i).testHost();
					}
					
					//wait until they're done
					while(allNotTesting())
					{
						Thread.sleep(50);
					}
				}
				catch(InterruptedException e)
				{
					
				}
				
				setState(NOT_RUNNING);
			}
		});
		t.start();
	}
	
	private boolean allNotTesting()
	{
		for(int i = 0; i < getNumHosts(); i++)
		{
			if(getHost(i).getHostState() == SSHHost.TESTING) return false;
		}
		
		return true;
	}
	
	public void doNetworking(int noIteration, long maxPathLengt, File simBinar, boolean feedbac, boolean resultsFeed)
	{
		doNetworking(noIteration, maxPathLengt, simBinar, feedbac, resultsFeed, null, null, null);
	}
	
	public void doNetworking(int noIteration, long maxPathLengt, File simBinar, boolean feedbac, boolean resultsFeed, Values modelConstant, ArrayList propertyConstants, ResultsCollection rc)
	{
		this.simBinary = simBinar;
		this.maxPathLength = maxPathLengt;
		this.noIterations = noIteration;
		this.feedback = feedbac;
		this.resultsFeedback = resultsFeed;
		this.modelConstants = modelConstant;
		this.propertyConstantRanges = propertyConstants;
		this.resultsCollection = rc;
		
		Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					System.out.println("RUNNING THE NETWORK");
					setState(RUNNING);
					RemoteHandler.configure(sshCommand.toString(), scpCommand.toString());
					
					//Send the input files to each filesystem
					for(int i = 0; i < fileSystems.size(); i++)
					{
						getFileSystem(i).sendFiles(simBinary);
					}
					
					int noHosts = countReadyHosts();
					
					int perHost = (noIterations-countIterationsDone()) / noHosts;
					
					//Start off each host that is in "READY" state
					for(int i = 0; i < getNumHosts(); i++)
					{
						SSHHost sshh = getHost(i);
						if(sshh.getHostState() == SSHHost.READY_OKAY || sshh.getHostState() == SSHHost.READY_UNKNOWN_STATUS)
						{
							sshh.startStint(perHost, maxPathLength, simBinary.getName(), feedback, resultsFeedback);
						}
					}
					srf.reset();
					
					try
					{
						//main loop
						while(countIterationsDone() < noIterations && countOkayHosts() > 0)
						{
							for(int i = 0; i < getNumHosts(); i++)
							{
								SSHHost sshh = getHost(i);
								int state = sshh.getHostState();
								if(state == SSHHost.ERROR)
								{
									//error, get rid of these iterations so other hosts can take them
									sshh.setErroneousStintToZero();
								}
								else if(state == SSHHost.DONE)
								{
									//collate results for the stint
									sshh.doneSuccessfulStint();
									System.out.println("");
									System.out.println("");
									System.out.println("done a stint");
									System.out.println("");
									System.out.println("");
									sortOutResultsFile(true);
									System.out.println("srf = "+srf.toString());
								}
								else if(state == SSHHost.READY_OKAY || state == SSHHost.READY_UNKNOWN_STATUS)
								{
									//ready for redistribution
									int noLeftToGo = noIterations - countIterationsAssigned();
									int use = noLeftToGo;
									if(use > 0)System.out.println("use is "+use);
									if(noLeftToGo > 1000)
									{
										System.out.println("left is > 1000");
										int numFailed = getNumHosts()-countOkayHosts();
										if(numFailed > 0)
										{
											System.out.println("numfailed > 0");
											System.out.println("formula = "+((noIterations/getNumHosts())*numFailed)/countOkayHosts());
											System.out.println("noLeftToGo ="+noLeftToGo);
											System.out.println("use = "+use);
											use = Math.min(use, ((noIterations/getNumHosts())*numFailed)/countOkayHosts());
										}
										
										use = Math.max(1000, use);
									}
									
									if(use > 0)
										sshh.startStint(use,maxPathLength,simBinary.getPath(), feedback, resultsFeedback);
								}
								
							}
							if(resultsFeedback) sortOutResultsFile(true);
							Thread.sleep(200); //no need to run this all of the time.
						}
					}
					catch(InterruptedException e)
					{
						
					}
					//Deal with STOPPED hosts
					for(int i = 0; i < getNumHosts(); i++)
					{
						SSHHost sshh = getHost(i);
						if(sshh.getHostState() == SSHHost.STOPPED) sshh.doneSuccessfulStint();
					}
					
					//TO-DO cancel running threads and tidy up results
					
					sortOutResultsFile(false);
					
					setState(NOT_RUNNING);
					System.out.println(srf);
				}
				catch(Exception e)
				{
					e.printStackTrace();
					System.out.println("error ");
					setState(NOT_RUNNING);
				}
			}
		});
		t.start();
	}
	
	public void stopNetworking() throws PrismException
	{
		for(int i = 0; i < getNumHosts(); i++)
		{
			getHost(i).stopStint();
		}
	}
	
	public void sortOutResultsFile(boolean includeTempFiles)
	{
		srf.reset();
		
		if(includeTempFiles)
		{
			boolean notChanged = true;
			for(int j = 0; j < getNumHosts() && notChanged; j++)
			{
				notChanged = !getHost(j).haveResultsChanged();
			}
			
			if(notChanged) return;
		}
		
		for(int j = 0; j < getNumHosts(); j++)
		{
			SSHHost ssh = getHost(j);
			
			for(int k = 0; k < ssh.getTotalStints(); k++)
			{
				File f = ssh.getResultsFile(k);
				try
				{
					System.out.println("merging results file "+j+","+k);
					srf.mergeResultsFile(f);
				}
				catch(PrismException e)
				{
					System.out.println("exception "+e.getMessage());
				}
			}
			if(includeTempFiles && ssh.hasTempResults())
			{
				try
				{
					//System.out.println("going temp result");
					srf.mergeResultsFile(ssh.getTempResults());
				}
				catch(PrismException e)
				{
					//do nothing
				}
			}
			
		}
		
		//If we are adding temporarily to a ResultsCollection, do this here.
		if(resultsCollection != null && modelConstants != null && propertyConstantRanges != null)
		{
			
				for(int i = 0; i < propertyConstantRanges.size(); i++)
				{
					Values pcs = (Values)propertyConstantRanges.get(i);
					double res = srf.getResult(i);
					Object result = (res < 0.0)?null:new Double(res);
					resultsCollection.setResult(modelConstants, pcs, result);
				}
			
		}
		
		srf.tellObservers();
	}
	
	public boolean stillRunning()
	{
		boolean stillRunning = false;
		for(int i = 0; i < fileSystems.size() && !stillRunning; i++)
		{
			stillRunning = getFileSystem(i).stillRunning();
		}
		return stillRunning;
	}
	
	public int countReadyHosts()
	{
		int count = 0;
		for(int i = 0; i < getNumHosts(); i++)
		{
			SSHHost sshh = getHost(i);
			if(sshh.getHostState() == SSHHost.READY_OKAY || sshh.getHostState() == SSHHost.READY_UNKNOWN_STATUS) count++;
		}
		return count;
	}
	
	public int countOkayHosts()
	{
		int count = 0;
		for(int i = 0; i < getNumHosts(); i++)
		{
			SSHHost sshh = getHost(i);
			if(sshh.getHostState() != SSHHost.ERROR && sshh.getHostState() != SSHHost.STOPPED) count++;
		}
		return count;
	}
	
	public int countIterationsDone()
	{
		int count = 0;
		for(int i = 0; i < getNumHosts(); i++)
		{
			SSHHost sshh = getHost(i);
			count += sshh.getTotalDone();
		}
		return count;
	}
	
	public int countIterationsAssigned()
	{
		int count = 0;
		for(int i = 0; i < getNumHosts(); i++)
		{
			SSHHost sshh = getHost(i);
			count += sshh.getNoToDoThisStint()+sshh.getTotalDone();
		}
		return count;
	}
	
	public void loadNetworkFromXML(File f) throws PrismException
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(true);
			factory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(this);
			Document doc = builder.parse(f);
			Element prismNetwork = doc.getDocumentElement();
			
			networkName.setValue(prismNetwork.getAttribute("systemName"));
			//sshDir.setProperty(prismNetwork.getAttribute("sshDir"));
			sshCommand.setValue(prismNetwork.getAttribute("sshCommand"));
			scpCommand.setValue(prismNetwork.getAttribute("scpCommand"));
			
			NodeList fsNodes = prismNetwork.getChildNodes();
			
			removeAll();
			
			for(int i = 0; i < fsNodes.getLength(); i++)
			{
				Element fsNode = (Element)fsNodes.item(i);
				String fsName = fsNode.getAttribute("name");
				String fsExecDir = fsNode.getAttribute("executeDir");
				String fsBinaryDir = fsNode.getAttribute("binaryDir");
				String fsResultsDir = fsNode.getAttribute("resultsDir");
				addFileSystem(fsName, fsBinaryDir, fsResultsDir);
				FileSystem fs = getFileSystem(fileSystems.size()-1);
				NodeList hostNodes = fsNode.getChildNodes();
				for(int j = 0; j < hostNodes.getLength(); j++)
				{
					Element hostNode = (Element)hostNodes.item(j);
					String hostName = hostNode.getAttribute("hostName");
					String userName = hostNode.getAttribute("userName");
					fs.addHost(hostName, userName);
				}
			}
		}
		catch(Exception e)
		{
			removeAll();
			System.err.println("error in loading network "+e);
			e.printStackTrace();
			throw new PrismException(e.getMessage());
		}
	}
	
	public void saveNetworkToXML(File file) throws PrismException
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			
			Element prismNetwork = doc.createElement("prismNetwork");
			prismNetwork.setAttribute("systemName", getName());
			prismNetwork.setAttribute("sshCommand", getSSHCommand());
			prismNetwork.setAttribute("scpCommand", getSCPCommand());
			
			for(int i = 0; i < getNumFileSystems(); i++)
			{
				FileSystem ss = getFileSystem(i);
				Element fsn = doc.createElement("fileSystem");
				fsn.setAttribute("name", ss.getName());
				fsn.setAttribute("executeDir","");
				fsn.setAttribute("binaryDir", ss.getInputDir());
				fsn.setAttribute("resultsDir", ss.getOutputDir());
				
				for(int j = 0; j < ss.getNumHosts(); j++)
				{
					SSHHost hh = ss.getHost(j);
					Element hos = doc.createElement("host");
					hos.setAttribute("hostName", hh.getHostName());
					hos.setAttribute("userName", hh.getUserName());
					fsn.appendChild(hos);
				}
				prismNetwork.appendChild(fsn);
			}
			
			doc.appendChild(prismNetwork);
			
			//File writing
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty("doctype-system", "prismnetwork.dtd");
			t.setOutputProperty("indent", "yes");
			t.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(file)));
			
			
		}
		catch(DOMException e)
		{
			//TODO handle these exceptions
			throw new PrismException("Problem saving network settings: DOM Exception: "+e.getMessage());
		}
		catch(ParserConfigurationException e)
		{
			throw new PrismException("Problem saving network settings: Parser Exception: "+e.getMessage());
		}
		catch(TransformerConfigurationException e)
		{
			throw new PrismException("Problem saving network settings: Error in creating XML: "+e.getMessage());
		}
		catch(FileNotFoundException e)
		{
			throw new PrismException("Problem saving network settings: File Not Found: "+e.getMessage());
		}
		catch(TransformerException e)
		{
			throw new PrismException("Problem saving network settings: Transformer Exception: "+e.getMessage());
		}
	}
	
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
	{
		InputSource inputSource = null;
		
		// override the resolve method for the dtd
		if (systemId.endsWith("dtd"))
		{
			// get appropriate dtd from classpath
			InputStream inputStream = SimulatorNetworkHandler.class.getClassLoader().getResourceAsStream("dtds/prismnetwork.dtd");
			if (inputStream != null) inputSource = new InputSource(inputStream);
		}
		
		return inputSource;
	}
	
	public void notifyChanged(TreeNode node)
	{
		tableModel.fireTableDataChanged();
		setChanged();
		notifyObservers(node);
	}
	
	//TreeNode Methods
	
	public Enumeration children()
	{
		return fileSystems.elements();
	}
	
	public boolean getAllowsChildren()
	{
		return true;
	}
	
	public TreeNode getChildAt(int childIndex)
	{
		return (TreeNode)fileSystems.get(childIndex);
	}
	
	public int getChildCount()
	{
		return fileSystems.size();
	}
	
	public int getIndex(TreeNode node)
	{
		return fileSystems.indexOf(node);
	}
	
	public TreeNode getParent()
	{
		return null;
	}
	
	public boolean isLeaf()
	{
		return false;
	}
	
	//TableModel methods
	private AbstractTableModel tableModel = new AbstractTableModel()
	{
		
		public Class getColumnClass(int columnIndex)
		{
			if(columnIndex == 3) return JProgressBar.class;
			else return String.class;
		}
		
		public int getColumnCount()
		{
			return 5;
		}
		
		public String getColumnName(int columnIndex)
		{
			switch(columnIndex)
			{
				case 0: return "Name";
				case 1: return "Status";
				case 2: return "Iterations Total";
				case 3: return "Current Progress";
				case 4: return "Number of Starts";
				default: return "";
			}
		}
		
		public int getRowCount()
		{
			int count=0;
			for(int i = 0; i < getNumFileSystems(); i++)
			{
				count += getFileSystem(i).getNumHosts();
			}
			return count;
		}
		
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			SSHHost host = getHost(rowIndex);
			switch(columnIndex)
			{
				case 0: return host.getHostName();
				case 1:
				{
					String str = "";
					switch(host.getHostState())
					{
						case SSHHost.READY_UNKNOWN_STATUS:
						case SSHHost.READY_OKAY: str = "Ready"; break;
						case SSHHost.SENDING_FILES: str = "Sending Files...";break;
						case SSHHost.RUNNING: str = "Running..."; break;
						case SSHHost.RETRIEVING_FILES: str = "Retrieving Files..."; break;
						case SSHHost.ERROR: return "Error: "+host.getErrorMessage(); 
						case SSHHost.DONE: return "Done"; 
						case SSHHost.STOPPED: return "Stopped";
						default: return "";
					}
					if(host.isStopping()) str += " (stopping)";
					return str;
				}
				case 2: return ""+host.getTotalDone();
				case 3: return host.getProgressBar();
				case 4: return ""+host.getTotalStints();
				default: return "";
			}
		}
		
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return false;
		}
		
		
	};
	
	public void addTableModelListener(TableModelListener l)
	{
		tableModel.addTableModelListener(l);
	}
	
	public Class getColumnClass(int columnIndex)
	{
		return tableModel.getColumnClass(columnIndex);
	}
	
	public int getColumnCount()
	{
		return tableModel.getColumnCount();
	}
	
	public String getColumnName(int columnIndex)
	{
		return tableModel.getColumnName(columnIndex);
	}
	
	public int getRowCount()
	{
		return tableModel.getRowCount();
	}
	
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		return tableModel.getValueAt(rowIndex, columnIndex);
	}
	
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return tableModel.isCellEditable(rowIndex, columnIndex);
	}
	
	public void removeTableModelListener(javax.swing.event.TableModelListener l)
	{
		tableModel.removeTableModelListener(l);
	}
	
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		tableModel.setValueAt(aValue, rowIndex, columnIndex);
	}
	
	
	
	
	/**
	 *	Searches through the filesystems and returns the indexed SSHHost
	 */
	private SSHHost getHost(int index)
	{
		for(int i = 0; i < getNumFileSystems(); i++)
		{
			if(index >= getFileSystem(i).getNumHosts()) index -= getFileSystem(i).getNumHosts();
			else
			{
				return getFileSystem(i).getHost(index);
			}
		}
		return null;
	}
	
	private int getNumHosts()
	{
		int count = 0;
		for(int i = 0; i < getNumFileSystems(); i++)
		{
			count += getFileSystem(i).getNumHosts();
		}
		return count;
	}
	
}
