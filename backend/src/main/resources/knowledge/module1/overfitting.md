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

# 过拟合与欠拟合

## 概念介绍

过拟合（Overfitting）和欠拟合（Underfitting）是模型在训练数据与新数据上表现不一致的两个核心问题。

- **过拟合**：模型把训练数据的"细节 + 噪声"都学进去了，结果训练集分数很高但测试集很差。就像学生死记硬背了练习题答案，遇到新题完全不会。
- **欠拟合**：模型太简单，连训练数据的基本规律都没学到，训练集和测试集都表现很差。就像学生上课没听讲，做练习题也错，做新题也错。

调参的核心就是"在过拟合和欠拟合之间找平衡"。

## 核心原理

### 表现特征

| 现象 | 训练误差 | 验证误差 | 性质 |
|------|--------|---------|------|
| 欠拟合 | 高 | 高 | 高偏差 |
| 正常 | 低 | 低（与训练接近） | — |
| 过拟合 | 很低 | 远高于训练 | 高方差 |

### 根本原因

- **欠拟合**：模型能力不足（特征不够 / 模型太简单 / 训练不充分）。
- **过拟合**：训练数据相对模型过于复杂（特征过多 / 噪声被学 / 训练轮数过多）。

### 解决方法

| 对策 | 适用情况 | 具体做法 |
|------|---------|---------|
| 增加数据 | 过拟合 | 收集更多样本 / 数据增强 |
| 特征选择 | 过拟合 | 减少特征数 / 用正则化筛选 |
| 正则化 | 过拟合 | L1/L2 / Dropout（神经网络） |
| 简化模型 | 过拟合 | 降低多项式次数 / 减少树深度 |
| 增加特征 | 欠拟合 | 加新特征 / 特征交叉 |
| 增加模型复杂度 | 欠拟合 | 用更复杂的模型 |
| 早停（Early Stopping） | 过拟合 | 验证误差不再下降时停止 |
| 集成学习 | 过拟合 | Bagging / Boosting |

## 代码实现

```python
import numpy as np
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split, validation_curve
from sklearn.tree import DecisionTreeClassifier
from sklearn.datasets import load_iris

X, y = load_iris(return_X_y=True)
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.3, random_state=42, stratify=y)

# 用 validation_curve 观察 max_depth 的影响
param_range = np.arange(1, 11)
train_scores, val_scores = validation_curve(
    DecisionTreeClassifier(random_state=42), X_train, y_train,
    param_name='max_depth', param_range=param_range, cv=5, scoring='accuracy'
)

train_mean = train_scores.mean(axis=1)
val_mean = val_scores.mean(axis=1)

print(f"{'max_depth':>10} {'训练准确率':>12} {'验证准确率':>12}")
for d, t, v in zip(param_range, train_mean, val_mean):
    print(f"{d:>10d} {t:>12.4f} {v:>12.4f}")
```

## 适用场景

- 任何监督学习任务都需要诊断与处理过拟合/欠拟合
- 模型选择与超参数调优（学习曲线是必备工具）

## 常见易错点

1. **不看学习曲线就调参**：先画学习曲线判断是过拟合还是欠拟合，再对症下药。
2. **过拟合一味加数据**：数据有限时，加正则化比加数据更现实。
3. **欠拟合不分青红皂白加模型**：可能是特征质量差，盲目加深网络没用。
4. **依赖单一指标**：同时看训练和验证误差的差距才能定位问题。

## 练习题

1. **选择题**：训练准确率 99%，测试准确率 70%，最可能的原因是？（A）欠拟合 （B）过拟合 （C）数据量太少 （D）特征数太少
   - 答案：B

2. **填空题**：判断过拟合的依据是____远大于____。
   - 答案：验证误差；训练误差。

3. **简答题**：请分别说出三种解决过拟合的方法和三种解决欠拟合的方法。
   - 答案：过拟合：增加训练数据、特征选择、正则化（L1/L2）、Dropout、早停、集成学习。欠拟合：增加特征、增加模型复杂度、减少正则化、训练更多轮。

## 推荐阅读

- 周志华《机器学习》（西瓜书）第2章
- Andrew Ng《机器学习》课程第10-11周
- Scikit-learn 验证曲线文档：https://scikit-learn.org/stable/modules/learning_curve.html
