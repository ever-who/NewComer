#!/bin/bash

function collect()
{
	for i in $(git status .)
	do
		if [ -e $i -o -d $i ];then
			if [ x$i == x"rsync.sh" -o x$i == x"TrustKernel_patch" ];then
				echo "----------"
				echo $i
				echo "----------"
			else
				echo "$i in-rsync"
				rsync -Rr $i $patch_folder/
			fi
		else
			echo "$i not exist"
		fi
	done
}

ROOT_PATH=$(pwd)
PATCH_NAME="simplehome"

dir0="idh.code/bird"

list="$dir0"
echo "=================="
echo $list
for dir in $list;
do
	echo "------------"
	echo $dir
	patch_folder="$ROOT_PATH/$PATCH_NAME/$dir"
	mkdir -p $patch_folder
	cd $dir 
	collect
	cd $ROOT_PATH
done
