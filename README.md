# Synopsis <a name="Synopsis"></a>

This is a plugin for [PS3 Media Server](http://code.google.com/p/ps3mediaserver/) (PMS) that adds GrooveShark support.

# Installation <a name="Install"></a>

* download the [GrooveShark jar file](https://github.com/downloads/SharkHunter/GSPMS/gs_plugin.jar) and place it in the PMS `plugins` directory
* shut down PMS; open `PMS.conf` in a text editor; and add GrooveShark specific configuration see below. 
* restart PMS

## Upgrading <a name="Upgrade"></a>

To upgrade to a new version of the plugin, simply replace the old jar file in the `plugins` directory with the [new version](https://github.com/downloads/SharkHunter/GSPMS/gs_plugin.jar) and restart PMS.

## Uninstalling <a name="Uninstall"></a>

To uninstall PMSEncoder, remove the jar file from the `plugins` directory and restart PMS.

## Configuration <a name="Configuration"></a>

The GrooveShark plugin has the following configuration options all should be entered in the PMS.conf file:

* gs_plugin.path - The path to were the plugin will store it's internal data. (For example c:\\gs_data)
  This should normally be set to something.

* gs_plugin.init_delay - The number of milliseconds the plugin buffers before starting to stream. Defaults to 3000. 

* gs_plugin.max_display - How many hits that will be showed per folder. The plugin ads a "More" folder after this 
  number of hits. Defaults to 32.
  
* gs_plugin.private_dbg - If set to "true" the plugin will send it's debug to a private file called
  "gs_debug.txt" placed in the gs_plugin.path. Other wise the debug ends up in the normal PMS debug file.
 
* gs_plugin.cover - If set to "old" the plugin fetches the cover thumbnails from an external website. 
  Otherwise it uses GrooveSharks thumbnail site. 
 
 * gs_plugin.tiny - If set to "true" the plugin uses GrooveSharks alt. site TinySong to fetch data from.
   Should be used only if the normal method dont' work.


