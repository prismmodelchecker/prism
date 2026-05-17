package explicit;

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import prism.PrismComponent;
import prism.PrismException;

/**
 * Tests for {@link SCCComputerTarjan}, focusing on the trivial-SCC filter.
 *
 * A trivial SCC is a singleton with no self-loop.  With filterTrivialSCCs=true,
 * trivial SCCs must be silently dropped; singletons WITH a self-loop must be kept.
 *
 * The filter check in SCCComputerTarjan.tarjan() reads
 *   frameHadSelfloop[frameTop]
 * after the line
 *   frameTop--;
 * At that point frameTop == fi (the just-popped frame's index), so this reads
 * the correct, current node's flag — not the parent's.  GitHub Copilot raised a
 * concern that it reads the parent frame's flag, but that analysis is incorrect:
 * before the decrement frameTop = fi+1, so afterwards frameTop = fi, which is
 * still the current frame.  The tests below confirm the correct behaviour and
 * would catch a regression if the indexing were ever changed to an off-by-one.
 */
public class SCCComputerTarjanTest
{
	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	/** Build a DTMC whose transition graph is described by the given edge list. */
	private DTMCSimple<Double> buildDTMC(int numStates, int[][] edges)
	{
		DTMCSimple<Double> dtmc = new DTMCSimple<>(numStates);
		for (int[] edge : edges) {
			dtmc.setProbability(edge[0], edge[1], 1.0);
		}
		return dtmc;
	}

	/** Run the Tarjan SCC computer and return each SCC as a Set<Integer>. */
	private List<Set<Integer>> computeSCCs(DTMCSimple<Double> dtmc, boolean filterTrivial) throws PrismException
	{
		SCCConsumerStore consumer = new SCCConsumerStore();
		SCCComputerTarjan scc = new SCCComputerTarjan(new PrismComponent(), dtmc, consumer);
		scc.computeSCCs(filterTrivial);
		return consumer.getSCCs().stream()
				.map(SCCComputerTarjanTest::toSet)
				.collect(Collectors.toList());
	}

	private static Set<Integer> toSet(BitSet bs)
	{
		Set<Integer> s = new HashSet<>();
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			s.add(i);
		}
		return s;
	}

	// -----------------------------------------------------------------------
	// Basic singleton cases
	// -----------------------------------------------------------------------

	@Test
	void singletonWithSelfLoopIsKeptByFilter() throws PrismException
	{
		// Graph: 0 → 0  (self-loop)
		// Self-loop makes this a non-trivial singleton; must be reported.
		DTMCSimple<Double> dtmc = buildDTMC(1, new int[][]{{0, 0}});
		List<Set<Integer>> sccs = computeSCCs(dtmc, true);
		assertEquals(1, sccs.size());
		assertEquals(Set.of(0), sccs.get(0));
	}

	@Test
	void singletonWithoutSelfLoopIsDroppedByFilter() throws PrismException
	{
		// Graph: 0  (isolated node, no edges)
		// Trivial singleton must be filtered out.
		DTMCSimple<Double> dtmc = buildDTMC(1, new int[][]{});
		List<Set<Integer>> sccs = computeSCCs(dtmc, true);
		assertEquals(0, sccs.size());
	}

	@Test
	void singletonWithoutSelfLoopIsRetainedWithoutFilter() throws PrismException
	{
		// Same graph as above, but filterTrivialSCCs=false → must be included.
		DTMCSimple<Double> dtmc = buildDTMC(1, new int[][]{});
		List<Set<Integer>> sccs = computeSCCs(dtmc, false);
		assertEquals(1, sccs.size());
	}

	// -----------------------------------------------------------------------
	// Chains — the scenario that prompted the Copilot review comment.
	//
	// In a chain the DFS pops deeper frames before shallower ones.  If the
	// self-loop flag index were off by one (reading the child's or parent's
	// frame instead of the current frame), a node could inherit the wrong flag.
	// -----------------------------------------------------------------------

	@Test
	void chainWhereDeepNodeHasSelfLoop() throws PrismException
	{
		// Graph:  0 → 1 → 1  (self-loop on 1)
		// DFS order: push 0, push 1; pop 1 (has self-loop → kept), pop 0 (no self-loop → filtered).
		// Expected with filter=true:  [{1}]
		// A wrong-index bug would propagate 1's self-loop flag to 0, incorrectly keeping {0}.
		DTMCSimple<Double> dtmc = buildDTMC(2, new int[][]{{0, 1}, {1, 1}});
		List<Set<Integer>> sccs = computeSCCs(dtmc, true);
		assertEquals(1, sccs.size());
		assertEquals(Set.of(1), sccs.get(0));
	}

	@Test
	void chainWhereShallowNodeHasSelfLoop() throws PrismException
	{
		// Graph:  0 → 0, 0 → 1  (self-loop on 0; 1 has no self-loop)
		// Expected with filter=true:  [{0}]
		// A wrong-index bug could give 0's self-loop flag to 1, incorrectly keeping {1}.
		DTMCSimple<Double> dtmc = buildDTMC(2, new int[][]{{0, 0}, {0, 1}});
		List<Set<Integer>> sccs = computeSCCs(dtmc, true);
		assertEquals(1, sccs.size());
		assertEquals(Set.of(0), sccs.get(0));
	}

	@Test
	void longerChainEndWithSelfLoop() throws PrismException
	{
		// Graph:  0 → 1 → 2 → 2  (self-loop only on 2)
		// Expected with filter=true:  [{2}]
		// Intermediate node 1 must not inherit node 2's self-loop flag.
		DTMCSimple<Double> dtmc = buildDTMC(3, new int[][]{{0, 1}, {1, 2}, {2, 2}});
		List<Set<Integer>> sccs = computeSCCs(dtmc, true);
		assertEquals(1, sccs.size());
		assertEquals(Set.of(2), sccs.get(0));
	}

	@Test
	void longerChainMiddleNodeHasSelfLoop() throws PrismException
	{
		// Graph:  0 → 1 → 2,  1 → 1  (self-loop on 1)
		// Expected with filter=true:  [{1}]
		DTMCSimple<Double> dtmc = buildDTMC(3, new int[][]{{0, 1}, {1, 2}, {1, 1}});
		List<Set<Integer>> sccs = computeSCCs(dtmc, true);
		assertEquals(1, sccs.size());
		assertEquals(Set.of(1), sccs.get(0));
	}

	@Test
	void chainWithNoSelfLoopsIsFullyFiltered() throws PrismException
	{
		// Graph:  0 → 1 → 2 → 3  (no self-loops anywhere)
		// Expected with filter=true:  []
		DTMCSimple<Double> dtmc = buildDTMC(4, new int[][]{{0, 1}, {1, 2}, {2, 3}});
		List<Set<Integer>> sccs = computeSCCs(dtmc, true);
		assertEquals(0, sccs.size());
	}

	@Test
	void chainWithNoSelfLoopsRetainedWithoutFilter() throws PrismException
	{
		// Same graph, filterTrivialSCCs=false → all 4 singletons reported.
		DTMCSimple<Double> dtmc = buildDTMC(4, new int[][]{{0, 1}, {1, 2}, {2, 3}});
		List<Set<Integer>> sccs = computeSCCs(dtmc, false);
		assertEquals(4, sccs.size());
	}

	// -----------------------------------------------------------------------
	// Non-trivial (multi-node) SCCs — always kept regardless of filter
	// -----------------------------------------------------------------------

	@Test
	void twoCycleIsAlwaysKept() throws PrismException
	{
		// Graph:  0 → 1 → 0  (2-node SCC, never trivial)
		// Expected with filter=true:  [{0,1}]
		DTMCSimple<Double> dtmc = buildDTMC(2, new int[][]{{0, 1}, {1, 0}});
		List<Set<Integer>> sccs = computeSCCs(dtmc, true);
		assertEquals(1, sccs.size());
		assertEquals(Set.of(0, 1), sccs.get(0));
	}

	@Test
	void twoCycleWithTrailingSingletonNoSelfLoop() throws PrismException
	{
		// Graph:  0 → 1, 1 → 0, 0 → 2  (cycle {0,1} plus trailing singleton 2)
		// Expected with filter=true:  [{0,1}]  (2 is filtered as trivial)
		DTMCSimple<Double> dtmc = buildDTMC(3, new int[][]{{0, 1}, {1, 0}, {0, 2}});
		List<Set<Integer>> sccs = computeSCCs(dtmc, true);
		assertEquals(1, sccs.size());
		assertEquals(Set.of(0, 1), sccs.get(0));
	}

	@Test
	void twoCycleWithTrailingSingletonWithSelfLoop() throws PrismException
	{
		// Graph:  0 → 1, 1 → 0, 0 → 2, 2 → 2  (cycle plus self-loop singleton)
		// Expected with filter=true:  [{0,1}, {2}]  (both kept)
		DTMCSimple<Double> dtmc = buildDTMC(3, new int[][]{{0, 1}, {1, 0}, {0, 2}, {2, 2}});
		List<Set<Integer>> sccs = computeSCCs(dtmc, true);
		assertEquals(2, sccs.size());
		Set<Set<Integer>> sccSet = new HashSet<>(sccs);
		assertTrue(sccSet.contains(Set.of(0, 1)));
		assertTrue(sccSet.contains(Set.of(2)));
	}
}
