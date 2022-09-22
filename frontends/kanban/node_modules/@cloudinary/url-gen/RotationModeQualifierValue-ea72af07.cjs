'use strict';

var QualifierValue = require('./QualifierValue-e770d619.cjs');

/**
 * @description Acts as a marker for inputs passed into Rotate.mode()
 * @memberOf Qualifiers.RotationMode
 * @extends SDK.QualifierValue
 */
class RotationModeQualifierValue extends QualifierValue.QualifierValue {
    constructor(val) {
        super();
        this.val = val;
    }
    toString() {
        return this.val;
    }
}

exports.RotationModeQualifierValue = RotationModeQualifierValue;
