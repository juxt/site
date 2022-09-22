"use strict";
function __export(m) {
    for (var p in m) if (!exports.hasOwnProperty(p)) exports[p] = m[p];
}
Object.defineProperty(exports, "__esModule", { value: true });
var compress_1 = require("./compress");
var writer_1 = require("./compressor/writer");
var decompress_1 = require("./decompress");
__export(require("./compressor/writer"));
__export(require("./decompressor/common"));
/**
 * Parse a zipson data string
 */
function parse(data) {
    var orderedIndex = decompress_1.makeOrderedIndex();
    return decompress_1.decompress(data, orderedIndex);
}
exports.parse = parse;
/**
 * Incrementally parse a zipson data string in chunks
 */
function parseIncremental() {
    var orderedIndex = decompress_1.makeOrderedIndex();
    var _a = decompress_1.decompressIncremental(orderedIndex), cursor = _a.cursor, increment = _a.increment;
    return function (data) {
        increment(data);
        if (data === null) {
            return cursor.rootTarget.value;
        }
    };
}
exports.parseIncremental = parseIncremental;
/**
 * Stringify any data to a zipson writer
 */
function stringifyTo(data, writer, options) {
    if (options === void 0) { options = {}; }
    var invertedIndex = compress_1.makeInvertedIndex();
    var context = compress_1.makeCompressContext();
    compress_1.compress(context, data, invertedIndex, writer, options);
    writer.end();
}
exports.stringifyTo = stringifyTo;
/**
 * Stringify any data to a string
 */
function stringify(data, options) {
    var writer = new writer_1.ZipsonStringWriter();
    stringifyTo(data, writer, options);
    return writer.value;
}
exports.stringify = stringify;
//# sourceMappingURL=index.js.map