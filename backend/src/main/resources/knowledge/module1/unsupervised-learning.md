# 无监督学习

## 概念介绍

无监督学习（Unsupervised Learning）是给计算机"没有答案"的数据，让它自己发现数据中的结构和规律。就像让学生看一堆水果自己分类——没有老师告诉他"这是苹果、这是香蕉"，他只能根据颜色、形状、味道等特征把相似的水果归到一组。

无监督学习的典型任务：
- **聚类（Clustering）**：把相似数据分成一组（如客户分群、新闻聚合）。
- **降维（Dimensionality Reduction）**：压缩特征维度同时保留关键信息（如 PCA、t-SNE）。
- **密度估计**：找到数据的高密度区域。
- **异常检测**：找出与大多数数据不同的样本。

## 核心原理

### 与监督学习的关键区别

| 维度 | 监督学习 | 无监督学习 |
|------|---------|-----------|
| 数据 | 有标签 (x, y) | 只有 x |
| 目标 | 学 f: X → Y | 发现 X 内部结构 |
| 评估 | 有标签可量化 | 难度大，靠业务解释 |

### 常用算法

| 类别 | 典型算法 | 适用场景 |
|-----|---------|---------|
| 聚类 | K-Means、层次聚类、DBSCAN | 客户分群、图像分割 |
| 降维 | PCA、t-SNE、UMAP | 可视化、特征压缩、噪声去除 |
| 概率模型 | 高斯混合、EM | 密度估计、生成式建模 |
| 关联规则 | Apriori、FP-Growth | 购物篮分析 |

每个算法的原理详见 module3。

### 评估难点

无监督学习没有标签，常用以下方式评估：
- **内部指标**：轮廓系数、Calinski-Harabasz、戴维森堡丁指数（仅基于数据本身）。
- **业务指标**：聚类后业务上是否讲得通、客户分群后营销转化是否提升。

## 代码实现

```python
import numpy as np
from sklearn.datasets import make_blobs, load_iris
from sklearn.cluster import KMeans
from sklearn.decomposition import PCA
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import silhouette_score

# ========== 聚类示例 ==========
X, _ = make_blobs(n_samples=300, centers=4, cluster_std=0.6, random_state=42)
X_scaled = StandardScaler().fit_transform(X)

kmeans = KMeans(n_clusters=4, init='k-means++', n_init=10, random_state=42)
labels = kmeans.fit_predict(X_scaled)

print(f"[聚类] 轮廓系数: {silhouette_score(X_scaled, labels):.4f}")
print(f"[聚类] 簇中心:\n{kmeans.cluster_centers_}")

# ========== 降维示例 ==========
iris = load_iris()
X_iris = StandardScaler().fit_transform(iris.data)
X_2d = PCA(n_components=2).fit_transform(X_iris)

print(f"\n[PCA] 累计方差解释率: {PCA(n_components=2).fit(X_iris).explained_variance_ratio_.sum():.4f}")
print(f"[PCA] 降维后形状: {X_2d.shape}")
```

## 适用场景

- 没有标签但希望理解数据结构
- 数据预处理（聚类特征、PCA 降维）
- 推荐系统的"相似物品"
- 异常检测（如欺诈检测）
- 数据可视化（降到 2D/3D 画图）

## 常见易错点

1. **聚类结果当"真"标签**：聚类算法只保证簇内相似、簇间不同，不一定对应业务分类。
2. **不做标准化**：K-Means、PCA 都对特征尺度敏感，必须先标准化。
3. **K 随意选**：应该用肘部法则或轮廓系数选择 K（见 module3）。
4. **评估只看单一指标**：轮廓系数高不代表业务上有效，必须结合业务解读。

## 练习题

1. **选择题**：以下哪个属于无监督学习？（A）垃圾邮件分类 （B）客户分群 （C）房价预测 （D）手写数字识别
   - 答案：B。客户分群没有标签，需要算法自己发现分组。

2. **填空题**：两种典型的无监督学习任务是____和____。
   - 答案：聚类；降维。

3. **简答题**：聚类评估为什么比分类评估难？
   - 答案：分类问题有真实标签可直接比较；聚类没有"正确答案"，只能间接用轮廓系数等内部指标，或结合业务效果判断。

## 推荐阅读

- 周志华《机器学习》（西瓜书）第9章
- Scikit-learn 无监督学习文档：https://scikit-learn.org/stable/unsupervised_learning.html
