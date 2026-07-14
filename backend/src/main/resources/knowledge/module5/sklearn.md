# Scikit-learn 模型训练

## 概念介绍

Scikit-learn 是 Python 最流行的机器学习库，提供了统一的 API 接口，涵盖了分类、回归、聚类、降维、预处理等功能。它的设计哲学是"一致的接口"——所有模型都遵循 `fit` / `predict` / `transform` 的 API，学一个就会所有。

Scikit-learn 是机器学习入门和快速原型开发的首选工具，几乎所有经典的机器学习算法都能在里面找到。

## 核心原理

### 统一 API

```
# 分类/回归
model.fit(X_train, y_train)      # 训练
y_pred = model.predict(X_test)   # 预测
score = model.score(X_test, y_test)  # 评估

# 预处理/降维
scaler.fit_transform(X_train)    # 训练 + 转换
scaler.transform(X_test)         # 只转换（用训练集的参数）
```

### Pipeline（管道）

把多个步骤串联成一个流水线，避免数据泄露：

```python
Pipeline([
    ('scaler', StandardScaler()),
    ('pca', PCA(n_components=50)),
    ('clf', LogisticRegression())
])
```

## 代码实现

```python
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.decomposition import PCA
from sklearn.model_selection import train_test_split, GridSearchCV, cross_val_score
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.svm import SVC
from sklearn.datasets import load_iris
from sklearn.metrics import classification_report
import numpy as np

# 加载数据
X, y = load_iris(return_X_y=True)
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# ========== Pipeline：预处理 + 模型 ==========
pipe = Pipeline([
    ('scaler', StandardScaler()),
    ('clf', LogisticRegression(max_iter=1000))
])
pipe.fit(X_train, y_train)
print(f"Pipeline准确率: {pipe.score(X_test, y_test):.4f}")

# ========== GridSearchCV：自动调参 ==========
param_grid = {
    'clf__C': [0.1, 1, 10],
    'clf__solver': ['lbfgs', 'liblinear']
}
grid = GridSearchCV(pipe, param_grid, cv=5, scoring='accuracy')
grid.fit(X_train, y_train)
print(f"最佳参数: {grid.best_params_}")
print(f"最佳分数: {grid.best_score_:.4f}")

# ========== 多模型对比 ==========
models = {
    'LogisticRegression': LogisticRegression(max_iter=1000),
    'RandomForest': RandomForestClassifier(n_estimators=100),
    'SVM': SVC()
}

for name, model in models.items():
    pipe = Pipeline([
        ('scaler', StandardScaler()),
        ('clf', model)
    ])
    scores = cross_val_score(pipe, X_train, y_train, cv=5)
    print(f"{name}: {scores.mean():.4f} ± {scores.std():.4f}")
```

## 适用场景

- 快速原型开发和模型对比
- 传统机器学习任务（分类、回归、聚类）
- Kaggle 竞赛的基线模型
- 教学和学习

## 常见易错点

1. **在全量数据上 fit**：应该只在训练集上 fit，测试集只 transform
2. **不用 Pipeline**：分开做预处理容易导致数据泄露
3. **不设置 random_state**：结果不可复现
4. **忽略交叉验证**：只看一次 train_test_split 的结果不够稳定

## 练习题

1. 用 Pipeline 构建一个完整的 ML 流水线（预处理 + 降维 + 分类）
2. 用 GridSearchCV 对随机森林做调参
3. 对比3个不同模型在同一数据集上的表现

## 推荐阅读

- Scikit-learn 官方教程：https://scikit-learn.org/stable/tutorial/
- 《Hands-On Machine Learning》第2章

<!-- ============================================ -->
<!-- 以下内容由 scripts/sync-knowledge.py 同步自顶层原稿 knowledge/ -->
<!-- 仅供阅读参考；正文以本文件原有章节为准，重复段落由维护者清理。 -->
<!-- ============================================ -->

# Scikit-learn模型训练

## 概念介绍

Scikit-learn是Python最流行的机器学习库，提供了统一的API接口，涵盖了分类、回归、聚类、降维、预处理等功能。它的设计哲学是"一致的接口"——所有模型都遵循fit/predict/transform的API，学一个就会所有。

Scikit-learn是机器学习入门和快速原型开发的首选工具，几乎所有经典的机器学习算法都能在里面找到。

## 核心原理

### 统一API

```
# 分类/回归
model.fit(X_train, y_train)      # 训练
y_pred = model.predict(X_test)   # 预测
score = model.score(X_test, y_test)  # 评估

# 预处理/降维
scaler.fit_transform(X_train)    # 训练+转换
scaler.transform(X_test)         # 只转换（用训练集的参数）
```

### Pipeline（管道）

把多个步骤串联成一个流水线，避免数据泄露：

```python
Pipeline([
    ('scaler', StandardScaler()),
    ('pca', PCA(n_components=50)),
    ('clf', LogisticRegression())
])
```

## 代码实现

```python
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.decomposition import PCA
from sklearn.model_selection import train_test_split, GridSearchCV, cross_val_score
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.svm import SVC
from sklearn.datasets import load_iris
from sklearn.metrics import classification_report
import numpy as np

# 加载数据
X, y = load_iris(return_X_y=True)
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# ========== Pipeline：预处理 + 模型 ==========
pipe = Pipeline([
    ('scaler', StandardScaler()),
    ('clf', LogisticRegression(max_iter=1000))
])
pipe.fit(X_train, y_train)
print(f"Pipeline准确率: {pipe.score(X_test, y_test):.4f}")

# ========== GridSearchCV：自动调参 ==========
param_grid = {
    'clf__C': [0.1, 1, 10],
    'clf__solver': ['lbfgs', 'liblinear']
}
grid = GridSearchCV(pipe, param_grid, cv=5, scoring='accuracy')
grid.fit(X_train, y_train)
print(f"最佳参数: {grid.best_params_}")
print(f"最佳分数: {grid.best_score_:.4f}")

# ========== 多模型对比 ==========
models = {
    'LogisticRegression': LogisticRegression(max_iter=1000),
    'RandomForest': RandomForestClassifier(n_estimators=100),
    'SVM': SVC()
}

for name, model in models.items():
    pipe = Pipeline([
        ('scaler', StandardScaler()),
        ('clf', model)
    ])
    scores = cross_val_score(pipe, X_train, y_train, cv=5)
    print(f"{name}: {scores.mean():.4f} ± {scores.std():.4f}")
```

## 适用场景

- 快速原型开发和模型对比
- 传统机器学习任务（分类、回归、聚类）
- Kaggle竞赛的基线模型
- 教学和学习

## 常见易错点

1. **在全量数据上fit**：应该只在训练集上fit，测试集只transform
2. **不用Pipeline**：分开做预处理容易导致数据泄露
3. **不设置random_state**：结果不可复现
4. **忽略交叉验证**：只看一次train_test_split的结果不够稳定

## 练习题

1. 用Pipeline构建一个完整的ML流水线（预处理+降维+分类）
2. 用GridSearchCV对随机森林做调参
3. 对比3个不同模型在同一数据集上的表现

## 推荐阅读

- Scikit-learn官方教程：https://scikit-learn.org/stable/tutorial/
- 《Hands-On Machine Learning》第2章
