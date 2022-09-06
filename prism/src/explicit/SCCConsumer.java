//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package explicit;

import prism.PrismException;

/**
 * Interface for a consumer of SCC information, for use with an {@code SCCComputer}.
 * When a new SCC is discovered, first {@code notifyStartSCC()} will be called.
 * Subsequently, for each state of the SCC, {@code notifyStateInSCC()} will be called.
 * When all states of the SCC have been notified, {@code notifyEndSCC()} will be called.
 * When the whole SCC computation is finished, {@code notifyDone()} will be called once.
 */
public interface SCCConsumer {
	/**
	 * Call-back function, will be called once at the start.
	 * Default implementation: Ignore.
	 */
	public default void notifyStart(Model model)
	{
		// ignore
	}

	/**
	 * Call-back function, will be called when a new SCC is discovered.
	 **/
	public void notifyStartSCC() throws PrismException;

	/**
	 * Call-back function, will be called once for each state in the SCC.
	 */
	public void notifyStateInSCC(int stateIndex) throws PrismException;

	/**
	 * Call-back function, will be called when all states of the SCC have been
	 * discovered.
	 **/
	public void notifyEndSCC() throws PrismException;

	/**
	 * Call-back function. Will be called after SCC computation is complete.
	 */
	public default void notifyDone() {}

}
