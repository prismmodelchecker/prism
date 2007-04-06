//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
//	
//------------------------------------------------------------------------------
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

#include <stdio.h>
#include <string>
#include <math.h>
#include "simupdater.h"
#include "simstate.h"
#include "simreasoning.h"
#include "simrandom.h"
#include "simutil.h"


using std::cout;
using std::endl;
using std::string;
using std::vector;

//=============================================================================
//	Description
//=============================================================================

/*
 *	This interface provides functionality that updates a state of the current
 *	model.  The updates can either be manual or automatic.  In both cases,
 *	the transition reward is calculated and for continuous models the transition
 *	time is also calculated and can be accessed after this calculation.
 *
 *	The loop detection interface is meant to be overridden for different 
 *	implementations for path or non-path based loop detection.
 */

//=============================================================================
//	Class Definitions
//=============================================================================

CLoopDetectionHandler::CLoopDetectionHandler()
{
	exploring_deterministically = false;
	proven_looping = false;
	proven_loop_start = -1;
	deadlock = false;
}

bool CLoopDetectionHandler::Is_Exploring_Deterministically()
{				
	return exploring_deterministically;
}

bool CLoopDetectionHandler::Is_Proven_Looping()
{
	return proven_looping;
}

void CLoopDetectionHandler::Set_Proven_Looping(bool pr)
{
	proven_looping = pr;
}

int CLoopDetectionHandler::Get_Proven_Loop_Start()
{
	return proven_loop_start;
}

void CLoopDetectionHandler::Set_Proven_Loop_Start(int st)
{
	proven_loop_start = st;
}

bool CLoopDetectionHandler::Is_Deadlock()
{
	return deadlock;
}

void CLoopDetectionHandler::Set_Deadlock(bool dl)
{
	deadlock = dl;
}

/*
 *	When the updates to a particular state are deterministic, this
 *	method detects we are now in a loop.
 */
void CLoopDetectionHandler::Notify_Deterministic_State(bool on_the_fly)
{
	//if not previously exploring deterministically
	//set the start of a potential loop to this step
	if(!Is_Exploring_Deterministically()) 
	{
		Start_New_Deterministic_Path();
		Notify_State(state_variables);
		
		// this function now reports a loop if the current state has been seen before.
		// we don't look ahead from this state to see if there *will* be a loop in the next step.
		// hence the code below is redundant.
		
		/*if(on_the_fly)
		{
			//if we have calculated the update on the fly, if we get here, we really need to
			//actually calculate the updates...
			Calculate_Updates(state_variables);
		}
		
		//check to see whether applying all current updates to the current state will create a loop
		int* compare_state = new int[no_state_variables];
		bool all_same = true;
		for(int i = 0; i < Get_No_Updates(); i++)
		{
			Copy_Int_Array(state_variables, compare_state, no_state_variables);
			Execute_Update(i, compare_state);
			if(!Int_Arrays_Equals(state_variables, compare_state, no_state_variables))
			{
				all_same = false;
				break;
			}
		}
		delete[] compare_state;
		if(all_same)
		{
			Set_Proven_Looping(true);
			Set_Proven_Loop_Start(0);
		} */
	}
	else //this means that we are already looping deterministically
	{
		Notify_State(state_variables);
		
		for(int i = 0; i < Path_Size()-1; i++)
		{
			if(Int_Arrays_Equals(Path_At(i), state_variables, no_state_variables))
			{
				Set_Proven_Looping(true);
				Set_Proven_Loop_Start(i);
				break;
			}
		}
	}
}

//=============================================================================
// Local Function Declarations
//=============================================================================

/*
 *	Local function which handles automatic updates to the state stored in
 *	state_variables for dtmcs and mdps.
 */
inline void Automatic_Update_Discrete(CLoopDetectionHandler* loop_handler, double& selected_probability);

/*
 *	Local function which handles automatic updates to the state stored in
 *	state_variables for ctmcs.
 */
inline void Automatic_Update_Continuous(CLoopDetectionHandler* loop_handler, double& selected_probability);


//=============================================================================
//	Local Data
//=============================================================================

double sampled_time; //The time taken to do the last transition
double *transition_reward;  //The rewards accumulated for the last transition


//=============================================================================
//	Functions
//=============================================================================

/*
 *	If transition_reward has been allocated previously, this function removes it
 *	from memory.
 */
void Deallocate_Updater()
{
	if(transition_reward != NULL)
	{
		delete[] transition_reward;
	}
	transition_reward = NULL;
}

/*
 *	Allocates memory for transition_reward
 *	
 *	throws an exception for out of memory
 */
void Allocate_Updater()
{
	transition_reward = new double[no_reward_structs];
	
	if(transition_reward == NULL) 
	{
		Report_Error("Simulator engine ran out of memory when allocating updater.");
		throw "out of memory exception simupdater.cc";
	}
	
	//Initially all rewards are zero
	for(int i = 0; i < no_reward_structs; i++)
	{
		transition_reward[i] = 0.0;
	}
}

/*
 *	This function performs an update to the state stored in variables.  It 
 *	performs the indexed update calculated by the reasoning interface.
 *	For dtmcs, the value d should always be 1.0, for ctmcs, it should be time
 *	the transition will take, if -1.0 is given, this function will sample the
 *	time appropriately.  The do_transition_rewards parameter should be true
 *	if this is an update to the current state.
 */
void Perform_Update(int index, double d, int*variables, bool do_transition_rewards)
{
	if(index >= Get_No_Updates())
	{
		Report_Error("Attempt to access out of bounds update");
		throw "Out of bounds update: simupdate.cc 001";
	}
	
	if(d < 0.0) // for ctmcs, we should make a guess at the time taken
	{
		double sum_rates = Calculate_Sum_Rates();
		sampled_time = (-1*log(Random_Uniform()))/sum_rates;
	}
	else
	{
		sampled_time = d;
	}

	if(do_transition_rewards)
	{
		for(int i = 0; i < no_reward_structs; i++) {
			transition_reward[i] = 0.0;
			for(int j = 0; j < no_transition_rewards[i]; j++)
			{
				CTransitionReward* rew = transition_rewards_table[i][j];
				transition_reward[i] += rew->Get_Reward_For_Selected_Transition(variables, Get_Action_Index_Of_Update(index));
			}
		}
	}

	int no_assignments = Get_Num_Assignments_Of_Update(index);

	Execute_Update(index, variables);
}

/*
 *	This function performs an update to the state stored in variables.  It 
 *	performs the indexed update calculated by the reasoning interface.
 *	For dtmcs, the value d should always be 1.0, for ctmcs, it should be time
 *	the transition will take, if -1.0 is given, this function will sample the
 *	time appropriately.  This function is used if variables is definately the current
 *	state.
 */
void Perform_Update(int index, double d, int*variables)
{
	Perform_Update(index, d, variables, true);
}

/*
 *	This function performs an update to the state stored in variables with a dtmc or mdp
 *	model.  It performs the indexed update calculated by the reasoning interface.
 *	For dtmcs, the value d should always be 1.0, for ctmcs, it should be time
 *	the transition will take, if -1.0 is given, this function will sample the
 *	time appropriately.  The do_transition_rewards parameter should be true
 *	if this is an update to the current state.
 */
void Perform_Update(int index, int*variables, bool do_transition_rewards)
{
	Perform_Update(index, 1.0, variables, do_transition_rewards);
}

/*
 *	This function performs an update to the state stored in variables with a dtmc or mdp
 *	model.  It performs the indexed update calculated by the reasoning interface.
 *	For dtmcs, the value d should always be 1.0, for ctmcs, it should be time
 *	the transition will take, if -1.0 is given, this function will sample the
 *	time appropriately.  This function is used if variables is definately the current
 *	state.
 */
void Perform_Update(int index, int*variables)
{
   Perform_Update(index, 1.0, variables, true);
}

/*
 *	This function automatically selects and performs an update to the current state
 *	stored in state_variables.  It selects the correct functionality for each type of
 *	model.
 *
 *	Returns the probability of the update that was selected.
 */
void Automatic_Update(CLoopDetectionHandler* loop_detection, double& selected_prob)
{
	if(model_type == PROBABILISTIC)
	{
		Automatic_Update_Discrete(loop_detection, selected_prob);
	}
	else if(model_type == NONDETERMINISTIC)
	{
		Automatic_Update_Discrete(loop_detection, selected_prob);
	}
	else if(model_type == STOCHASTIC)
	{
		Automatic_Update_Continuous(loop_detection, selected_prob);
	}
}


//==========================
//	Querying functions
//==========================

/*
 *	Function which provides access to the time taken for the last calculated
 *	transition.
 */
double Get_Sampled_Time()
{
	return sampled_time;
}

/*
 *	Function which provides access to the reward accumulated for the last
 *	calculated transition.
 */
double Get_Transition_Reward(int i)
{
	return transition_reward[i];
}


//=============================================================================
//	Local Functions
//=============================================================================

/*
 *	Local function which handles automatic updates to the state stored in
 *	state_variables for dtmcs and mdps.
 */
inline void Automatic_Update_Discrete(CLoopDetectionHandler* loop_handler, double& selected_prob)
{	
	bool up_determ = false;
	CUpdate* choice = Choose_Discrete_Update_On_The_Fly(up_determ, selected_prob);
	
	//Because the things are calculated on the fly, need to do loop detection here....
	//sort out loop detection
	if(up_determ)
	{
		loop_handler->Notify_Deterministic_State(true);
	}
	else
	{
		loop_handler->Notify_Deterministic_Path_End();
	}
	// if we are not in a deadlock, move to the next state
	if(choice != NULL) {
		for(int i = 0; i < no_reward_structs; i++) {
			transition_reward[i] = 0.0;
			for(int j = 0; j < no_transition_rewards[i]; j++)
			{
				CTransitionReward* rew = transition_rewards_table[i][j];
				transition_reward[i] += rew->Get_Reward_For_Selected_Transition(state_variables, choice->action_index);
			}
		}
		choice->Do_Update(state_variables);
	}
	// otherwise stay in this state and store fact that this is a deadlock
	else {
		loop_handler->Set_Deadlock(true);
	}
}

/*
 *	Local function which handles automatic updates to the state stored in
 *	state_variables for ctmcs.
 */
inline void Automatic_Update_Continuous(CLoopDetectionHandler* loop_handler, double& selected_prob)
{
	Calculate_Updates(state_variables);
	
	bool updeterm = Are_Updates_Deterministic();
	
	if(updeterm)
	{
		loop_handler->Notify_Deterministic_State(false);
	}
	else
	{
		loop_handler->Notify_Deterministic_Path_End();
	}
	
	double sum_rates = Calculate_Sum_Rates();
	
	int choice = Choose_Continuous_Update(sum_rates);
	
	// If there was a deadlock, we assume that there is a single self-loop with rate 1.
	if(choice < 0) sum_rates = 1;
	
	// Sample random time delay.
	double random_num = 0;
	while(random_num == 0) // 0 completely screws up everything... remove it
	{
		random_num = Random_Uniform();
	}
	sampled_time = (-1*log(random_num))/sum_rates;
	
	// If no deadlock...
	if (choice >= 0) {
		for(int i = 0; i < no_reward_structs; i++) {
			transition_reward[i] = 0.0;
			for(int j = 0; j < no_transition_rewards[i]; j++)
			{
				CTransitionReward* rew = transition_rewards_table[i][j];
				transition_reward[i] += rew->Get_Reward_For_Selected_Transition(state_variables, Get_Action_Index_Of_Update(choice));
			}
		}
		selected_prob = Get_Update(choice)->probability;
		Execute_Update(choice, state_variables);
	}
	// If deadlock...
	else {
		selected_prob = 1.0;
		loop_handler->Set_Deadlock(true);
		// note: no transition rewards in this case obviously
	}
}
