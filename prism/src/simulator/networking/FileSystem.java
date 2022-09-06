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

import prism.*;
import settings.*;

public class FileSystem implements SettingOwner, TreeNode
{
	private SimulatorNetworkHandler network;
	
	private SingleLineStringSetting fsName;
	private SingleLineStringSetting inputDir;
	private SingleLineStringSetting outputDir;
	
	private Vector hosts;
	
	/** Creates a new instance of FileSystem */
	public FileSystem(SimulatorNetworkHandler network, String name, String inputDir,
	String outputDir)
	{
		this.network = network;
		this.fsName = new SingleLineStringSetting("name", name, "A convienience name to identify this network cluster.", this, true);
		this.inputDir = new SingleLineStringSetting("simulator input directory", inputDir, "The location on the network cluster's filesystem to which the binary file containing information about the verification task will be sent.", this, true);
		this.outputDir = new SingleLineStringSetting("simulator output directory", outputDir, "The location on the network cluster's filesystem to which verification results will be written.", this, true);
		
		
		hosts = new Vector();
	}
	
	//Access methods
	public String getName()
	{
		return fsName.getStringValue();
	}
	
	public String getInputDir()
	{
		return inputDir.getStringValue();
	}
	
	public String getOutputDir()
	{
		return outputDir.getStringValue();
	}
	
	public String toString()
	{
		return getSettingOwnerClassName() +" "+getSettingOwnerName();
	}
	
	//SettingOwner methods
	
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
	
	SettingDisplay display;
	
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
			case 0: return fsName;
			case 1: return inputDir;
			default: return outputDir;
		}
	}
	
	public String getSettingOwnerClassName()
	{
		return "Network Cluster";
	}
	
	public int getSettingOwnerID()
	{
		return PropertyConstants.FILESYSTEM;
	}
	
	public String getSettingOwnerName()
	{
		return getName();
	}
	
	public void notifySettingChanged(Setting setting)
	{
		notifyChange(this);
	}
	
	public void setDisplay(SettingDisplay display)
	{
		this.display = display;
	}
	
	public void addHost(String hostName, String userName)
	{
		SSHHost host = new SSHHost(hostName, userName, this);
		hosts.add(host);
	}
	
	public void deleteHost(SSHHost host)
	{
		hosts.remove(host);
		network.notifyChanged(this);
	}
	
	public SSHHost getHost(int index)
	{
		return (SSHHost)hosts.get(index);
	}
	
	public int getNumHosts()
	{
		return hosts.size();
	}
	
	/**
	 *	This method attempts to send the appropriate files to the filesystem
	 *	by connecting to each host one by one and trying to send.
	 */
	public void sendFiles(File simBinary) throws PrismException
	{
		boolean sent = false;
		//System.out.println("Sending files to filesystem "+name);
		int index = 0;
		while(!sent && index < hosts.size())
		{
			try
			{
				//System.out.println("trying to send");
				getHost(index).sendFilesToFileSystem(simBinary);
				sent = true;
			}
			catch(PrismException e)
			{
				//System.out.println("error "+e);
				e.printStackTrace();
			}
			index++;
		}
		if(!sent)
		{
			throw new PrismException("Could not connect to any hosts on fileserver "+getName());
		}
	}
	
	/**
	 *	Checks each host, if at least one is running then returns true
	 */
	public boolean stillRunning()
	{
		boolean stillRunning = false;
		for(int i = 0 ; i < hosts.size() && !stillRunning; i++)
		{
			stillRunning = getHost(i).getHostState() == SSHHost.RUNNING || getHost(i).getHostState() == SSHHost.READY_OKAY;
		}
		return stillRunning;
	}
	
	//TreeNode methods
	
	public Enumeration children()
	{
		return hosts.elements();
	}	
	
	public boolean getAllowsChildren()
	{
		return true;
	}	
	
	public TreeNode getChildAt(int childIndex)
	{
		return (TreeNode)hosts.get(childIndex);
	}
	
	public int getChildCount()
	{
		return hosts.size();
	}
	
	public int getIndex(TreeNode node)
	{
		return hosts.indexOf(node);
	}
	
	public TreeNode getParent()
	{
		return network;
	}
	
	public boolean isLeaf()
	{
		return false;
	}
	
	public void notifyChange(TreeNode node)
	{
		network.notifyChanged(node);
	}
	
}


