"""Compile the production DH height/readiness code against isolated fixtures and test its GL mask."""
import argparse
from pathlib import Path
import subprocess
import sys

parser = argparse.ArgumentParser()
parser.add_argument("--dh-root", type=Path, required=True)
parser.add_argument("--java-home", type=Path, required=True)
parser.add_argument("--lwjgl", type=Path, required=True)
parser.add_argument("--natives", type=Path, required=True)
args = parser.parse_args()
fixtures = Path(__file__).resolve().parent
output = fixtures.parents[1] / "build/dh-height-check"
output.mkdir(parents=True, exist_ok=True)
classpath = (args.dh_root / "build/dh-checks/classpath.txt").read_text().strip() + ";" + str(args.lwjgl)
sources = list(fixtures.rglob("*.java"))
sources += [args.dh_root / ("src/main/java/com/seibel/distanthorizons/" + name) for name in (
    "common/render/openGl/terrain/OptifineVanillaChunkTracker.java",
    "common/render/openGl/terrain/ReadyChunkMask.java",
    "common/wrappers/chunk/CaveAbyssCompat.java",
)]
commands = [
    [str(args.java_home / "bin/javac.exe"), "-proc:none", "-cp", classpath, "-d", str(output)] + list(map(str, sources)),
    [str(args.java_home / "bin/java.exe"), "-cp", str(output) + ";" + classpath, "DhHeightReadinessCheck"],
    [str(args.java_home / "bin/java.exe"), "-Dorg.lwjgl.librarypath=" + str(args.natives),
     "-cp", str(output) + ";" + classpath,
     "com.seibel.distanthorizons.common.render.openGl.terrain.DhHeightMaskGlCheck", str(args.dh_root)],
]
for command in commands:
    result = subprocess.run(command)
    if result.returncode:
        sys.exit(result.returncode)
