if [ $# -gt 2 ];
then
    echo "Usage: ./get-extensions.sh DEPENDENCY_NAME [OUTPUT_FILE_NAME]"
    exit 1
fi

curl -X 'GET' \
  'https://registry.quarkus.io/client/extensions/all' \
  -H 'accept: application/json' > quarkus-extensions.json

dependency_exists=`cat quarkus-extensions.json | grep "${1}" | wc -l`

if [ $dependency_exists -gt 0 ];
then
  # Custom output file name is not defined
  if [ -z "$2" ];
  then
    mvn clean install exec:java -Dsearched.dependency="$1"
  else
    mvn clean install exec:java -Dsearched.dependency="$1" -Doutput.file.name="$2"
  fi
else
  echo "Given dependency does not exist"
fi

rm quarkus-extensions.json