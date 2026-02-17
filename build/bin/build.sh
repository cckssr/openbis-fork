#!/bin/bash
usage()
{
  echo ""
  echo "Usage: ./build.sh branch tag"
  echo ""
  echo "Example: ./build.sh S175.x S175.0"
  exit 1
}

move_to_file_server() 
{
  echo "Moving new openBIS components to file server"
  
  OPENBIS_PATH=~openbis/fileserver/sprint_builds/openBIS
  SPRINT_DIR=$OPENBIS_PATH/$TODAY-$tag
  mkdir -p $SPRINT_DIR
  mv *$tag*.{zip,gz} $SPRINT_DIR/
  chmod g+w -R $SPRINT_DIR
}

if [ $# -ne 2 ]
then
	usage
fi

TODAY=`date "+%Y-%m-%d"`

branch=$1
tag=$2

# cd to repository root directory
cd "$(dirname "$0")/../.."

# checkout tag
git checkout $tag
if [ $? -ne 0 ]; then echo "Tag does not exist!"; exit 1; fi

# build
#cd core-plugin-openbis
#./gradlew :clientsAndApis -x test
#./gradlew :generateJavadoc
#cd ../app-openbis-installer
#./gradlew :build -x test

cd release
./gradlew release -Dorg.gradle.jvmargs="--add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.desktop/java.awt.font=ALL-UNNAMED"


cd ../..

# move documentation to fileserver
#cp -r openbis/core-plugin-openbis/targets/gradle/docs/javadoc ~openbis/fileserver/doc/openbis/$tag
#cd ~openbis/fileserver/doc/openbis
#if [ ${tag:0:1} == "S" ]; then
#  rm current
#  ln -s $tag current
#else
#  dir=${tag%.*}
#  rm $dir
#  ln -s $tag $dir
#fi

cd -

# move components to fileserver
mv openbis/release/build/openbis-release-*.tar.gz .

move_to_file_server
