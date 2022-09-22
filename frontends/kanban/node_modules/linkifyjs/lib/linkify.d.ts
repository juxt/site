export var __esModule: boolean;
/**
 * @class Options
 * @param {Object} [opts] Set option properties besides the defaults
 */
export function Options(opts?: any): void;
export class Options {
    /**
     * @class Options
     * @param {Object} [opts] Set option properties besides the defaults
     */
    constructor(opts?: any);
    defaultProtocol: any;
    events: any;
    format: any;
    formatHref: any;
    nl2br: any;
    tagName: any;
    target: any;
    rel: any;
    validate: any;
    truncate: any;
    className: any;
    attributes: any;
    ignoreTags: any[];
    resolve: (token: any) => {
        formatted: any;
        formattedHref: any;
        tagName: any;
        className: any;
        target: any;
        rel: any;
        events: any;
        attributes: any;
        truncate: any;
    };
    check: (token: any) => any;
    get: (key: string, operator: any, token: MultiToken) => any;
    getObject: (key: any, operator: any, token: any) => any;
}
/**
    Find a list of linkable items in the given string.
    @param {string} str string to find links in
    @param {string} [type] (optional) only find links of a specific type, e.g.,
    'url' or 'email'
*/
export function find(str: string, ...args: any[]): {
    type: string;
    value: any;
    isLink: boolean;
    href: string;
    start: number;
    end: number;
}[];
/**
 * Initialize the linkify state machine. Called automatically the first time
 * linkify is called on a string, but may be called manually as well.
 */
export function init(): void;
export var options: Readonly<{
    __proto__: any;
    defaults: {
        defaultProtocol: string;
        events: any;
        format: typeof noop;
        formatHref: typeof noop;
        nl2br: boolean;
        tagName: string;
        target: any;
        rel: any;
        validate: boolean;
        truncate: number;
        className: any;
        attributes: any;
        ignoreTags: any[];
    };
    Options: typeof Options;
}>;
/**
 * Detect URLs with the following additional protocol. Anything following
 * "protocol:" will be considered a link.
 * @param {string} protocol
 */
export function registerCustomProtocol(protocol: string): void;
/**
 * Register a linkify extension plugin
 * @param {string} name of plugin to register
 * @param {Function} plugin function that accepts mutable linkify state
 */
export function registerPlugin(name: string, plugin: Function): void;
/**
 * De-register all plugins and reset the internal state-machine. Used for
 * testing; not required in practice.
 * @private
 */
export function reset(): void;
/**
 * Is the given string valid linkable text of some sort. Note that this does not
 * trim the text for you.
 *
 * Optionally pass in a second `type` param, which is the type of link to test
 * for.
 *
 * For example,
 *
 *     linkify.test(str, 'email');
 *
 * Returns `true` if str is a valid email.
 * @param {string} str string to test for links
 * @param {string} [type] optional specific link type to look for
 * @returns boolean true/false
 */
export function test(str: string, ...args: any[]): boolean;
/**
    Parse a string into tokens that represent linkable and non-linkable sub-components
    @param {string} str
    @return {MultiToken[]} tokens
*/
export function tokenize(str: string): MultiToken[];
/**
    Abstract class used for manufacturing tokens of text tokens. That is rather
    than the value for a token being a small string of text, it's value an array
    of text tokens.

    Used for grouping together URLs, emails, hashtags, and other potential
    creations.

    @class MultiToken
    @param {string} value
    @param {{t: string, v: string, s: number, e: number}[]} tokens
    @abstract
*/
declare function MultiToken(): void;
declare class MultiToken {
    t: string;
    isLink: boolean;
    toString: () => string;
    toHref: () => string;
    startIndex: () => number;
    endIndex: () => number;
    toObject: (...args: any[]) => {
        type: string;
        value: any;
        isLink: boolean;
        href: string;
        start: number;
        end: number;
    };
}
declare function noop(val: any): any;
export {};
