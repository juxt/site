# Installation
> `npm install --save @types/json-stable-stringify`

# Summary
This package contains type definitions for json-stable-stringify (https://github.com/substack/json-stable-stringify).

# Details
Files were exported from https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/json-stable-stringify.
## [index.d.ts](https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/json-stable-stringify/index.d.ts)
````ts
// Type definitions for json-stable-stringify 1.0
// Project: https://github.com/substack/json-stable-stringify
// Definitions by: Matt Frantz <https://github.com/mhfrantz>
// Definitions: https://github.com/DefinitelyTyped/DefinitelyTyped

/**
 * Deterministic version of JSON.stringify() so you can get a consistent hash from stringified results.
 *
 * @returns Deterministic json result.
 */
declare function stringify(obj: any, opts?: stringify.Comparator | stringify.Options): string;

declare namespace stringify {
    interface Element {
        key: string;
        value: any;
    }

    type Comparator = (a: Element, b: Element) => number;

    type Replacer = (key: string, value: any) => any;

    interface Options {
        /**
         * Custom comparator for key
         */
        cmp?: Comparator;

        /**
         * Indent the output for pretty-printing.
         *
         * Supported is either a string or a number of spaces.
         */
        space?: string | number;

        /**
         * Option to replace values to simpler values
         */
        replacer?: Replacer;

        /**
         * true to allow cycles, by marking the entries as __cycle__.
         */
        cycles?: boolean;
    }
}

export = stringify;

````

### Additional Details
 * Last updated: Wed, 02 Mar 2022 17:31:50 GMT
 * Dependencies: none
 * Global values: none

# Credits
These definitions were written by [Matt Frantz](https://github.com/mhfrantz).
