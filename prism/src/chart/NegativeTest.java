//==============================================================================
//
//	Copyright (c) 2002-2004, Andrew Hinton, Dave Parker
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

package chart;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import settings.*;

public class NegativeTest extends javax.swing.JFrame
{
    
    public NegativeTest()
    {
        super("Graph Test 1");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                System.exit(0);
            }
        });
       
        MultiGraphModel theModel;
       try
        {
            theModel = new MultiGraphModel().load(new File("c:\\test2.xml"));
        }
        catch(Exception e)
        {
            theModel = new MultiGraphModel();
            System.err.println(e.getMessage());
        }
        
        
        //Set up the graph model
	try
	{
         theModel = new MultiGraphModel();
        theModel.setXTitle("x");
        theModel.setYTitle("f(x), g(x)");
        int j = theModel.addGraph ("f(x) = x^3");
        for(double i = -10; i <= 10; i = i + 0.5)
        {
            double x = i;
            double y = Math.pow(x, 3);
            theModel.addPoint(j, new GraphPoint(x, y, theModel), false);
        }
        j = theModel.addGraph ("g(x) = x^2");
        for(double i = -10; i <= 10; i = i + 0.5)
        {
            double x = i;
            double y = Math.pow(x, 2);
            theModel.addPoint(j, new GraphPoint(x, y, theModel), false);
        }
        theModel.setGraphTitle ("A Test Graph");
        
       
       //ColorChooserComponentFactory
        
        MultiGraphView graph = new MultiGraphView(theModel);
        theModel.addObserver(graph);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(graph, BorderLayout.CENTER);
        getContentPane().setSize(new Dimension(400,400));
        graph.setPreferredSize (new Dimension(400,400));
        theModel.setMinX(-10);
        theModel.setMinY(-1000);
        theModel.setMaxX(10);
        theModel.setMaxY(1000);
        theModel.setMajorXInterval(1);
        theModel.setMajorYInterval(100);
        theModel.setMinorXInterval(0.5);
        theModel.setMinorYInterval(50);
	}
	catch(SettingException eee)
	{
	    System.out.println("SEttinException: "+eee.getMessage());
	}
	pack();
        
        javax.swing.JFrame op = new javax.swing.JFrame("Graph Options Test");
        
        GraphOptionsPanel gop = new GraphOptionsPanel(op, theModel);
        
        op.getContentPane().add(gop);
        
        op.pack();
        op.show();
        op.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        
        //MultiGraphOptions op = new MultiGraphOptions(theModel);
        //op.show();
        setLocation((int)op.getLocationOnScreen().getX()+op.getWidth(), (int)op.getLocationOnScreen().getY()+50);
        /*
        try
        {
            File f = new File("c:\\test2.xml");
            MultiGraphModel.save(theModel, f);
        }
        catch(Exception e)
        {
            System.err.println(e.getMessage());
        }*/
    }
    public static void main(String[]args)
    {
        new NegativeTest().show();
    }
}

