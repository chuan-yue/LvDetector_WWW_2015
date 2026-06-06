package extanalysis.callgraph.logparser;

public class CallGraphCallsite {
	private String _caller;
	private String _callee;
	private String _file;
	private int _lineno;

	public CallGraphCallsite(String caller, String callee, String file, String line) {
		this._caller = caller;
		this._callee = callee;
		this._file = file;
		this._lineno = Integer.valueOf(line).intValue();
	}

	public String getCaller() {
		return this._caller;
	}

	public String getCallee() {
		return this._callee;
	}

	public int getLineNo() {
		return this._lineno;
	}

	public String getFileName() {
		return this._file;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("CALLSITE@@");
		sb.append(this._caller);
		sb.append("@@");
		sb.append(this._callee);
		sb.append("@@");
		sb.append(this._file);
		sb.append("@@");
		sb.append(this._lineno);
		return sb.toString();
	}

	public boolean equalsTo(CallGraphCallsite c) {
		if (this._caller.equals(c._caller) && this._callee.equals(c._callee) && this._lineno == c._lineno && this._file.equals(c._file)) {
			return true;
		}
		return false;
	}
}
