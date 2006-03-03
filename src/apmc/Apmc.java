//==============================================================================
//	
//	Copyright (c) 2002-2005, Thomas Hérault, Frédéric Magniett, Sylvain Peyronnet, Dave Parker
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

package apmc;
import java.util.Vector;
import parser.*;
import prism.PrismLog;

public class Apmc {

	// this is just the stub so apmc is disabled
	public static boolean isEnabled() { return false; }
	
    public static final int CST =1;
    public static final int ID  =2;
    public static final int NOT   =11;
    public static final int NEXT  =12;
    public static final int CEIL  =13;
    public static final int FLOOR =14;
    public static final int AND     =21;
    public static final int OR      =22;
    public static final int EQUAL   =23;
    public static final int NEQ     =24;
    public static final int LESS    =25;
    public static final int GREATER =26;
    public static final int LEQ     =27;
    public static final int GEQ     =28;
    public static final int PLUS    =29;
    public static final int MINUS   =30;
    public static final int TIMES   =31;
    public static final int DIV     =32;
    public static final int UNTIL   =33;
    public static final int MIN     =34;
    public static final int MAX     =35;

    public native void newConstdef(String name, int value);
    public native void newDoubledef(String name, float value);
    public native void newVardef(String name, int min, int max, int initial_value);
    public native void newFormula(String name, int expr);
    public native int  createEmptyModule(String name);
    public native void addLineToModule(int mref, int lref);
    public native int  createRule(String synch, int cond, int act);
    public native int  createDetAct(int aff);
    public native int  createEmptyProbAct();
    public native void addProbAct(int pa, int prob, int aff);
    public native int  createAffectation(String name, int exp, int aff);
    public native void displayPM();
    public native int newUnaryOperand(int op, int arg);
    public native int newBinaryOperand(int op, int argleft, int argright);
    public native int newIdent(String name);
    public native int newConst(int value);
    public native int newConst(double value);
    public native int newBoundedUntil(int lBound, int uBound, int argleft, int argright);
    public native void displayFormula(int f, boolean flat);
    public native void setInitialState(Vector names, Vector values);
    public native Vector compilerList();
    public native int newCompiler(String filename, int strategy, int expr);
    public Apmc() {}
	public Apmc(PrismLog log) {}
    public void addFormula(int f) {}
    public void registerProb(String Op, Double prob) throws ApmcException {}
    public void setEvaluateContext(Values c, Values v, LabelList ll) {}
    public Values getConstantValues() { return null; }
    public Values getVarValues() { return null; }
    public LabelList getLabelList() { return null; }
    public void cleanup() {}
    public Object runLocalClient(int pathlen, int nbpath, int strategy) throws ApmcException { return null; }
    public int computeNumPaths(double approx, double confidence) { return 0; }
    public void setInitialState(Values values) throws ApmcException {}
}
