package net.pms.external;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;

public class GsArtist {
		public boolean Ok;
		// Fields
		private String albumId;
		private String artistId;
		private String album;
		private String artist;
		private String verified;
		private Cover cover;
		private String coverURL;
		
		// Internals
		private Gs parent;
		
		private static final String[] stdOrder={"artistID","albumID","artist","album","verified"};
		private static final String stdReg="ArtistID:([0-9]+),.*,AlbumID:([0-9]+),Track.*,ArtistName:(.*),"+
				"AlbumName:(.*),Cover.*,IsVerified:([0-9]+),";
		
		private static final String songReg="SongID:([0-9]+),Name:(.*),Est.*,AlbumName:(.*),AlbumID:([0-9]+),"+
		".*,ArtistID:([0-9]+),ArtistName:(.*),.*,";
		
		private static final String[] songOrder={"songID","name","album","albumID","artistID","artist"};
		
		public GsArtist(String parseData,Gs parent,String reg,String[] order) {
			this.Ok=false;
			/*Pattern re=Pattern.compile(reg);
			Matcher m=re.matcher(parseData);
			if(!m.find())
				return;
			albumId=Gs.getField(m, order, "albumID");
			artistId=Gs.getField(m, order, "artistID");
			artist=Gs.getField(m, order, "artist");
			album=Gs.getField(m, order, "album");
			verified=Gs.getField(m, order, "verified");*/
			String[] s=Gs.jsonSplit(parseData);
			albumId=Gs.getField(s, "albumID");
			artistId=Gs.getField(s, "artistID");
			artist=Gs.getField(s, "artistName");
			album=Gs.getField(s, "albumName");
			verified=Gs.getField(s, "verified");
			this.coverURL=Gs.getField(s, "CoverArtFilename");
			cover=new Cover(artist,album);
			this.parent=parent;
			this.Ok=true;
			
		}
		
		public String getArtist() {
			return this.artist;
		}
		
		public String getCoverURL() {
			if(parent.useGsCover())
				return parent.cover(coverURL);
			else
				return cover.getURL();
		}
		
		private GsSong[] filterSongs(GsSong[] rawSongs,String id) {
			GsSong[] res=new GsSong[1];
			int size=0;
			for(int i=0;i<rawSongs.length;i++) {
				if(rawSongs[i]==null)
					continue;
				if(id.compareTo(rawSongs[i].getArtistId())!=0)
					continue;
				if(size!=0) {
					GsSong[] newArr=new GsSong[size+1];
					System.arraycopy(res, 0, newArr, 0, size);
					res=newArr;
				}
				res[size]=rawSongs[i];
				size++;
			}
			return res;
		}
		
		
		public GsSong[] getSongs() {
			GsSong[] songs=new GsSong[1];
			songs[0]=null;
			PMS.debug("artist get songs called");
			String param=parent.jsonString("offset", "0")+","+
					     parent.jsonString("isVerified", verified)+","+
					     parent.jsonString("artistID", artistId);
			String p=parent.request(param, "artistGetAllSongs");
			//PMS.debug("page "+p);
			Pattern re=Pattern.compile("\\[(.*)\\]");
			Matcher m=re.matcher(p);
			if(!m.find())
				return songs;
			String songData=m.group(1);
			return filterSongs(GsSong.parseSongs(songData, parent, songReg, songOrder,GsSong.Layout_Songs),artistId);
		}
		
		public static GsArtist[] parseArtists(String data,Gs parent) {
			Pattern re=Pattern.compile("\\{result:\\[(.*)\\}");
			Matcher m=re.matcher(data);
			GsArtist[] res=new GsArtist[1];
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
				start=realData.indexOf('{',start+1);
				int stop=realData.indexOf('}',start+1);
				if(stop==-1)
					break;
				pos=stop;
			GsArtist artist=new GsArtist(realData.substring(start+1,stop),parent,stdReg,stdOrder);
			if(!artist.Ok)
				continue;
			if(size!=0) {
				GsArtist[] newArr=new GsArtist[size+1];
				System.arraycopy(res, 0, newArr, 0, size);
				res=newArr;
			}
			res[size]=artist;
			size++;
		}
		return res;
	}
}
