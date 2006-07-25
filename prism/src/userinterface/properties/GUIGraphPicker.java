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

package userinterface.properties;
import userinterface.GUIPrism;
import userinterface.GUIPlugin;
import javax.swing.*;
import java.awt.*;
import prism.*;
import parser.*;
import java.util.*;
import settings.*;
/**
 *
 * @author  ug60axh
 */
public class GUIGraphPicker extends javax.swing.JDialog
{
    private ResultsCollection rc;
    private GraphConstantPickerList pickerList;
    private GUIGraphHandler gh;
    private GUIPrism gui;
    private GUIPlugin plugin;
    
    //Values to be gotten from elsewhere
    private String ranger;
    private Values otherValues;
    private ArrayList multiSeries;
    
    private chart.MultiGraphModel mgm;
    private DefinedConstant c;
    private boolean shouldDo = false;
    
    private static final int MAX_NUM_SERIES_BEFORE_QUERY = 11;
    
    /** Creates new form GUIGraphPicker */
    public GUIGraphPicker(GUIPrism parent, GUIPlugin plugin, ResultsCollection rc, GUIGraphHandler gh)
    {
        super(parent, true);
        this.gui = parent;
        this.plugin = plugin;
        setTitle("New Graph Series");
        this.rc = rc;
        this.gh = gh;
        multiSeries = new ArrayList();
        initComponents();
        setResizable(false);
        init();
		setLocationRelativeTo(getParent()); // centre
    }
    
    public void startGraphExperiment(GUIExperimentTable et, int experimentIndex)
    {
        show();
        if(shouldDo)
        {
            //Determine whether multiple series are required
            DefinedConstant[]ms = new DefinedConstant[multiSeries.size()];
            Vector rangers = rc.getRangingConstants();
            for(int i = 0; i < ms.length; i++)
            {
                String str = (String)multiSeries.get(i);
                for(int j = 0; j < rangers.size(); j++)
                {
                    DefinedConstant c = (DefinedConstant)rangers.get(j);
                    if(c.getName().equals(str))
                    {
                        ms[i] = c;
                    }
                }
            }
            //Collate a collection of Values objects representing each combination
            ArrayList values = new ArrayList();
            values.add(otherValues);
            for(int i = 0; i < ms.length; i++)
            {
                ArrayList temp = (ArrayList)values.clone();
                values.clear();
                for(int j = 0; j <ms[i].getNumSteps(); j++)
                {
                    ArrayList copy = (ArrayList)temp.clone();
                    for(int k = 0; k < copy.size(); k++)
                    {
                        Values v = new Values();
                        Values cp = (Values)copy.get(k);
                        v.addValues(cp);
                        v.addValue(ms[i].getName(), ms[i].getValue(j));
                        values.add(v);
                    }
                }
            }
            
            ArrayList seriesHeaders = new ArrayList();
            for(int i = 0; i < values.size(); i++)
            {
                Values v = (Values)values.get(i);
                if(values.size() > 1)
                {
                    seriesHeaders.add(v.toString());
                }
                else
                {
                    seriesHeaders.add(seriesNameField.getText());
                }
            }
            
            et.startExperiment(experimentIndex,mgm,ranger,values,seriesHeaders);
            gh.jumpToGraph(mgm);
            gh.graphIsChanging(mgm);
        }
        else
        {
            et.startExperiment(experimentIndex);
        }
    }
    
    public void pickNewSeries()
    {
        show();
        
        if(shouldDo)
        {
            //Determine whether multiple series are required
            DefinedConstant[]ms = new DefinedConstant[multiSeries.size()];
            Vector rangers = rc.getRangingConstants();
            for(int i = 0; i < ms.length; i++)
            {
                String str = (String)multiSeries.get(i);
                for(int j = 0; j < rangers.size(); j++)
                {
                    DefinedConstant cc = (DefinedConstant)rangers.get(j);
                    if(cc.getName().equals(str))
                    {
                        ms[i] = cc;
                    }
                }
            }
            //Collate a collection of Values objects representing each combination
            ArrayList values = new ArrayList();
            values.add(otherValues);
            for(int i = 0; i < ms.length; i++)
            {
                ArrayList temp = (ArrayList)values.clone();
                values.clear();
                for(int j = 0; j <ms[i].getNumSteps(); j++)
                {
                    ArrayList copy = (ArrayList)temp.clone();
                    for(int k = 0; k < copy.size(); k++)
                    {
                        Values v = new Values();
                        Values cp = (Values)copy.get(k);
                        v.addValues(cp);
                        v.addValue(ms[i].getName(), ms[i].getValue(j));
                        values.add(v);
                    }
                }
            }
            
            for(int i = 0; i < values.size(); i++)
            {
                Values v = (Values)values.get(i);
            }
            //System.exit(-1);
            
            for(int comb = 0; comb < values.size(); comb++) //each combination of series
            {
                
                Values v = (Values)values.get(comb);
                String seriesName = seriesNameField.getText();
                if(values.size() > 1) seriesName = v.toString();
                int series = -1;
                try
                {
                    series = mgm.addGraph(seriesName);
                }
                catch(SettingException e)
                {
                    //do nothing
                }
                for(int i = 0; i < c.getNumSteps(); i++)
                {
                    try
                    {
                        Object value = c.getValue(i);
                        Values useThis = new Values();
                        useThis.addValues(v);
                        useThis.addValue(ranger, value);
                        Object result = rc.getResult(useThis);
                        double x;
                        double y;
                        if(value instanceof Double)
                        {
                            x = ((Double)value).doubleValue();
                        }
                        else if (value instanceof Integer)
                        {
                            x = ((Integer)value).intValue();
                        }
                        else
                        {
                            throw new PrismException("Not a double or an integer, tis a " + value.getClass().toString());
                        }
                        if(result instanceof Double)
                        {
                            y = ((Double)result).doubleValue();
                        }
                        else if(result instanceof Integer)
                        {
                            y = ((Integer)result).intValue();
                        }
                        else throw new PrismException("Not a double or an integer, tis a " + value.getClass().toString());
                        try
                        {
                            mgm.addPoint(series, new chart.GraphPoint(x,y,mgm), false, true, true);
                        }
                        catch(SettingException e)
                        {
                            //do nothing
                        }
                        
                    }
                    catch(PrismException e)
                    {
                    }
                }
                rc.addWholeGraph(mgm, series);
            }
        }
        gh.jumpToGraph(mgm);
        gh.graphIsChanging(mgm);
    }
    
    /** According to what is stored in 'rc', set up the table to pick the constants
     */
    private void init()
    {
        // set up "define other constants" table
        // create header
        GraphConstantHeader header = new GraphConstantHeader();
        constantTablePanel.add(header, BorderLayout.NORTH);
        // create scroller
        JScrollPane scroller = new JScrollPane();
        constantTablePanel.add(scroller, BorderLayout.CENTER);
        // create picker list
        pickerList = new GraphConstantPickerList();
        scroller.setViewportView(pickerList);
        
        // for each ranging constant in rc, add:
        // (1) a row in the picker list
        // (2) an item in the "x axis" drop down menu
        for(int i = 0; i < rc.getRangingConstants().size(); i++)
        {
            DefinedConstant dc = (DefinedConstant)rc.getRangingConstants().get(i);
            pickerList.addConstant(new GraphConstantLine(dc, this));
            this.selectAxisConstantCombo.addItem(dc.getName());
        }
        
        // select the first constant for the x axis
        if(selectAxisConstantCombo.getItemCount() > 0) selectAxisConstantCombo.setSelectedIndex(0);
        // and disable it in the picker list
        pickerList.disableLine(0);
        
        // if there is only one ranging constant, disable controls
        if(rc.getRangingConstants().size() == 1)
        {
            selectAxisConstantCombo.setEnabled(false);
            pickerList.setEnabled(false);
            header.setEnabled(false);
            this.middleLabel.setEnabled(false);
            this.topComboLabel.setEnabled(false);
        }
        
        // default graph option is "new graph"
        this.newGraphRadio.setSelected(true);
        
        // add existing graphs to choose from
        for(int i = 0; i < gh.getNumModels(); i++)
        {
            existingGraphCombo.addItem(gh.getGraphName(i));
        }
        // if there are no graphs, disable control
        if(gh.getNumModels() == 0)
        {
            existingGraphCombo.setEnabled(false);
            this.existingGraphRadio.setEnabled(false);
        }
        
        // create a default series name
        resetAutoSeriesName();
        
        // other enables/disables
        doEnables();
        
        pack();
    }
    
    public void doEnables()
    {
        this.existingGraphCombo.setEnabled(this.existingGraphRadio.isSelected());
    }
    
    // create a default series name
    public void resetAutoSeriesName()
    {
        DefinedConstant temp;
        Object value;
        
        if(selectAxisConstantCombo.getSelectedItem() == null)
        {
            return;
        }
        
        // see which constant is on x axis
        ranger = selectAxisConstantCombo.getSelectedItem().toString();
        // init arrays
        otherValues = new Values();
        multiSeries = new ArrayList();
        // go through constants in picker list
        for(int j = 0; j < pickerList.getNumConstants(); j++)
        {
            // get constant
            temp = pickerList.getConstantLine(j).getDC();
            // ignore constant for x-axis
            if(temp.getName().equals(ranger)) continue;
            // get value
            value = pickerList.getConstantLine(j).getSelectedValue();
            // if we find any constants selected "All Series", clear name, disable and bail out
            if(value instanceof String)
            {
                this.seriesNameLabel.setEnabled(false);
                this.seriesNameField.setText("");
                this.seriesNameField.setEnabled(false);
                return;
            }
            // we add other constants to a list
            else
            {
                otherValues.addValue(temp.getName(), value);
            }
        }
        // use values object string for name
        if(otherValues.getNumValues() != 0)
        {
            this.seriesNameField.setText(otherValues.toString());
        }
        else
        {
            this.seriesNameField.setText("New Series");
        }
        this.seriesNameLabel.setEnabled(true);
        this.seriesNameField.setEnabled(true);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;
        
        buttonGroup1 = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        topComboLabel = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        selectAxisConstantCombo = new javax.swing.JComboBox();
        jPanel7 = new javax.swing.JPanel();
        middleLabel = new javax.swing.JLabel();
        constantTablePanel = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        newGraphRadio = new javax.swing.JRadioButton();
        existingGraphRadio = new javax.swing.JRadioButton();
        jPanel11 = new javax.swing.JPanel();
        existingGraphCombo = new javax.swing.JComboBox();
        jPanel12 = new javax.swing.JPanel();
        seriesNameLabel = new javax.swing.JLabel();
        seriesNameField = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        lineOkayButton = new javax.swing.JButton();
        lineCancelButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });
        
        jTabbedPane1.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        jPanel1.setLayout(new java.awt.BorderLayout());
        
        jPanel1.setBorder(new javax.swing.border.TitledBorder("Line Graph"));
        jPanel1.setFocusable(false);
        jPanel1.setEnabled(false);
        jPanel3.setLayout(new java.awt.GridBagLayout());
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel3.add(jPanel5, gridBagConstraints);
        
        topComboLabel.setText("Select x axis constant:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(topComboLabel, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        jPanel3.add(jPanel6, gridBagConstraints);
        
        selectAxisConstantCombo.setPreferredSize(new java.awt.Dimension(100, 24));
        selectAxisConstantCombo.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                selectAxisConstantComboActionPerformed(evt);
            }
        });
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel3.add(selectAxisConstantCombo, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        jPanel3.add(jPanel7, gridBagConstraints);
        
        middleLabel.setText("Define other constants:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(middleLabel, gridBagConstraints);
        
        constantTablePanel.setLayout(new java.awt.BorderLayout());
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel3.add(constantTablePanel, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        jPanel3.add(jPanel9, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        jPanel3.add(jPanel10, gridBagConstraints);
        
        jLabel3.setText("Add Series to:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(jLabel3, gridBagConstraints);
        
        newGraphRadio.setText("New Graph");
        buttonGroup1.add(newGraphRadio);
        newGraphRadio.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                newGraphRadioActionPerformed(evt);
            }
        });
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(newGraphRadio, gridBagConstraints);
        
        existingGraphRadio.setText("Existing Graph");
        buttonGroup1.add(existingGraphRadio);
        existingGraphRadio.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                existingGraphRadioActionPerformed(evt);
            }
        });
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(existingGraphRadio, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        jPanel3.add(jPanel11, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(existingGraphCombo, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        jPanel3.add(jPanel12, gridBagConstraints);
        
        seriesNameLabel.setText("Series name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(seriesNameLabel, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel3.add(seriesNameField, gridBagConstraints);
        
        jPanel1.add(jPanel3, java.awt.BorderLayout.CENTER);
        
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        
        lineOkayButton.setText("Okay");
        lineOkayButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                lineOkayButtonActionPerformed(evt);
            }
        });
        
        jPanel4.add(lineOkayButton);
        
        lineCancelButton.setText("Cancel");
        lineCancelButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                lineCancelButtonActionPerformed(evt);
            }
        });
        
        jPanel4.add(lineCancelButton);
        
        jPanel1.add(jPanel4, java.awt.BorderLayout.SOUTH);
        
        //jTabbedPane1.addTab("", GUIPrism.getIconFromImage("lineGraph.gif"), jPanel1);
        
        jPanel2.setBorder(new javax.swing.border.TitledBorder("Bar Graph"));
        jPanel2.setEnabled(false);
        //jTabbedPane1.addTab("", GUIPrism.getIconFromImage("barGraph.gif"), jPanel2);
        
        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);
        
        pack();
    }
    
    private void lineCancelButtonActionPerformed(java.awt.event.ActionEvent evt)
    {
        shouldDo = false;
        
        hide();
    }
    
    private void lineOkayButtonActionPerformed(java.awt.event.ActionEvent evt)
    {
        DefinedConstant temp;
        int numSeries = 1;
        
        // see which constant is on x axis
        ranger = selectAxisConstantCombo.getSelectedItem().toString();
        // init arrays
        otherValues = new Values();
        multiSeries = new ArrayList();
        // go through all constants in picker list
        for(int j = 0; j < pickerList.getNumConstants(); j++)
        {
            // get constant
            temp = pickerList.getConstantLine(j).getDC();
            // if its the constant for the x-axis, store info about the constant
            if(temp.getName().equals(ranger))
            {
                c = temp;
            }
            // otherwise store info about the selected values
            else
            {
                // get value
                Object value = pickerList.getConstantLine(j).getSelectedValue();
                if(value instanceof String)
                {
                    multiSeries.add(pickerList.getConstantLine(j).getName());
                    numSeries *= temp.getNumSteps();
                }
                else
                {
                    otherValues.addValue(temp.getName(), value);
                }
            }
        }
        //sort out which one to add it to
        if(c == null) return;
        
        // if there are a lot of series, check if this is what the user really wanted
        if (numSeries > MAX_NUM_SERIES_BEFORE_QUERY)
        {
            String[] choices =
            {"Yes", "No"};
            int choice = -1;
            choice = plugin.optionPane("Warning: This will plot "+numSeries+" series.\nAre you sure you want to continue?", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, choices, choices[0]);
            if (choice != 0) return;
        }
        
        if(this.newGraphRadio.isSelected())
        {
            try
            {
                int gi = gh.addGraph(rc.getResultName());
                
                mgm = gh.getModel(gi);
                mgm.setXTitle(ranger);
            }
            catch(SettingException e)
            {
                //do nothing
            }
        }
        else // do it to add to an existing graph
        {
            mgm = gh.getModel(existingGraphCombo.getSelectedItem().toString());
            if(!ranger.equals(mgm.getXTitle()))//must do this better in future
            {
                try
                {
                if(!roughExists(ranger, mgm.getXTitle()))mgm.setXTitle(mgm.getXTitle()+", "+ranger);
                }
                catch(SettingException e)
                {}
            }
        }
        shouldDo = true;
        hide();
        
    }
    
    private void existingGraphRadioActionPerformed(java.awt.event.ActionEvent evt)
    {
        doEnables();
    }
    
    private void newGraphRadioActionPerformed(java.awt.event.ActionEvent evt)
    {
        doEnables();
    }
    
    private void selectAxisConstantComboActionPerformed(java.awt.event.ActionEvent evt)
    {
        pickerList.disableLine(selectAxisConstantCombo.getSelectedIndex());
        resetAutoSeriesName();
    }
    
    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt)
    {
        setVisible(false);
        dispose();
    }
    
    // Variables declaration - do not modify
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JPanel constantTablePanel;
    private javax.swing.JComboBox existingGraphCombo;
    private javax.swing.JRadioButton existingGraphRadio;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JButton lineCancelButton;
    private javax.swing.JButton lineOkayButton;
    private javax.swing.JLabel middleLabel;
    private javax.swing.JRadioButton newGraphRadio;
    private javax.swing.JComboBox selectAxisConstantCombo;
    private javax.swing.JTextField seriesNameField;
    private javax.swing.JLabel seriesNameLabel;
    private javax.swing.JLabel topComboLabel;
    // End of variables declaration
    
    public static int factorial(int i)
    {
        if(i < 0)return 1;
        if(i == 0)return 1;
        else
            return i * factorial(i-1);
    }
    
    public static boolean roughExists(String test, String inThis)
    {
        int i = inThis.indexOf(test);
        if(i == -1) return false;
        if(!((i == 0) || (inThis.charAt(i-1) == ' '))) return false;
        if(!((inThis.length() == i+1) || (inThis.charAt(i+1) == ','))) return false;
        return true;
    }
    
    public static void main(String[] args)
    {
        System.out.println("testing: a in a: "+ roughExists("a","a"));
        System.out.println("testing: a in a, b: "+ roughExists("a","a, b"));
        System.out.println("testing: a in b, a: "+ roughExists("a","b, a"));
        System.out.println("testing: a in b, a, c: "+ roughExists("a","b, a, c"));
        System.out.println("testing: a in b: "+ roughExists("a","b"));
        System.out.println("testing: a in b, c: "+ roughExists("a","b, c"));
        System.out.println("testing: a in flkjda: "+ roughExists("a","flkjda"));
        System.out.println("testing: a in asdsd: "+ roughExists("a","asdsd"));
        System.out.println("testing: a in dfdfafdf: "+ roughExists("a","dfdfafdf"));
    }
}
