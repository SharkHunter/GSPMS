package net.pms.external;
import java.io.*;

import net.pms.PMS;
import net.pms.dlna.virtual.VirtualFolder;

public class GsRoot extends VirtualFolder{
	private Gs gsObj;
	private String dbgFile;
	
	private void setDelay() {
		String delay=(String)PMS.getConfiguration().getCustomProperty("gs_plugin.init_delay");
		if(delay!=null) {
			try {
				gsObj.delay=Integer.parseInt(delay);
			}
			catch (Exception e) {
				PMS.minimal("Illegal init_delay value "+e.toString());
			}
		}
	}
	
	private void setMaxDisplay() {
		String disp=(String)PMS.getConfiguration().getCustomProperty("gs_plugin.max_display");
		if(disp!=null) {
			try {
				Gs.DisplayLimit=Integer.parseInt(disp);
			}
			catch (Exception e) {
				PMS.minimal("Illegal max_display value "+e.toString());
			}
		}
	}
	
	private void setPath(String privDbg) {
		try {
			File saveFolder=new File(PMS.getConfiguration().getTempFolder(),"gs_plugin");
			String confPath=(String)PMS.getConfiguration().getCustomProperty("gs_plugin.path");
			String path;
			if(confPath==null) {
				saveFolder.mkdir();
				path=saveFolder.toString();
			}
			else 
				path=confPath;
			gsObj.setPath(path);
			if(privDbg!=null&&privDbg.equalsIgnoreCase("true"))
				dbgFile=path+File.separator+"gs_debug.txt";
		}
		catch (Exception e) {
			PMS.minimal("could not set gs path correctly "+e.toString());
		}
	}
	
	private void setConfig() {
		String privDbg=(String)PMS.getConfiguration().getCustomProperty("gs_plugin.private_dbg");
		setDelay();
		setPath(privDbg);
		setMaxDisplay();
		String tiny=(String)PMS.getConfiguration().getCustomProperty("gs_plugin.tiny");
		String save=(String)PMS.getConfiguration().getCustomProperty("gs_plugin.xxx.yyy.sAvE");
		String cover=(String)PMS.getConfiguration().getCustomProperty("gs_plugin.cover");
		String country=(String)PMS.getConfiguration().getCustomProperty("gs_plugin.country");
		String zero=(String)PMS.getConfiguration().getCustomProperty("gs_plugin.zero_fill");
		
		if(tiny!=null) {
			if(tiny.compareToIgnoreCase("true")==0)
				gsObj.setTiny(true);
			else
				gsObj.setTiny(false);
		}
		if(save!=null) {
			gsObj.setSave(true);
		}
		if(cover!=null&&(cover.compareToIgnoreCase("old")==0))
			gsObj.setCoverSrc(false);
		if(country!=null&&country.length()!=0)
			gsObj.setCountry(country);
		if(zero!=null&&zero.length()>0)
			if(zero.equalsIgnoreCase("true"))
				gsObj.zero_fill=true;
	}
	
	public GsRoot() {
		super("GrooveShark",null);
		this.gsObj=new Gs();
		this.gsObj.setDebug(new GsPMSDbg());
		if(gsObj.initError!=null)
			gsObj.error(gsObj.initError);
		setConfig();
		
		String info="Gs 0.50 using path "+gsObj.savePath+" tiny mode "+gsObj.useTiny()+" init delay "+gsObj.delay+
		" zero_fill "+gsObj.zero_fill;
		PMS.minimal(info);
		if(dbgFile!=null) {
			gsObj.setDebug(new GsPrivDbg(new File(dbgFile),new GsPMSDbg()));
			gsObj.debug("Private debug started");
			gsObj.debug(info);
			PMS.minimal("Gs private debug file "+dbgFile);
			
		}
		gsObj.debug("Grooveshark country "+gsObj.jsonCountry());
	}
	
	public InputStream getThumbnailInputStream() {
		try {
			return new ByteArrayInputStream(downloadAndSendBinary(Gs.IconURL));
		}
		catch (Exception e) {
			return super.getThumbnailInputStream();
		}
	}
	public void init() {
		SearchFolder a=new SearchFolder("Songs",new GsSearch(this.gsObj,"Songs"));
		addChild(a);
		a=new SearchFolder("Albums",new GsSearch(this.gsObj,"Albums"));
		addChild(a);
		a=new SearchFolder("Artists",new GsSearch(this.gsObj,"Artists"));
		addChild(a);
		GsPMSPlaylists b=new GsPMSPlaylists(this.gsObj);
		addChild(b);
		a=new SearchFolder("Playlist search",new GsSearch(this.gsObj,"Playlists"));
		addChild(a);
		GsPlaylist ps=new GsPlaylist("Popular","pop","","",this.gsObj);
		ps.setPopular(true);
		GsPMSPlaylist c=new GsPMSPlaylist(ps);
		addChild(c);
		addChild(new GsSearches(this.gsObj));
	}
}
