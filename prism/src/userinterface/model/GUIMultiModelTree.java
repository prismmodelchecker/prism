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

package userinterface.model;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import parser.ast.ConstantList;
import parser.ast.Declaration;
import parser.ast.DeclarationInt;
import parser.ast.Expression;
import parser.ast.ExpressionIdent;
import parser.ast.Module;
import parser.ast.ModulesFile;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeClock;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.ModelType;
import prism.Prism;
import prism.PrismException;
import userinterface.GUIPrism;

@SuppressWarnings("serial")
public class GUIMultiModelTree extends JPanel implements MouseListener
{
	//Constants
	public static final int TREE_SYNCHRONIZED_GOOD = 0;
	public static final int TREE_SYNCHRONIZED_BAD = 1;
	public static final int TREE_NOT_SYNCHRONIZED = 2;

	//Attributes
	private GUIMultiModelHandler handler;
	private boolean editable;

	//Graphical elements
	private JTree tree;
	private JPopupMenu moduleCollectionPopup;
	private Action addModule;
	private JPopupMenu declarationCollectionPopup;
	private Action addBooleanGlobal, addIntegerGlobal;
	private JPopupMenu constantsCollectionPopup;
	private Action addBooleanConstant, addDoubleConstant, addIntegerConstant;
	private JPopupMenu modulePopup;
	private Action renameModule, removeModule, addBoolean, addInteger;
	private JPopupMenu declarationPopup;
	private Action renameDeclaration, removeDeclaration;
	private JPopupMenu expressionPopup;
	private Action editExpression;
	private JPopupMenu modelTypePopup;
	private JRadioButton non, pro, sto;

	//Tree nodes
	private DefaultTreeModel theModel;
	private ModelRootNode root;
	private ModelTypeNode modelType;
	private ModuleCollectionNode modules;
	private DeclarationCollectionNode declarations;
	private ConstantCollectionNode constants;

	private ArrayList<ModuleNode> editableModules;
	private ArrayList<DeclarationNode> editableDeclarations;
	private ArrayList<ConstantNode> editableConstants;

	private ImageIcon animIcon;
	private boolean isParsing;
	private IconThread parseThread;

	private PrismTreeNode lastPopNode;

	//State
	private int parseSynchState;

	public GUIMultiModelTree(GUIMultiModelHandler handler, boolean editable)
	{
		this.handler = handler;
		this.editable = editable;
		root = new ModelRootNode();
		theModel = new DefaultTreeModel(root);
		newTree(editable);
		initComponents();
		animIcon = GUIPrism.getIconFromImage("smallClockAnim1.png");
		isParsing = false;
		tree.addMouseListener(this);
		tree.setEditable(true);
		ToolTipManager.sharedInstance().registerComponent(tree);
	}

	public GUIMultiModelTree(GUIMultiModelHandler handler)
	{
		this(handler, false);
	}

	static int modCounter = 1;

	public void a_addModule()
	{
		ModuleNode newNode = new ModuleNode("newModule" + modCounter, true);
		newNode.addVariable(new StateVarNode(newNode));
		editableModules.add(newNode);
		modules.addModule(newNode);
		int index = modules.getIndex(newNode);
		theModel.nodesWereInserted(modules, new int[] { index });
		modCounter++;
	}

	public ModuleNode a_requestNewModule(String name)
	{
		ModuleNode newNode = new ModuleNode(name, true);
		newNode.addVariable(new StateVarNode(newNode));
		editableModules.add(newNode);
		modules.addModule(newNode);
		int index = modules.getIndex(newNode);
		theModel.nodesWereInserted(modules, new int[] { index });
		modCounter++;
		return newNode;
	}

	static int globCounter = 1;

	public void a_addIntegerGlobal()
	{
		try {
			Expression init = Prism.parseSingleExpressionString("0");
			Expression min = Prism.parseSingleExpressionString("0");
			Expression max = Prism.parseSingleExpressionString("0");

			GlobalNode newNode = new GlobalNode("g" + globCounter, init, min, max, true);
			editableDeclarations.add(newNode);
			declarations.addDeclaration(newNode);
			int index = declarations.getIndex(newNode);
			theModel.nodesWereInserted(declarations, new int[] { index });
			globCounter++;
		} catch (Exception e) {
			System.err.println("UNEXPECTED ERROR: " + e.getMessage());
			e.printStackTrace();
			//This should never happen
		}

		handler.hasModified(true);
	}

	public void a_addIntegerGlobal(String name, String mins, String maxs, String inits) throws PrismException
	{
		try {
			Expression init = Prism.parseSingleExpressionString(inits);
			Expression min = Prism.parseSingleExpressionString(mins);
			Expression max = Prism.parseSingleExpressionString(maxs);

			GlobalNode newNode = new GlobalNode(name, init, min, max, true);
			editableDeclarations.add(newNode);
			declarations.addDeclaration(newNode);
			int index = declarations.getIndex(newNode);
			theModel.nodesWereInserted(declarations, new int[] { index });
			globCounter++;
		} catch (Exception e) {
			throw new PrismException("Global integer " + name + " has invalid parameter");
		}

		handler.hasModified(true);
	}

	public void a_addBooleanGlobal()
	{
		try {
			Expression init = Prism.parseSingleExpressionString("false");
			GlobalBoolNode newNode = new GlobalBoolNode("g" + globCounter, init, true);
			editableDeclarations.add(newNode);
			declarations.addDeclaration(newNode);
			int index = declarations.getIndex(newNode);
			theModel.nodesWereInserted(declarations, new int[] { index });
			globCounter++;
		} catch (Exception e) {
			System.err.println("UNEXPECTED ERROR: " + e.getMessage());
			e.printStackTrace();
			//This should never happen
		}
		handler.hasModified(true);
	}

	public void a_addBooleanGlobal(String name, String inits) throws PrismException
	{
		try {
			Expression init = Prism.parseSingleExpressionString(inits);
			GlobalBoolNode newNode = new GlobalBoolNode(name, init, true);
			editableDeclarations.add(newNode);
			declarations.addDeclaration(newNode);
			int index = declarations.getIndex(newNode);
			theModel.nodesWereInserted(declarations, new int[] { index });
			globCounter++;
		} catch (Exception e) {
			throw new PrismException("Global boolean " + name + " has invalid parameter");
		}
		handler.hasModified(true);
	}

	static int consCount = 1;

	public void a_addIntegerConstant()
	{
		try {
			Expression value = Prism.parseSingleExpressionString("0");
			IntegerConstantNode newNode = new IntegerConstantNode("c" + consCount, value, true);
			editableConstants.add(newNode);
			constants.addConstant(newNode);
			int index = constants.getIndex(newNode);
			theModel.nodesWereInserted(constants, new int[] { index });
			consCount++;
		} catch (Exception e) {
			System.err.println("UNEXPECTED ERROR: " + e.getMessage());
			e.printStackTrace();
		}
		handler.hasModified(true);
	}

	public void a_addIntegerConstant(String name, String val) throws PrismException
	{
		try {
			Expression value;
			if (val == null)
				value = null;
			else
				value = Prism.parseSingleExpressionString(val);
			IntegerConstantNode newNode = new IntegerConstantNode(name, value, true);
			editableConstants.add(newNode);
			constants.addConstant(newNode);
			int index = constants.getIndex(newNode);
			theModel.nodesWereInserted(constants, new int[] { index });
			consCount++;
		} catch (Exception e) {
			throw new PrismException("Constant integer " + name + " has invalid parameter");
		}
		handler.hasModified(true);
	}

	public void a_addBooleanConstant()
	{
		try {
			Expression value = Prism.parseSingleExpressionString("false");
			BoolConstantNode newNode = new BoolConstantNode("c" + consCount, value, true);
			editableConstants.add(newNode);
			constants.addConstant(newNode);
			int index = constants.getIndex(newNode);
			theModel.nodesWereInserted(constants, new int[] { index });
			consCount++;
		} catch (Exception e) {
			System.err.println("UNEXPECTED ERROR: " + e.getMessage());
			e.printStackTrace();
		}
		handler.hasModified(true);
	}

	public void a_addBooleanConstant(String name, String val) throws PrismException
	{
		try {
			Expression value;
			if (val == null)
				value = null;
			else
				value = Prism.parseSingleExpressionString(val);
			BoolConstantNode newNode = new BoolConstantNode(name, value, true);
			editableConstants.add(newNode);
			constants.addConstant(newNode);
			int index = constants.getIndex(newNode);
			theModel.nodesWereInserted(constants, new int[] { index });
			consCount++;
		} catch (Exception e) {
			throw new PrismException("Constant boolean " + name + " has invalid parameter");
		}
		handler.hasModified(true);
	}

	public void a_addDoubleConstant()
	{
		try {
			Expression value = Prism.parseSingleExpressionString("0.0");
			DoubleConstantNode newNode = new DoubleConstantNode("c" + consCount, value, true);
			editableConstants.add(newNode);
			constants.addConstant(newNode);
			int index = constants.getIndex(newNode);
			theModel.nodesWereInserted(constants, new int[] { index });
			consCount++;
		} catch (Exception e) {
			System.err.println("UNEXPECTED ERROR: " + e.getMessage());
			e.printStackTrace();
		}
		handler.hasModified(true);
	}

	public void a_addDoubleConstant(String name, String val) throws PrismException
	{
		try {
			Expression value;
			if (val == null)
				value = null;
			else
				value = Prism.parseSingleExpressionString(val);
			DoubleConstantNode newNode = new DoubleConstantNode(name, value, true);
			editableConstants.add(newNode);
			constants.addConstant(newNode);
			int index = constants.getIndex(newNode);
			theModel.nodesWereInserted(constants, new int[] { index });
			consCount++;
		} catch (Exception e) {
			throw new PrismException("Constant double " + name + " has invalid parameter");
		}
		handler.hasModified(true);
	}

	public void a_removeModule(ModuleNode m)
	{
		int index = modules.getIndex(m);
		editableModules.remove(m);
		modules.remove(m);
		theModel.nodesWereRemoved(modules, new int[] { index }, new Object[] { m });
		handler.hasModified(true);
	}

	public void a_renameModule(ModuleNode m)
	{

		String s = JOptionPane.showInputDialog("New module name:", m.getName());
		if (s == null)
			return;

		try {
			Expression exp = Prism.parseSingleExpressionString(s);
			if (exp instanceof ExpressionIdent) {
				m.setName(s);
				theModel.nodeChanged(m);
				theModel.nodeStructureChanged(m);
			} else {
				handler.getGUIPlugin().error("Invalid module name");
			}
		} catch (PrismException exx) {
			handler.getGUIPlugin().error("Invalid module name");
		}

		handler.hasModified(true);
	}

	public void a_renameDeclaration(DeclarationNode d)
	{
		String type = "";
		if (d instanceof BoolNode || d instanceof VarNode)
			type = " variable";
		else if (d instanceof GlobalNode || d instanceof GlobalBoolNode)
			type = " global";
		else if (d instanceof ConstantNode)
			type = " constant";

		String s = JOptionPane.showInputDialog("New" + type + " name:", d.getName());
		if (s == null)
			return;

		try {
			Expression exp = Prism.parseSingleExpressionString(s);
			if (exp instanceof ExpressionIdent) {
				d.setName(s);
				theModel.nodeChanged(d);
				theModel.nodeStructureChanged(d);
			} else {
				handler.getGUIPlugin().error("Invalid declaration name");
			}
		} catch (PrismException exx) {
			handler.getGUIPlugin().error("Invalid module name");
		}

		handler.hasModified(true);
	}

	public void a_removeVariable(DeclarationNode d, ModuleNode m)
	{
		int index = m.getIndex(d);
		m.remove(d);
		theModel.nodesWereRemoved(m, new int[] { index }, new Object[] { d });
		handler.hasModified(true);
	}

	public void a_removeGlobal(DeclarationNode d)
	{
		int index = declarations.getIndex(d);
		editableDeclarations.remove(d);
		declarations.remove(d);
		theModel.nodesWereRemoved(declarations, new int[] { index }, new Object[] { d });
		handler.hasModified(true);
	}

	public void a_removeConstant(ConstantNode c)
	{
		int index = constants.getIndex(c);
		editableConstants.remove(c);
		constants.remove(c);
		theModel.nodesWereRemoved(constants, new int[] { index }, new Object[] { c });
		handler.hasModified(true);
	}

	public void a_addLocalBoolean(ModuleNode m, BooleanVariable var) throws PrismException
	{
		try {
			Expression init;
			if (var != null)
				init = Prism.parseSingleExpressionString(var.init);
			else
				init = Prism.parseSingleExpressionString("false");

			BoolNode newNode = new BoolNode(var.name, init, true);
			m.add(newNode);
			int index = m.getIndex(newNode);
			theModel.nodesWereInserted(m, new int[] { index });
			varCount++;
		} catch (Exception e) {
			throw new PrismException("Local boolean " + var.name + " has an invalid parameter");
		}
		handler.hasModified(true);
	}

	static int varCount = 1;

	public void a_addLocalBoolean(ModuleNode m)
	{
		try {
			Expression init = Prism.parseSingleExpressionString("false");

			BoolNode newNode = new BoolNode("v" + varCount, init, true);
			m.add(newNode);
			int index = m.getIndex(newNode);
			theModel.nodesWereInserted(m, new int[] { index });
			varCount++;
		} catch (Exception e) {
			System.err.println("UNEXPECTED ERROR: " + e.getMessage());
			e.printStackTrace();
			//This should never happen
		}
		handler.hasModified(true);
	}

	public void a_addLocalInteger(ModuleNode m, IntegerVariable var) throws PrismException
	{
		try {
			Expression init;
			if (var.init == null)
				init = Prism.parseSingleExpressionString("0");
			else
				init = Prism.parseSingleExpressionString(var.init);
			Expression min = Prism.parseSingleExpressionString(var.min);
			Expression max = Prism.parseSingleExpressionString(var.max);

			VarNode newNode = new VarNode(var.name, init, min, max, true);
			m.addVariable(newNode);
			int index = m.getIndex(newNode);
			theModel.nodesWereInserted(m, new int[] { index });
			varCount++;
		} catch (Exception e) {
			throw new PrismException("Local integer " + var.name + " has an invalid parameter");
		}
		handler.hasModified(true);
	}

	public void a_addLocalInteger(ModuleNode m)
	{
		try {
			Expression init = Prism.parseSingleExpressionString("0");
			Expression min = Prism.parseSingleExpressionString("0");
			Expression max = Prism.parseSingleExpressionString("0");

			VarNode newNode = new VarNode("v" + varCount, init, min, max, true);
			m.addVariable(newNode);
			int index = m.getIndex(newNode);
			theModel.nodesWereInserted(m, new int[] { index });
			varCount++;
		} catch (Exception e) {
			System.err.println("UNEXPECTED ERROR: " + e.getMessage());
			e.printStackTrace();
			//This should never happen
		}
		handler.hasModified(true);
	}

	public void a_editExpression(ExpressionNode en)
	{
		String defa;
		if (en.getValue() == null)
			defa = "?";
		else
			defa = en.getValue().toString();
		String s = JOptionPane.showInputDialog("New Expression:", defa);
		if (s == null)
			return;
		try {
			if (s.equals("")) {
				en.setValue(null);
			} else {
				Expression exp = Prism.parseSingleExpressionString(s);
				en.setValue(exp);
			}
			theModel.nodeChanged(en);
		} catch (Exception e) {
			handler.getGUIPlugin().error("Syntax error in expression:\n" + s);
		}

		handler.hasModified(true);
	}

	public void a_setModelType(ModelType type)
	{
		modelType.setModelType(type);
		handler.hasModified(true);
	}

	public int getParseSynchState()
	{
		return parseSynchState;
	}

	public void startParsing()
	{
		isParsing = true;
		if (parseThread != null)
			parseThread.interrupt();
		parseThread = new IconThread(0);
		parseThread.start();
	}

	public void stopParsing()
	{
		isParsing = false;
		if (parseThread != null)
			parseThread.interrupt();
		tree.repaint();
	}

	public void newTree()
	{
		this.newTree(false);
	}

	public void newTree(boolean editable)
	{
		this.editable = editable;
		root.removeAllChildren();
		parseSynchState = TREE_NOT_SYNCHRONIZED;
		{
			root.setUserObject("Model: " + handler.getShortActiveFileName());
			modelType = new ModelTypeNode("<Unknown>", editable);
			root.add(modelType);
			if (editable) {
				modules = new GUIMultiModelTree.ModuleCollectionNode();
				declarations = new GUIMultiModelTree.DeclarationCollectionNode();
				constants = new GUIMultiModelTree.ConstantCollectionNode();
				root.add(modules);
				root.add(declarations);
				root.add(constants);
				theModel.nodeStructureChanged(root);
			} else {
				modules = new ModuleCollectionNode();
				declarations = new DeclarationCollectionNode();
				constants = new ConstantCollectionNode();
				theModel.nodeStructureChanged(root);
			}
		}
		editableModules = new ArrayList<ModuleNode>();
		editableDeclarations = new ArrayList<DeclarationNode>();
		editableConstants = new ArrayList<ConstantNode>();

		modCounter = 1;
	}

	public void update(ModulesFile parsedModel)
	{
		parseSynchState = (parsedModel == null) ? TREE_NOT_SYNCHRONIZED : TREE_SYNCHRONIZED_GOOD;
		if (editable)
			updateEditable(parsedModel);
		else
			updateUnEditable(parsedModel);
	}

	private void updateUnEditable(ModulesFile parsedModel)
	{
		String fn = handler.getShortActiveFileName();
		if (!root.getUserObject().equals(fn)) {
			root.setUserObject("Model: " + fn);
			theModel.nodeChanged(root);
		}

		if (parsedModel != null) {
			String tp = parsedModel.getTypeString();
			if (!modelType.getValue().equals(tp)) {
				modelType.setUserObject(tp);
				theModel.nodeChanged(modelType);
			}
		} else {
			String tp = "<Unknown>";
			if (!modelType.getValue().equals(tp)) {
				modelType.setUserObject(tp);
				theModel.nodeChanged(modelType);
			}
		}

		if (tree != null)
			tree.repaint();
	}

	public boolean moduleExists(ModuleNode mNode, ArrayList<Module> inTree, ArrayList<Module> outTree)
	{
		for (int i = 0; i < inTree.size(); i++) {
			Module m = inTree.get(i);
			if (mNode.getName().equals(m.getName())) {
				return true;
			}
		}
		for (int i = 0; i < outTree.size(); i++) {
			Module m = outTree.get(i);
			if (mNode.getName().equals(m.getName())) {
				return true;
			}
		}
		return false;
	}

	public boolean variableExists(DeclarationNode vNode, ArrayList<Declaration> inTree, ArrayList<Declaration> outTree)
	{
		for (int i = 0; i < inTree.size(); i++) {
			Declaration d = inTree.get(i);
			if (vNode.getName().equals(d.getName())) {
				return true;
			}
		}
		for (int i = 0; i < outTree.size(); i++) {
			Declaration d = outTree.get(i);
			if (vNode.getName().equals(d.getName())) {
				return true;
			}
		}
		return false;
	}

	public boolean declarationExists(DeclarationNode vNode, ArrayList<Declaration> inTree, ArrayList<Declaration> outTree)
	{
		for (int i = 0; i < inTree.size(); i++) {
			Declaration d = inTree.get(i);
			if (vNode.getName().equals(d.getName())) {
				return true;
			}
		}
		for (int i = 0; i < outTree.size(); i++) {
			Declaration d = outTree.get(i);
			if (vNode.getName().equals(d.getName())) {
				return true;
			}
		}
		return false;
	}

	public boolean constantExists(ConstantNode cNode, ArrayList<ConstantNode> inTree, ArrayList<ConstantNode> outTree)
	{
		for (int i = 0; i < inTree.size(); i++) {
			ConstantNode c = inTree.get(i);
			if (c.equals(cNode))
				return true;
		}

		for (int i = 0; i < outTree.size(); i++) {
			ConstantNode c = outTree.get(i);
			if (c.equals(cNode))
				return true;
		}
		return false;
	}

	public boolean moduleIsMember(Module aMod)
	{
		boolean member = false;
		for (int i = 0; i < modules.getChildCount(); i++) {
			ModuleNode node = (ModuleNode) modules.getChildAt(i);
			if (node.getName().equals(aMod.getName())) {
				member = true;
				break;
			}
		}
		return member;
	}

	public boolean declarationIsMember(Declaration d)
	{
		boolean member = false;
		for (int i = 0; i < declarations.getChildCount(); i++) {
			DeclarationNode dec = (DeclarationNode) declarations.getChildAt(i);
			if (dec.getName().equals(d.getName())) //check same name
			{
				if (dec instanceof GlobalBoolNode && d.getType() instanceof TypeBool) //check type equality
				{
					member = true;
					break;
				}
				if (dec instanceof GlobalNode && d.getType() instanceof TypeInt) {
					member = true;
					break;
				}
			}
		}
		return member;
	}

	public boolean constantIsMember(ConstantNode n)
	{
		boolean member = false;
		for (int i = 0; i < constants.getChildCount(); i++) {
			ConstantNode inC = (ConstantNode) constants.getChildAt(i);
			if (inC.equals(n)) {
				member = true;
				break;
			}
		}
		return member;
	}

	public boolean variableIsMember(Declaration d, ModuleNode node)
	{
		boolean member = false;
		for (int i = 0; i < node.getChildCount(); i++) {
			DeclarationNode dec = (DeclarationNode) node.getChildAt(i);
			if (dec.getName().equals(d.getName())) //check same name
			{
				if (dec instanceof BoolNode && d.getType() instanceof TypeBool) //check type equality
				{
					member = true;
					break;
				}
				if (dec instanceof VarNode && d.getType() instanceof TypeInt) {
					member = true;
					break;
				}
			}
		}
		return member;
	}

	public int getVarTreeIndexOf(Declaration d, ModuleNode node)
	{
		int member = -1;
		for (int i = 0; i < node.getChildCount(); i++) {
			DeclarationNode vNode = (DeclarationNode) node.getChildAt(i);
			if (vNode.getName().equals(d.getName())) {
				member = i;
				break;
			}
		}
		return member;
	}

	public int getModuleTreeIndexOf(Module aMod)
	{
		int member = -1;
		for (int i = 0; i < modules.getChildCount(); i++) {
			ModuleNode node = (ModuleNode) modules.getChildAt(i);
			if (node.getName().equals(aMod.getName())) {
				member = i;
				break;
			}
		}
		return member;
	}

	public int getIndexOfDec(Declaration d)
	{
		int member = -1;
		for (int i = 0; i < declarations.getChildCount(); i++) {
			DeclarationNode dec = (DeclarationNode) declarations.getChildAt(i);
			if (dec.getName().equals(d.getName())) {
				member = i;
				break;
			}
		}
		return member;
	}

	public int getIndexOfCon(ConstantNode cn)
	{
		int member = -1;

		for (int i = 0; i < constants.getChildCount(); i++) {
			ConstantNode cc = (ConstantNode) constants.getChildAt(i);
			if (cc.equals(cn)) {
				member = i;
				break;
			}
		}
		return member;
	}

	private void updateEditable(ModulesFile parsedModel)
	{
		String fn = handler.getShortActiveFileName();
		if (!root.getUserObject().equals(fn)) {
			root.setUserObject("Model: " + fn);
			theModel.nodeChanged(root);
		}

		if (parsedModel != null) {
			if (!modelType.getValue().equals(parsedModel.getType().toString())) {
				modelType.setModelType(parsedModel.getModelType());
			}

			//MODULES

			//If there is no module tree, but we need to have modules, add the tre
			if (root.getIndex(modules) == -1 && parsedModel.getNumModules() > 0) {
				root.add(modules);
				theModel.nodesWereInserted(root, new int[] { root.getIndex(modules) });
			}

			/*  Create 2 ArrayLists.  One of Modules in the tree and one of modules
			 *  not in the tree. */
			ArrayList<Module> inTree = new ArrayList<Module>();
			ArrayList<Module> notInTree = new ArrayList<Module>();

			for (int i = 0; i < parsedModel.getNumModules(); i++) {
				Module aMod = parsedModel.getModule(i);
				if (moduleIsMember(aMod))
					inTree.add(aMod);
				else
					notInTree.add(aMod);
			}

			//make sure modules in the tree are valid
			for (int i = 0; i < inTree.size(); i++) {
				Module inTreeMod = inTree.get(i);
				int treeIndex = getModuleTreeIndexOf(inTreeMod); //should not be -1
				ModuleNode inTreeNode = (ModuleNode) modules.getChildAt(treeIndex);

				/*  Check its variables getting variables which are already there
				 *  and putting them in varInTree, and getting variables which
				 *  are not there and putting them in varNotInTree*/
				ArrayList<Declaration> varInTree = new ArrayList<Declaration>();
				ArrayList<Declaration> varNotInTree = new ArrayList<Declaration>();

				for (int j = 0; j < inTreeMod.getNumDeclarations(); j++) {
					Declaration d = inTreeMod.getDeclaration(j);
					if (variableIsMember(d, inTreeNode))
						varInTree.add(d);
					else
						varNotInTree.add(d);
				}

				//make sure variables in this module tree are valid
				for (int j = 0; j < varInTree.size(); j++) {
					Declaration inTreeDec = varInTree.get(j);
					int decIndex = getVarTreeIndexOf(inTreeDec, inTreeNode);
					DeclarationNode dNode = (DeclarationNode) inTreeNode.getChildAt(decIndex);
					if (dNode instanceof VarNode) {
						VarNode vNode = (VarNode) dNode;
						vNode.setInitial(parsedModel.getSystemDefn() == null ? inTreeDec.getStartOrDefault() : null);
						if (inTreeDec.getDeclType() instanceof DeclarationInt) {
							DeclarationInt declInt = (DeclarationInt) inTreeDec.getDeclType();
							vNode.setMin(declInt.getLow());
							vNode.setMax(declInt.getHigh());
						}
						theModel.nodesChanged(vNode, new int[] { 0, 1, 2 });
					} else if (dNode instanceof BoolNode) {
						BoolNode bNode = (BoolNode) dNode;
						bNode.setInitial(parsedModel.getSystemDefn() == null ? inTreeDec.getStartOrDefault() : null);
						theModel.nodesChanged(bNode, new int[] { 0 });
					}

				}
				//add variable in varNotInTree to the tree
				int[] cIndices = new int[varNotInTree.size()];
				for (int j = 0; j < varNotInTree.size(); j++) {
					Declaration notTreeDec = varNotInTree.get(j);
					if (notTreeDec.getType() instanceof TypeInt) {
						if (notTreeDec.getDeclType() instanceof DeclarationInt) {
							DeclarationInt declInt = (DeclarationInt) notTreeDec.getDeclType();
							VarNode newVariable = new VarNode(notTreeDec.getName(),
									parsedModel.getSystemDefn() == null ? notTreeDec.getStartOrDefault() : null, declInt.getLow(), declInt.getHigh(), false);
							inTreeNode.add(newVariable);
							cIndices[j] = getVarTreeIndexOf(notTreeDec, inTreeNode);
						} else {
							VarNode newVariable = new VarNode(notTreeDec.getName(),
									parsedModel.getSystemDefn() == null ? notTreeDec.getStartOrDefault() : null, null, null, false);
							inTreeNode.add(newVariable);
							cIndices[j] = getVarTreeIndexOf(notTreeDec, inTreeNode);
						}
					} else if (notTreeDec.getType() instanceof TypeBool) {
						BoolNode newVariable = new BoolNode(notTreeDec.getName(), parsedModel.getSystemDefn() == null ? notTreeDec.getStartOrDefault() : null,
								false);
						inTreeNode.add(newVariable);
						cIndices[j] = getVarTreeIndexOf(notTreeDec, inTreeNode);
					}
				}
				theModel.nodesWereInserted(inTreeNode, cIndices);

				/*  remove variables which are in the tree but not in varInTree or
				 *  varNotInTree */
				ArrayList<DeclarationNode> removeNodes = new ArrayList<DeclarationNode>();
				for (int j = 0; j < inTreeNode.getChildCount(); j++) {
					DeclarationNode vNode = (DeclarationNode) inTreeNode.getChildAt(j);
					if (!variableExists(vNode, varInTree, varNotInTree)) {
						removeNodes.add(vNode);
					}
				}
				Object[] remObj = new Object[removeNodes.size()];
				int[] remInd = new int[removeNodes.size()];
				for (int j = 0; j < removeNodes.size(); j++) {
					DeclarationNode vNode = removeNodes.get(j);
					int index = inTreeNode.getIndex(vNode);
					remObj[j] = vNode;
					remInd[j] = index;
				}
				//remove nodes backwards
				for (int j = removeNodes.size() - 1; j >= 0; j--) {
					inTreeNode.remove(remInd[j]);
				}
				theModel.nodesWereRemoved(inTreeNode, remInd, remObj);
			}

			//add modules which are not in the tree
			for (int i = 0; i < notInTree.size(); i++) {
				Module aMod = notInTree.get(i);
				ModuleNode newNode = new ModuleNode(aMod.getName(), false);
				//add variables to this
				for (int j = 0; j < aMod.getNumDeclarations(); j++) {
					Declaration aDec = aMod.getDeclaration(j);

					if (aDec.getType() instanceof TypeInt) {
						if (aDec.getDeclType() instanceof DeclarationInt) {
							DeclarationInt declInt = (DeclarationInt) aDec.getDeclType();
							VarNode newVariable = new VarNode(aDec.getName(), parsedModel.getSystemDefn() == null ? aDec.getStartOrDefault() : null,
									declInt.getLow(), declInt.getHigh(), false);
							newNode.add(newVariable);
						} else {
							VarNode newVariable = new VarNode(aDec.getName(), parsedModel.getSystemDefn() == null ? aDec.getStartOrDefault() : null, null,
									null, false);
							newNode.add(newVariable);
						}
					} else if (aDec.getType() instanceof TypeBool) {
						BoolNode newVariable = new BoolNode(aDec.getName(), parsedModel.getSystemDefn() == null ? aDec.getStartOrDefault() : null, false);
						newNode.add(newVariable);
					}
				}
				modules.addModule(newNode);
				int index = modules.getIndex(newNode);
				theModel.nodesWereInserted(modules, new int[] { index });
			}
			//remove modules from the tree which are not in either inTree or notInTree
			ArrayList<DefaultMutableTreeNode> removeNodes = new ArrayList<DefaultMutableTreeNode>();
			for (int i = 0; i < modules.getChildCount(); i++) {
				ModuleNode mNode = (ModuleNode) modules.getChildAt(i);
				if (!mNode.isEditable() && !moduleExists(mNode, inTree, notInTree)) {
					removeNodes.add(mNode);
				}
			}
			Object[] remObj = new Object[removeNodes.size()];
			int[] remInd = new int[removeNodes.size()];
			for (int i = 0; i < removeNodes.size(); i++) {
				ModuleNode mNode = (ModuleNode) removeNodes.get(i);
				int index = modules.getIndex(mNode);
				remObj[i] = mNode;
				remInd[i] = index;
			}
			//remove nodes backwards
			for (int i = removeNodes.size() - 1; i >= 0; i--) {
				modules.remove(remInd[i]);
			}
			theModel.nodesWereRemoved(modules, remInd, remObj);

			//DECLARATIONS

			//If there is no declaration tree, but we need to have declarations, add the tre
			if (root.getIndex(declarations) == -1 && parsedModel.getNumGlobals() > 0) {
				root.add(declarations);
				theModel.nodesWereInserted(root, new int[] { root.getIndex(declarations) });
			}

			/*  Create 2 ArrayLists.  One of Declarations in the tree and one of declarations
			 *  not in the tree. */
			ArrayList<Declaration> decInTree = new ArrayList<Declaration>();
			ArrayList<Declaration> decNotInTree = new ArrayList<Declaration>();

			for (int i = 0; i < parsedModel.getNumGlobals(); i++) {
				Declaration d = parsedModel.getGlobal(i);
				if (declarationIsMember(d))
					decInTree.add(d);
				else
					decNotInTree.add(d);
			}

			//make sure declarations in this declaration tree are valid
			for (int i = 0; i < decInTree.size(); i++) {
				Declaration inTreeDec = decInTree.get(i);
				int decIndex = getIndexOfDec(inTreeDec);
				DeclarationNode dNode = (DeclarationNode) declarations.getChildAt(decIndex);
				if (dNode instanceof GlobalNode) {
					GlobalNode vNode = (GlobalNode) declarations.getChildAt(decIndex);
					vNode.setInitial(parsedModel.getSystemDefn() == null ? inTreeDec.getStartOrDefault() : null);
					if (inTreeDec.getDeclType() instanceof DeclarationInt) {
						DeclarationInt declInt = (DeclarationInt) inTreeDec.getDeclType();
						vNode.setMin(declInt.getLow());
						vNode.setMax(declInt.getHigh());
					}
					theModel.nodesChanged(vNode, new int[] { 0, 1, 2 });
				} else if (dNode instanceof GlobalBoolNode) {
					GlobalBoolNode bNode = (GlobalBoolNode) dNode;
					bNode.setInitial(parsedModel.getSystemDefn() == null ? inTreeDec.getStartOrDefault() : null);
					theModel.nodesChanged(bNode, new int[] { 0 });
				}

			}

			//add declarations in decNotInTree to the tree
			int[] cIndices = new int[decNotInTree.size()];
			for (int i = 0; i < decNotInTree.size(); i++) {
				Declaration notTreeDec = decNotInTree.get(i);
				if (notTreeDec.getType() instanceof TypeInt) {
					if (notTreeDec.getDeclType() instanceof DeclarationInt) {
						DeclarationInt declInt = (DeclarationInt) notTreeDec.getDeclType();
						GlobalNode newVariable = new GlobalNode(notTreeDec.getName(), parsedModel.getSystemDefn() == null ? notTreeDec.getStartOrDefault()
								: null, declInt.getLow(), declInt.getHigh(), false);
						declarations.add(newVariable);
					} else {
						GlobalNode newVariable = new GlobalNode(notTreeDec.getName(), parsedModel.getSystemDefn() == null ? notTreeDec.getStartOrDefault()
								: null, null, null, false);
						declarations.add(newVariable);
					}
					cIndices[i] = getIndexOfDec(notTreeDec);
				} else if (notTreeDec.getType() instanceof TypeBool) {
					GlobalBoolNode newVariable = new GlobalBoolNode(notTreeDec.getName(), parsedModel.getSystemDefn() == null ? notTreeDec.getStartOrDefault()
							: null, false);
					declarations.add(newVariable);
					cIndices[i] = getIndexOfDec(notTreeDec);
				}
			}
			theModel.nodesWereInserted(declarations, cIndices);

			/*  remove declarations which are in the tree but not in decInTree or
			 *  decNotInTree */
			removeNodes = new ArrayList<DefaultMutableTreeNode>();
			for (int i = 0; i < declarations.getChildCount(); i++) {
				DeclarationNode vNode = (DeclarationNode) declarations.getChildAt(i);
				if (!vNode.isEditable() && !declarationExists(vNode, decInTree, decNotInTree)) {
					removeNodes.add(vNode);
				}
			}
			remObj = new Object[removeNodes.size()];
			remInd = new int[removeNodes.size()];
			for (int i = 0; i < removeNodes.size(); i++) {
				DeclarationNode vNode = (DeclarationNode) removeNodes.get(i);
				int index = declarations.getIndex(vNode);
				remObj[i] = vNode;
				remInd[i] = index;
			}
			//remove nodes backwards
			for (int i = removeNodes.size() - 1; i >= 0; i--) {
				declarations.remove(remInd[i]);
			}
			theModel.nodesWereRemoved(declarations, remInd, remObj);

			//CONSTANTS

			ConstantList csts = parsedModel.getConstantList();
			//If there is no constant tree, but we need to have constant, add the tre
			if (root.getIndex(constants) == -1 && csts.size() > 0) {
				root.add(constants);
				theModel.nodesWereInserted(root, new int[] { root.getIndex(constants) });
			}

			/*  Create 2 ArrayLists.  One of Constants in the tree and one of constants
			 *  not in the tree. */
			ArrayList<ConstantNode> conInTree = new ArrayList<ConstantNode>();
			ArrayList<ConstantNode> conNotInTree = new ArrayList<ConstantNode>();

			for (int i = 0; i < csts.size(); i++) {
				String name = csts.getConstantName(i);
				Expression expr = csts.getConstant(i);
				Type type = csts.getConstantType(i);
				ConstantNode cn;
				if (type instanceof TypeBool) {
					cn = new BoolConstantNode(name, expr, false);
				} else if (type instanceof TypeDouble) {
					cn = new DoubleConstantNode(name, expr, false);
				} else {
					cn = new IntegerConstantNode(name, expr, false);
				}
				if (constantIsMember(cn))
					conInTree.add(cn);
				else
					conNotInTree.add(cn);
			}

			//make sure constants in this declaration tree are valid
			for (int i = 0; i < conInTree.size(); i++) {
				ConstantNode inTreeCon = conInTree.get(i);
				int conIndex = getIndexOfCon(inTreeCon);
				ConstantNode cNode = (ConstantNode) constants.getChildAt(conIndex);
				if (cNode instanceof IntegerConstantNode) {
					IntegerConstantNode iNode = (IntegerConstantNode) cNode;

					iNode.setName(inTreeCon.getName());
					iNode.setValue(inTreeCon.getValue());
					theModel.nodesChanged(iNode, new int[] { 0 });
				} else if (cNode instanceof BoolConstantNode) {
					BoolConstantNode bNode = (BoolConstantNode) cNode;

					bNode.setName(inTreeCon.getName());
					bNode.setValue(inTreeCon.getValue());
					theModel.nodesChanged(bNode, new int[] { 0 });
				} else if (cNode instanceof DoubleConstantNode) {
					DoubleConstantNode dNode = (DoubleConstantNode) cNode;

					dNode.setName(inTreeCon.getName());
					dNode.setValue(inTreeCon.getValue());
					theModel.nodesChanged(dNode, new int[] { 0 });
				}

			}

			//add declarations in decNotInTree to the tree
			cIndices = new int[conNotInTree.size()];
			for (int i = 0; i < conNotInTree.size(); i++) {
				ConstantNode notTreeCon = conNotInTree.get(i);
				constants.add(notTreeCon);
				cIndices[i] = getIndexOfCon(notTreeCon);
			}
			theModel.nodesWereInserted(constants, cIndices);

			/*  remove declarations which are in the tree but not in decInTree or
			 *  decNotInTree */
			removeNodes = new ArrayList<DefaultMutableTreeNode>();
			for (int i = 0; i < constants.getChildCount(); i++) {
				ConstantNode cNode = (ConstantNode) constants.getChildAt(i);
				if (!cNode.isEditable() && !constantExists(cNode, conInTree, conNotInTree)) {
					removeNodes.add(cNode);
				}
			}
			remObj = new Object[removeNodes.size()];
			remInd = new int[removeNodes.size()];
			for (int i = 0; i < removeNodes.size(); i++) {
				ConstantNode vNode = (ConstantNode) removeNodes.get(i);
				int index = constants.getIndex(vNode);
				remObj[i] = vNode;
				remInd[i] = index;
			}
			//remove nodes backwards
			for (int i = removeNodes.size() - 1; i >= 0; i--) {
				constants.remove(remInd[i]);
			}
			theModel.nodesWereRemoved(constants, remInd, remObj);

		} else {
			/*String tp = "<Unknown>";
			if(!modelType.getValue().equals(tp))
			{
			        modelType.setUserObject(tp);
			        theModel.nodeChanged(modelType);
			}*/
		}

		if (tree != null)
			tree.repaint();
	}

	public void lastParseFailed()
	{
		parseSynchState = TREE_SYNCHRONIZED_BAD;
		tree.repaint();
	}

	public void makeNotUpToDate()
	{
		parseSynchState = TREE_NOT_SYNCHRONIZED;
		tree.repaint();
	}

	private void initComponents()
	{
		tree = new JTree(theModel)
		{
			public String getToolTipText(MouseEvent event)
			{
				String ret = null;
				TreePath selectedPath = tree.getPathForLocation(event.getX(), event.getY());
				if (selectedPath != null) {
					//if(selectedPath.getLastPathComponent() instanceof ModelRootNode)
					if (parseSynchState == TREE_SYNCHRONIZED_BAD)
						return handler.getParseErrorMessage();
					else if (parseSynchState == TREE_SYNCHRONIZED_GOOD)
						return "Model parsed successfully";
					else if (!handler.isAutoParse())
						return "Auto-parsing disabled";
					else
						return "Model not parsed";
				}

				return ret;
			}

			public boolean isPathEditable(TreePath path)
			{
				Object c = path.getLastPathComponent();
				if (c instanceof ValueNode) {
					ValueNode v = (ValueNode) c;
					return v.isEditable();
				}
				return false;
			}

		};
		PrismNodeRenderer prnr = new PrismNodeRenderer();
		tree.setCellRenderer(prnr);
		/*JComboBox typeCombo = new JComboBox(new Object[]
		{"Probabilistic", "Non-deterministic", "Stochastic"});
		typeCombo.setFont(tree.getFont());
		typeCombo.setBackground(tree.getBackground());
		ValueNodeCellEditor cellEdit = new ValueNodeCellEditor(new JTextField(), typeCombo);
		tree.setCellEditor(new DefaultTreeCellEditor(tree, prnr, cellEdit));*/
		tree.setEditable(true);
		tree.setCellEditor(new GUIMultiModelTree.ModelTreeCellEditor(tree, prnr));

		setLayout(new BorderLayout());
		JScrollPane scr = new JScrollPane();
		scr.setViewportView(tree);
		add(scr, BorderLayout.CENTER);

		moduleCollectionPopup = new JPopupMenu();
		addModule = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_addModule();
			}
		};
		addModule.putValue(Action.LONG_DESCRIPTION, "Adds an editable module to the tree");
		addModule.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
		addModule.putValue(Action.NAME, "Add Module");
		moduleCollectionPopup.add(addModule);

		declarationCollectionPopup = new JPopupMenu();
		addIntegerGlobal = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_addIntegerGlobal();
			}
		};
		addIntegerGlobal.putValue(Action.NAME, "Add Global Integer");
		addBooleanGlobal = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_addBooleanGlobal();
			}
		};
		addBooleanGlobal.putValue(Action.NAME, "Add Global Boolean");
		declarationCollectionPopup.add(addIntegerGlobal);
		declarationCollectionPopup.add(addBooleanGlobal);

		constantsCollectionPopup = new JPopupMenu();
		addIntegerConstant = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_addIntegerConstant();
			}
		};
		addIntegerConstant.putValue(Action.NAME, "Add Integer Constant");
		addBooleanConstant = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_addBooleanConstant();
			}
		};
		addBooleanConstant.putValue(Action.NAME, "Add Boolean Constant");
		addDoubleConstant = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_addDoubleConstant();
			}
		};
		addDoubleConstant.putValue(Action.NAME, "Add Double Constant");
		constantsCollectionPopup.add(addIntegerConstant);
		constantsCollectionPopup.add(addBooleanConstant);
		constantsCollectionPopup.add(addDoubleConstant);

		modulePopup = new JPopupMenu();
		removeModule = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (lastPopNode != null && lastPopNode instanceof ModuleNode) {
					a_removeModule((ModuleNode) lastPopNode);
					lastPopNode = null;
				}
			}
		};
		removeModule.putValue(Action.NAME, "Remove");
		addBoolean = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (lastPopNode != null && lastPopNode instanceof ModuleNode) {
					a_addLocalBoolean((ModuleNode) lastPopNode);
					lastPopNode = null;
				}

			}
		};
		addBoolean.putValue(Action.NAME, "Add Boolean Variable");
		addInteger = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (lastPopNode != null && lastPopNode instanceof ModuleNode) {
					a_addLocalInteger((ModuleNode) lastPopNode);
					lastPopNode = null;
				}
			}
		};
		addInteger.putValue(Action.NAME, "Add Integer Variable");
		renameModule = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (lastPopNode != null && lastPopNode instanceof ModuleNode) {
					a_renameModule((ModuleNode) lastPopNode);
					lastPopNode = null;
				}
			}
		};
		renameModule.putValue(Action.NAME, "Rename");
		modulePopup.add(renameModule);
		modulePopup.add(removeModule);
		//modulePopup.add(new JSplitter());
		modulePopup.add(addInteger);
		modulePopup.add(addBoolean);

		declarationPopup = new JPopupMenu();
		removeDeclaration = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (lastPopNode != null) {
					if (lastPopNode instanceof BoolNode || lastPopNode instanceof VarNode) {
						ModuleNode m = whichModule((DeclarationNode) lastPopNode);
						if (m != null)
							a_removeVariable((DeclarationNode) lastPopNode, m);
					} else if (lastPopNode instanceof GlobalNode || lastPopNode instanceof GlobalBoolNode) {
						a_removeGlobal((DeclarationNode) lastPopNode);
					} else if (lastPopNode instanceof ConstantNode) {
						a_removeConstant((ConstantNode) lastPopNode);
					}
				}
			}
		};
		removeDeclaration.putValue(Action.NAME, "Remove");
		declarationPopup.add(removeDeclaration);

		renameDeclaration = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (lastPopNode != null) {
					a_renameDeclaration((DeclarationNode) lastPopNode);
				}
			}
		};
		renameDeclaration.putValue(Action.NAME, "Rename");
		declarationPopup.add(renameDeclaration);

		expressionPopup = new JPopupMenu();

		editExpression = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (lastPopNode != null) {
					a_editExpression((ExpressionNode) lastPopNode);
				}
			}
		};
		editExpression.putValue(Action.NAME, "Edit");
		expressionPopup.add(editExpression);

		modelTypePopup = new JPopupMenu();

		non = new JRadioButton("Non-deterministic (mdp)");
		pro = new JRadioButton("Probabilistic (dtmc)");
		sto = new JRadioButton("Stochastic (ctmc)");

		ButtonGroup gro = new ButtonGroup();
		gro.add(non);
		gro.add(pro);
		gro.add(sto);

		non.setSelected(true);

		modelTypePopup.add(non);
		modelTypePopup.add(pro);
		modelTypePopup.add(sto);

		non.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (non.isSelected())
					a_setModelType(ModelType.MDP);
				modelTypePopup.setVisible(false);
			}
		});

		pro.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (pro.isSelected())
					a_setModelType(ModelType.DTMC);
				modelTypePopup.setVisible(false);
			}
		});

		sto.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (sto.isSelected())
					a_setModelType(ModelType.CTMC);
				modelTypePopup.setVisible(false);
			}
		});
	}

	public ModuleNode whichModule(DeclarationNode isIt)
	{
		for (int i = 0; i < editableModules.size(); i++) {
			ModuleNode mod = editableModules.get(i);
			int index = mod.getIndex(isIt);
			if (index > -1) {
				return mod;
			}
		}
		return null;
	}

	public void mouseClicked(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
		if (e.isPopupTrigger()) {
			lastPopNode = null;
			//root popup
			if (tree.getClosestPathForLocation(e.getX(), e.getY()).equals(new TreePath(root.getPath()))) {
				tree.setSelectionPath(new TreePath(root.getPath()));
				//now the root is selected, show the popup
				//(diabled for now)
				//((GUIMultiModel)(handler.getGUIPlugin())).getPopup().show(tree, e.getX(), e.getY());
			}
			//model type node
			if (editable && tree.getClosestPathForLocation(e.getX(), e.getY()).equals(new TreePath(modelType.getPath()))) {
				tree.setSelectionPath(new TreePath(modelType.getPath()));
				modelTypePopup.show(tree, e.getX(), e.getY());
			}
			//module collection popup
			else if (editable && modules != null && tree.getClosestPathForLocation(e.getX(), e.getY()).equals(new TreePath(modules.getPath()))) {
				tree.setSelectionPath(new TreePath(modules.getPath()));
				moduleCollectionPopup.show(tree, e.getX(), e.getY());
			}
			//declaration collection popup
			else if (editable && declarations != null && tree.getClosestPathForLocation(e.getX(), e.getY()).equals(new TreePath(declarations.getPath()))) {
				tree.setSelectionPath(new TreePath(declarations.getPath()));
				declarationCollectionPopup.show(tree, e.getX(), e.getY());
			} else if (editable && constants != null && tree.getClosestPathForLocation(e.getX(), e.getY()).equals(new TreePath(constants.getPath())))//constant collection node
			{
				tree.setSelectionPath(new TreePath(constants.getPath()));
				constantsCollectionPopup.show(tree, e.getX(), e.getY());
			} else // is the path on a ModuleNode or a DeclarationNode or an ExpressionNode
			{
				TreePath selectedPath = tree.getClosestPathForLocation(e.getX(), e.getY());
				for (int i = 0; i < editableModules.size(); i++) //search each module
				{
					ModuleNode mn = editableModules.get(i);
					if (new TreePath(mn.getPath()).equals(selectedPath)) {
						tree.setSelectionPath(selectedPath);
						if (mn.isEditable()) {
							modulePopup.show(tree, e.getX(), e.getY());
							lastPopNode = mn;
						}
						return;
					}
					for (int j = 0; j < mn.getChildCount(); j++)//search local variables
					{
						DeclarationNode dn = (DeclarationNode) mn.getChildAt(j);
						if (new TreePath(dn.getPath()).equals(selectedPath)) {
							tree.setSelectionPath(selectedPath);
							if (dn.isEditable()) {
								declarationPopup.show(tree, e.getX(), e.getY());
								lastPopNode = dn;
							}
							return;
						}
					}
				}
				for (int i = 0; i < editableDeclarations.size(); i++)//search globals
				{
					DeclarationNode dn = editableDeclarations.get(i);
					if (new TreePath(dn.getPath()).equals(selectedPath)) {
						tree.setSelectionPath(selectedPath);
						if (dn.isEditable()) {
							declarationPopup.show(tree, e.getX(), e.getY());
							lastPopNode = dn;
						}
						return;
					}
				}
				for (int i = 0; i < editableConstants.size(); i++)//search constants
				{
					DeclarationNode dn = editableConstants.get(i);
					if (new TreePath(dn.getPath()).equals(selectedPath)) {
						tree.setSelectionPath(selectedPath);
						if (dn.isEditable()) {
							declarationPopup.show(tree, e.getX(), e.getY());
							lastPopNode = dn;
						}
						return;
					}
				}

				//Could be an ExpressionNode
				if (tree.getSelectionPath() != null) {
					TreeNode selectedNode = (TreeNode) tree.getSelectionPath().getLastPathComponent();
					if (selectedNode instanceof ExpressionNode) {
						ExpressionNode en = (ExpressionNode) selectedNode;
						if (en.isEditable()) {
							expressionPopup.show(tree, e.getX(), e.getY());
							lastPopNode = en;
						}
					}
				}

			}

		} else {
			TreePath selectedPath = tree.getClosestPathForLocation(e.getX(), e.getY());
			for (int i = 0; i < editableModules.size(); i++) //search each module
			{
				ModuleNode mn = editableModules.get(i);
				if (new TreePath(mn.getPath()).equals(selectedPath)) {
					tree.setSelectionPath(selectedPath);
					return;
				}
			}
		}

	}

	public void mouseReleased(MouseEvent e)
	{
		if (e.isPopupTrigger()) {
			lastPopNode = null;
			//root popup
			if (tree.getClosestPathForLocation(e.getX(), e.getY()).equals(new TreePath(root.getPath()))) {
				tree.setSelectionPath(new TreePath(root.getPath()));
				//now the root is selected, show the popup
				//(diabled for now)
				//((GUIMultiModel)(handler.getGUIPlugin())).getPopup().show(tree, e.getX(), e.getY());
			}
			//model type node
			if (editable && tree.getClosestPathForLocation(e.getX(), e.getY()).equals(new TreePath(modelType.getPath()))) {
				tree.setSelectionPath(new TreePath(modelType.getPath()));
				modelTypePopup.show(tree, e.getX(), e.getY());
			}
			//module collection popup
			else if (editable && modules != null && tree.getClosestPathForLocation(e.getX(), e.getY()).equals(new TreePath(modules.getPath()))) {
				tree.setSelectionPath(new TreePath(modules.getPath()));
				moduleCollectionPopup.show(tree, e.getX(), e.getY());
			}
			//declaration collection popup
			else if (editable && declarations != null && tree.getClosestPathForLocation(e.getX(), e.getY()).equals(new TreePath(declarations.getPath()))) {
				tree.setSelectionPath(new TreePath(declarations.getPath()));
				declarationCollectionPopup.show(tree, e.getX(), e.getY());
			} else if (editable && constants != null && tree.getClosestPathForLocation(e.getX(), e.getY()).equals(new TreePath(constants.getPath())))//constant collection node
			{
				tree.setSelectionPath(new TreePath(constants.getPath()));
				constantsCollectionPopup.show(tree, e.getX(), e.getY());
			} else // is the path on a ModuleNode or a DeclarationNode or an ExpressionNode
			{
				TreePath selectedPath = tree.getClosestPathForLocation(e.getX(), e.getY());
				for (int i = 0; i < editableModules.size(); i++) //search each module
				{
					ModuleNode mn = editableModules.get(i);
					if (new TreePath(mn.getPath()).equals(selectedPath)) {
						tree.setSelectionPath(selectedPath);
						if (mn.isEditable()) {
							modulePopup.show(tree, e.getX(), e.getY());
							lastPopNode = mn;
						}
						return;
					}
					for (int j = 0; j < mn.getChildCount(); j++)//search local variables
					{
						DeclarationNode dn = (DeclarationNode) mn.getChildAt(j);
						if (new TreePath(dn.getPath()).equals(selectedPath)) {
							tree.setSelectionPath(selectedPath);
							if (dn.isEditable()) {
								declarationPopup.show(tree, e.getX(), e.getY());
								lastPopNode = dn;
							}
							return;
						}
					}
				}
				for (int i = 0; i < editableDeclarations.size(); i++)//search globals
				{
					DeclarationNode dn = editableDeclarations.get(i);
					if (new TreePath(dn.getPath()).equals(selectedPath)) {
						tree.setSelectionPath(selectedPath);
						if (dn.isEditable()) {
							declarationPopup.show(tree, e.getX(), e.getY());
							lastPopNode = dn;
						}
						return;
					}
				}
				for (int i = 0; i < editableConstants.size(); i++)//search constants
				{
					DeclarationNode dn = editableConstants.get(i);
					if (new TreePath(dn.getPath()).equals(selectedPath)) {
						tree.setSelectionPath(selectedPath);
						if (dn.isEditable()) {
							declarationPopup.show(tree, e.getX(), e.getY());
							lastPopNode = dn;
						}
						return;
					}
				}

				//Could be an ExpressionNode
				if (tree.getSelectionPath() != null) {
					TreeNode selectedNode = (TreeNode) tree.getSelectionPath().getLastPathComponent();
					if (selectedNode instanceof ExpressionNode) {
						ExpressionNode en = (ExpressionNode) selectedNode;
						if (en.isEditable()) {
							expressionPopup.show(tree, e.getX(), e.getY());
							lastPopNode = en;
						}
					}
				}

			}

		} else {
			TreePath selectedPath = tree.getClosestPathForLocation(e.getX(), e.getY());
			for (int i = 0; i < editableModules.size(); i++) //search each module
			{
				ModuleNode mn = editableModules.get(i);
				if (new TreePath(mn.getPath()).equals(selectedPath)) {
					tree.setSelectionPath(selectedPath);
					return;
				}
			}
		}

	}

	public String getToolTipText()
	{
		return handler.getParseErrorMessage();
	}

	public ModelType getModelType()
	{
		return ModelType.valueOf(modelType.getUserObject().toString());
	}

	public static final String TAB = "	";

	public ArrayList<String> getEditableConstantNames()
	{
		ArrayList<String> names = new ArrayList<String>();
		for (int i = 0; i < editableConstants.size(); i++) {
			DeclarationNode dn = editableConstants.get(i);
			names.add(dn.getName());
		}
		return names;
	}

	public ArrayList<String> getEditableConstantValues()
	{
		ArrayList<String> values = new ArrayList<String>();
		for (int i = 0; i < editableConstants.size(); i++) {
			ConstantNode cn = editableConstants.get(i);
			if (cn.getValue() != null) {
				if (!cn.getValue().toString().equals("")) {
					values.add(cn.getValue().toString());
				} else
					values.add(null);
			} else
				values.add(null);
		}
		return values;
	}

	public ArrayList<Type> getEditableConstantTypes()
	{
		ArrayList<Type> types = new ArrayList<Type>();
		for (int i = 0; i < editableConstants.size(); i++) {
			Object node = editableConstants.get(i);
			if (node instanceof BoolConstantNode) {
				types.add(TypeBool.getInstance());
			} else if (node instanceof IntegerConstantNode) {
				types.add(TypeInt.getInstance());
			} else if (node instanceof DoubleConstantNode) {
				types.add(TypeDouble.getInstance());
			} else
				types.add(null);
		}
		return types;
	}

	public ArrayList<String> getEditableGlobalNames()
	{
		ArrayList<String> names = new ArrayList<String>();
		for (int i = 0; i < editableDeclarations.size(); i++) {
			DeclarationNode dn = editableDeclarations.get(i);
			names.add(dn.getName());
		}
		return names;
	}

	public ArrayList<String> getEditableGlobalMins()
	{
		ArrayList<String> mins = new ArrayList<String>();
		for (int i = 0; i < editableDeclarations.size(); i++) {
			Object node = editableDeclarations.get(i);
			if (node instanceof GlobalNode) {
				GlobalNode gn = (GlobalNode) node;
				mins.add(gn.getMin().toString());
			} else
				mins.add(null);
		}
		return mins;
	}

	public ArrayList<String> getEditableGlobalMaxs()
	{
		ArrayList<String> maxs = new ArrayList<String>();
		for (int i = 0; i < editableDeclarations.size(); i++) {
			Object node = editableDeclarations.get(i);
			if (node instanceof GlobalNode) {
				GlobalNode gn = (GlobalNode) node;
				maxs.add(gn.getMax().toString());
			} else
				maxs.add(null);

		}
		return maxs;
	}

	public ArrayList<String> getEditableGlobalInits()
	{
		ArrayList<String> inits = new ArrayList<String>();
		for (int i = 0; i < editableDeclarations.size(); i++) {
			Object node = editableDeclarations.get(i);
			if (node instanceof GlobalNode) {
				GlobalNode gn = (GlobalNode) node;
				inits.add(gn.getInitial().toString());
			} else if (node instanceof GlobalBoolNode) {
				GlobalBoolNode gbn = (GlobalBoolNode) node;
				inits.add(gbn.getInitial().toString());
			}
		}
		return inits;
	}

	public ArrayList<Type> getEditableGlobalTypes()
	{
		ArrayList<Type> types = new ArrayList<Type>();
		for (int i = 0; i < editableDeclarations.size(); i++) {
			Object node = editableDeclarations.get(i);
			if (node instanceof GlobalBoolNode) {
				types.add(TypeBool.getInstance());
			} else if (node instanceof GlobalNode) {
				types.add(TypeInt.getInstance());
			} else
				types.add(null);
		}
		return types;
	}

	public ArrayList<String> getVariableNames(ModuleNode m)
	{
		ArrayList<String> names = new ArrayList<String>();
		for (int i = 0; i < m.getChildCount(); i++) {
			DeclarationNode vn = (DeclarationNode) m.getChildAt(i);
			if (!(vn instanceof StateVarNode)) {
				names.add(vn.getName());
			}
		}
		return names;
	}

	public ArrayList<Type> getVariableTypes(ModuleNode m)
	{
		ArrayList<Type> types = new ArrayList<Type>();
		for (int i = 0; i < m.getChildCount(); i++) {
			DeclarationNode dn = (DeclarationNode) m.getChildAt(i);
			if (!(dn instanceof StateVarNode)) {
				if (dn instanceof BoolNode)
					types.add(TypeBool.getInstance());
				else if (dn instanceof VarNode)
					types.add(TypeInt.getInstance());
				else
					types.add(null);
			}
		}
		return types;
	}

	public ArrayList<String> getVariableMins(ModuleNode m)
	{
		ArrayList<String> mins = new ArrayList<String>();
		for (int i = 0; i < m.getChildCount(); i++) {
			DeclarationNode dn = (DeclarationNode) m.getChildAt(i);
			if (!(dn instanceof StateVarNode)) {
				if (dn instanceof BoolNode)
					mins.add(null);
				else if (dn instanceof VarNode) {
					VarNode vn = (VarNode) dn;
					String s = vn.getMin().toString();
					mins.add(s);
				} else
					mins.add(null);
			}
		}
		return mins;
	}

	public ArrayList<String> getVariableMaxs(ModuleNode m)
	{
		ArrayList<String> maxs = new ArrayList<String>();
		for (int i = 0; i < m.getChildCount(); i++) {
			DeclarationNode dn = (DeclarationNode) m.getChildAt(i);
			if (!(dn instanceof StateVarNode)) {
				if (dn instanceof BoolNode)
					maxs.add(null);
				else if (dn instanceof VarNode) {
					VarNode vn = (VarNode) dn;
					String s = vn.getMax().toString();
					maxs.add(s);
				} else
					maxs.add(null);
			}
		}
		return maxs;
	}

	public ArrayList<String> getVariableInits(ModuleNode m)
	{
		ArrayList<String> inits = new ArrayList<String>();
		for (int i = 0; i < m.getChildCount(); i++) {
			DeclarationNode dn = (DeclarationNode) m.getChildAt(i);
			if (!(dn instanceof StateVarNode)) {
				if (dn instanceof BoolNode) {
					BoolNode bn = (BoolNode) dn;
					String s = bn.getInitial().toString();
					inits.add(s);
				} else if (dn instanceof VarNode) {
					VarNode vn = (VarNode) dn;
					String s = vn.getInitial().toString();
					inits.add(s);
				} else
					inits.add(null);
			}
		}
		return inits;
	}

	public void updateTooltip()
	{
		tree.setToolTipText(handler.getParseErrorMessage());
	}

	//Node Data Structure Classes

	interface PrismTreeNode
	{
		public boolean isEditable();
	}

	class ModelRootNode extends DefaultMutableTreeNode implements PrismTreeNode
	{
		public ModelRootNode()
		{
			super("", true);
		}

		public boolean isEditable()
		{
			return false;
		}

	}

	class ModuleCollectionNode extends DefaultMutableTreeNode implements PrismTreeNode
	{
		public ModuleCollectionNode()
		{
			super("Modules", true);
			super.setAllowsChildren(true);
		}

		public void addModule(ModuleNode mod)
		{
			add(mod);
		}

		public void removeModule(ModuleNode mod)
		{
			remove(mod);
		}

		public boolean isEditable()
		{
			return editable;
		}

	}

	class DeclarationCollectionNode extends DefaultMutableTreeNode implements PrismTreeNode
	{
		public DeclarationCollectionNode()
		{
			super("Global Variables", true);
			super.setAllowsChildren(true);
		}

		public void addDeclaration(DeclarationNode dec)
		{
			add(dec);
		}

		public void removeDeclaration(DeclarationNode dec)
		{
			remove(dec);
		}

		public boolean isEditable()
		{
			return editable;
		}

	}

	class ConstantCollectionNode extends DefaultMutableTreeNode implements PrismTreeNode
	{
		public ConstantCollectionNode()
		{
			super("Constants", true);
		}

		public void addConstant(ConstantNode n)
		{
			add(n);
		}

		public void removeConstant(ConstantNode n)
		{
			remove(n);
		}

		public boolean isEditable()
		{
			return editable;
		}
	}

	public class ModuleNode extends DefaultMutableTreeNode implements PrismTreeNode
	{
		private boolean editable;

		public ModuleNode(String moduleName, boolean editable)
		{
			super(moduleName, true);
			this.editable = editable;
		}

		public void addVariable(VarNode var)
		{
			add(var);
		}

		public void removeVariable(VarNode var)
		{
			remove(var);
		}

		public void setName(String str)
		{
			setUserObject(str);
		}

		public String getName()
		{
			return (String) getUserObject();
		}

		public boolean isEditable()
		{
			return editable;
		}

		public void setEditable(boolean b)
		{
			editable = b;
		}

		public void childrenChanged()
		{
			theModel.nodeChanged(this);
			theModel.nodeStructureChanged(this);
		}

		public boolean isLeaf()
		{
			return false;
		}

	}

	//Editor for ModuleNode
	class ModuleEditor extends JTextField implements TreeCellEditor
	{
		String name;
		Vector<CellEditorListener> listeners = new Vector<CellEditorListener>();
		private static final int minWidth = 64;

		public ModuleEditor()
		{
			super("");
			addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent as)
				{
					if (stopCellEditing())
						fireEditingStopped();
				}
			});
		}

		public void cancelCellEditing()
		{
			setText("");
		}

		public boolean stopCellEditing()
		{
			Expression exp;
			String str = getText();
			try {
				exp = Prism.parseSingleExpressionString(str);
			} catch (PrismException e) {
				return false;
			}
			if (exp instanceof ExpressionIdent) {
				name = str;
				return true;
			} else {
				return false;
			}
		}

		public Object getCellEditorValue()
		{
			return name;
		}

		public boolean isCellEditable(EventObject eo)
		{
			if ((eo == null) | ((eo instanceof MouseEvent) && (((MouseEvent) eo).isMetaDown()))) {
				return true;
			}
			return false;
		}

		public boolean shouldSelectCell(EventObject eo)
		{
			return true;
		}

		public void removeCellEditroLIstner(CellEditorListener cel)
		{
			listeners.removeElement(cel);
		}

		protected void fireEditingStopped()
		{
			if (listeners.size() > 0) {
				ChangeEvent ce = new ChangeEvent(this);
				for (int i = listeners.size() - 1; i >= 0; i--) {
					listeners.elementAt(i).editingStopped(ce);
				}

			}
		}

		//make sure the JTree gives the editor enough space
		public void setBounds(Rectangle r)
		{
			r.width = Math.max(minWidth, r.width);
			super.setBounds(r);
		}

		public void setBounds(int x, int y, int w, int h)
		{
			w = Math.max(minWidth, w);
			super.setBounds(x, y, w, h);
		}

		public void addCellEditorListener(CellEditorListener l)
		{
			listeners.addElement(l);
		}

		public void removeCellEditorListener(CellEditorListener l)
		{
			listeners.removeElement(l);
		}

		public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row)
		{
			return this;
		}

		//same listener and bounds methods as above
	}

	public static final int LOCAL_INTEGER = 0;
	public static final int LOCAL_BOOL = 1;
	public static final int GLOBAL_INTEGER = 2;
	public static final int GLOBAL_BOOL = 3;
	public static final int CONST_INTEGER = 4;
	public static final int CONST_BOOL = 5;
	public static final int CONST_DOUBLE = 6;

	abstract class DeclarationNode extends DefaultMutableTreeNode implements PrismTreeNode
	{
		private boolean editable;
		private int type;

		public DeclarationNode(int type, String name, boolean editable)
		{
			super(name, true);
			this.editable = editable;
			this.type = type;
		}

		public boolean getAllowsChildren()
		{
			return true;
		}

		public boolean isLeaf()
		{
			return false;
		}

		public boolean isEditable()
		{
			return editable;
		}

		public String getName()
		{
			return (String) getUserObject();
		}

		public void setName(String str)
		{
			setUserObject(str);
		}

		public String toString()
		{
			String name = getName();
			switch (type) {
			case LOCAL_INTEGER:
				return name;
			case GLOBAL_INTEGER:
			case CONST_INTEGER:
				return name + " : int";
			case LOCAL_BOOL:
			case GLOBAL_BOOL:
			case CONST_BOOL:
				return name + " : bool";
			case CONST_DOUBLE:
				return name + " : double";
			default:
				return "";
			}
		}

		public abstract String getParseText();
	}

	//Editor for DeclarationNode
	class DeclarationEditor extends JTextField implements TreeCellEditor
	{
		String name;
		Vector<CellEditorListener> listeners = new Vector<CellEditorListener>();
		private static final int minWidth = 64;

		public DeclarationEditor()
		{
			super("");
			addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent as)
				{
					if (stopCellEditing())
						fireEditingStopped();
				}
			});
		}

		public void cancelCellEditing()
		{
			setText("");
		}

		public boolean stopCellEditing()
		{
			Expression exp;
			String str = getText();
			try {
				exp = Prism.parseSingleExpressionString(str);
			} catch (PrismException e) {
				return false;
			}
			if (exp instanceof ExpressionIdent) {
				name = str;
				return true;
			} else {
				return false;
			}
		}

		public Object getCellEditorValue()
		{
			return name;
		}

		public boolean isCellEditable(EventObject eo)
		{
			if ((eo == null) | ((eo instanceof MouseEvent) && (((MouseEvent) eo).isMetaDown()))) {
				return true;
			}
			return false;
		}

		public boolean shouldSelectCell(EventObject eo)
		{
			return true;
		}

		protected void fireEditingStopped()
		{
			if (listeners.size() > 0) {
				ChangeEvent ce = new ChangeEvent(this);
				for (int i = listeners.size() - 1; i >= 0; i--) {
					listeners.elementAt(i).editingStopped(ce);
				}

			}
		}

		//make sure the JTree gives the editor enough space
		public void setBounds(Rectangle r)
		{
			r.width = Math.max(minWidth, r.width);
			super.setBounds(r);
		}

		public void setBounds(int x, int y, int w, int h)
		{
			w = Math.max(minWidth, w);
			super.setBounds(x, y, w, h);
		}

		public void addCellEditorListener(CellEditorListener l)
		{
			listeners.addElement(l);
		}

		public void removeCellEditorListener(CellEditorListener l)
		{
			listeners.addElement(l);
		}

		public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row)
		{
			return this;
		}

	}

	class VarNode extends DeclarationNode
	{

		public VarNode(String name, Expression init, Expression min, Expression max, boolean editable)
		{
			super(LOCAL_INTEGER, name, editable);
			super.add(new ExpressionNode("min: ", min, editable));
			super.add(new ExpressionNode("max: ", max, editable));
			super.add(new ExpressionNode("init: ", init, editable));
		}

		public VarNode(String name, String init, String min, String max, boolean editable)
		{
			super(LOCAL_INTEGER, name, editable);
			try {
				Expression e_init = Prism.parseSingleExpressionString(init);
				Expression e_min = Prism.parseSingleExpressionString(min);
				Expression e_max = Prism.parseSingleExpressionString(max);
				super.add(new ExpressionNode("min: ", e_min, editable));
				super.add(new ExpressionNode("max: ", e_max, editable));
				super.add(new ExpressionNode("init: ", e_init, editable));
			} catch (Exception e) {
			}
		}

		public void setInitial(Expression e)
		{
			ExpressionNode n = (ExpressionNode) getChildAt(2);
			n.setUserObject(e);
			theModel.nodeChanged(getChildAt(2));
		}

		public Expression getInitial()
		{
			ExpressionNode n = (ExpressionNode) getChildAt(2);
			return n.getValue();
		}

		public void setMin(Expression e)
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			n.setUserObject(e);
			theModel.nodeChanged(getChildAt(0));
		}

		public Expression getMin()
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			return n.getValue();
		}

		public void setMax(Expression e)
		{
			ExpressionNode n = (ExpressionNode) getChildAt(1);
			n.setUserObject(e);
			theModel.nodeChanged(getChildAt(1));
		}

		public Expression getMax()
		{
			ExpressionNode n = (ExpressionNode) getChildAt(1);
			return n.getValue();
		}

		public String getParseText()
		{
			String str = getName() + " : [" + getMin() + ".." + getMax() + "] init " + getInitial() + ";";
			return str;
		}

	}

	public class StateVarNode extends VarNode
	{
		ModuleNode mn;

		public StateVarNode(ModuleNode mn)
		{
			super("statevariable", "0", "0", "0", false);
			this.mn = mn;
		}

		public String getName()
		{
			return mn.getName() + "_s";
		}
	}

	class BoolNode extends DeclarationNode
	{
		public BoolNode(String name, Expression init, boolean editable)
		{
			super(LOCAL_BOOL, name, editable);
			super.add(new ExpressionNode("init: ", init, editable));
		}

		public void setInitial(Expression e)
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			n.setUserObject(e);
		}

		public Expression getInitial()
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			return n.getValue();
		}

		public String getParseText()
		{
			String str = getName() + " : bool init " + getInitial() + ";";
			return str;
		}

	}

	class GlobalNode extends DeclarationNode
	{

		public GlobalNode(String name, Expression init, Expression min, Expression max, boolean editable)
		{
			super(GLOBAL_INTEGER, name, editable);
			super.add(new ExpressionNode("min: ", min, editable));
			super.add(new ExpressionNode("max: ", max, editable));
			super.add(new ExpressionNode("init: ", init, editable));
		}

		public void setInitial(Expression e)
		{
			ExpressionNode n = (ExpressionNode) getChildAt(2);
			n.setUserObject(e);
		}

		public Expression getInitial()
		{
			ExpressionNode n = (ExpressionNode) getChildAt(2);
			return n.getValue();
		}

		public void setMin(Expression e)
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			n.setUserObject(e);
		}

		public Expression getMin()
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			return n.getValue();
		}

		public void setMax(Expression e)
		{
			ExpressionNode n = (ExpressionNode) getChildAt(1);
			n.setUserObject(e);
		}

		public Expression getMax()
		{
			ExpressionNode n = (ExpressionNode) getChildAt(1);
			return n.getValue();
		}

		public String getParseText()
		{
			String str = "global " + getName() + " : [" + getMin() + ".." + getMax() + "] init " + getInitial() + ";";
			return str;
		}

	}

	class GlobalBoolNode extends DeclarationNode
	{
		public GlobalBoolNode(String name, Expression init, boolean editable)
		{
			super(GLOBAL_BOOL, name, editable);
			super.add(new ExpressionNode("init: ", init, editable));
		}

		public void setInitial(Expression e)
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			n.setUserObject(e);
		}

		public Expression getInitial()
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			return n.getValue();
		}

		public String getParseText()
		{
			String str = "global " + getName() + " : bool init " + getInitial() + ";";
			return str;
		}

	}

	abstract class ConstantNode extends DeclarationNode
	{
		public ConstantNode(int type, String name, boolean editable)
		{
			super(type, name, editable);
		}

		public abstract void setValue(Expression e);

		public abstract Expression getValue();

		public boolean equals(Object obj)
		{
			if (obj instanceof ConstantNode) {
				ConstantNode other = (ConstantNode) obj;
				if (!other.getName().equals(getName()))
					return false;
				else
					return true; //can only compare the equality of the names
			} else
				return false;
		}

		public abstract String getParseText();

	}

	class IntegerConstantNode extends ConstantNode
	{
		public IntegerConstantNode(String name, Expression value, boolean editable)
		{
			super(CONST_INTEGER, name, editable);
			super.add(new ExpressionNode("value: ", value, editable));
		}

		public void setValue(Expression e)
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			n.setUserObject(e);
		}

		public Expression getValue()
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			return n.getValue();
		}

		public String getParseText()
		{
			String str = "const int " + getName();
			if (getValue() != null) {
				str += " = " + getValue() + ";";
			} else {
				str += ";";
			}
			return str;
		}

	}

	class DoubleConstantNode extends ConstantNode
	{
		public DoubleConstantNode(String name, Expression value, boolean editable)
		{
			super(CONST_DOUBLE, name, editable);
			super.add(new ExpressionNode("value: ", value, editable));
		}

		public void setValue(Expression e)
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			n.setUserObject(e);
		}

		public Expression getValue()
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			return n.getValue();
		}

		public String getParseText()
		{
			String str = "const double " + getName();
			if (getValue() != null) {
				str += " = " + getValue() + ";";
			} else {
				str += ";";
			}
			return str;
		}

	}

	class BoolConstantNode extends ConstantNode
	{
		public BoolConstantNode(String name, Expression value, boolean editable)
		{
			super(CONST_BOOL, name, editable);
			super.add(new ExpressionNode("value: ", value, editable));
		}

		public void setValue(Expression e)
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			n.setUserObject(e);
		}

		public Expression getValue()
		{
			ExpressionNode n = (ExpressionNode) getChildAt(0);
			return n.getValue();
		}

		public String getParseText()
		{
			String str = "const bool " + getName();
			if (getValue() != null) {
				str += " = " + getValue() + ";";
			} else {
				str += ";";
			}
			return str;
		}

	}

	abstract class ValueNode extends DefaultMutableTreeNode implements PrismTreeNode
	{
		private String tag;

		public ValueNode(String tag, Object value)
		{
			super(value);
			this.tag = tag;
		}

		public String toString()
		{
			if (getUserObject() == null)
				return tag + "?";
			else
				return tag + getUserObject().toString();
		}

		public abstract boolean isEditable();

		public boolean isLeaf()
		{
			return true;
		}

		public void setUserObject(Object obj)
		{
			super.setUserObject(obj);
		}
	}

	class StringNode extends ValueNode
	{
		private boolean editable;

		public StringNode(String tag, String value, boolean editable)
		{
			super(tag, value);
			this.editable = editable;
		}

		public boolean isEditable()
		{
			return editable;
		}

		public String getValue()
		{
			return (String) getUserObject();
		}

	}

	class ExpressionNode extends ValueNode
	{
		private boolean editable;

		public ExpressionNode(String tag, Expression value, boolean editable)
		{
			super(tag, value);
			this.editable = editable;
		}

		public boolean isEditable()
		{
			return editable;
		}

		public Expression getValue()
		{
			return (Expression) getUserObject();
		}

		public void setValue(Expression value)
		{
			super.setUserObject(value);
		}

	}

	/*
	class ExpressionEditor extends javax.swing.DefaultCellEditor
	{
	        Expression exp;
	        int minWidth = 100;
	 
	        public ExpressionEditor()
	        {
	                super(new JTextField());
	        }
	 
	        public Component getComponent()
	        {
	                return editorComponent;
	        }
	 
	        public boolean stopCellEditing()
	        {
	                String str = ((JTextField)editorComponent).getText();
	 
	                try
	                {
	                        Expression s = handler.getGUIPlugin().getPrism().parseSingleExpressionString(str);
	                        exp = s;
	                }
	                catch(Exception e)
	                {
	                        handler.getGUIPlugin().message("Error: Syntax Error");
	                        super.fireEditingStopped();
	                        return true;
	                }
	                super.fireEditingStopped();
	                return true;
	 
	        }
	 
	        public void setBounds(Rectangle r)
	        {
	                r.width = Math.max(minWidth, r.width);
	                getComponent().setBounds(r);
	                tree.repaint();
	        }
	 
	        public void setBounds(int x, int y, int w, int h)
	        {
	                w = Math.max(minWidth, w);
	                getComponent().setBounds(x,y,w,h);
	                tree.repaint();
	        }
	}*/

	//Cell edit for ExpressionNode
	class ExpressionEditor extends JTextField implements TreeCellEditor
	{
		Expression exp;
		Vector<CellEditorListener> listeners = new Vector<CellEditorListener>();
		private static final int minWidth = 64;

		public ExpressionEditor()
		{
			super("");
			addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent as)
				{
					if (stopCellEditing())
						fireEditingStopped();
				}
			});
		}

		public void cancelCellEditing()
		{
			setText("");
		}

		public boolean stopCellEditing()
		{
			String str = getText();

			try {
				Expression s = Prism.parseSingleExpressionString(str);
				exp = s;
			} catch (Exception e) {
				handler.getGUIPlugin().message("Error: Syntax Error");
				return true;
			}
			return true;

		}

		public Object getCellEditorValue()
		{
			return exp;
		}

		public boolean isCellEditable(EventObject eo)
		{
			if ((eo == null) | ((eo instanceof MouseEvent) && (((MouseEvent) eo).isMetaDown()))) {
				MouseEvent me = (MouseEvent) eo;
				if (me.getClickCount() >= 2) {
					return true;
				}
			}
			return false;
		}

		public boolean shouldSelectCell(EventObject eo)
		{
			if ((eo == null) | ((eo instanceof MouseEvent) && (((MouseEvent) eo).isMetaDown()))) {
				MouseEvent me = (MouseEvent) eo;
				if (me.getClickCount() >= 2) {
					return true;
				}
			}
			return false;
		}

		protected void fireEditingStopped()
		{
			if (listeners.size() > 0) {
				ChangeEvent ce = new ChangeEvent(this);
				for (int i = listeners.size() - 1; i >= 0; i--) {
					listeners.elementAt(i).editingStopped(ce);
				}

			}
		}

		//make sure the JTree gives the editor enough space
		public void setBounds(Rectangle r)
		{
			r.width = Math.max(minWidth, r.width);
			super.setBounds(r);
			tree.repaint();
		}

		public void setBounds(int x, int y, int w, int h)
		{
			w = Math.max(minWidth, w);
			super.setBounds(x, y, w, h);
			tree.repaint();
		}

		public void addCellEditorListener(CellEditorListener l)
		{
			listeners.addElement(l);
		}

		public void removeCellEditorListener(CellEditorListener l)
		{
			listeners.addElement(l);
		}

		public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row)
		{
			return this;
		}

	}

	class ModelTypeNode extends StringNode
	{
		public ModelTypeNode(String init, boolean editable)
		{
			super("Type: ", init, editable);
		}

		public void setModelType(ModelType type)
		{
			setUserObject(type.toString());
			switch (type) {
			case MDP:
				non.setSelected(true);
				break;
			case DTMC:
				pro.setSelected(true);
				break;
			case CTMC:
				sto.setSelected(true);
				break;
			}
			theModel.nodeChanged(this);
		}

		public void setUserObject(Object obj)
		{
			super.setUserObject(obj);
			//System.out.println("setting model type user object "+obj);

		}
	}

	//Cell Editor for ModelTypeNode
	class ModelTypeEditor extends JComboBox implements TreeCellEditor
	{
		String value;
		Vector<CellEditorListener> listeners = new Vector<CellEditorListener>();
		Object[] list;

		public ModelTypeEditor(Object[] list)
		{
			super(list);
			setFont(tree.getFont());
			setBackground(Color.white);
			setForeground(Color.blue);

			this.list = list;
			setEditable(false);//why???
			value = list[0].toString();

			addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					if (stopCellEditing())
						fireEditingStopped();
				}
			});
		}

		public void setSelectedType(String str)
		{
			for (int i = 0; i < list.length; i++) {
				String cur = (String) list[i];
				if (cur.equals(str)) {
					super.setSelectedIndex(i);
					break;
				}
			}
		}

		public void cancelCellEditing()
		{
		}

		public boolean stopCellEditing()
		{
			try {
				value = (String) getSelectedItem();
				if (value == null) {
					value = (String) getItemAt(0);
				}
				return true;

			} catch (Exception e) {
				return false;
			}
		}

		public Object getCellEditorValue()
		{
			return value;
		}

		public boolean isCellEditable(EventObject eo)
		{
			if ((eo == null) || ((eo instanceof MouseEvent) && (((MouseEvent) eo).isMetaDown()))) {
				MouseEvent me = (MouseEvent) eo;
				if (me.getClickCount() >= 2) {
					return true;
				}
			}
			return false;
		}

		public boolean shouldSelectCell(EventObject eo)
		{
			if ((eo == null) | ((eo instanceof MouseEvent) && (((MouseEvent) eo).isMetaDown()))) {
				MouseEvent me = (MouseEvent) eo;
				if (me.getClickCount() >= 2) {
					return true;
				}
			}
			return false;
		}

		public void addCellEditorListener(CellEditorListener cel)
		{
			listeners.addElement(cel);
		}

		public void removeCellEditorListener(CellEditorListener cel)
		{
			listeners.removeElement(cel);
		}

		protected void fireEditingStopped()
		{
			if (listeners.size() > 0) {
				ChangeEvent ce = new ChangeEvent(this);
				for (int i = listeners.size() - 1; i >= 0; i--) {
					listeners.elementAt(i).editingStopped(ce);
				}

			}
		}

		public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row)
		{
			return this;
		}

		/*
		                //make sure the JTree gives the editor enough space
		                public void setBounds(Rectangle r)
		                {
		                                r.width = Math.max(minWidth, r.width);
		                                super.setBounds(r);
		                }
		 
		                public void setBounds(int x, int y, int w, int h)
		                {
		                                w = Math.max(minWidth, w);
		                                super.setBounds(x,y,w,h);
		                }*/

	}

	//Node Renderer
	class PrismNodeRenderer extends DefaultTreeCellRenderer
	{
		ImageIcon VAR = GUIPrism.getIconFromImage("smallVariable.png");
		ImageIcon MOD = GUIPrism.getIconFromImage("smallModule.png");
		//ImageIcon EXP = GUIPrism.getIconFromImage("smallExpression.png");
		ImageIcon VAL = GUIPrism.getIconFromImage("smallValue.png");
		ImageIcon GOOD = GUIPrism.getIconFromImage("smallTick.png");
		ImageIcon BAD = GUIPrism.getIconFromImage("smallCross.png");
		ImageIcon OUT_OF_SYNCH = GUIPrism.getIconFromImage("smallFilePrism.png");
		ImageIcon CLOCK = GUIPrism.getIconFromImage("smallClockAnim1.png");

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			PrismTreeNode node = (PrismTreeNode) value;

			if (node == root) {
				if (isParsing) {
					setIcon(animIcon);
				} else {
					switch (parseSynchState) {
					case TREE_SYNCHRONIZED_GOOD:
						setIcon(GOOD);
						break;
					case TREE_SYNCHRONIZED_BAD:
						setIcon(BAD);
						break;
					case TREE_NOT_SYNCHRONIZED:
						setIcon(OUT_OF_SYNCH);
						break;
					}
				}
			} else if (node instanceof VarNode) {
				setIcon(VAR);
			} else if (node instanceof BoolNode) {
				setIcon(VAR);
			} else if (node instanceof DeclarationNode) {
				setIcon(VAR);
			} else if (node instanceof ModuleNode) {
				setIcon(MOD);
			} else if (node instanceof ValueNode) {
				//if(node instanceof ExpressionNode)
				//{
				//setIcon(EXP);
				//}
				//else
				{
					setIcon(VAL);
				}
			} else if (node instanceof ModuleCollectionNode || node instanceof DeclarationCollectionNode || node instanceof ConstantCollectionNode) {
				setIcon(this.getDefaultClosedIcon());
			}
			if (node.isEditable()) {
				setForeground(Color.blue);
			} else
				setForeground(Color.black);

			return this;
		}
	}

	class ModelTreeCellEditor extends DefaultTreeCellEditor
	{
		ModelTypeEditor modelTypeEditor;
		ExpressionEditor expressionEditor;
		ModuleEditor moduleEditor;
		DeclarationEditor declarationEditor;

		String[] types = new String[] { "Non-deterministic", "Probabilistic", "Stochastic" };

		public ModelTreeCellEditor(JTree tree, PrismNodeRenderer renderer)
		{
			super(tree, renderer);
			modelTypeEditor = new ModelTypeEditor(types);
			expressionEditor = new ExpressionEditor();
			moduleEditor = new ModuleEditor();
			declarationEditor = new DeclarationEditor();
			realEditor = declarationEditor;
		}

		public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row)
		{
			if (value instanceof ExpressionNode) {
				realEditor = expressionEditor;
				//((JTextField)expressionEditor.getComponent()).setText(((ExpressionNode)value).getValue().toString());
			} else if (value instanceof ModelTypeNode) {
				realEditor = modelTypeEditor;
				modelTypeEditor.setSelectedType(((ModelTypeNode) value).getValue());
				//tag = "Type: ";
			} else if (value instanceof ModuleNode) {
				realEditor = moduleEditor;
				moduleEditor.setText(((ModuleNode) value).getName());
			} else if (value instanceof DeclarationNode) {
				realEditor = declarationEditor;
				declarationEditor.setText(((DeclarationNode) value).getName());
			}
			return super.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);
		}

		public void addCellEditorListener(CellEditorListener l)
		{

			modelTypeEditor.addCellEditorListener(l);
			expressionEditor.addCellEditorListener(l);

			moduleEditor.addCellEditorListener(l);

			declarationEditor.addCellEditorListener(l);
			super.addCellEditorListener(l);
		}

		public void removeCellEditorListener(CellEditorListener l)
		{
			modelTypeEditor.removeCellEditorListener(l);
			expressionEditor.removeCellEditorListener(l);

			moduleEditor.removeCellEditorListener(l);

			declarationEditor.removeCellEditorListener(l);
			super.removeCellEditorListener(l);
		}
	}

	/*
	class ModelTreeCellEditor_n implements TreeCellEditor
	{
	        ModelTypeEditor modelTypeEditor;
	        ExpressionEditor expressionEditor;
	        ModuleEditor moduleEditor;
	        DeclarationEditor declarationEditor;
	 
	        CellEditor currentEditor;
	 
	        DefaultTreeCellEditor ttt;
	 
	        String[] types = new String[]
	        {"Non-deterministic", "Probabilistic", "Stochastic"};
	 
	        PrismNodeRenderer renderer;
	 
	        public ModelTreeCellEditor_n(PrismNodeRenderer renderer)
	        {
	                this.renderer = renderer;
	                modelTypeEditor = new ModelTypeEditor(types);
	                expressionEditor = new ExpressionEditor();
	                moduleEditor = new ModuleEditor();
	                declarationEditor = new DeclarationEditor();
	                currentEditor = declarationEditor;
	        }
	 
	 
	 
	        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row)
	        {
	                String tag = "";
	                if(value instanceof ExpressionNode)
	                {
	                        currentEditor = expressionEditor;
	//				((JTextField)expressionEditor.getComponent()).setText(((ExpressionNode)value).getValue().toString());
	                }
	                else if(value instanceof ModelTypeNode)
	                {
	                        currentEditor = modelTypeEditor;
	                        modelTypeEditor.setSelectedType(((ModelTypeNode)value).getValue());
	                        tag = "Type: ";
	                }
	                else if(value instanceof ModuleNode)
	                {
	                        currentEditor = moduleEditor;
	                        moduleEditor.setText(((ModuleNode)value).getName());
	                }
	                else if(value instanceof DeclarationNode)
	                {
	                        currentEditor = declarationEditor;
	                        declarationEditor.setText(((DeclarationNode)value).getName());
	                }
	                DefaultTreeCellRenderer c = (DefaultTreeCellRenderer)renderer.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, true);
	                JPanel p = new JPanel();
	                p.setBackground(Color.white);
	                p.setLayout(new BorderLayout());
	                JLabel l = new JLabel(tag,c.getIcon(), JLabel.HORIZONTAL);
	                l.setBackground(Color.white);
	                l.setForeground(Color.blue);
	                l.setFont(tree.getFont());
	                p.add(l, BorderLayout.WEST);
	                if(currentEditor instanceof ExpressionEditor)
	                {
	                        p.add(((ExpressionEditor)currentEditor).getComponent(), BorderLayout.CENTER);
	                }
	                else
	                {
	                        p.add((Component)currentEditor, BorderLayout.CENTER);
	                }
	                return p;//(Component)currentEditor;
	        }
	 
	        public Object getCellEditorValue()
	        {
	                return currentEditor.getCellEditorValue();
	 
	        }
	 
	        public boolean isCellEditable(EventObject event)
	        {
	                return true;
	                //return currentEditor.isCellEditable(event);
	        }
	 
	        public boolean shouldSelectCell(EventObject event)
	        {
	                return currentEditor.shouldSelectCell(event);
	        }
	 
	        public boolean stopCellEditing()
	        {
	                return currentEditor.stopCellEditing();
	        }
	 
	        public void cancelCellEditing()
	        {
	                currentEditor.cancelCellEditing();
	        }
	 
	        public void addCellEditorListener(CellEditorListener l)
	        {
	 
	                modelTypeEditor.addCellEditorListener(l);
	                expressionEditor.addCellEditorListener(l);
	 
	                moduleEditor.addCellEditorListener(l);
	 
	                declarationEditor.addCellEditorListener(l);
	        }
	 
	        public void removeCellEditorListener(CellEditorListener l)
	        {
	                modelTypeEditor.removeCellEditorListener(l);
	                expressionEditor.removeCellEditorListener(l);
	 
	                moduleEditor.removeCellEditorListener(l);
	 
	                declarationEditor.removeCellEditorListener(l);
	        }
	 
	}
	 
	 */

	class IconThread extends Thread
	{
		int index;
		ImageIcon[] images;
		boolean canContinue = false;

		public IconThread(int index)
		{
			this.index = index;
			images = new ImageIcon[8];
			images[0] = GUIPrism.getIconFromImage("smallClockAnim1.png");
			images[1] = GUIPrism.getIconFromImage("smallClockAnim2.png");
			images[2] = GUIPrism.getIconFromImage("smallClockAnim3.png");
			images[3] = GUIPrism.getIconFromImage("smallClockAnim4.png");
			images[4] = GUIPrism.getIconFromImage("smallClockAnim5.png");
			images[5] = GUIPrism.getIconFromImage("smallClockAnim6.png");
			images[6] = GUIPrism.getIconFromImage("smallClockAnim7.png");
			images[7] = GUIPrism.getIconFromImage("smallClockAnim8.png");
		}

		public void run()
		{
			try {
				int counter = 0;
				while (!interrupted() && index > -1) {
					counter++;
					counter = counter % 8;
					animIcon = images[counter];
					tree.repaint();
					sleep(150);
				}
			} catch (InterruptedException e) {
			}
			canContinue = true;
		}
	}

	private static String removeCarriages(String line)
	{
		String lineBuffer = "";
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c != '\n')
				lineBuffer += c;
			else
				lineBuffer += " ";
		}
		return lineBuffer;
	}

	public class Variable
	{
		public String name;
	}
	
	public class IntegerVariable extends Variable
	{
		public String min, max, init="0";
		
		public IntegerVariable(String name, String min, String max, String init)
		{
			super.name = name;
			this.min = min;
			this.max = max;
			this.init = init;
		}
		
		public IntegerVariable(String name, String min, String max)
		{
			this.name = name;
			this.min = min;
			this.max = max;
		}
	}
	
	public class BooleanVariable extends Variable
	{
		public String init = "false";
		
		public BooleanVariable(String name, String init)
		{
			this.init = init;
			this.name = name;
		}
		
		public BooleanVariable(String name)
		{
			this.name = name;
		}
	}
}
