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

import prism.PrismUtils;

/**
 * Class to represent coalition info, e.g., in an ExpressionStrategy object.
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

	public void setAllPlayers()
	{
		allPlayers = true;
		players = null;
	}

	public void setPlayers(List<String> players)
	{
		allPlayers = false;
		this.players.clear();
		this.players.addAll(players);
	}

	// Get methods

	public boolean isAllPlayers()
	{
		return allPlayers;
	}
	
	public List<String> getPlayers()
	{
		return players;
	}
	
	@Override
	public String toString()
	{
		return allPlayers ? "*" : PrismUtils.joinString(players, ",");
	}
}
