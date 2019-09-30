import sys, os, fnmatch, re, shutil

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

#

###################################################
# Main Script
###################################################
   
#for moduleFile in find_files(PROJECT_ROOT_DIR, INIT_USB_FILE):
if __name__ == '__main__':
	modify_apmk_file(sys.argv[1],sys.argv[2])

###################################################
# End of File
###################################################
