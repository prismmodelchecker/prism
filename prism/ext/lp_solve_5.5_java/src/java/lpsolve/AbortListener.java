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
 * to the <code>putAbortfunc</code> method of the <code>LpSolve</code> class.
 * 
 * @author Juergen Ebert
 * @see LpSolve#putAbortfunc
 * @see "lp_solve documentation for 'put_abortfunc'"
 */
public interface AbortListener {

	/**
	 * When set, the abort routine is called regularly during solve(). 
	 * The user can do whatever he wants in this routine.
	 * 
	 * @param problem the problem this Listener was defined for
	 * @param userhandle the userhandle object that was passed to <code>putAbortfunc</code>
	 * @return if true, then lp_solve aborts the solver and returns with an appropriate code
	 * @throws LpSolveException
	 */
	public boolean abortfunc(LpSolve problem, Object userhandle) throws LpSolveException;

}
