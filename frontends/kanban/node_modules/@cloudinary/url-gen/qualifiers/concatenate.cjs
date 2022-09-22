'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var VideoSource = require('../VideoSource-c3c76a47.cjs');
var ImageSource = require('../ImageSource-2890c2e5.cjs');
var FetchSource = require('../FetchSource-b49b90bf.cjs');
require('../BaseSource-d2277592.cjs');
require('../QualifierModel-0923d819.cjs');
require('../unsupportedError-74070138.cjs');
require('../FormatQualifier-ffbb8eb3.cjs');
require('../QualifierValue-e770d619.cjs');
require('../base64Encode-08c19f63.cjs');

/**
 * @description This namespace contains different sources that can be used when concatenating to a video
 * @memberOf Qualifiers
 * @namespace Concatenate
 * @see Visit {@link Actions.VideoEdit.concatenate|VideoEdit.concatenate} for an example
 */
/**
 * @summary qualifier
 * @description Returns an instance of an ImageSource
 * @memberOf Qualifiers.Concatenate
 * @param {string} publicID The publicID of the image to be used to concatenate
 * @return {Source.ImageSource}
 */
function imageSource(publicID) {
    return new ImageSource.ImageSource(publicID);
}
/**
 * @summary qualifier
 * @description Returns an instance of a VideoSource
 * @memberOf Qualifiers.Concatenate
 * @param {string} publicID The publicID of the video to be used to concatenate
 * @return {Source.VideoSource}
 */
function videoSource(publicID) {
    return new VideoSource.VideoSource(publicID);
}
/**
 * @summary qualifier
 * @description Returns an instance of a FetchSource
 * @memberOf Qualifiers.Concatenate
 * @param {string} remoteURL The URL of the remote asset to fetch as and to be used to concatenate
 * @return {Source.FetchSource}
 */
function fetchSource(remoteURL) {
    return new FetchSource.FetchSource(remoteURL);
}
const Concatenate = { imageSource, videoSource, fetchSource };

exports.Concatenate = Concatenate;
exports.fetchSource = fetchSource;
exports.imageSource = imageSource;
exports.videoSource = videoSource;
