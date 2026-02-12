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

package jdd;

import prism.*;

/**
 * A {@code PrismLibrary} wrapper around native code in the JDD library.
 */
public class JDDLibrary implements PrismLibrary
{
    /** Has CUDD been initialised? */
    private static boolean cuddInitialised = false;

    @Override
    public void initialise(Prism prism) throws PrismException
    {
        long cuddMaxMem = PrismUtils.convertMemoryStringtoKB(prism.getCUDDMaxMem());
        JDD.InitialiseCUDD(cuddMaxMem, prism.getCUDDEpsilon());
        cuddInitialised = true;
    }

    @Override
    public void setMainLog(PrismLog mainLog) throws PrismException
    {
        // If possible, pass the (file/stdout) pointer to JDD.
        // This is mainly so that diagnostic/error messages from the
        // DD native code end up in the same place as the rest of the log output.
        long fp = mainLog.getFilePointer();
        if (fp > 0) {
            JDD.SetOutputStream(fp);
        }
    }

    @Override
    public void notifySettings(PrismSettings settings)
    {
        if (cuddInitialised) {
            JDD.SetCUDDEpsilon(settings.getDouble(PrismSettings.PRISM_CUDD_EPSILON));
            try {
                long cuddMaxMem = PrismUtils.convertMemoryStringtoKB(settings.getString(PrismSettings.PRISM_CUDD_MAX_MEM));
                JDD.SetCUDDMaxMem(cuddMaxMem);
            } catch (PrismException e) {
                // Fail silently if memory string is invalid
            }
            jdd.SanityJDD.enabled = settings.getBoolean(PrismSettings.PRISM_JDD_SANITY_CHECKS);
        }
    }

    @Override
    public void closeDown(boolean check)
    {
        if (cuddInitialised) {
            JDD.CloseDownCUDD(check);
            cuddInitialised = false;
        }
    }
}
