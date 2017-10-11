# Name:
#	rfid_reader_task.ps1
# By:
#	Peter Wolfe, wolfe@pobox.com
#
# Description:
#	Task script to be executed by the Windows Task Scheduler
#
# Assumptions:
#	Assume that the main rfid_driver.ps1 script is in the systeam PATH
#   (so we don't have to hardcode a path here...)
#
# Usage:
#   Run the Windows Task Schedule app and create a basic task
#   Trigger is daily
#   Start is  12:00AM on the first day of build season, recur every 1 day
#   Action is Start a program
#   Program script (powershell )
#       Add arguments: -noprofile -executionpolicy bypass -file <path>\rfid_reader_task.ps1
#       Start in: the directory where rfid_reader_task.ps1 installed 
#       (same dir where rfid_reader.ps1)
#   Choose to open additional options (task properties)
#       Run when user is logged on or not
#   


$current_dir  = $PSScriptRoot
$current_file = $current_dir + "\" + "sync\current.csv"

if (Test-Path $current_file) {

    $last = Get-Content -tail 1 $current_file
    write-host "last line: $last"
    # Split the line at the commas
    $a = $last.split(",")
    $date = $a[0]       # Grab first element (the date - yyyy/mm/dd format)
    write-host "date: $date"
    $date = $date -replace '"', ''  # Strip double quotes
    $date = $date -replace "/", "-" # Strip slashes
    $current_dated_file = "$PSScriptRoot\sync\current_" + "$date" + ".csv" 

write-host "here" + $current_dated_file + " " + $current_file
    # Backup current.csv regardless of existance
    Copy-Item $current_file $current_dated_file
    
}
# The last line should be 
#<date>, <name>, <checkins>, <total time>
# we want to get that date fromt he last line and rename
# the current 

powershell -noprofile -executionpolicy bypass -file $PSScriptRoot\rfid_reader.ps1 --report > $current_file
