'use strict';

var Qualifier = require('./Qualifier-6633a22f.cjs');
var QualifierValue = require('./QualifierValue-e770d619.cjs');

/**
 * @memberOf Gravity.GravityQualifier
 * @extends {SDK.Qualifier}
 */
class GravityQualifier extends Qualifier.Qualifier {
    /**
     * @param value, an array containing (GravityObject | AutoGravity | string) or a string;
     */
    constructor(value) {
        super('g', new QualifierValue.QualifierValue(value));
    }
}

exports.GravityQualifier = GravityQualifier;
