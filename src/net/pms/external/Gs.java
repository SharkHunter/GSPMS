package net.pms.external;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.*;
import java.net.*;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;

import java.math.*;
import java.util.HashMap;

public class Gs {
	// Public fields sort of configuration
	public String savePath;
	public static final int DefaultDisplayLimit=32;
	public static final int DefaultDownloadDelay=3000;
	public static final String IconURL="http://grooveshark.com/webincludes/logo/Grooveshark_Logo_Vertical.png";
	public int delay;
	public static int DisplayLimit;
	
	// Fields
	private String session;
	private String token;
	private long lastToken;
	private String clientRev;
	private String secret;
	private boolean tiny;
	private boolean save;
	private HashMap<String, byte[]> cache; 

	// Generated fields
	private Random rand;
	private String sessionHash;
	private MessageDigest md5;
	private MessageDigest sha1;
	private String uuid;
	private String country;
	private GsDebug dbg;
	private boolean gsCover;

	
	// Constructors
	public Gs() {
		this("20101012.37","quitStealinMahShit");
	}
	
	public Gs(String cliRev,String secretString) {
		try {
			this.clientRev=cliRev;
			this.secret=secretString;
			this.rand=new Random();
			URL url=new URL("http://listen.grooveshark.com/");
			HttpURLConnection conn =(HttpURLConnection)url.openConnection();
			conn.setFollowRedirects(true);
			conn.setRequestMethod("GET");
			Pattern re=Pattern.compile("sessionID\":\"([A-z0-9]+)\",");
			String page=fetchPage(conn);
			//System.out.println("page "+page);
			Matcher m=re.matcher(page);
		
			if(!m.find()) { // weired stuff
				return;
			}	
			
			Pattern co=Pattern.compile("country\":\\{(.*)\\}");
			Matcher m1=co.matcher(page);
			if(m1.find())
				this.country="{"+URLDecoder.decode(m1.group(1),"UTF-8");
			if(country==null||country.length()==0)
				this.country="{\"CC3\":\"222305843009213693952\",\"CC2\":\"0\",\"ID\":\"190\",\"CC1\":\"0\",\"CC4\":\"0\"}";
			this.md5=MessageDigest.getInstance("MD5");
			this.sha1=MessageDigest.getInstance("SHA1");
			this.session=m.group(1);
			this.md5.reset();
			this.md5.update(this.session.getBytes());
			this.sessionHash=new String(toHex(this.md5.digest()));
			this.uuid=UUID.randomUUID().toString();
			this.token=getToken();
			this.savePath="";
			this.tiny=false;
			this.delay=DefaultDownloadDelay;
			this.DisplayLimit=DefaultDisplayLimit;
			this.save=false;
			this.dbg=null;
			//this.save=true;
			this.cache=new HashMap<String, byte[]>();
			this.gsCover=true;
			}
		catch (Exception e) {
			error("exception during init "+e);
			return ;
		}
	}	

	// JSON functions
	
	public String jsonString(String key,String val) {
		return "\""+key+"\":\""+val+"\"";
	}
	
	public String jsonBlock(String name,String data) {
		return "\""+name+"\":{"+data+"}";
	}
	
	private String deJsonify(String str) {
		return str.replaceAll("\"","");
	}
	
	public String jsonCountry() {
		return "\"country\":"+country;
	}
	
	private String jsonHeader(String method) {
		String tStr="";
		if(method.compareTo("getCommunicationToken")!=0)
			tStr=","+jsonString("token",generateToken(method));
		String hdrData=jsonString("client","gslite")+","+
				   	   jsonString("clientRevision",this.clientRev)+","+
				   	   jsonString("uuid",this.uuid)+","+
				   	   jsonString("session",this.session)+","+
				   	   jsonCountry()+tStr;
		return jsonBlock("header",hdrData);
		
	}
	
	// Get JSON page
	
	private String postPage(URLConnection connection,String param,String method) {
		try {
			connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8 (.NET CLR 3.5.30729)");
			connection.setRequestProperty("Content-Type","application/json");
			connection.setDoInput(true);
			connection.setDoOutput(true);	
			String params=jsonBlock("parameters",param);
			String postData="{"+jsonHeader(method)+","+params+","+jsonString("method",method)+"}";
		
		  //Send request
			DataOutputStream wr = new DataOutputStream (
					connection.getOutputStream ());
			wr.writeBytes (postData);
			wr.flush ();
			wr.close ();
			
	

	      //Get Response	
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer(); 
			while((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			//System.out.println("resp "+response.toString());
			return response.toString();
		}
		catch (Exception e) {
			debug("exceptiuon "+e);
		    return "";
		}
	}

	// Used to get inital gws page to retrive session id
	private String fetchPage(URLConnection connection) {
		try {
			connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows; U; Windows NT 6.1; sv-SE; rv:1.9.2.3) Gecko/20100409 Firefox/3.6.3");
			connection.setDoInput(true);
			connection.setDoOutput(true);	
			
			//Send request
			DataOutputStream wr = new DataOutputStream (
					connection.getOutputStream ());
			wr.writeBytes ("");
			wr.flush ();
			wr.close ();

	      //Get Response	
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer(); 
			while((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
		    return response.toString();
		}
		catch (Exception e) {
			debug("fetch page internal "+e);
		    return "";
		}
	}
	
	// Convert the hash to a hex string
	private static String toHex(byte[] bytes) {
	    BigInteger bi = new BigInteger(1, bytes);
	    return String.format("%0" + (bytes.length << 1) + "x", bi);
	}

	
	// Internal gs api functions
	
	private String generateToken(String method) {
		String ranChar="";
		int i,x;
		
		if((System.currentTimeMillis()-this.lastToken)>1000) 
			this.token=getToken();
		for(i=0;i<6;i++) {
			x=rand.nextInt(16);
			ranChar=ranChar+Integer.toHexString(x);
		}	
		String t=method+":"+this.token+":"+this.secret+":" +ranChar;
		this.sha1.reset();
		this.sha1.update(t.getBytes());
		return ranChar+toHex(this.sha1.digest());
	}
	
	private String getToken() {
		try {
			//URL url=new URL("https://cowbell.grooveshark.com/service.php");
			URL url=new URL("https://cowbell.grooveshark.com/more.php");
			HttpsURLConnection conn=(HttpsURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			Pattern r=Pattern.compile("result\":\"([A-z0-9]+)\"");
			String param=jsonString("secretKey",this.sessionHash);
			token=postPage(conn,param,"getCommunicationToken");
			Matcher m=r.matcher(token);
			if(!m.find())
				return "";
			this.lastToken=System.currentTimeMillis();
			return m.group(1);
		}
		catch (Exception e) {
			error("fetch token "+e);
			return "";
		}
	}
	
	// External api functions to make requests
	
	public String getPopular() {
		Pattern re=Pattern.compile("\\[(.*)\\]");
		String res=request("","popularGetSongs");
		Matcher m=re.matcher(res);
		if(!m.find())
			return "";
		return m.group(1);
	}
	
	public String request(String param,String method) {
		try {
			URL url=new URL("http://cowbell.grooveshark.com/more.php");
			HttpURLConnection conn =(HttpURLConnection)url.openConnection();
			conn.setFollowRedirects(true);
			conn.setRequestMethod("POST");
			String page=postPage(conn,param,method);
			Pattern re=Pattern.compile("\"result\":(.*)");
			Matcher m=re.matcher(page);
			if(!m.find())
				return page;
			return deJsonify(m.group(1));
		}
		catch (Exception e) {
			error("request page "+e);
			return "";
		}
	}
	
	private String ucFirst(String str) {
		char first=str.charAt(0);
		return String.valueOf(first).toUpperCase()+str.substring(1);
	}
	
	public String search(String str) {
		return search(str,"Songs");
	}
	public String search(String str,String type) {
		String param=jsonString("query",str)+","+jsonString("type",ucFirst(type));
		return request(param,"getSearchResultsEx");
	}
	
	public String tinySearch(String str) {
		str=str.replace(' ', '+');
		try {
			URL url=new URL("http://www.tinysong.com/s/"+str+"?format=json&limit="+
					DefaultDisplayLimit);
			String page=fetchPage(url.openConnection());
			return deJsonify(page);
		}
		catch (Exception e) {
			error("tiny serached failed "+e);
			return "";
		}
	}
	
	public static String[] jsonSplit(String str) {
		return str.split(",");
	}
	
	public static String getField(String[] list,String field) {
		for(int i=0;i<list.length;i++) {
			String[] s=list[i].split(":");
			if(s[0].compareToIgnoreCase(field)==0)
				if(s.length>1)
					return s[1];
		}
		return "";
	}
	// Other external functions
	
	public static String getField(Matcher m,String[] order,String field) {
		for(int i=0;i<order.length;i++) {
			if(order[i].compareTo(field)==0) { 
				if(i+1>m.groupCount())
					return "";
				return m.group(i+1);
			}
		}
		return "";
	}
	
	public void setPath(String path) {
		this.savePath=path;
	}
	 
	public boolean useTiny() {
		return tiny;
	}
	
	public void setTiny(boolean b) {
		tiny=b;
	}
	
	public boolean saveSong() {
		return save;
	}
	
	public void setSave(boolean b) {
		save=b;
	}
	
	// Cache handling
	
	public void cachePut(String key,byte[] data) {
		if(cache.get(key)!=null)
			cache.remove(key);
		cache.put(key,data);
	}
	
	public byte[] cacheGet(String key) {
		return cache.get(key);
	}
	
	// Debug things
	public void setDebug(GsDebug d) {
		this.dbg=d;
	}
	
	public void debug(String str) {
		debug(str,100);
	}
	
	public void debug(String str,int level) {
		if(dbg==null)
			return;
		dbg.debug(str, level);
	}
	
	public void error(String str) {
		if(dbg==null)
			return;
		dbg.error(str);
	}
	
	// Cover handling
	public String cover(String dataURL) {
		return "http://beta.grooveshark.com/static/amazonart/m"+dataURL;
	}
	
	public void setCoverSrc(boolean b) {
		gsCover=b;
	}
	
	public boolean useGsCover() {
		return gsCover;
	}
	
	public static void main(String args[]) {
		int i;
		Gs g =new Gs();
		g.setPath("c:\\gs_tst");
		g.setSave(true);
		String tpage=g.tinySearch("fear of the dark");
		System.out.println("tpage "+tpage);
		GsSong[] songs=GsSong.parseTiny(tpage, g);
		for(i=0;i<songs.length;i++)
			
			System.out.println("song "+songs[i].toString());
		String gpage=g.search("fear of the dark");
		System.out.println("gpage "+gpage);
		GsSong[] songs1=GsSong.parseSongs(gpage, g);
		try {
		System.out.println("gsong "+URLDecoder.decode(songs1[0].getId(),"UTF-8"));
		}
		catch (Exception e) {
			;
		}
		String lpage=g.search("fear of the dark","Albums");
		System.out.println("lpage "+lpage);
		GsAlbum[] albums=GsAlbum.parseAlbums(lpage, g);
		GsSong[] songs2=albums[0].getSongs();
		for(i=0;i<songs2.length;i++)
			System.out.println("song "+songs2[i].toString());
		//String p=g.search("gubbrock","Playlists");
		String p=g.search("viperfx's Playlist 1","Playlists");
		System.out.println("play "+p);
		GsPlaylist[] ps=GsPlaylist.parsePlaylist(p, g);
		GsSong[] ss=ps[0].getSongs();
		for(i=0;i<ss.length;i++)
			System.out.println("song "+ss[i].toString());
		//songs1[0].download();
		String apage=g.search("iron maiden", "Artists");
		System.out.println("apage "+apage);
		GsArtist[] artists=GsArtist.parseArtists(apage,g);
		GsSong[] pop=GsSong.parsePop(g.getPopular(),g);
		//pop[0].download();
		//songs1[0].download();
		System.out.println("all done");
	}
	
}
