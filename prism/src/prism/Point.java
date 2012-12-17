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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;


public class Point
{	
	/**
	 * A number used as a basis when neglecting roundoff errors in tests
	 * for equality.
	 */
	protected static final double SMALL_NUMBER = 10e-6;
	
	/**
	 * Internal storage for the coordinates
	 */
	private double[] coords;

	/**
	 * Creates a point of dimension {@code dimensions} in which all
	 * coordinates are initially set to 0.
	 * @param dimensions
	 */
	public Point(int dimensions)
	{
		this.coords = new double[dimensions];
	}

	/**
	 * Creates a point whose dimension is equal to the length of the
	 * array provided and whose values are taken from the array.
	 * @param coords The initial values.
	 */
	public Point(double[] coords)
	{
		this.coords = coords;
	}

	/**
	 * gets the calue of the {@code dim}-th coordinate.
	 * @param dim
	 * @return
	 */
	public double getCoord(int dim)
	{
		return coords[dim];
	}

	/**
	 * sets the value of the {@code dim}-th coordinate to {@code val}
	 * @param dim Which coordinate to set.
	 * @param val What value to set.
	 */
	public void setCoord(int dim, double val)
	{
		this.coords[dim] = val;
	}

	/**
	 * Gets the number of dimension this point has.
	 * @return
	 */
	public int getDimension()
	{
		return coords.length;
	}

	/**
	 * Determines if two points are equals. Two points are equal if they have
	 * the same dimension and all their coordinates are precisely equal. Use
	 * {@link #isCloseTo(Point)} if a numerical error could have been introduced
	 * when computing the point and so a tolerance is needed.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof Point))
			return false;
		else {
			Point second = (Point) obj;

			if (second.getDimension() != this.getDimension())
				return false;

			for (int i = 0; i < this.getDimension(); i++) {
				if (second.getCoord(i) != this.getCoord(i))
					return false;
			}
			//System.err.println("comparing " + this + " and " + second + " with the result " + (x == second.getX() && y == second.getY()));
			return true;
		}
	}
	
	@Override
	public int hashCode()
	{
		return (int) (this.coords[0] * 100);
	}
	

	/**
	 * Determines if this point is very close to the point {@code p}, where
	 * two points are very close if all of their coordinated differ at most by
	 * {@link #SMALL_NUMBER}. When a point is computed using a numerical method
	 * and thus can be rounded off, this method is a good replacement for {@link #equals(Object)}
	 * 
	 * @param p
	 * @return
	 */
	public boolean isCloseTo(Point p)
	{
		if (p.getDimension() != this.getDimension())
			return false;

		for (int i = 0; i < this.getDimension(); i++) {
			if (Math.abs(p.getCoord(i) - this.getCoord(i)) >= SMALL_NUMBER)
				return false;
		}
		//System.err.println("comparing " + this + " and " + second + " with the result " + (x == second.getX() && y == second.getY()));
		return true;
	}

	/**
	 * Returns the point in which the coordinates are reweighted so that the
	 * norm of the point is equal to 1.
	 * @return
	 */
	public Point normalize()
	{
		double[] ret = new double[this.coords.length];

		double sum = 0;
		for (Double d : this.coords)
			sum += Math.abs(d);
		
		//TODO throw exception when sum is 0?
		for (int i = 0; i < ret.length; i++) {
			ret[i] = Math.abs(this.coords[i]) / sum;
			if (ret[i] < 1e-10)
				ret[i] = 0;
		}

		return new Point(ret);
	}

	/**
	 * Returns the Euclidean distance from this point to {@code p}
	 * @param p The point to compute the distance from.
	 * @return The Euclidean distance.
	 * @throws IllegalArgumentException Thrown when the points have different dimensions
	 */
	public double distanceTo(Point p) throws IllegalArgumentException
	{
		if (p.getDimension() != this.getDimension())
			throw new IllegalArgumentException("The method Point.distanceTo(Point) can be only applied to points of equal dimensions");

		double result = 0.0;
		for (int i = 0; i < this.getDimension(); i++) {
			result += Math.pow((this.getCoord(i) - p.getCoord(i)), 2.0);
		}
		return Math.sqrt(result);
	}
	
	/**
	 * Returns the euclidean norm of this point.
	 */
	public double euclideanNorm()
	{
		return this.distanceTo(new Point(this.getDimension()));
	}
	
	@Override
	public String toString()
	{
		return Arrays.toString(coords).replace('[', '(').replace(']', ')');
	}
	
	/**
	 * Returns a point with the same coordinates as this point.
	 */
	public Point clone() {
		return new Point(this.coords.clone());
	}
	
	/**
	 * Returns the vector representing the coordinates of this point. It is
	 * a copy of the vector stored internally, so it can be modified without
	 * any problem.
	 * 
	 * @return
	 */
	public double[] getCoords()
	{
		return this.coords.clone();
	}

	/**
	 * Returns the point obtained from the current one by projecting it to the
	 * dimensions specified by {@code b}.
	 * @param b A bitset with the size same as the dimension equal to the one of this point.
	 * @return Projected point
	 */
	public Point project(BitSet b)
	{
		ArrayList<Double> al = new ArrayList<Double>();
		for (int i = 0; i < this.getDimension(); i++)
			if (b.get(i))
				al.add(this.getCoord(i));
		
		double[] ar = new double[al.size()];
		for (int i = 0; i < ar.length; i++)
			ar[i] = al.get(i);
		
		return new Point(ar);
	}

	/**
	 *  This methods ensures that the point's values corresponds to the properties
	 *  the user did input. Namely, when the rewards were minimizing, the value
	 *  is multiplied by -1, and when the probabilities were minimizing,
	 *  a new value is obtained by 1-value.
	 * @return
	 */
	public Point toRealProperties(OpsAndBoundsList obl)
	{
		double[] newCoords = coords.clone();

		for (int i = 0; i < obl.probSize(); i++) {
			if (obl.isProbNegated(i)) {
				newCoords[i] = 1-newCoords[i];
			}
		}

		for (int i = 0; i < obl.rewardSize(); i++) {
			if (obl.getRewardOperator(i) == Operator.R_MIN
					|| obl.getRewardOperator(i) == Operator.R_LE)
				newCoords[i + obl.probSize()] = -newCoords[i + obl.probSize()];			
		}
		
		return new Point(newCoords);
	}
	
	/**
	 * True if all coordinates are exactly 0.
	 * @return
	 */
	public boolean isZero() {
		for (int i = 0; i < this.getDimension(); i++)
			if (this.getCoord(i) != 0)
				return false;
		return true;
	}
}