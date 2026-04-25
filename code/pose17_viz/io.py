from __future__ import annotations

from pathlib import Path
import csv
import json
from typing import Any

POSE17_SCHEMA_VERSION = "pose17_v1"
POSE17_LANDMARK_COUNT = 17


def load_sample(path: str | Path) -> dict[str, Any]:
    sample_path = Path(path)
    if not sample_path.exists():
        raise FileNotFoundError(f"Sample not found: {sample_path}")

    with sample_path.open("r", encoding="utf-8") as file:
        sample = json.load(file)

    if sample.get('landmark_schema_version') != POSE17_SCHEMA_VERSION:
        raise ValueError(
            f"Unsupported schema: {sample.get('landmark_schema_version')}"
        )

    frames = sample.get("frames")
    if not isinstance(frames, list) or not frames:
        raise ValueError("Sample must contain a non-empty frames list")

    previous_timestamp = None
    for index, frame in enumerate(frames):
        pose_landmarks = frame.get("pose_landmarks")
        if not isinstance(pose_landmarks, list) or len(pose_landmarks) != POSE17_LANDMARK_COUNT:
            raise ValueError(f"Frame {index} must contain exactly 17 landmarks")

        timestamp_ms = frame.get("timestamp_ms")
        if not isinstance(timestamp_ms, int):
            raise ValueError(f"Frame {index} is missing integer timestamp_ms")

        if previous_timestamp is not None and timestamp_ms <= previous_timestamp:
            raise ValueError(
                f"timestamp_ms must be strictly increasing, failed at frame {index}"
            )
        previous_timestamp = timestamp_ms

    sample["_source_path"] = str(sample_path)
    return sample


def summarize_sample(sample: dict[str, Any]) -> dict[str, Any]:
    frames = sample["frames"]
    timestamps = [frame["timestamp_ms"] for frame in frames]
    visibility_values = [
        point.get("visibility", 0.0)
        for frame in frames
        for point in frame["pose_landmarks"]
    ]
    has_pose_false_count = sum(1 for frame in frames if not frame.get("has_pose", True))

    return {
        "sample_id": sample.get("sample_id"),
        "action_name": sample.get("action_name"),
        "subject_id": sample.get("subject_id"),
        "frame_count": len(frames),
        "duration_ms": timestamps[-1] - timestamps[0] if len(timestamps) > 1 else 0,
        "has_pose_false_count": has_pose_false_count,
        "average_visibility": (
            sum(visibility_values) / len(visibility_values) if visibility_values else 0.0
        ),
        "source_path": sample.get("_source_path"),
    }


def normalized_csv_columns() -> list[str]:
    columns = ["filename", "sample_id", "frame_index", "timestamp_ms"]
    for index in range(POSE17_LANDMARK_COUNT):
        columns.extend([f"x{index}", f"y{index}", f"z{index}"])
    columns.append("label")
    return columns


def load_normalized_csv(path: str | Path) -> dict[str, Any]:
    csv_path = Path(path)
    if not csv_path.exists():
        raise FileNotFoundError(f"CSV not found: {csv_path}")

    expected_columns = normalized_csv_columns()
    with csv_path.open("r", encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file)
        if reader.fieldnames != expected_columns:
            raise ValueError(
                f"Unexpected CSV columns. Expected {len(expected_columns)} columns."
            )
        rows = list(reader)

    return {
        "columns": expected_columns,
        "rows": rows,
        "source_path": str(csv_path),
    }


def summarize_normalized_csv(normalized_csv: dict[str, Any]) -> dict[str, Any]:
    rows = normalized_csv["rows"]
    if not rows:
        return {
            "frame_count": 0,
            "duration_ms": 0,
            "feature_dim": POSE17_LANDMARK_COUNT * 3,
            "labels": [],
            "source_path": normalized_csv.get("source_path"),
        }

    timestamps = [int(row["timestamp_ms"]) for row in rows]
    labels = sorted({str(row["label"]) for row in rows if row.get("label")})

    return {
        "sample_id": rows[0]["sample_id"],
        "frame_count": len(rows),
        "duration_ms": timestamps[-1] - timestamps[0] if len(timestamps) > 1 else 0,
        "feature_dim": POSE17_LANDMARK_COUNT * 3,
        "labels": labels,
        "source_path": normalized_csv.get("source_path"),
    }
