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

#include "simsampling.h"
#include "simpctl.h"
#include "simupdater.h"
#include "simreasoning.h"
#include "simstate.h"
#include "simutil.h"
#include "simlog.h"
#include <stdio.h>
#include <iostream>
#include <vector>

using std::cout;
using std::endl;
using std::vector;

//=============================================================================
//	Description
//=============================================================================

/*
 *	This interface is used to perform approximate verification of properties
 *	via a Monte-Carlo based sampling technique.
 *
 *	The vector registered_sample_holders can be populated to contain objects
 *	called CSampleHolders that simply make a number of samples to obtain an
 *	average (be it probability or reward) for the result of a CPathFormula for
 *	a given path.
 *
 *	Once this vector has been populated, the Do_Sampling method can be run, which
 *	uses a cut-down loop detection handler (no path required) to perform the sampling
 *	algorithm.
 */


//=============================================================================
//	Local Data
//=============================================================================

vector<CSampleHolder*> registered_sample_holders;  //P=? or R=?
int no_sample_holders = 0; //size of registered_sample_holders
int no_iterations = 412000;


//=============================================================================
//	Globals
//=============================================================================

bool should_stop_sampling = false; //used to stop the algorithm in its tracks

//=============================================================================
//	Class Definitions
//=============================================================================

/*
 *	CSampleHolder
 *	=============
 *	
 *	The CSampleHolder class is responsible for storing a cumulative value of
 *	the property so far, together with the number of samples so that the mean
 *	value can be calculated.
 *
 */

/*
 *	Constructor for a CSampleHolder object.
 */
CSampleHolder::CSampleHolder()
{
	cumulative_value = 0.0;
	no_samples = 0;
	no_reach_max = 0; 
}

/*
 *	Adds the value of sample to the cumulative_value and increments the no_samples.
 */
void CSampleHolder::Sample(double sample)
{
	if(sample == UNDEFINED_DOUBLE) //infinite detected
	{
		cumulative_value = UNDEFINED_DOUBLE;
	}
	else
	{
		cumulative_value += sample;
		no_samples++;
	}
}

/*
 *	A sample where the maximum path length has been reached.
 */
void CSampleHolder::Sample_Maximum_Path_Reached()
{
	no_reach_max++;
}

/*
 *	Reset for another sampling algorithm.
 */
void CSampleHolder::Reset()
{
	cumulative_value = 0.0;
	no_samples = 0;
	no_reach_max = 0; 
}

/*
 *	Give the number of times that the maximum path length was reached for
 *	the current sampling run.
 */
int CSampleHolder::Get_Number_Reached_Maximum_Path()
{
	return no_reach_max;
}


/*
 *	CProbEqualsQuestion
 *	===================
 *	
 *	The CProbEqualsQuestion class overrides CSampleHolder to provide functionality
 *	for properties of the form P=? [...].  These have a link to a registered
 *	CPathFormula, which is the path formula to be sampled.
 *
 */

/*
 *	Constructor for a CProbEqualsQuestion object.
 */
CProbEqualsQuestion::CProbEqualsQuestion(CPathFormula* formula) : CSampleHolder()
{
	this->formula = formula;
	required_runs = no_iterations;

}

/*
 *	Destructor for a CProbEqualsQuestion object.
 */
CProbEqualsQuestion::~CProbEqualsQuestion()
{
}

/*
 *	Returns true if the number of samples taken is >= the amount required.
 */
bool CProbEqualsQuestion::Done()
{
	return (no_samples >= required_runs);
}

/*
 *	Returns the mean value result(cumulative/no_samples)
 */
double CProbEqualsQuestion::Get_Result()
{
	return cumulative_value / (double)no_samples;
}

/*
 *	Returns a pointer to the path formula
 */
CPathFormula* CProbEqualsQuestion::Get_Path_Formula()
{
	return formula;
}

/*
 *	Method to set the number of iterations required to approximately verify this property.
 */
void CProbEqualsQuestion::Set_No_Iterations(int no_iterations_param)
{
	required_runs = no_iterations_param;
}

void CProbEqualsQuestion::Write_Holder(int fd)
{
    //write type
    int holder_type = HOLDER_PROB;
    write(fd, &holder_type, sizeof(int));

    //Write the index of the path formula
    int index = Index_Of_Path_Formula(formula);
    write(fd, &index, sizeof(int));

    //Write null byte
    write(fd, "\0", 1);
}


/*
 *	CRewardEqualsQuestion
 *	=====================
 *	
 *	The CRewardEqualsQuestion class overrides CSampleHolder to provide functionality
 *	for properties of the form R=? [...].  These have a link to a registered
 *	CPathFormula, which is the reward formula to be sampled.
 *
 */

/*
 *	Constructor for a CRewardEqualsQuestion object.
 */
CRewardEqualsQuestion::CRewardEqualsQuestion(CPathFormula* formula) : CSampleHolder()
{
	this->formula = formula;
	required_runs = no_iterations;
}

/*
 *	Destructor for a CRewardEqualsQuestion object.
 */
CRewardEqualsQuestion::~CRewardEqualsQuestion()
{			   
}

/*
 *	Returns true if the number of samples taken >= the number of samples required. Or
 *	if the cumulative_value is determined to be infinite... if which case the answer
 *	will always be infinite.
 */
bool CRewardEqualsQuestion::Done()
{
	return (cumulative_value == UNDEFINED_DOUBLE || no_samples >= required_runs);
}

/*
 *	Returns the mean value result(cumulative/no_samples)
 */
double CRewardEqualsQuestion::Get_Result()
{
	if(cumulative_value == UNDEFINED_DOUBLE)
		return cumulative_value;
	else
		return cumulative_value / (double)no_samples;
}

/*
 *	Returns a pointer to the reward formula 
 */	
CPathFormula* CRewardEqualsQuestion::Get_Path_Formula()
{
	return formula;
}

/*
 *	Method to set the number of iterations required to approximately verify this property.
 */
void CRewardEqualsQuestion::Set_No_Iterations(int no_iterations_param)
{
	required_runs = no_iterations_param;
}

void CRewardEqualsQuestion::Write_Holder(int fd)
{
    //write type
    int holder_type = HOLDER_REWARD;
    write(fd, &holder_type, sizeof(int));

    //Write the index of the path formula
    int index = Index_Of_Path_Formula(formula);
    write(fd, &index, sizeof(int));

    //Write null byte
    write(fd, "\0", 1);
}

//Class CSamplingLoopDetectionHandler

CSamplingLoopDetectionHandler::CSamplingLoopDetectionHandler()
{
	//start with a loop path of 10 as default
	for(int i = 0; i < 10; i++)
	{
		CPathState* new_ps = new CPathState();
		loop_path.push_back(new_ps);
	}
	exploring_deterministically = false;
	proven_looping = false;
	next_loop_index = 0;
	deadlock = false;
}

CSamplingLoopDetectionHandler::~CSamplingLoopDetectionHandler()
{
	for(int i = 0 ; i< loop_path.size(); i++)
	{
		delete loop_path[i];
	}

	loop_path.clear();
}

void CSamplingLoopDetectionHandler::Start_New_Deterministic_Path()
{
	exploring_deterministically = true;
	next_loop_index = 0;
	proven_looping = false;
}

void CSamplingLoopDetectionHandler::Notify_State(int* state_variables)
{
	if(next_loop_index >= loop_path.size())	 //need to allocate some more path
	{
		debug(<<"Assigning more memory"<<endl);
		//Add a path of 10 to loop_path
		for(int i = 0; i < 10; i++)
		{
			CPathState* new_ps = new CPathState();
			loop_path.push_back(new_ps);
		}
	}
	loop_path[next_loop_index++]->Make_This_Current_State(); 
}

int* CSamplingLoopDetectionHandler::Path_At(int index)
{
	return loop_path[index]->variables;
}


void  CSamplingLoopDetectionHandler::Notify_Deterministic_Path_End()
{
	exploring_deterministically = false;
	next_loop_index = 0;
	proven_looping = false;
}

void CSamplingLoopDetectionHandler::Reset()
{
	exploring_deterministically = false;
	next_loop_index = 0;
	proven_looping = false;
	deadlock = false;
}

int CSamplingLoopDetectionHandler::Path_Size()
{
	return next_loop_index;
}


//=============================================================================
// Functions
//=============================================================================

/*
 *	Deallocates memory for the sampling interface.  All registered_sample_holders
 *	are removed from memory.
 */
void Deallocate_Sampling()
{
	if(&registered_sample_holders != NULL)
	{
		for(int i = 0; i < registered_sample_holders.size(); i++)
		{
			delete registered_sample_holders[i];
		}
		registered_sample_holders.clear();
	}
	
	no_sample_holders = 0;
	
}

/*
 *	Assigns memory to store sample_holders
 */
void Allocate_Sampling()
{
	registered_sample_holders.reserve(10);
	no_sample_holders = 0;
}

/*
 *	Adds a CSampleHolder to the registered_sample_holders vector.
 */
int Register_Sample_Holder(CSampleHolder* sample_holder)
{
	registered_sample_holders.push_back(sample_holder);
	no_sample_holders++;

	return no_sample_holders-1;
}

/*
 *	Returns true if each member of registered_sample_holders has done all
 *	of the necessary computation to get a result.
 */
bool All_Done_Sampling()
{
	for(int i = 0; i < no_sample_holders; i++)
	{
		if(!registered_sample_holders[i]->Done()) return false;
	}
	return true;
}

/*
 *	Returns true if for each path formula of each registered_sample_holder,
 *	the answer is known for the current execution path.
 */
bool All_PCTL_Answers_Known(CLoopDetectionHandler* loop_detection)
{
	bool finished = true;
	for(int j = 0; j < no_sample_holders && finished; j++)
	{
		CPathFormula * pf = registered_sample_holders[j]->Get_Path_Formula();
		finished = finished && pf->Is_Answer_Known(loop_detection);
	}
	return finished;
}

/*
 *	Performs a sample according to the calculation on each CPathFormula of the
 *	CSampleHolders in registered_sample_holders.  Each CPathFormula is reset
 *	for the next path.
 */
void Do_A_Sample(CLoopDetectionHandler* loop_detection)
{
	for(int i = 0; i < no_sample_holders; i++)
	{
		CPathFormula * pf = registered_sample_holders[i]->Get_Path_Formula();
		
		// store the current answer as a a sample
		registered_sample_holders[i]->Sample(pf->Get_Answer_Double());
		
		// if don't know the final answer (perhaps path length too short), register this fact...
		if(!pf->Is_Answer_Known(loop_detection)) registered_sample_holders[i]->Sample_Maximum_Path_Reached();
		
		pf->Reset();
	}
}

/*
 *	Force the sampling algorithm to stop.
 */
void Stop_Sampling()
{
	should_stop_sampling = true;
}

/*
 *	Print out the sampling results to the command line.
 */
void Print_Sampling_Results()
{

	cout << "Sampling Results: " <<endl << endl;
	for(int i = 0; i < no_sample_holders; i++)
	{
		cout << registered_sample_holders[i]->Get_Result() << "\t" <<
			registered_sample_holders[i]->no_samples << "\t" <<
				registered_sample_holders[i]->cumulative_value << "\t"
			"\t" << registered_sample_holders[i]->Get_Path_Formula()->To_String() <<endl;
	}

	cout << endl << endl;
}

/*
 *	Get the calculated result of the CSampleHolder at the given index.
 */
double Get_Sampling_Result(int index)
{
	return registered_sample_holders[index]->Get_Result();
}

/*
 *	Get the number of times the CSampleHolder at the given index had to be evaluated
 *	to the maximum path length in order to make a sample.
 */
int Get_Num_Reached_Max_Path(int index)
{
	return registered_sample_holders[index]->Get_Number_Reached_Maximum_Path();
}

/*
 *	Get the total number of times any CSampleHolder at the given index had to be evaluated
 *	to the maximum path length in order to make a sample.
 */
int Get_Total_Num_Reached_Max_Path()
{
	int count = 0;
	for(int j = 0; j < no_sample_holders; j++)
	{
		count += Get_Num_Reached_Max_Path(j);
	}
	return count;
}

/*
 *	For each CSampleHolder in registered_sample_holders, this function sets the
 *	number of iterations for the sampling algorithm.
 */
void Set_No_Iterations(int no_iterations_param)
{
	no_iterations = no_iterations_param;
	for(int i = 0; i < no_sample_holders; i++)
		registered_sample_holders[i]->Set_No_Iterations(no_iterations);
}

/*
 *	The sampling algorithm.
 */
void Do_Sampling(int path_length)
{
	//#############################################
	//#INITIALISE VARIABLES FOR SAMPLING ALGORITHM#
	//#############################################
	
	//Last state is required for the pctl/csl methods
	CPathState* last_state = new CPathState();
	
	//Loop detection requires that deterministic paths are
	//stored, until that determinism is broken.  This path
	//is allocated dynamically, 10 steps at a time.
	CSamplingLoopDetectionHandler* loop_detection = new CSamplingLoopDetectionHandler();
	
	//Clone the starting state from state_variables
	int* starting_variables = new int[no_state_variables];
	for(int i = 0; i < no_state_variables; i++)
		starting_variables[i] = state_variables[i];
	
	//External flag set to false
	should_stop_sampling = false;
	
	//####################
	//#MAIN SAMPLING LOOP#
	//####################
	
	int iteration_counter = 0;
	int last_percentage_done = -1;
	int percentage_done = -1;
	double average_path_length = 0;
	int min_path_length = 0, max_path_length = 0;
	bool stopped_early = false;
	bool deadlocks_found = false;
	
	//Variables used within the sampling loop
	//=======================================
	
	//The current path
	int current_index;
	
	double* path_cost = new double[no_reward_structs];
	double* total_state_cost = new double[no_reward_structs];
	double* total_transition_cost = new double[no_reward_structs];
	
	//Timers
	clock_t start, start2, stop;
	double time_taken;
	
	start = start2 = clock();
	
	Sim_Print_To_Main_Log("\nSampling progress: [");
	
	//The loop continues until each pctl/csl formula is satisfied:
	//E.g. usually, this is when the correct number of iterations
	//has been performed, but for some, such as cumulative rewards
	//this can be early if a loop is detected at any point, this
	//is all handled by the All_Done_Sampling() function
	while(!should_stop_sampling && !All_Done_Sampling()) //while there are more samples needed
	{
		// output of progress
		percentage_done = ((10*(iteration_counter))/no_iterations)*10;
		if (percentage_done > last_percentage_done) {
			last_percentage_done = percentage_done;
			//cout << " " << last_percentage_done << "%" << endl;
			Sim_Print_To_Main_Log(" %d%%", last_percentage_done);
			Sim_Flush_Main_Log();
		}
		//if (iteration_counter%100000 == 0) {
			//cout << "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\bIteration: " << iteration_counter;
			//cout << "Iteration: " << iteration_counter << endl;
		//}
		
		if(((double)(clock()-start2)/ CLOCKS_PER_SEC) > 2) //do polling and feedback every 2 seconds
		{
			Write_Feedback(iteration_counter, no_iterations, false);
			int poll = Poll_Control_File();
			
			if(poll & STOP_SAMPLING == STOP_SAMPLING)
			{
				should_stop_sampling = true;
			}
			 start2 = clock();
		}
		
		iteration_counter++;
		
		//Start a (sample) new path for this iteration
		//============================================
		
		//Initialise variables for this loop
		loop_detection->Reset();
		for (int i = 0; i < no_reward_structs; i++) {
			path_cost[i] = 0.0;
			total_state_cost[i] = 0.0;
			total_transition_cost[i] = 0.0;
		}
		
		//set the current state to the correct starting state
		for(int i = 0; i < no_state_variables; i++)
			state_variables[i] = starting_variables[i];
		
		//Calculate initial state reward
		Calculate_State_Reward(state_variables);   
		
		Notify_Path_Formulae(last_state, state_variables, loop_detection);
		
		//====================================
		//Generate a path of up to path_length
		//====================================
		
		for(current_index = 0; 
			!All_PCTL_Answers_Known(loop_detection) &&	//not got answers for all properties yet
			//!loop_detection->Is_Proven_Looping()		//not looping (removed this - e.g. cumul rewards can't stop yet)
			current_index < path_length;				//not exceeding path_length
			current_index++)
		{
			last_state->Make_This_Current_State();
			for (int i = 0; i < no_reward_structs; i++) {
				last_state->state_instant_cost[i] = Get_State_Reward(i);
			}
			
			//Make an automatic choice for the current state
			double dummy = 0.0;
			Automatic_Update(loop_detection, dummy);
			Calculate_State_Reward(state_variables);
			
			//if(!loop_detection->Is_Proven_Looping()) { // removed this: for e.g. cumul rewards need to keep counting in loops...
			
			if(model_type == STOCHASTIC)
			{
				double time_in_state = Get_Sampled_Time();
				
				last_state->time_spent_in_state = time_in_state;
				
				for (int i = 0; i < no_reward_structs; i++) {
					last_state->state_cost[i] = last_state->state_instant_cost[i]*time_in_state;
					last_state->transition_cost[i] = Get_Transition_Reward(i);
					total_state_cost[i] += last_state->state_instant_cost[i]*time_in_state;
					total_transition_cost[i] += last_state->transition_cost[i];
					path_cost[i] = total_state_cost[i] + total_transition_cost[i];
					
					last_state->cumulative_state_cost[i] = total_state_cost[i];
					last_state->cumulative_transition_cost[i] = total_transition_cost[i];
				}
				
				Notify_Path_Formulae(last_state, state_variables, loop_detection);
			}
			else
			{
				for (int i = 0; i < no_reward_structs; i++) {
					last_state->state_cost[i] = last_state->state_instant_cost[i];
					last_state->transition_cost[i] = Get_Transition_Reward(i);
					total_state_cost[i] += last_state->state_instant_cost[i];
					total_transition_cost[i] += last_state->transition_cost[i];
					path_cost[i] = total_state_cost[i] + total_transition_cost[i];
					
					last_state->cumulative_state_cost[i] = total_state_cost[i];
					last_state->cumulative_transition_cost[i] = total_transition_cost[i];	
				}
				
				Notify_Path_Formulae(last_state, state_variables, loop_detection); 
			}
			//}
		
		}  //end path generation for loop
		
		// record if we found any deadlocks (can check this outside path gen loop because never escape deadlocks)
		if (loop_detection->Is_Deadlock()) deadlocks_found = true;
		
		// compute path length statistics so far
		average_path_length = (average_path_length*(iteration_counter-1)+(current_index))/iteration_counter;
		min_path_length = (iteration_counter == 1) ? current_index : ((current_index < min_path_length) ? current_index : min_path_length);
		max_path_length = (iteration_counter == 1) ? current_index : ((current_index > max_path_length) ? current_index : max_path_length);
		
		//Get samples and notify sample collectors
		Do_A_Sample(loop_detection);
		
		// stop early if any of the properties couldn't be sampled
		if (Get_Total_Num_Reached_Max_Path() > 0) { stopped_early = true; break; }
		
	}//end sampling while
	
	if (!stopped_early) {
		if (!should_stop_sampling) Sim_Print_To_Main_Log(" 100%% ]");
		Sim_Print_To_Main_Log("\n");
		stop = clock();
		time_taken = (double)(stop - start)/CLOCKS_PER_SEC;
		Sim_Print_To_Main_Log("\nSampling complete: %d iterations in %.2f seconds (average %.6f)\n", iteration_counter, time_taken, time_taken/iteration_counter);
		Sim_Print_To_Main_Log("Path length statistics: average %.1f, min %d, max %d\n", average_path_length, min_path_length, max_path_length);
	} else {
		Sim_Print_To_Main_Log(" ...\n\nSampling terminated early after %d iterations.\n", iteration_counter);
	}
	
	// print a warning if deadlocks occured at any point
	if (deadlocks_found) Sim_Print_To_Main_Log("\nWarning: Deadlocks were found during simulation: self-loops were added\n");
	
	// print a warning if simulation was stopped by the user
	if (should_stop_sampling) Sim_Print_To_Main_Log("\nWarning: Simulation was terminated before completion.\n");
	
	//write to feedback file with true to indicate that we have finished sampling
	Write_Feedback(iteration_counter, no_iterations, true);
	
	//Print_Sampling_Results();
	delete loop_detection;
	delete[] starting_variables;
	delete last_state;
	delete[] path_cost;
	delete[] total_state_cost;
	delete[] total_transition_cost;
	
	if (stopped_early) {
		Report_Error("One or more of the properties being sampled could not be checked on a sample. Consider increasing the maximum path length");
		throw 0;
	}
}

//==============================================================================
//      IO Functions
//==============================================================================

void Write_Sampling(int fd)
{
    //sampling identifier
    Write_Length_And_String("sp", fd);
    
    //write no propeties
    write(fd, &no_sample_holders, sizeof(int));

    //Formulae table
    //==============
    
    for(int i = 0; i < no_sample_holders; i++)
    {
        registered_sample_holders[i]->Write_Holder(fd);
    }

    //write null byte
    write(fd, "\0", 1);
}

void Read_Sampling(int fd)
{
	char buf_str[256];
    //cout << "read_sampling called" << endl;
    int int_buf;
    //read model header
    read(fd, &int_buf, sizeof(int));
    read(fd, &buf_str, int_buf+1);
    if(strcmp(buf_str, "sp") != 0)
    {
        throw "Error when importing binary file: sampling header not found";
    }
    //cout << "done reading header" << endl;
    //read no properties
    read(fd, &int_buf, sizeof(int));
    //cout << "There should be " << int_buf << "sample holders" << endl;

    //read in the properties
    for(int i = 0; i < int_buf; i++)
    {
        CSampleHolder* holder = Read_Sample_Holder(fd);
        //cout << "this is the holder loaded" << endl;
        //cout << holder->Get_Path_Formula()->To_String() << endl;
        Register_Sample_Holder(holder);
    }

    //read off null byte
    read(fd, &buf_str, 1);
    if(strcmp(buf_str, "") != 0)
    {
        throw "Error when importing binary file: state space not terminated correctly";
    }
}

CSampleHolder* Read_Sample_Holder(int fd)
{
	char buf_str[256];
    int int_buf;

    //read type of holder

    read(fd, &int_buf, sizeof(int));

    int value;
    
    switch(int_buf)
    {
        case HOLDER_PROB:
            //read the index
            read(fd, &value, sizeof(int));
            //read off null byte
            read(fd, &buf_str, 1);
            if(strcmp(buf_str, "") != 0)
            {
                throw "Error when importing binary file: state space not terminated correctly";
            }
            return new CProbEqualsQuestion(Get_Path_Formula(value));
        case HOLDER_REWARD:
            //read the index
            read(fd, &value, sizeof(int));
            //read off null byte
            read(fd, &buf_str, 1);
            if(strcmp(buf_str, "") != 0)
            {
                throw "Error when importing binary file: state space not terminated correctly";
            }
            return new CRewardEqualsQuestion(Get_Path_Formula(value));
        default:
            throw "error: unexpected sample holder type";
    }
}

void Write_Sampling_Results(int fd)
{
	char str[256];
    for(int i = 0; i < no_sample_holders; i++)
    {
        sprintf(str, "%d", i);
        write(fd, str, strlen(str));
        write(fd, "\t", 1);
        sprintf(str, "%d", registered_sample_holders[i]->no_samples);
        write(fd, str, strlen(str));
        write(fd, "\t", 1);
        sprintf(str, "%lf", registered_sample_holders[i]->cumulative_value);
        write(fd, str, strlen(str));
        write(fd, "\n", 1);
    }
}

