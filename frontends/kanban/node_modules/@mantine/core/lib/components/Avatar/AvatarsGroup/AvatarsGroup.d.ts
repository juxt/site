import React from 'react';
import { DefaultProps, MantineNumberSize, ClassNames } from '@mantine/styles';
import useStyles from './AvatarsGroup.styles';
export declare type AvatarsGroupStylesNames = ClassNames<typeof useStyles>;
export interface AvatarsGroupProps extends DefaultProps<AvatarsGroupStylesNames>, React.ComponentPropsWithoutRef<'div'> {
    /** <Avatar /> components only */
    children?: React.ReactNode;
    /** Child <Avatar /> components width and height */
    size?: MantineNumberSize;
    /** Child <Avatar /> radius */
    radius?: MantineNumberSize;
    /** Maximum amount of <Avatar /> components rendered, everything after limit is truncated */
    limit?: number;
    /** Spacing between avatars */
    spacing?: MantineNumberSize;
    /** Total number of child <Avatar />, overrides the truncated amount */
    total?: number;
}
export declare const AvatarsGroup: React.ForwardRefExoticComponent<AvatarsGroupProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=AvatarsGroup.d.ts.map