package net.pms.external;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.*;
import java.io.*;

public class GsSong implements Runnable,Comparable<Object>{
	// Special flag to indicate if parsing worked
	public boolean Ok;
	
	// Fields
	private String id;
	private String name;
	private String artist;
	private String artistId;
	private String album;
	private String albumId;
	private Cover cover;
	private String coverURL;
	private String plays;
	private int trackNo;
	
	// Internals
	private String streamKey;
	private String streamServerId;
	private String streamServer;
	private OutputStream[] outStream;
	private InputStream inStream;
	
	private Gs parent;
	private boolean streamFetched;
	private long length;
	
	// RegExps to parse songs from different API methods
	
	private static final String stdReg="SongID:([0-9]+),ArtistID:([0-9]+),.*,AlbumID:([0-9]+),TrackNum:([0-9]+),"+
	 ".*,Name:(.*),SongName.*,ArtistName:(.*),AlbumName:(.*),Cover.*,SongPlays:([0-9]+),Artist.*";
	
	private static final String[] stdOrder={"songID","artistID","albumID","track","name","artist","album","plays"};
	
	public static final String popReg="Name:(.*),SongID:([0-9]+),.*,ArtistName:(.*),ArtistID:([0-9]+),AlbumName:(.*),"+
	"AlbumID:([0-9]+),.*,TrackNum:([0-9]+),";
	
	public static final String[] popOrder={"name","songID","artist","artistID","album","albumID","track"};
	
	public static final String tinyReg="Url:.*,SongID:([0-9]+),SongName:(.*),ArtistID:([0-9]+),ArtistName:(.*),"+
	"AlbumID:([0-9]+),AlbumName:(.*)";
	
	public static final String[] tinyOrder={"songID","name","artistID","artist","albumID","album"};
	
	public static final String playReg="SongID:([0-9]+),Name:(.*),ArtistID:([0-9]+),ArtistName:(.*),"+
	"Flags:.*,AlbumID:([0-9]+),AlbumName:(.*),Cover.*";
	
	public static final String[] playOrder={"songID","name","artistID","artist","albumID","album"};
	
	public static final int Layout_GS=0;
	public static final int Layout_Tiny=1;
	public static final int Layout_Songs=2;
	
	
	public GsSong(String parseData,Gs parent) {
		this(parseData,parent,stdReg,stdOrder);
	}
	
	public GsSong(String parseData,Gs parent,String reg,String[] order) {
		this.Ok=false;

		//parent.debug("gs song parse "+parseData);
		String[] s=Gs.jsonSplit(parseData);
		this.id=Gs.getField(s,"songID");
		this.artistId=Gs.getField(s,"artistID");
		this.albumId=Gs.getField(s,"albumID");
		this.name=Gs.getField(s,"SongName");
		if(name==null||name.length()==0)
			name=Gs.getField(s,"Name"); // try this instead
//		parent.debug("name si "+name.replace("\\u00e4", "ä"));
		name=name.replace("\\u00e4", "ä").replace("\\u00e5", "å").replace("\\u00f6", "ö");
		this.artist=Gs.getField(s,"artistName");
		this.album=Gs.getField(s,"albumName");
		this.plays=Gs.getField(s,"plays");
		try {
			this.trackNo=Integer.parseInt(Gs.getField(s,"trackNum"));
		}
		catch (Exception e) {
			this.trackNo=0;
		}
		this.parent=parent;
		this.coverURL=Gs.getField(s, "CoverArtFilename");
		this.cover=new Cover(artist,album,name,parent);
		//cover.setSize(Cover.LARGE);
		this.streamFetched=false;
		this.length=0;
		this.Ok=true;		
	}
	
	public GsSong(String[] fileData,Gs parent) {
		this.Ok=false;
		this.name=fileData[0];
		this.album=fileData[1];
		this.artist=fileData[2];
		this.id=fileData[3];
		if(fileData.length>4)
			this.albumId=fileData[4];
		if(fileData.length>5)
			this.artistId=fileData[5];
		this.trackNo=0;
		this.streamFetched=false;
		this.length=0;
		this.parent=parent;
		this.cover=new Cover(fileData[2],fileData[1],fileData[0],parent);
		this.Ok=true;
	}
	
	// Internal song download/post methods (this is were the action is)
	
	public final void run() {
		retriveData(this.outStream,this.inStream);
	}
	
	private InputStream postData_i() {
		try {
		URL url=new URL("http://"+this.streamServer+"/stream.php");
		HttpURLConnection connection = null;
		String postData="streamKey="+URLEncoder.encode(this.streamKey,"UTF-8");
		//Create connection
		connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("POST");
	
		connection.setRequestProperty("Content-Length", "" + 
				Integer.toString(postData.getBytes().length));
		//connection.setRequestProperty("Content-Language", "en-US");  
				
		connection.setUseCaches (false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
	
		//	Send request
		DataOutputStream wr = new DataOutputStream (connection.getOutputStream ());
		wr.writeBytes (postData);
		wr.flush ();
		wr.close ();
		length=connection.getContentLength();
		parent.debug("GSSong posted request "+postData+" to url "+url.toString()+" cont len "+length);
		return connection.getInputStream();
		}
		catch (Exception e) {
			parent.error("GsSong post exception "+e.toString());
			return null;
		}
	}
	
	private InputStream postData() { // simple fix to be able to repeat postData...
		InputStream in=null;
		for(int i=0;i<10;i++) {
			in=postData_i();
			if(in!=null)
				return in;
			try {
				Thread.sleep(250);
			}
			catch (Exception e) {
				;
			}
		}	
		return null;
	}
	
	private void retriveData(OutputStream[] out) {
		retriveData(out,postData());
	}
	
	private void retriveData(OutputStream[] os,InputStream is) {
    try {
    	//Get Response	
    	byte[] buf=new byte[1];
    	parent.debug("GSSong start reading response");
    	for(;;) {
    		if(is.read(buf,0,1)==-1)
    			break;
    		for(int i=0;i<os.length;i++) 
    			os[i].write(buf,0,1);
       }	
       for(int i=0;i<os.length;i++) {
    	   os[i].flush();	
    	   os[i].close();
       }
      is.close();
      parent.debug("GSSong download complete");
    }
	catch (Exception e) {
		parent.error("GSsong error reading data "+e.toString());
		return ;
	}
   }
	
	// Fetch the stream data from GS

	public void fetchStreamData() {
		if(this.streamFetched) // No need to fetch it twice
			return;
		String param=this.parent.jsonString("songID",this.id)+","+
					 this.parent.jsonString("prefetch","False")+","+
					 this.parent.jsonCountry();
		//Pattern re=Pattern.compile("streamKey:([a-zA-Z0-9]+),streamServerID:([0-9]+),ip:([a-zA-Z0-9\\.]+)");
		String p=this.parent.request(param, "getStreamKeyFromSongIDEx",
				Gs.StreamCliName,Gs.StreamCliRev,Gs.StreamCliSecret);
		//Matcher m=re.matcher(p);
		//parent.debug("stream data page "+p);
		int start=p.indexOf('{');
		if(start==-1)
			return;
		int stop=p.indexOf('}',start+1);
		if(stop==-1)
			return;
		/*if(!m.find()) 
			return ;
		this.streamKey=m.group(1);
		this.streamServerId=m.group(2);
		this.streamServer=m.group(3);*/
		String[] s=Gs.jsonSplit(p.substring(start+1, stop));
		this.streamKey=Gs.getField(s,"streamKey");
		this.streamServerId=Gs.getField(s,"streamServerId");
		this.streamServer=Gs.getField(s, "ip");
		this.streamFetched=true;
	}
	
	
	// Download methods
	
	public void download() {
		download(this.fileName(),false);
	}
	
	public void download(boolean spawn) {
		download(this.fileName(),spawn);
	}
	
	public void download(String file) {
		download(file,false);
	}
	
	public void download(String file,boolean spawn) {
		try {
			FileOutputStream out=new FileOutputStream(file);
			download(out,spawn);
		}
		catch (Exception e) {
			return ;
		}
	}
	
	public void download(OutputStream out) {
		download(out,false);
	}
	
	public void download(OutputStream[] out) {
		download(out,false);
	}
	
	public void download(OutputStream out,boolean spawn) {
		OutputStream[] os=new OutputStream[1];
		os[0]=out;
		download(os,spawn);
	}
	
	public void download(OutputStream[] out,boolean spawn) {
		if(!this.Ok)
			return;
		fetchStreamData();
		parent.debug("GSSong stream data fetched "+toString());
		parent.debug("GSSong stream data "+this.streamFetched+" key "+
					this.streamKey+".");
		if(streamKey==null) { // no stream key? give up
			parent.error("No streamKey found.");
			return;
		}
		if(spawn) {
			this.outStream=out;
			this.inStream=postData();
			if(inStream==null)  // something is terrible wrong, give up early
				return;
			Thread t=new Thread(this);
			parent.debug("GSSong start download thread");
			t.start();
		}
		else
			retriveData(out);
	}
	
	// Access functions
	
	public String toString() {
		return "Song: "+this.name+" Album: "+this.album+" Artist: "+this.artist+
		       " SongId: "+this.id+" plays "+this.plays+" trackNum "+this.trackNo;
	}
	
	public String getName() {
		return name.trim();
	}
	
	public String fileName() {
		String noSlashU=getName().replace("\\u", "_");
		return this.parent.savePath+File.separator+noSlashU+".mp3";
	}
	
	
	public String getAlbum() {
		return this.album;
	}
	
	public String getAlbumId() {
		return this.albumId;
	}
	
	public String getArtist() {
		return this.artist;	
	}
	
	public String getArtistId() {
		return this.artistId;
	}
	
	public String getId() {
		return id;
	}
	
	public boolean save() {
		return parent.saveSong();
	}
	
	public void setId(String id) {
		this.id=id;
	}
	
	public long getLength() {
		return length;
	}
	
	public String savePath() {
		return parent.savePath;
	}
	
	public String getCoverURL() {
		if(parent.useGsCover())
			return parent.cover(coverURL);
		else
			return cover.getURL();
		
	}
	
	public int delay() {
		return parent.delay;
	}
	
	public byte[] getCachedCover() {
		return parent.cacheGet(artist+album);
	}
	
	public void cacheCover(byte[] data) {
		parent.cachePut(artist+album, data);
	}
	
	public int trackNum(){
		return trackNo;
	}
	
	public void setTrack(int t) {
		trackNo=t;
	}
	
	// Compare function
	
	public int compareTo(Object o) {
		GsSong s=(GsSong)o;
		if(this.trackNo>s.trackNo)
			return 1;
		if(this.trackNo<s.trackNo)
			return -1;
		if(this.trackNo==s.trackNo) {
			return this.plays.compareTo(s.plays);
		}
		throw new ClassCastException();
	}
	
	// Static parse functions
	public static GsSong[] parseSongs(String data,Gs parent) {
		return parseSongs(data,parent,stdReg,stdOrder);
	}
	
	public static GsSong[] parseSongs(String data,Gs parent,int layout) {
		return parseSongs(data,parent,stdReg,stdOrder,layout);
	}
	
	public static GsSong[] parsePop(String data,Gs parent) {
		return parseSongs(data,parent,popReg,popOrder);
	}
	
	public static GsSong[] parseTiny(String data,Gs parent) {
		return parseSongs(data,parent,tinyReg,tinyOrder,GsSong.Layout_Tiny);
	}
	
	public static GsSong[] parseSongs(String data,Gs parent,String reg,String[] order) {
		return parseSongs(data,parent,reg,order,GsSong.Layout_GS);
	}
	
	public static GsSong[] parseSongs(String data,Gs parent,String reg,String[] order,int layout) {
		int size=0;
		int pos=0;
		Pattern re;
		GsSong[] res=new GsSong[1];
		if(layout!=GsSong.Layout_Songs) { 
			re=Pattern.compile("\\[(.*)\\]");
			Matcher m=re.matcher(data);
			if(!m.find()) {
				res[0]=null;
				return res;
			}
			data=m.group(1);
		}
		for(;;) {
			int start=data.indexOf('{',pos);
			if(start==-1)
				break;
			int stop=data.indexOf('}',start+1);
			if(stop==-1)
				break;
			pos=stop;
			GsSong song=new GsSong(data.substring(start+1,stop),parent,reg,order);
			if(!song.Ok)
				continue;
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
}
