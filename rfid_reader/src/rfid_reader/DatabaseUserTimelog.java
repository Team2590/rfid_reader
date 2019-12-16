package rfid_reader;

import static com.sleepycat.persist.model.DeleteAction.CASCADE;
import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;
import static com.sleepycat.persist.model.Relationship.ONE_TO_ONE;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;

import rfid_reader.Constants.LoginType;

import com.sleepycat.persist.model.*;

import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;


// A User's time log record can be added to a Day. 
// It has an in/out timestamp, the number of check-ins, and total time the user
// has accumulated for a single day
// 

@Persistent
public class DatabaseUserTimelog {


	// The user's name primary key is the user name rather than 
	// being an ID that is assigned automatically
	//@PrimaryKey
	private String username;

	
	//private ZonedDateTime timeIn; 			// Scanned in timestamp
	//private ZonedDateTime timeOut; 			// Scanned out timestamp
	private Date timeIn; 						// Scanned in timestamp
	private Date timeOut; 						// Scanned out timestamp
	private int			  checkins = 0; 		// Number of checkings today
	//private Period	  totalTimeToday;		// Total time spent in lab today (HH:MM:SS)
	private Long		  totalTimeToday = 0L; 	// Total time spent in lab in minutes
	
	public DatabaseUserTimelog(String name) {
	    this.username = name;
	}
	//public  DatabaseUserTimelog(String username, ZonedDateTime timeIn) {
	public  DatabaseUserTimelog(String username, Date timeIn) {
		this.username = username;
		this.timeIn = timeIn;
	}
	
	/** A default constructor is needed by the DPL for deserialization. */
	private DatabaseUserTimelog() {
	}

	/**
	 * Based on the existing timestand and current user record, 
	 * determine if this is a login or logout and update checkins and elapsed time properly
	 * 
	 * @param date - current timestamp of the tagswipe
	 * @return LoginType - login or logout or mismatched...
	 */
	public Constants.LoginType update(Date date) {

		if (timeIn == null) {			// If never scanned in
			this.timeIn = date;
			Debug.log("Scanning in for today: " + username);
			return Constants.LoginType.LOGIN; 
		}
		
		// If here, we have a timeIn logged for this user. This must be a timeOut time... (e.g. a logout!)
		// Validate that current time is greater thant the timeIn time
		if (timeIn.compareTo(date) > 0) {
			System.err.println("Error: checkin time " + timeIn + " is before the checkout time " + date + " for user " + username );
			System.err.println("Possible time issue on RFID reader system. Please alert a mentor.");
			return null;
		}
		
		// Make sure timeIn and timeOut are on the same day!!!! cannot span days!!!
    	SimpleDateFormat sd = new SimpleDateFormat("yyyy/MM/dd");
    	String timeIn_day  = sd.format(timeIn);					// This is just the year/month/day
    	String current_day = sd.format(date);
    	if (!timeIn_day.equals(current_day)) {
    		timeIn = null; 								// Clear scan-in time to prep for a new scan in...
    		return Constants.LoginType.INVALID_TIME_SPAN;
    	
    	}
    	
		// Determine delta time and update totalTimeToday field
		Debug.log("timeIn: " + timeIn.toString() + " current time: " + date.toString());
		long diff = date.getTime() - timeIn.getTime(); 	// Get delta time in milliseconds
		// TODO: Make sure this is not negative!! e.g. clock issue
		
		long diffMinutes = diff / 1000 / 60; 	// Convert ms to minutes
		Debug.log("diff minutes: " + diffMinutes);
		totalTimeToday += diffMinutes; 
		timeIn = null; 								// Clear scan in time to prep for a new scan in...
		checkins++;									// Count this is a checkin for today
		Debug.log("Scanning out for today");
		return Constants.LoginType.LOGOUT;
		
		
	}
	
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	//public ZonedDateTime getTimeIn() {
	public Date getTimeIn() {
		return timeIn;
	}
	//public void setTimeIn(ZonedDateTime timeIn) {
	public void setTimeIn(Date timeIn) {
		this.timeIn = timeIn;
	}

	public Date getTimeOut() {
		return timeOut;
	}
	//public void setTimeOut(ZonedDateTime timeOut) {
	public void setTimeOut(Date timeOut) {
		this.timeOut = timeOut;
	}

	//public Period getTotalTimeToday() {
	public Long getTotalTimeToday() {
		return totalTimeToday;
	}

	public int getCheckins() {
		return checkins;
	}
	
	
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("[username: ");
		buff.append(username);
		buff.append(" timeIn: ");
		buff.append(timeIn);
		buff.append(" timeOut: ");
		buff.append(timeOut);
		buff.append(" Check-ins ");
		buff.append(checkins);
		buff.append(" Total time today: ");
		buff.append(totalTimeToday);
		buff.append("]\n");
		
	    return buff.toString();
	    }
} // end class DatabaseUserTimelog    
	
