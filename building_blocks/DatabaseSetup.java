//class that contains methods to set up database and create tables to store data
//from SchoolRunner's various API endpoints

public class DatabaseSetup {
    
    //database file name
    private String dbName;
    
    private static String schoolsTableName = "SCHOOLS";
    private static String schoolsTableStatement = (
        "(LONG_NAME             TEXT," +
        " SR_SCHOOL_ID          TEXT    PRIMARY KEY     NOT NULL, " + 
        " PS_SCHOOL_ID          TEXT, " + 
        " DISPLAY_NAME          TEXT, " + 
        " SHORT_NAME            TEXT)");
        
    private static String studentsTableName = "STUDENTS";
    private static String studentsTableStatement = (
        "(FIRST_NAME            TEXT," +
        " LAST_NAME             TEXT, " +
        " SR_SCHOOL_ID          TEXT, " +
        " GRADE_LEVEL_ID        TEXT, " +
        " SR_STUDENT_ID         TEXT    PRIMARY KEY     NOT NULL, " + 
        " PS_STUDENT_NUMBER     TEXT, " + 
        " PS_STUDENT_ID         TEXT, " +
        " UID                   TEXT, " +
        " ACTIVE                TEXT)");
        
    private static String staffTableName = "STAFF";
    private static String staffTableStatement = (
        "(FIRST_NAME            TEXT," +
        " LAST_NAME             TEXT, " +
        " DISPLAY_NAME          TEXT, " +
        " SR_SCHOOL_ID          TEXT, " +
        " TITLE                 TEXT, " +
        " SR_STAFF_MEMBER_ID    TEXT    PRIMARY KEY     NOT NULL, " + 
        " SR_USER_ID            TEXT, " + 
        " PS_TEACHER_NUMBER     TEXT, " + 
        " PS_TEACHER_ID         TEXT, " +
        " EMAIL                 TEXT, " +
        " ACTIVE                TEXT)");
    
    private static String coursesTableName = "COURSES";
    private static String coursesTableStatement = (
        "(COURSE_ID             TEXT    PRIMARY KEY     NOT NULL, " +
        " COURSE_NAME           TEXT," + 
        " SR_SCHOOL_ID          TEXT, " + 
        " DISPLAY_NAME          TEXT, " + 
        " ACTIVE                TEXT)");
        
    private static String assessmentsTableName = "ASSESSMENTS";
    private static String assessmentsTableStatement = (
        "(ASSESSMENT_ID         TEXT    PRIMARY KEY     NOT NULL, " +
        " SR_SCHOOL_ID          TEXT," + 
        " SR_STAFF_MEMBER_ID    TEXT, " + 
        " COURSE_ID             TEXT, " + 
        " ASSESSMENT_DATE       TEXT, " +
        " ASSESSMENT_NAME       TEXT, " +
        " ASSESSMENT_TYPE_ID    TEXT, " +
        " AVG_SCORE             TEXT, " +
        " PRESENT_STUDENTS      TEXT, " +
        " ENROLLED_STUDENTS     TEXT, " +
        " ACTIVE                TEXT)");
        
    private static String absenceTypesTableName = "ABSENCE_TYPES";
    private static String absenceTypesTableStatement = (
        "(ABSENCE_TYPE_ID       TEXT    PRIMARY KEY     NOT NULL, " +
        " ABSENCE_NAME          TEXT," + 
        " ABSENCE_CODE          TEXT, " + 
        " IN_SCHOOL             TEXT, " + 
        " DISPLAY_NAME          TEXT)");
        
    private static String absencesTableName = "ABSENCES";
    private static String absencesTableStatement = (
        "(ABSENCE_ID            TEXT    PRIMARY KEY     NOT NULL, " +
        " SR_SCHOOL_ID          TEXT," + 
        " SR_STUDENT_ID         TEXT, " + 
        " SR_STAFF_MEMBER_ID    TEXT, " + 
        " ABSENCE_DATE          TEXT, " +
        " ABSENCE_TYPE_ID       TEXT, " +
        " ACTIVE                TEXT)");
    
    private Login login = new Login();
    
    //constructor, runs through login procedures to get username and password for connecting to API
    public DatabaseSetup() {
        
        System.out.println("SCHOOLRUNNER LOGIN");
        login.setUsername();
        login.setPassword();
    }
    
    //overloaded constructor, runs through login procedures to get username and password for connecting to API
    //also creates a database with a file name passed in as a parameter
    public DatabaseSetup(String dbName) {
        
        System.out.println("SCHOOLRUNNER LOGIN");
        login.setUsername();
        login.setPassword();
        
        this.dbName = dbName;
        CreateDatabase database = new CreateDatabase(this.dbName);
        database.run();
    }
    
    //get the String database name
    public String getDatabaseName() {
        return this.dbName;
    }
    
    //method to create database, requires the database file name as a parameter
    public void createDatabase(String dbName) {
        this.dbName = dbName;
        CreateDatabase database = new CreateDatabase(this.dbName);
        database.run();
    }
    
    //method to create schools table and load in data from API, uses default endpoint from SchoolsAPI class
    public void createSchoolsTable() {  
        CreateTable schoolsTable = new CreateTable(dbName, schoolsTableName, schoolsTableStatement);
        schoolsTable.run();
        SchoolsAPI schoolsAPI = new SchoolsAPI(login.getUsername(), login.getPassword(), dbName, schoolsTableName);
        schoolsAPI.run();
    }
    
    //overloaded method to create schools table and load in data from API, uses custom endpoint
    public void createSchoolsTable(String endpoint) {  
        CreateTable schoolsTable = new CreateTable(dbName, schoolsTableName, schoolsTableStatement);
        schoolsTable.run();
        SchoolsAPI schoolsAPI = new SchoolsAPI(login.getUsername(), login.getPassword(), dbName, schoolsTableName, endpoint);
        schoolsAPI.run();
    }
    
    //method to create students table and load in data from API, uses default endpoint from StudentsAPI class
    public void createStudentsTable() {   
        CreateTable studentsTable = new CreateTable(dbName, studentsTableName, studentsTableStatement);
        studentsTable.run();
        StudentsAPI studentsAPI = new StudentsAPI(login.getUsername(), login.getPassword(), dbName, studentsTableName);
        studentsAPI.run();
    }
    
    //overloaded method to create students table and load in data from API, uses custom endpoint
    public void createStudentsTable(String endpoint) {   
        CreateTable studentsTable = new CreateTable(dbName, studentsTableName, studentsTableStatement);
        studentsTable.run();
        StudentsAPI studentsAPI = new StudentsAPI(login.getUsername(), login.getPassword(), dbName, studentsTableName, endpoint);
        studentsAPI.run();
    }
    
    //method to create staff table and load in data from API, uses default endpoint from StaffAPI class
    public void createStaffTable() {   
        CreateTable staffTable = new CreateTable(dbName, staffTableName, staffTableStatement);
        staffTable.run();
        StaffAPI staffAPI = new StaffAPI(login.getUsername(), login.getPassword(), dbName, staffTableName);
        staffAPI.run();
    }
    
    //overloaded method to create staff table and load in data from API, uses custom endpoint
    public void createStaffTable(String endpoint) {   
        CreateTable staffTable = new CreateTable(dbName, staffTableName, staffTableStatement);
        staffTable.run();
        StaffAPI staffAPI = new StaffAPI(login.getUsername(), login.getPassword(), dbName, staffTableName, endpoint);
        staffAPI.run();
    }
    
    //method to create courses table and load in data from API, uses default endpoint from CoursesAPI class
    public void createCoursesTable() {   
        CreateTable coursesTable = new CreateTable(dbName, coursesTableName, coursesTableStatement);
        coursesTable.run();
        CoursesAPI coursesAPI = new CoursesAPI(login.getUsername(), login.getPassword(), dbName, coursesTableName);
        coursesAPI.run();
    }
    
    //overloaded method to create courses table and load in data from API, uses custom endpoint
    public void createCoursesTable(String endpoint) {   
        CreateTable coursesTable = new CreateTable(dbName, coursesTableName, coursesTableStatement);
        coursesTable.run();
        CoursesAPI coursesAPI = new CoursesAPI(login.getUsername(), login.getPassword(), dbName, coursesTableName, endpoint);
        coursesAPI.run();
    }
    
    //method to create assessments table and load in data from API, uses default endpoint from AssessmentsAPI class  
    public void createAssessmentsTable() {
        CreateTable assessmentsTable = new CreateTable(dbName, assessmentsTableName, assessmentsTableStatement);
        assessmentsTable.run();
        AssessmentsAPI assessmentsAPI = new AssessmentsAPI(login.getUsername(), login.getPassword(), dbName, assessmentsTableName);
        assessmentsAPI.run();
    }
    
    //overloaded method to create assessments table and load in data from API, uses custom endpoint   
    public void createAssessmentsTable(String endpoint) {
        CreateTable assessmentsTable = new CreateTable(dbName, assessmentsTableName, assessmentsTableStatement);
        assessmentsTable.run();
        AssessmentsAPI assessmentsAPI = new AssessmentsAPI(login.getUsername(), login.getPassword(), dbName, assessmentsTableName, endpoint);
        assessmentsAPI.run();
    }
    
    //method to create absence types table and load in data from API, uses default endpoint from AbsenceTypesAPI class  
    public void createAbsenceTypesTable() {
        CreateTable absenceTypesTable = new CreateTable(dbName, absenceTypesTableName, absenceTypesTableStatement);
        absenceTypesTable.run();
        AbsenceTypesAPI absenceTypesAPI = new AbsenceTypesAPI(login.getUsername(), login.getPassword(), dbName, absenceTypesTableName);
        absenceTypesAPI.run();
    }
    
    //overloaded method to create absence types table and load in data from API, uses custom endpoint   
    public void createAbsenceTypesTable(String endpoint) {
        CreateTable absenceTypesTable = new CreateTable(dbName, absenceTypesTableName, absenceTypesTableStatement);
        absenceTypesTable.run();
        AbsenceTypesAPI absenceTypesAPI = new AbsenceTypesAPI(login.getUsername(), login.getPassword(), dbName, absenceTypesTableName, endpoint);
        absenceTypesAPI.run();
    }
    
    //method to create absence types table and load in data from API, uses default endpoint from AbsencesAPI class  
    public void createAbsencesTable() {
        CreateTable absencesTable = new CreateTable(dbName, absencesTableName, absencesTableStatement);
        absencesTable.run();
        AbsencesAPI absencesAPI = new AbsencesAPI(login.getUsername(), login.getPassword(), dbName, absencesTableName);
        absencesAPI.run();
    }
    
    //overloaded method to create absence table and load in data from API, uses custom endpoint   
    public void createAbsencesTable(String endpoint) {
        CreateTable absencesTable = new CreateTable(dbName, absencesTableName, absencesTableStatement);
        absencesTable.run();
        AbsencesAPI absencesAPI = new AbsencesAPI(login.getUsername(), login.getPassword(), dbName, absencesTableName, endpoint);
        absencesAPI.run();
    }
    
} //end class DatabaseSetup