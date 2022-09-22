import React from 'react';
import { DefaultProps, MantineNumberSize, MantineShadow, ClassNames } from '@mantine/styles';
import useStyles from './PopoverBody.styles';
export declare type PopoverBodyStylesNames = ClassNames<typeof useStyles>;
export interface PopoverBodyProps extends DefaultProps<PopoverBodyStylesNames>, Omit<React.ComponentPropsWithoutRef<'div'>, 'title'> {
    shadow: MantineShadow;
    radius: MantineNumberSize;
    spacing: MantineNumberSize;
    withCloseButton: boolean;
    titleId: string;
    bodyId: string;
    onClose(): void;
    closeButtonLabel: string;
    width?: number | string;
    title?: React.ReactNode;
}
export declare const PopoverBody: React.ForwardRefExoticComponent<PopoverBodyProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=PopoverBody.d.ts.map