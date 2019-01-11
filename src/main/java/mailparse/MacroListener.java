package mailparse;

import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;

public class MacroListener implements POIFSReaderListener{

	private boolean macroDetcted = false;
	
	@Override
	public void processPOIFSReaderEvent(POIFSReaderEvent event)
	{
	    if(event.getPath().toString().startsWith("\\Macros")
	            || event.getPath().toString().startsWith("\\_VBA")) {
	    	setMacroDetcted(true);
	    }
	}

	public boolean isMacroDetcted()
	{
		return macroDetcted;
	}

	private void setMacroDetcted(boolean macroDetcted)
	{
		this.macroDetcted = macroDetcted;
	}
}
