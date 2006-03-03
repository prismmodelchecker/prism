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


#include "simmodel.h"
#include "simexpression.h"
#include "simstate.h"
#include "simutil.h"
#include "simiohandler.h"
#include <cstdio>
#include <string>
#include <iostream>
#ifdef _WIN32
#include <io.h>
#else
#include <unistd.h>
#endif
#include <fcntl.h>

using std::cout;
using std::endl;
using std::string;


//=============================================================================
//	Description
//=============================================================================

/*
 *	This interface is for the storage of the current PRISM model.  The only
 *	modifications to the global members (for efficiency) are the initial
 *	setup methods and the deallocation methods.
 *
 *	The model storage contains:
 *		A Command Table:			Which is an array of CCommand objects
 *		A State Rewards Table:		Which is an array of CStateReward objects
 *		A Transition Rewards Table:	Which is an array of CTransitionReward objects
 *
 *	These objects use the expression data structures declared in 
 *	simexpression.h and refer to and manipulate the current state space 
 *	referred to in "simstate.h"
 */

//=============================================================================
//	Class Definitions
//=============================================================================

/*
 *	CAssignment
 *	===========
 *
 *	The CAssignment class is used to store assignments of the current model.
 *	It also provides the means to actually perform these assignments on a 
 *	given set of variables (usually state_variables)
 */

/*
 *	Constructor for CAssignment object. rhs should already be populated.
 */
CAssignment::CAssignment(int variable_index, CNormalExpression * rhs)
{
	this->rhs = rhs;
	this->variable_index = variable_index;
	this->result = 0;
}

/*
 *  Destructor for CAssignment object. deletes rhs from memory.
 */
CAssignment::~CAssignment()
{
	//cout << "CAssignment destructor called " << endl;
	if(rhs != NULL)
	{
		//cout << "deleting rhs" << endl;
		delete rhs;
	}
	rhs = NULL;
}

/*
 *	Calculates the value of rhs for the current state (state_variables) and
 *	stores the result.
 */
void CAssignment::Do_Assign()
{
	result = rhs->Evaluate(); //calculate the result;
}

/*
 *	Calculates the value of rhs for the state pointed to by vars and
 *	stores the result.
 */
void CAssignment::Do_Assign(int* vars)
{
	result = rhs->Evaluate(vars);
}

/*
 *	Actually updates the current state (state_variables) to what has
 *	been stored in result.
 */
void CAssignment::Complete_Assign()
{
	state_variables[variable_index] = result;
}

/*
 *	Actually updates the vars to what has been stored in result.
 */
void CAssignment::Complete_Assign(int* vars)
{
	vars[variable_index] = result;
}

/*
 *	Access method for the index of the assignment variable.
 */
int CAssignment::Get_Variable_Index()
{
	return this->variable_index;
}

/*
 *	Access method for the assignment expression.
 */
CNormalExpression* CAssignment::Get_RHS_Expression()
{
	return this->rhs;
}

/*
 *	Access method for the temporary assignment result.
 */
int CAssignment::Get_Result()
{
	Do_Assign();
	return this->result;
}

/*
 *	Returns a string representation of the CAssignment object.
 */
string CAssignment::To_String()
{
	string assign = "{"+Int_To_String(variable_index)+"} '= "+rhs->To_String();
	return assign;
}

void CAssignment::Write_Assignment(int fd)
{
        Write_Length_And_String("as", fd);

        //write variable index
        write(fd, &variable_index, sizeof(int));

        //Do Expression
        rhs->Write_Expression(fd);
}

/*
 *	CUpdate
 *	===========
 *
 *	The CUpdate class is used to store updates of the current model.
 *	Each update has an associated probability (rate) and an array of 
 *	CAssignment objects which should be applied if this update is
 *	selected.  The action_index for the update is stored also.
 *
 *	Note that this object is also used for synchronisation purposes
 *	and hence a synchronised CUpdate object does not own its
 *	assignments, this is important, as when it is deallocated, it
 *	should not delete them.
 */

/*
 *	Constructor for a CUpdate object.  probability should be
 *	populated.
 *	
 *	throws an exception if out of memory
 */
CUpdate::CUpdate(CExpression * probability,
				 int no_assignments)
{
	this->probability = probability;
	this->no_assignments = no_assignments;

	assignments = new CAssignment*[no_assignments];
	if(assignments == NULL)
	{
		Report_Error("Simulator engine ran out of memory when allocating an update.");
		throw "out of memory exception simmodel.cc 009";
	}
	for(int i = 0; i < no_assignments; i++)
	{
		assignments[i] = NULL;
	}
	assignment_count = 0;
	this->action_index = -1;
	
	owns_assignments = true;
	
	//cout << "do i own my assignments in the constructor" << owns_assignments << endl;
}

/*
 *	Destructor for a CUpdate object.  Deletes probability but
 *	should only delete assignments if owns_assignments is true.
 */
CUpdate::~CUpdate()
{
	//cout << "CUpdate destructor called" << endl;
	if(probability != NULL)
		delete probability;
	//cout << "do i own my assignments" << owns_assignments << endl;
	if(this->owns_assignments)
	{
		//cout << "owns em" << endl;
		if(assignments != NULL)
		{
			////cout << "trying to delete assignments " << endl;
			for(int i = 0; i < assignment_count; i++)
			{
				//cout << "loop " << i << endl;
				if(assignments[i] != NULL)
				{
					//cout << "should be deleting " << endl;
					delete assignments[i];
				}
			}
		}
	}
	delete[] assignments;
}

/*
 *	Used to populate assignments.  Adds	a CAssignment object to
 *	the next available position.
 *
 *	throws an exception if too many have already been added.
 */
void CUpdate::Add_Assignment(CAssignment * assign)
{
	if(assignment_count >= no_assignments) 
	{
		Report_Error("Unexpected error: too many assignments added to an update");
		throw "array access exception simmodel.cc 010";
	}
	assignments[assignment_count++] = assign;
}

/*
 *	Update method for action_index.
 */
void CUpdate::Set_Action_Index(int ai)
{
	this->action_index = ai;
}

/*
 *	Update method for owns_assignments.
 */
void CUpdate::Set_Owns_Assignments(bool owns)
{
	//cout << "srtting owns to " << owns << endl;
	this->owns_assignments = owns;
}

/*
 *	Returns a string representation of this CUpdate object.
 */
std::string CUpdate::To_String()
{
	std::string upd = "";
	upd+= this->probability->To_String()+": ";
	for(int i = 0; i < no_assignments; i++)
	{
		if(assignments[i] != NULL)
			upd += assignments[i]->To_String();
		if(i < no_assignments-1) upd += " + ";
	}
	return upd;
}

/*
 *	Perform this update on state_varaibles
 */
void CUpdate::Do_Update()
{
	//Calculate the cost for this transition
	//Calculate_Current_Transition_Reward(this->action_index);
	for(int i = 0; i < assignment_count; i++) //here assignment_count == no_assignments
		assignments[i]->Do_Assign();
	for(int i = 0; i < assignment_count; i++)
		assignments[i]->Complete_Assign();
}

/*
 *	Perform this update on vars
 */
void CUpdate::Do_Update(int*vars)
{
	//Calculate the cost for this transition
	//Calculate_Current_Transition_Reward(this->action_index);
	for(int i = 0; i < assignment_count; i++) //here assignment_count == no_assignments
		assignments[i]->Do_Assign(vars);
	for(int i = 0; i < assignment_count; i++)
		assignments[i]->Complete_Assign(vars);
}

/*
 *
 */
void CUpdate::Write_Update(int fd)
{
    Write_Length_And_String("up", fd);

    //write action_index
    write(fd, &action_index, sizeof(int));

    //Do Probability
    probability->Write_Expression(fd);

    //write no_assignments
    write(fd, &no_assignments, sizeof(int));

    //Do Assignments
    for(int i = 0; i < no_assignments; i++)
    {
        assignments[i]->Write_Assignment(fd);
    }
}


/*
 *	CCommand
 *	===========
 *
 *	The CCommand class is used to store commands of the current model.
 *	Each command has its own module and action label which are 
 *	indexed and has a guard expression.  Furthermore, it has an
 *	array of CUpdate objects for each member of the command's probability
 *	distribution.
 *
 *	Note that this object is also used for synchronisation purposes.  If
 *	used in this way, owns_guard should be set to false, so that when 
 *	the synchronised command is deleted, the guard is not lost from
 *	the model.
 */

/*
 *	Constructor for a CCommand object. guard should already
 *	be populated.
 *
 *	throws an exception if out of memory
 */
CCommand::CCommand(CNormalExpression *guard,
				   int action_index,
				   int module_index,
				   int no_updates)
{
	this->guard = guard;
	this->action_index = action_index;
	this->module_index = module_index;
	this->no_updates = no_updates;

	updates = new CUpdate*[no_updates];
	if(updates == NULL)
	{
		Report_Error("Simulator engine ran out of memory when allocating a Command object");
		throw "out of memory exception simmodel.cc 011";
	}
	for(int i = 0; i < no_updates; i++)
	{
		updates[i] = NULL;
	}
	update_count = 0;
	
	owns_guard = true;
}

/*
 *	Destructor for a CCommand object.  If owns_guard is
 *	false then guard is not deleted.
 */
CCommand::~CCommand()
{
	if(owns_guard)
	{
		//cout << "destructing command" << endl;
		if(guard != NULL)
			delete guard;
	}
	if(updates != NULL)
	{
		for(int i = 0; i < no_updates; i++)
		{
			if(updates[i] != NULL)
			{
				//if(owns_guard) cout << "deleting an update" << endl;
				delete updates[i];
			}
		}
		delete[] updates;
		updates = NULL;
	}
}


/*
 *	Populates updates, adding update to the next available position.
 *
 *	throws an exception if too many updates have been added.
 */
void CCommand::Add_Update(CUpdate* update)
{
	if(update_count >= no_updates) 
	{
		Report_Error("Unexpected Error: too many updates added to a command");
		throw "array access exception simmodel.cc 012";
	}
	updates[update_count++] = update;
	update->Set_Action_Index(this->action_index);
}

/*
 *	Returns a string representation of this CCommand object
 */
string CCommand::To_String()
{
	string comm = "["+Int_To_String(action_index)+"]\t"
		+"(module"+Int_To_String(module_index)+")\t"
		+guard->To_String()+"\t";
	for(int i = 0; i < no_updates; i++)
		if(updates[i] != NULL)
			comm += updates[i]->To_String() + "\t";
	return comm;
}

/*
 *	Update method for owns_guard
 */
void CCommand::Set_Owns_Guard(bool own)
{
	this->owns_guard = own;
}

void CCommand::Write_Command(int fd)
{
    Write_Length_And_String("co", fd);
    
    //write action index
    write(fd, &action_index, sizeof(int));

    //write module index
    write(fd, &module_index, sizeof(int));

    //Do Guard
    guard->Write_Expression(fd);

    //Do Updates

    //write no updates
    write(fd, &no_updates, sizeof(int));

    //write the updates
    for(int i = 0; i < no_updates; i++)
    {
        updates[i]->Write_Update(fd);
    }
}


/*
 *	CStateReward
 *	============
 *
 *	The CStateReward class is used to store each lines of the 
 *	rewards construct that correspond to state rewards.
 */

/*
 *	Constructor for a CStateReward object
 */
CStateReward::CStateReward(CExpression* state_guard, CExpression* reward)
{
	this->state_guard = state_guard;
	this->reward = reward;
}

/*
 *	Destructor for a CStateReward object.  This deletes
 *	state_guard and reward.
 */
CStateReward::~CStateReward()
{
	if(state_guard != NULL)
		delete this->state_guard;
	if(reward != NULL)
		delete this->reward;
}

/*
 *	If state_guard evaluates to true with state_variables, then
 *	reward is evaluated over state_variables and returned, else 0
 *	is returned
 */
double CStateReward::Get_Reward_For_State(int* variables)
{
	if(state_guard->Evaluate(variables))
		return reward->EvaluateDouble(variables);
	else return 0.0;
}

/*
 *	Returns a string representation of this CStateReward object.
 */
string CStateReward::To_String()
{
	return state_guard->To_String() + "\t" + reward->To_String();
}

void CStateReward::Write_Reward(int fd)
{
    Write_Length_And_String("sr", fd);
    

    //Do Guard
    state_guard->Write_Expression(fd);
    
    //DO Reward
    reward->Write_Expression(fd);
    
}

/*
 *	CTransitionReward
 *	=================
 *
 *	The CTransitionReward class is used to store each lines of the 
 *	rewards construct that correspond to transition rewards.
 */

/*
 *	Constructor for a CTransitionReward object
 */
CTransitionReward::CTransitionReward(int action_index, CExpression* state_guard, CExpression* reward)
{
	this->action_index = action_index;
	this->state_guard = state_guard;
	this->reward = reward;
}

/*
 *	Destructor for a CTransitionReward object.  This deletes
 *	state_guard and reward.
 */
CTransitionReward::~CTransitionReward()
{
	if(state_guard != NULL)
	delete this->state_guard;
	if(reward != NULL)
	delete this->reward;
}

/*
 *	If state_guard evaluates to true with state_variables, and 
 *	this->action_index == action_index then
 *	reward is evaluated over state_variables and returned, else 0
 *	is returned
 */
double CTransitionReward::Get_Reward_For_Selected_Transition(int* variables, int action_index)
{
	if(action_index == this->action_index)
	{
		if(state_guard->Evaluate(variables))
		{
			return reward->EvaluateDouble(variables);
		}
		else return 0.0;
	}
	else return 0.0;
}

/*
 *	Returns a string representation of this CTransitionReward object.
 */
string CTransitionReward::To_String()
{
	return "["+Int_To_String(action_index)+"]\t"+state_guard->To_String() + "\t" + reward->To_String();
}

void CTransitionReward::Write_Reward(int fd)
{
    Write_Length_And_String("tr", fd);
    
    //Do Action Index
    write(fd, &action_index, sizeof(int));

    //Do Guard
    state_guard->Write_Expression(fd);
    
    //DO Reward
    reward->Write_Expression(fd);
}


//=============================================================================
//	Globals
//=============================================================================

//The Type of Model
int model_type;	//NOT_LOADED, PROBABILISTIC, NONDETERMINISTIC or STOCHASTIC.

//The Command Table
CCommand** command_table; 
int no_commands;

 //The State Rewards Table
CStateReward** state_rewards_table;
int no_state_rewards;

//The Transition Rewards Table
CTransitionReward** transition_rewards_table; 
int no_transition_rewards;

//The Alphabet stores which modules contain which action labels
bool** alphabet; // can be accessed by [module_index][action_index]
int no_modules;
int no_actions;

//=============================================================================
//	Local Data
//=============================================================================

int no_commands_added; //used to keep track of populating command_table
int no_state_rewards_added;	//used to keep track of populating state_rewards_table
int no_transition_rewards_added; // used to keep track of populating transition_rewards_table


//==============================================================================
//	Functions
//==============================================================================

/*
 *	If there is a model stored, this function completely removes it
 *	from memory.
 */
void Deallocate_Model()
{
	//cout << "deallocating model " << endl;
	model_type = NOT_LOADED;

	//Delete Command Table
	if(command_table != NULL)
	{
		//cout << "table not null no_commands_added = "<< no_commands_added << endl;
		for(int i = 0; i < no_commands_added; i++)
		{
			//cout << "command " << i << endl;
			if(command_table[i] != NULL)
			{
				//cout << "deallocating table at " << i << endl;
				delete command_table[i];
			}
		}
		delete [] command_table;
		command_table = NULL;
	}
	no_commands = 0;
	no_commands_added = 0;

	//Delete State Rewards Table
	if(state_rewards_table != NULL)
	{
		for(int i = 0; i < no_state_rewards_added; i++)
		{
			CStateReward* reward = state_rewards_table[i];
			if(reward != NULL)
				delete reward;
		}
		delete[] state_rewards_table;
		state_rewards_table = NULL;
	}
	no_state_rewards = 0;
	no_state_rewards_added = 0;

	//Delete Transition Rewards Table
	if(transition_rewards_table != NULL)
	{
		for(int i = 0; i < no_transition_rewards_added; i++)
		{
			CTransitionReward* reward = transition_rewards_table[i];
			if(reward != NULL)
				delete reward;
		}
		delete[] transition_rewards_table;
		transition_rewards_table = NULL;
	}
	no_transition_rewards = 0;
	no_transition_rewards_added = 0;

	//Delete Alphabet
	if(alphabet != NULL)
	{
		for(int i = 0; i < no_modules; i++)
		{
			if(alphabet[i] != NULL)
			delete [] alphabet[i];
			alphabet[i] = NULL;
		}
		delete [] alphabet;
		alphabet = NULL;
	}
	no_modules = 0;
	no_actions = 0;
}

/*
 *	Initialises the model storage.  type is one of the constants stored in 
 *	simutil.h: NOT_LOADED, PROBABILISTIC, NONDETERMINISTIC or STOCHASTIC.
 *	Memory is allocated for the command table, state reward table, transition
 *	reward table and alphabet table.
 *
 *	throws an exception for out of memory.
 */
void Allocate_Model(int type, int no_comms, int no_state_rews, 
					int no_transition_rews, int no_mods, int no_acts)
{
	model_type = type;

	//Allocate the Command Table
	no_commands = no_comms;
	command_table = new CCommand*[no_commands];
	if(command_table == NULL) 
	{
		Report_Error("Simulator engine ran out of memory when allocating transition table.");
		throw "out of memory exception simmodel.cc 001";
	}
	for(int i = 0; i < no_commands; i++)
		command_table[i] = NULL;
	no_commands_added = 0;

	//Allocate the State Reward Table
	no_state_rewards = no_state_rews;
	state_rewards_table = new CStateReward*[no_state_rewards];
	if(state_rewards_table == NULL) 
	{
		Report_Error("Simulator engine ran out of memory when allocating state rewards table.");
		throw "out of memory exception simmodel.cc 002";
	}
	for(int i = 0; i < no_state_rewards; i++)
		state_rewards_table[i] = NULL;
	no_state_rewards_added = 0;

	//Allocate the Transition Reward Table
	no_transition_rewards = no_transition_rews;
	transition_rewards_table = new CTransitionReward*[no_transition_rewards];
	if(transition_rewards_table == NULL)  
	{
		Report_Error("Simulator engine ran out of memory when allocating transition rewards table.");
		throw "out of memory exception simmodel.cc 003";
	}
	for(int i = 0; i < no_transition_rewards; i++)
		transition_rewards_table[i] = NULL;
	no_transition_rewards_added = 0;

	//Allocate Alphabet
	no_modules = no_mods;
	no_actions = no_acts;

	alphabet = new bool*[no_modules];
	if(alphabet == NULL)
	{
		Report_Error("Simulator engine ran out of memory when allocating alphabet table.");
		throw "out of memory exception simmodel.cc 004";
	}
	for(int i = 0; i < no_modules; i++)
	{
		alphabet[i] = new bool[no_actions];
		if(alphabet[i] == NULL)
		{
			Report_Error("Simulator engine ran out of memory when allocating alphabet table.");
			throw "out of memory exception simmodel.cc 005";
		}
		for(int j = 0; j < no_actions; j++)
			alphabet[i][j] = false;
	}

}

/*
 *	Adds the CCommand pointed to by comm to the command_table at the next
 *	available position.	Also sorts out the alphabet for this command
 *
 *	throws an exception if the table is already full.
 */
void Add_Command_To_Model(CCommand* comm)
{
        
	if(no_commands_added == no_commands) 
	{
		Report_Error("Unexpected error when loading model into Simulator engine:\n Too many transitions have been added.");
		throw "array access exception: simmodel.cc 006";
	}
	command_table[no_commands_added++] = comm;
	//sort out alphabet information for this command
	if(comm->action_index > -1)
		alphabet[comm->module_index][comm->action_index] = true;
}

/*
 *	Adds the CStateReward pointed to by sr to the state_reward_table at the next
 *	available position.
 *
 *	throws an exception if the table is already full.
 */
void Add_State_Reward_To_Model(CStateReward* sr)
{
	if(no_state_rewards_added == no_state_rewards) 
	{
		Report_Error("Unexpected error when loading model into Simulator engine:\n Too many state rewards have been added.");
		throw "array access exception: simmodel.cc 007";
	}
	state_rewards_table[no_state_rewards_added++] = sr;
}

/*
 *	Adds the CTransitionReward pointed to by tr to the state_reward_table 
 *	at the next available position.
 *
 *	throws an exception if the table is already full.
 */
void Add_Transition_Reward_To_Model(CTransitionReward* tr)
{
	if(no_transition_rewards_added == no_transition_rewards) 
	{
		Report_Error("Unexpected error when loading model into Simulator engine:\n Too many state rewards have been added.");
		throw "array access exception: simmodel.cc 008";
	}
	transition_rewards_table[no_transition_rewards_added++] = tr;
}


//=============================================================================
//	Helper Functions
//=============================================================================

/*
 *	Returns a string representation of the model
 */
string Model_To_String()
{
	string model = "Loaded Model:\n\n";
	model+="Model Type:\n\n";
	model+=Model_Type_To_String()+"\n\n";
	
	model+="Variable Table:\n\n";
	model+=State_Variables_To_String()+"\n\n";
	
	model+="Command Table:\n\n";
	model+=Command_Table_To_String()+"\n\n";
	
	model+="State Rewards Table:\n\n";
	model+=State_Reward_To_String()+"\n\n";

	model+="Transition Rewards Table:\n\n";
	model+=Transition_Reward_To_String()+"\n\n";

	return model;
}

/*
 *	Returns a string representation of the model type
 */
string Model_Type_To_String()
{
	switch(model_type)
	{
		case NOT_LOADED: return "Not Loaded";
		case PROBABILISTIC: return "dtmc";
		case NONDETERMINISTIC: return "mdp";
		case STOCHASTIC: return "ctmc";
		default: return "Invalid model type";
	}
}

/*
 *	Returns a string representation of the Command Table
 */
string Command_Table_To_String()
{
	string ttable = "action\tmodule\tguard\tupdates\n";
	for(int i = 0; i < no_commands; i++)
	{
		if(command_table[i] != NULL)
		{
			ttable += command_table[i]->To_String() + "\n";
		}
	}
	return ttable;
}

/*
 *	Returns a string representation of the State Reward Table
 */
string State_Reward_To_String()
{
	string rewards_string = "guard\treward\n";
	for(int i = 0; i < no_state_rewards; i++)
	{
		if(state_rewards_table[i] != NULL)
		{
			rewards_string += state_rewards_table[i]->To_String();
		}
	}
	return rewards_string;
}

/*
 *	Returns a string representation of the Transition Reward Table
 */
string Transition_Reward_To_String()
{
	string rewards_string = "guard\treward\n";
	for(int i = 0; i < no_transition_rewards; i++)
	{
		if(transition_rewards_table[i] != NULL)
		{
			rewards_string += transition_rewards_table[i]->To_String() + "\n";
		}
	}
	return rewards_string;
}

//==============================================================================
//      IO Functions
//==============================================================================

void Write_Model(int fd)
{
    //model identifier
    Write_Length_And_String("mo", fd);
    
    //write model type
    write(fd, &model_type, sizeof(int));

    //Command table
    //=============
    
    //write number of commands
    write(fd, &no_commands, sizeof(int));

    //Commands
    for(int i = 0; i < no_commands; i++)
    {
        command_table[i]->Write_Command(fd);
    }

    //State Rewards Table
    //===================

    //write number of state rewards
    write(fd, &no_state_rewards, sizeof(int));

    for(int i = 0; i < no_state_rewards; i++)
    {
        state_rewards_table[i]->Write_Reward(fd);
    }

    //Transition Rewards Table
    //========================

    //write number of transition rewards
    write(fd, &no_transition_rewards, sizeof(int));

    for(int i = 0; i < no_transition_rewards; i++)
    {
        transition_rewards_table[i]->Write_Reward(fd);
    }

    //Alphabet
    //========

    //write number of modules
    write(fd, &no_modules, sizeof(int));

    //write number of synchs
    write(fd, &no_actions, sizeof(int));

    //write the contents of the alphabet 2D array
    for(int i = 0; i<no_modules; i++)
    {
        for(int j = 0; j < no_actions; j++)
        {
            write(fd, &alphabet[i][j], sizeof(bool));
        }
    }
    //write null byte
    write(fd, "\0", 1);

}

void Read_Model(int fd)
{
    //cout << "attempting to load model..." << endl;
    int int_buf;
	char buf_str[256];
    //read model header
    read(fd, &int_buf, sizeof(int));
    read(fd, &buf_str, int_buf+1);
    if(strcmp(buf_str, "mo") != 0)
    {
        throw "Error when importing binary file: model header not found";
    }
    //cout << "reading model type..." ;
    //read model type
    read(fd, &int_buf, sizeof(int));

    model_type = int_buf;
    //cout << model_type << endl;

    //read command table
    //==================
    //cout << "reading size of command table...";
    //read size of command table
    read(fd, &int_buf, sizeof(int));

    no_commands = int_buf;
    //cout << no_commands << endl;
    command_table = new CCommand*[no_commands];
    if(command_table == NULL) 
    {
	Report_Error("Simulator engine ran out of memory when allocating transition table.");
	throw "out of memory exception simmodel.cc 040";
    }
    for(int i = 0; i < no_commands; i++)
		command_table[i] = NULL;
    no_commands_added = 0;
    //cout << "command table allocated" << endl;
    //read commands from the command table
    for(int i = 0; i < no_commands; i++)
    {
        //cout << "doing next command" << endl;
        CCommand* comm = Read_Command(fd);
        //cout << "the command is " << endl;
        //cout << comm->To_String() << endl;
        command_table[i] = comm;
	no_commands_added++;
    }
    //cout << "finished reading commands" << endl;
    //read state rewards table
    //========================
    
    //read size of state rewards table
    read(fd, &int_buf, sizeof(int));

    no_state_rewards = int_buf;
    state_rewards_table = new CStateReward*[no_state_rewards];
    if(state_rewards_table == NULL) 
    {
	Report_Error("Simulator engine ran out of memory when allocating state rewards table.");
	throw "out of memory exception simmodel.cc 041";
    }
    for(int i = 0; i < no_state_rewards; i++)
		state_rewards_table[i] = NULL;
    no_state_rewards_added = 0;
	//cout <<"importmodel1"<<endl;
    //read commands from the command table
    for(int i = 0; i < no_state_rewards; i++)
    {
        CStateReward* comm = Read_State_Reward(fd);
        Add_State_Reward_To_Model(comm);
    }
//cout <<"importmodel2"<<endl;
    //read transition rewards table
    //=============================
    
    //read size of state rewards table
    read(fd, &int_buf, sizeof(int));

    no_transition_rewards = int_buf;
    transition_rewards_table = new CTransitionReward*[no_transition_rewards];
    if(transition_rewards_table == NULL) 
    {
	Report_Error("Simulator engine ran out of memory when allocating transition rewards table.");
	throw "out of memory exception simmodel.cc 042";
    }
    for(int i = 0; i < no_transition_rewards; i++)
		transition_rewards_table[i] = NULL;
    no_transition_rewards_added = 0;
//	cout <<"importmodel3"<<endl;
    //read commands from the command table
    for(int i = 0; i < no_transition_rewards; i++)
    {
        CTransitionReward* comm = Read_Transition_Reward(fd);
        Add_Transition_Reward_To_Model(comm);
    }
//	cout <<"importmodel4"<<endl;
    //read alphabet
    //=============

    //read no modules
    read(fd, &int_buf, sizeof(int));
    no_modules = int_buf;
    //read no synchs
    read(fd, &int_buf, sizeof(int));
    no_actions = int_buf;
    //allocate storage for alphabet
    alphabet = new bool*[no_modules];
    if(alphabet == NULL)
    {
	Report_Error("Simulator engine ran out of memory when allocating alphabet table.");
	throw "out of memory exception simmodel.cc 043";
    }
    for(int i = 0; i < no_modules; i++)
    {
	alphabet[i] = new bool[no_actions];
	if(alphabet[i] == NULL)
	{
		Report_Error("Simulator engine ran out of memory when allocating alphabet table.");
		throw "out of memory exception simmodel.cc 044";
	}
        for(int j = 0; j < no_actions; j++)
            alphabet[i][j] = false;
    }

//cout <<"importmodel5"<<endl;
    //read it in
    bool bool_buf;
    for(int i = 0; i<no_modules; i++)
    {
        for(int j = 0; j < no_actions; j++)
        {
            read(fd, &bool_buf, sizeof(bool));
            alphabet[i][j] = bool_buf;
        }
    }
	//cout <<"importmodel6"<<endl;
    //read off null byte
    read(fd, &buf_str, 1);
    if(strcmp(buf_str, "") != 0)
    {
        throw "Error when importing binary file: model not terminated correctly";
    }
	//cout <<"importmodel7"<<endl;
}

CCommand* Read_Command(int fd)
{
    //cout << "Reading a command" << endl;
    int int_buf;
	char buf_str[256];
    //read model header
    read(fd, &int_buf, sizeof(int));
    read(fd, &buf_str, int_buf+1);
    if(strcmp(buf_str, "co") != 0)
    {
        throw "Error when importing binary file: command header not found";
    }

    //cout << "Reading action index...";
    //read action index
    int action_index;
    read(fd, &action_index, sizeof(int));
    //cout << action_index << endl;

    //cout << "Reading module index...";
    //read module index
    int module_index;
    read(fd, &module_index, sizeof(int));
    //cout << module_index << endl;

    //Read guard
    //cout << "Reading guard" << endl;
    CNormalExpression* guard = Read_Normal_Expression(fd);
    
    //cout << "Guard done " << guard->To_String() << endl;

    //Read Updates
    
    //cout << "Reading updates..."<< endl;
    //read no updates
    read(fd, &int_buf, sizeof(int));
    int no_updates = int_buf;
    //allocate the command
    CCommand* command = new CCommand(guard, action_index, module_index, no_updates);

    for(int i = 0; i < no_updates; i++)
    {
        CUpdate* upd = Read_Update(fd);
        command->Add_Update(upd);
    }

    //cout << "done updates" << endl;

    return command;
}

CUpdate* Read_Update(int fd)
{
    //cout << "reading update" << endl;
    int int_buf;
	char buf_str[256];
    //read model header
    read(fd, &int_buf, sizeof(int));
    //cout << "int_buf = " << int_buf << endl;
	read(fd, &buf_str, int_buf+1);
	//cout << "buf_str = " << buf_str << endl;
    if(strcmp(buf_str, "up") != 0)
    {
        throw "Error when importing binary file: update header not found";
    }

    //cout << "reading action index...";
    //read the action index
    int action_index;
    read(fd, &int_buf, sizeof(int));
    action_index = int_buf;
    //cout << action_index << endl;

    //Read probability
    //cout << "reading probability expression" << endl;
    CExpression* probability = Read_Expression(fd);
    //cout << "done probability: " << probability->To_String() << endl;

    //cout << "doing assignments" << endl;
    //Read Assignments
    int no_assignments;
    read(fd, &int_buf, sizeof(int));
    no_assignments = int_buf;
    //cout << "there should be " << no_assignments << " assignments " << endl;
    //allocate the update
    CUpdate* upd = new CUpdate(probability, no_assignments);
    upd->Set_Action_Index(action_index);
    //cout << "update allocated " << endl;
    for(int i = 0; i < no_assignments; i++)
    {
        //cout << "attempting to read an assignment" << endl;
        CAssignment* assign = Read_Assignment(fd);
        //cout << "reading assignment done " << assign->To_String() << endl;
        upd->Add_Assignment(assign);
    }
    //cout << "done assignments" << endl;
    
    return upd;
}


CAssignment* Read_Assignment(int fd)
{
    //cout << "Reading an assignment" << endl;
   int int_buf;
   char buf_str[256];
    //read model header
    read(fd, &int_buf, sizeof(int));
    read(fd, &buf_str, int_buf+1);
    if(strcmp(buf_str, "as") != 0)
    {
        throw "Error when importing binary file: assignment header not found";
    }

    //read variable index
    int variable_index;
    read(fd, &int_buf, sizeof(int));
    variable_index = int_buf;
        
    //Read Expression
    //cout << "reading the assignment expression " << endl;
    CNormalExpression* expr = Read_Normal_Expression(fd);
    //cout << "done" << endl;
    //cout << "it is: " << expr->To_String();
    return new CAssignment(variable_index, expr);
}

CStateReward* Read_State_Reward(int fd)
{
	//cout << "reading state reward" << endl;
    int int_buf;
	char buf_str[256];
    //read model header
    //cout << "reading state reward 1" << endl;
	read(fd, &int_buf, sizeof(int));
    read(fd, &buf_str, int_buf+1);
    if(strcmp(buf_str, "sr") != 0)
    {
        throw "Error when importing binary file: state reward header not found";
    }
	//cout << "reading state reward 2" << endl;
    CExpression* guard = Read_Expression(fd);
	//cout << "reading state reward 3" << endl;
    CExpression* reward = Read_Expression(fd);
	//cout << "reading state reward 4" << endl;
    return new CStateReward(guard, reward);
}



CTransitionReward* Read_Transition_Reward(int fd)
{
    int int_buf;
	char buf_str[256];
    //read model header
    read(fd, &int_buf, sizeof(int));
    read(fd, &buf_str, int_buf+1);
    if(strcmp(buf_str, "tr") != 0)
    {
        throw "Error when importing binary file: transition reward header not found";
    }

    //read action index
    int action_index;
    read(fd, &action_index, sizeof(int));
    
    CExpression* guard = Read_Expression(fd);
    CExpression* reward = Read_Expression(fd);

    return new CTransitionReward(action_index, guard, reward);
}

