#include <stdio.h>
#include <limits.h>


int i=0;
int array_test();

int main(){
	int* point=&i;
	printf("i=%d, addr=%d\n",i,*point);
	printf("my test 01\n");
	printf("size of int = %lu\n",sizeof(int));
	while(i<5){
		printf("while test %d\n",i++);
	}
	for(;i<10;){
		printf("for test %d\n",i++);	
	}

	/* pointer test   */
	int p=1;
	int *pp=NULL;
	pp= &p;
	printf("pointer test p=%d\n",p);
	printf("pointer test &p=%p\n",&p);
	printf("pointer test *pp=%d\n",*pp);
	printf("pointer test pp=%p\n",pp);
	array_test();
	return 0;
}


int array_test(){
	int array[]={5,6,4};
	printf("size of array[]=%ld\n",sizeof(array)/sizeof(int));
	int nums[10];
	for(int j=0;j<10;j++){
		nums[j]=j;
	}
	return 0;
}
