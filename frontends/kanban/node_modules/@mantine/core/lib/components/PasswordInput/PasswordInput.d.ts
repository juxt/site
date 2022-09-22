import React from 'react';
import { ClassNames, DefaultProps } from '@mantine/styles';
import { TextInputProps, TextInputStylesNames } from '../TextInput';
import useStyles from './PasswordInput.styles';
export declare type PasswordInputStylesNames = ClassNames<typeof useStyles> | TextInputStylesNames;
export interface PasswordInputProps extends DefaultProps<PasswordInputStylesNames>, Omit<TextInputProps, 'classNames' | 'styles'> {
    /** Toggle button tabIndex, set to 0 to make button focusable with tab key */
    toggleTabIndex?: -1 | 0;
    /** Provide your own visibility toggle icon */
    visibilityToggleIcon?: React.FC<{
        reveal: boolean;
        size: number;
    }>;
}
export declare const PasswordInput: React.ForwardRefExoticComponent<PasswordInputProps & React.RefAttributes<HTMLInputElement>>;
//# sourceMappingURL=PasswordInput.d.ts.map