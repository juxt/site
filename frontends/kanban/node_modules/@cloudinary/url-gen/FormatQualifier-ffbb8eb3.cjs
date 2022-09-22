'use strict';

var QualifierValue = require('./QualifierValue-e770d619.cjs');

/**
 * @memberOf Qualifiers.Format
 * @extends {SDK.QualifierValue}
 */
class FormatQualifier extends QualifierValue.QualifierValue {
    constructor(val) {
        super(val);
        this.val = val;
    }
    getValue() {
        return this.val;
    }
}

exports.FormatQualifier = FormatQualifier;
