"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var constants_1 = require("../constants");
var util_1 = require("../util");
/**
 * Compress date (as unix timestamp) to writer
 */
function compressDate(compressors, context, obj, invertedIndex, writer, options) {
    var foundRef;
    /**
     * Determine if we should represent the date with low precision
     */
    var lowPrecisionDate = obj / constants_1.DATE_LOW_PRECISION;
    var isLowPrecision = lowPrecisionDate % 1 === 0;
    if (isLowPrecision) {
        if ((foundRef = invertedIndex.lpDateMap[lowPrecisionDate]) !== void 0) {
            writer.write("" + constants_1.REF_LP_DATE_TOKEN + foundRef);
        }
        else {
            var ref = util_1.compressInteger(invertedIndex.lpDateCount);
            var compressedDate = util_1.compressInteger(lowPrecisionDate);
            var newRef = "" + constants_1.LP_DATE_TOKEN + compressedDate;
            if (ref.length + constants_1.REFERENCE_HEADER_LENGTH < newRef.length) {
                invertedIndex.lpDateMap[lowPrecisionDate] = ref;
                invertedIndex.lpDateCount++;
                writer.write(newRef);
            }
            else {
                writer.write("" + constants_1.UNREFERENCED_LP_DATE_TOKEN + compressedDate);
            }
        }
    }
    else {
        if ((foundRef = invertedIndex.dateMap[obj]) !== void 0) {
            writer.write("" + constants_1.REF_DATE_TOKEN + foundRef);
        }
        else {
            var ref = util_1.compressInteger(invertedIndex.dateCount);
            var compressedDate = util_1.compressInteger(obj);
            var newRef = "" + constants_1.DATE_TOKEN + compressedDate;
            if (ref.length + constants_1.REFERENCE_HEADER_LENGTH < newRef.length) {
                invertedIndex.dateMap[obj] = ref;
                invertedIndex.dateCount++;
                writer.write(newRef);
            }
            else {
                writer.write("" + constants_1.UNREFERENCED_DATE_TOKEN + compressedDate);
            }
        }
    }
}
exports.compressDate = compressDate;
//# sourceMappingURL=date.js.map