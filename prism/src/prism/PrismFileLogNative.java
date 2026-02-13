package prism;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A PrintStream that will write to a file (or stdout) using native code,
 */
public class PrismFileLogNative extends PrintStream
{
    /** Native file pointer, cast to a long */
    protected long fp;
    /** Are we writing to stdout? */
    protected boolean stdout;

    /**
     * Create a stream that will write to {@code filename}, overwriting any previous contents.
     * If {@code filename} is "stdout", then output will be written to standard output.
     * In both cases, native code will be used to do the writing.
     * @param filename Filename of log file
     */
    public PrismFileLogNative(String filename) throws FileNotFoundException
    {
        this(filename, false);
    }

    /**
     * Create a stream that will write to {@code filename}, appending to an existing file if requested.
     * If {@code filename} is "stdout", then output will be written to standard output.
     * In both cases, native code will be used to do the writing.
     * @param filename Filename of log file
     * @param append Append to the existing file?
     */
    public PrismFileLogNative(String filename, boolean append) throws FileNotFoundException
    {
        super(OutputStream.nullOutputStream());
        this.stdout = "stdout".equals(filename);
        if (stdout) {
            fp = PrismNative.PN_GetStdout();
        } else {
            fp = append ? PrismNative.PN_OpenFileAppend(filename) : PrismNative.PN_OpenFile(filename);
        }
    }

    /**
     * Is this stream ready to be written to? (i.e. is the file pointer valid?)
     */
    public boolean ready()
    {
        return fp != 0;
    }

    /**
     * Get the native file pointer (cast to a long) that this stream is writing to.
     */
    public long getFilePointer()
    {
        return fp;
    }

    // Methods to overwrite the PrintStream implementation

    @Override
    public void write(byte[] buf, int off, int len)
    {
        if (fp == 0) {
            throw new IllegalStateException("Trying to write to an invalid file handle (already closed?)");
        }
        // Convert the byte array to a String and pass to the native method
        if (len > 0) {
            String s = new String(buf, off, len, java.nio.charset.StandardCharsets.UTF_8);
            PrismNative.PN_PrintToFile(fp, s);
        }
    }

    @Override
    public void write(int b)
    {
        write(new byte[]{(byte) b}, 0, 1);
    }

    @Override
    public void flush()
    {
        if (fp == 0) {
            throw new IllegalStateException("Trying to flush an invalid file handle (already closed?)");
        }
        PrismNative.PN_FlushFile(fp);
    }

    @Override
    public void close()
    {
        // Ignore if already closed (as specified by Closable contract)
        if (fp == 0) {
            return;
        }
        // We never close stdout
        if (stdout) {
            return;
        }
        PrismNative.PN_CloseFile(fp);
        // Set pointer to zero to indicate that the file handle is not valid anymore
        fp = 0;
    }
}
