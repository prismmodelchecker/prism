/*
 * Copyright 2025 Dave Parker (University of Oxford)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pmctools.umbj;

import java.util.PrimitiveIterator;
import java.util.function.LongConsumer;

/**
 * Miscellaneous utilities for working with UMB files.
 */
public class UMBUtils
{
	/**
	 * Class to convert a primitive operator (int to long).
	 */
	public static class IntToLongIteratorAdapter implements PrimitiveIterator.OfLong
	{
		private final PrimitiveIterator.OfInt intIterator;

		public IntToLongIteratorAdapter(PrimitiveIterator.OfInt intIterator)
		{
			this.intIterator = intIterator;
		}

		@Override
		public boolean hasNext()
		{
			return intIterator.hasNext();
		}

		@Override
		public long nextLong()
		{
			return intIterator.nextInt();
		}

		@Override
		public void forEachRemaining(LongConsumer action)
		{
			intIterator.forEachRemaining((int value) -> action.accept(value));
		}
	}
}
