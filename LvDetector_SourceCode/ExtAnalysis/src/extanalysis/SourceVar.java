package extanalysis;

public class SourceVar extends Var {

	public String _annotate;

	public SourceVar(String file, String func, String vn, String var, String annotate) {
		super(file, func, vn, var);
		this._annotate = annotate;
	}
}