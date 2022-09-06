package prism;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class PEPA2Prism extends PrismLanguageTranslator
{
	private File modelFile;

	@Override
	public String getName()
	{
		return "pepa";
	}

	@Override
	public void load(File file) throws PrismException
	{
		modelFile = file;
	}

	@Override
	public void load(String s) throws PrismException
	{
		try {
			// Create temporary file containing pepa model
			modelFile = File.createTempFile("tempPepa" + System.currentTimeMillis(), ".pepa");
			FileWriter write = new FileWriter(modelFile);
			write.write(s);
			write.close();
		} catch (IOException e) {
			if (modelFile != null) {
				modelFile.delete();
				modelFile = null;
			}
			throw new PrismException("Couldn't create temporary file for PEPA conversion");
		}
	}

	@Override
	public void load(InputStream in) throws PrismException
	{
		try {
			// Create temporary file containing pepa model
			modelFile = File.createTempFile("tempPepa" + System.currentTimeMillis(), ".pepa");
			FileWriter write = new FileWriter(modelFile);
			int byteIn = in.read();
			while (byteIn != -1) {
				write.write(byteIn);
				byteIn = in.read();
			}
			write.close();
		} catch (IOException e) {
			if (modelFile != null) {
				modelFile.delete();
				modelFile = null;
			}
			throw new PrismException("Couldn't create temporary file for PEPA conversion");
		}
	}

	@Override
	public String translateToString() throws PrismException
	{
		// Translate PEPA model to PRISM model string
		String prismModelString = null;
		try {
			prismModelString = pepa.compiler.Main.compile("" + modelFile);
		} catch (pepa.compiler.InternalError e) {
			throw new PrismException("Could not import PEPA model:\n" + e.getMessage());
		}
		return prismModelString;
	}

	@Override
	public void translate(PrintStream out) throws PrismException
	{
		out.print(translateToString());
	}
}
