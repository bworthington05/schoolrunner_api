#!/usr/bin/python2.7
# -*- coding: utf-8 -*-

import general_endpoint
import general_endpoint_inner_json

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
    
    
def get_incidents(url, user, pw):
    
    endpoint_name = 'incidents'
    
    fields = ['incident_id',
            'school_id', 
            'staff_member_id',
            'date',
            'incident_type_id',
            'short_description',
            'long_description',
            'from_date',
            'thru_date',
            'active',
            'display_name']

    # Get back a multidimensional list of data
    data = general_endpoint.get_data(url, endpoint_name, fields, user, pw)
    return data
    

# Be sure that the endpoint URL includes parameter "with_related=true"
# Same general endpoint as incidents, though, but includes inner layer of JSON lists
# for all the students tied to each incident
def get_incident_students(url, user, pw):
    
    endpoint_name = 'incidents'
    
    inner_json_name = 'incident_students'
    
    fields = ['incident_student_id',
            'incident_id', 
            'student_id',
            'incident_role_id',
            'minutes_out_of_class',
            'from_date',
            'thru_date',
            'active']

    # Get back a multidimensional list of data
    data = general_endpoint_inner_json.get_data(url, endpoint_name, inner_json_name, fields, user, pw)
    return data
    

# Be sure that the endpoint URL includes parameter "with_related=true"
# Same general endpoint as incidents, though, but includes inner layer of JSON lists
# for all the students tied to each incident
def get_incident_suspensions(url, user, pw):
    
    endpoint_name = 'incidents'
    
    inner_json_name = 'incident_suspensions'
    
    fields = ['incident_suspension_id',
            'incident_id', 
            'incident_student_id',
            'student_id',
            'num_days',
            'start_date',
            'suspension_type_id',
            'from_date',
            'thru_date',
            'active']

    # Get back a multidimensional list of data
    data = general_endpoint_inner_json.get_data(url, endpoint_name, inner_json_name, fields, user, pw)
    return data
    
    
def get_suspension_types(url, user, pw):
    
    endpoint_name = 'suspension_types'
    
    fields = ['suspension_type_id',
            'name', 
            'external_name',  # Name for PS import
            'school_id',
            'absence_type_id',
            'active',
            'display_name']

    # Get back a multidimensional list of data
    data = general_endpoint.get_data(url, endpoint_name, fields, user, pw)
    return data
