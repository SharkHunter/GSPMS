package net.pms.external;

public class GsStdDbg implements GsDebug {
	public void debug(String str,int level) {
		System.out.println(str);
	}
	public void error(String str) {
		System.err.println(str);
	}
}
