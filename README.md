# Android-CV-Taichi

这是一个基于安卓摄像头进行视觉识别，并尝试对太极动作进行评分的小项目。

项目主要使用 `CameraX` 获取实时画面，通过 `MediaPipe Pose Landmarker` 识别人体关键点，在安卓端完成骨架显示、动作样本采集和关键点数据导出。后续这些数据可以继续用于动作评分、规则分析或模型训练。

## 目录

- [项目说明](#项目说明)
- [时间线](#时间线)
- [如何运行](#如何运行)
- [尾声](#尾声)

## 项目说明

这个项目的核心目标是把安卓手机变成一个轻量的动作采集与分析工具。当前客户端已经可以完成：

- 调用安卓摄像头并显示实时预览
- 使用 `MediaPipe Pose Landmarker` 识别人体姿态
- 在画面上绘制人体骨架
- 采集单段动作样本
- 导出关键点 JSON，便于后续在电脑端分析和处理
- 配合 Python 工具做基础的数据清洗、归一化和可视化

整体思路大致是：

```text
Android Camera
-> Pose Keypoints
-> JSON Export
-> Data Processing
-> Action Scoring / Model Exploration
```

本仓库主要完成的是安卓客户端和数据采集分析链路。模型选择、训练和部署属于后续可以继续推进的部分。

## 时间线

- **Week 1：安卓相机采集基础**
  - 完成 Android 项目搭建，接入 `CameraX Preview` 和 `ImageAnalysis`，并在真机上验证相机预览与分析帧回调。
  - 日志：[001](logs/dev_log_001_2026-03-18.md), [002](logs/dev_log_002_2026-03-18.md), [003](logs/dev_log_003_2026-03-20.md), [004](logs/dev_log_004_2026-03-20.md), [005](logs/dev_log_005_2026-03-20.md)

- **Week 2：姿态识别与采集导出**
  - 接入 `MediaPipe Pose Landmarker`，实现实时骨架 overlay，并跑通 `Start Capture / Stop Capture` 的样本采集流程。
  - 日志：[006](logs/dev_log_006_2026-03-30.md), [007](logs/dev_log_007_2026-03-30.md), [008](logs/dev_log_008_2026-04-02.md)

- **Week 3：关键点格式整理**
  - 将正式采集数据整理为更适合后续训练和分析的 17 点骨架格式，补充 JSON schema，并处理无人体帧的补零逻辑。
  - 日志：[009](logs/dev_log_009_2026-04-08.md)

- **Week 4：兼容性与数据处理工具**
  - 升级 `MediaPipe Tasks Vision`，修复部分 vivo / iQOO 设备兼容问题；同时整理 Python 可视化、归一化脚本和 notebook 工作流。
  - 日志：[010](logs/dev_log_010_2026-04-09.md), [011](logs/dev_log_011_2026-04-09.md), [012](logs/dev_log_012_2026-04-18.md), [013](logs/dev_log_013_2026-04-25.md)

## 如何运行

1. 用 Android Studio 打开本目录：

```text
android_app
```

2. 等待 Gradle Sync 完成。

3. 连接一台开启 USB 调试的安卓手机。

4. 点击 Android Studio 的 `Run`，将 App 安装到真机。

5. 在 App 中输入 `subject_id` 和 `action_name`，点击 `Start Capture` 开始采集，点击 `Stop Capture` 保存样本。

导出的数据可通过 Android Studio 的 Device Explorer 拉取：

```text
data/data/com.lenovo.taichivision/files/captures/
```

其中关键点文件通常在：

```text
files/captures/landmarks/
```

## 尾声

这个项目算是一个在学期中进行尝试的小项目。后续更完整的方向已经由导师继续推进和完善了。

在比较紧张的学期里，我们自己查资料、搭环境、调安卓端、试姿态识别，也一点点把数据采集和分析流程搭了起来。当然也有遗憾：我们主要完成的是安卓客户端侧的数据采集和基础分析工作，模型的具体选取、训练和部署没有来得及再深入推进，orz。

如果继续往动作分类和评分方向做，可以参考 [ZHmQAQ/PoseClassifier](https://github.com/ZHmQAQ/PoseClassifier)。它和我们的工作有不少重合，比如人体关键点、传统动作分类、动作评分等，而且整体完成度更高，可以作为后续模型和评分路线的参考。
