//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de (TU Dresden)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

package prism;

import java.util.Map;
import java.util.Map.Entry;

import com.martiansoftware.nailgun.NGContext;

/**
 * Entry point for Nailgun.
 */
public class PrismNG
{
	/** The current NailGun context */
	private static NGContext currentNailGunContext = null;

	/**
	 * This method is called on the Nailgun server to start a new PRISM instance
	 * to serve as the counterpart for a client invocation.
	 * <br/>
	 * This method is synchronized to ensure that there can only be a single
	 * running instance of PRISM in the Nailgun server VM.
	 * <br/>
	 * Sets the working directory to that of the client.
	 */
	public synchronized static void nailMain(NGContext context) throws InterruptedException
	{
		currentNailGunContext = context;
		if (PrismNative.setWorkingDirectory(context.getWorkingDirectory()) != 0) {
			System.err.println("Nailgun: Can not change working directory to " + context.getWorkingDirectory());
			System.exit(1);
		}

		PrismCL.main(context.getArgs());
	}

	/**
	 * Modifies the environment of the ProcessBuilder by adding the environment
	 * variables passed by the Nailgun client.
	 * <br/>
	 * If we are not running in a Nailgun context, this is a no-op.
	 * */
	public static void setupChildProcessEnvironment(ProcessBuilder builder)
	{
		if (currentNailGunContext == null)
			return;

		Map<String, String> env = builder.environment();
		for (Entry<Object, Object> entry : currentNailGunContext.getEnv().entrySet()) {
			if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
				env.put((String) entry.getKey(), (String) entry.getValue());
			}
		}
	}
}
