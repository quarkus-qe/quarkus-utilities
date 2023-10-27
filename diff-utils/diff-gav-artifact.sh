if [[ $# -ne 2 ]]; then
  echo "Usage: diff-gav-artifact.sh NEW_QUARKUS_BUILD_URL OLD_QUARKUS_BUILD_URL"
  exit 1
fi

NEW_QUARKUS_BUILD_URL=$1
OLD_QUARKUS_BUILD_URL=$2

rm -f artifacts_*
rm -f .*html

QUARKUS_VERSION_NEW=$(echo "${NEW_QUARKUS_BUILD_URL%'/'}" | awk -F- '{print $NF}')
QUARKUS_VERSION_OLD=$(echo "${OLD_QUARKUS_BUILD_URL%'/'}" | awk -F- '{print $NF}')


wget -O artifacts_${QUARKUS_VERSION_NEW}.txt ${NEW_QUARKUS_BUILD_URL}/extras/repository-artifact-list.txt
wget -O artifacts_${QUARKUS_VERSION_OLD}.txt ${OLD_QUARKUS_BUILD_URL}/extras/repository-artifact-list.txt

cat artifacts_${QUARKUS_VERSION_NEW}.txt | cut -d: -f1,2 | tee artifacts_${QUARKUS_VERSION_NEW}_GA.txt
cat artifacts_${QUARKUS_VERSION_OLD}.txt | cut -d: -f1,2 | tee artifacts_${QUARKUS_VERSION_OLD}_GA.txt


echo 'get & unzip productized maven repo'
wget -q -O quarkus-maven-repo.zip "http://download.eng.bos.redhat.com/rcm-guest/staging/quarkus/quarkus-platform-${QUARKUS_VERSION_NEW}/rh-quarkus-platform-${QUARKUS_VERSION_NEW}-maven-repository.zip"
MAVEN_REPO_ROOT_DIR_NAME=$(unzip -Z -1 quarkus-maven-repo.zip | head -n 1 | cut -d '/' -f 1)
unzip -q quarkus-maven-repo.zip

wget -O ~/.m2/settings.xml https://gitlab.cee.redhat.com/quarkus-qe/jenkins-jobs/-/raw/main/jobs/rhbq/files/settings.xml
sed -i -e "s|/path_to_repo|$PWD/${MAVEN_REPO_ROOT_DIR_NAME}/maven-repository|" ~/.m2/settings.xml
LOCAL_REPO="$(pwd)/${MAVEN_REPO_ROOT_DIR_NAME}/maven-repository/"

./create_colorful_diff.sh diff-gav-artifacts.html artifacts_${QUARKUS_VERSION_OLD}_GA.txt artifacts_${QUARKUS_VERSION_NEW}_GA.txt
rm artifacts_${QUARKUS_VERSION_OLD}_GA.txt artifacts_${QUARKUS_VERSION_NEW}_GA.txt

while read line; do
  OLD_VERSION=`echo $line | cut -d: -f3`
  GA=`echo $line | cut -d: -f1-2`
  NEW_VERSION=`cat artifacts_${QUARKUS_VERSION_NEW}.txt | grep "$GA:" | cut -d: -f3`
  NEW_VERSION_COUNT=`wc -w <<< $NEW_VERSION`

  if [ $NEW_VERSION_COUNT -eq 1 ]; then
    if [ "$NEW_VERSION" = "$OLD_VERSION" ]; then
      echo "$GA - $OLD_VERSION  => $NEW_VERSION  ::  IDENTICAL" >> artifacts_${QUARKUS_VERSION_NEW}_IDENTICAL.txt

    elif [ "$(printf '%s\n' "$OLD_VERSION" "$NEW_VERSION" | sort -V | head -n1)" = "$OLD_VERSION" ]; then
      echo "$GA - $OLD_VERSION  => $NEW_VERSION  ::  UPGRADED" >> artifacts_${QUARKUS_VERSION_NEW}_UPGRADED.txt

    else
      echo "$GA - $OLD_VERSION  => $NEW_VERSION  ::  DOWNGRADED" >> artifacts_${QUARKUS_VERSION_NEW}_DOWNGRADED.txt

    fi
  elif [ $NEW_VERSION_COUNT -eq 0 ]; then
    echo "$GA - $OLD_VERSION  ::  REMOVED" >> artifacts_${QUARKUS_VERSION_NEW}_REMOVED.txt

  else
    # echo "$GA - $OLD_VERSION  vs. $NEW_VERSION ... $NEW_VERSION_COUNT  ::  MULTIPLE - Multiple versions to compare with, skipping"
    echo "$GA" >> artifacts_${QUARKUS_VERSION_NEW}_MULTIPLE.txt
  fi
done < artifacts_${QUARKUS_VERSION_OLD}.txt

while read line; do
  NEW_VERSION=`echo $line | cut -d: -f3`
  GA=`echo $line | cut -d: -f1-2`
  OLD_VERSION_COUNT=`cat artifacts_${QUARKUS_VERSION_OLD}.txt | grep "$GA:" | cut -d: -f3 | wc -w`

  if [ $OLD_VERSION_COUNT -eq 0 ]; then
    echo "$GA - $NEW_VERSION  ::  ADDED" >> artifacts_${QUARKUS_VERSION_NEW}_ADDED.txt
    echo "$GA" >> added_artifacts_list.txt
  fi
done < artifacts_${QUARKUS_VERSION_NEW}.txt

# Dependencies print of new added artifacts
mvn clean install exec:java -Dquarkus.maven.dir="$LOCAL_REPO" -Dquarkus.new-artifacts-list="added_artifacts_list.txt"
mv added_artifacts_deps.txt artifacts_${QUARKUS_VERSION_NEW}_ADDED_WITH_DEPENDENTS.txt
rm -f added_artifacts_list.txt

cat artifacts_${QUARKUS_VERSION_NEW}_MULTIPLE.txt | sort | uniq > artifacts_${QUARKUS_VERSION_NEW}_MULTIPLE_TMP.txt
mv artifacts_${QUARKUS_VERSION_NEW}_MULTIPLE_TMP.txt artifacts_${QUARKUS_VERSION_NEW}_MULTIPLE.txt
while read line; do
  grep $line artifacts_${QUARKUS_VERSION_OLD}.txt >> artifacts_${QUARKUS_VERSION_OLD}_MULTIPLE_DETAILS.txt
  grep $line artifacts_${QUARKUS_VERSION_NEW}.txt >> artifacts_${QUARKUS_VERSION_NEW}_MULTIPLE_DETAILS.txt
done < artifacts_${QUARKUS_VERSION_NEW}_MULTIPLE.txt
./create_colorful_diff.sh diff-artifacts-with-multiple-versions.html artifacts_${QUARKUS_VERSION_OLD}_MULTIPLE_DETAILS.txt artifacts_${QUARKUS_VERSION_NEW}_MULTIPLE_DETAILS.txt

rm -f artifacts_${QUARKUS_VERSION_OLD}_MULTIPLE_DETAILS.txt artifacts_${QUARKUS_VERSION_NEW}_MULTIPLE_DETAILS.txt

rm -f quarkus-maven-repo.zip
rm -f ~/.m2/settings.xml
rm -rf ${MAVEN_REPO_ROOT_DIR_NAME}

wc -l artifacts_*.txt       