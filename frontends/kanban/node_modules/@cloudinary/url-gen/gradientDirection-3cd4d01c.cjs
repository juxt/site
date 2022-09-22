'use strict';

var GradientDirectionQualifierValue = require('./GradientDirectionQualifierValue-a6c6b20b.cjs');

/**
 * @description Defines the direction for a background gradient fade effect.
 * @memberOf Qualifiers
 * @namespace GradientDirection
 */
/**
 * @summary qualifier
 * @description Blend the colors horizontally.
 * @memberOf Qualifiers.GradientDirection
 * @return {Qualifiers.GradientDirection.GradientDirectionQualifierValue}
 */
function horizontal() {
    return new GradientDirectionQualifierValue.GradientDirectionQualifierValue('horizontal');
}
/**
 * @summary qualifier
 * @description Blend the colors vertically.
 * @memberOf Qualifiers.GradientDirection
 * @return {Qualifiers.GradientDirection.GradientDirectionQualifierValue}
 */
function vertical() {
    return new GradientDirectionQualifierValue.GradientDirectionQualifierValue('vertical');
}
/**
 * @summary qualifier
 * @description Blend the colors diagonally from top-left to bottom-right.
 * @memberOf Qualifiers.GradientDirection
 * @return {Qualifiers.GradientDirection.GradientDirectionQualifierValue}
 */
function diagonalDesc() {
    return new GradientDirectionQualifierValue.GradientDirectionQualifierValue('diagonal_desc');
}
/**
 * @summary qualifier
 * @description Blend the colors diagonally from bottom-left to top-right.
 * @memberOf Qualifiers.GradientDirection
 * @return {Qualifiers.GradientDirection.GradientDirectionQualifierValue}
 */
function diagonalAsc() {
    return new GradientDirectionQualifierValue.GradientDirectionQualifierValue('diagonal_asc');
}
const GradientDirection = {
    horizontal,
    vertical,
    diagonalDesc,
    diagonalAsc
};

exports.GradientDirection = GradientDirection;
exports.diagonalAsc = diagonalAsc;
exports.diagonalDesc = diagonalDesc;
exports.horizontal = horizontal;
exports.vertical = vertical;
