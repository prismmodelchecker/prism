//==============================================================================
//
//	Copyright (c) 2026-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

/**
 * Interface for libraries that can be loaded and used by PRISM.
 * Currently, this is mainly used to provide a clean way for
 * native code to be detached from the main Prism class.
 */
public interface PrismLibrary
{
    /**
     * Initialise the library.
     * @param prism The main Prism object
     */
    void initialise(Prism prism) throws PrismException;

    /**
     * Set the main log for the library to use.
     * @param mainLog The log
     */
    void setMainLog(PrismLog mainLog) throws PrismException;

    /**
     * Notify the library of changes to PRISM settings.
     * @param settings The new settings
     */
    void notifySettings(PrismSettings settings);

    /**
     * Close down the library, tidying up any resources.
     * @param check Do additional checks to make sure close down was clean?
     */
    void closeDown(boolean check);
}
