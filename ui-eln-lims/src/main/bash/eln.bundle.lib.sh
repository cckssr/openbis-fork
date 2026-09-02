#!/bin/bash

# 0. Define input folder
INPUT_FOLDER="../../../src/core-plugins/eln-lims/src/as/webapps/eln-lims/html/lib"

# 1. Define the output file
OUTPUT_FILE="../../../src/core-plugins/eln-lims/src/as/webapps/eln-lims/html/eln.bundle.lib.js"

# 2. List your files in the exact order they need to load
FILES=(
	"$INPUT_FOLDER/cycle/js/cycle.js"
	"$INPUT_FOLDER/jquery/js/jquery-1.11.3.min.js"
	"$INPUT_FOLDER/jquery/js/jquery.cookie.js"
	"$INPUT_FOLDER/jquery-ui/js/jquery-ui.min.js"
	"$INPUT_FOLDER/jquery-fancytree/js/jquery.fancytree-all.min.js"
	"$INPUT_FOLDER/bootstrap-datetimepicker/js/moment.js"
	"$INPUT_FOLDER/bootstrap/js/bootstrap.min.js"
	"$INPUT_FOLDER/bootstrap-datetimepicker/js/bootstrap-datetimepicker.min.js"
	"$INPUT_FOLDER/bootstrap-multiselect/js/bootstrap-multiselect.js"
	"$INPUT_FOLDER/bootstrap-slider/js/bootstrap-slider.js"
	"$INPUT_FOLDER/bootstrap-filestyle/js/bootstrap-filestyle.min.js"
	"$INPUT_FOLDER/jquery-jnotify/js/jNotify.sis.jquery.js"
	"$INPUT_FOLDER/jquery-blockui/js/jquery.blockUI.js"
	"$INPUT_FOLDER/jquery-history/js/bundled/html5/jquery.history.js"
	"$INPUT_FOLDER/jquery-tooltipster/js/tooltipster.bundle.min.js"
	"$INPUT_FOLDER/jquery-tsv/js/jquery.tsv-0.96.min.js"
	"$INPUT_FOLDER/fuelux/js/fuelux.min.js"
	"$INPUT_FOLDER/jquery-select2/js/select2.full.js"
	"$INPUT_FOLDER/d3/js/d3.min.js"
	"$INPUT_FOLDER/c3/c3.min.js"
	"$INPUT_FOLDER/d3-dagre/js/dagre-d3.min.js"
	"$INPUT_FOLDER/naturalsort/js/naturalSort.js"
	"$INPUT_FOLDER/mozilla/js/infra.js"
	"$INPUT_FOLDER/filesaver/js/FileSaver.js"
	"$INPUT_FOLDER/drawingboard/js/drawingboard.min.js"
	"$INPUT_FOLDER/ckeditor/js/ckeditor.js"
	"$INPUT_FOLDER/DOMPurify/purify.js"
	"$INPUT_FOLDER/jsuites/jsuites.js"
	"$INPUT_FOLDER/jexcel/formula.js"
	"$INPUT_FOLDER/jexcel/index.js"
	"$INPUT_FOLDER/bwip-js/bwipp.js"
	"$INPUT_FOLDER/bwip-js/bwipjs.js"
	"$INPUT_FOLDER/bwip-js/lib/bitmap.js"
	"$INPUT_FOLDER/bwip-js/lib/symdesc.js"
	"$INPUT_FOLDER/bwip-js/lib/canvas-toblob.js"
	"$INPUT_FOLDER/jspdf/js/jspdf.min.js"
	"$INPUT_FOLDER/zxing-js/js/zxing-js-min.js"
	"$INPUT_FOLDER/react/react.production.min.js"
	"$INPUT_FOLDER/react/react-dom.production.min.js"
	"$INPUT_FOLDER/grid/js/Grid.js"
	# "$INPUT_FOLDER/bwip-js/lib/xhr-fonts.js" -- script cannot be bundled as it searches for it own <script> tag
	# "$INPUT_FOLDER/react/Components.js" -- script cannot be bundled because of embedded images
)

# 3. Bundle
./bundle.sh "$OUTPUT_FILE" "${FILES[@]}"

# 4. Strip map files
sed -i '/\/\/# sourceMappingURL=/d' $OUTPUT_FILE