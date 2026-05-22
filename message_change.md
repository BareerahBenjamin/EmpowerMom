# 留言板功能变更清单（本地版本为主）

对比对象：
- 本地：`D:\PDD\EmpowerMom-master`（你当前修改的版本）
- 远端：`D:\PDD\EmpowerMom-master\EmpowerMom-remote`（从 GitHub 克隆的对照版本）

说明：
- 这份清单以“功能/体验”为维度整理差异，并以本地版本作为目标实现（即：远端相对本地缺失/简化的能力，也会在此体现）。

## 功能导向差异

### 1) 分区/筛选
- 本地：使用 Tab 体系（All / Private / Category）进行筛选；其中 Private 用于汇总“仅自己可见/危机内容”。
- 远端：仅使用单一 `selectedCategory` 做筛选，无 Tab 体系、无 Private 汇总入口。

### 2) 搜索
- 本地：支持搜索弹窗与搜索结果页（搜索状态、结果列表、关闭/返回等完整流程）。
- 远端：无搜索相关 UI 状态与逻辑。

### 3) 发帖编辑器（能力范围）
- 本地：功能更完整
  - 媒体附件：支持选择图片/视频（含权限申请与附件管理）
  - 标签：支持预置标签 + 自定义标签弹窗
  - 可见性：支持“仅自己可见”（私密开关）
  - 匿名/昵称：支持匿名发布与昵称输入
  - 提交：提交中状态/错误提示等
- 远端：编辑器被收敛为“分区 + 内容 + 预置标签 + 匿名/昵称 + 发布”，不包含附件/私密/自定义标签；另加入“取消匿名后自动聚焦昵称输入框”的交互。

### 4) 匿名与昵称校验规则
- 本地：非匿名时作者名有多重兜底（Profile 昵称/输入昵称/默认值），不强制必须填写昵称。
- 远端：非匿名且昵称为空会被校验拦截（必须填昵称）。

### 5) 私密与可见性策略（仅自己可见 + 危机帖）
- 本地：
  - 数据层全链路支持 `isPrivateOnly`（UI → ViewModel → Repository → Room）
  - 列表默认过滤 `isHidden = 0 AND isPrivateOnly = 0`
  - 通过 Private Tab 展示“私密/危机”汇总内容
- 远端：
  - 仅有 `isHidden`（危机帖隐藏）策略
  - 无 `isPrivateOnly` 字段与对应的查询/展示链路

### 6) 附件存储与展示
- 本地：Message/Entity/Repository 均包含附件字段与 JSON 映射（`attachmentsJson` + `MediaAttachment/MediaKind`）。
- 远端：不包含附件字段与映射（附件能力缺失/未接入）。

### 7) 详情页（阅读/回复体验）
- 本地：详情页 UI 更“强交互/强展示”（包含用户资料参与、回复输入聚焦、回复项更丰富展示）。
- 远端：详情页展示更基础（回复项为简化版布局，不依赖用户资料）。

### 8) 留言/回复的数据加载方式（响应式）
- 本地：`observeMessageWithReplies()` 使用 `observeMessageById()` + `combine(repliesFlow)`，更容易做到 message 主体变化时的实时刷新。
- 远端：以 replies 变化为驱动，使用 `getMessageById()` 组装 message，message 主体更新的响应性相对弱一些。

### 9) 点赞/共鸣/回复发送、AI 回应
- 两边：都有点赞/共鸣切换、回复发送（含发送中与失败提示）、以及 AI 回应生成并写回数据库的流程。
- 本地：AI 回应链路与“附件/私密/危机跳转详情”等逻辑结合更紧（整体功能更完整）。

### 10) 数据库与 AI 模板（额外改动）
- 数据库：`AppDatabase` 的版本从 `2` 升级到 `4`（用于承接你本地项目中的数据库结构演进）。
- AI 模板：`PromptTemplates` 调整为更贴近产品设定的“星芽”口吻，并匹配当前留言板分区枚举（情绪树洞/妈妈互助/家庭关系）。

## 所有改动过的文件（留言板功能相关）

留言板功能目录：
- `app/src/main/java/com/empowermom/app/feature/messageboard/model/Message.kt`
- `app/src/main/java/com/empowermom/app/feature/messageboard/ui/MessageBoardScreen.kt`
- `app/src/main/java/com/empowermom/app/feature/messageboard/ui/MessageDetailScreen.kt`
- `app/src/main/java/com/empowermom/app/feature/messageboard/ui/MessageEditor.kt`
- `app/src/main/java/com/empowermom/app/feature/messageboard/viewmodel/MessageBoardViewModel.kt`
- `app/src/main/java/com/empowermom/app/feature/messageboard/viewmodel/MessageDetailViewModel.kt`

留言板依赖的数据层/全局配置：
- `app/src/main/java/com/empowermom/app/core/data/local/dao/MessageDao.kt`
- `app/src/main/java/com/empowermom/app/core/data/local/entity/MessageEntity.kt`
- `app/src/main/java/com/empowermom/app/core/data/repository/MessageRepository.kt`
- `app/src/main/java/com/empowermom/app/core/data/local/AppDatabase.kt`
- `app/src/main/java/com/empowermom/app/core/network/PromptTemplates.kt`
- `message_change.md`
