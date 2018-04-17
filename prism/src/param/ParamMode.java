//==============================================================================
//	
//	Copyright (c) 2017-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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


package param;

/** Mode (parametric / exact) */
public enum ParamMode {
	/** Parametric analysis mode for the parametric engine */
	PARAMETRIC("parametric"),
	/** Exact analysis mode (i.e., using constant functions) for the parametric engine */
	EXACT("exact");

	/** The mode name */
	private final String name;

	/** Private constructor */
	private ParamMode(String name)
	{
		this.name = name;
	}

	/** Get either "parametric" or "exact", depending on mode. */
	public String toString()
	{
		return name;
	}

	/** Get "parametric engine" or "exact engine", depending on mode. */
	public String engine()
	{
		return name + " engine";
	}

	/** Get "Parametric engine" or "Exact engine", depending on mode. */
	public String Engine()
	{
		return name.substring(0,1).toUpperCase() + name.substring(1) + " engine";
	}
}