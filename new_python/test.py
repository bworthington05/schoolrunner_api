import sys
import sr_endpoints
import database_setup

# Authentication credentials for SR API, supplied as command line arguments
# First argument = username, second argument is password

user = sys.argv[1]
pw = sys.argv[2]

url = 'https://renew.schoolrunner.org/api/v1/students?limit=30000'

data = sr_endpoints.get_students(url, user, pw)


db = '/home/ubuntu/workspace/databases/python_db1.db'

database_setup.create_students_table(db, data)




