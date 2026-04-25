from __future__ import annotations

import argparse
import csv
import json
import math
from pathlib import Path
from typing import Any

POSE17_SCHEMA = "pose17_v1"
POSE17_LANDMARK_COUNT = 17
POSE17_LEFT_SHOULDER = 1
POSE17_RIGHT_SHOULDER = 2
POSE17_LEFT_HIP = 7
POSE17_RIGHT_HIP = 8
DEFAULT_CONFIDENCE_THRESHOLD = 0.5


def normalize_pose17(landmarks: list[dict[str, Any]]) -> list[float]:
    """Normalize one pose17 frame with hip centering and torso scaling."""
    coords = [
        [float(landmark["x"]), float(landmark["y"]), float(landmark["z"])]
        for landmark in landmarks
    ]

    hip_center = [
        (coords[POSE17_LEFT_HIP][axis] + coords[POSE17_RIGHT_HIP][axis]) / 2.0
        for axis in range(3)
    ]
    centered = [
        [coord[axis] - hip_center[axis] for axis in range(3)]
        for coord in coords
    ]

    shoulder_center = [
        (
            centered[POSE17_LEFT_SHOULDER][axis]
            + centered[POSE17_RIGHT_SHOULDER][axis]
        )
        / 2.0
        for axis in range(3)
    ]
    torso_size = math.sqrt(sum(value * value for value in shoulder_center))

    if torso_size > 1e-6:
        centered = [
            [value / torso_size for value in coord]
            for coord in centered
        ]

    return [value for coord in centered for value in coord]


def build_csv_header() -> list[str]:
    header = ["filename", "sample_id", "frame_index", "timestamp_ms"]
    for index in range(POSE17_LANDMARK_COUNT):
        header.extend([f"x{index}", f"y{index}", f"z{index}"])
    header.append("label")
    return header


def load_pose17_sample(input_json: str | Path) -> dict[str, Any]:
    input_path = Path(input_json)
    with input_path.open("r", encoding="utf-8") as file:
        sample = json.load(file)

    schema = sample.get("landmark_schema_version")
    if schema != POSE17_SCHEMA:
        raise ValueError(f"Unsupported schema: {schema}")

    frames = sample.get("frames")
    if not isinstance(frames, list) or not frames:
        raise ValueError("Sample must contain a non-empty frames list")

    return sample


def is_valid_frame(frame: dict[str, Any], threshold: float) -> bool:
    landmarks = frame.get("pose_landmarks")
    if not isinstance(landmarks, list) or len(landmarks) != POSE17_LANDMARK_COUNT:
        return False

    if not frame.get("has_pose", True):
        return False

    visibilities = [landmark.get("visibility", 0.0) for landmark in landmarks]
    average_visibility = sum(visibilities) / len(visibilities)
    return float(average_visibility) >= threshold


def process_one_json(
    input_json: str | Path,
    output_dir: str | Path,
    label: str,
    threshold: float = DEFAULT_CONFIDENCE_THRESHOLD,
) -> Path:
    input_path = Path(input_json)
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    sample = load_pose17_sample(input_path)
    sample_id = sample.get("sample_id") or input_path.stem
    rows: list[list[Any]] = []

    for frame in sample["frames"]:
        if not is_valid_frame(frame, threshold):
            continue

        landmarks = frame["pose_landmarks"]
        normalized_coords = normalize_pose17(landmarks)
        row = [
            input_path.name,
            sample_id,
            int(frame.get("frame_index", -1)),
            int(frame.get("timestamp_ms", -1)),
            *normalized_coords,
            label,
        ]
        rows.append(row)

    output_csv = output_path / f"{sample_id}_normalized.csv"
    with output_csv.open("w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file)
        writer.writerow(build_csv_header())
        writer.writerows(rows)
    return output_csv


def main() -> None:
    parser = argparse.ArgumentParser(description="Normalize one pose17_v1 JSON sample.")
    parser.add_argument("input_json", type=Path)
    parser.add_argument("output_dir", type=Path)
    parser.add_argument("label")
    parser.add_argument("--threshold", type=float, default=DEFAULT_CONFIDENCE_THRESHOLD)
    args = parser.parse_args()

    output_csv = process_one_json(
        input_json=args.input_json,
        output_dir=args.output_dir,
        label=args.label,
        threshold=args.threshold,
    )
    print(f"Saved normalized CSV: {output_csv}")


if __name__ == "__main__":
    main()
