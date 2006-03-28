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

#include "simreasoning.h"
#include "simutil.h"
#include "simstate.h"
#include "simmodel.h"
#include "simrandom.h"
#include <vector>
#include <cstdio>
#include <string>

using std::string;
using std::cout;
using std::endl;
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
//	Local Function Declarations
//=============================================================================

/*
 *	Returns the calculated update at the given index.  Because these updates
 *	are preallocated, if it is required that more are needed than have been
 *	allocated, then this method doubles the allocation of potential update
 *	storage.
 *
 *	throws an exception if out of memory
 */
//inline CFinalUpdate* Get_Update(int index);
CFinalUpdate* Get_Update(int index); // Mac OS X doesn't like this being inline for some reason

/*
 *	Finds all of the commands that are true in the command table for the given
 *	variables and populates the true_commands array with pointers to them
 *	also sets num_distinct_actions
 */
inline void Find_True_Commands(int* variables);

/*
 *	Forms the product of what is in the sorted_slot ([no_modules][synch_index]
 *	and what is in the given slot [synch_index][module_index]
 */
inline void Product_Commands(int synch_index, int module_index);

/*
 *	Forms the product of the two CCommand parameters. i.e. the guards
 *	are ignored, but the product of the updates is created.
 */
inline CCommand* Product_Two_Commands(CCommand* comm1, CCommand* comm2);

/*
 *	Forms the product of two updates.  The probability or rate, is
 *	simply the product of the two and the assignment set is the
 *	union of the two product sets.
 */
inline CUpdate* Merge_Two_Updates(CUpdate* upd1, CUpdate* upd2);

/*
 *	Outputs the contents of true_commands to the command line  .
 */	
inline void Print_True_Commands();


//=============================================================================
//	Class Definitions
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

/*
 *	Constructor: populates an empty update
 */
CFinalUpdate::CFinalUpdate(int max_assignments)
{
	this->action_index = -1;
	this->module_index = -1;
	this->max_assignments = max_assignments;
	if(&assignments == NULL)
	{
		Report_Error("Out of memory when allocating reasoning data structures");
		throw string("Out of memory error: simreasoning.cc 008");
	}
	assignments.reserve(max_assignments);
	
	this->no_assignments = 0;
	this->all_defined = false;

	this->probability = UNDEFINED_DOUBLE;
	this->probability_distribution = -1;
}

/*
 *	Destructor for a CFinalUpdate object.  This only
 *	needs to remove all of the pointers to assignments
 *	from the assignments vector.  It does not need
 *	to delete the assingments themselves.
 */
CFinalUpdate::~CFinalUpdate()
{
	//only delete the array not the whole thing
	if(&assignments != NULL)
		assignments.clear();
}

/*
 *	Populates this CFinalUpdate object with all of the relevant
 *	information to make it a valid update.  This method
 *	extracts all of the assignments from upd.
 */
void CFinalUpdate::Set(double probability, int distribution_index,
							int action_index, int module_index,
							CUpdate * upd)
{
	this->probability = probability;
	this->probability_distribution = distribution_index;
	this->action_index = action_index;
	this->module_index = module_index;


	int curr_assign_index = 0;
	
	if(upd != NULL)
	{
		assignments.clear();
		this->no_assignments = upd->no_assignments;
		for(int j = 0; j < upd->no_assignments; j++)
			assignments.push_back(upd->assignments[j]);
		all_defined = true;
	}
}

/*
 *	For DTMCs, it is necessary to normalise the probabilities if
 *	there is more than one probability distribution.  This method
 *	is responsible for doing this.
 */
void CFinalUpdate::Normalise(int no_distributions)
{
	probability /= (double)no_distributions;
	probability_distribution = 0;	//when normalised, we only have
}

/*
 *	Prints a representation of this CFinalUpdate object to the command line.
 */
void CFinalUpdate::Print_Update()
{
	cout 
		<< probability_distribution 
		<< "\t[" 
		<< action_index 
		<< "]\t" 
		<< module_index 
		<< "\t" 
		<< probability 
		<< "\t";
	for(int i = 0; i < no_assignments; i++)
	{
		cout << assignments[i]->To_String();
		if(i != no_assignments-1) cout << ",";
	}
}

/*
 *	Executes the assignments pointed to by the assignments vector for this
 *	CFinalUpdate object.  The assignments are performed on the variable 
 *	array parameter.  The temporary results for the current state are
 *	first calculates and then they are applied together.
 */
void CFinalUpdate::Perform_Update(int*variables)
{
	for(int i = 0; i < no_assignments; i++)
		assignments[i]->Do_Assign(variables);
	for(int i = 0; i < no_assignments; i++)
		assignments[i]->Complete_Assign(variables);
}

//=============================================================================
//	Local Data
//=============================================================================

	vector<CFinalUpdate*> the_updates; //The update set
	int no_updates = 0; //The size of the update set
	int max_updates = 0; //The current maximum size of the_updates
	int max_assignments = 0; //The current maximum number of assignments per update
	int* update_variables = NULL; //The variables that this update set corresponds to
	int no_distributions = 0;  //Used for DTMCs to normalise the update set
	
	//this can be indexed [moduleIndex][synchIndex][index] returns a CCommand*
	CCommand**** true_commands = NULL; //Used for Expression evaluation
	//this can be indexed [moduleIndex][synchIndex]
	int** no_true_commands = NULL;

	//TODO make this a vector too
	CCommand** temp_commands = NULL; //pointers to temporary commands used for synchronisation
	int temp_commands_count = 0;

	int* count_synchs = NULL;   //used for fast updates
	int no_used_synchs = 0;

	//Rewards
	double state_reward = 0.0; //The reward of being in the state update_variables


//=============================================================================
//	Functions
//=============================================================================

/*
 *	Delete the data structures required to reason about a state of the model.
 *	Important: deallocate this before the model is deallocated
 */
void Deallocate_Reasoning()
{
	if(&the_updates != NULL)
	{
		CFinalUpdate* upd = NULL;
		for(int i = 0; i < the_updates.size(); i++)
		{
			upd = Get_Update(i);
			if(upd != NULL) delete upd;
		}
		the_updates.clear();
	}
	
	if(true_commands != NULL) 
	{
		for(int i = 0; i < no_modules+1; i++) 
		{
			
			for(int j = 0; j < no_actions+1; j++)
			{
				//cout << "deleting " << i << " " << j << endl;
				delete[] true_commands[i][j];
			}
			delete[] true_commands[i];
			//cout << "deleting " << i << endl;
		}
		
		delete[] true_commands;
	}	
	true_commands = NULL;
	
	if(no_true_commands != NULL)
	{
		for(int i = 0; i < no_modules+1; i++) 
		{
			
			delete[] no_true_commands[i];
		}
		delete[] no_true_commands;
	}

	no_true_commands = NULL;
	
	if(count_synchs != NULL) delete[] count_synchs;

	count_synchs = NULL;
	
	if(temp_commands != NULL)
	{
		//Delete any old synchronisation data
		for(int i = 0; i < temp_commands_count; i++)
		{
			if(temp_commands[i] != NULL)
				delete temp_commands[i];
		}
		temp_commands_count = 0;
		delete[] temp_commands;
		temp_commands = NULL;
	}
}

/*
 *	Allocate the resources for reasoning about the current state of the model.
 *	Some of the data allocated is dependent upon the current loaded model,
 *	and it is a prerequisite that the model has been successfully loaded.
 */
void Allocate_Reasoning()
{
	max_assignments = DEFAULT_MAX_NO_ASSIGNMENTS;
	
	if(&the_updates == NULL)
	{
		Report_Error("Out of memory when allocating reasoning data structures");
		throw string("Out of memory error: simreasoning.cc 001");
	}

	the_updates.reserve(DEFAULT_MAX_NO_UPDATES);

	//Allocate holders for CFinalUpdate objects and their pointers
	for(int i = 0; i < DEFAULT_MAX_NO_UPDATES; i++)
	{
		CFinalUpdate* upd = new CFinalUpdate(max_assignments);
		if(upd == NULL)
		{
			Report_Error("Out of memory when allocating reasoning data structures");
			throw string("Out of memory error: simreasoning.cc 002");
		}
		the_updates.push_back(upd);
	}
	
	no_updates = 0;

	//setup storage for true command pointers
	true_commands = new CCommand***[no_modules+1];		//access to true_commands[no_modules] gives
	if(true_commands == NULL)
	{
		Report_Error("Out of memory when allocating reasoning data structures");
		throw string("Out of memory error: simreasoning.cc 003");
	}
	no_true_commands = new int*[no_modules+1];			//sorted out synchronised commands
	if(no_true_commands == NULL)
	{
		Report_Error("Out of memory when allocating reasoning data structures");
		throw string("Out of memory error: simreasoning.cc 004");
	}
	for(int i = 0; i < no_modules+1; i++) 
	{
		true_commands[i] = new CCommand**[no_actions+1];	//access to true_commands[][nosynchs] gives
		if(true_commands[i] == NULL)
		{
			Report_Error("Out of memory when allocating reasoning data structures");
		throw string("Out of memory error: simreasoning.cc 005");
		}
		no_true_commands[i] = new int[no_actions+1];	//module commands
		if(no_true_commands[i] == NULL)
		{
			Report_Error("Out of memory when allocating reasoning data structures");
			throw string("Out of memory error: simreasoning.cc 006");
		}
		for(int j = 0; j < no_actions+1; j++)
		{
			true_commands[i][j] = new CCommand*[no_commands]; //no_transitions to be on the safe side
			if(true_commands[i][j] == NULL)
			{
				Report_Error("Out of memory when allocating reasoning data structures");
				throw string("Out of memory error: simreasoning.cc 007");
			}
			no_true_commands[i][j] = 0;
			for(int k = 0; k < no_commands; k++)
				true_commands[i][j][k] = NULL;
		}
	}
	no_distributions = 1;


	//setup storage for fast updating

	count_synchs = new int[no_actions];
	for(int i = 0; i < no_actions; i++)
		count_synchs[i] = -1;
	no_used_synchs = 0;

	temp_commands_count = 0;

	//allocate space for array of pointers to temporary
	//commands used for synchronisation
	//TODO Work out the actual maximum possible number of synchronised transitions
	temp_commands = new CCommand*[no_commands*1000];
}


//==============================================
//Functions to reason about a particular state
//==============================================

/*
 *	Populates the_updates with the_updates appropriate for the given state
 */
void Calculate_Updates(int* variables)
{
	Find_True_Commands(variables);

	//Print_True_Commands();
	
	//Delete any old synchronisation data
	for(int i = 0; i < temp_commands_count; i++)
	{
		if(temp_commands[i] != NULL)
			delete temp_commands[i];
	}
	temp_commands_count = 0;

	//Form products of synchronous updates
	for(int i = 0; i < no_actions; i++)
	{
		for(int j = 0; j < no_modules; j++)
		{
			Product_Commands(i,j);
		}
	}
	
	//Populate the_updates
	int count_distributions = 0; //count probability distributions
	no_updates = 0;
	
	CCommand* comm;
	CUpdate* upd;
	//do asynchronous first
	for(int i = 0; i < no_modules; i++)
	{
		//cout << "looking at the " << i << "th module" << endl;
		for(int j = 0; j < no_true_commands[i][no_actions]; j++)
		{
			//cout << "in here for the " << j <<"th command" << endl;
			count_distributions++;
			comm = true_commands[i][no_actions][j];
			for(int k = 0; k < comm->no_updates; k++)
			{
				//cout << "should be actually adding it" << endl;
				upd = comm->updates[k];
				
				Get_Update(no_updates++)->Set
					(upd->probability->EvaluateDouble(variables),(count_distributions-1),-1,i,upd);
			}
		}
	}
	
	//now do synchronous
	for(int i = 0; i < no_actions; i++)
	{
		for(int j = 0; j < no_true_commands[no_modules][i]; j++)
		{
			count_distributions++;
			comm = true_commands[no_modules][i][j];
			for(int k = 0; k < comm->no_updates; k++)
			{
				//cout << "should be adding a synchronous update" << endl;
				upd = comm->updates[k];
				Get_Update(no_updates++)->Set
					(upd->probability->EvaluateDouble(variables),(count_distributions-1),i,-1,upd);
			}
		}
	}
	
	no_distributions = count_distributions;

	//For DTMCs, we need to normalise the updates
	if(model_type == PROBABILISTIC)
	{
		if(no_distributions > 1) //no need to divide by 1
		{
			for(int i = 0; i < no_updates; i++)
			{
				Get_Update(i)->Normalise(no_distributions);
			}
			no_distributions = 1;

		}
	}

	
	update_variables = variables;
}

/*
 *	Calculates the reward of being in the state describes by the variables parameter.
 */
void Calculate_State_Reward(int* variables)
{
	state_reward = 0.0;
	for(int i = 0; i < no_state_rewards; i++)
	{
		CStateReward* rew = state_rewards_table[i];
		state_reward += rew->Get_Reward_For_State(variables);
	}
}

/*
 *	This function makes a random choice from the update set
 *	so long as the model is continuous time and sum_rates
 *	is indeed the sum of the rates in the update set.
 *	Returns -1 if there were no updates to choose from (i.e. deadlock).
 */
int Choose_Continuous_Update(double sum_rates)
{
	//because the probability of an update being selected
	//is rate / all rates
	//we can just sample from the sum of all rates
	//and work out which was selected
	double sample = sum_rates * Random_Uniform();

	double counter = 0.0;
	int selected_index = -1;

	for(int i = 0; i < Get_No_Updates(); i++)
	{
		CFinalUpdate* upd = Get_Update(i);
		counter += upd->probability;
		if(sample <= counter)
		{
			selected_index = i;
			break;
		}
	}

	return selected_index;
}

/*
 *	This function makes a random selection according
 *	to the current state (state_variables) for discrete
 *	time markov chains and markov decision processes.
 *	It does not need to complete the entire update set.
 *	Returns NULL if there were no updates to choose from (i.e. deadlock).
 */
CUpdate* Choose_Discrete_Update_On_The_Fly(bool& up_determ, double& selected_prob)
{
	Find_True_Commands(state_variables);
	//Print_True_Commands();
	
	//Find number of asynchronous commands
	int count_asynch = Count_Asynchronous_Commands();

	//Find number of synchronous commands
	no_used_synchs = 0;
	for(int i = 0; i < no_actions; i++)
	{
		count_synchs[i] = Count_Synchronous_Commands(i);
		no_used_synchs += count_synchs[i];
	}
	
	if(count_asynch+no_used_synchs == 0) return NULL; //if there is a deadlock

	up_determ = Are_Updates_Deterministic_On_The_Fly(state_variables, count_asynch, no_used_synchs);

	//remove any products that were created
	for(int i = 0; i < no_actions; i++)
		no_true_commands[no_modules][i] = 0;

	//randomly select the probability distribution
	//this is modelling the non-deterministic choice
	//by simply uniformly choosing between probability distributions
	int selection = Random_Uniform_From_Range(0, count_asynch+no_used_synchs);
	
	CCommand* selected_command = NULL;
	if(selection < count_asynch) //synchronous update selected
	{
		//search for the selected command
		int counter = 0;
		for(int i = 0; i < no_modules; i++)
			for(int j = 0; j < no_true_commands[i][no_actions]; j++)
				if(counter++ == selection)
					selected_command = true_commands[i][no_actions][j];
	}
	else //must be synchronous
	{
		selection -= count_asynch; //ignore asynchronous commands
		int synch_index = -1;
		int synch_sub_index = -1;
		int so_far = 0;
		
		//find the synch and sub_synch index of the command
		for(int i = 0; i < no_actions; i++)
		{
			if(selection < so_far+count_synchs[i])
			{
				synch_index = i;
				synch_sub_index = selection-so_far;
				break;
			}
			so_far+=count_synchs[i];
		}
		
		//Do the necessary product to get this command
		//(this could be optimised further)
		for(int i = 0; i < no_modules; i++)
			Product_Commands(synch_index, i);
		
		selected_command = 
			true_commands[no_modules][synch_index][synch_sub_index];
	}

	//Declare storage for the probability distribution
	double*distribution;
	int size_distribution=0;

	if(selected_command == NULL) 
	{
		return NULL;;
	}

	//create probability distribution
	size_distribution = selected_command->no_updates;
	
	distribution = new double[size_distribution];
	
	for(int i = 0; i < size_distribution; i++)
		distribution[i] = 
		selected_command->updates[i]->probability->EvaluateDouble();

	int answer = Random_From_Prob_Distribution(distribution, size_distribution);

	selected_prob = distribution[answer];
	delete[] distribution;

	return selected_command->updates[answer];
}

/*
 *	Counts the number of synchronous commands in the true_commands data structure
 *	for a particular action parameterised by synch_index
 */
int Count_Synchronous_Commands(int synch_index)
{
	if(synch_index == no_actions) return Count_Asynchronous_Commands(); 
	int number_sorted = 0;
	for(int module_index = 0; module_index < no_modules; module_index++)
	{
		if(no_true_commands[module_index][synch_index] == 0) 
			continue; // nothing to do
		else if(number_sorted == 0) //if nothing in sorted yet
			number_sorted = no_true_commands[module_index][synch_index];
		else
			number_sorted = number_sorted * no_true_commands[module_index][synch_index];
	}
	return number_sorted;
}

/*
 *	Counts the number of asynchronous command in the true_commands data structure.
 */
int Count_Asynchronous_Commands()
{
	int counter = 0;
	for(int module_index = 0; module_index < no_modules; module_index++)
		counter += no_true_commands[module_index][no_actions];
	return counter;

}

//=====================================
//Actually carry out one of the updates
//=====================================

/*
 *	Actually perform the calculated update at updates[index] on
 *	the given variable set.
 */
void Execute_Update(int index, int* variables)
{
	//cout << "executing update" << endl;
	Get_Update(index)->Perform_Update(variables);
}

//=====================================
//Functions to query calculated results
//=====================================

/*
 *	Reasons about the calculated update set to say whether it 
 *	is deterministic.
 */
bool Are_Updates_Deterministic()
{
	if(no_updates <= 1) return true;
	else
	{
		//cout << "Are_Updates_Deterministic 1" << endl;
		int* compare_state = new int[no_state_variables];
		int* new_state = new int[no_state_variables];
		//create a new place to put the state
		Copy_Int_Array(state_variables, new_state, no_state_variables);

		//cout << "Are_Updates_Deterministic 2" << endl;
		CFinalUpdate* first = Get_Update(0);
		//look at what the first update would do (compare_state)
		for(int i = 0; i < first->no_assignments; i++)
		{
			first->assignments[i]->Do_Assign(new_state);
		}
		for(int i = 0; i < first->no_assignments; i++)
		{
			first->assignments[i]->Complete_Assign(new_state);
		}
		//cout << "Are_Updates_Deterministic 3" << endl;
		//do each of the other updates match
		for(int i = 1; i < no_updates; i++)
		{
			//cout << "Are_Updates_Deterministic 4" << endl;
			Copy_Int_Array(state_variables, compare_state, no_state_variables);
			for(int j = 0; j < Get_Update(i)->no_assignments; j++)
			{
				Get_Update(i)->assignments[j]->Do_Assign(compare_state);
			}
			for(int j = 0; j < Get_Update(i)->no_assignments; j++)
			{
				Get_Update(i)->assignments[j]->Complete_Assign(compare_state);
			}
			//if any are not the same we are not moving deterministically, return false
			if(!Int_Arrays_Equals(compare_state, new_state, no_state_variables))
			{
				delete[]compare_state;
				delete[]new_state;
				return false;
			}
		}
		//cout << "Are_Updates_Deterministic 4" << endl;
		delete[] compare_state;
		delete[] new_state;
		return true;

	}
}

/*
 *	Determines whether the updates to the given variable set will be determninistic without
 *	having to first calculate the whole update set.
 */
bool Are_Updates_Deterministic_On_The_Fly(int* variables, int num_asynch, int num_synch)
{
	int* state1 = new int[no_state_variables];
	int* state2 = new int[no_state_variables];
	bool assigned_one = false;
	int curr_state = 1; //false = state1 true = state2
	CCommand* comm;
	CUpdate* upd;
	
	Copy_Int_Array(variables, state2, no_state_variables);
	
	// check asynchronous first (if necessary)
	if(num_asynch != 0)
	{
		for(int i = 0; i < no_modules; i++)
		{
			for(int j = 0; j < no_true_commands[i][no_actions]; j++)
			{
				comm = true_commands[i][no_actions][j];
				for(int k = 0; k < comm->no_updates; k++)
				{
					upd = comm->updates[k];
					if(!assigned_one)//curr_state must be 1
					{
						Copy_Int_Array(state_variables, state1, no_state_variables);
						upd->Do_Update(state1);
						curr_state = 2;
						assigned_one = true;
					}
					else
					{
						if(curr_state == 1)//if in the state1
						{
							Copy_Int_Array(state_variables, state1, no_state_variables);
							upd->Do_Update(state1);
							curr_state = 2;
						}
						else //if in state1 
						{
							Copy_Int_Array(state_variables, state2, no_state_variables);
							upd->Do_Update(state2);
							curr_state = 1;
						}
						//if the last two things are not the same return false
						if(!Int_Arrays_Equals(state1, state2, no_state_variables))
						{
							delete[] state1;
							delete[] state2;
							return false;
						}
					}
				}
			}
		}
	}
	
	//if we are here all of the asynchronous ones are deterministic, now
	//go through the (slow) process of finding out about the synchronous
	//ones.
	if(num_synch != 0)
	{
		//algorithm: for each synch, work out whether the product command
		//produces the same result as all of the rest
		for(int i = 0; i < no_actions; i++)
		{
			//tidy up any old temporary commands
			for(int j = 0; j < temp_commands_count; j++)
			{
				if(temp_commands[j] != NULL)
					delete temp_commands[j];
			}
			temp_commands_count = 0;
			//form the product for this synch
			for(int j = 0; j < no_modules; j++)
				Product_Commands(i,j);
			
			//now check this product for equality with other previous updates
			for(int j = 0; j < no_true_commands[no_modules][i]; j++)
			{
				comm = true_commands[no_modules][i][j];
				for(int k = 0; k < comm->no_updates; k++)
				{
					upd = comm->updates[k];
					if(!assigned_one)//curr_state must be 1
					{
						Copy_Int_Array(state_variables, state1, no_state_variables);
						upd->Do_Update(state1);
						curr_state = 2;
						assigned_one = true;
					}
					else
					{
						if(curr_state == 1)//if in the state1
						{
							Copy_Int_Array(state_variables, state1, no_state_variables);
							upd->Do_Update(state1);
							curr_state = 2;
						}
						else //if in state1 
						{
							Copy_Int_Array(state_variables, state2, no_state_variables);
							upd->Do_Update(state2);
							curr_state = 1;
						}
						//if the last two things are not the same return false
						if(!Int_Arrays_Equals(state1, state2, no_state_variables))
						{
							delete[] state1;
							delete[] state2;
							return false;
						}
					}
				}
			}
		}
	}
	delete[] state1;
	delete[] state2;
	
	return true;
}

double Calculate_Sum_Rates()
{
	//find the sum of all the rates
	double sum_rates = 0;

	for(int i = 0; i < Get_No_Updates(); i++)
	{
		CFinalUpdate* upd = Get_Update(i);
		sum_rates += upd->probability;
	}

	return sum_rates;
}


//Methods for accessing the CUpdates objects theUpdates

/*
 *	Returns the reward calcualated by calling Calculate_State_Reward()
 */
double Get_State_Reward()
{
	return state_reward;
}

//=======================================================
//	Methods for accessing the CUpdates objects theUpdates 
//=======================================================

/*
 *	Returns the current size of the update set.
 */
int Get_No_Updates()
{
	return no_updates;
}

/*
 *	Returns the probability of the update at the given index
 */
double Get_Probability_Of_Update(int update_index)
{
	return Get_Update(update_index)->probability;
}

/*
 *	Returns the probability distribution index of the update at the given index
 */
int Get_Distribution_Index_Of_Update(int update_index)
{
	return Get_Update(update_index)->probability_distribution;
}

/*
 *	Returns the action index of the update at the given index
 */
int Get_Action_Index_Of_Update(int update_index)
{
	return Get_Update(update_index)->action_index;
}

/*
 *	Returns the module index of the update at the given index
 */
int Get_Module_Of_Update(int update_index)
{
	return Get_Update(update_index)->module_index;
}

//maybe move this
int Get_Result_Of_Update(int update_index, int var_index)
{
	int* copy = new int[no_state_variables];
	Copy_Int_Array(update_variables, copy, no_state_variables);
	Get_Update(update_index)->Perform_Update(copy);
	int result = copy[var_index];
	delete[]copy;
	return result;
}

/*
 *	Returns the number of assignments of the updates at the given index.
 */
int Get_Num_Assignments_Of_Update(int update_index)
{
	return Get_Update(update_index)->no_assignments;
}

/*
 *	Returns the state index of the update_indexth updates assignment_indexth assignment.
 */
int Get_Assignment_Variable_Index_Of_Update(int update_index, int assignment_index)
{
	return Get_Update(update_index)->assignments[assignment_index]->Get_Variable_Index();
}

//maybe move this
int Get_Assignment_Value_Of_Update(int update_index, int assignment_index)
{
	return Get_Result_Of_Update
		(update_index, 
		Get_Assignment_Variable_Index_Of_Update(update_index, assignment_index));
}

/*
 *	Returns the calculated update at the given index.  Because these updates
 *	are preallocated, if it is required that more are needed than have been
 *	allocated, then this method doubles the allocation of potential update
 *	storage.
 *
 *	throws an exception if out of memory
 */
CFinalUpdate* Get_Update(int index)
{
	int upd_size = the_updates.size();
	if(index >= upd_size)
	{
		for(int i = 0; i < upd_size; i++)
		{
			CFinalUpdate* fupd = new CFinalUpdate(max_assignments);
			if(fupd == NULL)
			{
				Report_Error("Out of memory when allocating reasoning data structures");
				throw string("Out of memory error: simreasoning.cc 010");
			}
			the_updates.push_back(fupd);
		}
	}
	return the_updates[index];
}


//=============================================================================
//	Local Functions
//=============================================================================





/*
 *	Finds all of the commands that are true in the command table for the given
 *	variables and populates the true_commands array with pointers to them
 *	also sets num_distinct_actions
 */
inline void Find_True_Commands(int* variables)
{
	//cout << "Find_True_Commands 1" << endl;
	//Print_Array(variables, no_state_variables);
	for(int i = 0; i < no_modules+1; i++)
	{
		for(int j = 0; j < no_actions+1; j++)
		{
			no_true_commands[i][j] = 0;
		}
	}
	//cout << "Find_True_Commands 2" << endl;
	int mod_index;
	int synch_index;

	//check each command in the command_table and see whether it evaluates to true

	for(int i = 0; i < no_commands; i++)
	{

		CCommand* comm = command_table[i];

		if(comm->guard->Evaluate(variables))
		{

			mod_index = comm->module_index;

			if(comm->action_index == -1)	//asynchronous
				synch_index = no_actions;
			else							//synchronous
				synch_index = comm->action_index;

			true_commands[mod_index][synch_index][no_true_commands[mod_index][synch_index]++] = comm;

		}
	}
	//cout << "Find_True_Commands 3" << endl;
	//sort out blocking
	//a command can block the whole synch if no other commands
	//are true in the same module as the blocker

	//for each synch - note that synchs are i and modules are j (usually the other way round)
	for(int i = 0; i < no_actions; i++)
	{
		//check that there is at least one in each module (if the module has that synch in its alphabet)
		for(int j = 0; j < no_modules; j++)
			if(alphabet[j][i]) //it is possible that this module could block the action
				if(no_true_commands[j][i] == 0) //if this module has no synchs but should
				{
					for(int k = 0; k < no_modules; k++)//block it
						no_true_commands[k][i] =0; //by setting num_true_commands for all modules to 0 for this synch
					break; //next synch as this has been blocked
				}
	}

	//cout << "Find_True_Commands 4" << endl;
}

/*
 *	Forms the product of what is in the sorted_slot ([no_modules][synch_index]
 *	and what is in the given slot [synch_index][module_index]
 */
inline void Product_Commands(int synch_index, int module_index)
{
	if(no_true_commands[module_index][synch_index] == 0) return; // nothing to do
	else if(no_true_commands[no_modules][synch_index] == 0) //if nothing in sorted_slot
		for(int i = 0; i < no_true_commands[module_index][synch_index]; i++)//just copy (create a pointer) the contents to the sorted_slot
			true_commands[no_modules][synch_index][no_true_commands[no_modules][synch_index]++] =
			true_commands[module_index][synch_index][i]; 
	else //we have to form a product
	{
		//storage for product
		CCommand** products = 
			new CCommand*[no_true_commands[no_modules][synch_index] * no_true_commands[module_index][synch_index]];
		if(products == NULL)
		{
			Report_Error("Out of memory when allocating reasoning data structures");
			throw string("Out of memory error: simreasoning.cc 011");
		}
		int products_done = 0;

		for(int i = 0; i < no_true_commands[no_modules][synch_index]; i++) //for each command in sorted_slot
			for(int j = 0; j < no_true_commands[module_index][synch_index]; j++) //for each command in given slot
			{
				products[products_done++] = 
					Product_Two_Commands(
					true_commands[no_modules][synch_index][i],
					true_commands[module_index][synch_index][j]); //do the product
			}
			//remove old pointers
			delete [] true_commands[no_modules][synch_index];

			//set the slot to the new pointers
			true_commands[no_modules][synch_index] = products;
			no_true_commands[no_modules][synch_index] = products_done;
	}

}

/*
 *	Forms the product of the two CCommand parameters. i.e. the guards
 *	are ignored, but the product of the updates is created.
 */
inline CCommand* Product_Two_Commands(CCommand* comm1, CCommand* comm2)
{
	CCommand* new_command = new CCommand(comm1->guard, comm1->action_index, no_modules, comm1->no_updates*comm2->no_updates);
	if(new_command == NULL)
	{
		Report_Error("Out of memory when allocating reasoning data structures");
		throw string("Out of memory error: simreasoning.cc 012");
	}
	temp_commands[temp_commands_count++] = new_command;
	
	for(int i = 0; i < comm1->no_updates; i++)
	{
		for(int j = 0; j < comm2->no_updates; j++)
		{
			new_command->Add_Update(Merge_Two_Updates(comm1->updates[i], comm2->updates[j]));
		}
	}
	new_command->Set_Owns_Guard(false); //so it doesn't delete its children
	return new_command;
}

/*
 *	Forms the product of two updates.  The probability or rate, is
 *	simply the product of the two and the assignment set is the
 *	union of the two product sets.
 */
inline CUpdate* Merge_Two_Updates(CUpdate* upd1, CUpdate* upd2)
{
	CRealTimes* new_prob = new CRealTimes(upd1->probability, upd2->probability);
	if(new_prob == NULL)
	{
		Report_Error("Out of memory when allocating reasoning data structures");
		throw string("Out of memory error: simreasoning.cc 013");
	}
	new_prob->Set_Owns_Children(false);

	CUpdate* new_update = new CUpdate(new_prob,
		(upd1->no_assignments + upd2->no_assignments));
	for(int i = 0; i < upd1->no_assignments; i++)
		new_update->Add_Assignment(upd1->assignments[i]);
	for(int i = 0; i < upd2->no_assignments; i++)
		new_update->Add_Assignment(upd2->assignments[i]);
	new_update->Set_Owns_Assignments(false);
	return new_update;

}


//=============================================================================
//	Helper Functions
//=============================================================================
/*
 *	Outputs the contents of updates to the command line.
 */
void Print_Updates()
{
	cout << "Current update set:\n\n" ;

	cout 
		<< "no."
		<< "\t"
		<< "dist" 
		<< "\t" 
		<< "synch" 
		<< "\t" 
		<< "module" 
		<< "\t" 
		<< "prob" 
		<< "\t"
		<< "assigns\t\n";
	int curr_dist = -1;
	for(int i = 0; i < no_updates; i++)
	{
		if(Get_Update(i)->probability_distribution != curr_dist)
		{
			curr_dist = Get_Update(i)->probability_distribution;
			cout << "------------------------------------------------------------" << endl;
		}
		cout << i << ":\t";
		Get_Update(i)->Print_Update();
		cout << endl;
	}
	cout << "------------------------------------------------------------" << endl;
	cout << "Number of probability distributions: " << no_distributions << endl;
	cout << "------------------------------------------------------------" << endl;	
}


/*
 *	Outputs the contents of true_commands to the command line  .
 */	
inline void Print_True_Commands()
{

	cout << "True commands for current state: "<< endl << endl;
	for(int i = 0; i < no_modules+1; i++)
		for(int j = 0; j < no_actions+1; j++)
			for(int k = 0; k < no_true_commands[i][j]; k++)
			{
				cout << (true_commands[i][j][k]->To_String()) << endl;
			}
}




