package net.pms.external;

import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

public class Cover {
	
	private String album;
	private String artist;
	private Gs parent;
	private int size;
	
	public static final int SMALL=0;
	public static final int MEDIUM=1;
	public static final int LARGE=2;
	
	public Cover(String artist) {
		this(artist,"","",null);
	}
	
	
	public Cover(String artist,String album) {
		this(artist,album,"",null);
	}
	
	public Cover(String artist,String album,String song,Gs parent) {
		this.album=album;
		this.artist=artist;
		this.parent=parent;
		size=LARGE;
	}
	
	// Interface functions
	
	public void setSize(int s) {
		size=s;
	}
	
	
	public String getURL() {
		String res;
		res=coverSearch(album,artist);
		if(res.length()==0) {
			res=coverSearch(artist,artist);
		}
		return res;
	}
	
	// Internals doing the actual search etc.
	
	private String coverSearch(String str,String artist) {
		try {
			if(str.length()==0) // no idea to search for ""
				return "";
			return findCoverURL(str,artist);
		}
		catch (Exception e) {
			parent.error("Error while feching cover "+str+" "+e);
			return "";
		}
	}
	
	private String fixSize(String url) {
		if(url.length()==0)
			return url;
		if(size==Cover.SMALL)
			return url;
		if(size==Cover.MEDIUM)
			return url.replaceAll("front", "preview");
		if(size==Cover.LARGE)
			return url.replaceAll("front", "big").replaceAll("thumb", "preview");
		return url;
	}
	
	private String findCoverURL(String str,String artist) {
		try {
			String baseURL="http://www.freecovers.net/search.php?search=";
			String searchStr=URLEncoder.encode(str,"UTF-8");
			String page=fetchPage(baseURL+searchStr+"&cat=4&x=0&y=0");
			Pattern re=Pattern.compile("href=\"javascript:toggleVersionListCovers.*?(http.*?),.*?>(.*?)</a>");
			Matcher m=re.matcher(page);
			while(m.find()) {
				String g2=m.group(2).toLowerCase();
				String a=artist.toLowerCase();
				if(!g2.matches(a+".*"))
					continue;
				return fixSize(m.group(1));
			}
		}
		catch (Exception e) {
			parent.error("Error while parsing cover "+str+" "+e);
		}
		return "";
	}
	
	private String fetchPage(String url) {
		try {
			URL urlobj=new URL(url);
		    BufferedReader in = new BufferedReader(new InputStreamReader(urlobj.openStream()));
		    StringBuilder page=new StringBuilder();
		    String str;
		    while ((str = in.readLine()) != null) { 
			page.append("\n");
			page.append(str.trim());
		    }
		    in.close();
		    return page.toString();
		}
		catch (Exception e) {
			parent.error("Error while feching cover page "+url+" "+e);
		    return "";
		}
	}
}
