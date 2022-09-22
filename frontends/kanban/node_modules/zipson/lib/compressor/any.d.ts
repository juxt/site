import { InvertedIndex, Context, CompressOptions, Compressors } from "./common";
import { ZipsonWriter } from "./writer";
/**
 * Compress any data type to writer
 */
export declare function compressAny(compressors: Compressors, context: Context, obj: any, invertedIndex: InvertedIndex, writer: ZipsonWriter, options: CompressOptions): void;
