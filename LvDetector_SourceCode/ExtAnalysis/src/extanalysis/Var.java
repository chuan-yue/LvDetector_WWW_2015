package extanalysis;

public class Var {

	public String _file_name;
	public String _func_name;
	public String _value_number;
	public String _var;

	public Var(String file, String func, String vn, String var) {
		this._file_name = file;
		this._func_name = func;
		this._value_number = vn;
		this._var = var;
	}

	public String toString() {
		String ret = "";
		ret += this._value_number;
		ret += "@";
		ret += this._func_name;
		ret += "@";
		ret += this._file_name;

		return ret;
	}
	
	public String toString2() {
		String ret = "";
		ret += this._value_number;
		ret += "@";
		ret += this._func_name;
		ret += "@";
		ret += this._file_name;
		
		if (_var != null) {
			ret = this._var + "," + ret;
		}

		return ret;
	}

	public boolean equals(Var sum) {
		return this.toString().equals(sum.toString());
	}
}
