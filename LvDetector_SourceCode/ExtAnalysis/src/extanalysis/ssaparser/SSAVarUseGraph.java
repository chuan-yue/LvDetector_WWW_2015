package extanalysis.ssaparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import extanalysis.AnalysisUtil;
import extanalysis.callgraph.CallGraphCall;

public class SSAVarUseGraph {

	private SSAFunc _func;

	private String _file_name;
	private String _func_name;

	private ArrayList<Integer> _vertexes = new ArrayList<Integer>();
	private HashMap<Integer, VarUseEdge> _operation_edges = new HashMap<Integer, VarUseEdge>();
	private HashMap<Integer, VarUseEdge> _value_flow_edges = new HashMap<Integer, VarUseEdge>();

	public SSAVarUseGraph(SSAFunc func) {

		this._func = func;
		this._file_name = func.getFileName();
		this._func_name = func.getFuncName();

		HashMap<Integer, SSAInst> instructions = func.getInstructions();

		List<Integer> tos = new ArrayList<Integer>();
		for (int i = 0; i < instructions.size(); i++) {
			SSAInst inst = instructions.get(i);
			String op = inst.getOp();
			if (op == null) {
				continue;
			}
			if (op.contains("PUT_FIELD") || op.contains("PUT_VALUE_TO_FIELD")) {
				tos.add(inst.getLhsVar());
			}
		}

		List<Integer> needToBeReversed = new ArrayList<Integer>();
		for (int j = 0; j < instructions.size(); j++) {
			SSAInst inst = instructions.get(j);
			String op = inst.getOp();
			if (op == null) {
				continue;
			}
			if (op.equals("LEXICAL") || op.equals("GLOBAL")) {
				int lhsVar = inst.getLhsVar();
				if (tos.contains(lhsVar)) {
					needToBeReversed.add(j);
				}
			}
		}

		for (int k = 0; k < instructions.size(); k++) {
			SSAInst inst = instructions.get(k);
			String op = inst.getOp();
			if (op == null) {
				continue;
			}

			int lhsVar = inst.getLhsVar();
			List<Integer> rhsVars = inst.getRhsVars();
			if (rhsVars == null || rhsVars.size() == 0) {
				continue;
			}

			for (Integer rhsVar : rhsVars) {
				if (!_vertexes.contains(rhsVar)) {
					_vertexes.add(rhsVar);
				}
			}
			if (!_vertexes.contains(lhsVar)) {
				_vertexes.add(lhsVar);
			}
			for (Integer rhsVar : rhsVars) {
				VarUseEdge tmp = null;
				if (needToBeReversed.contains(k)) {
					tmp = new VarUseEdge(lhsVar, rhsVar, func.getCallForOp(op), op, inst.getOpPrototype(), func, inst);
				} else {
					tmp = new VarUseEdge(rhsVar, lhsVar, func.getCallForOp(op), op, inst.getOpPrototype(), func, inst);
				}
				_operation_edges.put(this._operation_edges.size(), tmp);
			}
		}
	}

	private String[] getOpAndField(String op) {
		String[] ret = new String[2];
		if (op.contains("[")) {
			ret[0] = op.substring(0, op.indexOf("["));
			ret[1] = op.substring(op.indexOf("[") + 1, op.indexOf("]"));
		} else {
			ret[0] = op;
			ret[1] = null;
		}
		return ret;
	}

	public void recompute() {
		// valuenumber, list of op
		HashMap<Integer, List<String>> used = new HashMap<Integer, List<String>>();
		List<VarUseEdge> waitingProtoEdge = new ArrayList<VarUseEdge>();
		for (int i = 0; i < this._operation_edges.size(); i++) {
			VarUseEdge vue = this._operation_edges.get(i);
			if (vue._op == null) {
				// for edge with a callsite
				this._value_flow_edges.put(this._value_flow_edges.size(), vue);
				continue;
			}

			if (AnalysisUtil.getInstance().IS_VARUSEGRAPH_RECOMPUTE) {
				if (!vue._op.equals("PROTOTYPE")) {

					if (!vue._op.equals("CONSTRUCT")) {
						this._value_flow_edges.put(this._value_flow_edges.size(), vue);
					}

					if (vue._op.contains("PUT_FIELD") || vue._op.contains("PUT_VALUE_TO_FIELD")) {
						if (used.containsKey(vue._to)) {
							used.get(vue._to).add(getOpAndField(vue._op)[1]);
						} else {
							List<String> tmp = new ArrayList<String>();
							tmp.add(getOpAndField(vue._op)[1]);
							used.put(vue._to, tmp);
						}
					} else if (vue._op.contains("GET_FIELD") || vue._op.contains("GET_VALUE_FROM_FIELD")) {
						// first get all related waiting edge
						for (VarUseEdge wait : waitingProtoEdge) {
							if (wait._to == vue._from) {
								if (used.containsKey(wait._from)) {
									if (used.get(wait._from).contains(getOpAndField(vue._op)[1])) {
										this._value_flow_edges.put(this._value_flow_edges.size(), wait);
									}
								}
							}
						}
					} else {
						// for now do nothing
					}
				} else {
					waitingProtoEdge.add(vue);
				}
			} else {
				this._value_flow_edges.put(this._value_flow_edges.size(), vue);
			}
		}

		// Then for each edge in _value_flow_edges
		// We check with ExtAnalysisUtil.getInstance()._need_further_process_funcs
		HashMap<Integer, String> tmp = new HashMap<Integer, String>();
		List<VarUseEdge> toAdd = new ArrayList<VarUseEdge>();
		for (int i = 0; i < this._value_flow_edges.size(); i++) {
			VarUseEdge vue = this._value_flow_edges.get(i);
			if (vue.getOpProto() == null) {
				continue;
			}
			for (String[] strs : AnalysisUtil.getInstance()._need_further_process_funcs) {
				if (vue.getOpProto().equals(strs[0])) {
					tmp.put(vue._from, strs[0]);
				} else if (vue.getOpProto().equals(strs[1])) {
					for (int newFrom : tmp.keySet()) {
						String value = tmp.get(newFrom);
						if (value.equals(strs[0])) {
							VarUseEdge newEdge = new VarUseEdge(newFrom, vue._to, null, vue._op, vue._op_proto, vue._func, null);
							toAdd.add(newEdge);
						}
					}
				}
			}
		}
		for (VarUseEdge edgeToAdd : toAdd) {
			this._value_flow_edges.put(this._value_flow_edges.size(), edgeToAdd);
		}
	}

	public SSAFunc getFunc() {
		return this._func;
	}

	public String getFileName() {
		return this._file_name;
	}

	public String getFuncName() {
		return this._func_name;
	}

	public ArrayList<Integer> getVertexes() {
		return this._vertexes;
	}

	public HashMap<Integer, VarUseEdge> getOperationEdges() {
		return this._operation_edges;
	}

	public HashMap<Integer, VarUseEdge> getFlowEdges() {
		return this._value_flow_edges;
	}

	public String getLocalName(int index) {
		return this._func.getVarLocalNameMap().get(index);
	}

	public class VarUseEdge {

		private int _from;
		private int _to;

		private CallGraphCall _call = null;
		private String _op;
		private String _op_proto;

		private SSAFunc _func;
		private SSAInst _inst;

		public VarUseEdge(int from, int to, CallGraphCall c, String op, String opp, SSAFunc func, SSAInst inst) {
			this._from = from;
			this._to = to;
			this._call = c;
			this._op = op;
			this._op_proto = opp;
			this._func = func;
			this._inst = inst;
		}

		public SSAInst getInstruction() {
			return this._inst;
		}

		public SSAFunc getContainedFunction() {
			return this._func;
		}

		public int getFrom() {
			return this._from;
		}

		public int getTo() {
			return this._to;
		}

		public String getOp() {
			return this._op;
		}

		public String getOpProto() {
			return this._op_proto;
		}

		public boolean isCall() {
			return this._call != null;
		}

		public CallGraphCall getCall() {
			return this._call;
		}
	}
}
