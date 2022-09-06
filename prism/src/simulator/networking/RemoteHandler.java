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

public class RemoteHandler
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
	
	public static int ssh(String [] arguments) throws InterruptedException
	{
		String[]args = new String[arguments.length+2];
		args[0] = sshCommand;
		args[1] = "-x";
		for(int i = 0 ; i < arguments.length; i++)
		{
			args[i+2] = arguments[i];
		}
		
		for(int i = 0; i < args.length; i++)
		{
			System.out.print(args[i]+" ");
		}
		System.out.println();
		try
		{
			Process proc = runtime.exec(args);
			System.out.println("started");
			ErrorGobbler errorGobbler = new ErrorGobbler(proc.getErrorStream(), "ERR"+args[1], proc);
			StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUT"+args[1]);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
			errorGobbler.start();
			//outputGobbler.start();
			
			
			//System.out.println("doing ssh");
			int exitVal = proc.waitFor();
			System.out.println("ssh returns "+exitVal);
			errorGobbler.interrupt();
			outputGobbler.interrupt();
			return exitVal;
			
		}
		catch(IOException t)
		{
			//t.printStackTrace();
			return -1;
		}
	}
	
	public static int scp(String []arguments) throws InterruptedException
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
		System.out.println();
		
		
		try
		{
			Process proc = runtime.exec(args);
			//System.out.println(proc.toString());
			
			ErrorGobbler errorGobbler = new ErrorGobbler(proc.getErrorStream(), "<ERROR: "+args[1], proc);
			//StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUT"+args[1]);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
			errorGobbler.start();
			//outputGobbler.start();
			
			//System.out.println("doing scp");
			int exitVal = proc.waitFor();
			//System.out.println("scp returns "+exitVal);
			errorGobbler.interrupt();
			//outputGobbler.interrupt();
			return exitVal;
			
		}
		catch(IOException t)
		{
			//t.printStackTrace();
			return -1;
		}
	}
	
	static class StreamGobbler extends Thread
	{
		InputStream is;
		String type;
		OutputStream os;

		StreamGobbler(InputStream is, String type)
		{
			this(is, type, null);
		}

		StreamGobbler(InputStream is, String type, OutputStream redirect)
		{
			this.is = is;
			this.type = type;
			this.os = redirect;
		}

		public void run()
		{
			try
			{
				PrintWriter pw = null;
				if (os != null)
					pw = new PrintWriter(os);

				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line=null;
				while ( (line = br.readLine()) != null && !interrupted())
				{
					if (pw != null)
						pw.println(line);
					if (pw != null)
						pw.flush();
					System.out.println(type + ">" + line);
				}
				if (pw != null)
					pw.flush();
			}
			catch (IOException ioe)
			{
				//ioe.printStackTrace();
			}
		}
	}

	static class ErrorGobbler extends Thread
	{
		InputStream is;
		String type;
		OutputStream os;
		Process proc;

		ErrorGobbler(InputStream is, String type, Process proc)
		{
			this(is, type, null, proc);
		}

		ErrorGobbler(InputStream is, String type, OutputStream redirect, Process proc)
		{
			this.is = is;
			this.type = type;
			this.os = redirect;
			this.proc = proc;
		}

		public void run()
		{
			try
			{
				PrintWriter pw = null;
				if (os != null)
					pw = new PrintWriter(os);

				InputStreamReader isr = new InputStreamReader(is);


				BufferedReader br = new BufferedReader(isr);
				String line=null;
				//System.out.println("1");
				//while (!br.ready() && !interrupted())// (line = br.readLine()) != null && !interrupted())
				while((line = br.readLine()) != null && !interrupted())
				{
					if (pw != null)
					pw.println(line);
				//System.out.println("3");
				if (pw != null)
					pw.flush();
					proc.destroy();
				}
				/*//System.out.println("2");
				if (pw != null)
					pw.println(line);
				//System.out.println("3");
				if (pw != null)
					pw.flush();
				//System.out.println("4");
				//System.out.println(type + ">" + line);
				//System.out.println("5");
				proc.destroy();*/


				//}
				System.out.println("6");
				//if (pw != null)
				//    pw.flush();
			}
			catch (IOException ioe)
			{
				//ioe.printStackTrace();
			}
		}
	}
}
