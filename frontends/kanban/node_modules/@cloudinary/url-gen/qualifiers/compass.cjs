'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var CompassQualifier = require('../CompassQualifier-59a71fa8.cjs');
require('../QualifierValue-e770d619.cjs');

/**
 * @summary qualifier
 * @memberOf Qualifiers.Compass
 * @description South center part (bottom center).
 * @return {Qualifiers.Compass.CompassQualifier} Compass
 */
function south() {
    return new CompassQualifier.CompassQualifier('south');
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.Compass
 * @description North center part (top center).
 * @return {Qualifiers.Compass.CompassQualifier} Compass
 */
function north() {
    return new CompassQualifier.CompassQualifier('north');
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.Compass
 * @description Middle east part (right).
 * @return {Qualifiers.Compass.CompassQualifier} Compass
 */
function east() {
    return new CompassQualifier.CompassQualifier('east');
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.Compass
 * @description Middle west part (left).
 * @return {Qualifiers.Compass.CompassQualifier} Compass
 */
function west() {
    return new CompassQualifier.CompassQualifier('west');
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.Compass
 * @description North west corner (top left).
 * @return {Qualifiers.Compass.CompassQualifier} Compass
 */
function northWest() {
    return new CompassQualifier.CompassQualifier('north_west');
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.Compass
 * @description North east corner (top right).
 * @return {Qualifiers.Compass.CompassQualifier} Compass
 */
function northEast() {
    return new CompassQualifier.CompassQualifier('north_east');
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.Compass
 * @description South west corner (bottom left).
 * @return {Qualifiers.Compass.CompassQualifier} Compass
 */
function southWest() {
    return new CompassQualifier.CompassQualifier('south_west');
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.Compass
 * @description South east corner (bottom right).
 * @return {Qualifiers.Compass.CompassQualifier} Compass
 */
function southEast() {
    return new CompassQualifier.CompassQualifier('south_east');
}
/**
 * @summary qualifier
 * @memberOf Qualifiers.Compass
 * @description The center of the image.
 * @return {Qualifiers.Compass.CompassQualifier} Compass
 */
function center() {
    return new CompassQualifier.CompassQualifier('center');
}
/**
 * @description Defines the focal Compass for certain methods of cropping.
 * @namespace Compass
 * @memberOf Qualifiers
 * @see Visit {@link Qualifiers.Gravity|Gravity} for an example
 */
class Compass {
}
Compass.north = north;
Compass.west = west;
Compass.east = east;
Compass.south = south;
Compass.center = center;
Compass.northWest = northWest;
Compass.southEast = southEast;
Compass.southWest = southWest;
Compass.northEast = northEast;

exports.Compass = Compass;
exports.center = center;
exports.east = east;
exports.north = north;
exports.northEast = northEast;
exports.northWest = northWest;
exports.south = south;
exports.southEast = southEast;
exports.southWest = southWest;
exports.west = west;
