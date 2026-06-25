// ==============================================================================
//
// Copyright (c) 2026-
// Authors:
// * Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//
// ------------------------------------------------------------------------------
//
// This file is part of PRISM.
//
// PRISM is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// PRISM is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with PRISM; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// ==============================================================================

package solver;

import prism.PrismException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Registry of available LP solver backends, discovered via Java SPI.
 * Any jar on the classpath that contains a META-INF/services/solver.LPSolverFactory
 * entry is picked up automatically — no code changes needed to register new backends.
 */
public class LPSolverRegistry
{
	private static Map<String, LPSolverFactory> factories;

	private static synchronized void init()
	{
		if (factories != null)
			return;
		factories = new LinkedHashMap<>();
		for (LPSolverFactory f : ServiceLoader.load(LPSolverFactory.class, LPSolverFactory.class.getClassLoader())) {
			factories.put(f.getId(), f);
		}
	}

	/**
	 * Create an LP solver with the given id and number of variables.
	 * Throws a descriptive error if the id is not registered.
	 */
	public static LPSolver create(String id, int numVars) throws PrismException
	{
		init();
		LPSolverFactory f = factories.get(id);
		if (f == null) {
			throw new PrismException("Unknown LP solver '" + id + "'. Available: " + factories.keySet());
		}
		return f.create(numVars);
	}

	/** IDs of all currently registered LP solver backends. */
	public static Set<String> getAvailableIds()
	{
		init();
		return java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(factories.keySet()));
	}
}
