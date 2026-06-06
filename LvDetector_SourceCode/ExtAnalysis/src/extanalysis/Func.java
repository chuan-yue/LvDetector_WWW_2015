package extanalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import extanalysis.ssaparser.SSAFunc;
import extanalysis.ssaparser.SSAInst;
import extanalysis.ssaparser.SSAVarUseGraph;
import extanalysis.ssaparser.SSAVarUseGraph.VarUseEdge;

public class Func {
	public String _file_name;
	public String _function_name;

	public SSAFunc _ssa_func;
	public int[] _parameters;
	public int[] _returns;

	public HashMap<SSAInst, SumCallsite> _callsites = new HashMap<SSAInst, SumCallsite>();

	// global name - value number
	public HashMap<String[], Integer> _global_var_def_map;
	public HashMap<Integer, String[]> _var_global_use_map;

	// lexical string - value number
	public HashMap<String[], Integer> _lexical_var_def_map;
	public HashMap<Integer, String[]> _var_lexical_use_map;

	public List<Var> _function_var = new ArrayList<Var>();

	public List<VarPair> _function_sum = new ArrayList<VarPair>();

	public Func(SSAVarUseGraph g, HashMap<String, List<String>> sum) {
		this._file_name = g.getFileName();
		this._function_name = g.getFuncName();
		this._ssa_func = g.getFunc();
		this._parameters = _ssa_func.getParameters();
		this._returns = _ssa_func.getReturns();

		// we also need to get all callsite info in this function
		for (VarUseEdge edge : g.getFlowEdges().values()) {
			if (edge.isCall()) {
				String call = edge.getCall().getCallsiteStr();
				String line = edge.getCall().getCallsiteLine();
				int[] arguments = edge.getInstruction().getArguments();
				int assigned = edge.getTo();
				String callToFile = edge.getCall().getCalleeFile();
				String callToFunc = edge.getCall().getCalleeFuncProto();
				if (!this._callsites.containsKey(edge.getInstruction())) {
					this._callsites.put(edge.getInstruction(), new SumCallsite(call, line, arguments, assigned, callToFile, callToFunc));
				}
			}
		}

		this._global_var_def_map = this._ssa_func.getGlobalVarDefMap();
		this._var_global_use_map = this._ssa_func.getVarGlobalUseMap();
		this._lexical_var_def_map = this._ssa_func.getLexicalVarDefMap();
		this._var_lexical_use_map = this._ssa_func.getVarLexicalUseMap();

		for (int vertex : g.getVertexes()) {
			String var = g.getFunc().getVarLocalNameMap().get(vertex);
			Var tmp = new Var(this._file_name, this._function_name, String.valueOf(vertex), var);
			this._function_var.add(tmp);
		}

		for (String str : sum.keySet()) {
			String str1 = str.substring(1, str.length() - 1).replace(" ", "");
			String[] vars = str1.split(":");
			String fvar = g.getFunc().getVarLocalNameMap().get(vars[0]);
			String tvar = g.getFunc().getVarLocalNameMap().get(vars[1]);

			VarPair tmp = new VarPair(fvar, vars[0], this._file_name, this._function_name, tvar, vars[1], this._file_name, this._function_name,
					sum.get(str));
			this._function_sum.add(tmp);
		}
	}

	public boolean isReturnedFunc() {
		if (this._returns.length > 0) {
			return true;
		} else {
			return false;
		}
	}

	public int isGlobalDefined(String[] global) {
		for (String[] keys : this._global_var_def_map.keySet()) {
			if (keys[0].equals(global[0]) && keys[1].equals(global[1]) && keys[2].equals(global[2])) {
				return this._global_var_def_map.get(keys);
			}
		}
		return -1;
	}

	public int isLexicalDefined(String[] lexical) {
		for (String[] keys : this._lexical_var_def_map.keySet()) {
			if (keys[0].equals(lexical[0]) && keys[1].equals(lexical[1]) && keys[2].equals(lexical[2])) {
				return this._lexical_var_def_map.get(keys);
			}
		}
		return -1;
	}

	public class SumCallsite {
		public String _call;
		public String _line;
		public int[] _arguments;
		public int _assigned;
		public String _to_file;
		public String _to_func;

		public SumCallsite(String c, String l, int[] arg, int ass, String fi, String fu) {
			this._call = c;
			this._line = l;
			this._arguments = arg;
			this._assigned = ass;
			this._to_file = fi;
			this._to_func = fu;
		}
	}
}
