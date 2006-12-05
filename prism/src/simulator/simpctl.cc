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

#include <stdio.h>
#include "simpctl.h"
#include "simutil.h"
#include "simstate.h"
#include "simmodel.h"
#include "simreasoning.h"
#include "simupdater.h"
#include <math.h>
#include <iostream>
#include <string>
#include <vector>

using std::cout;
using std::endl;
using std::string;
using std::vector;

//=============================================================================
//	Description
//=============================================================================

/*
 *	This interface provides the means of reasoning about executions through
 *	models.  It stores a collection of CPathFormula or CRewardFormula objects.
 *	These objects should be notified about any updates in the state, so that 
 *	they can reason about the path as a whole.  These updates should be notified
 *	in order and each object can be reset to look at a new path.
 */

//TODO integrate path formulae.cc stuff


//==============================================================================
//Class Definitions
//==============================================================================

/*
 *	CPathFormula
 *	===========
 *
 *	The CPathFormula class is used to represent any form of reasoning about 
 *	paths.  Primarily, this is used for (bounded/unbounded) untils and
 *	next operators, but is also overridden for specific support for rewards
 *	based operators.  
 *
 *	The important method is the Notify_State() method which should be overridden
 *	by each type of path operator to deal with an addition to the execution
 *	path.  When the answer for a particular path is known, answer_known is
 *	set to true and the result is stored in answer.  At this point no more
 *	evaluation of the path property is necessary.
 */

/*
 *	Constructor for CPathFormula object.
 */
CPathFormula::CPathFormula()
{
	answer_known = false;
	answer = false;
	negate = false;
}


/*
 *	Set whether or not the value of this formula should be negated
 */
void CPathFormula::Set_Negate(bool b)
{
	negate = b;
}

/*
 *	Access to the calculated answer for this CPathFormula object.
 */
bool CPathFormula::Get_Answer()
{
	return (negate)?(!answer):(answer);
}

/*
 *	Access to a real number version of the answer for this CPathFormula
 *	object.
 */
double CPathFormula::Get_Answer_Double()
{
	if(Get_Answer()) return 1.0;
	else return 0.0;
}

/*
 *	Resets this CPathFormula object for use in future calcuations.
 */
void CPathFormula::Reset()
{		
	answer_known = false;
	answer = false;
}

/*
 *	This method should be overridden by all reward subclasses to return
 *	true.
 */
bool CPathFormula::Is_Reward()
{
	return false;
}


/*
 *	CRewardFormula
 *	==============
 *
 *	The CRewardFormula class overrides CPathFormula to provide specific
 *	support for reward operators.  Essentially it provides an extra
 *	variables answer_double which is used to store the calculated reward
 *	for an execution path.  This can be queried by Get_Answer_Double().
 *
 */

/*
 *	Constructor for a CRewardFormula object.
 */
CRewardFormula::CRewardFormula() : CPathFormula()
{
	answer_double = 0.0;
}

/*
 *	Returns the calculated reward for this CRewardFormula.
 */
double CRewardFormula::Get_Answer_Double()
{
	return answer_double;
}

/*
 *	Returns the reward structure index for this CRewardFormula.
 */
int CRewardFormula::Get_Reward_Struct_Index()
{
	return reward_struct_index;
}

/*
 *	Resets this CRewardFormula object for use in future calcuations.
 */
void CRewardFormula::Reset()
{
	answer_known = false;
	answer_double = 0.0;
	answer = false;
}

/*
 *	Overridden to state that this object should be treated as a reward
 *	formulae.
 */
bool CRewardFormula::Is_Reward()
{
	return true;
}


/*
 *	CBoundedUntil
 *	==============
 *
 *	The CBoundedUntil class is used to store and deal with path formulae of the form:
 *	[ leftExpression U[lower_bound, upper_bound] rightExpression ].
 *	i.e. leftExpression must hold true until rightExpression is satisfied within
 *	the lower and upper time bounds.  For CTMCs, the time bounds refer to real time
 *	but for DTMCs, this bound refers to time steps.
 *
 */

/*
 *	Constructor for a CBoundedUntil object.
 */
CBoundedUntil::CBoundedUntil(CExpression* leftExpression, CExpression* rightExpression, double lower_bound, double upper_bound) : CPathFormula()
{
	this->leftExpression = leftExpression;
	this->rightExpression = rightExpression;
	this->upper_bound = upper_bound;
	this->lower_bound = lower_bound;
	this->counter = -1;
	this->time_so_far = 0.0;
}

/*
 *	Destructor for a CBoundedUntil object. Deletes the left and right expression
 *	structures.
 */
CBoundedUntil::~CBoundedUntil()
{
	if(leftExpression != NULL)
		delete leftExpression;
	if(rightExpression != NULL)
		delete rightExpression;
}

/*
 *	This method should be called for each update to the current state.  This
 *	method determines whether the answer can be determined for this update
 *	and if, so calculates the answer and puts it in the answer member variable.
 */
void CBoundedUntil::Notify_State(CPathState* last_state, int* current_state)
{
	counter++;
	
	if(model_type == STOCHASTIC)
	{
		
		if(counter > 0)
		{
			double time_last_state = last_state->time_spent_in_state;
			time_so_far += time_last_state; 

			if(time_so_far > upper_bound)
			{
				//must take into account the possibility of missing
				//lower bound

				//could have missed out evaluation of rightExpression
				if(time_so_far-time_last_state <= lower_bound)
				{
					int* vars = last_state->variables;
					if(rightExpression->Evaluate(vars))
					{
						answer_known = true;
						answer = true;
					}
					else
					{
						answer_known = true;
						answer = false;
					}
				}
				else
				{
					answer_known = true;
					answer = false;
				}
			}
			else if(time_so_far <= lower_bound)
			{
				if(!leftExpression->Evaluate(current_state))
				{
					answer_known = true;
					answer = false;
				}
			}
			else
			{
				if(rightExpression->Evaluate(current_state))
				{
					answer_known = true;
					answer = true;
				}
				else if(!leftExpression->Evaluate(current_state))
				{
					answer_known = true;
					answer = false;
				}
			}
		}
		else
		{
			if(lower_bound == 0.0)
			{
				if(rightExpression->Evaluate(current_state))
				{
					answer_known = true;
					answer = true;
				}
			}
			else
			{
				if(!leftExpression->Evaluate(current_state))
				{
					answer_known = true;
					answer = false;
				}
			}
		}

	}
	else
	{
		if(!answer_known)
		{
			if(counter > (int)upper_bound)
			{
				answer_known = true;
				answer = false;
			}
			else if(counter < (int)lower_bound)
			{
				if(!leftExpression->Evaluate(current_state))
				{
					answer_known = true;
					answer = false;
				}
			}
			else
			{
				if(rightExpression->Evaluate(current_state))
				{
					answer_known = true;
					answer = true;
				}
				else if(!leftExpression->Evaluate(current_state))
				{
					answer_known = true;
					answer = false;
				}
			}
		}
	}
}


/*
 *	Determines whether the answer is already known for this path formula.  This
 *	will either be if the answer has been proven, or if the answer has not been
 *	determined but the path is looping.
 */
bool CBoundedUntil::Is_Answer_Known(CLoopDetectionHandler* loop_detection)
{
	return answer_known || loop_detection->Is_Proven_Looping() || loop_detection->Is_Deadlock();
}

/*
 *	Returns a string representation of this CBoundedUntil object.
 */
string CBoundedUntil::To_String()
{
	return leftExpression->To_String() + " U<= "+ Double_To_String(upper_bound)+ " " + rightExpression->To_String();
}

/*
 *	Resets the counter and time_so_far variables, and calls the superclass Reset method.
 */
void CBoundedUntil::Reset()
{
	CPathFormula::Reset();
	counter = -1;
	time_so_far = 0.0;
}

void CBoundedUntil::Write_Formula(int fd)
{
    //write formula type
    int form_type = FORMULA_BOUNDED_UNTIL;
    write(fd, &form_type, sizeof(int));

    //lower bound
    write(fd, &lower_bound, sizeof(double));
    
    //upper bound
    write(fd, &upper_bound, sizeof(double));

    //Left Expression
    leftExpression->Write_Expression(fd);

    //Right Expression
    rightExpression->Write_Expression(fd);

    //write null byte
    write(fd, "\0", 1);
}

/*
 *	CUntil
 *	======
 *
 *	The CUntil class is used to store and deal with path formulae of the form:
 *	[ leftExpression U rightExpression ].
 *	i.e. leftExpression must hold true until rightExpression is satisfied.
 *
 */

/*
 *	Constructor for a CUntil object.
 */
CUntil::CUntil(CExpression* leftExpression, CExpression* rightExpression) : CPathFormula()
{
	this->leftExpression = leftExpression;
	this->rightExpression = rightExpression;
}

/*
 *	Destructor for a CBoundedUntil object. Deletes the left and right expression
 *	structures.
 */
CUntil::~CUntil()
{
	if(leftExpression != NULL)
		delete leftExpression;
	if(rightExpression != NULL)
		delete rightExpression;
}

/*
 *	This method should be called for each update to the current state.  This
 *	method determines whether the answer can be determined for this update
 *	and if, so calculates the answer and puts it in the answer member variable.
 */
void CUntil::Notify_State(CPathState* last_state, int* current_state)
{
	if(!answer_known)
	{
		if(rightExpression->Evaluate(current_state))
		{
			answer_known = true;
			answer = true;
		}
		else if(!leftExpression->Evaluate(current_state))
		{
			answer_known = true;
			answer = false;
		}
	}
}

/*
 *	Determines whether the answer is already known for this path formula.  This
 *	will either be if the answer has been proven, or if the answer has not been
 *	determined but the path is looping.
 */
bool CUntil::Is_Answer_Known(CLoopDetectionHandler* loop_detection)
{
	return answer_known || loop_detection->Is_Proven_Looping() || loop_detection->Is_Deadlock();
}

/*
 *	Returns a string representation of this CUntil object.
 */
string CUntil::To_String()
{
	return leftExpression->To_String() + " U "+ rightExpression->To_String();
}

void CUntil::Write_Formula(int fd)
{
    //write formula type
    int form_type = FORMULA_UNTIL;
    write(fd, &form_type, sizeof(int));

    //Left Expression
    leftExpression->Write_Expression(fd);

    //Right Expression
    rightExpression->Write_Expression(fd);

    //write null byte
    write(fd, "\0", 1);
}

/*
 *	CNext
 *	=====
 *
 *	The CNext class is used to store and deal with path formulae of the form:
 *	[ X expression ].
 *	i.e. expression must hold true in the state following the original state.
 *
 */

/*
 *	Constructor for a CNext object.
 */
CNext::CNext(CExpression* expression) : CPathFormula()
{
	this->expression = expression;
}

/*
 *	Destructor for a CNextUntil object. Deletes the expression
 *	structure.
 */
CNext::~CNext()
{
	if(expression != NULL)
		delete expression;
}

/*
 *	This method should be called for each update to the current state.  This
 *	method determines whether the answer can be determined for this update
 *	and if, so calculates the answer and puts it in the answer member variable.
 */
void CNext::Notify_State(CPathState* last_state, int* current_state)
{
	counter++;
	if(!answer_known)
	{
		if(counter == 1) // if in 'Next' state
		{
			answer_known = true;
			if(expression->Evaluate(current_state))
				answer = true;
			else
				answer = false;
		}
	}
}

/*
 *	Returns a string representation of this CNext object.
 */
string CNext::To_String()
{
	return "X "+expression->To_String();
}

/*
 *	Determines whether the answer is already known for this path formula.  This
 *	will either be if the answer has been proven, or if the answer has not been
 *	determined but the path is looping.
 */
bool CNext::Is_Answer_Known(CLoopDetectionHandler* loop_detection)
{
	return answer_known || loop_detection->Is_Proven_Looping() || loop_detection->Is_Deadlock();
}

/*
 *	Resets the counter for this CNext object and calls the superclass Reset method.
 */
void CNext::Reset() 
{
	CPathFormula::Reset();
	counter = -1;
}

void CNext::Write_Formula(int fd)
{
    //write formula type
    int form_type = FORMULA_NEXT;
    write(fd, &form_type, sizeof(int));

    //Write Expression
    expression->Write_Expression(fd);

    //write null byte
    write(fd, "\0", 1);
}

/*
 *	CRewardCumulative
 *	=================
 *
 *	The CRewardCumulative class is used to store and deal with rewards formulae 
 *	of the form:
 *	[ C<=time ].
 *	i.e. What is the reward accumulated before the time bound
 *
 */

/*
 *	Constructor for a CRewardCumulative object.
 */
CRewardCumulative::CRewardCumulative(int rsi, double time) : CRewardFormula()
{
	this->reward_struct_index = rsi;
	this->time = time;
	this->counter = -1;
	time_so_far = 0;
	answer_double = 0.0;
}

/*
 *	Destructor for a CRewardCumulative object.
 */
CRewardCumulative::~CRewardCumulative()
{
}

/*
 *	This method should be called for each update to the current state.  This
 *	method determines whether the answer can be determined for this update
 *	and if, so calculates the answer and puts it in the answer member variable.
 */
void CRewardCumulative::Notify_State(CPathState* last_state, int* current_state)
{
	counter++;
	if(model_type == STOCHASTIC)
	{
		if(counter > 0)
		{
			double time_last_state = last_state->time_spent_in_state;
			time_so_far += time_last_state; 
			
			if(time_so_far > time)
			{
				//time occured in last state
				answer_known = true;
				if (reward_struct_index >= 0 && reward_struct_index < no_reward_structs) {
					answer_double = 
						last_state->cumulative_state_cost[reward_struct_index] + last_state->cumulative_transition_cost[reward_struct_index] - 
							last_state->transition_cost[reward_struct_index] + 
							last_state->state_instant_cost[reward_struct_index] * (time - time_so_far);
				} else {
					answer_double = 0.0;
				}
			}
			else if(time_so_far == time) //unlikely, but just in case
			{
				answer_known = true;
				if (reward_struct_index >= 0 && reward_struct_index < no_reward_structs) {
					answer_double = 
						last_state->cumulative_state_cost[reward_struct_index] + last_state->cumulative_transition_cost[reward_struct_index];
				} else {
					answer_double = 0.0;
				}
			}
		}
	}
	else
	{
		if(counter > 0)
		{
			if(counter >= time)
			{
				answer_known = true;
				if (reward_struct_index >= 0 && reward_struct_index < no_reward_structs) {
					answer_double = last_state->cumulative_state_cost[reward_struct_index] + last_state->cumulative_transition_cost[reward_struct_index];
				} else {
					answer_double = 0.0;
				}
			}
		}
	}
}

/*
 *	Returns a string representation of this CRewardCumulative object.
 */
string CRewardCumulative::To_String()
{
	return "[ C <= "+Double_To_String(time)+" ]";
}

/*
 *	Resets the counter, time_so_far and answer_double and calls the superclass
 *	Reset() method.
 */	
void CRewardCumulative::Reset()
{
	CRewardFormula::Reset();
	counter = -1;
	time_so_far = 0;
	answer_double =0.0;
}

/*
 *	Determines whether the answer is already known for this path formula.  This
 *	will *only* be if the answer has been proven, *not* if the path is looping.
 */
bool CRewardCumulative::Is_Answer_Known(CLoopDetectionHandler* loop_detection)
{
	return answer_known;
}

void CRewardCumulative::Write_Formula(int fd)
{
    //write formula type
    int form_type = FORMULA_CUMULATIVE;
    write(fd, &form_type, sizeof(int));

    //rsi
    write(fd, &reward_struct_index, sizeof(int));

    //time
    write(fd, &time, sizeof(double));

    //write null byte
    write(fd, "\0", 1);
}


/*
 *	CRewardInstantanious
 *	====================
 *
 *	The CRewardCumulative class is used to store and deal with rewards formulae 
 *	of the form:
 *	[ I time ].
 *	i.e. What is the instantaneous state reward at the given time.
 *
 */

/*
 *	Constructor for a CRewardInstantaneous object...and yes it does set the
 *	time_so_far variable to 0.0 now... ooops
 */
CRewardInstantanious::CRewardInstantanious(int rsi, double time) : CRewardFormula()
{
	this->reward_struct_index = rsi;
	this->time = time;
	this->counter = -1;
	time_so_far = 0;
	answer_double = 0.0;
}

/*
 *	Destructor for a CRewardInstantaneous object.
 */
CRewardInstantanious::~CRewardInstantanious()
{

}

/*
 *	This method should be called for each update to the current state.  This
 *	method determines whether the answer can be determined for this update
 *	and if, so calculates the answer and puts it in the answer member variable.
 */
void CRewardInstantanious::Notify_State(CPathState* last_state, int* current_state)
{
	counter++;
	if(model_type == STOCHASTIC)
	{
		if(counter > 0)
		{
			double time_last_state = last_state->time_spent_in_state;
			time_so_far += time_last_state;
			
			if(time_so_far > time)
			{
				//time occured in last state
				answer_known = true;
				if (reward_struct_index >= 0 && reward_struct_index < no_reward_structs) {
					answer_double = last_state->state_instant_cost[reward_struct_index];
				} else {
					answer_double = 0.0;
				}
			}
			else if(time_so_far == time) //unlikely, but just in case
			{
				answer_known = true;
				if (reward_struct_index >= 0 && reward_struct_index < no_reward_structs) {
					answer_double = Get_State_Reward(reward_struct_index);
				} else {
					answer_double = 0.0;
				}
			}
		}
		else
		{
			if(time == 0)
			{
				answer_known = true;
				if (reward_struct_index >= 0 && reward_struct_index < no_reward_structs) {
					answer_double = Get_State_Reward(reward_struct_index);
				} else {
					answer_double = 0.0;
				}
			}
		}
	}
	else
	{
		if(counter > 0)
		{
			if(counter >= time)
			{
				answer_known = true;
				if (reward_struct_index >= 0 && reward_struct_index < no_reward_structs) {
					answer_double = Get_State_Reward(reward_struct_index);
				} else {
					answer_double = 0.0;
				}
			}
		}
		else
		{
			if(time == 0)
			{
				answer_known = true;
				if (reward_struct_index >= 0 && reward_struct_index < no_reward_structs) {
					answer_double = Get_State_Reward(reward_struct_index);
				} else {
					answer_double = 0.0;
				}
			}
		}
	}
}


/*
 *	Returns a string representation of this CRewardInstantaneous object.
 */
string CRewardInstantanious::To_String()
{
	return "[ I ="+Double_To_String(time)+" ] answer_known = "+Bool_To_String(answer_known); 
}

/*
 *	Resets the counter, time_so_far and answer_double variables and calls the superclass
 *	Reset() method.
 */
void CRewardInstantanious::Reset()
{
	CRewardFormula::Reset();
	counter = -1;
	time_so_far = 0;
	answer_double = 0.0;
}

/*
 *	Determines whether the answer is already known for this path formula.  This
 *	will *only* be if the answer has been proven, *not* if the path is looping.
 */
bool CRewardInstantanious::Is_Answer_Known(CLoopDetectionHandler* loop_detection)
{
	return answer_known;
}

void CRewardInstantanious::Write_Formula(int fd)
{
    //write formula type
    int form_type = FORMULA_INSTANTANEOUS;
    write(fd, &form_type, sizeof(int));

    //rsi
    write(fd, &reward_struct_index, sizeof(int));

    //time
    write(fd, &time, sizeof(double));

    //write null byte
    write(fd, "\0", 1);
}

/*
 *	CRewardReachability
 *	===================
 *
 *	The CRewardReachability class is used to store and deal with rewards formulae 
 *	of the form:
 *	[ F expression ].
 *	i.e. What is the accumulated reward when expression is satisfied.
 *
 */

/*
 *	Constructor for a CRewardReachability object.
 */
CRewardReachability::CRewardReachability(int rsi, CExpression* expression) : CRewardFormula()
{
	this->reward_struct_index = rsi;
	this->expression = expression;
}

/*
 *	Destructor for a CRewardReachability object.
 */
CRewardReachability::~CRewardReachability()
{
	if(expression != NULL)
		delete expression;
}

/*
 *	This method should be called for each update to the current state.  This
 *	method determines whether the answer can be determined for this update
 *	and if, so calculates the answer and puts it in the answer member variable.
 */
void CRewardReachability::Notify_State(CPathState* last_state, int* current_state)
{
	
	
	if(expression->Evaluate(current_state))
	{
		answer_known = true;
		if (reward_struct_index >= 0 && reward_struct_index < no_reward_structs) {
			answer_double = last_state->cumulative_state_cost[reward_struct_index] + last_state->cumulative_transition_cost[reward_struct_index];//Get_Path_Cost_No_Path();									
		} else {
			answer_double = 0.0;
		}
	}

}

/*
 *	Determines whether the answer is already known for this path formula.  This
 *	will either be if the answer has been proven, or if the path is looping.
 *	If the path is looping, this method also sets the answer_double to undefined
 *	because the reward is infinite.
 */
bool CRewardReachability::Is_Answer_Known(CLoopDetectionHandler* loop_detection)
{
	if (!answer_known) if(loop_detection->Is_Proven_Looping() || loop_detection->Is_Deadlock()) answer_double = UNDEFINED_DOUBLE;
	return answer_known || loop_detection->Is_Proven_Looping() || loop_detection->Is_Deadlock();
}

/*
 *	Returns a string representation of this CRewardReachability object.
 */
string CRewardReachability::To_String()
{
	return "[ F "+expression->To_String()+" ]";
}

void CRewardReachability::Write_Formula(int fd)
{
    //write formula type
    int form_type = FORMULA_REACHABILITY;
    write(fd, &form_type, sizeof(int));

    //rsi
    write(fd, &reward_struct_index, sizeof(int));

    //Write Expression
    expression->Write_Expression(fd);

    //write null byte
    write(fd, "\0", 1);
}



//==============================================================================
//	Local Data
//==============================================================================

vector<CPathFormula*> registered_path_formulae;	 //all path and reward formulae
int no_path_formulae; //size of registered_path_formulae


//==============================================================================
//Function Definitions
//==============================================================================

/**
*	This function removes all registered path formulae
*	from memory.
*/
void Deallocate_PCTL_Core()
{
	//cout << "Deallocate_PCTL_Core 1" << endl;
	if(&registered_path_formulae !=NULL)
	{
		//cout << "Deallocate_PCTL_Core 2" << endl;
		for(int i = 0; i < no_path_formulae; i++)
		{
			//cout << "Deallocate_PCTL_Core 3" << endl;
			//cout << registered_path_formulae[i]->To_String();
			if(registered_path_formulae[i] != NULL)
				delete registered_path_formulae[i];
			registered_path_formulae[i] = NULL;
		}
		//cout << "Deallocate_PCTL_Core 4" << endl;
		registered_path_formulae.clear();
	}
	//cout << "Deallocate_PCTL_Core 5" << endl;
	no_path_formulae = 0;
	//cout << "Deallocate_PCTL_Core 6" << endl;
}

/*
 *	Allocates storage for path formulae to be registered.
 */
void Allocate_PCTL_Core()
{
	registered_path_formulae.reserve(10);
	no_path_formulae = 0;
}

/*
 *	Adds the given path formula to registered_path_formula.
 */
int Register_Path_Formula(CPathFormula* formula)
{
	//cout << "Registering formula " << formula->To_String() << endl;
	registered_path_formulae.push_back(formula);
	no_path_formulae++;
	return no_path_formulae-1;
}

/*
 *	Returns the index of the formula parameter in registered_path_formula
 */
int Index_Of_Path_Formula(CPathFormula* formula)
{
	for(int i = 0; i < registered_path_formulae.size(); i++)
	{
		if(registered_path_formulae[i] == formula)
			return i;
	}
	return -1;
}

/*
 *	Calls the Notify_State method of each path or reward formula in
 *	registered_path_formulae.
 */
void Notify_Path_Formulae(CPathState* last_state, int* current_state, CLoopDetectionHandler* loop_detection)
{
	for(int i = 0; i< no_path_formulae; i++)
	{
		if(!registered_path_formulae[i]->Is_Answer_Known(loop_detection))
			registered_path_formulae[i]->Notify_State(last_state, current_state);
	}
}

//TODO move this to path PCTL manager

/*
void Recalculate_Path_Formulae()
{
	//first rest each path formula
	Reset_Path_Formulae();

	for(int i = 0; i < no_path_formulae; i++)
	{
		for(int j = 0; j<=thePath->currentIndex; j++)
		{
			registered_path_formulae[i]->Notify_State(j);
		}
	}
}*/

/*
 *	Resets each path or reward formula in registered_path_formulae for
 *	new calculations.
 */
void Reset_Path_Formulae()
{
	for(int i = 0; i< no_path_formulae; i++)
		registered_path_formulae[i]->Reset();

}

/*
 *	Outputs the contents of registered_path_formulae to the command line.
 */
void Print_Formulae()
{
	cout << endl;
	cout << "Status of loaded PCTL path formulae:" << endl << endl;
	cout << "Known\tAnswer\tFormulae" << endl;
	cout << "===================================================================" << endl;
	for(int i = 0; i < no_path_formulae; i++)
	{
		CPathFormula* form = registered_path_formulae[i];

		/*cout << form->Is_Answer_Known() << "\t";
		if(form->Is_Answer_Known())
			cout << form->Get_Answer() << "\t";
		else
			cout << "\t";  */
		cout << form->To_String() << endl;
	}
	cout << endl;
}

/*
 *	Method for direct querying of a registered path formula.
 *	Returns -1 if the answer is not known (or an error)
 *	Returns 0 if the answer is known as false
 *	Returns 1 if the answer is known as true
 *	Returns 2 if the answer is numeric.
 */
int Query_Path_Formula(int index)
{
	
	if(index >= no_path_formulae) return -1;
	
	if(registered_path_formulae[index] == NULL) return -1;
	
	if(!registered_path_formulae[index]->answer_known) return -1;
	
	//check for numeric
	if(registered_path_formulae[index]->Is_Reward()) return 2;
	if (registered_path_formulae[index]->Get_Answer()) return 1;
	else return 0;
}

/*
 *	If the path formula at the index is numeric, this returns the answer
 */
double Query_Path_Formula_Numeric(int index)
{
	if(index >= no_path_formulae) return -1;
	return registered_path_formulae[index]->Get_Answer_Double();
}

CPathFormula* Get_Path_Formula(int index)
{
    return registered_path_formulae[index];
}

//==============================================================================
//      IO Functions
//==============================================================================

void Write_Pctl_Manager(int fd)
{
    //model identifier
    Write_Length_And_String("pctl", fd);
    
    //write no formulae
    write(fd, &no_path_formulae, sizeof(int));

    //Formulae table
    //==============
    
    for(int i = 0; i < no_path_formulae; i++)
    {
        registered_path_formulae[i]->Write_Formula(fd);
    }

    //write null byte
    write(fd, "\0", 1);
}

void Read_Pctl(int fd)
{
	char buf_str[256];
    int int_buf;
    //read model header
    read(fd, &int_buf, sizeof(int));
    read(fd, &buf_str, int_buf+1);
    if(strcmp(buf_str, "pctl") != 0)
    {
        throw "Error when importing binary file: pctl header not found";
    }

    read(fd, &int_buf, sizeof(int));

    for(int i = 0 ; i < int_buf; i++)
    {
        CPathFormula* formula = Read_Path_Formula(fd);
        Register_Path_Formula(formula);
    }

    //read off null byte
    read(fd, &buf_str, 1);
    if(strcmp(buf_str, "") != 0)
    {
        throw "Error when importing binary file: state space not terminated correctly";
    }
}

CPathFormula* Read_Path_Formula(int fd)
{
    int int_buf;
	char buf_str[256];
    //read formula type
    read(fd, &int_buf, sizeof(int));

    CExpression* expr;
    CExpression* expr1;
    CExpression* expr2;
    double value1, value2;
	int rsi;
    
    switch(int_buf)
    {
        case FORMULA_BOUNDED_UNTIL:
            read(fd, &value1, sizeof(double));
            read(fd, &value2, sizeof(double));
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &buf_str, 1);
            if(strcmp(buf_str, "") != 0)
            {
                throw "Error when importing binary file: state space not terminated correctly";
            }
            return new CBoundedUntil(expr1, expr2, value1, value2);
        case FORMULA_UNTIL:
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &buf_str, 1);
            if(strcmp(buf_str, "") != 0)
            {
                throw "Error when importing binary file: state space not terminated correctly";
            }
            return new CUntil(expr1, expr2);
        case FORMULA_NEXT:
            expr= Read_Expression(fd);
            //read off null byte
            read(fd, &buf_str, 1);
            if(strcmp(buf_str, "") != 0)
            {
                throw "Error when importing binary file: state space not terminated correctly";
            }
            return new CNext(expr);
        case FORMULA_CUMULATIVE:
            read(fd, &rsi, sizeof(int));
            read(fd, &value1, sizeof(double));
            //read off null byte
            read(fd, &buf_str, 1);
            if(strcmp(buf_str, "") != 0)
            {
                throw "Error when importing binary file: state space not terminated correctly";
            }
            return new CRewardCumulative(rsi, value1);
        case FORMULA_REACHABILITY:
            read(fd, &rsi, sizeof(int));
            expr= Read_Expression(fd);
            //read off null byte
            read(fd, &buf_str, 1);
            if(strcmp(buf_str, "") != 0)
            {
                throw "Error when importing binary file: state space not terminated correctly";
            }
            return new CRewardReachability(rsi, expr);
        case FORMULA_INSTANTANEOUS:
            read(fd, &rsi, sizeof(int));
            read(fd, &value1, sizeof(double));
            //read off null byte
            read(fd, &buf_str, 1);
            if(strcmp(buf_str, "") != 0)
            {
                throw "Error when importing binary file: state space not terminated correctly";
            }
            return new CRewardInstantanious(rsi, value1);
       default:
            throw "unexpected formula type when loading pctl formula";
    }
}

