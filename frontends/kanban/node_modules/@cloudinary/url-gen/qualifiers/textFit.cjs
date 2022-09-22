'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var Qualifier = require('../Qualifier-6633a22f.cjs');
require('../QualifierValue-e770d619.cjs');
require('../QualifierModel-0923d819.cjs');
require('../unsupportedError-74070138.cjs');

/**
 * @description Contains functions that Applies automatic multi-line text wrap.
 * <b>Learn more</b>: {@link https://cloudinary.com/documentation/layers#adding_multi_line_text|Adding multi line text}
 * @memberOf Qualifiers
 * @namespace TextFitQualifier
 */
class TextFitQualifier extends Qualifier.Qualifier {
    constructor(width, height) {
        //@ts-ignore
        super();
        this._width = width;
        this._height = height;
    }
    height(height) {
        this._height = height;
        return this;
    }
    toString() {
        return this._height ? `c_fit,w_${this._width},h_${this._height}` : `c_fit,w_${this._width}`;
    }
}
/**
 * @summary qualifier Adding an automatic multi-line text wrap.
 * @memberOf Qualifiers.TextFitQualifier
 * @param {number} width The width in pixels.
 * @param {number} height The height in pixels.
 */
function size(width, height) {
    return new TextFitQualifier(width, height);
}
const TextFit = { size };

exports.TextFit = TextFit;
exports.TextFitQualifier = TextFitQualifier;
exports.size = size;
