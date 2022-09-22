"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var constants_1 = require("../constants");
var util_1 = require("../util");
/**
 * Compress string to
 */
function compressString(compressors, context, obj, invertedIndex, writer, options) {
    var foundRef;
    //
    var stringIdent = constants_1.STRING_IDENT_PREFIX + obj;
    // Detect if string is utc timestamp if enabled
    if (options.detectUtcTimestamps && obj[obj.length - 1] === 'Z' && obj.match(constants_1.DATE_REGEX)) {
        var date = Date.parse(obj);
        compressors.date(compressors, context, date, invertedIndex, writer, options);
    }
    else if ((foundRef = invertedIndex.stringMap[stringIdent]) !== void 0) {
        writer.write("" + constants_1.REF_STRING_TOKEN + foundRef);
    }
    else {
        var ref = util_1.compressInteger(invertedIndex.stringCount);
        var newRef = "" + constants_1.STRING_TOKEN + obj.replace(constants_1.REGEX_ESCAPE_CHARACTER, constants_1.ESCAPE_CHARACTER + constants_1.ESCAPE_CHARACTER).replace(constants_1.REGEX_STRING_TOKEN, constants_1.ESCAPED_STRING_TOKEN) + constants_1.STRING_TOKEN;
        if (ref.length + constants_1.REFERENCE_HEADER_LENGTH + 1 < newRef.length) {
            invertedIndex.stringMap[stringIdent] = ref;
            invertedIndex.stringCount++;
            writer.write(newRef);
        }
        else {
            writer.write("" + constants_1.UNREFERENCED_STRING_TOKEN + obj.replace(constants_1.REGEX_ESCAPE_CHARACTER, constants_1.ESCAPE_CHARACTER + constants_1.ESCAPE_CHARACTER).replace(constants_1.REGEX_UNREFERENCED_STRING_TOKEN, constants_1.ESCAPED_UNREFERENCED_STRING_TOKEN) + constants_1.UNREFERENCED_STRING_TOKEN);
        }
    }
}
exports.compressString = compressString;
//# sourceMappingURL=string.js.map