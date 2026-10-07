import { useEffect, useState } from "react";
import { Database } from "lucide-react";
import { AppShell, ShellStatusOutlet, WorkbenchRouteId } from "./AppShell";
import { RoleProvider, useWorkbenchRole } from "./RoleContext";

const routeTitles: Record<WorkbenchRouteId, string> = {
  "line-overview": "产线概览",
  "approval-queue": "改善审批",
  "improvement-experiment": "改善实验",
};

export function WorkbenchApp() {
  const [activeRoute, setActiveRoute] = useState<WorkbenchRouteId>(readRoute);

  useEffect(() => {
    const restoreRoute = () => setActiveRoute(readRoute());
    window.addEventListener("hashchange", restoreRoute);
    window.addEventListener("popstate", restoreRoute);
    return () => {
      window.removeEventListener("hashchange", restoreRoute);
      window.removeEventListener("popstate", restoreRoute);
    };
  }, []);

  function navigate(route: WorkbenchRouteId) {
    if (route === activeRoute) return;
    window.history.pushState(null, "", `#/${route}`);
    setActiveRoute(route);
  }

  return (
    <RoleProvider>
      <AppShell
        activeRoute={activeRoute}
        onNavigate={navigate}
        title={routeTitles[activeRoute]}
      >
        <PageOutlet route={activeRoute} />
      </AppShell>
    </RoleProvider>
  );
}

function readRoute(): WorkbenchRouteId {
  const route = window.location.hash.slice(2).split("?")[0];
  return Object.hasOwn(routeTitles, route) ? (route as WorkbenchRouteId) : "line-overview";
}

function PageOutlet({ route }: { route: WorkbenchRouteId }) {
  const { role } = useWorkbenchRole();
  const restricted = role.id === "readonly" ||
    (route === "approval-queue" && !role.canApproveProposal) ||
    (route === "improvement-experiment" && !role.canStartExperiment);
  const reason = role.id === "readonly" ? undefined : route === "approval-queue"
    ? "当前演示身份可查阅审批结果；审批操作需要管理者身份。"
    : "当前演示身份可查看实验结果；启动操作需要工程师身份。";
  return (
    <ShellStatusOutlet mode={restricted ? "readonly" : "default"} permissionReason={reason}>
      <div className="empty-outlet">
        <Database aria-hidden="true" />
        <h2>业务页面尚未接入</h2>
        <p>暂无可展示的{routeTitles[route]}数据。</p>
      </div>
    </ShellStatusOutlet>
  );
}
