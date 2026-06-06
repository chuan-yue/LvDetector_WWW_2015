package extanalysis.ssaparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ibm.wala.cast.ir.ssa.AbstractReflectiveGet;
import com.ibm.wala.cast.ir.ssa.AbstractReflectivePut;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.js.ssa.PrototypeLookup;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.ssa.ReflectiveMemberAccess;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.Value;

import extanalysis.callgraph.CallGraph;
import extanalysis.callgraph.CallGraphCall;

public class SSAInst {
	private CallGraph _call_graph;
	
	private String _func_name;
	private String _file_name;
	private SSAInstruction _ssa_inst;
	private int _ssa_inst_index;
	private String _line_no = "-1";
	private SymbolTable _st;
	private IR _ir;

	private HashMap<String[], Integer> _global_var_def_map;
	private HashMap<Integer, String[]> _var_global_use_map;

	private HashMap<String[], Integer> _lexical_var_def_map;
	private HashMap<Integer, String[]> _var_lexical_use_map;

	private HashMap<Integer, Integer> _var_constructor_map;
	private HashMap<Integer, Integer> _var_prototype_map;
	private HashMap<String, CallGraphCall> _operation_callsite_map;

	private HashMap<Integer, String> _var_localname_map;

	private HashMap<Integer, Integer> _arg_index_map = new HashMap<Integer, Integer>();
	private ArrayList<Integer> _returned_var_list;
	private int _lhs_var = -1;
	private List<Integer> _rhs_vars = null;
	private String _op = null;

	private HashMap<String, String> _invoke_prototype_map;

	public SSAInst(CallGraph cg, String fileName, String funcName, SSAInstruction ssaInst, int index, SymbolTable st, IR ir, HashMap<String[], Integer> gd,
			HashMap<Integer, String[]> gu, HashMap<Integer, String> vn, HashMap<String[], Integer> ld, HashMap<Integer, String[]> lu,
			HashMap<Integer, Integer> cm, HashMap<Integer, Integer> pm, HashMap<String, String> ipm, HashMap<String, CallGraphCall> cl,
			ArrayList<Integer> rv) {
		this._call_graph = cg;
		
		this._file_name = fileName;
		this._func_name = funcName;
		this._ssa_inst = ssaInst;
		this._ssa_inst_index = index;
		this._st = st;
		this._ir = ir;

		this._global_var_def_map = gd;
		this._var_global_use_map = gu;
		this._var_localname_map = vn;
		this._lexical_var_def_map = ld;
		this._var_lexical_use_map = lu;
		this._var_constructor_map = cm;
		this._var_prototype_map = pm;
		this._invoke_prototype_map = ipm;
		this._operation_callsite_map = cl;
		this._returned_var_list = rv;

		try {
			String callInfo = _ir.instructionPosition(this._ssa_inst_index);
			this._line_no = SSAUtil.getInstance().get_lineNumber(callInfo);
		} catch (Exception e) {

		}

		parseSSAInst();
	}

	public int getLhsVar() {
		return this._lhs_var;
	}

	public List<Integer> getRhsVars() {
		return this._rhs_vars;
	}

	public List<Integer> getAllVars() {
		List<Integer> ret = new ArrayList<Integer>();
		if (this._rhs_vars != null) {
			for (int var : this._rhs_vars) {
				ret.add(var);
			}
		}
		if (this._lhs_var != -1) {
			ret.add(this._lhs_var);
		}
		return ret;
	}

	public String getVarName(int valuenumber) {
		return this._var_localname_map.get(valuenumber);
	}

	public String getOp() {
		return this._op;
	}

	public String getOpPrototype() {
		return this._invoke_prototype_map.get(this._op);
	}

	public String getLineNo() {
		return this._line_no;
	}

	public int getArgIndex(int arg) {
		for (int i = 0; i < this._arg_index_map.size(); i++) {
			if (this._arg_index_map.get(i).equals(arg)) {
				return i;
			}
		}
		return -1;
	}

	public int[] getArguments() {
		int[] ret = new int[this._arg_index_map.keySet().size()];
		for (int i = 0; i < this._arg_index_map.size(); i++) {
			ret[i] = this._arg_index_map.get(i);
		}
		return ret;
	}

	public boolean isCallsite() {
		return SSAAbstractInvokeInstruction.class.isInstance(_ssa_inst);
	}

	private void parseSSAInst() {
		preprocessVars();
		parseInstruction();
	}

	private void preprocessVars() {
		for (int iDef = 0; iDef < this._ssa_inst.getNumberOfDefs(); iDef++) {
			int valueNumber = -1;
			valueNumber = this._ssa_inst.getDef(iDef);
			//if (this._valuenumber_var_map.containsKey(valueNumber)) {
			//	continue;
			//}
			if (valueNumber != -1) {
				// this._def_valuenumbers.add(valueNumber);
				String var = localize(valueNumber);
				if (var != null) {
					var = var.replace("\n", "");
					this._var_localname_map.put(valueNumber, var);
				}
			}
		}
		for (int iUse = 0; iUse < this._ssa_inst.getNumberOfUses(); iUse++) {
			int valueNumber = -1;
			valueNumber = this._ssa_inst.getUse(iUse);
			//if (this._valuenumber_var_map.containsKey(valueNumber)) {
			//	continue;
			//}
			if (valueNumber != -1) {
				// this._use_valuenumbers.add(valueNumber);
				String var = localize(valueNumber);
				if (var != null) {
					var = var.replace("\n", "");
					this._var_localname_map.put(valueNumber, var);
				}
			}
		}
	}

	private void parseInstruction() {

		if (AstGlobalRead.class.isInstance(_ssa_inst)) {
			int def = _ssa_inst.getDef();
			String gName = ((AstGlobalRead) _ssa_inst).getGlobalName();
			if (!gName.contains("__WALA__int3rnal__global") && !gName.contains("$$undefined")) {
				gName = gName.replace("global ", "");
				this._var_localname_map.put(def, gName.replace("\n", ""));

				String[] tmp = new String[3];
				tmp[0] = this._file_name; // file
				tmp[1] = this._func_name; // function
				tmp[2] = gName; // variable
				this._var_global_use_map.put(def, tmp);

				int a = isGlobalInDefMap(tmp);
				if (a != -1) {
					_lhs_var = def;
					_rhs_vars = new ArrayList<Integer>();
					_rhs_vars.add(a);
					_op = "GLOBAL";
				}
			}
		} else if (AstGlobalWrite.class.isInstance(_ssa_inst)) {
			int use = _ssa_inst.getUse(0);
			String gName = ((AstGlobalWrite) _ssa_inst).getGlobalName();
			if (!gName.contains("__WALA__int3rnal__global") && !gName.contains("$$undefined")) {
				gName = gName.replace("global ", "");
				this._var_localname_map.put(use, gName.replace("\n", ""));

				String[] tmp = new String[3];
				tmp[0] = this._file_name; // file
				tmp[1] = this._func_name; // function
				tmp[2] = gName; // variable		
				putToGlobalDefMap(tmp, use);
			}
		} else if (PrototypeLookup.class.isInstance(_ssa_inst)) {
			int def = _ssa_inst.getDef(0);
			int use = _ssa_inst.getUse(0);
			this._var_localname_map.put(def, this._var_localname_map.get(use));
			this._var_prototype_map.put(def, use);

			_lhs_var = def;
			_rhs_vars = new ArrayList<Integer>();
			_rhs_vars.add(use);
			_op = "PROTOTYPE";
		} else if (AstLexicalRead.class.isInstance(_ssa_inst)) {
			for (int i = 0; i < _ssa_inst.getNumberOfDefs(); i++) {
				int def = _ssa_inst.getDef(i);
				String gName = ((AstLexicalRead) _ssa_inst).getAccess(i).variableName.replace("\n", "");
				this._var_localname_map.put(def, gName);

				String[] tmp = new String[3];
				tmp[0] = this._file_name; // file
				tmp[1] = this._func_name; // function
				tmp[2] = gName; // variable		
				this._var_lexical_use_map.put(def, tmp);

				int a = isLexicalInDefMap(tmp);
				if (a != -1) {
					_lhs_var = def;
					_rhs_vars = new ArrayList<Integer>();
					_rhs_vars.add(a);
					_op = "LEXICAL";
				}
			}
		} else if (AstLexicalWrite.class.isInstance(_ssa_inst)) {
			for (int i = 0; i < _ssa_inst.getNumberOfUses(); i++) {
				int use = _ssa_inst.getUse(i);
				String gName = ((AstLexicalWrite) _ssa_inst).getAccess(i).variableName.replace("\n", "");
				this._var_localname_map.put(use, gName);

				String[] tmp = new String[3];
				tmp[0] = this._file_name; // file
				tmp[1] = this._func_name; // function
				tmp[2] = gName; // variable
				putToLexicalDefMap(tmp, use);
			}
		} else if (SSAGetInstruction.class.isInstance(_ssa_inst)) {
			int use = _ssa_inst.getUse(0);
			int def = _ssa_inst.getDef(0);
			String field = parseGlobalName((SSAFieldAccessInstruction) _ssa_inst);
			String gName = _var_localname_map.get(use) + "[" + field + "]";
			this._var_localname_map.put(def, gName.replace("\n", ""));

			_lhs_var = def;
			_rhs_vars = new ArrayList<Integer>();
			_rhs_vars.add(use);
			_op = "GET_FIELD[" + field + "]";
		} else if (SSAPutInstruction.class.isInstance(_ssa_inst)) {
			int use = _ssa_inst.getUse(0);
			int def = _ssa_inst.getDef(0);
			String field = parseGlobalName((SSAFieldAccessInstruction) _ssa_inst);
			String gName = _var_localname_map.get(use) + "[" + field + "]";
			this._var_localname_map.put(def, gName.replace("\n", ""));

			_lhs_var = def;
			_rhs_vars = new ArrayList<Integer>();
			_rhs_vars.add(use);
			_op = "PUT_FIELD[" + field + "]";
		} else if (AbstractReflectiveGet.class.isInstance(_ssa_inst)) {
			int use = _ssa_inst.getUse(0);
			int def = _ssa_inst.getDef(0);
			int ref = ((ReflectiveMemberAccess) _ssa_inst).getMemberRef();

			String field;
			if (this._var_localname_map.containsKey(ref)) {
				field = this._var_localname_map.get(ref).replace("#", "");
			} else {
				field = "?";
			}
			String gName = _var_localname_map.get(use) + "[" + field + "]";
			this._var_localname_map.put(def, gName.replace("\n", ""));

			// put a field's value into a variable
			_lhs_var = parseLhsVar(_ssa_inst);
			_rhs_vars = parseRhsVars(_ssa_inst);
			_op = "GET_VALUE_FROM_FIELD[" + field + "]";
		} else if (AbstractReflectivePut.class.isInstance(_ssa_inst)) {
			int use = _ssa_inst.getUse(0);
			int def = _ssa_inst.getDef(0);
			int ref = ((ReflectiveMemberAccess) _ssa_inst).getMemberRef();

			String field;
			if (this._var_localname_map.containsKey(ref)) {
				field = this._var_localname_map.get(ref).replace("#", "");
			} else {
				field = "?";
			}
			String gName = _var_localname_map.get(use) + "[" + field + "]";
			this._var_localname_map.put(def, gName.replace("\n", ""));

			// put a variable's value into a field
			_lhs_var = parseLhsVar(_ssa_inst);
			_rhs_vars = parseRhsVars(_ssa_inst);
			_op = "PUT_VALUE_TO_FIELD[" + field + "]";
		} else if (SSAAbstractInvokeInstruction.class.isInstance(_ssa_inst)) {
			if (((SSAAbstractInvokeInstruction) _ssa_inst).getCallSite().getDeclaredTarget().equals(JavaScriptMethods.ctorReference)) {
				// This is an constructor
				int def = _ssa_inst.getDef();
				int use = _ssa_inst.getUse(0);
				this._var_constructor_map.put(def, use);

				_lhs_var = def;
				_rhs_vars = new ArrayList<Integer>();
				_rhs_vars.add(use);
				_op = "CONSTRUCT";
			} else if (((SSAAbstractInvokeInstruction) _ssa_inst).getCallSite().getDeclaredTarget().equals(JavaScriptMethods.dispatchReference)) {
				// This is a method call
				_lhs_var = parseLhsVar(_ssa_inst);
				_rhs_vars = parseRhsVars(_ssa_inst);

				int method = parseInvokeMethod(_ssa_inst);
				_op = this._var_localname_map.get(method).replace("#", "").replace("\n", "");

				int object = parseObject(_ssa_inst);

				if (this._var_constructor_map.containsKey(object)) {
					int tmpCons = this._var_constructor_map.get(object);
					if (this._var_localname_map.get(tmpCons).equals("Array")) {
						// This is an array operation
						if (_op.equals("push") || _op.equals("unshift")) {
							_lhs_var = object;
							_op = "ARRAYOP";
						} else if (_op.equals("concat") || _op.equals("join") || _op.equals("shift") || _op.equals("slice") || _op.equals("toString")
								|| _op.equals("valueOf")) {
							_rhs_vars.add(object);
							_op = "ARRAYOP";
						} else {
							_lhs_var = -1;
							_rhs_vars = null;
							_op = null;
						}
					}
				} else {
					if (_op.equals("split") || _op.equals("concat") || _op.equals("slice") || _op.equals("substring") || _op.equals("substr")
							|| _op.equals("toLocaleLowerCase") || _op.equals("toLowerCase") || _op.equals("toUpperCase")
							|| _op.equals("toLocaleUpperCase") || _op.equals("toString") || _op.equals("valueOf") || _op.equals("replace")
							|| _op.equals("match")) {
						// This is string operation
						_rhs_vars.add(object);
						_op = "STRINGOP";
					} else if (_op.equals("setAttribute")) {
						_lhs_var = object;
						int field = _rhs_vars.remove(0);
						String fieldName = this._var_localname_map.get(field).replace("#", "");
						_op = "SETATTRIBUTE[" + fieldName + "]";
					} else {
						// This is just a method call
						String objName = this._var_localname_map.get(object);

						String invokeStr = _op;
						String fullInvoke = "";
						if (objName != null) {
							fullInvoke = objName + "." + invokeStr;
						}
						_op = fullInvoke + "()";

						if (!this._var_localname_map.containsKey(_lhs_var)) {
							this._var_localname_map.put(_lhs_var, _op);
						}

						CallGraphCall foundCall = this._call_graph.getReferenceCall(this._file_name, this._func_name, this._line_no,
								fullInvoke);
						if (foundCall != null) {
							// we do not need to process prototype for this object if there is a callsite
							_operation_callsite_map.put(_op, foundCall);
						} else {
							// this call has no callsite
							String proto_op = null;

							String constructorName;
							if (this._invoke_prototype_map.containsKey(objName)) {
								constructorName = this._invoke_prototype_map.get(objName);
							} else {
								int prototype_var = object;

								// first look at if it is a lexical related
								if (this._var_lexical_use_map.keySet().contains(prototype_var)) {
									prototype_var = isLexicalInDefMap(this._var_lexical_use_map.get(prototype_var));
								}

								// second look at if it is a prototype lookup related
								// so far prototype is related to field reference
								if (this._var_prototype_map.containsKey(prototype_var)) {
									prototype_var = this._var_prototype_map.get(prototype_var);
								}

								// third look at if there is an constructor
								if (this._var_constructor_map.containsKey(prototype_var)) {
									prototype_var = this._var_constructor_map.get(prototype_var);
								}

								// finally we get it!
								constructorName = this._var_localname_map.get(prototype_var);
							}

							if (constructorName != null) {
								proto_op = constructorName + "." + invokeStr + "()";
							}

							// here we record the invokation's prototype for later analyze the sink points
							// sink point could not be implemented in extensions, it must be in library
							if (proto_op != null) {
								this._invoke_prototype_map.put(_op, proto_op);
								if (this._var_localname_map.get(_lhs_var) != null) {
									this._invoke_prototype_map.put(this._var_localname_map.get(_lhs_var), proto_op);
								}
							}
						}
					}
				}
			} else {
				// This is a function call
				int invoke = parseInvokeMethod(_ssa_inst);
				if (_var_localname_map.containsKey(invoke)) {
					_lhs_var = parseLhsVar(_ssa_inst);
					_rhs_vars = parseRhsVars(_ssa_inst);
					_op = _var_localname_map.get(invoke).replace("#", "").replace("\n", "");

					String invokeStr = _op;
					_op = invokeStr + "()";

					if (!this._var_localname_map.containsKey(_lhs_var)) {
						this._var_localname_map.put(_lhs_var, _op);
					}

					CallGraphCall foundCall = this._call_graph.getReferenceCall(this._file_name, this._func_name, this._line_no, invokeStr);
					if (foundCall != null) {
						_operation_callsite_map.put(_op, foundCall);
					} else {
						// do nothing
					}
				}
			}
		} else if (SSAReturnInstruction.class.isInstance(_ssa_inst)) {
			this._returned_var_list.add(parseReturnVar((SSAReturnInstruction) _ssa_inst));
		} else if (SSABinaryOpInstruction.class.isInstance(_ssa_inst)) {
			// Followings instructions are for value transformations
			_lhs_var = parseLhsVar(_ssa_inst);
			_rhs_vars = parseRhsVars(_ssa_inst);
			_op = "BINARY_OP";
		} else if (SSAPhiInstruction.class.isInstance(_ssa_inst)) {
			// Followings instructions are for value transformations
			_lhs_var = parseLhsVar(_ssa_inst);
			_rhs_vars = parseRhsVars(_ssa_inst);
			_op = "PHI";
		} else if (SSAUnaryOpInstruction.class.isInstance(_ssa_inst)) {
			// Followings instructions are for value transformations
			_lhs_var = parseLhsVar(_ssa_inst);
			_rhs_vars = parseRhsVars(_ssa_inst);
			_op = "UNARY_OP";
		}

	}

	private String localize(Integer valueNumber) {
		String li = getSymbolValue(valueNumber);
		if (li == null) {
			li = getLocalName(valueNumber);
		}
		//if (li != null && li.substring(0, 1).equals("#")) {
		//	li = li.substring(1, li.length());
		//}
		if (li != null && li.contains(".")) {
			if (li.substring(li.indexOf(".") + 1, li.length()).equals("0")) {
				li = li.substring(0, li.indexOf("."));
			}
		}
		return li;
	}

	private String getLocalName(int valueNumber) {
		String[] localNames = _ir.getLocalNames(_ssa_inst_index, valueNumber);
		if (localNames.length > 0) {
			if (!localNames[localNames.length - 1].contains("$$destructure") && !localNames[localNames.length - 1].contains("arguments")
					&& !localNames[localNames.length - 1].contains("or temp")) {
				return localNames[localNames.length - 1];
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private String getSymbolValue(int valueNumber) {
		Value value = _st.getValue(valueNumber);
		if (value != null) {
			return value.toString(); // #1 format
		} else {
			return null;
		}
	}

	private int parseLhsVar(SSAInstruction ins) {
		if (AbstractReflectivePut.class.isInstance(ins)) {
			return ((ReflectiveMemberAccess) ins).getObjectRef();
		} else if (AbstractReflectiveGet.class.isInstance(ins)) {
			return ins.getDef();
		} else {
			return ins.getDef();
		}
	}

	private List<Integer> parseRhsVars(SSAInstruction ins) {
		int numUses = ins.getNumberOfUses();
		List<Integer> ret = new ArrayList<Integer>();
		if (AbstractReflectivePut.class.isInstance(ins)) {
			ret.add(((AbstractReflectivePut) ins).getValue());
		} else if (AbstractReflectiveGet.class.isInstance(ins)) {
			ret.add(((ReflectiveMemberAccess) ins).getObjectRef());
		} else {
			int from = 0;
			if (SSAAbstractInvokeInstruction.class.isInstance(ins)) {
				from = 2;
			}
			for (int i = from; i < numUses; i++) {
				ret.add(ins.getUse(i));
				if (SSAAbstractInvokeInstruction.class.isInstance(ins)) {
					this._arg_index_map.put(i - 2, ins.getUse(i));
				} else {
					this._arg_index_map.put(i, ins.getUse(i));
				}
			}
		}
		return ret;
	}

	private int parseInvokeMethod(SSAInstruction ins) {
		return ins.getUse(0);
	}

	private int parseObject(SSAInstruction ins) {
		return ins.getUse(1);
	}

	private int parseReturnVar(SSAReturnInstruction ins) {
		return ins.getResult();
	}

	private String parseGlobalName(SSAFieldAccessInstruction ins) {
		return ins.getDeclaredField().getName().toString();
	}

	private void putToGlobalDefMap(String[] global, int use) {
		for (String[] keys : this._global_var_def_map.keySet()) {
			if (keys[0].equals(global[0]) && keys[1].equals(global[1]) && keys[2].equals(global[2])) {
				this._global_var_def_map.remove(keys);
				break;
			}
		}
		this._global_var_def_map.put(global, use);
	}

	private int isGlobalInDefMap(String[] global) {
		for (String[] keys : this._global_var_def_map.keySet()) {
			if (keys[0].equals(global[0]) && keys[1].equals(global[1]) && keys[2].equals(global[2])) {
				return this._global_var_def_map.get(keys);
			}
		}
		return -1;
	}

	private void putToLexicalDefMap(String[] lexical, int use) {
		for (String[] keys : this._lexical_var_def_map.keySet()) {
			if (keys[0].equals(lexical[0]) && keys[1].equals(lexical[1]) && keys[2].equals(lexical[2])) {
				this._lexical_var_def_map.remove(keys);
				break;
			}
		}
		this._lexical_var_def_map.put(lexical, use);
	}

	private int isLexicalInDefMap(String[] lexical) {
		for (String[] keys : this._lexical_var_def_map.keySet()) {
			if (keys[0].equals(lexical[0]) && keys[1].equals(lexical[1]) && keys[2].equals(lexical[2])) {
				return this._lexical_var_def_map.get(keys);
			}
		}
		return -1;
	}
}
