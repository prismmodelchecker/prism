package io;

/**
 * Model export formats
 */
public enum ModelExportFormat
{
	EXPLICIT, MATLAB, DOT, DD_DOT, DRN, UMB;

	public String description()
	{
		switch (this) {
			case EXPLICIT:
				return "in plain text format";
			case MATLAB:
				return "in Matlab format";
			case DOT:
				return "in Dot format";
			case DD_DOT:
				return "in DD Dot format";
			case DRN:
				return "in DRN format";
			case UMB:
				return "in UMB format";
			default:
				return this.toString();
		}
	}

	public boolean isBinary()
	{
		return this == UMB;
	}
}
