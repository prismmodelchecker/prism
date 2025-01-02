package io;

/**
 * Model export formats
 */
public enum ModelExportFormat
{
	EXPLICIT, MATLAB, DOT, DRN;

	public String description()
	{
		switch (this) {
			case EXPLICIT:
				return "in plain text format";
			case MATLAB:
				return "in Matlab format";
			case DOT:
				return "in Dot format";
			case DRN:
				return "in DRN format";
			default:
				return this.toString();
		}
	}
}
