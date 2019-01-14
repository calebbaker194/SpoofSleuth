package mailparse;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.poifs.macros.VBAMacroExtractor;
import org.apache.poi.poifs.macros.VBAMacroReader;


public class PoiMaster {
	
	public static boolean hasMacro(InputStream microFile) {

		
		try
		{
			VBAMacroReader macroReader = new VBAMacroReader(microFile);
			boolean hasMacros = macroReader.readMacros().isEmpty();
			macroReader.close();
			return !hasMacros;
			
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}


		return true;
	}

}
