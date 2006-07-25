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
#include "simstate.h"
#include "simutil.h"
#include "simiohandler.h"
#include <cstdio>
#include <string>
#ifdef _WIN32
#include <io.h>
#else
#include <unistd.h>
#endif
#include <stdio.h>
#include <iostream>

using std::string;
using std::cout;
using std::endl;


//=============================================================================
//	Description
//=============================================================================

/*
 *	This interface is for the the current state space.  It simply
 *	maintains an array of values of the current set of variables.  
 *	Each variable, whether boolean or int is stored as an int and can be
 *	accessed by using the globally accessible state_variables array or by
 *	the set of convienience methods.
 */


//=============================================================================
//	Class Definitions
//=============================================================================

/*
 *	CPathState
 *	==========
 *
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

/*
 *	Constructs the state according with no_state_variables storage spaces 
 *	choice_made is initially PATH_NO_CHOICE_MADE (no choice made yet)
 */
CPathState::CPathState()
{
	this->variables = new int[no_state_variables];
	this->choice_made = PATH_NO_CHOICE_MADE;
	//Initially fill with undefined data
	for(int i = 0; i < no_state_variables; i++)
	{
		variables[i] = UNDEFINED_INT;
	}
	this->time_spent_in_state = 0.0;
	this->time_known = false;
	this->state_cost = new double[no_reward_structs];
	this->state_instant_cost = new double[no_reward_structs];
	this->path_cost_so_far = new double[no_reward_structs];
	this->transition_cost = new double[no_reward_structs];
	for(int i = 0; i < no_reward_structs; i++) {
		this->state_cost[i] = 0.0;
		this->state_instant_cost[i] = 0.0;
		this->path_cost_so_far[i] = 0.0;
		this->transition_cost[i] = 0.0;
	}
	this->probability = 0.0;
}

/*
 *	Destructor removes the variables array.
 */
CPathState::~CPathState()
{
	if(variables != NULL) delete[] variables;
	if(state_cost != NULL) delete[] state_cost;
	if(state_instant_cost != NULL) delete[] state_instant_cost;
	if(path_cost_so_far != NULL) delete[] path_cost_so_far;
	if(transition_cost != NULL) delete[] transition_cost;
}

/*
 *	Set this state to match a copy of state_variables.
 */
void CPathState::Make_This_Current_State()
{
	//std::cout << "Called Make_This_Current_State()" << std::endl;
	for(int i = 0; i < no_state_variables; i++)
		variables[i] = state_variables[i];
	this->time_spent_in_state = 0.0;
	this->time_known = false;
	for(int i = 0; i < no_reward_structs; i++) {
		this->state_cost[i] = 0.0;
		this->state_instant_cost[i] = 0.0;
		this->path_cost_so_far[i] = 0.0;
		this->transition_cost[i] = 0.0;
	}
	this->probability = 0.0;
}

/*
 *	Set state_variables to match a copy of variables.
 */
void CPathState::Make_Current_State_This()
{
	for(int i = 0; i < no_state_variables; i++)
		state_variables[i] = variables[i];
}

/*
 *	Returns a string representation of this path state.
 */
string CPathState::To_String()
{
	string returner = "";
	for(int i = 0; i < no_state_variables; i++)
	{
		returner += Int_To_String(variables[i]);
		returner += "\t";
	}
	if(time_known)
	{
		returner += "t = "+Double_To_String(this->time_spent_in_state);
		returner += "\tsc = [";
		for (int i=0; i < no_reward_structs; i++) returner += " "+Double_To_String(this->state_cost[i]);
		returner += " ]";
		returner += "\ttc = [";
		for (int i=0; i < no_reward_structs; i++) returner += " "+Double_To_String(this->transition_cost[i]);
		returner += " ]";
	}
	return returner;
}


//=============================================================================
//	Globals
//=============================================================================

//State space
int * state_variables  = NULL; //pointer to the state space array
int no_state_variables = 0; //size of the state space

//=============================================================================
//	Functions
//=============================================================================

/*
 *	If state_variables has been allocated previously, this function removes it
 *	from memory.
 */
void Deallocate_State_Space()
{
	if(state_variables != NULL)
	{
		delete[] state_variables;
		state_variables = NULL;
	}
	no_state_variables = 0;
}

/*
 *	Allocates memory for an integer array of size no_variables, this array is
 *	then pointed to by state_variables and no_state_variables is set to
 *	no_variables.
 *	
 *	throws an exception for out of memory
 */
void Allocate_State_Space(int no_variables)
{
	no_state_variables = no_variables;
	
	//Allocate state space
	state_variables = new int [no_state_variables];
	
	if(state_variables == NULL) 
	{
		Report_Error("Simulator engine ran out of memory when allocating state space.");
		throw "out of memory exception simstate.cc 001";
	}
	
	//Initially all values are undefined
	for(int i = 0; i < no_state_variables; i++)
	{
		state_variables[i] = UNDEFINED_INT;
	}
}

/*
 *	Sets state_variables[index] to value.
 *
 *	throws an exception if index is out of range
 */
void Set_State_Variable(int index, int value)
{
	if(index >= no_state_variables)
	{
		Report_Error("Unexpected Error: Attempt to update variable that is out of range.");
		throw "array access exception simstate.cc 002";
	}
	else
	{
		state_variables[index] = value;
	}
}

/*
 *	Copies the values pointed to by variables into state_variables.
 *
 *	throws an exception is variables is NULL.
 */
void Set_State_Space(int* variables)
{
	if(variables == NULL)
	{
		Report_Error("Unexpected Error: Attempt to update variables with null value.");
		throw "null pointer exception simstate.cc 003";
	}
	else
	{
		Copy_Int_Array(variables, state_variables, no_state_variables);
	}
}

/*
 *	Returns the value of state_variables[index].
 *
 *	throws an exception if index is out of range.
 */
int Get_State_Variable_Value(int index)
{
	if(index >= no_state_variables)
	{
		Report_Error("Unexpected Error: Attempt to access variable that is out of range.");
		throw "array access exception simstate.cc 004";
	}
	else
	{
		return state_variables[index];
	}
}

/*
 *	Returns the size of the state space, i.e. no_state_variables.
 */
int Get_Num_State_Variables()
{
	return no_state_variables;
}

//=============================================================================
//	Helper Functions
//=============================================================================

string State_Variables_To_String()
{
	string vars = "";
	for(int i = 0; i < no_state_variables; i++)
	{
		vars += Int_To_String(i) + "\t" + Int_To_String(state_variables[i]) + "\n";
	}
	return vars;
}

//==============================================================================
//      IO Functions
//==============================================================================

void Write_State_Space(int fd)
{   
	char buf_str[256];
    Write_Length_And_String("ss", fd);
    //write the number of variables
    write(fd, &no_state_variables, sizeof(int));

    //write each variable value
    for(int i = 0; i < no_state_variables; i++)
    {
        write(fd, &state_variables[i], sizeof(int));
    }

    //terminate state space output with null byte
    write(fd, "\0", 1);
}

void Read_State_Space(int fd)
{
	char str_buf[256];
    //cout <<"Attempting to read state space" << endl;
    int int_buf;
    //read state space header
    read(fd, &int_buf, sizeof(int));
    read(fd, &str_buf, int_buf+1);
    if(strcmp(str_buf, "ss") != 0)
    {
        throw "Error when importing binary file: state space header not found";
    }

    //read the number of variables and allocate state space
    read(fd, &int_buf, sizeof(int));
    Allocate_State_Space(int_buf);

    //cout << "there were " << int_buf << " variables." << endl;

    //read variable values

    for(int i = 0; i < no_state_variables; i++)
    {
        read(fd, &int_buf, sizeof(int));
        Set_State_Variable(i, int_buf);
        //cout << int_buf << ", ";
    }
    //cout<<endl;

    //read off null byte
    read(fd, str_buf, 1);
    if(strcmp(str_buf, "") != 0)
    {
        throw "Error when importing binary file: state space not terminated correctly";
    }
    //cout << "Successfully loaded state space" << endl;

    
}
