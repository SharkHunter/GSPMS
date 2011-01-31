package net.pms.external;

import java.io.*;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.external.GsPMSSong;

public class GsPMSAlbum extends VirtualFolder {
	private GsAlbum album;
	
	public GsPMSAlbum(GsAlbum album) {
		super(album.getAlbum(),null);
		this.album=album;
		//PMS.debug("create pms alb "+album.getAlbum());
	}
	
	public void discoverChildern() {
		//PMS.debug("album disc");
		GsSong[] songs=album.getSongs();
		//PMS.debug("alb disc song "+songs.length);
		for(int i=0;i<songs.length;i++) {
			if(i>Gs.DisplayLimit) {
				addChild(new GsMore(songs,i,"songs"));
				break;
			}
			addChild(new GsPMSSong(songs[i]));
		}	
	}
	
	public boolean isSearched() {
		return true;
	}
	
	public InputStream getThumbnailInputStream()  {
	//	PMS.debug("get thumb");
		String url=album.getCoverURL();
		if(url.length()==0)
			return super.getThumbnailInputStream();
		try {
			return downloadAndSend(url,true);
		}
		catch (Exception e) {
			return super.getThumbnailInputStream();
		}
	}
}
