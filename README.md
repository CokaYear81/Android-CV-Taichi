# Android-CV-Taichi

[English README](README_EN.md)

一个基于安卓摄像头与 `MediaPipe Pose` 的太极动作采集与可视化项目。  
当前版本重点不是训练和评分，而是先把**手机端关键点采集链路**做稳定：

- 相机实时预览
- 人体姿态关键点检测
- 骨架 overlay 叠加显示
- 原始视频导出
- 训练友好的关键点 `JSON` 导出

---

## 当前进度

### 已完成
- `week1`
  - `CameraX Preview`
  - `ImageAnalysis`
  - 真机运行与基础构建验证

- `week2`
  - `MediaPipe Pose Landmarker` 接入
  - `33` 个姿态关键点实时返回
  - 骨架 overlay 创建与对齐修复
  - 手机端采集流程：
    - 输入 `subject_id`
    - 输入 `action_name`
    - `Start Capture / Stop Capture`
  - 导出：
    - 原始视频 `mp4`
    - 固定长度关键点 `json`

### 当前项目状态
当前仓库已经可以作为一个**安卓关键点采集工具**使用，而不只是演示 Demo。

已经验证通过的能力：
- 实时显示人体骨架
- 采集一段动作
- 在手机 App 私有目录中生成：
  - `json`
  - `mp4`
- `json` 结构可直接用于后续 Python / PyTorch 数据读取

### 下一步计划
- `week3`
  - 选定第一个动作
  - 定义错误标签
  - 制定单机位正面采集规范
  - 试采一批样本并复盘质量

---

## 项目目标

本项目的整体路线是：

```text
Android Camera
-> Pose Keypoints
-> JSON / Video Export
-> Python / PyTorch Training
-> 规则分析 / 模型评估
-> 后续端侧部署
```

当前仓库只覆盖这条链路里的**安卓采集端**部分。

---

## 目录说明

```text
android_app/
├── app/
│   ├── src/main/java/com/lenovo/taichivision/
│   │   ├── data/         # JSON 数据模型与写文件工具
│   │   ├── pose/         # Pose 结果轻量对象
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
- 一台支持调试的安卓手机
- 可联网的 Gradle 环境

建议：
- 使用真机而不是模拟器
- 采用后置摄像头
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

默认模型文件名：

```text
app/src/main/assets/pose_landmarker_lite.task
```

如果你拉下仓库后发现本地缺少这个文件，请按 MediaPipe 官方说明补入该模型文件。

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

### 当前采集特点
- 固定正面机位
- 单人采集
- 不记录视角字段
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

当前 JSON 设计目标是：**方便后续 PyTorch 训练**。

### 顶层字段
- `sample_id`
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
每帧固定 `33` 个关键点，每个点包含：
- `x`
- `y`
- `z`
- `visibility`

### 特别说明
如果某一帧没有检测到人体：
- 该帧仍然保留
- `has_pose=false`
- `pose_landmarks` 补齐为 `33` 个零点

这让后续读取时可以直接组织成：

```text
T x 33 x 4
```

---

## 当前限制

当前版本仍然是一个早期采集工具，暂不包含：

- 动作评分
- 规则引擎
- PyTorch 训练代码
- Hand Landmarker
- 多人检测
- 自动动作识别
- ExecuTorch 部署

---

## 协作说明

如果你是第一次接手这个仓库，建议按下面顺序理解：

1. 先跑通安卓 App
2. 确认骨架实时显示正常
3. 做一次 `Start Capture -> Stop Capture`
4. 在 `Device Explorer` 中确认生成：
   - `json`
   - `mp4`
5. 再进入后续数据采集或训练工作

---

## 后续建议

当前最合理的下一步不是“直接训练大模型”，而是：

- 选定第一个太极动作
- 定义标准动作与错误标签
- 固化采集规范
- 先积累一批干净样本

---

## 致谢

- Android CameraX
- MediaPipe Pose Landmarker
- PyTorch（后续训练阶段使用）
