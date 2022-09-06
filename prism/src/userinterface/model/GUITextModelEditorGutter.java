//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Charles Harley <cd.harley@talk21.com> (University of Edinburgh)
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

package userinterface.model;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SizeSequence;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

import prism.PrismSettings;
import userinterface.GUIPrism;
import userinterface.OptionsPanel;

/**
 * Information gutter for the text model editor. It displays line numbers for 
 * the text in the given text pane and the location of a parsing error. The 
 * font the panel uses is taken from the font for the given text pane.
 */
public class GUITextModelEditorGutter extends JPanel implements PropertyChangeListener, DocumentListener
{

	/** Rendering information for the current font, used to draw the gutter
	 * info. It allows the font height and width needed for the panel to
	 * be calculated.
	 */
	private FontMetrics fontMetrics;
	/** Text pane that the gutter gets the information from.
	 */
	private JTextComponent textPane;
	/** The amount of padding applied to the left edge of the line numbers.
	 */
	private final int PADDING_LEFT = 20;
	/** The amount of padding applied to the right edge of the line numbers, 
	 * between the line numbers and the border.
	 */
	private final int PADDING_RIGHT = 4;
	/** Colour of the border between the panel and the text pane. */
	private final static Color BORDER_COLOR = Color.GRAY;
	/** Colour of the background of the panel. */
	private final static Color BACKGROUND_COLOR = new Color(204,204,204);
	/** The top padding for the text pane. */
	private int textPaneTopPadding;
	/** The distance from the text's baseline to the top of most 
	 * alphanumeric characters.
	 */
	private int textFontAscent;
	/** The current height of the text within the text pane. */
	private int textHeight;
	/** The total width of the panel. */
	private int panelWidth;
	/** The width required to display the line numbers for the current line 
	 * count.
	 */
	private int requiredLineNumbersWidth;
	/** The heights for each of the lines within the text pane. */
	private SizeSequence lineHeights;
	/** The current line count for the text pane. */
	private int lineCount = 1;
	/** The most recent line that was changed in the document. */
	private int updatedLine = 0;
	/** Indicates if the most recent change to the document spanned multiple 
	 * lines.
	 */
	private boolean multipleLinesChanged = true;
	/** 
	 * Mapping from line numbers to error messages.
	 */
	private Map<Integer, String> errorMessages;
	/** Error icon displayed along side the line where the parsing error
	 * has occurred.
	 */
	private ImageIcon errorIcon = GUIPrism.getIconFromImage("tinyError.png");

	/**
	 * Constructor, the panel is initialised with the information for the 
	 * current text in the given text pane.
	 * 
	 * @param textPane Text pane to display the information about.
	 */
	public GUITextModelEditorGutter(JTextComponent textPane)
	{
		//super(prismGUI, true);
		// Check that a text pane has been given.
		if (textPane == null) {
			throw new IllegalArgumentException("The given text pane cannot be null.");
		}
		this.textPane = textPane;
		setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));
		
		updatePaintingCache();
		textPane.addPropertyChangeListener(this);
		textPane.getDocument().addDocumentListener(this);
		
		this.setToolTipText("dummy");
	}	

	@Override
	public String getToolTipText(MouseEvent event) 
	{
		if (errorMessages != null)
		{
			int y = event.getY();
			
			int line = lineHeights.getIndex(y);
			if (errorMessages.containsKey(line+1))
				return errorMessages.get(line+1);
			else
				return null;				
		}
		else
			return null;	
	}

	/**
	 * @return The preferred size of the component.
	 */
	public Dimension getPreferredSize()
	{
		return new Dimension(panelWidth, textPane.getHeight());
	}
	
	public void paintComponent(Graphics g)
	{
		// Draw the background of the panel.
		Rectangle drawArea = g.getClipBounds();
		g.setColor(BACKGROUND_COLOR);
		g.fillRect(drawArea.x, drawArea.y, panelWidth, drawArea.height);
		g.setColor(getForeground());
		
		// Work out which line numbers to draw. Only want the line
		// numbers that are currently visible.
		int base = drawArea.y - textPaneTopPadding;
		int firstLine = lineHeights.getIndex(base);
		int lastLine = lineHeights.getIndex(base + drawArea.height);

		// Draw the line numbers
		String numberText = "";
		for (int i = firstLine; i <= lastLine; i++) {
			numberText = String.valueOf(i + 1);
			int x = PADDING_LEFT + requiredLineNumbersWidth - fontMetrics.stringWidth(numberText);
			int y = lineHeights.getPosition(i) + textFontAscent + textPaneTopPadding;
			
			boolean isErrorLine = false;
			
			if (errorMessages != null)
			{
				if (errorMessages.containsKey(new Integer(i+1)))
					isErrorLine = true;
			}
			
			g.setColor(Color.black);
			g.drawString(numberText, x, y);
			
			
			
			// If a parsing error has occurred on this line then display
			// the error icon.
			
			if (isErrorLine)
				g.drawImage(errorIcon.getImage(), drawArea.x, lineHeights.getPosition(i) + 3, errorIcon.getImageObserver());			
		}
	}
	
	/** Property change listener for the text component, e.g. the font or tab size. */
	public void propertyChange(PropertyChangeEvent event)
	{
		Object oldValue = event.getOldValue();
		Object newValue = event.getNewValue();
		
		// If the document has changed, reset the document listeners.
		if ("document".equals(event.getPropertyName())) {
			if (oldValue != null && oldValue instanceof Document) {
				((Document) oldValue).removeDocumentListener(this);
			}
			if (newValue != null && newValue instanceof Document) {
				((Document) newValue).addDocumentListener(this);
			}
		}
		
		updatePaintingCache();
		// The property change affects the whole document so the whole panel
		// gets redrawn.
		updatePanel(0, true);
	}

	/** Insert listener event for the document, updates the panel to reflect
	 * the change.
	 */
	public void insertUpdate(DocumentEvent event)
	{
		documentUpdated(event);
	}

	/** Remove listener event for the document, updates the panel to reflect
	 * the change.
	 */
	public void removeUpdate(DocumentEvent event)
	{
		documentUpdated(event);
	}

	/** Change listener event for the document, updates the panel to reflect the 
	 * change.
	 */
	public void changedUpdate(DocumentEvent event)
	{
		// This event is covered by the insert and delete listeners, 
		// so it's not implemented.
	}	
	
	/** Returns the height of the given line in the text pane.
	 * 
	 * @param lineNumber The line number
	 *            
	 * @return The height, in pixels, for the given line.
	 */
	private int getLineHeight(int lineNumber)
	{
		int position = lineHeights.getPosition(lineNumber) + textPaneTopPadding;
		int height = textHeight;
		try {
			Element rootElement = textPane.getDocument().getDefaultRootElement();
			int lastChar = rootElement.getElement(lineNumber).getEndOffset() - 1;
			Rectangle charView = textPane.modelToView(lastChar);
			height = (charView.y - position) + charView.height;
		} catch (BadLocationException ex) {
			/* Do nothing -- this occurs normally on LnF update */
			//ex.printStackTrace();
		} catch (NullPointerException ex) {
			/* Do nothing and return height */
		}
		return height;
	}
	
	/* Copied from javax.swing.text.PlainDocument */
	private int getAdjustedLineCount()
	{
		// There is an implicit break being modeled at the end of the
		// document to deal with boundary conditions at the end. This
		// is not desired in the line count, so we detect it and remove
		// its effect if throwing off the count.
		Element rootElement = textPane.getDocument().getDefaultRootElement();
		int count = rootElement.getElementCount();
		Element lastLine = rootElement.getElement(count - 1);
		if ((lastLine.getEndOffset() - lastLine.getStartOffset()) > 1) {
			return count;
		}

		return count - 1;
	}
	
	/** Updates the line heights, if needed. */
	private void updateLineHeights()
	{
		// We only want to update if the document has been changed since
		// the last time the line heights were updated.
		if (updatedLine < 0) {
			return;
		}

		// If multiple lines were changed then update all the line heights.
		if (multipleLinesChanged) {
			// Recreate the line sizes info for the whole document.
			int lineCount = getAdjustedLineCount();
			lineHeights = new SizeSequence(lineCount);
			for (int i = 0; i < lineCount; i++) {
				lineHeights.setSize(i, getLineHeight(i));
			}
			multipleLinesChanged = false;
		} else {
			lineHeights.setSize(updatedLine, getLineHeight(updatedLine));
		}

		updatedLine = -1;
	}

	/** Updates the painting cache as some values used in the painting process 
	 * can be cached to improve performance.
	 */
	private void updatePaintingCache()
	{
		updateLineHeights();
		lineCount = getAdjustedLineCount();
		textPaneTopPadding = textPane.getInsets().top;
		
		// Update the font info
		setFont(textPane.getFont());
		fontMetrics = getFontMetrics(getFont());
		textHeight = fontMetrics.getHeight();
		textFontAscent = fontMetrics.getAscent();

		// Update the panel dimensions
		requiredLineNumbersWidth = fontMetrics.stringWidth(String.valueOf(lineCount));
		panelWidth = PADDING_LEFT + requiredLineNumbersWidth + PADDING_RIGHT;
	}

	/** Indicates that the document was updated and the panel needs to be redrawn. */
	private void documentUpdated(DocumentEvent event)
	{
		// Get the line where the document was updated.
		Element rootElement = textPane.getDocument().getDefaultRootElement();
		int line = rootElement.getElementIndex(event.getOffset());
		// Get the structure change if there was one and update the panel.
		DocumentEvent.ElementChange change = event.getChange(rootElement);
		updatePanel(line, change != null);
	}
	
	/** Update the panel as a result of a change in the document.
	 * 
	 * @param updateLine The line that was updated.
	 * @param multipleLineChange Indicates if the update spanned multiple lines.
	 */
	private void updatePanel(int updateLine, boolean multipleLineChange)
	{
		// Make a note of how much changed, used when repainting.
		this.updatedLine = updateLine;
		this.multipleLinesChanged = multipleLineChange;
		updatePaintingCache();
		revalidate();
		repaint();
	}
	
	// ----- Methods required by GUIPlugin but not needed. -----

	public boolean displaysTab() {return false;}
	public JMenu getMenu() {return null;}
	public OptionsPanel getOptions() {return null;}
	public String getTabText() {return null;}
	public JToolBar getToolBar() {return null;}
	public String getXMLIDTag() {return null;}
	public Object getXMLSaveTree() {return null;}
	public void loadXML(Object c) {}
	public void takeCLArgs(String[] args) {}
	public void notifySettings(PrismSettings settings) {}

	public void setParseErrors(Map<Integer, String> errorLines) 
	{
		this.errorMessages = errorLines;
		repaint();
	}

}
