//==============================================================================
//	
//	Copyright (c) 2021-
//	Authors:
//	* Steffen Märcker <steffen.maercker@tu-dresden.de> (TU Dresden)
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

package userinterface.properties;

import java.util.Vector;

import parser.Values;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import prism.DefinedConstant;
import prism.Result;
import prism.ResultsCollection;
import prism.UndefinedConstants;

/**
 * A class the represents imported experiment results.
 * Since the results are already present, methods that run/control an experiment do nothing.
 */
public class GUIExperimentImported extends GUIExperiment
{
	/**
	 * Create an experiment from a result collection.
	 * As the results are already complete, the experiment is finished and serves as a mockup.
	 */
	public GUIExperimentImported(GUIExperimentTable table, GUIMultiProperties guiProp, Property property, ResultsCollection results)
	{
		super(table, guiProp, asPropertiesFile(property), new UndefinedConstants(property, results), false, results);
		experimentDone();
	}

	protected static PropertiesFile asPropertiesFile(Property property)
	{
		PropertiesFile properties = new PropertiesFile(null);
		properties.addProperty(property);
		return properties;
	}

	@Override
	public int getTotalIterations()
	{
		// For imported results, the last iteration is the number of total iterations.
		return results.getCurrentIteration();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector<DefinedConstant> getRangingConstants()
	{
		return results.getRangingConstants();
	}

	@Override
	public void startExperiment()
	{
		// Do nothing since experiment is already done.
	}

	@Override
	public synchronized void setResult(Values mfValues, Values pfValues, Result res)
	{
		// Do nothing since experiment is already done.
	}

	@Override
	public void stop()
	{
		// Do nothing since experiment is already done.
	}

}
