'use strict';

var QualifierValue = require('./QualifierValue-e770d619.cjs');
var QualifierModel = require('./QualifierModel-0923d819.cjs');

/**
 * @summary SDK
 * @memberOf SDK
 */
class Qualifier extends QualifierModel.QualifierModel {
    constructor(key, qualifierValue) {
        super();
        this.delimiter = '_'; // {key}{delimiter}{qualifierValue}
        this.key = key;
        if (qualifierValue instanceof QualifierValue.QualifierValue) {
            this.qualifierValue = qualifierValue;
        }
        else {
            this.qualifierValue = new QualifierValue.QualifierValue();
            this.qualifierValue.addValue(qualifierValue);
        }
    }
    toString() {
        const { key, delimiter, qualifierValue } = this;
        return `${key}${delimiter}${qualifierValue.toString()}`;
    }
    addValue(value) {
        this.qualifierValue.addValue(value);
        return this;
    }
}

exports.Qualifier = Qualifier;
