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
import java.io.*;
import javax.swing.tree.*;
import javax.swing.*;

import prism.*;
import settings.*;

public class SSHHost extends Thread implements SettingOwner, TreeNode
{
	//state constants
	public static final int READY_UNKNOWN_STATUS = 0;
	public static final int READY_OKAY = 1;
	public static final int SENDING_FILES = 2;
	public static final int RUNNING = 3;
	public static final int TESTING = 4;
	public static final int RETRIEVING_FILES = 5;
	public static final int DONE = 6;
	public static final int ERROR = 7;
	public static final int STOPPED = 8;
	
	//owning filesystem
	private FileSystem owner;
	
	//information for this host
	private SingleLineStringSetting hostName;
	private SingleLineStringSetting userName;
	
	//State
	private int state = READY_UNKNOWN_STATUS;
	private String errorMessage = "";
	
	//Current execution progress
	private int totalDone;
	private int totalStints;
	private ArrayList resultFiles;
	
	File localResults; //TO-DO deal with results handling
	
	//Current stint/job
	private int noToDoThisStint;
	private int noDoneThisStint; //progress
	
	//Feedback information
	private boolean doFeedback = false;
	private boolean doFeedbackResults = false;
	private String feedbackName = "";
	private String feedbackResults = "";
	private String controlName = "";
	private JProgressBar progressBar;
	private File tempResults;
	private File controlFile;
	private boolean stopping = false;
	
	//Threads
	private Thread testThread = null;
	private Thread feedbackThread = null;
	private Thread stintThread = null;
	
	//because we cannot call 'this' from within a nested class
	private SSHHost instance;
	
	public SSHHost(String hostName, String userName, FileSystem owner)
	{
		super();
		
		instance = this;
		this.owner = owner;
		
		this.hostName =
				new SingleLineStringSetting(
				"host name",
				hostName,
				"This value must be the name used to connect to a host on the relevant network cluster.  This must be in the form of an IP address, or an name which can be resolved to an IP address from your version of PRISM.",
				this, true);
		
		this.userName =
				new SingleLineStringSetting(
				"user name",
				userName,
				"This value must be a valid username for this host.  The host must also be setup for passwordless access using this username.",
				this, true);
		
		progressBar = new JProgressBar();
		
		resetAll();
	}
	
	//State controllers
	
	public void resetAll()
	{
		setState(READY_UNKNOWN_STATUS);
		totalDone = 0;
		totalStints = 0;
		localResults = null;
		
		noToDoThisStint = 0;
		noDoneThisStint = 0;
		
		resultFiles = new ArrayList();
		
		owner.notifyChange(this);
	}
	
	private synchronized void setState(int state)
	{
		this.state = state;
		owner.notifyChange(this);
	}
	
	public synchronized boolean isStopping()
	{
		return stopping;
	}
	
	public synchronized void doneSuccessfulStint()
	{
		totalDone += noDoneThisStint;
		totalStints++;
		noToDoThisStint = 0;
		noDoneThisStint = 0;
		resultFiles.add(localResults);
		resultsChanged = true;
		tempResults = null;
		//deal with results TO-DO
		setState(READY_OKAY);
	}
	
	private synchronized void setNoDoneThisStint(int noDone)
	{
		this.noDoneThisStint = noDone;
		progressBar.repaint();
		owner.notifyChange(this);
	}
	
	public void setErroneousStintToZero()
	{
		this.noDoneThisStint = 0;
		this.noToDoThisStint = 0;
		owner.notifyChange(this);
	}
	
	
	//Access Methods
	
	public File getResultsFile(int i)
	{
		return (File)resultFiles.get(i);
	}
	
	public String getHostName()
	{
		return hostName.getStringValue();
	}
	
	public String getUserName()
	{
		return userName.getStringValue();
	}
	
	public String toString()
	{
		return "Host "+getHostName();
	}
	
	public synchronized int getHostState()
	{
		return state;
	}
	
	public synchronized String getErrorMessage()
	{
		return errorMessage;
	}
	
	public synchronized int getTotalDone()
	{
		return totalDone;
	}
	
	public synchronized int getTotalStints()
	{
		return totalStints;
	}
	
	public synchronized int getNoToDoThisStint()
	{
		return noToDoThisStint;
	}
	
	public synchronized int getNoDoneThisStint()
	{
		return noDoneThisStint;
	}
	
	private boolean resultsChanged = false;
	
	public synchronized boolean haveResultsChanged()
	{
		return resultsChanged;
	}
	
	public synchronized void resultsGot()
	{
		resultsChanged = false;
	}
	
	
	public synchronized JProgressBar getProgressBar()
	{
		progressBar.setMaximum(getNoToDoThisStint());
		progressBar.setMinimum(0);
		progressBar.setValue(getNoDoneThisStint());
		progressBar.setStringPainted(true);
		progressBar.setString(getNoDoneThisStint()+"/"+getNoToDoThisStint());
		
		return progressBar;
	}
	
	public synchronized boolean hasTempResults()
	{
		return tempResults != null;
	}
	
	public synchronized File getTempResults()
	{
		return tempResults;
	}
	
	//Execution methods
	
	/**
	 *	This method obtains the precompiled simulator executable and sends it to
	 *	this host's filesystem (network cluster) via scp.  If this is successful,
	 *	the binary file 'simBinary' is also sent to this host's network cluster
	 *	filesystem.
	 */
	public void sendFilesToFileSystem(File simBinary) throws PrismException
	{
		
		//URL executable = ClassLoader.getSystemResource("bin/"+SimulatorNetworkHandler.EXECUTABLE);
		//System.out.println("executable = "+executable.toString());
		
		
		setState(SENDING_FILES);
		int result = 0;
		try
		{
			String[] parameters =
			{"-p",
					 "bin/"+SimulatorNetworkHandler.EXECUTABLE, //to-do proper classpath
					 getUserName()+"@"+getHostName()+":"+owner.getInputDir()
			};
			System.out.println("send1");
			SSHHandler.scp(getUserName(), getHostName(), parameters); //scp the executable
			System.out.println("send2");
			String [] parameters2 =
			{"-p",
					 simBinary.getPath(),
					 getUserName()+"@"+getHostName()+":"+owner.getInputDir()
			};
			
			System.out.println("send3");
			SSHHandler.scp(getUserName(), getHostName(), parameters2);
			
			System.out.println("send4");
		}
		catch(PrismException ex)
		{
			errorMessage = ex.getMessage();
			setState(ERROR);
			throw ex;
		}
		setState(READY_OKAY);
		
	}
	
	public void startStint(int noIterations, long maxPathLength, String binaryName, boolean doFeedback, boolean resultsFeedback)
	{
		if(getHostState() != READY_OKAY && getHostState() != READY_UNKNOWN_STATUS) return; //must be ready
		
		noToDoThisStint = noIterations;
		noDoneThisStint = 0;
		
		setState(RUNNING);
		if(doFeedback || resultsFeedback)
		{
			this.doFeedback = doFeedback;
			this.doFeedbackResults = resultsFeedback;
			
			feedbackName = "feedbackFile"+System.currentTimeMillis()+".txt";
			if(resultsFeedback)
				feedbackResults = "feedbackResultsFile"+System.currentTimeMillis()+".txt";
			feedbackThread = new StintFeedbackThread(doFeedback, resultsFeedback);
			feedbackThread.start();
			
		}
		else
		{
			feedbackName = "";
		}
		
		controlName = "control"+binaryName+System.currentTimeMillis();
		
		
		stintThread = new StintThread(binaryName, controlName, maxPathLength);
		stopping = false;
		stintThread.start();
	}
	
	public void stopStint() throws PrismException
	{
		Thread stopThread = new Thread(new Runnable()
		{
			public void run()
			{
				if(getHostState() == RUNNING)
				{
					//The aim of this method is to send a stop request to the control file
					//The running thread should deal with everything else.
					String cFile = controlName;
					
					try
					{
						//somewhere to store the results when they are retrieved
						controlFile = File.createTempFile(cFile, ".txt");
					}
					catch(IOException ee)
					{
						return;
						//throw new PrismException("Local machine cannot create a control temporary file.");
					}
					try
					{
						PrintWriter pw = new PrintWriter(new FileWriter(controlFile));
						pw.write("STOP");
						pw.flush();
						pw.close();
					}
					catch(IOException eee)
					{
						return;
						//throw new PrismException("Could not write to stop control file.");
					}
					//TEST Results directory
					String[]parameters2 =
					{
						"-p",
								controlFile.getPath(),
								getUserName()+"@"+getHostName()+":"+owner.getInputDir()+"/"+controlName+".txt"
					};
					// do the scp call
					try
					{
						SSHHandler.scp(getUserName(), getHostName(), parameters2);
					}
					catch(PrismException e)
					{
						return;
					}
					stopping = true;
				}
			}
		});
		stopThread.start();
	}
	
	public void testHost()
	{
		testThread = new TestThread();
		testThread.start();
	}
	
	
	//Threads
	
	class TestThread extends Thread
	{
		
		public void run()
		{
			if(getHostState() != READY_UNKNOWN_STATUS && getHostState() != READY_OKAY && getHostState() != ERROR) return; //check we can do the test
			
			
			try
			{
				setState(TESTING);
				
				//first test whether we can connect to the host
				String[]parameters =
				{
					"test"
				};
				SSHHandler.ssh(getUserName(), getHostName(), "echo", parameters);
				
				//Setup a file to test the simulator input/output directories
				String testFile = "test123123"+System.currentTimeMillis();
				File file = File.createTempFile(testFile, ".txt");
				
				//Test input directory by copying a file into it
				String[]parameters2 =
				{
					"-p",
							file.getPath(),
							getUserName()+"@"+getHostName()+":"+owner.getInputDir()+"/"+testFile+".txt"
				};
				// do the scp call
				SSHHandler.scp(getUserName(), getHostName(), parameters2);
				
				//Tidy up input directory again
				String[]parameters3 =
				{
					"-f", owner.getInputDir()+"/"+testFile+".txt"
				};
				//do ssh call
				SSHHandler.ssh(getUserName(), getHostName(), "rm", parameters3);
				
				//Test output directory by creating a file in it, and copying it here`
				String[]parameters4 =
				{
					"test", owner.getOutputDir()+"/"+testFile+".txt"
				};
				SSHHandler.ssh(getUserName(), getHostName(), "echo", parameters4);
				String[]parameters5 =
				{
					"-p",
							getUserName()+"@"+getHostName()+":"+owner.getOutputDir()+"/"+testFile+".txt",
							file.getPath()
				};
				SSHHandler.scp(getUserName(), getHostName(), parameters5);
				
				//Tidy up output directory again
				String[]parameters6 =
				{
					"-f", owner.getOutputDir()+"/"+testFile+".txt"
				};
				//do ssh call
				SSHHandler.ssh(getUserName(), getHostName(), "rm", parameters3);
				
				if(file!=null) file.delete();
				
			}
			catch(PrismException ee)
			{
				errorMessage = ee.getMessage();
				setState(ERROR);
				return;
			}
			catch(IOException ee)
			{
				errorMessage = ee.getMessage();
				setState(ERROR);
				return;
			}
			
			setState(READY_OKAY);
			owner.notifyChange(instance);
		}
		
	}
	
	
	
	class StintThread extends Thread
	{
		private String binaryName;
		private String controlName;
		private long maxPathLength;
		
		public StintThread(String binaryName, String controlName, long maxPathLength)
		{
			this.binaryName = binaryName;
			this.controlName = controlName;
			this.maxPathLength = maxPathLength;
		}
		
		public void run()
		{
			//Set a unique name for the binary results file on the host filesystem
			String resultsFile = binaryName+userName+hostName+System.currentTimeMillis();
			
			//setup the ssh call to run the simulator executable
			String[] parameters;
			
			String command = owner.getInputDir()+"/"+SimulatorNetworkHandler.EXECUTABLE;
			
			if(feedbackName.equals(""))
			{
				parameters = new String[5];
				
				parameters[0] = owner.getInputDir()+"/"+binaryName;
				parameters[1] = owner.getOutputDir()+"/"+resultsFile+".txt";
				parameters[2] = ""+getNoToDoThisStint();
				parameters[3] = ""+maxPathLength;
				parameters[4] = owner.getInputDir()+"/"+controlName+".txt";
			}
			else if(feedbackResults.equals(""))
			{
				parameters = new String[6];
				
				parameters[0] = owner.getInputDir()+"/"+binaryName;
				parameters[1] = owner.getOutputDir()+"/"+resultsFile+".txt";
				parameters[2] = ""+getNoToDoThisStint();
				parameters[3] = ""+maxPathLength;
				parameters[4] = owner.getInputDir()+"/"+controlName+".txt";
				parameters[5] = owner.getOutputDir()+"/"+feedbackName;
			}
			else
			{
				parameters = new String[7];
				
				parameters[0] = owner.getInputDir()+"/"+binaryName;
				parameters[1] = owner.getOutputDir()+"/"+resultsFile+".txt";
				parameters[2] = ""+getNoToDoThisStint();
				parameters[3] = ""+maxPathLength;
				parameters[4] = owner.getInputDir()+"/"+controlName+".txt";
				parameters[5] = owner.getOutputDir()+"/"+feedbackName;
				parameters[6] = owner.getOutputDir()+"/"+feedbackResults;
			}
			
			
			//do the ssh call
			
			
			try
			{
				setState(RUNNING);
				//do the ssh call
				SSHHandler.ssh(getUserName(), getHostName(), command, parameters);
				
				//local storage for results
				localResults = File.createTempFile(resultsFile, ".txt");
				
				//transfer results
				String[]parameters2 =
				{
					"-p",
							getUserName()+"@"+getHostName()+":"+owner.getOutputDir()+"/"+resultsFile+".txt",
							localResults.getPath()
				};
				setState(RETRIEVING_FILES);
				//do the scp call
				SSHHandler.scp(getUserName(), getHostName(), parameters2);
				
				//tidy up the host
				String[]parameters3 =
				{
					"-f", owner.getOutputDir()+"/"+resultsFile+".txt"
				};
				SSHHandler.ssh(getUserName(), getHostName(), "rm",  parameters3);
				
				
			}
			catch(PrismException ee)
			{
				if(getHostState() == RUNNING) errorMessage = "Error when executing simulator on "+getHostName()+"\n"+ee.getMessage();
				else if(getHostState() == RETRIEVING_FILES) errorMessage = "Error when retrieving files from "+getHostName()+"\n"+ee.getMessage();
				else errorMessage = ee.getMessage();
				setState(ERROR);
				if(!stopping) return;
			}
			catch(IOException ee)
			{
				errorMessage = ee.getMessage();
				setState(ERROR);
				return;
			}
			
			
			if(stopping)
			{
				//look at results file for how many iterations have been done.
				SimulatorResultsFile re = new SimulatorResultsFile();
				try
				{
					re.mergeResultsFile(localResults);
					if(re.getNumResults() > 0)
						noDoneThisStint = re.getIterations(0);
					else
						noDoneThisStint = 0;
					
					owner.notifyChange(instance);
				}
				catch(PrismException e)
				{
					noDoneThisStint = 0;
				}
				setState(STOPPED);
			}
			else
			{
				noDoneThisStint = noToDoThisStint;
				owner.notifyChange(instance);
				
				setState(DONE);
			}
			
			System.out.println(hostName+" is done");
			
		}
	}
	
	
	class StintFeedbackThread extends Thread
	{
		private boolean regular = false;
		private boolean results = false;
		
		public StintFeedbackThread(boolean regularFeedback, boolean resultsFeedback)
		{
			this.regular = regularFeedback;
			this.results = resultsFeedback;
		}
		
		public void run()
		{
			File localFeedback = null;
			File localResultsFeedback = null;
			try
			{
				localFeedback = File.createTempFile("localFeedback"+System.currentTimeMillis(), ".txt");
				localResultsFeedback = File.createTempFile("resultsFeedback"+System.currentTimeMillis(), ".txt");
			}
			catch(IOException e)
			{
				System.err.println("Warning: could not initialise feedback thread on "+getHostName());
				return;
			}
			
			try
			{
				while(getHostState() == RUNNING || getHostState() == SENDING_FILES)
				{
					Thread.sleep(100);
					
					//check that the host still exists
					String[]params =
					{
						"test"
					};
					// do the scp call
					SSHHandler.ssh(getUserName(), getHostName(), "echo", params);
					
					if(regular)
					{
						//setup the scp call to transfer the results from the host filesystem
						String[]parameters =
						{
							"-p",
									getUserName()+"@"+getHostName()+":"+owner.getOutputDir()+"/"+feedbackName,
									localFeedback.getPath()
						};
						// do the scp call
						SSHHandler.scp(getUserName(), getHostName(), parameters);
						
						try
						{
							
							BufferedReader reader = new BufferedReader(new FileReader(localFeedback));
							int done = Integer.parseInt(reader.readLine());
							int total = Integer.parseInt(reader.readLine());
							int finished = Integer.parseInt(reader.readLine());
							
							noDoneThisStint = done;
							owner.notifyChange(instance);
						}
						catch(FileNotFoundException ee)
						{
							//System.out.println("filenotfoundexception");
						}
						catch(IOException ee)
						{
							//System.out.println("ioexception");
						}
						catch(NumberFormatException ee)
						{
							//System.out.println("numberformatexception");
						}
						
						
					}
					
					if(results)
					{
						String[]parameters =
						{
							"-p",
									getUserName()+"@"+getHostName()+":"+owner.getOutputDir()+"/"+feedbackResults,
									localResultsFeedback.getPath()
						};
						// do the scp call
						SSHHandler.scp(getUserName(), getHostName(), parameters);
						
						
						resultsChanged = true;
						tempResults = localResultsFeedback;
						System.out.println("got result");
						owner.notifyChange(instance);
					}
				}
			}
			catch(PrismException ex)
			{
				stintThread.interrupt();
				return;
				//System.out.println("feedback thread interrupted");
			}
			catch(InterruptedException ee)
			{
				stintThread.interrupt();
				return;
			}
			tempResults = null;
			localFeedback.delete();
			localResultsFeedback.delete();
			
			//delete the files on the host
			String[]parameters =
			{
				"-f", owner.getOutputDir()+"/"+feedbackName
			}; //note if the tidy up doesn't work, it doesn't really matter.
			try
			{
				SSHHandler.ssh(getUserName(), getHostName(), "rm", parameters);
			}
			catch(PrismException e)
			{
				return;
			}
			String []parameters2 =
			{
				"-f", owner.getOutputDir()+"/"+feedbackResults
			};
			try
			{
				SSHHandler.ssh(getUserName(), getHostName(), "rm", parameters2);
			}
			catch(PrismException e)
			{
			}
			//TODO delete the file on the host
			
			
			//System.out.println("feedback thread ended");
		}
	}
	
	
	//TreeNode Methods
	
	public Enumeration children()
	{
		return null;
	}
	
	public boolean getAllowsChildren()
	{
		return false;
	}
	
	public TreeNode getChildAt(int childIndex)
	{
		return null;
	}
	
	public int getChildCount()
	{
		return 0;
	}
	
	public int getIndex(TreeNode node)
	{
		return -1;
	}
	
	public TreeNode getParent()
	{
		return owner;
	}
	
	public boolean isLeaf()
	{
		return true;
	}
	
	//Setting owner methods
	
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
		return 2;
	}
	
	public Setting getSetting(int index)
	{
		switch(index)
		{
			case 0: return hostName;
			default: return userName;
		}
	}
	
	public String getSettingOwnerClassName()
	{
		return "SSH Host";
	}
	
	public int getSettingOwnerID()
	{
		return PropertyConstants.SSHHOST;
	}
	
	public String getSettingOwnerName()
	{
		return getHostName();
	}
	
	public void notifySettingChanged(Setting setting)
	{
		if(state == ERROR || state == READY_OKAY) setState(READY_UNKNOWN_STATUS);
		owner.notifyChange(this);
	}
	
	public void setDisplay(SettingDisplay display)
	{
		this.display = display;
	}
	
}
