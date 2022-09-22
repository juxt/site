'use strict';

var QualifierValue = require('./QualifierValue-e770d619.cjs');

/**
 * @memberOf Qualifiers.Compass
 * @extends {SDK.QualifierValue}
 */
class CompassQualifier extends QualifierValue.QualifierValue {
    constructor(val) {
        super();
        this.val = val;
    }
    toString() {
        return this.val;
    }
}

exports.CompassQualifier = CompassQualifier;
