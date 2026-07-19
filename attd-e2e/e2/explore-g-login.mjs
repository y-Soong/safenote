// QTUSERG 로그인 가능 여부 실측(ACCOUNT_STATUS=04 인증대기 상태).
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERG", "QtTest!2026");
    console.log("URL:", page.url());
    const text = await page.evaluate(() => document.body.innerText);
    console.log(text.slice(0, 800));
    await page.screenshot({ path: shotPath("QE-2-x", "app", "g-login-probe"), fullPage: true });
  } catch (e) {
    console.log("LOGIN-FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
