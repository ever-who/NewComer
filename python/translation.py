# -*- coding: UTF-8 -*-
import sys, os, xlrd, io

###################################################
# Global Constants
###################################################

#############################################################
# test if the makefile is accessible
#
#      add by BIRD@hujingcheng 20191004 
#############################################################
def can_xls_access(path):
    if os.path.exists(path) :
        if os.path.isfile(path) :
            if os.access(path,os.R_OK) :
                if os.path.getsize(path)>0:
                    print("xls : "+path+" exist ,size= "+str(os.path.getsize(path)))                    
                    return True
    return False

#############################################################
# define const for translation
#
#      add by hujingcheng 20210219 
#############################################################
TRANSLATION_DIR = "/home/ubuntu/bak/lenovo/M7/translation"
TARGET_ROW_INDEX = [75,76]
TARGET_COLUMN_INDEX = 2
TARGET_SHEET_NAME = "Sheet1"
TARGET_RES_DIR = "/mnt/work/code/lenovo/M7-tablet-20201208/packages/apps/Launcher3/res/"
TARGET_VALUES_SUFFIX="values-"
TARGET_STRING_XML="/strings.xml"
TARGET_STRING_ENDINGS ="</resources>"

#############################################################
# insert translation into target string.xml
#
#      add by hujingcheng 20210219 
#############################################################
def insert_translation(all_list):
    if len(all_list)>0:
        for target_list in all_list:
            if len(target_list)==3:
                target_file_path=TARGET_RES_DIR+TARGET_VALUES_SUFFIX+target_list[0]+TARGET_STRING_XML
            if not os.path.exists(target_file_path):
                print(target_file_path+" not exist insert_translation")
                continue
            with io.open(target_file_path, "r", encoding="utf-8") as f:
                lines = f.readlines()
            with io.open(target_file_path, "w", encoding="utf-8") as f_w:
                for line in lines:
                    if TARGET_STRING_ENDINGS in line:
                        append_string = "    <string name=\"allow_munis_one_title\">\""+target_list[1]+"\"</string>\n"
                        append_string = append_string+"    <string name=\"allow_munis_one_summary\">\""+target_list[2]+"\"</string>\n"
                        line = append_string+"</resources>"
                    f_w.write(line)

#############################################################
# read xls by path to get translation
#
#      add by hujingcheng 20210219 
#############################################################
def can_dir_access(path):
    if os.path.exists(path) :
        if os.path.isdir(path) :
           print("dir : "+path+" exist")
           return True
    return False


#############################################################
# read xls by path to get translation
#
#      add by hujingcheng 20210219 
#############################################################
def read_xls(path):
    all_list=[]
    if os.path.isdir(path):
        for root,dirs,files in os.walk(path):
            print(files)
            print("===================================================")            
            for i in range(len(files)-1,-1,-1):
                if files[i].endswith(".xls"):
                    locale = files[i].replace(".xls","")
                    xls_path = os.path.join(root,files[i])
                    print("name="+files[i]+",locale="+locale+",xls_path="+xls_path)
                    if can_xls_access(xls_path):
                        target_list = get_target_index(xls_path,TARGET_ROW_INDEX)
                        if len(target_list)>0:
                            #target_list.insert(0,xls_path)
                            target_list.insert(0,locale)        
                            all_list.append(target_list)
                        print(target_list)
    return all_list

#############################################################
# read xls by path to get translation
#
#      add by hujingcheng 20210219 
#############################################################
def get_target_index(path, row_list):
    target_list=[]
    data = xlrd.open_workbook(path)
    table = data.sheet_by_name(TARGET_SHEET_NAME)
    if len(row_list) >0:
        #print(table.cell(74,1).value)
        for row_index in row_list:
            #print("path = "+path+", cell("+str(row_index)+","+str(TARGET_COLUMN_INDEX)+")="+table.cell(row_index-1,TARGET_COLUMN_INDEX-1).value)
            target_list.append(table.cell(row_index-1,TARGET_COLUMN_INDEX-1).value)
    else:
        print("row_list is empty!")
    return target_list

###################################################
# Main Script
###################################################
   
#for insert translations into the right place
if __name__ == '__main__':
    if not can_dir_access(TRANSLATION_DIR) :
        print("dir :"+TRANSLATION_DIR+" cannot be enter!")
        exit(1)
    print("let's go")

    all_list = read_xls(TRANSLATION_DIR)
    insert_translation(all_list)
    
    
#    makefile_macro_configs(sys.argv[1],sys.argv[2])
#    modify_apmk_file(sys.argv[1],sys.argv[2]) # abandon on 201910 for less time without regenerating ninja

###################################################
# End of File
###################################################
