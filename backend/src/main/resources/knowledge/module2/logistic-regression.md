# 逻辑回归

## 一、概念介绍

逻辑回归（Logistic Regression）虽然名字里有「回归」，但实际是**分类**算法，主要用于二分类任务（如垃圾邮件识别、疾病诊断）。它通过 Sigmoid 函数将线性输出映射到 (0, 1) 区间，表示样本属于正类的概率。

## 二、核心原理

### 2.1 数学推导

Sigmoid 函数：

$$\sigma(z) = \frac{1}{1 + e^{-z}}$$

预测概率：

$$P(y=1 | \mathbf{x}) = \sigma(\mathbf{w}^\top \mathbf{x} + b) = \frac{1}{1 + e^{-(\mathbf{w}^\top \mathbf{x} + b)}}$$

损失函数（交叉熵 / 对数损失）：

$$L = -\frac{1}{m} \sum_{i=1}^{m} \left[ y^{(i)} \log \hat{y}^{(i)} + (1 - y^{(i)}) \log (1 - \hat{y}^{(i)}) \right]$$

### 2.2 算法流程

1. 准备数据（特征 + 二元标签）
2. 特征归一化 / 标准化
3. 初始化 $\mathbf{w}$、$b$
4. 迭代训练：计算 $\hat{y}$ → 计算交叉熵损失 → 反向传播
5. 阈值划分（默认 0.5）→ 计算准确率 / F1 / AUC

## 三、代码实现

```python
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report

# 以鸢尾花二分类为例（仅取 setosa vs versicolor）
from sklearn.datasets import load_iris
iris = load_iris()
X = iris.data[:100]
y = iris.target[:100]

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

model = LogisticRegression(max_iter=200)
model.fit(X_train, y_train)

y_pred = model.predict(X_test)
print(classification_report(y_test, y_pred))
```

**预期输出**：

```
              precision    recall  f1-score   support
           0       1.00      1.00      1.00         9
           1       1.00      1.00      1.00        11
    accuracy                           1.00        20
```

## 四、适用场景

✅ **适合**：
- 二分类任务
- 需要输出概率（而非仅类别）
- 对模型可解释性要求高（系数即特征贡献）

❌ **不适合**：
- 多分类且类别数很多（考虑 Softmax 回归或神经网络）
- 特征与目标关系高度非线性

## 五、常见易错点

1. **未做特征归一化**：梯度下降收敛慢或震荡
2. **类别严重不平衡**：准确率高但召回率为 0，应使用 `class_weight='balanced'` 或过采样
3. **多分类直接套二分类**：应使用 `LogisticRegression(multi_class='multinomial')`

## 六、练习题

1. 选择题：逻辑回归的输出值范围是？
   - [ ] A. [-1, 1]
   - [x] B. (0, 1)
   - [ ] C. [0, 1]
   - [ ] D. 全体实数

2. 简答题：解释 Sigmoid 函数为什么能将线性输出转为概率。

## 七、拓展阅读

- 周志华《机器学习》第 3 章 3.3 节
- Scikit-learn：https://scikit-learn.org/stable/modules/linear_model.html#logistic-regression

<!-- ============================================ -->
<!-- 以下内容由 scripts/sync-knowledge.py 同步自顶层原稿 knowledge/ -->
<!-- 仅供阅读参考；正文以本文件原有章节为准，重复段落由维护者清理。 -->
<!-- ============================================ -->

# 逻辑回归

## 概念介绍

逻辑回归（Logistic Regression）虽然名字里有"回归"，但它实际上是一个**分类算法**，是分类问题中最基础、最常用的算法之一。它的核心思想是：在线性回归的基础上，通过Sigmoid函数将输出压缩到0-1之间，表示属于某个类别的概率。

逻辑回归广泛应用于二分类问题：垃圾邮件识别（是/否）、疾病诊断（阳性/阴性）、用户是否点击广告等。它简单高效，可解释性强，是面试和竞赛中的高频考点。

## 核心原理

### Sigmoid函数

逻辑回归的核心是Sigmoid函数，它把任意实数映射到(0,1)区间：

```
σ(z) = 1 / (1 + e⁻ᶻ)
```

其中 z = wᵀx + b（线性部分）

输出σ(z)可以理解为样本属于正类的概率：P(y=1|x)

### 决策规则

- 如果σ(z) ≥ 0.5，预测为正类（y=1）
- 如果σ(z) < 0.5，预测为负类（y=0）

### 损失函数

逻辑回归使用**交叉熵损失**（也叫对数损失），而不是均方误差：

```
L = -(1/n) * Σ[yᵢlog(ŷᵢ) + (1-yᵢ)log(1-ŷᵢ)]
```

为什么不用MSE？因为Sigmoid+MSE的损失函数是非凸的，有很多局部最优；而交叉熵损失是凸函数，能保证找到全局最优。

### 多分类扩展

- **OvR（One-vs-Rest）**：训练K个二分类器，每个区分"该类 vs 其他所有类"
- **Softmax**：直接扩展为多项逻辑回归，输出属于每个类别的概率

## 代码实现

```python
from sklearn.linear_model import LogisticRegression
from sklearn.datasets import load_breast_cancer
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report, roc_auc_score
from sklearn.preprocessing import StandardScaler

# 加载乳腺癌数据集（二分类）
data = load_breast_cancer()
X, y = data.data, data.target

# 特征标准化（逻辑回归对特征尺度敏感）
scaler = StandardScaler()
X = scaler.fit_transform(X)

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# 训练逻辑回归
model = LogisticRegression(max_iter=1000, random_state=42)
model.fit(X_train, y_train)

# 预测
y_pred = model.predict(X_test)
y_proba = model.predict_proba(X_test)[:, 1]  # 预测概率

print(f"准确率: {accuracy_score(y_test, y_pred):.4f}")
print(f"AUC: {roc_auc_score(y_test, y_proba):.4f}")
print(f"\n分类报告:\n{classification_report(y_test, y_pred, target_names=data.target_names)}")

# 查看特征权重（可解释性）
importance = sorted(zip(data.feature_names, model.coef_[0]), key=lambda x: abs(x[1]), reverse=True)
print("\nTop5重要特征:")
for name, weight in importance[:5]:
    print(f"  {name}: {weight:.4f}")
```

## 适用场景

- 二分类问题：垃圾邮件、疾病诊断、用户流失预测
- 需要概率输出的场景（不只是分类结果，还要知道概率多大）
- 需要可解释性的场景（可以直接看特征权重）
- 作为基线模型，快速验证问题可行性

## 常见易错点

1. **忘记做特征标准化**：逻辑回归使用正则化时，特征尺度不同会导致正则化效果不一致
2. **把逻辑回归当回归算法**：名字有"回归"但它是分类算法
3. **混淆概率和分类结果**：predict_proba返回概率，predict返回类别
4. **阈值固定为0.5**：有些场景需要调整阈值，如癌症筛查可以降低阈值提高召回率

## 练习题

1. **选择题**：逻辑回归使用什么函数将输出映射到0-1之间？（A）ReLU （B）Tanh （C）Sigmoid （D）Softmax
   - 答案：C。二分类用Sigmoid，多分类用Softmax。

2. **选择题**：逻辑回归的损失函数是？（A）MSE （B）交叉熵 （C）Hinge Loss （D）MAE
   - 答案：B

3. **简答题**：为什么逻辑回归不用均方误差作为损失函数？
   - 答案：因为Sigmoid+MSE是非凸函数，有多个局部最优解；而交叉熵损失是凸函数，能保证梯度下降收敛到全局最优。

4. **编程题**：用逻辑回归做一个鸢尾花分类（三分类），打印分类报告。
   - 参考上面代码，数据集换成load_iris。

## 推荐阅读

- 吴恩达《机器学习》第3周
- 西瓜书第3章
- 李航《统计学习方法》第6章
