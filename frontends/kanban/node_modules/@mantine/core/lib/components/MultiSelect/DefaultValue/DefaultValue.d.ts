import React from 'react';
import { DefaultProps, MantineSize, MantineNumberSize, ClassNames } from '@mantine/styles';
import useStyles from './DefaultValue.styles';
export declare type DefaultValueStylesNames = ClassNames<typeof useStyles>;
export interface MultiSelectValueProps extends DefaultProps<DefaultValueStylesNames>, React.ComponentPropsWithoutRef<'div'> {
    label: string;
    onRemove(): void;
    disabled: boolean;
    size: MantineSize;
    radius: MantineNumberSize;
}
export declare function DefaultValue({ label, classNames, styles, className, onRemove, disabled, size, radius, ...others }: MultiSelectValueProps): JSX.Element;
export declare namespace DefaultValue {
    var displayName: string;
}
//# sourceMappingURL=DefaultValue.d.ts.map