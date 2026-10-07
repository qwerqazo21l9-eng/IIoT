import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { WorkbenchApp } from "./WorkbenchApp";
import { useState } from "react";
import { AppShell, ShellStatusOutlet, OutletMode } from "./AppShell";
import { RoleProvider, useWorkbenchRole } from "./RoleContext";

afterEach(() => {
  vi.restoreAllMocks();
  window.history.replaceState(null, "", "/");
});

function DraftPage() {
  const [draft, setDraft] = useState("");
  const { requestIdentityHeaders } = useWorkbenchRole();
  return <>
    <label>未提交操作上下文<textarea value={draft} onChange={(event) => setDraft(event.target.value)} /></label>
    <output aria-label="业务请求身份">{JSON.stringify(requestIdentityHeaders)}</output>
  </>;
}

function OutletFixture({ mode = "default" }: { mode?: OutletMode }) {
  return <RoleProvider>
    <AppShell activeRoute="line-overview" onNavigate={() => {}} title="产线概览">
      <ShellStatusOutlet mode={mode}><DraftPage /></ShellStatusOutlet>
    </AppShell>
  </RoleProvider>;
}

describe("WorkbenchApp app-shell", () => {
  it("restores the current navigation entry on a direct visit or refresh", () => {
    window.history.replaceState(null, "", "/#/approval-queue");
    const view = render(<WorkbenchApp />);
    expect(screen.getByRole("heading", { name: "改善审批" })).toBeInTheDocument();
    view.unmount();
    render(<WorkbenchApp />);
    expect(screen.getByRole("button", { name: "改善审批" })).toHaveAttribute("aria-current", "page");
  });
  it("renders the PRD navigation order and current outlet", () => {
    render(<WorkbenchApp />);

    const navigation = screen.getByLabelText("主导航");
    const links = within(navigation).getAllByRole("button");

    expect(links.map((link) => link.textContent)).toEqual(["产线概览", "改善审批", "改善实验"]);
    expect(screen.getByRole("heading", { name: "产线概览" })).toBeInTheDocument();
    expect(screen.getByRole("region", { name: "页面内容" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "产线概览" })).toHaveAttribute("aria-current", "page");
  });

  it("navigates by keyboard-operable buttons without losing the shell", async () => {
    const user = userEvent.setup();
    render(<WorkbenchApp />);

    await user.click(screen.getByRole("button", { name: "改善审批" }));

    expect(screen.getByRole("heading", { name: "改善审批" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "工程师演示身份" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "改善审批" })).toHaveAttribute("aria-current", "page");
  });

  it("switches role, returns focus, and clears unsubmitted context", async () => {
    const user = userEvent.setup();
    render(<OutletFixture />);

    await user.type(screen.getByLabelText("未提交操作上下文"), "pending text");
    await user.click(screen.getByRole("button", { name: /工程师演示身份/ }));
    await user.click(screen.getByRole("menuitemradio", { name: /管理者演示身份/ }));

    expect(screen.getByRole("button", { name: /管理者演示身份/ })).toHaveFocus();
    expect(screen.getByLabelText("业务请求身份")).toHaveTextContent('"X-Demo-Subject":"demo-manager"');
    expect(screen.getByLabelText("业务请求身份")).toHaveTextContent('"X-Demo-Role":"manager"');
    expect(screen.getByLabelText("未提交操作上下文")).toHaveValue("");
  });

  it("keeps the draft when the current identity is selected again", async () => {
    const user = userEvent.setup();
    render(<OutletFixture />);
    await user.type(screen.getByLabelText("未提交操作上下文"), "pending text");
    await user.click(screen.getByRole("button", { name: /工程师演示身份/ }));
    await user.keyboard("{Enter}");
    expect(screen.getByLabelText("未提交操作上下文")).toHaveValue("pending text");
  });

  it("shows read-only content and a permission reason for a read-only identity", async () => {
    const user = userEvent.setup();
    render(<WorkbenchApp />);
    await user.click(screen.getByRole("button", { name: /工程师演示身份/ }));
    await user.click(screen.getByRole("menuitemradio", { name: /只读观察身份/ }));
    expect(screen.getByText(/当前演示身份无写权限/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "改善审批" })).toBeEnabled();
  });

  it("preserves the selected demonstration identity on refresh", async () => {
    const user = userEvent.setup();
    const view = render(<WorkbenchApp />);
    await user.click(screen.getByRole("button", { name: /工程师演示身份/ }));
    await user.keyboard("{ArrowDown}{Enter}");
    expect(screen.getByRole("button", { name: "管理者演示身份" })).toBeInTheDocument();
    view.unmount();
    render(<WorkbenchApp />);
    expect(screen.getByRole("button", { name: "管理者演示身份" })).toBeInTheDocument();
  });

  it("dismisses the identity menu when the user clicks outside it", async () => {
    const user = userEvent.setup();
    render(<WorkbenchApp />);
    await user.click(screen.getByRole("button", { name: "工程师演示身份" }));
    expect(screen.getByRole("menu", { name: "切换演示身份" })).toBeInTheDocument();
    await user.click(screen.getByRole("heading", { name: "产线概览" }));
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("keeps navigation available while the initial content is loading", () => {
    render(<RoleProvider><AppShell activeRoute="line-overview" onNavigate={() => {}} title="产线概览">
      <ShellStatusOutlet mode="loading" />
    </AppShell></RoleProvider>);
    expect(screen.getByLabelText("内容正在加载")).toHaveAttribute("aria-busy", "true");
    expect(screen.getByRole("button", { name: "产线概览" })).toBeEnabled();
  });

  it("keeps previous content and navigation when a local request fails", () => {
    render(<OutletFixture mode="error" />);
    expect(screen.getByRole("alert")).toHaveTextContent("内容区加载失败");
    expect(screen.getByRole("alert")).toHaveTextContent("当前内容未更新");
    expect(screen.getByLabelText("未提交操作上下文")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "产线概览" })).toBeEnabled();
  });

  it("preserves existing input through refresh and local failure", async () => {
    const user = userEvent.setup();
    const view = render(<OutletFixture />);
    await user.type(screen.getByLabelText("未提交操作上下文"), "saved input");
    view.rerender(<OutletFixture mode="loading" />);
    expect(screen.getByRole("region", { name: "页面内容" })).toHaveAttribute("aria-busy", "true");
    expect(screen.getByLabelText("未提交操作上下文")).toHaveValue("saved input");
    view.rerender(<OutletFixture mode="error" />);
    expect(screen.getByRole("alert")).toHaveTextContent("当前内容未更新");
    expect(screen.getByLabelText("未提交操作上下文")).toHaveValue("saved input");
  });

  it("retries a failed region without removing other available content", async () => {
    const user = userEvent.setup();
    function RetryPage() {
      const [failed, setFailed] = useState(true);
      return <RoleProvider><AppShell activeRoute="line-overview" onNavigate={() => {}} title="产线概览">
        <ShellStatusOutlet mode={failed ? "error" : "default"} onRetry={() => setFailed(false)}>
          <p>已有查询结果</p>
        </ShellStatusOutlet>
      </AppShell></RoleProvider>;
    }
    render(<RetryPage />);
    await user.click(screen.getByRole("button", { name: "重试" }));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.getByText("已有查询结果")).toBeInTheDocument();
  });

  it("opens and closes the narrow navigation menu", async () => {
    const user = userEvent.setup();
    render(<WorkbenchApp />);

    const trigger = screen.getByRole("button", { name: "打开主导航" });
    await user.click(trigger);
    expect(trigger).toHaveAttribute("aria-expanded", "true");

    await user.click(trigger);
    expect(trigger).toHaveAttribute("aria-expanded", "false");
  });

  it("closes narrow navigation with Escape and returns focus to its trigger", async () => {
    const user = userEvent.setup();
    render(<WorkbenchApp />);
    const trigger = screen.getByRole("button", { name: "打开主导航" });
    await user.click(trigger);
    await user.tab();
    await user.keyboard("{Escape}");
    expect(trigger).toHaveAttribute("aria-expanded", "false");
    expect(trigger).toHaveFocus();
  });
});
