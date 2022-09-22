import { CSSObject, MantineMargins, MantineTheme } from '@mantine/styles';
declare type Sx = CSSObject | ((theme: MantineTheme) => CSSObject);
export declare type BoxSx = Sx | Sx[];
export declare function useSx(sx: BoxSx, margins: MantineMargins, className: string): string;
export {};
//# sourceMappingURL=use-sx.d.ts.map