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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
	 * The name of the environment variable that can be used
	 * to override the default main class (PrismCL).
	 */
	private final static String envNameNGMainClass = "NG_MAINCLASS";

	/**
	 * This method is called on the Nailgun server to start a new PRISM
	 * command-line instance to serve as the counterpart for a client invocation.
	 * <br/>
	 * This method is synchronized to ensure that there can only be a single
	 * running instance of PRISM in the Nailgun server VM.
	 * <br/>
	 * Sets the working directory to that of the client.
	 * <br/>
	 * If the environment variable NG_MAINCLASS is defined,
	 * the {@code main} method of that class is used instead of
	 * the default {@code PrismCL.main()}.
	 */
	public synchronized static void nailMain(NGContext context) throws InterruptedException
	{
		currentNailGunContext = context;
		if (PrismNative.setWorkingDirectory(context.getWorkingDirectory()) != 0) {
			System.err.println("Nailgun: Can not change working directory to " + context.getWorkingDirectory());
			System.exit(1);
		}

		String ngMainClass = null;
		try {
			ngMainClass = System.getenv(envNameNGMainClass);
		} catch (SecurityException e) {
			// ignore environment variable, proceed with default PRISM invocation
		}

		if (ngMainClass == null) {
			// default: start a PRISM command-line instance
			PrismCL.main(context.getArgs());
		} else {
			Class<?> clazz = null;
			try {
				clazz = context.getClass().getClassLoader().loadClass(ngMainClass);
			} catch (ClassNotFoundException e) {
				System.err.println("PrismNG: Can not find class '" + ngMainClass + "' (from environment variable " + envNameNGMainClass + ")");
				System.exit(1);
			}

			String[] args = context.getArgs();
			Method m = null;
			try {
				m = clazz.getMethod("main", args.getClass());
			} catch (NoSuchMethodException e) {
				System.err.println("PrismNG: Class '" + ngMainClass + "' (from environment variable " + envNameNGMainClass + ") does not have a suitable main method");
				System.exit(1);
			} catch (SecurityException e) {
				System.err.println("PrismNG: Security exception trying to load main method of class '" + ngMainClass + "' (from environment variable " + envNameNGMainClass + "): " + e.getMessage());
				System.exit(1);
			}
			try {
				m.invoke(null, new Object[]{args});
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				System.err.println("PrismNG: Exception trying to invoke main method of class '" + ngMainClass + "' (from environment variable " + envNameNGMainClass + "): " + e.getMessage());
				System.exit(1);
			}
		}
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
