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
#include <string>
#include <time.h>
#include <vector>
#include "simengine.h"
#include "simstate.h"
#include "simmodel.h"
#include "simreasoning.h"
#include "simupdater.h"
#include "simpctl.h"
#include "simsampling.h"
#include "simpath.h"
#include "simformulae.h"
#include "simrandom.h"
#include "simutil.h"

using std::cout;
using std::endl;
using std::vector;

//==============================================================================
//	Description
//==============================================================================

//==============================================================================
//      Model Checking
//==============================================================================

vector<double> Engine_Do_Model_Check(int* property_indices, int no_properties,
                                     int no_iterations, int max_path)
{
    vector<double> results;

    Engine_Do_Sampling(no_iterations, max_path);

    for(int i = 0; i < no_properties; i++)
    {
        results.push_back(Engine_Get_Sampling_Result(property_indices[i]));
    }

    return results;

}


//==============================================================================
//	MODEL INITIALISATION
//==============================================================================

int Engine_Tidy_Up_Everything()
{
	//Sort out the random number generator
	Set_Generator(0);
	Seed_Generator_With_System_Clock(); //for now
	try
	{
		Deallocate_Label_Manager();
		Deallocate_Path();
		Deallocate_Sampling();
		Deallocate_PCTL_Core();
		Deallocate_Reasoning();
		Deallocate_Updater();
		Deallocate_Model();
		Deallocate_State_Space();
		
	}
	catch(char* str)
	{
		return ERROR;
	}
	return 0;
}

//==============================================================================
//	STATE SPACE METHODS
//==============================================================================

int Engine_Allocate_State_Space(int no_variables)
{
	try
	{
		Allocate_State_Space(no_variables);
	}
	catch(char* str)
	{
		return ERROR;
	}
	return 0;
}

void Engine_Define_Variable(int index, int value)
{
	try
	{
		Set_State_Variable(index, value);
	}
	catch(char* str)
	{
		cout << str << endl;
	}
}

int Engine_Get_No_Variables()
{
    return no_state_variables;
}


//==============================================================================
//	MODEL LOADING METHODS
//==============================================================================

int Engine_Allocate_Model(int type, int no_commands, int no_rew_structs, int *no_state_rewards, 
                          int *no_trans_rewards, int no_modules, int no_actions)
{
	try
	{
		Allocate_Model(type, no_commands, no_rew_structs, no_state_rewards, 
                               no_trans_rewards, no_modules, no_actions);
		Allocate_Reasoning();
		Allocate_Updater();
	}
	catch(char* str)
	{
		return ERROR;
	}
	return 0;
}

int Engine_Setup_Add_Transition(CCommand* comm)
{
	try
	{
		Add_Command_To_Model(comm);
	}
	catch(char* str)
	{
		return ERROR;
	}
	return 0;
}


int Engine_Setup_Add_State_Reward(int i, CStateReward* reward)
{
	try
	{
		Add_State_Reward_To_Model(i, reward);
	}
	catch(char* str)
	{
		return ERROR;
	}
	return 0;
}


int Engine_Setup_Add_Transition_Reward(int i, CTransitionReward* reward)
{
	try
	{
		Add_Transition_Reward_To_Model(i, reward);
	}
	catch(char* str)
	{
		return ERROR;
	}
	return 0;
}

//==============================================================================
//	PATH INITIALISATION AND SETUP METHODS
//==============================================================================


int Engine_Allocate_Path()
{
	try
	{
		Allocate_Path();
	}
	catch(char* str)
	{
		return ERROR;
	}
        return 0;
}



int Engine_Start_Path(int* initial_state)
{
	try
	{
                Copy_Int_Array(initial_state, state_variables, no_state_variables);
		Start_Path();
	}
	catch(char* str)
	{
		return ERROR;
	} 
        return 0;
}


//==============================================================================
//	PATH ACCESS METHODS
//==============================================================================


int Engine_Get_Path_Size()
{
	return Get_Path_Length();
}

int Engine_Get_Path_Data(int var_index, int state_index)
{
	return Get_Path_Data(var_index, state_index);   
}

double Engine_Get_Time_Spent_In_Path_State(int state_index)
{
	return Get_Time_Spent_In_Path_State(state_index);
}

double Engine_Get_Cumulative_Time_Spent_In_Path_State(int state_index)
{
	return Get_Cumulative_Time_Spent_In_Path_State(state_index);
}

double Engine_Get_State_Reward_Of_Path_State(int state_index, int i)
{
	return Get_State_Reward_Of_Path_State(state_index, i);
}

double Engine_Get_Transition_Reward_Of_Path_State(int state_index, int i)
{
	return Get_Transition_Reward_Of_Path_State(state_index, i);
}

double Engine_Get_Total_Transition_Reward_Of_Path_State(int state_index, int i)
{
	return Get_Total_Transition_Reward_Of_Path_State(state_index, i);
}

double Engine_Get_Total_State_Reward_Of_Path_State(int state_index, int i)
{
	return Get_Total_State_Reward_Of_Path_State(state_index, i);
}

double Engine_Get_Total_Path_Time()
{
	return Get_Total_Path_Time();
}

double Engine_Get_Total_Path_Reward(int i)
{
	return Get_Path_Cost(i);
}

double Engine_Get_Total_Transition_Reward(int i)
{
        return Get_Path_Transition_Cost(i);
}

double Engine_Get_Total_State_Reward(int i)
{
     return Get_Path_State_Cost(i);
}

bool Engine_Is_Path_Looping()
{
	return Is_Proven_Looping();
}

int Engine_Loop_Start()
{
	return Get_Start_Loop();
}

int Engine_Loop_End()
{
	return Get_End_Loop();
}


int Engine_getChosenIndexOfOldUpdate(int step)
{
	return Get_Chosen_Index_Of_Old_Updates(step);
}


//==============================================================================
//	UPDATE HANDLER UPDATE METHODS
//==============================================================================

int Engine_Make_Manual_Update(int index)
{
	try
	{
		Manual_Update(index);
	}
	catch(char* str)
	{
		return ERROR;
	}   
	return 0;
}

int Engine_Make_Manual_Update(int index, double time_in_state)
{
	try
	{
		Manual_Update(index, time_in_state);
	}
	catch(char* str)
	{
		return ERROR;
	}	 
	return 0;
}

int Engine_Do_Automatic_Choices(int n)
{
	return Engine_Do_Automatic_Choices(n, true);
}

int Engine_Do_Automatic_Choices(int n, bool detect)
{
	try
	{
		Automatic_Choices(n, detect);
	}
	catch(char* str)
	{
		return ERROR;
	}  
	return 0;
}

int Engine_Do_Backtrack(int step)
{
	try
	{
		Backtrack(step);
	}
	catch(char* str)
	{
		return ERROR;
	}   
	return 0;
	
}

int Engine_Do_Remove_Preceding_States(int step)
{
	try
	{
		Remove_Preceding_States(step);
	}
	catch(char* str)
	{
		return ERROR;
	}   
	return 0;
	
}

int Engine_Calculate_Old_Updates(int step)
{
	try
	{
		Calculate_Updates(Get_Path_State(step)->variables);
		Calculate_State_Reward(Get_Path_State(step)->variables);
	}
	catch(char* str)
	{
		return ERROR;
	}	
	return 0;
}

int Engine_Finished_With_Old_Updates()
{
	try
	{
		Calculate_Updates(state_variables);
		Calculate_State_Reward(state_variables);
	}
	catch(char* str)
	{
		return ERROR;
	}	 
	return 0;
}

//==============================================================================
//	UPDATE HANDLER ACCESS METHODS
//==============================================================================

int Engine_Get_No_Updates()
{
	return Get_No_Updates();
}

int Engine_Get_Action_Index_Of_Update(int update_index)
{
	return Get_Action_Index_Of_Update(update_index);
}

int Engine_Get_Module_Index_Of_Update(int update_index)
{
	return Get_Module_Of_Update(update_index);
}

 double  Engine_Get_Probability_Of_Update(int update_index)
{
	return Get_Probability_Of_Update(update_index);
}

int Engine_Get_No_Assignments_Of_Update(int update_index)
{
	return Get_Num_Assignments_Of_Update(update_index);
}

int Engine_Get_Assignment_Variable_Index_Of_Update
(int update_index, int assign_index)
{
	return Get_Assignment_Variable_Index_Of_Update(update_index, 
                                                       assign_index);
}

int Engine_Get_Assignment_Value_Of_Update(int update_index, int assign_index)
{
	return Get_Assignment_Value_Of_Update(update_index, assign_index);
}

int Engine_Get_Distribution_Index_Of_Update(int update_index)
{
	return Get_Distribution_Index_Of_Update(update_index);
}


//==============================================================================
//	PROPERTIES AND SAMPLING
//==============================================================================

int Engine_Allocate_PCTL_Manager()
{
	try
	{
		Allocate_PCTL_Core();
	}
	catch(char* str)
	{
		return ERROR;
	}
	return 0;
}


int Engine_Allocate_Sampling()
{
	try
	{
		Allocate_Sampling();
	}
	catch(char* str)
	{
		return ERROR;
	}
	return 0;
}


int Engine_Do_Sampling(int no_iterations, int max_path_length)
{
	try
	{
		Set_No_Iterations(no_iterations);
		Do_Sampling(max_path_length);
	}
	catch(int e)
	{
		return ERROR;
	}
	return 0;
}

void  Engine_Stop_Sampling()
{
	Stop_Sampling();
}

double Engine_Get_Sampling_Result(int index)
{
	return (double)Get_Sampling_Result(index);
}

int Engine_Get_Num_Reached_Max_Path(int index)
{
	return Get_Num_Reached_Max_Path(index);
}

//==============================================================================
//	STATE PROPOSITION METHODS
//==============================================================================

int Engine_Load_Proposition(CNormalExpression* expr)
{
	return Load_Proposition(expr);   
}

int Engine_Query_Proposition(int index)
{
	return Query_Proposition(index);	
}

int Engine_Query_Proposition(int index, int step)
{
	return Query_Proposition(index, step);
}

int Engine_Query_Is_Initial()
{
	return Query_Is_Initial();
}

int Engine_Query_Is_Initial(int step)
{
	return Query_Is_Initial(step);
}

int Engine_Query_Is_Deadlock()
{
	return Query_Is_Deadlock();
}

int Engine_Query_Is_Deadlock(int step)
{
	return Query_Is_Deadlock(step);
}

//==============================================================================
//	PATH FORMULA METHODS
//==============================================================================


int Engine_Find_Path_Formula_Index(CPathFormula* path)
{
	return Index_Of_Path_Formula(path);
}

int Engine_Query_Path_Formula(int index)
{
	return Query_Path_Formula(index);
}

double Engine_Query_Path_Formula_Numeric(int index)
{
	return Query_Path_Formula_Numeric(index);
}


//==============================================================================
//	EXPRESSION CREATION METHODS
//==============================================================================


CExpression*  Engine_Create_Integer_Var(int var_index)
{
	int* location = &(state_variables[var_index]);
	return new CIntegerVar(location, var_index);
}

CExpression* Engine_createBooleanVar(int var_index)
{
	int* location = &(state_variables[var_index]);
	return new CBooleanVar(location, var_index);
}

CExpression* Engine_Create_Double(double value)
{
	return new CDouble(value);
}

CExpression* Engine_Create_Integer(int value)
{
	return new CInteger(value);
}

CExpression* Engine_Create_Boolean(bool value)
{
	return new CBoolean(value);
}

CExpression* Engine_Create_Ceil(CExpression* expr)
{
	return new CCeil(expr);
}

CExpression* Engine_Create_Floor(CExpression* expr)
{
	return new CFloor(expr);
}

CExpression* Engine_Create_Normal_Pow(CNormalExpression* base, CNormalExpression* exp)
{
	return new CNormalPow(base, exp);
}

CExpression* Engine_Create_Real_Pow(CExpression* base, CExpression* exp)
{
	return new CRealPow(base, exp);
}

CExpression* Engine_Create_Mod(CNormalExpression* left, CNormalExpression* right)
{
	return new CMod(left, right);
}

CExpression*  Engine_Create_Not(CNormalExpression* expr)
{
	return new CNot(expr);
}

CExpression* Engine_Create_And(CNormalExpression** exprs, int no_expressions)
{
	return new CAnd(exprs, no_expressions);
}

CExpression* Engine_Create_Or(CNormalExpression** exprs, int no_expressions)
{
	return new COr(exprs, no_expressions);
}

CExpression* Engine_Create_Normal_Max(CNormalExpression** exprs, int no_expressions)
{
	return new CNormalMax(exprs, no_expressions);
}

CExpression* Engine_Create_Normal_Min(CNormalExpression** exprs, int no_expressions)
{
	return new CNormalMin(exprs, no_expressions);
}

CExpression*  Engine_Create_Real_Max(CExpression** exprs, int no_expressions)
{
	return new CRealMax(exprs, no_expressions);
}

CExpression* Engine_Create_Real_Min(CExpression** exprs, int no_expressions)
{
	return new CRealMin(exprs, no_expressions);
}

CExpression* Engine_Create_Normal_Times(CNormalExpression* lexpr, CNormalExpression* rexpr)
{
	return new CNormalTimes(lexpr, rexpr);
}

CExpression* Engine_Create_Normal_Plus(CNormalExpression* lexpr, CNormalExpression* rexpr)
{
	return new CNormalPlus(lexpr, rexpr);
}

CExpression* Engine_Create_Normal_Minus(CNormalExpression * lexpr, CNormalExpression * rexpr)
{
	return new CNormalMinus(lexpr, rexpr);
}

CExpression* Engine_Create_Real_Times(CExpression* lexpr, CExpression* rexpr)
{
	return new CRealTimes(lexpr, rexpr);
}

CExpression* Engine_Create_Divide(CExpression* lexpr, CExpression* rexpr)
{
	return new CDivide(lexpr, rexpr);
}

CExpression* Engine_Create_Real_Plus(CExpression* lexpr, CExpression* rexpr)
{
	return new CRealPlus(lexpr, rexpr);
}

CExpression* Engine_Create_Real_Minus(CExpression* lexpr, CExpression* rexpr)
{
	return new CRealMinus(lexpr, rexpr);
}

CExpression* Engine_Create_Real_Ite
(CNormalExpression* condition, CExpression* trueExpr, CExpression* falseExpr)
{
	return new CRealIte(condition, trueExpr, falseExpr);
}

CExpression* Engine_createIte
(CNormalExpression* condition, CExpression* trueExpr, CExpression* falseExpr)
{
	return new CIte(condition, trueExpr, falseExpr);
}

CExpression* Engine_Create_Normal_Equals(CNormalExpression* lexpr, CNormalExpression* rexpr)
{
	return new CNormalEquals(lexpr, rexpr);
}

CExpression* Engine_Create_Real_Equals(CExpression* lexpr, CExpression* rexpr)
{
	return new CRealEquals(lexpr, rexpr);
}

CExpression* Engine_Create_Normal_Not_Equals(CNormalExpression* lexpr, CNormalExpression* rexpr)
{
	return new CNormalNotEquals(lexpr, rexpr);
}

CExpression* Engine_Create_Real_Not_Equals(CExpression* lexpr, CExpression* rexpr)
{
	return new CRealNotEquals(lexpr, rexpr);
}

CExpression* Engine_Create_Normal_Less_Than(CNormalExpression* lexpr, CNormalExpression* rexpr)
{
	return new CNormalLessThan(lexpr, rexpr);
}

CExpression*  Engine_Create_Real_Less_Than(CExpression* lexpr, CExpression* rexpr)
{
	return new CRealLessThan(lexpr, rexpr);
}

CExpression* Engine_Create_Normal_Greater_Than(CNormalExpression* lexpr, CNormalExpression* rexpr)
{
	return new CNormalGreaterThan(lexpr, rexpr);
}

CExpression* Engine_Create_Real_Greater_Than(CExpression* lexpr, CExpression* rexpr)
{
	return new CRealGreaterThan(lexpr, rexpr);
}

CExpression* Engine_Create_Normal_Less_Than_Equal(CNormalExpression* lexpr, CNormalExpression* rexpr)
{
	return new CNormalLessThanEqual(lexpr, rexpr);
}

CExpression* Engine_Create_Real_Less_Than_Equal(CExpression* lexpr, CExpression* rexpr)
{
	return new CRealLessThanEqual(lexpr, rexpr);
}

CExpression* Engine_Create_Normal_Greater_Than_Equal
(CNormalExpression* lexpr, CNormalExpression* rexpr)
{
	return new CNormalGreaterThanEqual(lexpr, rexpr);
}

CExpression* Engine_Create_Real_Greater_Than_Equal
(CExpression* lexpr, CExpression* rexpr)
{
	return new CRealGreaterThanEqual(lexpr, rexpr);
}

//==============================================================================
//	PCTL FORMULAE CREATION METHODS
//==============================================================================


CPathFormula* Engine_Load_Pctl_Bounded_Until
(CExpression* expr1, CExpression* expr2, double lower_bound, double upper_bound)
{
	CBoundedUntil* bu = new CBoundedUntil(expr1, expr2, lower_bound, upper_bound);

	Register_Path_Formula(bu);

	return bu;
}

CPathFormula* Engine_Load_Pctl_Until(CExpression* expr1, CExpression* expr2)
{
	CUntil* u = new CUntil(expr1, expr2);

	Register_Path_Formula(u);

	return u;
}

CPathFormula* Engine_Load_Pctl_Next(CExpression* expr)
{
	CNext* nx = new CNext(expr);

	Register_Path_Formula(nx);

	return nx;
}

CPathFormula*  Engine_load_Pctl_Reachability(int rsi, CExpression* expr)
{
	CRewardReachability* reach = new CRewardReachability(rsi, expr);

	Register_Path_Formula(reach);

	return reach;
}

CPathFormula* Engine_Load_Pctl_Cumulative(int rsi, double time)
{
	CRewardCumulative* cumul = new CRewardCumulative(rsi, time);

	Register_Path_Formula(cumul);

	return cumul;
}

CPathFormula* Engine_Load_Pctl_Instantanious(int rsi, double time)
{
	CRewardInstantanious* instant = new CRewardInstantanious(rsi, time);

	Register_Path_Formula(instant);

	return instant;
}

int Engine_Load_Prob_Question(CPathFormula* formula)
{
	CProbEqualsQuestion* sampler = new CProbEqualsQuestion(formula);
	return Register_Sample_Holder(sampler);
}

int Engine_Load_Reward_Question(CRewardFormula* formula)
{
	CRewardEqualsQuestion* sampler = new CRewardEqualsQuestion(formula);
	return Register_Sample_Holder(sampler);
}

//==============================================================================
//	TRANSITION TABLE CREATION METHODS
//==============================================================================

CCommand* Engine_Create_Command
  (CNormalExpression* guard, int action_index, int module_index, int num_updates)
{
	return new CCommand(guard, action_index, module_index, num_updates);
}

CUpdate* Engine_Add_Update(CCommand* command, CRealExpression* prob, int no_assignments)
{
	
	CUpdate * update = new CUpdate(prob, no_assignments);

	command->Add_Update(update);
	
	return update;
}

CAssignment* Engine_Add_Assignment
  (CUpdate* update, int var_index, CNormalExpression* rhs)
{
	
	CAssignment* assign = new CAssignment(var_index, rhs);
	update->Add_Assignment(assign);

	return assign;
}


//==============================================================================
//	REWARDS TABLE CREATION METHODS
//==============================================================================

CStateReward* Engine_Create_State_Reward(CExpression* guard, CExpression* reward)
{
	return  new CStateReward(guard, reward);
}

CTransitionReward* Engine_Create_Transition_Reward
  (int action_index, CExpression* guard, CExpression* reward)
{
	return new CTransitionReward(action_index, guard, reward);
}

//==============================================================================
//	UTILITY
//==============================================================================


string Engine_Model_To_String()
{
	return Model_To_String();
}

string  Engine_Path_To_String()
{
	return "";
}

void  Engine_Print_Current_Updates()
{
        Print_Updates();
}


//==============================================================================
//	ERROR HANDLING
//==============================================================================

string Engine_Get_Last_Error_Message()
{
	return Get_Last_Error();
}

