# 构建和签名

## 准备环境

安装 Python 3.9+、JDK 17+、Android SDK Platform 35（Android 15）和 Build-Tools 35.0.0 或更新正式版。可在 Android Studio 的 SDK Manager 安装后查看 Android SDK Location。Android Studio 自带的 jbr 可作为 JDK。

这是原生 Java + Android SDK 项目，目前使用 Python 调用 SDK 工具构建，不是 Gradle 项目。Android Studio 可以浏览和编辑文件，不要假定其 Run 按钮已经配置完成。

## Windows 构建

在项目根目录打开 PowerShell：

```powershell
py build.py --sdk "C:\你的SDK目录" --jdk "C:\你的JDK目录"
```

也可设置 ANDROID_SDK_ROOT、JAVA_HOME 后运行 `py build.py`。macOS/Linux 对应使用 `python3 build.py`。构建脚本检查依赖并报告缺少的文件；不会把 SDK 下载或打包进仓库。

构建自动运行159条断言，再执行资源编译、Java编译、DEX、对齐、签名及验签。输出 APK 在 `dist/`。临时文件在 `.build/`，每次独立构建，不复用旧 class。

只需检查规则时：

```powershell
py build.py --test-only --jdk "C:\你的JDK目录"
```

## 签名：覆盖安装最容易卡住的地方

默认首次构建会在 `.local/debug.keystore` 生成本机开发密钥。这是新的本机签名；它和原安装包的签名不同，不能直接覆盖原安装。源码文件和 GitHub 上的 APK 都不能导出原私钥。

若需要为已经安装的版本持续更新，使用原签名文件和原 alias。不要将签名文件或密码传到 GitHub。

```powershell
py build.py --sdk "你的SDK目录" --jdk "你的JDK目录" --keystore "仓库外的签名文件路径" --key-alias "原密钥alias"
```

构建时会隐藏输入地询问密码，不将密码写入命令历史或源码。自动化环境也可以通过 GAP_KEYSTORE_PASSWORD 环境变量传入。

当前已确认初版的原签名保存在本机旧工作目录的 `work/signing/demo.jks`，alias 为 `pianke-demo`；仅供项目所有者本机继续构建，实际密码单独保管，本发布目录不包含私钥或密码。不依赖旧目录也能构建，但会使用新的开发签名。

当前 versionName 为0.1.0，versionCode 为27。版本在 `app/src/main/AndroidManifest.xml`，发布更新时 versionCode 必须递增；包名不能随意变动。签名和包名一致才可保留安装数据覆盖更新。

## 常见问题

- 找不到 javac：传入 JDK 目录，不是只有 java 的 JRE 目录。
- 找不到 android.jar：SDK Manager 安装 API 35，确认 SDK 路径。
- 找不到 aapt2/d8：安装 Build-Tools。特殊安装可使用 `--platform` 和 `--tools` 显式指定路径。
- 提示签名冲突：使用原密钥重新构建；不要直接卸载已有应用以免丢失数据。
- XML 编译报错：检查 strings.xml 标签、引号、`&` 和 `<` 是否正确转义。
- 构建通过但无障碍故障：属于运行问题，记录机型、系统和日志，在 Issues 提交；编译通过不代表后台保活稳定。
