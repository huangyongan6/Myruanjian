# PCA主成分分析

## 概念介绍

PCA（Principal Component Analysis，主成分分析）是最常用的降维算法。它的核心思想是：找到数据中变化最大的方向（主成分），把高维数据投影到这些方向上，用更少的维度来表示数据，同时尽可能保留原始信息。

打个比方：给一群人拍照，正面照比侧面照能保留更多面部信息。PCA就是找到那个"正面"方向，让投影后的数据信息损失最小。PCA广泛用于数据可视化（降到2D/3D画图）、去噪、特征压缩。

## 核心原理

### 算法步骤

1. 对数据进行标准化（均值为0，方差为1）
2. 计算协方差矩阵
3. 对协方差矩阵做特征值分解，得到特征值和特征向量
4. 按特征值从大到小排序，选择前k个特征向量
5. 用这k个特征向量将数据投影到k维空间

### 方差解释率

每个主成分解释了原始数据多少方差：

```
解释率 = λᵢ / Σλⱼ
```

选择主成分数时，通常要求累计解释率≥85%或95%。

### 数学表达

找到投影方向 w，使得投影后方差最大：

```
max wᵀΣw
s.t. ||w|| = 1
```

其中 Σ 是数据的协方差矩阵。解就是 Σ 的最大的特征值对应的特征向量。

## 代码实现

```python
from sklearn.decomposition import PCA
from sklearn.datasets import load_iris
from sklearn.preprocessing import StandardScaler
import matplotlib.pyplot as plt
import numpy as np

# 加载数据
iris = load_iris()
X = iris.data  # 4维特征
y = iris.target
target_names = iris.target_names

# 标准化
X_scaled = StandardScaler().fit_transform(X)

# ========== PCA降到2维 ==========
pca = PCA(n_components=2)
X_2d = pca.fit_transform(X_scaled)

print(f"各主成分方差解释率: {pca.explained_variance_ratio_}")
print(f"累计方差解释率: {pca.explained_variance_ratio_.sum():.4f}")

# 可视化
plt.figure(figsize=(8, 6))
for i, name in enumerate(target_names):
    mask = y == i
    plt.scatter(X_2d[mask, 0], X_2d[mask, 1], label=name, alpha=0.7)
plt.xlabel(f'PC1 ({pca.explained_variance_ratio_[0]:.1%})')
plt.ylabel(f'PC2 ({pca.explained_variance_ratio_[1]:.1%})')
plt.title('PCA降维可视化 - 鸢尾花数据集')
plt.legend()
plt.savefig("pca_iris.png", dpi=100)
plt.show()

# ========== 选择主成分数 ==========
pca_full = PCA().fit(X_scaled)
cumsum = np.cumsum(pca_full.explained_variance_ratio_)

plt.figure(figsize=(8, 4))
plt.plot(range(1, len(cumsum)+1), cumsum, 'bo-')
plt.axhline(y=0.95, color='r', linestyle='--', label='95%阈值')
plt.xlabel('主成分数量')
plt.ylabel('累计方差解释率')
plt.title('PCA累计方差解释率')
plt.legend()
plt.savefig("pca_cumsum.png", dpi=100)
plt.show()

n_95 = np.argmax(cumsum >= 0.95) + 1
print(f"保留95%方差需要的主成分数: {n_95}")
```

## 适用场景

- 高维数据可视化（降到2D/3D画图）
- 数据预处理（降维后作为模型输入，减少计算量）
- 去噪（去掉方差小的主成分，去除噪声）
- 特征压缩（减少存储和计算开销）

## 常见易错点

1. **忘记做标准化**：PCA对特征尺度敏感，必须先标准化
2. **降维后特征不可解释**：PCA的新特征是原始特征的线性组合，不再有直观含义
3. **非线性数据用PCA**：PCA只能捕捉线性结构，非线性数据应该用t-SNE或UMAP
4. **方差解释率太低**：如果前几个主成分解释率很低，说明数据没有明显的低维结构

## 练习题

1. **选择题**：PCA的目标是什么？（A）最大化类间距离 （B）最小化重构误差（等价于最大化投影方差） （C）最小化簇内距离 （D）最大化准确率
   - 答案：B

2. **填空题**：PCA通过____分解来找到主成分方向。
   - 答案：特征值（对协方差矩阵做特征值分解）

3. **简答题**：PCA和t-SNE的区别是什么？什么时候用哪个？
   - 答案：PCA是线性降维，速度快，保留全局结构，适合预处理和压缩；t-SNE是非线性降维，速度慢，保留局部结构，适合可视化。

4. **编程题**：对sklearn的digits数据集（64维）做PCA降到2维，画图观察数字是否可分。
   - 参考上面代码，数据集换成load_digits。

## 推荐阅读

- 西瓜书第10章
- Scikit-learn PCA文档：https://scikit-learn.org/stable/modules/decomposition.html
- Josh Starmer的PCA讲解视频（StatQuest）

<!-- ============================================ -->
<!-- 以下内容由 scripts/sync-knowledge.py 同步自顶层原稿 knowledge/ -->
<!-- 仅供阅读参考；正文以本文件原有章节为准，重复段落由维护者清理。 -->
<!-- ============================================ -->

# PCA主成分分析

## 概念介绍

PCA（Principal Component Analysis，主成分分析）是最常用的降维算法。它的核心思想是：找到数据中变化最大的方向（主成分），把高维数据投影到这些方向上，用更少的维度来表示数据，同时尽可能保留原始信息。

打个比方：给一群人拍照，正面照比侧面照能保留更多面部信息。PCA就是找到那个"正面"方向，让投影后的数据信息损失最小。PCA广泛用于数据可视化（降到2D/3D画图）、去噪、特征压缩。

## 核心原理

### 算法步骤

1. 对数据进行标准化（均值为0，方差为1）
2. 计算协方差矩阵
3. 对协方差矩阵做特征值分解，得到特征值和特征向量
4. 按特征值从大到小排序，选择前k个特征向量
5. 用这k个特征向量将数据投影到k维空间

### 方差解释率

每个主成分解释了原始数据多少方差：

```
解释率 = λᵢ / Σλⱼ
```

选择主成分数时，通常要求累计解释率≥85%或95%。

### 数学表达

找到投影方向w，使得投影后方差最大：

```
max wᵀΣw
s.t. ||w|| = 1
```

其中Σ是数据的协方差矩阵。解就是Σ的最大的特征值对应的特征向量。

## 代码实现

```python
from sklearn.decomposition import PCA
from sklearn.datasets import load_iris, fetch_openml
from sklearn.preprocessing import StandardScaler
import matplotlib.pyplot as plt
import numpy as np

# 加载数据
iris = load_iris()
X = iris.data  # 4维特征
y = iris.target
target_names = iris.target_names

# 标准化
X_scaled = StandardScaler().fit_transform(X)

# ========== PCA降到2维 ==========
pca = PCA(n_components=2)
X_2d = pca.fit_transform(X_scaled)

print(f"各主成分方差解释率: {pca.explained_variance_ratio_}")
print(f"累计方差解释率: {pca.explained_variance_ratio_.sum():.4f}")

# 可视化
plt.figure(figsize=(8, 6))
for i, name in enumerate(target_names):
    mask = y == i
    plt.scatter(X_2d[mask, 0], X_2d[mask, 1], label=name, alpha=0.7)
plt.xlabel(f'PC1 ({pca.explained_variance_ratio_[0]:.1%})')
plt.ylabel(f'PC2 ({pca.explained_variance_ratio_[1]:.1%})')
plt.title('PCA降维可视化 - 鸢尾花数据集')
plt.legend()
plt.savefig("pca_iris.png", dpi=100)
plt.show()

# ========== 选择主成分数 ==========
pca_full = PCA().fit(X_scaled)
cumsum = np.cumsum(pca_full.explained_variance_ratio_)

plt.figure(figsize=(8, 4))
plt.plot(range(1, len(cumsum)+1), cumsum, 'bo-')
plt.axhline(y=0.95, color='r', linestyle='--', label='95%阈值')
plt.xlabel('主成分数量')
plt.ylabel('累计方差解释率')
plt.title('PCA累计方差解释率')
plt.legend()
plt.savefig("pca_cumsum.png", dpi=100)
plt.show()

n_95 = np.argmax(cumsum >= 0.95) + 1
print(f"保留95%方差需要的主成分数: {n_95}")
```

## 适用场景

- 高维数据可视化（降到2D/3D画图）
- 数据预处理（降维后作为模型输入，减少计算量）
- 去噪（去掉方差小的主成分，去除噪声）
- 特征压缩（减少存储和计算开销）

## 常见易错点

1. **忘记做标准化**：PCA对特征尺度敏感，必须先标准化
2. **降维后特征不可解释**：PCA的新特征是原始特征的线性组合，不再有直观含义
3. **非线性数据用PCA**：PCA只能捕捉线性结构，非线性数据应该用t-SNE或UMAP
4. **方差解释率太低**：如果前几个主成分解释率很低，说明数据没有明显的低维结构

## 练习题

1. **选择题**：PCA的目标是什么？（A）最大化类间距离 （B）最小化重构误差（等价于最大化投影方差） （C）最小化簇内距离 （D）最大化准确率
   - 答案：B

2. **填空题**：PCA通过____分解来找到主成分方向。
   - 答案：特征值（对协方差矩阵做特征值分解）

3. **简答题**：PCA和t-SNE的区别是什么？什么时候用哪个？
   - 答案：PCA是线性降维，速度快，保留全局结构，适合预处理和压缩；t-SNE是非线性降维，速度慢，保留局部结构，适合可视化。

4. **编程题**：对sklearn的digits数据集（64维）做PCA降到2维，画图观察数字是否可分。
   - 参考上面代码，数据集换成load_digits。

## 推荐阅读

- 西瓜书第10章
- Scikit-learn PCA文档：https://scikit-learn.org/stable/modules/decomposition.html
- Josh Starmer的PCA讲解视频（StatQuest）
