# test on 20190203 for python 3.5 
if True:
	print("test 01 ")
else:
	print("test failed")
# 注意同一代码块中缩进应保持一致	
print("发发发")

str="AceKing"
# 字符串索引从0开始，str[0] = A 第一个字符
print(str[0])
print(str[-2])
# AceK  -2:倒数第二个 [str[0],str[-2])
print(str[0:-2])
# *代表重复
print(str[0]*3)
# +代表字符串拼接
print(str[0]+"gree 5")
# \代表转义字符 
print(str[0]+"\teeeeee")
# r"\n....."字符串前加r，表示不转义
print(str[0]+r"\nffeee")

# 输入
input("please input")