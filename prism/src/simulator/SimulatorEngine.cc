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

#include "SimulatorEngine.h"
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
#include "simiohandler.h"
#include "simlog.h"

using std::string;
using std::cout;
using std::endl;

//==============================================================================
//	PRISM LOG
//==============================================================================

JNIEXPORT void JNICALL Java_simulator_SimulatorEngine_Set_1Main_1Log
(JNIEnv *env, jclass cls, jobject log)
{
	Sim_Set_Main_Log(env, log);
}


//==============================================================================
//	MODEL INITIALISATION
//==============================================================================

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_tidyUpEverything
(JNIEnv *env, jclass cls)
{
	//cout << "Deallocating everything..." << endl;
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
	catch(string str)
	{
		//cout << "ERROR ERROR ERROR" << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}
	return 0;
}

//==============================================================================
//	STATE SPACE METHODS
//==============================================================================

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_allocateStateSpace
  (JNIEnv *env, jclass cls, jint noVariables)
{
	try
	{
		Allocate_State_Space((int)noVariables);
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}
	return 0;
}

JNIEXPORT void JNICALL Java_simulator_SimulatorEngine_defineVariable
(JNIEnv *env, jclass cls, jint index, jint value)
{
	try
	{
		Set_State_Variable((int)index, (int)value);
	}
	catch(string str)
	{
		cout << str << endl;
	}
}


//==============================================================================
//	MODEL LOADING METHODS
//==============================================================================

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_allocateModel
  (JNIEnv *env, jclass cls, jint type, jint noCommands, jint noRewardStructs, jintArray noStateRewards, 
  jintArray noTransitionRewards, jint noModules, jint noActions)
{
	try
	{
		jint *nsr = env->GetIntArrayElements(noStateRewards, 0);
		jint *ntr = env->GetIntArrayElements(noTransitionRewards, 0);
		Allocate_Model((int)type, (int)noCommands, (int)noRewardStructs, (int*)nsr, (int*)ntr,
			(int)noModules, (int)noActions);
		env->ReleaseIntArrayElements(noStateRewards, nsr, 0);
		env->ReleaseIntArrayElements(noTransitionRewards, ntr, 0);
		
		Allocate_Reasoning();
		Allocate_Updater();
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}
	return 0;
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_setupAddTransition
(JNIEnv *env, jclass cls, jint commPointer)
{
	try
	{
		Add_Command_To_Model((CCommand*)(commPointer));
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}
	return 0;
}


JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_setupAddStateReward
(JNIEnv *env, jclass cls, jint i, jint rewardPointer)
{
	try
	{
		Add_State_Reward_To_Model((int)i, (CStateReward*)(rewardPointer));
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}
	return 0;
}


JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_setupAddTransitionReward
(JNIEnv *env, jclass cls, jint i, jint rewardPointer)
{
	try
	{
		Add_Transition_Reward_To_Model((int)i, (CTransitionReward*)(rewardPointer));
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}
	return 0;
}

//==============================================================================
//	PATH INITIALISATION AND SETUP METHODS
//==============================================================================


JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_allocatePath
  (JNIEnv *env, jclass cls)
{
	try
	{
		Allocate_Path();
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}
}



JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_startPath
(JNIEnv *env, jclass cls)
{
	try
	{
		Start_Path();

	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}  
}


//==============================================================================
//	PATH ACCESS METHODS
//==============================================================================


JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_getPathSize
(JNIEnv *env, jclass cls)
{
	return Get_Path_Length();
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_getPathData
(JNIEnv *env, jclass cls, jint varIndex, jint stateIndex)
{
	return Get_Path_Data(varIndex, stateIndex);   
}

JNIEXPORT jdouble JNICALL Java_simulator_SimulatorEngine_getTimeSpentInPathState
(JNIEnv *env, jclass cls, jint stateIndex)
{
	return Get_Time_Spent_In_Path_State(stateIndex);
}

JNIEXPORT jdouble JNICALL Java_simulator_SimulatorEngine_getCumulativeTimeSpentInPathState
(JNIEnv *env, jclass cls, jint stateIndex)
{
	return Get_Cumulative_Time_Spent_In_Path_State(stateIndex);
}

JNIEXPORT jdouble JNICALL Java_simulator_SimulatorEngine_getStateRewardOfPathState
(JNIEnv *env, jclass cls, jint stateIndex, jint i)
{
	return Get_State_Reward_Of_Path_State(stateIndex, i);
}

JNIEXPORT jdouble JNICALL Java_simulator_SimulatorEngine_getTransitionRewardOfPathState
(JNIEnv *env, jclass cls, jint stateIndex, jint i)
{
	return Get_Transition_Reward_Of_Path_State(stateIndex, i);
}

JNIEXPORT jdouble JNICALL Java_simulator_SimulatorEngine_getTotalStateRewardOfPathState
(JNIEnv *env, jclass cls, jint stateIndex, jint i)
{
	return Get_Total_State_Reward_Of_Path_State(stateIndex, i);
}

JNIEXPORT jdouble JNICALL Java_simulator_SimulatorEngine_getTotalTransitionRewardOfPathState
(JNIEnv *env, jclass cls, jint stateIndex, jint i)
{
	return Get_Total_Transition_Reward_Of_Path_State(stateIndex, i);
}

JNIEXPORT jdouble JNICALL Java_simulator_SimulatorEngine_getTotalPathTime
(JNIEnv *env, jclass cls)
{
	return Get_Total_Path_Time();
}

JNIEXPORT jdouble JNICALL Java_simulator_SimulatorEngine_getTotalPathReward
(JNIEnv *env, jclass cls, jint i)
{
	return Get_Path_Cost(i);
}

JNIEXPORT jdouble JNICALL Java_simulator_SimulatorEngine_getTotalTransitionReward
(JNIEnv *env, jclass cls, jint i)
{
     return Get_Path_Transition_Cost(i);
}

JNIEXPORT jdouble JNICALL Java_simulator_SimulatorEngine_getTotalStateReward
(JNIEnv *env, jclass cls, jint i)
{
     return Get_Path_State_Cost(i);
}

JNIEXPORT jboolean JNICALL Java_simulator_SimulatorEngine_isPathLooping
(JNIEnv *env, jclass cls)
{
	return Is_Proven_Looping();
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loopStart
(JNIEnv *env, jclass cls)
{
	return Get_Start_Loop();
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loopEnd
(JNIEnv *env, jclass cls)
{
	return Get_End_Loop();
}


JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_getChosenIndexOfOldUpdate
(JNIEnv *env, jclass cls, jint step)
{
	return Get_Chosen_Index_Of_Old_Updates((int) step);
}


//==============================================================================
//	UPDATE HANDLER UPDATE METHODS
//==============================================================================

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_makeManualUpdate__I
(JNIEnv *env, jclass cls, jint index)
{
	try
	{
		Manual_Update(index);
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}   
	return 0;
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_makeManualUpdate__ID
(JNIEnv *env, jclass cls, jint index, jdouble time_in_state)
{
	try
	{
		Manual_Update(index, time_in_state);
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}	 
	return 0;
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_doAutomaticChoices__IZ
  (JNIEnv *, jclass, jint n, jboolean detect)
{
	try
	{
		Automatic_Choices((int)n, (bool)detect);
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}  
	return 0;
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_doAutomaticChoices__DZ
  (JNIEnv *, jclass, jdouble time, jboolean detect)
{
	try
	{
		Automatic_Choices((double)time, (bool)detect);
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}  
	return 0;
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_doBacktrack__I
(JNIEnv *env, jclass cls, jint step)
{
	try
	{
		Backtrack((int)step);
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}   
	return 0;
	
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_doBacktrack__D
(JNIEnv *env, jclass cls, jdouble time)
{
	try
	{
		Backtrack((double)time);
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}   
	return 0;
	
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_doRemovePrecedingStates
(JNIEnv *env, jclass cls, jint step)
{
	try
	{
		Remove_Preceding_States(step);
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}   
	return 0;
	
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_calculateOldUpdates
  (JNIEnv *env, jclass cls, jint step)
{
	try
	{
		//cout << "doing calculation of old updates" << endl;
		if(no_state_variables > 0 )
		{
			Calculate_Updates(Get_Path_State(step)->variables);
			Calculate_State_Reward(Get_Path_State(step)->variables);
		}
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}	
	return 0;
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_finishedWithOldUpdates
  (JNIEnv *env, jclass step)
{
	try
	{
		if(no_state_variables > 0) //safeguard, in case the state space is not loaded.
		{
			Calculate_Updates(state_variables);
			Calculate_State_Reward(state_variables);
		}
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}	 
	return 0;
}

//==============================================================================
//	UPDATE HANDLER ACCESS METHODS
//==============================================================================

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_getNumUpdates
(JNIEnv *env, jclass cls)
{
	return Get_No_Updates();
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_getActionIndexOfUpdate
(JNIEnv *env, jclass cls, jint updateIndex)
{
	return Get_Action_Index_Of_Update((int)updateIndex);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_getModuleIndexOfUpdate
(JNIEnv *env, jclass cls, jint updateIndex)
{
	return Get_Module_Of_Update((int)updateIndex);
}

JNIEXPORT jdouble JNICALL Java_simulator_SimulatorEngine_getProbabilityOfUpdate
(JNIEnv *env, jclass cls, jint updateIndex)
{
	return Get_Probability_Of_Update((int)updateIndex);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_getNumAssignmentsOfUpdate
(JNIEnv *env, jclass cls, jint updateIndex)
{
	return Get_Num_Assignments_Of_Update(updateIndex);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_getAssignmentVariableIndexOfUpdate
(JNIEnv *env, jclass cls, jint updateIndex, jint assignmentIndex)
{
	return Get_Assignment_Variable_Index_Of_Update(updateIndex, assignmentIndex);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_getAssignmentValueOfUpdate
(JNIEnv *env, jclass cls, jint updateIndex, jint assignmentIndex)
{
	return Get_Assignment_Value_Of_Update(updateIndex, assignmentIndex);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_getDistributionIndexOfUpdate
(JNIEnv *env, jclass cls, jint updateIndex)
{
	return Get_Distribution_Index_Of_Update(updateIndex);
}


//==============================================================================
//	PROPERTIES AND SAMPLING
//==============================================================================

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_allocatePCTLManager
  (JNIEnv *env, jclass cls)
{
	try
	{
		Allocate_PCTL_Core();
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}
	return 0;
}


JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_allocateSampling
  (JNIEnv *env, jclass cls)
{
	try
	{
		Allocate_Sampling();
	}
	catch(string str)
	{
		cout << str << endl;
		return simulator_SimulatorEngine_ERROR;
	}
	return 0;
}


JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_exportBinary
(JNIEnv *env, jclass cls, jstring filename)
{
    const char *str = env->GetStringUTFChars(filename, 0);
    Export_Engine_To_Binary_File(str);
    env->ReleaseStringUTFChars(filename, str);
    //env->ReleaseStringChars(filename, (const char*)filename8);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_doSampling
(JNIEnv *env, jclass cls,  jint noIterations, jint maxPathLength)
{
	try {
		Set_No_Iterations((int)noIterations);
		        
		Sim_Enable_Main_Log_For_Current_Thread(env);
		
		Do_Sampling((int)maxPathLength);
		
		Sim_Disable_Main_Log();
	}
	catch(int e)
	{
		return simulator_SimulatorEngine_ERROR;
	}
	return 0;
}

JNIEXPORT void JNICALL Java_simulator_SimulatorEngine_stopSampling
(JNIEnv *env, jclass cls)
{
	Stop_Sampling();
}

JNIEXPORT jdouble JNICALL Java_simulator_SimulatorEngine_getSamplingResult
(JNIEnv *env, jclass cls, jint index)
{
	return (jdouble)Get_Sampling_Result((int)index);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_getNumReachedMaxPath
(JNIEnv *env, jclass cls, jint index)
{
	return (jint)Get_Num_Reached_Max_Path((int)index);
}

//==============================================================================
//	STATE PROPOSITION METHODS
//==============================================================================

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loadProposition
  (JNIEnv *env, jclass cls, jint exprPointer)
{
	CNormalExpression* expr = (CNormalExpression*)exprPointer;
	return Load_Proposition(expr);   
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_queryProposition__I
  (JNIEnv *env, jclass cls, jint index)
{
	return Query_Proposition((int)index);	
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_queryProposition__II
  (JNIEnv *env, jclass cls, jint index, jint step)
{
	return Query_Proposition((int)index, (int)step);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_queryIsInitial__
  (JNIEnv *env, jclass cls)
{
	return Query_Is_Initial();
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_queryIsInitial__I
  (JNIEnv *env, jclass cls, jint step)
{
	return Query_Is_Initial((int)step);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_queryIsDeadlock__
  (JNIEnv *env, jclass cls)
{
	return Query_Is_Deadlock();
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_queryIsDeadlock__I
  (JNIEnv *env, jclass cls, jint step)
{
	return Query_Is_Deadlock((int)step);
}

//==============================================================================
//	PATH FORMULA METHODS
//==============================================================================


JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_findPathFormulaIndex
  (JNIEnv *env, jclass cls, jint pathPointer)
{
	return Index_Of_Path_Formula((CPathFormula*)pathPointer);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_queryPathFormula
  (JNIEnv *env, jclass cls, jint index)
{
	return Query_Path_Formula((int)index);
}

JNIEXPORT jdouble JNICALL Java_simulator_SimulatorEngine_queryPathFormulaNumeric
  (JNIEnv *env, jclass cls, jint index)
{
	return Query_Path_Formula_Numeric((int)index);
}


//==============================================================================
//	EXPRESSION CREATION METHODS
//==============================================================================

//can be found in simexpressionbuilder.cc


//==============================================================================
//	PCTL FORMULAE CREATION METHODS
//==============================================================================

//can be found in simpctlbuilder.cc


//==============================================================================
//	TRANSITION TABLE CREATION METHODS
//==============================================================================

//can be found in simtransitionbuilder.cc


//==============================================================================
//	REWARDS TABLE CREATION METHODS
//==============================================================================

//can be found in simrewardsbuilder.cc


//==============================================================================
//	UTILITY
//==============================================================================


JNIEXPORT jstring JNICALL Java_simulator_SimulatorEngine_modelToString
(JNIEnv *env, jclass cls)
{
	std::string str = Model_To_String();

	return env->NewStringUTF(str.c_str());
}

JNIEXPORT jstring JNICALL Java_simulator_SimulatorEngine_pathToString
(JNIEnv *env, jclass cls)
{
	/*std::string str = Path_To_String();

	return env->NewStringUTF(str.c_str());*/
}

JNIEXPORT void JNICALL Java_simulator_SimulatorEngine_printCurrentUpdates
(JNIEnv *env, jclass cls)
{
	//TODO put this in
	//Print_Updates();
}


//==============================================================================
//	ERROR HANDLING
//==============================================================================

JNIEXPORT jstring JNICALL Java_simulator_SimulatorEngine_getLastErrorMessage
(JNIEnv *env, jclass cls)
{
	std::string str = Get_Last_Error();

	return env->NewStringUTF(str.c_str());
}

