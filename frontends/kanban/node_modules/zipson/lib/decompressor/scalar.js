"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var constants_1 = require("../constants");
var common_1 = require("./common");
var util_1 = require("../util");
function decompressScalar(token, data, cursor, orderedIndex) {
    var startIndex = cursor.index;
    var endIndex = cursor.index + 1;
    // Find end index of token value
    var foundStringToken;
    if (((token === constants_1.STRING_TOKEN) && (foundStringToken = constants_1.STRING_TOKEN))
        || ((token === constants_1.UNREFERENCED_STRING_TOKEN) && (foundStringToken = constants_1.UNREFERENCED_STRING_TOKEN))) {
        var escaped = true;
        while (escaped && endIndex < data.length) {
            endIndex = data.indexOf(foundStringToken, endIndex);
            var iNumEscapeCharacters = 1;
            escaped = false;
            while (data[endIndex - iNumEscapeCharacters] === constants_1.ESCAPE_CHARACTER) {
                escaped = iNumEscapeCharacters % 2 === 1;
                iNumEscapeCharacters++;
            }
            endIndex++;
        }
        if (endIndex <= startIndex) {
            endIndex = data.length;
        }
    }
    else {
        while (!(data.charCodeAt(endIndex) > constants_1.DELIMITING_TOKENS_THRESHOLD) && endIndex < data.length) {
            endIndex++;
        }
    }
    if (!cursor.drain && endIndex === data.length) {
        return common_1.SKIP_SCALAR;
    }
    // Update cursor end index
    cursor.index = endIndex - 1;
    var tokenCharCode = token.charCodeAt(0);
    // Decompress the token value
    if (tokenCharCode > constants_1.INTEGER_SMALL_TOKEN_EXCLUSIVE_BOUND_LOWER && tokenCharCode < constants_1.INTEGER_SMALL_TOKEN_EXCLUSIVE_BOUND_UPPER) {
        return tokenCharCode + constants_1.INTEGER_SMALL_TOKEN_OFFSET;
    }
    else if (token === constants_1.ARRAY_REPEAT_MANY_TOKEN) {
        return util_1.decompressInteger(data.substring(startIndex + 1, endIndex));
    }
    else if (token === constants_1.REF_STRING_TOKEN) {
        return orderedIndex.strings[util_1.decompressInteger(data.substring(startIndex + 1, endIndex))];
    }
    else if (token === constants_1.REF_INTEGER_TOKEN) {
        return orderedIndex.integers[util_1.decompressInteger(data.substring(startIndex + 1, endIndex))];
    }
    else if (token === constants_1.REF_FLOAT_TOKEN) {
        return orderedIndex.floats[util_1.decompressInteger(data.substring(startIndex + 1, endIndex))];
    }
    else if (token === constants_1.REF_DATE_TOKEN) {
        return orderedIndex.dates[util_1.decompressInteger(data.substring(startIndex + 1, endIndex))];
    }
    else if (token === constants_1.REF_LP_DATE_TOKEN) {
        return orderedIndex.lpDates[util_1.decompressInteger(data.substring(startIndex + 1, endIndex))];
    }
    else if (token === constants_1.STRING_TOKEN) {
        return orderedIndex.strings[orderedIndex.strings.length] = data.substring(startIndex + 1, endIndex - 1).replace(constants_1.REGEX_ESCAPED_ESCAPE_CHARACTER, constants_1.ESCAPE_CHARACTER).replace(constants_1.REGEX_ESCAPED_STRING_TOKEN, constants_1.STRING_TOKEN);
    }
    else if (token === constants_1.INTEGER_TOKEN) {
        return orderedIndex.integers[orderedIndex.integers.length] = util_1.decompressInteger(data.substring(startIndex + 1, endIndex));
    }
    else if (token === constants_1.FLOAT_TOKEN) {
        return orderedIndex.floats[orderedIndex.floats.length] = util_1.decompressFloat(data.substring(startIndex + 1, endIndex));
    }
    else if (token === constants_1.DATE_TOKEN) {
        return orderedIndex.dates[orderedIndex.dates.length] = new Date(util_1.decompressInteger(data.substring(startIndex + 1, endIndex))).toISOString();
    }
    else if (token === constants_1.LP_DATE_TOKEN) {
        return orderedIndex.lpDates[orderedIndex.lpDates.length] = new Date(constants_1.DATE_LOW_PRECISION * util_1.decompressInteger(data.substring(startIndex + 1, endIndex))).toISOString();
    }
    else if (token === constants_1.UNREFERENCED_STRING_TOKEN) {
        return data.substring(startIndex + 1, endIndex - 1).replace(constants_1.REGEX_ESCAPED_ESCAPE_CHARACTER, constants_1.ESCAPE_CHARACTER).replace(constants_1.REGEX_UNREFERENCED_ESCAPED_STRING_TOKEN, constants_1.UNREFERENCED_STRING_TOKEN);
    }
    else if (token === constants_1.UNREFERENCED_INTEGER_TOKEN) {
        return util_1.decompressInteger(data.substring(startIndex + 1, endIndex));
    }
    else if (token === constants_1.UNREFERENCED_FLOAT_TOKEN) {
        return util_1.decompressFloat(data.substring(startIndex + 1, endIndex));
    }
    else if (token === constants_1.UNREFERENCED_DATE_TOKEN) {
        return new Date(util_1.decompressInteger(data.substring(startIndex + 1, endIndex))).toISOString();
    }
    else if (token === constants_1.UNREFERENCED_LP_DATE_TOKEN) {
        return new Date(constants_1.DATE_LOW_PRECISION * util_1.decompressInteger(data.substring(startIndex + 1, endIndex))).toISOString();
    }
    else if (token === constants_1.BOOLEAN_TRUE_TOKEN) {
        return true;
    }
    else if (token === constants_1.BOOLEAN_FALSE_TOKEN) {
        return false;
    }
    else if (token === constants_1.NULL_TOKEN) {
        return null;
    }
    else if (token === constants_1.UNDEFINED_TOKEN) {
        return undefined;
    }
    throw new Error("Unexpected scalar " + token + " at " + startIndex + "-" + endIndex);
}
exports.decompressScalar = decompressScalar;
//# sourceMappingURL=scalar.js.map