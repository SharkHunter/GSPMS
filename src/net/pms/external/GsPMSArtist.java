package net.pms.external;

import java.io.*;
import net.pms.dlna.virtual.*;
import net.pms.external.GsPMSSong;

public class GsPMSArtist extends VirtualFolder {
	private GsArtist artist;
	
	public GsPMSArtist(GsArtist artist) {
		super(artist.getArtist(),null);
		this.artist=artist;
	}
	
	public String getName() {
		return artist.getArtist();
	}
	
	public String getSystemName() {
		return getName();
	}
	
	public InputStream getThumbnailInputStream()  {
		String url=artist.getCoverURL();
		if(url.length()==0)
			return super.getThumbnailInputStream();
		try {
			return downloadAndSend(url,true);
		}
		catch (Exception e) {
			return super.getThumbnailInputStream();
		}
	}
	
	public boolean isSearched() {
		return true;
	}
	
	public void discoverChildern() {
		//PMS.debug("artist dissc child");
		GsSong[] songs=artist.getSongs();
		for(int i=0;i<songs.length;i++) {
			if(i>Gs.DisplayLimit) {
				addChild(new GsMore(songs,i,"songs"));
				break;
			}
			addChild(new GsPMSSong(songs[i]));
		}	
	}

}
