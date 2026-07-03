# ⚡ QuickCommand

> 在屏幕上画个手势，瞬间唤起你需要的命令。

QuickCommand 是一款 Android 快捷命令工具，核心功能是**手势唤起**——通过自定义手势（圆圈、三角形、对勾、V 形、方形、滑动等）快速执行预设命令，如打开应用、设置提醒等。无需解锁桌面、无需翻找 App，一个手势搞定。

## ✨ 功能特性

### 🖐️ 手势引擎
- 基于 [$1 Unistroke Recognizer](https://depts.washington.edu/acelab/proj/dollar/) 算法
- 支持 5 种预定义手势 + 自定义手势录制
- 手势识别全流程：重采样 → 旋转归一化 → 缩放归一化 → 黄金分割角度搜索匹配

| 预定义手势 | 形状 |
|:---:|:---:|
| 圆圈 | ⭕ |
| 三角形 | 🔺 |
| 方形 | ⬜ |
| V 形 | ✌️ |
| 对勾 | ✅ |

额外支持四向滑动手势（↑ ↓ ← →）。

### 📋 命令系统
- **打开应用** — 绑定包名，一键启动任意 App
- **提醒通知** — 设置文字提醒，到手势触发时推送通知
- 可扩展的命令类型设计，方便后续增加更多操作

### 🔮 悬浮球
- 屏幕任意位置常驻半透明悬浮球
- 点击即可呼出手势绘制界面
- 支持拖动调整位置

### 🎨 手势绘制
- 全屏手势绘制区域，支持实时轨迹绘制
- 触觉反馈（振动）确认手势完成
- 绘制完成后自动识别并执行

## 🏗️ 技术架构

```
QuickCommand/
├── app/src/main/java/com/quickcommand/
│   ├── MainActivity.kt              # 主界面：命令列表 + 悬浮球开关
│   ├── GestureCaptureActivity.kt    # 手势绘制界面
│   ├── AddEditCommandActivity.kt    # 命令添加/编辑界面
│   ├── gesture/
│   │   ├── GestureMatcher.kt        # $1 手势识别引擎
│   │   └── GestureDrawView.kt       # 自定义手势绘制 View
│   ├── model/
│   │   ├── Command.kt               # 数据模型 + Room TypeConverters
│   │   ├── GesturePoint.kt          # 手势点数据类
│   │   └── GestureType.kt           # 手势类型枚举
│   ├── service/
│   │   ├── FloatingBallService.kt   # 悬浮球服务
│   │   └── CommandExecutor.kt       # 命令执行器
│   ├── viewmodel/
│   │   └── CommandViewModel.kt      # ViewModel + StateFlow
│   └── repository/
│       └── CommandRepository.kt     # Room 数据仓库
├── app/src/main/res/
│   ├── layout/                      # XML 布局文件
│   ├── drawable/                    # 图标素材
│   └── values/                      # 主题、字符串、颜色
└── website/
    └── index.html                   # 项目官网（纯前端）
```

### 技术栈

| 层级 | 技术 |
|---|---|
| 语言 | Kotlin |
| 架构 | MVVM + Repository |
| 数据库 | Room (SQLite) |
| 异步 | Kotlin Coroutines + StateFlow |
| UI | Android View System + ViewBinding |
| 手势识别 | $1 Unistroke Recognizer |
| 悬浮窗 | WindowManager + System Alert Window |

## 📱 系统要求

- Android 8.0 (API 26) 及以上
- 需要悬浮窗权限（`SYSTEM_ALERT_WINDOW`）

## 🚀 开始使用

### 从源码构建

```bash
# 克隆仓库
git clone https://github.com/your-username/QuickCommand.git
cd QuickCommand

# 使用 Android Studio 打开项目，或命令行构建
./gradlew assembleDebug

# 安装到已连接的设备
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 使用步骤

1. 打开 QuickCommand，授予悬浮窗权限
2. 点击右下角 **+** 添加命令
3. 选择手势类型（如「圆圈」）、命令类型（如「打开应用」）、填写目标包名
4. 保存后，点击悬浮球
5. 在手势区域画出对应手势，命令立即执行

## 🔒 权限说明

| 权限 | 用途 |
|---|---|
| `SYSTEM_ALERT_WINDOW` | 显示悬浮球 |
| `VIBRATE` | 手势完成时的触觉反馈 |
| `POST_NOTIFICATIONS` | 推送提醒通知 |
| `QUERY_ALL_PACKAGES` | 查询已安装应用列表 |

## 📄 License

MIT License - 详见 [LICENSE](LICENSE)

## 🙏 致谢

- [$1 Unistroke Recognizer](https://depts.washington.edu/acelab/proj/dollar/) - University of Washington
- 所有预定义手势模板的生成基于该算法的归一化流程
