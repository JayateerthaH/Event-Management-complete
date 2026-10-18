import api from "./apiClient";

/** Phone notifications (Web Push) for this browser/device. */
const urlBase64ToUint8Array = (b64) => {
  const raw = atob((b64 + "=".repeat((4 - (b64.length % 4)) % 4)).replace(/-/g, "+").replace(/_/g, "/"));
  return Uint8Array.from(raw, (c) => c.charCodeAt(0));
};

export const pushSupported = () =>
  typeof window !== "undefined" && "serviceWorker" in navigator && "PushManager" in window && "Notification" in window;

/** iPhone/iPad only allow web push from the app added to the Home Screen. */
export const needsHomeScreenInstall = () =>
  /iPad|iPhone|iPod/.test(navigator.userAgent) && !(window.matchMedia?.("(display-mode: standalone)").matches || navigator.standalone);

export const getPushKey = () => api.get("/public/push/key").then((r) => r.data);

const registration = async () => (await navigator.serviceWorker.getRegistration()) || navigator.serviceWorker.register("/sw.js");

export const currentSubscription = async () => {
  if (!pushSupported()) return null;
  const reg = await navigator.serviceWorker.getRegistration();
  return reg ? reg.pushManager.getSubscription() : null;
};

/** Asks permission, subscribes this device and tells the server. Throws with a readable message. */
export const enablePush = async () => {
  const { enabled, publicKey } = await getPushKey();
  if (!enabled) throw new Error("Phone notifications aren't set up on the server yet.");
  const permission = await Notification.requestPermission();
  if (permission !== "granted") throw new Error("Notifications are blocked. Allow them for this site in your browser settings.");
  await registration();
  const reg = await navigator.serviceWorker.ready;
  const sub = (await reg.pushManager.getSubscription())
    || (await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: urlBase64ToUint8Array(publicKey) }));
  await api.post("/social/push/subscription", sub.toJSON());
  return sub;
};

export const disablePush = async () => {
  const sub = await currentSubscription();
  if (!sub) return;
  await api.delete("/social/push/subscription", { data: { endpoint: sub.endpoint } }).catch(() => {});
  await sub.unsubscribe();
};

export const sendTestPush = () => api.post("/social/push/test").then((r) => r.data);
