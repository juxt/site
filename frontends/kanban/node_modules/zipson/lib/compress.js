"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var any_1 = require("./compressor/any");
var array_1 = require("./compressor/array");
var string_1 = require("./compressor/string");
var number_1 = require("./compressor/number");
var object_1 = require("./compressor/object");
var date_1 = require("./compressor/date");
var object_2 = require("./compressor/template/object");
var compressors = {
    any: any_1.compressAny,
    array: array_1.compressArray,
    object: object_1.compressObject,
    string: string_1.compressString,
    date: date_1.compressDate,
    number: number_1.compressNumber,
    template: {
        Object: object_2.TemplateObject
    }
};
/**
 * Create a new compression context
 */
function makeCompressContext() {
    return {
        arrayItemWriters: [],
        arrayLevel: 0,
    };
}
exports.makeCompressContext = makeCompressContext;
/**
 * Create an inverted index for compression
 */
function makeInvertedIndex() {
    return {
        stringMap: {},
        integerMap: {},
        floatMap: {},
        dateMap: {},
        lpDateMap: {},
        stringCount: 0,
        integerCount: 0,
        floatCount: 0,
        dateCount: 0,
        lpDateCount: 0,
    };
}
exports.makeInvertedIndex = makeInvertedIndex;
/**
 * Compress all data onto a provided writer
 */
function compress(context, obj, invertedIndex, writer, options) {
    compressors.any(compressors, context, obj, invertedIndex, writer, options);
}
exports.compress = compress;
//# sourceMappingURL=compress.js.map