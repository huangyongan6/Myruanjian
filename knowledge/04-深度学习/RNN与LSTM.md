# RNN/LSTM序列模型

## 概念介绍

RNN（Recurrent Neural Network，循环神经网络）是专门处理序列数据的神经网络。与普通神经网络不同，RNN有"记忆"功能——它能记住之前的信息，用于当前的决策。就像读一句话时，你的理解是基于前面读过的每个词。

RNN广泛应用于自然语言处理（文本分类、机器翻译）、语音识别、时间序列预测等。但原始RNN有"长期依赖"问题，LSTM通过门控机制解决了这个问题。

## 核心原理

### RNN的基本结构

```
x₁ → [RNN] → h₁
x₂ → [RNN] → h₂
x₃ → [RNN] → h₃
...
```

每个时刻t的隐藏状态：hₜ = tanh(Wₓₕxₜ + Wₕₕhₜ₋₁ + b)

关键：hₜ不仅取决于当前输入xₜ，还取决于上一时刻的隐藏状态hₜ₋₁，这就是"记忆"。

### LSTM（Long Short-Term Memory）

LSTM通过三个"门"来控制信息的流动：

**遗忘门**：决定丢弃哪些旧信息
```
fₜ = σ(Wf·[hₜ₋₁, xₜ] + bf)
```

**输入门**：决定存储哪些新信息
```
iₜ = σ(Wi·[hₜ₋₁, xₜ] + bi)
C̃ₜ = tanh(Wc·[hₜ₋₁, xₜ] + bc)
```

**输出门**：决定输出哪些信息
```
oₜ = σ(Wo·[hₜ₋₁, xₜ] + bo)
hₜ = oₜ * tanh(Cₜ)
```

细胞状态更新：Cₜ = fₜ * Cₜ₋₁ + iₜ * C̃ₜ

## 代码实现

```python
import torch
import torch.nn as nn

# ========== 文本分类LSTM模型 ==========
class TextLSTM(nn.Module):
    def __init__(self, vocab_size, embed_dim, hidden_dim, output_dim):
        super().__init__()
        self.embedding = nn.Embedding(vocab_size, embed_dim)
        self.lstm = nn.LSTM(embed_dim, hidden_dim, batch_first=True, num_layers=2, dropout=0.3)
        self.fc = nn.Linear(hidden_dim, output_dim)

    def forward(self, x):
        # x: (batch, seq_len)
        embedded = self.embedding(x)           # (batch, seq_len, embed_dim)
        output, (hidden, cell) = self.lstm(embedded)  # output: (batch, seq_len, hidden_dim)
        # 取最后一个时间步的输出
        last_hidden = output[:, -1, :]         # (batch, hidden_dim)
        return self.fc(last_hidden)            # (batch, output_dim)

# 模型实例化
model = TextLSTM(vocab_size=10000, embed_dim=128, hidden_dim=256, output_dim=2)

# 模拟输入
x = torch.randint(0, 10000, (32, 50))  # batch=32, seq_len=50
output = model(x)
print(f"输出形状: {output.shape}")  # (32, 2)
```

## 适用场景

- 文本分类（情感分析、垃圾邮件检测）
- 机器翻译
- 语音识别
- 时间序列预测（股价、天气）
- 生成式任务（文本生成、音乐生成）

## 常见易错点

1. **不处理变长序列**：文本长度不同需要padding和pack_padded_sequence
2. **忘记设置batch_first**：PyTorch默认seq_len在第一维
3. **LSTM层数太多**：2-3层通常够了，太深容易过拟合且训练慢
4. **不用预训练词向量**：用GloVe或Word2Vec初始化embedding效果更好

## 练习题

1. **选择题**：LSTM中"遗忘门"的作用是什么？（A）输入新信息 （B）丢弃旧信息 （C）输出信息 （D）计算损失
   - 答案：B

2. **简答题**：为什么原始RNN不适合处理长序列？
   - 答案：原始RNN在反向传播时梯度需要连乘，长序列会导致梯度消失（或爆炸），使得网络无法学习长距离依赖关系。LSTM通过门控机制和细胞状态解决了这个问题。

3. **编程题**：用LSTM对IMDB电影评论做情感二分类。
   - 参考上面代码，使用torchtext加载IMDB数据。

## 推荐阅读

- 吴恩达深度学习课程第5周
- Christopher Olah的《Understanding LSTM Networks》
- PyTorch LSTM教程
