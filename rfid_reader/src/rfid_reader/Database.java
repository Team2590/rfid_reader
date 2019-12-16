/**
 *  Name: 
 *  	Database - Our RFID reader needs a simple database. The original version use a google spreadsheet
 *  	and tried to update it in real time. That had a number of disadvantages, notably the reliability 
 *  	of the school's Internet access (problems with the Sonic firewall) and performance. 
 *  	The version of attendance system will use a local database. We can copy the data to a google
 *  	spreadsheet as needed. 
 *  
 *  	What DB? We want a simple embedded DB. There are MANY with varying feature and complexity. 
 *  	We choose KISS here and are using standard Berkley DB (cause that's all we need). Basically
 *  	B-tree indexed access. 
 *  
 *  	Our table will be indexed by day (primary key) and username (secondary key) 
 *  	The "value" data for each day
 *  		- username
 *  		- time in
 *  		- time out
 *  		- total time for today
 *  	For example:
 *  		Key: 					 Time In   Time Out	   Check-ins | Time Today
 *  		1/12/2017 Wolfe, Peter | 9:00	 | 18:00 	|  1		 | 9:00:00
 *  
 * 		If you log in/out multiple times on the same day there are several use-cases:
 * 		1) Single login/logout:
 * 			- The day's first login updates Time In
 * 			- A subsequent logout updates the number of check-ins
 * 			  and the Time Today (elapsed time) and clears the in/out fields.
 * 		2) Multiple login/logouts on the same day 
 * 			- The day's 1st login updates Time In
 * 			- A 1st logout updates, check-ins, Time Today and clears the in/out fields.
 *  		- Repeat for subsequent login/out pairs, adding to check-ins, Time Today each time.
 *  	3) Login in with no logout
 *  		- User's time-in updated but gets no credit (check-ins and Time Today never updated)
 *  		- Pjw: TODO: if student realizes, need to allow a manual (mentor approved) udpate. See below. 
 *  	4) Logout with no login
 *  		- User gets no credit. (check-ins and Time Today never updated). 
 *  		- Pjw: TODO: we should warn the user of this error and allow a manual (mentor approved)
 *  		  update of the database. Need to create a code path to allow that
 *  		  (e.g. some CLI DB maintenance tasks)
 *		PJW: The above implies we don't really need a "Time Out" field. For the single login/logout
 *		case it can be computed. The the multi-login/logout case it really applies only to the last
 *		logout so is not really useful. For the logout with no login case, it can be used forensically
 *		to make sure students are staying honest with their claims so leave it for now. 
 *
 *		PJW: Modeling question. Above I modeled this as a day has a set of users. 
 *		Could have done a user has a set of days. Not sure it really matters (?). Still have
 *		to index by a primary and secondary key. However, I'm not a DB guy...
 *
 *		There is an invaluable Berkely DB tutorial here: 
 *			http://www.oracle.com/technetwork/testcontent/o27berkeleydb-100623.html
 * 
 *  	There are command line tools to load and dump the DB
 *  	See: http://docs.oracle.com/cd/E17277_02/html/java/com/sleepycat/je/util/DbDump.html
 *  	for an example:
 *   		java { com.sleepycat.je.util.DbDump |
 *       		-jar je-<version>.jar DbDump }
 *  			-h <dir>           # environment home directory
 * 				[-f <fileName>]     # output file, for non -rR dumps
 * 				[-l]                # list databases in the environment
 * 				[-p]                # output printable characters
 * 				[-r]                # salvage mode
 * 				[-R]                # aggressive salvage mode
 * 				[-d] <directory>    # directory for *.dump files (salvage mode)
 * 				[-s <databaseName>] # database to dump
 * 				[-v]                # verbose in salvage mode
 * 				[-V]                # print JE version number	 * 
 * 
 * 		Example DB dump command:
 * 		   java -jar ..\..\lib\je-7.4.5.jar DbDump -h . -s persist#RFIDStore#rfid_reader.DatabaseDay -p
 */


package rfid_reader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date; // Apparently Berkeley DB cannot persist Java8 MonthDay objects. So use the old date object
import java.util.Map;


import com.opencsv.CSVWriter;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;




public class Database {

	private Environment env;		// Berkley DB environment is a set of files in the DB directory
	private EntityStore store;		// DB store for managing entity objects
	private PrimaryIndex<String, DatabaseDay> dayByDate;
	
	
	/**
	 * Initialize our database - a Berkeley DB for Java instance
	 * 
	 * @param read_write - true if the DB is open for read/write access
	 * @throws DatabaseException
	 */
    public void DBinit(String db_dir_filename, Boolean read_only) throws DatabaseException {
    	
    	
        EnvironmentConfig envConfig = new EnvironmentConfig();
    	final File db_dir = new File(db_dir_filename);
    	
        db_dir.mkdirs();						// Create dir if it doesn't exist
        
        /* Open a transactional Berkeley DB Environment. */
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        envConfig.setReadOnly(read_only);
        
        try {
        	env = new Environment(db_dir, envConfig);
        } catch (Exception e) {
        	System.err.println("ERROR: Cannot open database!! Error is:");
        	System.err.println(e.getMessage());
        	System.exit(1);
        }
       
        
        /* Open a transactional EntityStore. */
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(true);
        storeConfig.setReadOnly(read_only);
        store = new EntityStore(env, "RFIDStore", storeConfig);

        /* Initialize the index objects. */
        
        
        dayByDate 	= store.getPrimaryIndex(String.class, DatabaseDay.class);
        
                    
    } // end DBinit  	
   
    public Constants.LoginType write(String user) throws DatabaseException {

    	
    	/*
         * Begin a transaction that will be used to atomically commit all
         * operations in this method.  Note that if no transaction were used,
         * auto-commit would be used for each individual operation.
         */
        Transaction txn = env.beginTransaction(null, null);
        boolean success = false;
        Constants.LoginType login_type;
        
        try { 

        	String today = null; 
        	

        	// User Debug method to get the date. If debugging not enabled, 
        	// it returns the current date. If enabled, it returns the user-specified date (for testing)
        	Date date = Debug.getDate();				// Timestamp for right now including hh:mm:ss
        	
        	
        	SimpleDateFormat sd = new SimpleDateFormat("yyyy/MM/dd");
        	today = sd.format(date);			// This is just the year/month/day
        	Debug.log("Today's database day is: " + today.toString());
        				
        	
        	DatabaseDay dd = dayByDate.get(today);		// Is there an existing db record for today?
        	if (dd == null) {
        		dd = new DatabaseDay(today);
        		Debug.log("New day for DB");
        		
        	} else {
        		Debug.log("Existing day in DB");
        	}
        	        	
        	login_type = dd.setUser_timelog(user, date);

        	dayByDate.put(txn, dd); 

        	success = true; 
        } finally {
        	/*
             * The transaction must be committed or aborted before this method
             * exits to avoid resource leaks.  Success will be false if an
             * exception occurs, in which case we abort the transaction.
             */
            if (success) {
                txn.commit();
            } else {
                txn.abort();
                Debug.log("ERROR: Transaction aborted!!! for user: " + user);
            }       	
        }
    	
        return login_type; 
        
    } // end writeData

    
    /**
     * 
     * 
     * @throws DatabaseException
     */
    public void queryDB() throws DatabaseException {
    
    } // end queryDB

    /**
     * Read the DB to create reports (or emit CSV files for use with excel)
     * 
     * @throws DatabaseException
     */	
    public void reportFromDB() throws DatabaseException {
        
    	EntityCursor<DatabaseDay> dds = dayByDate.entities();	// Database days
    	Map<String, DatabaseUserTimelog> users_timelog_map;
    	DatabaseUserTimelog user_timelog;
    	
    	// Ug. When trying to use System.out on Windows, the output file is ALWAYS UTF-16 no matter what I did. 
    	// Specify the OutputStream's charset, running the entire jvm with -Dfile.encodin=ISO8859-1, converting Strings to Latin-1 bytes...
    	// It all failed. Just writing straight to plain file works fine - I get plain text - no UTF-16 encoding (easily seen with emacs)
    	// Symptoms are that the first two charcters are lowercase y-with-an-umlaut and thorn (decimal 255 and 254, U-00FE,  U-00FF ).
    	// That's the Bye Order Mark (BOM). This is annoying and irrelevant for UTF-8 (only makes sense for multi-byte encodings like UTF-16 where
    	// it indicates the byte order). Our CSV files have every character prepended with a null (^@ in emacs) which indicates they 
    	// are being written in UTF-16?
  
    	try {
	    System.setOut(new PrintStream(System.out, true, StandardCharsets.ISO_8859_1.name()));
    	} catch (UnsupportedEncodingException e1) {
    		e1.printStackTrace();
    	}
 
    	
/*
    	// Leaving the name here stdout - which is now an anachronism given the above restriction - I can't
    	// really write string to System.out. Hoping to someday find a solution that works. 
    	BufferedWriter stdout = new BufferedWriter(new OutputStreamWriter(System.out));
    	//BufferedWriter stdout = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.ISO_8859_1));
*/
/*    	
    	BufferedWriter stdout = null;
    	
    	try {
			stdout = new BufferedWriter(new OutputStreamWriter(new PrintStream(System.out, true, StandardCharsets.ISO_8859_1.name())));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
*/    	
    	
  
    	// Geez louise I give up. Just write to a stupid file cause I cannot bend console output to my ISO-88590-1 will
    	// and end up with UTF-16 files which google sheets chokes on. 
    	Writer stdout = null;
		try {
			//stdout = Files.newBufferedWriter(Paths.get(REPORT_FILENAME), StandardCharsets.ISO_8859_1);
			stdout = Files.newBufferedWriter(Paths.get(Constants.REPORT_FILENAME));

		} catch (IOException e1) {		
			e1.printStackTrace();
		}
   	
    	CSVWriter writer = new CSVWriter(stdout); 

    	
    	try {

/**
 * quick for debugging
    		for (DatabaseDay dd : dds) {
	    		System.out.println(dd);
	    	}
   */ 		

/**
 * quick for sanity checking output
    		for (DatabaseDay dd : dds) {						// For each day
    			users_timelog_map = dd.getUser_timelog();		// Get all the user records for this day
    			for (String key: users_timelog_map.keySet()) { 	// For each user name
    				user_timelog = users_timelog_map.get(key);
    				System.out.println(dd.getDay() + " " + user_timelog.getUsername() + " " + user_timelog.getCheckins() + " " + user_timelog.getTotalTimeToday());
    			}
	    	}
*/
    		
    		// Write CSV header to stdout 
    		String [] s	= new String [] {"Date", "Name", "Checkins", "Total Time"};

    		writer.writeNext(s, false); 						// Write the header, quote only when needed
    		
    		for (DatabaseDay dd : dds) {						// For each day
    			users_timelog_map = dd.getUser_timelog();		// Get all the user records for this day
    			for (String key: users_timelog_map.keySet()) { 	// For each user name
    				user_timelog = users_timelog_map.get(key);
    				s[0] = dd.getDay();
    				s[1] = user_timelog.getUsername();
    				s[2] = Integer.toString(user_timelog.getCheckins());
    				s[3] = Long.toString(user_timelog.getTotalTimeToday());
    				writer.writeNext(s, false);
    			}
	    	}

    	} finally {
     		dds.close();
     		try {
     			writer.flush();
     			writer.close();
     		} catch (IOException e) {
     			System.err.println("Error: cannot close stream: " + e.getMessage());
     			e.printStackTrace();
     		}
     		System.out.println("Report written to " + Constants.REPORT_FILENAME);
    	}
    	
    	
    } // end reportFromDB

public void dumpDB() throws DatabaseException {
        
    	EntityCursor<DatabaseDay> dds = dayByDate.entities();
    	
    	try {
    		System.out.println("DB dump: ");


    		for (DatabaseDay dd : dds) {
	    		System.out.println(dd);
	    	}
    		
    	} finally {
    		dds.close();
    	}
    	    	
    } // end dumpDB
    
    
    public void close() throws DatabaseException {
		/* Always close the store first, then the environment. */
        store.close();
        env.close();
	}
    
} // end class Database
