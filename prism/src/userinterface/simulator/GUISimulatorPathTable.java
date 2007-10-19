//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
//	* Mark Kattenbelt <mxk@cs.bham.uc.uk> (University of Birmingham)
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

package userinterface.simulator;

import simulator.*;
import prism.*;
import userinterface.util.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import java.text.*;
import java.awt.font.*;

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
	private GUISimulator simulator;
    
	private JList header;
	private GUISimulatorPathTable.RowHeaderListModel headerModel;
    
	/** Creates a new instance of GUISimulatorPathTable */
	public GUISimulatorPathTable(GUISimulator simulator, GUISimulator.PathTableModel ptm, SimulatorEngine engine)
	{
		super(ptm);
		this.ptm = ptm;
		this.simulator = simulator;
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
			
		setDefaultRenderer(Object.class, new PathChangeTableRenderer(true));
	
		
	}
	
	public void switchToChangeRenderer()
	{
		setDefaultRenderer(Object.class, new PathChangeTableRenderer(true));
		repaint();
	}
    
	public void switchToBoringRenderer()
	{
		setDefaultRenderer(Object.class, new PathChangeTableRenderer(false));
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
	
	class PathChangeCellRenderer extends JPanel 
	{
		private PathChangeTableRenderer pctr;
		
		private Object value;
		private String stringValue;
		
		private boolean isSelected;
				
		private int row;	

		public PathChangeCellRenderer(PathChangeTableRenderer pctr, Object value, boolean isSelected, int row) 
		{
			super();
			this.pctr = pctr;
			this.value = value;
			this.isSelected = isSelected;
			this.row = row;
			
			if (value instanceof String)
			{
				stringValue = (String)value;
				this.setToolTipText("State \"" + row + "\" of this path");
			}	
			else if (value instanceof GUISimulator.TimeValue)
			{	
				GUISimulator.TimeValue timeValue = (GUISimulator.TimeValue)value;
				if (timeValue.isTimeValueUnknown())
				{	
					stringValue = "?";
					if (timeValue.isCumulative())
						this.setToolTipText("The cumulative time spent in states up to and including state \"" + (row) + "\" is not yet known");
					else
						this.setToolTipText("The time spent in state \"" + (row) + "\" is not yet known");
				}
				else
				{
					stringValue = (PrismUtils.formatDouble(simulator.getPrism().getSettings(), ((Double)timeValue.getValue())));
					if (timeValue.isCumulative())
						this.setToolTipText("The cumulative time spent in states up to and including state \"" + (row) + "\" is \"" + stringValue + "\" time units");
					else
						this.setToolTipText("The time spent in state \"" + (row) + "\" is \"" + stringValue + "\" time units");
				}
			}
			else if (value instanceof GUISimulator.VariableValue)
			{	
				GUISimulator.VariableValue variableValue = (GUISimulator.VariableValue)value;
				stringValue = (variableValue.getValue() instanceof Double) ? (PrismUtils.formatDouble(simulator.getPrism().getSettings(), ((Double)variableValue.getValue()))) : variableValue.getValue().toString();
								
				this.setToolTipText("Value of variable \"" + variableValue.getVariable().getName() + "\" in state \"" + (row) + "\" is \"" + stringValue + "\"");
			}
			else if (value instanceof GUISimulator.RewardStructureValue)
			{	
				GUISimulator.RewardStructureValue rewardValue = (GUISimulator.RewardStructureValue)value;
				String rewardName = rewardValue.getRewardStructureColumn().getRewardStructure().getColumnName();
						
				if (rewardValue.isRewardValueUnknown())
				{
					stringValue = "?";				
										
					if (rewardValue.getRewardStructureColumn().isCumulativeReward())
						this.setToolTipText("The cumulative reward of reward structure " + rewardName + " up to and including step \"" + (row) + "\" is not yet known");
					if (rewardValue.getRewardStructureColumn().isStateReward())
						this.setToolTipText("The state reward of reward structure " + rewardName + " in state \"" + (row) + "\" is not yet known");
					if (rewardValue.getRewardStructureColumn().isTransitionReward())
						this.setToolTipText("The transition reward of reward structure " + rewardName + " for step \""+(row)+"\" (from state \"" + (row) + "\" to \"" + (row + 1) + "\") is not yet known");
				}
				else
				{
					stringValue = PrismUtils.formatDouble(simulator.getPrism().getSettings(), rewardValue.getRewardValue());				
					
					if (rewardValue.getRewardStructureColumn().isCumulativeReward())
						this.setToolTipText("The cumulative reward of reward structure " + rewardName + " up to and including step \"" + (row) + "\" is \"" + (stringValue) + "\"");
					if (rewardValue.getRewardStructureColumn().isStateReward())
						this.setToolTipText("The state reward of reward structure " + rewardName + " in state \"" + (row) + "\" is \"" + (stringValue) + "\"");
					if (rewardValue.getRewardStructureColumn().isTransitionReward())
						this.setToolTipText("The transition reward of reward structure " + rewardName + " for step \""+(row)+"\" (from state \"" + (row) + "\" to \"" + (row + 1) + "\") is \"" + (stringValue) + "\"");
				}
				
			}
			
			
			
			
		}
		
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
	    
			Graphics2D g2 = (Graphics2D)g;
						
			if (value instanceof String)
			{
				String stringValue = (String)value;
				
				double width = getStringWidth(stringValue, g2);
				double height = g2.getFont().getSize();
								
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);	
				
				g2.setColor(Color.black);
				g2.drawString(stringValue, (int)((getWidth()/2 + 0.5)- (width/2)), 12);			
			}
			else if (value instanceof GUISimulator.VariableValue)
			{
				GUISimulator.VariableValue variableValue = (GUISimulator.VariableValue)value;
				
				double width = getStringWidth(stringValue, g2);
				
				RoundRectangle2D.Double rec = new RoundRectangle2D.Double((getWidth()/2)-(width/2)-5, 1, width+10, getHeight()-3, 8, 8);
				
				Color color = (variableValue.hasChanged()) ? (Color.black) : (Color.lightGray);
								
				if (pctr.onlyShowChange())
				{
					g2.setColor(Color.black);
					g2.drawLine(getWidth()/2, 0, getWidth()/2, getHeight());					
					
					if (isSelected || variableValue.hasChanged())
					{		
						g2.setColor(Color.white);
						g2.fill(rec);					
						
						g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);	
											
						g2.setColor(color);				
						g2.draw(rec);
						
						g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);	
										
						g2.drawString(stringValue, (int)((getWidth()/2 + 0.5)- (width/2)), 12);
					}
				}
				else
				{
					g2.setColor(color);					
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);									
					g2.drawString(stringValue, (int)((getWidth()/2 + 0.5)- (width/2)), 12);
				}
			}
			else if (value instanceof GUISimulator.RewardStructureValue)
			{
				GUISimulator.RewardStructureValue rewardValue = (GUISimulator.RewardStructureValue)value;
				
				double width = getStringWidth(stringValue, g2);
				
				RoundRectangle2D.Double rec = new RoundRectangle2D.Double((getWidth()/2)-(width/2)-5, 1, width+10, getHeight()-3, 8, 8);
				
				Color color = (rewardValue.hasChanged() || rewardValue.isRewardValueUnknown()) ? (Color.black) : (Color.lightGray);
								
				if (pctr.onlyShowChange())
				{
					g2.setColor(Color.black);
					g2.drawLine(getWidth()/2, 0, getWidth()/2, getHeight());
										
					if ((isSelected || rewardValue.hasChanged()))
					{
						g2.setColor(Color.white);
						g2.fill(rec);					
						
						g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);	
											
						g2.setColor(color);				
						g2.draw(rec);
						
						g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);	
										
						g2.drawString(stringValue, (int)((getWidth()/2 + 0.5)- (width/2)), 12);
					}
				}
				else
				{
					g2.setColor(color);	
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);	
					g2.drawString(stringValue, (int)((getWidth()/2 + 0.5)- (width/2)), 12);
				}
			}
			else if (value instanceof GUISimulator.TimeValue)
			{				
				GUISimulator.TimeValue timeValue = (GUISimulator.TimeValue)value;
				
				String stringValue = (timeValue.isTimeValueUnknown()) ? "?" : PrismUtils.formatDouble(simulator.getPrism().getSettings(), timeValue.getValue());
				
				if (timeValue.isTimeValueUnknown())
					this.setToolTipText("The time spent in state " + row + " of this path is not yet known");
				else
					this.setToolTipText(stringValue + " time units were spent in state " + row + " of this path");
					
				double width = getStringWidth(stringValue, g2);
					
				Color color = (Color.black);
				
				g2.setColor(color);	
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				g2.drawString(stringValue, 3, 12);				
			}
		}
	}
	
	class PathChangeTableRenderer implements TableCellRenderer
	{	
		private boolean onlyShowChange;
				
		private Color defaultColor;

		private Color selectedColor;
		private Color labelColor;
		private Color selectedLabelColor;
		
		public PathChangeTableRenderer(boolean onlyShowChange)
		{
			super();
			
			this.onlyShowChange = onlyShowChange;
			
			defaultColor = Color.white;
			
			selectedColor = new Color(defaultColor.getRed()-20, defaultColor.getGreen()-20, defaultColor.getBlue());
			selectedLabelColor = new Color(selectedColor.getRed()-20, selectedColor.getGreen(), selectedColor.getBlue()-20);
			labelColor = new Color(defaultColor.getRed()-50, defaultColor.getGreen(), defaultColor.getBlue()-50);				
								
		}
		
		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
		{	
			PathChangeCellRenderer pctr = new PathChangeCellRenderer(this, value, isSelected, row);
						
			boolean shouldColourRow = ptm.shouldColourRow(row);
			
			Color backGround = defaultColor;
			
			if(isSelected && !shouldColourRow)
				backGround = selectedColor;
			else if (isSelected && shouldColourRow)
				backGround = selectedLabelColor;
			else if (!isSelected && shouldColourRow)
				backGround = labelColor;
			
			pctr.setBackground(backGround);
			
			return pctr;
		}	
		

		public boolean onlyShowChange() 
		{
			return onlyShowChange;
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
