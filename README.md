# schoolrunner_api
The purpose of this project is to use SchoolRunner's API and do stuff with data that is not possible or practical through the app.  Source code is written in Java and SQLite (plus a few simple stand-alone Python scripts).

Here's the general idea:

1. Connect to one or more of SR's API endpoints (students, assessments, courses, etc.).
2. Pasrse the JSON results and save the data to a SQLite database.
3. Use SQLite to query the database to get the specific information we want.

Possible applications:

*Identifying assessments that are missing objectives and the teachers who created them (by linking the assessments and staff tables) and automatically emailing those staff with the assessments that need to be fixed.

### Requirements
This code requires a few .jar files be installed first:

Two files related to actually connecting to the API are: **okhttp-2.5.0.jar** and **okio-1.6.0.jar**.  Find out more and download them from http://square.github.io/okhttp/

**json-simple.1.1.1.jar** is for JSON parsing.  Find out more and download it from https://code.google.com/p/json-simple/

**sqlite-jdbc-3.8.11.2.jar** is for the SQLite database.  Find out more and download it from http://www.tutorialspoint.com/sqlite/sqlite_java.htm

**javax.mail.jar** is for sending emails.  Find out more and download it from https://java.net/projects/javamail/pages/Home  You can also see some examples of using Java to send emails at http://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/

### How It's Organized
**building_blocks** is a folder that contains the core building blocks of this project:

* **ConnectToSRAPI** is the class that handles the connection to a given API endpoint.
* **Login** is the class that reads in a username and password for basic authentication in the API connection.
* **CreateDatabase** is the class that creates a database.
* **CreateTable** is the class that creates a given table within the database.
* **AssessmentsAPI** is the class that connects to the Assessments endpoint (using ConnectToSRAPI).  It can use a default URL or receive a specified URL (for example, with more parameters) through an overloaded constructor.  It parses the JSON results and saves the data to the relevant table of the database.  The same idea holds true for other similar classes related to specific endpoints (AbsencesAPI, StudentsAPI, StaffAPI, etc.).  These classes all extend an abstract superclass called GeneralEndpointAPI.
* **DatabaseSetup** is the class that manages setting up an entire database.  It uses the CreateDatabase and CreateTable classes to setup a database with specific tables.  It provides public methods that can be invoked in other classes to set up a database with any combination of tables we want.  The methods that set up a given table, like Assessments, also call on the related EndpointAPI class, like AssessmentsAPI, to go ahead and connect to the API and populate the table with that data.  When the DatabaseSetup constructor is used, that calls on the Login class to get the username and password, which are passed into the EndpointAPI objects and then to the ConnectToSRAPI object.
* **SendEmail** is a class that contains a method send() that sends an email via Gmail SMTP server.

**unaligned_assessments** is a folder that contains code for identifying assessments that are missing objectives and then emailing teachers with instructions to fix the assessments.

* **AssessmentsWithoutObjectives** is a driver class that identifies assessments not aligned to objectives. It relies on the "has_objectives=0" parameter in the Assessments endpoint URL.  It pulls in data from several API endpoints (Schools, Courses, Assessments, Staff_Members) and puts the data in a database.  Then it queries the database with SQLite to get the desired records and outputs a .csv file with the assessment info, course, school, and staff member.  Finally, it utilizes the EmailAssessmentsWithoutObjectives class to email the teachers who created each assessment not aligned to objectives.  See PDF image of example email in the example_outputs folder.

* **EmailAssessmentsWithoutObjectives** is a class that uses the SendEmail class to send email notifications to staff who created an assessment in SchoolRunner that is not aligned to objectives.  It contains the specific text for the email.  The constructor requires arrary lists with information about the assessments (course, staff email, assessment name, date, etc.).  These array lists are created in the AssessmentsWithoutObjectives class.  It loops through the array lists and sends an email to the staff member who created each assessment.

**SuspensionComparisonAcrossSchools** contains a driver class that calculates the average number of out-of-school suspension days per thousand students for each school in the network and saves the results as a .csv file.  Note: this is based on daily attendance data and assumes there is a separate attendance code for out-of-school suspensions that is recorded when students are suspended 1 or more days.

**python_scripts** contains some simple scripts written in Python 2.7.

* **SR_Sync_Check (Python)** is a simple script that generates a report about recent syncs with whatever SIS SchoolRunner connects to.  It then emails the report to a specified list of recipients. 

This script requires a few modules that may not be included with a standard python installation:

* HTML module to generate the tables in the email: http://www.decalage.info/python/html
* Requests module to connect to API and parse JSON: http://docs.python-requests.org/en/latest

It can be useful to have this script run automatically on a daily basis to confirm the sync is running successfully.  One option to make this happen easily is a site called pythonanywhere (https://www.pythonanywhere.com).  It's an online IDE and web hosting service that has a great scheduled tasks feature for automatically running python scripts.

* **SR_Class_Attendance_Export (Python)** is a script that pulls class/meeting attendance from the SchoolRunner API and saves it in a .txt file (see corresponding example) that can be imported into PowerSchool.  This script requires connecting to PowerSchool's Oracle database to get some information about courses, sections, and periods.  It uses cx_Oracle module to do this.  Check out: http://cx-oracle.sourceforge.net

Updated 2/6/2016
