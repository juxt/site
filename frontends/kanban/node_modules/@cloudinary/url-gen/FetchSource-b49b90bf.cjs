'use strict';

var BaseSource = require('./BaseSource-d2277592.cjs');
var FormatQualifier = require('./FormatQualifier-ffbb8eb3.cjs');
var base64Encode = require('./base64Encode-08c19f63.cjs');

/**
 * @memberOf Qualifiers.Source
 * @extends {Qualifiers.Source.BaseSource}
 * @description Defines how to manipulate a Fetch layer
 * <div class="panel panel-warning">
 *   <div class="panel-heading">Notice</div>
 *   <div class="panel-body">
 *     This class is used as a Qualifier for the asset.overlay() and asset.underlay() methods.</br>
 *     You can find regular images and videos transformations below:
 *   </div>
  *   <ul>
 *     <li>{@link SDK.ImageTransformation| Image Transformations}</li>
 *     <li>{@link SDK.VideoTransformation| Video Transformations}</li>
 *   </ul>
 * </div>
 *
 * {@link https://cloudinary.com/documentation/fetch_remote_images|Learn more about fetching from a remote URL}
 */
class FetchSource extends BaseSource.BaseSource {
    constructor(remoteURL) {
        super();
        this._qualifierModel = {
            sourceType: 'fetch',
            url: remoteURL
        };
        this._remoteURL = remoteURL;
    }
    /**
     * @description
     * Returns the opening string of the layer,
     * This method is used internally within {@link SDK.LayerAction|LayerAction}
     * @returns {string}
     */
    getOpenSourceString(layerType) {
        if (this._format) {
            return `${layerType}_fetch:${base64Encode.base64Encode(this._remoteURL)}.${this._format.toString()}`;
        }
        else {
            return `${layerType}_fetch:${base64Encode.base64Encode(this._remoteURL)}`;
        }
    }
    /**
     * @description
     * Apply a format for the image source of the layer
     * @param {FormatQualifier} format A to apply to the layered image, see more {@link Qualifiers.Format|here}
     * @returns {this}
     */
    format(format) {
        this._qualifierModel.format = format.toString();
        this._format = format;
        return this;
    }
    static fromJson(qualifierModel, transformationFromJson) {
        const { url, transformation, format } = qualifierModel;
        // We are using this() to allow inheriting classes to use super.fromJson.apply(this, [qualifierModel])
        // This allows the inheriting classes to determine the class to be created
        const result = new this(url);
        if (transformation) {
            result.transformation(transformationFromJson(transformation));
        }
        if (format) {
            result.format(new FormatQualifier.FormatQualifier(format));
        }
        return result;
    }
}

exports.FetchSource = FetchSource;
