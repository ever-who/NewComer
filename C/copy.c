#include <stdio.h>



int main (int argc,char *argv[]){
        if(argc!=3){
                printf("args invalid ");
                return -1;
        }       
        FILE *source;
        FILE *out;

        source = fopen (argv[1],"r");
        if( NULL == source ){
                perror(argv[1]);
                return -2;
        }

        out = fopen (argv[2],"w");
        if( NULL == out ){
                perror(argv[2]);
                fclose(source);
                return -2;
        }

        char ch;
        while((ch=fgetc(source))!=EOF){
                fputc(ch,out);
        }

        fclose(source);
        fclose(out);
        
        return 0;
}
