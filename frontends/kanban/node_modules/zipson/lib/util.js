"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var constants_1 = require("./constants");
var maxInteger = 2147483648;
var minInteger = -2147483649;
var base62 = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
/**
 * Convert number to base62 string
 */
function compressInteger(number) {
    if (number === 0) {
        return '0';
    }
    var result = '';
    var carry = number < 0 ? -number : number;
    var current = 0;
    var fraction;
    while (carry > 0) {
        carry = carry / 62;
        fraction = carry % 1;
        current = ((fraction * 62) + 0.1) << 0;
        carry -= fraction;
        result = base62[current] + result;
    }
    result = number < 0 ? '-' + result : result;
    return result;
}
exports.compressInteger = compressInteger;
/**
 * Convert base62 string to number
 */
function decompressInteger(compressedInteger) {
    var value = 0;
    if (compressedInteger[0] === '0') {
        return value;
    }
    else {
        var negative = compressedInteger[0] === '-';
        var multiplier = 1;
        var leftBound = negative ? 1 : 0;
        for (var i = compressedInteger.length - 1; i >= leftBound; i--) {
            var code = compressedInteger.charCodeAt(i);
            var current = code - 48;
            if (code >= 97) {
                current -= 13;
            }
            else if (code >= 65) {
                current -= 7;
            }
            value += current * multiplier;
            multiplier *= 62;
        }
        return negative ? -value : value;
    }
}
exports.decompressInteger = decompressInteger;
/**
 * Convert float to base62 string for integer and fraction
 */
function compressFloat(float, fullPrecision) {
    if (fullPrecision === void 0) { fullPrecision = false; }
    if (fullPrecision) {
        var _a = float.toString().split('.'), integer = _a[0], fraction = _a[1];
        var operator = integer === '-0' ? '-' : '';
        return "" + operator + compressInteger(parseInt(integer)) + constants_1.FLOAT_FULL_PRECISION_DELIMITER + fraction;
    }
    else {
        var integer = float >= maxInteger ? Math.floor(float) : float <= minInteger ? Math.ceil(float) : float << 0;
        var fraction = Math.round((constants_1.FLOAT_COMPRESSION_PRECISION * (float % 1)));
        return "" + compressInteger(integer) + constants_1.FLOAT_REDUCED_PRECISION_DELIMITER + compressInteger(fraction);
    }
}
exports.compressFloat = compressFloat;
/**
 * Convert base62 integer and fraction to float
 */
function decompressFloat(compressedFloat) {
    if (compressedFloat.indexOf(constants_1.FLOAT_FULL_PRECISION_DELIMITER) > -1) {
        var _a = compressedFloat.split(constants_1.FLOAT_FULL_PRECISION_DELIMITER), integer = _a[0], fraction = _a[1];
        var mult = integer === '-0' ? -1 : 1;
        var uncompressedInteger = decompressInteger(integer);
        return mult * parseFloat(uncompressedInteger + '.' + fraction);
    }
    else {
        var _b = compressedFloat.split(constants_1.FLOAT_REDUCED_PRECISION_DELIMITER), integer = _b[0], fraction = _b[1];
        var uncompressedInteger = decompressInteger(integer);
        var uncompressedFraction = decompressInteger(fraction);
        return uncompressedInteger + uncompressedFraction / constants_1.FLOAT_COMPRESSION_PRECISION;
    }
}
exports.decompressFloat = decompressFloat;
//# sourceMappingURL=util.js.map