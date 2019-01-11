package mailparse;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.poifs.eventfilesystem.POIFSReader;


public class PoiMaster {
	
	public static boolean hasMacro(InputStream microFile) {
		MacroListener mlis = new MacroListener();
		POIFSReader r = new POIFSReader();
		r.registerListener(mlis);
		try
		{
			r.read(microFile);
			return mlis.isMacroDetcted();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return false;
	}

}
