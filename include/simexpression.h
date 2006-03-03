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

#ifndef _Included_simulator_Expressionheader
#define _Included_simulator_Expressionheader

#include <string>
#include <iostream>
#ifdef _WIN32
#include <io.h>
#else
#include <unistd.h>
#endif
#include <fcntl.h>
#include "simiohandler.h"


using std::cout;
using std::endl;

//used as class identifiers for parsing
const int EXPR_INTEGER_VAR                  = 0;
const int EXPR_BOOLEAN_VAR                  = 1;
const int EXPR_DOUBLE                       = 2;
const int EXPR_INTEGER                      = 3;
const int EXPR_BOOLEAN                      = 4;
const int EXPR_CEIL                         = 5;
const int EXPR_FLOOR                        = 6;
const int EXPR_NOT                          = 7;
const int EXPR_AND                          = 8;
const int EXPR_OR                           = 9;
const int EXPR_ITE                          = 10;
const int EXPR_REAL_ITE                     = 11;
const int EXPR_NORMAL_MAX                   = 12;
const int EXPR_NORMAL_MIN                   = 13;
const int EXPR_REAL_MAX                     = 14;
const int EXPR_REAL_MIN                     = 15;
const int EXPR_NORMAL_POW                   = 16;
const int EXPR_REAL_POW                     = 17;
const int EXPR_MOD                          = 18;
const int EXPR_NORMAL_TIMES                 = 19;
const int EXPR_NORMAL_PLUS                  = 20;
const int EXPR_NORMAL_MINUS                 = 21;
const int EXPR_REAL_TIMES                   = 22;
const int EXPR_DIVIDE                       = 23;
const int EXPR_REAL_PLUS                    = 24;
const int EXPR_REAL_MINUS                   = 25;
const int EXPR_NORMAL_EQUALS                = 26;
const int EXPR_REAL_EQUALS                  = 27;
const int EXPR_NORMAL_NOT_EQUALS            = 28;
const int EXPR_REAL_NOT_EQUALS              = 29;
const int EXPR_NORMAL_LESS_THAN             = 30;
const int EXPR_REAL_LESS_THAN               = 31;
const int EXPR_NORMAL_GREATER_THAN          = 32;
const int EXPR_REAL_GREATER_THAN            = 33;
const int EXPR_NORMAL_LESS_THAN_EQUAL       = 34;
const int EXPR_REAL_LESS_THAN_EQUAL         = 35;
const int EXPR_NORMAL_GREATER_THAN_EQUAL    = 36;
const int EXPR_REAL_GREATER_THAN_EQUAL      = 37;

//Class definitions

class CExpression
{
	public:

		virtual ~CExpression()
		{
		}
		//For boolean and integer expressions CExpression is used,
		//therefore this will be used to 
		virtual int Get_Type()=0;
		//is there only one possible result for this value?
		//This can be used for optimisation
		virtual bool One_Result()=0;


		//note that these two methods should not create recursive loops
		//at least one should be always be overridden
		virtual int Evaluate(){ return (int)EvaluateDouble(); } 

		virtual double EvaluateDouble(){ return (double)Evaluate(); } //should only get called when comparing an int with a double
		//but this code is still RISKY... test this muchly

		//These methods don't evaluate the current state, but
		//a separate array which should be the same size as 
		//the current state
		virtual int Evaluate(int*vars)
		{ 
			return (int)EvaluateDouble(vars);
		}
		virtual double EvaluateDouble(int*vars)
		{
			return (double)Evaluate();
		}

		//return a string representation
		virtual std::string To_String()=0;

                virtual void Write_Expression(int fd)=0;

		
};

class CNormalExpression : public CExpression
{
	public:
		virtual ~CNormalExpression()
		{
			//cout << "CNormalExpression destructor called." << endl;
		}
		//without touching the information stored in the expression,
		//this returns the result
		virtual int Evaluate()=0;
		virtual int Evaluate(int*)=0;
		virtual std::string To_String()=0;
                virtual void Write_Expression(int fd)=0;
};

class CRealExpression : public CExpression
{
	public:
		virtual ~CRealExpression()
		{
		}
		//without touching the information stored in the expression,
		//this returns the result
		virtual double EvaluateDouble()=0;
		virtual double EvaluateDouble(int*)=0;
		virtual std::string To_String()=0;
                virtual void Write_Expression(int fd)=0;
};

//Terminals

//Note we don't need defined constants, as we just evaluate those automatically
//at load time.  We do need undefined constants to store before they have been
//defined for the current path. NOT TRUE

class CNormalConstant: public CNormalExpression //for undefined constants
{
	private:
		int * index; //pointer to where this constant will be defined
		int type;
	public:
		CNormalConstant(int *, int);
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);    

		  
};

class CRealConstant: public CRealExpression //for undefined constants
{
	private:
		double * index; //pointer to where this constant will be defined
	public:
		CRealConstant(double *);
		int Get_Type();
		double EvaluateDouble();
		double EvaluateDouble(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CIntegerVar: public CNormalExpression //for all integer variables
{
	private:
		int * value; //points to variable table
		int index;
	public:
		CIntegerVar(int * statePointer, int index);
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
        void Write_Expression(int fd);
		  
};

class CBooleanVar: public CNormalExpression //for all boolean variables
{
	private:
		int * value; //points to variable table
		int index;
	public:
		CBooleanVar(int *statePointer, int index);
		~CBooleanVar();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();	
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CDouble: public CRealExpression //for all double values
{
	private:
		double value; //the actual value
	public:
		CDouble(double);
		int Get_Type();
		double EvaluateDouble();
		double EvaluateDouble(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CInteger: public CNormalExpression //for all integer values
{
	private:
		int value; //the actual value;
	public:
		CInteger(int);
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CBoolean: public CNormalExpression //for all boolean values
{
	private:
		bool value; //the actual value (true or false)
	public:
		CBoolean(bool);
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
};


//Unary Operators

class CCeil: public CNormalExpression
{
	private:
		CExpression * expr;
	public:
		CCeil(CExpression *); //constructor must check the type is !bool
		~CCeil();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
		
};

class CFloor: public CNormalExpression
{
	private:
		CExpression * expr;
	public:
		CFloor(CExpression *); //constructor must check the type is !bool
		~CFloor();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
		
};

class CNot: public CNormalExpression
{
	private:
		CNormalExpression * expr;
	public:
		CNot(CNormalExpression *); //constructor check the type is bool
		~CNot();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
		
};

//Nary operators
class CAnd: public CNormalExpression
{
	private:
		CNormalExpression ** exprs; //pointer to an array of expressions
		int no_exprs;
	public:
		CAnd(CNormalExpression **, int);
		~CAnd();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
		
};

class COr: public CNormalExpression
{
	private:
		CNormalExpression ** exprs; //pointer to an array of expressions
		int no_exprs;
	public:
		COr(CNormalExpression **, int);
		~COr();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
		
};

class CIte: public CNormalExpression //if then else expression
{
	private:
		CNormalExpression * condition;
		CExpression * trueCase;
		CExpression * falseCase;
		int type; //we can determine the type at the beginning

	public:
		CIte(CNormalExpression *, CExpression *, CExpression *);
		~CIte();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
};

class CRealIte: public CRealExpression //if then else expression
{
	private:
		CNormalExpression * condition;
		CExpression * trueCase;
		CExpression * falseCase;
		int type; //we can determine the type at the beginning

	public:
		CRealIte(CNormalExpression *, CExpression *, CExpression *);
		~CRealIte();
		int Get_Type();
		double EvaluateDouble();
		double EvaluateDouble(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CNormalMax: public CNormalExpression
{
	private:
		CNormalExpression ** exprs; //pointer to an array of expressions
		int no_exprs;
	public:
		CNormalMax(CNormalExpression **, int);
		~CNormalMax();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CNormalMin: public CNormalExpression
{
	private:
		CNormalExpression ** exprs; //pointer to an array of expressions
		int no_exprs;
	public:
		CNormalMin(CNormalExpression **, int);
		~CNormalMin();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CRealMax: public CRealExpression
{
	private:
		CExpression ** exprs; //pointer to an array of expressions
		int no_exprs;
	public:
		CRealMax(CExpression **, int);
		~CRealMax();
		int Get_Type();
		double EvaluateDouble();
		double EvaluateDouble(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CRealMin: public CRealExpression
{
	private:
		CExpression ** exprs; //pointer to an array of expressions
		int no_exprs;
	public:
		CRealMin(CExpression **, int);
		~CRealMin();
		int Get_Type();
		double EvaluateDouble();
		double EvaluateDouble(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
		  
};


//Binary operators

class CNormalPow: public CNormalExpression
{
	private:
		CNormalExpression * lexpr;
		CNormalExpression * rexpr;
	public:
		CNormalPow(CNormalExpression *, CNormalExpression *);
		~CNormalPow();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
};

class CRealPow: public CRealExpression
{
	private:
		CExpression * lexpr;
		CExpression * rexpr;
	public:
		CRealPow(CExpression *, CExpression *);
		~CRealPow();
		int Get_Type();
		double EvaluateDouble();
		double EvaluateDouble(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
};

class CMod: public CNormalExpression
{
	private:
		CNormalExpression * lexpr;
		CNormalExpression * rexpr;
	public:
		CMod(CNormalExpression *, CNormalExpression *);
		~CMod();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);
};

class CNormalTimes: public CNormalExpression
{
	private:
		CNormalExpression * lexpr;
		CNormalExpression * rexpr;
	public:
		CNormalTimes(CNormalExpression *, CNormalExpression *);
		~CNormalTimes();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);

		  
};

class CNormalPlus: public CNormalExpression
{
	private:
		CNormalExpression * lexpr;
		CNormalExpression * rexpr;
	public:
		CNormalPlus(CNormalExpression *, CNormalExpression *);
		~CNormalPlus();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);

		  
};

class CNormalMinus: public CNormalExpression
{
	private:
		CNormalExpression * lexpr;
		CNormalExpression * rexpr;
	public:
		CNormalMinus(CNormalExpression *, CNormalExpression *);
		~CNormalMinus();
		int Get_Type();
		int Evaluate();
		int Evaluate(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);

		  
};

class CRealTimes: public CRealExpression
{
	private:
		CExpression * lexpr;
		CExpression * rexpr;
		bool owns_children;
	public:
		CRealTimes(CExpression *, CExpression *);
		~CRealTimes();
		int Get_Type();
		double EvaluateDouble();
		double EvaluateDouble(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);

		  

		//This is to handle probabilities for merged updates
		void Set_Owns_Children(bool);
};

class CDivide: public CRealExpression
{
	private:
		CExpression * lexpr;
		CExpression * rexpr;
	public:
		CDivide(CExpression *, CExpression *);
		~CDivide();
		int Get_Type();
		double EvaluateDouble();
		double EvaluateDouble(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);

		  
};

class CRealPlus: public CRealExpression
{ 
	private:
		CExpression * lexpr;
		CExpression * rexpr;
	public:
		CRealPlus(CExpression *, CExpression *);
		~CRealPlus();
		int Get_Type();
		double EvaluateDouble();
		double EvaluateDouble(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);

		  
};

class CRealMinus: public CRealExpression
{
	private:
		CExpression * lexpr;
		CExpression * rexpr;
	public:
		CRealMinus(CExpression *, CExpression *);
		~CRealMinus();
		int Get_Type();
		double EvaluateDouble();
		double EvaluateDouble(int*);
		bool One_Result();
		std::string To_String();
                void Write_Expression(int fd);

		  
};

//Relative Operators


class CRelOp : public CNormalExpression
{
	protected:
		CExpression * lexpr;
		CExpression * rexpr;
	public:
		CRelOp(CExpression *, CExpression *);
		~CRelOp();
		int Get_Type();
		bool One_Result();
		virtual int Evaluate()=0;
		virtual int Evaluate(int*)=0;
		virtual std::string To_String()=0;
                virtual void Write_Expression(int fd)=0;
};


class CNormalEquals : public CRelOp
{
	public:
		CNormalEquals(CNormalExpression *, CNormalExpression *);
		int Evaluate();
		int Evaluate(int*);
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CRealEquals : public CRelOp
{	
	public:
		CRealEquals(CExpression *, CExpression *);
		int Evaluate();
		int Evaluate(int*);
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CNormalNotEquals: public CRelOp
{
	public:
		CNormalNotEquals(CNormalExpression *, CNormalExpression *);
		int Evaluate();
		int Evaluate(int*);
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CRealNotEquals: public CRelOp
{
	public:
		CRealNotEquals(CExpression *, CExpression *);
		int Evaluate();
		int Evaluate(int*);
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CNormalLessThan: public CRelOp
{
	public:
		CNormalLessThan(CNormalExpression *, CNormalExpression *);
		int Evaluate();
		int Evaluate(int*);
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CRealLessThan: public CRelOp
{
	public:
		CRealLessThan(CExpression *, CExpression *);
		int Evaluate();
		int Evaluate(int*);
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CNormalGreaterThan: public CRelOp
{
	public:
		CNormalGreaterThan(CNormalExpression *, CNormalExpression *);
		int Evaluate();
		int Evaluate(int*);
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CRealGreaterThan: public CRelOp
{
	public:
		CRealGreaterThan(CExpression *, CExpression *);
		int Evaluate();
		int Evaluate(int*);
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CNormalLessThanEqual: public CRelOp
{
	public:
		CNormalLessThanEqual(CNormalExpression *, CNormalExpression *);
		int Evaluate();
		int Evaluate(int*);
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CRealLessThanEqual: public CRelOp
{
	public:
		CRealLessThanEqual(CExpression *, CExpression *);
		int Evaluate();
		int Evaluate(int*);
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CNormalGreaterThanEqual: public CRelOp
{
	public:
		CNormalGreaterThanEqual(CNormalExpression *, CNormalExpression *);
		int Evaluate();
		int Evaluate(int*);
		std::string To_String();
                void Write_Expression(int fd);
		  
};

class CRealGreaterThanEqual: public CRelOp
{
	public:
		CRealGreaterThanEqual(CExpression *, CExpression *);
		int Evaluate();
		int Evaluate(int*);
		std::string To_String();
                void Write_Expression(int fd);
		  
};

//==============================================================================
//      Functions
//==============================================================================

CExpression* Read_Expression(int fd);

CNormalExpression* Read_Normal_Expression(int fd);

CRealExpression* Read_Real_Expression(int fd);



#endif
