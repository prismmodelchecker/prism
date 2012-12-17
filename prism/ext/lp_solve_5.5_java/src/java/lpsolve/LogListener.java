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
 * Classes that implement this interface may be passed
 * to the <code>putLogfunc</code> method of the <code>LpSolve</code> class.
 * 
 * @author Juergen Ebert
 * @see LpSolve#putLogfunc
 * @see "lp_solve documentation for 'put_logfunc'"
 */
public interface LogListener {

	/**
	 * When set, the log routine is called when lp_solve has someting 
	 * to report (error conditions or so).
	 *  
	 * @param problem the problem this Listener was defined for
	 * @param userhandle the userhandle object that was passed to <code>putLogfunc</code>
	 * @param buf the log message
	 * @throws LpSolveException
	 */
	public void logfunc(LpSolve problem, Object userhandle, String buf) throws LpSolveException;

}
