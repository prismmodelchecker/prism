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

#ifndef _Included_simulator_Reasoningheader
#define _Included_simulator_Reasoningheader

#include <string>
#include <vector>
#include "simmodel.h"

using std::string;
using std::vector;

//=============================================================================
// Description
//=============================================================================

/*
 *	This interface allows reasoning to be performed on the model for any
 *	valid configuration of the current state.  It allows the calculation of
 *	updates to a state and also the calculation of rewards of
 *	the a state.
 *	
 *	The results can be queryed by a series of access methods.
 */

//=============================================================================
//	Constants
//=============================================================================

const int DEFAULT_MAX_NO_UPDATES = 10;
const int DEFAULT_MAX_NO_ASSIGNMENTS = 10;

//=============================================================================
//	Classes
//=============================================================================

/*
 *	CFinalUpdate
 *	============
 *	
 *	The CFinalUpdate class is responsible for storing the updates calculated
 *	as potential transitions of the current state.  Each update has a value for
 *	probability (rate), module index, action index and a list of pointers
 *	to the assignments in the model data structures.
 *
 */
class CFinalUpdate
{
private:
	bool all_defined; // have all assignments been defined?
	int max_assignments; //stored so we can destruct
public:
	double probability;
	int probability_distribution;
	int action_index;
	int module_index;
	vector<CAssignment*> assignments;
	int no_assignments;

	/*
	 *	Constructor: populates an empty update
	 */
	CFinalUpdate(int max_assignments);

	/*
	 *	Destructor for a CFinalUpdate object.  This only
	 *	needs to remove all of the pointers to assignments
	 *	from the assignments vector.  It does not need
	 *	to delete the assingments themselves.
	 */
	~CFinalUpdate();

	/*
	 *	Populates this CFinalUpdate object with all of the relevant
	 *	information to make it a valid update.  This method
	 *	extracts all of the assignments from upd.
	 */
	void Set(double probability, int distribution_index,
		int action_index, int module_index,
		CUpdate * upd);

	/*
	 *	For DTMCs, it is necessary to normalise the probabilities if
	 *	there is more than one probability distribution.  This method
	 *	is responsible for doing this.
	 */
	void Normalise(int no_distributions);

	/*
	 *	Prints a representation of this CFinalUpdate object to the command line.
	 */
	void Print_Update();

	/*
	 *	Executes the assignments pointed to by the assignments vector for this
	 *	CFinalUpdate object.  The assignments are performed on the variable 
	 *	array parameter.  The temporary results for the current state are
	 *	first calculates and then they are applied together.
	 */
	void Perform_Update(int*variables);
};


//=============================================================================
//	Functions
//=============================================================================

/*
 *	Delete the data structures required to reason about a state of the model.
 */
void Deallocate_Reasoning();

/*
 *	Allocate the resources for reasoning about the current state of the model.
 *	Some of the data allocated is dependent upon the current loaded model,
 *	and it is a prerequisite that the model has been successfully loaded.
 */
void Allocate_Reasoning();


//==============================================
//Functions to reason about a particular state
//==============================================

/*
 *	Populates the_updates with the_updates appropriate for the given state
 */
void Calculate_Updates(int* variables);

/*
 *	Calculates the reward of being in the state describes by the variables parameter.
 */
void Calculate_State_Reward(int* variables);

/*
 *	Counts the number of synchronous commands in the true_commands data structure
 *	for a particular action parameterised by synch_index
 */
int Count_Synchronous_Commands(int synch_index);

/*
 *	Counts the number of asynchronous command in the true_commands data structure.
 */
int Count_Asynchronous_Commands();

//=====================================
//Actually carry out one of the updates
//=====================================

/*
 *	Actually perform the calculated update at updates[index] on
 *	the given variable set.
 */
void Execute_Update(int index, int* variables);


/*
 *	This function makes a random choice from the update set
 *	so long as the model is continuous time and sum_rates
 *	is indeed the sum of the rates in the update set.
 */
int Choose_Continuous_Update(double sum_rates);

/*
 *	This function makes a random selection according
 *	to the current state (state_variables) for discrete
 *	time markov chains and markov decision processes.
 *	It does not need to complete the entire update
 *	set.
 */
CUpdate* Choose_Discrete_Update_On_The_Fly(bool& up_determ, double& selected_prob);

//=====================================
//Functions to query calculated results
//=====================================

/*
 *	Reasons about the calculated update set to say whether it 
 *	is deterministic.
 */
bool Are_Updates_Deterministic();

/*
 *	States whether the updates to the given variable set will be determninistic without
 *	having to first calcualte the whole update set.
 */
bool Are_Updates_Deterministic_On_The_Fly(int* variables, int num_asynch, int num_synch);

double Calculate_Sum_Rates();

/*
 *	Returns the reward calcualated by calling Calculate_State_Reward()
 */
double Get_State_Reward(int i);



//=======================================================
//	Methods for accessing the CUpdates objects theUpdates 
//=======================================================

/*
 *	Returns the current size of the update set.
 */
int Get_No_Updates();

/*
 *	Returns the probability of the update at the given index
 */
double Get_Probability_Of_Update(int update_index);

/*
 *	Returns the probability distribution index of the update at the given index
 */
int Get_Distribution_Index_Of_Update(int update_index);

/*
 *	Returns the action index of the update at the given index
 */
int Get_Action_Index_Of_Update(int update_index);

/*
 *	Returns the module index of the update at the given index
 */
int Get_Module_Of_Update(int update_index);

int Get_Result_Of_Update(int* variables, int update_index, int var_index);

/*
 *	Returns the number of assignments of the updates at the given index.
 */
int Get_Num_Assignments_Of_Update(int update_index);

/*
 *	Returns the state index of the update_indexth updates assignment_indexth assignment.
 */
int Get_Assignment_Variable_Index_Of_Update(int update_index, int assignment_index);

int Get_Assignment_Value_Of_Update(int update_index, int assignment_index);

/*
 *	Returns the calculated update at the given index.  Because these updates
 *	are preallocated, if it is required that more are needed than have been
 *	allocated, then this method doubles the allocation of potential update
 *	storage.
 *
 *	throws an exception if out of memory
 */
CFinalUpdate* Get_Update(int index);

void Print_Updates();


#endif
