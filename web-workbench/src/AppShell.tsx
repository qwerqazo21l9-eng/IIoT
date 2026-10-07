import {
  AlertTriangle,
  BarChart3,
  Check,
  ChevronDown,
  ClipboardCheck,
  FlaskConical,
  Info,
  Menu,
  X,
} from "lucide-react";
import {
  KeyboardEvent,
  ReactNode,
  RefObject,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import { useWorkbenchRole } from "./RoleContext";
import { WORKBENCH_ROLES, WorkbenchRoleId } from "./roles";

export type WorkbenchRouteId = "line-overview" | "approval-queue" | "improvement-experiment";

export type NavigationItem = {
  id: WorkbenchRouteId;
  label: string;
  description: string;
};

const navigationItems: NavigationItem[] = [
  { id: "line-overview", label: "产线概览", description: "指标、工位状态、候选诊断" },
  { id: "approval-queue", label: "改善审批", description: "提议审批与审批结果" },
  { id: "improvement-experiment", label: "改善实验", description: "配对实验、进度、收益评价" },
];

const routeIcons = {
  "line-overview": BarChart3,
  "approval-queue": ClipboardCheck,
  "improvement-experiment": FlaskConical,
};

type AppShellProps = {
  activeRoute: WorkbenchRouteId;
  onNavigate: (route: WorkbenchRouteId) => void;
  title: string;
  actionBar?: ReactNode;
  children: ReactNode;
};

export function AppShell({ activeRoute, onNavigate, title, actionBar, children }: AppShellProps) {
  const [isNarrowMenuOpen, setIsNarrowMenuOpen] = useState(false);
  const { draftScopeKey } = useWorkbenchRole();
  const navigationTriggerRef = useRef<HTMLButtonElement>(null);
  const shellRef = useRef<HTMLDivElement>(null);
  const headerRef = useRef<HTMLElement>(null);

  useLayoutEffect(() => {
    if (!headerRef.current || typeof ResizeObserver === "undefined") return;
    const updateHeaderHeight = () => {
      const height = headerRef.current?.offsetHeight;
      if (height) shellRef.current?.style.setProperty("--header-height", `${height}px`);
    };
    updateHeaderHeight();
    const observer = new ResizeObserver(updateHeaderHeight);
    observer.observe(headerRef.current);
    return () => observer.disconnect();
  }, []);

  function closeNavigation() {
    setIsNarrowMenuOpen(false);
    navigationTriggerRef.current?.focus();
  }

  useEffect(() => {
    setIsNarrowMenuOpen(false);
  }, [activeRoute]);

  return (
    <div ref={shellRef} className="workbench-shell" onKeyDown={(event) => {
      if (event.key === "Escape" && isNarrowMenuOpen && !event.defaultPrevented) {
        event.preventDefault();
        closeNavigation();
      }
    }}>
      <header ref={headerRef} className="topbar">
        <div className="topbar-left">
          <button
            ref={navigationTriggerRef}
            className="icon-button narrow-menu-trigger"
            type="button"
            aria-label={isNarrowMenuOpen ? "关闭主导航" : "打开主导航"}
            aria-expanded={isNarrowMenuOpen}
            aria-controls="primary-navigation"
            onClick={() => setIsNarrowMenuOpen((open) => !open)}
          >
            {isNarrowMenuOpen ? <X aria-hidden="true" /> : <Menu aria-hidden="true" />}
          </button>
          <span className="product-title">产线诊断工作台</span>
        </div>
        <RoleMenu />
      </header>
      <div className="shell-body">
        <aside
          id="primary-navigation"
          className={`sidebar ${isNarrowMenuOpen ? "is-open" : ""}`}
          aria-label="主导航"
        >
          <PrimaryNavigation activeRoute={activeRoute} onNavigate={(route) => {
            onNavigate(route);
            if (isNarrowMenuOpen) closeNavigation();
          }} />
        </aside>
        {isNarrowMenuOpen ? (
          <button
            className="nav-scrim"
            type="button"
            aria-label="关闭主导航"
            onClick={closeNavigation}
          />
        ) : null}
        <main className="content-area">
          <div className="page-header">
            <div>
              <h1>{title}</h1>
            </div>
            {actionBar ? <div className="action-bar">{actionBar}</div> : null}
          </div>
          <div key={draftScopeKey}>{children}</div>
        </main>
      </div>
    </div>
  );
}

function PrimaryNavigation({
  activeRoute,
  onNavigate,
}: {
  activeRoute: WorkbenchRouteId;
  onNavigate: (route: WorkbenchRouteId) => void;
}) {
  return (
    <nav>
      <ul className="nav-list">
        {navigationItems.map((item) => {
          const Icon = routeIcons[item.id];
          return (
            <li key={item.id}>
              <button
                type="button"
                className="nav-item"
                aria-current={item.id === activeRoute ? "page" : undefined}
                onClick={() => onNavigate(item.id)}
              >
                <Icon aria-hidden="true" />
                <span>{item.label}</span>
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}

function RoleMenu() {
  const { role, switchRole } = useWorkbenchRole();
  const [isOpen, setIsOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const itemRefs = useRef<Array<HTMLButtonElement | null>>([]);

  useEffect(() => {
    if (isOpen) {
      const selectedIndex = WORKBENCH_ROLES.findIndex((candidate) => candidate.id === role.id);
      itemRefs.current[selectedIndex]?.focus();
    }
  }, [isOpen, role.id]);

  useEffect(() => {
    if (!isOpen) return;
    function dismissOutside(event: PointerEvent) {
      if (event.target instanceof Node && !menuRef.current?.contains(event.target)) {
        closeAndReturnFocus(triggerRef);
      }
    }
    document.addEventListener("pointerdown", dismissOutside);
    return () => document.removeEventListener("pointerdown", dismissOutside);
  }, [isOpen]);

  function closeAndReturnFocus(ref: RefObject<HTMLButtonElement>) {
    setIsOpen(false);
    ref.current?.focus();
  }

  function onMenuKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    const selectedIndex = itemRefs.current.findIndex((item) => item === document.activeElement);
    if (event.key === "Escape") {
      event.preventDefault();
      closeAndReturnFocus(triggerRef);
    }
    if (event.key === "ArrowDown") {
      event.preventDefault();
      const nextIndex = selectedIndex >= 0 ? (selectedIndex + 1) % WORKBENCH_ROLES.length : 0;
      itemRefs.current[nextIndex]?.focus();
    }
    if (event.key === "ArrowUp") {
      event.preventDefault();
      const nextIndex =
        selectedIndex >= 0
          ? (selectedIndex - 1 + WORKBENCH_ROLES.length) % WORKBENCH_ROLES.length
          : WORKBENCH_ROLES.length - 1;
      itemRefs.current[nextIndex]?.focus();
    }
  }

  return (
    <div ref={menuRef} className="role-menu">
      <button
        ref={triggerRef}
        className="role-trigger"
        type="button"
        aria-haspopup="menu"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((open) => !open)}
      >
        <span>{role.label}</span>
        <ChevronDown aria-hidden="true" />
      </button>
      {isOpen ? (
        <div className="role-popover" role="menu" aria-label="切换演示身份" onKeyDown={onMenuKeyDown}>
          {WORKBENCH_ROLES.map((candidate, index) => (
            <button
              key={candidate.id}
              ref={(item) => {
                itemRefs.current[index] = item;
              }}
              className="role-option"
              role="menuitemradio"
              aria-checked={candidate.id === role.id}
              type="button"
              onClick={() => {
                switchRole(candidate.id as WorkbenchRoleId);
                closeAndReturnFocus(triggerRef);
              }}
            >
              <span className="role-check">{candidate.id === role.id ? <Check aria-hidden="true" /> : null}</span>
              <span>
                <strong>{candidate.label}</strong>
                <small>{candidate.description}</small>
              </span>
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}

export type OutletMode = "default" | "loading" | "error" | "readonly";

export function ShellStatusOutlet({
  mode = "default", children, onRetry, permissionReason,
}: {
  mode?: OutletMode;
  children?: ReactNode;
  onRetry?: () => void;
  permissionReason?: string;
}) {
  const { role } = useWorkbenchRole();
  return (
    <section aria-label="页面内容" aria-busy={mode === "loading"}>
      {mode === "readonly" || role.id === "readonly" ? <ReadOnlyNotice reason={permissionReason} /> : null}
      {children}
      {mode === "loading" ? children ? <p role="status">正在刷新，保留当前内容。</p> : <SkeletonRegion /> : null}
      {mode === "error" ? <LocalError onRetry={onRetry} hasPreviousContent={Boolean(children)} /> : null}
    </section>
  );
}

function SkeletonRegion() {
  return (
    <div className="skeleton-region" aria-busy="true" aria-label="内容正在加载">
      <span className="skeleton title" aria-hidden="true" />
      <span className="skeleton line" aria-hidden="true" />
      <span className="skeleton line short" aria-hidden="true" />
      <span className="skeleton row" aria-hidden="true" />
    </div>
  );
}

function LocalError({ onRetry, hasPreviousContent }: { onRetry?: () => void; hasPreviousContent: boolean }) {
  return (
    <div className="local-error" role="alert">
      <AlertTriangle aria-hidden="true" />
      <div>
        <strong>内容区加载失败</strong>
        <p>{hasPreviousContent ? "当前内容未更新。" : "暂时无法获取内容。"}</p>
      </div>
      {onRetry ? <button type="button" className="button secondary" onClick={onRetry}>
        重试
      </button> : null}
    </div>
  );
}

function ReadOnlyNotice({ reason }: { reason?: string }) {
  return (
    <div className="readonly-notice">
      <Info aria-hidden="true" />
      <p>{reason ?? "当前演示身份无写权限。可继续查看内容。"}</p>
    </div>
  );
}
