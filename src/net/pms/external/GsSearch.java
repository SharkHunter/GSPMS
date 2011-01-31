package net.pms.external;
import net.pms.PMS;
import net.pms.dlna.*;


public class GsSearch implements SearchObj {
	private Gs parent;
	private String searchType;
	private boolean store;
	
	public GsSearch(Gs parent,String searchType) {
		this(parent,searchType,true);
	}
	
	public GsSearch(Gs parent,String searchType,boolean store) {
		this.parent=parent;
		this.searchType=searchType;
		if(searchType.compareToIgnoreCase("playlists")!=0)
			if(parent.useTiny())
				this.searchType="Songs";
		this.store=store;
	}
	
	
	private void addSongs(GsSong[] songs,DLNAResource searcher) {
		for(int i=0;i<songs.length;i++) {
			if(i>Gs.DisplayLimit) {
				searcher.addChild(new GsMore(songs,i,"songs"));
				break;
			}	
			if(songs[i]==null)
				continue;
			searcher.addChild(new GsPMSSong(songs[i]));
		}
	}
	

	public void search(String searchStr,DLNAResource searcher){
		PMS.debug("search for "+searchStr+" type "+searchType);	
		if(this.searchType.compareToIgnoreCase("popular")==0) {
			String res=this.parent.getPopular();
			GsSong[] songs=GsSong.parsePop(res, parent);
			addSongs(songs,searcher);
		}
		else {
			if(store) {
				GsSearches g=new GsSearches(parent);
				g.store(searchStr,searchType);
			}
			String res;			
			if(this.searchType.compareToIgnoreCase("songs")==0) {
				GsSong[] songs;
				if(parent.useTiny()) {
					res=this.parent.tinySearch(searchStr);
					songs=GsSong.parseTiny(res,parent);
				}
				else {
					res=this.parent.search(searchStr,this.searchType);	
					songs=GsSong.parseSongs(res,parent);
				}
				addSongs(songs,searcher);
			}
			else if(this.searchType.compareToIgnoreCase("albums")==0) {
				res=this.parent.search(searchStr,this.searchType);
				GsAlbum[] albums=GsAlbum.parseAlbums(res,parent);
				for(int i=0;i<albums.length;i++) {
					if(i>Gs.DisplayLimit) {
						searcher.addChild(new GsMore(albums,i,"albums"));
						break;
					}	
					if(albums[i]==null)
						continue;
					//searcher.addChild(new GsPMSAlbum(albums[i]));
					searcher.addChild(new GsPMSPlaylist(albums[i]));
				}
			}
			else if(this.searchType.compareToIgnoreCase("artists")==0) {
				res=this.parent.search(searchStr,this.searchType);
				GsArtist[] artists=GsArtist.parseArtists(res,parent);
				for(int i=0;i<artists.length;i++) {
					if(i>Gs.DisplayLimit) {
						searcher.addChild(new GsMore(artists,i,"artists"));
						break;
					}	
					if(artists[i]==null)
						continue;
					//searcher.addChild(new GsPMSArtist(artists[i]));
					searcher.addChild(new GsPMSPlaylist(artists[i]));
				}
			}
			else if(this.searchType.compareToIgnoreCase("playlists")==0) {
				res=this.parent.search(searchStr,this.searchType);	
				GsPlaylist[] playlists=GsPlaylist.parsePlaylist(res,parent);
				for(int i=0;i<playlists.length;i++) {
					if(i>Gs.DisplayLimit) {
						searcher.addChild(new GsMore(playlists,i,"playlists"));
						break;
					}	
					if(playlists[i]==null)
						continue;
					searcher.addChild(new GsPMSPlaylist(playlists[i]));
				}
			}
		}	
	}
}

