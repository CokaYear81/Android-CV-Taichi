# Android-CV-Taichi

[English README](README_EN.md)

An Android camera-based motion capture and visualization project built on `MediaPipe Pose`.

At the current stage, the project has moved beyond simply stabilizing the mobile capture pipeline. The current focus is now:

- keep the mobile keypoint capture pipeline stable
- use `pose17_v1` as the formal sample format
- build the first trainable dataset around **Baduanjin preparation posture**
- prepare for a later `PyTorch + GRU` binary classification baseline

---

## Current Progress

### Completed
- `week1`
  - `CameraX Preview`
  - `ImageAnalysis`
  - real-device run and basic build validation

- `week2`
  - `MediaPipe Pose Landmarker` integration
  - real-time pose inference pipeline
  - skeleton overlay creation and alignment fix
  - mobile capture flow:
    - input `subject_id`
    - input `action_name`
    - `Start Capture / Stop Capture`
  - export of both:
    - raw video `mp4`
    - training-friendly keypoint `json`

- `week3` already completed so far
  - formal pose format switched from `33` landmarks to `17`
  - new top-level field:
    - `landmark_schema_version = "pose17_v1"`
  - overlay now displays only the `17`-landmark skeleton
  - export logic now writes fixed `17` landmarks and still keeps zero-padded no-pose frames

  - `week4` (Completed)
  - **Core Dependency Upgrade**: Upgraded `MediaPipe Tasks Vision` to **`0.10.14`**.
    - Resolved the native `SIGSEGV` crash issues associated with Android 15 kernels (specifically addressing the 16KB Page Size alignment requirement).
    - Improved stability on the latest devices from vivo, iQOO, and others.

### Current Project State
This repository is now usable as an **Android keypoint capture tool**, not just a demo.

Already validated:
- real-time skeleton display
- capture of one motion segment
- generation of both files inside the app private directory:
  - `json`
  - `mp4`
- formal sample format:
  - `pose17_v1`
  - `T x 17 x 4`

### Current Week 3 Goal
The formal week 3 target is now:

- build the first trainable dataset for **Baduanjin preparation posture**
- fix the first learning task as:
  - **standard / non-standard binary classification**
- focus on:
  - sample collection
  - label definition
  - dataset organization
  - training preparation

---

## Current Project Roadmap

```text
Android Camera
-> Pose Keypoints
-> pose17_v1 JSON / Video Export
-> Dataset / Labels / Splits
-> Python / PyTorch Training
-> GRU Baseline
-> Later Rule-based Analysis / On-device Deployment
```

This repository currently covers the **Android capture side** of the pipeline and now starts to support the training-preparation stage.

---

## Project Structure

```text
android_app/
├── app/
│   ├── src/main/java/com/lenovo/taichivision/
│   │   ├── data/         # JSON models and file writer
│   │   ├── pose/         # pose result objects and 33 -> 17 landmark subset logic
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
- a real Android phone with USB debugging
- a Gradle environment that can download dependencies

Recommended:
- use a real device instead of an emulator
- use the rear camera
- keep the full body inside the frame

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
- enable Developer Options and USB debugging on the phone
- connect the device
- click `Run` in Android Studio

---

## Model File

This project uses `MediaPipe Pose Landmarker`.

Default model file:

```text
app/src/main/assets/pose_landmarker_lite.task
```

If the file is missing after cloning, add it according to the official MediaPipe instructions.

---

## How the App Works Right Now

After launch, you will see:
- camera preview
- real-time skeleton overlay
- a small capture input area at the bottom

### Capture Steps
1. enter `subject_id`
2. enter `action_name`
3. tap `Start Capture`
4. perform the movement
5. tap `Stop Capture`

### Current Capture Assumptions
- fixed front-view camera setup
- single person
- full body inside the frame
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

You can then save the files locally.

---

## JSON Format

The current formal JSON design is intended to be **training-friendly**.

### Top-level fields
- `sample_id`
- `landmark_schema_version`
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
Each frame stores exactly `17` landmarks, and each landmark contains:
- `x`
- `y`
- `z`
- `visibility`

### Formal schema
- `landmark_schema_version = "pose17_v1"`

### Landmark order
1. `nose`
2. `left_shoulder`
3. `right_shoulder`
4. `left_elbow`
5. `right_elbow`
6. `left_wrist`
7. `right_wrist`
8. `left_hip`
9. `right_hip`
10. `left_knee`
11. `right_knee`
12. `left_ankle`
13. `right_ankle`
14. `left_heel`
15. `right_heel`
16. `left_foot_index`
17. `right_foot_index`

### Important rule
If no pose is detected in a frame:
- the frame is still kept
- `has_pose=false`
- `pose_landmarks` is padded with `17` zero landmarks

This makes it easy to load the data as:

```text
T x 17 x 4
```

---

## Current Week 3 Training Preparation Target

The formal week 3 target is fixed as:
- action:
  - `baduanjin_preparation`
- first learning task:
  - `is_standard` binary classification
- first model direction:
  - `GRU`
- first training window:
  - `32 x 17 x 4`

Week 3 is not about achieving strong model accuracy yet. It is about finishing:
- sample collection
- label definition
- subject-wise dataset split
- minimum training pipeline readiness

---

## Current Limitations

The current version still does **not** include:
- movement scoring
- rule engine
- full `PyTorch` training code
- `Hand Landmarker`
- multi-person detection
- automatic action recognition
- `ExecuTorch` deployment

---

## Recommended Next Step

The most useful next step is not training a large model immediately.  
Instead:

- collect the first formal batch for Baduanjin preparation
- build a label table
- split data by `subject_id` into train / val / test
- run a first `PyTorch` `GRU` binary classification baseline

---

## Acknowledgements

- Android CameraX
- MediaPipe Pose Landmarker
- PyTorch (for the later training stage)
