# EmpowerMom · 暖心妈妈

> 为产后 0–24 个月妈妈设计的身心关怀 Android APP

> 演示视频：【EmpowerMom 演示视频】 https://www.bilibili.com/video/BV1zjgC62E1Y/?share_source=copy_web

---

## 目录

- [产品简介](#产品简介)
- [功能模块](#功能模块)
- [技术架构](#技术架构)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [后端配置](#后端配置)
- [设计规范](#设计规范)
- [Git 工作流](#git-工作流)

---

## 产品简介

产后妈妈普遍面临**时间极度碎片化**、**情绪支持匮乏**、**育儿知识不足**三重压力。暖心妈妈的设计原则是：

- **最低的时间成本** → 发一条留言 < 3 步，记录状态 < 90 秒
- **最及时的情绪支持** → AI 自动回应、同经历妈妈互动、危机关键词自动识别
- **轻量社群而非重社交** → 匿名优先，保护隐私，无社交压力

**目标用户**：产后 0–24 个月的妈妈
**平台**：Android（minSdk 26，Android 8.0+）

---

## 功能模块

### 留言板

产品核心功能。一块贴满便利贴的数字留言墙，按时间倒序展示其他妈妈的留言。

| 功能点 | 说明 |
|--------|------|
| 分区浏览 | 情绪树洞 / 妈妈互助 / 家庭关系，顶部 Tab 切换 |
| 写留言 | 点击「写心事」→ 半屏编辑器，选分区 → 输内容 → 加标签 → 匿名发布 |
| 预置标签 | 宝宝肠胀气、产后脱发、婆媳关系、睡眠不足等标签，最多选 3 个 |
| 匿名模式 | 默认勾选匿名，也可输入自定义昵称 |
| 点赞 / 共鸣 | 👍 点赞 + 💡「我也经历过」共鸣，可撤销 |
| 回复 | 💬 短文本回复 |
| AI 回应 | 每条留言自动附 AI 情绪回应，按分区定制 prompt |
| 危机干预 | 内容包含危机关键词时，自动置顶心理援助热线，该帖仅自己可见 |
| 删除 | 可删除自己的留言和回复 |

### 每日状态速记

每天回答 3 个随机问题，点选卡片 < 90 秒完成。

| 功能点 | 说明 |
|--------|------|
| 核心题 | 今日情绪状态，颜色卡片选择 |
| 生活题 | 生活状态随机一题（睡眠、进食、运动等） |
| 开放题 | 自由书写今日感受 |
| AI 今日卡片 | 根据回答生成温暖鼓励文案 |
| 本周小结 | 连续记录后 AI 生成本周身心小结 |
| 私密模式 | 可将当日记录设为仅自己可见 |
| 历史记录 | 按日历/列表查看所有历史速记 |

### 用户资料

| 功能点 | 说明 |
|--------|------|
| 昵称 + 头像 | 支持 emoji 头像或上传照片 |
| 头像存储 | 照片上传至 Supabase Storage，跨设备同步 |
| 宝宝年龄 | 记录产后天数 |
| 用户认证 | Supabase Auth 邮箱注册/登录 |

### 数字心理沙盘（占位）

独立入口，WebView 加载外部沙盘页面。

### 家庭协同（占位）

预留入口，未来生成脱敏版「妈妈状态报告」发给伴侣。

---

## 技术架构

### 技术栈

| 层级 | 选型 | 说明 |
|------|------|------|
| 语言 | Kotlin | 2.1.20 |
| UI | Jetpack Compose + Material3 | BOM 2024.11.00 |
| 架构模式 | MVVM + MVI | Intent → ViewModel → UiState |
| 导航 | Navigation Compose | 2.8.4 |
| 本地数据库 | Room | 离线优先，后台同步 |
| 后端 | Supabase | PostgREST + Auth + Storage + Realtime |
| AI | DeepSeek API | 卡片文案、小结、留言回应 |
| 依赖注入 | Hilt | 2.52 |
| 图片加载 | Coil | 2.7.0 |
| 本地存储 | DataStore Preferences | 用户配置持久化 |

### 架构分层

```
┌─────────────────────────────────────────┐
│  UI Layer (Compose Screens)             │  ← 只持有 UiState，调用 handleIntent()
├─────────────────────────────────────────┤
│  ViewModel Layer (MVI)                  │  ← Intent → State 转换，持有 StateFlow
├─────────────────────────────────────────┤
│  Repository Layer                       │  ← 协调 Local (Room/DataStore) + Remote (Supabase)
├──────────────────┬──────────────────────┤
│  Local           │  Remote              │
│  Room / DataStore│  Supabase / DeepSeek │
└──────────────────┴──────────────────────┘
```

**离线优先模式**：数据先写入本地 Room，再通过 `CoroutineScope(Dispatchers.IO)` 后台同步到 Supabase。

---

## 项目结构

```
EmpowerMom/
├── gradle/
│   └── libs.versions.toml              # 统一版本管理
├── supabase/
│   ├── migrations/                     # SQL 迁移脚本
│   ├── create_daily_logs.sql           # 每日速记表
│   └── create_user_profiles.sql        # 用户资料表
├── app/
│   ├── build.gradle.kts
│   └── src/main/java/com/empowermom/app/
│       ├── core/
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── AppDatabase.kt          # Room 数据库（v6）
│       │   │   │   ├── dao/                     # DailyLogDao, MessageDao 等
│       │   │   │   └── entity/                  # Room 实体
│       │   │   ├── remote/
│       │   │   │   └── dto/
│       │   │   │       └── RemoteDto.kt         # Supabase DTO（MessageDto, DailyLogDto 等）
│       │   │   └── repository/
│       │   │       ├── AuthRepository.kt        # Supabase Auth
│       │   │       ├── SupabaseRepository.kt    # Supabase PostgREST + Storage
│       │   │       ├── MessageRepository.kt     # 留言板数据层
│       │   │       ├── DailyLogRepository.kt    # 每日速记数据层 + AI
│       │   │       ├── UserRepository.kt        # 用户资料（DataStore + Supabase）
│       │   │       └── DraftRepository.kt       # 编辑器草稿
│       │   └── network/
│       │       ├── DeepSeekApiService.kt        # DeepSeek API (Retrofit)
│       │       └── PromptTemplates.kt           # AI prompt 模板
│       ├── di/
│       │   ├── AppModule.kt                     # Hilt：Room DB + Retrofit
│       │   └── SupabaseModule.kt                # Hilt：Supabase Client
│       ├── feature/
│       │   ├── auth/                            # 登录/注册页面
│       │   ├── messageboard/                    # 留言板（列表/编辑器/详情）
│       │   ├── dailylog/                        # 每日速记（问题/卡片/历史）
│       │   ├── profile/                         # 用户资料 + 历史记录
│       │   ├── sandbox/                         # 沙盘（WebView）
│       │   └── family/                          # 家庭协同（占位）
│       ├── navigation/
│       │   ├── Screen.kt                        # 路由定义
│       │   └── AppNavGraph.kt                   # NavHost 配置
│       ├── MainActivity.kt                      # 入口 + 底部导航
│       └── EmpowerMomApplication.kt             # @HiltAndroidApp
└── README.md
```

---

## 快速开始

### 环境要求

- **Android Studio** Hedgehog (2023.1.1) 或更新
- **JDK** 17
- **Android SDK** compileSdk 35，minSdk 26（Android 8.0+）

### 导入项目

```bash
git clone <repo-url>
cd EmpowerMom
# 用 Android Studio 打开，等待 Gradle sync 完成
```

### 构建类型

| 类型 | 用途 |
|------|------|
| `debug` | 日常开发，OkHttp 日志开启 |
| `release` | 发布包，开启代码混淆和资源压缩 |

---

## 后端配置

项目使用 Supabase 作为后端，需要在 Supabase Dashboard 完成以下配置：

### 1. 创建 Supabase 项目

在 [supabase.com](https://supabase.com) 创建项目，获取 URL 和 anon key。

### 2. 创建数据表

在 SQL Editor 中依次执行 `supabase/` 目录下的 SQL 脚本：

- `supabase/migrations/00001_create_messageboard_tables.sql` — 留言板相关表
- `supabase/create_daily_logs.sql` — 每日速记表
- `supabase/create_user_profiles.sql` — 用户资料表

### 3. 授权

对 `anon` 和 `authenticated` 角色授权：

```sql
GRANT SELECT, INSERT, UPDATE, DELETE ON public.messages TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.replies TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.daily_logs TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_profiles TO anon, authenticated;
GRANT USAGE, SELECT ON SEQUENCE messages_id_seq TO anon, authenticated;
GRANT USAGE, SELECT ON SEQUENCE replies_id_seq TO anon, authenticated;
GRANT USAGE, SELECT ON SEQUENCE daily_logs_id_seq TO anon, authenticated;
```

### 4. 创建 Storage Bucket

在 Storage 中创建 `avatars` bucket（Public），用于头像上传。

### 5. 配置密钥

在 `local.properties` 或环境变量中配置：

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
DEEPSEEK_API_KEY=your-deepseek-api-key
```

---

## 设计规范

### 颜色

| Token | 值 | 用途 |
|-------|----|------|
| `Peach` | `#F4845F` | 主色，按钮 |
| `PeachPale` | `#FFF0EB` | 浅色背景 |
| `Amber` | `#F6B93B` | 强调色 |
| `AmberPale` | `#FFF8E1` | 浅色强调背景 |
| `TextDark` | `#2D2D2D` | 主文字 |
| `TextLight` | `#999999` | 辅助文字 |
| `Coral` | `#E74C3C` | 危机内容标识 |
| `Teal` | `#009688` | 功能区分色 |

支持 **Dark Mode**，深色方案自动切换。

### 字体

- 标题：`FontWeight.Light`
- 正文：`FontWeight.Normal`
- 字号：28sp → 22sp → 18sp → 14sp → 12sp → 11sp

### 形状

全局使用 `RoundedCornerShape(10.dp)` 圆角风格。

---

## Git 工作流

### 分支规范

```
main            生产分支（保护，仅 PR 合并）
develop         开发主分支
feature/<name>  功能开发
fix/<name>      Bug 修复
```

### Commit 规范

```
feat:     新功能
fix:      Bug 修复
refactor: 重构
style:    样式调整
docs:     文档更新
test:     测试
chore:    构建配置、依赖升级
```
