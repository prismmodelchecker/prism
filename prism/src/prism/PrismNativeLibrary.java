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

import java.nio.file.Path;

/**
 * A {@code PrismLibrary} wrapper around native code in the "prism" shared library.
 */
public class PrismNativeLibrary implements PrismLibrary
{
    @Override
    public void initialise(Prism prism) throws PrismException
    {
        Path workingDirectory = Prism.getWorkingDirectory();
        if (workingDirectory != null) {
            PrismNative.setWorkingDirectory(workingDirectory.toString());
        }
        PrismNative.initialise(prism);
    }

    @Override
    public void setMainLog(PrismLog mainLog) throws PrismException
    {
        PrismNative.setMainLog(mainLog);
    }

    @Override
    public void notifySettings(PrismSettings settings)
    {
        PrismNative.SetExportIterations(settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS));
    }

    @Override
    public void closeDown(boolean check)
    {
        PrismNative.closeDown();
    }
}
