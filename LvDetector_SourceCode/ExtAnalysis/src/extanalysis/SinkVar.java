package extanalysis;


public class SinkVar extends Var {

	public String _sink_method;
	public boolean _sink_network;
	
	public SinkVar(String file, String func, String vn, String var, String method, boolean isNetwork) {
		super(file, func, vn, var);
		this._sink_method = method;
		this._sink_network = isNetwork;
	}
}
