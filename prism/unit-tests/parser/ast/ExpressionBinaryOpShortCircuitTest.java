package parser.ast;

import static org.junit.jupiter.api.Assertions.*;
import static parser.ast.ExpressionBinaryOp.IMPLIES;
import static parser.ast.ExpressionBinaryOp.OR;
import static parser.ast.ExpressionBinaryOp.AND;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import parser.EvaluateContext;
import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
import prism.PrismLangException;

import java.util.Objects;

/**
 * Test whether short-circuiting is exploited during expression evaluation.
 */
public class ExpressionBinaryOpShortCircuitTest
{
	// Dummy context for #evaluate
	EvaluateContext ec = new EvaluateContext()
	{
		@Override
		public Object getVarValue(String name, int index)
		{
			return null;
		}
	};

	@ParameterizedTest
	@CsvSource({"false, false", "false, true", "true, false", "true, true"})
	public void testEvaluateImplies(boolean v1, boolean v2) throws PrismLangException
	{
		ExpressionMock op1 = new ExpressionMock(v1);
		ExpressionMock op2 = new ExpressionMock(v2);

		assertEquals(!v1 || v2, new ExpressionBinaryOp(IMPLIES, op1, op2).evaluate(ec));
	}

	@ParameterizedTest
	@CsvSource({"false, false", "false, true"})
	public void testShortCircuitImplies(boolean v1, boolean v2) throws PrismLangException
	{
		ExpressionMock op1 = new ExpressionMock(v1);
		ExpressionMock op2 = new ExpressionMock(v2);

		new ExpressionBinaryOp(IMPLIES, op1, op2).evaluate(ec);
		assertFalse(op1.isEvaluated() && op2.isEvaluated());
	}

	@ParameterizedTest
	@CsvSource({"false, false", "false, true", "true, false", "true, true"})
	public void testEvaluateOr(boolean v1, boolean v2) throws PrismLangException
	{
		ExpressionMock op1 = new ExpressionMock(v1);
		ExpressionMock op2 = new ExpressionMock(v2);

		assertEquals(v1 || v2, new ExpressionBinaryOp(OR, op1, op2).evaluate(ec));
	}

	@ParameterizedTest
	@CsvSource({"true, false", "true, true"})
	public void testShortCircuitOr(boolean v1, boolean v2) throws PrismLangException
	{
		ExpressionMock op1 = new ExpressionMock(v1);
		ExpressionMock op2 = new ExpressionMock(v2);

		new ExpressionBinaryOp(OR, op1, op2).evaluate(ec);
		assertFalse(op1.isEvaluated() && op2.isEvaluated());
	}

	@ParameterizedTest
	@CsvSource({"false, false", "false, true", "true, false", "true, true"})
	public void testEvaluateAnd(boolean v1, boolean v2) throws PrismLangException
	{
		ExpressionMock op1 = new ExpressionMock(v1);
		ExpressionMock op2 = new ExpressionMock(v2);

		assertEquals(v1 && v2, new ExpressionBinaryOp(AND, op1, op2).evaluate(ec));
	}

	@ParameterizedTest
	@CsvSource({"false, false", "false, true"})
	public void testShortCircuitAnd(boolean v1, boolean v2) throws PrismLangException
	{
		ExpressionMock op1 = new ExpressionMock(v1);
		ExpressionMock op2 = new ExpressionMock(v2);

		new ExpressionBinaryOp(AND, op1, op2).evaluate(ec);
		assertFalse(op1.isEvaluated() && op2.isEvaluated());
	}


	/**
	 * Mocked expression signaling whether #evaluate was invoked or not.
	 */
	public static class ExpressionMock extends Expression
	{
		protected boolean evaluated = false;
		protected Object value;

		public ExpressionMock(Object value)
		{
			this.value = Objects.requireNonNull(value);
		}

		public boolean isEvaluated()
		{
			return evaluated;
		}

		@Override
		public Object accept(ASTVisitor v) throws PrismLangException
		{
			return null;
		}

		@Override
		public ExpressionMock deepCopy(DeepCopy copier)
		{
			return this;
		}

		@Override
		public String toString()
		{
			return "Evaluated? " + evaluated;
		}

		@Override
		public boolean isConstant()
		{
			return true;
		}

		@Override
		public boolean isProposition()
		{
			return true;
		}

		@Override
		public Object evaluate(EvaluateContext ec) throws PrismLangException
		{
			evaluated = true;
			return value;
		}

		@Override
		public boolean returnsSingleValue()
		{
			return false;
		}

		@Override
		public Expression deepCopy()
		{
			return this;
		}
	}
}
