'use strict';

var AnimatedFormatQualifierValue = require('./AnimatedFormatQualifierValue-48d5a908.cjs');

/**
 * @description Contains methods to specify the animated format
 * @namespace AnimatedFormat
 * @memberOf Qualifiers
 * @see Visit {@link Actions.Transcode|Transcode} for an example
 */
/**
 * @description Automatically sets the animated format
 * @summary qualifier
 * @memberOf Qualifiers.AnimatedFormat
 * @return {Qualifiers.AnimatedFormatQualifierValue}
 */
function auto() {
    return new AnimatedFormatQualifierValue.AnimatedFormatQualifierValue('auto');
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.AnimatedFormat
 * @return {Qualifiers.AnimatedFormatQualifierValue}
 */
function gif() {
    return new AnimatedFormatQualifierValue.AnimatedFormatQualifierValue('gif');
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.AnimatedFormat
 * @return {Qualifiers.AnimatedFormatQualifierValue}
 */
function webp() {
    return new AnimatedFormatQualifierValue.AnimatedFormatQualifierValue('webp');
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.AnimatedFormat
 * @return {Qualifiers.AnimatedFormatQualifierValue}
 */
function png() {
    return new AnimatedFormatQualifierValue.AnimatedFormatQualifierValue('png');
}
const AnimatedFormat = { auto, gif, webp, png };

exports.AnimatedFormat = AnimatedFormat;
exports.auto = auto;
exports.gif = gif;
exports.png = png;
exports.webp = webp;
