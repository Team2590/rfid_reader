/**
 * Name: 
 * 		RFID reader
 * 
 * By: 
 * 		Peter Wolfe
 * 
 * Description:
 * 		RFID tag reader based on Java's smartcard.io framework. We are current using the ACS ACR122U reader. 
 * 		This reader can process MiFare Classic RFID tags (and other tag types). See https://en.wikipedia.org/wiki/MIFARE
 * 		There are 1K tags which can store 1024 byes and 4K tags. I think we've been buying only 1K tags.
 * 		We aren't actually storing any data on tag today - only reading it's ID. However, I'm toying with 
 * 		the idea of storing the timestamp and cumulative data on the RFID tag. Downside of that: Children can hack the data unless
 * 		we layer on encryption/other annoying security. Or we just trust the evil children? No, since we plan to 
 * 		support barcodes, we don't want to do anything that is device specific like on-device storage. 
 * 		The cardcode support is predicated on a barcoder reader that behaves like a keyboard!!  It reads the barcode
 * 		and sends it to stdin (with a hard return!!!)
 *   
 * 		The original project started by Mr. Meredith was based on Processing (the java-based language). 
 * 		Processing makes it easy to create GUIs but is pretty horrible to code and debug in. That
 * 		was the impetus to create to pure Java based version. That and the students are more
 * 		java-savvy than processing-savvy as well.  
 *  
 * 		I started with code from: 
 * 			https://oneguyoneblog.com/2017/02/28/acr122u-nfc-java-eclipse-raspberry-pi/
 * 		which had the basic scan-one-card features using smartcard.io and referred to various
 * 		other open source projects for more detailed usage of the API. 
 * 
 * Additional Info:
 * 		For the code to make sense, refer to the smartcard.io JavaDoc and 
 * 		the ISO 7816-4 spec. Here's a summary of what you need to know. 
 * 
 *		Terms from java's smartcard.io:
 * 		Card 			A Smart Card with which a connection has been established.
 * 		CardChannel 	A logical channel connection to a Smart Card.
 * 		CardPermission 	A permission for Smart Card operations.
 * 		CardTerminal 	A Smart Card terminal, sometimes referred to as a Smart Card Reader.
 * 		CardTerminals 	The set of terminals supported by a TerminalFactory.
 * 		CommandAPDU 	A command APDU following the structure defined in ISO/IEC 7816-4.
 * 		ResponseAPDU 	A response APDU as defined in ISO/IEC 7816-4.
 * 		TerminalFactory A factory for CardTerminal objects.
 * 		APDU			Application Protocol Data Unit - the communication "unit" between a reader and a card
 * 
 *  	ISO/IEC 7816-4 is the standard the defines communications protocols with RFID tags, smartcards etc. 
 *  	SW1 and SW2 are the names for the status bytes from the spec (so two bytes when concatenated)
 *  	Cheat sheet for the SW1-SW2 codes is as follows:
 * 			9000 and 61XX - Process completed normally
 * 			62XX and 63XX - Process completed with warnings
 *   		64XX to  66XX - Process aborted - execution error
 *   		67XX to  6FXX - Process aborted - checking error
 *  	What I've seen so far with our RFID read is 9000 for a successful read
 *  	and 6881 when it tries to read with no card present. We shouldn't
 *  	be trying to read with no card present! Need to debug why the wait for card
 *  	calls aren't waiting...
 *  	The spec says SW1 = 68 is "Functions in CLA not supported. See SW2"
 *  	CLA == "class byte" or the "class" of the command. Things like
 *  	command chaining control, secure message indication, logical channel numbers, etc. 
 *  	6881 == Logical channel not supported
 *  
 *  Usage:
 *  	To run this outside of eclipse (from a command line), you need to set CLASSPATH. One way:
 *  		java -classpath "bin;lib\*" rfid_reader.RFIDreader
 *  	which assumes your PWD is the parent dir for bin and lib. 
 *  
 *  Bigger picture usage:
 *  	On the Windows cardreader PC we'll write
 *  	We'll run a Windows scheduled task on the cardreader PC that invokes
 *  	us with the --report > current.csv in order to get the latest data. 
 *      That file will be sync'd to our google drive and referenced by
 *      a google sheet that summarizes the data.
 *      Q: When does this stop? last day of build season? We can have that in the scheduled task
 *      as the stop date. 
 *      Also, only one current.csv exists at a time. In order to preserve historical data, the 
 *      current current.csv is renamed to current_<date>.csv
 *      We get the best of both worlds this way. No weird reliance on the internet/google for attendance
 *      and lazy sung   
 */

package rfid_reader;
 
import java.lang.System;
import java.util.HashMap;
import java.util.List;
import java.math.BigInteger;
import javax.smartcardio.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
 


public class RFIDreader implements Runnable {
	private static Database db;
	private static Integer count = 0; 
	

	
	public void run() {

		Card card = null;
		CardChannel channel = null;		
		// Command to get data from the card on the reader
		CommandAPDU command = new CommandAPDU(new byte[] { (byte) 0xFF, (byte) ISO7816.INS_GET_DATA, (byte) 0x00, (byte) 0x00, (byte) 0x00 });

	    try {
	        
 
	    	TerminalFactory factory = TerminalFactory.getDefault();
		    CardTerminals terminals = factory.terminals();
		    CardTerminal acr122 = null; 
		    if (terminals.list().isEmpty()) { 
	            System.err.println("No readers found. Connect a reader and try again."); 
	            System.exit(1); 
	        } else {
	        	// Pjw: Todo: filter list to just all ACR122s!!!
	        	// Right now it's finding TPM drivers and other readers in addition to ours
	            Debug.log("RFID readers detected: " + terminals.list());
	            acr122 = terminals.getTerminal(Constants.READER_NAME);		// This just finds the first one. TODO: Need to iterate for multi-reader support
	            if (acr122 == null) {
	            	System.err.println("No ACR122 reader found. Connect a reader and try again."); 
		            System.exit(1);
	            }
	        }
	
		    //System.out.println("Place you card/tag/etc. on the reader to start"); 

		    
	        while (  (card = waitForCard(acr122)) != null ) {			// loop forever scanning for user input
	        	
	        	try {
		        	
					channel = card.getBasicChannel();
									
					ResponseAPDU response = channel.transmit(command);	// Get Data command returns the card UID
					Debug.log("Response: " + response.toString());
					   
					//if (response.getSW1() == 0x63 && response.getSW2() == 0x00)  System.out.println("Failed");
					if (response.getSW() != ISO7816.SW_NO_ERROR) {
						System.err.println("ERROR: Failed to read card. Error codes SW1+SW2: " + response.toString());
						System.err.println("Try again. If this error keeps happening, please tell a mentor!");
					
					} else {
						String UID = bin2hex(response.getData());
						Debug.log("RFID UID: " + UID);
						
						write_user(UID, Constants.TagType.RFID);		
					
					}
	        	} catch (Exception e) {
	        		System.err.println("ERROR: problem processing card:");
	        		System.err.println(e.getMessage());
	        		if (Debug.isEnabled()) {
	        			e.printStackTrace();
	        		}
	        		System.err.println("Please try again. If the problem continues, please tell a mentor.");        		        		
	        	} finally {
	        		try {
	        			//card.endExclusive();
	        			card.disconnect(false);						// Done with this card channel
	        			acr122.waitForCardAbsent(0);
	        		} catch (Exception e ) {
	        			System.err.println("ERROR: error releasing exclusive lock on the card:");
	        			System.err.println(e.getMessage());
	        			System.err.println(e.getCause());
	        			if (Debug.isEnabled()) {
	        				e.printStackTrace();
	        			}
	        		}
	        		card = null; 
	        	}

	        	if (Debug.isEnabled()) {
					db.dumpDB();
	        	}
			
			} // end while scan for cards on the terminal
	        System.err.println("Yikes! Shouldn't get here unless card reader was unplugged!");

/*
  			// Comment out the entire card reader loop above and uncomment this section in order to populate
  			// the DB with static data. Each time you run the program it's a login or out for these users. 
  			// TODO: Right now all this data gets added for a single day. Need a smarter simulation to populate
  			// the DB with data for multiple days... 
	    	UserTag user = UserTags.getUser("D58BABD2");
	    	Debug.log("User is: " + user);	
	    	db.write(user.getUsername());
	    	user = UserTags.getUser("B5FCACD2");
	    	Debug.log("User is: " + user);	
	    	db.write(user.getUsername());
	    	user = UserTags.getUser("05FCACD2");
	    	Debug.log("User is: " + user);	
	    	db.write(user.getUsername());
	    	db.reportFromDB();
*/
	    	
		} catch(Exception e) {
			System.err.println("Unknown error reading RFID: " + e.toString());
			e.printStackTrace(System.err);
	 	}

		
	}
	
	
	/**
	 * Initialize the card readers and process cards
	 * 
	 * @param args	array of command line arguments
	 */
	
    public static void main(String[] args) {
    	
    	

    	parseCommandLine(args);
    	
    	// Initialise our DB
    	db = new Database();
    	db.DBinit(Constants.DATABASE_DIR, false);		/* Open the DB for read/write */
 
    	
    	
    	
    	// Initialize tag-to-user database
    	UserTags.read_user_tags(Constants.USER_RFIDTAG_MAPPING_FILENAME);

    	// ZonedDateTime.now( ZoneId.of( "America/New_York" )) for a fixed timezone
    	// System we're running on MUST have proper time/timezone set!!!
    	ZonedDateTime current_time = ZonedDateTime.now();						// Format is: 2017-09-04T02:51:39.905-04:00[America/New_York]		
    	DateTimeFormatter formatter =
    			DateTimeFormatter.ofPattern(Constants.dateTimeFormatPattern); 	// Format is nicer YYYY-MM-DD time tz
    	System.out.println("Validate that the current time is: " + 
    			formatter.format(current_time)); 
    	
    	//System.out.println("Place your card/tag/etc. on the reader to start"); 
    	System.out.println("Place your RFID tag on the reader to login in or out"); 
    	System.out.println("OR read your school ID with the bardcode reader"); 
    	System.out.println("OR type in your student ID number..."); 
    	
    	(new Thread(new RFIDreader())).start(); // Start reading from the RFID card reader
    	Runnable barcode_reader_thread = new BardcodeReaderThread();
    	Thread barcode_reader = new Thread(barcode_reader_thread);
    	barcode_reader.start();					// Start reading from stdin
    	
    	
	 } // end main

    /**
     * Simple CLI parser. 
     * Really need to use jopt-simple if we add more arguments
     * KISS argument processing for now
     * pjw: TODO: What if the user supplies --report --inventory? We don't have exclusion checking...
     * Right now, first arg wins
     * 
     * @param args - string array of command line arguments
     */
    public static void parseCommandLine(String[] args) {
 
    	if (args.length != 0) {
    		for (String argument: args) {
    			
    			if        (argument.equals("-d") || argument.equals("--debug") ) {
    			    Debug.enable(true);
    			
    			} else if (argument.equals("-i") || argument.equals("--inventory") ) {
    				tagInventory(); 											// Inventory a set of tags to stdout
    				System.exit(0);

    			} else if (argument.startsWith("--date")) {						// --date="yyyy/MM/dd hh:mm"
    				// Specify a later date to simulate the passage of time. IOW, scan in a bunch of tags normally and then 
    				// restart with --date specifying a later date/time for the scan out. Allows you to test across multiple 
    				// days as well. 
    				if (argument.contains("=")) {
        				String s[] = argument.split("=");
        				if (s.length > 1 && !s[1].isEmpty()) {
        					Debug.setDate(s[1]);					
        				}
    				}
    			
    			} else if (argument.equals("-r") || argument.equals("--report")) {
    				db = new Database();
    				db.DBinit(Constants.DATABASE_DIR, true);			// Open for read-only access
    				db.reportFromDB();
    				System.exit(0);
    			} else {
    				Usage();
    				
    			} // end if else
    			
    		} // end for each argument
    	}
		
	} // end parseCommandLine


	private static void Usage() {
		System.out.println("Usage: rfid_reader [-i | --inventory] [-d | --debug] [--date='yyy/mm/dd hh:mm:ss [AM|PM]' ] [-r | --report]" );
		System.exit(0);
	} // end Usage


	/**
     * PJW: Make this the "inventory" process OR use the CLI opensc tool    
     */
    public static void tagInventory() {
		Card card = null;
		CardChannel channel = null;		
		// Command to get data from the card on the reader
		CommandAPDU command = new CommandAPDU(new byte[] { (byte) 0xFF, (byte) ISO7816.INS_GET_DATA, (byte) 0x00, (byte) 0x00, (byte) 0x00 });

		try {
	        
		    TerminalFactory factory = TerminalFactory.getDefault();
		    CardTerminals terminals = factory.terminals();
		    if (terminals.list().isEmpty()) { 
	            System.err.println("No readers found. Connect a reader and try again."); 
	            System.exit(1); 
	        } else {
	            Debug.log("RFID readers detected: " + terminals.list());
	            if (Debug.isEnabled()) { 							// Print the reader names for use with CardTerminal.getTerminal("name")
	            	for (CardTerminal ct : terminals.list()) {
	            		System.out.println(ct.getName());
	            	}
	            }
	        }
	
		    
			while (  (card = waitForCard(terminals)) != null ) {
	        	
	        	card.beginExclusive();							// Only our thread should process this card
	        	try {
	
					channel = card.getBasicChannel();
									
					ResponseAPDU response = channel.transmit(command);	// Get Data command returns the card UID
					Debug.log("Response: " + response.toString());
					   
					//if (response.getSW1() == 0x63 && response.getSW2() == 0x00)  System.out.println("Failed");
					if (response.getSW() != ISO7816.SW_NO_ERROR) {
						System.err.println("ERROR: Failed to read card. Error codes SW1+SW2: " + response.toString());
						System.err.println("If this keeps happening, please tell a mentor!");
					} else {
						System.out.println("UID: " + bin2hex(response.getData()));
	        		}
	        	} finally {
	        		card.endExclusive();
	        		card.disconnect(false);						// Done with this card channel
	        		card = null; 
	        	}
			}
			System.err.println("Yikes! Shouldn't get here!");
	   
 
		} catch(Exception e) {
			System.err.println("Unknown error reading RFID: " + e.toString());
			e.printStackTrace(System.err);
	 	}
    	
    } // end method tagInventory
    
	/**
	 * Block until we get a card insertion event from any terminal
	 * 
	 * @param terminals		We can have multiple readers. Scan all readers for cards
	 * @return				Connects to the card on the reader and returns the resulting Card object 
	 * @throws CardException
	 * 
	 * Notes: I've never tested with multiple readers!!!
	 */
    private static Card waitForCard(CardTerminals terminals) {

    	  while (true) { 
    		  try {

    			  terminals.waitForChange(0);		// Block forever waiting for state change - no timeout
    			  for (CardTerminal ct : terminals.list(CardTerminals.State.CARD_INSERTION)) { 
    				  return ct.connect("*"); 		// Connect via any available protocol (e.g. half or full duplex)
    			  }     
    	    	} catch (Exception e) {
    	    		System.err.println("ERROR: Unexpected error in card reader loop: " + e.toString() );
    	    		e.printStackTrace(System.err);
    	    	}
    	}
    	
    	
    	} // end waitForCard
	
	/**
	 * Block until we get a card insertion event from a specific terminal
	 * 
	 * @param ct			Scan this one cardreader
	 * @return				Connects to the card on the reader and returns the resulting Card object 
	 * @throws CardException
	 */
    private static Card waitForCard(CardTerminal ct) {

    	  while (true) { 
    		  try {

    			  ct.waitForCardPresent(0);		// Block forever waiting for state change - no timeout
    			  return ct.connect("*"); 		// Connect via any available protocol (e.g. half or full duplex)
    	    	} catch (Exception e) {
    	    		
    	    		// Classmate PC was so slow we constantly get javax.smartcardio.CardNotPresentException: card not present
    	    		// PCSCException: SCARD_W_REMOVED_CARD
    	    		// It's clear to the user when his card his read so we'll just surpress these
    	    		if (Debug.isEnabled()) {
        	    		System.err.println("ERROR: Unexpected error in card reader loop: " + e.toString() );
        	    		e.printStackTrace(System.err);
    	    			
    	    		}
    	    		// If reader was unplugged we get caught in an infinite loop...
    	    		// Just delay so we aren't compute bound
    	    		try {
    	    			Thread.sleep(1000);
    	    		} catch (InterruptedException ie) {
    	    			
    	    		}
    	    	}
    	}
    	
    	
    	} // end waitForCard

	
	/**
	 * Returns the hex-formatted version of the byte inputs. 
	 * Used to format the data returned from the card reader
	 * which is ISO 7816-4 based
	 * 
	 * @param data	Byte data from the card
	 * @return		hex-formatted string
	 */
	static String bin2hex(byte[] data) {
	    return String.format("%0" + (data.length * 2) + "X", new BigInteger(1,data));
	}

	/*
	 *   Given a user's ID and type (either RFID or barcode), write to the DB
	 *   Synchonize to prevent threading issues even though I *think* the DB layer
	 *   would protect us (it's supposed to be thread safe) but this is simple and guarantees 
	 *   single writer at a time for the DB...
	 */
	public synchronized static void write_user(String uid, Constants.TagType type) {
		String tagtype_name = (type == Constants.TagType.RFID) ? "RFID tag" : "Student ID";
		Constants.LoginType login_type; 
		
		
		UserTag user = UserTags.getUser(uid, type); 
		
		if (user != null) {
			Debug.log("User is: " + user);
			Debug.log("Scanning in via a " + tagtype_name);
			count++;
			Debug.log("Users processed thus far: " + count);

			login_type = db.write(user.getUsername());	// Add this user's scan in or out to the DB
			
			switch (login_type) {						// and print a customized message
			case LOGIN:
				System.out.println("Signing in: " 	+ user.getUsername() + ". " + user.getUserLoginMsg());	
				break;

			case LOGOUT:
				System.out.println("Signing out: " + user.getUsername() + ". " + user.getUserLogoutMsg());	
				break;
			
			case INVALID_TIME_SPAN:
				System.err.println("ERROR: login/outs cannot span multiple days. Login again. ");
				break;
			
			default:									// Should never get here!
				System.err.println("ERROR: unknown login type. Please report this to a mentor.");
				break;	
			} // end switch
		} else {										// Unknown tag. Your RFID tag or Your Student ID....
			System.out.println("Hey!!! Your " + tagtype_name + ": " + uid + " is not in the database. Please see a mentor! Thanks.");
		}

	}
	
	
	
}
