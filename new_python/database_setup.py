#!/usr/bin/python2.7
# -*- coding: utf-8 -*-
"""
This module contains methods that setup a SQLite database using data supplied
by the get_endpoint functions in the sr_endpoints module.get_students

"""

import sqlite3

def create_generic_table(database_path, table, create_statement, insert_statement, data):
    
    # Connect to the specified database
    db = sqlite3.connect(database_path)
    cursor = db.cursor()
    
    # Drop table if it already exists so we can start fresh
    cursor.execute('''DROP TABLE IF EXISTS ''' + table)
    db.commit()
    cursor.execute(create_statement)
    db.commit()
    print 'SQLite table created: ' + table
    
    # Insert the supplied dataset
    cursor.executemany(insert_statement, data)
    db.commit()
    print 'inserted ' + str(len(data)) + ' records into ' + table + ' table'
    
    db.close()
    
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


