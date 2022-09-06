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

// TODO support to print scheduler instead of values
// TODO stop printing if infinity occurs as value, as plot will be unusable

package param;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

/**
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final class ResultExporter {
	class PointComparator implements Comparator<Point> {
		@Override
		public int compare(Point o1, Point o2) {
			int numDims = o1.size();
			for (int dim = numDims - 2; dim > 0; dim--) {
				int cmp = o1.getDimension(dim).compareTo(o2.getDimension(dim));
				if (cmp != 0) {
					return cmp;
				}
			}
			return 0;
		}
	}
	
	private RegionValues values;
	private BoxRegionFactory regionFactory;
	private OutputStream output;
	private int pointsPerDim;
	
	ResultExporter()
	{
		
	}
	
	void setRegionValues(RegionValues values)
	{
		this.values = values;
		this.regionFactory = (BoxRegionFactory) values.getRegionFactory();
	}
	
	void setOutputStream(OutputStream output)
	{
		this.output = output;
	}
	
	void setPointsPerDimension(int pointsPerDim)
	{
		this.pointsPerDim = pointsPerDim;
	}

	void print()
	{
		PrintStream print = new PrintStream(output);
		int numVariables = regionFactory.numVariables();
		boolean isBoolean = values.booleanValues();
		
		if (numVariables == 2 && isBoolean) {
			printBooleanTwoVars(print);
		} else if (numVariables == 2 && !isBoolean) {
			printFunctionTwoVars(print);
		} else {
			throw new RuntimeException("not implemented");
		}
				
		print.flush();
	}

	private void printBooleanTwoVars(PrintStream print)
	{
		print.println("\\begin{tikzpicture}[scale=5]");
		print.print("\\draw[rectangle,fill=gray] ");
		print.print("(" + regionFactory.getLowerBound(0).doubleValue());
		print.print("," + regionFactory.getLowerBound(1).doubleValue() + ")");
		print.print(" rectangle ");
		print.print("(" + regionFactory.getUpperBound(0).doubleValue());
		print.print("," + regionFactory.getUpperBound(1).doubleValue() + ");\n");
		for (Entry<Region,StateValues> entry : values) {
			boolean value = entry.getValue().getInitStateValueAsBoolean();
			BoxRegion region = (BoxRegion) entry.getKey();
			String fillColor = value ? "white" : "black";
			String drawColor = value ? "black" : "white";
			print.print("\\draw[rectangle,");
			print.print("fill=" + fillColor + ",");
			print.print("draw=" + drawColor + "] ");			
			print.print("(" + region.getDimensionLower(0).doubleValue());
			print.print("," + region.getDimensionLower(1).doubleValue() + ")");
			print.print(" rectangle ");
			print.print("(" + region.getDimensionUpper(0).doubleValue());
			print.print("," + region.getDimensionUpper(1).doubleValue() + ");\n");					
		}
		print.print("\\draw[rectangle,draw=black] ");
		print.print("(" + regionFactory.getLowerBound(0).doubleValue());
		print.print("," + regionFactory.getLowerBound(1).doubleValue() + ")");
		print.print(" rectangle ");
		print.print("(" + regionFactory.getUpperBound(0).doubleValue());
		print.print("," + regionFactory.getUpperBound(1).doubleValue() + ");\n");
		print.println("\\end{tikzpicture}");
	}
	
	private ArrayList<Point> samplePoints()
	{
		// compute step width
		int numVariables = regionFactory.numVariables();
		BigRational[] stepWidth = new BigRational[numVariables];
		for (int var = 0; var < numVariables; var++) {
			stepWidth[var] = regionFactory.sideWidth(var).divide(pointsPerDim - 1);
		}

		// compute point grid
		ArrayList<Point> points = new ArrayList<Point>();
		int numTotalPoints = (int) Math.pow(pointsPerDim, numVariables);
		for (int pointNr = 0; pointNr < numTotalPoints; pointNr++) {
			int pointRest = pointNr;
			BigRational[] point = new BigRational[numVariables];
			for (int dim = 0; dim < numVariables; dim++) {
				int dimPosNr = pointRest % pointsPerDim;
				pointRest /= pointsPerDim;
				BigRational vl = regionFactory.getLowerBound(dim).add(stepWidth[dim].multiply(dimPosNr));
				point[dim] = vl;
			}
			points.add(new Point(point));
		}
		
		// compute values of points contained in a region
		HashMap<Point,BigRational> pointValues = new HashMap<Point,BigRational>();
		for (Point point : points) {
			for (Entry<Region,StateValues> entry : values) {
				Region region = entry.getKey();
				Function value = entry.getValue().getInitStateValueAsFunction();
				if (region.contains(point)) {
					pointValues.put(point, value.evaluate(point));
					break;
				}
			}
		}
				
		// interpolate values of points not in a region
		HashSet<Function> allFunctions = new HashSet<Function>();
		for (Entry<Region,StateValues> entry : values) {
			allFunctions.add(entry.getValue().getInitStateValueAsFunction());
		}
		for (Point point : points) {
			if (!pointValues.containsKey(point)) {
				boolean finish = false;
				BigRational average = BigRational.ZERO;
				for (int distNr = 0; (distNr < pointsPerDim) && !finish; distNr++) {
					BigRational sum = BigRational.ZERO;
					int numFound = 0;
					for (int dim = 0; (dim < numVariables); dim++) {
						for (int direction = -1; direction <= 1; direction += 2) {
							BigRational offset = stepWidth[dim].multiply(dim).multiply(direction);
							BigRational[] dimensions = point.toArray();
							dimensions[dim] = dimensions[dim].add(offset);
							Point distPoint = new Point(dimensions);
							BigRational value = pointValues.get(distPoint);
							if (value != null) {
								finish = true;
								sum = sum.add(value);
								numFound++;
							}
						}
					}
					if (finish) {
						average = sum.divide(numFound);
					}
				}
				BigRational minDiff = new BigRational(1000);
				BigRational bestValue = new BigRational(1000);
				for (Function function : allFunctions) {
					BigRational newValue = function.evaluate(point);
					BigRational newDiff = average.subtract(newValue).abs();
					if (newDiff.compareTo(minDiff) == -1) {
						bestValue = newValue;
						minDiff = newDiff;
					}
				}
				pointValues.put(point, bestValue);
			}
		}		
		
		// combine points and their values in result list and sort
		ArrayList<Point> result = new ArrayList<Point>();
		for (Point point : points) {
			BigRational[] entry = new BigRational[numVariables + 1];
			for (int var = 0; var < numVariables; var++) {
				entry[var] = point.getDimension(var);
			}
			entry[numVariables] = pointValues.get(point);
			result.add(new Point(entry));
		}		
		PointComparator comparator = new PointComparator();
		Collections.sort(result, comparator);

		return result;
	}
	
	private void printFunctionTwoVars(PrintStream print)
	{
		print.println("\\begin{tikzpicture}");
		print.println("\\begin{axis}[");
		print.println("  view={30}{30}");
//		print.println("  x tick label style={right=1pt},");
//		print.println("  xtick={0,0.2,0.4,0.6,0.8,1.0},");
//		print.println("  ytick={0.2}");
		print.println("  ]");
		print.println("  \\addplot3[mesh,color=black] coordinates {");
		ArrayList<Point> points =  samplePoints();
		BigRational lastY = points.get(0).getDimension(1);
		for (Point point : points) {
			if (!lastY.equals(point.getDimension(1))) {
				print.print("\n\n");
				lastY = point.getDimension(1);
			}
			print.print(" (");
			for (int dim = 0; dim < point.size(); dim++) {
				print.print(point.getDimension(dim).doubleValue());
				if (dim < point.size() - 1) {
					print.print(",");
				}
			}
			print.print(")");
		}
		print.println();
		print.println("  };");
		print.println("\\end{axis}");
		print.println("\\end{tikzpicture}");
	}
}
