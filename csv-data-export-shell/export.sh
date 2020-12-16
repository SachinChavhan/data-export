#!/bin/bash

date=$(date '+%Y-%m-%d-%H:%M:%S')

if [ $# -ne 6 ]; then
   echo "bash export.sh <zkNodes> <zkPath> <customer> <datamodel> <usdm-y/n> <plo-y/n> <custom-y/n>"
    exit 1;
fi

error_handler(){

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [ERROR] - $1"
	exit 1
}


# Grab command arguments
ZK_NODES="$1"
ZK_PATH="$2"
ZK_CUSTOMER="$3"
USDM=$(echo $4 | cut -f2 -d-)
PLO=$(echo $5 | cut -f2 -d-)
CUSTOM=$(echo $6 | cut -f2 -d-)

# Construct zookeeper paths
ZK_EXPORT="$ZK_PATH/$ZK_CUSTOMER"
ZK_DB_PARAMETERS="$ZK_EXPORT/dbParameters"
ZK_DB_HOST="$ZK_DB_PARAMETERS/hostname"
ZK_DB_PORT="$ZK_DB_PARAMETERS/port"
ZK_DB_NAME="$ZK_DB_PARAMETERS/database"
ZK_DB_USER="$ZK_DB_PARAMETERS/username"
ZK_DB_PASSWORD="$ZK_DB_PARAMETERS/password"
ZK_DB_SCHEMA="$ZK_DB_PARAMETERS/schema"
ZK_SFTP="$ZK_EXPORT/sftpParameters"
ZK_SFTP_HOST="$ZK_SFTP/hostname"
ZK_SFTP_PASSWORD="$ZK_SFTP/password"
ZK_SFTP_USER="$ZK_SFTP/username"
ZK_SFTP_LOCATION="$ZK_SFTP/sftplocation"
ZK_REPOSITORY="$ZK_EXPORT/repository"
ZK_CCDM_REPO="$ZK_REPOSITORY/ccdm"
ZK_QUERY_REPO="$ZK_REPOSITORY/query"
ZK_DIN_CUST_REPO="$ZK_REPOSITORY/din-customer"
ZK_DIN_CUST_TAG="$ZK_DIN_CUST_REPO/tag"
ZK_DIN_CUST_XML="$ZK_DIN_CUST_REPO/xmllocation"
ZK_CCDM_TAG="$ZK_CCDM_REPO/tag"

# Other common constants
WORK_DIR="/home/comprehend"
LOG_FILE="$WORK_DIR/log"
ZOOKEEPER="zookeepercli"
LOCAL_DATA_DIR="$WORK_DIR"
REPO_PATH="/home/comprehend/repo"
QUERY_REPO_PATH="/home/comprehend/repo/query"
CCDM_REPO_PATH="/home/comprehend/repo/ccdm"
CUST_REPO_PATH="/home/comprehend/repo/din-customer"

# Obtain info from zookeeper
echo "===> Obtaining credentials from zookeeper..."
DB_HOST=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_DB_HOST)
echo "===> DB host: $DB_HOST"
DB_PORT=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_DB_PORT)
echo "===> DB port: $DB_PORT"
DB_NAME=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_DB_NAME)
echo "===> DB name: $DB_NAME"
DB_USER=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_DB_USER)
echo "===> DB user: $DB_USER"
DB_PASSWORD=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_DB_PASSWORD)
echo "===> DB password: (masked)"
DB_SCHEMA=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_DB_SCHEMA)
echo "===> DB schema: $DB_SCHEMA"
SFTP_HOST=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_SFTP_HOST)
echo "===> Sftp host: $SFTP_HOST"
SFTP_USER=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_SFTP_USER)
echo "===> Sftp user: $SFTP_USER"
SFTP_PASSWORD=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_SFTP_PASSWORD)
echo "===> Sftp password: (masked)"
SFTP_LOCATION=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_SFTP_LOCATION)
echo "===> Sftplocation: $SFTP_LOCATION"
CCDM_REPO=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_CCDM_REPO)
echo "===> Ccdm repo: $CCDM_REPO"
QUERY_REPO=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_QUERY_REPO)
echo "===> query repo: $QUERY_REPO"
CUST_REPO=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_DIN_CUST_REPO)
echo "===> Cust repo: $CUST_REPO"
CUST_XML=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_DIN_CUST_XML)
echo "===> Cust xml: $CUST_XML"
CUST_TAG=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_DIN_CUST_TAG)
echo "===> Cust tag: $CUST_TAG"
CCDM_TAG=$($ZOOKEEPER --servers $ZK_NODES -c get $ZK_CCDM_TAG)
echo "===> ccdm tag: $CCDM_TAG"

echo "===> Cloning ccdm repository..."
REPO_CCDM_NAME=$(echo "$CCDM_REPO" | sed 's/.*\/\(.*\)\.git/\1/')
printf "host github.com\n HostName github.com\n IdentityFile $WORK_DIR/vault/${REPO_CCDM_NAME}_deploy_private_key\n" >> ~/.ssh/config
git clone $CCDM_REPO --branch $CCDM_TAG --depth 1 $CCDM_REPO_PATH
pushd $CCDM_REPO_PATH

echo "===> Cloning query repository..."
REPO_QUERY_NAME=$(echo "$QUERY_REPO" | sed 's/.*\/\(.*\)\.git/\1/')
printf "host github.com\n HostName github.com\n IdentityFile $WORK_DIR/vault/${REPO_QUERY_NAME}_deploy_private_key\n" >> ~/.ssh/config
git clone $QUERY_REPO --depth 1 $QUERY_REPO_PATH
pushd $QUERY_REPO_PATH

echo "===> Cloning din-customer repository..."
DIN_REPO_NAME=$(echo "$CUST_REPO" | sed 's/.*\/\(.*\)\.git/\1/')
printf "host github.com\n HostName github.com\n IdentityFile $WORK_DIR/vault/${DIN_REPO_NAME}_deploy_private_key\n" >> ~/.ssh/config
git clone $CUST_REPO --branch $CUST_TAG --depth 1 $CUST_REPO_PATH
pushd $CUST_REPO_PATH


echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER : creating local export directory and copying data from db"

mkdir -p "$LOCAL_DATA_DIR"

CQS_DICT_PATH="$CCDM_REPO_PATH/resources/validation/cqs/global_cqs/"

XML_PATH="$CUST_REPO_PATH/$CUST_XML"
ls -lrt $XML_PATH
cp $XML_PATH $QUERY_REPO_PATH
cp $CQS_DICT_PATH/cqs_dictionary.py $QUERY_REPO_PATH
ls -lrt $QUERY_REPO_PATH
echo "=========== $QUERY_REPO_PATH"

export_data_tables(){
    DATA_MODEL=$2
    LOCAL_PATH=$WORK_DIR/$DATA_MODEL/$date
	mkdir -p $LOCAL_PATH
	for table in $(echo $1 | sed "s/,/ /g"); do
	    
		query="select * from $DB_SCHEMA.$table"
		
		PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "COPY ($query) TO STDOUT WITH CSV HEADER" | gzip > $LOCAL_PATH/$table.csv.gz
		
		if [ "$?" -eq "1" ]; then
			error_handler "$ZK_CUSTOMER $DATA_MODEL : copy command failed. Table : $table"
		fi
		
	done
	
	echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER $DATA_MODEL : copied data from db successful"

	echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER $DATA_MODEL : transferring data to remote location"

	sshpass -p $SFTP_PASSWORD scp -o StrictHostKeyChecking=no -r $WORK_DIR/$DATA_MODEL  $SFTP_USER@$SFTP_HOST:$SFTP_LOCATION
	
	if [ "$?" -eq "1" ]; then
			error_handler "$ZK_CUSTOMER $DATA_MODEL : failed to transfer data to remote location"
	fi

	echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER $DATA_MODEL : Transferred data to remote location successful"
	
	echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER $DATA_MODEL : unzipping data at remote location"

	sshpass -p $SFTP_PASSWORD ssh -o StrictHostKeyChecking=no  $SFTP_USER@$SFTP_HOST 'gunzip -r '$SFTP_LOCATION/$DATA_MODEL
	
	if [ "$?" -eq "1" ]; then
			error_handler "$ZK_CUSTOMER $DATA_MODEL : failed unzipping at remote location"
	fi

	echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER $DATA_MODEL : unzipped data at remote location successful"

	echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER $DATA_MODEL : CSV export process completed successfully"
}

export_data_custom(){
	a=$1
	LOCAL_CUSTOM_DATA_PATH=$WORK_DIR/custom/$date
	mkdir -p $LOCAL_CUSTOM_DATA_PATH
	for j in "${a[@]}"
	do 
		filename=$(echo $j | cut -f1 -d#)
		
		query=$(echo $j | cut -f2 -d#)		
		
		PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "COPY ($query) TO STDOUT WITH CSV HEADER" | gzip > $LOCAL_CUSTOM_DATA_PATH/$filename.csv.gz
		
		if [ "$?" -eq "1" ]; then
			error_handler "$ZK_CUSTOMER $filename : copy command failed. Table : $table"
		fi
		
		echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER $filename : copied data from db successful"

		echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER $filename : transferring data to remote location"

		sshpass -p $SFTP_PASSWORD scp -o StrictHostKeyChecking=no -r $WORK_DIR/custom  $SFTP_USER@$SFTP_HOST:$SFTP_LOCATION
	
		if [ "$?" -eq "1" ]; then
				error_handler "$ZK_CUSTOMER $filename : failed to transfer data to remote location"
		fi

		echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER $filename : Transferred data to remote location successful"
	
		echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER $filename  : unzipping data at remote location"
		
		echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER $filename : unzipping data at remote location"

		sshpass -p $SFTP_PASSWORD ssh -o StrictHostKeyChecking=no  $SFTP_USER@$SFTP_HOST 'gunzip -r '$SFTP_LOCATION/custom
	
		if [ "$?" -eq "1" ]; then
				error_handler "$ZK_CUSTOMER $filename : failed unzipping at remote location"
		fi

		echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER $filename : unzipped data at remote location successful"

		echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - $ZK_CUSTOMER $filename : CSV export process completed successfully"
	  	  
	done
}

if [ $USDM = "y" ];then
	echo "===> export process for usdm"
	usdm_tables="$(python $QUERY_REPO_PATH/read_params.py u)"
	echo "===> python done usdm_tables : $usdm_tables"
	export_data_tables $usdm_tables "usdm"
fi 

if [ $PLO = "y" ];then
	echo "===> export process for plo"
	plo_tables="$(python $QUERY_REPO_PATH/read_params.py p)"
	export_data_tables $plo_tables "plo"
fi 

if [ $CUSTOM = "y" ];then
	echo "===> export process for custom"
	python $QUERY_REPO_PATH/read_params.py c > custom.txt
	readarray a < custom.txt
	export_data_custom $a
fi 

echo "[$(date '+%Y-%m-%d %H:%M:%S')] $remote_user [INFO] - CSV export process completed successfully"