# Android-CV-Taichi

[中文说明](README.md)

一个基于安卓摄像头与 `MediaPipe Pose` 的动作采集与可视化项目。  
当前阶段的重点已经从“单纯打通采集链路”推进到：

- 继续稳定手机端关键点采集
- 使用 `pose17_v1` 作为正式样本格式
- 围绕**八段锦预备式**建立第一版可训练数据集
- 为后续 `PyTorch + GRU` 二分类训练做准备

---

## 当前进度

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

- `week3` 当前已完成
  - 正式关键点格式从 `33` 点切换到 `17` 点
  - 新增顶层字段：
    - `landmark_schema_version = "pose17_v1"`
  - overlay 改为只显示 `17` 点骨架
  - 导出逻辑改为固定 `17` 点，且无 pose 帧仍补零保留

### 当前项目状态
当前仓库已经可以作为一个**安卓端关键点采集工具**使用，而不是仅仅作为演示 Demo。

当前已经验证通过的能力：
- 实时显示人体骨架
- 采集单段动作
- 在手机 App 私有目录中生成：
  - `json`
  - `mp4`
- 使用正式样本格式：
  - `pose17_v1`
  - `T x 17 x 4`

### 第三周当前目标
第三周的正式目标已经调整为：

- 围绕 **八段锦预备式** 建立第一版可训练数据集
- 第一版训练任务固定为：
  - **标准 / 不标准 二分类**
- 本周重点不是继续扩手机功能，而是：
  - 样本采集
  - 标签定义
  - 数据集整理
  - 训练准备

---

## 当前项目路线

```text
Android Camera
-> Pose Keypoints
-> pose17_v1 JSON / Video Export
-> Dataset / Labels / Splits
-> Python / PyTorch Training
-> GRU Baseline
-> Later Rule-based Analysis / On-device Deployment
```

当前仓库主要覆盖这条链路中的**安卓采集端**，并开始为训练阶段做数据准备。

---

## 项目目录说明

```text
android_app/
├── app/
│   ├── src/main/java/com/lenovo/taichivision/
│   │   ├── data/         # JSON 数据模型与写文件工具
│   │   ├── pose/         # Pose 结果对象与 33 -> 17 点裁剪
│   │   └── ui/           # OverlayView
│   ├── src/main/res/
│   │   └── layout/       # 主界面布局
│   └── src/main/assets/  # MediaPipe 模型文件
├── gradle/
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
- 底部采集输入区

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
- `Save As...`
  或
- `Pull`

即可保存到电脑本地。

---

## JSON 结构说明

当前正式 JSON 设计目标是：**方便后续 PyTorch 训练**。

### 顶层字段
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

### 每帧字段
- `frame_index`
- `timestamp_ms`
- `has_pose`
- `pose_landmarks`

### 关键点字段
每帧固定 `17` 个关键点，每个点包含：
- `x`
- `y`
- `z`
- `visibility`

### 当前正式 schema
- `landmark_schema_version = "pose17_v1"`

### 17 点顺序
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

### 特别说明
如果某一帧没有检测到人体：
- 该帧仍然保留
- `has_pose=false`
- `pose_landmarks` 补齐为 `17` 个零点

这让后续读取时可以直接组织成：

```text
T x 17 x 4
```

---

## 当前第三周训练准备口径

当前第三周正式目标固定为：
- 动作：
  - `baduanjin_preparation`
- 第一版任务：
  - `is_standard` 二分类
- 第一版模型方向：
  - `GRU`
- 第一版训练窗口：
  - `32 x 17 x 4`

第三周不是追求模型精度，而是要先完成：
- 样本采集
- 标签定义
- 按人划分数据集
- 最小训练链路跑通

---

## 当前限制

当前版本仍然不包含：
- 动作评分
- 规则引擎
- 完整 `PyTorch` 训练代码
- `Hand Landmarker`
- 多人检测
- 自动动作识别
- `ExecuTorch` 部署

---

## 后续建议

当前最合理的下一步不是“直接训练大模型”，而是：

- 围绕八段锦预备式采第一批正式样本
- 建立标签表
- 按 `subject_id` 划分 train / val / test
- 用 `PyTorch` 跑通第一版 `GRU` 二分类基线

---

## 致谢

- Android CameraX
- MediaPipe Pose Landmarker
- PyTorch（后续训练阶段使用）
