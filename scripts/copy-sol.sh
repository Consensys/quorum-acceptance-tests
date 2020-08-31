#!/usr/bin/env bash
srcFilePath=$1
tags=$2

echo "copying permission solc files from $srcFilePath/permissions/{eea|basic} ...tags=$tags"
permissionBasic=`echo $tags|grep "permissions-basic"|wc -l`
permissionEea=`echo $tags|grep "permissions-eea"|wc -l`
echo "permissionBasic=$permissionBasic permissionEea=$permissionEea"

if [ $permissionBasic -eq 1 ];then
  echo "cp $srcFilePath/permissions/basic/*.sol $srcFilePath/"
  cp $srcFilePath/permissions/basic/*.sol $srcFilePath/
fi

if [ $permissionEea -eq 1 ];then
  echo "cp $srcFilePath/permissions/eea/*.sol $srcFilePath/"
  cp $srcFilePath/permissions/eea/*.sol $srcFilePath/
fi
