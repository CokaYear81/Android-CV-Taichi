from __future__ import annotations

from pathlib import Path
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
