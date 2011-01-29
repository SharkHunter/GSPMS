package net.pms.external;
import java.util.HashMap;
import java.io.*;



public class GsPMSCache {
	private class CacheData {
		ByteArrayOutputStream out;
		public int size;
	}
	
	private HashMap<String, CacheData> cache;
	
	public GsPMSCache() {
		cache=new HashMap<String, CacheData>();
	}
	
	public void add(String id,ByteArrayOutputStream out,int size){
		CacheData d=cache.get(id);
		if(d==null) 
			d=new CacheData();
		d.out=out;
		d.size=size;
		cache.put(id, d);
	}
	
	public InputStream getInputStream(String id) {
		CacheData d=cache.get(id);
		if(d==null)
			return null;
		return new GsByteInputStream(d.out,d.size);
	}
	
	public boolean complete(String id) {
		CacheData d=cache.get(id);
		if(d==null)
			return false;
		return d.size==d.out.size();
	}
}
