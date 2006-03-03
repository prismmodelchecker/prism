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

#ifndef _Included_simulator_Pctlheader
#define _Included_simulator_Pctlheader

#include "simexpression.h"
#include "simstate.h"
#include "simupdater.h"
#include <string>
#include <iostream>

using std::string;
using std::cout;
using std::endl;


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

//=============================================================================
//      Constants
//=============================================================================

const int FORMULA_BOUNDED_UNTIL = 0;
const int FORMULA_UNTIL         = 1;
const int FORMULA_NEXT          = 2;
const int FORMULA_CUMULATIVE    = 3;
const int FORMULA_REACHABILITY  = 4;
const int FORMULA_INSTANTANEOUS = 5;


//=============================================================================
//Class definitions
//=============================================================================

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
class CPathFormula
{
	protected:
		bool answer;

	public:
		bool answer_known;
		/*
		 *	Constructor for CPathFormula object.
		 */
		CPathFormula();

		/*
		 *	Virtual destructor for a CPathFormula object.
		 */
		virtual ~CPathFormula()
		{
		}

		/*
		 *	Access to the calculated answer for this CPathFormula object.
		 */
		bool Get_Answer();

		/*
		 *	Access to a real number version of the answer for this CPathFormula
		 *	object.
		 */
		virtual double Get_Answer_Double();

		/*
		 *	Resets this CPathFormula object for use in future calcuations.
		 */
		virtual void Reset();

		/*
		 *	This method should be overridden by all reward subclasses to return
		 *	true.
		 */
		virtual bool Is_Reward();

		/*
		 *	This method should be called for each update to the current state.  This
		 *	method should be overridden to determine whether the answer can be determined for this update
		 *	and if, so calculates the answer and puts it in the answer member variable.
		 */
		virtual void Notify_State(CPathState* last_state, int* current_state)=0;

		/*
		 *	This method should be overridden to give a string representation of the
		 *	path or reward formula.
		 */
		virtual string To_String() =0;

		/*
		 *	Determines whether the answer is already known for this path formula.
		 */
		virtual bool Is_Answer_Known(CLoopDetectionHandler* loop_detection)=0;

                virtual void Write_Formula(int fd)=0;
		
};

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
class CRewardFormula : public CPathFormula
{

	protected:
		double answer_double; //used to store the reward answer

	public:
		/*
		 *	Constructor for a CRewardFormula object.
		 */
		CRewardFormula();

		/*
		 *	Returns the calculated reward for this CRewardFormula.
		 */
		double Get_Answer_Double();

		/*
		 *	Resets this CRewardFormula object for use in future calcuations.
		 */
		virtual void Reset();

		/*
		 *	Overridden to state that this object should be treated as a reward
		 *	formulae.
		 */
		bool Is_Reward();

                virtual void Write_Formula(int fd)=0;
};

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
class CBoundedUntil : public CPathFormula
{
	private:
		CExpression* leftExpression;
		CExpression* rightExpression;
		double upper_bound;
		double lower_bound;
		int counter;
		double time_so_far;

	public:

		/*
		 *	Constructor for a CBoundedUntil object.
		 */
		CBoundedUntil(CExpression* leftExpression, CExpression* rightExpression, double lower_bound, double bound);

		/*
		 *	Destructor for a CBoundedUntil object. Deletes the left and right expression
		 *	structures.
		 */
		~CBoundedUntil();

		/*
		 *	This method should be called for each update to the current state.  This
		 *	method determines whether the answer can be determined for this update
		 *	and if, so calculates the answer and puts it in the answer member variable.
		 */
		void Notify_State(CPathState* last_state, int* current_state);

		/*
		 *	Determines whether the answer is already known for this path formula.  This
		 *	will either be if the answer has been proven, or if the answer has not been
		 *	determined but the path is looping.
		 */
		bool Is_Answer_Known(CLoopDetectionHandler* loop_detection);

		/*
		 *	Returns a string representation of this CBoundedUntil object.
		 */
		string To_String();

		/*
		 *	Resets the counter and time_so_far variables, and calls the superclass Reset method.
		 */
		void Reset();

                void Write_Formula(int fd);
};

/*
 *	CUntil
 *	======
 *
 *	The CUntil class is used to store and deal with path formulae of the form:
 *	[ leftExpression U rightExpression ].
 *	i.e. leftExpression must hold true until rightExpression is satisfied.
 *
 */
class CUntil : public CPathFormula
{
	private:
		CExpression* leftExpression;
		CExpression* rightExpression;

	public:

		/*
		 *	Constructor for a CUntil object.
		 */
		CUntil(CExpression* leftExpression, CExpression* rightExpression);

		/*
		 *	Destructor for a CBoundedUntil object. Deletes the left and right expression
		 *	structures.
		 */
		~CUntil();

		/*
		 *	This method should be called for each update to the current state.  This
		 *	method determines whether the answer can be determined for this update
		 *	and if, so calculates the answer and puts it in the answer member variable.
		 */
		void Notify_State(CPathState* last_state, int* current_state);

		/*
		 *	Determines whether the answer is already known for this path formula.  This
		 *	will either be if the answer has been proven, or if the answer has not been
		 *	determined but the path is looping.
		 */
		bool Is_Answer_Known(CLoopDetectionHandler* loop_detection);

		/*
		 *	Returns a string representation of this CUntil object.
		 */
		string To_String();

                void Write_Formula(int fd);
};

/*
 *	CNext
 *	=====
 *
 *	The CNext class is used to store and deal with path formulae of the form:
 *	[ X expression ].
 *	i.e. expression must hold true in the state following the original state.
 *
 */
class CNext : public CPathFormula
{
	private:
		CExpression* expression;
		int counter;

	public:

		/*
		 *	Constructor for a CNext object.
		 */
		CNext(CExpression* expression);

		/*
		 *	Destructor for a CNextUntil object. Deletes the expression
		 *	structure.
		 */
		~CNext();

		/*
		 *	This method should be called for each update to the current state.  This
		 *	method determines whether the answer can be determined for this update
		 *	and if, so calculates the answer and puts it in the answer member variable.
		 */
		void Notify_State(CPathState* last_state, int* current_state);

		/*
		 *	Determines whether the answer is already known for this path formula.  This
		 *	will either be if the answer has been proven, or if the answer has not been
		 *	determined but the path is looping.
		 */
		bool Is_Answer_Known(CLoopDetectionHandler* loop_detection);

		/*
		 *	Returns a string representation of this CNext object.
		 */
		string To_String();

		/*
		 *	Resets the counter for this CNext object and calls the superclass Reset method.
		 */
		void Reset();
 
                void Write_Formula(int fd);
};

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
class CRewardCumulative : public CRewardFormula
{
	private:
		double time;
		int counter;
		double time_so_far;

	public:

		/*
		 *	Constructor for a CRewardCumulative object.
		 */
		CRewardCumulative(double time);

		/*
		 *	Destructor for a CRewardCumulative object.
		 */
		~CRewardCumulative();

		/*
		 *	This method should be called for each update to the current state.  This
		 *	method determines whether the answer can be determined for this update
		 *	and if, so calculates the answer and puts it in the answer member variable.
		 */
		void Notify_State(CPathState* last_state, int* current_state);

		/*
		 *	Returns a string representation of this CRewardCumulative object.
		 */
		string To_String();

		/*
		 *	Resets the counter, time_so_far and answer_double and calls the superclass
		 *	Reset() method.
		 */
		void Reset();

		/*
		 *	Determines whether the answer is already known for this path formula.  This
		 *	will *only* be if the answer has been proven, *not* if the path is looping.
		 */
		bool Is_Answer_Known(CLoopDetectionHandler* loop_detection); //must override this because do not want looping to stop verification

                void Write_Formula(int fd);
};

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
class CRewardInstantanious : public CRewardFormula
{
	private:
		double time;
		int counter;
		double time_so_far;

	public:

		/*
		 *	Constructor for a CRewardInstantaneous object...and yes it does set the
		 *	time_so_far variable to 0.0 now... ooops
		 */
		CRewardInstantanious(double time);

		/*
		 *	Destructor for a CRewardInstantaneous object.
		 */
		~CRewardInstantanious();

		/*
		 *	This method should be called for each update to the current state.  This
		 *	method determines whether the answer can be determined for this update
		 *	and if, so calculates the answer and puts it in the answer member variable.
		 */
		void Notify_State(CPathState* last_state, int* current_state);

		/*
		 *	Returns a string representation of this CRewardInstantaneous object.
		 */
		string To_String();

		/*
		 *	Resets the counter, time_so_far and answer_double variables and calls the superclass
		 *	Reset() method.
		 */
		void Reset();

		/*
		 *	Determines whether the answer is already known for this path formula.  This
		 *	will *only* be if the answer has been proven, *not* if the path is looping.
		 */
		bool Is_Answer_Known(CLoopDetectionHandler* loop_detection); //must override this because do not want looping to stop verification

                void Write_Formula(int fd);
};

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
class CRewardReachability : public CRewardFormula
{
	private:
		CExpression* expression;

	public:

		/*
		 *	Constructor for a CRewardReachability object.
		 */
		CRewardReachability(CExpression* expression);

		/*
		 *	Destructor for a CRewardReachability object.
		 */
		~CRewardReachability();

		/*
		 *	This method should be called for each update to the current state.  This
		 *	method determines whether the answer can be determined for this update
		 *	and if, so calculates the answer and puts it in the answer member variable.
		 */
		void Notify_State(CPathState* last_state, int* current_state);

		/*
		 *	Returns a string representation of this CRewardReachability object.
		 */
		string To_String();

		/*
		 *	Determines whether the answer is already known for this path formula.  This
		 *	will either be if the answer has been proven, or if the path is looping.
		 *	If the path is looping, this method also sets the answer_double to undefined
		 *	because the reward is infinite.
		 */
		bool Is_Answer_Known(CLoopDetectionHandler* loop_detection);
                
                void Write_Formula(int fd);
};



//=============================================================================
// Function Declarations
//=============================================================================

/**
*	This function removes all registered path formulae
*	from memory.
*/
void Deallocate_PCTL_Core();

/*
 *	Allocates storage for path formulae to be registered.
 */
void Allocate_PCTL_Core();

/*
 *	Adds the given path formula to registered_path_formula.
 */
int Register_Path_Formula(CPathFormula* formula);

/*
 *	Returns the index of the formula parameter in registered_path_formula
 */
int Index_Of_Path_Formula(CPathFormula* formula);

/*
 *	Calls the Notify_State method of each path or reward formula in
 *	registered_path_formulae.
 */
void Notify_Path_Formulae(CPathState* last_state, int* current_state, CLoopDetectionHandler* loop_detection);

//TODO move this to path PCTL manager

/*
void Recalculate_Path_Formulae();*/

/*
 *	Resets each path or reward formula in registered_path_formulae for
 *	new calculations.
 */
void Reset_Path_Formulae();

/*
 *	Method for direct querying of a registered path formula.
 *	Returns -1 if the answer is not known (or an error)
 *	Returns 0 if the answer is known as false
 *	Returns 1 if the answer is known as true
 *	Returns 2 if the answer is numeric.
 */
int Query_Path_Formula(int index);

/*
 *	If the path formula at the index is numeric, this returns the answer
 */
double Query_Path_Formula_Numeric(int index);

/*
 *	Outputs the contents of registered_path_formulae to the command line.
 */
void Print_Formulae();

CPathFormula* Get_Path_Formula(int index);

//==============================================================================
//      IO Functions
//==============================================================================

void Write_Pctl_Manager(int fd);

void Read_Pctl(int fd);

CPathFormula* Read_Path_Formula(int fd);

#endif
