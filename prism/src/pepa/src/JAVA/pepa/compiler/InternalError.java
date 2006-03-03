package pepa.compiler;

public class InternalError extends Exception {
    private static final String flag = "\n[><]  ----------->   ";

    protected InternalError(String s) {
	super(flag + flag + s + flag);
    }

}
