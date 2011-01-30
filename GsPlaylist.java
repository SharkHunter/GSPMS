package net.pms.external;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GsPlaylist {

	private static final String stdReg="PlaylistID:([0-9]+),Name:(.*),.*?,"+
								       "Username:(.*),Pic.*";
	private static final String[] stdOrder={"id","name","user"};

	// Fields
	public boolean Ok;
	private Gs parent;
	private String id;
	private String name;
	private String size;
	private String user;
	private boolean popular;
	
	
	public GsPlaylist(String parseData,Gs parent) {
		this(parseData,parent,stdReg,stdOrder);
	}
	
	public GsPlaylist(String parseData,Gs parent,String reg,String[] order) {
		this.Ok=false;
		Pattern re=Pattern.compile(reg);
		Matcher m=re.matcher(parseData);
		if(!m.find())
			return;
		this.id=Gs.getField(m, order, "id");
		this.name=Gs.getField(m, order, "name");
		this.size=Gs.getField(m, order, "size");
		this.user=Gs.getField(m, order, "user");
		this.parent=parent;
		this.popular=false;
		this.Ok=true;
	}
	
	public GsPlaylist(String name,String id,String size,String user,Gs parent) {
		this.id=id;
		this.name=name;
		this.size=size;
		this.user=user;
		this.parent=parent;
		this.popular=false;
		this.Ok=true;
	}
	
	public GsSong[] getSongs() {
		String p;
		if(this.popular) 
			p=this.parent.getPopular();
		else {
			String param=this.parent.jsonString("playlistID",id);
			p=this.parent.request(param, "playlistGetSongs");
		}
		return GsSong.parseSongs(p, parent,GsSong.playReg,GsSong.playOrder,GsSong.Layout_Tiny);
	}
	
	public void downloadAll() {
		GsSong[] s=getSongs();
		for(int i=0;i<s.length;i++)
			s[i].download();
	}
	
	public void setPopular(boolean b) {
		this.popular=b;
	}
	
	
	public String getName() {
		return name;
	}
	
	public String getID() {
		return id;
	}
	
	public String getUser() {
		return user;
	}
	
	public String saveFile() {
		return this.parent.savePath+File.separator+"gs_play_"+
				id+".gsp";
	}
	
	public Gs getParent() {
		return parent;
	}
	
	public String toString() {
		return "Playlist "+name+" from "+user+" has "+size+" songs id is "+id;
	}
	
	public static GsPlaylist[] parsePlaylist(String data,Gs parent) {
		Pattern re=Pattern.compile("\\{result:\\[(.*)\\]");
		Matcher m=re.matcher(data);
		GsPlaylist[] res=new GsPlaylist[1];
		if(!m.find()) {
			res[0]=null;
			return res;
		}
		String realData=m.group(1);
		
		int size=0;
		int pos=0;
		for(;;) {
			int start=realData.indexOf('{',pos);
			if(start==-1)
				break;
			int stop=realData.indexOf('}',start);
			if(stop==-1)
				break;
			pos=stop;
			GsPlaylist playlist=new GsPlaylist(realData.substring(start+1,stop),
											    parent,stdReg,stdOrder);
			if(!playlist.Ok)
				continue;
			if(size!=0) {
				GsPlaylist[] newArr=new GsPlaylist[size+1];
				System.arraycopy(res, 0, newArr, 0, size);
				res=newArr;
			}
			res[size]=playlist;
			size++;
		}
		return res;
	}
}
