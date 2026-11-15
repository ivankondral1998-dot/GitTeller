#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TMP="${TMPDIR:-/tmp}/gitteller-test-$$"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP"
javac -encoding UTF-8 -d "$TMP" "$ROOT"/tests-local/org/json/*.java \
  "$ROOT"/app/src/main/java/app/unocontrol/UiMode.java "$ROOT"/app/src/main/java/app/unocontrol/Coverage.java "$ROOT"/app/src/main/java/app/unocontrol/ProjectSearch.java "$ROOT"/app/src/main/java/app/unocontrol/DrawerMotion.java "$ROOT"/app/src/main/java/app/unocontrol/AtomicCipherFile.java "$ROOT"/app/src/main/java/app/unocontrol/EventFeed.java "$ROOT"/app/src/main/java/app/unocontrol/Summary.java "$ROOT"/app/src/main/java/app/unocontrol/ChangeSet.java "$ROOT"/app/src/main/java/app/unocontrol/CommitExplainer.java "$ROOT"/app/src/main/java/app/unocontrol/HumanReport.java "$ROOT"/app/src/main/java/app/unocontrol/GithubClient.java "$ROOT"/app/src/main/java/app/unocontrol/SnapshotCodec.java \
  "$ROOT"/tests-local/app/unocontrol/CoverageSearchSmoke.java "$ROOT"/tests-local/app/unocontrol/UiModeSmoke.java "$ROOT"/tests-local/app/unocontrol/Smoke.java "$ROOT"/tests-local/app/unocontrol/GithubAuditSmoke.java "$ROOT"/tests-local/app/unocontrol/AuditRegression1081.java "$ROOT"/tests-local/app/unocontrol/IndependentProbe109.java
java -cp "$TMP" app.unocontrol.CoverageSearchSmoke
java -cp "$TMP" app.unocontrol.UiModeSmoke
java -cp "$TMP" app.unocontrol.Smoke
java -cp "$TMP" app.unocontrol.GithubAuditSmoke
java -cp "$TMP" app.unocontrol.AuditRegression1081
java -cp "$TMP" app.unocontrol.IndependentProbe109
javac -encoding UTF-8 -d "$TMP" "$ROOT"/tests-local/ParseOnly.java
find "$ROOT/app/src" -name '*.java' -print | sort | xargs java -cp "$TMP" ParseOnly
