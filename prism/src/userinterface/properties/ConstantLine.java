//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//  * Mark Kattenbelt <mxk@comlab.ox.ac.uk> (University of Oxford)
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

import java.awt.*;

import prism.*;
import parser.type.*;

public class ConstantLine extends javax.swing.JPanel
{
	public static final String SINGLE_DEFAULT = "0";
	public static final String RANGE_START_DEFAULT = "0";
	public static final String RANGE_END_DEFAULT = "1";
	public static final String STEP_DEFAULT = "1";
	
	private Type type;
	
	/** Creates new form ConstantLine */
	public ConstantLine(String name, Type type)
	{
	initComponents();
	//setBorder(new BottomBorder());
	setPreferredSize(new Dimension(1, getFontMetrics(getFont()).getHeight() + 4));
	setConstName(name);
	setConstType(type);
	doDefaults();
	}
	
	public void setConstName(String str)
	{
	nameLabel.setText(str);
	}
	
	public void setConstType(Type type)
	{
		this.type = type;
		if (type instanceof TypeBool) {
			typeLabel.setText("bool");
			rangeCombo.setEnabled(false);
			singleValueCombo.setSelected(true);
			
			remove(singleValueField);
			remove(boolSingleValueCombo);
			remove(sizerPanel);
			java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 3;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 0.2;
			gridBagConstraints.ipadx = 5;
			add(boolSingleValueCombo, gridBagConstraints);
			//add(sizerPanel, gridBagConstraints);
		}
		else if (type instanceof TypeDouble) {
			typeLabel.setText("double");
			rangeCombo.setEnabled(true);
			singleValueCombo.setSelected(true);
			
			remove(singleValueField);
			remove(boolSingleValueCombo);
			remove(sizerPanel);
			java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 3;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 0.2;
			gridBagConstraints.ipadx = 5;
			add(singleValueField, gridBagConstraints);
		}
		else if (type instanceof TypeInt) {
			typeLabel.setText("int");
			rangeCombo.setEnabled(true);
			singleValueCombo.setSelected(true);
			remove(singleValueField);
			remove(boolSingleValueCombo);
			remove(sizerPanel);
			java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 3;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 0.2;
			gridBagConstraints.ipadx = 5;
			add(singleValueField, gridBagConstraints);
		}
		else {
			typeLabel.setText("unknown");
			rangeCombo.setEnabled(true);
			singleValueCombo.setSelected(true);
			remove(singleValueField);
			remove(boolSingleValueCombo);
			remove(sizerPanel);
			java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 3;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 0.2;
			add(singleValueField, gridBagConstraints);
		}
	}
	
	public void doDefaults()
	{
	singleValueField.setText(SINGLE_DEFAULT);
	startValueField.setText(RANGE_START_DEFAULT);
	endValueField.setText(RANGE_END_DEFAULT);
	stepValueField.setText(STEP_DEFAULT);
	singleValueCombo.setSelected(true);
	
	doEnables();
	}
	
	public void doEnables()
	{
	if(singleValueCombo.isSelected())
	{
		startValueField.setEnabled(false);
		endValueField.setEnabled(false);
		stepValueField.setEnabled(false);
		boolSingleValueCombo.setEnabled(true);
		singleValueField.setEnabled(true);
	}
	else if(rangeCombo.isSelected())
	{
		startValueField.setEnabled(true);
		endValueField.setEnabled(true);
		stepValueField.setEnabled(true);
		boolSingleValueCombo.setEnabled(false);
		singleValueField.setEnabled(false);
	}
	}
	
	//ACCESS METHODS
	
	public Type getType()
	{
		return type;
	}
	
	public boolean isRange()
	{
		return rangeCombo.isSelected();
	}
	
	public void setIsRange(boolean b)
	{
		rangeCombo.setSelected(b);
		singleValueCombo.setSelected(!b);
		doEnables();
	}
	
	public String getSingleValue()
	{
		if(type instanceof TypeBool)
		{
			return boolSingleValueCombo.getSelectedItem().toString();
		}
		else
		{
			return singleValueField.getText();
		}
	}
	
	public void setSingleValue(String str)
	{
		if(type instanceof TypeBool)
		{
			if (str != null) boolSingleValueCombo.setSelectedIndex(str.equals("true") ? 0 : 1);
			else boolSingleValueCombo.setSelectedIndex(0); // default
		}
		else
		{
			if (str != null) singleValueField.setText(str);
			else singleValueField.setText(SINGLE_DEFAULT);
		}
	}
	
	public String getStartValue()
	{
		return startValueField.getText();
	}
	
	public void setStartValue(String str)
	{
		startValueField.setText((str != null) ? str : RANGE_START_DEFAULT);
	}
	
	public String getEndValue()
	{
		return endValueField.getText();
	}
	
	public void setEndValue(String str)
	{
		endValueField.setText((str != null) ? str : RANGE_END_DEFAULT);
	}
	
	public String getStepValue()
	{
		return stepValueField.getText();
	}
	
	public void setStepValue(String str)
	{
		stepValueField.setText((str != null) ? str : STEP_DEFAULT);
	}
	
	public String getName()
	{
		return nameLabel.getText();
	}
	
	public void checkValid() throws PrismException
	{
		String s = "";
		
		if(type instanceof TypeBool)
		{
			s = boolSingleValueCombo.getSelectedItem().toString();
			if (!(s.equals("true") | s.equals("false"))) {
				throw new PrismException("Invalid value \""+s+"\" for Boolean constant \""+getName()+"\"");
			}
		}
		else if(type instanceof TypeInt)
		{
			try {
				s = singleValueField.getText(); Integer.parseInt(s);
				if (isRange()) {
					s = startValueField.getText(); Integer.parseInt(s);
					s = endValueField.getText(); Integer.parseInt(s);
					s = stepValueField.getText(); Integer.parseInt(s);
				}
			} catch (NumberFormatException e) {
				throw new PrismException("Invalid value \""+s+"\" for integer constant \""+getName()+"\"");
			}
		}
		else if(type instanceof TypeDouble)
		{
			try {
				s = singleValueField.getText(); Double.parseDouble(s);
				if (isRange()) {
					s = startValueField.getText(); Double.parseDouble(s);
					s = endValueField.getText(); Double.parseDouble(s);
					s = stepValueField.getText(); Double.parseDouble(s);
				}
			} catch (NumberFormatException e) {
				throw new PrismException("Invalid value \""+s+"\" for double constant \""+getName()+"\"");
			}
		}
		else {
			// should never happen
			throw new PrismException("Unknown type for constant \""+getName()+"\"");
		}
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        choiceButtonGroup = new javax.swing.ButtonGroup();
        boolSingleValueCombo = new javax.swing.JComboBox();
        sizerPanel = new javax.swing.JPanel();
        nameLabel = new javax.swing.JLabel();
        typeLabel = new javax.swing.JLabel();
        singleValueField = new javax.swing.JTextField();
        startValueField = new javax.swing.JTextField();
        endValueField = new javax.swing.JTextField();
        stepValueField = new javax.swing.JTextField();
        singleValueCombo = new javax.swing.JRadioButton();
        rangeCombo = new javax.swing.JRadioButton();

        boolSingleValueCombo.setBackground(new java.awt.Color(255, 255, 255));
        boolSingleValueCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "true", "false" }));
        boolSingleValueCombo.setMinimumSize(new java.awt.Dimension(4, 19));
        boolSingleValueCombo.setPreferredSize(new java.awt.Dimension(4, 19));

        setLayout(new java.awt.GridBagLayout());

        setBackground(new java.awt.Color(255, 255, 255));
        setPreferredSize(new java.awt.Dimension(640, 23));
        nameLabel.setText("a");
        nameLabel.setMaximumSize(new java.awt.Dimension(100, 15));
        nameLabel.setMinimumSize(new java.awt.Dimension(50, 15));
        nameLabel.setPreferredSize(new java.awt.Dimension(100, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.1;
        add(nameLabel, gridBagConstraints);

        typeLabel.setText("double");
        typeLabel.setMaximumSize(new java.awt.Dimension(150, 15));
        typeLabel.setMinimumSize(new java.awt.Dimension(50, 15));
        typeLabel.setPreferredSize(new java.awt.Dimension(100, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.1;
        add(typeLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.weightx = 0.2;
        add(singleValueField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.weightx = 0.2;
        add(startValueField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.weightx = 0.2;
        add(endValueField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.weightx = 0.2;
        add(stepValueField, gridBagConstraints);

        singleValueCombo.setBackground(new java.awt.Color(255, 255, 255));
        choiceButtonGroup.add(singleValueCombo);
        singleValueCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                singleValueComboActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 5;
        add(singleValueCombo, gridBagConstraints);

        rangeCombo.setBackground(new java.awt.Color(255, 255, 255));
        choiceButtonGroup.add(rangeCombo);
        rangeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rangeComboActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 5;
        add(rangeCombo, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents
	
	private void rangeComboActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rangeComboActionPerformed
	{//GEN-HEADEREND:event_rangeComboActionPerformed
	doEnables();
	}//GEN-LAST:event_rangeComboActionPerformed
	
	private void singleValueComboActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_singleValueComboActionPerformed
	{//GEN-HEADEREND:event_singleValueComboActionPerformed
	doEnables();
	}//GEN-LAST:event_singleValueComboActionPerformed
	
	
    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JComboBox boolSingleValueCombo;
    private javax.swing.ButtonGroup choiceButtonGroup;
    javax.swing.JTextField endValueField;
    javax.swing.JLabel nameLabel;
    javax.swing.JRadioButton rangeCombo;
    javax.swing.JRadioButton singleValueCombo;
    javax.swing.JTextField singleValueField;
    javax.swing.JPanel sizerPanel;
    javax.swing.JTextField startValueField;
    javax.swing.JTextField stepValueField;
    javax.swing.JLabel typeLabel;
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
