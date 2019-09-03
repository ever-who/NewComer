#include <stdio.h>
#include <string.h>

/**
* create by hujingcheng 20190903 to test strtok 
* strtok原理：查找分割字符，替换为'\0'
*/

int split(char **array,int size,char *origin,const char *delimeter);
void print_char_array(char **array,char size );

int main(){
	char hello[16]="hello_my_girl";
	printf("strtok test enter ,%s\n",hello);

	// split string hello by "_"	分割测试
	char *p;
	p=strtok(hello,"_");//注意：此时hello字符串，已经改变;原hello字符串已经存入strtok函数
	while(p!=NULL){
		printf("split 1st p=%s,hello=%s\n",p,hello); //取分割的字符需要在 再次分割之前
		p=strtok(NULL,"_");//第一个参数取NULL，意味着使用之前存入的字符串
	}
	printf("split 1st p=%s,hello=%s\n",p,hello);

	// test the situation when the spliter isn't included in the origin string 分割不含分隔符的字符串
	char empty[16]="dddiwffffo";
	char *p1;
        p1=strtok(empty,"_");
        while(p1!=NULL){
                printf("split 2nd p1=%s,hello=%s\n",p1,empty);//若原字符串中不包含分割字符，则原字串不会改变
                p1=strtok(NULL,"_");
        }
        printf("split 1st p1=%s,hello=%s\n",p1,empty); 

	//test split strings and store into arrays 分割并存入字串数组
	int size = 4;
	char *array[size];
	char origin[16]="sda-fewf s111";
	const char *delimeter=" -_"; //分隔符可以有多个
	int result = split(array,size,origin,delimeter);
	if(result <0){
		printf("split not success\n");
		return -1;
	}
	print_char_array(array,size);
	return 0;
}


int split(char **array,int size,char *origin,const char *delimeter){
	char *tmp;
	tmp = strtok(origin, delimeter);
	int i=0;
	while(tmp!=NULL){
		*(array+i) = tmp;
		i++;
		tmp =strtok(NULL,delimeter);
		if(tmp!=NULL && i>size-1 ){
			printf("array full in size\n");
			return -1;
		}
	}
	return 1;
}


void print_char_array(char **array,char size ){
	for(int i=0;i<size;i++){
		if(*(array+i)==NULL){
			return;
		}
		printf("array [%d]= %s \n",i,*(array+i));
	}
}

