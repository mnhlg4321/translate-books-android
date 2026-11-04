#!/bin/bash
# Cold-start A/B on emulator-5554: v4.24.1 vs 4.25.0 benchmark variants, same macrobenchmark APK, alternating, 3 rounds x 5 iterations.
export MSYS_NO_PATHCONV=1
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"; E="$ADB -s emulator-5554"
O=D:/P5E-builds/wt-v4.25/artifacts/qa/v4.25.0/performance-ab; mkdir -p $O
T=D:/P5E-builds/qa-v4250-traces; mkdir -p $T
OLD=D:/P5E-builds/bench-v4241-tag/app/build/outputs/apk/benchmark/TranslateBooks-v4.13-p0-dev-benchmark.apk
NEW=D:/P5E-builds/verify-rel-v4250-e014f8c9/app/build/outputs/apk/benchmark/TranslateBooks-v4.13-p0-dev-benchmark.apk
MB=D:/P5E-builds/verify-rel-v4250-e014f8c9/macrobenchmark/build/outputs/apk/benchmark/macrobenchmark-benchmark.apk
$E uninstall com.ml.tblandroidtxt.macrobenchmark >/dev/null; $E install -r -t $MB | tail -1
for r in 1 2 3; do
 for v in v4241 v4250; do
  A=$OLD; [ $v = v4250 ] && A=$NEW
  $E uninstall com.ml.tblandroidtxt >/dev/null; $E install -r -t $A | tail -1
  $E shell rm -rf /sdcard/Android/media/com.ml.tblandroidtxt.macrobenchmark
  sleep 5
  $E shell am instrument -w -r -e androidx.benchmark.suppressErrors EMULATOR,LOW-BATTERY -e class com.ml.tblandroidtxt.macrobenchmark.StartupMacrobenchmark#coldStartup com.ml.tblandroidtxt.macrobenchmark/androidx.test.runner.AndroidJUnitRunner > $O/$v-round$r.txt 2>&1
  mkdir -p $T/$v-round$r; $E pull /sdcard/Android/media/com.ml.tblandroidtxt.macrobenchmark/. $T/$v-round$r/ >/dev/null
  echo "$v round $r: $(grep -o 'median=[0-9.]*' $O/$v-round$r.txt | head -1) $(grep -o 'min=[0-9.]*' $O/$v-round$r.txt | head -1) $(grep -o 'max=[0-9.]*' $O/$v-round$r.txt | head -1)"
 done
done
echo AB_DONE
