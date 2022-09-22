import { Context, InvertedIndex, CompressOptions, Compressors } from "./common";
import { ZipsonWriter } from "./writer";
/**
 * Compress object to writer
 */
export declare function compressObject(compressors: Compressors, context: Context, obj: any, invertedIndex: InvertedIndex, writer: ZipsonWriter, options: CompressOptions): void;
