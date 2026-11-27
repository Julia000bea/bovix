import { useSyncExternalStore } from "react";
import { assinarEstadoOffline, lerEstadoOffline } from "../services/offline";

/** Estado do modo offline: online?, lançamentos pendentes, falhas, dados guardados... */
export function useOffline() {
  return useSyncExternalStore(assinarEstadoOffline, lerEstadoOffline, lerEstadoOffline);
}
