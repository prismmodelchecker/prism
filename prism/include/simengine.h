//==============================================================================
//	
//	Copyright (c) 2004-2005, Andrew Hinton
//	
//	This file is part of PRISM and 
//	Andrew Hinton's M60 Software Project
//	Supervisors: David Parker and Marta Kwiatkowska
//	The University of Birmingham
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
#ifndef _Included_simulator_Engineheader
#define _Included_simulator_Engineheader

#include <stdio.h>
#include <string>
#include <time.h>
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

//=============================================================================
//	Description
//=============================================================================



//=============================================================================
//	Functions
//=============================================================================

//==============================================================================
//	MODEL INITIALISATION
//==============================================================================

int Engine_Tidy_Up_Everything();

//==============================================================================
//	STATE SPACE METHODS
//==============================================================================

int Engine_Allocate_State_Space(int no_variables);

void Engine_Define_Variable(int index, int value);

//==============================================================================
//	MODEL LOADING METHODS
//==============================================================================

int Engine_Allocate_Model(int type, int no_commands, int no_reward_structs, int *no_state_rewards, 
                          int *no_trans_rewards, int no_modules, int no_actions);

int Engine_Setup_Add_Transition(CCommand* comm);

int Engine_Setup_Add_State_Reward(int i, CStateReward* reward);

int Engine_Setup_Add_Transition_Reward(int i, CTransitionReward* reward);

//==============================================================================
//	PATH INITIALISATION AND SETUP METHODS
//==============================================================================

int Engine_Allocate_Path();

int Engine_Start_Path();

//==============================================================================
//	PATH ACCESS METHODS
//==============================================================================

int Engine_Get_Path_Size();

int Engine_Get_Path_Data(int var_index, int state_index);

double Engine_Get_Time_Spent_In_Path_State(int state_index);

double Engine_Get_Cumulative_Time_Spent_In_Path_State(int state_index);

double Engine_Get_State_Reward_Of_Path_State(int state_index);

double Engine_Get_Transition_Reward_Of_Path_State(int state_index);

double Engine_Get_Total_State_Reward_Of_Path_State(int state_index);

double Engine_Get_Total_Transition_Reward_Of_Path_State(int state_index);

double Engine_Get_Total_Path_Time();

double Engine_Get_Total_Path_Reward();

double Engine_Get_Total_Transition_Reward();

double Engine_Get_Total_State_Reward();

bool Engine_Is_Path_Looping();

int Engine_Loop_Start();

int Engine_Loop_End();

int Engine_getChosenIndexOfOldUpdate(int step);

//==============================================================================
//	UPDATE HANDLER UPDATE METHODS
//==============================================================================

int Engine_Make_Manual_Update(int index);

int Engine_Make_Manual_Update(int index, double time_in_state);

int Engine_Do_Automatic_Choices(int n);
int Engine_Do_Automatic_Choices(int n, bool detect);

int Engine_Do_Backtrack(int step);

int Engine_Do_Remove_Preceding_States(int step);

int Engine_Calculate_Old_Updates(int step);

int Engine_Finished_With_Old_Updates();

//==============================================================================
//	UPDATE HANDLER ACCESS METHODS
//==============================================================================

int Engine_Get_No_Updates();

int Engine_Get_Action_Index_Of_Update(int update_index);

int Engine_Get_Module_Index_Of_Update(int update_index);

double Engine_Get_Probability_Of_Update(int update_index);

int Engine_Get_No_Assignments_Of_Update(int update_index);

int Engine_Get_Assignment_Variable_Index_Of_Update
(int update_index, int assign_index);

int Engine_Get_Assignment_Value_Of_Update(int update_index, int assign_index);

int Engine_Get_Distribution_Index_Of_Update(int update_index);

//==============================================================================
//	PROPERTIES AND SAMPLING
//==============================================================================

int Engine_Allocate_PCTL_Manager();

int Engine_Allocate_Sampling();

int Engine_Do_Sampling(int no_iterations, int max_path_length);

void  Engine_Stop_Sampling();

double Engine_Get_Sampling_Result(int index);

int Engine_Get_Num_Reached_Max_Path(int index);

//==============================================================================
//	STATE PROPOSITION METHODS
//==============================================================================

int Engine_Load_Proposition(CNormalExpression* expr);

int Engine_Query_Proposition(int index);

int Engine_Query_Proposition(int index, int step);

int Engine_Query_Is_Initial();

int Engine_Query_Is_Initial(int step);

int Engine_Query_Is_Deadlock();

int Engine_Query_Is_Deadlock(int step);

//==============================================================================
//	PATH FORMULA METHODS
//==============================================================================


int Engine_Find_Path_Formula_Index(CPathFormula* path);

int Engine_Query_Path_Formula(int index);

double Engine_Query_Path_Formula_Numeric(int index);


//==============================================================================
//	EXPRESSION CREATION METHODS
//==============================================================================


CExpression*  Engine_Create_Integer_Var(int var_index);

CExpression* Engine_createBooleanVar(int var_index);

CExpression* Engine_Create_Double(double value);

CExpression* Engine_Create_Integer(int value);

CExpression* Engine_Create_Boolean(bool value);

CExpression* Engine_Create_Ceil(CExpression* expr);

CExpression* Engine_Create_Floor(CExpression* expr);

CExpression* Engine_Create_Normal_Pow(CNormalExpression* base, CNormalExpression* exp);

CExpression* Engine_Create_Real_Pow(CExpression* base, CExpression* exp);

CExpression* Engine_Create_Mod(CNormalExpression* left, CNormalExpression* right);

CExpression*  Engine_Create_Not(CNormalExpression* expr);

CExpression* Engine_Create_And(CNormalExpression** exprs, int no_expressions);

CExpression* Engine_Create_Or(CNormalExpression** exprs, int no_expressions);

CExpression* Engine_Create_Normal_Max(CNormalExpression** exprs, int no_expressions);

CExpression* Engine_Create_Normal_Min(CNormalExpression** exprs, int no_expressions);

CExpression*  Engine_Create_Real_Max(CExpression** exprs, int no_expressions);

CExpression* Engine_Create_Real_Min(CExpression** exprs, int no_expressions);

CExpression* Engine_Create_Normal_Times(CNormalExpression* lexpr, CNormalExpression* rexpr);

CExpression* Engine_Create_Normal_Plus(CNormalExpression* lexpr, CNormalExpression* rexpr);

CExpression* Engine_Create_Normal_Minus(CNormalExpression * lexpr, CNormalExpression * rexpr);

CExpression* Engine_Create_Real_Times(CExpression* lexpr, CExpression* rexpr);

CExpression* Engine_Create_Divide(CExpression* lexpr, CExpression* rexpr);

CExpression* Engine_Create_Real_Plus(CExpression* lexpr, CExpression* rexpr);

CExpression* Engine_Create_Real_Minus(CExpression* lexpr, CExpression* rexpr);

CExpression* Engine_Create_Real_Ite
(CNormalExpression* condition, CExpression* trueExpr, CExpression* falseExpr);

CExpression* Engine_createIte
(CNormalExpression* condition, CExpression* trueExpr, CExpression* falseExpr);

CExpression* Engine_Create_Normal_Equals(CNormalExpression* lexpr, CNormalExpression* rexpr);

CExpression* Engine_Create_Real_Equals(CExpression* lexpr, CExpression* rexpr);

CExpression* Engine_Create_Normal_Not_Equals(CNormalExpression* lexpr, CNormalExpression* rexpr);

CExpression* Engine_Create_Real_Not_Equals(CExpression* lexpr, CExpression* rexpr);

CExpression* Engine_Create_Normal_Less_Than(CNormalExpression* lexpr, CNormalExpression* rexpr);

CExpression*  Engine_Create_Real_Less_Than(CExpression* lexpr, CExpression* rexpr);

CExpression* Engine_Create_Normal_Greater_Than(CNormalExpression* lexpr, CNormalExpression* rexpr);

CExpression* Engine_Create_Real_Greater_Than(CExpression* lexpr, CExpression* rexpr);

CExpression* Engine_Create_Normal_Less_Than_Equal(CNormalExpression* lexpr, CNormalExpression* rexpr);

CExpression* Engine_Create_Real_Less_Than_Equal(CExpression* lexpr, CExpression* rexpr);

CExpression* Engine_Create_Normal_Greater_Than_Equal
(CNormalExpression* lexpr, CNormalExpression* rexpr);

CExpression* Engine_Create_Real_Greater_Than_Equal
(CExpression* lexpr, CExpression* rexpr);

//==============================================================================
//	PCTL FORMULAE CREATION METHODS
//==============================================================================


CPathFormula* Engine_Load_Pctl_Bounded_Until
(CExpression* expr1, CExpression* expr2, double lower_Bound, double upper_bound);

CPathFormula* Engine_Load_Pctl_Until(CExpression* expr1, CExpression* expr2);

CPathFormula* Engine_Load_Pctl_Next(CExpression* expr);

CPathFormula*  Engine_load_Pctl_Reachability(CExpression* expr);

CPathFormula* Engine_Load_Pctl_Cumulative(double time);

CPathFormula* Engine_Load_Pctl_Instantanious(double time);

int Engine_Load_Prob_Question(CPathFormula* formula);

int Engine_Load_Reward_Question(CRewardFormula* formula);

//==============================================================================
//	TRANSITION TABLE CREATION METHODS
//==============================================================================

CCommand* Engine_Create_Command
  (CNormalExpression* guard, int action_index, int module_index, int num_updates);

CUpdate* Engine_Add_Update(CCommand* command, CRealExpression* prob, int no_assignments);

CAssignment* Engine_Add_Assignment
  (CUpdate* update, int var_index, CNormalExpression* rhs);


//==============================================================================
//	REWARDS TABLE CREATION METHODS
//==============================================================================

CStateReward* Engine_Create_State_Reward(CExpression* guard, CExpression* reward);

CTransitionReward* Engine_Create_Transition_Reward
  (int action_index, CExpression* guard, CExpression* reward);

//==============================================================================
//	UTILITY
//==============================================================================


string Engine_Model_To_String();

string  Engine_Path_To_String();

void  Engine_Print_Current_Updates();


//==============================================================================
//	ERROR HANDLING
//==============================================================================

string Engine_Get_Last_Error_Message();



#endif
