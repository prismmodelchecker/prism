//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
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

package prism;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for parsing and comparing PRISM version strings.
 */
public final class VersionUtils
{
	private static final Pattern VERSION_ALPHA_PATTERN = Pattern.compile("alpha(\\d*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern VERSION_BETA_PATTERN = Pattern.compile("beta(\\d*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern VERSION_DEV_PATTERN = Pattern.compile("dev(\\d*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern VERSION_POST_PATTERN = Pattern.compile("post(\\d*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern VERSION_LEGACY_DOT_DEV_PATTERN = Pattern.compile("\\.dev(\\d*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern VERSION_REVISION_PATTERN = Pattern.compile("r(\\d+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern VERSION_NUMERIC_PATTERN = Pattern.compile("\\d+");

	private VersionUtils()
	{
	}

	/**
	 * Compare two version numbers of PRISM (strings).
	 * Supports standard semantic versioning, e.g., 2 < 2.1 < 2.1.1-dev < 2.1.1-alpha < 2.1.1-beta < 2.1.1.
	 * But also respects legacy PRISM use of ".dev" where 2.1.1 < 2.1.1.dev.
	 * To achieve this, ".dev" is interpreted as ".post" and differently to "-dev".
	 * Example ordering: { "1", "2.0", "2.1-dev", "2.1.alpha", "2.1.beta", "2.1", "2.1.post", "2.1.2", "2.9", "3", "3.4"};
	 * Returns: 1 if v1&gt;v2, -1 if v1&lt;v2, 0 if v1=v2.
	 */
	public static int compareVersions(String v1, String v2)
	{
		if (v1.equals(v2))
			return 0;

		String n1 = normaliseLegacyDotDev(v1);
		String n2 = normaliseLegacyDotDev(v2);
		if (n1.equals(n2))
			return 0;

		String[] ss1 = n1.split("[.-]", -1);
		String[] ss2 = n2.split("[.-]", -1);
		int n = Math.max(ss1.length, ss2.length);
		for (int i = 0; i < n; i++) {
			VersionPart p1 = parseVersionPart(i < ss1.length ? ss1[i] : "");
			VersionPart p2 = parseVersionPart(i < ss2.length ? ss2[i] : "");
			int cmp = p1.compareTo(p2);
			if (cmp != 0) {
				return cmp;
			}
		}

		return 0;
	}

	private static String normaliseLegacyDotDev(String version)
	{
		return VERSION_LEGACY_DOT_DEV_PATTERN.matcher(version).replaceAll(".post$1");
	}

	private static VersionPart parseVersionPart(String raw)
	{
		String s = raw == null ? "" : raw.trim();
		if (s.isEmpty()) {
			return new VersionPart(VersionPartType.NUMERIC, 0, "");
		}

		Matcher alpha = VERSION_ALPHA_PATTERN.matcher(s);
		if (alpha.matches()) {
			return new VersionPart(VersionPartType.ALPHA, parseInt(alpha.group(1)), "");
		}

		Matcher beta = VERSION_BETA_PATTERN.matcher(s);
		if (beta.matches()) {
			return new VersionPart(VersionPartType.BETA, parseInt(beta.group(1)), "");
		}

		Matcher dev = VERSION_DEV_PATTERN.matcher(s);
		if (dev.matches()) {
			return new VersionPart(VersionPartType.DEV, parseInt(dev.group(1)), "");
		}

		Matcher post = VERSION_POST_PATTERN.matcher(s);
		if (post.matches()) {
			return new VersionPart(VersionPartType.POST, parseInt(post.group(1)), "");
		}

		Matcher revision = VERSION_REVISION_PATTERN.matcher(s);
		if (revision.matches()) {
			return new VersionPart(VersionPartType.NUMERIC, parseInt(revision.group(1)), "");
		}

		Matcher numeric = VERSION_NUMERIC_PATTERN.matcher(s);
		if (numeric.matches()) {
			return new VersionPart(VersionPartType.NUMERIC, parseInt(s), "");
		}

		return new VersionPart(VersionPartType.TEXT, 0, s.toLowerCase(Locale.ROOT));
	}

	private static int parseInt(String s)
	{
		if (s == null || s.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}

	private static class VersionPart implements Comparable<VersionPart>
	{
		private final VersionPartType type;
		private final int numericValue;
		private final String textValue;

		private VersionPart(VersionPartType type, int numericValue, String textValue)
		{
			this.type = type;
			this.numericValue = numericValue;
			this.textValue = textValue;
		}

		@Override
		public int compareTo(VersionPart other)
		{
			// Post-release tags are newer than release (0) but older than patch levels >= 1.
			if (type == VersionPartType.POST && other.type == VersionPartType.NUMERIC) {
				return other.numericValue == 0 ? 1 : -1;
			}
			if (type == VersionPartType.NUMERIC && other.type == VersionPartType.POST) {
				return numericValue == 0 ? -1 : 1;
			}

			int rankCmp = Integer.compare(type.rank, other.type.rank);
			if (rankCmp != 0) {
				return rankCmp;
			}
			int numericCmp = Integer.compare(numericValue, other.numericValue);
			if (numericCmp != 0) {
				return numericCmp;
			}
			return textValue.compareTo(other.textValue);
		}
	}

	private static enum VersionPartType
	{
		DEV(0), ALPHA(1), BETA(2), TEXT(3), NUMERIC(4), POST(5);

		private final int rank;

		private VersionPartType(int rank)
		{
			this.rank = rank;
		}
	}
}
