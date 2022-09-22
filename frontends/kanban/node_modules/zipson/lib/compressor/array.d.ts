import { Context, InvertedIndex, CompressOptions, Compressors } from "./common";
import { ZipsonWriter } from "./writer";
/**
 * Compress array to writer
 */
export declare function compressArray(compressors: Compressors, context: Context, array: any[], invertedIndex: InvertedIndex, writer: ZipsonWriter, options: CompressOptions): void;
