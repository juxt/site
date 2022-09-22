/// <reference types="react" />
import { MantineSize } from '@mantine/styles';
export interface SelectRightSectionProps {
    shouldClear: boolean;
    clearButtonLabel?: string;
    onClear?: () => void;
    size: MantineSize;
    error?: any;
    disabled?: boolean;
}
export declare function SelectRightSection({ shouldClear, clearButtonLabel, onClear, size, error, }: SelectRightSectionProps): JSX.Element;
export declare namespace SelectRightSection {
    var displayName: string;
}
//# sourceMappingURL=SelectRightSection.d.ts.map