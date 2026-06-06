package extanalysis;

import java.util.HashMap;
import java.util.Iterator;

public class RelationEdgesSet {
	private static RelationEdgesSet _ext_array_instance = null;

	private HashMap<Integer, RelationEdge> _RELATION_ELEMENT = new HashMap<Integer, RelationEdge>();

	public RelationEdgesSet() {

	}

	public static RelationEdgesSet getInstance() {
		if (_ext_array_instance == null) {
			_ext_array_instance = new RelationEdgesSet();
		}
		return _ext_array_instance;
	}

	public int addRelationElement(String fin, String fun, String fvn, String fvar, String tin, String tun, String tvn, String tvar, String op,
			String orop, String fld) {
		RelationEdge tmp = new RelationEdge(fin, fun, fvn, fvar, tin, tun, tvn, tvar, op, orop, fld);
		for (int i = 0; i < this._RELATION_ELEMENT.size(); i++) {
			if (tmp.equals(this._RELATION_ELEMENT.get(i))) {
				return i;
			}
		}
		int index = this._RELATION_ELEMENT.size();
		this._RELATION_ELEMENT.put(index, tmp);
		return index;
	}

	public RelationEdge getRelationElement(int index) {
		return this._RELATION_ELEMENT.get(index);
	}

	public void dumpExtRelationElementArray() {
		StringBuffer sb = new StringBuffer();
		Iterator<Integer> it = _RELATION_ELEMENT.keySet().iterator();
		while (it.hasNext()) {
			int index = it.next();
			RelationEdge e = _RELATION_ELEMENT.get(index);
			sb.append(index);
			sb.append(":");
			sb.append(e.toString());
			sb.append("\n");
		}
		AnalysisUtil.getInstance().writeToFile(AnalysisUtil.getInstance().VAR_GLOBAL_RELATION_ELEM, sb.toString(), false);
	}
}
