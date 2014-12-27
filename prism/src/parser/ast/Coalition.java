//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

package parser.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import prism.PrismUtils;

/**
 * Class to represent coalition info, e.g., in an ExpressionStrategy object.
 * Stored as a list of strings which are either player names (e.g. "controller")
 * or player indices (e.g. "2") starting from 1. Alternatively, "*" denotes all players.
 */
public class Coalition
{
	/** Is coalition all players? (denoted by *) */
	protected boolean allPlayers;
	
	/** Coalition: list of player names */
	protected List<String> players; 
	
	// Constructors

	/**
	 * Default constructor: empty player list
	 */
	public Coalition()
	{
		// Default" empty list of players
		allPlayers = false;
		players = new ArrayList<String>();
	}

	/**
	 * Copy constructor
	 */
	public Coalition(Coalition c)
	{
		if (c.isAllPlayers())
			allPlayers = true;
		else
			players = new ArrayList<String>(c.getPlayers());
	}

	// Set methods

	/**
	 * Make this coalition comprise all players (denoted "*")
	 */
	public void setAllPlayers()
	{
		allPlayers = true;
		players = null;
	}

	/**
	 * Make this coalition comprise these players
	 * (each string can be a player name or a (1-indexed) player index)
	 */
	public void setPlayers(List<String> players)
	{
		allPlayers = false;
		this.players.clear();
		this.players.addAll(players);
	}

	// Get methods

	/**
	 * Does this coalition comprise all players? (denoted "*")
	 */
	public boolean isAllPlayers()
	{
		return allPlayers;
	}
	
	/**
	 * Get a list of strings describing the coalition
	 * (each string can be a player name or a (1-indexed) player index).
	 * This will be null is the coalition is "*" (all players).
	 */
	public List<String> getPlayers()
	{
		return players;
	}
	
	// Other methods
	
	/**
	 * Check if a given player (specified by its index) is in the coalition.
	 * Player indices start from 1. A list of player names must be passed in,
	 * whose size equals the number of players, but in which some names can be null or "".
	 */
	public boolean isPlayerIndexInCoalition(int index, List<String> playerNames)
	{
		if (allPlayers)
			return true;
		int playerIndex;
		for (String player : players) {
			try {
				playerIndex = Integer.parseInt(player);
				if (index == playerIndex)
					return true;
			} catch (NumberFormatException e) {
				playerIndex = playerNames.indexOf(player);
				if (playerIndex != -1 && index == playerIndex)
					return true;
			}
		}
		return false;
	}
	
	/**
	 * Check if a given player (specified by its index) is in the coalition.
	 * Player indices start from 1. A mapping from player names to indices is passed in.
	 */
	public boolean isPlayerIndexInCoalition(int index, Map<String, Integer> playerNamesMap)
	{
		if (allPlayers)
			return true;
		int playerIndex;
		for (String player : players) {
			try {
				playerIndex = Integer.parseInt(player);
				if (index == playerIndex)
					return true;
			} catch (NumberFormatException e) {
				Integer playerIndexI = playerNamesMap.get(player);
				if (playerIndexI != null && index == (int) playerIndexI)
					return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return allPlayers ? "*" : PrismUtils.joinString(players, ",");
	}
}
