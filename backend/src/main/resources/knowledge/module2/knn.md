# KNN K近邻算法

## 概念介绍

KNN（K-Nearest Neighbors）是最直观、最简单的机器学习算法之一。它的思想非常朴素：要判断一个新样本属于哪个类别，就看它周围最近的 K 个邻居，哪个类别多就归为哪个类别。就像判断一个人的性格，看他最亲密的5个朋友是什么性格。

KNN 是"懒惰学习"的代表——它不需要训练过程，只是把数据存起来，预测时才开始计算。优点是简单易懂，缺点是预测时计算量大。

## 核心原理

### 算法步骤

1. 选择 K 值和距离度量方式
2. 对新样本，计算它与所有训练样本的距离
3. 找到距离最近的 K 个样本（K 个邻居）
4. 分类：投票，K 个邻居中最多的类别即为预测结果
5. 回归：取 K 个邻居的目标值平均

### 距离度量

**欧氏距离**（最常用）：
```
d(x, y) = √(Σ(xᵢ - yᵢ)²)
```

**曼哈顿距离**：
```
d(x, y) = Σ|xᵢ - yᵢ|
```

**闵可夫斯基距离**（通用形式）：
```
d(x, y) = (Σ|xᵢ - yᵢ|ᵖ)^(1/p)
```

### K 值选择

- K 太小（如 K=1）：对噪声敏感，容易过拟合。
- K 太大：边界模糊，容易欠拟合。
- 通常用交叉验证选择最优 K，一般取奇数（避免平票）。

## 代码实现

```python
from sklearn.neighbors import KNeighborsClassifier, KNeighborsRegressor
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import accuracy_score
import numpy as np

# 加载数据
X, y = load_iris(return_X_y=True)

# KNN 需要特征标准化
scaler = StandardScaler()
X = scaler.fit_transform(X)

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# 用交叉验证选择最优 K
k_range = range(1, 21)
scores = []
for k in k_range:
    model = KNeighborsClassifier(n_neighbors=k)
    score = cross_val_score(model, X_train, y_train, cv=5).mean()
    scores.append(score)
    print(f"K={k:2d}, 交叉验证准确率: {score:.4f}")

best_k = list(k_range)[np.argmax(scores)]
print(f"\n最优 K 值: {best_k}")

# 用最优 K 训练最终模型
model = KNeighborsClassifier(n_neighbors=best_k)
model.fit(X_train, y_train)
y_pred = model.predict(X_test)
print(f"测试集准确率: {accuracy_score(y_test, y_pred):.4f}")
```

## 适用场景

- 数据量不大（样本数 < 10 万）
- 分类边界不规则的场景
- 推荐系统的基础（协同过滤）
- 作为基线模型快速验证

## 常见易错点

1. **忘记做特征标准化**：KNN 基于距离，特征尺度不同会导致大尺度特征主导距离。
2. **K 值不调优**：直接用默认 K=5，应该用交叉验证选最优 K。
3. **大数据集用 KNN**：预测时需要计算和所有训练样本的距离，O(n) 复杂度，大数据很慢。
4. **不考虑距离权重**：默认投票时所有邻居权重相同，可以用 weights='distance' 让近的邻居权重更大。

## 练习题

1. **选择题**：KNN 属于什么类型的学习？（A）急切学习 （B）懒惰学习 （C）强化学习 （D）半监督学习
   - 答案：B。KNN 没有训练过程，是典型的懒惰学习。

2. **填空题**：KNN 中 K 值过小容易导致____，K 值过大容易导致____。
   - 答案：过拟合；欠拟合。

3. **简答题**：为什么 KNN 需要做特征标准化？
   - 答案：KNN 基于距离计算，如果某个特征的范围是 0-1000，另一个是 0-1，那么第一个特征会完全主导距离计算，导致第二个特征被忽略。

4. **编程题**：用 KNN 对手写数字数据集（load_digits）做分类，比较不同 K 值的效果。

## 推荐阅读

- 西瓜书第10章
- Scikit-learn KNN 文档：https://scikit-learn.org/stable/modules/neighbors.html

<!-- ============================================ -->
<!-- 以下内容由 scripts/sync-knowledge.py 同步自顶层原稿 knowledge/ -->
<!-- 仅供阅读参考；正文以本文件原有章节为准，重复段落由维护者清理。 -->
<!-- ============================================ -->

# KNN K近邻算法

## 概念介绍

KNN（K-Nearest Neighbors）是最直观、最简单的机器学习算法之一。它的思想非常朴素：要判断一个新样本属于哪个类别，就看它周围最近的K个邻居，哪个类别多就归为哪个类别。就像判断一个人的性格，看他最亲密的5个朋友是什么性格。

KNN是"懒惰学习"的代表——它不需要训练过程，只是把数据存起来，预测时才开始计算。优点是简单易懂，缺点是预测时计算量大。

## 核心原理

### 算法步骤

1. 选择K值和距离度量方式
2. 对新样本，计算它与所有训练样本的距离
3. 找到距离最近的K个样本（K个邻居）
4. 分类：投票，K个邻居中最多的类别即为预测结果
5. 回归：取K个邻居的目标值平均

### 距离度量

**欧氏距离**（最常用）：
```
d(x, y) = √(Σ(xᵢ - yᵢ)²)
```

**曼哈顿距离**：
```
d(x, y) = Σ|xᵢ - yᵢ|
```

**闵可夫斯基距离**（通用形式）：
```
d(x, y) = (Σ|xᵢ - yᵢ|ᵖ)^(1/p)
```

### K值选择

- K太小（如K=1）：对噪声敏感，容易过拟合
- K太大：边界模糊，容易欠拟合
- 通常用交叉验证选择最优K，一般取奇数（避免平票）

## 代码实现

```python
from sklearn.neighbors import KNeighborsClassifier, KNeighborsRegressor
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import accuracy_score
import numpy as np

# 加载数据
X, y = load_iris(return_X_y=True)

# KNN需要特征标准化
scaler = StandardScaler()
X = scaler.fit_transform(X)

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# 用交叉验证选择最优K
k_range = range(1, 21)
scores = []
for k in k_range:
    model = KNeighborsClassifier(n_neighbors=k)
    score = cross_val_score(model, X_train, y_train, cv=5).mean()
    scores.append(score)
    print(f"K={k:2d}, 交叉验证准确率: {score:.4f}")

best_k = list(k_range)[np.argmax(scores)]
print(f"\n最优K值: {best_k}")

# 用最优K训练最终模型
model = KNeighborsClassifier(n_neighbors=best_k)
model.fit(X_train, y_train)
y_pred = model.predict(X_test)
print(f"测试集准确率: {accuracy_score(y_test, y_pred):.4f}")
```

## 适用场景

- 数据量不大（样本数<10万）
- 分类边界不规则的场景
- 推荐系统的基础（协同过滤）
- 作为基线模型快速验证

## 常见易错点

1. **忘记做特征标准化**：KNN基于距离，特征尺度不同会导致大尺度特征主导距离
2. **K值不调优**：直接用默认K=5，应该用交叉验证选最优K
3. **大数据集用KNN**：预测时需要计算和所有训练样本的距离，O(n)复杂度，大数据很慢
4. **不考虑距离权重**：默认投票时所有邻居权重相同，可以用weights='distance'让近的邻居权重更大

## 练习题

1. **选择题**：KNN属于什么类型的学习？（A）急切学习 （B）懒惰学习 （C）强化学习 （D）半监督学习
   - 答案：B。KNN没有训练过程，是典型的懒惰学习。

2. **填空题**：KNN中K值过小容易导致____，K值过大容易导致____。
   - 答案：过拟合；欠拟合

3. **简答题**：为什么KNN需要做特征标准化？
   - 答案：KNN基于距离计算，如果某个特征的范围是0-1000，另一个是0-1，那么第一个特征会完全主导距离计算，导致第二个特征被忽略。

4. **编程题**：用KNN对手写数字数据集（load_digits）做分类，比较不同K值的效果。
   - 参考上面代码，数据集换成load_digits。

## 推荐阅读

- 西瓜书第10章
- Scikit-learn KNN文档：https://scikit-learn.org/stable/modules/neighbors.html
