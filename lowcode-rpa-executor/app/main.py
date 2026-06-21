import json
import asyncio
import traceback
from typing import Dict, Any, List, Optional
from datetime import datetime
from pydantic import BaseModel
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from playwright.async_api import async_playwright, Browser, Page, BrowserContext

app = FastAPI(title="RPA Executor Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class ExecuteRequest(BaseModel):
    script: str
    params: Optional[Dict[str, Any]] = None
    targetUrl: Optional[str] = None


class ValidateRequest(BaseModel):
    script: str


@app.get("/health")
async def health_check():
    return {"status": "UP", "timestamp": datetime.now().isoformat()}


@app.post("/api/validate")
async def validate_script(request: ValidateRequest):
    try:
        script_data = json.loads(request.script)
        steps = script_data.get("steps", [])
        if not isinstance(steps, list):
            return {"success": False, "message": "脚本格式错误: steps必须是数组"}

        for i, step in enumerate(steps):
            action = step.get("action")
            if not action:
                return {"success": False, "message": f"步骤{i + 1}缺少action字段"}

            valid_actions = ["navigate", "click", "input", "select", "scroll", "wait",
                             "extract", "screenshot", "hover", "press", "check", "uncheck"]
            if action not in valid_actions:
                return {"success": False, "message": f"步骤{i + 1}存在无效的action: {action}"}

        return {"success": True, "message": "脚本验证通过", "data": {"stepCount": len(steps)}}
    except json.JSONDecodeError as e:
        return {"success": False, "message": f"脚本JSON格式错误: {str(e)}"}
    except Exception as e:
        return {"success": False, "message": f"验证失败: {str(e)}"}


@app.post("/api/execute")
async def execute_script(request: ExecuteRequest):
    try:
        script_data = json.loads(request.script)
        steps = script_data.get("steps", [])
        params = request.params or {}

        result = await run_playwright_script(steps, params, request.targetUrl)

        return {
            "success": True,
            "message": "执行成功",
            "data": result
        }
    except json.JSONDecodeError as e:
        raise HTTPException(status_code=400, detail=f"脚本JSON格式错误: {str(e)}")
    except Exception as e:
        traceback.print_exc()
        return {
            "success": False,
            "message": f"执行失败: {str(e)}",
            "data": None
        }


async def run_playwright_script(steps: List[Dict], params: Dict, target_url: Optional[str]) -> Dict:
    results = {
        "extractedData": {},
        "screenshots": [],
        "logs": [],
        "stepResults": []
    }

    async with async_playwright() as p:
        browser: Browser = await p.chromium.launch(
            headless=True,
            args=[
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080"
            ]
        )

        context: BrowserContext = await browser.new_context(
            viewport={"width": 1920, "height": 1080},
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )

        page: Page = await context.new_page()

        try:
            if target_url:
                log_step(results, f"导航到目标URL: {target_url}")
                await page.goto(target_url, wait_until="domcontentloaded", timeout=60000)

            for i, step in enumerate(steps):
                step_result = await execute_step(page, step, params, results, i)
                results["stepResults"].append(step_result)

                if not step_result.get("success"):
                    raise Exception(f"步骤{i + 1}执行失败: {step_result.get('error')}")

            return results

        finally:
            await context.close()
            await browser.close()


async def execute_step(page: Page, step: Dict, params: Dict, results: Dict, step_index: int) -> Dict:
    action = step.get("action")
    selector = step.get("selector")
    value = step.get("value", "")
    wait_time = step.get("waitTime", 0)
    timeout = step.get("timeout", 30000)

    log_step(results, f"执行步骤{step_index + 1}: {action} {selector if selector else ''}")

    try:
        if isinstance(value, str):
            for key, val in params.items():
                placeholder = f"${{{key}}}"
                if placeholder in value:
                    value = value.replace(placeholder, str(val))

        if action == "navigate":
            url = step.get("url", value)
            await page.goto(url, wait_until="domcontentloaded", timeout=timeout)
            return {"success": True, "action": action, "url": url}

        elif action == "click":
            await page.wait_for_selector(selector, timeout=timeout)
            await page.click(selector, timeout=timeout)
            return {"success": True, "action": action, "selector": selector}

        elif action == "input":
            await page.wait_for_selector(selector, timeout=timeout)
            await page.fill(selector, str(value), timeout=timeout)
            return {"success": True, "action": action, "selector": selector, "value": value}

        elif action == "select":
            await page.wait_for_selector(selector, timeout=timeout)
            await page.select_option(selector, value, timeout=timeout)
            return {"success": True, "action": action, "selector": selector, "value": value}

        elif action == "scroll":
            direction = step.get("direction", "down")
            pixels = step.get("pixels", 500)
            if direction == "down":
                await page.evaluate(f"window.scrollBy(0, {pixels})")
            elif direction == "up":
                await page.evaluate(f"window.scrollBy(0, -{pixels})")
            elif direction == "right":
                await page.evaluate(f"window.scrollBy({pixels}, 0)")
            elif direction == "left":
                await page.evaluate(f"window.scrollBy(-{pixels}, 0)")
            return {"success": True, "action": action, "direction": direction}

        elif action == "wait":
            seconds = step.get("seconds", 2)
            await asyncio.sleep(seconds)
            return {"success": True, "action": action, "waited": seconds}

        elif action == "extract":
            await page.wait_for_selector(selector, timeout=timeout)
            extract_type = step.get("extractType", "text")
            field_name = step.get("fieldName", f"field_{step_index}")

            if extract_type == "text":
                extracted = await page.inner_text(selector, timeout=timeout)
            elif extract_type == "value":
                extracted = await page.input_value(selector, timeout=timeout)
            elif extract_type == "attribute":
                attr = step.get("attribute", "href")
                extracted = await page.get_attribute(selector, attr, timeout=timeout)
            elif extract_type == "html":
                extracted = await page.inner_html(selector, timeout=timeout)
            elif extract_type == "all_text":
                elements = await page.query_selector_all(selector)
                extracted = [await el.inner_text() for el in elements]
            else:
                extracted = await page.inner_text(selector, timeout=timeout)

            results["extractedData"][field_name] = extracted
            log_step(results, f"提取数据 [{field_name}]: {str(extracted)[:100]}")
            return {"success": True, "action": action, "fieldName": field_name, "extracted": extracted}

        elif action == "screenshot":
            name = step.get("name", f"screenshot_{step_index}")
            full_page = step.get("fullPage", False)
            screenshot_bytes = await page.screenshot(full_page=full_page)
            import base64
            screenshot_b64 = base64.b64encode(screenshot_bytes).decode("utf-8")
            results["screenshots"].append({"name": name, "data": screenshot_b64})
            return {"success": True, "action": action, "name": name}

        elif action == "hover":
            await page.wait_for_selector(selector, timeout=timeout)
            await page.hover(selector, timeout=timeout)
            return {"success": True, "action": action, "selector": selector}

        elif action == "press":
            await page.wait_for_selector(selector, timeout=timeout)
            await page.press(selector, value, timeout=timeout)
            return {"success": True, "action": action, "selector": selector, "key": value}

        elif action == "check":
            await page.wait_for_selector(selector, timeout=timeout)
            await page.check(selector, timeout=timeout)
            return {"success": True, "action": action, "selector": selector}

        elif action == "uncheck":
            await page.wait_for_selector(selector, timeout=timeout)
            await page.uncheck(selector, timeout=timeout)
            return {"success": True, "action": action, "selector": selector}

        else:
            return {"success": False, "action": action, "error": f"未知的操作类型: {action}"}

    except Exception as e:
        error_msg = str(e)
        log_step(results, f"步骤执行错误: {error_msg}")
        return {"success": False, "action": action, "error": error_msg}


def log_step(results: Dict, message: str):
    timestamp = datetime.now().isoformat()
    results["logs"].append(f"[{timestamp}] {message}")
    print(f"[{timestamp}] {message}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
