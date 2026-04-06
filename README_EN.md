# Android-CV-Taichi

[中文说明](README.md)

An Android-based Taichi motion capture and visualization project built with the device camera and `MediaPipe Pose`.

At the current stage, the goal is **not** scoring or model training yet. The focus is to make the **mobile-side keypoint capture pipeline** stable and reusable:

- Real-time camera preview
- Human pose keypoint detection
- Skeleton overlay visualization
- Raw video export
- Training-friendly keypoint `JSON` export

---

## Current Progress

### Completed
- `week1`
  - `CameraX Preview`
  - `ImageAnalysis`
  - Real-device run and basic build validation

- `week2`
  - `MediaPipe Pose Landmarker` integration
  - Real-time return of `33` pose landmarks
  - Skeleton overlay creation and alignment fix
  - Mobile capture flow:
    - input `subject_id`
    - input `action_name`
    - `Start Capture / Stop Capture`
  - Export:
    - raw video `mp4`
    - fixed-length keypoint `json`

### Current Project State
This repository is now usable as an **Android keypoint capture tool**, not just a demo.

Already validated:
- Real-time skeleton display
- Capture of one motion segment
- Generation of both files inside the app private directory:
  - `json`
  - `mp4`
- JSON structure that can be read later by Python / PyTorch

### Next Planned Stage
- `week3`
  - choose the first Taichi movement
  - define error labels
  - define a fixed single-camera front-view collection protocol
  - collect and review a first batch of samples

---

## Project Goal

The overall roadmap is:

```text
Android Camera
-> Pose Keypoints
-> JSON / Video Export
-> Python / PyTorch Training
-> Rule-based analysis / model evaluation
-> Later on-device deployment
```

This repository currently covers the **Android capture side** of that pipeline.

---

## Project Structure

```text
android_app/
├── app/
│   ├── src/main/java/com/lenovo/taichivision/
│   │   ├── data/         # JSON data models and file writer
│   │   ├── pose/         # lightweight pose result bundle
│   │   └── ui/           # OverlayView
│   ├── src/main/res/
│   │   └── layout/       # main UI layout
│   └── src/main/assets/  # MediaPipe model file
├── gradle/
└── README.md
```

---

## Requirements

- Android Studio
- Android SDK
- A real Android phone with USB debugging
- A Gradle environment that can download dependencies

Recommended:
- Use a real device instead of an emulator
- Use the rear camera
- Keep the full body inside the frame

---

## How to Run

### 1. Clone the repository
```bash
git clone https://github.com/CokaYear81/Android-CV-Taichi.git
```

### 2. Open the project in Android Studio
Open:

```text
android_app
```

### 3. Sync Gradle
Wait until `Gradle Sync` finishes.

### 4. Connect a phone and run
- Enable Developer Options and USB debugging on the phone
- Connect the device
- Click `Run` in Android Studio

---

## Model File

This project uses `MediaPipe Pose Landmarker`.

Default model file:

```text
app/src/main/assets/pose_landmarker_lite.task
```

If the file is missing after cloning, add the model file according to the official MediaPipe instructions.

---

## How the App Works Right Now

After launch, you will see:
- camera preview
- real-time skeleton overlay
- a small capture input area at the bottom

### Capture Steps
1. Enter `subject_id`
2. Enter `action_name`
3. Tap `Start Capture`
4. Perform the movement
5. Tap `Stop Capture`

### Current Capture Assumptions
- fixed front-view camera setup
- single person
- no separate view label
- saves both:
  - raw video
  - keypoint JSON

---

## Exported File Location

The app saves files inside the app private directory:

```text
files/captures/landmarks/
files/captures/raw_videos/
```

In Android Studio `Device Explorer`, the full path is:

```text
data/data/com.lenovo.taichivision/files/captures/landmarks/
data/data/com.lenovo.taichivision/files/captures/raw_videos/
```

---

## How to Pull Files to Your Computer

In Android Studio:

```text
View -> Tool Windows -> Device Explorer
```

Then open:

```text
data/data/com.lenovo.taichivision/files/captures/
```

Right-click the target `json` or `mp4` and choose:
- `Save As...`
  or
- `Pull`

You can then use the files locally for:
- data inspection
- annotation preparation
- PyTorch training

---

## JSON Format

The current JSON design is intended to be **training-friendly**.

### Top-level fields
- `sample_id`
- `subject_id`
- `action_name`
- `capture_started_at`
- `capture_ended_at`
- `device_id`
- `image_width`
- `image_height`
- `rotation_degrees`
- `video_file`
- `is_standard`
- `error_tags`
- `frames`

### Per-frame fields
- `frame_index`
- `timestamp_ms`
- `has_pose`
- `pose_landmarks`

### Landmark fields
Each frame stores exactly `33` landmarks, and each landmark contains:
- `x`
- `y`
- `z`
- `visibility`

### Important rule
If no human pose is detected in a frame:
- the frame is still kept
- `has_pose=false`
- `pose_landmarks` is padded with `33` zero landmarks

This makes it easy to load the data as:

```text
T x 33 x 4
```

---

## Current Limitations

This is still an early-stage capture tool. It does **not** yet include:

- movement scoring
- rule engine
- PyTorch training code
- Hand Landmarker
- multi-person detection
- automatic action recognition
- ExecuTorch deployment

---

## Collaboration Notes

If you are opening this repo for the first time, the recommended order is:

1. run the Android app
2. confirm the skeleton overlay works
3. complete one `Start Capture -> Stop Capture` cycle
4. confirm that both files are generated:
   - `json`
   - `mp4`
5. then move on to data collection or training-related work

---

## Recommended Next Step

The next useful step is **not** training a large model immediately.  
Instead:

- choose the first Taichi movement
- define standard vs error cases
- fix the collection protocol
- collect a clean first batch of samples

---

## Acknowledgements

- Android CameraX
- MediaPipe Pose Landmarker
- PyTorch (for the later training stage)
