# 房价预测（回归项目）

## 项目概述

本项目使用机器学习回归算法预测房价，是机器学习入门的经典实战项目。通过这个项目可以掌握完整的ML流程：数据加载 → 探索性分析 → 特征工程 → 模型训练 → 评估 → 调参。

## 数据集

使用 California Housing 数据集：
- 特征：收入、房龄、房间数、卧室数、人口、经纬度等
- 目标：房价中位数

## 完整代码

```python
import numpy as np
import pandas as pd
from sklearn.datasets import fetch_california_housing
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LinearRegression, Ridge, Lasso
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.metrics import mean_squared_error, r2_score, mean_absolute_error
import warnings
warnings.filterwarnings('ignore')

# ========== 1. 数据加载 ==========
data = fetch_california_housing()
X = pd.DataFrame(data.data, columns=data.feature_names)
y = data.target

# ========== 2. 特征工程 ==========
X['RoomsPerHousehold'] = X['AveRooms'] / X['AveOccup']
X['BedroomRatio'] = X['AveBedrms'] / X['AveRooms']

# 划分训练 / 测试集
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# 标准化
scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)

# ========== 3. 多模型对比 ==========
models = {
    'LinearRegression': LinearRegression(),
    'Ridge': Ridge(alpha=1.0),
    'Lasso': Lasso(alpha=0.01),
    'RandomForest': RandomForestRegressor(n_estimators=100, random_state=42),
    'GBDT': GradientBoostingRegressor(n_estimators=100, random_state=42),
}

results = {}
for name, model in models.items():
    scores = cross_val_score(model, X_train_scaled, y_train, cv=5, scoring='neg_mean_squared_error')
    rmse = np.sqrt(-scores.mean())
    results[name] = rmse
    print(f"{name:20s} CV-RMSE: {rmse:.4f}")

# ========== 4. 最佳模型评估 ==========
best_model_name = min(results, key=results.get)
print(f"\n最佳模型: {best_model_name}")

best_model = models[best_model_name]
best_model.fit(X_train_scaled, y_train)
y_pred = best_model.predict(X_test_scaled)

print(f"测试集 RMSE: {np.sqrt(mean_squared_error(y_test, y_pred)):.4f}")
print(f"测试集 MAE:  {mean_absolute_error(y_test, y_pred):.4f}")
print(f"测试集 R²:   {r2_score(y_test, y_pred):.4f}")
```

## 关键知识点

- 回归问题的评估指标：RMSE、MAE、R²
- 特征工程：构造新特征（RoomsPerHousehold 等）
- 多模型对比和选择
- 交叉验证评估

## 学完应该掌握

完整的 ML 回归流程：数据探索 → 特征工程 → 多模型对比 → 评估 → 可视化

## 推荐阅读

- Scikit-learn 官方教程回归部分
- Kaggle Learn《Intro to Machine Learning》

<!-- ============================================ -->
<!-- 以下内容由 scripts/sync-knowledge.py 同步自顶层原稿 knowledge/ -->
<!-- 仅供阅读参考；正文以本文件原有章节为准，重复段落由维护者清理。 -->
<!-- ============================================ -->

# 房价预测（回归项目）

## 项目概述

本项目使用机器学习回归算法预测房价，是机器学习入门的经典实战项目。通过这个项目可以掌握完整的ML流程：数据加载→探索性分析→特征工程→模型训练→评估→调参。

## 数据集

使用California Housing数据集（或Kaggle的House Prices数据集）：
- 特征：收入、房龄、房间数、卧室数、人口、经纬度等
- 目标：房价中位数

## 完整代码

```python
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from sklearn.datasets import fetch_california_housing
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LinearRegression, Ridge, Lasso
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.metrics import mean_squared_error, r2_score, mean_absolute_error
import warnings
warnings.filterwarnings('ignore')

# ========== 1. 数据加载与探索 ==========
data = fetch_california_housing()
X = pd.DataFrame(data.data, columns=data.feature_names)
y = data.target

print("数据形状:", X.shape)
print("\n特征统计:")
print(X.describe())
print("\n目标变量统计:")
print(f"均值: {y.mean():.4f}, 标准差: {y.std():.4f}, 最小: {y.min():.4f}, 最大: {y.max():.4f}")

# ========== 2. 数据可视化 ==========
fig, axes = plt.subplots(2, 2, figsize=(12, 10))
axes[0,0].hist(y, bins=50, edgecolor='black')
axes[0,0].set_title('房价分布')
axes[0,0].set_xlabel('房价')

axes[0,1].scatter(X['MedInc'], y, alpha=0.1, s=5)
axes[0,1].set_title('收入 vs 房价')
axes[0,1].set_xlabel('收入')
axes[0,1].set_ylabel('房价')

axes[1,0].scatter(X['AveRooms'], y, alpha=0.1, s=5)
axes[1,0].set_title('房间数 vs 房价')

axes[1,1].scatter(X['HouseAge'], y, alpha=0.1, s=5)
axes[1,1].set_title('房龄 vs 房价')

plt.tight_layout()
plt.savefig("house_price_eda.png", dpi=100)
plt.show()

# ========== 3. 特征工程 ==========
# 添加新特征
X['RoomsPerHousehold'] = X['AveRooms'] / X['AveOccup']
X['BedroomRatio'] = X['AveBedrms'] / X['AveRooms']

# 划分数据集
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# 标准化
scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)

# ========== 4. 多模型对比 ==========
models = {
    'LinearRegression': LinearRegression(),
    'Ridge': Ridge(alpha=1.0),
    'Lasso': Lasso(alpha=0.01),
    'RandomForest': RandomForestRegressor(n_estimators=100, random_state=42),
    'GBDT': GradientBoostingRegressor(n_estimators=100, random_state=42),
}

results = {}
for name, model in models.items():
    scores = cross_val_score(model, X_train_scaled, y_train, cv=5, scoring='neg_mean_squared_error')
    rmse = np.sqrt(-scores.mean())
    results[name] = rmse
    print(f"{name:20s} CV-RMSE: {rmse:.4f} ± {np.sqrt(-scores).std():.4f}")

# ========== 5. 最佳模型训练与评估 ==========
best_model_name = min(results, key=results.get)
print(f"\n最佳模型: {best_model_name}")

best_model = models[best_model_name]
best_model.fit(X_train_scaled, y_train)
y_pred = best_model.predict(X_test_scaled)

print(f"测试集RMSE: {np.sqrt(mean_squared_error(y_test, y_pred)):.4f}")
print(f"测试集MAE: {mean_absolute_error(y_test, y_pred):.4f}")
print(f"测试集R²: {r2_score(y_test, y_pred):.4f}")

# 预测 vs 真实值散点图
plt.figure(figsize=(8, 6))
plt.scatter(y_test, y_pred, alpha=0.3, s=10)
plt.plot([y_test.min(), y_test.max()], [y_test.min(), y_test.max()], 'r--')
plt.xlabel('真实值')
plt.ylabel('预测值')
plt.title('房价预测：真实值 vs 预测值')
plt.savefig("house_price_pred.png", dpi=100)
plt.show()
```

## 关键知识点

- 回归问题的评估指标：RMSE、MAE、R²
- 特征工程：构造新特征
- 多模型对比和选择
- 交叉验证评估

## 学完应该掌握

完整的ML回归流程：数据探索→特征工程→多模型对比→评估→可视化
