# -*- coding: UTF-8 -*-
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
		
		
###################################################
# repeat operation 3 新年擂台
#      ---  coordination for xiaomi MIX2S
###################################################
def papa_3_new_year(num):
	os.system('adb devices')
	for i in range(1,num):
		print(str(i)+" start")
		time.sleep(random.randint(2,4))
		os.system('adb shell input tap 851 534')#化敌为友
		time.sleep(random.randint(2,4))
		os.system('adb shell input tap 1550 644')#征战
		time.sleep(random.randint(6,8))
		os.system('adb shell input tap 2045 916')#开战
		time.sleep(random.randint(3,5))
		os.system('adb shell input tap 1562 950')#跳过
		time.sleep(random.randint(95,100))
		os.system('adb shell input tap 1109 926')#退出
		time.sleep(random.randint(5,10))
		os.system('adb shell input tap 851 534')#抽卡
		time.sleep(random.randint(5,8))
		os.system('adb shell input tap 851 534')#抽卡
		time.sleep(random.randint(5,8))
		os.system('adb shell input tap 1100 940')#继续
		time.sleep(random.randint(5,8))

###################################################
# repeat operation 4 富商快跑
#      ---  coordination for xiaomi MIX2S
###################################################
def papa_3_rich_merchant(num):
	os.system('adb devices')
	for i in range(1,num):
		print(str(i)+" start")
		time.sleep(random.randint(2,4))
		os.system('adb shell input tap 1116 874')#征战
		time.sleep(random.randint(4,5))
		os.system('adb shell input tap 2045 916')#开战
		time.sleep(random.randint(35,40))
		os.system('adb shell input tap 1562 950')#跳过
		time.sleep(random.randint(5,10))
		os.system('adb shell input tap 1100 529')#天降鸿福抽奖
		time.sleep(random.randint(3,5))
		os.system('adb shell input tap 1100 529')#天降鸿福退出
		time.sleep(random.randint(2,4))
		os.system('adb shell input tap 2078 62')#关闭
		time.sleep(random.randint(2,4))

# Main Script
###################################################
if __name__ == '__main__':
	num=int(sys.argv[2])+1
	if sys.argv[1]=="-g":
		papa_3_gold_card(num)
	elif sys.argv[1]=="-w":
		papa_3_workshop(num)
	elif sys.argv[1]=="-y":
		papa_3_new_year(num)
	elif sys.argv[1]=="-r":
	    papa_3_rich_merchant(num)

###################################################
# End of File
###################################################