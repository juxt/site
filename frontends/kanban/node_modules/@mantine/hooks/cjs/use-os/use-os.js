'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

function getOS() {
  const { userAgent } = window.navigator;
  const { platform } = window.navigator;
  const macosPlatforms = ["Macintosh", "MacIntel", "MacPPC", "Mac68K"];
  const windowsPlatforms = ["Win32", "Win64", "Windows", "WinCE"];
  const iosPlatforms = ["iPhone", "iPad", "iPod"];
  let os = "undetermined";
  if (macosPlatforms.indexOf(platform) !== -1) {
    os = "macos";
  } else if (iosPlatforms.indexOf(platform) !== -1) {
    os = "ios";
  } else if (windowsPlatforms.indexOf(platform) !== -1) {
    os = "windows";
  } else if (/Android/.test(userAgent)) {
    os = "android";
  } else if (/Linux/.test(platform)) {
    os = "linux";
  }
  return os;
}
function useOs() {
  if (typeof window !== "undefined") {
    return getOS();
  }
  return "undetermined";
}

exports.useOs = useOs;
//# sourceMappingURL=use-os.js.map
