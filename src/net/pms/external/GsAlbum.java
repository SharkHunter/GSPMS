package net.pms.external;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;

public class GsAlbum {
	public boolean Ok;
	// Fields
	private String albumId;
	private String artistId;
	private String album;
	private String artist;
	private String verified;
	private String coverURL;
	
	// Internals
	private Gs parent;
	private Cover cover;
	
	private static final String[] stdOrder={"artistID","albumID","artist","album"};
	private static final String stdReg="AlbumID:([0-9]+),ArtistID:([0-9]+),.*,AlbumName:(.*),ArtistName:(.*),"+
			",Year.*";
	
	private static final String songReg="SongID:([0-9]+),Name:(.*),Est.*,AlbumName:(.*),AlbumID:([0-9]+),"+
	"Ye.*,ArtistID:([0-9]+),ArtistName:(.*),Pop.*,TrackNum:([0-9]+)";
	
	private static final String[] songOrder={"songID","name","album","albumID","artistID","artist","trackNum"};
	
	GsAlbum(String parseData,Gs parent,String reg,String[] order) {
		this.Ok=false;
		/*Pattern re=Pattern.compile(reg);
		Matcher m=re.matcher(parseData);
		if(!m.find())
			return;*/
		String[] s=Gs.jsonSplit(parseData);
		albumId=Gs.getField(s,"albumID");
		artistId=Gs.getField(s,"artistID");
		artist=Gs.getField(s, "artistName");
		album=Gs.getField(s, "albumName");
		verified=Gs.getField(s, "verified");
		this.parent=parent;
		this.coverURL=Gs.getField(s, "CoverArtFilename");
		cover=new Cover(artist,album);
		this.Ok=true;
	}
	
	private GsSong[] removeDup(GsSong[] songs) {
		int track=0;
		int size=0;
		GsSong[] res=new GsSong[1];
		for(int i=0;i<songs.length;i++) {
			if(songs[i]==null)
				continue;
			if(songs[i].trackNum()==track)
				continue;
			track=songs[i].trackNum();
			if(size!=0) {
				GsSong[] newArr=new GsSong[size+1];
				System.arraycopy(res, 0, newArr, 0, size);
				res=newArr;
			}
			res[size]=songs[i];
			size++;
		}
		return res;
	}
	
	private GsSong[] filterSongs(GsSong[] rawSongs,String id) {
		GsSong[] res=new GsSong[1];
		int size=0;
		for(int i=0;i<rawSongs.length;i++) {
			if(rawSongs[i]==null)
				continue;
			if(id.compareTo(rawSongs[i].getAlbumId())!=0)
				continue;
			if(rawSongs[i].trackNum()==0)
				continue;
			if(size!=0) {
				GsSong[] newArr=new GsSong[size+1];
				System.arraycopy(res, 0, newArr, 0, size);
				res=newArr;
			}
			res[size]=rawSongs[i];
			size++;
		}
		Arrays.sort(res);
		return removeDup(res);
	}
	
	public GsSong[] getSongs() {
		//PMS.debug("get song for album "+album);
		String param=parent.jsonString("offset", "0")+","+
				     parent.jsonString("isVerified", verified)+","+
				     parent.jsonString("albumID", albumId);
		String p=parent.request(param, "albumGetAllSongs");
		Pattern re=Pattern.compile("\\[(.*)\\}");
		Matcher m=re.matcher(p);
		if(!m.find()) {
			GsSong[] songs=new GsSong[1];
			songs[0]=null;
			return songs;
		}
		String songData=m.group(1);
		return filterSongs(GsSong.parseSongs(songData, parent, songReg, songOrder,GsSong.Layout_Songs),albumId);
	}
	
	public void downloadAll() {
		GsSong[] songs=getSongs();
		if(songs==null)
			return;
		for(int i=0;i<songs.length;i++)
			songs[i].download();
	}
	
	public String getAlbum() {
		return album;
	}
	
	public String getCoverURL() {
		if(parent.useGsCover())
			return parent.cover(coverURL);
		else
			return cover.getURL();
	}
	
	public static GsAlbum[] parseAlbums(String data,Gs parent) {
		Pattern re=Pattern.compile("\\{result:\\[(.*)\\}");
		Matcher m=re.matcher(data);
		GsAlbum[] res=new GsAlbum[1];
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
			GsAlbum album=new GsAlbum(realData.substring(start+1,stop),parent,stdReg,stdOrder);
			if(!album.Ok)
				continue;
			if(size!=0) {
				GsAlbum[] newArr=new GsAlbum[size+1];
				System.arraycopy(res, 0, newArr, 0, size);
				res=newArr;
			}
			res[size]=album;
			size++;
		}
		return res;
	}

}
