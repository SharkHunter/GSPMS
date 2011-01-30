package net.pms.external;

import net.pms.PMS;

public class GsPMSDbg implements GsDebug {
	public void debug(String str,int level) {
		if(level==0)
			PMS.minimal(str);
		else
			PMS.debug(str);
	}
	
	public void error(String str) {
		PMS.minimal(str);
	}
}
