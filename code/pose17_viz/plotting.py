from __future__ import annotations

from typing import Any

from .io import POSE17_LANDMARK_COUNT
from .transform import rotate_coords, rotate_normalized_coords

POSE17_CONNECTIONS = [
    (0, 1), (0, 2),
    (1, 3), (3, 5),
    (2, 4), (4, 6),
    (1, 2),
    (1, 7), (2, 8),
    (7, 8),
    (7, 9), (9, 11), (11, 13), (13, 15),
    (8, 10), (10, 12), (12, 14), (14, 16),
]


def _sample_to_arrays(sample: dict[str, Any]) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    import numpy as np

    coords = []
    visibilities = []
    timestamps = []

    for frame in sample["frames"]:
        points = frame["pose_landmarks"]
        coords.append([[point["x"], point["y"]] for point in points])
        visibilities.append([point.get("visibility", 0.0) for point in points])
        timestamps.append(frame["timestamp_ms"])

    return (
        np.asarray(coords, dtype=np.float32),
        np.asarray(visibilities, dtype=np.float32),
        np.asarray(timestamps, dtype=np.int64),
    )


def _format_timestamp(timestamp_ms: int) -> str:
    minutes = timestamp_ms // 60000
    seconds = (timestamp_ms % 60000) // 1000
    milliseconds = timestamp_ms % 1000
    return f"{minutes:02d}:{seconds:02d}.{milliseconds:03d}"


def _normalized_csv_to_arrays(normalized_csv: dict[str, Any]):
    import numpy as np

    rows = normalized_csv["rows"]
    coords = []
    for index in range(POSE17_LANDMARK_COUNT):
        coords.append([
            [
                float(row[f"x{index}"]),
                float(row[f"y{index}"]),
                float(row[f"z{index}"]),
            ]
            for row in rows
        ])

    stacked = np.stack([np.asarray(item, dtype=np.float32) for item in coords], axis=1)
    timestamps = np.asarray([int(row["timestamp_ms"]) for row in rows], dtype=np.int64)
    return stacked, timestamps


def _set_normalized_limits(axis, coords: np.ndarray) -> None:
    import numpy as np

    xy = coords[..., :2].reshape(-1, 2)
    finite_xy = xy[np.isfinite(xy).all(axis=1)]
    if finite_xy.size == 0:
        axis.set_xlim(-1.0, 1.0)
        axis.set_ylim(1.0, -1.0)
        return

    min_xy = finite_xy.min(axis=0)
    max_xy = finite_xy.max(axis=0)
    span = max(max_xy[0] - min_xy[0], max_xy[1] - min_xy[1], 1.0)
    center = (min_xy + max_xy) / 2.0
    padding = span * 0.15
    half_span = span / 2.0 + padding
    axis.set_xlim(center[0] - half_span, center[0] + half_span)
    axis.set_ylim(center[1] + half_span, center[1] - half_span)


def preview_frames(
    sample: dict[str, Any],
    rotation: str = "clockwise_90",
    point_size: int = 36,
    line_width: float = 2.0,
):
    import matplotlib.pyplot as plt

    coords, visibilities, timestamps = _sample_to_arrays(sample)
    display_coords = rotate_coords(coords, rotation=rotation)
    frame_count = display_coords.shape[0]
    preview_indices = sorted({0, frame_count // 2, frame_count - 1})

    fig, axes = plt.subplots(1, len(preview_indices), figsize=(5 * len(preview_indices), 5))
    if len(preview_indices) == 1:
        axes = [axes]

    for axis, frame_index in zip(axes, preview_indices):
        axis.set_title(f"Frame {frame_index} | {_format_timestamp(int(timestamps[frame_index]))}")
        axis.set_xlim(0.0, 1.0)
        axis.set_ylim(1.0, 0.0)
        axis.set_aspect("equal")
        axis.grid(True, alpha=0.2)

        points = display_coords[frame_index]
        visibility = visibilities[frame_index]
        valid_mask = visibility > 0.0

        axis.scatter(points[valid_mask, 0], points[valid_mask, 1], s=point_size)
        for start_idx, end_idx in POSE17_CONNECTIONS:
            if valid_mask[start_idx] and valid_mask[end_idx]:
                axis.plot(
                    [points[start_idx, 0], points[end_idx, 0]],
                    [points[start_idx, 1], points[end_idx, 1]],
                    linewidth=line_width,
                )

    plt.tight_layout()
    return fig


def preview_normalized_csv(
    dataframe: pd.DataFrame,
    rotation: str = "none",
    point_size: int = 36,
    line_width: float = 2.0,
):
    import matplotlib.pyplot as plt

    coords, timestamps = _normalized_csv_to_arrays(dataframe)
    display_coords = rotate_normalized_coords(coords, rotation=rotation)
    frame_count = display_coords.shape[0]
    preview_indices = sorted({0, frame_count // 2, frame_count - 1})

    fig, axes = plt.subplots(1, len(preview_indices), figsize=(5 * len(preview_indices), 5))
    if len(preview_indices) == 1:
        axes = [axes]

    for axis, frame_index in zip(axes, preview_indices):
        axis.set_title(f"Frame {frame_index} | {_format_timestamp(int(timestamps[frame_index]))}")
        axis.set_aspect("equal")
        axis.grid(True, alpha=0.2)
        _set_normalized_limits(axis, display_coords)

        points = display_coords[frame_index]
        axis.scatter(points[:, 0], points[:, 1], s=point_size)
        for start_idx, end_idx in POSE17_CONNECTIONS:
            axis.plot(
                [points[start_idx, 0], points[end_idx, 0]],
                [points[start_idx, 1], points[end_idx, 1]],
                linewidth=line_width,
            )

    plt.tight_layout()
    return fig


def animate_sample(
    sample: dict[str, Any],
    rotation: str = "clockwise_90",
    interval_ms: int = 33,
    frame_step: int = 1,
    point_size: int = 36,
    line_width: float = 2.0,
):
    import matplotlib.pyplot as plt
    import numpy as np
    from IPython.display import HTML
    from matplotlib.animation import FuncAnimation

    coords, visibilities, timestamps = _sample_to_arrays(sample)
    display_coords_full = rotate_coords(coords, rotation=rotation)
    display_indices = np.arange(0, display_coords_full.shape[0], frame_step, dtype=np.int32)
    display_coords = display_coords_full[display_indices]
    display_visibilities = visibilities[display_indices]
    display_timestamps = timestamps[display_indices]

    fig, ax = plt.subplots(figsize=(6, 6))
    ax.set_xlim(0.0, 1.0)
    ax.set_ylim(1.0, 0.0)
    ax.set_aspect("equal")
    ax.grid(True, alpha=0.2)
    ax.set_title("pose17_v1 Skeleton Animation")

    scatter = ax.scatter([], [], s=point_size, color="tab:blue")
    line_artists = [
        ax.plot([], [], linewidth=line_width, color="tab:orange")[0]
        for _ in POSE17_CONNECTIONS
    ]
    time_text = ax.text(
        0.98,
        0.04,
        "00:00.000",
        transform=ax.transAxes,
        ha="right",
        va="bottom",
        fontsize=11,
        bbox={"facecolor": "white", "alpha": 0.8, "edgecolor": "none"},
    )

    def init():
        scatter.set_offsets(np.empty((0, 2)))
        for line in line_artists:
            line.set_data([], [])
        time_text.set_text("00:00.000")
        return [scatter, *line_artists, time_text]

    def update(frame_idx: int):
        points = display_coords[frame_idx]
        visibility = display_visibilities[frame_idx]
        valid_mask = visibility > 0.0

        if valid_mask.any():
            scatter.set_offsets(points[valid_mask])
        else:
            scatter.set_offsets(np.empty((0, 2)))

        for line, (start_idx, end_idx) in zip(line_artists, POSE17_CONNECTIONS):
            if valid_mask[start_idx] and valid_mask[end_idx]:
                line.set_data(
                    [points[start_idx, 0], points[end_idx, 0]],
                    [points[start_idx, 1], points[end_idx, 1]],
                )
            else:
                line.set_data([], [])

        time_text.set_text(_format_timestamp(int(display_timestamps[frame_idx])))
        return [scatter, *line_artists, time_text]

    animation = FuncAnimation(
        fig,
        update,
        frames=len(display_indices),
        init_func=init,
        interval=interval_ms,
        blit=True,
        repeat=True,
    )
    plt.close(fig)
    return HTML(animation.to_jshtml())


def animate_normalized_csv(
    dataframe: pd.DataFrame,
    rotation: str = "none",
    interval_ms: int = 33,
    frame_step: int = 1,
    point_size: int = 36,
    line_width: float = 2.0,
):
    import matplotlib.pyplot as plt
    import numpy as np
    from IPython.display import HTML
    from matplotlib.animation import FuncAnimation

    coords, timestamps = _normalized_csv_to_arrays(dataframe)
    display_coords_full = rotate_normalized_coords(coords, rotation=rotation)
    display_indices = np.arange(0, display_coords_full.shape[0], frame_step, dtype=np.int32)
    display_coords = display_coords_full[display_indices]
    display_timestamps = timestamps[display_indices]

    fig, ax = plt.subplots(figsize=(6, 6))
    ax.set_aspect("equal")
    ax.grid(True, alpha=0.2)
    ax.set_title("pose17_v1 Normalized Skeleton Animation")
    _set_normalized_limits(ax, display_coords)

    scatter = ax.scatter([], [], s=point_size, color="tab:blue")
    line_artists = [
        ax.plot([], [], linewidth=line_width, color="tab:orange")[0]
        for _ in POSE17_CONNECTIONS
    ]
    time_text = ax.text(
        0.98,
        0.04,
        "00:00.000",
        transform=ax.transAxes,
        ha="right",
        va="bottom",
        fontsize=11,
        bbox={"facecolor": "white", "alpha": 0.8, "edgecolor": "none"},
    )

    def init():
        scatter.set_offsets(np.empty((0, 2)))
        for line in line_artists:
            line.set_data([], [])
        time_text.set_text("00:00.000")
        return [scatter, *line_artists, time_text]

    def update(frame_idx: int):
        points = display_coords[frame_idx]
        scatter.set_offsets(points[:, :2])

        for line, (start_idx, end_idx) in zip(line_artists, POSE17_CONNECTIONS):
            line.set_data(
                [points[start_idx, 0], points[end_idx, 0]],
                [points[start_idx, 1], points[end_idx, 1]],
            )

        time_text.set_text(_format_timestamp(int(display_timestamps[frame_idx])))
        return [scatter, *line_artists, time_text]

    animation = FuncAnimation(
        fig,
        update,
        frames=len(display_indices),
        init_func=init,
        interval=interval_ms,
        blit=True,
        repeat=True,
    )
    plt.close(fig)
    return HTML(animation.to_jshtml())
