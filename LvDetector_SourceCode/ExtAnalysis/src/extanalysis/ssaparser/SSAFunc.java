package extanalysis.ssaparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;

import extanalysis.callgraph.CallGraph;
import extanalysis.callgraph.CallGraphCall;

public class SSAFunc {
	private CallGraph _call_graph;

	private IR _ir;
	private SymbolTable _st;
	private String _file_name;
	private String _func_name;

	// global name - value number
	private HashMap<String[], Integer> _global_var_def_map = new HashMap<String[], Integer>();
	private HashMap<Integer, String[]> _var_global_use_map = new HashMap<Integer, String[]>();

	// lexical string - value number
	private HashMap<String[], Integer> _lexical_var_def_map = new HashMap<String[], Integer>();
	private HashMap<Integer, String[]> _var_lexical_use_map = new HashMap<Integer, String[]>();
	// value number - constructor
	private HashMap<Integer, Integer> _var_constructor_map = new HashMap<Integer, Integer>();
	// value number - prototype value number
	private HashMap<Integer, Integer> _var_prototype_map = new HashMap<Integer, Integer>();
	// operation - callsite
	private HashMap<String, CallGraphCall> _operation_callsite_map = new HashMap<String, CallGraphCall>();

	// value number - local var name
	private HashMap<Integer, String> _var_localname_map = new HashMap<Integer, String>();

	// returned var list
	private ArrayList<Integer> _returned_var_list = new ArrayList<Integer>();
	// index - parameter
	private HashMap<Integer, Integer> _index_parameter_map = new HashMap<Integer, Integer>();

	// invoke - prototype of invoke
	private HashMap<String, String> _invoke_prototype_map = new HashMap<String, String>();

	// instruction list
	private HashMap<Integer, SSAInst> _instructions = new HashMap<Integer, SSAInst>();

	public SSAFunc(CallGraph cg, String filename, IR ir) {
		this._call_graph = cg;

		this._file_name = filename;
		this._ir = ir;
		this._func_name = _ir.getMethod().getDeclaringClass().getName().toString();
		this._func_name = this._func_name.substring(this._func_name.lastIndexOf("/") + 1);
		this._st = ir.getSymbolTable();
		parseParameter();
		parseFuncIR();
	}

	public HashMap<Integer, SSAInst> getInstructions() {
		return this._instructions;
	}

	public String getFileName() {
		return this._file_name;
	}

	public String getFuncName() {
		return this._func_name;
	}

	public HashMap<Integer, String> getVarLocalNameMap() {
		return this._var_localname_map;
	}

	public boolean isCall(String op) {
		return this._operation_callsite_map.containsKey(op);
	}

	public CallGraphCall getCallForOp(String op) {
		// Here link the operation() to a callsite
		return this._operation_callsite_map.get(op);
	}

	public SSAVarUseGraph getVarUseGraph() {
		return new SSAVarUseGraph(this);
	}

	public String getIR() {
		return this._ir.toString();
	}

	private void parseParameter() {
		int paras[] = _ir.getParameterValueNumbers();
		for (int i = 2; i < paras.length; i++) {
			this._index_parameter_map.put(i - 2, paras[i]);
		}
	}

	public int[] getParameters() {
		int ret[] = new int[this._index_parameter_map.values().size()];
		for (int i = 0; i < this._index_parameter_map.values().size(); i++) {
			ret[i] = getIthParameter(i);
		}
		return ret;
	}

	public int getParameterNum() {
		return this._index_parameter_map.values().size();
	}

	public int getIthParameter(int ith) {
		if (this._index_parameter_map.containsKey(ith)) {
			return this._index_parameter_map.get(ith);
		} else {
			return -1;
		}
	}

	public boolean isReturnedFunc() {
		if (this._returned_var_list.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	public ArrayList<Integer> getReturnedValueNumber() {
		return this._returned_var_list;
	}

	public int[] getReturns() {
		int[] ret = new int[this._returned_var_list.size()];
		for (int i = 0; i < this._returned_var_list.size(); i++) {
			ret[i] = this._returned_var_list.get(i);
		}
		return ret;
	}

	public HashMap<String[], Integer> getGlobalVarDefMap() {
		return this._global_var_def_map;
	}

	public HashMap<Integer, String[]> getVarGlobalUseMap() {
		return this._var_global_use_map;
	}

	public HashMap<String[], Integer> getLexicalVarDefMap() {
		return this._lexical_var_def_map;
	}

	public HashMap<Integer, String[]> getVarLexicalUseMap() {
		return this._var_lexical_use_map;
	}

	private void parseFuncIR() {
		SSAInstruction[] irIns = _ir.getInstructions();
		// go through each instruction
		for (int i = 0; i < irIns.length; i++) {
			SSAInstruction ins = irIns[i];
			if (ins == null) {
				continue;
			}

			this._instructions.put(this._instructions.size(), new SSAInst(_call_graph, _file_name, _func_name, ins, i, _st, _ir, _global_var_def_map,
					_var_global_use_map, _var_localname_map, _lexical_var_def_map, _var_lexical_use_map, _var_constructor_map, _var_prototype_map,
					_invoke_prototype_map, _operation_callsite_map, _returned_var_list));
		}

		@SuppressWarnings("unchecked")
		Iterator<SSAInstruction> it = (Iterator<SSAInstruction>) _ir.iteratePhis();
		while (it.hasNext()) {
			SSAInstruction ins = it.next();
			if (ins == null) {
				continue;
			}
			this._instructions.put(this._instructions.size(), new SSAInst(_call_graph, _file_name, _func_name, ins, -1, _st, _ir,
					_global_var_def_map, _var_global_use_map, _var_localname_map, _lexical_var_def_map, _var_lexical_use_map, _var_constructor_map,
					_var_prototype_map, _invoke_prototype_map, _operation_callsite_map, _returned_var_list));
		}
	}

	@SuppressWarnings("unused")
	private void printSymbolTable() {
		System.out.println("SymbolTable:");
		int symbolNum = _st.getMaxValueNumber();
		for (int i = 0; i < symbolNum; i++) {
			System.out.println(_st.getValueString(i));
		}
		System.out.println();
	}
}
