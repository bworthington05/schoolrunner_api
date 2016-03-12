#!/usr/bin/python2.7
# -*- coding: utf-8 -*-

import general_endpoint

# These functions get a multidimensional list of records from a given Schoolrunner API endpoint
# by calling the general_endpoint's get_data() function and passing in arguments about the specific
# endpoint (URL, endpoint name, list of fields, username, password)

# Each function below requires the endpoint's url with any parameters and login credentials
# Endpoint URL MUST already include a parameter OR end in a ? so that "page=" can be appended to it below
# URL should not already include a page parameter though

def get_students(url, user, pw):
    
    endpoint_name = 'students'
    
    # List of fields that wil be pulled 
    # The order the items in this list is really important for saving data to SQL database
    fields = ['student_id',
            'first_name', 
            'last_name',
            'school_id',
            'grade_level_id',
            'external_id',  # PS student_id
            'sis_id',       # PS student_number
            'state_id',
            'active']
    
    # Get back a multidimensional list of data
    data = general_endpoint.get_data(url, endpoint_name, fields, user, pw)
    return data
    
    
def get_schools(url, user, pw):
    
    endpoint_name = 'schools'
    
    fields = ['school_id',
            'long_name', 
            'short_name',
            'display_name',
            'suffix',
            'external_id',  # PS school_id
            'min_grade_number',
            'max_grade_number']
    
    # Get back a multidimensional list of data
    data = general_endpoint.get_data(url, endpoint_name, fields, user, pw)
    return data

