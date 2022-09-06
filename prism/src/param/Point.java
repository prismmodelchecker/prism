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

/**
 * Represents a given evaluation of parameters.
 * Objects of this class are immutable. Once it is created, no further changes
 * are possible. Objects of this class are mainly intended to be used with
 * {@code Region} objects, which represent sets of points, and check
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * @see Region
 */
public final class Point
{
	/** coordinates of point */
	private BigRational[] dimensions;

	/**
	 * Constructs a new point.
	 * 
	 * @param dimensions coordinates of this point
	 */
	public Point(BigRational[] dimensions)
	{
		this.dimensions = new BigRational[dimensions.length];
		System.arraycopy(dimensions, 0, this.dimensions, 0, dimensions.length);
	}

	/**
	 * Returns value of a given coordinate
	 * @param dim coordinate to return value of
	 * @return value of given coordinate
	 */
	public BigRational getDimension(int dim)
	{
		return dimensions[dim];
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();

		builder.append("(");
		for (int dim = 0; dim < dimensions.length; dim++) {
			builder.append(dimensions[dim].doubleValue());
			if (dim != dimensions.length - 1) {
				builder.append(",");
			}
		}
		builder.append(")");

		return builder.toString();
	}

	/**
	 * Returns array representation of point.
	 * 
	 * @return array representation of point
	 */
	public BigRational[] toArray()
	{
		BigRational[] result = new BigRational[dimensions.length];
		System.arraycopy(this.dimensions, 0, result, 0, this.dimensions.length);
		return result;
	}

	/**
	 * Returns number of dimensions of point.
	 * 
	 * @return number of dimensions of point
	 */
	public int size()
	{
		return dimensions.length;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Point)) {
			return false;
		}

		Point other = (Point) obj;
		if (this.dimensions.length != other.dimensions.length) {
			return false;
		}
		for (int dim = 0; dim < dimensions.length; dim++) {
			if (!this.dimensions[dim].equals(other.dimensions[dim])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		int hash = 0;

		for (int i = 0; i < dimensions.length; i++) {
			hash = dimensions[i].hashCode() + (hash << 6) + (hash << 16) - hash;
		}

		return hash;
	}

	/**
	 * Returns array of doubles approximating this point.
	 * 
	 * @return array of doubles approximating this point
	 */
	double[] doubleValues()
	{
		double[] result = new double[dimensions.length];
		for (int dim = 0; dim < dimensions.length; dim++) {
			result[dim] = dimensions[dim].doubleValue();
		}
		return result;
	}
}
