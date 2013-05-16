//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

import java.util.ArrayList;

/**
 * Searches for bounds of minimal/maximal values.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final class Optimiser {
	RegionValues values;
	RegionValues filter;
	boolean min;

	public Optimiser(RegionValues values, RegionValues filter, boolean min) {
		this.values = values;
		this.filter = filter;
		this.min = min;
	}

	public String optimise() {
		int numStates = values.getResult(0).getNumStates();
		BigRational[] bounds = new BigRational[numStates];
		for (int state = 0; state < numStates; state++) {
			bounds[state] = min ? BigRational.INF : BigRational.MINF;
		}
		Point[] optPoints = new Point[numStates];
		
		RegionValuesIntersections co = new RegionValuesIntersections(values, filter);
		for (RegionIntersection inter : co) {
			Region region = inter.getRegion();
			StateValues value1 = inter.getStateValues1();
			StateValues value2 = inter.getStateValues2();
			for (int state = 0; state < value1.getNumStates(); state++) {
				ArrayList<Point> edges = region.specialPoints();
				for (Point edge : edges) {
					BigRational currentValue = value1.getStateValueAsFunction(state).evaluate(edge);
					if (value2.getStateValueAsBoolean(state)) {
						if (currentValue.compareTo(bounds[state]) == (min ? -1 : 1)) {
							bounds[state] = currentValue;
							optPoints[state] = edge;
						}
					}
				}
			}
		}
		
		BigRational initBound = bounds[values.getInitState()];
		
		return (min ? "at most " : "at least ") + initBound.doubleValue() + " at " + optPoints[values.getInitState()];
	}

}
