import { Context, InvertedIndex, CompressOptions, Compressors } from "./common";
import { ZipsonWriter } from "./writer";
/**
 * Compress date (as unix timestamp) to writer
 */
export declare function compressDate(compressors: Compressors, context: Context, obj: number, invertedIndex: InvertedIndex, writer: ZipsonWriter, options: CompressOptions): void;
