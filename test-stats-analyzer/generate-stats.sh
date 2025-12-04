add_to_report() {
  echo "$1" >> generated_test_stats
}

translate_combination_to_mvn_args() {
  local result=''

  # it is not intuitive that when you run 'mvn clean verify -Dnative' or '-Dopenshift'
  # on Quarkus QE Test Framework, the 'examples' profile, which is active by default is disabled
  # so let's activate it as well
  if [[ -f pom.xml ]] && grep -q "github.com:quarkus-qe/quarkus-test-framework.git" pom.xml; then
    result="-Pframework,examples"
  else
    result="-Dexclude.quarkus.cli.tests=no"
  fi

  local combination=$1
  # do nothing for 'jvm-mode' as it is not necessary
  if [[ "$combination" == *"native-mode"* ]]; then
    result="$result -Dnative -Dall-modules"
  fi
  if [[ "$combination" == *"openshift"* ]]; then
    result="$result -Dopenshift -Dall-modules"
  fi
  if [[ "$combination" == *"kubernetes"* ]]; then
    result="$result -Dkubernetes -Dall-modules"
  fi
  echo "$result"
}

print_aggregated_failsafe_summaries() {
   local command_mvn_args="$1"
   local git_branch="$2"
   local combination="$3"
   parse_xml () { local IFS=\> ; read -d \< E C ;}
   local completed=0
   local skipped=0
   add_to_report "   <combination>"
   if [[ -z $(echo $command_mvn_args | xargs) ]]; then
     add_to_report "     <command-arguments></command-arguments>"
   else
     add_to_report "     <command-arguments>$command_mvn_args</command-arguments>"
   fi
   add_to_report "     <git-branch>$git_branch</git-branch>"
   add_to_report "     <modules>"
   for report_file_path in `find . -name 'failsafe-summary.xml'`
   do
      add_to_report "      <module>"
      add_to_report "        <path>$report_file_path</path>"
      
      # Extract project name from pom.xml (3 levels up from target/failsafe-reports/)
      local pom_path=$(dirname $(dirname $(dirname "$report_file_path")))/pom.xml
      if [[ -f "$pom_path" ]]; then
        local project_name=$(grep -m1 '<name>' "$pom_path" | sed 's/.*<name>\(.*\)<\/name>.*/\1/')
        if [[ -n "$project_name" ]]; then
          add_to_report "        <name>$project_name</name>"
        fi
      fi
      while parse_xml; do
        if [[ $E = completed ]]; then
          add_to_report "        <completed>$C</completed>"
          (( completed=completed+C ))
        fi
        if [[ $E = skipped ]]; then
          add_to_report "        <skipped>$C</skipped>"
          (( skipped=skipped+C ))
        fi
        if [[ $E = errors ]] && [[ $C != 0 ]]; then
          echo "There was $C errors during the test run, exiting stats generation"
          exit -1
        fi
	    done < $report_file_path
	    add_to_report "      </module>"
   done
   add_to_report "     </modules>"
   local end_message="Total number of executed tests is $completed while $skipped tests were skipped for combination '$combination', git branch '$git_branch'"
   add_to_report "     <completed>$completed</completed>"
   add_to_report "     <skipped>$skipped</skipped>"
   add_to_report "     <summary>$end_message</summary>"
   add_to_report "   </combination>"
   echo "- $end_message"
}

generate_stats_for_branch() {
    local target_dir=$1
    local target_branch=$2
    local mvn_args=$3
    local recipe_dir=$4
    local dont_build=$5
    local combinations=$6

    quarkus_version_arg=''
    if [[ ${dont_build} == "false" ]]; then

      # some branches like Quarkus QE Test Suite 3.15 and 3.20 requires respective snapshots like 3.20.999-SNAPSHOT
      # that may or may not be available locally and since Quarkus version is probably irrelevant, let's avoid failure
      # it could be an issue if some later 3.20.x releases added dependencies or classes that 3.20.1 doesn't support
      if [[ ${target_branch} == "3.20" ]]; then
        quarkus_version_arg='-Dquarkus.platform.version=3.20.1'
        mvn_args="$mvn_args $quarkus_version_arg"
      fi
      if [[ ${target_branch} == "3.15" ]]; then
        quarkus_version_arg='-Dquarkus.platform.version=3.15.5'
        mvn_args="$mvn_args $quarkus_version_arg"
      fi

      git reset --hard
      git checkout $target_branch
      echo "Build project without running tests"
      # this is done because OpenRewrite acts differently when the project is built
      # and it can result in some parsing errors when we don't build the project
      mvn clean install -DskipTests -DskipITs -Dcheckstyle.skip $quarkus_version_arg
    fi

    # copy 'rewrite.yml' to project for which we want stats
    local target_project_root_recipe_location="$target_dir/rewrite.yml"
    local recipe_project_root_recipe_location="$recipe_dir/test-stats-analyzer/rewrite.yml"
    echo "Copying recipe file '$recipe_project_root_recipe_location' to '$target_project_root_recipe_location'"
    cp $recipe_project_root_recipe_location $target_project_root_recipe_location

    # run openrewrite recipe
    echo "Applying OpenRewrite recipe, this should make all tests methods and JUnit callback methods empty in order to avoid unnecessary test execution"
    mvn -U org.openrewrite.maven:rewrite-maven-plugin:runNoFork -Drewrite.activeRecipes=io.quarkus.qe.PrepareDryRun -Drewrite.recipeArtifactCoordinates='io.quarkus.qe:test-stats-analyzer:1.0-SNAPSHOT' $quarkus_version_arg -DskipMavenParsing=true --errors

    if [[ $combinations == *","* ]]; then
      IFS=',' read -r -a combinations_array <<< "$combinations"
    else
      combinations_array=("$combinations")
    fi

    for combination in "${combinations_array[@]}"
    do
      enhanced_mvn_args="$mvn_args $(translate_combination_to_mvn_args $combination)"

      # run tests for the target project (surefire, failsafe, native, openshift and possibly podman / docker / windows)
      # note: ATM actual 'dry-run' implemented in JUnit 5 doesn't count properly parametrized tests etc. so we don't trust it
      echo "Performing dry-run using command 'mvn clean verify -Dquarkus.build.skip=true -Dcheckstyle.skip $enhanced_mvn_args' on the target project"
      mvn clean verify -Dquarkus.build.skip=true -Dcheckstyle.skip $enhanced_mvn_args

      # collect information about executed tests and print them
      print_aggregated_failsafe_summaries "$enhanced_mvn_args" "$target_branch" "$combination"
    done
}

# project that generate stats, it includes this script
RECIPE_PROJECT_BRANCH='main'
RECIPE_PROJECT_URL='https://github.com/quarkus-qe/quarkus-utilities.git'
RECIPE_PROJECT_DIRECTORY=''

# project for which we want stats
TARGET_PROJECT_DIRECTORY=''
TARGET_PROJECT_URL='https://github.com/quarkus-qe/quarkus-test-suite.git'
BRANCHES_TO_TEST='main'

# arguments with which we run tests and have direct impact on which tests are run, e.g. '-Dopenshift' or '-Dnative'
EXTRA_MAVEN_ARGS=''

# possible combinations (JVM mode, native mode, OpenShift, Kubernetes)
COMBINATIONS='jvm-mode'

# where we store generated stats
WORKING_DIRECTORY=''

while getopts b:u:d:f:t:s:a:r:c: opt
do
    case "${opt}" in
        s) RECIPE_PROJECT_BRANCH=${OPTARG};;
        u) RECIPE_PROJECT_URL=${OPTARG};;
        d) RECIPE_PROJECT_DIRECTORY=${OPTARG};;
        f) TARGET_PROJECT_DIRECTORY=${OPTARG};;
        t) TARGET_PROJECT_URL=${OPTARG};;
        b) BRANCHES_TO_TEST=${OPTARG};;
        a) EXTRA_MAVEN_ARGS=${OPTARG};;
        r) WORKING_DIRECTORY=${OPTARG};;
        c) COMBINATIONS=${OPTARG};;
    esac
done

# build this project so that GAV with the recipe is resolved by OpenRewrite maven plugin
if [ -z "${RECIPE_PROJECT_DIRECTORY}" ]; then
    RECIPE_PROJECT_DIRECTORY=$(mktemp -d)
    echo "Building recipe project '$RECIPE_PROJECT_URL' branch '$RECIPE_PROJECT_BRANCH' in directory '$RECIPE_PROJECT_DIRECTORY"
    PREVIOUS_DIR="$PWD"
    cd $RECIPE_PROJECT_DIRECTORY
    git clone --depth=1 $RECIPE_PROJECT_URL -b $RECIPE_PROJECT_BRANCH .
    cd test-stats-analyzer
    mvn clean install -DskipTests -DskipITs
    echo "Project 'io.quarkus.qe:test-stats-analyzer:1.0-SNAPSHOT' has been installed to local Maven repository"
    cd $PREVIOUS_DIR
fi

# prepare project for which we require stats
if [ -z "${TARGET_PROJECT_DIRECTORY}" ]; then
    SKIP_BUILD="false"
    TARGET_PROJECT_DIRECTORY=$(mktemp -d)
    cd $TARGET_PROJECT_DIRECTORY
    echo "Cloning project $TARGET_PROJECT_URL into directory $TARGET_PROJECT_DIRECTORY"
    git clone $TARGET_PROJECT_URL .
    git fetch origin
else
    SKIP_BUILD="true"
    cd $TARGET_PROJECT_DIRECTORY
fi

if [ -z "${WORKING_DIRECTORY}" ]; then
    WORKING_DIRECTORY=$(mktemp -d)
fi
echo "Results will be stored in directory $WORKING_DIRECTORY"

if [[ $BRANCHES_TO_TEST == *","* ]]; then
  # multiple git branches to analyze
  IFS=',' read -r -a BRANCHES_TO_TEST_ARRAY <<< "$BRANCHES_TO_TEST"
else
  # only one git branch to analyze
  BRANCHES_TO_TEST_ARRAY=("$BRANCHES_TO_TEST")
fi

rm -f generated_test_stats
add_to_report "<report>"
if [[ "$SKIP_BUILD" == "true" ]]; then
  add_to_report "  <project-directory>$TARGET_PROJECT_DIRECTORY</project-directory>"
else
  add_to_report "  <project-url>$TARGET_PROJECT_URL</project-url>"
fi
add_to_report "  <project-git-branches>$BRANCHES_TO_TEST</project-git-branches>"
add_to_report "  <combinations>"
for BRANCH_TO_TEST in "${BRANCHES_TO_TEST_ARRAY[@]}"
do
  echo "branch $BRANCH_TO_TEST"
  generate_stats_for_branch "$TARGET_PROJECT_DIRECTORY" "$BRANCH_TO_TEST" "$EXTRA_MAVEN_ARGS" "$RECIPE_PROJECT_DIRECTORY" "$SKIP_BUILD" "$COMBINATIONS"
done
add_to_report "  </combinations>"
add_to_report "</report>"

# HINT: file 'generated_test_stats' intentionally doesn't have 'xml' extension at first,
# because during 'Quarkus QE TS: Parent' build some plugins (like 'xml-format:xml-format') may validate it and fail
cp $PWD/generated_test_stats $WORKING_DIRECTORY/generated_test_stats.xml
echo "Complete report has been generated to '$WORKING_DIRECTORY/generated_test_stats.xml' file"
