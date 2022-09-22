import type { MantineMargins } from '../../types';
export declare function extractMargins(others: MantineMargins & {
    [key: string]: any;
}): {
    margins: {
        m: import("../../types").MantineNumberSize | (string & {});
        mx: import("../../types").MantineNumberSize | (string & {});
        my: import("../../types").MantineNumberSize | (string & {});
        mt: import("../../types").MantineNumberSize | (string & {});
        mb: import("../../types").MantineNumberSize | (string & {});
        ml: import("../../types").MantineNumberSize | (string & {});
        mr: import("../../types").MantineNumberSize | (string & {});
    };
    rest: {
        [key: string]: any;
    };
};
//# sourceMappingURL=extract-margins.d.ts.map