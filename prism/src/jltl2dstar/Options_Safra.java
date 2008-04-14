/*
 * This file is part of a Java port of the program ltl2dstar
 * (http://www.ltl2dstar.de/) for PRISM (http://www.prismmodelchecker.org/)
 * Copyright (C) 2005-2007 Joachim Klein <j.klein@ltl2dstar.de>
 * Copyright (c) 2007 Carlos Bederian
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as 
 *  published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jltl2dstar;

/**
 * Options for Safra's algorithm
 */

public class Options_Safra implements Cloneable {
	/** Optimize accepting true loops */
	public boolean opt_accloop; 
	/** Optimize all successor accepting */
	public boolean opt_accsucc; 
	/** Renaming optimization (templates) */
	public boolean opt_rename;  
	/** Try to reorder Safra trees */
	public boolean opt_reorder; 
	/** Use stuttering */
	public boolean stutter;
	/** Check for stutter insensitive */
	public boolean partial_stutter_check;
	/** Perform stutter closure on NBA before conversion */
	public boolean stutter_closure;
	/** Check for DBA */
	public boolean dba_check;
	/** Provide statistics */
	public boolean stat;
	/** Optimize accepting true loops in union construction */
	public boolean union_trueloop;

	/** Constructor */
	public Options_Safra() {
		opt_none();

		dba_check=false;
		//    tree_verbose=false;
		stat=false;
		union_trueloop=true;

		stutter=false;
		partial_stutter_check=false;
		stutter_closure=false;
	}
	
	public Options_Safra clone()
	{
		Options_Safra rv = new Options_Safra();
		
		rv.opt_accloop = opt_accloop; 
		rv.opt_accsucc = opt_accsucc; 
		rv.opt_rename = opt_rename;  
		rv.opt_reorder = opt_reorder; 
		rv.stutter = stutter;
		rv.partial_stutter_check = partial_stutter_check;
		rv.stutter_closure = stutter_closure;
		rv.dba_check = dba_check;
		rv.stat = stat;
		rv.union_trueloop = union_trueloop;
		return rv;
	}

	/** Enable all opt_ options */
	public void opt_all() {
		opt_accloop
		=opt_accsucc
		=opt_rename
		=opt_reorder
		=true;
	}

	/** Disable all opt_ options */
	public void opt_none() {
		opt_accloop
		=opt_accsucc
		=opt_rename
		=opt_reorder
		=false;
	}
}
