import sys, os, time, random


###################################################
# repeat operation 
#      ---  coordination for xiaomi MIX2S
###################################################
def taobao_coin_repeat(num):
	os.system('adb devices')
	for i in range(1,num):
		print(str(i)+" start")
		time.sleep(random.randint(2,4))
		os.system('adb shell input tap 906 1842')#1333
		time.sleep(random.randint(6,9))
		count=0
		for j in range(1,random.randint(3,4)):
			os.system('adb shell input roll 0 3')
			time.sleep(random.randint(3,5))
			count=+1
		time.sleep(random.randint(16-count*3,18-count*3))
		os.system('adb shell input keyevent 4')

# Main Script
###################################################
if __name__ == '__main__':
	num=int(sys.argv[1])+1
	taobao_coin_repeat(num)

###################################################
# End of File
###################################################