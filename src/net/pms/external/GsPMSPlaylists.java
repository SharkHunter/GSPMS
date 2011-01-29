package net.pms.external;

import java.io.*;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.*;

public class GsPMSPlaylists extends VirtualFolder{
	
	private Gs parent;
	
	public GsPMSPlaylists(Gs parent) {
		super("Playlists",null);
		this.parent=parent;		
	}
	
	private String readFirstLine(File f) {
		try {
			BufferedReader in=new BufferedReader(new FileReader(f));
			String line;
			line=in.readLine();
			in.close();
			return line;
		}
		catch (Exception e) {
			return "";
		}
	}
	
	public void discoverChildren() {
		File path=new File(parent.savePath);
		File[] files=path.listFiles();
		for(int i=0;i<files.length;i++) {
			PMS.debug("file "+files[i].getName());
			if(!files[i].getName().endsWith(".gsp"))
				continue;
			String line=readFirstLine(files[i]);
			if(line.length()==0)
				continue;
			String[] toks=line.split(",");
			String name=toks[0].substring(1).trim();
			String id="";
			if(toks.length>1)
				id=toks[1].trim();
			GsPlaylist p=new GsPlaylist(name,id,"","",parent);
			addChild(new GsPMSPlaylist(p,true));
		}
	}
	
	public boolean isSearched() {
		return true;
	}

}
