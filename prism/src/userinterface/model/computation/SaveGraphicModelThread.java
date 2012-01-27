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

package userinterface.model.computation;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

import prism.*;
import parser.type.*;
import userinterface.*;
import userinterface.model.*;
import userinterface.model.graphicModel.*;
import userinterface.util.*;

public class SaveGraphicModelThread extends Thread
{
    private GUIModelEditor theEditor;
    private GUIMultiModelHandler handler;
    private File f;
    private GUIPlugin plug;
    private boolean error;
    
    /** Creates a new instance of SaveGraphicModelThread */
    public SaveGraphicModelThread(File f, GUIMultiModelHandler handler, GUIModelEditor theEditor)
    {
        this.theEditor = theEditor;
        this.handler = handler;
        this.f = f;
        error = false;
        plug = handler.getGUIPlugin();
    }
    
    public void run()
    {
        try
        {
            SwingUtilities.invokeAndWait(new Runnable()
            {
                public void run()
                {
                    plug.startProgress();
                    plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, plug));
                    plug.setTaskBarText("Saving model...");
                }
            });
            if(!(theEditor instanceof GUIGraphicModelEditor)) throw new PrismException("Wrong model type");
            GUIGraphicModelEditor editor = (GUIGraphicModelEditor) theEditor;
            
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element gmoRoot = doc.createElement("gmo");
            String modTypeStr = editor.getModelType().toString().toLowerCase();
            gmoRoot.setAttribute("type", modTypeStr);
            gmoRoot.setAttribute("filename", f.getPath());
            gmoRoot.setAttribute("autolayout", ""+editor.isAutolayout());
            
            
            //Constants
            {
                Element cons = doc.createElement("constants");
                ArrayList consNames = editor.getEditableConstantNames();
                ArrayList consValues = editor.getEditableConstantValues();
                ArrayList<Type> consTypes = editor.getEditableConstantTypes();
                
                for(int i = 0; i < consTypes.size(); i++)
                {
                    Object value = consValues.get(i);
                    String name = (String)consNames.get(i);
                    Element newCons = null;
                    
                    newCons = doc.createElement(consTypes.get(i).getTypeString() + "Constant");
                    newCons.setAttribute("name", name);
                    if(value != null) {
                        newCons.setAttribute("value", (String)value);
                    }
                    if(newCons != null) cons.appendChild(newCons);
                }
                gmoRoot.appendChild(cons);
            }
            //Globals
            {
                Element decl = doc.createElement("globals");
                ArrayList declNames = editor.getEditableGlobalNames();
                ArrayList declMins  = editor.getEditableGlobalMins();
                ArrayList declMaxs  = editor.getEditableGlobalMaxs();
                ArrayList declInits = editor.getEditableGlobalInits();
                ArrayList<Type> declTypes = editor.getEditableGlobalTypes();
                
                for(int i = 0; i < declTypes.size(); i++)
                {
                    Type type = declTypes.get(i);
                    String name = (String)declNames.get(i);
                    Element newDecl = null;
                    Object init;
                    if (type instanceof TypeInt) {
                            newDecl = doc.createElement("variable");
                            newDecl.setAttribute("name", name);
                            newDecl.setAttribute("min", (String)declMins.get(i));
                            newDecl.setAttribute("max", (String)declMaxs.get(i));
                            init = declInits.get(i);
                            if(init != null)
                                newDecl.setAttribute("init", (String)declInits.get(i));
                    }
                    else if (type instanceof TypeBool) {
                            newDecl = doc.createElement("boolVariable");
                            newDecl.setAttribute("name", name);
                            init = declInits.get(i);
                            if(init != null)
                                newDecl.setAttribute("init", (String)declInits.get(i));
                    }
                    if(newDecl != null) decl.appendChild(newDecl);
                }
                gmoRoot.appendChild(decl);
            }
            //System Information
            {
                Element sysInfo = doc.createElement("systemDescription");
                String text = editor.getTextEditorString();
                Text textNode = doc.createTextNode(text);
                sysInfo.appendChild(textNode);
                gmoRoot.appendChild(sysInfo);
            }
            
            ModuleModel[] theModules = editor.getModuleArray();
            //Modules
            {
                for(int i = 0; i < theModules.length; i++)
                {
                    ArrayList decisions = new ArrayList();
                    ArrayList probTrans = new ArrayList();
                    ArrayList branchTrans = new ArrayList();
                    Element module = doc.createElement("module");
                    module.setAttribute("name", theModules[i].getCorrespondingModuleNode().getName());
                    
                    java.awt.Rectangle pos = theModules[i].getContainer().getRectangle();
                    
                    module.setAttribute("x", ""+pos.x);
                    module.setAttribute("y", ""+pos.y);
                    module.setAttribute("width", ""+pos.width);
                    module.setAttribute("height", ""+pos.height);
                    module.setAttribute("zoom", "" + theModules[i].getZoom());
                    
                    //States
                    for(int j = 0; j < theModules[i].getNumStates(); j++)
                    {
                        userinterface.model.graphicModel.State st = theModules[i].getState(j);
                        if(st instanceof Decision)
                        {
                            decisions.add(st);
                        }
                        else
                        {
                            Element state = doc.createElement("state");
                            state.setAttribute("id", ""+j);
                            state.setAttribute("initial", ""+st.isInitial());
                            //position
                            double posX = st.getX();
                            double posY = st.getY();
                            {
                                Element position = doc.createElement("position");
                                position.setAttribute("x", ""+posX);
                                position.setAttribute("y", ""+posY);
                                state.appendChild(position);
                            }
//                            //invariant
//                            String invariant = st.getInvarientLabel().getString();
//                            if(!invariant.equals(""))
//                            {
//                                Element inva = doc.createElement("invariant");
//                                inva.setAttribute("name", invariant);
//                                double invX = st.getInvarientLabel().getOffsetX();
//                                double invY = st.getInvarientLabel().getOffsetY();
//                                {
//                                    Element position = doc.createElement("position");
//                                    position.setAttribute("x", ""+invX);
//                                    position.setAttribute("y", ""+invY);
//                                    inva.appendChild(position);
//                                }
//                                state.appendChild(inva);
//                            }
                            //state name
                            String sName = st.getCommentLabel().getString();
                            if(!sName.equals(""))
                            {
                                Element name = doc.createElement("sName");
                                name.setAttribute("name", sName);
                                double namX = st.getCommentLabel().getOffsetX();
                                double namY = st.getCommentLabel().getOffsetY();
                                {
                                    Element position = doc.createElement("position");
                                    position.setAttribute("x", ""+namX);
                                    position.setAttribute("y", ""+namY);
                                    name.appendChild(position);
                                }
                                state.appendChild(name);
                            }
                            
                            module.appendChild(state);
                        }
                    }
                    for(int j = 0; j < theModules[i].getNumTransitions(); j++)
                    {
                        Transition tr = theModules[i].getTransition(j);
                        if(tr instanceof ProbTransition)
                        {
                            branchTrans.add(tr);
                        }
                        else if(tr.getTo() instanceof Decision)
                        {
                            probTrans.add(tr);
                        }
                        else // must be a normal transition
                        {
                            Element transition = doc.createElement("transition");
                            int from = theModules[i].getStateIndex(tr.getFrom());
                            int to   = theModules[i].getStateIndex(tr.getTo());
                            transition.setAttribute("from", ""+from);
                            transition.setAttribute("to", ""+to);
                            //guard
                            String guard = tr.getGuardLabel().getString();
                            if(!guard.equals(""))
                            {
                                Element guar = doc.createElement("guard");
                                guar.setAttribute("value", guard);
                                double guardX = tr.getGuardLabel().getOffsetX();
                                double guardY = tr.getGuardLabel().getOffsetY();
                                {
                                    Element position = doc.createElement("position");
                                    position.setAttribute("x",""+guardX);
                                    position.setAttribute("y",""+guardY);
                                    guar.appendChild(position);
                                }
                                transition.appendChild(guar);
                            }
                            //sync
                            String sync = tr.getSyncLabel().getString();
                            if(!sync.equals(""))
                            {
                                Element syn = doc.createElement("sync");
                                syn.setAttribute("value", sync);
                                double syncX = tr.getSyncLabel().getOffsetX();
                                double syncY = tr.getSyncLabel().getOffsetY();
                                {
                                    Element position = doc.createElement("position");
                                    position.setAttribute("x",""+syncX);
                                    position.setAttribute("y",""+syncY);
                                    syn.appendChild(position);
                                }
                                transition.appendChild(syn);
                            }
                            //assign
                            String assign = tr.getAssignmentLabel().getString();
                            if(!assign.equals(""))
                            {
                                Element ass = doc.createElement("assign");
                                ass.setAttribute("value", assign);
                                double assignX = tr.getAssignmentLabel().getOffsetX();
                                double assignY = tr.getAssignmentLabel().getOffsetY();
                                {
                                    Element position = doc.createElement("position");
                                    position.setAttribute("x",""+assignX);
                                    position.setAttribute("y",""+assignY);
                                    ass.appendChild(position);
                                }
                                transition.appendChild(ass);
                            }
                            //prob
                            String prob = tr.getProbabilityLabel().getString();
                            if(!prob.equals(""))
                            {
                                Element trPr = doc.createElement("tranProb");
                                trPr.setAttribute("value", prob);
                                double probX = tr.getProbabilityLabel().getOffsetX();
                                double probY = tr.getProbabilityLabel().getOffsetY();
                                {
                                    Element position = doc.createElement("position");
                                    position.setAttribute("x",""+probX);
                                    position.setAttribute("y",""+probY);
                                    trPr.appendChild(position);
                                }
                                transition.appendChild(trPr);
                            }
                            //nails
                            for(int k = 0; k < tr.getNails().size(); k++)
                            {
                                Nail na = (Nail)(tr.getNails().get(k));
                                Element nail = doc.createElement("nail");
                                if(na.getFrom() instanceof Nail)
                                {
                                    nail.setAttribute("from", ""+tr.getNails().indexOf(na.getFrom()));
                                }
                                else
                                {
                                    nail.setAttribute("from", "FROMSTATE");
                                }
                                if(na.getTo() instanceof Nail)
                                {
                                    nail.setAttribute("to", ""+tr.getNails().indexOf(na.getTo()));
                                }
                                else
                                {
                                    nail.setAttribute("to", "TOSTATE");
                                }
                                nail.setAttribute("id", ""+k);
                                double nailX = na.getX();
                                double nailY = na.getY();
                                {
                                    Element position = doc.createElement("position");
                                    position.setAttribute("x", ""+nailX);
                                    position.setAttribute("y", ""+nailY);
                                    nail.appendChild(position);
                                }
                                transition.appendChild(nail);
                            }
                            module.appendChild(transition);
                        }
                    }
                    //Variables
                    ArrayList variableNames = editor.getVariableNames(theModules[i]);
                    ArrayList<Type> variableTypes = editor.getVariableTypes(theModules[i]);
                    ArrayList variableInits = editor.getVariableInits(theModules[i]);
                    ArrayList variableMins  = editor.getVariableMins(theModules[i]);
                    ArrayList variableMaxs  = editor.getVariableMaxs(theModules[i]);
                    for(int j = 0; j < variableTypes.size(); j++)
                    {
                        Type type = variableTypes.get(j);
                        
                        Element var = null;
                        String name = (String)variableNames.get(j);
                        Object init = variableInits.get(j);
                        if (type instanceof TypeInt) {
                                var = doc.createElement("variable");
                                var.setAttribute("name", name);
                                var.setAttribute("min", (String)variableMins.get(j));
                                var.setAttribute("max", (String)variableMaxs.get(j));
                                if(init != null)
                                {
                                    var.setAttribute("init", (String)init);
                                }
                        }
                        else if (type instanceof TypeBool) {
                                var = doc.createElement("boolVariable");
                                var.setAttribute("name", name);
                                if(init != null)
                                {
                                    var.setAttribute("init", (String)init);
                                }
                        }
                        if(var != null)module.appendChild(var);
                    }
                    //Branched Transitions
                    for(int j = 0; j < decisions.size(); j++)
                    {
                        Decision de = (Decision)decisions.get(j);
                        Element branchT = doc.createElement("branchtrans");
                        
                        //find which transition points to this decision
                        Transition tr = null;
                        for(int k = 0; k < theModules[i].getNumTransitions(); k++)
                        {
                            Transition temptr = theModules[i].getTransition(k);
                            if(temptr.getTo() == de)
                            {
                                tr = temptr;
                                break;
                            }
                        }
                        if(tr != null)
                        {
                            //search for from state index
                            int fromState = 0;
                            for(int k = 0; k < theModules[i].getNumStates(); k++)
                            {
                                if(theModules[i].getState(k) == tr.getFrom())
                                {
                                    fromState = k;
                                    break;
                                }
                            }
                            branchT.setAttribute("from",  ""+fromState);
                        }
                        else branchT.setAttribute("from", "0");
                        
                        //position
                        double nodePosX = de.getX();
                        double nodePosY = de.getY();
                        {
                            Element position = doc.createElement("position");
                            position.setAttribute("x", ""+nodePosX);
                            position.setAttribute("y", ""+nodePosY);
                            branchT.appendChild(position);
                        }
                        
                        //branches
                        //get all branches of this decision
                        ArrayList extract = new ArrayList();
                        for(int k = 0; k < branchTrans.size(); k++)
                        {
                            ProbTransition prtr = (ProbTransition)branchTrans.get(k);
                            if(prtr.getFrom() == de) extract.add(prtr);
                        }
                        
                        for(int k = 0; k < extract.size(); k++)
                        {
                            ProbTransition prtr = (ProbTransition)extract.get(k);
                            Element aBranch = doc.createElement("branch");
                            int toState = 0;
                            for(int l = 0; l < theModules[i].getNumStates(); l++)
                            {
                                if(theModules[i].getState(l) == prtr.getTo())
                                {
                                    toState = l;
                                    break;
                                }
                            }
                            aBranch.setAttribute("to", ""+toState);
                            //nail position
                            if(prtr.getNails().size() > 0)
                            {
                                double nailX = ((Nail)(prtr.getNails().get(0))).getX();
                                double nailY = ((Nail)(prtr.getNails().get(0))).getY();
                                {
                                    Element position = doc.createElement("position");
                                    position.setAttribute("x", ""+nailX);
                                    position.setAttribute("y", ""+nailY);
                                    aBranch.appendChild(position);
                                }
                            }
                            //assign
                            String assign = prtr.getAssignmentLabel().getString();
                            if(!assign.equals(""))
                            {
                                Element ass = doc.createElement("assign");
                                ass.setAttribute("value", assign);
                                double assX = prtr.getAssignmentLabel().getOffsetX();
                                double assY = prtr.getAssignmentLabel().getOffsetY();
                                {
                                    Element position = doc.createElement("position");
                                    position.setAttribute("x", ""+assX);
                                    position.setAttribute("y", ""+assY);
                                    ass.appendChild(position);
                                }
                                aBranch.appendChild(ass);
                            }
                            //prob
                            String probab = prtr.getProbabilityLabel().getString();
                            if(!probab.equals(""))
                            {
                                Element pro = doc.createElement("tranProb");
                                pro.setAttribute("value", probab);
                                double proX = prtr.getProbabilityLabel().getOffsetX();
                                double proY = prtr.getProbabilityLabel().getOffsetY();
                                {
                                    Element position = doc.createElement("position");
                                    position.setAttribute("x", ""+proX);
                                    position.setAttribute("y", ""+proY);
                                    pro.appendChild(position);
                                }
                                aBranch.appendChild(pro);
                            }
                            branchT.appendChild(aBranch);
                        }
                        //guard
                        try
                        {
                            String guard = tr.getGuardLabel().getString();
                            if(!guard.equals(""))
                            {
                                Element g = doc.createElement("guard");
                                g.setAttribute("value", guard);
                                double gX = tr.getGuardLabel().getOffsetX();
                                double gY = tr.getGuardLabel().getOffsetY();
                                {
                                    Element position = doc.createElement("position");
                                    position.setAttribute("x", ""+gX);
                                    position.setAttribute("y", ""+gY);
                                    g.appendChild(position);
                                }
                                branchT.appendChild(g);
                            }
                        }
                        catch(NullPointerException e)
                        {
                        }
                        //sync
                        try
                        {
                            String sync = tr.getSyncLabel().getString();
                            if(!sync.equals(""))
                            {
                                Element syn = doc.createElement("sync");
                                syn.setAttribute("value", sync);
                                double syncX = tr.getSyncLabel().getOffsetX();
                                double syncY = tr.getSyncLabel().getOffsetY();
                                {
                                    Element position = doc.createElement("position");
                                    position.setAttribute("x",""+syncX);
                                    position.setAttribute("y",""+syncY);
                                    syn.appendChild(position);
                                }
                                branchT.appendChild(syn);
                            }
                        }
                        catch(NullPointerException e)
                        {
                        }
                        module.appendChild(branchT);
                    }
                    gmoRoot.appendChild(module);
                }
            }
            
            doc.appendChild(gmoRoot);
            //File writing
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty("doctype-system", "gmo.dtd");
            t.setOutputProperty("indent", "yes");
            t.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(f)));
            
            //success
            SwingUtilities.invokeAndWait(new Runnable()
            {
                public void run()
                {
                    plug.stopProgress();
                    plug.setTaskBarText("Saving model... done.");
                    plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, plug));
                    handler.graphicFileWasSaved(f);
                }
            });
            
        }
        catch(DOMException e)
        {
            System.err.println("DOM Exception: "+e);
            e.printStackTrace();
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        plug.stopProgress();
                        plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
                        plug.setTaskBarText("Saving model... error.");
                    }
                });
            }
            catch(Exception ex)
            {
                
            }
            plug.error("Problem saving file: " + e.getMessage());
            
            return;
        }
        catch(ParserConfigurationException e)
        {
            System.err.println("Parser Exception: "+e);
            e.printStackTrace();
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        plug.stopProgress();
                        plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
                        plug.setTaskBarText("Saving model... error.");
                    }
                });
            }
            catch(Exception ex)
            {
                
            }
            plug.error("Problem saving file: " + e.getMessage());
            
            return;
        }
        catch(TransformerConfigurationException e)
        {
            e.printStackTrace();
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        plug.stopProgress();
                        plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
                        plug.setTaskBarText("Saving model... error.");
                    }
                });
            }
            catch(Exception ex)
            {
                
            }
            plug.error("Problem saving file: " + e.getMessage());
            
            return;
        }
        catch(FileNotFoundException e)
        {
            System.err.println("File not found: "+e);
            e.printStackTrace();
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        plug.stopProgress();
                        plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
                        plug.setTaskBarText("Saving model... error.");
                    }
                });
            }
            catch(Exception ex)
            {
                
            }
            plug.error("Problem saving file: " + e.getMessage());
            
            return;
        }
        catch(TransformerException e)
        {
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        plug.stopProgress();
                        plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
                        plug.setTaskBarText("Saving model... error.");
                    }
                });
            }
            catch(Exception ex)
            {
                
            }
            plug.error("Problem saving file: " + e.getMessage());
            
            return;
        }
        catch(PrismException e)
        {
            e.printStackTrace();
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        plug.stopProgress();
                        plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
                        plug.setTaskBarText("Saving model... error.");
                    }
                });
            }
            catch(Exception ex)
            {
                
            }
            plug.error("Problem saving file: " + e.getMessage());
            
            return;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        plug.stopProgress();
                        plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
                        plug.setTaskBarText("Saving model... error.");
                    }
                });
            }
            catch(Exception ex)
            {
                
            }
            plug.error("Problem saving file: " + e.getMessage());
            
            return;
        }
    }
    
}
