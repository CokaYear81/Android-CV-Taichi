# Android-CV-Taichi

一个基于安卓摄像头与 `MediaPipe Pose` 的动作采集与可视化项目，当前重点是稳定完成**手机端关键点采集、骨架显示、样本导出**，为后续 `PyTorch` 训练做准备。


---

## 当前状态

### 已完成
- `week1`
  - `CameraX Preview`
  - `ImageAnalysis`
  - 真机运行与基础构建验证

- `week2`
  - `MediaPipe Pose Landmarker` 接入
  - 实时姿态识别链路跑通
  - 骨架 overlay 创建与对齐修复
  - 采集流程跑通：
    - 输入 `subject_id`
    - 输入 `action_name`
    - `Start Capture / Stop Capture`
  - 同步导出：
    - 原始视频 `mp4`
    - 训练友好的关键点 `json`

- `week3`
  - 正式关键点格式从 `33` 点切换到 `17` 点
  - 新增顶层字段：
    - `landmark_schema_version = "pose17_v1"`
  - overlay 改为只显示 `17` 点骨架
  - 导出逻辑改为固定 `17` 点，且无 pose 帧仍补零保留

- `week4`
  - 核心依赖升级：`MediaPipe Tasks Vision -> 0.10.14`
  - 针对新款 vivo / iQOO 等设备的运行兼容性做了修复与验证
  - 当前版本已经完成本地联调，并确认软件可正常运行

### 当前项目定位
当前仓库已经不只是演示 Demo，而是一个可用于：
- 实时显示人体骨架
- 采集单段动作样本
- 同步导出 `json + mp4`
- 为后续 `PyTorch + GRU` 训练准备数据
- 在本地 notebook 中快速检查 `pose17_v1` 样本质量与时间轴

---

## 当前技术路线

```text
Android Camera
-> Pose Keypoints
-> pose17_v1 JSON / Video Export
-> Dataset / Labels / Splits
-> Python / PyTorch Training
-> GRU Baseline
-> Later Rule-based Analysis / On-device Deployment
```

当前仓库主要覆盖这条链路中的**安卓采集端**。

---

## 项目目录说明

```text
android_app/
├── app/
│   ├── src/main/java/com/lenovo/taichivision/
│   │   ├── data/         # JSON 数据模型与写文件工具
│   │   ├── pose/         # Pose 结果对象与 17 点格式处理
│   │   └── ui/           # OverlayView
│   ├── src/main/res/
│   │   └── layout/       # 主界面布局
│   └── src/main/assets/  # MediaPipe 模型文件
├── data/                  # 原始数据集
│   ├── 0_ready/           
│   ├── 1_lift_sky/        
│   ├── ...                
│   ├── negative/          # 负样本/干扰动作 JSON 文件
│   └── pose17_viz_demo.ipynb #可视化 Demo Notebook
├── data_transform/        # 数据归一化文件
│   └── trans.py           # 核心脚本：负责 JSON 到 CSV 的清洗与归一化
├── outputfile/            # 成品数据
│   └── baduanjin_normalized.csv # 最终生成的全量归一化训练数据集
├── pose17_viz/           # pose17_v1 JSON 读取、预览与动画工具包
├── gradle/
├── logs/                 # 开发日志
└── README.md
```

---

## 环境要求
- Android Studio
- Android SDK
- 一台支持 USB 调试的安卓手机
- 可联网的 Gradle 环境

建议：
- 使用真机而不是模拟器
- 使用后置摄像头
- 保证人物全身入镜

---

## 如何运行

### 1. 克隆仓库
```bash
git clone https://github.com/CokaYear81/Android-CV-Taichi.git
```

### 2. 用 Android Studio 打开项目
打开目录：

```text
android_app
```

### 3. 同步依赖
首次打开后等待 `Gradle Sync` 完成。

### 4. 连接手机并运行
- 打开手机开发者模式和 USB 调试
- 连接手机
- 在 Android Studio 中点击 `Run`

---

## 模型文件说明
项目使用 `MediaPipe Pose Landmarker`。

默认模型文件：

```text
app/src/main/assets/pose_landmarker_lite.task
```

如果拉下仓库后缺少该文件，请按 MediaPipe 官方说明补入模型文件。

---

## 当前 App 怎么用
启动后你会看到：
- 相机预览
- 实时骨架 overlay
- 采集输入区

### 采集步骤
1. 输入 `subject_id`
2. 输入 `action_name`
3. 点击 `Start Capture`
4. 做动作
5. 点击 `Stop Capture`

### 当前采集假设
- 固定正面机位
- 单人采集
- 全身入镜
- 采集时同步保存：
  - 原始视频
  - 关键点 JSON

---

## 导出文件位置
导出文件保存在手机 App 私有目录：

```text
files/captures/landmarks/
files/captures/raw_videos/
```

在 Android Studio 的 `Device Explorer` 中可找到完整路径：

```text
data/data/com.lenovo.taichivision/files/captures/landmarks/
data/data/com.lenovo.taichivision/files/captures/raw_videos/
```

---

## 如何把数据拉到电脑
在 Android Studio 中：

```text
View -> Tool Windows -> Device Explorer
```

然后进入：

```text
data/data/com.lenovo.taichivision/files/captures/
```

找到目标 `json` 或 `mp4` 后：
- 右键
- 选择 `Save As...` 或 `Pull`
- 保存到本地目录做后续训练与整理

---

## 导出 JSON 的当前格式
当前正式样本格式为：
- `landmark_schema_version = "pose17_v1"`
- 单样本核心张量形状可整理为：
  - `T x 17 x 4`
- 每帧保留：
  - `frame_index`
  - `timestamp_ms`
  - `has_pose`
  - `pose_landmarks`

无 pose 帧仍会补零保留，方便后续直接做训练张量堆叠。

---

## 下一步方向
- 围绕八段锦预备式建立第一版可训练数据集
- 继续稳定手机端关键点采集
- 完成数据清洗、标签定义与数据划分
- 为后续 `PyTorch + GRU` 二分类训练做准备

---

## Python 可视化工具

为方便在训练前快速检查 `pose17_v1` 样本，仓库当前补充了一套轻量的 Python 可视化工具：

- `pose17_viz/`
  - 负责 `pose17_v1` JSON 的读取、校验、摘要、静态预览和骨架动画
- `data/pose17_viz_demo.ipynb`
  - 推荐的 notebook 入口
  - 只需要修改 `INPUT_JSON` 文件名，就可以查看同目录下的其他样本
- `data/0004_firstaction1_20260408_202501.json`
  - 当前默认示例样本

### 使用方式
1. 用 Jupyter 打开：
   - `data/pose17_viz_demo.ipynb`
2. 在第一格中修改：
   - `INPUT_JSON = "0004_firstaction1_20260408_202501.json"`
3. 按顺序运行 notebook 单元格

### 当前默认行为
- 只读取 `data/` 目录中的原始样本
- 默认显示方向为：`clockwise_90`
- 动图右下角显示当前帧时间，格式为：`mm:ss.SSS`
- 不改原始 JSON，不做重采样，不导出 `gif/mp4`

### Python 依赖
运行 notebook 前请确保当前环境安装：
- `numpy`
- `matplotlib`
- `jupyter`

---

## Python 归一化脚本（data_transform）

当前仓库新增并统一了 `pose17_v1` 口径的归一化脚本：

- `data_transform/trans.py`
  - 面向 `pose17_v1` 导出格式（读取 `frames[].pose_landmarks`）
  - 对每帧 `17` 点做中心化 + 躯干尺度归一化
  - 跳过 `has_pose=false` 帧与低可见度帧（均值阈值默认 `0.5`）
  - 输出帧级 CSV（默认文件：`baduanjin_normalized.csv`）

### 当前默认输入假设
- 输入目录按动作分类子文件夹组织（`DATA_DIR/<label>/*.json`）
- JSON 顶层 `landmark_schema_version = "pose17_v1"`
- 帧结构包含：
  - `frame_index`
  - `timestamp_ms`
  - `pose_landmarks`（长度固定 `17`）

### 输出字段
- `filename`
- `sample_id`
- `frame_index`
- `timestamp_ms`
- `x0,y0,z0 ... x16,y16,z16`
- `label`

---

## 开发日志

当前仓库已同步收录开发日志，便于协作、回溯和阶段复盘：

- [dev_log_001_2026-03-18.md](logs/dev_log_001_2026-03-18.md)
- [dev_log_002_2026-03-18.md](logs/dev_log_002_2026-03-18.md)
- [dev_log_003_2026-03-20.md](logs/dev_log_003_2026-03-20.md)
- [dev_log_004_2026-03-20.md](logs/dev_log_004_2026-03-20.md)
- [dev_log_005_2026-03-20.md](logs/dev_log_005_2026-03-20.md)
- [dev_log_006_2026-03-30.md](logs/dev_log_006_2026-03-30.md)
- [dev_log_007_2026-03-30.md](logs/dev_log_007_2026-03-30.md)
- [dev_log_008_2026-04-02.md](logs/dev_log_008_2026-04-02.md)
- [dev_log_009_2026-04-08.md](logs/dev_log_009_2026-04-08.md)
- [dev_log_010_2026-04-09.md](logs/dev_log_010_2026-04-09.md)
- [dev_log_011_2026-04-09.md](logs/dev_log_011_2026-04-09.md)
- [dev_log_012_2026-04-18.md](logs/dev_log_012_2026-04-18.md)

## 说明
当前仓库以中文 README 为主，便于项目内部同步与开发记录对齐。

