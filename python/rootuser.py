import sys, os

###################################################
# Global Constants
###################################################


###################################################

###################################################
def modify_apmk_file(filename,flag):
	moduleFile = open(filename, "r")
	moduleFileLines = moduleFile.readlines()
	moduleFile.close()
	#moduleContent = []
	os.remove(filename)
	usbfile = open(filename, "w")
	write_flag=0
	for line in moduleFileLines:
		if "BIRD_ROOT_USER" in line:
		   if flag == "rootuser":
		   	usbfile.write("BIRD_ROOT_USER = yes\n")
		   else :
			usbfile.write("BIRD_ROOT_USER = no\n")
		   write_flag=1
		   continue
		else :
		   usbfile.write(line)
	        if not moduleFileLines:	
	             break
	if write_flag==0 :
		if flag == "rootuser":
			usbfile.write("BIRD_ROOT_USER = yes\n")	
	usbfile.close()
	#return ''.join(moduleContent)


#############################################################
# test if the makefile is accessible
#
#      add by BIRD@hujingcheng 20191004 
#############################################################
def can_mk_access(path):
	if os.path.exists(path) :
		if os.path.isfile(path) :
			if os.access(path,os.R_OK) :
				if os.access(path,os.W_OK) :				
					print("path : "+path+" exist")
					return True
	return False


#############################################################
# get key-value mappings from makefile lines
# exclude the interference of annotation word '#'
#
#      add by BIRD@hujingcheng 20191004 
#############################################################
def get_value_from_makefile(line,key):
	if key != "" and line != "":
		line = line.strip()
		if "#" in line:
			if line[0] == "#":
				return ""
			else :
				origins = line.split("#")
				first = origins[0].strip()
				if key in first and "=" in first:
					mappings = first.split("=")
					if len(mappings) != 2:
						return ""
					elif key == mappings[0].strip() :
						return mappings[1].strip()
			return ""
		else:
			if key in line and "=" in line:
				mappings = line.split("=")
				if len(mappings) != 2:
					return ""
				elif key == mappings[0].strip() :
					return mappings[1].strip()			
	return ""		


#############################################################
# config macro in projectconfig.mk from parameter of script oneCmdMake
# no need to rewrite the makefile every time when "./oneCmdMake xxxx rootuser"
#
#      add by BIRD@hujingcheng 20191004 
#############################################################
def makefile_macro_configs(filename,flag):
	try:
		reader = open(filename, "r")
		moduleFileLines = reader.readlines()
	finally:
		reader.close()
	keyExistFlag=0
	rewriteFlag=False
	targetList=[]
	for line in moduleFileLines:
		if "BIRD_ROOT_USER" in line:
			value=get_value_from_makefile(line,"BIRD_ROOT_USER")
			value=value.strip("\n").strip()
			print("BIRD_ROOT_USER from mk value = "+value+",flag ="+flag)
			keyExistFlag += 1			
			if flag == "rootuser":
				if value == "yes":
					targetList.append(line)
					continue
				else:
					rewriteFlag=True
					targetList.append("BIRD_ROOT_USER = yes\n")
			else :
				if value != "yes":
					targetList.append(line)
					continue
				else:
					rewriteFlag=True
		else:
			targetList.append(line)
        	if not moduleFileLines:	
             		break
	if keyExistFlag==0 and flag == "rootuser":
		keyExistFlag +=1
		rewriteFlag=True
		targetList.append("BIRD_ROOT_USER = yes\n")
	if keyExistFlag>0 and rewriteFlag :
		with open(filename, "w") as writer:
			writer.writelines(targetList)


###################################################
# Main Script
###################################################
   
#for moduleFile in find_files(PROJECT_ROOT_DIR, INIT_USB_FILE):
if __name__ == '__main__':
	if not can_mk_access(sys.argv[1]) :
		print("path :"+sys.argv[1]+" cannot be access!")
		exit(1)
	makefile_macro_configs(sys.argv[1],sys.argv[2])
#	modify_apmk_file(sys.argv[1],sys.argv[2]) # abandon on 201910 for less time without regenerating ninja

###################################################
# End of File
###################################################
