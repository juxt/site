import React, { useState, useRef } from 'react';
import { createPortal } from 'react-dom';
import { useIsomorphicEffect } from '@mantine/hooks';
import { useMantineTheme } from '@mantine/styles';

function Portal({
  children,
  zIndex = 1,
  position = "relative",
  target,
  className
}) {
  const theme = useMantineTheme();
  const [mounted, setMounted] = useState(false);
  const ref = useRef();
  useIsomorphicEffect(() => {
    setMounted(true);
    ref.current = !target ? document.createElement("div") : typeof target === "string" ? document.querySelector(target) : target;
    if (!target) {
      document.body.appendChild(ref.current);
    }
    return () => {
      !target && document.body.removeChild(ref.current);
    };
  }, [target]);
  if (!mounted) {
    return null;
  }
  return createPortal(/* @__PURE__ */ React.createElement("div", {
    className,
    dir: theme.dir,
    style: { position, zIndex }
  }, children), ref.current);
}
Portal.displayName = "@mantine/core/Portal";

export { Portal };
//# sourceMappingURL=Portal.js.map
