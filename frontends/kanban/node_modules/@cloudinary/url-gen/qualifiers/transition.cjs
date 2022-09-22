'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var VideoSource = require('../VideoSource-c3c76a47.cjs');
require('../BaseSource-d2277592.cjs');
require('../QualifierModel-0923d819.cjs');
require('../unsupportedError-74070138.cjs');

/**
 * @description This namespace contains different sources that can be used as a transition between two videos
 * @memberOf Qualifiers
 * @namespace Transition
 * @see Visit {@link Actions.VideoEdit.concatenate|VideoEdit.concatenate} for an example
 */
/**
 * @description Returns an instance of a VideoSource
 * @memberOf Qualifiers.Transition
 * @param {string} publicID The publicID of the video to be used as a transition
 * @return {Qualifiers.Source.VideoSource}
 */
function videoSource(publicID) {
    return new VideoSource.VideoSource(publicID);
}
const Transition = { videoSource };

exports.Transition = Transition;
exports.videoSource = videoSource;
