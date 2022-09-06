/*
	This software is a Java wrapper for the lp_solve optimization library.
	
	Copyright (C) 2004  Juergen Ebert (juergen.ebert@web.de)

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package lpsolve;

/**
 * Contains the full version info for a lp_solve library instance.
 * 
 * @author Juergen Ebert
 */
public class VersionInfo {
	
	private int _majorversion;
	private int _minorversion;
	private int _release;
	private int _build;

	/**
	 * Creates a new instance of this class
	 */
	public VersionInfo(int major, int minor, int release, int build) {
		_majorversion = major;
		_minorversion = minor;
		_release = release;
		_build = build;
	}

	/**
	 * @return value of the build attribute
	 */
	public int getBuild() {
		return _build;
	}

	/**
	 * @return value of the majorversion attribute
	 */
	public int getMajorversion() {
		return _majorversion;
	}

	/**
	 * @return value of the minorversion attribute
	 */
	public int getMinorversion() {
		return _minorversion;
	}

	/**
	 * @return value of the release attribute
	 */
	public int getRelease() {
		return _release;
	}

}
