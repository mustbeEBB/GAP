# 上传 GitHub 与发布 APK

本次首个公开版本为 GAP 0.1.0。以下源码、安装包、标签和发布说明统一使用这个版本号。

## 源码上传到仓库文件区

1. 解压 GAP-github-v0.1.0.zip，进入 GAP-github 文件夹。
2. 如果仓库已存在 README 和 .gitignore，以本目录文件为基础合并替换；不要删除 `.git`。
3. GitHub 仓库主页点击 Add file → Upload files。
4. 从文件夹内部选中 `app`、`tests`、`docs`、`build.py`、README 等内容上传；确认仓库根目录直接出现 app/ 和 build.py，而不是多套一层 GAP-github/。
5. `.gitignore`、`.gitattributes` 是点开头文件；资源管理器请显示隐藏项目，网页上传时也要检查是否包含。网页不接受时可使用 GitHub Desktop 或 Git 命令提交。
6. 填写提交说明，例如 `Prepare GAP 0.1.0 initial release`，提交到默认分支。

不要把本次ZIP文件作为唯一源码上传。ZIP用于下载和搬运，GitHub 应能直接浏览 app/ 与 Java 文件。

不要上传整个工作区。本目录已排除 SDK、签名、手机日志、历史APK与临时产物。今后也不要使用包含私钥的目录全选上传。

官方操作：[上传文件](https://docs.github.com/en/repositories/working-with-files/managing-files/adding-a-file-to-a-repository)。如果数量或目录上传不方便，推荐在GitHub Desktop中先Clone已有仓库，再把本目录内部文件复制进去，Review changes后Commit和Push；不要对已有仓库再强制推送或覆盖历史。

## APK 放 Releases

1. 仓库主页进入 Releases → Draft a new release / Create a new release。
2. 创建标签 `v0.1.0`，选择已经提交了上述源码的分支。
3. 标题填 `GAP 0.1.0 · 初版`。
4. 复制同目录 RELEASE-NOTES.md 的内容。
5. 附件上传单独提供的 `gap-0.1.0.apk` 和 SHA256.txt。
6. 检查后 Publish release。用户之后从这个附件下载安装。

官方操作：[管理 Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)。GitHub自动生成的Source code ZIP不是Android安装包。

## 发布后检查

确认 README 在根目录显示、文案和隐私说明可点开、截图能显示、APK附件能下载。当前素材来源见 THIRD_PARTY_NOTICES.md；源码许可尚未由项目所有者选定，不应将第三方素材自动包含在源码许可证中。

当前没有取得你的仓库地址，也没有登录或向远程仓库推送。以上为准备好的上传材料及手动步骤。
