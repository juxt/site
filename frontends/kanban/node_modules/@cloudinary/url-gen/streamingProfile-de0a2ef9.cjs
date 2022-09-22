'use strict';

/**
 * @description Qualifiers for applying an ordered dither filter to the image.
 * @namespace StreamingProfile
 * @memberOf Qualifiers
 * @see Visit {@link Actions
 */
/**
 * @summary qualifier
 * @memberOf Qualifiers.StreamingProfile
 * @return {string}
 */
function fullHd() {
    return 'full_hd';
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.StreamingProfile
 * @return {string}
 */
function hd() {
    return 'hd';
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.StreamingProfile
 * @return {string}
 */
function sd() {
    return 'sd';
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.StreamingProfile
 * @return {string}
 */
function fullHdWifi() {
    return 'full_hd_wifi';
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.StreamingProfile
 * @return {string}
 */
function fullHdLean() {
    return 'full_hd_lean';
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.StreamingProfile
 * @return {string}
 */
function hdLean() {
    return 'hd_lean';
}
const StreamingProfile = {
    hd,
    sd,
    hdLean,
    fullHd,
    fullHdLean,
    fullHdWifi
};

exports.StreamingProfile = StreamingProfile;
exports.fullHd = fullHd;
exports.fullHdLean = fullHdLean;
exports.fullHdWifi = fullHdWifi;
exports.hd = hd;
exports.hdLean = hdLean;
exports.sd = sd;
