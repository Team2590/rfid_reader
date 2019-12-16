/**
 *    Usage:
 *  	To run this outside of eclipse (from a command line), you need to set CLASSPATH. One way:
 *  		java -classpath "bin;lib\*" rfid_readerCSVDemo
 *  	which assumes your PWD is the parent dir for bin and lib. 
 */

package rfid_reader;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class CSVDemo {

	
		// When run from eclipse directly the current dir is the top level project dir (not bin where the executable lives). 
		// IOW, eclipse has <top level project dir> and under that: src/ bin/ data/ lib/ etc. 
	    private static final String ADDRESS_FILE = "data/FIRST Attendance - RFID tags.csv";

	    public static void main(String[] args) throws IOException {

	    	Path currentRelativePath = Paths.get("");
	    	String s = currentRelativePath.toAbsolutePath().toString();
	    	System.out.println("Current relative path is: " + s);
	    	
	        CSVReader reader = new CSVReader(new FileReader(ADDRESS_FILE));
	        String[] nextLine;
	        while ((nextLine = reader.readNext()) != null) {
	            System.out.println("Tag: [" + nextLine[0] + "]\nBarcode: [" + nextLine[1] +"]\nName: [" + nextLine[2] + "]\nLogin: [" + nextLine[3] + "]\nLogout: [" + nextLine[4] + "]");
	        }

/**	        
	        
	        
	        // Try writing it back out as CSV to the console
	        CSVReader reader2 = new CSVReader(new FileReader(ADDRESS_FILE));
	        List<String[]> allElements = reader2.readAll();
	        StringWriter sw = new StringWriter();
	        CSVWriter writer = new CSVWriter(sw);
	        writer.writeAll(allElements);

	        System.out.println("\n\nGenerated CSV File:\n\n");
	        System.out.println(sw.toString());
*/

	    }
}
