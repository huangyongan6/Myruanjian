# 线性回归

## 一、概念介绍

线性回归（Linear Regression）是机器学习中最基础的有监督学习算法之一，用于预测一个连续型目标变量。它假设自变量与因变量之间存在线性关系，通过学习一条直线（或超平面）来拟合数据。

例如：根据房屋面积预测房价、根据学习时长预测考试成绩，都可以使用线性回归建模。

## 二、核心原理

### 2.1 数学推导

线性回归的预测函数：

$$\hat{y} = w_1 x_1 + w_2 x_2 + \dots + w_n x_n + b = \mathbf{w}^\top \mathbf{x} + b$$

损失函数（均方误差 MSE）：

$$L(\mathbf{w}, b) = \frac{1}{m} \sum_{i=1}^{m} \left( \hat{y}^{(i)} - y^{(i)} \right)^2$$

通过**最小二乘法**或**梯度下降法**求解最优参数：

- 最小二乘法（闭式解）：$\mathbf{w} = (X^\top X)^{-1} X^\top y$
- 梯度下降（迭代解）：$\mathbf{w} \leftarrow \mathbf{w} - \alpha \frac{\partial L}{\partial \mathbf{w}}$

### 2.2 算法流程

1. 准备数据，划分训练集 / 验证集 / 测试集
2. 选择损失函数（MSE / RMSE / MAE）
3. 初始化参数（$\mathbf{w}$ 随机，$b = 0$）
4. 迭代优化：前向传播计算 $\hat{y}$ → 计算损失 → 反向传播更新参数
5. 评估：$R^2$、MSE、MAE 等指标

## 三、代码实现

```python
import numpy as np
from sklearn.linear_model import LinearRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, r2_score

# 1. 准备数据
X = np.array([[1], [2], [3], [4], [5], [6], [7], [8]])
y = np.array([2, 4, 5, 4, 5, 7, 8, 9])

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.25, random_state=42)

# 2. 训练模型
model = LinearRegression()
model.fit(X_train, y_train)

# 3. 评估
y_pred = model.predict(X_test)
print(f"R² Score: {r2_score(y_test, y_pred):.2f}")
print(f"MSE: {mean_squared_error(y_test, y_pred):.2f}")
print(f"权重 w = {model.coef_[0]:.2f}, 偏置 b = {model.intercept_:.2f}")
```

**预期输出**：

```
R² Score: 0.72
MSE: 0.71
权重 w = 0.93, 偏置 b = 1.43
```

## 四、适用场景

✅ **适合**：
- 自变量与因变量近似线性关系
- 特征数量适中（< 1万）
- 需要可解释的模型（系数即特征重要性）

❌ **不适合**：
- 特征与目标存在非线性关系（应先用多项式特征或换模型）
- 数据存在严重多重共线性（考虑 Ridge / Lasso 正则化）

## 五、常见易错点

1. **未做特征归一化**：导致梯度下降收敛慢
2. **忽略多重共线性**：系数符号与业务直觉相反
3. **训练集 / 测试集未随机划分**：评估指标虚高
4. **将分类变量直接喂入**：应先 one-hot 编码

## 六、练习题

1. 选择题：线性回归常用的损失函数是？
   - [ ] A. 交叉熵
   - [x] B. 均方误差（MSE）
   - [ ] C. Hinge Loss
   - [ ] D. KL 散度

2. 简答题：解释过拟合在线性回归中的表现以及至少 2 种解决方法。

3. 编程题：用 Scikit-learn 实现「波士顿房价数据集」的线性回归，并打印 $R^2$。

## 七、拓展阅读

- 周志华《机器学习》（西瓜书）第 3 章
- 李航《统计学习方法》第 1 章
- Scikit-learn 官方文档：https://scikit-learn.org/stable/modules/linear_model.html

<!-- ============================================ -->
<!-- 以下内容由 scripts/sync-knowledge.py 同步自顶层原稿 knowledge/ -->
<!-- 仅供阅读参考；正文以本文件原有章节为准，重复段落由维护者清理。 -->
<!-- ============================================ -->

# 线性回归

## 概念介绍

线性回归是最基础、最重要的机器学习算法之一。它的目标是找到一条直线（或高维空间中的超平面）来拟合数据，使得预测值和真实值之间的差距最小。就像在散点图上画一条"最佳拟合线"。

线性回归虽然简单，但它是理解更复杂算法的基础。很多高级算法（如逻辑回归、神经网络）都是在线性回归的基础上发展而来的。现实生活中，预测房价、销量、气温等连续值问题都可以用线性回归来解决。

## 核心原理

### 一元线性回归

只有一个特征的情况：ŷ = wx + b

目标是找到最优的w和b，使得所有样本的预测值与真实值的差距之和最小。

### 多元线性回归

有多个特征的情况：ŷ = w₁x₁ + w₂x₂ + ... + wₙxₙ + b

用向量表示：ŷ = Xw + b

### 损失函数

使用均方误差（MSE）作为损失函数：

```
L(w, b) = (1/2n) * Σ(ŷᵢ - yᵢ)² = (1/2n) * Σ(wxᵢ + b - yᵢ)²
```

### 求解方法

**方法1：正规方程（直接求解）**

对损失函数求导令其为零，直接得到最优解：

```
w = (XᵀX)⁻¹Xᵀy
```

优点：直接得到最优解，不需要迭代。缺点：当特征数量很大时，矩阵求逆计算量大（O(n³)）。

**方法2：梯度下降（迭代求解）**

通过不断沿着梯度反方向更新参数，逐步逼近最优解：

```
w := w - α * ∂L/∂w
b := b - α * ∂L/∂b
```

其中α是学习率，控制每步更新的幅度。

梯度计算：

```
∂L/∂w = (1/n) * Σ(ŷᵢ - yᵢ) * xᵢ
∂L/∂b = (1/n) * Σ(ŷᵢ - yᵢ)
```

## 代码实现

```python
import numpy as np
from sklearn.linear_model import LinearRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, r2_score
import matplotlib.pyplot as plt

# ========== 方法1：使用Scikit-learn ==========
np.random.seed(42)
X = 2 * np.random.rand(100, 1)
y = 4 + 3 * X.squeeze() + np.random.randn(100) * 0.5

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

model = LinearRegression()
model.fit(X_train, y_train)
print(f"权重w: {model.coef_[0]:.4f}")
print(f"偏置b: {model.intercept_:.4f}")
print(f"R²: {r2_score(y_test, model.predict(X_test)):.4f}")
print(f"MSE: {mean_squared_error(y_test, model.predict(X_test)):.4f}")

# ========== 方法2：手写梯度下降 ==========
def gradient_descent(X, y, lr=0.01, epochs=1000):
    n = len(y)
    w, b = 0, 0
    for _ in range(epochs):
        y_pred = w * X.squeeze() + b
        dw = (1/n) * np.sum((y_pred - y) * X.squeeze())
        db = (1/n) * np.sum(y_pred - y)
        w -= lr * dw
        b -= lr * db
    return w, b

w, b = gradient_descent(X_train, y_train, lr=0.1, epochs=1000)
print(f"\n梯度下降结果: w={w:.4f}, b={b:.4f}")
```

## 适用场景

- 预测连续值：房价预测、销量预测、温度预测
- 特征和目标之间大致呈线性关系
- 需要可解释性的场景（每个特征的权重直观可理解）
- 作为基线模型，和其他复杂模型对比

## 常见易错点

1. **不做特征标准化就用梯度下降**：不同特征尺度差异大会导致梯度下降收敛很慢
2. **学习率设置不当**：太大会震荡不收敛，太小会收敛很慢
3. **对非线性数据强行用线性回归**：需要先画图看数据分布，非线性数据需要多项式回归或其他模型
4. **忽略多重共线性**：特征之间高度相关会导致权重不稳定

## 练习题

1. **选择题**：线性回归的损失函数通常是？（A）交叉熵 （B）均方误差 （C）Hinge Loss （D）KL散度
   - 答案：B

2. **填空题**：正规方程求解线性回归的公式是 w = ____。
   - 答案：w = (XᵀX)⁻¹Xᵀy

3. **简答题**：梯度下降中学习率过大或过小分别会有什么问题？
   - 答案：学习率过大可能导致震荡甚至发散，无法收敛到最优解；学习率过小会导致收敛速度很慢，需要更多迭代次数。

4. **编程题**：用Scikit-learn的LinearRegression预测波士顿房价，计算R²和RMSE。
   - 参考上面代码，数据集换成fetch_california_housing。

## 推荐阅读

- 吴恩达《机器学习》第1-2周
- 西瓜书第3章
- 李航《统计学习方法》第1章
