#!/bin/bash
# sxs3.sh <reference.mp4> <before-frames> <after-frames> <out.mp4>  — local only (private reference on top)
REF=$1; A=$2; B=$3; OUT=$4
FONT="C\:/Windows/Fonts/segoeui.ttf"
L="drawtext=fontfile='$FONT':x=12:y=10:fontsize=18:fontcolor=white@0.9:box=1:boxcolor=black@0.55"
ffmpeg -v error -y -i "$REF" -framerate 60 -i "$A/f%05d.png" -framerate 60 -i "$B/f%05d.png" -filter_complex "\
[0:v]fps=60,scale=780:468,setpts=PTS-STARTPTS,$L:text='iOS 26 reference (private recording)',drawtext=fontfile='$FONT':text='%{pts\:hms}':x=w-130:y=10:fontsize=18:fontcolor=white@0.85[a];\
[1:v]scale=780:468,setpts=PTS-STARTPTS,$L:text='BEFORE  efcc3b5 Calm (Codex handoff)'[b];\
[2:v]scale=780:468,setpts=PTS-STARTPTS,$L:text='AFTER  2026-10-13 Calm  (deterministic 60 fps, inferred input)'[c];\
[a][b][c]vstack=inputs=3[v]" -map "[v]" -r 60 -c:v libx264 -pix_fmt yuv420p -crf 18 -shortest "$OUT"
