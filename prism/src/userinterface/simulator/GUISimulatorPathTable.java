//==============================================================================
//
//	Copyright (c) 2004-2006, Andrew Hinton, Mark Kattenbelt
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

package userinterface.simulator;

import javax.swing.*;
import javax.swing.table.*;
import userinterface.util.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import java.text.*;
import java.awt.font.*;
import simulator.*;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.*;
import javax.swing.event.*;


/**
 *
 * @author  ug60axh
 */
public class GUISimulatorPathTable extends GUIGroupedTable
{
	private GUISimulator.PathTableModel ptm;
	private PathRowHeader pathRowHeader;
    
	private SimulatorEngine engine;
    
	private JList header;
	private GUISimulatorPathTable.RowHeaderListModel headerModel;
    
	/** Creates a new instance of GUISimulatorPathTable */
	public GUISimulatorPathTable(GUISimulator.PathTableModel ptm, SimulatorEngine engine)
	{
		super(ptm);
		this.ptm = ptm;
		this.engine = engine;
	
		setColumnSelectionAllowed(false);
		getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	
		pathRowHeader = new GUISimulatorPathTable.PathRowHeader();
	
		headerModel = new RowHeaderListModel();
		JList rowHeader = new JList(headerModel);
	
		rowHeader.setBackground(new JPanel().getBackground());
	
		rowHeader.setFixedCellWidth(25);
	
		rowHeader.setFixedCellHeight(getRowHeight());
		//+ getRowMargin());
		//getIntercellSpacing().height);
		rowHeader.setCellRenderer(new RowHeaderRenderer(this));
	
		this.header = rowHeader;
			
		setDefaultRenderer(Object.class, new PathChangeTableRenderer());
	
		//setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);		
	}
	
	public void switchToChangeRenderer()
	{
		setDefaultRenderer(Object.class, new PathChangeTableRenderer());
		repaint();
	}
    
	public void switchToBoringRenderer()
	{
		setDefaultRenderer(Object.class, new PathTableRenderer());
		repaint();
	}
    
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
	
		//pathRowHeader.paintComponent(g);
		headerModel.updateHeader();	
	}
    
	/**
	 * Getter for property pathRowHeader.
	 * @return Value of property pathRowHeader.
	 */
	public Component getPathRowHeader()
	{
		return header; //pathRowHeader;
	}
    
	class PathRowHeader extends JPanel
	{
		public PathRowHeader()
		{
			super();
	    
	    
			setBackground(Color.yellow);
		}
	
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
	    
			Graphics2D g2 = (Graphics2D) g;
	    
			g2.clearRect(0,0,getWidth(), getHeight());
	    
			g2.setColor(Color.black);
			double y;
			for(int i = 0; i < ptm.getRowCount(); i++)
			{
				y = i * 10;
		
				g2.drawLine(0, (int)y, 10, (int)y);
			}
		}
	}
    
	class RowHeaderRenderer extends JPanel implements ListCellRenderer
	{
	
		boolean startLoop, midLoop, endLoop;
	
		RowHeaderRenderer(JTable table)
		{
			/*JTableHeader header = table.getTableHeader();
			 setOpaque(true);
			 setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			 setHorizontalAlignment(CENTER);
			 setForeground(header.getForeground());
			 setBackground(header.getBackground());
			 setFont(header.getFont());*/
		}
	
		public Component getListCellRendererComponent( JList list,
			Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			//setText((value == null) ? "" : value.toString());
			//setBorder(new LineBorder(Color.black, 1));
			if(ptm.isPathLooping())
			{
				if(index == ptm.getLoopEnd() && index == ptm.getLoopStart())
				{
					startLoop = true;
					endLoop = true;
					midLoop = false;
				}
				else if(index == ptm.getLoopEnd())
				{
					startLoop = false;
					midLoop = false;
					endLoop = true;
				}
				else if(index == ptm.getLoopStart())
				{
					startLoop = true;
					midLoop = false;
					endLoop = false;
				}
				else if(index > ptm.getLoopStart() && index < ptm.getLoopEnd())
				{
					startLoop = false;
					midLoop = true;
					endLoop = false;
				}
				else
				{
					startLoop = false;
					midLoop = false;
					endLoop = false;
				}
			}
			else
			{
				startLoop = false;
				midLoop = false;
				endLoop = false;
			}
	    
			return this;
		}
	
		public void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    
			g2.setColor(getBackground());
			g2.fillRect(0,0,getWidth(), getHeight());
			
			if (!ptm.isDisplayPathLoops()) return;
			
			g2.setColor(Color.black);
			if(startLoop && endLoop)
			{
				GeneralPath gp = new GeneralPath();
				gp.moveTo(getWidth(), (getHeight()/2)+5);
				gp.lineTo((getWidth()/2)+5, (getHeight()/2)+5);
				gp.quadTo(getWidth()/2, (getHeight()/2)+5, getWidth()/2, (getHeight()/2));
				gp.quadTo(getWidth()/2, (getHeight()/2)-5, (getWidth()/2)+5, (getHeight()/2)-5);
				gp.lineTo((getWidth()), (getHeight()/2)-5);
				g2.draw(gp);
				gp = new GeneralPath();
				gp.moveTo(getWidth(), (getHeight()/2)-5);
				gp.lineTo(getWidth()-5, (getHeight()/2) - 8);
				gp.lineTo(getWidth()-5, (getHeight()/2) - 2);
				gp.closePath();
				g2.fill(gp);
			}
			else if(startLoop)
			{
				GeneralPath gp = new GeneralPath();
				gp.moveTo(getWidth(), getHeight()/2);
				gp.lineTo((getWidth()/2)+5, getHeight()/2);
				gp.quadTo(getWidth()/2, getHeight()/2, getWidth()/2, (getHeight()/2)+5);
				gp.lineTo(getWidth()/2, getHeight());
				g2.draw(gp);
				gp = new GeneralPath();
				gp.moveTo(getWidth(), getHeight()/2);
				gp.lineTo(getWidth()-5, (getHeight()/2) - 3);
				gp.lineTo(getWidth()-5, (getHeight()/2) + 3);
				gp.closePath();
				g2.fill(gp);
			}
			else if(midLoop)
			{
				g2.drawLine(getWidth()/2, 0, getWidth()/2, getHeight());
			}
			else if(endLoop)
			{
				GeneralPath gp = new GeneralPath();
				gp.moveTo(getWidth(), getHeight()/2);
				gp.lineTo((getWidth()/2)+5, getHeight()/2);
				gp.quadTo(getWidth()/2, getHeight()/2, getWidth()/2, (getHeight()/2)-5);
				gp.lineTo(getWidth()/2, 0);
				g2.draw(gp);
			}
		}
	}
    
	class RowHeaderListModel extends AbstractListModel
	{
	
		public Object getElementAt(int index)
		{
			return "";
		}
	
		public int getSize()
		{
			return ptm.getRowCount();
		}
	
		public void updateHeader()
		{
			fireContentsChanged(this, 0, ptm.getRowCount());
		}
	
	}
    
	class PathTableRenderer implements TableCellRenderer
	{
		JTextField renderer;
	
		public PathTableRenderer()
		{
			renderer = new JTextField("");
		}
		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
		{
	    
			if(value instanceof Double && ((Double)value).doubleValue() == SimulatorEngine.UNDEFINED_DOUBLE)
				renderer.setText("");
			else renderer.setText(value.toString());   
	    
			Color c = Color.white;    
	    
			if(isSelected)
			{
				Color newCol = new Color(c.getRed()-20, c.getGreen()-20, c.getBlue());
				if(ptm.shouldColourRow(row))
				{
					newCol = new Color(newCol.getRed()-20, newCol.getGreen(), newCol.getBlue()-20);
		    
					renderer.setBackground(newCol);
				}
				else
					renderer.setBackground(newCol);
			}
			else
			{
				if(ptm.shouldColourRow(row))
				{
					Color newCol = new Color(c.getRed()-100, c.getGreen(), c.getBlue()-100);
		    
					renderer.setBackground(newCol);
				}
				else
					renderer.setBackground(c);
			}
	    
	    
	    
			renderer.setBorder(new EmptyBorder(1, 1, 1, 1));
			return renderer;
		}
	}
    
	class PathChangeTableRenderer extends JPanel implements TableCellRenderer
	{
	
		Object value;
		boolean top;
		boolean mid;
		boolean bottom;
		boolean last;
		boolean selected;
		int row;
		JTextField field;
		Color bg;
	
		public PathChangeTableRenderer()
		{
			super();
	    
			top = true;
			mid = false;
			bottom = false;
			last = false;
			selected = false;
			bg = Color.white;
	    
			field = new JTextField();
		}
		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
		{
	    
			//column = table.getColumnModel().getColumnIndex(table.getColumnName(column));
			this.row = row;
			column = table.convertColumnIndexToModel(column);
	    
			bg = Color.white;
			if(column == 0 || column > ptm.getView().getVisibleVariables().size())
			{
				field.setToolTipText(null);
				
				if(value instanceof Double)
				{
					Double dv = (Double)value;
					double dvd = dv.doubleValue();
		    
					if(dvd == SimulatorEngine.UNDEFINED_DOUBLE)
						field.setText("");
					else field.setText(value.toString());
				}
				else if (value instanceof GUISimulator.RewardStructureValue)
				{
					GUISimulator.RewardStructureValue rewardValue = (GUISimulator.RewardStructureValue)value;
					if (rewardValue.getStateReward() != null && rewardValue.getTransitionReward() != null)
					{
						field.setText(rewardValue.getStateReward().toString() +  "  [  " + rewardValue.getTransitionReward().toString() + "  ]");						
					}
					else if (rewardValue.getStateReward() == null && rewardValue.getTransitionReward() != null)
					{
						field.setText("[  " + rewardValue.getTransitionReward().toString() + "  ]");						
					}
					else if (rewardValue.getStateReward() != null && rewardValue.getTransitionReward() == null)
					{
						field.setText(rewardValue.getStateReward().toString());						
					}
					else
					{
						field.setText("");
					}
					
					field.setHorizontalAlignment(JTextField.CENTER);	
					field.setToolTipText("State Reward [ Transition Reward ]");
				}
				else 
				{
					field.setText(value.toString());
					field.setHorizontalAlignment(JTextField.CENTER);
				}	
				
				if(isSelected)
				{
					Color newCol = new Color(bg.getRed()-20, bg.getGreen()-20, bg.getBlue());
					if(ptm.shouldColourRow(row))
					{
						newCol = new Color(newCol.getRed()-20, newCol.getGreen(), newCol.getBlue()-20);
			
						field.setBackground(newCol);
					}
					else
						field.setBackground(newCol);
				}
				else
				{
					if(ptm.shouldColourRow(row))
					{
						Color newCol = new Color(bg.getRed()-50, bg.getGreen(), bg.getBlue()-50);
			
						field.setBackground(newCol);
					}
					else
						field.setBackground(bg);
				}
		
				field.setBorder(new EmptyBorder(1, 1, 1, 1));
				return field;
			}
			else if(row == 0)
			{
				this.value = value;
				this.top = true;
				this.mid = false;
				this.bottom = false;
				this.selected = isSelected;
				this.row = row;
				if(row == ptm.getRowCount()-1)
					this.last = true;
				else
					this.last = false;
				return this;
		
			}
			else if(ptm.getValueAt(row-1, column).equals(value))
			{
				this.value = value;
				this.top = false;
				this.mid = true;
				this.bottom = false;
				this.selected = isSelected;
				this.row = row;
				if(row == ptm.getRowCount()-1)
					this.last = true;
				else this.last = false;
				return this;
			}
			else
			{
				this.value = value;
				this.top = false;
				this.mid = false;
				this.bottom = true;
				this.selected = isSelected;
				this.row = row;
				if(row == ptm.getRowCount()-1)
					this.last = true;
				else this.last = false;
				return this;
			}
	    
	    
	    
	    
	    
		}
	
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
	    
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    
			if(selected)
			{
				Color newCol = new Color(bg.getRed()-20, bg.getGreen()-20, bg.getBlue());
		
				g2.setColor(newCol);
			}
			else
			{
				g2.setColor(bg);
			}
	    
			if(selected)
			{
				Color newCol = new Color(bg.getRed()-20, bg.getGreen()-20, bg.getBlue());
				if(ptm.shouldColourRow(row))
				{
					newCol = new Color(newCol.getRed()-20, newCol.getGreen(), newCol.getBlue()-20);
		    
					g2.setColor(newCol);
				}
				else
					g2.setColor(newCol);
			}
			else
			{
				if(ptm.shouldColourRow(row))
				{
					Color newCol = new Color(bg.getRed()-50, bg.getGreen(), bg.getBlue()-50);
		    
					g2.setColor(newCol);
				}
				else
					g2.setColor(bg);
			}
	    
			g2.fillRect(0,0,getWidth(), getHeight());
	    
			g2.setColor(Color.black);
	    
			double width = getStringWidth(value.toString(), g2);
			double height = g2.getFont().getSize();
	    
			if(top || bottom || last || selected)
			{
				g2.drawString(value.toString(), (int)((getWidth()/2)- (width/2)), 12);
		
				RoundRectangle2D.Double rec = new RoundRectangle2D.Double((getWidth()/2)-(width/2)-5, 1, width+10, getHeight()-3, 8, 8);
				if(last)
				{
					g2.setColor(new Color(253,255,201));
				}
				else if(mid)
				{
					g2.setColor(new Color(252,252,252));
				}
				else
				{
					g2.setColor(Color.white);
				}
				g2.fill(rec);
		
				g2.setColor(Color.black);
		
				if(mid)
					g2.setColor(Color.lightGray);
				g2.draw(rec);
		
				g2.setColor(Color.black);
				g2.drawLine(getWidth()/2, 0, getWidth()/2, 1);
				g2.drawLine(getWidth()/2, getHeight(), getWidth()/2, getHeight()-2);
		
				if(mid)
					g2.setColor(Color.lightGray);
				g2.drawString(value.toString(), (int)((getWidth()/2)- (width/2)), 12);
			}
			else if(mid)
			{
				g2.drawLine(getWidth()/2, 0, getWidth()/2, getHeight());
			}
			/*else if(mid)
			 {
			 g2.drawLine(getWidth()/2, 0, getWidth()/2, getHeight()/2);
			 g2.drawLine((getWidth()/2)-10, getHeight()/2, (getWidth()/2)+10, getHeight()/2);
			 }*/
	    
	    
		}
	}
    
	/** Method which computes width of a string for a given Graphics2D object
	 */
	public static double getStringWidth(String s, Graphics2D g2)
	{
		// catch special cases...
		// ...TextLayout constructor crashes with null or zero-length string
		if (s == null) return 0;
		if (s.length() == 0) return 0;
		TextLayout layout = new TextLayout(s, g2.getFont(), g2.getFontRenderContext());
		Rectangle r = layout.getOutline(new AffineTransform()).getBounds();
		return r.getWidth();
	}
    
}
