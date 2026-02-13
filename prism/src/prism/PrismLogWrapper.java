package prism;

/**
 * Class providing a wrapper around an existing PrismLog object.
 * All method calls are passed through to the wrapped log,
 * except for {@link #close()}, so that this log can safely be closed
 * without affecting the wrapped log. This makes it easy to
 * provide temporary, close-able access to a log.
 */
public class PrismLogWrapper extends PrismLog
{
    /** The log being "wrappepd" */
    protected PrismLog log;

    /**
     * Create a new PrismLogWrapper object, wrapping the provided log.
     * @param log The log to wrap
     */
    public PrismLogWrapper(PrismLog log)
    {
        this.log = log;
    }

    @Override
    public boolean ready()
    {
        return log.ready();
    }

    @Override
    public long getFilePointer()
    {
        return log.getFilePointer();
    }

    @Override
    public void flush()
    {
        log.flush();
    }

    @Override
    public void close()
    {
        // Do nothing. In particular, do not close the wrapped log.
    }

    @Override
    public void print(boolean b)
    {
        log.print(b);
    }

    @Override
    public void print(char c)
    {
        log.print(c);
    }

    @Override
    public void print(double d)
    {
        log.print(d);
    }

    @Override
    public void print(float f)
    {
        log.print(f);
    }

    @Override
    public void print(int i)
    {
        log.print(i);
    }

    @Override
    public void print(long l)
    {
        log.print(l);
    }

    @Override
    public void print(Object obj)
    {
        log.print(obj);
    }

    @Override
    public void print(String s)
    {
        log.print(s);
    }

    @Override
    public void println()
    {
        log.println();
    }
}
