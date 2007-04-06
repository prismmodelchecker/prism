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

#ifndef _Included_simulator_Samplingheader
#define _Included_simulator_Samplingheader

#include "simpctl.h"
#include "simupdater.h"
#include "simstate.h"
#include <vector>

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
//      Constants
//=============================================================================

const int HOLDER_PROB = 0;
const int HOLDER_REWARD = 1;

//=============================================================================
//      Classes
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
class CSampleHolder
{	
	public:
		double cumulative_value; //i.e. for probabilities the sum of 0s and 1s for rewards the sum of the rewards
		int no_samples;
		int no_reach_max;

		/*
		 *	Constructor for a CSampleHolder object.
		 */
		CSampleHolder();
		
		/*
		 *	Destructor for a CSampleHolder object.
		 */
		virtual ~CSampleHolder()
		{
		}

		/*
		 *	Adds the value of sample to the cumulative_value and increments the no_samples.
		 */
		void Sample(double sample);

		/*
		 *	A sample where the maximum path length has been reached.
		 */
		virtual void Sample_Maximum_Path_Reached();

		/*
		 *	Reset for another sampling algorithm.
		 */
		void Reset();

		/*
		 *	Give the number of times that the maximum path length was reached for
		 *	the current sampling run.
		 */
		int Get_Number_Reached_Maximum_Path();

		/*
		 *	Should be overriden to return true when the sampling is complete.
		 */
		virtual bool Done()=0;

		/*
		 *	Should be overridden to return the result of the sampling.
		 */
		virtual double Get_Result()=0;

		/*
		 *	Should be overridden to return the path formula being sampled.
		 */
		virtual CPathFormula* Get_Path_Formula()=0;

		/*
		 *	Should be overridden to set the number of sampling iterations.
		 */
                virtual void Set_No_Iterations(int no_iterations)=0;

                virtual void Write_Holder(int fd)=0;


};

/*
 *	CProbEqualsQuestion
 *	===================
 *	
 *	The CProbEqualsQuestion class overrides CSampleHolder to provide functionality
 *	for properties of the form P=? [...].  These have a link to a registered
 *	CPathFormula, which is the path formula to be sampled.
 *
 */
class CProbEqualsQuestion : public CSampleHolder
{
	private:
		CPathFormula * formula;
		int required_runs;
	public:

		/*
		 *	Constructor for a CProbEqualsQuestion object.
		 */
		CProbEqualsQuestion(CPathFormula* formula);
	
		/*
		 *	Destructor for a CProbEqualsQuestion object.
		 */
		~CProbEqualsQuestion();

		/*
		 *	Returns true if the number of samples taken is >= the amount required.
		 */
		bool Done();

		/*
		 *	Returns a pointer to the path formula
		 */
		CPathFormula* Get_Path_Formula();

		/*
		 *	Returns the mean value result(cumulative/no_samples)
		 */
		double Get_Result();

		/*
		 *	Method to set the number of iterations required to approximately verify this property.
		 */
                void Set_No_Iterations(int no_iterations);

                void Write_Holder(int fd);
              
};

/*
 *	CRewardEqualsQuestion
 *	=====================
 *	
 *	The CRewardEqualsQuestion class overrides CSampleHolder to provide functionality
 *	for properties of the form R=? [...].  These have a link to a registered
 *	CPathFormula, which is the reward formula to be sampled.
 *
 */
class CRewardEqualsQuestion : public CSampleHolder
{
	private:
		CPathFormula * formula;
		int required_runs;
	public:
		/*
		 *	Constructor for a CRewardEqualsQuestion object.
		 */
		CRewardEqualsQuestion(CPathFormula* formula);
	
		/*
		 *	Destructor for a CRewardEqualsQuestion object.
		 */
		~CRewardEqualsQuestion();

		/*
		 *	Returns true if the number of samples taken >= the number of samples required. Or
		 *	if the cumulative_value is determined to be infinite... if which case the answer
		 *	will always be infinite.
		 */
		bool Done();

		/*
		 *	Returns a pointer to the reward formula 
		 */
		CPathFormula* Get_Path_Formula();

		/*
		 *	Returns the mean value result(cumulative/no_samples)
		 */
		double Get_Result();

		/*
		 *	Method to set the number of iterations required to approximately verify this property.
		 */
                void Set_No_Iterations(int no_iterations);

                void Write_Holder(int fd);
};

class CSamplingLoopDetectionHandler : public CLoopDetectionHandler
{
private:
	int next_loop_index;
	vector<CPathState*> loop_path;
public:
	CSamplingLoopDetectionHandler();
	~CSamplingLoopDetectionHandler();
	void Start_New_Deterministic_Path();
	void Notify_State(int* state_variables);
	void Reset();
	int Path_Size();
	int* Path_At(int i);
	void Notify_Deterministic_Path_End();
};

//=============================================================================
//	Globals
//=============================================================================

extern bool should_stop_sampling;


//=============================================================================
//	Functions
//=============================================================================

/*
 *	Deallocates memory for the sampling interface.  All registered_sample_holders
 *	are removed from memory.
 */
void Deallocate_Sampling();

/*
 *	Assigns memory to store sample_holders
 */
void Allocate_Sampling();

/*
 *	Adds a CSampleHolder to the registered_sample_holders vector.
 */
int Register_Sample_Holder(CSampleHolder* sample_holder);

/*
 *	Returns true if each member of registered_sample_holders has done all
 *	of the necessary computation to get a result.
 */
bool All_Done_Sampling();

/*
 *	Returns true if for each path formula of each registered_sample_holder,
 *	the answer is known for the current execution path.
 */
bool All_PCTL_Answers_Known();

/*
 *	Performs a sample according to the calculation on each CPathFormula of the
 *	CSampleHolders in registered_sample_holders.  Each CPathFormula is reset
 *	for the next path.
 */
void Do_A_Sample();

/*
 *	Force the sampling algorithm to stop.
 */
void Stop_Sampling();

/*
 *	Print out the sampling results to the command line.
 */
void Print_Sampling_Results();

/*
 *	Get the calculated result of the CSampleHolder at the given index.
 */
double Get_Sampling_Result(int index);

/*
 *	Get the number of times the CSampleHolder at the given index had to be evaluated
 *	to the maximum path length in order to make a sample.
 */
int Get_Num_Reached_Max_Path(int index);

/*
 *	Get the total number of times any CSampleHolder at the given index had to be evaluated
 *	to the maximum path length in order to make a sample.
 */
int Get_Total_Num_Reached_Max_Path();

/*
 *	For each CSampleHolder in registered_sample_holders, this function sets the
 *	number of iterations for the sampling algorithm.
 */
void Set_No_Iterations(int no_iterations_param);

/*
 *	The sampling algorithm.
 */
void Do_Sampling(int path_length);

//==============================================================================
//      IO Functions
//==============================================================================

void Write_Sampling(int fd);

void Write_Sampling_Results(int fd);

void Read_Sampling(int fd);

CSampleHolder* Read_Sample_Holder(int fd);

#endif
