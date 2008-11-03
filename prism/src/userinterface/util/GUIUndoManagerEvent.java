package userinterface.util;

import userinterface.GUIPlugin;

public class GUIUndoManagerEvent extends GUIEvent 
{
	public static final int UNDO = 0;
	public static final int REDO = 1; 
	public static final int UNDOABLE_EVENT = 2;
	public static final int DISCARD_ALL = 3;
	public static final int LIMIT_CHANGE = 4;
	public static final int DIED = 5;
	
	public GUIUndoManagerEvent(int id, GUIPlugin comp) 
	{
		super(id, comp);
	}

	public GUIUndoManagerEvent(int id) 
	{
		super(id);
	}
}
