//==============================================================================
//
//	Copyright (c) 2002-2006, Andrew Hinton, Dave Parker, Charles Harley
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

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.datatransfer.Clipboard;
import javax.swing.event.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import prism.PrismLangException;
import prism.PrismSettings;
import prism.PrismSettingsListener;
import userinterface.GUIClipboardEvent;
import userinterface.GUIPrism;
//import userinterface.PluginNotFoundException;

/** Editing pane with syntax highlighting and line numbers etc for text 
 * model files. Currently supports Prism and Pepa models. It also tells 
 * the GUIPrism, of which it is a member, about modified events.
 * 
 * @author Andrew Hinton, Dave Parker, Charles Harley (cd.harley@talk21.com),
 * 		   Sebastian Vermehren (seb03@hotmail.com)
 * @version 1.4.2 08/12/2006
 */
public class GUITextModelEditor extends GUIModelEditor implements DocumentListener, MouseListener
{

	private GUIMultiModelHandler handler;
	/** Standard java editor component for editing the model files. A custom
	 * editor kit is used to provide syntax highlighting and line numbers.
	 */
	private JEditorPane editor;
	private DefaultHighlighter.DefaultHighlightPainter errorHighlightPainter;
	
	/** Allows undo/redo operations to be performed on the model editor.
	 */
	private UndoManager undoManager;
	private JScrollPane editorScrollPane;
	/** The line numbers etc. gutter for the model editor. */
	private GUITextModelEditorGutter gutter;
	/** Stores the current known Prism setting for the 'show line numbers'
	 * setting. Makes it possible to check if the setting has changed when
	 * a Prism setting change notification is issued.
	 */
	private boolean showLineNumbersSetting = true;
	
	/** The popup menu for the context menu. */
	private JPopupMenu contextPopup;
	
	/** Actions for the context menu. */
	private Action actionCut, actionCopy, actionPaste, actionUndo, actionRedo, actionSearch;
	
	/** More actions */
	private Action insertDTMC, insertCTMC, insertMDP;
	
	/* both null if not existent */
	private PrismLangException parseError;
	private Object parseErrorHighlightKey;
		
	/** Mouse listener to listen for when the mouse is over the editor pane. */
	MouseListener editorMouseListener = new MouseListener()
	{
		
		public void mouseEntered(MouseEvent e)
		{
			// Horrible hack to get the cursor to change to the text cursor when
			// over the editor pane. Directly setting the cursor on the 
			// JEditorPane didn't work.
			//GUIPrism.getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		}
		
		public void mouseExited(MouseEvent e)
		{
			// Horrible hack to get the cursor to change to the text cursor when
			// over the editor pane. Directly setting the cursor on the 
			// JEditorPane didn't work.
			//GUIPrism.getGUI().setCursor(Cursor.getDefaultCursor());
		}

		public void mouseClicked(MouseEvent e) {}

		public void mousePressed(MouseEvent e) {}

		public void mouseReleased(MouseEvent e) {}
		
	};
	
	/** 
	 * Constructor, initialises the editor components.
	 * 
	 * @param initialText The initial text to be displayed in the editor.
	 * @param handler The GUI handler for this component.
	 */
	public GUITextModelEditor(String initialText, GUIMultiModelHandler handler)
	{
		this.handler = handler;
		setLayout(new BorderLayout());
		
		// Setup the editor with it's custom editor kits. To switch between
		// editor kits just use setContentType() for the desired content type. 
		editor = new JEditorPane() 
		{	
			@Override
			public String getToolTipText(MouseEvent event) 
			{	
				if (parseError != null)
				{
					try
					{
						int offset = this.viewToModel(new Point(event.getX(), event.getY()));
					
						int startOffset = computeDocumentOffset(parseError.getBeginLine(), parseError.getBeginColumn());
						int endOffset =  computeDocumentOffset(parseError.getEndLine(), parseError.getEndColumn())+1;
						
						if (offset >= startOffset && offset <= endOffset)
							return parseError.getMessage();
					}
					catch (BadLocationException e)
					{}
				}
				
				return null;
			}
		};
		
		editor.setToolTipText("dummy");
		
		editor.setEditorKitForContentType("text/prism", new PrismEditorKit(handler));
		editor.setEditorKitForContentType("text/pepa", new PepaEditorKit(handler));
		// The default editor kit is the Prism one.
		editor.setContentType("text/prism");
		editor.setBackground(Color.white);
		editor.addMouseListener(editorMouseListener);
		editor.setEditable(true);
		editor.setText(initialText);
		editor.getDocument().addDocumentListener(this);
		editor.getDocument().addUndoableEditListener(new EditorUndoableEditListener());
		editor.getDocument().putProperty( PlainDocument.tabSizeAttribute, new Integer(4) );
		
		editor.addMouseListener(this);
		errorHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255,192,192));
		undoManager = new UndoManager();
		undoManager.setLimit(200);		
		
		// Setup the scrollpane
		editorScrollPane = new JScrollPane(editor);
		add(editorScrollPane, BorderLayout.CENTER);
		gutter = new GUITextModelEditorGutter(editor);
		
		// Get the 'show line numbers' setting to determine 
		// if the line numbers should be shown.
		showLineNumbersSetting = handler.getGUIPlugin().getPrism().getSettings().getBoolean(PrismSettings.MODEL_SHOW_LINE_NUMBERS);
		if (showLineNumbersSetting) {
			editorScrollPane.setRowHeaderView(gutter);
		}
		
		// Add a Prism settings listener to catch changes made to the 
		// 'show line numbers' setting.
		handler.getGUIPlugin().getPrism().getSettings().addSettingsListener(new PrismSettingsListener ()
		{
			public void notifySettings(PrismSettings settings)
			{
				// Check if the setting has changed.
				if (settings.getBoolean(PrismSettings.MODEL_SHOW_LINE_NUMBERS) != showLineNumbersSetting) {
					showLineNumbersSetting =! showLineNumbersSetting;
					if (showLineNumbersSetting) {
						editorScrollPane.setRowHeaderView(gutter);
					} else {
						editorScrollPane.setRowHeaderView(null);
					}
				}
			}
		});
		
		// initialize the actions for the context menu
		initActions();
		
		// method to initialize the context menu popup
		initContextMenu();
		
	    InputMap inputMap = editor.getInputMap();
	    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK), "undo");
	    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK), "redo");
	    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK | java.awt.event.InputEvent.SHIFT_MASK), "redo");
	    
		ActionMap actionMap = editor.getActionMap();
		actionMap.put("undo", actionUndo);
		actionMap.put("redo", actionRedo);
		
		
	}
	
	/**
	 * Helper method to initialize the actions used for the buttons.
	 */
	private void initActions() {
		actionCut = new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				editor.cut();
			}
		};
		actionCut.putValue(Action.LONG_DESCRIPTION, "Cuts the highlighted text to the clipboard.");
		actionCut.putValue(Action.NAME, "Cut");
		//actionCut.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("editcut.png"));
		
		actionCopy = new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				editor.copy();
			}
		};
		actionCopy.putValue(Action.LONG_DESCRIPTION, "Copies the highlighted text to the clipboard.");
		actionCopy.putValue(Action.NAME, "Copy");
		//actionCopy.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("editcopy.png"));
		
		actionPaste = new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				editor.paste();
			}
		};
		actionPaste.putValue(Action.LONG_DESCRIPTION, "Pastes the text stored in the clipboard.");
		actionPaste.putValue(Action.NAME, "Paste");
		//actionPaste.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("editpaste.png"));
	
		actionUndo = new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				try {
					// do redo
					undoManager.undo();
					
					// notify undo manager/toolbar of change
					GUIPrism.getGUI().notifyEventListeners(
							new GUIClipboardEvent(GUIClipboardEvent.UNDOMANAGER_CHANGE, 
									GUIPrism.getGUI().getFocussedPlugin().getFocussedComponent()));
				} catch (CannotUndoException ex) {
					//GUIPrism.getGUI().getMultiLogger().logMessage(PrismLogLevel.PRISM_ERROR, ex.getMessage());
				}
			}
		};
		actionUndo.putValue(Action.LONG_DESCRIPTION, "Undos the most recent action.");
		actionUndo.putValue(Action.NAME, "Undo");
		actionUndo.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallUndo.png"));
		
		actionRedo = new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				try {
					// do redo
					undoManager.redo();
					
					// notify undo manager/toolbar of change
					GUIPrism.getGUI().notifyEventListeners(
							new GUIClipboardEvent(GUIClipboardEvent.UNDOMANAGER_CHANGE, 
									GUIPrism.getGUI().getFocussedPlugin().getFocussedComponent()));
				} catch (CannotRedoException ex) {
					//GUIPrism.getGUI().getMultiLogger().logMessage(PrismLogLevel.PRISM_ERROR, ex.getMessage());
				}
			}
		};
		actionRedo.putValue(Action.LONG_DESCRIPTION, "Redos the most recent undo");
		actionRedo.putValue(Action.NAME, "Redo");
		actionRedo.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallRedo.png"));
		
		
		// search and replace action
        actionSearch = new AbstractAction() {
        	public void actionPerformed(ActionEvent ae) {
        		
        		// System.out.println("search button pressed");
        		/*	if (GUIMultiModelHandler.isDoingSearch()) {
					
				} else {
					try {
						GUIMultiModelHandler.setDoingSearch(true);
						FindReplaceForm.launch(GUIPrism.getGUI().getMultiModel());
					} catch (PluginNotFoundException pnfe) {
						GUIPrism.getGUI().getMultiLogger().logMessage(prism.log.PrismLogLevel.PRISM_ERROR,
								pnfe.getMessage());
					}
				}*/
        	}
        };
        actionSearch.putValue(Action.LONG_DESCRIPTION, "Opens a find and replace dialog.");
        //actionSearch.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("find.png"));
        actionSearch.putValue(Action.NAME, "Find/Replace");
        //actionSearch.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
        
        insertDTMC = new AbstractAction() {
        	public void actionPerformed(ActionEvent ae) {
        		int caretPosition = editor.getCaretPosition();
        		try
        	    {
        			editor.getDocument().insertString(caretPosition, "dtmc", new SimpleAttributeSet());
        	    }
        		catch (BadLocationException ble)
        		{
        		   //todo log?
        		}
        	}
        };
        
        insertDTMC.putValue(Action.LONG_DESCRIPTION, "Marks this model as a \"Discrete-Time Markov Chain\"");
        //actionSearch.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("find.png"));
        insertDTMC.putValue(Action.NAME, "Probabilistic (DTMC)");
        
        insertCTMC = new AbstractAction() {
        	public void actionPerformed(ActionEvent ae) {
        		int caretPosition = editor.getCaretPosition();
        		try
        	    {
        			editor.getDocument().insertString(caretPosition, "ctmc", new SimpleAttributeSet());
        	    }
        		catch (BadLocationException ble)
        		{
        		   //todo log?
        		}
        	}
        };
        
        insertCTMC.putValue(Action.LONG_DESCRIPTION, "Marks this model as a \"Continous-Time Markov Chain\"");
        //actionSearch.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("find.png"));
        insertCTMC.putValue(Action.NAME, "Stochastic (CTMC)");
       
        insertMDP = new AbstractAction() {
        	public void actionPerformed(ActionEvent ae) {
        		int caretPosition = editor.getCaretPosition();
        		try
        	    {
        			editor.getDocument().insertString(caretPosition, "mdp", new SimpleAttributeSet());
        	    }
        		catch (BadLocationException ble)
        		{
        		   //todo log?
        		}
        	}
        };
        
        insertMDP.putValue(Action.LONG_DESCRIPTION, "Marks this model as a \"Markov Decision Process\"");
        //actionSearch.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("find.png"));
        insertMDP.putValue(Action.NAME, "Non-deterministic (MDP)");
	
	}
	
	/**
	 * Helper method that initializes the items for the context menu. This
	 * menu will include cut, copy, paste, undo/redo, and find/replace
	 * functionality.
	 *
	 */
	private void initContextMenu() {
		contextPopup = new JPopupMenu();		
		contextPopup.add(actionUndo);
		contextPopup.add(actionRedo);
		contextPopup.add(new JSeparator());
		contextPopup.add(actionCut);
		contextPopup.add(actionCopy);
		contextPopup.add(actionPaste);
		contextPopup.add(new JSeparator());
		contextPopup.add(actionSearch);
		
		
		if (editor.getContentType().equals("text/prism"))
		{
			contextPopup.add(new JSeparator());
			JMenu insertMenu = new JMenu("Insert elements");
			JMenu insertModelTypeMenu = new JMenu("Model type");
			insertMenu.add(insertModelTypeMenu);
			JMenu insertModule = new JMenu("Module");
			insertMenu.add(insertModule);
			JMenu insertVariable = new JMenu("Variable");
			insertMenu.add(insertVariable);
			
			insertModelTypeMenu.add(insertDTMC);
			insertModelTypeMenu.add(insertCTMC);
			insertModelTypeMenu.add(insertMDP);
			contextPopup.add(insertMenu);
		}
	}
	
	/** Sets the content type of the editor, currently accepts 'text/prism' 
	 * and 'text/pepa'. Setting the content type changes the editor kit of 
	 * the editor to a kit that provides syntax highlighting etc for that
	 * content type.
	 * 
	 * @param contentType The content type
	 */
	public void setContentType(String contentType) {
		editor.setContentType(contentType);
	}
	
	/** Loads the model editor with the text from the given stream reader, 
	 * discards any previous content in the editor.
	 * 
	 * @param reader A character stream reader
	 * @param object An object describing the stream; this might be a string, a File, a URL, etc. 
	 * 
	 * @throws IOException Thrown if there was an IO error reading the input stream.
	 */
	public void read(Reader reader, Object object) throws IOException
	{
		editor.read(reader, object);
		
		// For some unknown reason the listeners have to be added both here
		// and in the constructor, if they're not added here the editor won't
		// be listening.
		editor.getDocument().addDocumentListener(this);
		editor.getDocument().addUndoableEditListener(new EditorUndoableEditListener());
	}
	
	public void setText(String text)
	{
		editor.setText(text);
	}
	
	public void write(Writer writer) throws IOException
	{
		editor.write(writer);
	}
	
	public String getTextString()
	{
		return editor.getText();
	}
	
	/** Resets the model editor with a blank document.
	 */
	public void newModel()
	{
		// Note: we use the read() method instead of setText() because
		// this avoids triggering the listener methods and hence 
		// unwanted autoparsing.
		try
		{
			read(new StringReader(""), "");
		} catch (IOException ex)
		{ 
			//todo:mark
			//GUIPrism.getGUI().getMultiLogger().logMessage(PrismLogLevel.PRISM_ERROR, ex.getMessage());
		}
	}
	
	/** Notifies the GUI that the document has been modified.
	 * 
	 * @param event Event generated by the change in the document.
	 */
	public void changedUpdate(DocumentEvent event)
	{
		// No need to notify the GUI as this event is covered by 
		// the insert and delete update methods.
	}
	
	/** Notifies the GUI that the document has been modified.
	 * 
	 * @param event Event generated by the change in the document.
	 */
	public void insertUpdate(DocumentEvent event)
	{
		if (handler != null) handler.hasModified(true);
	}
	
	
	/** Notifies the GUI that the document has been modified.
	 * 
	 * @param event Event generated by the change in the document.
	 */
	public void removeUpdate(DocumentEvent event)
	{
		if (handler != null) handler.hasModified(true);
	}
	
	public String getParseText()
	{
		return editor.getText();
	}
	
	/** Performs an undo operation on the text in the model editor.
     */
	public void undo() {
		try {
			undoManager.undo();
		} catch (CannotUndoException ex) {
			//GUIPrism.getGUI().getMultiLogger().logMessage(PrismLogLevel.PRISM_ERROR, ex.getMessage());
		}
	}
	
	/** Performs a redo operation on the text in the model editor.
     */
	public void redo() {
		try {
			undoManager.redo();
		} catch (CannotRedoException ex) {
			//GUIPrism.getGUI().getMultiLogger().logMessage(PrismLogLevel.PRISM_ERROR, ex.getMessage());
		}
	}
	
	public void copy()
	{
		editor.copy();
	}
	
	public void cut()
	{
		editor.cut();
	}
	
	public void paste()
	{
		editor.paste();
	}
	
	public void delete()
	{
		try
		{
			editor.getDocument().remove(editor.getSelectionStart(), editor.getSelectionEnd()-editor.getSelectionStart());
		}
		catch(BadLocationException ex)
		{
			//GUIPrism.getGUI().getMultiLogger().logMessage(PrismLogLevel.PRISM_ERROR, ex.getMessage());
		}
	}
	
	public void selectAll()
	{
		editor.selectAll();
	}
	
	public boolean isEditable()
	{
		return editor.isEditable();
	}
	
	public void setEditable(boolean b)
	{
		editor.setEditable(b);
	}
	
	public void setEditorFont(Font f)
	{
		editor.setFont(f);
	}
	
	public void setEditorBackground(Color c)
	{
		editor.setBackground(c);
	}
	
	/** Listens for changes made to the model editor and updates the UndoManager,
	 * allowing undo/redo operations to be performed.
	 */
	protected class EditorUndoableEditListener implements UndoableEditListener
	{
		public void undoableEditHappened(UndoableEditEvent e)
		{
			// Remember the edit and raise a listener event for any
			// other interested objects.
			undoManager.addEdit(e.getEdit());
			GUIClipboardEvent clipboardEvent = new GUIClipboardEvent(GUIClipboardEvent.UNDOMANAGER_CHANGE, 
					handler.getGUIPlugin().getFocussedComponent());
			// Send the undo manager with the event so that interested
			// objects can determine the state of the undo manager.
			clipboardEvent.setUndoManager(undoManager);
			GUIPrism.getGUI().notifyEventListeners(clipboardEvent);
		}
	}
	
	// rajk
	public JEditorPane getEditorPane(){
		return this.editor;
	}

	/**
	 * Mouse Listener methods.
	 * 
	 * spv
	 */
	public void mouseClicked(MouseEvent me) {
	
	}
	public void mousePressed(MouseEvent me) {
		
		if (me.isPopupTrigger()) {
			
			
			// check if to have paste enabled or not
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			if (clipboard.getContents(null) != null) {
				actionPaste.setEnabled(false);
			}			
			else {
				actionPaste.setEnabled(true);
			}
			
			// check if text highlighted or not
			if (editor.getSelectedText() == null) {
				actionCut.setEnabled(false);
				actionCopy.setEnabled(false);
			}
			else {
				actionCut.setEnabled(true);
				actionCopy.setEnabled(true);
			}
			
			// check undo
			if (undoManager.canUndo()) {
				actionUndo.setEnabled(true);
			}
			else {
				actionUndo.setEnabled(false);
			}
			
			// check redo
			if (undoManager.canRedo()) {
				actionRedo.setEnabled(true);
			}
			else {
				actionRedo.setEnabled(false);
			}
			
			contextPopup.show(me.getComponent(), me.getX(), me.getY());
			
		}
	}
	public void mouseEntered(MouseEvent me) {
		
	}
	public void mouseExited(MouseEvent me) {
		
	}
	public void mouseReleased(MouseEvent me) {
		
	}
	
	public void refreshErrorDisplay()
	{
		if (parseErrorHighlightKey != null)
			editor.getHighlighter().removeHighlight(parseErrorHighlightKey);
		
		/* Mapping for gutter */
	    Map<Integer, String> errorLines = new HashMap<Integer, String>();
			    		
		if (parseError != null && parseError.hasLineNumbers() )
		{
			String error = parseError.getMessage();
			
			for (int line = parseError.getBeginLine();
			     line <= parseError.getEndLine();
			     line++)
			{
				errorLines.put(line, error);
			}
		}
		
		gutter.setParseErrors(errorLines);
		
		/* Highlighting errors in editor */
		if (parseError != null && parseError.hasLineNumbers())
		{
			/* Remove existing highlight */
			try
			{
				parseErrorHighlightKey = editor.getHighlighter().addHighlight(
				  computeDocumentOffset(parseError.getBeginLine(), parseError.getBeginColumn()), 
				  computeDocumentOffset(parseError.getEndLine(), parseError.getEndColumn())+1, errorHighlightPainter);
			}
			catch (BadLocationException e)
			{
				
			}
			
		}
		
	}
	
	public int computeDocumentOffset(int line, int column) throws BadLocationException
	{
		if (line < 0 || column < 0) throw new BadLocationException("Negative line/col", -1);
		
		Element lineElement = editor.getDocument().getDefaultRootElement().
		getElement(line-1);
		
		int beginLineOffset = lineElement.getStartOffset();
		int endLineOffset = lineElement.getEndOffset();
		
		String text = editor.getDocument().getText(beginLineOffset, endLineOffset - beginLineOffset);
		
		int parserChar = 1;
		int documentChar = 0;		
		
		while (parserChar < column)
		{
			if (text.charAt(documentChar) == '\t')
			{
				parserChar += 8;
				documentChar += 1;
			}
			else
			{
				parserChar += 1;
				documentChar += 1;
			}		
		}
				
		return beginLineOffset+documentChar;		
	}
	
	public void modelParseFailed(PrismLangException parserError) 
	{	
		this.parseError = parserError;		
		refreshErrorDisplay();
	}

	@Override
	public void modelParseSuccessful() 
	{
		this.parseError = null;
		refreshErrorDisplay();
	} 
	
	
}