//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * TODO complete
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final class RahdConstraintChecker extends ConstraintChecker {
	// TODO read from PrismSettings
	final static String rahdBin = "/home/scratch/svn/pers-sb/rahd/rahd-bin";	

	public RahdConstraintChecker(int numRandomPoints) {
		super(numRandomPoints);
	}
	
	private boolean runRahd(Region region, String formula)
	{
		boolean ok = false;
		StringBuilder varBuilder = new StringBuilder();
		for (int var = 0; var < region.getDimensions(); var++) {
			varBuilder.append("x");
			varBuilder.append(var);
			varBuilder.append(" ");
		}
			
		String[] command = {"timeout", "5m", rahdBin, "-v", varBuilder.toString(), "-f", formula};
		Process p = null;
		try {
			p = Runtime.getRuntime().exec(command);

			BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = bri.readLine()) != null) {
				if (line.equals(" unsat")) {
					ok = true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ok;
	}
	
	private String buildRegionString(Region region)
	{
		BoxRegion boxRegion = (BoxRegion) region;
		StringBuilder result = new StringBuilder();
		for (int dim = 0; dim < region.getDimensions(); dim++) {
			result.append("x");
			result.append(dim);
			result.append(">=");
			result.append(boxRegion.getDimensionLower(dim));
			result.append(" /\\ ");
			result.append("x");
			result.append(dim);
			result.append("<=");
			result.append(boxRegion.getDimensionUpper(dim));
			if (dim < region.getDimensions() - 1) {
				result.append(" /\\ ");
			}
		}
		
		return result.toString();
	}
	
	@Override
	boolean mainCheck(Region region, Function poly, boolean strict) {
		String regionString = buildRegionString(region);

		return runRahd(region, regionString + " /\\ " + poly + (strict ? " <= 0" : " < 0"));
	}

}
