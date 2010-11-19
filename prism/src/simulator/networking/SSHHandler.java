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

import java.io.*;
import java.util.*;
import prism.PrismException;

public class SSHHandler
{
	
	static String sshCommand = "ssh2";
	static String scpCommand = "scp2";
	static Runtime runtime;
	
	static
	{
		runtime = Runtime.getRuntime();
	}
	
	public static void configure(String shellCommand, String copyCommand)
	{
		sshCommand = shellCommand;
		scpCommand = copyCommand;
	}
	
	public static void ssh(String userName, String hostName, String command, String[]arguments) throws PrismException
	{
		String[]args = new String[arguments.length+4];
		args[0] = sshCommand;
		args[1] = "-x";
		args[2] = userName+"@"+hostName;
		args[3] = command;
		for(int i = 0 ; i < arguments.length; i++)
		{
			args[i+4] = arguments[i];
		}
		
		
		Process proc;
		try
		{
			proc = runtime.exec(args);
		}
		catch(IOException e)
		{
			throw new PrismException("Could not initiate ssh process on local machine.");
		}
		
		//Initialse and start a thread to catch the error stream for this ssh call.
		StringBuffer errorCollector = new StringBuffer("");
		ErrorGobbler errorThread = new ErrorGobbler(proc.getErrorStream(), proc, Thread.currentThread(), errorCollector);
		PingController pingThread = new PingController(userName, hostName, Thread.currentThread());
		errorThread.start();
		pingThread.start();
		
		try
		{
			int result = proc.waitFor();
			System.out.println("result = "+result);
			if(result != 0)
			{
				errorThread.interrupt();
				pingThread.interrupt();
				throw new PrismException("Unknown error executing "+command+ " on "+hostName);
			}
		}
		catch(InterruptedException e)
		{
			errorThread.interrupt();
			pingThread.interrupt();
			proc.destroy();
			if(errorCollector.toString().equals(""))
			{
				throw new PrismException("SSH process "+command+" on "+hostName+" has been stopped.");
			}
			else
				throw new PrismException("SSH process "+command+" on "+hostName+" has performed an error:\n"+errorCollector.toString());
		}
		
		errorThread.interrupt();
		pingThread.interrupt();
	}
	
	public static void scp(String userName, String hostName, String[]arguments) throws PrismException
	{
		String[]args = new String[arguments.length+1];
        args[0] = scpCommand;
        for(int i = 0 ; i < arguments.length; i++)
        {
            args[i+1] = arguments[i];
        }
        
        for(int i = 0; i < args.length; i++)
        {
            System.out.print(args[i]+" ");
        }
		
		
		Process proc;
		try
		{
			proc = runtime.exec(args);
		}
		catch(IOException e)
		{
			throw new PrismException("Could not initiate scp process on local machine.");
		}
		
		//Initialse and start a thread to catch the error stream for this ssh call.
		StringBuffer errorCollector = new StringBuffer("");
		ErrorGobbler errorThread = new ErrorGobbler(proc.getErrorStream(), proc, Thread.currentThread(), errorCollector);
		PingController pingThread = new PingController(userName, hostName, Thread.currentThread());
		errorThread.start();
		pingThread.start();
		
		try
		{
			int result = proc.waitFor();
			System.out.println("result = "+result);
			if(result != 0)
			{
				errorThread.interrupt();
				pingThread.interrupt();
				throw new PrismException("Unknown error executing scp command ");
			}
		}
		catch(InterruptedException e)
		{
			errorThread.interrupt();
			pingThread.interrupt();
			proc.destroy();
			if(errorCollector.toString().equals(""))
			{
				throw new PrismException("SCP process has been stopped.");
			}
			else
				throw new PrismException("SCP process has performed an error:\n"+errorCollector.toString());
		}
		
		errorThread.interrupt();
		pingThread.interrupt();
	}
	
	public static void main(String[]args)
	{
		Thread t = new Thread()
		{
			public void run()
			{
				try
				{
					String[]args =
					{	"~/prismnet/binary.bin",
						"~/prismnet/resandrew.txt",
						"1000000",
						"1000",
						"~/prismnet/control.txt",
						"~/prismnet/progress.txt",
						"~/prismnet/resultsProgress.txt"
					};
					ssh("ug60axh", "acws-3414", "~/prism/bin/prismsimulator", args);
				}
				catch(PrismException e)
				{
					System.out.println("MAIN THREAD CAUSED PRISM EXCEPTION: "+e.getMessage());
					return;
				}
				System.out.println("SUCCESSFUL OUTPUT");
			}
		};
		t.start();
	}
	
	static class ErrorGobbler extends Thread
	{
		InputStream is;
		Process proc;
		Thread owner;
		StringBuffer errorOut;
		boolean isPing;

		ErrorGobbler(InputStream is, Process proc, Thread owner, StringBuffer errorOut)
		{
			this(is, proc, owner, errorOut, false);
		}

		ErrorGobbler(InputStream is, Process proc, Thread owner, StringBuffer errorOut, boolean isPing)
		{
			this.is = is;
			this.proc = proc;
			this.owner = owner;
			this.errorOut = errorOut;
			this.isPing = isPing;
		}

		public void run()
		{
			BufferedInputStream bis = new BufferedInputStream(is);
			try
			{
				char ch = (char) bis.read();
				while(!interrupted() && ch != -1)
				{
					errorOut.append(ch);
					ch = (char) bis.read();
					proc.destroy();
				}
				errorOut.append(ch);
			}
			catch(IOException e)
			{
			}

			if(!isPing)owner.interrupt();
		}
	}

	static class PingController extends Thread
	{
		String userName;
		String hostName;
		Thread owner;
		Vector threads;

		PingController(String userName, String hostName, Thread owner)
		{
			this.userName = userName;
			this.hostName = hostName;
			this.owner = owner;
			threads = new Vector();
		}

		public void run()
		{
			try
			{
				while(!interrupted())
				{
					PingThread pt = new PingThread(userName, hostName, Thread.currentThread(), threads);
					pt.start();
					Thread.sleep(5000);
				}
			}
			catch(InterruptedException e)
			{
				owner.interrupt();
				while(true)
				{
					synchronized(threads)
					{
						if(threads.size() > 0)((Thread)threads.get(0)).interrupt();
						else break;
					}
				}
				return;
			}
		}
	}

	static class PingThread extends Thread
	{
		String userName;
		String hostName;
		Thread owner;
		Vector threads;

		PingThread(String userName, String hostName, Thread owner, Vector threads)
		{
			this.userName = userName;
			this.hostName = hostName;
			this.owner = owner;
			this.threads = threads;
			threads.add(this);
		}

		public void run()
		{

			Process proc;
			try
			{
				proc = SSHHandler.runtime.exec("ssh -x "+userName+"@"+hostName+" echo test");
			}
			catch(IOException e)
			{
				owner.interrupt();
				threads.remove(this);
				return;
			}

			//Initialse and start a thread to catch the error stream for this ssh call.
			StringBuffer errorCollector = new StringBuffer("");
			ErrorGobbler errorThread = new ErrorGobbler(proc.getErrorStream(), proc, Thread.currentThread(), errorCollector, false);
			errorThread.start();
			if(interrupted())
			{
				errorThread.interrupt();
				owner.interrupt();
				threads.remove(this);
				return;
			}
			try
			{
				int result = proc.waitFor();
				if(result != 0)
				{
					errorThread.interrupt();
					owner.interrupt();
				}
			}
			catch(InterruptedException e)
			{
				errorThread.interrupt();
				owner.interrupt();
			}

			errorThread.interrupt();
			threads.remove(this);
		}
	}
}
