/**
 *  Read barcodes from stding in a separate thread (in order to not block the RFID reader thread.
 *  The bardcode reader we're using behaves like a keyboard device! It scans teh code and "types it"
 *  to stdin AND includes a hard return!
 */
package rfid_reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author wolfe
 *
 */
public class BardcodeReaderThread implements Runnable {

	public void run() {

		//BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); 
		//String barcode; 
		NoEchoReader reader = new NoEchoReader();
		
		try {
			
			while (Boolean.TRUE) {
				char[] barcode = reader.readPassword();
				
				//barcode = reader.readLine();
				Debug.log("barcode: " + String.valueOf(barcode));
				RFIDreader.write_user(String.valueOf(barcode), Constants.TagType.BARCODE);
			}
			
		} catch (IOException e) {
			Debug.log("Exception reading from stdin!");
			e.printStackTrace();
		
		}
		
		
		
	}
	
}
