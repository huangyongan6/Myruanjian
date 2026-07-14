<!-- 拆篇源文件：偏差方差与过拟合欠拟合.md（位于顶层 knowledge/） -->

# 偏差方差与过拟合欠拟合

## 概念介绍

在机器学习中，模型表现不好通常有两种原因：**欠拟合**（模型太简单，学不到数据的规律）和**过拟合**（模型太复杂，把噪声也当成了规律）。理解偏差和方差是理解这两种问题的关键。

打个比方：欠拟合就像一个学生完全没复习，考试全靠猜，训练集和测试集都考得差；过拟合就像一个学生把练习题答案全背下来了，练习题全对，但换个题就不会了。

## 核心原理

### 偏差（Bias）

偏差衡量的是模型预测的期望值与真实值之间的差距。高偏差意味着模型太简单，无法捕捉数据的真实规律。

```
Bias = E[ŷ] - y
```

- 高偏差 → 欠拟合
- 例如：用一条直线去拟合二次函数的数据

### 方差（Variance）

方差衡量的是模型在不同训练集上预测结果的波动程度。高方差意味着模型对训练数据太敏感，换一批数据预测结果就大变。

```
Variance = E[(ŷ - E[ŷ])²]
```

- 高方差 → 过拟合
- 例如：用20次多项式拟合10个数据点

### 偏差-方差分解

模型的泛化误差可以分解为：

```
泛化误差 = 偏差² + 方差 + 不可约噪声
```

- **偏差²**：模型本身的简化假设带来的误差
- **方差**：模型对训练数据敏感带来的误差
- **不可约噪声**：数据本身的随机性，无法消除

### 过拟合与欠拟合的表现

**欠拟合（高偏差）**：
- 训练集准确率低
- 测试集准确率也低
- 模型太简单（如用线性模型拟合非线性数据）

**过拟合（高方差）**：
- 训练集准确率很高
- 测试集准确率明显低于训练集
- 模型太复杂（如决策树深度过大、多项式次数过高）

### 解决方案

**解决欠拟合**：
- 增加模型复杂度（更深的树、更多的特征）
- 减少正则化强度
- 增加特征（特征工程）

**解决过拟合**：
- 增加训练数据
- 减少模型复杂度（剪枝、减少层数）
- 增加正则化（L1/L2正则化）
- 使用Dropout（深度学习中）
- 使用交叉验证
- 早停（Early Stopping）

## 代码实现

```python
import numpy as np
import matplotlib.pyplot as plt
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import PolynomialFeatures
from sklearn.linear_model import LinearRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error

# 生成数据
np.random.seed(42)
X = np.sort(np.random.rand(100, 1) * 6, axis=0)
y = np.sin(X).ravel() + np.random.randn(100) * 0.3

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.3, random_state=42)

# 比较不同复杂度的模型
degrees = [1, 4, 15]  # 欠拟合、合适、过拟合
plt.figure(figsize=(14, 4))

for i, degree in enumerate(degrees):
    ax = plt.subplot(1, 3, i + 1)
    # 构建多项式回归管道
    model = Pipeline([
        ("poly", PolynomialFeatures(degree=degree)),
        ("linear", LinearRegression())
    ])
    model.fit(X_train, y_train)

    train_score = mean_squared_error(y_train, model.predict(X_train))
    test_score = mean_squared_error(y_test, model.predict(X_test))

    # 绘图
    X_plot = np.linspace(0, 6, 100).reshape(-1, 1)
    plt.scatter(X_train, y_train, s=10, label="训练集")
    plt.scatter(X_test, y_test, s=10, label="测试集")
    plt.plot(X_plot, model.predict(X_plot), 'r-', label="模型")
    plt.title(f"degree={degree}\n训练MSE={train_score:.3f}, 测试MSE={test_score:.3f}")
    plt.legend()

plt.tight_layout()
plt.savefig("bias_variance.png", dpi=100)
plt.show()

# degree=1: 欠拟合（训练和测试MSE都大）
# degree=4: 合适（训练和测试MSE都较小且接近）
# degree=15: 过拟合（训练MSE很小但测试MSE很大）
```

## 适用场景

- **学习曲线分析**：通过观察训练集和验证集的误差变化，判断模型处于欠拟合还是过拟合状态
- **模型选择**：在偏差和方差之间找平衡，选择复杂度合适的模型
- **调参指导**：如果欠拟合，增大模型复杂度；如果过拟合，增加正则化或数据量

## 常见易错点

1. **只看训练集表现**：训练集表现好不代表模型好，必须看测试集
2. **正则化强度搞反**：欠拟合时应该减小正则化，过拟合时应该增大正则化
3. **误以为更多数据一定更好**：如果模型本身有偏差问题，增加数据也无法解决
4. **混淆偏差和方差的诊断**：训练误差高 → 偏差问题；训练误差低但测试误差高 → 方差问题

## 练习题

1. **选择题**：模型在训练集上准确率95%，测试集上准确率60%，最可能的问题是？（A）欠拟合 （B）过拟合 （C）数据泄露 （D）特征不足
   - 答案：B。训练好测试差是典型的过拟合表现。

2. **选择题**：以下哪种方法可以缓解过拟合？（A）增加模型复杂度 （B）减少训练数据 （C）增加正则化 （D）减少特征数（答案C和D都可以，最优选C）
   - 答案：C。

3. **简答题**：请解释偏差-方差权衡（Bias-Variance Tradeoff）。
   - 答案：模型越简单偏差越高方差越低（欠拟合），模型越复杂偏差越低方差越高（过拟合）。需要找到一个平衡点，使总误差最小。

4. **编程题**：分别用1次、3次、10次多项式拟合一组非线性数据，画出学习曲线，判断哪个模型最合适。
   - 参考上面的代码实现。

## 推荐阅读

- 吴恩达《机器学习》第7周（偏差方差）
- 西瓜书第2章
- Scott Fortmann-Roe的《Understanding the Bias-Variance Tradeoff》


<!-- ============================================ -->
<!-- 以下为内部原稿（精简翻译版） -->
<!-- ============================================ -->

# 偏差与方差

## 概念介绍

偏差（Bias）和方差（Variance）是衡量模型泛化能力的两个关键维度。任何模型都会同时具有这两种误差，理解它们的来源和权衡是调参的核心。

- **偏差**：模型的"拟合能力"够不够。偏差高 = 模型太简单，学不到数据规律（欠拟合）。
- **方差**：模型的"稳定性"好不好。方差高 = 模型过度记忆训练数据的细节（过拟合）。

打个比方：偏差就像一个学生不认真学习考试内容，方差就像他只死记硬背这次考试的题目。两类学生新一次考试都不会考好。

完美的模型应该"低偏差、低方差"，但在实际中往往需要权衡——复杂的模型偏差低方差高，简单的模型偏差高方差低。

## 核心原理

### 数学表达

模型的期望误差可以分解为：

```
E[(f̂ - f)²] = Bias²(f̂) + Var(f̂) + Noise
```

- **Bias²**：模型预测的平均值与真实函数的偏离程度。
- **Var**：不同训练集训练出的模型之间的差异。
- **Noise**：数据本身的不可约误差（与模型无关）。

### 偏差-方差权衡

| 模型复杂度 | 偏差 | 方差 | 总误差 |
|-----------|------|------|--------|
| 简单（欠拟合） | 高 | 低 | 高 |
| 刚好 | 中 | 中 | 最低 |
| 复杂（过拟合） | 低 | 高 | 高 |

### 诊断方法

- **高偏差（欠拟合）**：训练误差和验证误差都很高，且两者接近。
- **高方差（过拟合）**：训练误差很低，但验证误差远高于训练误差。

## 代码实现

```python
import numpy as np
import matplotlib.pyplot as plt
from sklearn.model_selection import learning_curve
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.datasets import load_iris

X, y = load_iris(return_X_y=True)

# 用学习曲线判断偏差/方差
train_sizes, train_scores, val_scores = learning_curve(
    DecisionTreeClassifier(max_depth=3, random_state=42),
    X, y, cv=5, scoring='accuracy',
    train_sizes=np.linspace(0.1, 1.0, 10)
)

train_mean = train_scores.mean(axis=1)
val_mean = val_scores.mean(axis=1)

print(f"{'训练集大小':>10} {'训练准确率':>12} {'验证准确率':>12} {'差距':>8}")
for s, t, v in zip(train_sizes, train_mean, val_mean):
    print(f"{s*len(X):>10.0f} {t:>12.4f} {v:>12.4f} {t-v:>8.4f}")
```

## 适用场景

- 模型选择：用学习曲线诊断偏差/方差，对症下药。
- 超参数调优：在欠拟合与过拟合间找平衡点。

## 常见易错点

1. **混用"偏差"和"偏差项"**：偏差项 = 模型对真实函数的偏离；偏差陷阱 = 模型预测的平均偏差。
2. **不看学习曲线就调参**：必须先判断是偏差高还是方差高，再决定怎么调。
3. **把"高准确率"当万能目标**：高方差模型在测试集上可能崩盘，应该看验证集。

## 练习题

1. **选择题**：训练误差 0.01，验证误差 0.35，这是典型的？（A）欠拟合 （B）过拟合 （C）数据问题 （D）特征问题
   - 答案：B

2. **简答题**：欠拟合时应该怎么办？过拟合时应该怎么办？
   - 答案：欠拟合（高偏差）：增加模型复杂度、加特征、减小正则化。过拟合（高方差）：增加数据、特征选择、增强正则化、简化模型、用集成方法。

3. **编程题**：用 sklearn 的 `learning_curve` 画出决策树的学习曲线，判断其偏差/方差情况。

## 推荐阅读

- 周志华《机器学习》（西瓜书）第2章
- Andrew Ng《机器学习》Coursera 第10-11周
- Scikit-learn 学习曲线文档
