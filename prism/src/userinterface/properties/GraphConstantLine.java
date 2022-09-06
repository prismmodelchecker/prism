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

package userinterface.properties;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.UIManager;

import parser.type.*;
import prism.DefinedConstant;

public class GraphConstantLine extends javax.swing.JPanel
{
    public static final String SINGLE_DEFAULT = "0";
    public static final String RANGE_START_DEFAULT = "0";
    public static final String RANGE_END_DEFAULT = "1";
    public static final String STEP_DEFAULT = "1";
    
    private DefinedConstant dc;
    private GUIGraphPicker parent;
    private Type type;
    
    /** Creates new form ConstantLine */
    public GraphConstantLine(DefinedConstant dc, GUIGraphPicker parent)
    {
        this.dc = dc;
        this.parent = parent;
        
        initComponents();
        this.valuePicker.setRenderer(new DefaultListCellRenderer()
        {
            public Component getListCellRendererComponent(JList list, Object obj, int i, boolean selection, boolean hasFocus)
            {
                if(obj instanceof Double)
                {
                    setComponentOrientation(list.getComponentOrientation());
                    if (selection)
                    {
                        setBackground(list.getSelectionBackground());
                        setForeground(list.getSelectionForeground());
                    }
                    else
                    {
                        setBackground(list.getBackground());
                        setForeground(list.getForeground());
                    }
                    
                    if (obj instanceof Icon)
                    {
                        setIcon((Icon)obj);
                        setText("");
                    }
                    else
                    {
                        setIcon(null);
                        String str;
                        Double d = (Double) obj;
                        NumberFormat n = DecimalFormat.getInstance(Locale.UK);
                        n.setMaximumFractionDigits(6);
                        str = n.format(d.doubleValue());
                        setText((obj == null) ? "" : str);
                    }
                    
                    setEnabled(list.isEnabled());
                    setFont(list.getFont());
                    setBorder((hasFocus) ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
                    
                    return this;
                }
                else
                {
                    return super.getListCellRendererComponent(list, obj, i, selection, hasFocus);
                }
            }
        });
        setBorder(new BottomBorder());
        setConstName(dc.getName());
        setConstType(dc.getType());
        init();
    }
    
    private void init()
    {
        
        valuePicker.addItem("All series");
        
        for(int i = 0; i < dc.getNumSteps(); i++)
        {
            valuePicker.addItem(dc.getValue(i));
        }
        
        
    }
    
    public void setConstName(String str)
    {
        nameLabel.setText(str);
    }
    
    public void setConstType(Type type)
    {
    	this.type = type;
    	if (type instanceof TypeDouble) {
    		typeLabel.setText("double");
    	} else if (type instanceof TypeInt) {
    		typeLabel.setText("int");
    	} else {
    		typeLabel.setText("unknown");
        }
    }
    
    public void setEnabled(boolean b)
    {
        //System.out.println("setEnabloed called on a GraphConstantPickerList");
        super.setEnabled(b);
        nameLabel.setEnabled(b);
        typeLabel.setEnabled(b);
        valuePicker.setEnabled(b);
        if(b)
        {
            setBackground(Color.white);
            jPanel1.setBackground(Color.white);
            jPanel2.setBackground(Color.white);
            jPanel3.setBackground(Color.white);
        }
        else
        {
            setBackground(Color.lightGray);
            jPanel1.setBackground(Color.lightGray);
            jPanel2.setBackground(Color.lightGray);
            jPanel3.setBackground(Color.lightGray);
        }
    }
    
    //ACCESS METHODS
    
    
    public String getName()
    {
        return nameLabel.getText();
    }
    
    public Object getSelectedValue()
    {
        return valuePicker.getSelectedItem();
    }
    
    public DefinedConstant getDC()
    {
        return dc;
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        choiceButtonGroup = new javax.swing.ButtonGroup();
        boolSingleValueCombo = new javax.swing.JComboBox();
        sizerPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        nameLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        typeLabel = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        valuePicker = new javax.swing.JComboBox();

        boolSingleValueCombo.setBackground(new java.awt.Color(255, 255, 255));
        boolSingleValueCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "true", "false" }));
        boolSingleValueCombo.setMinimumSize(new java.awt.Dimension(4, 19));
        boolSingleValueCombo.setPreferredSize(new java.awt.Dimension(4, 19));

        setLayout(new java.awt.GridBagLayout());

        setBackground(new java.awt.Color(255, 255, 255));
        setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        setPreferredSize(new java.awt.Dimension(300, 26));
        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        add(jPanel1, gridBagConstraints);

        nameLabel.setText("a");
        nameLabel.setPreferredSize(new java.awt.Dimension(30, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.3;
        add(nameLabel, gridBagConstraints);

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        add(jPanel2, gridBagConstraints);

        typeLabel.setText("double");
        typeLabel.setPreferredSize(new java.awt.Dimension(50, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(typeLabel, gridBagConstraints);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        add(jPanel3, gridBagConstraints);

        valuePicker.setBackground(new java.awt.Color(255, 255, 255));
        valuePicker.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                valuePickerActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.7;
        add(valuePicker, gridBagConstraints);

    }//GEN-END:initComponents
    
    private void valuePickerActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_valuePickerActionPerformed
    {//GEN-HEADEREND:event_valuePickerActionPerformed
        parent.resetAutoSeriesName();
    }//GEN-LAST:event_valuePickerActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox boolSingleValueCombo;
    private javax.swing.ButtonGroup choiceButtonGroup;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JPanel sizerPanel;
    private javax.swing.JLabel typeLabel;
    private javax.swing.JComboBox valuePicker;
    // End of variables declaration//GEN-END:variables
    
    class BottomBorder implements javax.swing.border.Border
    {
        public Insets getBorderInsets(Component c)
        {
            return new Insets(0,0,1,0);
        }
        
        public boolean isBorderOpaque()
        {
            return true;
        }
        
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
        {
            g.setColor(Color.lightGray);
            g.drawLine(x,(y+height-1), (x+width), (y+height-1));
            
        }
    }
}
