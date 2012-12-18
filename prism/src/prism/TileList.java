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
import java.util.List;

import parser.ast.Expression;

/**
 * This class represent a list of tiles which have corners in Pareto points,
 * and which represent an under-approximation of the Pareto curve.
 */
public class TileList
{
	/**
	 * Threshold for approximation of the Pareto curve.
	 */
	private double tolerance;

	/**
	 * This is used when printing the tileList to the user. The information
	 * we need are whether the reward is min (to multiply by -1) and
	 * whether the probability is min (to do 1-value) 
	 */
	private OpsAndBoundsList opsAndBoundsList;

	public OpsAndBoundsList getOpsAndBoundsList()
	{
		return this.opsAndBoundsList;
	}

	/**
	 * This is where a TileLists are stored to be later retrieved in GUI.
	 * It is a workaround for the fact that Pareto curve can't be 
	 * returned in any reasonable way.
	 * 
	 * To ensure correct concurrent behaviour, object accessing any stored tile lists
	 * or elements should synchronise on this object;
	 */
	protected static List<TileList> storedTileLists;

	public static List<TileList> getStoredTileLists()
	{
		return storedTileLists;
	}

	/**
	 * Formulas for X and Y axes (or multi-obj formulas) of the corresponding
	 * elements of storedTileLists.
	 */
	protected static List<Expression> storedFormulasX;
	protected static List<Expression> storedFormulasY;
	protected static List<Expression> storedFormulas;

	public static List<Expression> getStoredFormulasX()
	{
		return storedFormulasX;
	}

	public static List<Expression> getStoredFormulasY()
	{
		return storedFormulasY;
	}

	public static List<Expression> getStoredFormulas()
	{
		return storedFormulas;
	}

	/** Removes all stored tile list and associated formulas */
	public static void clearStoredTileLists()
	{
		TileList.storedFormulasX.clear();
		TileList.storedFormulasY.clear();
		TileList.storedFormulas.clear();
		TileList.storedTileLists.clear();
	}

	static {
		storedFormulasX = new ArrayList<Expression>();
		storedFormulasY = new ArrayList<Expression>();
		storedFormulas = new ArrayList<Expression>();
		storedTileLists = new ArrayList<TileList>();
	}

	protected int currentProjectionIndex = 0;
	/**
	 * This list holds all tiles of this TileList.
	 */
	protected ArrayList<Tile> list;
	/**
	 * Initial tile passed in constructor.
	 * Currently only used for drawing nice tikz pics
	 */
	protected Tile initialTile;
	/**
	 * Dimension of the space, determined from the initial tile.
	 */
	protected int dim;

	/**
	 * Creates a new instance of the TileList, originally containing only one
	 * tile, {@code initialTile}.
	 * @param initialTile The first {@link Tile} to put in a list.
	 */
	public TileList(Tile initialTile, OpsAndBoundsList opsAndBounds, double tolerance)
	{
		this.dim = initialTile.cornerPoints.get(0).getDimension();
		this.initialTile = initialTile;
		this.list = new ArrayList<Tile>();
		this.list.add(initialTile);
		this.opsAndBoundsList = opsAndBounds;
		this.tolerance = tolerance;

		//this is a HACK so that we don't try projections in 2 dimensions.
		//these are not needed since projections are extremes in 1 dim,
		//which were done for initial tile
		if (this.dim == 2) {
			this.currentProjectionIndex = 2;
		}
	}

	@Override
	public String toString()
	{
		String s = "[";
		boolean first = true;
		for (int j = 0; j < this.list.size(); j++) {
			Tile t = this.list.get(j);
			for (Point p : t.cornerPoints) {
				// We want to print the user-readable values if possible
				if (this.opsAndBoundsList != null) {
					p = p.toRealProperties(this.opsAndBoundsList);
				}
				if (first)
					first = false;
				else
					s += ",\n";
				s += "(";
				for (int i = 0; i < p.getDimension(); i++) {
					if (i > 0)
						s += ",";
					s += p.getCoord(i);
				}
				s += ")";
			}
		}
		s += "]";
		return s;
	}

	/**
	 * Returns a weight vector which could yield a new Pareto point when used.
	 * {@code null} is returned if no candidate point exists.
	 * 
	 * TODO: currently just calls another method.
	 */
	public Point getCandidateHyperplane()
	{
		return getFreshRealCandidateHyperplane();
	}

	/**
	 * Queries the tiles of this TileList to get a new weight vector which 
	 * could yield a new pareto point when used. {@code null} is returned
	 * if no candidate point exists. It gives absolute preference to
	 * weight vectors having zero in one of the coordinates.
	 */
	private Point getFreshRealCandidateHyperplane()
	{
		//we have to exhaust the boundaries first
		while (this.currentProjectionIndex < this.dim) {
			for (Tile t : list) {
				if (!t.isUpperBound(this.currentProjectionIndex) && t.liesOnBoundary(this.currentProjectionIndex)) {
					t.hyperplaneSuggested = true;
					//System.out.println("fresh candidate hyperplane:" + t + "with weight " + Tile.getWeightsForTile(t));
					Point ret = Tile.getWeightsForTile(t);

					//We want to make sure we optimize on the boundary,
					//so we make the point parallel to an axis. But at
					//the same time we need to make sure it's not zero weight.
					Point vert = ret.clone();
					vert.setCoord(this.currentProjectionIndex, 0);
					return (vert.isZero()) ? ret : vert.normalize();
				}
				/*else {
					System.out.println("does not lie on " + this.currentProjectionIndex + " boundary: " + t);
				}*/
			}

			this.currentProjectionIndex++;
			//System.out.println("Increasing projection index to " + this.currentProjectionIndex);
			//System.out.println(this.toString());
		}

		//now the points inside the quadrant
		for (Tile t : list) {
			if (!t.isUpperBound(dim)) {
				t.hyperplaneSuggested = true;
				//System.out.println("fresh candidate hyperplane:" + t + "with weight " + Tile.getWeightsForTile(t));
				return Tile.getWeightsForTile(t);
			}
		}

		return null;
	}

	/**
	 * Returns the number of different points that form the tiles of this
	 * TileList. The implementation is rather inefficient and is intended
	 * only for debugging purposes.
	 */
	public int getNumberOfDifferentPoints()
	{
		return this.getPoints().size();
	}

	/**
	 * Returns the points that form the tiles of this
	 * TileList. The implementation is rather inefficient and is intended
	 * only for debugging pusposes.
	 */
	public List<Point> getPoints()
	{
		ArrayList<Point> a = new ArrayList<Point>();
		for (Tile t : this.list) {
			for (Point p : t.cornerPoints)
				if (!a.contains(p))
					a.add(p);
		}
		return a;
	}

	/**
	 * Adds a newly discovered point to this TileList. This basically means that
	 * some tiles will be split into smaller tiles. Also makes sure that the invariant
	 * "approximation is downward closed" is maintained true.
	 */
	public void addNewPoint(Point point) throws PrismException
	{
		//first create the projection to the boundary
		if (this.currentProjectionIndex < this.dim && point.getCoord(this.currentProjectionIndex) > 0.0) {
			Point projectionPoint = point.clone();
			projectionPoint.setCoord(this.currentProjectionIndex, 0.0);
			splitTilesByPoint(projectionPoint, false);
		} else {
			splitTilesByPoint(point, this.currentProjectionIndex == dim);
		}
	}

	/**
	 * Splits the Tiles using the new point. This basically means that
	 * some tiles will be replaced by smaller tiles. Note that this method
	 * does <em>not</em> check whether the tiles form a downward closed
	 * polyhedron, hence it shouldn't be called directly, but indirecly by
	 * calling {@link #addNewPoint(Point)}.
	 * 
	 * @param isRealyFoundPoint False if {@code point} is just a projection of a point found.
	 */
	protected void splitTilesByPoint(Point point, boolean isRealyFoundPoint) throws PrismException
	{
		//System.out.println("really processing point " + point);

		ArrayList<Tile> affectedTiles = new ArrayList<Tile>();
		for (Tile t : this.list) {
			boolean affected = t.processNewPoint(point, true, this.currentProjectionIndex);
			if (affected)
				affectedTiles.add(t);
		}

		ArrayList<Point> allPoints = new ArrayList<Point>();
		for (Tile t : affectedTiles) {
			allPoints.addAll(t.cornerPoints);
		}
		//System.out.println("allpoints " + allPoints);

		for (Tile t : affectedTiles) {
			list.remove(t);
			//System.out.println("removing " + t);
			List<Tile> l = t.splitByPoint(point, allPoints, this.tolerance);
			//System.out.println("adding " + l);
			this.list.addAll(l);
		}

		//System.out.println(this.toString());
	}

	/**
	 * returns the dimension of the points in this TileList
	 */
	public int getDimension()
	{
		return this.dim;
	}
}
