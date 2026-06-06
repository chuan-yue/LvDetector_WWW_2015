/**
 * All rights reserved
 * @author rzhao
 * CS Department
 * UCCS 2013
 */

import com.google.javascript.jscomp.CallGraph;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerPass;
import com.google.javascript.jscomp.CompilerTestCase;

public class MyGraph extends CompilerTestCase {
	private CallGraph currentProcessor;

	private boolean createForwardCallGraph;
	private boolean createBackwardCallGraph;

	@Override
	protected CompilerPass getProcessor(Compiler compiler) {
		// We store the new callgraph so it can be tested later
		currentProcessor = new CallGraph(compiler, createForwardCallGraph,
				createBackwardCallGraph);

		return currentProcessor;
	}

	/*
	static final String SHARED_EXTERNS = "var ExternalFunction = function(a) {}\n"
			+ "var externalnamespace = {}\n"
			+ "externalnamespace.prop = function(){};\n";
	*/
	static final String SHARED_EXTERNS = "";
	
	public CallGraph compileAndRunBackward(String js) {
		return compileAndRun(SHARED_EXTERNS, js, false, true);
	}

	public CallGraph compileAndRunForward(String js) {
		return compileAndRun(SHARED_EXTERNS, js, true, false);
	}

	private CallGraph compileAndRun(String externs, String js, boolean forward,
			boolean backward) {

		createBackwardCallGraph = backward;
		createForwardCallGraph = forward;

		testSame(externs, js, null);

		return currentProcessor;
	}

}
