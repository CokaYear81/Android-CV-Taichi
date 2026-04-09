from __future__ import annotations

import numpy as np


def rotate_coords(coords: np.ndarray, rotation: str = "clockwise_90") -> np.ndarray:
    if rotation == "none":
        return coords.copy()

    rotated = np.empty_like(coords)
    if rotation == "clockwise_90":
        rotated[..., 0] = 1.0 - coords[..., 1]
        rotated[..., 1] = coords[..., 0]
        return rotated

    if rotation == "counterclockwise_90":
        rotated[..., 0] = coords[..., 1]
        rotated[..., 1] = 1.0 - coords[..., 0]
        return rotated

    if rotation == "rotate_180":
        rotated[..., 0] = 1.0 - coords[..., 0]
        rotated[..., 1] = 1.0 - coords[..., 1]
        return rotated

    raise ValueError(f"Unsupported rotation: {rotation}")
