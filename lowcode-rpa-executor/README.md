# RPA Executor Service

基于 Playwright 的 RPA 浏览器自动化执行器服务。

## 功能特性

- 支持浏览器自动化操作（点击、输入、选择、滚动等）
- 支持数据提取（文本、属性、HTML等）
- 支持截图功能
- 提供 REST API 接口
- 支持参数化脚本执行

## 安装

```bash
pip install -r requirements.txt
playwright install chromium
```

## 启动

```bash
python -m app.main
```

服务默认运行在 http://localhost:8000

## API 接口

### 健康检查
```
GET /health
```

### 验证脚本
```
POST /api/validate
Content-Type: application/json

{
  "script": "{\"steps\": [...]}"
}
```

### 执行脚本
```
POST /api/execute
Content-Type: application/json

{
  "script": "{\"steps\": [...]}",
  "params": {"username": "test", "password": "123456"},
  "targetUrl": "https://example.com"
}
```

## 脚本格式

```json
{
  "steps": [
    {
      "action": "navigate",
      "url": "https://example.com/login"
    },
    {
      "action": "input",
      "selector": "#username",
      "value": "${username}"
    },
    {
      "action": "input",
      "selector": "#password",
      "value": "${password}"
    },
    {
      "action": "click",
      "selector": "#login-btn"
    },
    {
      "action": "extract",
      "selector": ".welcome-text",
      "fieldName": "welcomeText",
      "extractType": "text"
    }
  ]
}
```
