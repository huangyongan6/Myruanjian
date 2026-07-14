# PyTorch 入门

## 概念介绍

PyTorch 是 Facebook（Meta）开发的深度学习框架，以"动态计算图"和"Pythonic 风格"著称，是目前学术界最流行的深度学习框架。相比 TensorFlow，PyTorch 更灵活易用，调试更方便，特别适合研究和原型开发。

## 核心原理

### Tensor（张量）

Tensor 是 PyTorch 的核心数据结构，类似 NumPy 的 ndarray，但支持 GPU 加速和自动求导。

### 自动求导（Autograd）

PyTorch 通过计算图自动计算梯度，不需要手动写反向传播：

```python
x = torch.tensor([2.0], requires_grad=True)
y = x ** 2 + 3 * x
y.backward()
print(x.grad)  # dy/dx = 2x + 3 = 7.0
```

### 训练循环五步法

```python
for epoch in range(num_epochs):
    # 1. 前向传播
    output = model(x)
    loss = criterion(output, y)
    # 2. 清零梯度
    optimizer.zero_grad()
    # 3. 反向传播
    loss.backward()
    # 4. 更新参数
    optimizer.step()
    # 5. 打印 / 评估
```

## 代码实现

```python
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset

# ========== 基础操作 ==========
# 创建 Tensor
a = torch.tensor([1, 2, 3], dtype=torch.float32)
b = torch.randn(3, 3)          # 随机正态分布
c = torch.zeros(2, 3)           # 全0
d = torch.ones(2, 3)            # 全1

# GPU 加速
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
a_gpu = a.to(device)

# ========== 搭建模型 ==========
class MLP(nn.Module):
    def __init__(self, input_dim, hidden_dim, output_dim):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(input_dim, hidden_dim),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_dim, hidden_dim),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_dim, output_dim)
        )

    def forward(self, x):
        return self.net(x)

# ========== 完整训练流程 ==========
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

# 数据准备
X, y = load_iris(return_X_y=True)
X = StandardScaler().fit_transform(X)
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

X_train_t = torch.FloatTensor(X_train).to(device)
y_train_t = torch.LongTensor(y_train).to(device)
X_test_t = torch.FloatTensor(X_test).to(device)
y_test_t = torch.LongTensor(y_test).to(device)

# DataLoader（批处理）
train_dataset = TensorDataset(X_train_t, y_train_t)
train_loader = DataLoader(train_dataset, batch_size=16, shuffle=True)

# 模型、损失函数、优化器
model = MLP(4, 32, 3).to(device)
criterion = nn.CrossEntropyLoss()
optimizer = optim.Adam(model.parameters(), lr=0.001)

# 训练
for epoch in range(50):
    model.train()
    total_loss = 0
    for batch_x, batch_y in train_loader:
        optimizer.zero_grad()
        output = model(batch_x)
        loss = criterion(output, batch_y)
        loss.backward()
        optimizer.step()
        total_loss += loss.item()

    if (epoch + 1) % 10 == 0:
        model.eval()
        with torch.no_grad():
            test_output = model(X_test_t)
            pred = test_output.argmax(dim=1)
            acc = (pred == y_test_t).float().mean()
        print(f"Epoch {epoch+1}, Loss: {total_loss/len(train_loader):.4f}, Test Acc: {acc:.4f}")

# 模型保存与加载
torch.save(model.state_dict(), 'model.pth')
# model.load_state_dict(torch.load('model.pth'))
```

## 适用场景

- 深度学习研究和原型开发
- 计算机视觉（配合 torchvision）
- 自然语言处理（配合 transformers 库）
- 需要灵活自定义模型的场景

## 常见易错点

1. **忘记 model.train() / model.eval()**：训练时用 train()，评估时用 eval()（影响 Dropout 和 BatchNorm）
2. **忘记 optimizer.zero_grad()**：不清零梯度会累加
3. **数据没转 Tensor**：输入必须是 torch.Tensor，不能是 numpy 或 list
4. **忘记 .to(device)**：模型和数据要在同一个设备上（CPU 或 GPU）

## 练习题

1. 用 PyTorch 搭建一个 CNN 对手写数字做分类
2. 比较 SGD 和 Adam 优化器的收敛速度
3. 实现一个简单的自定义 Dataset 类

## 推荐阅读

- PyTorch 官方教程：https://pytorch.org/tutorials/
- 《动手学深度学习》（d2l.ai）PyTorch 版

<!-- ============================================ -->
<!-- 以下内容由 scripts/sync-knowledge.py 同步自顶层原稿 knowledge/ -->
<!-- 仅供阅读参考；正文以本文件原有章节为准，重复段落由维护者清理。 -->
<!-- ============================================ -->

# PyTorch入门

## 概念介绍

PyTorch是Facebook（Meta）开发的深度学习框架，以"动态计算图"和"Pythonic风格"著称，是目前学术界最流行的深度学习框架。相比TensorFlow，PyTorch更灵活易用，调试更方便，特别适合研究和原型开发。

## 核心原理

### Tensor（张量）

Tensor是PyTorch的核心数据结构，类似NumPy的ndarray，但支持GPU加速和自动求导。

### 自动求导（Autograd）

PyTorch通过计算图自动计算梯度，不需要手动写反向传播：

```python
x = torch.tensor([2.0], requires_grad=True)
y = x ** 2 + 3 * x
y.backward()
print(x.grad)  # dy/dx = 2x + 3 = 7.0
```

### 训练循环五步法

```python
for epoch in range(num_epochs):
    # 1. 前向传播
    output = model(x)
    loss = criterion(output, y)
    # 2. 清零梯度
    optimizer.zero_grad()
    # 3. 反向传播
    loss.backward()
    # 4. 更新参数
    optimizer.step()
    # 5. 打印/评估
```

## 代码实现

```python
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset

# ========== 基础操作 ==========
# 创建Tensor
a = torch.tensor([1, 2, 3], dtype=torch.float32)
b = torch.randn(3, 3)          # 随机正态分布
c = torch.zeros(2, 3)           # 全0
d = torch.ones(2, 3)            # 全1

# GPU加速
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
a_gpu = a.to(device)

# ========== 搭建模型 ==========
class MLP(nn.Module):
    def __init__(self, input_dim, hidden_dim, output_dim):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(input_dim, hidden_dim),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_dim, hidden_dim),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_dim, output_dim)
        )

    def forward(self, x):
        return self.net(x)

# ========== 完整训练流程 ==========
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

# 数据准备
X, y = load_iris(return_X_y=True)
X = StandardScaler().fit_transform(X)
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

X_train_t = torch.FloatTensor(X_train).to(device)
y_train_t = torch.LongTensor(y_train).to(device)
X_test_t = torch.FloatTensor(X_test).to(device)
y_test_t = torch.LongTensor(y_test).to(device)

# DataLoader（批处理）
train_dataset = TensorDataset(X_train_t, y_train_t)
train_loader = DataLoader(train_dataset, batch_size=16, shuffle=True)

# 模型、损失函数、优化器
model = MLP(4, 32, 3).to(device)
criterion = nn.CrossEntropyLoss()
optimizer = optim.Adam(model.parameters(), lr=0.001)

# 训练
for epoch in range(50):
    model.train()
    total_loss = 0
    for batch_x, batch_y in train_loader:
        optimizer.zero_grad()
        output = model(batch_x)
        loss = criterion(output, batch_y)
        loss.backward()
        optimizer.step()
        total_loss += loss.item()

    if (epoch + 1) % 10 == 0:
        model.eval()
        with torch.no_grad():
            test_output = model(X_test_t)
            pred = test_output.argmax(dim=1)
            acc = (pred == y_test_t).float().mean()
        print(f"Epoch {epoch+1}, Loss: {total_loss/len(train_loader):.4f}, Test Acc: {acc:.4f}")

# 模型保存与加载
torch.save(model.state_dict(), 'model.pth')
# model.load_state_dict(torch.load('model.pth'))
```

## 适用场景

- 深度学习研究和原型开发
- 计算机视觉（配合torchvision）
- 自然语言处理（配合transformers库）
- 需要灵活自定义模型的场景

## 常见易错点

1. **忘记model.train()/model.eval()**：训练时用train()，评估时用eval()（影响Dropout和BatchNorm）
2. **忘记optimizer.zero_grad()**：不清零梯度会累加
3. **数据没转Tensor**：输入必须是torch.Tensor，不能是numpy或list
4. **忘记.to(device)**：模型和数据要在同一个设备上（CPU或GPU）

## 练习题

1. 用PyTorch搭建一个CNN对手写数字做分类
2. 比较SGD和Adam优化器的收敛速度
3. 实现一个简单的自定义Dataset类

## 推荐阅读

- PyTorch官方教程：https://pytorch.org/tutorials/
- 《动手学深度学习》（d2l.ai）PyTorch版
