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


#include "simformulae.h"
#include "simpath.h"
#include "simupdater.h"
#include "simutil.h"
#include <stdio.h>
#include <iostream>
#include <string>
#include <vector>

using std::cout;
using std::endl;
using std::string;
using std::vector;

//=============================================================================
//	Description
//=============================================================================

/*
 *	This interface allows arbitrary state proposition labels to be loaded into
 *	the simulator, these can be queried against any element in the path, 
 *	with specific functionality provided for the initial state and deadlocks.
 */

//=============================================================================
//	Local Data
//=============================================================================

vector <CNormalExpression*> loaded_propositions;


//=============================================================================
//	Functions
//=============================================================================

/*
 *	Removes any loaded state proposition labels from memory.
 */
void Deallocate_Label_Manager()
{
       
	for(int i = 0; i < loaded_propositions.size(); i++)
	{
		if(loaded_propositions[i] != NULL)
		{
			delete loaded_propositions[i];
			loaded_propositions[i] = NULL;
		}
	}
	loaded_propositions.clear();

}

/*
 *	Allocates the state proposition label manager
 */
void Allocate_Label_Manager()
{
	loaded_propositions.reserve(10);
}


/* 
 *	Load the proposition into the manager
 *	returns the index of the successfully loaded
 *	update.
 */
int Load_Proposition(CNormalExpression* expr)
{
	loaded_propositions.push_back(expr);
	return loaded_propositions.size()-1;
}

/*
 *	Queries the indexed proposition against the current state
 */
int Query_Proposition(int prop_index)
{
	if(prop_index >= loaded_propositions.size()) return -1;
	return loaded_propositions[prop_index]->Evaluate();
}

/*
 *	Queryies the indexed proposition against the state at the given step
 */
int Query_Proposition(int prop_index, int step)
{
	
	if(prop_index >= loaded_propositions.size()) 
	{
		return -1;
	}
	
	int* old_variables = Get_Path_State(step)->variables;


	return loaded_propositions[prop_index]->Evaluate(old_variables);
}

/*
 *	Queries the current state as to whether it is the same as the initial state
 */
int Query_Is_Initial()
{
	if(Get_Path_Length() <= 0) return 0;
	int* initial = Get_Path_State(0)->variables;

	return Int_Arrays_Equals(state_variables, initial, no_state_variables);
}

/*
 *	Queries the indexed state as to whether it is the same as the initial state
 */
int Query_Is_Initial(int step)
{
    
	if(Get_Path_Length() <= 0) return 0;
        
	int* initial = Get_Path_State(0)->variables;
        
	int* query   = Get_Path_State(step)->variables;
       
	return Int_Arrays_Equals(query, initial, no_state_variables);
}

/*
 *	Queries the current state as to whether it is a deadlock state
 */
int Query_Is_Deadlock()
{
	// Note: need to recalculate updates for current state first
	Calculate_Updates(state_variables);
	return Get_No_Updates() == 0;
}

/*
 *	Queries the indexed state as to whether it is a deadlock state
 */
int Query_Is_Deadlock(int step)
{
	// Won't be a deadlock unless at end of path
	// (in which case would use previous method anyway?)
	if(step != Get_Path_Length()-1) return 0;
	else return Get_No_Updates();
}

