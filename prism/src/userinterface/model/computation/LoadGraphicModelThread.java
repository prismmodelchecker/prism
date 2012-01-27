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
import java.awt.*;
import javax.swing.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import userinterface.*;
import userinterface.model.*;
import userinterface.model.graphicModel.*;
import userinterface.util.*;

public class LoadGraphicModelThread extends Thread implements EntityResolver
{
	private GUIMultiModelHandler handler;
	private File f;
	private GUIPlugin plug;
	private GUIGraphicModelEditor graphicEdit;
	private boolean error;
	
	/** Creates a new instance of LoadGraphicModelThread */
	public LoadGraphicModelThread(GUIMultiModelHandler handler, File f)
	{
		this.handler = handler;
		this.f = f;
		plug = handler.getGUIPlugin(); //to communicate with rest of gui
		error = false;
	}
	
	public void run()
	{
		// initialise editor
		graphicEdit = new GUIGraphicModelEditor(handler, handler.getTree(), handler.getPropModel());
		graphicEdit.newModel();
		graphicEdit.setBusy(true);
		// initialise model storage
		ModuleModel[]moduleModels;
		Rectangle[] modulePositions;
		ArrayList[]moduleVariable;
		ArrayList theConstants = new ArrayList();
		ArrayList theDeclarations = new ArrayList();
		ArrayList stateVariables = new ArrayList();
		String theModelName="", theModelType="",sysInfo="";
		boolean autolayout = true;
		JFrame parent;
		
		try
		{
			//notify interface of start of computation and start the read into textEdit
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					plug.startProgress();
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_START, plug));
					plug.setTaskBarText("Loading model...");
				}
			});
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(true);
			factory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(this);
			Document doc = builder.parse(f);
			Element root = doc.getDocumentElement();
			//theModelName = root.getAttribute("name");
			theModelType = root.getAttribute("type");
			//System.out.println("The model type = "+theModelType);
			autolayout = Boolean.getBoolean(root.getAttribute("autolayout"));
			
			NodeList rootChildren = root.getChildNodes();
			
			//Constants
			//System.out.println("loading constants");
			Element cons = (Element)rootChildren.item(0);
			NodeList consChildren = cons.getChildNodes();
			for(int i = 0; i < consChildren.getLength(); i++)
			{
				Element aCons = (Element)consChildren.item(i);
				if(aCons.getTagName().equals("integerConstant"))
				{
					String name = aCons.getAttribute("name");
					String value = aCons.getAttribute("value");
					if(value != "")theConstants.add(new IntegerConstant(name, value));
					else theConstants.add(new IntegerConstant(name));
				}
				else if(aCons.getTagName().equals("booleanConstant"))
				{
					String name = aCons.getAttribute("name");
					String value = aCons.getAttribute("value");
					if(value != "")theConstants.add(new BooleanConstant(name, value));
					else theConstants.add(new BooleanConstant(name));
				}
				else if(aCons.getTagName().equals("doubleConstant"))
				{
					String name = aCons.getAttribute("name");
					String value = aCons.getAttribute("value");
					if(value != "")theConstants.add(new DoubleConstant(name, value));
					else theConstants.add(new DoubleConstant(name));
				}
			}
			//Declarations
			Element decl = (Element)rootChildren.item(1);
			NodeList declChildren = decl.getChildNodes();
			for(int i = 0; i < declChildren.getLength(); i++)
			{
				Element aDecl = (Element)declChildren.item(i);
				if(aDecl.getTagName().equals("variable"))
				{
					String name = aDecl.getAttribute("name");
					String min = aDecl.getAttribute("min");
					String max = aDecl.getAttribute("max");
					String init = aDecl.getAttribute("init");
					if(!init.equals(""))
						theDeclarations.add(new IntegerVariable(name, min, max,init));
					else
						theDeclarations.add(new IntegerVariable(name, min, max));
				}
				else if(aDecl.getTagName().equals("boolVariable"))
				{
					String name = aDecl.getAttribute("name");
					String init = aDecl.getAttribute("init");
					if(!init.equals(""))
						theDeclarations.add(new BooleanVariable(name, init));
					else
						theDeclarations.add(new BooleanVariable(name));
				}
			}
			
			//System Information
			Element sysI = (Element)rootChildren.item(2);
			Text text = (Text)sysI.getChildNodes().item(0);
			if(text != null)
				sysInfo = text.getData();
			else
				sysInfo = "";
			
			//Before going into the modules, construct the tree of with the constants,
			//declarations and system information.
			graphicEdit.loadAllExceptModules(theModelName, theModelType, theDeclarations, theConstants, sysInfo);
			
			//Modules
			moduleModels = new ModuleModel[rootChildren.getLength()-3];
			modulePositions = new Rectangle[rootChildren.getLength()-3];
			moduleVariable = new ArrayList[rootChildren.getLength()-3];
			stateVariables = new ArrayList(rootChildren.getLength()-3);
			for(int i = 3; i < rootChildren.getLength(); i++) // 0,1,2 must be included therefore start at 3
			{
				//System.out.println("outer loop");
				Element module = (Element)rootChildren.item(i);
				String moduleName = module.getAttribute("name");
				double moduleZoom;
				try
				{
					moduleZoom = Double.parseDouble(module.getAttribute("zoom"));
				}
				catch(NumberFormatException e)
				{
					moduleZoom = 1.0;
				}
				int x=0,y=0,width=200,height=200;
				
				try
				{
					x = Integer.parseInt(module.getAttribute("x"));
				}
				catch(NumberFormatException e)
				{ 
				}
				try
				{
					y = Integer.parseInt(module.getAttribute("y"));
				}
				catch(NumberFormatException e)
				{  
				}
				try
				{
					width = Integer.parseInt(module.getAttribute("width"));
				}
				catch(NumberFormatException e)
				{ 
				}
				try
				{
					height = Integer.parseInt(module.getAttribute("height"));
				}
				catch(NumberFormatException e)
				{
				}
				
				
				
				GUIMultiModelTree.ModuleNode corr = graphicEdit.requestNewModule(moduleName);
				ModuleModel theModel = graphicEdit.getModuleModel(corr);
				
				theModel.getContainer().setRectangle(new Rectangle(x, y, width, height));
				
				ArrayList moduleVariables = new ArrayList();
				
				NodeList moduleChildren = module.getChildNodes();
				//Get Out all the variables and states first
				for(int j = 0; j < moduleChildren.getLength(); j++)
				{
					Element modChild = (Element)moduleChildren.item(j);
					String tag = modChild.getTagName();
					if(tag.equals("variable"))
					{
						String name = modChild.getAttribute("name");
						String min = modChild.getAttribute("min");
						String max = modChild.getAttribute("max");
						String init = modChild.getAttribute("init");
						if(init.equals("")) init = "&&&Default&&&";
						//moduleVariables.add(new IntegerVariable(name,min,max,init));
						graphicEdit.addIntegerVariable(corr, new IntegerVariable(name,min,max,init));
					}
					else if(tag.equals("boolVariable"))
					{
						String name = modChild.getAttribute("name");
						String init = modChild.getAttribute("init");
						if(init.equals("")) init = "&&&Default&&&";
						graphicEdit.addBooleanVariable(corr, new BooleanVariable(name,init));
					}
					else if(tag.equals("state"))
					{
						int id = Integer.parseInt(modChild.getAttribute("id"));
						boolean init = Boolean.getBoolean(modChild.getAttribute("initial"));
						Element position = (Element)modChild.getChildNodes().item(0);
						double posX = Double.parseDouble(position.getAttribute("x"));
						double posY = Double.parseDouble(position.getAttribute("y"));
						if(modChild.getChildNodes().getLength() == 3) //must have both invariant and sName
						{
							Element invariant = (Element)modChild.getChildNodes().item(1);
							String iName = invariant.getAttribute("name");
							Element iPosition = (Element)invariant.getChildNodes().item(0);
							double iPosX = Double.parseDouble(iPosition.getAttribute("x"));
							double iPosY = Double.parseDouble(iPosition.getAttribute("y"));
							
							Element sName = (Element)modChild.getChildNodes().item(2);
							String ssName = sName.getAttribute("name");
							Element sPosition = (Element)sName.getChildNodes().item(0);
							double sPosX = Double.parseDouble(sPosition.getAttribute("x"));
							double sPosY = Double.parseDouble(sPosition.getAttribute("y"));
							
							userinterface.model.graphicModel.State aState = new userinterface.model.graphicModel.State(posX, posY);
							//aState.setStateName(ssName);
							//aState.setInvariant(iName);
							
							// for backward compatibility
							if(!iName.equals(""))
								aState.setComment(ssName + "\n"+iName);
							else
								aState.setComment(ssName);
							
							
							aState.setInitial(init);
							aState.getCommentLabel().setOffsetX(sPosX);
							aState.getCommentLabel().setOffsetY(sPosY);
							//aState.getInvarientLabel().setOffsetX(iPosX);
							//aState.getInvarientLabel().setOffsetY(iPosY);
							//System.out.println("should be adding a state now1");
							theModel.addState(aState);
						}
						else if(modChild.getChildNodes().getLength() == 2) // must have either invariant or sName
						{
							Element el = (Element)modChild.getChildNodes().item(1);
							if(el.getTagName().equals("invariant"))
							{
								Element invariant = (Element)modChild.getChildNodes().item(1);
								String iName = invariant.getAttribute("name");
								Element iPosition = (Element)invariant.getChildNodes().item(0);
								double iPosX = Double.parseDouble(iPosition.getAttribute("x"));
								double iPosY = Double.parseDouble(iPosition.getAttribute("y"));
								userinterface.model.graphicModel.State aState = new userinterface.model.graphicModel.State(posX, posY);
								aState.setComment(iName);
								aState.setInitial(init);
								aState.getCommentLabel().setOffsetX(iPosX);
								aState.getCommentLabel().setOffsetY(iPosY);
								//System.out.println("should be adding a state now2");
								theModel.addState(aState);
							}
							else //must be sName
							{
								Element sName = (Element)modChild.getChildNodes().item(1);
								String ssName = sName.getAttribute("name");
								Element sPosition = (Element)sName.getChildNodes().item(0);
								double sPosX = Double.parseDouble(sPosition.getAttribute("x"));
								double sPosY = Double.parseDouble(sPosition.getAttribute("y"));
								userinterface.model.graphicModel.State aState = new userinterface.model.graphicModel.State(posX, posY);
								aState.setComment(ssName);
								aState.setInitial(init);
								aState.getCommentLabel().setOffsetX(sPosX);
								aState.getCommentLabel().setOffsetY(sPosY);
								theModel.addState(aState);
							}
						}
						else //must have neither invariant or sName
						{
							userinterface.model.graphicModel.State aState = new userinterface.model.graphicModel.State(posX, posY);
							aState.setInitial(init);
							theModel.addState(aState);
							//System.out.println("should be adding a state now4");
						}
					}
				}
				
				//We should now have all states and variables in theModel, now get the transitions
				for(int j = 0; j < moduleChildren.getLength(); j++)
				{
					Element modChild = (Element)moduleChildren.item(j);
					String tag = modChild.getTagName();
					if(tag.equals("transition"))
					{
						int from = Integer.parseInt(modChild.getAttribute("from"));
						int to   = Integer.parseInt(modChild.getAttribute("to"));
						String guard = "";
						String sync = "";
						String assign = "";
						String tranProb = "";
						double guardX = 0;
						double guardY = 0;
						double syncX = 0;
						double syncY = 0;
						double assignX = 0;
						double assignY = 0;
						double probX = 0;
						double probY = 0;
						ArrayList nailElements = new ArrayList();
						NodeList tranChildren = modChild.getChildNodes();
						//Here we get label information and collect nailElements
						for(int k = 0; k < tranChildren.getLength(); k++)
						{
							//System.out.println("transitions inner loop start "+j);
							Element tranChild = (Element)tranChildren.item(k);
							String tTag = tranChild.getTagName();
							if(tTag.equals("guard"))
							{
								//System.out.println("doing guard");
								Element position = (Element)tranChild.getChildNodes().item(0);
								guardX = Double.parseDouble(position.getAttribute("x"));
								guardY = Double.parseDouble(position.getAttribute("y"));
								guard = tranChild.getAttribute("value");
							}
							else if(tTag.equals("sync"))
							{
								//System.out.println("doing sync");
								Element position = (Element)tranChild.getChildNodes().item(0);
								syncX = Double.parseDouble(position.getAttribute("x"));
								syncY = Double.parseDouble(position.getAttribute("y"));
								sync = tranChild.getAttribute("value");
							}
							else if(tTag.equals("assign"))
							{
								//System.out.println("doing assign");
								Element position = (Element)tranChild.getChildNodes().item(0);
								assignX = Double.parseDouble(position.getAttribute("x"));
								assignY = Double.parseDouble(position.getAttribute("y"));
								assign = tranChild.getAttribute("value");
							}
							else if(tTag.equals("tranProb"))
							{
								
								Element position = (Element)tranChild.getChildNodes().item(0);
								probX = Double.parseDouble(position.getAttribute("x"));
								probY = Double.parseDouble(position.getAttribute("y"));
								tranProb = tranChild.getAttribute("value");
							}
							else if(tTag.equals("nail"))
							{
								nailElements.add(tranChild);
							}
						}
						//we should have an ArrayList of Elements containinginformation about nails in nailElements
						ArrayList nails = new ArrayList();
						ArrayList froms = new ArrayList();
						ArrayList tos = new ArrayList();
						for(int k = 0; k < nailElements.size(); k++)
						{
							Element nail = (Element)nailElements.get(k);
							int nFrom = 0;
							try
							{
								nFrom = Integer.parseInt(nail.getAttribute("from"));
							}
							catch(NumberFormatException e)
							{
								if(nail.getAttribute("from").equals("FROMSTATE"))
									nFrom = -1;
							}
							int nTo = 0;
							try
							{
								nTo   = Integer.parseInt(nail.getAttribute("to"));
							}
							catch(NumberFormatException e)
							{
								if(nail.getAttribute("to").equals("TOSTATE"))
									nTo = -1;
							}
							Element position = (Element)nail.getChildNodes().item(0);
							double nX = Double.parseDouble(position.getAttribute("x"));
							double nY = Double.parseDouble(position.getAttribute("y"));
							Nail n = new Nail(nX, nY, null, null);
							nails.add(n);
							froms.add(new Integer(nFrom));
							tos.add(new Integer(nTo));
						}
						//do tos and froms of nails
						for(int k = 0; k < nails.size(); k++)
						{
							Nail aNail = (Nail)nails.get(k);
							int aFrom = ((Integer)froms.get(k)).intValue();
							int aTo   = ((Integer)tos.get(k)).intValue();
							if(aFrom!=-1)
								aNail.setFrom((Nail)nails.get(aFrom));
							else
								aNail.setFrom((userinterface.model.graphicModel.State)theModel.getState(from));
							if(aTo!=-1)
								aNail.setTo((Nail)nails.get(aTo));
							else
								aNail.setTo((userinterface.model.graphicModel.State)theModel.getState(to));
						}
						int tranInd = 0;
						if(nails.size() > 0)
						{
							tranInd = theModel.addTransition(theModel.getState(from), theModel.getState(to), nails);
							//System.out.println("adding nail, here nails.size() = "+nails.size());
						}
						else
						{
							tranInd = theModel.addTransition(theModel.getState(from), theModel.getState(to), false);
							//System.out.println("NOT ADDING NAILS");
						}
						theModel.getTransition(tranInd).setGuard(guard);
						theModel.getTransition(tranInd).setSync(sync);
						theModel.getTransition(tranInd).setAssignment(assign);
						theModel.getTransition(tranInd).setProbability(tranProb);
						theModel.getTransition(tranInd).getGuardLabel().setOffsetX(guardX);
						theModel.getTransition(tranInd).getGuardLabel().setOffsetY(guardY);
						theModel.getTransition(tranInd).getSyncLabel().setOffsetX(syncX);
						theModel.getTransition(tranInd).getSyncLabel().setOffsetY(syncY);
						theModel.getTransition(tranInd).getAssignmentLabel().setOffsetX(assignX);
						theModel.getTransition(tranInd).getAssignmentLabel().setOffsetY(assignY);
						theModel.getTransition(tranInd).getProbabilityLabel().setOffsetX(probX);
						theModel.getTransition(tranInd).getProbabilityLabel().setOffsetY(probY);
					}
					else if(tag.equals("branchtrans"))
					{
						int from  = Integer.parseInt(modChild.getAttribute("from"));
						String guard = "";
						String sync  = "";
						String assign = "";
						double assignX = 0;
						double assignY = 0;
						double nodeX = -100;
						double nodeY = -100;
						double guardX = 0;
						double guardY = 0;
						double syncX = 0;
						double syncY = 0;
						ArrayList branches = new ArrayList();
						Element position = (Element)modChild.getChildNodes().item(0);
						nodeX = Double.parseDouble(position.getAttribute("x"));
						nodeY = Double.parseDouble(position.getAttribute("y"));
						for(int k = 1; k < modChild.getChildNodes().getLength(); k++)
						{
							Element el = (Element)modChild.getChildNodes().item(k);
							String elTag = el.getTagName();
							if(elTag.equals("branch"))
							{
								int to = Integer.parseInt(el.getAttribute("to"));
								NodeList branchChildren = el.getChildNodes();
								String bProb = "";
								String bAssign = "";
								double bProbX = 0;
								double bProbY = 0;
								double bAssignX = 0;
								double bAssignY = 0;
								boolean branchHasNail = false;
								double nailPosX = 0;
								double nailPosY = 0;
								for(int l = 0; l < branchChildren.getLength(); l++)
								{
									Element child = (Element)branchChildren.item(l);
									String cTag = child.getTagName();
									if(cTag.equals("position"))
									{
										branchHasNail = true;
										nailPosX = Double.parseDouble(child.getAttribute("x"));
										nailPosY = Double.parseDouble(child.getAttribute("y"));
									}
									else if(cTag.equals("assign"))
									{
										bAssign = child.getAttribute("value");
										Element aPosition = (Element)child.getChildNodes().item(0);
										bAssignX = Double.parseDouble(aPosition.getAttribute("x"));
										bAssignY = Double.parseDouble(aPosition.getAttribute("y"));
									}
									else if(cTag.equals("tranProb"))
									{
										bProb = child.getAttribute("value");
										Element aPosition = (Element)child.getChildNodes().item(0);
										bProbX = Double.parseDouble(aPosition.getAttribute("x"));
										bProbY = Double.parseDouble(aPosition.getAttribute("y"));
									}
								}
								Branch br = new Branch();
								br.to = to;
								br.prob = bProb;
								br.assignment = bAssign;
								br.probX = bProbX;
								br.probY = bProbY;
								br.assignX = bAssignX;
								br.assignY = bAssignY;
								br.hasNail = branchHasNail;
								br.nailX = nailPosX;
								br.nailY = nailPosY;
								branches.add(br);
							}
							else if(elTag.equals("guard"))
							{
								guard = el.getAttribute("value");
								position = (Element)el.getChildNodes().item(0);
								guardX = Double.parseDouble(position.getAttribute("x"));
								guardY = Double.parseDouble(position.getAttribute("y"));
							}
							else if(elTag.equals("sync"))
							{
								sync = el.getAttribute("value");
								position = (Element)el.getChildNodes().item(0);
								syncX = Double.parseDouble(position.getAttribute("x"));
								syncY = Double.parseDouble(position.getAttribute("y"));
							}
							else if(elTag.equals("assign"))
							{
								assign = el.getAttribute("value");
								position = (Element)el.getChildNodes().item(0);
								assignX = Double.parseDouble(position.getAttribute("x"));
								assignY = Double.parseDouble(position.getAttribute("y"));
							}
						}
						
						int dec = theModel.addDecision(nodeX, nodeY);
						int trpos = theModel.addTransition(theModel.getState(from), theModel.getState(dec), false);
						theModel.getTransition(trpos).setGuard(guard);
						theModel.getTransition(trpos).setSync(sync);
						theModel.getTransition(trpos).getGuardLabel().setOffsetX(guardX);
						theModel.getTransition(trpos).getGuardLabel().setOffsetY(guardY);
						theModel.getTransition(trpos).getSyncLabel().setOffsetX(syncX);
						theModel.getTransition(trpos).getSyncLabel().setOffsetY(syncY);
						for(int k = 0; k < branches.size(); k++)
						{
							Branch br = (Branch)branches.get(k);
							Nail aNail = null;
							if(br.hasNail) aNail = new Nail(br.nailX, br.nailY, theModel.getState(dec), theModel.getState(br.to));
							int pos = theModel.addProbTransition(theModel.getState(dec),  theModel.getState(br.to), aNail);
							ProbTransition prtr = (ProbTransition)theModel.getTransition(pos);
							prtr.getProbabilityLabel().setString(br.prob);
							prtr.getProbabilityLabel().setOffsetX(br.probX);
							prtr.getProbabilityLabel().setOffsetY(br.probY);
							prtr.getAssignmentLabel().setString(br.assignment);
							prtr.getAssignmentLabel().setOffsetX(br.assignX);
							prtr.getAssignmentLabel().setOffsetY(br.assignY);
						}
					}
				}
				
				theModel.setModuleName(moduleName);
				theModel.setZoom(moduleZoom);
				theModel.deSelectAll();
			}
			
			graphicEdit.setAutolayout(autolayout);
			
			graphicEdit.setBusy(false);
			
			//If we get here, the load has been successful, notify the interface and tell the handler.
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					plug.stopProgress();
					plug.setTaskBarText("Loading model... done.");
					plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_DONE, plug));
					handler.graphicModelLoaded(graphicEdit, f );
				}
			});
		}
		// catch and ignore any thread exceptions
		catch (java.lang.InterruptedException e)
		{}
		catch (java.lang.reflect.InvocationTargetException e)
		{}
		
		catch(Exception e)
		{
			//If there was a problem with the loading, notify the interface.
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						plug.stopProgress();
						plug.notifyEventListeners(new GUIComputationEvent(GUIComputationEvent.COMPUTATION_ERROR, plug));
						plug.setTaskBarText("Loading model... error.");
					}
				});
			}
			catch(Exception ex)
			{
				
			}
			e.printStackTrace();
			plug.error("Problem reading file: " + e.getMessage());
			
			return;
		}
	}
	
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
	{
		InputSource inputSource = null;
		
		// override the resolve method for the dtd
		if (systemId.endsWith("dtd")) {
			// get appropriate dtd from classpath
			InputStream inputStream = LoadGraphicModelThread.class.getClassLoader().getResourceAsStream("dtds/gmo.dtd");
			if (inputStream != null) inputSource = new InputSource(inputStream);
		}
		
		return inputSource;
	}
	
	class Branch
	{
		int to;
		String prob;
		String assignment;
		double probX;
		double probY;
		double assignX;
		double assignY;
		double nailX;
		double nailY;
		boolean hasNail;
	}
	
	public abstract class Constant
	{
		public String name;
		public String value = null;
	}
	public class IntegerConstant extends Constant
	{
		public IntegerConstant(String name, String value)
		{
			this.name = name;
			this.value = value;
		}
		
		public IntegerConstant(String name)
		{
			this.name = name;
		}
	}
	public class DoubleConstant extends Constant
	{
		public DoubleConstant(String name, String value)
		{
			this.name = name;
			this.value = value;
		}
		
		public DoubleConstant(String name)
		{
			this.name = name;
		}
	}
	public class BooleanConstant extends Constant
	{
		public BooleanConstant(String name, String value)
		{
			super.name = name;
			this.value = value;
		}
		
		public BooleanConstant(String name)
		{
			super.name = name;
		}
	}
	
	public class Variable
	{
		public String name;
	}
	
	public class IntegerVariable extends Variable
	{
		public String min, max, init="0";
		
		public IntegerVariable(String name, String min, String max, String init)
		{
			super.name = name;
			this.min = min;
			this.max = max;
			this.init = init;
		}
		
		public IntegerVariable(String name, String min, String max)
		{
			this.name = name;
			this.min = min;
			this.max = max;
		}
	}
	
	public class BooleanVariable extends Variable
	{
		public String init = "false";
		
		public BooleanVariable(String name, String init)
		{
			this.init = init;
			this.name = name;
		}
		
		public BooleanVariable(String name)
		{
			this.name = name;
		}
	}
	
   
}
