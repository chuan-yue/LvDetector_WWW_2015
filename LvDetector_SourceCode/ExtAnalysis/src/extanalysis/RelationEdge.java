package extanalysis;

public class RelationEdge {
	public String _from_file_name;
	public String _from_func_name;
	public String _from_value_number;
	public String _from_var;

	public String _to_file_name;
	public String _to_func_name;
	public String _to_value_number;
	public String _to_var;

	public String _operation;
	public String _original_op;

	public String _field;

	public RelationEdge(String fin, String fun, String fvn, String fvar, String tin, String tun, String tvn, String tvar, String op,
			String orop, String fld) {
		this._from_file_name = fin;
		this._from_func_name = fun;
		this._from_value_number = fvn;
		this._from_var = fvar;
		this._to_file_name = tin;
		this._to_func_name = tun;
		this._to_value_number = tvn;
		this._to_var = tvar;
		this._operation = op;
		this._original_op = orop;
		this._field = fld;
	}

	public String toString() {
		String op = null;
		if (this._field == null) {
			op = this._operation;
		} else {
			op = this._operation + "[" + this._field + "]";
		}

		if (this._original_op != null) {
			return "(" + getFromString() + " : " + this._original_op + "," + op + " : " + getToString() + ")";
		} else {
			return "(" + getFromString() + " : " + op + " : " + getToString() + ")";
		}
	}

	public String getFullOp() {
		if (this._field == null) {
			return this._operation;
		} else {
			return this._operation + "[" + this._field + "]";
		}
	}

	public boolean equals(RelationEdge ere) {
		return this.toString().equals(ere.toString());
	}

	public String getFromString() {
		if (this._from_var != null) {
			return this._from_var + "," + this._from_value_number + "@" + this._from_func_name + "@" + this._from_file_name;
		} else {
			return this._from_value_number + "@" + this._from_func_name + "@" + this._from_file_name;
		}
	}

	public String getToString() {
		if (this._to_var != null) {
			return this._to_var + "," + this._to_value_number + "@" + this._to_func_name + "@" + this._to_file_name;
		} else {
			return this._to_value_number + "@" + this._to_func_name + "@" + this._to_file_name;
		}
	}
}
