# 低代码平台自定义组件开发脚手架

## 组件包结构

```
your-component.zip/
├── index.js              # 组件主入口文件（必需）
├── component.json        # 组件配置文件（必需）
├── style.css             # 组件样式文件（可选）
├── utils.js              # 工具函数（可选）
└── README.md             # 说明文档（可选）
```

## component.json 配置说明

```json
{
  "name": "组件包名称",
  "version": "1.0.0",
  "main": "index.js",

  "componentType": "CustomButton",
  "componentName": "自定义按钮",
  "componentCategory": "custom",
  "icon": "🔘",
  "description": "组件功能描述",
  "author": "作者名称",

  "propSchema": {
    "type": "object",
    "properties": {
      "text": {
        "type": "string",
        "title": "按钮文本",
        "default": "按钮",
        "x-decorator": "FormItem",
        "x-component": "Input"
      }
    }
  },

  "eventSchema": {
    "onClick": {
      "type": "object",
      "title": "点击事件",
      "properties": {
        "timestamp": { "type": "number" }
      }
    }
  },

  "exposedEvents": ["onClick", "onChange"],

  "defaultProps": {
    "text": "按钮"
  },

  "defaultStyle": {
    "padding": 8,
    "margin": 4
  }
}
```

### 配置字段说明

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `main` | string | 是 | 组件主入口文件名 |
| `componentType` | string | 是 | 组件类型唯一标识（全局唯一） |
| `componentName` | string | 是 | 组件显示名称 |
| `componentCategory` | string | 是 | 组件分类: basic/form/layout/data/chart/advanced/custom |
| `icon` | string | 否 | 组件图标（emoji或图标名称） |
| `propSchema` | object | 是 | 属性定义JSON Schema，用于生成属性配置面板 |
| `eventSchema` | object | 否 | 事件定义Schema |
| `exposedEvents` | string[] | 否 | 暴露给平台的事件名称列表 |
| `defaultProps` | object | 否 | 默认属性值 |
| `defaultStyle` | object | 否 | 默认样式 |

## propSchema 支持的组件类型

- `Input` - 文本输入
- `Input.TextArea` - 多行文本输入
- `NumberPicker` - 数字输入
- `Select` - 下拉选择
- `Switch` - 开关
- `ColorPicker` - 颜色选择器
- `Slider` - 滑块

## 事件绑定说明

在 `exposedEvents` 中声明的事件，平台会自动进行事件绑定。

组件代码中触发事件：
```javascript
const CustomButton = (props) => {
  const { onClick, onChange } = props

  const handleClick = () => {
    // 触发事件
    if (onClick) {
      onClick({
        timestamp: Date.now(),
        value: 'clicked'
      })
    }
  }

  return <button onClick={handleClick}>按钮</button>
}
```

在设计器中配置事件动作：
- 页面跳转
- API调用
- 提交表单
- 自定义代码
- 等等...

## 本地调试

1. 将组件文件放入 `src/components/component-scaffold/` 目录
2. 在本地开发环境中引入并测试组件
3. 确保组件能正常渲染和交互
4. 测试所有暴露的事件是否正确触发

## 打包上传

1. 将所有文件打包为ZIP格式
2. 确保ZIP包根目录包含 `index.js` 和 `component.json`
3. 登录平台，进入「组件管理」页面
4. 点击「上传组件」，填写组件信息并上传ZIP包
5. 上传成功后，组件会自动注册到设计器组件库

## 版本管理

- 首次上传创建组件和初始版本
- 后续上传新版本时，选择「升级」操作
- 每个版本保留独立的代码和配置
- 可以废弃历史版本，但不能删除
- 页面使用的组件版本可以升级

## 注意事项

1. 组件代码中可以使用React Hooks
2. 可以导入和使用antd组件库
3. 不要依赖项目内部的私有模块
4. 网络请求请使用平台提供的API
5. 组件大小控制在50MB以内
6. 确保组件在不同浏览器中都能正常工作
7. 遵循React最佳实践，避免性能问题
