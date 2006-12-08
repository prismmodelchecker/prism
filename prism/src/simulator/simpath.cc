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

#include "simpath.h"
#include "simpctl.h"
#include "simupdater.h"
#include "simreasoning.h"
#include "simmodel.h"
#include "simutil.h"
#include <stdio.h>
#include <iostream>

using std::cout;
using std::endl;

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

//============================================================================
//	Local Data
//============================================================================

vector<CPathState*> stored_path; // the path itself	
CPathLoopDetectionHandler* loop_detection; //loop detection for paths
int current_index; //refers to the current index
double path_timer; //the time taken to execute this path
double *path_cost; //the total costs accumulated for this path
double *total_state_cost; //the total state costs for this path
double *total_transition_cost; //the total transition costs for this path

//=============================================================================
//	Class Definitions
//=============================================================================

CPathLoopDetectionHandler::CPathLoopDetectionHandler()
{
	exploring_deterministically = false;
	start_loop = -1;
	end_loop = -1;
	proven_looping = false;
	proven_loop_start = -1;
	deadlock = false;
}

void CPathLoopDetectionHandler::Reset()
{
	exploring_deterministically = false;
	start_loop = -1;
	end_loop = -1;
	proven_looping = false;
	proven_loop_start = -1;
	deadlock = false;
}

void CPathLoopDetectionHandler::Start_New_Deterministic_Path()
{
	exploring_deterministically = true;
	start_loop = current_index;
	proven_looping = false;
	proven_loop_start = -1;
}

void CPathLoopDetectionHandler::Notify_State(int* state_variables)
{
}

int CPathLoopDetectionHandler::Path_Size()
{
	if(start_loop == -1) return 0;
	else
	{
		return (current_index-start_loop)+1;
	}
}

int* CPathLoopDetectionHandler::Path_At(int i)
{
	return stored_path[i+start_loop]->variables;
}

void CPathLoopDetectionHandler::Notify_Deterministic_Path_End()
{
	exploring_deterministically = false;
	start_loop = -1;
	end_loop = -1;
	proven_looping = false;
	proven_loop_start = -1;
}

void CPathLoopDetectionHandler::Backtrack(int step)
{
	// if there is no current deterministic path, nothing to do
	if (!exploring_deterministically) return;
	
	// if we are backtracking further than the start of the current deterministic path,
	// then this path ends
	if (step < start_loop) {
		Notify_Deterministic_Path_End();
	}
	// otherwise we have to re-establish if we are looping
	else {
		proven_looping = false;
		proven_loop_start = -1;
		Notify_Deterministic_State(false);
	}
}

void CPathLoopDetectionHandler::Remove_Preceding_States(int step)
{
	// if there is no current deterministic path, nothing to do
	if (!exploring_deterministically) return;
	
	// if the new start of the path falls inside the current deterministic path,
	// adjust start of deterministic path accordingly and re-establish if we are looping
	if (step >= start_loop) {
		start_loop = 0;
		proven_looping = false;
		proven_loop_start = -1;
		Notify_Deterministic_State(false);
	}
	// otherwise just shift start of path
	else {
		start_loop -= step;
	}
}

//============================================================================
//	Local Function Declarations
//============================================================================

/*
 *	Function which performs all of the necessary update calculations to add
 *	the current state of state_variables to the path.
 */
void Add_Current_State_To_Path();

int Calculate_Old_Choice_Made(int step);

/*
 *	Recalculates all of the registered path formulae for the current path.
 */
void Recalculate_Path_Formulae();

//=============================================================================
//	Functions
//=============================================================================

/*
*	Removes the path from memory.
*/
void Deallocate_Path()
{
	if(&stored_path != NULL)
	{
		for(int i = 0; i < stored_path.size(); i++)
		{
			delete stored_path[i];
		}
		stored_path.clear();
	}
	current_index = -1;
	path_timer = 0.0;
	
	if (path_cost != NULL) delete[] path_cost;
	path_cost = NULL;
	if (total_state_cost != NULL) delete[] total_state_cost;
	total_state_cost = NULL;
	if (total_transition_cost != NULL) delete[] total_transition_cost;
	total_transition_cost = NULL;
	
	if(loop_detection != NULL)
		delete loop_detection;
	loop_detection = NULL;
}

/*
*	Allocates a new path according to the current model.
*
*	throws an exception if there is not enough memory.
*/
void Allocate_Path()
{
	if(&stored_path == NULL)
	{
		Report_Error("Out of memory when allocating path.");
		throw "out of memory error: simpath.cc 001";
	}
	else
	{
		if(stored_path.size() > 0) stored_path.clear();
		//16 is the starting length, this will multiply by 2 when needed
		for(int i = 0; i < 16; i++)
		{
			CPathState* ps = new CPathState();
			if(ps == NULL)
			{
				Report_Error("Out of memory when allocating path.");
				throw "out of memory error: simpath.cc 002";
			}
			stored_path.push_back(ps);
		}
	}

	current_index = -1;
	path_timer = 0.0;
	
	path_cost = new double[no_reward_structs];
	total_state_cost = new double[no_reward_structs];
	total_transition_cost = new double[no_reward_structs];
	if(path_cost == NULL || total_state_cost == NULL || total_transition_cost == NULL)
	{
		Report_Error("Out of memory when allocating path.");
		throw "out of memory error: simpath.cc";
	}
	for(int i = 0; i < no_reward_structs; i++) {
		path_cost[i] = 0.0;
		total_state_cost[i] = 0.0;
		total_transition_cost[i] = 0.0;
	}

	loop_detection = new CPathLoopDetectionHandler();
}

/*
*	Starts a new path in the path object
*/
void Start_Path()
{
	loop_detection->Reset();

	current_index = -1;
	path_timer = 0.0;
	for(int i = 0; i < no_reward_structs; i++) {
		path_cost[i] = 0.0;
		total_state_cost[i] = 0.0;
		total_transition_cost[i] = 0.0;
	}

	Add_Current_State_To_Path();

	//Set the first set of updates
	Calculate_Updates(state_variables);
	Calculate_State_Reward(state_variables);

	if(Are_Updates_Deterministic())
	{
		loop_detection->Notify_Deterministic_State(false);
	}
	else
	{
		loop_detection->Notify_Deterministic_Path_End();
	}
}

/*
*	Function which handles a choice made by the user from
*	the current update set.  The update is made and 
*	the path is updated accordingly.
*/
void Manual_Update(int index)
{
	if(model_type == STOCHASTIC)
	{
		Perform_Update(index, -1.0, state_variables); //calculate time_taken
	}
	else
	{
		Perform_Update(index, state_variables);
	}
	stored_path[current_index]->choice_made = index;
	Add_Current_State_To_Path();

	Calculate_Updates(state_variables);
	Calculate_State_Reward(state_variables);

	// check for looping
	if(Are_Updates_Deterministic())
	{
		loop_detection->Notify_Deterministic_State(false);
	}
	else
	{
		loop_detection->Notify_Deterministic_Path_End();
	}
}

/*
*	Function which handles a choice made by the user from
*	the current update set.  The update is made and 
*	the path is updated accordingly.  This function
*	is specific to ctmcs because is allows the user to enter
*	the time.
*/
void Manual_Update(int index, double time_in_state)
{
	Perform_Update(index, time_in_state, state_variables); //calculate time_taken
	
	stored_path[current_index]->choice_made = index;
	Add_Current_State_To_Path();
	
	Calculate_Updates(state_variables);
	Calculate_State_Reward(state_variables);
	
	// check for looping
	if(Are_Updates_Deterministic())
	{
		loop_detection->Notify_Deterministic_State(false);
	}
	else
	{
		loop_detection->Notify_Deterministic_Path_End();
	}
}

/*
*	Make n automatic updates and store the path.
*/
void Automatic_Choices(int n){ Automatic_Choices(n, true); }

void Automatic_Choices(int n, bool detect)
{
	for(int i = 0;  i < n; i++) {
		if (detect && loop_detection->Is_Proven_Looping()) break;
		if (loop_detection->Is_Deadlock()) break;
		
		double probability = 0.0;
		Automatic_Update(loop_detection, probability);
		
		// Because we cannot guarantee that we know the selected index, we have to show this.
		stored_path[current_index]->choice_made = PATH_NO_CHOICE_MADE;
		stored_path[current_index]->probability = probability;
		// Unless requested not to (detect==false), this function will stop exploring when a loop is detected.
		// Because Automatic_Update() checks for loops before making a transition, we overshoot.
		// Hence at this point if we are looping we step back a state,
		// i.e. reset state_variables and don't add new state to the path.
		if (detect && loop_detection->Is_Proven_Looping()) {
			stored_path[current_index]->Make_Current_State_This();
		}
		else {
			// Add state to path (unless we have stayed in the same state because of deadlock).
			if (!loop_detection->Is_Deadlock()) Add_Current_State_To_Path();
		}
		Calculate_State_Reward(state_variables);
	}
	
	Calculate_Updates(state_variables);
	
	// check for looping
	if(Are_Updates_Deterministic())
	{
		loop_detection->Notify_Deterministic_State(false);
	}
	else
	{
		loop_detection->Notify_Deterministic_Path_End();
	}
}

/*
*	Removes all states following the given index from
*	the path and sets the state_variables to that state.
*/
void Backtrack(int step)
{
	if(step > current_index)
	{
		Report_Error("Attempt to backtrack to an invalid index.");
		throw "out of range error: simpath.cc 020";
	}
	
	// if go back at least one step, escape deadlock
	if (step < current_index) loop_detection->Set_Deadlock(false);
	
	current_index = step;
	
	//copy the state stored in this path to model_variables
	stored_path[current_index]->Make_Current_State_This();
	
	//recalculate timer and rewards
	path_timer = 0.0;
	for(int i = 0; i < no_reward_structs; i++) {
		path_cost[i] = 0.0;
		total_state_cost[i] = 0.0;
		total_transition_cost[i] = 0.0;
	}
	
	for(int i = 0; i < current_index; i++)
	{
		if(stored_path[i]->time_known)
			path_timer += stored_path[i]->time_spent_in_state;
		
		for (int j = 0; j < no_reward_structs; j++) {
			total_state_cost[j] += stored_path[i]->state_cost[j];
			total_transition_cost[j] += stored_path[i]->transition_cost[j];
		}
	}
	for (int j = 0; j < no_reward_structs; j++) {
		path_cost[j] = total_state_cost[j] + total_transition_cost[j];
	}
	
	Recalculate_Path_Formulae();
	
	Calculate_State_Reward(state_variables);
	
	Calculate_Updates(state_variables);
	
	loop_detection->Backtrack(step);
}

/*
*	Removes all states preceding the given index from
*	the path.
*/
void Remove_Preceding_States(int step)
{
	if(step > current_index)
	{
		Report_Error("Attempt to remove states before an invalid index.");
		throw "out of range error: simpath.cc 021";
	}
	
	// shift later states down path, move unneeded ones to end
	int i;
	CPathState** prefix = new CPathState*[step];
	for(i = 0; i < step; i++) prefix[i] = stored_path[i];
	for(i = 0; i <= current_index-step; i++) stored_path[i] = stored_path[i+step];
	for(i = current_index-step+1; i <= current_index; i++) stored_path[i] = prefix[i-current_index+step-1];
	delete prefix;
	current_index -= step;
	
	//recalculate timer and rewards
	path_timer = 0.0;
	for(int i = 0; i < no_reward_structs; i++) {
		path_cost[i] = 0.0;
		total_state_cost[i] = 0.0;
		total_transition_cost[i] = 0.0;
	}
	
	for(int i = 0; i < current_index; i++)
	{
		if(stored_path[i]->time_known)path_timer += stored_path[i]->time_spent_in_state;
		for (int j = 0; j < no_reward_structs; j++) {
			total_state_cost[j] += stored_path[i]->state_cost[j];
			total_transition_cost[j] += stored_path[i]->transition_cost[j];
		}
	}
	for (int j = 0; j < no_reward_structs; j++) {
		path_cost[j] = total_state_cost[j] + total_transition_cost[j];
	}
	
	Recalculate_Path_Formulae();
	
	Calculate_Updates(state_variables);
	
	loop_detection->Remove_Preceding_States(step);
}

//========================
//	Querying functions
//========================

CPathState* Get_Path_State(int i)
{
	return stored_path[i];
}

/*
*	Returns the total cost of the path so far
*/
double Get_Path_Cost(int i)
{
	return path_cost[i];
}

/*
*	Returns the total cost of being in the states stored in the path
*/
double Get_Path_State_Cost(int i)
{
	return total_state_cost[i];
}

/*
*	Returns the total cost of all of the path transitions
*/
double Get_Path_Transition_Cost(int i)
{
	return total_transition_cost[i];
}

/*
*	Returns the path length
*/
int Get_Path_Length()
{
	return current_index+1;
}

/*
*	Returns the value of the variable stored in the state_index
*	at var_index
*/
int Get_Path_Data(int var_index, int state_index)
{
	CPathState* state = stored_path[state_index];
	if(state == NULL)
	{
		Report_Error("Unexpected error: attempt to access state that is not in range of path");
		throw "out of range error simpath.cc 030";
	}
	else
	{
		if(var_index > no_state_variables || var_index < 0)
		{
			Report_Error("Unexpected error: attempt to access state variable that is out of range");
			throw "out of range error simpath.cc 031";
		}
		return state->variables[var_index];
	}
}


/*
*	Returns the time spent in the path_state at state_index.
*/
double Get_Time_Spent_In_Path_State(int state_index)
{
	if(state_index == current_index) return UNDEFINED_DOUBLE;
	else
	{
		CPathState* ps = stored_path[state_index];
		//cout << ps->To_String() << endl;
		return ps->time_spent_in_state;
	}
}

/*
*	Returns the cumulative time spent in the path_state at state_index.
*/
double Get_Cumulative_Time_Spent_In_Path_State(int state_index)
{
	if(state_index == current_index) return UNDEFINED_DOUBLE;
	else
	{
		CPathState* ps = stored_path[state_index];
		//cout << ps->To_String() << endl;
		return ps->cumulative_time_spent_in_state;
	}
}

/*
*	Returns the reward accumulated in the path state at
*	state_index.
*/
double Get_State_Reward_Of_Path_State(int state_index, int i)
{
	if(state_index > current_index) return UNDEFINED_DOUBLE;
	else
		return stored_path[state_index]->state_instant_cost[i];
}

/*
*	Returns the reward accumulated in the transition from the
*	state at state_index.
*/
double Get_Transition_Reward_Of_Path_State(int state_index, int i)
{
	if(state_index >= current_index) return UNDEFINED_DOUBLE;
	else
		return stored_path[state_index]->transition_cost[i];
}

double Get_Total_State_Reward_Of_Path_State(int state_index, int i)
{
	if(state_index > current_index) return UNDEFINED_DOUBLE;
	else
		return stored_path[state_index]->cumulative_state_cost[i];
}

double Get_Total_Transition_Reward_Of_Path_State(int state_index, int i)
{
	if(state_index > current_index) return UNDEFINED_DOUBLE;
	else
		return stored_path[state_index]->cumulative_transition_cost[i];
}

bool Is_Proven_Looping()
{
	return loop_detection->Is_Proven_Looping();
}

int Get_Start_Loop()
{
	return loop_detection->start_loop + loop_detection->Get_Proven_Loop_Start();
}

int Get_End_Loop()
{
	return current_index;
}

/*
*	Returns the total time for the path.
*/
double Get_Total_Path_Time()
{
	return path_timer;
}

//TODO this
int Get_Chosen_Index_Of_Old_Updates(int step)
{
	int chosen = stored_path[step]->choice_made;
	if(chosen >= 0) return chosen;
	else
	{
		//If we get here, we do not have the data, probably because
		//the update was chosen automatically on the fly and so
		//the index was not known.
		//Therefore, we must work it out.
		//cout << "Attempting to recalculate the old_choice_made" << endl;
		stored_path[step]->choice_made = Calculate_Old_Choice_Made(step);
		return stored_path[step]->choice_made;
	}
}



//==============================================================================
//	To String helper functions
//==============================================================================

/*
 *	Returns a string representation of the path.
 *	TODO this
 */
string Path_To_String()
{
	return "";
	/*std::string returner = "Current Path:\n\n";
	if(thePath == NULL) return returner += "No Path Exists";
	else return returner += thePath->To_String();	*/
}


//============================================================================
//	Local Functions
//============================================================================

inline void Add_Current_State_To_Path()
{
	current_index++;

	if(current_index >= stored_path.size())
	{
		//need to allocate some more path
		int size;
		if(stored_path.size() > 0) //should never be, but just in case
		{
			size = 16;
		}
		else //double it
		{
			size = stored_path.size();
		}
		for(int i = 0; i < size; i++)
		{
			CPathState* ps = new CPathState();
			if(ps == NULL)
			{
				Report_Error("Out of memory when allocating new path");
				throw "out of memory error: simpath.cc 003";
			}
			stored_path.push_back(ps);			
		}
	}

	CPathState* curr_state = stored_path[current_index];
	curr_state->Make_This_Current_State();
	
	Calculate_State_Reward(curr_state->variables);
	for(int i = 0; i < no_reward_structs; i++) {
		curr_state->state_instant_cost[i] = Get_State_Reward(i);
	}

	if(current_index > 0)
	{
		CPathState * last_state = stored_path[current_index-1];
		
		if(model_type == STOCHASTIC)
		{
			double time_in_state = Get_Sampled_Time();
			
			last_state->time_spent_in_state = (time_in_state);
			
			if (current_index > 1)
			{
				CPathState * state_before_last_state = stored_path[current_index-2];
				last_state->cumulative_time_spent_in_state = last_state->time_spent_in_state + state_before_last_state->cumulative_time_spent_in_state;
			}
			else
			{
				last_state->cumulative_time_spent_in_state = last_state->time_spent_in_state;
			}
			
			last_state->time_known = true;
			path_timer += time_in_state;
			
			for(int i = 0; i < no_reward_structs; i++) {
				last_state->state_cost[i] = (last_state->state_instant_cost[i]*time_in_state);
				last_state->transition_cost[i] = (Get_Transition_Reward(i));
				
				total_state_cost[i] += last_state->state_instant_cost[i]*time_in_state;
				total_transition_cost[i] += Get_Transition_Reward(i);
				path_cost[i] = total_state_cost[i] + total_transition_cost[i];
				
				last_state->cumulative_state_cost[i] = total_state_cost[i];
				last_state->cumulative_transition_cost[i] = total_transition_cost[i];				
			}
			
			Notify_Path_Formulae(last_state, state_variables, loop_detection);

		}
		else
		{
			//current_index++;

			for(int i = 0; i < no_reward_structs; i++) {
				last_state->state_cost[i] = (last_state->state_instant_cost[i]);
				last_state->transition_cost[i] = (Get_Transition_Reward(i));

				total_state_cost[i] += last_state->state_instant_cost[i];
				total_transition_cost[i] += Get_Transition_Reward(i);
				path_cost[i] = total_state_cost[i] + total_transition_cost[i];
				
				last_state->cumulative_state_cost[i] = total_state_cost[i];
				last_state->cumulative_transition_cost[i] = total_transition_cost[i];
			}
			
			Notify_Path_Formulae(last_state, state_variables, loop_detection); 
		}
	}
}


// TODO This doesn't actually work at the moment, and I don't know why!!!
inline int Calculate_Old_Choice_Made(int step)
{
	int* next_vars = Get_Path_State(step+1)->variables;
	int* orig_vars = Get_Path_State(step)->variables;

	int* copy_vars = new int[no_state_variables];

	double expectedProb = Get_Path_State(step)->probability;


	int selected_index = 0; //if all else fails choose 0
	//This assumes that the update set represents the correct old upates

	//cout << "Calculate_Old_Choice_Made(" << step<<")" <<endl;
	for(int i = 0; i < Get_No_Updates(); i++)
	{
		CFinalUpdate* upd = Get_Update(i);
		//cout << "looking at update: " << endl;
		//upd->Print_Update();
		Copy_Int_Array(orig_vars, copy_vars, no_state_variables);
		upd->Perform_Update(copy_vars);
		//cout << "This is the array of the next state..." << endl;
		//Print_Array(next_vars, no_state_variables);
		//cout << "This is the array of the compare state for this update" << endl;
		//Print_Array(copy_vars, no_state_variables);

		if(Int_Arrays_Equals(next_vars, copy_vars, no_state_variables))
		{
			//cout << "this is the same, now check the probability" << endl;
			//cout << "expectedProb = " << expectedProb << " actualProb = " << upd->probability << endl;
			if(upd->probability == expectedProb)
			{
				//cout << "selecting an index as: " << i << endl;
				selected_index = i;
				break;
			}
		}
	}

	delete[]copy_vars;
	return selected_index;
}

inline void Recalculate_Path_Formulae()
{
	//first rest each path formula
	Reset_Path_Formulae();

	CPathState* last_state = NULL;
	int* curr_state = stored_path[0]->variables;
	Notify_Path_Formulae(last_state, curr_state, loop_detection);
	
	for(int j = 1; j<=current_index; j++)
	{
		last_state = stored_path[j-1];
		curr_state = stored_path[j]->variables;
		Notify_Path_Formulae(last_state, curr_state, loop_detection);
	}
}


