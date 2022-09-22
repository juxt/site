import React from 'react';
import { MantineTheme } from '@mantine/styles';
import { SelectRightSectionProps } from './SelectRightSection';
interface GetRightSectionProps extends SelectRightSectionProps {
    rightSection?: React.ReactNode;
    rightSectionWidth?: number;
    styles: Record<string, any>;
    theme: MantineTheme;
}
export declare function getSelectRightSectionProps({ styles, rightSection, rightSectionWidth, theme, ...props }: GetRightSectionProps): {
    rightSection: true | React.ReactChild | React.ReactFragment | React.ReactPortal;
    rightSectionWidth: number;
    styles: Record<string, any>;
} | {
    rightSectionWidth: number;
    rightSection: JSX.Element;
    styles: any;
};
export {};
//# sourceMappingURL=get-select-right-section-props.d.ts.map