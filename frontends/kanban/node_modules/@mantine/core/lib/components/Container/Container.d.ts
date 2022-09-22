import React from 'react';
import { DefaultProps, MantineNumberSize } from '@mantine/styles';
export interface ContainerProps extends DefaultProps, React.ComponentPropsWithoutRef<'div'> {
    /** Predefined container max-width or number for max-width in px */
    size?: MantineNumberSize;
    /** Horizontal padding defined in theme.spacing, or number value for padding in px */
    padding?: MantineNumberSize;
    /** If fluid is set to true, size prop is ignored and Container always take 100% of width */
    fluid?: boolean;
}
export declare const Container: React.ForwardRefExoticComponent<ContainerProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=Container.d.ts.map