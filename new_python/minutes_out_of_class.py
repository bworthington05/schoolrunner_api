import sys
import sr_endpoints
import database_setup
import sqlite3
import numpy

# Authentication credentials for SR API, supplied as command line arguments
# First argument = username, second argument is password

user = sys.argv[1]
pw = sys.argv[2]

# Database file path
db = '/home/ubuntu/workspace/databases/python_db1.db'

# SR API endpoints
students_url = 'https://renew.schoolrunner.org/api/v1/students?limit=30000'
schools_url = 'https://renew.schoolrunner.org/api/v1/schools?limit=30000'
incidents_url = 'https://renew.schoolrunner.org/api/v1/incidents?limit=30000&with_related=true&min_date=2015-07-22'
suspension_types_url = 'https://renew.schoolrunner.org/api/v1/suspension_types?limit=30000'

# Get data from endpoints
students = sr_endpoints.get_students(students_url, user, pw)
schools = sr_endpoints.get_schools(schools_url, user, pw)
incidents = sr_endpoints.get_incidents(incidents_url, user, pw)
incident_students = sr_endpoints.get_incident_students(incidents_url, user, pw)
suspension_types = sr_endpoints.get_suspension_types(suspension_types_url, user, pw)

# Save data in SQLite database
database_setup.create_students_table(db, students)
database_setup.create_schools_table(db, schools)
database_setup.create_incidents_table(db, incidents)
database_setup.create_incident_students_table(db, incident_students)
database_setup.create_suspensions_types_table(db, suspension_types)

# Connect to database and use SQL to get back desired info 
conn = sqlite3.connect(db)
print 'Opened database successfully'

select_statement = (

        'SELECT ' 
            'schools.display_name, ' 
            'students.ps_student_number, ' 
            'students.last_name, ' 
            'students.first_name, ' 
            
            # If the total is 0, then use null
            # If null, then replace with message
            'IFNULL((CASE WHEN min_out_of_class.total IS 0 THEN NULL ELSE min_out_of_class.total END), "No minutes or consequence recorded"), ' 

            # Divide total minutes out of class by 360 to get approximate number of equivalent school days
            'ROUND((CAST(min_out_of_class.total AS FLOAT) / 360), 1) ' 
                
        'FROM incident_students ' 
            'LEFT OUTER JOIN incidents ON incident_students.incident_id = incidents.incident_id ' 
            'LEFT OUTER JOIN students ON incident_students.sr_student_id = students.sr_student_id ' 
            'LEFT OUTER JOIN schools ON incidents.sr_school_id = schools.sr_school_id ' 
            
              # Sub-query to get total # of minutes out of class for each student who has at least some minutes recorded
              'LEFT OUTER JOIN ( ' 
                  'SELECT ' 
                      'incident_students.sr_student_id, ' 
                      'SUM(incident_students.minutes_out_of_class) as total ' 
                  'FROM incident_students ' 
                      'LEFT OUTER JOIN incidents ON incident_students.incident_id = incidents.incident_id ' 
                      'LEFT JOIN students ON incident_students.sr_student_id = students.sr_student_id ' 
                      
                  # Make sure BOTH the actual incident and incident_student record are active
                  'WHERE incidents.active = "1" AND incident_students.active = "1" ' 
                  'GROUP BY students.sr_student_id ' 
              ') min_out_of_class ON incident_students.sr_student_id = min_out_of_class.sr_student_id ' 

        'WHERE incidents.active = "1" AND incident_students.active = "1" ' 
        'GROUP BY students.sr_student_id ' 
        'ORDER BY schools.display_name, min_out_of_class.total DESC, students.last_name, students.first_name;')

results = conn.execute(select_statement)
results_list = list(results)

column_names = ('school', 'student_number', 'last_name', 'first_name', 'total_min_out_of_class', 'approx_days')
results_list.insert(0, column_names)
filename = '/home/ubuntu/workspace/output/min_out_of_class.txt'
numpy.savetxt(filename, results_list, delimiter='\t', fmt="%s")
print 'Saved file successfully'

conn.close()
print 'Closed database successfully'




