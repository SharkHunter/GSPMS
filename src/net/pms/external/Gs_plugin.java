package net.pms.external;
import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import javax.swing.*;

public class Gs_plugin implements AdditionalFolderAtRoot {

	private GsRoot gsRoot;

	public Gs_plugin() {
		gsRoot=new GsRoot();
		try {
			gsRoot.init();
		}
		catch (Exception e) {
			PMS.debug("exp "+e)	;
		}
	}

	public DLNAResource getChild() {
		return gsRoot;
	}

	public void shutdown() {
	}

	public String name() {
		return "GrooveShark";
	}

	public JComponent config() {
		return null;
	}
}
