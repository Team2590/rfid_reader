package rfid_reader;




import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


// A Day is a list of UserTimelog records 
// The relationship between a Day and a UserTimelog is many to one: Each day
// can have multiple UserTimelog records. 
// There is only ONE UserTimlog record per user

@Entity
public class DatabaseDay {

	
	//@PrimaryKey(sequence="ID") private long id; 	// DB sequence ID
	
	// Each Day has a record for the users that scanned in or out that day
	//private MonthDay day; 
	// The primary key is the date itself vs an ID that is automatically assigned
	// Formay is always "yyyy/mm/dd"
	@PrimaryKey
	private String day; // Each day/date has a list of UserTimelog entries


	//@SecondaryKey(relate=MANY_TO_ONE)
	//private List<DatabaseUserTimelog> user_timelog_list =
	//			new ArrayList<DatabaseUserTimelog>(); 		// Each day/date has a list of UserTimelog entries
	private Map<String, DatabaseUserTimelog> user_timelog_map = new HashMap<String, DatabaseUserTimelog>();
	
	/** A default constructor is needed by the DPL for deserialization. */
	private DatabaseDay() {
		
	}
	
	public DatabaseDay(String md) {
		day = md;
	}

	
	public String getDay() {
		return day;
	}


	//public void setDay(MonthDay md) {
	public void setDay(String md) {
		day = md;
	}


	public Map<String, DatabaseUserTimelog> getUser_timelog() {
		return user_timelog_map;
	}

	/**
	 * Add a DB record on this day for this user
	 * 
	 * @param user - name of the user to add to DB
	 * @param date - scan in or out timestamp
	 * @return	   - if record exists for this user, return logout, else login
	 */
	public Constants.LoginType setUser_timelog(String user, Date date) {
		DatabaseUserTimelog user_timelog = null; 
		Constants.LoginType type; 
		// See if there is a record for this user today
		if (user_timelog_map.containsKey(user)) {
			Debug.log("User already has a timelog entry for today (so has scanned in): " + user);
			user_timelog = user_timelog_map.get(user);
			type = user_timelog.update(date); // If user already existed, check for scan ir or out and update accordingly

			if (type == Constants.LoginType.INVALID_TIME_SPAN) {
				return type; //Return without updating the DB
			}
		
		} else {
			user_timelog = new DatabaseUserTimelog(user, date);
			type = Constants.LoginType.LOGIN;
		}
		

		this.user_timelog_map.put(user, user_timelog);
		return type;
		
				
		
	} // end setUser_timelog

	public String toString() {
		return day + " " + user_timelog_map.toString(); 
	}
		
}

