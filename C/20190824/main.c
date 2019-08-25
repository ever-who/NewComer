#include <stdio.h>
#include <stdlib.h>

int max(int x,int y){
	return x>y?x:y;
}

int x=10;
int y=0;

int get_random_num();
void print_int_array(int *array,int size);
void add_random_array(int *array,int size,int (*get_random_num)());

int main(){
	printf("20190824 function pointer test\n");
	printf("max(x,y) = %d\n",max(x,y));
	int (*m)(int,int) = max;
	//printf("function pointer address =%x\n",max);
	printf("function pointer usage : m(x,y)=%d\n",m(x,y));
	
	//add to test more for function pointer 函数指针回调
	int size=10;
	int array[size];
	printf("sizeof(array)=%ld, sizeof(int)=%ld\n",sizeof(array),sizeof(int));
	add_random_array(array,size,get_random_num);
	print_int_array(array,size);

	//to test function address --- 使用函数地址调用函数
	printf("to test function address --- 使用函数地址调用函数\n");
	int print_int_array_address=(int)print_int_array;
	((void(*)(int*,int))(print_int_array_address))(array ,size);
	return 0;
}

void print_int_array(int *array,int size){
	for(int i=0;i<size; i++){
		printf("array[%d] = %d\n",i,array[i]);
	}
}

void add_random_array(int *array,int size,int (*get_random_num)()){
	//printf("add_random_array sizeof(array)=%d, sizeof(int)=%d\n",sizeof(*array),sizeof(int)); //传数组的指针进来之后，不好计算数组的长度,所以C一般要传size
	for(int i=0; i<size; i++){
		array[i]=get_random_num();
		printf("add_random_array array[%d] = %d\n",i,array[i]);
	}
	return;
}


int get_random_num(){
	return rand();
}
