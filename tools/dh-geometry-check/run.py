from pathlib import Path
import subprocess
root = Path('E:/Ariamis_src/AriamisDistantHorizons')
out = Path('build/dh-geometry-check'); out.mkdir(exist_ok=True)
cp = (root/'build/dh-checks/classpath.txt').read_text().strip()
java = Path('C:/Program Files/Java/jdk-21/bin')
subprocess.run([str(java/'javac.exe'), '-proc:none', '-cp', cp, '-d', str(out), str(root/'src/main/java/com/seibel/distanthorizons/core/dataObjects/render/bufferBuilding/ColumnBox.java'), 'tools/dh-geometry-check/DhInteriorFaceCheck.java'], check=True)
raise SystemExit(subprocess.run([str(java/'java.exe'), '-cp', str(out)+';'+cp, 'DhInteriorFaceCheck']).returncode)
