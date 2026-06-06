/**
 * All rights reserved
 * @author rzhao
 * CS Department
 * UCCS 2013
 */

public class NameOffsetPair {
	private String[] paraSet;
	private int offset;
	private int lineno;

	public NameOffsetPair(String[] n, int o, int line){
		paraSet = new String[n.length];
		for(int i = 0; i < n.length; i++){
			paraSet[i] = n[i];
		}
		
		offset = o;
		lineno = line;
	}

	public String[] getParaSet() {
		return paraSet;
	}
	
	public int getOffset(){
		return offset;
	}
	
	public int getLineNo(){
		return lineno;
	}
}
