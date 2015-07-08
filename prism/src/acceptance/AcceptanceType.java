//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package acceptance;

/**
 * An omega-regular acceptance type.
 */
public enum AcceptanceType {
	RABIN,
 	STREETT,
 	REACH,
	GENERALIZED_RABIN,
 	GENERIC;

	/**
	 * Check whether an array of AcceptanceTypes contains a specific element.
	 * @param types the array of AcceptanceTypes
	 * @param type the AcceptanceType that is tested for
	 */
	public static boolean contains(AcceptanceType[] types, AcceptanceType type)
	{
		for (AcceptanceType t: types) {
			if (t.equals(type))
				return true;
		}
		return false;
	}
}
