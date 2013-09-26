#! /bin/sh

if [[ $# -ne 2 ]]; then
    echo "usage: $0 war-file ssh-host"
    exit -1
fi

scp $1 $2:/var/lib/tomcat7/webapps/ROOT.war
