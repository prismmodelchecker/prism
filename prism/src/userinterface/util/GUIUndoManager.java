//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//  * Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
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

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import userinterface.GUIPrism;

public class GUIUndoManager extends GUIEventHandler implements UndoableEditListener
{
	private UndoManager undoManager;
	
	public GUIUndoManager(GUIPrism gui) 
	{
		super(gui);	
		undoManager = new UndoManager();
	}

	public boolean addEdit(UndoableEdit anEdit) 
	{
		boolean result = undoManager.addEdit(anEdit);
		if (result)
			notifyListeners(new GUIUndoManagerEvent(GUIUndoManagerEvent.UNDOABLE_EVENT));
		return result;
	}

	public boolean canRedo() 
	{
		return undoManager.canRedo();
	}

	public boolean canUndo() 
	{
		return undoManager.canUndo();
	}

	public boolean canUndoOrRedo() 
	{
		return undoManager.canUndoOrRedo();
	}

	public void die() 
	{
		undoManager.die();
		notifyListeners(new GUIUndoManagerEvent(GUIUndoManagerEvent.DIED));
	}

	public void discardAllEdits() 
	{
		undoManager.discardAllEdits();
		notifyListeners(new GUIUndoManagerEvent(GUIUndoManagerEvent.DISCARD_ALL));
	}

	public void end() 
	{
		undoManager.end();
		notifyListeners(new GUIUndoManagerEvent(GUIUndoManagerEvent.DIED));
	}

	public boolean equals(Object obj) 
	{
		return undoManager.equals(obj);
	}

	public int getLimit() 
	{
		return undoManager.getLimit();
	}

	public String getPresentationName() 
	{
		return undoManager.getPresentationName();
	}

	public String getRedoPresentationName() 
	{
		return undoManager.getRedoPresentationName();
	}

	public String getUndoOrRedoPresentationName() 
	{
		return undoManager.getUndoOrRedoPresentationName();
	}

	public String getUndoPresentationName() 
	{
		return undoManager.getUndoPresentationName();
	}

	public int hashCode() 
	{
		return undoManager.hashCode();
	}

	public boolean isInProgress() 
	{
		return undoManager.isInProgress();
	}

	public boolean isSignificant() 
	{
		return undoManager.isSignificant();
	}

	public void redo() throws CannotRedoException {
		undoManager.redo();
		notifyListeners(new GUIUndoManagerEvent(GUIUndoManagerEvent.REDO));
	}

	public boolean replaceEdit(UndoableEdit anEdit) {
		return false;
	}

	public void setLimit(int l) {
		undoManager.setLimit(l);
		notifyListeners(new GUIUndoManagerEvent(GUIUndoManagerEvent.LIMIT_CHANGE));
	}

	public String toString() {
		return undoManager.toString();
	}

	public void undo() throws CannotUndoException {
		undoManager.undo();
		notifyListeners(new GUIUndoManagerEvent(GUIUndoManagerEvent.UNDO));
	}

	public void undoableEditHappened(UndoableEditEvent e) {
		undoManager.undoableEditHappened(e);
		notifyListeners(new GUIUndoManagerEvent(GUIUndoManagerEvent.UNDOABLE_EVENT));
	}

	public void undoOrRedo() throws CannotRedoException, CannotUndoException {
		undoManager.undoOrRedo();
		notifyListeners(new GUIUndoManagerEvent(GUIUndoManagerEvent.UNDO));		
	}
	
	
}
