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
 * or player indices (e.g. "2"). Alternatively, "*" denotes all players.
 */
public class Coalition
{
	/** Is coalition all players? (denoted by *) */
	protected boolean allPlayers;
	
	/** Coalition: list of player names/indices */
	protected List<String> players; 
	
	// Constructors

	/**
	 * Default constructor: empty player list
	 */
	public Coalition()
	{
		// Empty list of players
		allPlayers = false;
		players = new ArrayList<String>();
	}

	/**
	 * Copy constructor
	 */
	public Coalition(Coalition c)
	{
		// Copy all info (though, strictly speaking, "players" is irrelevant if allPlayers==true
		// (note that we access c.players directly since getPlayers() can return null)
		allPlayers = c.isAllPlayers();
		players = new ArrayList<String>(c.players);
	}

	// Set methods

	/**
	 * Make this coalition comprise all players (denoted "*")
	 */
	public void setAllPlayers()
	{
		allPlayers = true;
		players.clear();;
	}

	/**
	 * Make this coalition comprise these players
	 * (each string can be a player name or an integer player index)
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
	 * Is this coalition empty?
	 */
	public boolean isEmpty()
	{
		return !allPlayers && players.isEmpty();
	}
	
	/**
	 * Get a list of strings describing the coalition
	 * (each string can be a player name or an integer player index).
	 * This will be null is the coalition is "*" (all players).
	 */
	public List<String> getPlayers()
	{
		return allPlayers ? null : players;
	}
	
	/**
	 * Check if a given player (specified by its index) is in the coalition,
	 * i.e., if the index or the name of the player with this index is in the list.
	 * The mapping from player indices to player names is passed in.
	 */
	public boolean isPlayerIndexInCoalition(int index, Map<Integer, String> playerNames)
	{
		if (allPlayers) {
			return true;
		}
		if (players.contains("" + index)) {
			return true;
		}
		String playerName = playerNames.get(index);
		if (playerName != null && !"".equals(playerName) && players.contains(playerName)) {
			return true;
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return allPlayers ? "*" : PrismUtils.joinString(players, ",");
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (allPlayers ? 1231 : 1237);
		result = prime * result + ((players == null) ? 0 : players.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Coalition other = (Coalition) obj;
		if (allPlayers != other.allPlayers)
			return false;
		if (players == null) {
			if (other.players != null)
				return false;
		} else if (!players.equals(other.players))
			return false;
		return true;
	}
}
