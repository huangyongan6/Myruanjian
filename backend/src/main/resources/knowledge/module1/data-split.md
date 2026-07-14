# 数据集划分

## 概念介绍

在机器学习中，我们不会把所有数据都用来训练模型，而是把数据分成几份：一部分用来训练（让模型学习规律），一部分用来验证（调参和选择模型），一部分用来测试（最终评估模型的泛化能力）。这是因为我们真正关心的是模型在"没见过的数据"上的表现，而不是它在训练数据上能得几分。

数据集划分是防止"信息泄露"的关键。如果用测试集的数据来调参，那测试结果就不再可信——就像考试前老师把题目讲了一遍，考试分数再高也不能说明你真的掌握了知识。

## 核心原理

### 三类数据的角色

- **训练集（Train Set）**：用于模型参数学习。通常占 60%-80%。
- **验证集（Validation Set）**：用于超参数调整、模型选择。通常占 10%-20%。
- **测试集（Test Set）**：用于最终评估模型的泛化能力。通常占 10%-20%。

### 划分的原则

1. **数据分布一致**：训练、验证、测试集应保持相同的分布（都用随机划分）。
2. **样本互不重叠**：同一份数据不能同时出现在训练集和测试集。
3. **分层采样**：对分类问题，用 `stratify=y` 保证各类别比例一致。
4. **时间序列不随机**：对时间序列数据，按时间顺序切分（不能用未来的数据预测过去）。

### 交叉验证

数据量较少时，单次划分不够稳定，常用 K 折交叉验证：

- 把训练集等分成 K 份，每次用 K-1 份训练、1 份验证
- 重复 K 次，最终指标取平均
- 常用 5 折或 10 折

## 代码实现

```python
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split, KFold, cross_val_score
from sklearn.tree import DecisionTreeClassifier
from sklearn.metrics import accuracy_score

X, y = load_iris(return_X_y=True)

# ========== 简单划分（70/15/15） ==========
X_train, X_rest, y_train, y_rest = train_test_split(X, y, test_size=0.3, random_state=42, stratify=y)
X_val, X_test, y_val, y_test = train_test_split(X_rest, y_rest, test_size=0.5, random_state=42, stratify=y_rest)

print(f"训练集: {len(X_train)}, 验证集: {len(X_val)}, 测试集: {len(X_test)}")

# ========== K 折交叉验证 ==========
model = DecisionTreeClassifier(random_state=42)
kfold = KFold(n_splits=5, shuffle=True, random_state=42)
scores = cross_val_score(model, X, y, cv=kfold, scoring='accuracy')
print(f"5 折 CV 准确率: {scores.mean():.4f} ± {scores.std():.4f}")
```

## 适用场景

- 所有监督学习任务（分类、回归）
- 模型选择和超参数调优
- 评估模型的真实泛化能力

## 常见易错点

1. **没有验证集直接用测试集调参**：导致测试集评估偏高，泛化能力被高估。
2. **不设置 random_state**：每次运行结果不同，难以复现。
3. **忘记 stratify**：分类数据划分后各类别比例不一致，模型可能学偏。
4. **数据泄露**：用未来的数据训练（如用第 5 年的数据训练，预测第 1 年的数据）。

## 练习题

1. **选择题**：以下哪种划分方式最常用于小数据集？（A）70/30 划分 （B）80/20 划分 （C）5 折交叉验证 （D）50/50 划分
   - 答案：C

2. **填空题**：训练集用于____，验证集用于____，测试集用于____。
   - 答案：训练模型参数；调参与模型选择；最终评估泛化能力。

3. **简答题**：为什么分类问题要用 `stratify=y` 划分数据？
   - 答案：保证训练集、验证集、测试集中各类别比例与原始数据一致，避免某类样本过少或缺失导致模型学偏。

## 推荐阅读

- 周志华《机器学习》（西瓜书）第2章
- Scikit-learn 模型选择文档：https://scikit-learn.org/stable/model_selection.html

<!-- ============================================ -->
<!-- 以下内容由 scripts/sync-knowledge.py 同步自顶层原稿 knowledge/ -->
<!-- 仅供阅读参考；正文以本文件原有章节为准，重复段落由维护者清理。 -->
<!-- ============================================ -->

# 数据集划分

## 概念介绍

在机器学习中，我们不能把所有数据都用来训练模型，否则无法知道模型在新数据上的表现如何。这就像考试不能用练习题原题，否则分数再高也不能说明真正学会了。因此需要把数据分成不同的部分：训练集（Training Set）用来训练模型，验证集（Validation Set）用来调参选模型，测试集（Test Set）用来最终评估模型性能。

合理的数据划分是机器学习项目成功的基础。划分不当会导致模型评估失真——比如数据泄露（测试集信息泄露到训练过程），会让模型在测试集上表现很好，但实际使用时效果很差。

## 核心原理

### 常见划分比例

- **训练集 : 验证集 : 测试集 = 6 : 2 : 2**（数据量中等时常用）
- **训练集 : 测试集 = 8 : 2**（数据量较少时，不用验证集）
- **训练集 : 验证集 : 测试集 = 98 : 1 : 1**（大数据集时，如百万级数据）

### 三种划分方法

**1. 留出法（Hold-out）**
直接按比例随机划分，最简单最常用。

**2. K折交叉验证（K-Fold Cross Validation）**
把数据分成K份，轮流用其中1份做验证、剩下K-1份做训练，重复K次取平均。常用K=5或K=10。

**3. 留一法（Leave-One-Out）**
K折交叉验证的极端情况，K等于样本数。每次只留1个样本做验证。数据量小时使用。

### 交叉验证的数学表达

K折交叉验证的评估结果：

```
CV_score = (1/K) * Σ(score_i), i=1 to K
```

## 代码实现

```python
from sklearn.model_selection import train_test_split, KFold, cross_val_score
from sklearn.datasets import load_iris
from sklearn.tree import DecisionTreeClassifier
import numpy as np

# 加载数据
X, y = load_iris(return_X_y=True)

# ========== 方法1：留出法 ==========
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)
print(f"训练集大小: {len(X_train)}, 测试集大小: {len(X_test)}")

# ========== 方法2：K折交叉验证 ==========
model = DecisionTreeClassifier(random_state=42)
kf = KFold(n_splits=5, shuffle=True, random_state=42)
scores = cross_val_score(model, X, y, cv=kf)
print(f"5折交叉验证准确率: {scores}")
print(f"平均准确率: {scores.mean():.4f} ± {scores.std():.4f}")

# ========== 方法3：分层抽样（分类问题推荐） ==========
from sklearn.model_selection import StratifiedKFold
skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
scores = cross_val_score(model, X, y, cv=skf)
print(f"分层5折准确率: {scores.mean():.4f}")
```

## 适用场景

- **数据量大（>10万）**：用留出法即可，简单高效
- **数据量中等（1千-10万）**：用5折或10折交叉验证
- **数据量小（<1000）**：用留一法或K折交叉验证
- **分类问题**：用分层抽样（Stratified），保证每折中各类别比例一致
- **时间序列数据**：不能随机划分，要用时间顺序划分（前面训练，后面测试）

## 常见易错点

1. **数据泄露**：在划分之前做了全局标准化/特征选择，导致测试集信息泄露到训练过程
2. **随机性问题**：没有设置random_state，每次运行结果不同，无法复现
3. **分类问题不注意类别比例**：如果90%是正样本，模型全预测正也有90%准确率
4. **时间序列用随机划分**：时间序列数据必须按时间顺序划分，不能随机打乱

## 练习题

1. **选择题**：以下哪种情况适合使用10折交叉验证？（A）数据量100万 （B）数据量500 （C）时间序列数据 （D）二分类问题
   - 答案：B。数据量小时交叉验证更稳定，大数据集用留出法即可。

2. **填空题**：将数据集分为训练集和测试集时，通常测试集占比为____。
   - 答案：20%左右（即test_size=0.2）

3. **简答题**：什么是数据泄露？请举一个例子。
   - 答案：数据泄露是指测试集的信息在训练过程中被使用。例如，在划分数据前先对全部数据做标准化，这样测试集的均值和方差信息就泄露给了训练过程。

4. **编程题**：给定一个数据集，分别用留出法和5折交叉验证评估一个决策树模型，比较两种评估结果。
   - 参考上面的代码实现。

## 推荐阅读

- 吴恩达《机器学习》第7周（偏差方差与模型选择）
- Scikit-learn文档：Model Selection https://scikit-learn.org/stable/model_selection.html
- 西瓜书第2章
