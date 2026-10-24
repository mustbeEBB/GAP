"""Portable dependency-free Android builder. Python 3.9+, JDK 17+, SDK API 35.

Examples: python build.py --sdk /path/to/sdk --jdk /path/to/jdk
          python build.py --test-only --jdk /path/to/jdk
Secrets stay outside the repository. See docs/BUILD.md.
"""
import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parent
ANDROID = "{http://schemas.android.com/apk/res/android}"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--sdk", default=os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME"))
    parser.add_argument("--jdk", default=os.environ.get("JAVA_HOME"))
    parser.add_argument("--platform", help="Explicit SDK platform directory containing android.jar")
    parser.add_argument("--tools", help="Explicit SDK build-tools directory")
    parser.add_argument("--build-dir", default=str(ROOT / ".build"))
    parser.add_argument("--output-dir", default=str(ROOT / "dist"))
    parser.add_argument("--keystore", help="Existing private signing key; never commit this file")
    parser.add_argument("--key-alias", default="androiddebugkey")
    parser.add_argument("--test-only", action="store_true")
    args = parser.parse_args()
    suffix = ".exe" if os.name == "nt" else ""
    if args.jdk:
        jdk = Path(args.jdk).expanduser().resolve()
    elif shutil.which("javac"):
        jdk = Path(shutil.which("javac")).resolve().parent.parent
    else:
        parser.error("Install JDK 17+ and set JAVA_HOME, or pass --jdk. See docs/BUILD.md.")

    def java(name):
        path = jdk / "bin" / (name + suffix)
        if not path.is_file():
            parser.error(f"Missing JDK tool: {path}")
        return path

    def run(*command):
        subprocess.run([str(x) for x in command], cwd=ROOT, check=True)

    build = Path(args.build_dir).expanduser().resolve()
    # Do not reuse stale .class files: compilation output starts in an empty child directory.
    build.mkdir(parents=True, exist_ok=True)
    import tempfile
    with tempfile.TemporaryDirectory(prefix="gap-", dir=build) as temporary:
        work = Path(temporary)
        tests = work / "testclasses"
        tests.mkdir()
        package = ROOT / "app/src/main/java/app/gap/clarity"
        test_sources = sorted((ROOT / "tests").glob("*Test.java"))
        logic = [package / (name + ".java") for name in
                 ("BudgetEngine", "PauseGate", "ProductRules", "NavigationState", "TimerSlots")]
        run(java("javac"), "--release", "8", "-encoding", "UTF-8", "-d", tests, *logic, *test_sources)
        for source in test_sources:
            run(java("java"), "-cp", tests, "app.gap.clarity." + source.stem)
        if args.test_only:
            return
        sdk = Path(args.sdk).expanduser().resolve() if args.sdk else None
        if not args.platform and not sdk:
            parser.error("Set ANDROID_SDK_ROOT or pass --sdk; see docs/BUILD.md.")
        platform = Path(args.platform).expanduser().resolve() if args.platform else sdk / "platforms/android-35"
        jar = platform / "android.jar"
        if args.tools:
            tools = Path(args.tools).expanduser().resolve()
        else:
            if not sdk:
                parser.error("Pass --sdk or --tools.")
            versions = [p for p in (sdk / "build-tools").glob("*")
                        if p.is_dir() and re.fullmatch(r"\d+(\.\d+)+", p.name)]
            if not versions:
                parser.error("Install Android SDK Build-Tools (35.0.0 or newer).")
            tools = max(versions, key=lambda p: tuple(int(x) for x in p.name.split(".")))
        required = [jar, tools / ("aapt2" + suffix), tools / ("zipalign" + suffix),
                    tools / "lib/d8.jar", tools / "lib/apksigner.jar"]
        for path in required:
            if not path.is_file():
                parser.error(f"Missing SDK file: {path}. See docs/BUILD.md.")
        for name in ("generated", "classes", "dex"):
            (work / name).mkdir()
        run(tools / ("aapt2" + suffix), "compile", "--dir", ROOT / "app/src/main/res", "-o", work / "resources.zip")
        run(tools / ("aapt2" + suffix), "link", "-o", work / "resources.apk", "--manifest",
            ROOT / "app/src/main/AndroidManifest.xml", "-I", jar, "--java", work / "generated",
            "--auto-add-overlay", work / "resources.zip")
        sources = sorted(package.glob("*.java")) + sorted((work / "generated").rglob("*.java"))
        run(java("javac"), "--release", "8", "-encoding", "UTF-8", "-classpath", jar, "-d", work / "classes", *sources)
        with zipfile.ZipFile(work / "classes.jar", "w") as archive:
            for path in sorted((work / "classes").rglob("*.class")):
                archive.write(path, path.relative_to(work / "classes").as_posix())
        run(java("java"), "-cp", tools / "lib/d8.jar", "com.android.tools.r8.D8", "--lib", jar,
            "--min-api", "26", "--output", work / "dex", work / "classes.jar")
        with zipfile.ZipFile(work / "resources.apk") as source, zipfile.ZipFile(work / "unsigned.apk", "w") as dest:
            for entry in source.infolist():
                dest.writestr(entry, source.read(entry.filename))
            dest.write(work / "dex/classes.dex", "classes.dex")
        run(tools / ("zipalign" + suffix), "-f", "-p", "4", work / "unsigned.apk", work / "aligned.apk")
        if args.keystore:
            key = Path(args.keystore).expanduser().resolve()
            if not key.is_file():
                parser.error("The supplied keystore does not exist. No replacement key was created.")
            if not os.environ.get("GAP_KEYSTORE_PASSWORD"):
                import getpass
                os.environ["GAP_KEYSTORE_PASSWORD"] = getpass.getpass("Keystore password (hidden): ")
                if not os.environ["GAP_KEYSTORE_PASSWORD"]:
                    parser.error("A keystore password is required.")
            password = "env:GAP_KEYSTORE_PASSWORD"
        else:
            key = ROOT / ".local/debug.keystore"
            key.parent.mkdir(exist_ok=True)
            password = "pass:android"
            if not key.exists():
                run(java("keytool"), "-genkeypair", "-keystore", key, "-storepass", "android", "-keypass", "android",
                    "-alias", "androiddebugkey", "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
                    "-dname", "CN=GAP Debug, O=Local Development, C=CN")
            print("Using a LOCAL DEBUG key. It cannot replace an APK signed with another key.")
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        version = manifest.get(ANDROID + "versionName")
        output = Path(args.output_dir).expanduser().resolve()
        output.mkdir(parents=True, exist_ok=True)
        apk = output / f"gap-{version}.apk"
        run(java("java"), "-jar", tools / "lib/apksigner.jar", "sign", "--ks", key, "--ks-pass", password,
            "--ks-key-alias", args.key_alias, "--out", apk, work / "aligned.apk")
        run(java("java"), "-jar", tools / "lib/apksigner.jar", "verify", "--verbose", apk)
        print("APK ready:", apk)


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as error:
        sys.exit(error.returncode)
