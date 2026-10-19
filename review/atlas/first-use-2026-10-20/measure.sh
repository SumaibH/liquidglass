#!/bin/bash
# measure.sh <label> <runs> <tapX> <tapY> <tap2X> <tap2Y>: first-use hitch on the Pixel 7.
# Each run: cold start PhoneActivity, wait, reset gfxinfo, first tap on a tab, dump framestats; then a
# second tap on another tab (warm), dump again. Prints per run: frames, max frame ms, frames over 11.1 ms
# (90 Hz deadline), for the first and the second interaction.
export MSYS_NO_PATHCONV=1
A="D:/DevSetup/Android/Sdk/platform-tools/adb.exe -s 28121FDH2006ZF"
PKG=com.wexpa.liquidglass.sample
label=$1; runs=$2
stats() {  # framestats -> "frames max over"
  python -c "
import sys
lines=sys.stdin.read().splitlines()
head=next(l for l in lines if l.startswith('Flags,')).rstrip(',').split(',')
i0,i1=head.index('IntendedVsync'),head.index('FrameCompleted')
rows=[l.split(',') for l in lines if l and l[0].isdigit() and l.count(',')>10]
d=[(int(r[i1])-int(r[i0]))/1e6 for r in rows if int(r[0])==0 and int(r[i1])>0]
print(len(d), round(max(d),1) if d else 0, sum(1 for x in d if x>11.1))"
}
for i in $(seq 1 $runs); do
  $A shell am force-stop $PKG
  $A shell am start -W -n $PKG/.PhoneActivity >/dev/null
  sleep 3
  $A shell dumpsys gfxinfo $PKG reset >/dev/null
  $A shell input tap $3 $4
  sleep 1.5
  first=$($A shell dumpsys gfxinfo $PKG framestats | tr -d '\r' | stats)
  $A shell dumpsys gfxinfo $PKG reset >/dev/null
  $A shell input tap $5 $6
  sleep 1.5
  second=$($A shell dumpsys gfxinfo $PKG framestats | tr -d '\r' | stats)
  echo "$label run $i  first: $first  second: $second"
done
