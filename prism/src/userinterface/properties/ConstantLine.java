//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

import parser.Expression;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import java.lang.*;
import prism.*;
/**
 *
 * @author  ug60axh
 */
public class ConstantLine extends javax.swing.JPanel
{
	public static final String SINGLE_DEFAULT = "0";
	public static final String RANGE_START_DEFAULT = "0";
	public static final String RANGE_END_DEFAULT = "1";
	public static final String STEP_DEFAULT = "1";
	
	/** Creates new form ConstantLine */
	public ConstantLine(String name, int type)
	{
	initComponents();
	setBorder(new BottomBorder());
	setPreferredSize(new Dimension(1, getFontMetrics(getFont()).getHeight() + 4));
	setConstName(name);
	setConstType(type);
	doDefaults();
	}
	
	public void setConstName(String str)
	{
	nameLabel.setText(str);
	}
	
	public void setConstType(int type)
	{
	switch(type)
	{
		case Expression.BOOLEAN:
		{
		typeLabel.setText("bool");
		rangeCombo.setEnabled(false);
		singleValueCombo.setSelected(true);
		
		remove(singleValueField);
		remove(boolSingleValueCombo);
		remove(sizerPanel);
		java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 6;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 0.2;
		gridBagConstraints.ipadx = 1;
		add(boolSingleValueCombo, gridBagConstraints);
		add(sizerPanel, gridBagConstraints);
		
		break;
		}
		case Expression.DOUBLE:
		{
		typeLabel.setText("double");
		rangeCombo.setEnabled(true);
		singleValueCombo.setSelected(true);
		
		remove(singleValueField);
		remove(boolSingleValueCombo);
		remove(sizerPanel);
		java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 6;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 0.2;
		add(singleValueField, gridBagConstraints);
		break;
		}
		case Expression.INT:
		{
		typeLabel.setText("int");
		rangeCombo.setEnabled(true);
		singleValueCombo.setSelected(true);
		remove(singleValueField);
		remove(boolSingleValueCombo);
		remove(sizerPanel);
		java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 6;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 0.2;
		add(singleValueField, gridBagConstraints);
		break;
		}
		default:
		{
		typeLabel.setText("unknown");
		rangeCombo.setEnabled(true);
		singleValueCombo.setSelected(true);
		remove(singleValueField);
		remove(boolSingleValueCombo);
		remove(sizerPanel);
		java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 6;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 0.2;
		add(singleValueField, gridBagConstraints);
		}
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
	
	public int getType()
	{
		if(typeLabel.getText().equals("bool")) return Expression.BOOLEAN;
		else if(typeLabel.getText().equals("int")) return Expression.INT;
		else return Expression.DOUBLE;
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
		if(typeLabel.getText().equals("bool"))
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
		if(typeLabel.getText().equals("bool"))
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
		
		if(typeLabel.getText().equals("bool"))
		{
			s = boolSingleValueCombo.getSelectedItem().toString();
			if (!(s.equals("true") | s.equals("false"))) {
				throw new PrismException("Invalid value \""+s+"\" for Boolean constant \""+getName()+"\"");
			}
		}
		else if(typeLabel.getText().equals("int"))
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
		else if(typeLabel.getText().equals("double"))
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
		singleValueField = new javax.swing.JTextField();
		jPanel4 = new javax.swing.JPanel();
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
		setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
		setPreferredSize(new java.awt.Dimension(640, 23));
		jPanel1.setBackground(new java.awt.Color(255, 255, 255));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		add(jPanel1, gridBagConstraints);

		nameLabel.setText("a");
		nameLabel.setMaximumSize(new java.awt.Dimension(100, 15));
		nameLabel.setMinimumSize(new java.awt.Dimension(50, 15));
		nameLabel.setPreferredSize(new java.awt.Dimension(100, 15));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 0.05;
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

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 6;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 0.2;
		add(singleValueField, gridBagConstraints);

		jPanel4.setBackground(new java.awt.Color(255, 255, 255));
		jPanel4.setPreferredSize(new java.awt.Dimension(20, 10));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 7;
		gridBagConstraints.gridy = 0;
		add(jPanel4, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 9;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 0.2;
		add(startValueField, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 10;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 0.2;
		add(endValueField, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 11;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 0.2;
		add(stepValueField, gridBagConstraints);

		singleValueCombo.setBackground(new java.awt.Color(255, 255, 255));
		choiceButtonGroup.add(singleValueCombo);
		singleValueCombo.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				singleValueComboActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 5;
		gridBagConstraints.gridy = 0;
		add(singleValueCombo, gridBagConstraints);

		rangeCombo.setBackground(new java.awt.Color(255, 255, 255));
		choiceButtonGroup.add(rangeCombo);
		rangeCombo.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				rangeComboActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 8;
		gridBagConstraints.gridy = 0;
		add(rangeCombo, gridBagConstraints);

	}//GEN-END:initComponents
	
	private void rangeComboActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rangeComboActionPerformed
	{//GEN-HEADEREND:event_rangeComboActionPerformed
	doEnables();
	}//GEN-LAST:event_rangeComboActionPerformed
	
	private void singleValueComboActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_singleValueComboActionPerformed
	{//GEN-HEADEREND:event_singleValueComboActionPerformed
	doEnables();
	}//GEN-LAST:event_singleValueComboActionPerformed
	
	
	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JComboBox boolSingleValueCombo;
	private javax.swing.ButtonGroup choiceButtonGroup;
	private javax.swing.JTextField endValueField;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JLabel nameLabel;
	private javax.swing.JRadioButton rangeCombo;
	private javax.swing.JRadioButton singleValueCombo;
	private javax.swing.JTextField singleValueField;
	private javax.swing.JPanel sizerPanel;
	private javax.swing.JTextField startValueField;
	private javax.swing.JTextField stepValueField;
	private javax.swing.JLabel typeLabel;
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
