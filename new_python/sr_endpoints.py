#!/usr/bin/python2.7
# -*- coding: utf-8 -*-
"""
This module contains methods that connect to Schoolrunner's API, pulls, and 
parses the data we want

"""

import requests
from requests.auth import HTTPBasicAuth
import json
import numpy

# General function that receives data from some endpoint, returns raw data to the fuction that
# called it so that the data can be further processed

# Requires endpoint URL, endpoint name, user, and pw
# Endpoint url MUST already include a parameter OR end in a ? so that "page=" can be appended to it below
# URL should not already include a page parameter though
# Endpoint name is he name that comes after "results" in the JSON data
def get_data(url, endpoint_name, user, pw):
    
    # We have to loop through the results a few times because it could have multiple pages
    bucket=[]   # Bucket for adding data from API one page at a time
    page = 1    # Start wtih the 1st page of the API
    total_pages = 2  # Just to get things started until we find out exactly how many pages there are
    
    while page <= total_pages:
        
        # New URL that updates page parameter
        new_url = url + '&page=' + str(page)
        
        response = requests.get(new_url, auth=HTTPBasicAuth(user, pw))
        raw_text = response.text
    
        # JSON parsing
        json_all = json.loads(raw_text)
        json_results = json_all['results']  # Drill down from all data to the "results" stuff
        json_single_page = json_results[endpoint_name]  # Drill down from "results" to the data for this specific endpoint
        
        json_meta = json_all['meta']
        total_pages = json_meta['total_pages']  # Reset total_pages to the true value once we get it
        
        bucket.extend(json_single_page)  # Add the current page of data to the bucket
        
        print 'received page ' + str(page) + ' of ' + str(total_pages) + ' from ' + endpoint_name + ' endpoint'
        
        page += 1  # Move on to the next page of data
        
    return bucket
    

# Get and parse students data    
def get_students(url, user, pw):
    
    endpoint_name = 'students'
    
    # Get the bucket of raw data
    bucket = get_data(url, endpoint_name, user, pw)
    
    final_data = []
    temp_data = []
    
    record_count = 0
    
    # Loop through the bucket and parse JSON data for the specific values we want to save
    # The order of these items is really important- it's the order they will be added to SQL database
    for i in bucket:
        temp_data.append(i.get('student_id'))
        temp_data.append(i.get('first_name'))
        temp_data.append(i.get('last_name'))
        temp_data.append(i.get('school_id'))
        temp_data.append(i.get('grade_level_id'))
        temp_data.append(i.get('external_id'))
        temp_data.append(i.get('sis_id'))
        temp_data.append(i.get('state_id'))
        temp_data.append(i.get('active'))
        
        # Append the temp data (like a row) to the end of the final data list,
        # append it as a tuple though so that this whole data set can be inserted
        # into a SQLite database at once
        final_data.append(tuple(temp_data))
        temp_data = []  # Clear the temp data bucket before the next loop
        
        record_count += 1
        
    print endpoint_name + ' endpoint complete, ' + str(record_count) + ' records retrieved'
    
    return final_data

