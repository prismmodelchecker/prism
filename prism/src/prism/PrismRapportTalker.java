//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//  * Charlie Street <cstreet@robots.ox.ac.uk> (University of Oxford)
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

import parser.ast.*;

/**
* Example class demonstrating how to control PRISM programmatically,
* i.e. through the "API" exposed by the class prism.Prism.
* (this now uses the newer version of the API, released after PRISM 4.0.3)
* Test like this:
* PRISM_MAINCLASS=prism.PrismTest bin/prism ../prism-examples/polling/poll2.sm ../prism-examples/polling/poll3.sm
* The aim of this class is to talk to and answer requests for RAPPORT.
*/
public class PrismRapportTalker
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
	public PrismRapportTalker(int port, String workDir){
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
	 * Function calls Prism and returns an ArrayList of Result objects
	 * @param propList An ArrayList of properties to check
	 * @param modelPath The location of the PRISM model file
	 * @param generatePolicy should the model checking procedure output a policy
	 * @param getStateVector should the Result object store the state vector
	 * @return An ArrayList of Result objects
	 */
	public ArrayList<Result> callPrism(ArrayList<String> propList, String modelPath, boolean generatePolicy, boolean getStateVector)  {
		try {
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
				prism.setExportProductVector(true);
				prism.setExportProductVectorFilename(directory + modelFileName + "_guarantees.vect");
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
			
			if(generatePolicy) {
				prism.exportStatesToFile(Prism.EXPORT_PLAIN, new File(directory + modelFileName + "_original.sta"));
			}
			
			ArrayList<Result> resultArr = new ArrayList<Result>();
			
			for(int i = 0; i < propList.size(); i++) {
				String propString = propList.get(i);
				PropertiesFile prismSpec = prism.parsePropertiesString(currentModel, propString);
				resultArr.add(prism.modelCheck(prismSpec, prismSpec.getPropertyObject(0)));
				
			}
			
			return resultArr;
			
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
	 * Function sends a list of results back to Python, with acknowledgements
	 * @param formattedResult An ArrayList of String results
	 * @param in The inward socket communications
	 * @param out The outward socket communications
	 * @returns success was the operation successful
	 */
	public boolean sendResultList(ArrayList<String> formattedResult, BufferedReader in, PrintWriter out) {
		
		try {
			out.println("start");
			String ack = in.readLine();
			if(ack.equals("error")){
				System.out.println("Socket error, continuing without outputting result list");
				return false;
			}
			if (formattedResult != null) {
				for (int i = 0; i < formattedResult.size(); i++) {
					out.println(formattedResult.get(i));
					ack=in.readLine();
					if(ack.equals("error")){
						System.out.println("Socket error, continuing without outputting result list");
						return false;
					}
				}
			}
			out.println("end");
			
			return true;
		} catch(Exception e) {
			return false;
		}
		
	}

	/**
	 * Function gets a PRISM top-level method given its name
	 * @param method_name A String holding the method name to find
	 * @returns method m
	 */
	public Method getPRISMMethodByName(String method_name) {
		Class<?> c = prism.getClass();
		Method[] allMethods = c.getDeclaredMethods();
		for (Method m : allMethods) {
			if (m.getName().equals(method_name)) {
				return m;
			}
		}
		System.out.format("No match for method %s%n", method_name);
		return null;
	}

	/**
	 * Function gets a PRISM top-level method given its name
	 * @param constant_name A String holding the constant name to find
	 * @returns Integer value of constant
	 */
	public Integer getPRISMConstantValueByName(String constant_name) {
		Class<?> c = prism.getClass();
		Field[] fields = c.getFields();
		for (Field f : fields) {
			if (f.getName().equals(constant_name)) {

				Class field_type = f.getType();
				Object field_value;

				try {
					field_value = f.get(prism);
				} catch (IllegalAccessException x) {
					System.out.format("Error: Access to %s not allowed%n", constant_name);
					return null;
				}

				if (!(field_type == int.class)) {
					System.out.format("Error: Unsupported type %s%n", field_type);
					return null;
				}

				return (Integer) field_value;
			}
		}
		System.out.format("Error: No match for field %s%n", constant_name);
		return null;
	}

	/**
	 * Called when command is "configure". Exposes the PRISM Settings object.
	 * @param in The inward socket communications
	 * @param out The outward socket communications
	 */
	public void configurePrism(BufferedReader in, PrintWriter out) {

		String parameter, parameter_type, value;

		try {
			parameter = in.readLine();
			parameter_type = in.readLine();
			value = in.readLine();
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			out.println(PrismRapportTalker.FAILURE);
			return;
		}

		try {

			if (parameter_type.equals("int")) {
				prism.getSettings().set(parameter, Integer.parseInt(value));
				out.println(Integer.toString(prism.getSettings().getInteger(parameter)));

			} else if (parameter_type.equals("double")) {
				prism.getSettings().set(parameter, Double.parseDouble(value));
				out.println(Double.toString(prism.getSettings().getDouble(parameter)));

			} else if (parameter_type.equals("string")) {
				prism.getSettings().set(parameter, value);
				out.println(prism.getSettings().getString(parameter));

			} else if (parameter_type.equals("boolean")) {
				prism.getSettings().set(parameter, Boolean.parseBoolean(value));
				out.println(Boolean.toString(prism.getSettings().getBoolean(parameter)));

			} else {
				System.out.format("Error: Unsupported type %s%n", parameter_type);
				out.println(PrismRapportTalker.FAILURE);
			}

		} catch (PrismException e) {
			System.out.format("PRISM settings error: %s%n", e.getMessage());
			out.println(PrismRapportTalker.FAILURE);
			return;
		}
		return;
	}


	/**
	 * Main function runs main loop of PRISM server.
	 * @param args as standard
	 * @throws Exception 
	 */
	public static void main(String args[]) throws Exception {
		
		List<String> commands=Arrays.asList(new String[] {"check", "plan", "get_vector", "shutdown", "check_init_dist", "check_prop_list", "check_prop_list_init_dist"});
		ArrayList<String> propList, formattedResult = null;
		String command, modelFile;
		modelFile = null;
		Socket client;
		ArrayList<Result> result;
		
		//set up the connection
		PrismRapportTalker talker=new PrismRapportTalker(Integer.parseInt(args[0]), args[1]); 
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

				if (command.equals("configure")) {
					talker.configurePrism(in, out);
					continue;
				}

				if (!commands.contains(command)) {
					System.out.println("Socket comm is unsynchronised! Trying to recover...");
					continue;
				}
				
				// if not shutdown command, get the properties and model file
				if(!command.equals("shutdown")) {
					propList = new ArrayList<String>();
					if(command.contains("prop_list")) { // need to read until end command received
						String currentProp = in.readLine();
						while(!currentProp.contains("end")) {
							propList.add(currentProp);
							currentProp = in.readLine();
						}
					}
					else {
						propList.add(in.readLine());
					}
					
					modelFile = in.readLine();
					
					if(propList.contains(null) || propList.size() == 0 || modelFile == null) {
						out.println(PrismRapportTalker.FAILURE);
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
						result = talker.callPrism(propList, modelFile, false, false);
						if (result != null && result.get(0) != null){
							out.println(result.get(0).getResult().toString());
						} else {
							out.println(PrismRapportTalker.FAILURE);
						}
					} catch(Exception e) {
						out.println(PrismRapportTalker.FAILURE);
					}
					continue;
				}
				
				// command for planning and storing policies
				if (command.equals("plan")){
					try {
						result=talker.callPrism(propList, modelFile, true, false);
						if(result != null && result.get(0) != null) {
							out.println(talker.computeModelFileName(modelFile));
						} else {
							out.println(PrismRapportTalker.FAILURE);
						}
						
					} catch(Exception e) {
						out.println(PrismRapportTalker.FAILURE);
					}
					continue;
				}
				
				// command for returning state vector after model checking
				if (command.equals("get_vector")){
					try {
						result=talker.callPrism(propList, modelFile, false, true);
						StateVector vect = result.get(0).getVector();
						formattedResult = new ArrayList<String>();
						for (int i = 0; i < vect.getSize(); i++) {
							formattedResult.add(vect.getValue(i).toString());
						}
						vect.clear();
						
						boolean succ = talker.sendResultList(formattedResult, in, out);
						if(!succ) {
							continue;
						}
						
					} catch(Exception e) {
						out.println(PrismRapportTalker.FAILURE);
					}
					continue;
				}
				
				// command for model checking for models with initial state distributions 
				if (command.equals("check_init_dist")) {
					try {
						// get the initial distribution path
						String initDistFile = in.readLine();
						if (initDistFile == null) {
							out.println(PrismRapportTalker.FAILURE);
						}
						
						// make the initial call to prism
						result = talker.callPrism(propList, modelFile, false, true);
						if(result == null || result.get(0) == null) {
							out.println(PrismRapportTalker.FAILURE);
						}
						
						// call the multiplication with initial distribution
						double finalResult = talker.useInitialDistribution(result.get(0), new File(initDistFile));
						out.println(String.valueOf(finalResult));
						
					} catch(Exception e) {
						out.println(PrismRapportTalker.FAILURE);
					}
					continue;
				}
				
			    // command for model checking a list of properties
				if (command.contains("prop_list")) {
					try {
						// Get the initial state distribution if necessary
						String initDistFile = null;
						boolean useInit = false;
						if (command.contains("init_dist")) {
							initDistFile = in.readLine();
							useInit = true;
							if (initDistFile == null) {
								out.println(PrismRapportTalker.FAILURE);
							}
						}
						
						// Make the calls to prism
						result = talker.callPrism(propList, modelFile, false, useInit);
						if(result == null || result.contains(null)) {
							out.println(PrismRapportTalker.FAILURE);
						}
						
						//Now get everything into the format for sending
						formattedResult = new ArrayList<String>();
						for (int i = 0; i < result.size(); i++) {
							if (command.contains("init_dist")) { // Do the computation with the initial distribution
								formattedResult.add(String.valueOf(talker.useInitialDistribution(result.get(i), new File(initDistFile))));
							} else {
								formattedResult.add(String.valueOf(result.get(i).getResult()));
							}
						}
						
						// Now handle the comms with Python (almost the same as get_vector)
						boolean succ = talker.sendResultList(formattedResult, in, out);
						if(!succ) {
							continue;
						}
						
					} catch(Exception e) {
						out.println(PrismRapportTalker.FAILURE);
					}
					continue;
				}
				
			}
		}
		System.exit(0);
	}
}



