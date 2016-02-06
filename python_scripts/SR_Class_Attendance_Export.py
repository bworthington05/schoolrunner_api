#!/usr/bin/python2.7
# -*- coding: utf-8 -*-
"""
Created on Wed Sept 16 00:00:00 2015

@author: Brandon Worthington
"""
#Script that connects to SchoolRunner API to pull class attendance and saves it
#it in a spreadsheet that can be imported into PowerSchool

import cx_Oracle
import requests
from requests.auth import HTTPBasicAuth
import json
import numpy
from datetime import date, timedelta

##Authentication credentials for all SR API connections#######################
##############################################################################

user = 'user@awesomeschool.org'
pw = 'password123'

#Connect to SR Class Absences API for current school###########################
###############################################################################

#today's date used in attendance comments
today = date.today().strftime("%Y-%m-%d")

#min_date used for API URL parameters, today minus x days
min_date = (date.today() - timedelta(days=7)).strftime("%Y-%m-%d")

#we have to loop through the connection to the API a few times because it could
#have multiple pages

att_bucket=[]  #bucket for adding data from API one page at a time
page = 1  #start wtih the 1st page of the API
total_pages = 2  #just to get things started until we find out exactly how many pages are in the API

while page <= total_pages:
    
    #url parameters: active, school_id = 5 (RAHS), min_date
    url = 'https://renew.schoolrunner.org/api/v1/class-absences/?limit=30000&page='+str(page)+'&active=1&school_ids=5&min_date='+min_date
    response = requests.get(url, auth=HTTPBasicAuth(user, pw))
    raw_data = response.text

    #JSON stuff
    json_data = json.loads(raw_data)
    json_results = json_data['results'] #drill down from all data to the "results" stuff
    json_att = json_results['class_absences'] #drill down from "results" to "class_absences"stuff
    
    meta = json_data['meta']
    total_pages = meta['total_pages']  #resets total_pages to the true value once we get it from the API
    
    att_bucket.extend(json_att)  #add the current page of data to the bucket
    page += 1  #move on to the next page of data
    
#initialize lists for the fields we want to keep
att_student_id=[]
att_date=[]
att_absence_type_id=[]
att_section_period_id=[]

#parse JSON data into the lists above
for i in att_bucket:
    att_student_id.append(i.get('student_id'))
    att_date.append(i.get('date'))
    att_absence_type_id.append(i.get('absence_type_id'))
    att_section_period_id.append(i.get('section_period_id'))

#Connect to SR Section Period API #############################################
###############################################################################

#we have to loop through the connection to the API a few times because it could
#have multiple pages

sp_bucket=[]  #bucket for adding data from API one page at a time
page = 1  #start wtih the 1st page of the API
total_pages = 2  #just to get things started until we find out exactly how many pages are in the API

while page <= total_pages:
    
    url = 'https://renew.schoolrunner.org/api/v1/section-periods/?limit=30000&page='+str(page)
    response = requests.get(url, auth=HTTPBasicAuth(user, pw))
    raw_data = response.text

    #JSON stuff
    json_data = json.loads(raw_data)
    json_results = json_data['results'] #drill down from all data to the "results" stuff
    json_sp = json_results['section-periods'] #drill down from "results" to "section-periods"stuff
    
    meta = json_data['meta']
    total_pages = meta['total_pages']  #resets total_pages to the true value once we get it from the API
    
    sp_bucket.extend(json_sp)  #add the current page of data to the bucket
    page += 1  #move on to the next page of data
    
#initialize lists for the fields we want to keep
sp_section_period_id=[]
sp_school_id=[]
sp_section_id=[]

#parse JSON data into the lists above
for i in sp_bucket:
    sp_section_period_id.append(i.get('section_period_id'))
    sp_school_id.append(i.get('school_id'))
    sp_section_id.append(i.get('section_id'))

#Connect to SR Section API ####################################################
###############################################################################

#we have to loop through the connection to the API a few times because it could
#have multiple pages

sec_bucket=[]  #bucket for adding data from API one page at a time
page = 1  #start wtih the 1st page of the API
total_pages = 2  #just to get things started until we find out exactly how many pages are in the API

while page <= total_pages:
    
    url = 'https://renew.schoolrunner.org/api/v1/sections/?limit=30000&page='+str(page)
    response = requests.get(url, auth=HTTPBasicAuth(user, pw))
    raw_data = response.text

    #JSON stuff
    json_data = json.loads(raw_data)
    json_results = json_data['results'] #drill down from all data to the "results" stuff
    json_sec = json_results['sections'] #drill down from "results" to "section"stuff
    
    meta = json_data['meta']
    total_pages = meta['total_pages']  #resets total_pages to the true value once we get it from the API
    
    sec_bucket.extend(json_sec)  #add the current page of data to the bucket
    page += 1  #move on to the next page of data
    
#initialize lists for the fields we want to keep
sec_section_id=[]
sec_school_id=[]
sec_external_id=[]

#parse JSON data into the lists above
for i in sec_bucket:
    sec_section_id.append(i.get('section_id'))
    sec_school_id.append(i.get('school_id'))
    sec_external_id.append(i.get('external_id'))

    
#Connect to SR Students API ###################################################
###############################################################################

url = 'https://renew.schoolrunner.org/api/v1/students/?limit=30000&page=1&school_ids=5'

response = requests.get(url, auth=HTTPBasicAuth(user, pw))
raw_data = response.text

#JSON stuff
json_data = json.loads(raw_data)
json_results = json_data['results'] #drill down from all data to the "results" stuff
json_students = json_results['students']  #drill down from "results" stuff to "students" stuff

#initialize lists for the fields we want to keep
stu_student_id=[]
stu_sis_id=[]

#parse JSON data into the lists above
for i in json_students:
    stu_student_id.append(i.get('student_id'))
    stu_sis_id.append(i.get('sis_id'))

#Connect to SR Absence Types API #############################################
###############################################################################

url = 'https://renew.schoolrunner.org/api/v1/absence-types'

response = requests.get(url, auth=HTTPBasicAuth(user, pw))
raw_data = response.text

#JSON stuff
json_data = json.loads(raw_data)
json_results = json_data['results'] #drill down from all data to the "results" stuff
json_atttypes = json_results['absence-types']  #drill down from "results" stuff to "absence-types" stuff

#initialize lists for the fields we want to keep
atttypes_absence_type_id=[]
atttypes_code=[]
atttypes_external_id=[]

#parse JSON data into the lists above
for i in json_atttypes:
    atttypes_absence_type_id.append(i.get('absence_type_id'))
    atttypes_code.append(i.get('code'))
    atttypes_external_id.append(i.get('external_id'))         

#Connect to PowerSchool Oracle database########################################
##############################################################################

host = 'powerschoolhost'
port = 1521
SID = 'something'
dsn_tns = cx_Oracle.makedsn(host, port, SID)
username = 'psnavigator'
password = 'password123'
con = cx_Oracle.connect(username, password, dsn_tns)
cur = con.cursor()

#SQL code for getting sections from PowerSchool
#just get sections for the school and specific terms we care about
cur.execute('select\
                to_char(s.course_number),\
                to_char(s.section_number),\
                to_char(s.expression),\
                to_char(s.termid),\
                to_char(s.id),\
                to_char(s.dcid)\
            from sections s\
            where s.schoolid = 5 and s.termid in (2501, 2502, 2503, 2504)')

ps_sections=[]
for result in cur:
    ps_sections.append(result)

#split ps_sections into separate lists
ps_cn, ps_sn, ps_ex, ps_ti, ps_sec_id, ps_sec_dcid = zip(*ps_sections)

#convert PowerSchool expressions to period_id...
#there's probably a much better way to do this directly in the SQL code
#these period IDs are specific to the school

ps_period=[]
for i in ps_ex:
    if i == '1(A)':
        ps_period.append('639')
    elif i == '2(A)':
        ps_period.append('640')
    elif i == '3(A)':
        ps_period.append('641')
    elif i == '4(A)':
        ps_period.append('642')
    elif i == '5(A)':
        ps_period.append('643')
    elif i == '6(A)':
        ps_period.append('644')
    elif i == '7(A)':
        ps_period.append('751')
    else:
        print ('error with expression ' + str(i) + ' at index ' + str(ps_ex.index(i)))

#Match att_section_period_id to sp_section_period_id to get sp_section_id#####
##############################################################################

att_section_id=[]  #this list will hold the section_ids we're about to get

for i in att_section_period_id:
    
    #find i in sp_section_period_id list, save that index as x
    x = sp_section_period_id.index(i)
    
    #get that corresponding sp_seciton_id using index x
    att_section_id.append(sp_section_id[x])

#Match att_section__id to sec_section_id to get sec_external_id###############
##############################################################################

att_external_id=[]  #this list will hold the external_ids (PS section_ids) we're about to get

for i in att_section_id:
    
    #find i in sec_section_id list, save that index as x
    x = sec_section_id.index(i)
    
    #get that corresponding sec_external_id using index x
    att_external_id.append(sec_external_id[x])   

#Match att_external_id to PS section ID to get PowerSchool stuff##############
##############################################################################

att_ps_course_number=[]  #this list will hold the PS course_number
att_ps_section_number=[]  #this list will hold the PS section_number
att_ps_period=[]  #this list will hold the PS period
att_ps_termid=[]  #this list will hold the PS expression

for i in att_external_id:
    
    if i in ps_sec_id:
        #find i in ps_sec_id list, save that index as x
        x = ps_sec_id.index(i)
        
        #get that corresponding PowerSchool stuff using index x
        att_ps_course_number.append(ps_cn[x])
        att_ps_section_number.append(ps_sn[x])
        att_ps_period.append(ps_period[x])
        att_ps_termid.append(ps_ti[x])
    
    else:
        att_ps_course_number.append('No matching ps_sec_id')
        att_ps_section_number.append('No matching ps_sec_id')
        att_ps_period.append('No matching ps_sec_id')
        att_ps_termid.append('No matching ps_sec_id')

#Match att_student__id to stu_student_id to get stu_si_id######################
##############################################################################

att_student_number=[]  #this list will hold the stu_sis_ids (PS student_numbers) we're about to get

for i in att_student_id:
    
    #find i in stu_student_id list, save that index as x
    x = stu_student_id.index(i)
    
    #get that corresponding stu_sis_id using index x
    att_student_number.append(stu_sis_id[x])

#Get the actual attendance codes and PS attendance code ids####################
##############################################################################

att_code=[]  #these lists will hold the attendance codes/ids we're about to get
att_code_id=[]

for i in att_absence_type_id:
    
    #find i in atttypes_absence_type_id, save that index as x
    x = atttypes_absence_type_id.index(i)
    
    #get the corresponding attendance codes/ids using index x
    att_code.append(atttypes_code[x])
    att_code_id.append(atttypes_external_id[x])

#Make column with att_mode_code = "ATT_ModeMeeting" ##########################
##############################################################################
att_mode_code=[]
att_interval=[]
att_comment=[]

for i in att_code:
    att_mode_code.append('ATT_ModeMeeting')
    att_interval.append('0')
    att_comment.append("Uploaded on " + today)

#Assemble the spreadsheet that will be imported into PowerSchool##############
##############################################################################

combined = zip(att_student_number, att_date, att_code, att_comment, att_mode_code, att_interval, 
               att_ps_course_number, att_ps_section_number, att_ps_period)

fieldnames = ('student_number', 'att_date', 'attendance code', 'att_comment', 'att_mode_code', 'att_interval', 
              'course_number', 'section_number', 'periodID')

combined.insert(0, fieldnames)
               
filename = 'C:/Users/Brandon Worthington/Desktop/RAHS_Att/RAHS_meeting_attendance_'+today+'.txt'
             
numpy.savetxt(filename, combined, delimiter='\t', fmt="%s")