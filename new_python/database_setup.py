#!/usr/bin/python2.7
# -*- coding: utf-8 -*-

import sqlite3

# Sets up a SQLite database and creates tables using data from Schoolrunner's API

# Generic function for creating a table
# Requires database file path, table name, create statement, insert statement, and dataset to be inserted)
def create_generic_table(database_path, table, create_statement, insert_statement, data):
    
    # Connect to the specified database
    con = sqlite3.connect(database_path)
    cursor = con.cursor()
    
    # Drop table if it already exists so we can start fresh
    cursor.execute('''DROP TABLE IF EXISTS ''' + table)
    con.commit()
    cursor.execute(create_statement)
    con.commit()
    print 'SQLite table created: ' + table
    
    # Insert the supplied dataset
    cursor.executemany(insert_statement, data)
    con.commit()
    print 'inserted ' + str(len(data)) + ' records into ' + table + ' table\n'
    
    con.close()

# Creates this specific table using a specific list of field names
# Inserts a given dataset into the table
def create_students_table(database_path, data):
    
    table = 'students'
    
    # Creat table with fields in EXACT SAME order as in the sr_endpoints module
    # Field names may be named slightly differently here though
    create_statement = ('''CREATE TABLE ''' + table + '''(
    
        sr_student_id TEXT PRIMARY KEY,
        first_name TEXT,
        last_name TEXT,
        sr_school_id TEXT,
        grade_level_id TEXT,
        ps_student_id TEXT,
        ps_student_number TEXT,
        state_id TEXT,
        active TEXT)''')
    
    insert_statement = '''INSERT INTO ''' + table + '''(
        sr_student_id, first_name, last_name, sr_school_id, grade_level_id, ps_student_id,
        ps_student_number, state_id, active) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)'''
    
    # Call the generic table making function to put this all together
    create_generic_table(database_path, table, create_statement, insert_statement, data)


def create_schools_table(database_path, data):
    
    table = 'schools'
    
    # Creat table with fields in EXACT SAME order as in the sr_endpoints module
    # Field names may be named slightly differently here though
    create_statement = ('''CREATE TABLE ''' + table + '''(
    
        sr_school_id TEXT PRIMARY KEY,
        long_name TEXT,
        short_name TEXT,
        display_name TEXT,
        suffix TEXT,
        ps_school_id TEXT,
        min_grade_number TEXT,
        max_grade_number TEXT)''')
    
    insert_statement = '''INSERT INTO ''' + table + '''(
        sr_school_id, long_name, short_name, display_name, suffix, ps_school_id,
        min_grade_number, max_grade_number) VALUES(?, ?, ?, ?, ?, ?, ?, ?)'''
    
    # Call the generic table making function to put this all together
    create_generic_table(database_path, table, create_statement, insert_statement, data)
    
    
def create_incidents_table(database_path, data):
    
    table = 'incidents'
    
    # Creat table with fields in EXACT SAME order as in the sr_endpoints module
    # Field names may be named slightly differently here though
    create_statement = ('''CREATE TABLE ''' + table + '''(
    
        incident_id TEXT PRIMARY KEY,
        sr_school_id TEXT,
        sr_staff_member_id TEXT,
        date TEXT,
        incident_type_id TEXT,
        short_description TEXT,
        long_description TEXT,
        from_date TEXT,
        thru_date TEXT,
        active TEXT,
        display_name TEXT)''')
    
    insert_statement = '''INSERT INTO ''' + table + '''(
        incident_id, sr_school_id, sr_staff_member_id, date, incident_type_id, short_description,
        long_description, from_date, thru_date, active, display_name) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'''
    
    # Call the generic table making function to put this all together
    create_generic_table(database_path, table, create_statement, insert_statement, data)


def create_incident_students_table(database_path, data):
    
    table = 'incident_students'
    
    # Creat table with fields in EXACT SAME order as in the sr_endpoints module
    # Field names may be named slightly differently here though
    create_statement = ('''CREATE TABLE ''' + table + '''(
    
        incident_student_id TEXT PRIMARY KEY,
        incident_id TEXT,
        sr_student_id TEXT,
        incident_role_id TEXT,
        minutes_out_of_class TEXT,
        from_date TEXT,
        thru_date TEXT,
        active TEXT)''')
    
    insert_statement = '''INSERT INTO ''' + table + '''(
        incident_student_id, incident_id, sr_student_id, incident_role_id, minutes_out_of_class,
        from_date, thru_date, active) VALUES(?, ?, ?, ?, ?, ?, ?, ?)'''
    
    # Call the generic table making function to put this all together
    create_generic_table(database_path, table, create_statement, insert_statement, data)
    
def create_incident_suspensions_table(database_path, data):
    
    table = 'incident_suspensions'
    
    # Creat table with fields in EXACT SAME order as in the sr_endpoints module
    # Field names may be named slightly differently here though
    create_statement = ('''CREATE TABLE ''' + table + '''(
    
        incident_suspension_id TEXT PRIMARY KEY,
        incident_id TEXT,
        incident_student_id TEXT,
        sr_student_id TEXT,
        num_days TEXT,
        start_date TEXT,
        suspension_type_id TEXT,
        from_date TEXT,
        thru_date TEXT,
        active TEXT)''')
    
    insert_statement = '''INSERT INTO ''' + table + '''(
        incident_suspension_id, incident_id, incident_student_id, sr_student_id, num_days,
        start_date, suspension_type_id, from_date, thru_date, active) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'''
    
    # Call the generic table making function to put this all together
    create_generic_table(database_path, table, create_statement, insert_statement, data)
    
    
def create_suspensions_types_table(database_path, data):
    
    table = 'suspension_types'
    
    # Creat table with fields in EXACT SAME order as in the sr_endpoints module
    # Field names may be named slightly differently here though
    create_statement = ('''CREATE TABLE ''' + table + '''(
    
        suspension_type_id TEXT PRIMARY KEY,
        name TEXT,
        ps_name TEXT,
        sr_school_id TEXT,
        absence_type_id TEXT,
        active TEXT,
        display_name TEXT)''')
    
    insert_statement = '''INSERT INTO ''' + table + '''(
        suspension_type_id, name, ps_name, sr_school_id, absence_type_id,
        active, display_name) VALUES(?, ?, ?, ?, ?, ?, ?)'''
    
    # Call the generic table making function to put this all together
    create_generic_table(database_path, table, create_statement, insert_statement, data)


