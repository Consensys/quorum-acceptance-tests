#!/usr/bin/env bash
srcFilePath=$1
permission_type=$2

# java does not support string constants beyond 64K size
# this script processes solc generated java files to split up BYTECODE into multiple lines of smaller data size.
# this allows java compilation to pass for solc contracts with bytecode size > 64k
echo "process java source files from $srcFilePath ...permission_type=$permission_type"
gawk --version

cd $srcFilePath
for file in `ls -1 ./*.java` ; do
    if [ -f $file ]; then
        echo "Processing $file"
        mv "$file" "$file.bk"
        gawk '{ if( match($0, /^(.*?String BINARY = )"([^"]+)"/, m))
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
