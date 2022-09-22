import { Context, InvertedIndex, CompressOptions, Compressors } from "./common";
import { ZipsonWriter } from "./writer";
/**
 * Compress number (integer or float) to writer
 */
export declare function compressNumber(compressors: Compressors, context: Context, obj: number, invertedIndex: InvertedIndex, writer: ZipsonWriter, options: CompressOptions): void;
