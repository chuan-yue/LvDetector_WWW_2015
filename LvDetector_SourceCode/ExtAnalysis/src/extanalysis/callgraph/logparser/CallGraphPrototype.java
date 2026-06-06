package extanalysis.callgraph.logparser;

public class CallGraphPrototype {
	private String _prototype;
	private String _file;

	public CallGraphPrototype(String funPrt) {
		String[] strs = funPrt.split("@@");
		this._prototype = strs[0];
		this._file = strs[1];
	}

	public String getPrototype() {
		return this._prototype;
	}

	public String getFileName() {
		return this._file;
	}
}
