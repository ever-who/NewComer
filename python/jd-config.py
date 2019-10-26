import sys, os, time, random


###################################################
# repeat operation 
#      ---  coordination for xiaomi MIX2S
###################################################
def jd_gold_coin_repeat(num):
	os.system('adb devices')
	for i in range(1,num):
		print(str(i)+" start")
		time.sleep(random.randint(3,5))
		os.system('adb shell input tap 923 895')#1333
		time.sleep(random.randint(6,9))
		os.system('adb shell input keyevent 4')
		time.sleep(random.randint(3,5))
		os.system('adb shell input tap 565 1475')

# Main Script
###################################################
if __name__ == '__main__':
	num=int(sys.argv[1])
	jd_gold_coin_repeat(num)

###################################################
# End of File
###################################################