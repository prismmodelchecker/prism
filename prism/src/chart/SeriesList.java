//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
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

package chart;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.*;


/** The MultiGraphOptions dialog box contains this class as a component to describe
 * the list of series.  It is used to highlight a particular data series so that
 * its properties can be set elsewhere.
 */
public class SeriesList extends JList implements Observer
{
    
    //Attributes
    
    //private ArrayList theGraphs;
    //private DefaultListModel listModel;
    
    private MultiGraphModel theModel;
    
    /** Creates a new instance of SeriesList */
    public SeriesList (MultiGraphModel theModel)
    {
        this.theModel = theModel;
        //theGraphs = null;
        //listModel = new DefaultListModel ();
        setSelectionMode (ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setModel (theModel);
        this.setCellRenderer (new SeriesCellRenderer ());
    }
    
    //Access Methods
    
    /** Returns the number of data series'. */    
    public int getNumProperties ()
    {
        return theModel.getSize();//theGraphs.size ();
    }
    
    public ArrayList getSelectedSeries()
    {
        int[] selection = getSelectedIndices();
        
        ArrayList series = new ArrayList();
        for(int i =0; i < selection.length; i++)
        {
            series.add(theModel.getElementAt(selection[i]));
        }
        return series;
    }

	public ArrayList getEditors()
	{
		ArrayList editors = new ArrayList();

		ArrayList ss = getSelectedSeries();

		for(int i = 0; i < ss.size(); i++)
		{
			GraphList gl = (GraphList)ss.get(i);
			editors.add(gl.getEditor());
		}
		return editors;
	}
    
    public void deleteSelectedSeries()
    {
        int[] selection = getSelectedIndices();
        if(selection.length == 0)return;
        for(int i = selection.length-1; i>=0; i--)
        {
            GraphList gl = (GraphList)theModel.getElementAt(selection[i]);
            theModel.removeGraph(gl);
        }
        setGraphsFromModel(theModel);
    }
    
    /**
     *  Selects the first selected in the list and moves it up
     */
    public void moveUp()
    {
        int[] selection = getSelectedIndices();
        
        if(selection.length > 0)
        {
            this.setSelectedIndex(selection[0]);
            if(selection[0] == 0) return;
            else
            {
                theModel.swapGraphs(selection[0], selection[0]-1);
                setSelectedIndex(selection[0]-1);
            }
        }
    }
    
    public void moveDown()
    {
        int[] selection = getSelectedIndices();
        
        if(selection.length > 0)
        {
            this.setSelectedIndex(selection[0]);
            if(selection[0] == theModel.getSize()-1) return;
            else 
            {
                theModel.swapGraphs(selection[0], selection[0]+1);
                setSelectedIndex(selection[0]+1);
            }
        }
    }
    
    //Update
    
    /** Sets the list data elements according to the given MultiGraphModel. 
     *  Now obsolete
     */    
    public void setGraphsFromModel (MultiGraphModel theModel)
    {
        //theGraphs = theModel.getGraphs ();
//        listModel.clear();
//        for(int i = 0; i < theModel.getGraphs().size(); i++)
//        {
//            listModel.addElement (theModel.getGraphs().get (i));
//        }
    }
    
    public void update(Observable o, Object arg)
    {
        repaint();
    }
    
    //Renderer for cells of this list
    
    /** Renderer class needed to display the picture of the data series along with its
     * text.
     */    
    class SeriesCellRenderer extends javax.swing.JPanel implements ListCellRenderer
    {
        private JLabel theLabel;
        private JListPanel thePanel;
        public SeriesCellRenderer ()
        {
            setOpaque (true);
            
            theLabel = new JLabel ();
            theLabel.setOpaque (true);
	    
            thePanel = new JListPanel ();
            thePanel.setOpaque (true);
            setLayout (new BorderLayout ());
            add (theLabel, BorderLayout.CENTER);
            add (thePanel, BorderLayout.WEST);
            thePanel.setPreferredSize (new Dimension (35, this.getHeight ()));
        }
        public java.awt.Component getListCellRendererComponent (JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            theLabel.setFont (new Font ("SansSerif", Font.PLAIN, 12));
            theLabel.setText (((GraphList)theModel.getElementAt (index)).getGraphTitle ());
            Graphics g2 = thePanel.getGraphics ();
            thePanel.setIndex(index);
            thePanel.repaint ();
            
            if(isSelected)
            {
                
                
                theLabel.setForeground (Color.black);
                theLabel.setBackground (new Color (204, 204, 255));
                thePanel.setBackground (new Color (204, 204, 255));
            }
            else
            {
                theLabel.setBackground (Color.white);
                theLabel.setForeground (Color.black);
                thePanel.setBackground (Color.white);
            }
            repaint ();
            
            return this;
            
        }
        
	//Inner class storing what each item should look like
	
	/** Inner class needed to draw the picture of the series. */	
        class JListPanel extends javax.swing.JPanel
        {
            
            private int index;
            public JListPanel ()
            {
                super ();
            }
            
            public void setIndex(int i)
            {
                index = i;
            }
            public void paint (Graphics g)
            {
                Graphics2D g2 = (Graphics2D)g;
                g2.setColor(this.getBackground ());
                g2.fillRect(0,0,getWidth(),getHeight());
                
                if(g2 != null)
                {
                    //g2.setColor(Color.black);
                    AffineTransform t = g2.getTransform ();
                    g2.setColor (((GraphList)theModel.getElementAt (index)).getGraphColour ());
                    float[] dashPattern1 ={ 2, 2 };
                            float[] dashPattern2 ={ 2, 2, 4, 2 };
                            switch(((GraphList)theModel.getElementAt (index)).getLineStyle())
                            {
                                case 0: g2.setStroke(new BasicStroke((float)((GraphList)theModel.getElementAt (index)).getLineWidth())); break;
                                case 1: g2.setStroke(new BasicStroke((float)((GraphList)theModel.getElementAt (index)).getLineWidth(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0F, dashPattern1, 0)); break;
                                case 2: g2.setStroke(new BasicStroke((float)((GraphList)theModel.getElementAt (index)).getLineWidth(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0F, dashPattern2, 0));
                            }
                    if(((GraphList)theModel.getElementAt(index)).isShowLines())g2.drawLine (5,getHeight()/2, 30, getHeight()/2);
                    
                    Shape s = ((GraphList)theModel.getElementAt (index)).getPointShape ();
                    Rectangle2D rec= s.getBounds2D ();
                    g2.translate (17.5, getHeight ()/2);
                    g2.translate (-(rec.getWidth ()/2), -(rec.getHeight ()/2));
                    g2.fill (s);
                    
                    g2.setTransform (t);
                }
            }
        }
        
    }
}
