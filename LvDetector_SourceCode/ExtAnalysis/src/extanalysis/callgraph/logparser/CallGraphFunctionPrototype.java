package extanalysis.callgraph.logparser;

public class CallGraphFunctionPrototype {
	private String _func_name;
	private String _prototype;

	public CallGraphFunctionPrototype(String n, String p) {
		this._func_name = n;
		this._prototype = p;
	}

	public String getFunctionName() {
		return this._func_name;
	}

	public String getPrototype() {
		return this._prototype;
	}

	public String toString() {
		return "PROTO@@" + this._func_name + "@@" + this._prototype;
	}

}
