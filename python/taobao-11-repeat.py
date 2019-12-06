import sys, os, time, random


###################################################
# repeat operation 
#      ---  coordination for xiaomi MIX2S 
#  店铺button (906,1842);会场button (891,1294);
#  order : 0 -- 店铺 ， 1 -- 会场
###################################################
def taobao_coin_repeat(num,order):
	if num <=1 :
		return
	for i in range(0,num):
		time.sleep(random.randint(2,4))
		if order == 0:
			print(str(i+1)+" shop start")
			os.system('adb shell input tap 906 2018')# shop
		elif order ==1:
			print(str(i+1)+" hall start")
			os.system('adb shell input tap 891 1456')# hall
		time.sleep(random.randint(6,9))
		count=0
		for j in range(1,random.randint(3,4)):
			os.system('adb shell input roll 0 3')
			time.sleep(random.randint(3,5))
			count=+1
		time.sleep(random.randint(16-count*3,18-count*3))
		os.system('adb shell input keyevent 4')


###################################################
#   get adb devices SerialNo
# 
#   return SerialNo
###################################################
def get_devices_serials():
	device_list_op = os.popen('adb devices')
	device_list = device_list_op.readlines()
	print(device_list)
	devices = []
	for line in device_list :
		line = line.strip('\n').strip()
		print("--- lines --- "+line)
		if line == "List of devices attached":
			continue
		elif line != "":
			paras = line.split("\t")
			if paras[0]!="" and paras[1]=="device":
				devices.append(paras[0])
	return devices


# Main Script
###################################################
if __name__ == '__main__':
	shop_num=int(sys.argv[1])
	hall_num= int(sys.argv[2])
	devices = get_devices_serials()
	print(devices)
	taobao_coin_repeat(shop_num,0)
	taobao_coin_repeat(hall_num,1)

###################################################
# End of File
###################################################