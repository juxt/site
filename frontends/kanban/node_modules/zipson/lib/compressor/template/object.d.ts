import { Context, InvertedIndex, CompressOptions, Compressors, TemplateCompressor } from "../common";
import { ZipsonWriter } from "../writer";
export declare class TemplateObject implements TemplateCompressor<any> {
    isTemplating: boolean;
    private struct;
    /**
     * Create a new template object starting with two initial object that might have a shared structure
     */
    constructor(a: any, b: any);
    /**
     * Compress template to writer
     */
    compressTemplate(compressors: Compressors, context: Context, invertedIndex: InvertedIndex, writer: ZipsonWriter, options: CompressOptions): void;
    /**
     * Compress object values according to structure to writer
     */
    compressTemplateValues(compressors: Compressors, context: Context, invertedIndex: InvertedIndex, writer: ZipsonWriter, options: CompressOptions, obj: any): void;
    /**
     * Determine if object is templateable according to existing structure
     * If not the an ending token will be written to writer
     */
    isNextTemplateable(obj: any, writer: ZipsonWriter): void;
    /**
     * Finalize template object and write ending token
     */
    end(writer: ZipsonWriter): void;
}
