#!/usr/bin/env bash
srcFilePath=$1
tags=$2

echo "process java source files from $srcFilePath ...tags=$tags"
permissionBasic=`echo $tags|grep "permissions-basic"|wc -l`
permissionEea=`echo $tags|grep "permissions-eea"|wc -l`
echo "permissionBasic=$permissionBasic permissionEea=$permissionEea"

cd $srcFilePath
for file in `ls -1 ./*.java` ; do
    if [ -f $file ]; then
        echo "Processing $file"
        mv "$file" "$file.bk"
        awk '{ if( match($0, /^(.*?String BINARY = )"([^"]+)"/, m))
            {
                print m[1], "new StringBuilder()"
                n = split (m[2], ary, /.{1,200}/, seps);
                for (i=1; i<=n; i++)
                   printf ".append(\"%s\")%s", seps[i], (i<n ? "\n" : ".toString();\n")
           } else {
                print $0
           }
        }' "$file.bk" > "$file"
        rm "$file.bk"
    fi
done
