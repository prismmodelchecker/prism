//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package userinterface.simulator;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;

import simulator.*;
import userinterface.util.*;
import userinterface.simulator.SimulationView.*;

public class GUISimulatorPathTable extends GUIGroupedTable
{
	private static final long serialVersionUID = 1L;

	// Simulator
	private GUISimulator simulator;
	// Table model
	private GUISimulatorPathTableModel ptm;
	// Component on left hand side to show path loops
	private JList loopIndicator;
	private LoopIndicatorListModel loopIndicatorModel;

	/** Creates a new instance of GUISimulatorPathTable */
	public GUISimulatorPathTable(GUISimulator simulator, GUISimulatorPathTableModel ptm, SimulatorEngine engine)
	{
		super(ptm);
		this.ptm = ptm;
		this.simulator = simulator;

		// Table
		setColumnSelectionAllowed(false);
		getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setDefaultRenderer(Object.class, new PathChangeTableRenderer(true));
		// Loop indicator
		loopIndicatorModel = new LoopIndicatorListModel();
		loopIndicator = new JList(loopIndicatorModel);
		loopIndicator.setBackground(new JPanel().getBackground());
		loopIndicator.setFixedCellWidth(25);
		loopIndicator.setFixedCellHeight(getRowHeight());
		loopIndicator.setCellRenderer(new LoopIndicatorRenderer(this));
	}

	/** Override set font to set row height(s) */
	public void setFont(Font font)
	{
		super.setFont(font);
		setRowHeight(getFontMetrics(font).getHeight() + 4);
		if (loopIndicator != null)
			loopIndicator.setFixedCellHeight(getRowHeight());
	}

	public boolean usingChangeRenderer()
	{
		return ((PathChangeTableRenderer) getDefaultRenderer(Object.class)).onlyShowChange();
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
		loopIndicatorModel.updateIndicator();
	}

	public Component getPathLoopIndicator()
	{
		return loopIndicator;
	}

	// Cell renderer for list representing loop indicator (left of path table)

	class LoopIndicatorRenderer extends JPanel implements ListCellRenderer
	{
		private static final long serialVersionUID = 1L;

		boolean startLoop, midLoop, endLoop;

		LoopIndicatorRenderer(JTable table)
		{
			/*JTableHeader header = table.getTableHeader();
			 setOpaque(true);
			 setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			 setHorizontalAlignment(CENTER);
			 setForeground(header.getForeground());
			 setBackground(header.getBackground());
			 setFont(header.getFont());*/
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			//setText((value == null) ? "" : value.toString());
			//setBorder(new LineBorder(Color.black, 1));
			if (ptm.isPathLooping()) {
				if (index == ptm.getLoopEnd() && index == ptm.getLoopStart()) {
					startLoop = true;
					endLoop = true;
					midLoop = false;
				} else if (index == ptm.getLoopEnd()) {
					startLoop = false;
					midLoop = false;
					endLoop = true;
				} else if (index == ptm.getLoopStart()) {
					startLoop = true;
					midLoop = false;
					endLoop = false;
				} else if (index > ptm.getLoopStart() && index < ptm.getLoopEnd()) {
					startLoop = false;
					midLoop = true;
					endLoop = false;
				} else {
					startLoop = false;
					midLoop = false;
					endLoop = false;
				}
			} else {
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
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			g2.setColor(getBackground());
			g2.fillRect(0, 0, getWidth(), getHeight());

			if (!simulator.isDisplayPathLoops())
				return;

			g2.setColor(Color.black);
			if (startLoop && endLoop) {
				GeneralPath gp = new GeneralPath();
				gp.moveTo(getWidth(), (getHeight() / 2) + 5);
				gp.lineTo((getWidth() / 2) + 5, (getHeight() / 2) + 5);
				gp.quadTo(getWidth() / 2, (getHeight() / 2) + 5, getWidth() / 2, (getHeight() / 2));
				gp.quadTo(getWidth() / 2, (getHeight() / 2) - 5, (getWidth() / 2) + 5, (getHeight() / 2) - 5);
				gp.lineTo((getWidth()), (getHeight() / 2) - 5);
				g2.draw(gp);
				gp = new GeneralPath();
				gp.moveTo(getWidth(), (getHeight() / 2) - 5);
				gp.lineTo(getWidth() - 5, (getHeight() / 2) - 8);
				gp.lineTo(getWidth() - 5, (getHeight() / 2) - 2);
				gp.closePath();
				g2.fill(gp);
			} else if (startLoop) {
				GeneralPath gp = new GeneralPath();
				gp.moveTo(getWidth(), getHeight() / 2);
				gp.lineTo((getWidth() / 2) + 5, getHeight() / 2);
				gp.quadTo(getWidth() / 2, getHeight() / 2, getWidth() / 2, (getHeight() / 2) + 5);
				gp.lineTo(getWidth() / 2, getHeight());
				g2.draw(gp);
				gp = new GeneralPath();
				gp.moveTo(getWidth(), getHeight() / 2);
				gp.lineTo(getWidth() - 5, (getHeight() / 2) - 3);
				gp.lineTo(getWidth() - 5, (getHeight() / 2) + 3);
				gp.closePath();
				g2.fill(gp);
			} else if (midLoop) {
				g2.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
			} else if (endLoop) {
				GeneralPath gp = new GeneralPath();
				gp.moveTo(getWidth(), getHeight() / 2);
				gp.lineTo((getWidth() / 2) + 5, getHeight() / 2);
				gp.quadTo(getWidth() / 2, getHeight() / 2, getWidth() / 2, (getHeight() / 2) - 5);
				gp.lineTo(getWidth() / 2, 0);
				g2.draw(gp);
			}
		}
	}

	// Model for list representing loop indicator

	class LoopIndicatorListModel extends AbstractListModel
	{
		private static final long serialVersionUID = 1L;

		public Object getElementAt(int index)
		{
			return "";
		}

		public int getSize()
		{
			return ptm.getRowCount();
		}

		public void updateIndicator()
		{
			fireContentsChanged(this, 0, ptm.getRowCount());
		}

	}

	// Renderer for cells in path table

	class PathChangeCellRenderer extends JPanel
	{
		private static final long serialVersionUID = 1L;

		private PathChangeTableRenderer pctr;

		private Object value;
		private String stringValue;

		private boolean isSelected;

		public PathChangeCellRenderer(PathChangeTableRenderer pctr, Object value, boolean isSelected, int row)
		{
			super();
			this.pctr = pctr;
			this.value = value;
			this.isSelected = isSelected;

			if (value instanceof String) {
				stringValue = (String) value;
				this.setToolTipText("State " + row);
			} else if (value instanceof ActionValue) {
				ActionValue actionValue = (ActionValue) value;
				if (actionValue.isActionValueUnknown()) {
					// unused:
					stringValue = "?";
					this.setToolTipText("Module name or [action] label for transition from state " + (row - 1) + " to " + (row) + " (not yet known)");
				} else {
					stringValue = actionValue.getValue();
					String tooltip;
					if (row == 0) {
						tooltip = null;
					} else {
						tooltip = "Module name or [action] label for transition from state " + (row - 1) + " to " + (row);
					}
					this.setToolTipText(tooltip);
				}
			} else if (value instanceof TimeValue) {
				TimeValue timeValue = (TimeValue) value;
				if (timeValue.isTimeValueUnknown()) {
					stringValue = "?";
					if (timeValue.isCumulative())
						this.setToolTipText("Cumulative time up until entering state " + (row) + " (not yet known)");
					else
						this.setToolTipText("Time spent in state " + (row) + " (not yet known)");
				} else {
					stringValue = (simulator.formatDouble((Double) timeValue.getValue()));
					if (timeValue.isCumulative())
						this.setToolTipText("Cumulative time up until entering state " + (row));
					else
						this.setToolTipText("Time spent in state " + (row));
				}
			} else if (value instanceof VariableValue) {
				VariableValue variableValue = (VariableValue) value;
				stringValue = (variableValue.getValue() instanceof Double) ? (simulator.formatDouble(((Double) variableValue.getValue()))) : variableValue
						.getValue().toString();

				this.setToolTipText("Value of variable \"" + variableValue.getVariable().getName() + "\" in state " + (row));
			} else if (value instanceof RewardStructureValue) {
				RewardStructureValue rewardValue = (RewardStructureValue) value;
				String rewardName = rewardValue.getRewardStructureColumn().getRewardStructure().getColumnName();

				if (rewardValue.isRewardValueUnknown()) {
					stringValue = "?";

					if (rewardValue.getRewardStructureColumn().isCumulativeReward())
						this.setToolTipText("Cumulative reward of reward structure " + rewardName + " up until state " + (row) + " (not yet known)");
					if (rewardValue.getRewardStructureColumn().isStateReward())
						this.setToolTipText("State reward of reward structure " + rewardName + " in state " + (row) + " (not yet known)");
					if (rewardValue.getRewardStructureColumn().isTransitionReward())
						this.setToolTipText("Transition reward of reward structure " + rewardName + " from state " + (row) + " to " + (row + 1)
								+ " (not yet known)");
				} else {
					stringValue = simulator.formatDouble(rewardValue.getRewardValue());

					if (rewardValue.getRewardStructureColumn().isCumulativeReward())
						this.setToolTipText("Cumulative reward of reward structure " + rewardName + " up until state " + (row));
					if (rewardValue.getRewardStructureColumn().isStateReward())
						this.setToolTipText("State reward of reward structure " + rewardName + " in state " + (row));
					if (rewardValue.getRewardStructureColumn().isTransitionReward())
						this.setToolTipText("Transition reward of reward structure " + rewardName + " from state " + (row) + " to " + (row + 1));
				}

			}
		}

		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			Rectangle rect;
			int width, height, x, y;

			// Get graphics context
			Graphics2D g2 = (Graphics2D) g;
			// Get some info about the string
			rect = getStringBounds(stringValue, g2);
			width = (int) Math.ceil(rect.getWidth());
			height = (int) Math.ceil(rect.getHeight());

			// State index/action
			if (value instanceof String || value instanceof ActionValue) {
				// Position (horiz centred, vert centred)
				x = (getWidth() / 2) - (width / 2);
				y = (getHeight() / 2) + (height / 2);
				// Write value
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setColor(Color.black);
				g2.drawString(stringValue, x, y);
			}
			// Variable value
			else if (value instanceof VariableValue) {
				VariableValue variableValue = (VariableValue) value;
				// Position (horiz centred, vert centred)
				x = (getWidth() / 2) - (width / 2);
				y = (getHeight() / 2) + (height / 2);
				// Prepare box/colour
				RoundRectangle2D.Double rec = new RoundRectangle2D.Double(x - 5, 2, width + 10, getHeight() - 5, 8, 8);
				Color color = (variableValue.hasChanged()) ? (Color.black) : (Color.lightGray);
				// "Render changes" view
				if (pctr.onlyShowChange()) {
					// Vertical line in background
					g2.setColor(Color.black);
					g2.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
					// Only display box/value if there was a change
					if (isSelected || variableValue.hasChanged()) {
						g2.setColor(Color.white);
						g2.fill(rec);
						g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						g2.setColor(color);
						g2.draw(rec);
						g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
						g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
						g2.drawString(stringValue, x, y);
					}
				}
				// "Render all values" view
				else {
					// Just display value
					g2.setColor(color);
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
					g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g2.drawString(stringValue, x, y);
				}
			}
			// Reward value
			else if (value instanceof RewardStructureValue) {
				RewardStructureValue rewardValue = (RewardStructureValue) value;
				// Default case (everything except cumulative time)
				if (!(ptm.canShowTime() && rewardValue.getRewardStructureColumn().isCumulativeReward())) {
					// Position (horiz centred, vert centred)
					x = (getWidth() / 2) - (width / 2);
					y = (getHeight() / 2) + (height / 2);
					// Prepare box/colour
					RoundRectangle2D.Double rec = new RoundRectangle2D.Double(x - 5, 2, width + 10, getHeight() - 5, 8, 8);
					Color color = (rewardValue.hasChanged() || rewardValue.isRewardValueUnknown()) ? (Color.black) : (Color.lightGray);
					// "Render changes" view
					if (pctr.onlyShowChange()) {
						// Vertical line in background
						g2.setColor(Color.black);
						g2.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
						// Only display box/value if there was a change
						if ((isSelected || rewardValue.hasChanged())) {
							g2.setColor(Color.white);
							g2.fill(rec);
							g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
							g2.setColor(color);
							g2.draw(rec);
							g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
							g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
							g2.drawString(stringValue, x, y);
						}
					}
					// "Render all values" view
					else {
						// Just display value
						g2.setColor(color);
						g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
						g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
						g2.drawString(stringValue, x, y);
					}
				}
				// For continuous-time cumulative rewards, we left-align (like for display of time)
				else {
					// Position (left aligned, vert centred)
					x = 3;
					y = (getHeight() / 2) + (height / 2);
					// Write text
					g2.setColor(Color.black);
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
					g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g2.drawString(stringValue, x, y);
				}
			}
			// Time value
			else if (value instanceof TimeValue) {
				// Position (left aligned, vert centred)
				x = 3;
				y = (getHeight() / 2) + (height / 2);
				// Write text
				g2.setColor(Color.black);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.drawString(stringValue, x, y);
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

			selectedColor = new Color(defaultColor.getRed() - 20, defaultColor.getGreen() - 20, defaultColor.getBlue());
			selectedLabelColor = new Color(selectedColor.getRed() - 20, selectedColor.getGreen(), selectedColor.getBlue() - 20);
			labelColor = new Color(defaultColor.getRed() - 50, defaultColor.getGreen(), defaultColor.getBlue() - 50);

		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			PathChangeCellRenderer pctr = new PathChangeCellRenderer(this, value, isSelected, row);

			boolean shouldColourRow = ptm.shouldColourRow(row);

			Color backGround = defaultColor;

			if (isSelected && !shouldColourRow)
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

	/** Method which computes rectangle bounds of a string for a given Graphics2D object
	 */
	public static Rectangle getStringBounds(String s, Graphics2D g2)
	{
		// catch special cases...
		// ...TextLayout constructor crashes with null or zero-length string
		if (s == null)
			return new Rectangle(0, 0);
		if (s.length() == 0)
			return new Rectangle(0, 0);
		TextLayout layout = new TextLayout(s, g2.getFont(), g2.getFontRenderContext());
		return layout.getOutline(new AffineTransform()).getBounds();
	}

	/** Method which computes width of a string for a given Graphics2D object
	 */
	public static double getStringWidth(String s, Graphics2D g2)
	{
		return getStringBounds(s, g2).getWidth();
	}

	/** Method which computes height of a string for a given Graphics2D object
	 */
	public static double getStringHeight(String s, Graphics2D g2)
	{
		return getStringBounds(s, g2).getHeight();
	}

}
