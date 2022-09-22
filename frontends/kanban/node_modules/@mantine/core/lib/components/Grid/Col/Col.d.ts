import React from 'react';
import { DefaultProps, MantineNumberSize } from '@mantine/styles';
export interface ColProps extends DefaultProps, React.ComponentPropsWithoutRef<'div'> {
    /** Default col span */
    span?: number;
    /** Total amount of columns, controlled by Grid component */
    columns?: number;
    /** Column left offset */
    offset?: number;
    /** Column left offset at (min-width: theme.breakpoints.xs) */
    offsetXs?: number;
    /** Column left offset at (min-width: theme.breakpoints.sm) */
    offsetSm?: number;
    /** Column left offset at (min-width: theme.breakpoints.md) */
    offsetMd?: number;
    /** Column left offset at (min-width: theme.breakpoints.lg) */
    offsetLg?: number;
    /** Column left offset at (min-width: theme.breakpoints.xl) */
    offsetXl?: number;
    /** Space between columns from theme, or number to set value in px, controlled by Grid component */
    gutter?: MantineNumberSize;
    /** sets flex-grow to 1 if true, controlled by Grid component */
    grow?: boolean;
    /** Col span at (min-width: theme.breakpoints.xs) */
    xs?: number;
    /** Col span at (min-width: theme.breakpoints.sm) */
    sm?: number;
    /** Col span at (min-width: theme.breakpoints.md) */
    md?: number;
    /** Col span at (min-width: theme.breakpoints.lg) */
    lg?: number;
    /** Col span at (min-width: theme.breakpoints.xl) */
    xl?: number;
}
export declare function isValidSpan(span: number): boolean;
export declare const getColumnWidth: (colSpan: number, columns: number) => string;
export declare function Col({ children, span, gutter, offset, offsetXs, offsetSm, offsetMd, offsetLg, offsetXl, grow, xs, sm, md, lg, xl, columns, className, classNames, styles, id, ...others }: ColProps): JSX.Element;
export declare namespace Col {
    var displayName: string;
}
//# sourceMappingURL=Col.d.ts.map