"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var constants_1 = require("../constants");
var util_1 = require("../util");
/**
 * Compress number (integer or float) to writer
 */
function compressNumber(compressors, context, obj, invertedIndex, writer, options) {
    var foundRef;
    if (obj % 1 === 0) {
        // CHeck if the value is a small integer
        if (obj < constants_1.INTEGER_SMALL_EXCLUSIVE_BOUND_UPPER && obj > constants_1.INTEGER_SMALL_EXCLUSIVE_BOUND_LOWER) {
            writer.write(constants_1.INTEGER_SMALL_TOKENS[obj + constants_1.INTEGER_SMALL_TOKEN_ELEMENT_OFFSET]);
        }
        else if ((foundRef = invertedIndex.integerMap[obj]) !== void 0) {
            writer.write("" + constants_1.REF_INTEGER_TOKEN + foundRef);
        }
        else {
            var ref = util_1.compressInteger(invertedIndex.integerCount);
            var compressedInteger = util_1.compressInteger(obj);
            var newRef = "" + constants_1.INTEGER_TOKEN + compressedInteger;
            if (ref.length + constants_1.REFERENCE_HEADER_LENGTH < newRef.length) {
                invertedIndex.integerMap[obj] = ref;
                invertedIndex.integerCount++;
                writer.write(newRef);
            }
            else {
                writer.write("" + constants_1.UNREFERENCED_INTEGER_TOKEN + compressedInteger);
            }
        }
    }
    else {
        // Compress float prior to lookup to reuse for "same" floating values
        var compressedFloat = util_1.compressFloat(obj, options.fullPrecisionFloats);
        if ((foundRef = invertedIndex.floatMap[compressedFloat]) !== void 0) {
            writer.write("" + constants_1.REF_FLOAT_TOKEN + foundRef);
        }
        else {
            var ref = util_1.compressInteger(invertedIndex.floatCount);
            var newRef = "" + constants_1.FLOAT_TOKEN + compressedFloat;
            if (ref.length + constants_1.REFERENCE_HEADER_LENGTH < newRef.length) {
                invertedIndex.floatMap[compressedFloat] = ref;
                invertedIndex.floatCount++;
                writer.write(newRef);
            }
            else {
                writer.write("" + constants_1.UNREFERENCED_FLOAT_TOKEN + compressedFloat);
            }
        }
    }
}
exports.compressNumber = compressNumber;
//# sourceMappingURL=number.js.map