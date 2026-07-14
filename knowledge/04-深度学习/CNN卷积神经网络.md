# CNN卷积神经网络

## 概念介绍

CNN（Convolutional Neural Network）是专门处理图像数据的神经网络。传统全连接网络处理图片时参数太多（一张224×224的RGB图片有150528个像素），CNN通过"卷积核"局部扫描的方式，大幅减少参数量，同时能自动学习图像中的边缘、纹理、形状等特征。

CNN是计算机视觉的基础，广泛用于图像分类、目标检测、图像分割等任务。经典的CNN架构有LeNet、AlexNet、VGG、ResNet等。

## 核心原理

### 卷积层（Convolutional Layer）

用一个小的卷积核（如3×3）在图像上滑动，每个位置做点积运算，提取局部特征。

- **卷积核大小**：通常3×3或5×5
- **步长（Stride）**：卷积核每次移动的像素数
- **填充（Padding）**：在图像边缘补零，保持输出尺寸

### 池化层（Pooling Layer）

缩小特征图尺寸，减少计算量，增强平移不变性：

- **最大池化**：取窗口内最大值（最常用）
- **平均池化**：取窗口内平均值

### CNN结构

```
输入图像 → [卷积→ReLU→池化] × N → 展平 → 全连接层 → 输出
```

## 代码实现

```python
import torch
import torch.nn as nn
import torch.optim as optim
from torchvision import datasets, transforms
from torch.utils.data import DataLoader

# 数据预处理
transform = transforms.Compose([
    transforms.ToTensor(),
    transforms.Normalize((0.1307,), (0.3081,))
])

# 加载MNIST手写数字数据集
train_data = datasets.MNIST('./data', train=True, download=True, transform=transform)
test_data = datasets.MNIST('./data', train=False, transform=transform)
train_loader = DataLoader(train_data, batch_size=64, shuffle=True)
test_loader = DataLoader(test_data, batch_size=1000)

# 定义CNN
class SimpleCNN(nn.Module):
    def __init__(self):
        super().__init__()
        self.features = nn.Sequential(
            nn.Conv2d(1, 32, 3, padding=1),   # 1通道→32通道，3×3卷积
            nn.ReLU(),
            nn.MaxPool2d(2),                   # 28×28 → 14×14
            nn.Conv2d(32, 64, 3, padding=1),   # 32→64通道
            nn.ReLU(),
            nn.MaxPool2d(2),                   # 14×14 → 7×7
        )
        self.classifier = nn.Sequential(
            nn.Flatten(),
            nn.Linear(64 * 7 * 7, 128),
            nn.ReLU(),
            nn.Linear(128, 10)
        )

    def forward(self, x):
        x = self.features(x)
        x = self.classifier(x)
        return x

# 训练
model = SimpleCNN()
optimizer = optim.Adam(model.parameters(), lr=0.001)
criterion = nn.CrossEntropyLoss()

for epoch in range(3):
    model.train()
    for batch_x, batch_y in train_loader:
        optimizer.zero_grad()
        output = model(batch_x)
        loss = criterion(output, batch_y)
        loss.backward()
        optimizer.step()

    # 评估
    model.eval()
    correct = 0
    with torch.no_grad():
        for batch_x, batch_y in test_loader:
            output = model(batch_x)
            pred = output.argmax(dim=1)
            correct += (pred == batch_y).sum().item()
    print(f"Epoch {epoch+1}, 测试准确率: {correct/len(test_data):.4f}")
```

## 适用场景

- 图像分类（猫狗识别、医学影像）
- 目标检测（自动驾驶、安防监控）
- 图像分割（医学图像分割）
- 视频分析、人脸识别

## 常见易错点

1. **输入通道数搞错**：灰度图1通道，RGB图3通道
2. **不理解卷积核的作用**：每个卷积核提取一种特征（边缘、纹理等）
3. **池化层丢失信息**：最大池化会丢失部分空间信息

## 练习题

1. **选择题**：CNN中哪个层用于缩小特征图尺寸？（A）卷积层 （B）池化层 （C）全连接层 （D）激活层
   - 答案：B

2. **简答题**：CNN相比全连接网络处理图像有什么优势？
   - 答案：参数共享（同一个卷积核扫描整张图）大幅减少参数量；局部连接（只看局部区域）适合图像的局部相关性；平移不变性（同样的特征不管在图像哪个位置都能检测到）。

3. **编程题**：用上面的CNN在CIFAR-10数据集上训练，调到90%以上准确率。
   - 参考上面代码，增加数据增强和网络深度。

## 推荐阅读

- 吴恩达深度学习课程第4周
- CS231n课程笔记
- PyTorch CNN教程
