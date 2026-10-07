import { expect, test } from "@playwright/test";

for (const width of [320, 768, 1440]) {
  test(`shell fits ${width}px with accessible navigation and no overlap`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await page.goto("/");
    await expect(page.getByRole("heading", { name: "产线概览", exact: true })).toBeVisible();
    const header = await page.getByRole("banner").boundingBox();
    const content = await page.getByRole("main").boundingBox();
    expect(content!.y).toBeGreaterThanOrEqual(header!.y + header!.height);
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(width);
    if (width < 768) {
      await expect(page.getByRole("button", { name: "改善审批", exact: true })).not.toBeVisible();
      const trigger = page.getByRole("button", { name: "打开主导航" });
      await trigger.focus();
      await page.keyboard.press("Enter");
      await expect(page.getByRole("button", { name: "改善审批", exact: true })).toBeVisible();
      await page.screenshot({ path: `../docs/evidence/issue-3/narrow-menu-${width}.png`, fullPage: true });
      await page.keyboard.press("Escape");
      await expect(trigger).toBeFocused();
      await expect(page.getByRole("button", { name: "改善审批", exact: true })).not.toBeVisible();
    }
    await page.screenshot({ path: `../docs/evidence/issue-3/shell-${width}.png`, fullPage: true });
  });
}

test("navigation restores browser history and survives refresh and repeated selection", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "改善审批", exact: true }).focus();
  await page.keyboard.press("Enter");
  await expect(page).toHaveURL(/#\/approval-queue$/);
  await page.getByRole("button", { name: "改善审批", exact: true }).click();
  await page.getByRole("button", { name: "改善实验", exact: true }).click();
  await page.goBack();
  await expect(page.getByRole("heading", { name: "改善审批", exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("button", { name: "改善审批", exact: true })).toHaveAttribute("aria-current", "page");
  await page.goBack();
  await expect(page.getByRole("heading", { name: "产线概览", exact: true })).toBeVisible();
});

test("identity menu works by keyboard on narrow screens and persists on refresh", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 900 });
  await page.goto("/#/approval-queue");
  const trigger = page.getByRole("button", { name: "工程师演示身份", exact: true });
  await trigger.focus();
  await page.keyboard.press("Enter");
  await page.keyboard.press("ArrowDown");
  await page.screenshot({ path: "../docs/evidence/issue-3/role-menu-320.png", fullPage: true });
  await page.keyboard.press("Enter");
  await expect(page.getByRole("button", { name: "管理者演示身份", exact: true })).toBeFocused();
  await page.reload();
  await expect(page.getByRole("button", { name: "管理者演示身份", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "管理者演示身份", exact: true }).click();
  await page.keyboard.press("ArrowDown");
  await page.keyboard.press("Enter");
  await expect(page.getByText("当前演示身份无写权限。可继续查看内容。")).toBeVisible();
  await page.getByRole("button", { name: "打开主导航" }).click();
  await page.getByRole("button", { name: "改善实验", exact: true }).click();
  await expect(page.getByRole("heading", { name: "改善实验", exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "打开主导航" })).toBeFocused();
});

test("shell remains readable at 200 percent content zoom", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/");
  await page.evaluate(() => { document.documentElement.style.zoom = "2"; });
  await expect(page.getByRole("heading", { name: "产线概览", exact: true })).toBeVisible();
  await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(1440);
  await expect.poll(async () => {
    const header = await page.getByRole("banner").boundingBox();
    const main = await page.getByRole("main").boundingBox();
    return Math.abs(main!.y - header!.height);
  }).toBeLessThanOrEqual(1);
  await page.screenshot({ path: "../docs/evidence/issue-3/zoom-200.png", fullPage: true });
});
