//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

#include "simexpression.h"
//#include "SimulatorEngine.h"
#include "simutil.h"
#include "simstate.h"
#include "simiohandler.h"
#include <string>
#ifdef _WIN32
#include <io.h>
#else
#include <unistd.h>
#endif
#include <fcntl.h>
#include <math.h>

//=============================================================================
//	Description
//=============================================================================

//=============================================================================
//	Class Definitions
//=============================================================================

//Class methods for CIntegerVar


CIntegerVar::CIntegerVar(int * value, int index)
{
	//cout << "in the contructor" << endl;
	this->value = value;  //value points to the value in the variable table.
	this->index = index;
}

int CIntegerVar::Get_Type()
{
	return INTEGER;
}

int CIntegerVar::Evaluate()
{
	//std::cout << "evaluate called for cintegervar" << std::endl;
	
	return state_variables[index]; //return the value pointed to by index
}

int CIntegerVar::Evaluate(int*vars)
{
	//std::cout << "evaluate called for cintegervar" << std::cout;
	return vars[index]; //return the value pointed to by index
}

bool CIntegerVar::One_Result()
{
	return false;
}

std::string CIntegerVar::To_String()
{
	
	return "({iv@"+Int_To_String((int)(value - state_variables))+"})";
}

void CIntegerVar::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_INTEGER_VAR;
    write(fd, &expr_type, sizeof(int));
    //the information
    write(fd, &index, sizeof(int));
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CBooleanVar

CBooleanVar::~CBooleanVar()
{
}

CBooleanVar::CBooleanVar(int * value, int index)
{
	this->value = value;  //value points to the value in the variable table.
	this->index = index;
}

int CBooleanVar::Get_Type()
{
	return BOOLEAN;
}

int CBooleanVar::Evaluate()
{
	return *value; //return the value pointed to by index
}

int CBooleanVar::Evaluate(int*vars)
{
	return vars[this->index]; //return the value pointed to by index
}

bool CBooleanVar::One_Result()
{
	return false;
}

std::string CBooleanVar::To_String()
{
	std::string s1 = "({bv@";
	std::string s2 = Int_To_String((int)(value - state_variables));
	std::string s3 = "})";
	return s1+s2+s3;
}

void CBooleanVar::Write_Expression(int fd)
{
    int expr_type = EXPR_BOOLEAN_VAR;
    write(fd, &expr_type, sizeof(int));
    //the information
    write(fd, &index, sizeof(int));
    //null byte to finish off expression
    write(fd, "\0", 1);
}


//Class methods for CDouble

CDouble::CDouble(double value)
{
	this->value = value;  
}

int CDouble::Get_Type()
{
	return DOUBLE;
}

double CDouble::EvaluateDouble()
{
	return value; 
}

double CDouble::EvaluateDouble(int*vars)
{
	return value; 
}

bool CDouble::One_Result()
{
	return true;
}

std::string CDouble::To_String()
{
	return "|"+Double_To_String(value)+"|";
}

void CDouble::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_DOUBLE;
    write(fd, &expr_type, sizeof(int));
    //the information
    write(fd, &value, sizeof(double));
    //null byte to finish off expression
    write(fd, "\0", 1);
}



//Class methods for CInteger

CInteger::CInteger(int value)
{
	this->value = value;  
}

int CInteger::Get_Type()
{
	return INTEGER;
}

int CInteger::Evaluate()
{
	return value; 
}

int CInteger::Evaluate(int*vars)
{
	return value; 
}

bool CInteger::One_Result()
{
	return true;
}
std::string CInteger::To_String()
{
	//std::cout << "testing integer values" <<endl;
	return "|"+Int_To_String(value)+"|";
}

void CInteger::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_INTEGER;
    write(fd, &expr_type, sizeof(int));
    //the information
    write(fd, &value, sizeof(int));
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CBoolean

CBoolean::CBoolean(bool value)
{
	this->value = value;  
}

int CBoolean::Get_Type()
{
	return BOOLEAN;
}

int CBoolean::Evaluate()
{
	return value; 
}

int CBoolean::Evaluate(int*vars)
{
	return value; 
}

bool CBoolean::One_Result()
{
	return true;
}

std::string CBoolean::To_String()
{
	return "|"+Bool_To_String((bool)value)+"|";
}

void CBoolean::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_BOOLEAN;
    write(fd, &expr_type, sizeof(int));
    //the information
    write(fd, &value, sizeof(bool));
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CCeil


CCeil::CCeil(CExpression * expr)
{
	this->expr = expr;  

	//ceil functions does not accept boolean's throw an exception
	if(expr->Get_Type() == BOOLEAN) 
		throw "type error when constructing ceil: Boolean not accepted.";
}

CCeil::~CCeil()
{
	if(expr != NULL)
	delete expr;
}

int CCeil::Get_Type()
{
	return INTEGER;
}

int CCeil::Evaluate()
{
	if(expr->Get_Type() == DOUBLE)
		return (int)ceil(expr->EvaluateDouble());
	else
		return expr->Evaluate();
}

int CCeil::Evaluate(int*vars)
{
	if(expr->Get_Type() == DOUBLE)
		return (int)ceil(expr->EvaluateDouble(vars));
	else
		return expr->Evaluate(vars);
}

bool CCeil::One_Result()
{
	return expr->One_Result();
}

std::string CCeil::To_String()
{
	return "(ceil{"+expr->To_String()+"})";
}

void CCeil::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_CEIL;
    write(fd, &expr_type, sizeof(int));
    //the information
    expr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CFloor

CFloor::CFloor(CExpression * expr)
{
	this->expr = expr;  

	//ceil functions does not accept boolean's throw an exception
	if(expr->Get_Type() == BOOLEAN) 
		throw "type error when constructing floor: Boolean not accepted.";
}

CFloor::~CFloor()
{
	if(expr != NULL)
	delete expr;
}

int CFloor::Get_Type()
{
	return INTEGER;
}

int CFloor::Evaluate()
{
	if(expr->Get_Type() == DOUBLE)
		return (int)floor(expr->EvaluateDouble());
	else
		return expr->Evaluate();
}

int CFloor::Evaluate(int*vars)
{
	if(expr->Get_Type() == DOUBLE)
		return (int)floor(expr->EvaluateDouble(vars));
	else
		return expr->Evaluate(vars);
}

bool CFloor::One_Result()
{
	return expr->One_Result();
}

std::string CFloor::To_String()
{
	return "(floor{"+expr->To_String()+"})";
}

void CFloor::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_FLOOR;
    write(fd, &expr_type, sizeof(int));
    //the information
    expr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CNot

CNot::CNot(CNormalExpression * expr)
{
	this->expr = expr;  

	//ceil functions does not accept boolean's throw an exception
	if(expr->Get_Type() != BOOLEAN) 
		throw "type error when constructing not: expression should be Boolean.";
}

CNot::~CNot()
{
	//std::cout << "destructor for CNOT" << endl;
	if(expr != NULL)
	delete expr;
}

int CNot::Get_Type()
{
	return BOOLEAN;
}

int CNot::Evaluate()
{
	return !(expr->Evaluate()); //return the value pointed to by index
}

int CNot::Evaluate(int*vars)
{
	return !(expr->Evaluate(vars)); //return the value pointed to by index
}

bool CNot::One_Result()
{
	return expr->One_Result();
}

std::string CNot::To_String()
{
	//std::cout << "testing cnot To_String" << endl;
	return "(!{"+expr->To_String()+"})";
}

void CNot::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_NOT;
    write(fd, &expr_type, sizeof(int));
    //the information
    expr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CAnd


CAnd::CAnd(CNormalExpression ** exprs, int no_exprs)
{
	//std::cout << "CAnd constructor" <<endl;
	this->exprs = exprs;
	this->no_exprs = no_exprs;

	//std::cout <<"CAnd before boolean checks" <<endl;
	//Check that all expressions are Boolean
	for(int i = 0; i < no_exprs; i++)
	{
		//std::cout << "loop" << i << "looking at: " << &(exprs[i])<< " its To_String = " << exprs[i].To_String() << endl;
		
		
		if(exprs[i]->Get_Type() != BOOLEAN)
		{
			throw "type error when constructing and: expression should be Boolean.";
		}
	}
	//std::cout << "CAnd end" << endl;

	
}

CAnd::~CAnd()
{
	if(exprs != NULL)
	{
		for(int i = 0 ; i <no_exprs; i++)
		{
			if(exprs[i] != NULL) 
				delete exprs[i];
		}
		delete[] exprs;
	}
}

int CAnd::Get_Type()
{
	return BOOLEAN;
}

int CAnd::Evaluate()
{
	for(int i = 0; i < no_exprs; i++)
		if(!exprs[i]->Evaluate()) return false;
	return true;
}

int CAnd::Evaluate(int*vars)
{
	for(int i = 0; i < no_exprs; i++)
		if(!exprs[i]->Evaluate(vars)) return false;
	return true;
}

bool CAnd::One_Result()
{
	bool all_one = true;
	for(int i = 0; i < no_exprs; i++)
	{
		if(exprs[i]->One_Result())
			if(!exprs[i]->Evaluate()) return true; // if one is certainly false then only one answer(false)
		else
			all_one = false; //although we have found something not with multi results, carry on looking for one result expressions that are false
	}
	return all_one;
}

std::string CAnd::To_String()
{
	std::string str = "(";

	for(int i = 0; i < no_exprs; i++)
	{
		str = str + exprs[i]->To_String();
		if(i < no_exprs-1) str += "/\\";
	}
	
	return str + ")";
}

void CAnd::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_AND;
    write(fd, &expr_type, sizeof(int));
    //the information
    write(fd, &no_exprs, sizeof(int));
    for(int i = 0; i < no_exprs; i++)
    {
        exprs[i]->Write_Expression(fd);
    }
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for COr


COr::COr(CNormalExpression ** exprs, int no_exprs)
{
	this->exprs = exprs;
	this->no_exprs = no_exprs;

	//Check that all expressions are Boolean
	for(int i = 0; i < no_exprs; i++)
		if(exprs[i]->Get_Type() != BOOLEAN)
			throw "type error when constructing or: expression should be Boolean.";
}

COr::~COr()
{
	if(exprs != NULL)
	{
		for(int i = 0 ; i <no_exprs; i++)
		{
			if(exprs[i] != NULL) 
				delete exprs[i];
		}
		delete[] exprs;
	}
}

int COr::Get_Type()
{
	return BOOLEAN;
}

int COr::Evaluate()
{
	for(int i = 0; i < no_exprs; i++)
		if(exprs[i]->Evaluate()) return true;
	return false;
}

int COr::Evaluate(int*vars)
{
	for(int i = 0; i < no_exprs; i++)
		if(exprs[i]->Evaluate(vars)) return true;
	return false;
}

bool COr::One_Result()
{
	bool all_one = true;
	for(int i = 0; i < no_exprs; i++)
	{
		if(exprs[i]->One_Result())
			if(exprs[i]->Evaluate()) return true; // if one is certainly false then only one answer(false)
		else
			all_one = false; //although we have found something not with multi results, carry on looking for one result expressions that are true
	}
	return all_one;
}

std::string COr::To_String()
{
	std::string str = "(";

	for(int i = 0; i < no_exprs; i++)
	{
		str = str + exprs[i]->To_String();
		if(i < no_exprs-1) str += "\\/";
	}
	
	return str + ")";
}

void COr::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_OR;
    write(fd, &expr_type, sizeof(int));
    //the information
    write(fd, &no_exprs, sizeof(int));
    for(int i = 0; i < no_exprs; i++)
    {
        exprs[i]->Write_Expression(fd);
    }
    //null byte to finish off expression
    write(fd, "\0", 1);
}


//Class methods for CNormalMax

CNormalMax::CNormalMax(CNormalExpression ** exprs, int no_exprs)
{
	this->exprs = exprs;
	this->no_exprs = no_exprs;

	//Check that no expressions are Boolean
	for(int i = 0; i < no_exprs; i++)
		if(exprs[i]->Get_Type() == BOOLEAN)
			throw "type error when constructing max: expressions cannot be Boolean.";
}

CNormalMax::~CNormalMax()
{
	if(exprs != NULL)
	{
		for(int i = 0 ; i <no_exprs; i++)
		{
			if(exprs[i] != NULL) 
				delete exprs[i];
		}
		delete[] exprs;
	}
}

int CNormalMax::Get_Type()
{
	return INTEGER;
}

int CNormalMax::Evaluate()
{
	int max = exprs[0]->Evaluate();
	int value;
	for(int i = 1; i < no_exprs; i++)
	{
		value = exprs[i]->Evaluate();
		if(value > max) max = value;
	}
	return max;
}

int CNormalMax::Evaluate(int*vars)
{
	int max = exprs[0]->Evaluate(vars);
	int value;
	for(int i = 1; i < no_exprs; i++)
	{
		value = exprs[i]->Evaluate(vars);
		if(value > max) max = value;
	}
	return max;
}

bool CNormalMax::One_Result()
{
	for(int i = 0; i < no_exprs; i++)
		if(!exprs[i]->One_Result()) return false;
	return true;
}

std::string CNormalMax::To_String()
{
	std::string str = "(nmax{";

	for(int i = 0; i < no_exprs; i++)
	{
		str = str + exprs[i]->To_String();
		if(i < no_exprs-1) str += ",";
	}
	
	return str + "})";
}

void CNormalMax::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_NORMAL_MAX;
    write(fd, &expr_type, sizeof(int));
    //the information
    write(fd, &no_exprs, sizeof(int));
    for(int i = 0; i < no_exprs; i++)
    {
        exprs[i]->Write_Expression(fd);
    }
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CNormalMin

CNormalMin::CNormalMin(CNormalExpression ** exprs, int no_exprs)
{
	this->exprs = exprs;
	this->no_exprs = no_exprs;

	//Check that no expressions are Boolean
	for(int i = 0; i < no_exprs; i++)
		if(exprs[i]->Get_Type() == BOOLEAN)
			throw "type error when constructing max: expressions cannot be Boolean.";
}

CNormalMin::~CNormalMin()
{
	if(exprs != NULL)
	{
		for(int i = 0 ; i <no_exprs; i++)
		{
			if(exprs[i] != NULL) 
				delete exprs[i];
		}
		delete[] exprs;
	}
}

int CNormalMin::Get_Type()
{
	return INTEGER;
}

int CNormalMin::Evaluate()
{
	int min = exprs[0]->Evaluate();
	int value;
	for(int i = 1; i < no_exprs; i++)
	{
		value = exprs[i]->Evaluate();
		if(value < min) min = value;
	}
	return min;
}

int CNormalMin::Evaluate(int * vars)
{
	int min = exprs[0]->Evaluate(vars);
	int value;
	for(int i = 1; i < no_exprs; i++)
	{
		value = exprs[i]->Evaluate(vars);
		if(value < min) min = value;
	}
	return min;
}

bool CNormalMin::One_Result()
{
	for(int i = 0; i < no_exprs; i++)
		if(!exprs[i]->One_Result()) return false;
	return true;
}

std::string CNormalMin::To_String()
{
	std::string str = "(nmin{";

	for(int i = 0; i < no_exprs; i++)
	{
		str = str + exprs[i]->To_String();
		if(i < no_exprs-1) str += ",";
	}
	
	return str + "})";
}

void CNormalMin::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_NORMAL_MIN;
    write(fd, &expr_type, sizeof(int));
    //the information
    write(fd, &no_exprs, sizeof(int));
    for(int i = 0; i < no_exprs; i++)
    {
        exprs[i]->Write_Expression(fd);
    }
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CRealMax

CRealMax::CRealMax(CExpression ** exprs, int no_exprs)
{
	this->exprs = exprs;
	this->no_exprs = no_exprs;

	//Check that no expressions are Boolean
	for(int i = 0; i < no_exprs; i++)
		if(exprs[i]->Get_Type() == BOOLEAN)
			throw "type error when constructing max: expressions cannot be Boolean.";
}

CRealMax::~CRealMax()
{
	if(exprs != NULL)
	{
		for(int i = 0 ; i <no_exprs; i++)
		{
			if(exprs[i] != NULL) 
				delete exprs[i];
		}
		delete[] exprs;
	}
}

int CRealMax::Get_Type()
{
	return DOUBLE;
}

double CRealMax::EvaluateDouble()
{
	double max;
	if(exprs[0]->Get_Type() == DOUBLE)
		max = exprs[0]->EvaluateDouble();
	else
		max = exprs[0]->Evaluate();
	double value;
	for(int i = 1; i < no_exprs; i++)
	{
		if(exprs[i]->Get_Type() == DOUBLE)
			value = exprs[i]->EvaluateDouble();
		else
			value = exprs[i]->Evaluate();
		if(value > max) max = value;
	}
	return max;
}

double CRealMax::EvaluateDouble(int*vars)
{
	double max;
	if(exprs[0]->Get_Type() == DOUBLE)
		max = exprs[0]->EvaluateDouble(vars);
	else
		max = exprs[0]->Evaluate(vars);
	double value;
	for(int i = 1; i < no_exprs; i++)
	{
		if(exprs[i]->Get_Type() == DOUBLE)
			value = exprs[i]->EvaluateDouble(vars);
		else
			value = exprs[i]->Evaluate(vars);
		if(value > max) max = value;
	}
	return max;
}

bool CRealMax::One_Result()
{
	for(int i = 0; i < no_exprs; i++)
		if(!exprs[i]->One_Result()) return false;
	return true;
}

std::string CRealMax::To_String()
{
	std::string str = "(rmax{";

	for(int i = 0; i < no_exprs; i++)
	{
		str = str + exprs[i]->To_String();
		if(i < no_exprs-1) str += ",";
	}
	
	return str + "})";
}

void CRealMax::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_REAL_MAX;
    write(fd, &expr_type, sizeof(int));
    //the information
    write(fd, &no_exprs, sizeof(int));
    for(int i = 0; i < no_exprs; i++)
    {
        exprs[i]->Write_Expression(fd);
    }
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CRealMax

CRealMin::CRealMin(CExpression ** exprs, int no_exprs)
{
	this->exprs = exprs;
	this->no_exprs = no_exprs;

	//Check that no expressions are Boolean
	for(int i = 0; i < no_exprs; i++)
		if(exprs[i]->Get_Type() == BOOLEAN)
			throw "type error when constructing max: expressions cannot be Boolean.";
}

CRealMin::~CRealMin()
{
	if(exprs != NULL)
	{
		for(int i = 0 ; i <no_exprs; i++)
		{
			if(exprs[i] != NULL) 
				delete exprs[i];
		}
		delete[] exprs;
	}
}

int CRealMin::Get_Type()
{
	return DOUBLE;
}

double CRealMin::EvaluateDouble()
{
	double min;
	if(exprs[0]->Get_Type() == DOUBLE)
		min = exprs[0]->EvaluateDouble();
	else
		min = exprs[0]->Evaluate();
	double value;
	for(int i = 1; i < no_exprs; i++)
	{
		if(exprs[i]->Get_Type() == DOUBLE)
			value = exprs[i]->EvaluateDouble();
		else
			value = exprs[i]->Evaluate();
		if(value < min) min = value;
	}
	return min;
}

double CRealMin::EvaluateDouble(int*vars)
{
	double min;
	if(exprs[0]->Get_Type() == DOUBLE)
		min = exprs[0]->EvaluateDouble(vars);
	else
		min = exprs[0]->Evaluate(vars);
	double value;
	for(int i = 1; i < no_exprs; i++)
	{
		if(exprs[i]->Get_Type() == DOUBLE)
			value = exprs[i]->EvaluateDouble(vars);
		else
			value = exprs[i]->Evaluate(vars);
		if(value < min) min = value;
	}
	return min;
}

bool CRealMin::One_Result()
{
	for(int i = 0; i < no_exprs; i++)
		if(!exprs[i]->One_Result()) return false;
	return true;
}

std::string CRealMin::To_String()
{
	std::string str = "(rmin{";

	for(int i = 0; i < no_exprs; i++)
	{
		str = str + exprs[i]->To_String();
		if(i < no_exprs-1) str += ",";
	}
	
	return str + "})";
}

void CRealMin::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_REAL_MIN;
    write(fd, &expr_type, sizeof(int));
    //the information
    write(fd, &no_exprs, sizeof(int));
    for(int i = 0; i < no_exprs; i++)
    {
        exprs[i]->Write_Expression(fd);
    }
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CIte

CIte::CIte(CNormalExpression*condition, CExpression*trueCase, CExpression*falseCase)
{
	this->condition = condition;
	this->trueCase = trueCase;
	this->falseCase = falseCase;

	// "first operand must be boolean"
	if(condition->Get_Type() != BOOLEAN)
		throw "type error when constructing if-then-else: condition must be Boolean.";

	// "if second or third operand is boolean, both must be"
	if(trueCase->Get_Type() == BOOLEAN &&
		falseCase->Get_Type() == BOOLEAN)		//both boolean -> set type to BOOLEAN
		type = BOOLEAN;
	else if (trueCase->Get_Type() == BOOLEAN &&
		falseCase->Get_Type() != BOOLEAN)		//first boolean but other not -> throw exception
		throw "type error when constructing if-then-else: mixing Boolean types.";
	else if (trueCase->Get_Type() != BOOLEAN &&
		falseCase->Get_Type() == BOOLEAN)		//second boolean but other not -> throw exception
		throw "type error when constructing if-then-else: mixing Boolean types.";
	else if (trueCase->Get_Type() == DOUBLE ||
		falseCase->Get_Type() == BOOLEAN)		//either are doubles -> set type to DOUBLE
		type = DOUBLE;
	else																//otherwise -> set type to INTEGER
		type = INTEGER;			
}

CIte::~CIte()
{
	if(condition != NULL)
		delete condition;
	if(trueCase != NULL)
		delete trueCase;
	if(falseCase != NULL)
		delete falseCase;
}

int CIte::Get_Type()
{
	return type;
}

int CIte::Evaluate()
{
	if(condition->Evaluate())
		return trueCase->Evaluate();
	else
		return falseCase->Evaluate();
}

int CIte::Evaluate(int*vars)
{
	if(condition->Evaluate(vars))
		return trueCase->Evaluate(vars);
	else
		return falseCase->Evaluate(vars);
}

bool CIte::One_Result()
{
	return (condition->One_Result() && trueCase->One_Result() && falseCase->One_Result());
}

std::string CIte::To_String()
{
	std::string str = "(if"+condition->To_String()+"then"+trueCase->To_String()+"else"+falseCase->To_String()+")";
	return str;
}

void CIte::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_ITE;
    write(fd, &expr_type, sizeof(int));
    //the information
    condition->Write_Expression(fd);
    trueCase->Write_Expression(fd);
    falseCase->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CRealIte

CRealIte::CRealIte(CNormalExpression*condition, CExpression*trueCase, CExpression*falseCase)
{
	this->condition = condition;
	this->trueCase = trueCase;
	this->falseCase = falseCase;

	// "first operand must be boolean"
	if(condition->Get_Type() != BOOLEAN)
		throw "type error when constructing if-then-else: condition must be Boolean.";

	// "if second or third operand is boolean, both must be"
	if(trueCase->Get_Type() == BOOLEAN &&
		falseCase->Get_Type() == BOOLEAN)		//both boolean -> set type to BOOLEAN
		type = BOOLEAN;
	else if (trueCase->Get_Type() == BOOLEAN &&
		falseCase->Get_Type() != BOOLEAN)		//first boolean but other not -> throw exception
		throw "type error when constructing if-then-else: mixing Boolean types.";
	else if (trueCase->Get_Type() != BOOLEAN &&
		falseCase->Get_Type() == BOOLEAN)		//second boolean but other not -> throw exception
		throw "type error when constructing if-then-else: mixing Boolean types.";
	else if (trueCase->Get_Type() == DOUBLE ||
		falseCase->Get_Type() == DOUBLE)		//either are doubles -> set type to DOUBLE
		type = DOUBLE;
	else																//otherwise -> set type to INTEGER
		type = INTEGER;			
}

CRealIte::~CRealIte()
{
	if(condition != NULL)
		delete condition;
	if(trueCase != NULL)
		delete trueCase;
	if(falseCase != NULL)
		delete falseCase;
}

int CRealIte::Get_Type()
{
	return type;
}


double CRealIte::EvaluateDouble()
{
	if(condition->Evaluate())
		return trueCase->EvaluateDouble();
	else
		return falseCase->EvaluateDouble();
}

double CRealIte::EvaluateDouble(int*vars)
{
	if(condition->Evaluate(vars))
		return trueCase->EvaluateDouble(vars);
	else
		return falseCase->EvaluateDouble(vars);
}

bool CRealIte::One_Result()
{
	return (condition->One_Result() && trueCase->One_Result() && falseCase->One_Result());
}

std::string CRealIte::To_String()
{
	std::string str = "(ifreal"+condition->To_String()+"then"+trueCase->To_String()+"else"+falseCase->To_String()+")";
	return str;
}

void CRealIte::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_REAL_ITE;
    write(fd, &expr_type, sizeof(int));
    //the information
    condition->Write_Expression(fd);
    trueCase->Write_Expression(fd);
    falseCase->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CNormalPow


CNormalPow::CNormalPow(CNormalExpression * base, CNormalExpression * exp)
{
	this->lexpr = base; 
	this->rexpr = exp;

	
	if(lexpr->Get_Type() == BOOLEAN ||
		rexpr->Get_Type() == BOOLEAN) 
		throw "type error when constructing multplication: Boolean not accepted.";
}

CNormalPow::~CNormalPow()
{
	if(lexpr != NULL)
	delete lexpr;
	if(rexpr != NULL)
	delete rexpr;
}

int CNormalPow::Get_Type()
{
	return INTEGER;
}

int CNormalPow::Evaluate()
{
	return (int)pow((double)lexpr->Evaluate(), rexpr->Evaluate());
}

int CNormalPow::Evaluate(int* vars)
{
	return (int)pow((double)lexpr->Evaluate(vars), rexpr->Evaluate(vars));
}

bool CNormalPow::One_Result()
{
	return lexpr->One_Result() && rexpr->One_Result();
}

std::string CNormalPow::To_String()
{
	return "("+lexpr->To_String() + "n^" + rexpr->To_String()+")";
}

void CNormalPow::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_NORMAL_POW;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CRealPow


CRealPow::CRealPow(CExpression * base, CExpression * exp)
{
	this->lexpr = base; 
	this->rexpr = exp;

	
	if(lexpr->Get_Type() == BOOLEAN ||
		rexpr->Get_Type() == BOOLEAN) 
		throw "type error when constructing multplication: Boolean not accepted.";
}

CRealPow::~CRealPow()
{
	if(lexpr != NULL)
	delete lexpr;
	if(rexpr != NULL)
	delete rexpr;
}

int CRealPow::Get_Type()
{
	return DOUBLE;
}

double CRealPow::EvaluateDouble()
{
	return pow(lexpr->EvaluateDouble(), rexpr->EvaluateDouble());
}

double CRealPow::EvaluateDouble(int* vars)
{
	return pow(lexpr->EvaluateDouble(vars), rexpr->EvaluateDouble(vars));
}

bool CRealPow::One_Result()
{
	return lexpr->One_Result() && rexpr->One_Result();
}

std::string CRealPow::To_String()
{
	return "("+lexpr->To_String() + "r^" + rexpr->To_String()+")";
}

void CRealPow::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_REAL_POW;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CMod

CMod::CMod(CNormalExpression * left, CNormalExpression * right)
{
	this->lexpr = left; 
	this->rexpr = right;

	
	if(lexpr->Get_Type() == BOOLEAN ||
		rexpr->Get_Type() == BOOLEAN) 
		throw "type error when constructing multplication: Boolean not accepted.";
}

CMod::~CMod()
{
	if(lexpr != NULL)
	delete lexpr;
	if(rexpr != NULL)
	delete rexpr;
}

int CMod::Get_Type()
{
	return INTEGER;
}

int CMod::Evaluate()
{
	return (lexpr->Evaluate() % rexpr->Evaluate());
}

int CMod::Evaluate(int* vars)
{
	return (lexpr->Evaluate(vars) % rexpr->Evaluate(vars));
}

bool CMod::One_Result()
{
	return lexpr->One_Result() && rexpr->One_Result();
}

std::string CMod::To_String()
{
	return "("+lexpr->To_String() + "%" + rexpr->To_String()+")";
}

void CMod::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_MOD;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}


//Class methods for CLog

CLog::CLog(CExpression * left, CExpression * right)
{
	this->lexpr = left; 
	this->rexpr = right;
	if(lexpr->Get_Type() == BOOLEAN ||
		rexpr->Get_Type() == BOOLEAN) 
		throw "type error when constructing log: Boolean not accepted.";
}

CLog::~CLog()
{
	if(lexpr != NULL)
	delete lexpr;
	if(rexpr != NULL)
	delete rexpr;
}

int CLog::Get_Type()
{
	return DOUBLE;
}

double CLog::EvaluateDouble()
{
	double val, base, res;
	// Evaluate children
	if(lexpr->Get_Type() == INTEGER)
		val = lexpr->Evaluate();
	else
		val = lexpr->EvaluateDouble();
	if(rexpr->Get_Type() == INTEGER)
		base = rexpr->Evaluate();
	else
		base = rexpr->EvaluateDouble();
	// If base is <=0 or ==1 (or +Inf/NaN), then result is NaN
	if (base <= 0 || base == 1.0 || base == HUGE_VAL || base != base) { res = 0.0; res /= res; }
	// If arg is <0 or NaN, then result is NaN
	else if (val < 0 || val != val) { res = 0.0; res /= res; }
	// If arg is +Inf, then result is +Inf
	else if (val == HUGE_VAL) res = HUGE_VAL;
	// If arg is (positive/negative) 0, then result is -Inf
	else if (val == 0.0 || val == -0.0) res = -HUGE_VAL;
	// Default case: normal log
	else res = log(val) / log(base);
	
	return res;
}

double CLog::EvaluateDouble(int* vars)
{
	double val, base, res;
	// Evaluate children
	if(lexpr->Get_Type() == INTEGER)
		val = lexpr->Evaluate(vars);
	else
		val = lexpr->EvaluateDouble(vars);
	if(rexpr->Get_Type() == INTEGER)
		base = rexpr->Evaluate(vars);
	else
		base = rexpr->EvaluateDouble(vars);
	// If base is <0 or ==1 (or +Inf/NaN), then result is NaN
	if (base < 0 || base == 1.0 || base == HUGE_VAL || base != base) { res = 0.0; res /= res; }
	// If arg is <0 or NaN, then result is NaN
	else if (val < 0 || val != val) { res = 0.0; res /= res; }
	// If arg is +Inf, then result is +Inf
	else if (val == HUGE_VAL) res = HUGE_VAL;
	// If arg is (positive/negative) 0, then result is -Inf
	else if (val == 0.0 || val == -0.0) res = -HUGE_VAL;
	// Default case: normal log
	else res = log(val) / log(base);
	
	return res;
}

bool CLog::One_Result()
{
	return lexpr->One_Result() && rexpr->One_Result();
}

std::string CLog::To_String()
{
	return "log{"+lexpr->To_String() + "," + rexpr->To_String()+"}";
}

void CLog::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_LOG;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}


//Class methods for CNormalTimes


CNormalTimes::CNormalTimes(CNormalExpression * lexpr, CNormalExpression * rexpr)
{
	this->lexpr = lexpr; 
	this->rexpr = rexpr;

	
	if(lexpr->Get_Type() == BOOLEAN ||
		rexpr->Get_Type() == BOOLEAN) 
		throw "type error when constructing multplication: Boolean not accepted.";
}

CNormalTimes::~CNormalTimes()
{
	if(lexpr != NULL)
	delete lexpr;
	if(rexpr != NULL)
	delete rexpr;
}

int CNormalTimes::Get_Type()
{
	return INTEGER;
}

int CNormalTimes::Evaluate()
{
	return lexpr->Evaluate() * rexpr->Evaluate();
}

int CNormalTimes::Evaluate(int* vars)
{
	return lexpr->Evaluate(vars) * rexpr->Evaluate(vars);
}

bool CNormalTimes::One_Result()
{
	return lexpr->One_Result() && rexpr->One_Result();
}

std::string CNormalTimes::To_String()
{
	return "("+lexpr->To_String() + "n*" + rexpr->To_String()+")";
}

void CNormalTimes::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_NORMAL_TIMES;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}



//Class methods for CNormalPlus


CNormalPlus::CNormalPlus(CNormalExpression * lexpr, CNormalExpression * rexpr)
{
	this->lexpr = lexpr; 
	this->rexpr = rexpr;

	
	if(lexpr->Get_Type() == BOOLEAN ||
		rexpr->Get_Type() == BOOLEAN) 
		throw "type error when constructing addition: Boolean not accepted.";
}

CNormalPlus::~CNormalPlus()
{
	if(lexpr != NULL)
	delete lexpr;
	if(rexpr != NULL)
	delete rexpr;
}

int CNormalPlus::Get_Type()
{
	return INTEGER;
}

int CNormalPlus::Evaluate()
{
	return lexpr->Evaluate() + rexpr->Evaluate();
}

int CNormalPlus::Evaluate(int*vars)
{
	return lexpr->Evaluate(vars) + rexpr->Evaluate(vars);
}

bool CNormalPlus::One_Result()
{
	return lexpr->One_Result() && rexpr->One_Result();
}

std::string CNormalPlus::To_String()
{
	return "("+lexpr->To_String() + "n+" + rexpr->To_String()+")";
}

void CNormalPlus::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_NORMAL_PLUS;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CNormalPlus


CNormalMinus::CNormalMinus(CNormalExpression * lexpr, CNormalExpression * rexpr)
{
	this->lexpr = lexpr; 
	this->rexpr = rexpr;

	
	if(lexpr->Get_Type() == BOOLEAN ||
		rexpr->Get_Type() == BOOLEAN) 
		throw "type error when constructing subtraction: Boolean not accepted.";
}

CNormalMinus::~CNormalMinus()
{
	if(lexpr != NULL)
	delete lexpr;
	if(rexpr != NULL)
	delete rexpr;
}

int CNormalMinus::Get_Type()
{
	return INTEGER;
}

int CNormalMinus::Evaluate()
{
	return lexpr->Evaluate() - rexpr->Evaluate();
}

int CNormalMinus::Evaluate(int*vars)
{
	return lexpr->Evaluate(vars) - rexpr->Evaluate(vars);
}

bool CNormalMinus::One_Result()
{
	return lexpr->One_Result() && rexpr->One_Result();
}

std::string CNormalMinus::To_String()
{
	return "("+lexpr->To_String() + "n-" + rexpr->To_String()+")";
}

void CNormalMinus::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_NORMAL_MINUS;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}


//Class methods for CRealTimes


CRealTimes::CRealTimes(CExpression * lexpr, CExpression * rexpr)
{
	//cout << "CRealTimes constructor 0" << endl;
	this->lexpr = lexpr; 
	this->rexpr = rexpr;

	//cout << "CRealTimes constructor 1" << endl;
	
	if(lexpr->Get_Type() == BOOLEAN ||
		rexpr->Get_Type() == BOOLEAN) 
		throw "type error when constructing multplication: Boolean not accepted.";
	//cout << "CRealTimes constructor 2" << endl;
}

CRealTimes::~CRealTimes()
{
	if(owns_children)
	{
		if(lexpr != NULL)
	delete lexpr;
	if(rexpr != NULL)
	delete rexpr;
	}
}

int CRealTimes::Get_Type()
{
	return DOUBLE;
}

double CRealTimes::EvaluateDouble()
{
	double val1, val2;
	if(lexpr->Get_Type() == INTEGER)
		val1 = lexpr->Evaluate();
	else //tis a double
		val1 = lexpr->EvaluateDouble();
	if(rexpr->Get_Type() == INTEGER)
		val2 = rexpr->Evaluate();
	else
		val2 = rexpr->EvaluateDouble();
	return val1 * val2;
}

double CRealTimes::EvaluateDouble(int* vars)
{
	double val1, val2;
	if(lexpr->Get_Type() == INTEGER)
		val1 = lexpr->Evaluate(vars);
	else //tis a double
		val1 = lexpr->EvaluateDouble(vars);
	if(rexpr->Get_Type() == INTEGER)
		val2 = rexpr->Evaluate(vars);
	else
		val2 = rexpr->EvaluateDouble(vars);
	return val1 * val2;
}

bool CRealTimes::One_Result()
{
	//std::cout << "destructor for realtimes" << endl;
	return lexpr->One_Result() && rexpr->One_Result();
}

std::string CRealTimes::To_String()
{
	return "("+lexpr->To_String() + "r*" + rexpr->To_String()+")";
}



void CRealTimes::Set_Owns_Children(bool owns)
{
	this->owns_children = owns;
}

void CRealTimes::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_REAL_TIMES;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CRealDivide

CDivide::CDivide(CExpression * lexpr, CExpression * rexpr)
{
	this->lexpr = lexpr; 
	this->rexpr = rexpr;

	
	if(lexpr->Get_Type() == BOOLEAN ||
		rexpr->Get_Type() == BOOLEAN) 
		throw "type error when constructing divide: Boolean not accepted.";
}

CDivide::~CDivide()
{
	if(lexpr != NULL)
	delete lexpr;
	if(rexpr != NULL)
	delete rexpr;
}

int CDivide::Get_Type()
{
	return DOUBLE;
}

double CDivide::EvaluateDouble()
{
	double val1, val2;
	if(lexpr->Get_Type() == INTEGER)
		val1 = lexpr->Evaluate();
	else //tis a double
		val1 = lexpr->EvaluateDouble();
	if(rexpr->Get_Type() == INTEGER)
		val2 = rexpr->Evaluate();
	else
		val2 = rexpr->EvaluateDouble();
	return val1 / val2;
}

double CDivide::EvaluateDouble(int*vars)
{
	double val1, val2;
	if(lexpr->Get_Type() == INTEGER)
		val1 = lexpr->Evaluate(vars);
	else //tis a double
		val1 = lexpr->EvaluateDouble(vars);
	if(rexpr->Get_Type() == INTEGER)
		val2 = rexpr->Evaluate(vars);
	else
		val2 = rexpr->EvaluateDouble(vars);
	return val1 / val2;
}

bool CDivide::One_Result()
{
	return lexpr->One_Result() && rexpr->One_Result();
}

std::string CDivide::To_String()
{
	return "("+lexpr->To_String() + "/" + rexpr->To_String()+")";
}

void CDivide::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_DIVIDE;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CRealPlus

CRealPlus::CRealPlus(CExpression * lexpr, CExpression * rexpr)
{
	this->lexpr = lexpr; 
	this->rexpr = rexpr;

	
	if(lexpr->Get_Type() == BOOLEAN ||
		rexpr->Get_Type() == BOOLEAN) 
		throw "type error when constructing addition: Boolean not accepted.";
}

CRealPlus::~CRealPlus()
{
	if(lexpr != NULL)
	delete lexpr;
	if(rexpr != NULL)
	delete rexpr;
}

int CRealPlus::Get_Type()
{
	return DOUBLE;
}

double CRealPlus::EvaluateDouble()
{
	double val1, val2;
	if(lexpr->Get_Type() == INTEGER)
		val1 = lexpr->Evaluate();
	else //tis a double
		val1 = lexpr->EvaluateDouble();
	if(rexpr->Get_Type() == INTEGER)
		val2 = rexpr->Evaluate();
	else
		val2 = rexpr->EvaluateDouble();
	return val1 + val2;
}

double CRealPlus::EvaluateDouble(int*vars)
{
	double val1, val2;
	if(lexpr->Get_Type() == INTEGER)
		val1 = lexpr->Evaluate(vars);
	else //tis a double
		val1 = lexpr->EvaluateDouble(vars);
	if(rexpr->Get_Type() == INTEGER)
		val2 = rexpr->Evaluate(vars);
	else
		val2 = rexpr->EvaluateDouble(vars);
	return val1 + val2;
}

bool CRealPlus::One_Result()
{
	return lexpr->One_Result() && rexpr->One_Result();
}

std::string CRealPlus::To_String()
{
	return "("+lexpr->To_String() + "r+" + rexpr->To_String()+")";
}

void CRealPlus::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_REAL_PLUS;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}


//Class methods for CRealPlus

CRealMinus::CRealMinus(CExpression * lexpr, CExpression * rexpr)
{
	this->lexpr = lexpr; 
	this->rexpr = rexpr;

	
	if(lexpr->Get_Type() == BOOLEAN ||
		rexpr->Get_Type() == BOOLEAN) 
		throw "type error when constructing subtraction: Boolean not accepted.";
}

CRealMinus::~CRealMinus()
{
	if(lexpr != NULL)
	delete lexpr;
	if(rexpr != NULL)
	delete rexpr;
}

int CRealMinus::Get_Type()
{
	return DOUBLE;
}

double CRealMinus::EvaluateDouble()
{
	double val1, val2;
	if(lexpr->Get_Type() == INTEGER)
		val1 = lexpr->Evaluate();
	else //tis a double
		val1 = lexpr->EvaluateDouble();
	if(rexpr->Get_Type() == INTEGER)
		val2 = rexpr->Evaluate();
	else
		val2 = rexpr->EvaluateDouble();
	return val1 - val2;
}

double CRealMinus::EvaluateDouble(int*vars)
{
	double val1, val2;
	if(lexpr->Get_Type() == INTEGER)
		val1 = lexpr->Evaluate(vars);
	else //tis a double
		val1 = lexpr->EvaluateDouble(vars);
	if(rexpr->Get_Type() == INTEGER)
		val2 = rexpr->Evaluate(vars);
	else
		val2 = rexpr->EvaluateDouble(vars);
	return val1 - val2;
}

bool CRealMinus::One_Result()
{
	return lexpr->One_Result() && rexpr->One_Result();
}

std::string CRealMinus::To_String()
{
	return "("+lexpr->To_String() + "r-" + rexpr->To_String()+")";
}

void CRealMinus::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_REAL_MINUS;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}


//Class methods for CRelOp

CRelOp::CRelOp(CExpression * lexpr, CExpression * rexpr)
{
	this->lexpr = lexpr;
	this->rexpr = rexpr;
}

CRelOp::~CRelOp()
{
	//std::cout << "This is the destructor for RelOp, hope this works!!!!" << endl;
	if(lexpr != NULL)
	delete lexpr;
	if(rexpr != NULL)
	delete rexpr;
}
/*
int CRelOp::Evaluate()
{
	return 0;
}

*/
int CRelOp::Get_Type()
{
	return BOOLEAN;
}

bool CRelOp::One_Result()
{
	return lexpr->One_Result() && rexpr->One_Result();
}
/*
std::string CRelOp::To_String()
{
	return "error"; //this should never be returned
}
*/

//Class methods for CNormalEquals


CNormalEquals::CNormalEquals(CNormalExpression * lexpr, CNormalExpression * rexpr) : CRelOp(lexpr, rexpr)
{
}

int CNormalEquals::Evaluate()
{
	//std::cout << "relopn= evaluate() called" << std::endl;
	return lexpr->Evaluate() == rexpr->Evaluate();
}

int CNormalEquals::Evaluate(int*vars)
{
	//std::cout << "relopn= evaluate() called" << std::endl;
	return lexpr->Evaluate(vars) == rexpr->Evaluate(vars);
}

std::string CNormalEquals::To_String()
{
	return "("+lexpr->To_String()+"n="+rexpr->To_String()+")";
}

void CNormalEquals::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_NORMAL_EQUALS;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}


//Class methods for CRealEquals


CRealEquals::CRealEquals(CExpression * lexpr, CExpression * rexpr) : CRelOp(lexpr, rexpr)
{
}

int CRealEquals::Evaluate()
{
	return lexpr->EvaluateDouble() == rexpr->EvaluateDouble();
}

int CRealEquals::Evaluate(int*vars)
{
	return lexpr->EvaluateDouble(vars) == rexpr->EvaluateDouble(vars);
}

std::string CRealEquals::To_String()
{
	return "("+lexpr->To_String()+"r="+rexpr->To_String()+")";
}

void CRealEquals::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_REAL_EQUALS;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CNormalEquals

//Class methods for CNormalNotEquals

CNormalNotEquals::CNormalNotEquals(CNormalExpression * lexpr, CNormalExpression * rexpr) : CRelOp(lexpr, rexpr)
{
}

int CNormalNotEquals::Evaluate()
{
	return lexpr->Evaluate() != rexpr->Evaluate();
}

int CNormalNotEquals::Evaluate(int*vars)
{
	return lexpr->Evaluate(vars) != rexpr->Evaluate(vars);
}

std::string CNormalNotEquals::To_String()
{
	return "("+lexpr->To_String()+"n!="+rexpr->To_String()+")";
}

void CNormalNotEquals::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_NORMAL_NOT_EQUALS;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}


//Class methods for CRealEquals


CRealNotEquals::CRealNotEquals(CExpression * lexpr, CExpression * rexpr) : CRelOp(lexpr, rexpr)
{
}

int CRealNotEquals::Evaluate()
{
	return lexpr->EvaluateDouble() != rexpr->EvaluateDouble();
}

int CRealNotEquals::Evaluate(int*vars)
{
	return lexpr->EvaluateDouble(vars) != rexpr->EvaluateDouble(vars);
}

std::string CRealNotEquals::To_String()
{
	return "("+lexpr->To_String()+"r!="+rexpr->To_String()+")";
}

void CRealNotEquals::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_REAL_NOT_EQUALS;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CNormalLessThan

CNormalLessThan::CNormalLessThan(CNormalExpression * lexpr, CNormalExpression * rexpr) : CRelOp(lexpr, rexpr)
{
}

int CNormalLessThan::Evaluate()
{
	return lexpr->Evaluate() < rexpr->Evaluate();
}

int CNormalLessThan::Evaluate(int*vars)
{
	return lexpr->Evaluate(vars) < rexpr->Evaluate(vars);
}

std::string CNormalLessThan::To_String()
{
	return "("+lexpr->To_String()+"n<"+rexpr->To_String()+")";
}

void CNormalLessThan::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_NORMAL_LESS_THAN;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CRealEquals


CRealLessThan::CRealLessThan(CExpression * lexpr, CExpression * rexpr) : CRelOp(lexpr, rexpr)
{
}

int CRealLessThan::Evaluate()
{
	return lexpr->EvaluateDouble() < rexpr->EvaluateDouble();
}

int CRealLessThan::Evaluate(int*vars)
{
	
	return lexpr->EvaluateDouble(vars) < rexpr->EvaluateDouble(vars);
}

std::string CRealLessThan::To_String()
{
	return "("+lexpr->To_String()+"r<"+rexpr->To_String()+")";
}

void CRealLessThan::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_REAL_LESS_THAN;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CNormalLessThan

CNormalGreaterThan::CNormalGreaterThan(CNormalExpression * lexpr, CNormalExpression * rexpr) : CRelOp(lexpr, rexpr)
{
}

int CNormalGreaterThan::Evaluate()
{
	return lexpr->Evaluate() > rexpr->Evaluate();
}

int CNormalGreaterThan::Evaluate(int*vars)
{
	return lexpr->Evaluate(vars) > rexpr->Evaluate(vars);
}

std::string CNormalGreaterThan::To_String()
{
	//std::cout << "testing normalgreaterthan" << endl;
	return "("+lexpr->To_String()+"n>"+rexpr->To_String()+")";
}

void CNormalGreaterThan::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_NORMAL_GREATER_THAN;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CRealEquals


CRealGreaterThan::CRealGreaterThan(CExpression * lexpr, CExpression * rexpr) : CRelOp(lexpr, rexpr)
{
}

int CRealGreaterThan::Evaluate()
{
	return lexpr->EvaluateDouble() > rexpr->EvaluateDouble();
}

int CRealGreaterThan::Evaluate(int*vars)
{
	//cout << "When evaluated r<: lexpr = " << lexpr->EvaluateDouble(vars) << " rexpr = " << rexpr->EvaluateDouble() << endl;
	return lexpr->EvaluateDouble(vars) > rexpr->EvaluateDouble(vars);
}

std::string CRealGreaterThan::To_String()
{
	return "("+lexpr->To_String()+"r>"+rexpr->To_String()+")";
}

void CRealGreaterThan::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_REAL_GREATER_THAN;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CNormalLessThanEqual

CNormalLessThanEqual::CNormalLessThanEqual(CNormalExpression * lexpr, CNormalExpression * rexpr) : CRelOp(lexpr, rexpr)
{
}

int CNormalLessThanEqual::Evaluate()
{
	return lexpr->Evaluate() <= rexpr->Evaluate();
}

int CNormalLessThanEqual::Evaluate(int*vars)
{
	return lexpr->Evaluate(vars) <= rexpr->Evaluate(vars);
}

std::string CNormalLessThanEqual::To_String()
{
	return "("+lexpr->To_String()+"n<="+rexpr->To_String()+")";
}

void CNormalLessThanEqual::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_NORMAL_LESS_THAN_EQUAL;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}


//Class methods for CRealLessThanEqual


CRealLessThanEqual::CRealLessThanEqual(CExpression * lexpr, CExpression * rexpr) : CRelOp(lexpr, rexpr)
{
}

int CRealLessThanEqual::Evaluate()
{
	return lexpr->EvaluateDouble() <= rexpr->EvaluateDouble();
}

int CRealLessThanEqual::Evaluate(int*vars)
{
	return lexpr->EvaluateDouble(vars) <= rexpr->EvaluateDouble(vars);
}

std::string CRealLessThanEqual::To_String()
{
	return "("+lexpr->To_String()+"r<="+rexpr->To_String()+")";
}

void CRealLessThanEqual::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_REAL_LESS_THAN_EQUAL;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}

//Class methods for CNormalGreaterThanEqual

CNormalGreaterThanEqual::CNormalGreaterThanEqual(CNormalExpression * lexpr, CNormalExpression * rexpr) : CRelOp(lexpr, rexpr)
{
}

int CNormalGreaterThanEqual::Evaluate()
{
	return lexpr->Evaluate() >= rexpr->Evaluate();
}

int CNormalGreaterThanEqual::Evaluate(int*vars)
{
	return lexpr->Evaluate(vars) >= rexpr->Evaluate(vars);
}

std::string CNormalGreaterThanEqual::To_String()
{
	return "("+lexpr->To_String()+"n>="+rexpr->To_String()+")";
}

void CNormalGreaterThanEqual::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_NORMAL_GREATER_THAN_EQUAL;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}


//Class methods for CRealGreaterThanEqual


CRealGreaterThanEqual::CRealGreaterThanEqual(CExpression * lexpr, CExpression * rexpr) : CRelOp(lexpr, rexpr)
{
}

int CRealGreaterThanEqual::Evaluate()
{
	return lexpr->EvaluateDouble() >= rexpr->EvaluateDouble();
}

int CRealGreaterThanEqual::Evaluate(int*vars)
{
	return lexpr->EvaluateDouble(vars) >= rexpr->EvaluateDouble(vars);
}

std::string CRealGreaterThanEqual::To_String()
{
	return "("+lexpr->To_String()+"r>="+rexpr->To_String()+")";
}

void CRealGreaterThanEqual::Write_Expression(int fd)
{
    //the type of expression
    int expr_type = EXPR_REAL_GREATER_THAN_EQUAL;
    write(fd, &expr_type, sizeof(int));
    //the information
    lexpr->Write_Expression(fd);
    rexpr->Write_Expression(fd);
    //null byte to finish off expression
    write(fd, "\0", 1);
}


//Class methods for CNormalConstant (DEPRECATED)


CNormalConstant::CNormalConstant(int * index, int type)
{
	this->index = index;
	this->type = type;
}

int CNormalConstant::Get_Type()
{
	return type;
}

int CNormalConstant::Evaluate()
{
	return *index; //return the value pointed to by index
}

int CNormalConstant::Evaluate(int*vars)
{
	return *index; //return the value pointed to by index
}

bool CNormalConstant::One_Result()
{
	return *index != UNDEFINED_INT;
}

std::string CNormalConstant::To_String()
{
	return "";//"([nc@"+Int_To_String(((int)((int)index-(int)normal_constants)/sizeof(int)) - (int)((int)normal_constants/(sizeof(int))))+"])";
}

void CNormalConstant::Write_Expression(int fd)
{
    Write_Length_And_String("ERROR ERROR ERROR",fd);
}

//Class methods for CRealConstant (DEPRECATED)


CRealConstant::CRealConstant(double * index)
{
	this->index = index;
}

int CRealConstant::Get_Type()
{
	return DOUBLE;
}

double CRealConstant::EvaluateDouble()
{
	return *index; //return the value pointed to by index
}

double CRealConstant::EvaluateDouble(int*vars)
{
	return *index; //return the value pointed to by index
}

bool CRealConstant::One_Result()
{
	return *index != UNDEFINED_DOUBLE;
}

std::string CRealConstant::To_String()
{
	return "";//"([rc@"+Int_To_String(((int)((int)index-(int)real_constants)/sizeof(int)) - (int)((int)real_constants/(sizeof(int))))+"])";
}

void CRealConstant::Write_Expression(int fd)
{
    Write_Length_And_String("ERROR ERROR ERROR",fd);
}

//=============================================================================
//	Functions
//=============================================================================

CExpression* Read_Expression(int fd)
{
	char str_buf[256];
    //read expression type
    int expr_type;
    read(fd, &expr_type, sizeof(int));
    
    int buf_int =0;
    double value = 0.0;
    bool value_bool = false;
    CExpression* expr= NULL;
    CExpression* expr1= NULL;
    CExpression* expr2= NULL;
    CNormalExpression* n_expr= NULL;
    CNormalExpression* n_expr1= NULL;
    CNormalExpression* n_expr2= NULL;
    CNormalExpression** n_exprs= NULL;
    CExpression** exprs= NULL;

    switch(expr_type)
    {
        case EXPR_INTEGER_VAR:
            //read the index
            read(fd, &buf_int, sizeof(int));
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 050 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CIntegerVar(&state_variables[buf_int], buf_int);
        case EXPR_BOOLEAN_VAR:
            //read the index
            read(fd, &buf_int, sizeof(int));
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 051 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CBooleanVar(&state_variables[buf_int], buf_int);
        case EXPR_DOUBLE:
            //read the value
            read(fd, &value, sizeof(double));
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 052 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CDouble(value);
        case EXPR_INTEGER:
            //read the value
            read(fd, &buf_int, sizeof(int));
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 053 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CInteger(buf_int);
        case EXPR_BOOLEAN:
            //read the value
            read(fd, &value_bool, sizeof(bool));
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 054 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CBoolean(value_bool);
        case EXPR_CEIL:
            //Read the sub-expression
            expr = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 055 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CCeil(expr);
        case EXPR_FLOOR:
            //Read the sub-expression
            expr = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 056 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CFloor(expr);
        case EXPR_NOT:
            //Read the sub-expression
            n_expr = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 057 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CNot(n_expr);
        case EXPR_AND:
            //read the number of sub-expressions
            read(fd, &buf_int, sizeof(int));
            n_exprs = new CNormalExpression*[buf_int]; 
            for(int i = 0; i < buf_int; i++)
            {
                n_exprs[i] = Read_Normal_Expression(fd);
            }
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 058 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CAnd(n_exprs, buf_int);
        case EXPR_OR:
            //read the number of sub-expressions
            read(fd, &buf_int, sizeof(int));
            n_exprs = new CNormalExpression*[buf_int]; 
            for(int i = 0; i < buf_int; i++)
            {
                n_exprs[i] = Read_Normal_Expression(fd);
            }
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 059 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new COr(n_exprs, buf_int);
        case EXPR_ITE:
            //read off condition
            n_expr = Read_Normal_Expression(fd);
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 060 when importing binary file: expression not terminated correctly";
            }
            return new CIte(n_expr, expr1, expr2);
        case EXPR_REAL_ITE:
            //read off condition
            n_expr = Read_Normal_Expression(fd);
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 061 when importing binary file: expression not terminated correctly";
            }
            return new CRealIte(n_expr, expr1, expr2);
        case EXPR_NORMAL_MAX:
            //read the number of sub-expressions
            read(fd, &buf_int, sizeof(int));
            n_exprs = new CNormalExpression*[buf_int]; 
            for(int i = 0; i < buf_int; i++)
            {
                n_exprs[i] = Read_Normal_Expression(fd);
            }
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 062 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CNormalMax(n_exprs, buf_int);
        case EXPR_NORMAL_MIN:
            //read the number of sub-expressions
            read(fd, &buf_int, sizeof(int));
            n_exprs = new CNormalExpression*[buf_int]; 
            for(int i = 0; i < buf_int; i++)
            {
                n_exprs[i] = Read_Normal_Expression(fd);
            }
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 063 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CNormalMin(n_exprs, buf_int);
        case EXPR_REAL_MAX:
            //read the number of sub-expressions
            read(fd, &buf_int, sizeof(int));
            exprs = new CExpression*[buf_int]; 
            for(int i = 0; i < buf_int; i++)
            {
                exprs[i] = Read_Expression(fd);
            }
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 064 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CRealMax(exprs, buf_int);
        case EXPR_REAL_MIN:
            //read the number of sub-expressions
            read(fd, &buf_int, sizeof(int));
            exprs = new CExpression*[buf_int]; 
            for(int i = 0; i < buf_int; i++)
            {
                exprs[i] = Read_Expression(fd);
            }
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 065 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CRealMin(exprs, buf_int);
        case EXPR_NORMAL_POW:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 066 when importing binary file: expression not terminated correctly";
            }
            return new CNormalPow(n_expr1, n_expr2);
        case EXPR_REAL_POW:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 067 when importing binary file: expression not terminated correctly";
            }
            return new CRealPow(expr1, expr2);
        case EXPR_MOD:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 068 when importing binary file: expression not terminated correctly";
            }
            return new CMod(n_expr1, n_expr2);
        case EXPR_LOG:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 068.5 when importing binary file: expression not terminated correctly";
            }
            return new CLog(expr1, expr2);
        case EXPR_NORMAL_TIMES:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 069 when importing binary file: expression not terminated correctly";
            }
            return new CNormalTimes(n_expr1, n_expr2);
        case EXPR_NORMAL_PLUS:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 070 when importing binary file: expression not terminated correctly";
            }
            return new CNormalPlus(n_expr1, n_expr2);
        case EXPR_NORMAL_MINUS:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 071 when importing binary file: expression not terminated correctly";
            }
            return new CNormalMinus(n_expr1, n_expr2);
        case EXPR_REAL_TIMES:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 072 when importing binary file: expression not terminated correctly";
            }
            return new CRealTimes(expr1, expr2);
        case EXPR_DIVIDE:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 073 when importing binary file: expression not terminated correctly";
            }
            return new CDivide(expr1, expr2);
        case EXPR_REAL_PLUS:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 074 when importing binary file: expression not terminated correctly";
            }
            return new CRealPlus(expr1, expr2);
        case EXPR_REAL_MINUS:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 075 when importing binary file: expression not terminated correctly";
            }
            return new CRealMinus(expr1, expr2);
        case EXPR_NORMAL_EQUALS:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 076 when importing binary file: expression not terminated correctly";
            }
            return new CNormalEquals(n_expr1, n_expr2);
        case EXPR_REAL_EQUALS:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 077 when importing binary file: expression not terminated correctly";
            }
            return new CRealEquals(expr1, expr2);
        case EXPR_NORMAL_NOT_EQUALS:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 078 when importing binary file: expression not terminated correctly";
            }
            return new CNormalNotEquals(n_expr1, n_expr2);
        case EXPR_REAL_NOT_EQUALS:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 079 when importing binary file: expression not terminated correctly";
            }
            return new CRealNotEquals(expr1, expr2);
        case EXPR_NORMAL_LESS_THAN:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 080 when importing binary file: expression not terminated correctly";
            }
            return new CNormalLessThan(n_expr1, n_expr2);
        case EXPR_REAL_LESS_THAN:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 081 when importing binary file: expression not terminated correctly";
            }
            return new CRealLessThan(expr1, expr2);
        case EXPR_NORMAL_GREATER_THAN:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 082 when importing binary file: expression not terminated correctly";
            }
            return new CNormalGreaterThan(n_expr1, n_expr2);
        case EXPR_REAL_GREATER_THAN:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 083 when importing binary file: expression not terminated correctly";
            }
            return new CRealGreaterThan(expr1, expr2);
        case EXPR_NORMAL_LESS_THAN_EQUAL:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 084 when importing binary file: expression not terminated correctly";
            }
            return new CNormalLessThanEqual(n_expr1, n_expr2);
        case EXPR_REAL_LESS_THAN_EQUAL:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 085 when importing binary file: expression not terminated correctly";
            }
            return new CRealLessThanEqual(expr1, expr2);
        case EXPR_NORMAL_GREATER_THAN_EQUAL:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 086 when importing binary file: expression not terminated correctly";
            }
            return new CNormalGreaterThanEqual(n_expr1, n_expr2);
        case EXPR_REAL_GREATER_THAN_EQUAL:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 087 when importing binary file: expression not terminated correctly";
            }
            return new CRealGreaterThanEqual(expr1, expr2);
        default:
            throw "Error when importing binary file: invalid expression type found";
    }
    

}

CNormalExpression* Read_Normal_Expression(int fd)
{
    char str_buf[256];
    //read expression type
    int expr_type;
    read(fd, &expr_type, sizeof(int));
    
    int buf_int = 0;
    double value = 0.0;
    bool value_bool = false;
    CExpression* expr = NULL;
    CExpression* expr1= NULL;
    CExpression* expr2= NULL;
    CNormalExpression* n_expr= NULL;
    CNormalExpression* n_expr1= NULL;
    CNormalExpression* n_expr2= NULL;
    CNormalExpression** n_exprs= NULL;
    CExpression** exprs= NULL;

    switch(expr_type)
    {
        case EXPR_INTEGER_VAR:
            //read the index
            read(fd, &buf_int, sizeof(int));
            //read off null byte

			//throw "testing testing 123";
			char test[256];
            read(fd, &test, 1);//str_buf, 1);
            if(strcmp(test, "") != 0)
            {
                throw "Error 010 when importing binary file: expression not terminated correctly";
            }
			
            //create the expression
			//cout << "before error line" << endl;
            return new CIntegerVar(&state_variables[buf_int], buf_int);
        case EXPR_BOOLEAN_VAR:
            //read the index
            read(fd, &buf_int, sizeof(int));
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 011 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CBooleanVar(&state_variables[buf_int], buf_int);
        case EXPR_INTEGER:
            //read the value
            read(fd, &buf_int, sizeof(int));
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 012 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CInteger(buf_int);
        case EXPR_BOOLEAN:
            //read the value
            read(fd, &value_bool, sizeof(bool));
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 013 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CBoolean(value_bool);
        case EXPR_CEIL:
            //Read the sub-expression
            expr = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 014 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CCeil(expr);
        case EXPR_FLOOR:
            //Read the sub-expression
            expr = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 015 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CFloor(expr);
        case EXPR_NOT:
            //Read the sub-expression
            n_expr = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 017 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CNot(n_expr);
        case EXPR_AND:
            //read the number of sub-expressions
            read(fd, &buf_int, sizeof(int));
            n_exprs = new CNormalExpression*[buf_int]; 
            for(int i = 0; i < buf_int; i++)
            {
                n_exprs[i] = Read_Normal_Expression(fd);
            }
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 018 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CAnd(n_exprs, buf_int);
        case EXPR_OR:
            //read the number of sub-expressions
            read(fd, &buf_int, sizeof(int));
            n_exprs = new CNormalExpression*[buf_int]; 
            for(int i = 0; i < buf_int; i++)
            {
                n_exprs[i] = Read_Normal_Expression(fd);
            }
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 019 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new COr(n_exprs, buf_int);
        case EXPR_ITE:
            //read off condition
            n_expr = Read_Normal_Expression(fd);
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 020 when importing binary file: expression not terminated correctly";
            }
            return new CIte(n_expr, expr1, expr2);
        case EXPR_NORMAL_MAX:
            //read the number of sub-expressions
            read(fd, &buf_int, sizeof(int));
            n_exprs = new CNormalExpression*[buf_int]; 
            for(int i = 0; i < buf_int; i++)
            {
                n_exprs[i] = Read_Normal_Expression(fd);
            }
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 021 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CNormalMax(n_exprs, buf_int);
        case EXPR_NORMAL_MIN:
            //read the number of sub-expressions
            read(fd, &buf_int, sizeof(int));
            n_exprs = new CNormalExpression*[buf_int]; 
            for(int i = 0; i < buf_int; i++)
            {
                n_exprs[i] = Read_Normal_Expression(fd);
            }
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 022 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CNormalMin(n_exprs, buf_int);
        case EXPR_NORMAL_POW:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 023 when importing binary file: expression not terminated correctly";
            }
            return new CNormalPow(n_expr1, n_expr2);
        case EXPR_MOD:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 024 when importing binary file: expression not terminated correctly";
            }
            return new CMod(n_expr1, n_expr2);
        case EXPR_NORMAL_TIMES:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 025 when importing binary file: expression not terminated correctly";
            }
            return new CNormalTimes(n_expr1, n_expr2);
        case EXPR_NORMAL_PLUS:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 026 when importing binary file: expression not terminated correctly";
            }
            return new CNormalPlus(n_expr1, n_expr2);
        case EXPR_NORMAL_MINUS:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 027 when importing binary file: expression not terminated correctly";
            }
            return new CNormalMinus(n_expr1, n_expr2);
        case EXPR_NORMAL_EQUALS:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 028 when importing binary file: expression not terminated correctly";
            }
            return new CNormalEquals(n_expr1, n_expr2);
        case EXPR_NORMAL_NOT_EQUALS:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 029 when importing binary file: expression not terminated correctly";
            }
            return new CNormalNotEquals(n_expr1, n_expr2);
        case EXPR_NORMAL_LESS_THAN:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 030 when importing binary file: expression not terminated correctly";
            }
            return new CNormalLessThan(n_expr1, n_expr2);
        case EXPR_NORMAL_GREATER_THAN:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 031 when importing binary file: expression not terminated correctly";
            }
            return new CNormalGreaterThan(n_expr1, n_expr2);
        case EXPR_NORMAL_LESS_THAN_EQUAL:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 032 when importing binary file: expression not terminated correctly";
            }
            return new CNormalLessThanEqual(n_expr1, n_expr2);
        case EXPR_NORMAL_GREATER_THAN_EQUAL:
            //read 2 sub-expressions
            n_expr1 = Read_Normal_Expression(fd);
            n_expr2 = Read_Normal_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 034 when importing binary file: expression not terminated correctly";
            }
            return new CNormalGreaterThanEqual(n_expr1, n_expr2);
        default:
            throw "Error when importing binary file: invalid normal expression type found";
    }
}

CRealExpression* Read_Real_Expression(int fd)
{
	char str_buf[256];
    //read expression type
    int expr_type;
    read(fd, &expr_type, sizeof(int));
    
    int buf_int;
    double value;
    bool value_bool;
    CExpression* expr;
    CExpression* expr1;
    CExpression* expr2;
    CNormalExpression* n_expr;
    CNormalExpression* n_expr1;
    CNormalExpression* n_expr2;
    CNormalExpression** n_exprs;
    CExpression** exprs;

    //cout << "Attempting to read real expression of type " << expr_type << endl;

    switch(expr_type)
    {
        case EXPR_DOUBLE:
            //read the value
            read(fd, &value, sizeof(double));
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 035 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CDouble(value);
        case EXPR_REAL_ITE:
            //read off condition
            n_expr = Read_Normal_Expression(fd);
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 036 when importing binary file: expression not terminated correctly";
            }
            return new CRealIte(n_expr, expr1, expr2);
        case EXPR_REAL_MAX:
            //read the number of sub-expressions
            read(fd, &buf_int, sizeof(int));
            exprs = new CExpression*[buf_int]; 
            for(int i = 0; i < buf_int; i++)
            {
                exprs[i] = Read_Expression(fd);
            }
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 037 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CRealMax(exprs, buf_int);
        case EXPR_REAL_MIN:
            //read the number of sub-expressions
            read(fd, &buf_int, sizeof(int));
            exprs = new CExpression*[buf_int]; 
            for(int i = 0; i < buf_int; i++)
            {
                exprs[i] = Read_Expression(fd);
            }
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 038 when importing binary file: expression not terminated correctly";
            }
            //create the expression
            return new CRealMin(exprs, buf_int);
        case EXPR_LOG:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 038.5 when importing binary file: expression not terminated correctly";
            }
            return new CLog(expr1, expr2);
        case EXPR_REAL_POW:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 039 when importing binary file: expression not terminated correctly";
            }
            return new CRealPow(expr1, expr2);
        case EXPR_REAL_TIMES:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 040 when importing binary file: expression not terminated correctly";
            }
            return new CRealTimes(expr1, expr2);
        case EXPR_DIVIDE:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 041 when importing binary file: expression not terminated correctly";
            }
            return new CDivide(expr1, expr2);
        case EXPR_REAL_PLUS:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 042 when importing binary file: expression not terminated correctly";
            }
            return new CRealPlus(expr1, expr2);
        case EXPR_REAL_MINUS:
            //read 2 sub-expressions
            expr1 = Read_Expression(fd);
            expr2 = Read_Expression(fd);
            //read off null byte
            read(fd, &str_buf, 1);
            if(strcmp(str_buf, "") != 0)
            {
                throw "Error 043 when importing binary file: expression not terminated correctly";
            }
            return new CRealMinus(expr1, expr2);
       default:
            throw "Error when importing binary file: invalid real expression type found";
    }
}
