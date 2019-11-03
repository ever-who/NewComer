import sys, os, time, random


###################################################
# repeat operation 1 工坊
#      ---  coordination for xiaomi MIX2S
###################################################
def papa_3_workshop(num):
	os.system('adb devices')
	for i in range(1,num):
		print(str(i)+" start")
		time.sleep(random.randint(2,4))
		os.system('adb shell input tap 758 841')#英雄模式
		time.sleep(random.randint(2,4))
		os.system('adb shell input tap 1235 906')#征战button
		time.sleep(random.randint(4,6))
		os.system('adb shell input tap 2045 916')#开战
		time.sleep(random.randint(3,5))
		os.system('adb shell input tap 1562 950')#跳过
		time.sleep(random.randint(45,50))
		os.system('adb shell input tap 1109 926')#退出
		time.sleep(random.randint(3,5))
		os.system('adb shell input tap 1005 904')#结算页退出
		time.sleep(random.randint(5,8))
		
###################################################
# repeat operation 2 武将擂台
#      ---  coordination for xiaomi MIX2S
###################################################
def papa_3_gold_card(num):
	os.system('adb devices')
	for i in range(1,num):
		print(str(i)+" start")
		time.sleep(random.randint(2,4))
		os.system('adb shell input tap 1524 891')#英雄模式
		time.sleep(random.randint(6,8))
		os.system('adb shell input tap 2045 916')#开战
		time.sleep(random.randint(3,5))
		os.system('adb shell input tap 1562 950')#跳过
		time.sleep(random.randint(45,50))
		os.system('adb shell input tap 1109 926')#退出
		time.sleep(random.randint(15,20))
		os.system('adb shell input tap 1530 687')#继续解锁
		time.sleep(random.randint(5,8))

# Main Script
###################################################
if __name__ == '__main__':
	num=int(sys.argv[2])+1
	if sys.argv[1]=="-g":
		papa_3_gold_card(num)
	elif sys.argv[1]=="-w":
		papa_3_workshop(num)

###################################################
# End of File
###################################################