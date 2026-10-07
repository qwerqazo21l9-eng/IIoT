import { createContext, ReactNode, useContext, useEffect, useMemo, useState } from "react";
import { findWorkbenchRole, WORKBENCH_ROLES, WorkbenchRole, WorkbenchRoleId } from "./roles";

type RoleContextValue = {
  role: WorkbenchRole;
  draftScopeKey: number;
  requestIdentityHeaders: Record<string, string>;
  switchRole: (nextRoleId: WorkbenchRoleId) => void;
};

const RoleContext = createContext<RoleContextValue | null>(null);

export function RoleProvider({ children }: { children: ReactNode }) {
  const [roleId, setRoleId] = useState<WorkbenchRoleId>(() => {
    try {
      const saved = window.sessionStorage.getItem("iiot.demo-role");
      return WORKBENCH_ROLES.find((candidate) => candidate.id === saved)?.id ?? "engineer";
    } catch {
      return "engineer";
    }
  });
  const [draftScopeKey, setDraftScopeKey] = useState(0);
  const role = findWorkbenchRole(roleId);

  useEffect(() => {
    try {
      window.sessionStorage.setItem("iiot.demo-role", roleId);
    } catch {
      // The workbench remains usable when browser storage is unavailable.
    }
  }, [roleId]);

  const value = useMemo<RoleContextValue>(
    () => ({
      role,
      draftScopeKey,
      requestIdentityHeaders: {
        "X-Demo-Subject": role.subjectId,
        "X-Demo-Role": role.id,
      },
      switchRole(nextRoleId) {
        if (nextRoleId === role.id) return;
        setRoleId(nextRoleId);
        setDraftScopeKey((current) => current + 1);
      },
    }),
    [draftScopeKey, role],
  );

  return <RoleContext.Provider value={value}>{children}</RoleContext.Provider>;
}

export function useWorkbenchRole() {
  const value = useContext(RoleContext);
  if (!value) {
    throw new Error("useWorkbenchRole must be used within RoleProvider");
  }
  return value;
}
