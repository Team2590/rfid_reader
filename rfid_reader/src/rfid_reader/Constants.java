package rfid_reader;

public interface Constants {
	
	// Filenames
	public static final String USER_RFIDTAG_MAPPING_FILENAME = "data/FIRST Attendance - RFID tags.csv";
	public static final String USER_RFIDTAG_MAPPING_TESTING_FILENAME = "data/testing.csv";

	public static final String TIMELOG_FILENAME = "data/FIRST Attendance - timelog.csv";
	public static final String DATABASE_DIR 		= "data/attendance_DB";
	public static final String DATABASE_DIR_TESTING = "data/testing_DB";
	public static final String REPORT_FILENAME = "./sync/current.csv";
	
	
	// Other constants here
	public static final String READER_NAME = "ACS ACR122 0";
	public static final String dateTimeFormatPattern = "yyyy/MM/dd HH:mm:ss z";
	public static final long HOUR = 3600*1000; // An hour in milliseconds
	
	public enum LoginType {
		LOGIN, 
		LOGOUT,
		INVALID_TIME_SPAN		// Login/out pairs cannot span days...
	}

	public enum TagType {
			RFID,
			BARCODE
	}


}

