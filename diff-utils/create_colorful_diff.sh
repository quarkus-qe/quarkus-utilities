function create_colorful_diff() {
local FILENAME=$1
local TEXT1=$(cat $2)
local TEXT2=$(cat $3)
local TITLE=$4
local TEXT1_TITLE=$5
local TEXT2_TITLE=$6
local DEFAULT_DIFF_TYPE=${7:-0}
local CONTEXT_SIZE=$8

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
<style type="text/css">
body {font-size: 12px;font-family: Sans-Serif;} h2 {margin: 0.5em 0 0.1em;text-align: center;} .top {text-align: center;} .textInput {display: block;width: 49%;float: left;} textarea {width:100%;height:300px;} label:hover {text-decoration: underline;cursor: pointer;} .spacer {margin-left: 10px;} .viewType {font-size: 16px;clear: both;text-align: center;padding: 1em;} #diffoutput {width: 100%;} #diffoutput table.diff {margin-left:auto; margin-right:auto}
</style>

<script type="text/javascript">
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
TITLE=${4:-"$INPUT_FILE_1 vs $INPUT_FILE_2"}
TEXT_TITLE1=${5:-"$INPUT_FILE_1"}
TEXT_TITLE2=${6:-"$INPUT_FILE_2"}
DEFAULT_DIFF_TYPE=${7:-0}
CONTEXT_SIZE=$8

create_colorful_diff "$OUTPUT_FILE" "$INPUT_FILE_1" "$INPUT_FILE_2" "$TITLE" "$TEXT_TITLE1" "$TEXT_TITLE2" $DEFAULT_DIFF_TYPE $CONTEXT_SIZE
