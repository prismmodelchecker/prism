//==============================================================================
//
//	Copyright (c) 2005, Andrew Hinton
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
import javax.swing.border.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;
import settings.*;
/**
 *
 * @author  ug60axh
 */
public class SeriesDataEditor implements SettingEditor, ActionListener, FocusListener
{
	private JLabel renderer;
	private JPanel panel;
	private Font font = new Font("monospaced", Font.ITALIC, 12);
	private JButton button;
	private JTable lastTable = null;
	private int tableRow = -1;
	private int tableCol = -1;
	private JPanel blank1;
	private ImageIcon icon;
	
	private boolean dialogFocus = false;
	
	private boolean modified = false;
	
	private GraphList theSeries;
	private ArrayList multiSeries;
	
	public SeriesDataEditor()
	{
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		renderer = new JLabel();
		
		renderer = new javax.swing.JLabel();
		blank1 = new javax.swing.JPanel();
		button = new javax.swing.JButton("...");
		
		button.setFont(new Font("serif", Font.PLAIN, 7));
		
		panel.add(renderer, BorderLayout.CENTER);
		
		blank1.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		
		blank1.setPreferredSize(new Dimension(18,2));
		
		button.setPreferredSize(new Dimension(16,12));
		blank1.add(button);
		panel.add(blank1, BorderLayout.EAST);
		
		
		button.addActionListener(this);
		
		button.addFocusListener(this);
		
		//icon = new ImageIcon(ClassLoader.getSystemResource("images/smallGrid.gif"));
	}
	
	
	
	public Object getEditorValue()
	{
		if(modified)
		{
			modified = false;
			return NOT_CHANGED_VALUE;
		}
		else
			return NOT_CHANGED_VALUE;
	}
	
	public Component getTableCellEditorComponent(JTable table, Setting owner, Object value, boolean isSelected, int row, int column)
	{
		if (isSelected)
		{
			renderer.setForeground(table.getSelectionForeground());
			renderer.setBackground(table.getSelectionBackground());
			panel.setBackground(table.getSelectionBackground());
			blank1.setBackground(table.getSelectionBackground());
			button.setBackground(table.getSelectionBackground());
		}
		else
		{
			renderer.setForeground(table.getForeground());
			renderer.setBackground(table.getBackground());
			panel.setBackground(table.getBackground());
			blank1.setBackground(table.getBackground());
			button.setBackground(table.getBackground());
		}
		
		
		panel.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
		
		
		if(value instanceof GraphList)
		{
			
			renderer.setText("Series Data");
			renderer.setIcon(icon);
			
			renderer.setFont(new Font("monospaced", Font.PLAIN, 12));
			this.theSeries = (GraphList) value;
			this.multiSeries = null;
			
		}
		else if(value instanceof ArrayList)
		{
			
			renderer.setText("(Multiple Series)");
			renderer.setIcon(icon);
			
			renderer.setFont(new Font("monospaced", Font.ITALIC, 12));
			
			this.multiSeries = (ArrayList)value;
			this.theSeries = null;
		}
		
		
		lastTable = table;
		tableRow = row;
		tableCol = column;
		return panel;
	}
	
	public void stopEditing()
	{
	}
	
	public void actionPerformed(ActionEvent e)
	{
		dialogFocus = true;
		if(theSeries != null)
		{
			theSeries.getEditor().showEditor(null);
			modified = true;
		}
		else if(multiSeries != null)
		{
			GraphListEditor.showEditors(null, multiSeries);
			modified = true;
		}
		
		
	}
	
	public void focusGained(FocusEvent e)
	{
	}
	
	public void focusLost(FocusEvent e)
	{
		dialogFocus = false;
	}
	
}
