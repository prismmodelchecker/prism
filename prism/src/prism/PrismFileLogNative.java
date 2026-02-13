package prism;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * A PrintStream that will write to a file (or stdout) using native code,
 */
public class PrismFileLogNative extends PrintStream
{
    /** Native file pointer, cast to a long */
    protected long fp;
    /** Are we writing to stdout? */
    protected boolean stdout;

    /** Line buffer to accumulate bytes until a newline */
    private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(128);

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
    public synchronized void write(byte[] buf, int off, int len)
    {
        int start = off;
        for (int i = off; i < off + len; i++) {
            if (buf[i] == '\n') {
                // Print up to a newline
                lineBuffer.write(buf, start, (i - start) + 1);
                if (fp == 0) {
                    throw new IllegalStateException("Trying to write to an invalid file handle (already closed?)");
                }
                PrismNative.PN_PrintToFile(fp, lineBuffer.toString(StandardCharsets.UTF_8));
                lineBuffer.reset();
                start = i + 1;
            }
        }
        // Buffer any remaining bytes after the last newline
        if (start < off + len) {
            lineBuffer.write(buf, start, (off + len) - start);
        }
    }

    @Override
    public synchronized void write(int b)
    {
        lineBuffer.write(b);
        if (b == '\n') {
            if (fp == 0) {
                throw new IllegalStateException("Trying to write to an invalid file handle (already closed?)");
            }
            PrismNative.PN_PrintToFile(fp, lineBuffer.toString(StandardCharsets.UTF_8));
            lineBuffer.reset();
        }
    }

    @Override
    public synchronized void flush()
    {
        if (fp == 0) {
            throw new IllegalStateException("Trying to flush an invalid file handle (already closed?)");
        }
        if (lineBuffer.size() > 0) {
            PrismNative.PN_PrintToFile(fp, lineBuffer.toString(StandardCharsets.UTF_8));
            lineBuffer.reset();
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
        flush();
        // We never close stdout
        if (stdout) {
            return;
        }
        PrismNative.PN_CloseFile(fp);
        // Set pointer to zero to indicate that the file handle is not valid anymore
        fp = 0;
    }
}
