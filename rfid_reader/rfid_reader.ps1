# Name:
#	rfid_reader.ps1
# By:
#	Peter Wolfe, wolfe@pobox.com
#
# Description:
#	Set up environment to run the mighty FRC2590 RFID reader program
#
# Assumptions:
#	This script lives in our root installation directory
#	Copying this script to some other dir and executing it doens't work. 
#	It computes the proper paths from it's parent directory...
#
# Usage:
#   rfid_reader.ps1 [--debug] [--inventory] [--report] [--date='yyy/mm/dd hh:mm:ss [AM|PM]']

# PSScriptRoot is the directory from which we are executing
# (where this script lives! the top level of our 
# installation tree  - not the process's PWD)
# However, we look for all input data files relative to 
# our installation directy so we need to set and restore
# the user's PWD
$starting_dir = Get-Location 

# try/finally will trap crl-c so we can restore the PWD pre-script
# invocation when an unclean exit. 
# At the time of this writing, the java app never
# exits so ctrl-c is it...
try 
{
    # The directory structure for the app is:
    #     rfid_reader
    #      bin/rfid_reader
    #      data
    #      lib
    # The required java command line is:
    #     java -classpath "bin;lib\*" rfid_reader.RFIDreader

    $cp = $PSScriptRoot + "\bin;" + $PSScriptRoot + "\lib\*"

    Set-location $PSScriptRoot  # cd to execution dir

    #java "-Dfile.encoding=ISO-8859-1" -classpath $cp rfid_reader.RFIDreader $args

    java -classpath $cp rfid_reader.RFIDreader $args
}
finally
{
    # write-host "here: $current_dir"
    Set-Location $starting_dir
}