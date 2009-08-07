//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

package simulator;

import java.io.*;
import simulator.networking.*;
import userinterface.simulator.networking.*;
import userinterface.*;
/**
 *
 * @author  ug60axh
 */
public class NetTest
{
    
   public static final String EXE_PATH = "/home/students/ug/ug60axh/prism/bin";
   public static final String BINARY_PATH = "/home/students/ug/ug60axh/prism";
   public static final String RESULT_PATH = "/home/students/ug/ug60axh/nettest/results";
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        /*SSHClient client1 = new SSHClient
        ("tw",
         EXE_PATH,
         BINARY_PATH,
         RESULT_PATH);
        SSHClient client2 = new SSHClient
        ("hpc-003",
         EXE_PATH,
         BINARY_PATH,
         RESULT_PATH);
        SSHClient client3 = new SSHClient
        ("hpc-006",
         EXE_PATH,
         BINARY_PATH,
         RESULT_PATH);
        SSHClient client4 = new SSHClient
        ("wallace",
         EXE_PATH,
         BINARY_PATH,
         RESULT_PATH);
        SSHClient client5 = new SSHClient
        ("hpc-005",
         EXE_PATH,
         BINARY_PATH,
         RESULT_PATH);
        SSHClient client6 = new SSHClient
        ("hpc-007",
         EXE_PATH,
         BINARY_PATH,
         RESULT_PATH);
        SSHClient client7 = new SSHClient
        ("hpc-008",
         EXE_PATH,
         BINARY_PATH,
         RESULT_PATH);
        
        client1.setupNetworking(1000000, 2000, "");
        client2.setupNetworking(1000000, 2000, "");
        client3.setupNetworking(1000000, 2000, "");
        client4.setupNetworking(1000000, 2000, "");
        client5.setupNetworking(1000000, 2000, "");
        client6.setupNetworking(1000000, 2000, "");
        client7.setupNetworking(1000000, 2000, "");
        
        client1.start();
        client2.start();
        client3.start();
        client4.start();
        client5.start();
        client6.start();
        client7.start();*/
        
        SimulatorNetworkHandler net = new SimulatorNetworkHandler();
        
        try
        {
			GUIPrism.main(args);
			
            net.loadNetworkFromXML(new File("hpclab.xml"));
			
			
			
			GUINetworkEditor editor = new GUINetworkEditor(null, net);
			
			editor.show();
            //File binary = new File("andrew.bin");
            //System.out.println("Doing networking:");
            //net.doNetworking(100000, 1000, binary);
        }
        catch(Exception e)
        {
            System.err.println("ERRROOORR: "+e);
        }
    }
    
}
