"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var constants_1 = require("../constants");
/**
 * Compress any data type to writer
 */
function compressAny(compressors, context, obj, invertedIndex, writer, options) {
    var type = typeof obj;
    if (type === 'number') {
        compressors.number(compressors, context, obj, invertedIndex, writer, options);
    }
    else if (type === 'string') {
        compressors.string(compressors, context, obj, invertedIndex, writer, options);
    }
    else if (type === 'boolean') {
        writer.write(obj ? constants_1.BOOLEAN_TRUE_TOKEN : constants_1.BOOLEAN_FALSE_TOKEN);
    }
    else if (obj === null) {
        writer.write(constants_1.NULL_TOKEN);
    }
    else if (obj === undefined) {
        writer.write(constants_1.UNDEFINED_TOKEN);
    }
    else if (Array.isArray(obj)) {
        compressors.array(compressors, context, obj, invertedIndex, writer, options);
    }
    else if (obj instanceof Date) {
        compressors.date(compressors, context, obj.getTime(), invertedIndex, writer, options);
    }
    else {
        compressors.object(compressors, context, obj, invertedIndex, writer, options);
    }
}
exports.compressAny = compressAny;
//# sourceMappingURL=any.js.map