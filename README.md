# 暖心留言板

> 为产后 0–24 个月妈妈设计的极简产后身心关怀 Android APP

---

## 目录

- [产品简介](#产品简介)
- [功能模块](#功能模块)
- [技术架构](#技术架构)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [设计规范](#设计规范)
- [Git 工作流](#git-工作流)
- [开发进度](#开发进度)

---

## 产品简介

产后妈妈普遍面临**时间极度碎片化**、**情绪支持匮乏**、**育儿知识不足**三重压力。暖心留言板的设计原则是：

- **最低的时间成本** → 发一条留言 < 3 步，记录状态 < 90 秒
- **最及时的情绪支持** → AI 自动回应、同经历妈妈互动、危机关键词自动识别
- **轻量社群而非重社交** → 匿名优先，保护隐私，无社交压力

**目标用户**：产后 0–24 个月的妈妈
**平台**：Android（minSdk 26，Android 8.0+）

---

## 功能模块

### P0 · 留言板 ✅ 架构已完成

产品核心功能。一块贴满便利贴的数字留言墙，按时间倒序展示其他妈妈的留言。

| 功能点 | 说明 |
|--------|------|
| 分区浏览 | 情绪树洞 / 育儿求助 / 经验分享 / 身材恢复，顶部 Tab 切换 |
| 写留言 | 点击「写心事」→ 半屏编辑器，选分区 → 输内容（500字内）→ 加标签 → 匿名发布 |
| 预置标签 | 宝宝肠胀气、产后脱发、婆媳关系、睡眠不足等 8 个标签，最多选 3 个 |
| 匿名模式 | 默认勾选匿名，也可输入自定义昵称 |
| 点赞 | 👍 数字显示，可撤销，本地持久化 |
| 共鸣 | 💡「我也经历过」，计数 +1，不显示谁点过（保护隐私） |
| 回复 | 💬 短文本回复，支持 Enter 发送 |
| AI 回应 | 每条留言自动附 AI 情绪回应或知识解答 |
| 危机干预 | 内容包含危机关键词时，自动置顶心理援助热线，该帖仅自己可见 |

### P1 · 数字心理沙盘 🔲 占位中

独立入口、非强制、低频高价值的深度功能。用户主动选择主题，拖拽 2D 元素到画布，AI 生成心理状态分析报告，可分享给专业咨询师。

### P1 · 每日状态速记 🔲 占位中

每天 1–3 个轮询问题，点选卡片 < 90 秒完成。AI 自动生成「今日卡片」，连续 7 天触发「本周身心小结」。

### P2 · 微关怀推送 🔲 待开发

基于记录数据，在合适时机推送 1–5 分钟可完成的关怀动作（深呼吸音频、拉伸 GIF、一键生成给老公的文案等）。每日不超过 2 条。

### P2 · 家庭协同 🔲 占位中

生成脱敏版「妈妈本周状态报告」发给伴侣，并推送「今日可做的事」给家庭成员。

---

## 技术架构

### 技术栈

| 层级 | 选型 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material3 | BOM 2024.11.00 |
| 架构模式 | MVVM + Clean Architecture + MVI | — |
| 导航 | Navigation Compose | 2.8.4 |
| 本地数据库 | Room | 2.6.1 |
| 网络 | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| 依赖注入 | Hilt | 2.52 |
| 状态管理 | StateFlow + collectAsState | — |
| 图片加载 | Coil | 2.7.0 |
| 本地存储 | DataStore Preferences | 1.1.1 |

### 分层说明

```
┌─────────────────────────────────────────┐
│  UI Layer (Compose Screens)             │  ← 只持有 UiState，调用 handleIntent()
├─────────────────────────────────────────┤
│  ViewModel Layer (MVI)                  │  ← Intent → State 转换，持有 StateFlow
├─────────────────────────────────────────┤
│  Repository Layer                       │  ← 单一数据源，协调 Local + Remote
├──────────────────┬──────────────────────┤
│  Local (Room)    │  Remote (Retrofit)   │  ← 数据源层，不暴露给 UI
└──────────────────┴──────────────────────┘
```

**MVI 交互模式**（以留言板为例）：

```
用户点击「点赞」
    → handleIntent(ToggleLike(messageId))
        → repository.toggleLike()
            → Room 更新 + StateFlow 发出新 UiState
                → UI 重组
```

---

## 项目结构

```
EmpowerMom/
├── gradle/
│   └── libs.versions.toml          # 统一版本管理（Gradle Version Catalog）
├── app/
│   ├── build.gradle.kts            # 模块依赖，debug/release 构建类型
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/empowermom/app/
│           ├── core/
│           │   ├── data/
│           │   │   ├── local/
│           │   │   │   ├── AppDatabase.kt          # Room 数据库入口
│           │   │   │   ├── dao/
│           │   │   │   │   └── MessageDao.kt       # Message / Reply / 互动 DAO
│           │   │   │   └── entity/
│           │   │   │       └── MessageEntity.kt    # Room 实体（含 UserInteraction）
│           │   │   └── repository/
│           │   │       └── MessageRepository.kt   # 点赞/共鸣/发帖/回复
│           │   └── ui/
│           │       └── theme/
│           │           ├── Theme.kt               # 黑白极简主题，Light/Dark 双配色
│           │           └── Typography.kt          # 字体 + 形状（方形）
│           ├── di/
│           │   └── AppModule.kt                   # Hilt：DB / Network / Gson 注入
│           ├── feature/
│           │   ├── messageboard/                  # ✅ P0
│           │   │   ├── model/
│           │   │   │   └── Message.kt             # 领域模型 + 枚举 + 危机关键词
│           │   │   ├── ui/
│           │   │   │   ├── MessageBoardScreen.kt  # 列表页（TopBar + 卡片 + 底部按钮）
│           │   │   │   ├── MessageEditor.kt       # 半屏写留言编辑器
│           │   │   │   └── MessageDetailScreen.kt # 详情页 + 回复列表
│           │   │   └── viewmodel/
│           │   │       ├── MessageBoardViewModel.kt   # UiState + Intent 处理
│           │   │       └── MessageDetailViewModel.kt
│           │   ├── sandbox/ui/SandboxScreen.kt    # 🔲 P1 占位
│           │   ├── dailylog/ui/DailyLogScreen.kt  # 🔲 P1 占位
│           │   └── family/ui/FamilyScreen.kt      # 🔲 P2 占位
│           ├── navigation/
│           │   ├── Screen.kt                      # 所有路由定义（sealed class）
│           │   └── AppNavGraph.kt                 # NavHost 配置
│           ├── MainActivity.kt
│           └── EmpowerMomApplication.kt            # @HiltAndroidApp
└── README.md
```

---

## 快速开始

### 环境要求

- **Android Studio** Hedgehog (2023.1.1) 或更新版本
- **JDK** 17
- **Android SDK** compileSdk 35，minSdk 26（Android 8.0+）

### 导入项目

```bash
# 1. 克隆仓库
git clone <repo-url>
cd EmpowerMom

# 2. 用 Android Studio 打开
#    File → Open → 选择 EmpowerMom 文件夹

# 3. 等待 Gradle sync 完成（首次约 2–5 分钟）

# 4. 连接设备或启动模拟器，点击 Run ▶
```

### 构建类型

| 类型 | applicationId 后缀 | 用途 |
|------|--------------------|------|
| `debug` | `.debug` | 日常开发，OkHttp 日志开启 |
| `release` | 无 | 发布包，开启代码混淆和资源压缩 |

---

## 设计规范

主题遵循纯黑白极简风格，与 Web 原型保持一致。

### 颜色

| Token | 值 | 用途 |
|-------|----|------|
| `primary` | `#000000` | 主色，按钮、激活态 |
| `background` | `#FFFFFF` | 页面背景 |
| `surfaceVariant` | `#F9F9F9` | 卡片背景、AI 回应区块 |
| `outline` | `#E5E5E5` | 边框、分隔线 |
| `secondary` | `#666666` | 辅助文字、图标 |
| `error` | `#EF4444` | 危机内容标识（仅此一处用红色） |

支持 **Dark Mode**，深色方案自动切换（`isSystemInDarkTheme()`）。

### 字体

- 标题：`FontWeight.Light`，对应 Web 原型的 `font-light`
- 正文：`FontWeight.Normal`
- 字号体系：28sp（大标题）→ 22sp → 18sp → 14sp → 12sp → 11sp

### 形状

全局使用 `RoundedCornerShape(0.dp)`（直角），对应 Web 原型的 `rounded-none`。

---

## Git 工作流

### 分支规范

```
main            生产分支（保护，仅 PR 合并）
develop         开发主分支
feature/<name>  功能开发，如 feature/sandbox
fix/<name>      Bug 修复，如 fix/like-count
```

### Commit 规范（Conventional Commits）

```
feat:     新功能
fix:      Bug 修复
refactor: 重构（不改变功能）
style:    样式/格式调整
docs:     文档更新
test:     测试
chore:    构建配置、依赖升级
```

**示例**：

```
feat: 留言板支持按分区筛选
fix: 修复点赞数在重启后重置的问题
docs: 更新 README 快速开始章节
```

---

## 开发进度

| 模块 | 状态 | 说明 |
|------|------|------|
| 项目架构搭建 | ✅ 完成 | MVVM + Hilt + Room + Compose |
| 主题系统 | ✅ 完成 | 黑白极简，支持 Dark Mode |
| 留言板 UI | ✅ 完成 | 列表 / 编辑器 / 详情页 |
| 留言板数据层 | ✅ 完成 | Room + Repository + ViewModel |
| 危机干预 | ✅ 完成 | 关键词检测 + 热线展示 |
| AI 回应接入 | 🚧 进行中 | 待接入 Anthropic API |
| 数字心理沙盘 | 🔲 待开发 | P1 |
| 每日状态速记 | 🔲 待开发 | P1 |
| 微关怀推送 | 🔲 待开发 | P2 |
| 家庭协同 | 🔲 待开发 | P2 |
