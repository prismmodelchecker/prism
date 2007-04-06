//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
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

#ifndef _Included_simulator_Updaterheader
#define _Included_simulator_Updaterheader

//=============================================================================
//	Description
//=============================================================================

/*
 *	This interface provides functionality that updates a state of the current
 *	model.  The updates can either be manual or automatic.  In both cases,
 *	the transition reward is calculated and for continuous models the transition
 *	time is also calculated and can be accessed after this calculation.
 *
 *	The loop detection class is meant to be overridden for different 
 *	implementations for path or non-path based loop detection.
 */


//=============================================================================
//	Classes
//=============================================================================

class CLoopDetectionHandler
{
protected:
	bool exploring_deterministically;
	bool proven_looping;
	int proven_loop_start;
	bool deadlock;
public:
	CLoopDetectionHandler();
	virtual void Start_New_Deterministic_Path()=0;
	virtual void Notify_State(int* state_variables)=0;
	virtual void Reset()=0;
	virtual int Path_Size()=0;
	virtual int* Path_At(int i)=0;
	virtual void Notify_Deterministic_Path_End()=0;
	bool Is_Exploring_Deterministically();
	void Set_Proven_Looping(bool b);
	bool Is_Proven_Looping();
	void Set_Proven_Loop_Start(int i);
	int Get_Proven_Loop_Start();
	void Set_Deadlock(bool b);
	bool Is_Deadlock();
	void Notify_Deterministic_State(bool on_the_fly);
};

//=============================================================================
//	Functions
//=============================================================================

/*
 *	If transition_reward has been allocated previously, this function removes it
 *	from memory.
 */
void Deallocate_Updater();

/*
 *	Allocates memory for transition_reward
 *	
 *	throws an exception for out of memory
 */
void Allocate_Updater();

/*
 *	This function performs an update to the state stored in variables.  It 
 *	performs the indexed update calculated by the reasoning interface.
 *	For dtmcs, the value d should always be 1.0, for ctmcs, it should be time
 *	the transition will take, if -1.0 is given, this function will sample the
 *	time appropriately.  The do_transition_rewards parameter should be true
 *	if this is an update to the current state.
 */
void Perform_Update(int index, double d, int*variables, bool do_transition_rewards);

/*
 *	This function performs an update to the state stored in variables.  It 
 *	performs the indexed update calculated by the reasoning interface.
 *	For dtmcs, the value d should always be 1.0, for ctmcs, it should be time
 *	the transition will take, if -1.0 is given, this function will sample the
 *	time appropriately.  This function is used if variables is definately the current
 *	state.
 */
void Perform_Update(int index, double d, int*variables);

/*
 *	This function performs an update to the state stored in variables with a dtmc or mdp
 *	model.  It performs the indexed update calculated by the reasoning interface.
 *	For dtmcs, the value d should always be 1.0, for ctmcs, it should be time
 *	the transition will take, if -1.0 is given, this function will sample the
 *	time appropriately.  The do_transition_rewards parameter should be true
 *	if this is an update to the current state.
 */
void Perform_Update(int index, int*variables, bool do_transition_rewards);

/*
 *	This function performs an update to the state stored in variables with a dtmc or mdp
 *	model.  It performs the indexed update calculated by the reasoning interface.
 *	For dtmcs, the value d should always be 1.0, for ctmcs, it should be time
 *	the transition will take, if -1.0 is given, this function will sample the
 *	time appropriately.  This function is used if variables is definately the current
 *	state.
 */
void Perform_Update(int index, int*variables);

/*
 *	This function automatically selects and performs an update to the current state
 *	stored in state_variables.  It selects the correct functionality for each type of
 *	model.
 */
void Automatic_Update(CLoopDetectionHandler* loop_detection, double& selected_prob);


//==========================
//	Querying functions
//==========================

/*
 *	Function which provides access to the time taken for the last calculated
 *	transition.
 */
double Get_Sampled_Time();

/*
 *	Function which provides access to the reward accumulated for the last
 *	calculated transition.
 */
double Get_Transition_Reward(int i);


#endif
