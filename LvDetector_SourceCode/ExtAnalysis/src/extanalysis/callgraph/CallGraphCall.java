package extanalysis.callgraph;

public class CallGraphCall {
	private String _caller_file;
	private String _caller_func_proto;
	private String _callee_file;
	private String _callee_func_proto;

	private String _callsite_file;
	private String _callsite_line;
	private String _callsite_from;
	private String _callsite_str;

	public CallGraphCall(String callerFile, String callerFuncProto, String calleeFile, String calleeFuncProto, String callsiteFile, String callsiteLine,
			String callsiteFrom, String callsiteStr) {
		this._caller_file = callerFile;
		this._caller_func_proto = callerFuncProto;
		this._callee_file = calleeFile;
		this._callee_func_proto = calleeFuncProto;

		this._callsite_file = callsiteFile;
		this._callsite_line = callsiteLine;
		this._callsite_from = callsiteFrom;
		this._callsite_str = callsiteStr;
	}

	public CallGraphCall(String str) {
		String[] contents = str.split(",");
		if (contents.length == 8) {
			this._caller_file = contents[0];
			this._caller_func_proto = contents[1];
			this._callee_file = contents[2];
			this._callee_func_proto = contents[3];

			this._callsite_file = contents[4];
			this._callsite_line = contents[5];
			this._callsite_from = contents[6];
			this._callsite_str = contents[7];
		}
	}

	public String getCallerFuncProto() {
		return this._caller_func_proto;
	}

	public String getCalleeFuncProto() {
		return this._callee_func_proto;
	}

	public String getCallerFile() {
		return this._caller_file;
	}

	public String getCalleeFile() {
		return this._callee_file;
	}

	public String getCallsiteFile() {
		return this._callsite_file;
	}

	public String getCallsiteLine() {
		return this._callsite_line;
	}

	public String getCallsiteFrom() {
		return this._callsite_from;
	}

	public String getCallsiteStr() {
		return this._callsite_str;
	}

	public String toString() {
		String ret = this._caller_file + "," + this._caller_func_proto + "," + this._callee_file + "," + this._callee_func_proto + "," +
		this._callsite_file + "," + this._callsite_line + "," + this._callsite_from + "," + this._callsite_str + "\n";
		return ret;
	}
	
	public boolean equals(CallGraphCall c){
		return this.toString().equals(c.toString());
	}
}
