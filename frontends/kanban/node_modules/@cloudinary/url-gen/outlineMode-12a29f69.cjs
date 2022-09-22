'use strict';

/**
 * @description Contains functions to select the type of improvement to perform when using Adjust.improve().
 * @namespace Outline
 * @memberOf Qualifiers
 * @see Visit {@link Actions.Effect|Effect Action} for an example
 */
/**
 * @summary qualifier
 * @memberOf Qualifiers.Outline
 */
function fill() {
    return 'fill';
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.Outline
 */
function inner() {
    return 'inner';
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.Outline
 */
function innerFill() {
    return 'inner_fill';
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.Outline
 */
function outer() {
    return 'outer';
}
const OutlineMode = {
    outer,
    inner,
    innerFill,
    fill
};

exports.OutlineMode = OutlineMode;
exports.fill = fill;
exports.inner = inner;
exports.innerFill = innerFill;
exports.outer = outer;
