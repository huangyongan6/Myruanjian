# SVM支持向量机

## 概念介绍

支持向量机（Support Vector Machine, SVM）是一种强大的分类算法，它的核心思想是找到一个"最佳分割超平面"，使得不同类别的样本之间的间隔（margin）最大化。想象在桌子上混着红球和蓝球，SVM就是要找到一条线（或一个面），把红蓝球分开，并且让离线最近的球距离线越远越好。

SVM在小样本、高维数据上表现优秀，是传统机器学习中最强大的分类器之一。配合核函数，SVM还能处理非线性分类问题。

## 核心原理

### 硬间隔SVM

假设数据线性可分，找一个超平面 wᵀx + b = 0，使得：

```
正类：wᵀx + b ≥ 1
负类：wᵀx + b ≤ -1
```

间隔 = 2/||w||，目标是最大化间隔，等价于：

```
min (1/2)||w||²
s.t. yᵢ(wᵀxᵢ + b) ≥ 1, ∀i
```

### 软间隔SVM

实际数据通常不是完美线性可分的，引入松弛变量ξᵢ：

```
min (1/2)||w||² + C * Σξᵢ
s.t. yᵢ(wᵀxᵢ + b) ≥ 1 - ξᵢ, ξᵢ ≥ 0
```

C越大，对错误分类的惩罚越重，模型越复杂；C越小，允许更多错误，模型越简单。

### 核函数（处理非线性数据）

核函数将数据映射到高维空间，使原本线性不可分的数据变得线性可分。

常用核函数：
- **线性核**：K(x,z) = xᵀz（线性可分时使用）
- **多项式核**：K(x,z) = (xᵀz + c)ᵈ
- **RBF高斯核**：K(x,z) = exp(-γ||x-z||²)（最常用，默认选择）
- **Sigmoid核**：K(x,z) = tanh(αxᵀz + c)

## 代码实现

```python
from sklearn.svm import SVC, SVR
from sklearn.datasets import load_iris, make_moons
from sklearn.model_selection import train_test_split, GridSearchCV
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import accuracy_score
import numpy as np

# 加载数据
X, y = load_iris(return_X_y=True)

# SVM必须做特征标准化！
scaler = StandardScaler()
X = scaler.fit_transform(X)

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# ========== 线性SVM ==========
svm_linear = SVC(kernel='linear', C=1.0, random_state=42)
svm_linear.fit(X_train, y_train)
print(f"线性SVM准确率: {accuracy_score(y_test, svm_linear.predict(X_test)):.4f}")

# ========== RBF核SVM ==========
svm_rbf = SVC(kernel='rbf', C=1.0, gamma='scale', random_state=42)
svm_rbf.fit(X_train, y_train)
print(f"RBF核SVM准确率: {accuracy_score(y_test, svm_rbf.predict(X_test)):.4f}")

# ========== 网格搜索调参 ==========
param_grid = {
    'C': [0.1, 1, 10, 100],
    'gamma': ['scale', 'auto', 0.01, 0.1],
    'kernel': ['rbf', 'linear']
}
grid = GridSearchCV(SVC(random_state=42), param_grid, cv=5, scoring='accuracy', n_jobs=-1)
grid.fit(X_train, y_train)
print(f"\n最佳参数: {grid.best_params_}")
print(f"最佳分数: {grid.best_score_:.4f}")
```

## 适用场景

- 小样本高维数据（文本分类、基因分类）
- 二分类问题表现优异
- 需要最大化分类间隔的场景
- 数据维度高于样本数的场景

## 常见易错点

1. **忘记做特征标准化**：SVM对特征尺度非常敏感，必须标准化
2. **RBF核不调gamma**：gamma过大容易过拟合，过小容易欠拟合
3. **大数据集用SVM**：SVM训练时间复杂度O(n²~n³)，数据量超过10万不建议用
4. **不理解C的作用**：C大→过拟合，C小→欠拟合

## 练习题

1. **选择题**：SVM中"支持向量"是指什么？（A）所有训练样本 （B）离超平面最近的样本 （C）被错误分类的样本 （D）测试样本
   - 答案：B。支持向量是离超平面最近的那些样本，决定了超平面的位置。

2. **选择题**：以下哪个核函数最常用？（A）线性核 （B）多项式核 （C）RBF高斯核 （D）Sigmoid核
   - 答案：C

3. **简答题**：SVM为什么需要做特征标准化？
   - 答案：SVM基于距离计算间隔，如果特征尺度不同，大尺度的特征会主导距离计算，导致模型偏向这些特征。

4. **编程题**：用make_moons生成非线性可分数据，分别用线性核和RBF核SVM分类，对比效果。
   - 参考上面代码。

## 推荐阅读

- 李航《统计学习方法》第7章
- 西瓜书第6章
- Scikit-learn SVM文档：https://scikit-learn.org/stable/modules/svm.html
