//==============================================================================
//	
//	Copyright (c) 2004-2005, Andrew Hinton
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
#ifndef _Included_simulator_Formulaeheader
#define _Included_simulator_Formulaeheader

#include "simexpression.h"
#include "simpctl.h"
#include <stdio.h>
#include <string>

using std::string;
using std::cout;
using std::endl;

//=============================================================================
//	Description
//=============================================================================

/*
 *	This interface allows arbitrary state proposition labels to be loaded into
 *	the simulator, these can be queried against any element in the path, 
 *	with specific functionality provided for the initial state and deadlocks.
 */


//=============================================================================
//	Functions
//=============================================================================

/*
 *	Removes any loaded state proposition labels from memory.
 */
void Deallocate_Label_Manager();

/*
 *	Allocates the state proposition label manager
 */
void Allocate_Label_Manager();


/* 
 *	Load the proposition into the manager
 *	returns the index of the successfully loaded
 *	update.
 */
int Load_Proposition(CNormalExpression* expr);

/*
 *	Queries the indexed proposition against the current state
 */
int Query_Proposition(int prop_index);

/*
 *	Queryies the indexed proposition against the state at the given step
 */
int Query_Proposition(int prop_index, int step);

/*
 *	Queries the current state as to whether it is the same as the initial state
 */
int Query_Is_Initial();

/*
 *	Queries the indexed state as to whether it is the same as the initial state
 */
int Query_Is_Initial(int step);

/*
 *	Queries the current state as to whether it is a deadlock state
 */
int Query_Is_Deadlock();

/*
 *	Queries the indexed state as to whether it is a deadlock state
 */
int Query_Is_Deadlock(int step);


#endif
