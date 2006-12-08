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
#ifndef _Included_simulator_Pathheader
#define _Included_simulator_Pathheader

#include "simstate.h"
#include "simreasoning.h"
#include "simupdater.h" 
#include "simutil.h"
#include <stdio.h>
#include <vector>
#include <string>

using std::vector;
using std::string;

//============================================================================
//	Description
//============================================================================

/*
 *	This interface provides the means to explore the state space of the 
 *	current model.  It remembers a path through the state space by storing
 *	all relevant information about it in a vector of CPathState objects.
 *	The path can be generated either manually (by selecting an update from
 *	the update set for the current state or by requesting that the simulator
 *	engine make a number of random updates.
 */

//=============================================================================
//	Classes
//=============================================================================

class CPathLoopDetectionHandler : public CLoopDetectionHandler
{
private:
	int end_loop;
public:
	int start_loop;
	CPathLoopDetectionHandler();
	void Start_New_Deterministic_Path();
	void Notify_State(int* state_variables);
	void Reset();
	int Path_Size();
	int* Path_At(int i);
	void Notify_Deterministic_Path_End();
	void Backtrack(int step);
	void Remove_Preceding_States(int step);
};


//=============================================================================
// Constants
//=============================================================================

//The default maximum number of states than can be stored in the path
#define PATH_NO_CHOICE_MADE -1

//=============================================================================
//	Functions
//=============================================================================

/*
 *	Removes the path from memory.
 */
void Deallocate_Path();

/*
 *	Allocates a new path according to the current model.
 */
void  Allocate_Path();

/*
 *	Starts a new path in the path object
 */
void Start_Path();

/*
 *	Function which handles a choice made by the user from
 *	the current update set.  The update is made and 
 *	the path is updated accordingly.
 */
void Manual_Update(int index);

/*
 *	Function which handles a choice made by the user from
 *	the current update set.  The update is made and 
 *	the path is updated accordingly.  This function
 *	is specific to ctmcs because is allows the user to enter
 *	the time.
 */
void Manual_Update(int, double time_in_state);

/*
 *	Make n automatic updates and store the path.
 */
void Automatic_Choices(int n);
void Automatic_Choices(int n, bool detect);

/*
 *	Removes all states following the given index from
 *	the path and sets the state_variables to that state.
 */
void Backtrack(int index);

/*
 *	Removes all states preceding the given index from
 *	the path.
 */
void Remove_Preceding_States(int);

/*
 *	Returns a string representation of the path
 */
string Path_To_String();

//========================
//	Querying functions
//========================


CPathState* Get_Path_State(int i);
/*
 *	Returns the total cost of the path so far
 */
double Get_Path_Cost(int i);

/*
 *	Returns the total cost of being in the states stored in the path
 */
double Get_Path_State_Cost(int i);

/*
 *	Returns the total cost of all of the path transitions
 */
double Get_Path_Transition_Cost(int i);

/*
 *	Returns the path length
 */
int Get_Path_Length();

/*
 *	Returns the value of the variable stored in the state_index
 *	at var_index
 */
int Get_Path_Data(int var_index, int state_index);

/*
 *	Returns the time spent in the path_state at state_index.
 */
double Get_Time_Spent_In_Path_State(int state_index);

/*
 *	Returns the cumulative time spent in the path_state at state_index.
 */
double Get_Cumulative_Time_Spent_In_Path_State(int state_index);

/*
 *	Returns the reward accumulated in the path state at
 *	state_index.
 */
double Get_State_Reward_Of_Path_State(int state_index, int i);

/*
 *	Returns the reward accumulated in the transition from the
 *	state at state_index.
 */
double Get_Transition_Reward_Of_Path_State(int state_index, int i);

double Get_Total_State_Reward_Of_Path_State(int state_index, int i);

double Get_Total_Transition_Reward_Of_Path_State(int state_index, int i);

/*
 *	Returns the total time for the path.
 */
double Get_Total_Path_Time();


int Get_Chosen_Index_Of_Old_Updates(int step);

bool Is_Proven_Looping();

int Get_Start_Loop();

int Get_End_Loop();


#endif
