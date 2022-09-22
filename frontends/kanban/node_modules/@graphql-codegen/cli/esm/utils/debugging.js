import { getLogger } from './logger.js';
let queue = [];
export function debugLog(message, ...meta) {
    queue.push({
        message,
        meta,
    });
}
export function printLogs() {
    queue.forEach(log => {
        getLogger().info(log.message, ...log.meta);
    });
    resetLogs();
}
export function resetLogs() {
    queue = [];
}
