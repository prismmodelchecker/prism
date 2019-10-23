//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package prism;

import java.io.*;
import java.net.*;
import java.util.*;

import parser.ast.*;

/**
* Example class demonstrating how to control PRISM programmatically,
* i.e. through the "API" exposed by the class prism.Prism.
* (this now uses the newer version of the API, released after PRISM 4.0.3)
* Test like this:
* PRISM_MAINCLASS=prism.PrismTest bin/prism ../prism-examples/polling/poll2.sm ../prism-examples/polling/poll3.sm
* The aim of this class is to talk to and answer requests for RAPPORT.
*/
public class PrismPythonTalker
{
	private Prism prism;
	private ModulesFile currentModel;
	private ServerSocket server;
	String directory;
	int socketPort;
	public static final String SUCCESS = "success";
	public static final String FAILURE = "failure";
	
	/**
	 * Constructor initialises Prism Talker
	 * @param port the port for the PRISM server to be on
	 * @param workDir the working directory to be used by the server 
	 * 				  and anyone working with it
	 */
	public PrismPythonTalker(int port, String workDir){
		try{
			PrismLog mainLog;
			
			//init socket
			socketPort=port;
			server = new ServerSocket(socketPort);
			System.out.println("PRISM server running on port " + socketPort);
			
			directory=workDir;
			            
			// Initialise PRISM
			//mainLog = new PrismDevNullLog();
			mainLog = new PrismFileLog("stdout");
			prism = new Prism(mainLog);
			prism.initialise();
			setExports();
			prism.setEngine(Prism.EXPLICIT);
			
		} catch (PrismException e) {
			System.out.println("Error: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}
	}
	
	//Getters
	public Prism getPrism(){
		return prism;
	}

	public ModulesFile getCurrentModel(){
		return currentModel;
	}

	public ServerSocket getServer(){
		return server;
	}

	public int getSocketPort(){
		return socketPort;
	} 

	/**
	 * Function sets export locations for adversaries etc.
	 */
	public void setExports(){
		try {
			
			String modelFileName = computeModelFileName("");
			prism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV, "DTMC");
			prism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV_FILENAME, directory + modelFileName + "_adv.tra");
			prism.setExportProductStates(true);
			prism.setExportProductStatesFilename(directory + modelFileName + "_prod.sta");
			prism.setExportProductTrans(true);
			prism.setExportProductTransFilename(directory + modelFileName + "_prod.tra");
			prism.setExportTarget(true);
			prism.setExportTargetFilename(directory + modelFileName + "_prod.lab");
			prism.getSettings().setExportPropAut(true);
			prism.getSettings().setExportPropAutFilename(directory + modelFileName + "_prod.aut");
			prism.setExportProductVector(true);
			prism.setExportProductVectorFilename(directory + modelFileName + "_guarantees.vect");
		} catch (PrismException e) {
			System.out.println("File not found Error: " + e.getMessage());
		}
	}

	/**
	 * Function loads a PRISM file into Prism so it can be model checked
	 * @param modelPath the path of the PRISM file
	 * @return the success status of the operation
	 */
	public boolean loadPrismModelFile(String modelPath){
		try{
			currentModel = prism.parseModelFile(new File(modelPath));
			prism.loadPRISMModel(currentModel);
			return true;
		} catch (FileNotFoundException e) {
			System.out.println("Error: " + e.getMessage());
			return false;
		}
		catch (PrismException e) {
			System.out.println("Error: " + e.getMessage());
			return false;
		}
	}

	
	/**
	 * Given the path of a model, get the file name for all related files
	 * @param modelPath The path of the PRISM model file
	 * @return The file name minus directory path and extension
	 */
	public String computeModelFileName(String modelPath) {
		int startOfName = modelPath.lastIndexOf('/') + 1;
		int endOfName = modelPath.lastIndexOf('_');
		
		if (endOfName == -1) {
			//go to point before extension instead
			endOfName = modelPath.lastIndexOf('.');
		}
		
		if (startOfName == -1 || endOfName == -1) {
			return "model"; // a default
		}
		
		return modelPath.substring(startOfName, endOfName);
	}
	
	
	/**
	 * Function calls Prism and returns Result object
	 * @param ltlString The LTL property to model check
	 * @param modelPath The location of the PRISM model file
	 * @param generatePolicy should the model checking procedure output a policy
	 * @param getStateVector should the Result object store the state vector
	 * @return
	 */
	public Result callPrism(String ltlString, String modelPath, boolean generatePolicy, boolean getStateVector)  {
		try {
			PropertiesFile prismSpec;
			Result result;
			prism.setStoreVector(getStateVector);
			
			String modelFileName = computeModelFileName(modelPath);
			if (directory.charAt(directory.length()-1) != '/') {
				modelFileName = '/' + modelFileName;
			}
			
			if(generatePolicy){
				
				// settings for outputting state/policy information
				prism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV, "DTMC");
				prism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV_FILENAME, directory + modelFileName + "_adv.tra");
				prism.setExportProductStates(true);
				prism.setExportProductStatesFilename(directory + modelFileName + "_prod.sta");
				prism.setExportProductTrans(true);
				prism.setExportProductTransFilename(directory + modelFileName + "_prod.tra");
				prism.setExportTarget(true);
				prism.setExportTargetFilename(directory + modelFileName + "_prod.lab");
				prism.getSettings().setExportPropAut(true);
				prism.getSettings().setExportPropAutFilename(directory + modelFileName + "_prod.aut");
			} else {
				prism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV, "None");               
				prism.setExportProductStates(false);
				prism.setExportProductTrans(false);
				prism.setExportTarget(false);
			}
			
			boolean loadSuccess = loadPrismModelFile(modelPath);
			
			//if loading model failed
			if(!loadSuccess) {
				return null;
			}
			
			prism.exportStatesToFile(Prism.EXPORT_PLAIN, new File(directory + modelFileName + "_original.sta"));
			prismSpec=prism.parsePropertiesString(currentModel, ltlString);
			result = prism.modelCheck(prismSpec, prismSpec.getPropertyObject(0));
			return result;
			
		} catch (PrismException e) {
			System.out.println("Error: " + e.getMessage());
			return null;
		} catch (FileNotFoundException e) {
			System.out.println("File not found Error: " + e.getMessage());
			return null;
		}
	}
	

	/**
	 * Function takes an existing model checking result and does the 
	 * dot product between the initial distribution and the result's state vector
	 * @param res The model checking result
	 * @param initDistFile The file containing the initial distribution
	 * @return The dot product of the initial distribution and result vector
	 * @throws PrismException If error in computation
	 */
	public double useInitialDistribution(Result res, File initDistFile) throws PrismException{
		return prism.recomputeModelCheckingResultForInitialDistribution(res, initDistFile);
	}
	
	/**
	 * Main function runs main loop of PRISM server.
	 * @param args as standard
	 * @throws Exception 
	 */
	public static void main(String args[]) throws Exception {
		
		List<String> commands=Arrays.asList(new String[] {"check", "plan", "get_vector", "shutdown", "check_init_dist"});
		String command, ack, toClient, ltlString, modelFile;
		ltlString = modelFile = null;
		Socket client;
		Result result;
		
		//set up the connection
		PrismPythonTalker talker=new PrismPythonTalker(Integer.parseInt(args[0]), args[1]); 
		client = talker.server.accept();
		System.out.println("got connection on port" + talker.getSocketPort());  
		BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		PrintWriter out = new PrintWriter(client.getOutputStream(),true);
		boolean run = true;
		
		while(run) { 
			
			command = in.readLine();
			System.out.println("received: " + command); 
			if(command == null){
				client = talker.server.accept();
				System.out.println("got connection on port" + talker.getSocketPort());
			} else {
				if (!commands.contains(command)) {
					System.out.println("Socket comm is unsynchronised! Trying to recover...");
					continue;
				}
				
				// if not shutdown command, get the LTL string and model file
				if(!command.equals("shutdown")) {
					ltlString=in.readLine();
					modelFile = in.readLine();
					
					if(ltlString == null || modelFile == null) {
						out.println(PrismPythonTalker.FAILURE);
					}
				} else { // if shutdown command given
					run=false;
					client.close();
					talker.server.close();
					talker.prism.closeDown();
					continue;
				}
				
				// command for standard model checking queries, on models with one initial state
				// or for partial satisfiability guarantees
				if (command.equals("check")){
					try {
						result = talker.callPrism(ltlString, modelFile, false, false);
						if (result != null){
							out.println(result.getResult().toString());
						} else {
							out.println(PrismPythonTalker.FAILURE);
						}
					} catch(Exception e) {
						out.println(PrismPythonTalker.FAILURE);
					}
					continue;
				}
				
				// command for planning and storing policies
				if (command.equals("plan")){
					try {
						result=talker.callPrism(ltlString, modelFile, true, false);
						if(result != null) {
							out.println(talker.computeModelFileName(modelFile));
						} else {
							out.println(PrismPythonTalker.FAILURE);
						}
						
					} catch(Exception e) {
						out.println(PrismPythonTalker.FAILURE);
					}
					continue;
				}
				
				// command for returning state vector after model checking
				if (command.equals("get_vector")){
					try {
						result=talker.callPrism(ltlString, modelFile, false, true);
						StateVector vect = result.getVector();
						toClient="start";
						out.println(toClient);
						ack=in.readLine();
						if(ack.equals("error")){
							System.out.println("Socket error, continuing without outputting state vector");
							continue;
						}
						if (vect != null) {
							int n = vect.getSize();
							for (int i = 0; i < n; i++) {
								toClient=vect.getValue(i).toString();
								out.println(toClient);
								ack=in.readLine();
								if(ack.equals("error")){
									System.out.println("Socket error, continuing without outputting state vector");
									continue;
								}
							}
							vect.clear();
						}
						out.println("end");
					} catch(Exception e) {
						out.println(PrismPythonTalker.FAILURE);
					}
					continue;
				}
				
				// command for model checking for models with initial state distributions 
				if (command.equals("check_init_dist")) {
					try {
						// get the initial distribution path
						String initDistFile = in.readLine();
						if (initDistFile == null) {
							out.println(PrismPythonTalker.FAILURE);
						}
						
						// make the initial call to prism
						result = talker.callPrism(ltlString, modelFile, false, true);
						if(result == null) {
							out.println(PrismPythonTalker.FAILURE);
						}
						
						// call the multiplication with initial distribution
						double finalResult = talker.useInitialDistribution(result, new File(initDistFile));
						out.println(String.valueOf(finalResult));
						
					} catch(Exception e) {
						out.println(PrismPythonTalker.FAILURE);
					}
					continue;
				}
				
			}
		}
		System.exit(0);
	}
}



