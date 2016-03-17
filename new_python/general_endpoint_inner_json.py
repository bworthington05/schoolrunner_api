#!/usr/bin/python2.7
# -*- coding: utf-8 -*-

import requests
from requests.auth import HTTPBasicAuth
import json
import numpy

# General function that receives data from some Schoolrunner API endpoint and parses
# the results according to a list of specific fields passed into this function

# Use this function specifically for endpoints where the records have one extra inner layer of JSON lists

# Requires endpoint URL, endpoint name, list of fields, inner json list name, username, and password
# Endpoint URL MUST already include a parameter OR end in a ? so that "page=" can be appended to it below
# URL should not already include a page parameter though
# Endpoint name is the name that comes after "results" in the JSON data
# Inner JSON name is the name of the inner JSON lists in each record that need to be further parsed 
# Fields is a list of the fieldnames being pulled
def get_data(url, endpoint_name, inner_json_name, fields, user, pw):
    
    # We have to loop through the results a few times because it could have multiple pages
    bucket=[]   # Bucket for adding data from API one page at a time
    page = 1    # Start wtih the 1st page of the API
    total_pages = 2  # Just to get things started until we find out exactly how many pages there are
    
    while page <= total_pages:
        
        # New URL that updates the page parameter
        new_url = url + '&page=' + str(page)
        
        response = requests.get(new_url, auth=HTTPBasicAuth(user, pw))
        raw_text = response.text
    
        # JSON parsing
        json_all = json.loads(raw_text)
        json_results = json_all['results']  # Drill down from all data to the "results" stuff
        json_single_page = json_results[endpoint_name]  # Drill down from "results" to the data for this specific endpoint
        
        json_meta = json_all['meta']
        total_pages = json_meta['total_pages']  # Reset total_pages to the true value once we get it
        
        # Print meta data (total pages, total records, etc.)
        if page == 1:
            print endpoint_name + ' endpoint meta data:'
            for keys, values in json_meta.items():
                print str(keys) + ' : ' + str(values)
            print
        
        bucket.extend(json_single_page)  # Add the current page of data to the bucket
        
        print 'received page ' + str(page) + ' of ' + str(total_pages) + ' from ' + endpoint_name + ' endpoint'
        
        page += 1  # Move on to the next page of data

    final_data = []  # List to hold processed data that will be returned to client function
    temp_data = []   # Temporary holding bin for each record
    
    record_count = 0  # counter to track how many records have been processed

    # Loop through each item in the bucket
    # Parse out the inner JSON lists and then loop through each item in fields list
    # Parse the JSON data and save the fields we want
    for i in bucket:
        inner_json_bin = i[inner_json_name]
        
        for j in inner_json_bin:
            for f in fields:
                temp_data.append(j.get(f))
        
            # Append the temp data (like a row) to the end of the final data list,
            # append it as a tuple so that this whole data set can be inserted into a SQLite database at once
            final_data.append(tuple(temp_data))
            temp_data = []  # Clear the temp data bin before the next loop
        
            record_count += 1
        
    print inner_json_name + ' of ' + endpoint_name + ' endpoint complete, ' + str(record_count) + ' records retrieved\n'
        
    return final_data
    
    