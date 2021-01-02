//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package parser.visitor;

import java.util.List;

import parser.ast.*;
import parser.type.Type;
import prism.PrismLangException;

/**
 * Find all references to observations (by name), replace the ExpressionLabels with ExpressionObs objects.
 */
public class FindAllObsRefs extends ASTTraverseModify
{
	// A list of pairs of observable names/types
	private List<String> observableNames = null;
	private List<Type> observableTypes = null;
	
	public FindAllObsRefs(List<String> observableNames, List<Type> observableTypes)
	{
		this.observableNames = observableNames;
		this.observableTypes = observableTypes;
	}
	
	public Object visit(ExpressionLabel e) throws PrismLangException
	{
		// See if identifier corresponds to an observable
		int i = observableNames.indexOf(e.getName());
		if (i != -1) {
			// If so, replace it with an ExpressionObs object
			// (storing type explicitly)
			ExpressionObs expr = new ExpressionObs(e.getName());
			expr.setType(observableTypes.get(i));
			expr.setPosition(e);
			return expr;
		}
		// Otherwise, leave it unchanged
		return e;
	}
}

