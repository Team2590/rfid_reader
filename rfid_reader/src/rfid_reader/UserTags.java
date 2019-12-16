/**
 * Name:
 * 		UserTags - class for managing the user/tag database (aka the username to RFID tag mapping)
 * 		The data stored in a spreadsheet and exported to a CSV file for use here
 * 
 * 		For 2020, added a feature to support barcodes in addition to RFID tags. Basically
 * 		every student has a student ID and that ID number is equivalent to the barcode on their
 * 		student ID tag! So they can either scan in with an RFID tag, a barcode scanner, or typing their
 * 		ID number!!!
 * 
 * 		Notes: 
 * 		We have no control over the namespace of the barcodes vs RFID tags. The student badges appear
 * 		to be all-numeric. The RFID tags are hex so in ASCII they can have A-F. That means that a student
 * 		ID can collide with an RFID. No reason to chance it. We'll treat them as separate namespaces/lookup tables
 * 
 * 
 * 		
 */



package rfid_reader;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class UserTags {
	public static Map<String, UserTag> tag_map = new HashMap<String, UserTag>();
	public static Map<String, UserTag> barcode_map = new HashMap<String, UserTag>();
	
	public static void main(String[] args) {

		// Really need to use jopt-simple if we add more arguments
    	if (args.length != 0) {
    		for (String argument: args) {
    			if (argument.equals("-d") || argument.equals("--debug") )
    			    Debug.enable(true);
    		}
    	}
    	
    	
		try {
			read_user_tags(Constants.USER_RFIDTAG_MAPPING_FILENAME);
		} catch (Exception e) {
			System.err.println("Unknown error reading tags file " + e.toString());
			e.printStackTrace(System.err);
		}
		
	} // end main
    /**
     * Read the rfid tag to username table from a CSV file
     * 
     * @param user_rfid_filename	- name of the file to read
     */
    public static void read_user_tags(String user_rfid_filename)  {

    	
    	Path currentRelativePath = Paths.get("");
    	String s = currentRelativePath.toAbsolutePath().toString();
    	Debug.log("Current relative path is: " + s);
    	
    	try {
	    	//CSVReader reader = new CSVReader(new FileReader(Constants.USER_RFIDTAG_MAPPING));
	    	CSVReaderBuilder readerBuilder = new CSVReaderBuilder(new FileReader(user_rfid_filename))
	       											.withSkipLines(1);			// Skip header row!!
	    	CSVReader reader = readerBuilder.build();
	
	    	String[] line;
	
	        // File format is:
	        //	0		  1			2			  			3						  4
	        // 	RFID tag, Barcodem Username (last, first), optional Login Message, optional Logout Message
	        // The first line DOES have the header row text so we need to skip by it. The constructor
	        // for CSVReaderBuild conveniently does that. 
	        // Read in the data and place in a Map for fast tag lookups. 
	    	// pjw: TODO: We dont' expect dup's in the source CVS file - add hardening as we add to the map
	        
	        while ((line = reader.readNext()) != null) {
	            Debug.log("RFID: [" + line[0] + "RFID: [" + line[1] + "]\tName: [" + line[2] + "]\tLogin: [" + line[3] + "]\tLogout: [" + line[4] + "]");
	            UserTag user = new UserTag(line[0], line[1], line[2], line[3], line[4]);
	            tag_map.put(user.getUserTagRFID(), user);			// RFID to user map
	            barcode_map.put(user.getUserTagBarcode(), user);	// barcode to user map
	            
	            
	        }
		
	        // Sanity check our map
	        if (Debug.isEnabled()) {
		        for(String key: tag_map.keySet()) {
					System.out.println("key: " + key + " User data: " + tag_map.get(key));
		        }
	        }
    	} catch (Exception e) {
    		System.err.println("ERROR: Cannot read RFID tag datebase: " + Constants.USER_RFIDTAG_MAPPING_FILENAME);
			e.printStackTrace(System.err);
    	}
    }
    
    public static UserTag getUser(String uid, Constants.TagType type) {
    	
    	UserTag user;
    	
    	if (type == Constants.TagType.RFID) {
    		user =  tag_map.get(uid); 
    	}
    	else {
    		user = barcode_map.get(uid); 
    	}	
    	
    	return user;
    	 
    }
} // end public class UserTags

class UserTag {
	
	// We using MiFare RFID cards. UIDs can be 4, 7, or 10 bytes. Just treat it as a string
	private String tag_uid;
	private String barcode; 
	private String username;
	private String loginMsg;
	private String logoutMsg;
	
	public UserTag(String tag, String barcode, String username, String loginMsg, String logoutMsg) {
		this.tag_uid 	= tag;
		this.barcode	= barcode;
		this.username 	= username; 
		this.loginMsg 	= loginMsg;
		this.logoutMsg 	= logoutMsg;
	}

	public String getUserTagRFID() { 
		return tag_uid;
	}
	public String getUserTagBarcode() { 
		return barcode;
	}
	public String getUsername() {
		return username;
	}
	
	public String getUserFirstName() {
		return username; // PJW: TODO - parse to get the first name
	}
	public String getUserLoginMsg() {
		String msg; 
		if (loginMsg.isEmpty()) {
			msg = "Welcome!";
		} else {
			msg = loginMsg; 
		}
		return msg;
	}
	public String getUserLogoutMsg() { 
		String msg; 
		if (logoutMsg.isEmpty()) {
			msg = "Goodbye!";
		} else {
			msg = logoutMsg; 
		}
		return msg;
	}

	@Override
	public String toString() {
		return tag_uid + "|" + barcode + "|" + username + "|" + loginMsg + "|" + logoutMsg;  
	}
	
} // class UserTag
	

