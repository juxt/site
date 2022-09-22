/**
 * A zipson writer takes a piece of zipson compression output as a string
 */
export declare abstract class ZipsonWriter {
    abstract write(data: string): void;
    abstract end(): void;
}
/**
 * Writes zipson compression outupt in full to a string
 */
export declare class ZipsonStringWriter extends ZipsonWriter {
    value: string;
    write(data: string): void;
    end(): void;
}
