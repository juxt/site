'use strict';

var QualifierValue = require('./QualifierValue-e770d619.cjs');

/**
 * @namespace Expression
 * @memberOf Qualifiers.Expression
 * @extends {SDK.QualifierValue}
 */
class ExpressionQualifier extends QualifierValue.QualifierValue {
    constructor(value) {
        super();
        this.value = value;
    }
    toString() {
        return this.value;
    }
}

exports.ExpressionQualifier = ExpressionQualifier;
