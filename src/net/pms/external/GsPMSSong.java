package net.pms.external;

import java.io.*;
import java.util.ArrayList;

import net.pms.dlna.*;
import net.pms.PMS;
 

public class GsPMSSong extends DLNAResource{
	
	private GsSong song;
	private boolean downloading;
	private ByteArrayOutputStream out;

	public GsPMSSong(GsSong song) {
		this.song=song;
		this.downloading=false;
		this.out=new ByteArrayOutputStream();
	/*	if(getMedia()==null) {
			DLNAMediaInfo m=new DLNAMediaInfo();
			DLNAMediaAudio audio=new DLNAMediaAudio();
			audio.setAlbum(song.getAlbum());
			audio.setArtist(song.getArtist());
			audio.setSongname(song.getName());
			ArrayList<DLNAMediaAudio> a=new ArrayList<DLNAMediaAudio>();
			a.add(audio);
			m.setAudioCodes(a);
			setMedia(m);
		}*/
	}
	
	@Override
	public String getName() {
		return this.song.getName();
	}
	
	@Override
	public String getSystemName() {
		return getName()+".mp3";
	}
	
	public boolean isUnderlyingSeekSupported() {
		return true;
	}
	
	@Override
	public void resolve() {
	}
	
	@Override
	public boolean isValid() {
		checktype();
		return true;
		
	}
	
	@Override
	public long length() {
		return DLNAMediaInfo.TRANS_SIZE;
    }

	@Override
	public boolean isFolder() {
          return false;
    }
	
	public boolean isSearched() {
		return true;
	}
	
	
	@Override
	public InputStream getInputStream() {
		try {
			boolean spawn=true;
			if(!this.downloading) {
				this.downloading=true;
				if(song.delay()==-1)
					spawn=false;
				if(song.save()) {
					OutputStream[] os=new OutputStream[2];
					os[0]=this.out;
					os[1]=new FileOutputStream(song.fileName());
					this.song.download(os,spawn);
				}
				else {
					this.song.download(this.out,spawn);
				}
			}
			
			if(spawn)
				Thread.sleep(song.delay());
			return new GsByteInputStream(out,(int)song.getLength());
			//return Gs.cache.getInputStream(song.getId());
		}
		catch (Exception e) {
			PMS.debug("GSPMSSong exception occured "+e.toString());
			return null;
		}
	}
	
	public InputStream getThumbnailInputStream() {
			byte[] b=song.getCachedCover();
			if(b==null) {					
				String url=song.getCoverURL();
				try {
					if(url.length()==0) 
						return super.getThumbnailInputStream();
					b=downloadAndSendBinary(url);
					song.cacheCover(b);
				}
				catch (Exception e) {
					try {
						return super.getThumbnailInputStream();
					}
					catch (Exception e1) {
						return null;
					}
				}
			}
			return new ByteArrayInputStream(b);
	}
}
