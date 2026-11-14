#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TMP="${TMPDIR:-/tmp}/gitteller-test-$$"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP"
javac -encoding UTF-8 -d "$TMP" "$ROOT"/tests-local/org/json/*.java \
  "$ROOT"/app/src/main/java/app/unocontrol/Summary.java "$ROOT"/app/src/main/java/app/unocontrol/CommitExplainer.java "$ROOT"/app/src/main/java/app/unocontrol/HumanReport.java "$ROOT"/app/src/main/java/app/unocontrol/GithubClient.java \
  "$ROOT"/tests-local/app/unocontrol/Smoke.java "$ROOT"/tests-local/app/unocontrol/GithubAuditSmoke.java
java -cp "$TMP" app.unocontrol.Smoke
java -cp "$TMP" app.unocontrol.GithubAuditSmoke
javac -encoding UTF-8 -d "$TMP" "$ROOT"/tests-local/ParseOnly.java
find "$ROOT/app/src" -name '*.java' -print | sort | xargs java -cp "$TMP" ParseOnly
