#!/bin/bash
export MSYS_NO_PATHCONV=1
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"; E="$ADB -s emulator-5554"
O=D:/P5E-builds/wt-v4.24/artifacts/qa/v4.24.0-rc1/performance-ab; mkdir -p $O
V421=D:/P5E-builds/wt-v421ab/app/build/outputs/apk/benchmark/TranslateBooks-v4.13-p0-dev-benchmark.apk
RC=D:/P5E-builds/wt-rc1/app/build/outputs/apk/benchmark/TranslateBooks-v4.13-p0-dev-benchmark.apk
MB=D:/P5E-builds/wt-rc1/macrobenchmark/build/outputs/apk/benchmark/macrobenchmark-benchmark.apk
$E uninstall com.ml.tblandroidtxt.macrobenchmark >/dev/null; $E install -r -t $MB | tail -1
for r in 1 2 3; do
 for v in v421 rc; do
  A=$V421; [ $v = rc ] && A=$RC
  $E uninstall com.ml.tblandroidtxt >/dev/null; $E install -r -t $A | tail -1
  $E shell rm -rf /sdcard/Android/media/com.ml.tblandroidtxt.macrobenchmark
  sleep 5
  $E shell am instrument -w -r -e androidx.benchmark.suppressErrors EMULATOR,LOW-BATTERY -e class com.ml.tblandroidtxt.macrobenchmark.StartupMacrobenchmark#coldStartup com.ml.tblandroidtxt.macrobenchmark/androidx.test.runner.AndroidJUnitRunner > $O/$v-round$r.txt 2>&1
  mkdir -p $O/$v-round$r; $E pull /sdcard/Android/media/com.ml.tblandroidtxt.macrobenchmark/. $O/$v-round$r/ >/dev/null
  echo "$v round $r: $(grep -o 'median=[0-9.]*' $O/$v-round$r.txt) $(grep -o 'min=[0-9.]*' $O/$v-round$r.txt) $(grep -o 'max=[0-9.]*' $O/$v-round$r.txt)"
 done
done
echo AB_DONE
