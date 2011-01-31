package net.pms.external;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;

import net.pms.PMS;
import net.pms.dlna.virtual.VirtualFolder;


public class GsPMSPlaylist extends VirtualFolder {
	private GsPlaylist playlist;
	private boolean useFile;
	private GsAlbum album;
	private GsArtist artist;
	
	public GsPMSPlaylist(GsPlaylist playlist) {
		this(playlist,false);
	}
	
	public GsPMSPlaylist(GsPlaylist playlist,boolean useFile) {
		super(playlist.getName()+" "+playlist.getUser(),null);
		this.playlist=playlist;
		this.useFile=useFile;
		album=null;
		artist=null;
	}
	
	public GsPMSPlaylist(GsAlbum a) {
		super(a.getAlbum(),null);
		playlist=null;
		useFile=false;
		album=a;
		artist=null;
	}
	
	public GsPMSPlaylist(GsArtist a) {
		super(a.getArtist(),null);
		playlist=null;
		useFile=false;
		album=null;
		artist=a;
	}
	
	public String getName() {
		if(album!=null)
			return album.getAlbum();
		if(artist!=null)
			return artist.getArtist();
		return playlist.getName()+" "+playlist.getUser();
	}
	
	public String getSystemName() {
		return getName();
	}
	
	private void savePlaylist(GsSong[] song,File f) {
		try {
			BufferedWriter wr=new BufferedWriter(new FileWriter(f));
			wr.write("# "+playlist.getName()+","+playlist.getID()+"\n\r");
			for(int i=0;i<song.length;i++) {
				wr.write("\n\r"+song[i].getName()+","+song[i].getAlbum()+
						","+song[i].getArtist()+","+song[i].getId()+","+
						song[i].getAlbumId()+","+song[i].getArtistId()+"\n\r");
			}
			wr.flush();
			wr.close();
		}
		catch (Exception e) {
			PMS.debug("error writing playlist "+e);
			return;
		}
	}
	
	private GsSong[] readFile(File f) {
		if(!f.exists())
			return null;
		try {
			int size=0;
			GsSong[] res=new GsSong[1];
			res[0]=null;
			BufferedReader in=new BufferedReader(new FileReader(f));
			String line;
			while((line=in.readLine())!=null) {
				if(line.length()==0) // skip empty lines
					continue;
				if(line.charAt(0)=='#') // skip comment lines
					continue;
				String[] data=line.split(",");
				if(data.length<4)
					continue;
				GsSong song=new GsSong(data,playlist.getParent());
				if(size!=0) {
					GsSong[] newArr=new GsSong[size+1];
					System.arraycopy(res, 0, newArr, 0, size);
					res=newArr;
				}
				res[size]=song;
				size++;
			}
			return res;
		}
		catch (Exception e) {
			PMS.debug("error reading playlist "+e);
			return null;
		}
	}
	
	private void aDisc(GsSong[] songs) {
		int j=0;
		for(int i=0;i<songs.length;i++) {
			if(j>Gs.DisplayLimit) {
				addChild(new GsMore(songs,i,"songs"));
				break;
			}
			if(songs[i]==null)
				continue;
			j++;
			addChild(new GsPMSSong(songs[i]));
		}	
	}
	
	public void discoverChildren()  {
		GsSong[] songs=null;
		if(album!=null) 
			songs=album.getSongs();
		if(artist!=null) 
			songs=artist.getSongs();
		if(playlist!=null) {
			File f=new File(playlist.saveFile());
			if(useFile)
				songs=readFile(f);
			else {
				songs=playlist.getSongs();
				savePlaylist(songs,f);
			}
		}
		if(songs!=null)
			aDisc(songs);
	}
	
	public boolean isSearched() {
		return true;
	}
	
	public InputStream getThumbnailInputStream()  {
		if(playlist!=null)
			return super.getThumbnailInputStream();
		String url="";			
		if(album!=null)
			url=album.getCoverURL();
		if(artist!=null)
			url=artist.getCoverURL();
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
