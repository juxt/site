import { MantineColor, MantineNumberSize } from '@mantine/styles';
interface NotificationStyles {
    color: MantineColor;
    radius: MantineNumberSize;
    disallowClose: boolean;
}
declare const _default: (params: NotificationStyles, options?: import("@mantine/styles").UseStylesOptions<"icon" | "body" | "title" | "withIcon" | "root" | "loader" | "closeButton" | "description">) => {
    classes: Record<"icon" | "body" | "title" | "withIcon" | "root" | "loader" | "closeButton" | "description", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Notification.styles.d.ts.map