package net.pms.external;

import net.pms.dlna.virtual.*;
import net.pms.external.GsPMSSong;


public class GsMore extends VirtualFolder{
	private Object[] list;
	private int startIndex;
	private String type;
	
	public GsMore(Object[] objs,int startIndex,String type) {
		super("More",null);
		this.list=objs;
		this.startIndex=startIndex;
		this.type=type;
	}
	
	public void discoverChildren() {
		int j=0;
		for(int i=startIndex;i<list.length;i++) {
			if(j>Gs.DisplayLimit) {
				addChild(new GsMore(list,i,type));
				break;
			}
			j++;
			if(type.compareToIgnoreCase("songs")==0)
				addChild(new GsPMSSong((GsSong)list[i]));
			else if(type.compareToIgnoreCase("albums")==0)
				addChild(new GsPMSPlaylist((GsAlbum)list[i]));
				//addChild(new GsPMSAlbum((GsAlbum)list[i]));
			else if(type.compareToIgnoreCase("artists")==0)
				addChild(new GsPMSPlaylist((GsArtist)list[i]));
		}
	}
	
	public boolean isSearched()  {
		return true;
	}
}
