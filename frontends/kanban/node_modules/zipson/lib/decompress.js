"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var common_1 = require("./decompressor/common");
var stages_1 = require("./decompressor/stages");
/**
 * Create an ordered index for decompression
 */
function makeOrderedIndex() {
    return {
        strings: [],
        integers: [],
        floats: [],
        dates: [],
        lpDates: [],
    };
}
exports.makeOrderedIndex = makeOrderedIndex;
/**
 * Create a new cursor with a root target for specified drain mode
 */
function makeCursor(drain) {
    var rootTarget = { type: common_1.TargetType.SCALAR, value: void 0 };
    var stack = new Array(10);
    stack[0] = rootTarget;
    return { index: 0, rootTarget: rootTarget, stack: stack, currentTarget: rootTarget, pointer: 0, drain: drain };
}
/**
 * Decompress data string with provided ordered index
 */
function decompress(data, orderedIndex) {
    var cursor = makeCursor(true);
    stages_1.decompressStages(cursor, data, orderedIndex);
    return cursor.rootTarget.value;
}
exports.decompress = decompress;
/**
 * Decompress zipson data incrementally by providing each chunk of data in sequence
 */
function decompressIncremental(orderedIndex) {
    var cursor = makeCursor(false);
    // Keep an internal buffer for any unterminated chunks of data
    var buffer = '';
    function increment(data) {
        if (data === null) {
            // Move cursor to drain mode if we got the last chunk of data
            cursor.drain = true;
        }
        else if (data.length === 0) {
            return;
        }
        else {
            buffer += data;
        }
        // Decompress an determine amount of buffer that was parsed
        var cursorIndexBefore = cursor.index;
        stages_1.decompressStages(cursor, buffer, orderedIndex);
        var movedAmount = cursor.index - cursorIndexBefore;
        // Rotate parsed data out of buffer and move cursor back to next parsing position
        if (movedAmount > 0) {
            buffer = buffer.substring(movedAmount);
            cursor.index -= movedAmount;
        }
    }
    return { increment: increment, cursor: cursor };
}
exports.decompressIncremental = decompressIncremental;
//# sourceMappingURL=decompress.js.map