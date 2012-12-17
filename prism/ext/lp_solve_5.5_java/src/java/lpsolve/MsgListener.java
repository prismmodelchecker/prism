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
 * to the <code>putMsgfunc</code> method of the <code>LpSolve</code> class.
 * 
 * @author Juergen Ebert
 * @see LpSolve#putMsgfunc
 * @see "lp_solve documentation for 'put_msgfunc'"
 */
public interface MsgListener {

	/**
	 * This routine is called when a situation specified in the mask parameter
	 * of putMsgfunc occurs. 
	 * Note that this routine is called while solving the model. 
	 * This can be usefull to follow the solving progress.
	 *  
	 * @param problem the problem this Listener was defined for
	 * @param userhandle the userhandle object that was passed to <code>putMsgfunc</code>
	 * @param msg event code why this method was called
	 * @throws LpSolveException
	 */
	public void msgfunc(LpSolve problem, Object userhandle, int msg) throws LpSolveException;

}
