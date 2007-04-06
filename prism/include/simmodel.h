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

#ifndef _Included_simulator_Modelheader
#define _Included_simulator_Modelheader

#include "simexpression.h"
#include <string>

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
 *		A Command Table:					Which is an array of CCommand objects
 *		A set of State Rewards Tables:		Which is an array of arrays of CStateReward objects
 *		A set of Transition Rewards Tables:	Which is an array of arrays of CTransitionReward objects
 *
 *	These objects use the expression data structures declared in 
 *	simexpression.h and refer to and manipulate the current state space 
 *	referred to in "simstate.h"
 */

//=============================================================================
//	Classes
//=============================================================================

/*
 *	The CAssignment class is used to store assignments of the current model.
 *	It also provides the means to actually perform these assignments on a 
 *	given set of variables (usually state_variables)
 */
class CAssignment
{
	private:
		int variable_index; //Index of the assignment variable
		CNormalExpression * rhs; //Expression to obtain the assignment value
		int result; //The temporary result of the assignment, before it is applied
		
	public:
		/*
		 *	Constructor for CAssignment object. rhs should already be populated.
		 */
		CAssignment (int var_index, CNormalExpression* rhs);

		/*
		 *  Destructor for CAssignment object. deletes rhs from memory.
		 */
		~CAssignment();

		/*
		 *	Calculates the value of rhs for the current state (state_variables) and
		 *	stores the result.
		 */
		void Do_Assign();

		/*
		 *	Calculates the value of rhs for the state pointed to by vars and
		 *	stores the result.
		 */
		void Do_Assign(int* vars);

		/*
		 *	Actually updates the current state (state_variables) to what has
		 *	been stored in result.
		 */
		void Complete_Assign();

		/*
		 *	Actually updates the vars to what has been stored in result.
		 */
		void Complete_Assign(int*vars);

		/*
		 *	Access method for the index of the assignment variable.
		 */
		int Get_Variable_Index();

		/*
		 *	Access method for the assignment expression.
		 */
		CNormalExpression* Get_RHS_Expression();

		/*
		 *	Access method for the temporary assignment result.
		 */
		int Get_Result();

		/*
		 *	Returns a string representation of the CAssignment object.
		 */
		string To_String();

		void Write_Assignment(int fd);
};

/*
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
class CUpdate
{
	private:
		int assignment_count; //temporary no assignments added so far
		bool owns_assignments; //false if this update is synchronised
		
	public:
		int action_index; //The action index for this update
		CExpression* probability; //An expression representing the probability of the update
		CAssignment** assignments; //An array of assignments for this update
		int no_assignments; //The number of assignments

		/*
		 *	Constructor for a CUpdate object.  probability should be
		 *	populated.
		 *	
		 *	throws an exception if out of memory
		 */
		CUpdate(CExpression* probability, int no_assignments);

		/*
		 *	Destructor for a CUpdate object.  Deletes probability but
		 *	should only delete assignments if owns_assignments is true.
		 */
		~CUpdate();

		/*
		 *	Used to populate assignments.  Adds	a CAssignment object to
		 *	the next available position.
		 *
		 *	throws an exception if too many have already been added.
		 */
		void Add_Assignment(CAssignment* assign);

		/*
		 *	Update method for owns_assignments.
		 */
		void Set_Owns_Assignments(bool owns);

		/*
		 *	Update method for action_index.
		 */
		void Set_Action_Index(int index);

		/*
		 *	Returns a string representation of this CUpdate object.
		 */
		string To_String();

		/*
		 *	Do the update on state_variables
		 */
		void Do_Update();

		 /*
		  *	 Do the update on vars
		  */
		void Do_Update(int*vars);

		void Write_Update(int fd);
};

/*
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
class CCommand
{
	private:
		int update_count; //temporary no updates added so far
		bool owns_guard; //should be false if command is synchronised

	public:
		CNormalExpression* guard;	//guard
		int action_index; //action or synch
		int module_index; //module
		CUpdate** updates; //updates
		int no_updates;	//number of updates

		/*
		 *	Constructor for a CCommand object. guard should already
		 *	be populated.
		 *
		 *	throws an exception is out of memory
		 */
		CCommand (CNormalExpression* guard, int action_index, int module_index, int num_updates); 

		/*
		 *	Destructor for a CCommand object.  If owns_guard is
		 *	false then guard is not deleted.
		 */
		~CCommand();

		/*
		 *	Populates updates, adding update to the next available position.
		 *
		 *	throws an exception if too many updates have been added.
		 */
		void Add_Update(CUpdate* update);

		/*
		 *	Returns a string representation of this CCommand object
		 */
		string To_String();

		/*
		 *	Update method for owns_guard
		 */
		void Set_Owns_Guard(bool);

                void Write_Command(int fd);
};

/*
 *	The CStateReward class is used to store each lines of the 
 *	rewards construct that correspond to state rewards.
 */
class CStateReward
{
	private:
		CExpression* state_guard; //expression representing when this reward should be accumulated
		CExpression* reward; //expression for the value of the accumulated reward

	public:

		/*
		 *	Constructor for a CStateReward object
		 */
		CStateReward(CExpression* state_guard, CExpression* reward);

		/*
		 *	Destructor for a CStateReward object.  This deletes
		 *	state_guard and reward.
		 */
		~CStateReward();

		/*
		 *	If state_guard evaluates to true with state_variables, then
		 *	reward is evaluated over state_variables and returned, else 0
		 *	is returned
		 */
		double Get_Reward_For_State(int* variables);

		/*
		 *	Returns a string representation of this CStateReward object.
		 */
		string To_String();

                void Write_Reward(int fd);
};

/*
 *	The CTransitionReward class is used to store each lines of the 
 *	rewards construct that correspond to transition rewards.
 */
class CTransitionReward
{
	private:
		int action_index; //-1 is no action
		CExpression* state_guard;  //expression representing when this reward should be accumulated
		CExpression* reward; //expression for the value of the accumulated reward

	public:

		/*
		 *	Constructor for a CTransitionReward object
		 */
		CTransitionReward(int action_index, CExpression* state_guard, CExpression* reward);

		/*
		 *	Destructor for a CTransitionReward object.  This deletes
		 *	state_guard and reward.
		 */
		~CTransitionReward();

		/*
		 *	If state_guard evaluates to true with state_variables, and 
		 *	this->action_index == action_index then
		 *	reward is evaluated over state_variables and returned, else 0
		 *	is returned
		 */
		double Get_Reward_For_Selected_Transition(int* variables, int action_index);

		/*
		 *	Returns a string representation of this CTransitionReward object.
		 */
		string To_String();

                void Write_Reward(int fd);
};

//=============================================================================
//	Globals
//=============================================================================

//The Type of Model
extern int model_type;	//NOT_LOADED, PROBABILISTIC, NONDETERMINISTIC or STOCHASTIC.

//The Command Table
extern CCommand** command_table; 
extern int no_commands;

 //The Rewards Tables
extern int no_reward_structs;
extern CStateReward*** state_rewards_table;
extern int *no_state_rewards;
extern CTransitionReward*** transition_rewards_table; 
extern int *no_transition_rewards;

//The Alphabet stores which modules contain which action labels
extern bool** alphabet; // can be accessed by [module_index][action_index]
extern int no_modules;
extern int no_actions;


//==============================================================================
//	Functions
//==============================================================================

/*
 *	If there is a model stored, this function completely removes it
 *	from memory.
 */
void Deallocate_Model();

/*
 *	Initialises the model storage.  type is one of the constants stored in 
 *	simutil.h: NOT_LOADED, PROBABILISTIC, NONDETERMINISTIC or STOCHASTIC.
 *	Memory is allocated for the command table, state reward table, transition
 *	reward table and alphabet table.
 *
 *	throws an exception for out of memory.
 */
void Allocate_Model(int type, int no_comms, int no_rew_structs, 
					int *no_state_rews, int *no_transition_rews, int no_mods, int no_acts);

/*
 *	Adds the CCommand pointed to by comm to the command_table at the next
 *	available position.
 *
 *	throws an exception if the table is already full.
 */
void Add_Command_To_Model(CCommand* comm);

/*
 *	Adds the CStateReward pointed to by sr to the ith state_reward_table at the next
 *	available position.
 *
 *	throws an exception if the table is already full.
 */
void Add_State_Reward_To_Model(int i, CStateReward* sr);

/*
 *	Adds the CTransitionReward pointed to by tr to the ith transition_reward_table 
 *	at the next available position.
 *
 *	throws an exception if the table is already full.
 */
void Add_Transition_Reward_To_Model(int i, CTransitionReward* tr);

/*
 *	Calculates the maximum number of updates that can be made for any 
 *	state of the current model.
 */
int Get_Maximum_Num_Updates();

/*
 *	Calculates the maximum number of assignments that can be done for any
 *	potential update of the model.
 */
int Get_Maximum_Num_Assignments();


//=============================================================================
//	Helper Functions
//=============================================================================

/*
 *	Returns a string representation of the model
 */
string Model_To_String();

/*
 *	Returns a string representation of the model type
 */
string Model_Type_To_String();

/*
 *	Returns a string representation of the Command Table
 */
string Command_Table_To_String();

/*
 *	Returns a string representation of the ith State Reward Table
 */
string State_Reward_To_String(int i);

/*
 *	Returns a string representation of the ith Transition Reward Table
 */
string Transition_Reward_To_String(int i);

//==============================================================================
//      IO Functions
//==============================================================================

void Write_Model(int fd);

void Read_Model(int fd);

CCommand* Read_Command(int fd);

CUpdate* Read_Update(int fd);

CAssignment* Read_Assignment(int fd);

CStateReward* Read_State_Reward(int fd);

CTransitionReward* Read_Transition_Reward(int fd);


#endif
