#!/bin/bash

dbuser="mmsuser"
usedb=

mvn clean -P purge
echo
while [[ -n "$1" ]]; do
    #statements
    case "$1" in
        -u | --user) 
            shift
            case "$1" in
                -*) 
                    echo "* * * * * * * * * * * "
                    echo ""
                    echo "No user name supplied"
                    echo "Using user $USER"
                    echo ""
                    echo "* * * * * * * * * * * "
                    dbuser=$USER
                    sleep 2
                    break;;
            esac

            if [[ -n "$1" ]]; then
                #statements
                dbuser="$1"
                echo "Starting server as user $dbuser"
                sleep 1
            fi
            ;;
        -db | --database)  
            shift
            if [[ -n "$1" ]]; then
                #statements
                usedb="$1"
                echo "Using database : $usedb"
                sleep 1
            else
                usedb="mydb"
                echo "Using default database : $usedb"
                sleep 1
            fi
            ;;
        *) 
            echo "* * * * * * * * * * * "
            echo ""
            echo "$1 is not an option"
            echo ""
            echo "* * * * * * * * * * * "
            sleep 4
            ;;
    esac
    shift
done

#Configurations that need a suffix with a database name to run psql command
psql --set ON_ERROR_STOP=on -U $dbuser -f ./src/main/java/gov/nasa/jpl/view_repo/db/mms.sql $usedb 

psql_exit_status=$?

sleep 4

if [[ $psql_exit_status != 0 ]]; then
    #statements
    psql --set ON_ERROR_STOP=on -U $dbuser -f ./src/main/java/gov/nasa/jpl/view_repo/db/mms.sql mydb
    psql_exit_status=$?

fi

if [[ $psql_exit_status != 0 ]]; then
    #statements
    psql --set ON_ERROR_STOP=on -U $USER -f ./src/main/java/gov/nasa/jpl/view_repo/db/mms.sql mydb
    psql_exit_status=$?
fi

if [[ $psql_exit_status == 0 ]]; then
    mvn jrebel:generate
    sleep 2
    ./runserver.sh
fi
