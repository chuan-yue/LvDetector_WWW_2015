package extanalysis;

import java.util.List;

public class VarPair {
	public Var _from;
	public Var _to;

	public List<String> _enc_relation;

	public VarPair(String fvar, String f, String f_file, String f_func, String tvar, String t, String t_file, String t_func, List<String> r) {
		this._from = new Var(f_file, f_func, f, fvar);
		this._to = new Var(t_file, t_func, t, tvar);
		this._enc_relation = r;
	}

	public VarPair(Var f, Var t, List<String> r) {
		this._from = f;
		this._to = t;
		this._enc_relation = r;
	}

	public String toString() {
		return this._from.toString() + "->" + this._to.toString();
	}

	public boolean equals(VarPair v) {
		return this.toString().equals(v.toString());
	}

	public boolean isFollowedBy(VarPair v) {
		return this._to.toString().equals(v._from.toString());
	}
}
