function create_colorful_diff() {
local FILENAME=$1
local TEXT1=$(cat $2)
local TEXT2=$(cat $3)
local DEP_FILE=$(cat $4)
local TITLE=$5
local TEXT1_TITLE=$6
local TEXT2_TITLE=$7
local DEFAULT_DIFF_TYPE=${8:-0}
local CONTEXT_SIZE=$9

local DIFF_TYPE_SIDE_CHECKED=' checked="checked"'
local DIFF_TYPE_INLINE_CHECKED=

if [ "$DEFAULT_DIFF_TYPE" = "1" ]; then
	DIFF_TYPE_SIDE_CHECKED=
	DIFF_TYPE_INLINE_CHECKED=' checked="checked"'
fi

cat <<EOF > $FILENAME
<!DOCTYPE html>
<html><head><meta http-equiv="content-type" content="text/html; charset=UTF-8"><meta charset="utf-8"><meta http-equiv="X-UA-Compatible" content="IE=Edge,chrome=1">
	<title>$TITLE</title>
	<link rel="stylesheet" type="text/css" href="https://cemerick.github.io/jsdifflib/diffview.css"><script type="text/javascript" src="https://cemerick.github.io/jsdifflib/diffview.js"></script><script type="text/javascript" src="https://cemerick.github.io/jsdifflib/difflib.js"></script>

	<!-- Bootstrap CSS -->
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@4.0.0/dist/css/bootstrap.min.css" integrity="sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm" crossorigin="anonymous">
  <!-- jQuery-->
  <script src="https://code.jquery.com/jquery-3.5.1.slim.min.js"></script>
  <!-- Bootstrap JS -->
  <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/js/bootstrap.min.js"></script>

<style type="text/css">
  body {
    font-size: 12px;
    font-family: Sans-Serif;
  }

  h2 {
    margin: 0.5em 0 0.1em;
    text-align: center;
  }

  .top {
    text-align: center;
  }
  .textInput {
    display: block;
    width: 49%;
    float: left;
  }

  textarea {
    width:100%;
    height:300px;
  }

  label:hover {
    text-decoration: underline;
    cursor: pointer;
  }

  .spacer {
    margin-left: 10px;
  }

  .viewType {
    font-size: 16px;
    clear: both;
    text-align: center;
    padding: 1em;
  }

  #diffoutput {
    width: 100%;
  }

  #diffoutput table.diff {
    margin-left:auto;
    margin-right:auto
  }

  .modal-title {
    font-weight: bold;
    font-size: 24px;
  }

  .modal-body {
    font-size: 18px;
  }

  .modal-lg {
    width: 90%;
    max-width: 1200px;
  }
</style>

<script type="text/javascript">

var added_artifacts = \`$DEP_FILE\`.split('\n');

function diffUsingJS(viewType) {
	"use strict";
	var byId = function (id) { return document.getElementById(id); },
		base = difflib.stringAsLines(byId("baseText").value),
		newtxt = difflib.stringAsLines(byId("newText").value),
		sm = new difflib.SequenceMatcher(base, newtxt),
		opcodes = sm.get_opcodes(),
		diffoutputdiv = byId("diffoutput"),
		contextSize = byId("contextSize").value;

	diffoutputdiv.innerHTML = "";
	contextSize = contextSize || null;

	diffoutputdiv.appendChild(diffview.buildView({
		baseTextLines: base,
		newTextLines: newtxt,
		opcodes: opcodes,
		baseTextName: "$TEXT1_TITLE",
		newTextName: "$TEXT2_TITLE",
		contextSize: contextSize,
		viewType: viewType
	}));

	var elements = document.getElementsByTagName('tr');

  for (let i in elements) {
      var td2_element = elements.item(i).getElementsByTagName('td');
      td2_element = (viewType == 1) ? td2_element.item(0) : td2_element.item(1);

      if (td2_element != null) {
        if (td2_element.className === "replace" || td2_element.className === "insert") {
          var text = td2_element.innerHTML;
          td2_element.innerHTML = '<button onClick="printDependencies(this)" data-toggle="modal" data-target=".bd-example-modal-lg">' + text + '</button>'
        }
      }
  }
}

function printDependencies(artifact) {
  var addedArtifact = extractLinesBetweenPatterns("Dependants for " + artifact.textContent + " - ");
  var addedArtifactVersion = getArtifactVersion(addedArtifact);
  document.getElementById('modalLabel').innerHTML=artifact.textContent + " - " + addedArtifactVersion;
  createModalContent(addedArtifact);
}

function createModalContent(addedArtifact) {

  var dependentsArray = [];

  if (addedArtifact.includes("no dependents found")) {
    dependentsArray.push("NO DEPENDENTS FOUND");
  }
  else {
    const findDependentsRegex = /Dependants(.*(?:\n.*?)+)dependents: /im;
    var dependents = addedArtifact.replace(findDependentsRegex, '');
    var dependentsArray = dependents.split(',').map(dependent => dependent.trim());

    // Remove right parenthesis after last dependent
    var lastElement = dependentsArray.pop();
    lastElement = lastElement.replace(/\)/g, '');
    dependentsArray.push(lastElement);
  }

  // Clear modal content div before viewing
  var divContainer = document.getElementById("modal-body");
  divContainer.innerHTML="";

  var ul = document.createElement("ul");

  dependentsArray.forEach(function(itemText) {
    var li = document.createElement("li");
    li.textContent = itemText;
    ul.appendChild(li);
  });

  divContainer.appendChild(ul);
}

function extractLinesBetweenPatterns(startPattern) {
  let isBetweenPatterns = false;
  const result = [];

  for (const line of added_artifacts) {
    if (line.includes(startPattern)) {
      isBetweenPatterns = true;
    }

    if (isBetweenPatterns) {
      result.push(line);
    }

    if (isBetweenPatterns && line.length === 0) {
      isBetweenPatterns = false;
    }
  }

  return result.join('\n');
}

function getArtifactVersion(addedArtifact) {
   // Regex to match the version between '[' and ']'
   var versionRegex = /\[(.*?)\]/;

   var match = addedArtifact.match(versionRegex);
   return match[1];
}

</script>
</head>
<body onload="diffUsingJS($DEFAULT_DIFF_TYPE);"> 
	<h1 class="top">$TITLE</h1>
	<div class="viewType">
		<input name="_viewtype" id="sidebyside" onclick="diffUsingJS(0);" type="radio" $DIFF_TYPE_SIDE_CHECKED > 
		<label for="sidebyside">Side by Side Diff</label>
		&nbsp; &nbsp;
		<input name="_viewtype" id="inline" onclick="diffUsingJS(1);" type="radio" $DIFF_TYPE_INLINE_CHECKED > 
		<label for="inline">Inline Diff</label>
	</div>
  <!-- The Modal -->
  <div class="modal fade bd-example-modal-lg" id="myModal" tabindex="-1" role="dialog" aria-labelledby="myExtraLargeModalLabel" aria-hidden="true">
      <div class="modal-dialog modal-lg modal-dialog-centered" role="document">
          <div class="modal-content">
              <div class="modal-header">
                  <h5 class="modal-title" id="modalLabel"></h5>
                  <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                      <span aria-hidden="true">&times;</span>
                  </button>
              </div>
              <div class="modal-body" id="modal-body">
              </div>
          </div>
      </div>
  </div>
	<div class="top">
		<strong>Context size (empty means full):</strong> 
		<input id="contextSize" type="text" value="$CONTEXT_SIZE">
	</div>
	<div id="diffoutput"></div>
	<div class="textInput">
		<h2>$TEXT1_TITLE</h2>
		<textarea id="baseText">$TEXT1</textarea>
	</div>
	<div class="textInput spacer">
		<h2>$TEXT2_TITLE</h2>
		<textarea id="newText">$TEXT2</textarea>
	</div>
</body></html>
EOF

}



if [ $# -lt 3 ]; then
  echo "Atleast 3 arguments are expected, usage: $0 OUTPUT_FILE INPUT_FILE_1 INPUT_FILE_2 [TITLE [TEXT_TITLE1 [TEXT_TITLE2 [DEFAULT_DIFF_TYPE [CONTEXT_SIZE]]]]]"
  exit 1
fi

set -x

OUTPUT_FILE=$1
INPUT_FILE_1=$2 
INPUT_FILE_2=$3
DEPENDENTS_FILE=$4
TITLE=${5:-"$INPUT_FILE_1 vs $INPUT_FILE_2"}
TEXT_TITLE1=${6:-"$INPUT_FILE_1"}
TEXT_TITLE2=${7:-"$INPUT_FILE_2"}
DEFAULT_DIFF_TYPE=${8:-0}
CONTEXT_SIZE=$9

create_colorful_diff "$OUTPUT_FILE" "$INPUT_FILE_1" "$INPUT_FILE_2" "$DEPENDENTS_FILE" "$TITLE" "$TEXT_TITLE1" "$TEXT_TITLE2" $DEFAULT_DIFF_TYPE $CONTEXT_SIZE
