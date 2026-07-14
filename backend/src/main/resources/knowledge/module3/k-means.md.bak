# K-Means聚类

## 概念介绍

K-Means是最经典的无监督学习聚类算法，它的目标是把数据分成K个组（簇），使得同一组内的数据尽量相似，不同组的数据尽量不同。就像把一堆水果按颜色分成几堆——红色一堆、黄色一堆、绿色一堆，不需要事先知道有哪些颜色。

K-Means简单高效，广泛应用于客户分群、图像分割、文档聚类等场景。但它的缺点是需要预先指定K值，且对初始中心点敏感。

## 核心原理

### 算法步骤

1. **初始化**：随机选择K个点作为初始聚类中心
2. **分配**：把每个样本分配到距离最近的聚类中心
3. **更新**：重新计算每个簇的中心点（簇内所有点的均值）
4. **重复**：反复执行步骤2和3，直到中心点不再变化或达到最大迭代次数

### 目标函数

K-Means的目标是最小化所有样本到其所属簇中心的距离之和：

```
J = Σᵢ₌₁ᴷ Σₓ∈Cᵢ ||x - μᵢ||²
```

其中Cᵢ是第i个簇，μᵢ是第i个簇的中心。

### K值选择

**肘部法则（Elbow Method）**：画出K值和目标函数J的关系图，找拐点。

**轮廓系数（Silhouette Score）**：衡量簇内紧密度和簇间分离度，取值-1到1，越大越好。

## 代码实现

```python
from sklearn.cluster import KMeans
from sklearn.datasets import make_blobs, load_iris
from sklearn.metrics import silhouette_score
from sklearn.preprocessing import StandardScaler
import matplotlib.pyplot as plt
import numpy as np

# 生成数据
X, y_true = make_blobs(n_samples=300, centers=4, cluster_std=0.6, random_state=42)

# 标准化
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# ========== 训练K-Means ==========
kmeans = KMeans(n_clusters=4, init='k-means++', n_init=10, random_state=42)
y_pred = kmeans.fit_predict(X_scaled)

print(f"簇中心:\n{kmeans.cluster_centers_}")
print(f"惯性(Inertia): {kmeans.inertia_:.2f}")
print(f"轮廓系数: {silhouette_score(X_scaled, y_pred):.4f}")

# ========== 肘部法则选择K ==========
inertias = []
silhouettes = []
K_range = range(2, 11)

for k in K_range:
    km = KMeans(n_clusters=k, init='k-means++', n_init=10, random_state=42)
    km.fit(X_scaled)
    inertias.append(km.inertia_)
    silhouettes.append(silhouette_score(X_scaled, km.labels_))

# 画肘部图
fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 4))
ax1.plot(K_range, inertias, 'bo-')
ax1.set_xlabel('K')
ax1.set_ylabel('Inertia')
ax1.set_title('肘部法则')

ax2.plot(K_range, silhouettes, 'ro-')
ax2.set_xlabel('K')
ax2.set_ylabel('轮廓系数')
ax2.set_title('轮廓系数法')

plt.tight_layout()
plt.savefig("kmeans_elbow.png", dpi=100)
plt.show()

# 找最优K
best_k = list(K_range)[np.argmax(silhouettes)]
print(f"\n最优K值（轮廓系数）: {best_k}")
```

## 适用场景

- 客户分群（电商、营销）
- 图像分割（颜色聚类）
- 文档聚类（新闻分组）
- 异常检测（离所有簇中心都很远的点）
- 数据预处理（聚类后作为新特征）

## 常见易错点

1. **不做标准化**：K-Means基于距离，特征尺度不同会影响聚类结果
2. **K值随意选**：应该用肘部法则或轮廓系数来选择K
3. **对非球形簇效果差**：K-Means假设簇是球形的，对长条形或不规则形状效果差
4. **对初始中心敏感**：使用k-means++初始化（默认）可以缓解

## 练习题

1. **选择题**：K-Means的目标是最小化什么？（A）簇间距离 （B）簇内距离之和 （C）轮廓系数 （D）误差平方和（B和D本质相同）
   - 答案：B/D

2. **填空题**：K-Means中，每轮迭代包含两个步骤：____和____。
   - 答案：分配（Assignment）和更新（Update）

3. **简答题**：K-Means的优缺点是什么？
   - 答案：优点：简单高效、可扩展性好。缺点：需要预设K值、对初始中心敏感、假设簇为球形、对异常值敏感。

4. **编程题**：用K-Means对客户消费数据做聚类分析，用肘部法则选择最优K。
   - 参考上面代码。

## 推荐阅读

- 西瓜书第9章
- Scikit-learn K-Means文档：https://scikit-learn.org/stable/modules/clustering.html
