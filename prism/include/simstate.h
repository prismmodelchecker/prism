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
#ifndef _Included_simulator_Stateheader
#define _Included_simulator_Stateheader

#include <string>

using std::string;

//=============================================================================
// Description
//=============================================================================

/*
 *	This interface is for the the current state space.  It simply
 *	maintains an array of values of the current set of variables.  
 *	Each variable, whether boolean or int is stored as an int and can be
 *	accessed by using the globally accessible state_variables array or by
 *	the set of convienience methods.
 */

//=============================================================================
//	Constants
//=============================================================================

const int PATH_NO_CHOICE_MADE = -1;

//=============================================================================
//	Classes
//=============================================================================

/*
 *	States of the model will have information associated with them according
 *	to different executions of the system.  The CPathState class stores this
 *	information including:
 *		The copy of the variables for that state
 *		The index of the choice made to get out of that state
 *		The probability of the choice made to get out of that state
 *		The time spent in that state
 *		The instant state costs of that state
 *		The costs accumulated in that state (for real time models)
 *		The costs of the transition out of that state
 *		The costs of the path so far up until that state
 */
class CPathState
{
	//Made public for faster access
	public:
		int* variables; //The copy of the variables for that state
		int choice_made; //the choice made to get out of this state
		double probability; //this is used to determine which choice was made, if choice_made is -1
		double time_spent_in_state; //The time spent in that state
		bool time_known; 
		double *state_cost; //The costs accumulated in that state (for real time models)
		double *state_instant_cost; //The instant state costs of that state
		double *path_cost_so_far; //The costs of the path so far up until that state
		double *transition_cost; //The costs of the transition out of that state

	/*
	 *	Constructs the state according with no_state_variables storage spaces 
	 *	choice_made is initially PATH_NO_CHOICE_MADE (no choice made yet)
	 */
	CPathState();

	/*
	 *	Destructor removes the variables array.
	 */
	~CPathState();

	/*
	 *	Set this state to match a copy of state_variables.
	 */
	void Make_This_Current_State();

	/*
	 *	Set state_variables to match a copy of variables.
	 */
	void Make_Current_State_This();

	/*
	 *	Returns a string representation of this path state.
	 */
	string To_String();
};


//=============================================================================
//	Globals
//=============================================================================

//State space
extern int * state_variables; //pointer to the state space array
extern int no_state_variables; //size of the state space

//=============================================================================
//	Functions
//=============================================================================

/*
 *	If model_variables has been allocated previously, this function removes it
 *	from memory.
 */
void Deallocate_State_Space();

/*
 *	Allocates memory for an integer array of size no_variables, this array is
 *	then pointed to by state_variables and no_state_variables is set to
 *	no_variables.
 *	
 *	throws an exception for out of memory
 */
void Allocate_State_Space(int no_variables);

/*
 *	Sets state_variables[index] to value.
 *
 *	throws an exception if index is out of range
 */
void Set_State_Variable(int index, int value);

/*
 *	Copies the values pointed to by variables into state_variables.
 *
 *	throws an exception is variables is NULL.
 */
void Set_State_Space(int* variables);

/*
 *	Returns the value of state_variables[index].
 *
 *	throws an exception if index is out of range.
 */
int Get_State_Variable_Value(int index);

/*
 *	Returns the size of the state space, i.e. no_state_variables.
 */
int Get_Num_State_Variables();

//=============================================================================
//	Helper Functions
//=============================================================================

string State_Variables_To_String();

//==============================================================================
//      IO Functions
//==============================================================================

void Write_State_Space(int fd);

void Read_State_Space(int fd);

#endif
