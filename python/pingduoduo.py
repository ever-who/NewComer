import sys, os, time, random


###################################################
#   金猪印钞 -- 拼多多
#				
###################################################
def paper_money(num):
	if num <=1 :
		return
	os.system('adb shell input tap 901 1772')
	time.sleep(random.randint(2,4))
	os.system('adb shell input roll 0 3')
	for i in range(0,num):
		print(str(i)+" roll time")
		if i%2==0 :
			for j in range(0,20):
				time.sleep(random.randint(1,2))
				os.system('adb shell input roll 0 3')
		else :
			for j in range(0,20):
				time.sleep(random.randint(1,2))
				os.system('adb shell input roll 0 -3')
		time.sleep(random.randint(2,4))
		os.system('adb shell input tap 550 1515')
		
		
# Main Script
###################################################
if __name__ == '__main__':
	num=int(sys.argv[1])
	os.system('adb devices')
	paper_money(num)
###################################################
# End of File
###################################################