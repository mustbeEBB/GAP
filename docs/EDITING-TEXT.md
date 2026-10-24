# 怎么修改 GAP 界面文字

## 日常修改只需要这个文件

`app/src/main/res/values/strings.xml`

用 VS Code、Android Studio 或记事本打开，按 Ctrl+F 搜索屏幕上的原文字。文件使用 UTF-8 保存。

例如原来是：

```xml
<string name="home_section" formatted="false">"你的使用节奏"</string>
```

改成：

```xml
<string name="home_section" formatted="false">"按自己的节奏"</string>
```

只修改引号中间的字，保留 `name`、`formatted`、XML 标签和外层英文双引号。`name` 是代码使用的名称，改名会导致构建失败。

## 常用位置

| 名称 | 界面文字 |
| --- | --- |
| app_name | GAP：桌面和首页名称 |
| home_help | 使用说明入口 |
| home_portion_title | 本轮剩余额度 |
| home_daily_title | 今日剩余 |
| home_section | 你的使用节奏 |
| protect_target | 保护对象 |
| rules_entry | 规则设置 |
| permissions_entry | 权限与运行检查 |
| home_cooldown_title | 距离下一份额度 |
| home_cooldown_hint | 冷却中的解释 |
| status_connected | 服务已连接 |
| status_disconnected | 服务未连接 |
| rest_home_button | 回到桌面按钮 |
| rest_tomorrow | 每日耗尽后的“明日恢复” |
| rules_title | 规则设置页标题 |
| rules_save | 保存规则按钮 |
| service_name | 安卓无障碍设置中显示的服务名称 |
| brand_slogan | 应用内的 You need a GAP. 文字；图片不随它变化 |

其他页面内容也已集中到这个文件。`copy_001` 等名称是稳定索引，按原文搜索最方便，完整列表见 [COPY-CATALOG.md](COPY-CATALOG.md)。一些带应用名、剩余时间的句子由固定片段拼接，搜索同一句的前半部分即可找到；程序仍负责插入真实数字，不要把显示数字当成规则值。

## 特殊符号

- 换行使用 `\n`，不要删掉需要保留的换行。
- `&` 写成 `&amp;`，`<` 写成 `&lt;`。
- 英文双引号在文字内部写成 `\"`，英文单引号写成 `\'`。
- 不要删除显示时间的数字绘制代码或改变内部偏好键。
- 数字仍由程序实时生成，文字长度变化后要在手机上检查是否挤出屏幕。

## 修改后怎么显示到手机

1. 保存 strings.xml。
2. 在项目目录运行构建命令，见 [BUILD.md](BUILD.md)。
3. 如果这次要作为正式更新，先在 AndroidManifest.xml 将 versionCode 递增，versionName 改成新版本。例如27/0.1.0 → 28/0.1.1。
4. 使用原签名构建后覆盖安装，保留数据；签名不同就不能覆盖。源码包不包含原签名私钥。
5. 手动检查主页、规则页、授权提示、拦截页和三思弹窗。

修改 GitHub 上的 strings.xml 并不会自动修改已经安装的 APK；当前没有远程文案或自动发布服务。

## 不属于普通文案的内容

| 想改什么 | 文件 | 说明 |
| --- | --- | --- |
| 本子上的 You need a GAP. | res/drawable-nodpi/gap_rest.png | 是图片像素，需要编辑图片；保留顶部空墙，倒计时是实时绘制 |
| 桌面图标 | res/drawable/ic_launcher.xml | 白底黑色的矢量图 |
| 提示音 | res/raw/gap_notice.wav | 替换兼容的短WAV，当前约1.41秒 |
| 新安装默认额度 | java/app/gap/clarity/ProductRules.java | 改规则逻辑，不是改文案；已有用户偏好不自动重置 |
| 字号、间距、布局 | MainActivity.java、Ui.java | 改后需要真机检查；本轮未加入可视化拖拽编辑器 |

文案修改不会改变计时规则。不要只把“5分钟”改成“10分钟”就认为实际额度已经变化。
