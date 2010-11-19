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

package userinterface.model.graphicModel;

import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;

public class GraphicSelection implements ClipboardOwner, Transferable, Serializable
{
    SelectionPair selection;
    
    
    /** Creates a new instance of GraphicSelection 
     *  The constructor creates a selection according
     *  to what is selected in theModel.
     */
    public GraphicSelection(ModuleModel theModel)
    {
        ArrayList states = new ArrayList();
        ArrayList transitions = new ArrayList();
        double topX = Double.POSITIVE_INFINITY, topY = Double.POSITIVE_INFINITY;
        for(int i = 0; i < theModel.getNumStates(); i++)
        {
            State st = theModel.getState(i);
            if(st.isSelected())
            {
                if(st.getX() < topX)topX = st.getX();
                if(st.getY() < topY) topY = st.getY();
                states.add(new StateSelect(st, i));
            }
        }
        
        for(int i = 0; i < theModel.getNumTransitions(); i++)
        {
            Transition t = theModel.getTransition(i);
            if(t.isSelected() && t.getFrom().isSelected() && t.getTo().isSelected())
            {
                //Find the from and to StateSelect objects
                StateSelect from = null, to = null;
                for(int j = 0; j < theModel.getNumStates(); j++)
                {
                    State st = theModel.getState(j);
                    if(st == t.getFrom())
                    {
                        //System.out.println("frompart1"+j);
                        for(int k = 0; k < states.size(); k++)
                        {
                            StateSelect curr = (StateSelect)states.get(k);
                            //System.out.println("beforefrom");
                            if(curr.id == j) 
                            {
                                from = curr;
                                //System.out.println("fromdone " +j);
                            }
                        }
                    }
                    if(st == t.getTo())
                    {
                        //System.out.println("topart1"+j);
                        for(int k = 0; k < states.size(); k++)
                        {
                            StateSelect curr = (StateSelect)states.get(k);
                            //System.out.println("beforeto");
                            //System.out.println("j = "+j+" curr.id = "+curr.id);
                            if(curr.id == j) 
                            {
                                to = curr;
                                //System.out.println("todone" + j);
                            }
                        }
                    }
                }
                
                if(from != null && to != null)
                {
                    TransitionSelect ts = new TransitionSelect(t,from, to);
                    
                    //Do nails
                    NailSelect[] ns = new NailSelect[t.getNumNails()];
                    for(int j = 0; j < t.getNumNails(); j++)
                        ns[j] = new NailSelect(t.getNail(j));
                    
                    ts.nails = ns;
                    
                    transitions.add(ts);
                    //System.out.println("transition copy successful");
                }
                else
                {
                    //System.out.println("problem in transition");
                }
            }
                
        }
        
        selection = new SelectionPair(states, transitions);
        selection.topX = topX;
        selection.topY = topY;
        
        //Do Transitions
    }
    
    public GraphicSelection(SelectionPair pair)
    {
        this.selection = pair;
    }
    
   
    
    public void lostOwnership(Clipboard clipboard, Transferable contents)
    {
    }
    
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, java.io.IOException
    {
        if(!isDataFlavorSupported(flavor))
        {
            throw new UnsupportedFlavorException(flavor);
        }
        if(DataFlavor.stringFlavor.equals(flavor))
            return selection.toString();
        
        return selection;
        
    }
    
    public DataFlavor[] getTransferDataFlavors()
    {
        DataFlavor[] flavors = new DataFlavor[2];
        Class type = selection.getClass();
        String mimeType = "application/x-java-serialized-object;class="+type.getName();
        try
        {
            flavors[0] = new DataFlavor(mimeType);
            flavors[1] = DataFlavor.stringFlavor;
            return flavors;
        }
        catch(ClassNotFoundException ex)
        {
            return new DataFlavor[0];
        }
    }
    
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        return
            DataFlavor.stringFlavor.equals(flavor)
            || "application".equals(flavor.getPrimaryType())
            && "x-java-serialized-object".equals(flavor.getSubType())
            && flavor.getRepresentationClass().isAssignableFrom(selection.getClass());
    }
    
    class StateSelect implements Serializable
    {
        double x,y;
        String comment;
        double commentX, commentY;
        boolean initial;
        boolean decision;
        int id;
        
        public StateSelect(State st, int id)
        {
            this.id = id;
            this.x = st.getX();
            this.y = st.getY();
            this.comment = (String)st.getComment()+"";
            this.commentX = st.getCommentLabel().getOffsetX();
            this.commentY = st.getCommentLabel().getOffsetY();
            this.initial = st.isInitial();
            this.decision = st instanceof Decision;
        }
    }
    
    class TransitionSelect implements Serializable
    {
        StateSelect from, to;
        NailSelect[] nails;
        boolean prob;
        String assignment;
        double assignmentX, assignmentY;
        String guard;
        double guardX, guardY;
        String probability;
        double probabilityX, probabilityY;
        String sync;
        double syncX, syncY;
        
        public TransitionSelect(Transition t, StateSelect from, StateSelect to)
        {
            this.from = from;
            this.to = to;
            this.prob = t instanceof ProbTransition;
            this.assignment = t.getAssignmentLabel().getString()+""; //+"" to ensure clone
            this.assignmentX = t.getAssignmentLabel().getOffsetX();
            this.assignmentY = t.getAssignmentLabel().getOffsetY();
            this.guard = t.getGuardLabel().getString()+"";
            this.guardX = t.getGuardLabel().getOffsetX();
            this.guardY = t.getGuardLabel().getOffsetY();
            this.probability = t.getProbabilityLabel().getString()+"";
            this.probabilityX = t.getProbabilityLabel().getOffsetX();
            this.probabilityY = t.getProbabilityLabel().getOffsetY();
            this.sync = t.getSyncLabel().getString()+"";
            this.syncX = t.getSyncLabel().getOffsetX();
            this.syncY = t.getSyncLabel().getOffsetY();
            
            nails = new NailSelect[t.getNumNails()];
            for(int i = 0; i < t.getNumNails(); i++)
            {
                nails[i] = new NailSelect(t.getNail(i));
            }
        }
    }
    
    class NailSelect implements Serializable
    {
        double x,y;
        
        public NailSelect(Nail n)
        {
            this.x = n.getX();
            this.y = n.getY();
        }
    }
    
    class SelectionPair implements Serializable
    {
        StateSelect[] states;
        TransitionSelect[] transitions;
        double topX, topY;
        double offsetX = 0, offsetY = 0;
        
        public SelectionPair(ArrayList states, ArrayList transitions)
        {
            this.states = new StateSelect[states.size()];
            for(int i = 0; i < states.size(); i++)
                this.states[i] = (StateSelect)states.get(i);
            this.transitions = new TransitionSelect[transitions.size()];
            for(int i = 0; i < transitions.size(); i++)
                this.transitions[i] = (TransitionSelect)transitions.get(i);
            //System.out.println("Selection pair name: "+getClass().getName());
        }
        
        public String toString()
        {
            return "Selection from graphic model editor";
        }
    }
    
    
   
    
}
