import json
import os

import numpy as np
import pandas as pd

# --- Configuration ---
DATA_DIR = "../data"  # Root directory containing action folders
OUTPUT_DIR ="../outputfile"
OUTPUT_CSV = os.path.join(OUTPUT_DIR, "baduanjin_normalized.csv")
CONFIDENCE_THRESHOLD = 0.5  # Minimum average visibility required to keep a frame
POSE17_SCHEMA = "pose17_v1"
POSE17_LANDMARK_COUNT = 17
POSE17_LEFT_SHOULDER = 1
POSE17_RIGHT_SHOULDER = 2
POSE17_LEFT_HIP = 7
POSE17_RIGHT_HIP = 8

# Folder names for each action class
CLASSES = [
    '0_ready',        # Pre-exercise stance
    '1_lift_sky',     # 两手托天理三焦
    '2_archery',      # 左右开弓似射雕
    '3_single_arm',   # 调理脾胃须单举
    '4_look_back',    # 五劳七伤往后瞧
    '5_wag_tail',     # 摇头摆尾去心火
    '6_touch_feet',   # 两手攀足固肾腰
    '7_clench_fist',  # 攒拳怒目增气力
    '8_shaking',      # 背后七颠百病消
    'negative'        # Non-action / background / wrong movements
]


def normalize_pose17(landmarks):
    """
    Normalize one pose17 frame: centralization + torso scaling.
    landmarks: list of 17 points, each has x/y/z/visibility
    """
    coords = np.array([[lm["x"], lm["y"], lm["z"]] for lm in landmarks], dtype=np.float32)

    hip_center = (coords[POSE17_LEFT_HIP] + coords[POSE17_RIGHT_HIP]) / 2.0
    coords -= hip_center

    shoulder_center = (coords[POSE17_LEFT_SHOULDER] + coords[POSE17_RIGHT_SHOULDER]) / 2.0
    torso_size = np.linalg.norm(shoulder_center)

    if torso_size > 1e-6:
        coords /= torso_size

    return coords.flatten()


def process_all_jsons():
    def process_all_jsons():
    # 自动检查并创建输出目录
        if not os.path.exists(OUTPUT_DIR):
            os.makedirs(OUTPUT_DIR)
            print(f"Created directory: {OUTPUT_DIR}")
        
    dataset = []

    for label in CLASSES:
        folder_path = os.path.join(DATA_DIR, label)
        if not os.path.exists(folder_path):
            print(f"Warning: Directory not found: {folder_path}, skipping...")
            continue

        print(f"Processing action: {label}")

        for file_name in os.listdir(folder_path):
            if not file_name.endswith(".json"):
                continue

            file_path = os.path.join(folder_path, file_name)
            with open(file_path, "r", encoding="utf-8") as file:
                try:
                    data = json.load(file)
                except Exception as exc:
                    print(f"Failed to parse {file_name}: {exc}")
                    continue

            if data.get("landmark_schema_version") != POSE17_SCHEMA:
                print(f"Skip {file_name}: unsupported schema {data.get('landmark_schema_version')}")
                continue

            frames = data.get("frames", [])
            if not isinstance(frames, list) or not frames:
                print(f"Skip {file_name}: empty or invalid frames")
                continue

            sample_id = data.get("sample_id", os.path.splitext(file_name)[0])

            for frame in frames:
                landmarks = frame.get("pose_landmarks", [])
                if not isinstance(landmarks, list) or len(landmarks) != POSE17_LANDMARK_COUNT:
                    continue

                if not frame.get("has_pose", True):
                    continue

                visibilities = [lm.get("visibility", 0.0) for lm in landmarks]
                if float(np.mean(visibilities)) < CONFIDENCE_THRESHOLD:
                    continue

                norm_coords = normalize_pose17(landmarks)

                row = [
                    file_name,
                    sample_id,
                    int(frame.get("frame_index", -1)),
                    int(frame.get("timestamp_ms", -1)),
                ] + norm_coords.tolist() + [label]
                dataset.append(row)

    header = ["filename", "sample_id", "frame_index", "timestamp_ms"]
    for i in range(POSE17_LANDMARK_COUNT):
        header.extend([f"x{i}", f"y{i}", f"z{i}"])
    header.append("label")

    df = pd.DataFrame(dataset, columns=header)
    df.to_csv(OUTPUT_CSV, index=False)
    print("\nNormalization complete!")
    print(f"Total frames processed: {len(df)}")
    print(f"File saved to: {OUTPUT_CSV}")


if __name__ == "__main__":
    process_all_jsons()
