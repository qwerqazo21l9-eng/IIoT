export type WorkbenchRoleId = "engineer" | "manager" | "readonly";

export type WorkbenchRole = {
  id: WorkbenchRoleId;
  label: string;
  subjectId: string;
  description: string;
  canSubmitProposal: boolean;
  canApproveProposal: boolean;
  canStartExperiment: boolean;
};

export const WORKBENCH_ROLES: WorkbenchRole[] = [
  {
    id: "engineer",
    label: "工程师演示身份",
    subjectId: "demo-engineer",
    description: "可提交改善提议并启动已批准实验；审批需要管理者身份。",
    canSubmitProposal: true,
    canApproveProposal: false,
    canStartExperiment: true,
  },
  {
    id: "manager",
    label: "管理者演示身份",
    subjectId: "demo-manager",
    description: "可审批改善提议并查看实验结果，不代表正式生产认证。",
    canSubmitProposal: false,
    canApproveProposal: true,
    canStartExperiment: false,
  },
  {
    id: "readonly",
    label: "只读观察身份",
    subjectId: "demo-readonly",
    description: "可查看内容，不能提交提议、审批或启动实验。",
    canSubmitProposal: false,
    canApproveProposal: false,
    canStartExperiment: false,
  },
];

export function findWorkbenchRole(roleId: WorkbenchRoleId): WorkbenchRole {
  return WORKBENCH_ROLES.find((role) => role.id === roleId) ?? WORKBENCH_ROLES[0];
}
