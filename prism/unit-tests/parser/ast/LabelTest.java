package parser.ast;

import org.junit.jupiter.api.Test;
import parser.type.TypeBool;
import parser.type.TypeInt;
import prism.PrismLangException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LabelTest
{
	@Test
	public void testFindCycles_emptyList()
	{
		LabelList ll = new LabelList();
		assertDoesNotThrow(ll::findCycles);
	}

	@Test
	public void testFindCycles_1LabelCycle()
	{
		LabelList ll = createLabelListWith1LabelCycle();
		assertThrows(PrismLangException.class, ll::findCycles);
	}

	@Test
	public void testFindCycles_2LabelCycle()
	{
		LabelList ll = createLabelListWith2LabelCycle();
		assertThrows(PrismLangException.class, ll::findCycles);
	}

	@Test
	public void testFindCycles_noCycle()
	{
		LabelList ll = createLabelListWithoutCycle();
		assertDoesNotThrow(ll::findCycles);
	}

	/**
	 * <p>
	 * Helper method that will create a {@link LabelList} with the following Labels:
	 * </p>
	 * <ul>
	 *   <li>label "l2" = d<=3;</li>
	 * 	 <li>label "l1" = "l2" | d=5;</li>
	 * </ul>
	 * </p>
	 * Note: d is the only variable (index 0) and has {@link TypeInt}
	 * <p>
	 */
	protected static LabelList createLabelListWithoutCycle()
	{
		LabelList ll = new LabelList();
		ExpressionVar dVar = new ExpressionVar("d", TypeInt.getInstance());
		dVar.setIndex(0); // index of d is 0
		ll.addLabel(new ExpressionIdent("l1"), new ExpressionBinaryOp(ExpressionBinaryOp.OR,
				new ExpressionLabel("l2"), new ExpressionBinaryOp(ExpressionBinaryOp.EQ,
				dVar, new ExpressionLiteral(TypeInt.getInstance(), 5))));
		ll.addLabel(new ExpressionIdent("l2"), new ExpressionBinaryOp(ExpressionBinaryOp.LE,
				dVar, new ExpressionLiteral(TypeInt.getInstance(), 3)));
		return ll;
	}

	/**
	 * <p>
	 * Helper method that will create a {@link LabelList} with the following Labels:
	 * </p>
	 * <ul>
	 *   <li>label "l1" = "l1";</li>
	 * </ul>
	 */
	protected static LabelList createLabelListWith1LabelCycle()
	{
		LabelList ll = new LabelList();
		ll.addLabel(new ExpressionIdent("l1"), new ExpressionLabel("l1"));
		return ll;
	}

	/**
	 * <p>
	 * Helper method that will create a {@link LabelList} with the following Labels:
	 * </p>
	 * <ul>
	 *   <li>label "l1" = "l2";</li>
	 *   <li>label "l2" = "l1" | d=5;</li>
	 * </ul>
	 * </p>
	 * Note: d is the only variable (index 0) and has {@link TypeInt}
	 * <p>
	 */
	protected static LabelList createLabelListWith2LabelCycle()
	{
		LabelList ll = new LabelList();
		ll.addLabel(new ExpressionIdent("l1"), new ExpressionLabel("l2"));
		ll.addLabel(new ExpressionIdent("l2"), new ExpressionBinaryOp(ExpressionBinaryOp.OR,
				new ExpressionLabel("l1"), new ExpressionBinaryOp(ExpressionBinaryOp.EQ,
				new ExpressionVar("d", TypeInt.getInstance()), new ExpressionLiteral(TypeInt.getInstance(), 5))));
		return ll;
	}

	/**
	 * <p>
	 * Helper method that will create a {@link LabelList} with the following Labels:
	 * </p>
	 * <ul>
	 *   <li>label "l1" = "l2";</li>
	 *   <li>label "l2" = "l3";</li>
	 *   <li>label "l3" = true;</li>
	 * </ul>
	 */
	protected static LabelList createLabelListWithDependencyChain()
	{
		LabelList ll = new LabelList();
		ll.addLabel(new ExpressionIdent("l1"), new ExpressionLabel("l2"));
		ll.addLabel(new ExpressionIdent("l2"), new ExpressionLabel("l3"));
		ll.addLabel(new ExpressionIdent("l3"), new ExpressionLiteral(TypeBool.getInstance(), true));
		return ll;
	}
}
