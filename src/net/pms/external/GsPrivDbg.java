package net.pms.external;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class GsPrivDbg implements GsDebug {
	private BufferedWriter os;
	private GsDebug backup;
	
	public GsPrivDbg(File f,GsDebug back) {
		try {
			os=new BufferedWriter(new FileWriter(f,false));
			backup=back;
		} catch (Exception e) {
			os=null;
		}
	}
	
	public void debug(String str,int level) {
		try {
			os.write("\n\r"+str+"\n\r");
			os.flush();
		} catch (IOException e) {
			backup.debug(str, level);
		}
	}
	
	public void error(String str) {
		try {
			os.write("\n\r"+str+"\n\r");
			os.flush();
		} catch (IOException e) {
			backup.error(str);
		}
	}
		
}
