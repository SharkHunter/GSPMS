package net.pms.external;

import java.io.*;
import net.pms.PMS;
import net.pms.dlna.virtual.VirtualFolder;

public class GsSearches extends VirtualFolder {
	
	private Gs parent;
	private File storeFile;
	private boolean refresh;
	
	private static final String fileName="gs_searches.txt";

	public GsSearches(Gs parent) {
		super("Saved Searches",null);
		this.parent=parent;
		this.storeFile=new File(saveFile());
		refresh=false;
	}
	
	public String saveFile() {
		return this.parent.savePath+File.separator+fileName;
	}
	
	public void store(String str,String type) {
		try {
			BufferedWriter wr=new BufferedWriter(new FileWriter(storeFile,true));
			wr.write("\n\r"+str+","+type+"\n\r");
			wr.flush();
			wr.close();
			refresh=true;
		}
		catch (Exception e) {
			parent.debug("error writing file "+e);
			return;
		}
	}
	
	 public void discoverChildren()  {
		if(!this.storeFile.exists())
			return;
		try {
			BufferedReader in=new BufferedReader(new FileReader(storeFile));
			String line;
			while((line=in.readLine())!=null) {
				if(line.length()==0) // skip empty lines
					continue;
				if(line.charAt(0)=='#') // skip comment lines
					continue;
				String[] data=line.split(",");
				String type="Songs";
				if(data.length>1)
					type=data[1];
				GsSearch sobj=new GsSearch(parent,type,false);
				addChild(new Search(sobj,data[0]));
			}
			in.close();
		}
		catch (Exception e) {
			PMS.debug("exception reading searches file "+e.toString());
			return;
		}
	}
}
