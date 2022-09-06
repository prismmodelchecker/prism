//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains auxiliary static methods used in multi-objective verification.
 */
public class MultiObjUtils
{
	/**
	 * Returns a point that represents a hyperplane that separates {@code point}
	 * from {@code computedPoints}. An LP solver is used to get the hyperplane, but it
	 * is expected that the number of points  in {@code computedPoints} is relatively small
	 * and so this approach should be efficient
	 * 
	 * @param point A point which should be at one direction from the separating hyperplane.
	 * @param computedPoints Set of points which should be at the other direction from the separating hyperplane.
	 * @return A vector orthogonal to the computed separating hyperplane.
	 * @throws PrismException When the LP solver throws an exception or returns an unexpected result, an exception with a related message is thrown.
	 */
	public static Point getWeights(Point point, List<Point> computedPoints) throws PrismException {
		int dim = point.getDimension();
		if (computedPoints == null || computedPoints.size() == 0) {
			boolean nonZero = false;
			for(int i = 0; i < dim; i++) {
				if (point.getCoord(i) != 0) {
					nonZero = true;
					break;
				}
			}			
			
			Point resPoint;
			if (nonZero) {
				resPoint = point.normalize();
			} else {
				double[] result = new double[dim];
				for(int i = 0; i < dim; i++) {
					result[i] = 1.0;
				}
				resPoint = (new Point(result)).normalize();
			}
			return resPoint;
		}
		
		int epsilonCoord = dim + 1;
		//int numVars = dim + 1;
		double[] d;
		try {
			lpsolve.LpSolve solver = lpsolve.LpSolve.makeLp(0,dim + 2);
			solver.setVerbose(lpsolve.LpSolve.CRITICAL);
			
			//for some strange reason the 0th element of an array is ignored by addConstraint
			d = new double[dim + 3];
			for (int i = 0; i< dim; i++) {
				d[i + 1] = point.getCoord(i);
			}
			
			d[dim + 2] = -1.0;
			
			//System.out.println(Arrays.toString(d));
			solver.addConstraint(d, lpsolve.LpSolve.GE, 0.0);
			
			for (Point p : computedPoints)
			{
				d = new double[dim + 3];
				for (int i = 0; i< dim; i++) {
					d[i + 1] = p.getCoord(i);
				}
				d[epsilonCoord] = 1;
				
				d[dim + 2] = -1.0;
				
				solver.addConstraint(d, lpsolve.LpSolve.LE, 0.0);
			}
			
			for(int i = 0; i < dim; i++) {
				d = new double[dim + 3];
				d[i + 1] = 1;
				
				solver.addConstraint(d, lpsolve.LpSolve.GE, 0.0);
			}
			
			d = new double[dim + 3];
			
			for(int i = 0; i < dim; i++) {
				d[i + 1] = 1;
			}

			solver.addConstraint(d, lpsolve.LpSolve.LE, 1.0);

			d = new double[dim + 3];
			d[epsilonCoord] = 1;
				
			solver.setBounds(dim + 2, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
			
			solver.setObjFn(d);
			solver.setMaxim();
			
			int r = solver.solve();
					
			if (r == lpsolve.LpSolve.INFEASIBLE) {
				System.out.println("The solution is infeasible");
				return null;
			} else if (r == lpsolve.LpSolve.UNBOUNDED) {
				//this should never happen
				throw new PrismException("The solution of a linear program is unbounded.");
			} if (r != lpsolve.LpSolve.OPTIMAL)
				//this should also never happen
				throw new PrismException("LpSolve returns an unexpected value: " + r);

			double[] resultSolver = solver.getPtrVariables();
			//solver.printLp();
			if (resultSolver[epsilonCoord-1] == 0.0) {
				//there is no _strictly_ separating hyperplane (i.e. one that doesn't touch any of the points)
				return null;
			}
			
			//solver.printLp();
		    //System.out.println("solver says:" + r);
		    //System.out.println("solver results:" + Arrays.toString(resultSolver));
				
			double[] result = new double[dim];
			//copy all coordinates and ignore auxiliary variables
			for(int i = 0; i < dim; i++)
				result[i] = resultSolver[i];
			
			Point resPoint = new Point(result).normalize();
			
			return resPoint;
		} catch (lpsolve.LpSolveException ex) {
			PrismException ex2 = new PrismException("lpsolve threw an exception: " + ex.getMessage());
			throw ex2;
		}
	}

	/**
	 * Prints an auxiliary graph file in gnuplot format. Points must be 2D.
	 * 
	 * NOTE: not very nice now, should be enhanced in the future...
	 * 
	 * @param target Current target
	 * @param points Corner points of the pareto curve
	 * @param directions Directions associated with the corner points.
	 * @param directory The directory where to store the file.
	 * @param index Counter that should be incremented with every call of this method, determines a suffix of the name of the file.
	 * @throws PrismException
	 */
	public static void printGraphFileDebug(Point target, List<Point> points, List<Point> directions, String directory, int index) throws PrismException {
		//System.out.println("Plotting " + index + ": " + points + ", " + directions);
		
		double padding = 1.2;
		double maxX = target.getCoord(0) * padding;
		double maxY = target.getCoord(1) * padding;
		for (Point p : points) {
			double x = p.getCoord(0) * padding;
			double y = p.getCoord(1) * padding;
			maxX = (maxX >= x) ? maxX : x;
			maxY = (maxY >= y) ? maxY : y;
		}
		
		try {
			FileWriter fw;
			if (target != null) {
				fw = new FileWriter(new File(directory, "dataT" + index + ".dat"));
				fw.write(target.getCoord(0) + " " + target.getCoord(1));
				fw.close();
			}
			
			fw = new FileWriter(new File(directory,"dataP" + index + ".dat"));
			for (Point p : points) {
				fw.write(p.getCoord(0) + " " + p.getCoord(1) + "\n");
			}
			fw.close();
			
			fw = new FileWriter(new File(directory, "plot" + index + ".plot"));
			fw.write("set parametric\n");
			fw.write("set trange [0:" + maxX + "]\n");
			fw.write("set xrange [0:" + maxX + "]\n");
			fw.write("set yrange [0:" + maxY + "]\n");
			fw.write("set term aqua\n");
			//fw.write("set output \"output" + index + ".ps\"\n");
			fw.write("plot ");
			if (target != null)
				fw.write("\"dataT" + index + ".dat\" with points pt 9, ");
			fw.write("\"< sort dataP" + index + ".dat\"  with linespoints pt 9 lw 3");
			for (int i = 0; i < points.size(); i++) {
				Point p = points.get(i);
				Point d = directions.get(i);
				double k = (p.getCoord(0)*d.getCoord(0)) + (p.getCoord(1)*d.getCoord(1));
				if (d.getCoord(1) > 0)
					fw.write(", t,(-" + d.getCoord(0) + "*t + " + k + ")/" + d.getCoord(1) + " with lines lc 3");
				else
					fw.write(", " + p.getCoord(0) + ",t with lines lc 3");
			}
			fw.close();
		} catch (IOException ex) {
			throw new PrismException("An IOException error occured when writing graph files (exception message: " + ex.getMessage() + ").");
		}
	}

	public static void exportPareto(TileList tl, String filename) throws PrismException {
		//System.out.println("Plotting " + index + ": " + points + ", " + directions);
		try {
			FileWriter fw;
			fw = new FileWriter(filename);
			fw.write(tl.toString());
			fw.close();
				} catch (IOException ex) {
			throw new PrismException("An IOException error occured when writing graph files (exception message: " + ex.getMessage() + ").");
		}
	}
	
	/**
	 * This method takes a list of computed points together with their associated directions, where points are possibly
	 * repeated, and returns a map in which to each point from {@code computedPoints} corresponds the associated direction
	 * from {@code directions} which has the most extreme slope. Which extreme is taken depends on the {@code horiz} parameter.
	 * <br>
	 * Note that this method considers two points as "equal" if the method {@code isCloseTo(Point)} returns {@code true}.
	 * So the returned map will not contain two keys which are too close to each other, which should be fine w.r.t. roundoff
	 * errors, but might theoretically cause trouble if the corner points of the Pareto curve are very close to each other.
	 * <br>
	 * It is guaranteed that if there are two points {@code p1} and {@code p2} in {@code computedPoints} such that
	 * {@code p1.isCloseTo{p2}} is {@code true} and {@code p1} occurs in {@code computedPoints} before
	 * {@code p2}, then {@code p1} will be given precedence when picking a representative for the key in the map. This is crucial
	 * when using this method together with {@link MultiObjUtils#removeDuplicities(List)}.
	 * 
	 * @param computedPoints The computed points of the Pareto curve
	 * @param directions The directions associated with the computed points.
	 * @param horiz If true, gets the one which is closest to horizontal, if false then the one which is closest to vertical.
	 * @return A map in which to each point from {@code computedPoints} is associated the most extreme direction from {@code directions}.
	 */
	protected static java.util.Map<Point, Point> fillDirections(List<Point> computedPoints, List<Point> directions, boolean horiz) {
		HashMap<Point, Point> map = new HashMap<Point, Point>();
		for(int i = 0; i < computedPoints.size(); i++) {
			Point p = computedPoints.get(i);
			Point dir = directions.get(i);
			boolean added = false;
			for (Point key : map.keySet()) {
				if (key.isCloseTo(p)) {
					double slopeNew = (dir.getCoord(1) == 0) ? Double.POSITIVE_INFINITY :  dir.getCoord(0)/dir.getCoord(1);
					double slopeOrig = (map.get(key).getCoord(1) == 0) ? Double.POSITIVE_INFINITY : map.get(key).getCoord(0)/map.get(key).getCoord(1);
					if ((horiz && slopeNew < slopeOrig) || (!horiz && slopeNew > slopeOrig)) {
						map.put(key, dir);
						break;
					}
				}
			}
				
			if (!added) {
				map.put(p, dir);
			}
		}
		
		return map;
	}

	/**
	 * This method takes a list of points and returns a sub-list in which points that are very close to
	 * each other (where {@code isCloseTo(Point)} is used to determine what is "very close") are removed.
	 * The order of elements is preserved.
	 * 
	 * It is guaranteed that if there are two points {@code p1} and {@code p2} in {@code list} such that
	 * {@code p1.isCloseTo{p2}} is {@code true} and {@code p1} occurs in {@code list} before
	 * {@code p2}, then {@code p1} will be given precedence when picking the point to keep. This is crucial
	 * when using this method together with {@link fillDirections}.
	 * @param list
	 * @return
	 */
	protected static List<Point> removeDuplicities(List<Point> list) {
		//TODO implement faster alg if we can be bothered.
		ArrayList<Point> ret = new ArrayList<Point>();
		if (list.size() == 0)
			return ret;
		
		ret.add(list.get(0));
		for (int i = 1; i < list.size(); i++) {
			Point p = list.get(i);
			
			//check if contains
			boolean contains = false;
			for(Point p2 : ret) {
				if (p2.isCloseTo(p)) {
					contains = true;
					break;
				}
			}
			
			if (!contains)
				ret.add(p);
		}
		return ret;
	}

	/**
	 * This method takes the set of some corner points of a pareto curve {@code computedPoints} together
	 * with associated orthogonal lines {@code directions} that determine current over-approximation,
	 * and returns the points which determine the over-approximation. (These points are in fact the intersecting
	 * points of the directions which are not covered by other points.)
	 * @param computedPoints Corner points of the Pareto curve
	 * @param directions Directions (orthogonal lines) associated with the corner points
	 * @return The list of points that together with {@code computedPoints} determine the over-approximation of the pareto curve
	 */
	protected static List<Point> upperBoundPoints(List<Point> computedPoints, List<Point> directions)
	{
		java.util.Map<Point, Point> directionMapHoriz = fillDirections(computedPoints, directions, true);
		java.util.Map<Point, Point> directionMapVert = fillDirections(computedPoints, directions, false);
		
		PermutedLexicographicComparator plc = new PermutedLexicographicComparator(new int[] {0,1}, new boolean[] {true, false});
		List<Point> sortedLP = removeDuplicities(computedPoints);
		Collections.sort(sortedLP, plc);
		
		//System.out.println("LP: " + sortedLP);
		
		ArrayList<Point> ret = new ArrayList<Point>();
		for (int i =0; i < sortedLP.size() - 1; i++) {			
			Point p1 = sortedLP.get(i);
			Point p2 = sortedLP.get(i+1);
			Point d1 = directionMapVert.get(p1);
			Point d2 = directionMapHoriz.get(p2);
			
			//System.out.println("p1: " + p1 + ", p2: " + p2);
			Point p;
			if (d2.getCoord(1) > Point.SMALL_NUMBER) {		
				double k1 = p1.getCoord(0)*d1.getCoord(0) + p1.getCoord(1)* d1.getCoord(1);
				double k2 = p2.getCoord(0)*d2.getCoord(0) + p2.getCoord(1)* d2.getCoord(1);
				
				double[][] eqns = {{d1.getCoord(0), d1.getCoord(1)}, {d2.getCoord(0), d2.getCoord(1)}};
				double[] coords =MultiObjUtils.solveEqns(eqns, new double[] {k1,k2});
				
				if (coords == null) //HACK this is if two numbers are too close
					continue;
				
				//System.out.println("returned: " + Arrays.toString(coords));
				
				p = new Point(coords);
			} else if (d1.getCoord(0) < Point.SMALL_NUMBER && d2.getCoord(1) < Point.SMALL_NUMBER) {
				p = new Point(new double [] {p2.getCoord(0), p1.getCoord(1)});
				//System.out.println("returned': " + p);
			} else {
				double x = p2.getCoord(0);
				double k1 = p1.getCoord(0)*d1.getCoord(0) + p1.getCoord(1)* d1.getCoord(1);
				//System.out.println("x: " + x + ", k1: " + k1 + ", d1[0]: " + d1.getCoord(0) + ", d1[1]: " + d1.getCoord(1));
				double y = (k1 - d1.getCoord(0)*x)/d1.getCoord(1);
				p = new Point(new double[] {x,y});
				//System.out.println("returned'': " + p.toString());
			}
			
			//check if this point is in computedPoints
			boolean isCornerPoint = false;
			for (Point pc : computedPoints)
				if (pc.isCloseTo(p)) {
					isCornerPoint = true;
					break;
				}
					
			if (!isCornerPoint)
				ret.add(p);
		}
		return ret;
	}

	/**
	 * Returns new point for computation of two dimensional pareto curve. More formally,
	 * this method takes the points on the pareto curve computed so far, together with
	 * the directions (i.e. orthogonal hyperplanes) corresponding to the points, and
	 * returns a point which can still be in a pareto curve, and which is most distant
	 * from the points which are already known to be on the pareto curve. Here, most distant
	 * means that a point (x,y) is taken such that for every point (a,b) from the pareto
	 * curve the number max{x-a,y-b) is maximized.  
	 * 
	 * @param computedPoints The list of already computed corner points of the pareto curve.
	 * @param directions The list of directions associated with the computed corner points.
	 * @param errorTolerance The required precision of the pareto curve being approximated
	 * @return The point, or {@code null} if no point has a distance greater than {@code errorTolerance}
	 */
	protected static Point getNewTarget(List<Point> computedPoints, List<Point> directions, double errorTolerance, double[] maxValues)
	{
		//if we have less than 2 computed points, we take the most extreme directions
		if (computedPoints.size() == 0)
			return new Point(new double[] {maxValues[0], 0});
		if (computedPoints.size() == 1)
			return new Point(new double[] {0, maxValues[1]});
			//return new Point(new double[] {0, computedPoints.get(0).getCoord(1) + 1.0});
		
		//sortedLP is the set of computed corner points (i.e. their convex closure is an under-approx. of solution)
		//sortedUP is the set of points that toget (i.e. together with sortedLP, their convex closure is an over-approx. of solution)
		
		PermutedLexicographicComparator plc = new PermutedLexicographicComparator(new int[] {0,1}, new boolean[] {true, false});
		List<Point> sortedLP = removeDuplicities(computedPoints);
		Collections.sort(sortedLP, plc);
		System.out.println("sortedLP" + sortedLP);
		
		List<Point> sortedUP = upperBoundPoints(computedPoints, directions);
		Collections.sort(sortedUP, plc);
		System.out.println("sortedUP: " + sortedUP);
		
		//if (sortedUP.size() == 0)
		//	return null;
		int iLP = 0, iUP = 0;
		double maxVal = 0.0;
		int maxUP = -1;
		while (iLP < sortedLP.size() - 1 && iUP < sortedUP.size()) {
			if(sortedUP.get(iUP).getCoord(0) > sortedLP.get(iLP + 1).getCoord(0)) {
				iLP++;
			} else if(sortedUP.get(iUP).getCoord(0) <= sortedLP.get(iLP + 1).getCoord(0)) {
				double a1  = sortedLP.get(iLP).getCoord(0);
				double a2  = sortedLP.get(iLP).getCoord(1);
				double b1  = sortedLP.get(iLP + 1).getCoord(0);
				double b2  = sortedLP.get(iLP + 1).getCoord(1);
				
				//distance on the x axis
				double x2  = sortedUP.get(iUP).getCoord(1);
				double x1 = ((a1-b1)*(x2-b2))/(a2-b2) + b1;
				double d = Math.abs((sortedUP.get(iUP).getCoord(0)-x1)/x1);
				//double d = (sortedUP.get(iUP).getCoord(0)-x1);
				//System.out.println("a1: " + a1 + "  a2: " + a2 + "  b1: " + b1 + "  b2: " + b2 + "  x1: " + x1 + "  x2: " + x2 + "  d:" + d);
				if (maxVal < d) {
					maxVal = d;
					maxUP = iUP;
					//System.out.println("val: " + maxVal + ", point: " + sortedUP.get(iUP));
				}
				
				//distance on the y axis
				x1 = sortedUP.get(iUP).getCoord(0);
				x2 = ((a2-b2)*(x1-b1))/(a1-b1) + b2;
				d = Math.abs((sortedUP.get(iUP).getCoord(1)-x2)/x2);
				//d = (sortedUP.get(iUP).getCoord(1)-x2);
				if (maxVal < d) {
					maxVal = d;
					maxUP = iUP;
				}
				
				//distance along the y axis
				
				iUP++;
			} else {
				break;
			}
		}
		
		if (maxUP != -1 && maxVal > errorTolerance) {
			//System.out.println("distance:" + maxVal);
			return sortedUP.get(maxUP);
		} else {
			return null;
		}
	}

	/**
	 * Uses Gaussian elimination to solve equations given by {@code eqns} and {@code b}.
	 * The equations are given by eqns.x=b, where eqns is a matrix, and b is a column vector.
	 * 
	 * @return Array containing values for each variable.
	 */
	protected static double[] solveEqns(double[][] eqns, double[] b)
	{
		//System.out.println("Solving eqns");
		//using Gaussian elimination
	
		//System.out.println("y " + Arrays.deepToString(eqns) + " " + Arrays.toString(b));
		
		int numVars = eqns[0].length;
		for (int i = 0; i < numVars; i++) {
			//find pivot (the greatest number) for i-th column
			int index = i;
			for (int j = i + 1; j < numVars; j++) {
				if (eqns[j][i] > eqns[index][i])
					index = j;
			}
			
			double[] row = eqns[i];
			eqns[i] = eqns[index];
			eqns[index] = row;
			double right = b[i];
			b[i] = b[index];
			b[index] = right;
			
			
			if (Math.abs(eqns[i][i]) > Point.SMALL_NUMBER)
			{
				for (int j = i+1; j < eqns.length; j++)
				{
					double factor = eqns[j][i] / eqns[i][i];
					for (int k = i; k < numVars; k++) {
						eqns[j][k] -= factor * eqns[i][k];
					}
					b[j] -= factor * b[i];
				}
			}
			
			//System.out.println("z " + Arrays.deepToString(eqns) + " " + Arrays.toString(b));
		}		
		
		//System.out.println("x " + Arrays.deepToString(eqns) + " " + Arrays.toString(b));
		
		for(int i = numVars; i < eqns.length; i++)
		{
			if (Math.abs(eqns[i][numVars-1]) > Point.SMALL_NUMBER || Math.abs(b[i]) > Point.SMALL_NUMBER) {
				System.out.println("m " + Arrays.deepToString(eqns) + " " + Arrays.toString(b));
				return null;
			}
		}
		
		double[] res = new double[eqns[0].length];
		for (int i = eqns[0].length - 1; i >= 0; i--)
		{
			double sum = 0.0;
			for (int j = i + 1; j < eqns[0].length; j++) {
				sum += eqns[i][j]*res[j];
			}
			
			if (eqns[i][i] < Point.SMALL_NUMBER && Math.abs(b[i] - sum) < Point.SMALL_NUMBER)
			{
				res[i] = 0.0;
			}
			/*
			else if (eqns[i][i] < Point.SMALL_NUMBER && Math.abs(b[i] - sum) > Point.SMALL_NUMBER)
			{
				System.out.println("n " + Arrays.deepToString(eqns) + " " + Arrays.toString(b));
				return null;
			}*/
			else 
			{
				res[i] = (b[i] - sum ) / eqns[i][i];
			}
		}
		return res;
	}

/*	protected static double[] solveEqnsUsingLP(double[][] eqns, double[] b)
	{
		int dim = eqns[0].length;
		//lpsolve.LpSolve solver = lpsolve.LpSolve.makeLp(0,dim + 1);
		solver.setVerbose(lpsolve.LpSolve.CRITICAL);
		
		//for some strange reason the 0th element of an array is ignored by addConstraint
		
		for (int i = 0; i < eqns.length; i++) {
			double[] d = new double[dim + 1];
			for (int j = 0; j< dim; j++) {
				d[j + 1] = eqns[i][j];
			}
			//solver.addConstraint(d, lpsolve.LpSolve.EQ, b[i]);
		}
		return null;
	}*/
}
