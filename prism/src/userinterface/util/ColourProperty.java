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

package userinterface.util;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.util.*;

public class ColourProperty extends SingleProperty
{
	private ArrayList<ActionListener> actionListeners;

	/** Creates a new instance of FontProperty */
	public ColourProperty(PropertyOwner owner, String name, Color property)
	{
		this(owner, name, property, "");
	}

	public ColourProperty(PropertyOwner owner, String name, Color property, String comment)
	{
		super(owner, name, property, "", false, comment);
		//FlowLayout fl = new FlowLayout(FlowLayout.CENTER, 0, 0);
		pan.setLayout(new BorderLayout());
		renderer.setBorder(null);
		pan.add(renderer);

		actionListeners = new ArrayList<ActionListener>();

		edit = new JButton("...");
		edit.setPreferredSize(new Dimension(20, 30));

		editValue = Color.BLACK;

		edit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				//editValue = chart.ChartColorChooser.showDialog(GUIPrism.getGUI(), "Choose Colour", editValue);
				fireActionPerformed(e);
			}

		});
	}

	public void setColor(Color property)
	{
		try {
			setProperty(property);
		} catch (PropertyException e) {
			//This will NEVER happen! (hopefully!)
		}
	}

	public Color getColor()
	{
		return (Color) getProperty();
	}

	public void setProperty(Object property) throws PropertyException
	{
		if (property instanceof Color)
			super.setProperty(property);
		else {
			throw new PropertyException("Value must be of type Color");
		}

	}

	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);

		if (renderer != null)
			renderer.setEnabled(enabled);
		if (edit != null)
			edit.setEnabled(enabled);
	}

	JPanel pan = new JPanel();
	JPanel renderer = new JPanel();

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		if (editDocked)
			;
		{
			pan.remove(edit);
			editDocked = false;
		}

		renderer.setOpaque(true);
		renderer.setBackground(getColor());

		if (hasFocus) {
			pan.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
			if (table.isCellEditable(row, column)) {
				pan.setForeground(UIManager.getColor("Table.focusCellForeground"));
				pan.setBackground(UIManager.getColor("Table.focusCellBackground"));
			}
		} else {
			pan.setBorder(new EmptyBorder(0, 2, 2, 1));
			pan.setForeground(UIManager.getColor("Table.focusCellForeground"));
			pan.setBackground(UIManager.getColor("Table.focusCellBackground"));
		}

		return pan;
	}

	public Component getTableCellRendererComponentMulti(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column,
			boolean allTheSame)
	{
		if (allTheSame) {
			return getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		} else {
			if (editDocked)
				;
			{
				pan.remove(edit);
				editDocked = false;
			}

			renderer.setOpaque(true);
			renderer.setBackground(new Color(240, 240, 240));

			if (hasFocus) {
				pan.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
				if (table.isCellEditable(row, column)) {
					pan.setForeground(UIManager.getColor("Table.focusCellForeground"));
					pan.setBackground(UIManager.getColor("Table.focusCellBackground"));
				}
			} else {
				pan.setBorder(new EmptyBorder(0, 2, 2, 1));
				pan.setForeground(UIManager.getColor("Table.focusCellForeground"));
				pan.setBackground(UIManager.getColor("Table.focusCellBackground"));
			}
			return pan;
		}
	}

	JButton edit;
	boolean editDocked = false;

	//THIS WILL NEED TO OVERRIDE THE EDITOR
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		editValue = getColor();
		if (!editDocked) {
			pan.add(edit, BorderLayout.EAST);
			editDocked = true;
		}
		//renderer.setSelected(getBoolValue());
		pan.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		pan.setForeground(UIManager.getColor("Table.focusCellForeground"));
		pan.setBackground(UIManager.getColor("Table.focusCellBackground"));

		return pan;
	}

	public Component getTableCellEditorComponentMulti(JTable table, Object value, boolean isSelected, int row, int column, boolean allTheSame)
	{
		if (allTheSame) {
			return getTableCellEditorComponent(table, value, isSelected, row, column);
		}
		editValue = getColor();
		if (!editDocked) {
			pan.add(edit, BorderLayout.EAST);
			editDocked = true;
		}

		renderer.setBackground(new Color(240, 240, 240));
		//renderer.setSelected(getBoolValue());
		pan.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		pan.setForeground(UIManager.getColor("Table.focusCellForeground"));
		pan.setBackground(UIManager.getColor("Table.focusCellBackground"));

		return pan;
	}

	public Color getEditorValue()
	{
		return editValue;
	}

	private Color editValue;

	public void addListenerToEditor(ActionListener e)
	{
		actionListeners.add(e);
	}

	public void removeListenerFromEditor(ActionListener e)
	{
		actionListeners.remove(e);
	}

	public void fireActionPerformed(ActionEvent e)
	{
		for (int i = 0; i < actionListeners.size(); i++) {
			actionListeners.get(i).actionPerformed(e);
		}
	}
}
