#!/usr/bin/python2.7
# -*- coding: utf-8 -*-
"""
Created on July 2015

@author: Brandon Worthington
"""
#script that connects to SchoolRunner's API sync endpoint and emails specified recipients
#with info about recent syncs

import requests
from requests.auth import HTTPBasicAuth
import json
from datetime import timedelta
from datetime import datetime
import HTML
import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

#SchoolRunner authentication credentials
user = 'user@awesomeschool.org'
pw = 'password123'

#local time (CST) is 6 hours behind server time (UTC), this is used several times below for time conversions
local_time_difference = 6

#time referenced in email so we can know exactly when the data was pulled
now = (datetime.now() - timedelta(hours=local_time_difference)) #subtract from server time (UTC) to convert to local time (CST)
current_time = datetime.strftime(now, "%Y-%m-%d %H:%M:%S") #convert the "now" datetime to a string

#Connect to SR Sync API########################################################
##############################################################################

#API sync endpoint url
url = 'https://mynetwork.schoolrunner.org/api/v1/sync/summary/?limit=30000&page=1'

response = requests.get(url, auth=HTTPBasicAuth(user, pw))
rawdata = response.text

#JSON parsing
jsondata = json.loads(rawdata)
jsonresults = jsondata['results'] #drill down from all data to the "results" stuff

#Initialize lists for sync data we want to save (for the top SUMMARY portion of the email)
#########################################################################################

summary_start_time_SERVER = []
summary_end_time_SERVER = []
summary_start_time_CST = []
summary_end_time_CST = []
summary_sync_run_id = []
summary_sync_status_name = []
summary_sync_run_duration_hms = []
summary_number_of_tables = []

n=0
while n < 6: #start at first sync record returned, process only the 6 most recent syncs (approx. last 24 hrs)
    
    #get the sync start times
    str_time = jsonresults[n]['start_time'] #put string time into a temporary holding bin
    
    #if the start time is not None/null, it's ok to do the math to convert from server time to local time
    if str_time is not None:
        date_time = datetime.strptime(str_time,'%Y-%m-%d %H:%M:%S') #convert string time to real datetime
        cst_time = (date_time - timedelta(hours=local_time_difference)) #convert from server time to local time
        final_time = cst_time.strftime("%Y-%m-%d %H:%M:%S") #convert CST datetime to a string (final step)
        summary_start_time_SERVER.append(jsonresults[n]['start_time'])
        
    #if the start time is None/null, don't try to convert to local time (subtracting from null is an error)   
    else:
        final_time = 'null'
        summary_start_time_SERVER.append('null')
        
    summary_start_time_CST.append(final_time) #save the final string version of the CST time (whether it's 'null' or a real time)
    
    #get the sync end times
    str_time = jsonresults[n]['end_time'] #put string time into a temporary holding bin
    
    #if the end time is not None/null, it's ok to do the math to convert from server time to local time
    if str_time is not None:
        date_time = datetime.strptime(str_time,'%Y-%m-%d %H:%M:%S') #convert string time to real datetime
        cst_time = (date_time - timedelta(hours=local_time_difference)) #convert from server time to local time
        final_time = cst_time.strftime("%Y-%m-%d %H:%M:%S") #convert CST datetime to a string (final step)
        summary_end_time_SERVER.append(jsonresults[n]['end_time'])
        
    #if the end time is None/null, don't try to convert to local time (subtracting from null is an error)   
    else:
        final_time = 'null'
        summary_end_time_SERVER.append('null')
        
    summary_end_time_CST.append(final_time) #save the final string version of the CST time (whether it's 'null' or a real time)
    
    #get other important info for each sync summary
    summary_sync_run_id.append(jsonresults[n]['sync_run_id'])
    summary_sync_status_name.append(jsonresults[n]['sync_status_name'])
    summary_sync_run_duration_hms.append(jsonresults[n]['sync_run_duration_hms'])
    summary_number_of_tables.append(len(jsonresults[n]['sync_table_runs'])) #length of 'sync_table_runs' gives us the number of tables synced
    
    n = n+1 #move on to the next sync

#zip together the columns and rows we will use in the email
summary_sync_table = zip(summary_sync_run_id,summary_start_time_SERVER,summary_end_time_SERVER,
                   summary_start_time_CST,summary_end_time_CST,summary_sync_status_name,
                   summary_sync_run_duration_hms,summary_number_of_tables)

#column alignment in the table we're going to email
summary_table_align = ['center','center','center','center','center','center','center','center']

#headers for the table we're going to email
summary_table_headers = ['sync_run_id','start_time (SERVER)','end_time (SERVER)','start_time (CST)','end_time (CST)',
                 'sync_status_name','sync_run_duration_hms','number_of_tables_synced']

#Initialize lists for sync data we want to save (for the bottom DETAILED portion of the email)
#############################################################################################

start_time_SERVER = []
end_time_SERVER = []
start_time_CST = []
end_time_CST = []
sync_run_id = []
sync_status_name = []
sync_table_name = []
sync_table_run_duration_hms = []
total_change_count = []
total_completed_count = []

n=0
while n < 6: #start at first sync record returned, process only the 6 most recent syncs (last 24 hrs)

    #sync_table_runs is a list itself of 20 smaller lists with info about each table that synced
    #this gives us info about specific SIS tables that synced (students, courses, sections, etc.)
    json_sync_table_runs = jsonresults[n]['sync_table_runs'] #put "sync_table_runs" for each sync into temporary list
    
    m=0 #m used for index of json_sync_table_runs list
    while m != len(json_sync_table_runs): #save the info we want from each sync_table_run
    
        #get the table sync start times
        str_time = json_sync_table_runs[m]['start_time'] #put string time into a temporary holding bin
        
        #if the start time is not None/null, it's ok to do the math to convert from server time to local time
        if str_time is not None:
            date_time = datetime.strptime(str_time,'%Y-%m-%d %H:%M:%S') #convert string time to real datetime
            cst_time = (date_time - timedelta(hours=local_time_difference)) #convert from server time to local time
            final_time = cst_time.strftime("%Y-%m-%d %H:%M:%S") #convert CST datetime to a string (final step)
            start_time_SERVER.append(json_sync_table_runs[m]['start_time'])
            
        #if the start time is None/null, don't try to convert to local time (subtracting from null is an error)    
        else:
            final_time = 'null'
            start_time_SERVER.append('null')
            
        start_time_CST.append(final_time) #save the final string version of the CST time (whether it's 'null' or a real time)
        
        #get the table sync end times
        str_time = json_sync_table_runs[m]['end_time'] #put string time into a temporary holding bin
        
        #if the end time is not None/null, it's ok to do the math to convert from server time to local time
        if str_time is not None:
            date_time = datetime.strptime(str_time,'%Y-%m-%d %H:%M:%S') #convert string time to real datetime
            cst_time = (date_time - timedelta(hours=local_time_difference)) #convert from server time to local time
            final_time = cst_time.strftime("%Y-%m-%d %H:%M:%S") #convert CST datetime to a string (final step)
            end_time_SERVER.append(json_sync_table_runs[m]['end_time'])
            
        #if the end time is None/null, don't try to convert to local time (subtracting from null is an error)   
        else:
            final_time = 'null'
            end_time_SERVER.append('null')
        
        end_time_CST.append(final_time) #save the final string version of the CST time (whether it's 'null' or a real time)
        
        #get other important info for each table that was synced
        sync_run_id.append(json_sync_table_runs[m]['sync_run_id'])
        sync_status_name.append(json_sync_table_runs[m]['sync_status_name'])
        sync_table_name.append(json_sync_table_runs[m]['sync_table_name'])
        sync_table_run_duration_hms.append(json_sync_table_runs[m]['sync_table_run_duration_hms'])
        total_change_count.append(json_sync_table_runs[m]['total_change_count'])
        total_completed_count.append(json_sync_table_runs[m]['total_completed_count'])
        
        m = m+1 #move on to the next sync table (still within this current overall sync)
    
    n = n+1 #move on to the next overall sync

#zip together the columns and rows we will use in the email
sync_table = zip(sync_run_id,start_time_SERVER,end_time_SERVER,start_time_CST,end_time_CST,
                 sync_status_name,sync_table_name,sync_table_run_duration_hms,total_change_count,total_completed_count)

#column alignment in the table we're going to email
table_align = ['center','center','center','center','center','center','center','center','center','center']

#headers for the table we're going to email
table_headers = ['sync_run_id','start_time (SERVER)','end_time (SERVER)','start_time (CST)','end_time (CST)',
                 'sync_status_name','sync_table_name','sync_table_run_duration_hms','total_change_count',
                 'total_completed_count']

#Email########################################################################
##############################################################################

#email address and password used to send the email
fromaddress = 'from@awesomeschool.org'
password = 'password123'

#email recipients list
toaddress = ['recipient1@awesomeschool.org', 'recipient2@awesomeschool.org']

msg = MIMEMultipart('alternative')
msg['Subject'] = ('SchoolRunner Sync History as of '+ current_time)
msg['From'] = fromaddress
msg['To'] = ', '.join(toaddress)

#create the body of the message
detail_message = ('<p><strong>This table shows details about the 6 most recent SchoolRunner syncs as of '
            + current_time + '.</strong></p>')

summary_message = ('<p><strong>This table shows a summary of the 6 most recent SchoolRunner syncs as of '
            + current_time + '.</strong></p>')

body = (summary_message + HTML.table(summary_sync_table, header_row=summary_table_headers, col_align=summary_table_align) +
        detail_message + HTML.table(sync_table, header_row=table_headers, col_align=table_align))

part1 = MIMEText(body, 'html')

msg.attach(part1)
# Send the message via local SMTP server.
mail = smtplib.SMTP('smtp.gmail.com', 587)
mail.ehlo()
mail.starttls()
mail.login(fromaddress, password)
mail.sendmail(fromaddress, toaddress, msg.as_string())
mail.quit()

