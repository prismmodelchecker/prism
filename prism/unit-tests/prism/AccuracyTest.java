package prism;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import prism.Accuracy.AccuracyLevel;
import prism.Accuracy.AccuracyType;

public class AccuracyTest
{
	@Test
	public void testComplementOfProbs()
	{
		// Unknown accuracy stays unknown
		assertNull(AccuracyFactory.complementOfProbs(null));
		// Absolute (and exact) accuracies are unchanged
		Accuracy abs = new Accuracy(AccuracyLevel.BOUNDED, 1e-6, AccuracyType.ABSOLUTE);
		assertSame(abs, AccuracyFactory.complementOfProbs(abs));
		Accuracy exact = new Accuracy(AccuracyLevel.EXACT_FLOATING_POINT);
		assertSame(exact, AccuracyFactory.complementOfProbs(exact));
		// Relative accuracies become absolute, with the same bound and level
		Accuracy rel = new Accuracy(AccuracyLevel.ESTIMATED_BOUNDED, 1e-5, AccuracyType.RELATIVE);
		Accuracy comp = AccuracyFactory.complementOfProbs(rel);
		assertEquals(AccuracyLevel.ESTIMATED_BOUNDED, comp.getLevel());
		assertEquals(AccuracyType.ABSOLUTE, comp.getType());
		assertEquals(1e-5, comp.getErrorBound());
		assertEquals(1e-5, comp.getAbsoluteErrorBound(1e-6));
		// The probability of a probabilistic bound is kept
		Accuracy prob = new Accuracy(AccuracyLevel.PROBABLY_BOUNDED, 1e-2, AccuracyType.RELATIVE);
		prob.setProbability(0.99);
		comp = AccuracyFactory.complementOfProbs(prob);
		assertEquals(AccuracyLevel.PROBABLY_BOUNDED, comp.getLevel());
		assertEquals(AccuracyType.ABSOLUTE, comp.getType());
		assertEquals(0.99, comp.getProbability());
	}

	@Test
	public void testGetTestingAccuracyForResult()
	{
		Accuracy def = ResultTesting.DEFAULT_TESTING_ACCURACY;
		// Missing accuracy: default
		assertSame(def, ResultTesting.getTestingAccuracy(null, 0.5));
		// Non-estimated accuracies are used as they are
		Accuracy bounded = new Accuracy(AccuracyLevel.BOUNDED, 1e-3, AccuracyType.ABSOLUTE);
		assertSame(bounded, ResultTesting.getTestingAccuracy(bounded, 0.5));
		Accuracy exact = new Accuracy(AccuracyLevel.EXACT_FLOATING_POINT);
		assertSame(exact, ResultTesting.getTestingAccuracy(exact, 0.0));
		// An estimate's bound is used if larger than the default's, e.g. near 0
		Accuracy est = new Accuracy(AccuracyLevel.ESTIMATED_BOUNDED, 9e-6, AccuracyType.ABSOLUTE);
		for (double r : new double[] { 1e-6, 0.0, -1e-6 }) {
			Accuracy acc = ResultTesting.getTestingAccuracy(est, r);
			assertEquals(AccuracyType.ABSOLUTE, acc.getType());
			assertEquals(9e-6, acc.getAbsoluteErrorBound(r));
		}
		// ... but capped at MAX_ESTIMATED_TESTING_ERROR
		Accuracy estLarge = new Accuracy(AccuracyLevel.ESTIMATED_BOUNDED, 1e-2, AccuracyType.ABSOLUTE);
		assertEquals(ResultTesting.MAX_ESTIMATED_TESTING_ERROR, ResultTesting.getTestingAccuracy(estLarge, 0.02).getAbsoluteErrorBound(0.02));
		assertEquals(ResultTesting.MAX_ESTIMATED_TESTING_ERROR, ResultTesting.getTestingAccuracy(estLarge, 0.0).getAbsoluteErrorBound(0.0));
		// ... and the default is used if the (capped) estimate is not larger
		assertSame(def, ResultTesting.getTestingAccuracy(estLarge, 2.0));
		Accuracy estSmall = new Accuracy(AccuracyLevel.ESTIMATED_BOUNDED, 1e-8, AccuracyType.ABSOLUTE);
		assertSame(def, ResultTesting.getTestingAccuracy(estSmall, 0.5));
		Accuracy estRel = new Accuracy(AccuracyLevel.ESTIMATED_BOUNDED, 1e-6, AccuracyType.RELATIVE);
		assertSame(def, ResultTesting.getTestingAccuracy(estRel, 0.0));
		assertSame(def, ResultTesting.getTestingAccuracy(estRel, 0.5));
		// Infinite results: default
		assertSame(def, ResultTesting.getTestingAccuracy(est, Double.POSITIVE_INFINITY));
	}
}
