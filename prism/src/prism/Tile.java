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
import java.util.List;

/**
 * This class represents a face (or a part of face) of a Pareto curve or its
 * underapproximation, given
 * by it's corner points. The number of corner points must be equal to the dimension,
 * e.g. a line in a 2d, and a triangle in 3d. So for example a square face must
 * be represented as two triangle Tiles.
 */
public class Tile
{
	/**
	 * The corner points that determine this Tile
	 */
	protected ArrayList<Point> cornerPoints;

	protected int dim;
	
	/**
	 * Creates a new Tile with a given corner points
	 * @param cornerPoints The number of corner points must be equal to the dimension.
	 */
	public Tile(ArrayList<Point> cornerPoints)
	{
		//TODO check equal dimension
		this.cornerPoints = cornerPoints;
		dim = this.cornerPoints.size();
		projectionUpperBound = new boolean[dim];
	}
	
	/**
	 * Returns a list containing the corner points representing this tile.
	 */
	public ArrayList<Point> getCornerPoints()
	{
		return this.cornerPoints;
	}
	
	/**
	 * If true, this tile is already known to be part of a facet of a Pareto
	 * curve, not just it's underapproximation.
	 */
	protected boolean upperBound;
	protected boolean[] projectionUpperBound;
	
	protected boolean hyperplaneSuggested;

	/**
	 * Returns true if this tile is already known to be part of a facet of a Pareto
	 * curve, not just its underapproximation.
	 */
	public boolean isUpperBound(int index)
	{
		if (index == dim)
			return this.upperBound;
		else
			return this.projectionUpperBound[index];
	}
	
	/**
	 * Returns true if {@code point} lies strictly above the hyperplane given
	 * by this tile.
	 */
	protected boolean pointAboveHyperplane(Point point) throws PrismException
	{
		//System.out.println("t: " + this);
		for (Point cp : this.cornerPoints) {
			if (point.isCloseTo(cp))
				return false;
		}
		
		Point w = Tile.getWeightsForTile(this);
		
		double productTile = 0;
		for (int j = 0; j < dim; j++)
			for (int i = 0; i < point.getDimension(); i++)
				productTile += w.getCoord(i) * this.cornerPoints.get(j).getCoord(i); 
		productTile = productTile/point.getDimension();
		
		
		double productPoint = 0;
		for (int i = 0; i < point.getDimension(); i++)
			productPoint += w.getCoord(i) * point.getCoord(i);
		//System.out.println(productPoint + " " + productTile + " " + (productPoint - productTile));
		return productPoint >= productTile + Point.SMALL_NUMBER;
	}
	
	/**
	 * Returns true if this tile will be changed when {@code point} is added
	 * to a parent {@link TileList}. If {@code updateUpperBounds} is set, then also
	 * the information about the tile lying on a pareto curve is updated
	 *
	 * @param updateUpperBounds If true, the new point does not split the tile, and
	 * the tile's corresponding hyperplane was suggested, then the information
	 * about the tile being an upper bound is updated. This can be either information
	 * for the whole tile or its border lines, depending on {@code upperBoundsIndex} 
	 * @param upperBoundsIndex If equal to {@code dim}, when the information
	 * about this tile being an upper bound is updated, the whole tile will be
	 * marked as upper bound. If lower than {@code dim}, only the line having
	 * zero value in {@code upperBoundsIndex}-th coordinate will be marked as upper
	 * bound
	 * 
	 * @throws PrismException
	 * 
	 */
	
	public boolean processNewPoint(Point point, boolean updateUpperBounds, int upperBoundsIndex) throws PrismException
	{
		boolean ret = pointAboveHyperplane(point);
		
		if (ret) {
			//System.out.println("is affected: " + this);
			hyperplaneSuggested = false;
			return true;
		} else {
			if (hyperplaneSuggested && updateUpperBounds) {
				hyperplaneSuggested = false;
				if (upperBoundsIndex == this.cornerPoints.size()) {
					this.upperBound = true;
				} else
					this.projectionUpperBound[upperBoundsIndex] = true;
			}
			return false;
		}
	
	}
	
	/**
	 * Returns a list of tiles that should be added to the parent TileList
	 * when {@code point} is added, replacing the current tile. For the computation
	 * to be performed correctly, the list {@code otherPoints} must contain the
	 * set of points from all affected tiles.
	 * 
	 * @param tolerance When the newly created tiles will differ only in a point
	 * which is {@code tolerance} far from the original tiles, the new tile will
	 * be considered finished.
	 */
	public List<Tile> splitByPoint(Point point, List<Point> otherPoints, double tolerance) throws PrismException
	{
		Point weights = Tile.getWeightsForTile(this);
		double pointWeight = 0;
		for (int i = 0; i< point.getDimension(); i++) {
			pointWeight += weights.getCoord(i) * point.getCoord(i);
		}
		
		double thisTileWeight = 0;
		for (int i = 0; i< point.getDimension(); i++) {
			thisTileWeight += weights.getCoord(i) * this.getCornerPoints().get(0).getCoord(i);
		}
		
		boolean isClose = (pointWeight - thisTileWeight) < tolerance;

		//System.out.println("SPLITTING");
		ArrayList<Tile> tiles = new ArrayList<Tile>();
		for (int i = 0; i < this.cornerPoints.size(); i++) {
			ArrayList<Point> newPoints = (ArrayList<Point>) this.cornerPoints.clone();
			newPoints.set(i, point);
			Tile t = new Tile(newPoints);
			
			boolean onBorder=true;
			for (int j = 0 ; j < t.cornerPoints.size(); j ++) {
				onBorder = true;
				for (int k = 0 ; k < t.cornerPoints.size(); k ++)
					if (t.cornerPoints.get(k).getCoord(j) != 0.0) {
						onBorder =false;
						break;
					}
				if (onBorder)
					break;
			}
			//System.out.println("hyperplane:" + t);
			
			boolean pointAboveExists = false;
			for (Point p : otherPoints) {
				//System.out.println("point " + p);
				if (t.pointAboveHyperplane(p)) {
					pointAboveExists = true;
					//System.out.println("is above");
					break;
				}
					
			}
			
			//the tile is upper bound if the original was (this is not a contradiction,
			//due to the approx nature of computation even an upper bound tile could be
			//improved), or is upper bound if the change is rather small.
			if (isClose || this.isUpperBound(this.dim))
				t.upperBound = true;
			
			if (!onBorder && !pointAboveExists)
				tiles.add(t);
		}
		//System.out.println("RET: " + tiles);
		return tiles;
	}
	
	/**
	 * Gets a weight vector representing the hyperplane given by the tile.
	 * @param t The tile to compute the weight vector for.
	 * @return
	 */
	protected static Point getWeightsForTile(Tile t)
	{
		int dim = t.cornerPoints.size();
		
		//First check if we have a nonzero point and remember one.
		int nonzeroIndex = -1;
		for (int j = 0; j < dim; j++) {
			if (!t.cornerPoints.get(j).isZero()) {
				nonzeroIndex = j;
				break;
			}
		}
		
		//If all are zero, we return anything meaningful
		if (nonzeroIndex == -1) {
			double[] pointCoords = new double[dim];
			pointCoords[0] = 1;
			return new Point(pointCoords);
		}
		
		
		double[][] d = new double [dim][dim];
		double[] b = new double [dim];
		
		Point pFixed = t.cornerPoints.get(dim-1);
		for (int j = 0; j < dim-1; j++) {
			Point p = t.cornerPoints.get(j);
			for (int i = 0; i < dim; i++) {
				d[j][i] = p.getCoord(i) - pFixed.getCoord(i);
			}
			b[j] = 0;
		}

		for (int i = 0; i < dim; i++) {
			d[dim-1][i] = 1;
		}
		b[dim-1] = 1;
		
		double[] ret = MultiObjUtils.solveEqns(d, b);
		double[] pointCoords = new double[dim];
		for (int i = 0; i < dim; i++)
			pointCoords[i] = ret[i];	
		//System.out.println("returned points: " + Arrays.toString(pointCoords));
		
		return new Point(pointCoords).normalize();
	}
	
	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append('{');
		for (int i = 0; i < this.cornerPoints.size() - 1; i++) {
			sb.append(this.cornerPoints.get(i).toString());
			sb.append("; ");
		}
		sb.append(this.cornerPoints.get(this.cornerPoints.size()-1).toString());
		sb.append('}');
		return sb.toString();
	}
	
	
	/**
	 * True if there is one of the boundaries of the Tile is
	 * a line orthogonal to {@code index}'s axis
	 */
	public boolean liesOnBoundary(int index) {
		int count = 0;
		
		for (Point p : this.cornerPoints) {
			if (p.getCoord(index) == 0)
				count++;
		}
		
		//System.out.println(count);
		
		/*//this can happen in fact in all zeros
		  if (count == 3) {
			throw new Error("Error in design of Pareto curve algorithm. The whole Tile lies on a boundary.");
		}*/
		
		return (count >= 2);
				
	}
}
