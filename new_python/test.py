import sys
import sr_endpoints
import database_setup

# Authentication credentials for SR API, supplied as command line arguments
# First argument = username, second argument is password

user = sys.argv[1]
pw = sys.argv[2]

url = 'https://renew.schoolrunner.org/api/v1/students?limit=2000'

students = sr_endpoints.get_students(url, user, pw)

url = 'https://renew.schoolrunner.org/api/v1/schools?limit=30000'

schools = sr_endpoints.get_schools(url, user, pw)

url = 'https://renew.schoolrunner.org/api/v1/incidents?limit=30000&with_related=true'

incidents = sr_endpoints.get_incidents(url, user, pw)

incident_students = sr_endpoints.get_incident_students(url, user, pw)

db = '/home/ubuntu/workspace/databases/python_db1.db'

database_setup.create_students_table(db, students)

database_setup.create_schools_table(db, schools)

database_setup.create_incidents_table(db, incidents)

database_setup.create_incident_students_table(db, incident_students)




