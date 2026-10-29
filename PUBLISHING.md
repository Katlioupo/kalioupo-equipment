# 发布指南（GitHub / Modrinth / CurseForge）

这份文档写给不熟悉 Git 的作者本人：照着点就行。所有命令都在项目根目录
（`kalioupo-equipment`）里执行。

---

## 0. 先准备好要发布的文件

```powershell
# 在项目根目录构建（离线也能跑）
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot'
.\gradlew.bat build --offline --no-daemon
```

产物：`build/libs/kalioupo-equipment-<版本>.jar`
（`*-sources.jar` 是源码包，发布时不需要）

---

## 1. 上传到 GitHub

### 路线 A：GitHub Desktop（推荐，全图形界面）

1. 安装 <https://desktop.github.com/>
2. 打开 GitHub Desktop → 登录你的 GitHub 账号
3. 菜单 **File → Add local repository…** → 选择 `kalioupo-equipment` 文件夹
4. 它会提示"This directory does not appear to be a Git repository"？
   —— 本地已经初始化好了（见文末「已经做好的准备」），正常应该直接读到一个仓库
5. 点右上角 **Publish repository**
   - Name：`kalioupo-equipment`
   - 取消勾选 **Keep this code private**（要公开）
   - 点 **Publish repository**
6. 以后每次改动：左下角填一句说明 → **Commit to main** → 右上角 **Push origin**

### 路线 B：命令行（git 已装好，2.54）

在 GitHub 网页上先建一个**空仓库**（不要勾选 README / .gitignore / License）：

1. <https://github.com/new>
2. Repository name：`kalioupo-equipment`
3. 选 **Public**，其它都不勾，点 **Create repository**
4. 回到项目目录执行（把 `你的用户名` 换成自己的）：

```powershell
git remote add origin https://github.com/你的用户名/kalioupo-equipment.git
git branch -M main
git push -u origin main
```

第一次 push 会弹出浏览器让你登录 GitHub，点同意即可。

### 仓库信息（About / Topics）

仓库页面右上角 **⚙ About** 里填三样东西：

**Description**（复制这一段）：

```
Minecraft 26.3 Fabric mod: 11 tiers of tools, armour and shields, active skills, a smelter, a backpack and a socket table that lets old gear keep working. 有趣的装备：11 级装备线 + 主动技能 + 熔炼炉 + 背包 + 镶嵌台（副位）。
```

**Website**：填 Modrinth 项目地址（发布后才有，先留空）

**Topics**（一个格子一个，回车确认）：

```
minecraft fabric minecraft-mod fabric-mod equipment armor tools magic
technologymod minecraft-26-3
```

**License**：GitHub 会按 `LICENSE` 文件自动识别，我们的自定义许可证会显示成
"Unknown license"——这是正常的，不影响发布。如果你想要一个 GitHub 认得的标签，
只能换成 MIT / Apache-2.0 这类标准许可证，但它们允许别人闭源再发布甚至卖钱，
和现在的条款冲突，所以建议保持自定义。

### 发版本（Release）

1. 打开仓库页面 → 右侧 **Releases** → **Draft a new release**
2. **Choose a tag**：输入 `v1.2.7` → 点 "Create new tag"
3. Title：`1.2.7`
4. 说明：把 `CHANGELOG.md` 里对应段落复制进去
5. **Attach binaries**：拖入 `build/libs/kalioupo-equipment-1.2.7.jar`
6. **Publish release**

---

## 2. 上传到 Modrinth（玩家最常用的地方）

1. 注册并登录 <https://modrinth.com/>
2. 右上角头像 → **Create a project**
3. 填：
   - Name：`Kalioupo's Interesting Equipment`
   - Summary（简介，见下方文案）
   - 类型选 **Mod**，环境选 **Fabric**，Minecraft 版本 **26.3**
   - 上传图标：`src/main/resources/assets/kalioupo_equipment/icon.png`
4. **Versions → Create version**
   - 版本号 `1.2.7`、渠道选 **Release**
   - 上传 `kalioupo-equipment-1.2.7.jar`
   - 依赖：**Fabric API**（必填），JEI / Mod Menu / Cloth Config 选填
   - 更新说明：从 `CHANGELOG.md` 复制
5. 提交审核（通常几分钟到几小时）

## 3. 上传到 CurseForge

1. <https://www.curseforge.com/> → 头像 → **Author Dashboard** → **Create Project**
2. 分类选 **Mods → Minecraft**，游戏版本 26.3，加载器 Fabric
3. 上传同样的 jar，填写简介与更新日志

---

## 4. 现成的文案

**简短简介（Modrinth Summary）**

> 从碳钢到星辉的 11 级装备线：工具、盔甲、盾牌、可召雷的紫水晶剑、回响与星辉技能，外加冶炼炉、背包和让旧装备不报废的「镶嵌台」副位系统。

**长简介（CurseForge / Modrinth 正文开头）**

```
Kalioupo's Interesting Equipment 有趣的装备

一条从碳钢一路做到星辉的 11 级材料线，每一样材料都有明确用途：

- 11 级工具与四件套盔甲，黑曜石档起就压过原版下界合金
- 三档带棱刺的盾牌，震飞技能有独立冷却条，用完照样能举盾
- 紫水晶剑：打怪充能，满能量召雷，32 格射程 + 近身自动索敌
- 回响之刃、星辉之刃：声波冲击、闪现、落雷、祝福四种模式
- 冶炼炉：按配方炼制合金，支持漏斗自动化与比较器
- 背包：可配置容量
- 镶嵌台（副位）：旧装备不报废——把铜合金镶进武器换充能速度，
  把紫水晶剑镶进新武器继承雷霆，把玫瑰金镶进装备让它自己修复

需要 Fabric Loader 0.19.5+ 与 Fabric API 0.163.0+26.3。
```

**标签建议**：`equipment` `tools` `armor` `magic` `technology` `fabric`

---

## 已经做好的准备（不用你操作）

- 本地已 `git init` 并完成首次提交，提交信息记录了 1.2.7 的内容
- `.gitignore` 已排除 `build/`、`.gradle/`、`run/`、崩溃日志
- `libs/jei-26.3-fabric-31.16.0.88.jar` 是**编译用**依赖，已随仓库提交，
  这样别人 clone 下来也能离线构建；运行时不需要它
- 许可证：[`LICENSE`](LICENSE)（允许整合包收录，转载需署名，禁止售卖）
- 版本说明与更新日志：[`CHANGELOG.md`](CHANGELOG.md)

## 5. 日常更新流程（改完代码之后）

```powershell
cd "C:\Users\Administrator\Documents\deepseek-harness\default-workspace\kalioupo-equipment"

$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot'
.\gradlew.bat build --offline --no-daemon

git add -A
git commit -m "1.2.9: 改了什么"
git push

# 想发版就再打个标签，然后按上面第 1 节的步骤发 Release
git tag v1.2.9
git push origin v1.2.9
```

版本号在 `gradle.properties` 的 `mod_version` 里改；`CHANGELOG.md` 记得加一段。

## 常见问题

**Q：忘了改版本号就发布了？**
改 `gradle.properties` 里的 `mod_version`，重新构建，再发一个 Release。

**Q：推送时报 "rejected non-fast-forward"？**
网页上提前建仓库时勾了 README，本地和远端都有提交。执行
`git pull --rebase origin main` 再 `git push` 即可。

**Q：想改仓库名或简介？**
GitHub 仓库页 **Settings** 里改，Modrinth/CurseForge 在项目设置里改。
