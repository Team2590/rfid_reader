package rfid_reader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Class for automated regression testing
 * 
 * @author wolfep
 *
 */
public class Tester {
	
	private static Database db;
	
	
	public static void main(String[] args) throws ParseException, InterruptedException {
	
		parseCommandLine(args);
		
		db = new Database();
		db.DBinit(Constants.DATABASE_DIR_TESTING, false);			// Open test DB for read-write access
		

		// Initialize tag-to-user database
    	UserTags.read_user_tags(Constants.USER_RFIDTAG_MAPPING_TESTING_FILENAME);
		
    	// Populate a test database with 6 weeks of data (the entire build season)
    	// for 60 users each day, where each user has logged in and out twice each day
    	// Use the 2018 build season: Jan 6th - Feb 20th
    	// TODO: Need to delete current DB directory so we build the data from scratch
    	
    	
    	SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
 
    	final String START_OF_BUILD_SEASON = "2018/01/06 09:01:00";
    	final String END_OF_BUILD_SEASON   = "2018/02/20 23:59:00";
    	
    	
    	
	   	Date date_start = formatter.parse(START_OF_BUILD_SEASON);
	   	Date date_end 	= formatter.parse(END_OF_BUILD_SEASON);

		Calendar cal_start = Calendar.getInstance();
		cal_start.setTime(date_start);
		Calendar cal_end = Calendar.getInstance();
		cal_end.setTime(date_end);
	
		while( !cal_start.after(cal_end)) { // For each day of the season
		    Date d = cal_start.getTime();
		    Debug.setDateObj(d); 				// Simulate this date system-wide
		    System.out.println("Time in :" + d + "============================================================================================================");
		    
		    // For each of the 60 student test RFID tags - Login for this day
		    for (int i = 10001; i < 10060; i++ ) {  	
		    	UserTag user = UserTags.getUser(Integer.toString(i), Constants.TagType.RFID);
		    	Debug.log("User is: " + user);	
		    	db.write(user.getUsername());
		    }
		    
		    d = new Date(d.getTime() + 3 * Constants.HOUR); 	// Three hours later
		    Debug.setDateObj(d);
		    System.out.println("Time out: " + d +"============================================================================================================");
		    
		    // For each of the 60 student test RFID tags - Logout for this day
		    for (int i = 10001; i < 10060; i++ ) { 	
		    	UserTag user = UserTags.getUser(Integer.toString(i), Constants.TagType.RFID);
		    	Debug.log("User is: " + user);	
		    	db.write(user.getUsername());
		    }	    
		    //Thread.sleep(10000);				// Delay so I could watch debug output
		    cal_start.add(Calendar.DATE, 1);   	// .. tomorrow is just a day away...
		    
		} // end for each day of the season 

		db.dumpDB();
		db.reportFromDB();
	
		
/* here's the for loop version
		for (Date d = cal_start.getTime(); cal_start.before(cal_end); cal_start.add(Calendar.DATE, 1), d = cal_start.getTime()) {
			    // do code here
			    System.out.println(d);
		}

	    // Example of using java 8's new time classes for this
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    	LocalDate start = LocalDate.parse("2018/01/06", formatter);	// Start of 2018 build season
    	LocalDate end   = LocalDate.parse("2018/02/20", formatter);	// Stop Build day for 2018
    	
    	LocalDate next = start.minusDays(1);
    	while ( (next = next.plusDays(1)).isBefore(end.plusDays(1)) ) {
    		System.out.println(next);
 		}
 */  	
  	

	} // end main
	
    /**
     * Simple CLI parser. 
     * Really need to use jopt-simple if we add more arguments
     * KISS argument processing for now
     * 
     * @param args - string array of command line arguments
     */
    public static void parseCommandLine(String[] args) {
 
    	if (args.length != 0) {
    		for (String argument: args) {
    			
    			if        (argument.equals("-d") || argument.equals("--debug") ) {
    			    Debug.enable(true);
    			
    			} else if (argument.startsWith("--date")) {						// --date="yyyy/MM/dd hh:mm"
    				if (argument.contains("=")) {
        				String s[] = argument.split("=");
        				if (s.length > 1 && !s[1].isEmpty()) {
        					Debug.setDate(s[1]);					
        				}
    				}
    			
    			} else if (argument.equals("-r") || argument.equals("--report")) {
    				db = new Database();
    				db.DBinit(Constants.DATABASE_DIR_TESTING, true);			// Open for read-only access
    				db.reportFromDB();
    				System.exit(0);
    			} else {
    				Usage();
    				
    			} // end if else
    			
    		} // end for each argument
    	}
		
	} // end parseCommandLine

	private static void Usage() {
		System.out.println("Usage: tester [-r | --report] [-d | --debug] [--date='yyy/mm/dd hh:mm:ss [AM|PM]' ]" );
		System.exit(0);
	} // end Usage

} // end class Tester
