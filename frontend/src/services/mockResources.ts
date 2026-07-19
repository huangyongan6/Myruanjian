import type { LearningResource } from '@/types/resource'

/**
 * 学习路径页用的推荐形态（与 RecommendService.RecommendedResource 对齐）。
 */
export interface MockRecommendedResource {
  resource: LearningResource
  reason: string
}

/**
 * 资源中心 Mock 数据。
 *
 * <p>共 200 条，覆盖 5 种资源类型和 20 个机器学习核心知识点。
 *
 * <p>知识点（20个）：
 * 1.线性回归 2.逻辑回归 3.决策树 4.朴素贝叶斯 5.SVM 6.K-Means 7.PCA 8.神经网络
 * 9.CNN 10.RNN 11.梯度下降 12.过拟合与正则化 13.评估指标 14.特征工程 15.集成学习
 * 16.激活函数 17.损失函数 18.优化器 19.文本处理 20.推荐系统
 *
 * <p>内容来源：吴恩达 Coursera、周志华《机器学习》、Scikit-learn 官方文档、arXiv 论文等。
 */

// ═══════════════════════════════════════════════════════════════
// 知识点常量
// ═══════════════════════════════════════════════════════════════
const TOPICS = [
  { id: 'linear', name: '线性回归', en: 'Linear Regression' },
  { id: 'logistic', name: '逻辑回归', en: 'Logistic Regression' },
  { id: 'dt', name: '决策树', en: 'Decision Tree' },
  { id: 'nb', name: '朴素贝叶斯', en: 'Naive Bayes' },
  { id: 'svm', name: '支持向量机', en: 'SVM' },
  { id: 'kmeans', name: 'K-Means', en: 'K-Means Clustering' },
  { id: 'pca', name: 'PCA降维', en: 'PCA' },
  { id: 'nn', name: '神经网络', en: 'Neural Network' },
  { id: 'cnn', name: '卷积神经网络', en: 'CNN' },
  { id: 'rnn', name: '循环神经网络', en: 'RNN' },
  { id: 'gd', name: '梯度下降', en: 'Gradient Descent' },
  { id: 'overfit', name: '过拟合与正则化', en: 'Overfitting & Regularization' },
  { id: 'metrics', name: '评估指标', en: 'Evaluation Metrics' },
  { id: 'feature', name: '特征工程', en: 'Feature Engineering' },
  { id: 'ensemble', name: '集成学习', en: 'Ensemble Learning' },
  { id: 'activation', name: '激活函数', en: 'Activation Function' },
  { id: 'loss', name: '损失函数', en: 'Loss Function' },
  { id: 'optimizer', name: '优化器', en: 'Optimizer' },
  { id: 'nlp', name: '文本处理', en: 'NLP' },
  { id: 'recommend', name: '推荐系统', en: 'Recommendation System' }
]

// ═══════════════════════════════════════════════════════════════
// DOC 数据 (45条，每知识点2-3篇)
// ═══════════════════════════════════════════════════════════════

// 线性回归文档
const docLinear1 = {
  markdown: `# 线性回归详解

## 概述
线性回归是监督学习中用于回归任务的基础算法，通过最小化预测值与真实值的均方误差来学习线性参数。

## 核心公式
**预测函数**：$\\hat{y} = \\mathbf{w}^\\top \\mathbf{x} + b$

**MSE损失**：$L = \\frac{1}{m}\\sum_{i=1}^{m}(\\hat{y}^{(i)} - y^{(i)})^2$

## 求解方法
1. **闭式解**：$\\mathbf{w} = (\\mathbf{X}^\\top\\mathbf{X})^{-1}\\mathbf{X}^\\top\\mathbf{y}$
2. **梯度下降**：迭代更新 $\\mathbf{w} \\leftarrow \\mathbf{w} - \\alpha\\frac{\\partial L}{\\partial \\mathbf{w}}$

## 代码实现
\`\`\`python
import numpy as np
from sklearn.linear_model import LinearRegression
X = np.array([[1],[2],[3],[4],[5]])
y = np.array([2.1,4.0,5.9,8.1,10.2])
model = LinearRegression().fit(X, y)
print(f"斜率: {model.coef_[0]:.3f}, 截距: {model.intercept_:.3f}")
\`\`\`

## 参考
- 吴恩达《机器学习》第2周课程
- 周志华《机器学习》第3章`,
  summary: '线性回归的原理、损失函数、求解方法及代码实现。'
}

const docLinear2 = {
  markdown: `# 线性回归进阶：多项式回归与正则化

## 多项式回归
当数据不满足线性关系时，可使用多项式回归：
$\\hat{y} = w_0 + w_1x + w_2x^2 + ... + w_nx^n$

## 正则化
为防止过拟合，加入正则化项：
- **Ridge(L2)**：$L = MSE + \\alpha\\sum w_i^2$
- **Lasso(L1)**：$L = MSE + \\alpha\\sum|w_i|$

## sklearn 实现
\`\`\`python
from sklearn.linear_model import Ridge, Lasso, PolynomialFeatures
from sklearn.pipeline import Pipeline

# 多项式回归
poly = PolynomialFeatures(degree=3)
X_poly = poly.fit_transform(X)

# Ridge回归
model = Pipeline([('poly', poly), ('ridge', Ridge(alpha=1.0))])
model.fit(X_train, y_train)
\`\`\`

## 参考
- sklearn 官方文档：https://scikit-learn.org/stable/modules/linear_model.html
- 《机器学习》周志华 第3章`,
  summary: '多项式回归扩展和正则化技术防止过拟合。'
}

const docLogistic1 = {
  markdown: `# 逻辑回归详解

## 概述
逻辑回归虽名含"回归"，实为分类算法。通过Sigmoid函数将线性输出映射到(0,1)区间，表示概率。

## 核心公式
**Sigmoid函数**：$\\sigma(z) = \\frac{1}{1+e^{-z}}$

**预测概率**：$\\hat{P}(y=1|x) = \sigma(\\mathbf{w}^\\top\\mathbf{x}+b)$

**交叉熵损失**：$L = -[y\\log\\hat{y} + (1-y)\\log(1-\\hat{y})]$

## 二分类决策边界
当$\\hat{P}>0.5$时预测为正类，即$\\mathbf{w}^\\top\\mathbf{x}+b>0$

## sklearn 实现
\`\`\`python
from sklearn.linear_model import LogisticRegression
from sklearn.datasets import load_iris

X, y = load_iris(return_X_y=True)
X_bin, y_bin = X[y<2], y[y<2]
model = LogisticRegression(max_iter=200).fit(X_bin, y_bin)
print(f"准确率: {model.score(X_bin, y_bin):.3f}")
\`\`\`

## 参考
- 吴恩达《机器学习》第3周课程
- sklearn 文档：https://scikit-learn.org/stable/modules/generated/sklearn.linear_model.LogisticRegression.html`,
  summary: '逻辑回归原理、Sigmoid函数、交叉熵损失及代码实现。'
}

const docLogistic2 = {
  markdown: `# 逻辑回归进阶：多分类与过采样

## 多分类扩展
1. **One-vs-Rest (OvR)**：为每个类别训练一个二分类器
2. **Multinomial**：使用softmax函数直接建模多分类

## 类别不平衡处理
\`\`\`python
from sklearn.linear_model import LogisticRegression

# 方法1：class_weight='balanced'
model = LogisticRegression(class_weight='balanced', max_iter=200)

# 方法2：SMOTE过采样
from imblearn.over_sampling import SMOTE
X_res, y_res = SMOTE().fit_resample(X_train, y_train)
model.fit(X_res, y_res)
\`\`\`

## 参考
- sklearn 文档：https://scikit-learn.org/stable/modules/generated/sklearn.linear_model.LogisticRegression.html
- imbalanced-learn 文档：https://imbalanced-learn.org/stable/`,
  summary: '逻辑回归多分类策略和类别不平衡处理方法。'
}

const docDT1 = {
  markdown: `# 决策树详解

## 概述
决策树通过递归地选择最优特征进行划分，直到满足停止条件。每个内部节点对应一个特征判断，叶节点对应输出。

## 划分准则
1. **信息增益**（ID3）：$Gain(D, A) = H(D) - \\sum\\frac{|D_v|}{|D|}H(D_v)$
2. **信息增益率**（C4.5）：$Gain\_ratio = Gain/|A|$
3. **基尼系数**（CART）：$Gini(D) = 1 - \\sum p_k^2$

## 剪枝策略
- **预剪枝**：限制树深度、叶节点样本数等
- **后剪枝**（CCP）：通过最小化代价复杂度进行剪枝

## sklearn 实现
\`\`\`python
from sklearn.tree import DecisionTreeClassifier, plot_tree
import matplotlib.pyplot as plt

model = DecisionTreeClassifier(max_depth=5, min_samples_split=10)
model.fit(X_train, y_train)
plot_tree(model, feature_names=features, class_names=classes)
plt.show()
\`\`\`

## 参考
- 吴恩达《机器学习》第9周课程
- sklearn 文档：https://scikit-learn.org/stable/modules/tree.html`,
  summary: '决策树的划分准则（信息增益、基尼系数）和剪枝策略。'
}

const docDT2 = {
  markdown: `# 决策树进阶：CART算法与回归树

## CART算法
Classification and Regression Tree (CART) 同时支持分类和回归：
- 分类树：使用基尼系数
- 回归树：使用均方误差MSE

## 回归树预测
回归树叶节点输出为该节点所有样本的目标均值：
$\\hat{y} = \\frac{1}{N_v}\\sum_{x_i \\in R_v} y_i$

## 代码实现
\`\`\`python
from sklearn.tree import DecisionTreeRegressor

# 回归树
model = DecisionTreeRegressor(max_depth=5, min_samples_leaf=5)
model.fit(X_train, y_train)
print(f"测试集R²: {model.score(X_test, y_test):.3f}")
\`\`\`

## 参考
- 《统计学习方法》李航 第5章
- sklearn 文档：https://scikit-learn.org/stable/modules/tree.html#regression`,
  summary: 'CART算法详解及回归树的实现。'
}

const docNB1 = {
  markdown: `# 朴素贝叶斯分类器

## 概述
基于贝叶斯定理，假设特征之间条件独立：$P(y|x) = \\frac{P(x|y)P(y)}{P(x)}$

## 常见模型
1. **高斯朴素贝叶斯**：适用于连续特征，假设每类特征服从正态分布
2. **多项式朴素贝叶斯**：适用于文本分类，基于词频统计
3. **伯努利朴素贝叶斯**：适用于二值特征

## sklearn 实现
\`\`\`python
from sklearn.naive_bayes import GaussianNB, MultinomialNB

# 高斯朴素贝叶斯
gnb = GaussianNB()
gnb.fit(X_train, y_train)

# 多项式朴素贝叶斯（文本分类）
mnb = MultinomialNB()
mnb.fit(X_train_vec, y_train)
\`\`\`

## 参考
- sklearn 文档：https://scikit-learn.org/stable/modules/naive_bayes.html
- 吴恩达《机器学习》第4周课程`,
  summary: '朴素贝叶斯分类器的原理和三种常见模型。'
}

const docSVM1 = {
  markdown: `# 支持向量机（SVM）详解

## 概述
SVM通过找到最大间隔的分类超平面来实现分类。对于线性不可分数据，使用核函数将特征映射到高维空间。

## 优化目标
$\\min_{w,b} \\frac{1}{2}||w||^2$  subject to $y_i(w^Tx_i+b) \\geq 1$

## 核函数
- **线性核**：$K(x, z) = x^Tz$
- **多项式核**：$K(x, z) = (x^Tz+1)^d$
- **RBF核（高斯核）**：$K(x, z) = exp(-\\gamma||x-z||^2)$

## sklearn 实现
\`\`\`python
from sklearn.svm import SVC

model = SVC(kernel='rbf', C=1.0, gamma='scale')
model.fit(X_train, y_train)
print(f"准确率: {model.score(X_test, y_test):.3f}")
\`\`\`

## 参考
- 吴恩达《机器学习》第7周课程
- sklearn 文档：https://scikit-learn.org/stable/modules/svm.html`,
  summary: 'SVM原理、最大间隔分类器和核函数。'
}

const docSVM2 = {
  markdown: `# SVM进阶：回归与调参

## SVM回归（SVR）
SVR通过允许一定的误差容忍来实现回归：
$\\min_{w,b} \\frac{1}{2}||w||^2 + C\\sum \\xi_i$

## 超参数调优
\`\`\`python
from sklearn.svm import SVR
from sklearn.model_selection import GridSearchCV

param_grid = {'C': [0.1, 1, 10], 'gamma': ['scale', 'auto'], 'kernel': ['rbf', 'poly']}
svr = SVR()
grid_search = GridSearchCV(svr, param_grid, cv=5, scoring='r2')
grid_search.fit(X_train, y_train)
print(f"最佳参数: {grid_search.best_params_}")
\`\`\`

## 参考
- sklearn 文档：https://scikit-learn.org/stable/modules/svm.html#svm-regression`,
  summary: 'SVM回归（SVR）原理和超参数调优方法。'
}

const docKMeans1 = {
  markdown: `# K-Means 聚类算法

## 概述
K-Means是无监督聚类算法，将数据划分为K个簇，使得簇内方差最小。

## 算法步骤
1. 随机选择K个初始质心
2. 分配每个样本到最近的质心
3. 更新质心为簇内样本均值
4. 重复2-3直到收敛

## sklearn 实现
\`\`\`python
from sklearn.cluster import KMeans
import numpy as np

# 寻找最优K值（肘部法）
inertias = []
for k in range(1, 11):
    km = KMeans(n_clusters=k, random_state=42)
    km.fit(X)
    inertias.append(km.inertia_)

# 最终聚类
kmeans = KMeans(n_clusters=3, random_state=42, n_init=10)
labels = kmeans.fit_predict(X)
print(f"轮廓系数: {silhouette_score(X, labels):.3f}")
\`\`\`

## 参考
- sklearn 文档：https://scikit-learn.org/stable/modules/clustering.html#k-means`,
  summary: 'K-Means算法步骤及K值选择方法。'
}

const docKMeans2 = {
  markdown: `# K-Means 进阶：Mini-Batch与评估指标

## Mini-Batch K-Means
对大数据集使用小批量随机样本加速：
\`\`\`python
from sklearn.cluster import MiniBatchKMeans

mb_kmeans = MiniBatchKMeans(n_clusters=3, batch_size=1000, n_init=10)
labels = mb_kmeans.fit_predict(X)
\`\`\`

## 聚类评估指标
\`\`\`python
from sklearn.metrics import silhouette_score, davies_bouldin_score

# 轮廓系数（-1到1，越高越好）
sil = silhouette_score(X, labels)

# Davies-Bouldin指数（越小越好）
dbi = davies_bouldin_score(X, labels)
\`\`\`

## 参考
- sklearn 文档：https://scikit-learn.org/stable/modules/clustering.html#mini-batch-kmeans`,
  summary: 'Mini-Batch K-Means加速方法和聚类评估指标。'
}

const docPCA1 = {
  markdown: `# PCA 主成分分析详解

## 概述
PCA通过线性变换将高维数据投影到低维空间，同时保留最大方差信息。

## 数学推导
1. 计算协方差矩阵：$\\Sigma = \\frac{1}{n}\\mathbf{X}^\\top\\mathbf{X}$
2. 特征分解：$\\Sigma v = \\lambda v$
3. 选择前k个最大特征值对应的特征向量
4. 投影：$\\mathbf{Z} = \\mathbf{X}\\mathbf{V}_k$

## sklearn 实现
\`\`\`python
from sklearn.decomposition import PCA
from sklearn.preprocessing import StandardScaler

# 标准化后PCA
X_scaled = StandardScaler().fit_transform(X)
pca = PCA(n_components=0.95)  # 保留95%方差
X_pca = pca.fit_transform(X_scaled)
print(f"主成分数: {pca.n_components_}')
print(f"各成分方差占比: {pca.explained_variance_ratio_}")
\`\`\`

## 参考
- sklearn 文档：https://scikit-learn.org/stable/modules/decomposition.html#pca`,
  summary: 'PCA原理、特征值分解和方差解释。'
}

const docPCA2 = {
  markdown: `# PCA 进阶：核PCA与增量PCA

## 核PCA
使用核函数实现非线性降维：
\`\`\`python
from sklearn.decomposition import KernelPCA

kpca = KernelPCA(n_components=2, kernel='rbf', gamma=0.1)
X_kpca = kpca.fit_transform(X)
\`\`\`

## 增量PCA（IPCA）
处理大规模数据：
\`\`\`python
from sklearn.decomposition import IncrementalPCA

ipca = IncrementalPCA(n_components=50, batch_size=100)
for batch in X_batches:
    ipca.partial_fit(batch)
X_ipca = ipca.transform(X)
\`\`\`

## 参考
- sklearn 文档：https://scikit-learn.org/stable/modules/decomposition.html#kernel-pca`,
  summary: '核PCA实现非线性降维和增量PCA处理大数据。'
}

const docNN1 = {
  markdown: `# 神经网络基础详解

## 概述
神经网络由输入层、隐藏层和输出层组成，每层包含多个神经元。神经元通过权重连接，信号正向传播，误差反向传播。

## 前向传播
$\\mathbf{a}^{(l)} = \sigma(\\mathbf{z}^{(l)})$，其中 $\\mathbf{z}^{(l)} = \\mathbf{W}^{(l)}\\mathbf{a}^{(l-1)} + \\mathbf{b}^{(l)}$

## 反向传播
梯度通过链式法则计算：
$\\frac{\\partial L}{\\partial \\mathbf{W}^{(l)}} = \\frac{\\partial L}{\partial \\mathbf{z}^{(l)}}\\mathbf{a}^{(l-1)\\top}$

## sklearn 实现
\`\`\`python
from sklearn.neural_network import MLPClassifier

model = MLPClassifier(
    hidden_layer_sizes=(100, 50),  # 两层隐藏层
    activation='relu',
    solver='adam',
    max_iter=500
)
model.fit(X_train, y_train)
\`\`\`

## 参考
- 吴恩达《深度学习》课程第1-3课
- sklearn 文档：https://scikit-learn.org/stable/modules/neural_networks_supervised.html`,
  summary: '神经网络结构、前向传播和反向传播原理。'
}

const docNN2 = {
  markdown: `# 神经网络进阶：正则化与初始化

## 权重正则化
- **L2正则化**：在损失函数中加入 $\\lambda\\sum||\\mathbf{W}||^2$
- **Dropout**：训练时随机丢弃部分神经元

## 权重初始化
\`\`\`python
from sklearn.neural_network import MLPClassifier

model = MLPClassifier(
    hidden_layer_sizes=(100, 50),
    alpha=0.001,           # L2正则化参数
    learning_rate_init=0.001,
    max_iter=500
)
\`\`\`

## 激活函数与梯度消失
- ReLU: $\\max(0, x)$
- sigmoid: $\\frac{1}{1+e^{-x}}$
- tanh: $\\frac{e^x-e^{-x}}{e^x+e^{-x}}$

## 参考
- 吴恩达《深度学习》课程第4-5课`,
  summary: '神经网络正则化方法（Dropout、L2）和权重初始化策略。'
}

const docCNN1 = {
  markdown: `# 卷积神经网络（CNN）详解

## 概述
CNN是一种专门用于处理图像数据的深度学习模型，核心组件包括卷积层、池化层和全连接层。

## 核心组件
1. **卷积层**：使用卷积核提取局部特征
2. **池化层**：下采样减少参数和计算量
3. **全连接层**：整合特征进行分类

## PyTorch 实现
\`\`\`python
import torch.nn as nn

class SimpleCNN(nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = nn.Conv2d(3, 32, kernel_size=3, padding=1)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(32, 64, kernel_size=3, padding=1)
        self.fc = nn.Linear(64 * 8 * 8, 10)

    def forward(self, x):
        x = self.pool(torch.relu(self.conv1(x)))
        x = self.pool(torch.relu(self.conv2(x)))
        x = x.view(-1, 64 * 8 * 8)
        return self.fc(x)
\`\`\`

## 参考
- 吴恩达《深度学习》卷积神经网络课程
- PyTorch 官方文档：https://pytorch.org/tutorials/`,
  summary: 'CNN基本结构、卷积层、池化层原理和PyTorch实现。'
}

const docCNN2 = {
  markdown: `# CNN 进阶：经典架构与数据增强

## 经典网络
1. **LeNet-5**：早期CNN，用于手写数字识别
2. **AlexNet**：ImageNet竞赛冠军，引入ReLU和Dropout
3. **VGG-16**：使用3x3小卷积核，结构简洁

## 数据增强
\`\`\`python
from torchvision import transforms

train_transform = transforms.Compose([
    transforms.RandomHorizontalFlip(),
    transforms.RandomRotation(10),
    transforms.ColorJitter(brightness=0.2),
    transforms.ToTensor(),
])
\`\`\`

## 参考
- 论文：ImageNet Classification with Deep CNN (Krizhevsky et al., 2012)
- PyTorch Model Zoo：https://pytorch.org/tutorials/beginner/transfer_learning_tutorial.html`,
  summary: '经典CNN架构（LeNet、AlexNet、VGG）和数据增强技术。'
}

const docRNN1 = {
  markdown: `# 循环神经网络（RNN）详解

## 概述
RNN适合处理序列数据，通过隐藏状态记住之前的信息，实现对序列的建模。

## 前向传播
$\\mathbf{h}^{(t)} = \sigma(\\mathbf{W}_{hh}\\mathbf{h}^{(t-1)} + \\mathbf{W}_{xh}\\mathbf{x}^{(t)} + \\mathbf{b})$

## 梯度问题
长序列训练时存在梯度消失/爆炸问题，导致长期依赖难以学习。

## PyTorch 实现
\`\`\`python
import torch.nn as nn

class SimpleRNN(nn.Module):
    def __init__(self, input_size, hidden_size, num_layers, output_size):
        super().__init__()
        self.rnn = nn.RNN(input_size, hidden_size, num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_size, output_size)

    def forward(self, x):
        out, _ = self.rnn(x)
        out = self.fc(out[:, -1, :])
        return out
\`\`\`

## 参考
- 吴恩达《深度学习》序列模型课程
- PyTorch RNN文档：https://pytorch.org/docs/stable/generated/torch.nn.RNN.html`,
  summary: 'RNN原理、梯度问题（消失/爆炸）和PyTorch实现。'
}

const docRNN2 = {
  markdown: `# RNN 进阶：LSTM与GRU

## LSTM（长短期记忆网络）
通过门控机制解决长期依赖问题：
- **遗忘门**：决定丢弃哪些信息
- **输入门**：决定更新哪些信息
- **输出门**：决定输出哪些信息

## PyTorch 实现
\`\`\`python
class LSTMClassifier(nn.Module):
    def __init__(self, vocab_size, embed_size, hidden_size, num_layers):
        super().__init__()
        self.embedding = nn.Embedding(vocab_size, embed_size)
        self.lstm = nn.LSTM(embed_size, hidden_size, num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_size, 2)

    def forward(self, x):
        embedded = self.embedding(x)
        _, (hidden, _) = self.lstm(embedded)
        return self.fc(hidden[-1])
\`\`\`

## 参考
- 论文：Long Short-Term Memory (Hochreiter & Schmidhuber, 1997)
- PyTorch LSTM文档：https://pytorch.org/docs/stable/generated/torch.nn.LSTM.html`,
  summary: 'LSTM的门控机制（遗忘门、输入门、输出门）和实现。'
}

const docGD1 = {
  markdown: `# 梯度下降详解

## 概述
梯度下降是优化算法的基础，通过沿梯度负方向迭代更新参数，找到损失函数的最小值。

## 更新公式
$\\theta \\leftarrow \\theta - \\alpha \\nabla_{\\theta}J(\theta)$

其中 $\\alpha$ 为学习率。

## 变体
1. **批量梯度下降（BGD）**：使用全部样本
2. **随机梯度下降（SGD）**：使用单个样本
3. **小批量梯度下降（Mini-batch GD）**：使用batch_size个样本

## sklearn 实现
\`\`\`python
from sklearn.linear_model import SGDRegressor

model = SGDRegressor(
    loss='squared_error',
    penalty='l2',
    alpha=0.0001,
    learning_rate='optimal',
    max_iter=1000
)
model.fit(X_train, y_train)
\`\`\`

## 参考
- 吴恩达《机器学习》第4周课程
- sklearn 文档：https://scikit-learn.org/stable/modules/sgd.html`,
  summary: '梯度下降原理、三种变体（批量、随机、小批量）和学习率选择。'
}

const docOverfit1 = {
  markdown: `# 过拟合与正则化

## 概述
过拟合指模型在训练集上表现良好，但在测试集上泛化能力差。正则化通过增加约束防止过拟合。

## 常用正则化方法
1. **L1正则化（Lasso）**：$\\lambda\\sum|w_i|$，产生稀疏权重
2. **L2正则化（Ridge）**：$\\lambda\\sum w_i^2$，权重衰减
3. **Elastic Net**：L1+L2组合

## sklearn 实现
\`\`\`python
from sklearn.linear_model import Lasso, Ridge, ElasticNet
from sklearn.preprocessing import StandardScaler

# 标准化后应用正则化
X_scaled = StandardScaler().fit_transform(X_train)

lasso = Lasso(alpha=0.1).fit(X_scaled, y_train)
ridge = Ridge(alpha=1.0).fit(X_scaled, y_train)
elastic = ElasticNet(alpha=0.1, l1_ratio=0.5).fit(X_scaled, y_train)
\`\`\`

## 参考
- 吴恩达《机器学习》第7周课程
- sklearn 文档：https://scikit-learn.org/stable/modules/linear_model.html`,
  summary: '过拟合原因、L1/L2/Elastic Net正则化方法和sklearn实现。'
}

const docOverfit2 = {
  markdown: `# 防止过拟合的其他策略

## 策略
1. **增加数据量**：最直接有效的方法
2. **Early Stopping**：监控验证集损失，连续不降时停止训练
3. **Dropout**：训练时随机丢弃神经元
4. **Batch Normalization**：归一化层间激活值

## PyTorch 实现
\`\`\`python
import torch.nn as nn

class DropoutNet(nn.Module):
    def __init__(self):
        super().__init__()
        self.fc1 = nn.Linear(784, 256)
        self.dropout = nn.Dropout(0.5)
        self.fc2 = nn.Linear(256, 10)

    def forward(self, x):
        x = torch.relu(self.fc1(x))
        x = self.dropout(x)  # 训练时随机丢弃
        x = self.fc2(x)
        return x
\`\`\`

## 参考
- 吴恩达《深度学习》第2课
- Dropout论文：Dropout: A Simple Way to Prevent NN from Overfitting (Srivastava et al., 2014)`,
  summary: 'Early Stopping、Dropout、Batch Normalization等防过拟合策略。'
}

const docMetrics1 = {
  markdown: `# 分类评估指标详解

## 混淆矩阵
|  | 预测正 | 预测负 |
|--|--------|--------|
| 实际正 | TP | FN |
| 实际负 | FP | TN |

## 核心指标
- **准确率**：$(TP+TN)/(TP+TN+FP+FN)$
- **精确率**：$TP/(TP+FP)$
- **召回率**：$TP/(TP+FN)$
- **F1分数**：$2/(1/P + 1/R) = 2PR/(P+R)$

## sklearn 实现
\`\`\`python
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix

y_pred = model.predict(X_test)
print(f"准确率: {accuracy_score(y_test, y_pred):.3f}")
print(f"精确率: {precision_score(y_test, y_pred):.3f}")
print(f"召回率: {recall_score(y_test, y_pred):.3f}")
print(f"F1分数: {f1_score(y_test, y_pred):.3f}")
print(f"混淆矩阵:\\n{confusion_matrix(y_test, y_pred)}")
\`\`\`

## 参考
- sklearn 文档：https://scikit-learn.org/stable/modules/model_evaluation.html`,
  summary: '混淆矩阵、准确率、精确率、召回率、F1分数及sklearn实现。'
}

const docMetrics2 = {
  markdown: `# 回归与聚类评估指标

## 回归评估
\`\`\`python
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score

y_pred = model.predict(X_test)
print(f"MSE: {mean_squared_error(y_test, y_pred):.3f}")
print(f"RMSE: {np.sqrt(mean_squared_error(y_test, y_pred)):.3f}")
print(f"MAE: {mean_absolute_error(y_test, y_pred):.3f}")
print(f"R²: {r2_score(y_test, y_pred):.3f}")
\`\`\`

## 聚类评估
\`\`\`python
from sklearn.metrics import silhouette_score, davies_bouldin_score, adjusted_rand_score

labels = kmeans.predict(X_test)
print(f"轮廓系数: {silhouette_score(X_test, labels):.3f}")
print(f"DB指数: {davies_bouldin_score(X_test, labels):.3f}")
\`\`\`

## 参考
- sklearn 文档：https://scikit-learn.org/stable/modules/model_evaluation.html`,
  summary: '回归指标（MSE、RMSE、MAE、R²）和聚类指标（轮廓系数、DB指数）。'
}

const docFeature1 = {
  markdown: `# 特征工程详解

## 概述
特征工程是将原始数据转换为模型能更好利用的特征的过程，是机器学习成功的关键。

## 常见技术
1. **数值特征**：标准化、归一化、分箱
2. **类别特征**：独热编码、标签编码、目标编码
3. **时间特征**：年、月、日、星期、季节等
4. **文本特征**：TF-IDF、词嵌入

## sklearn 实现
\`\`\`python
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline

preprocessor = ColumnTransformer([
    ('num', StandardScaler(), ['age', 'income']),
    ('cat', OneHotEncoder(), ['job', 'education'])
])

pipeline = Pipeline([
    ('preprocessor', preprocessor),
    ('classifier', RandomForestClassifier())
])
\`\`\`

## 参考
- sklearn 文档：https://scikit-learn.org/stable/modules/preprocessing.html`,
  summary: '特征工程的重要性和常见技术（编码、标准化、分箱等）。'
}

const docFeature2 = {
  markdown: `# 特征选择与降维

## 特征选择方法
1. **过滤法**：方差阈值、相关性分析
2. **包装法**：递归特征消除（RFE）
3. **嵌入法**：L1正则化、树模型特征重要性

## sklearn 实现
\`\`\`python
from sklearn.feature_selection import SelectKBest, RFE, mutual_info_classif
from sklearn.ensemble import RandomForestClassifier

# 方差阈值
from sklearn.feature_selection import VarianceThreshold
selector = VarianceThreshold(threshold=0.5)
X_selected = selector.fit_transform(X)

# 基于模型的特征选择
rfe = RFE(estimator=RandomForestClassifier(), n_features_to_select=10)
rfe.fit(X, y)
X_selected = rfe.transform(X)
\`\`\`

## 参考
- sklearn 文档：https://scikit-learn.org/stable/modules/feature_selection.html`,
  summary: '特征选择三种方法：过滤法、包装法、嵌入法及sklearn实现。'
}

const docEnsemble1 = {
  markdown: `# 集成学习详解

## 概述
集成学习通过组合多个基学习器来提升预测性能，主要方法包括Bagging和Boosting。

## Bagging vs Boosting
- **Bagging**：并行训练，减小方差，如随机森林
- **Boosting**：串行训练，减小偏差，如AdaBoost、GBDT、XGBoost

## sklearn 实现
\`\`\`python
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier, AdaBoostClassifier

rf = RandomForestClassifier(n_estimators=100, random_state=42)
gb = GradientBoostingClassifier(n_estimators=100, learning_rate=0.1)
ada = AdaBoostClassifier(n_estimators=100, learning_rate=0.1)

rf.fit(X_train, y_train)
gb.fit(X_train, y_train)
\`\`\`

## 参考
- 吴恩达《机器学习》第8周课程
- sklearn 文档：https://scikit-learn.org/stable/modules/ensemble.html`,
  summary: '集成学习原理、Bagging与Boosting区别及sklearn实现。'
}

const docEnsemble2 = {
  markdown: `# XGBoost 与 LightGBM

## XGBoost
\`\`\`python
import xgboost as xgb

model = xgb.XGBClassifier(
    n_estimators=100,
    max_depth=6,
    learning_rate=0.1,
    subsample=0.8,
    colsample_bytree=0.8
)
model.fit(X_train, y_train)
\`\`\`

## LightGBM（更快）
\`\`\`python
import lightgbm as lgb

model = lgb.LGBMClassifier(
    n_estimators=100,
    max_depth=6,
    learning_rate=0.1,
    num_leaves=31
)
model.fit(X_train, y_train)
\`\`\`

## 参考
- XGBoost论文：XGBoost: A Scalable Tree Boosting System (Chen & Guestrin, 2016)
- LightGBM文档：https://lightgbm.readthedocs.io/`,
  summary: 'XGBoost和LightGBM的原理及sklearn风格API实现。'
}

const docActivation1 = {
  markdown: `# 激活函数详解

## 概述
激活函数为神经网络引入非线性，使网络能够学习复杂模式。

## 常见激活函数
1. **Sigmoid**：$\\sigma(x) = 1/(1+e^{-x})$，输出(0,1)
2. **Tanh**：$\\tanh(x) = (e^x-e^{-x})/(e^x+e^{-x})$，输出(-1,1)
3. **ReLU**：$f(x) = \\max(0, x)$，计算快
4. **Leaky ReLU**：$f(x) = \\max(0.01x, x)$
5. **Softmax**：$softmax(x_i) = e^{x_i}/\\sum e^{x_j}$，用于多分类

## PyTorch 实现
\`\`\`python
import torch.nn as nn

layers = [
    nn.ReLU(),           # 隐藏层常用
    nn.LeakyReLU(0.2),   # 防止死亡神经元
    nn.Sigmoid(),        # 二分类输出
    nn.Softmax(dim=1)    # 多分类输出
]
\`\`\`

## 参考
- 吴恩达《深度学习》第1课
- PyTorch文档：https://pytorch.org/docs/stable/nn.html#non-linear-activations`,
  summary: '常用激活函数（Sigmoid、Tanh、ReLU、Softmax）的公式和PyTorch实现。'
}

const docLoss1 = {
  markdown: `# 损失函数详解

## 概述
损失函数衡量模型预测与真实值的差距，是模型优化的目标。

## 分类损失
1. **交叉熵损失**：$L = -\\sum y_i \\log \\hat{y}_i$，分类最常用
2. **二元交叉熵**：$L = -[y\\log\\hat{y} + (1-y)\\log(1-\\hat{y})]$
3. **Hinge Loss**：$L = \\max(0, 1 - y\\hat{y})$，SVM使用

## 回归损失
1. **MSE**：$L = \\frac{1}{n}\\sum(\\hat{y}_i - y_i)^2$
2. **MAE**：$L = \\frac{1}{n}\\sum|\\hat{y}_i - y_i|$，对异常值鲁棒

## PyTorch 实现
\`\`\`python
import torch.nn as nn

criterion = nn.CrossEntropyLoss()      # 多分类
criterion = nn.BCELoss()              # 二分类
criterion = nn.MSELoss()               # 回归
criterion = nn.L1Loss()                # MAE
\`\`\`

## 参考
- PyTorch文档：https://pytorch.org/docs/stable/nn.html#loss-functions`,
  summary: '分类和回归常用损失函数（交叉熵、MSE、MAE、Hinge Loss）的公式和实现。'
}

const docOptimizer1 = {
  markdown: `# 优化器详解

## 概述
优化器根据损失函数的梯度更新模型参数。

## 常见优化器
1. **SGD**：$\\theta \\leftarrow \\theta - \\alpha \\nabla_{\\theta}J(\theta)$
2. **Momentum**：加入动量项加速收敛
3. **Adagrad**：自适应学习率
4. **Adam**：结合Momentum和RMSProp

## PyTorch 实现
\`\`\`python
import torch.optim as optim

# SGD with Momentum
optimizer = optim.SGD(model.parameters(), lr=0.01, momentum=0.9)

# Adam（最常用）
optimizer = optim.Adam(model.parameters(), lr=0.001, betas=(0.9, 0.999))

# 学习率调度
scheduler = optim.lr_scheduler.StepLR(optimizer, step_size=10, gamma=0.1)
\`\`\`

## 参考
- 吴恩达《深度学习》第4课
- PyTorch文档：https://pytorch.org/docs/stable/optim.html`,
  summary: 'SGD、Momentum、Adagrad、Adam等优化器原理和PyTorch实现。'
}

const docNLP1 = {
  markdown: `# 文本处理基础

## 概述
文本处理将原始文本转换为机器学习模型可处理的数值特征。

## 常用技术
1. **分词**：中文（jieba）、英文（spacy）
2. **词干提取**：Porter、Lemmatization
3. **词向量化**：TF-IDF、Word2Vec

## sklearn 实现
\`\`\`python
from sklearn.feature_extraction.text import TfidfVectorizer
import jieba

# 中文分词
texts = [' '.join(jieba.cut(text)) for text in chinese_texts]

# TF-IDF向量化
vectorizer = TfidfVectorizer(max_features=5000)
X = vectorizer.fit_transform(texts)
\`\`\`

## 参考
- sklearn文档：https://scikit-learn.org/stable/modules/feature_extraction.html
- jieba文档：https://github.com/fxsjy/jieba`,
  summary: '文本处理流程（分词、词干提取）和TF-IDF向量化。'
}

const docNLP2 = {
  markdown: `# 词嵌入与预训练模型

## 词嵌入
将词语映射到低维稠密向量，捕捉语义关系。

## 预训练模型
\`\`\`python
from transformers import BertTokenizer, BertModel
import torch

tokenizer = BertTokenizer.from_pretrained('bert-base-chinese')
model = BertModel.from_pretrained('bert-base-chinese')

inputs = tokenizer("你好世界", return_tensors='pt')
outputs = model(**inputs)
embedding = outputs.last_hidden_state
\`\`\`

## 参考
- BERT论文：BERT: Pre-training of Deep Bidirectional Transformers (Devlin et al., 2018)
- Hugging Face文档：https://huggingface.co/docs/transformers`,
  summary: '词嵌入原理和BERT等预训练模型的使用方法。'
}

const docRecommend1 = {
  markdown: `# 推荐系统基础

## 概述
推荐系统根据用户行为和物品特征预测用户兴趣，实现个性化推荐。

## 协同过滤
1. **基于用户的协同过滤**：找到相似用户，推荐他们喜欢的物品
2. **基于物品的协同过滤**：找到相似物品，推荐给用户

## sklearn 实现
\`\`\`python
from sklearn.neighbors import NearestNeighbors

# 基于物品的协同过滤
item_features = item_matrix.T  # (特征数, 物品数)
model = NearestNeighbors(metric='cosine', algorithm='brute')
model.fit(item_features)

distances, indices = model.kneighbors(user_vector.reshape(1, -1), n_neighbors=10)
recommended_items = [items[i] for i in indices[0]]
\`\`\`

## 参考
- 推荐系统实战（项亮）
- sklearn文档：https://scikit-learn.org/stable/modules/neighbors.html#nearest-neighbors`,
  summary: '推荐系统概述和协同过滤算法原理。'
}

const docRecommend2 = {
  markdown: `# 矩阵分解与深度学习推荐

## 矩阵分解（SVD）
将用户-物品评分矩阵分解为用户隐向量和物品隐向量：
$\\mathbf{R} \\approx \\mathbf{U}\\mathbf{V}^\\top$

## ALS（交替最小二乘）
\`\`\`python
from scipy.sparse import csr_matrix
from sklearn.decomposition import TruncatedSVD

R = csr_matrix(ratings_matrix)
svd = TruncatedSVD(n_components=50)
U = svd.fit_transform(R)  # 用户隐向量
V = svd.components_       # 物品隐向量
\`\`\`

## 深度学习推荐（Wide & Deep）
结合记忆能力和泛化能力。

## 参考
- Netflix Prize论文
- TensorFlow Recommenders：https://www.tensorflow.org/recommenders`,
  summary: '矩阵分解（SVD）和深度学习推荐模型。'
}

// ═══════════════════════════════════════════════════════════════
// MINDMAP 数据 (20条，每知识点1张)
// ═══════════════════════════════════════════════════════════════

const mindmapLinear = {
  tree: {
    content: '线性回归',
    children: [
      { content: '原理', children: [{ content: '最小二乘法' }, { content: '梯度下降' }] },
      { content: '正则化', children: [{ content: '岭回归(L2)' }, { content: 'Lasso(L1)' }] },
      { content: '评估指标', children: [{ content: '均方误差' }, { content: '决定系数R²' }] }
    ]
  }
}

const mindmapLogistic = {
  tree: {
    content: '逻辑回归',
    children: [
      { content: 'Sigmoid函数' },
      { content: '损失函数', children: [{ content: '交叉熵损失' }] },
      { content: '多分类扩展', children: [{ content: '一对多法' }, { content: 'Softmax回归' }] }
    ]
  }
}

const mindmapDT = {
  tree: {
    content: '决策树',
    children: [
      { content: '划分准则', children: [{ content: '信息增益' }, { content: '基尼系数' }] },
      { content: '剪枝策略', children: [{ content: '预剪枝' }, { content: '后剪枝' }] },
      { content: '经典算法', children: [{ content: 'ID3' }, { content: 'C4.5' }, { content: 'CART' }] }
    ]
  }
}

const mindmapNB = {
  tree: {
    content: '朴素贝叶斯',
    children: [
      { content: '贝叶斯定理' },
      { content: '模型类型', children: [{ content: '高斯型' }, { content: '多项式型' }, { content: '伯努利型' }] },
      { content: '典型应用', children: [{ content: '垃圾邮件分类' }, { content: '文本分类' }] }
    ]
  }
}

const mindmapSVM = {
  tree: {
    content: '支持向量机',
    children: [
      { content: '间隔最大化原理' },
      { content: '核函数', children: [{ content: '线性核' }, { content: 'RBF核' }, { content: '多项式核' }] },
      { content: '回归扩展SVR' }
    ]
  }
}

const mindmapKMeans = {
  tree: {
    content: 'K-Means聚类',
    children: [
      { content: '算法流程' },
      { content: 'K值确定', children: [{ content: '肘部法则' }, { content: '轮廓系数法' }] },
      { content: '算法变体', children: [{ content: '小批量K-Means' }, { content: 'K-Means++初始化' }] }
    ]
  }
}

const mindmapPCA = {
  tree: {
    content: 'PCA主成分分析',
    children: [
      { content: '方差最大化准则' },
      { content: '奇异值分解SVD' },
      { content: '应用场景', children: [{ content: '数据可视化' }, { content: '特征抽取' }] }
    ]
  }
}

const mindmapNN = {
  tree: {
    content: '神经网络',
    children: [
      { content: '网络结构', children: [{ content: '输入层' }, { content: '隐藏层' }, { content: '输出层' }] },
      { content: '前向传播算法' },
      { content: '反向传播算法' },
      { content: '正则化技术', children: [{ content: 'Dropout' }, { content: 'L2正则化' }] }
    ]
  }
}

const mindmapCNN = {
  tree: {
    content: '卷积神经网络',
    children: [
      { content: '卷积层' },
      { content: '池化层', children: [{ content: '最大池化' }, { content: '平均池化' }] },
      { content: '经典网络架构', children: [{ content: 'LeNet' }, { content: 'AlexNet' }, { content: 'VGG' }] }
    ]
  }
}

const mindmapRNN = {
  tree: {
    content: '循环神经网络',
    children: [
      { content: '序列建模能力' },
      { content: '梯度问题', children: [{ content: '梯度消失' }, { content: '梯度爆炸' }] },
      { content: '改进模型LSTM/GRU' }
    ]
  }
}

const mindmapGD = {
  tree: {
    content: '梯度下降优化',
    children: [
      { content: '批量梯度下降' },
      { content: '随机梯度下降' },
      { content: '小批量梯度下降' },
      { content: '学习率调整' }
    ]
  }
}

const mindmapOverfit = {
  tree: {
    content: '过拟合与正则化',
    children: [
      { content: '过拟合原因', children: [{ content: '模型过于复杂' }, { content: '训练数据不足' }] },
      { content: 'L1正则化(稀疏解)' },
      { content: 'L2正则化(权重衰减)' },
      { content: '早停法Early Stopping' }
    ]
  }
}

const mindmapMetrics = {
  tree: {
    content: '模型评估指标',
    children: [
      { content: '分类评估', children: [{ content: '准确率' }, { content: '精确率' }, { content: '召回率' }, { content: 'F1分数' }] },
      { content: '回归评估', children: [{ content: '均方误差' }, { content: '均方根误差' }, { content: '平均绝对误差' }, { content: '决定系数R²' }] }
    ]
  }
}

const mindmapFeature = {
  tree: {
    content: '特征工程',
    children: [
      { content: '数值特征处理', children: [{ content: '标准化' }, { content: '归一化' }] },
      { content: '类别特征编码', children: [{ content: '独热编码' }, { content: '标签编码' }] },
      { content: '特征选择方法' }
    ]
  }
}

const mindmapEnsemble = {
  tree: {
    content: '集成学习方法',
    children: [
      { content: 'Bagging并行集成', children: [{ content: '随机森林' }] },
      { content: 'Boosting序列化集成', children: [{ content: 'AdaBoost' }, { content: '梯度提升树' }, { content: 'XGBoost' }] },
      { content: 'Stacking堆叠集成' }
    ]
  }
}

const mindmapActivation = {
  tree: {
    content: '激活函数',
    children: [
      { content: 'Sigmoid函数' },
      { content: 'Tanh双曲正切' },
      { content: 'ReLU线性整流' },
      { content: 'Leaky ReLU带泄露ReLU' },
      { content: 'Softmax归一化指数' }
    ]
  }
}

const mindmapLoss = {
  tree: {
    content: '损失函数',
    children: [
      { content: '分类损失', children: [{ content: '交叉熵损失' }, { content: 'Hinge铰链损失' }] },
      { content: '回归损失', children: [{ content: '均方误差MSE' }, { content: '平均绝对误差MAE' }] }
    ]
  }
}

const mindmapOptimizer = {
  tree: {
    content: '优化器算法',
    children: [
      { content: '随机梯度下降SGD' },
      { content: '动量优化Momentum' },
      { content: '自适应学习率Adagrad' },
      { content: '自适应矩估计Adam' }
    ]
  }
}

const mindmapNLP = {
  tree: {
    content: '自然语言处理',
    children: [
      { content: '中文分词技术' },
      { content: '词向量表示', children: [{ content: 'TF-IDF词频-逆文档频率' }, { content: 'Word2Vec词嵌入' }, { content: 'BERT预训练模型' }] },
      { content: '命名实体识别NER' }
    ]
  }
}

const mindmapRecommend = {
  tree: {
    content: '推荐系统',
    children: [
      { content: '协同过滤算法', children: [{ content: '基于用户的协同过滤' }, { content: '基于物品的协同过滤' }] },
      { content: '矩阵分解方法', children: [{ content: '奇异值分解SVD' }, { content: '交替最小二乘ALS' }] },
      { content: '深度学习推荐', children: [{ content: 'Wide&Deep Wide联合Deep' }] }
    ]
  }
}

// ═══════════════════════════════════════════════════════════════
// QUIZ 数据 (45条，每知识点2-3套)
// ═══════════════════════════════════════════════════════════════

const quizLinear1 = {
  questions: [
    { type: 'single', question: '线性回归的损失函数是什么？', options: ['交叉熵', '均方误差(MSE)', 'Hinge Loss', '0-1损失'], answer: '均方误差(MSE)', explanation: '线性回归使用MSE作为损失函数，通过最小二乘法求解。' },
    { type: 'single', question: '线性回归的闭式解为？', options: ['w = X^(-1)y', 'w = (X^T X)^(-1) X^T y', 'w = X^T y', 'w = (X^T X) y'], answer: 'w = (X^T X)^(-1) X^T y', explanation: '根据最小二乘法，最优权重为正规方程的解。' },
    { type: 'truefalse', question: '线性回归可以用于分类问题。', options: ['正确', '错误'], answer: '错误', explanation: '线性回归输出连续值，适用于回归任务；分类应使用逻辑回归。' },
    { type: 'short', question: '简述L1正则化和L2正则化的区别。', answer: 'L1正则化产生稀疏权重，可用于特征选择；L2正则化使权重衰减但保持稠密。', explanation: 'L1=Lasso(λ∑|w|)，L2=Ridge(λ∑w²)' }
  ]
}

const quizLinear2 = {
  questions: [
    { type: 'single', question: 'Ridge回归使用的正则化方式是？', options: ['L1正则化', 'L2正则化', 'L1+L2正则化', '无正则化'], answer: 'L2正则化', explanation: 'Ridge回归在损失函数中加入权重平方和的惩罚项。' },
    { type: 'multiple', question: '下列哪些方法可以确定线性回归的最优参数？（多选）', options: ['闭式解', '梯度下降', '决策树分裂', 'K-Means'], answer: ['闭式解', '梯度下降'], explanation: '线性回归可通过正规方程闭式求解或梯度下降迭代求解。' },
    { type: 'truefalse', question: '如果特征之间存在多重共线性，线性回归仍能很好地拟合。', options: ['正确', '错误'], answer: '错误', explanation: '多重共线性导致(X^T X)矩阵奇异或病态，影响求解稳定性。' }
  ]
}

const quizLogistic1 = {
  questions: [
    { type: 'single', question: '逻辑回归的输出范围是？', options: ['(-∞, +∞)', '(0, 1)', '(-1, 1)', '[0, 1]'], answer: '(0, 1)', explanation: '通过Sigmoid函数将线性输出映射到(0,1)概率区间。' },
    { type: 'single', question: '逻辑回归使用的损失函数是？', options: ['MSE', '交叉熵', 'Hinge Loss', '指数损失'], answer: '交叉熵', explanation: '逻辑回归使用二元交叉熵损失，凸函数性质利于优化。' },
    { type: 'truefalse', question: '逻辑回归只能处理二分类问题。', options: ['正确', '错误'], answer: '错误', explanation: '可通过OvR或Softmax扩展到多分类。' },
    { type: 'short', question: '为什么逻辑回归比线性回归更适合分类任务？', answer: '逻辑回归输出概率(0,1)，通过Sigmoid约束，具有更好的概率解释性。', explanation: '线性回归输出无约束，不适合直接输出类别概率。' }
  ]
}

const quizDT1 = {
  questions: [
    { type: 'single', question: '决策树分裂时，信息增益偏向于选择？', options: ['取值少的特征', '取值多的特征', '与取值无关', '连续值特征'], answer: '取值多的特征', explanation: '信息增益对取值多的特征有偏好，C4.5使用信息增益率解决这个问题。' },
    { type: 'single', question: 'CART决策树使用什么准则进行分裂？', options: ['信息增益', '信息增益率', '基尼系数', '方差减少'], answer: '基尼系数', explanation: 'CART(Classification and Regression Tree)使用基尼系数作为分类准则。' },
    { type: 'truefalse', question: '决策树不需要特征标准化。', options: ['正确', '错误'], answer: '正确', explanation: '决策树基于特征阈值分裂，不受特征尺度影响。' },
    { type: 'multiple', question: '防止决策树过拟合的方法有？（多选）', options: ['限制树深度', '最小样本分裂数', '增加数据量', 'L1正则化'], answer: ['限制树深度', '最小样本分裂数'], explanation: '预剪枝（限制深度、样本数）和后剪枝是常用方法。' }
  ]
}

const quizNB1 = {
  questions: [
    { type: 'single', question: '朴素贝叶斯"朴素"指的是？', options: ['模型简单', '特征条件独立假设', '无需训练', '线性分类器'], answer: '特征条件独立假设', explanation: '"朴素"假设特征在给定类别条件下相互独立。' },
    { type: 'single', question: '高斯朴素贝叶斯假设特征服从？', options: ['伯努利分布', '多项式分布', '正态分布', '均匀分布'], answer: '正态分布', explanation: '高斯朴素贝叶斯假设连续特征在每个类别下服从正态分布。' },
    { type: 'truefalse', question: '朴素贝叶斯分类器在高维稀疏数据上表现良好。', options: ['正确', '错误'], answer: '正确', explanation: '朴素贝叶斯对维度灾难不敏感，在文本分类等高维数据上常用。' }
  ]
}

const quizSVM1 = {
  questions: [
    { type: 'single', question: 'SVM通过什么实现分类？', options: ['贝叶斯规则', '最大间隔超平面', '决策树分裂', '神经网络'], answer: '最大间隔超平面', explanation: 'SVM找到使两类间隔最大的分类超平面。' },
    { type: 'single', question: 'RBF核将数据映射到？', options: ['原始空间', '有限维空间', '无穷维空间', '二维空间'], answer: '无穷维空间', explanation: 'RBF核对应的特征映射是无穷维的，但计算在原空间完成。' },
    { type: 'truefalse', question: 'SVM对特征缩放敏感。', options: ['正确', '错误'], answer: '正确', explanation: 'SVM基于距离计算，特征尺度不同会影响间隔大小。' },
    { type: 'multiple', question: '下列哪些是SVM的核函数？（多选）', options: ['线性核', '多项式核', 'RBF核', 'Dropout核'], answer: ['线性核', '多项式核', 'RBF核'], explanation: '核函数包括线性、多项式、RBF等，Dropout不是核函数。' }
  ]
}

const quizKMeans1 = {
  questions: [
    { type: 'single', question: 'K-Means算法中，质心的更新规则是？', options: ['随机选择', '簇内样本均值', '簇内样本最大值', '簇内样本最小值'], answer: '簇内样本均值', explanation: '每次迭代重新计算每个簇所有样本的均值作为新质心。' },
    { type: 'single', question: 'K-Means的K值如何选择？', options: ['必须小于10', '必须大于样本数', '可使用肘部法或轮廓系数', '无所谓'], answer: '可使用肘部法或轮廓系数', explanation: '肘部法观察SSE拐点，轮廓系数衡量聚类质量。' },
    { type: 'truefalse', question: 'K-Means保证收敛到全局最优解。', options: ['正确', '错误'], answer: '错误', explanation: 'K-Means只保证收敛到局部最优，结果依赖初始化。' }
  ]
}

const quizPCA1 = {
  questions: [
    { type: 'single', question: 'PCA寻找的是？', options: ['最大方差方向', '最小方差方向', '最近邻方向', '正交方向'], answer: '最大方差方向', explanation: 'PCA通过特征值分解找到解释数据方差最大的正交方向。' },
    { type: 'single', question: 'PCA降维后各主成分之间是什么关系？', options: ['线性相关', '正交（独立）', '非线性相关', '无关系'], answer: '正交（独立）', explanation: 'PCA得到的主成分是相互正交的，协方差为零。' },
    { type: 'truefalse', question: 'PCA可用于去除数据中的噪声。', options: ['正确', '错误'], answer: '正确', explanation: '保留主要方差成分，丢弃方差小的成分可去除噪声。' }
  ]
}

const quizNN1 = {
  questions: [
    { type: 'single', question: '神经网络中隐藏层激活函数常用？', options: ['Sigmoid', 'Tanh', 'ReLU', 'Purelin'], answer: 'ReLU', explanation: 'ReLU计算快、梯度消失问题小，是隐藏层常用激活函数。' },
    { type: 'single', question: '反向传播算法基于？', options: ['贪心搜索', '动态规划', '链式法则', '聚类分析'], answer: '链式法则', explanation: '反向传播使用链式法则计算每个参数对损失函数的梯度。' },
    { type: 'truefalse', question: 'Dropout只在训练时使用。', options: ['正确', '错误'], answer: '正确', explanation: 'Dropout是训练时的正则化技术，预测时不使用。' }
  ]
}

const quizCNN1 = {
  questions: [
    { type: 'single', question: '卷积层的主要作用是？', options: ['降维', '特征提取', '分类', '池化'], answer: '特征提取', explanation: '卷积核扫描图像提取局部特征，如边缘、纹理等。' },
    { type: 'single', question: '池化层的主要作用是？', options: ['增加参数', '减少空间尺寸和计算量', '增加非线性', '归一化'], answer: '减少空间尺寸和计算量', explanation: '池化（如Max Pooling）下采样，减少参数并提供平移不变性。' },
    { type: 'truefalse', question: '1x1卷积核可以改变通道数。', options: ['正确', '错误'], answer: '正确', explanation: '1x1卷积在通道维度上进行信息整合，可改变通道数。' }
  ]
}

const quizRNN1 = {
  questions: [
    { type: 'single', question: 'RNN适合处理什么类型的数据？', options: ['静态数据', '独立样本', '序列数据', '图像数据'], answer: '序列数据', explanation: 'RNN通过隐藏状态记住历史信息，适合序列建模。' },
    { type: 'single', question: 'LSTM通过什么解决长期依赖问题？', options: ['增加层数', '门控机制', '激活函数', '池化层'], answer: '门控机制', explanation: 'LSTM通过遗忘门、输入门、输出门控制信息流动。' },
    { type: 'truefalse', question: '标准RNN存在梯度消失问题。', options: ['正确', '错误'], answer: '正确', explanation: '长序列反向传播时梯度连乘导致梯度指数级减小。' }
  ]
}

const quizGD1 = {
  questions: [
    { type: 'single', question: '批量梯度下降每一步更新需要？', options: ['1个样本', 'batch_size个样本', '全部样本', '无样本'], answer: '全部样本', explanation: '批量梯度下降计算整个数据集的梯度后更新一次。' },
    { type: 'single', question: '学习率过大会导致？', options: ['收敛变慢', '震荡或发散', '陷入局部最优', '无法训练'], answer: '震荡或发散', explanation: '学习率过大可能导致参数在最优解附近震荡或越过最优点。' },
    { type: 'truefalse', question: 'Adam优化器结合了Momentum和RMSProp的优点。', options: ['正确', '错误'], answer: '正确', explanation: 'Adam利用梯度的一阶矩估计和二阶矩估计自适应调整学习率。' }
  ]
}

const quizOverfit1 = {
  questions: [
    { type: 'single', question: '过拟合是指？', options: ['模型太简单', '训练误差大测试误差小', '训练误差小测试误差大', '欠拟合'], answer: '训练误差小测试误差大', explanation: '过拟合是模型在训练集上好但在测试集上泛化能力差。' },
    { type: 'single', question: 'L2正则化使权重？', options: ['置零', '稀疏化', '平滑衰减', '不变'], answer: '平滑衰减', explanation: 'L2正则化惩罚权重的平方和，使权重趋向于小但不为零。' },
    { type: 'multiple', question: '防止过拟合的方法有？（多选）', options: ['增加数据', '减少模型复杂度', 'L1正则化', 'L2正则化'], answer: ['增加数据', '减少模型复杂度', 'L1正则化', 'L2正则化'], explanation: '所有选项都可有效防止过拟合。' }
  ]
}

const quizMetrics1 = {
  questions: [
    { type: 'single', question: '精确率和召回率的调和平均是？', options: ['准确率', 'F1分数', 'AUC', 'RMSE'], answer: 'F1分数', explanation: 'F1 = 2/(1/P + 1/R)，平衡精确率和召回率。' },
    { type: 'single', question: '对于不平衡数据集，哪指标更合适？', options: ['准确率', '精确率', '召回率', 'F1分数'], answer: 'F1分数', explanation: 'F1分数综合考虑精确率和召回率，适合不平衡数据。' },
    { type: 'truefalse', question: 'R²可以大于1。', options: ['正确', '错误'], answer: '错误', explanation: 'R²最大为1，表示模型解释了所有方差；小于0表示比均值预测还差。' }
  ]
}

const quizFeature1 = {
  questions: [
    { type: 'single', question: '独热编码适用于？', options: ['有序类别', '无序类别', '数值特征', '文本特征'], answer: '无序类别', explanation: '独热编码将类别转为二进制向量，避免引入虚假的顺序关系。' },
    { type: 'single', question: '标准化会改变数据的分布形态吗？', options: ['会', '不会', '取决于方法', '不确定'], answer: '不会', explanation: '标准化只改变均值和标准差，不改变分布形态（如果是正态分布仍为正态）。' },
    { type: 'truefalse', question: '特征选择可以减少过拟合风险。', options: ['正确', '错误'], answer: '正确', explanation: '移除无关或噪声特征可降低模型复杂度，减少过拟合。' }
  ]
}

const quizEnsemble1 = {
  questions: [
    { type: 'single', question: '随机森林是哪种集成方法？', options: ['Boosting', 'Bagging', 'Stacking', 'Blending'], answer: 'Bagging', explanation: '随机森林采用Bagging策略，通过自助采样和特征子采样降低方差。' },
    { type: 'single', question: 'XGBoost是哪类算法？', options: ['Bagging', 'Boosting', 'Stacking', 'Bagging+Boosting'], answer: 'Boosting', explanation: 'XGBoost是梯度提升的变体，通过串行训练弱分类器逐步减少偏差。' },
    { type: 'truefalse', question: '集成学习总能提升性能。', options: ['正确', '错误'], answer: '错误', explanation: '如果基分类器表现都很差或高度相关，集成可能无效甚至更差。' }
  ]
}

const quizActivation1 = {
  questions: [
    { type: 'single', question: 'ReLU函数的公式是？', options: ['f(x)=max(0,x)', 'f(x)=1/(1+e^-x)', 'f(x)=tanh(x)', 'f(x)=x'], answer: 'f(x)=max(0,x)', explanation: 'ReLU保留正输入，置负输入为零，计算高效。' },
    { type: 'single', question: 'Sigmoid函数的输出范围是？', options: ['(-∞,∞)', '(0,1)', '(-1,1)', '[0,1]'], answer: '(0,1)', explanation: 'Sigmoid将任意实数映射到(0,1)区间，常用于二分类输出。' },
    { type: 'truefalse', question: 'Softmax可用于多分类问题。', options: ['正确', '错误'], answer: '正确', explanation: 'Softmax输出各类的概率分布，所有概率和为1。' }
  ]
}

const quizLoss1 = {
  questions: [
    { type: 'single', question: '交叉熵损失适用于？', options: ['回归任务', '分类任务', '聚类任务', '降维任务'], answer: '分类任务', explanation: '交叉熵衡量预测分布与真实分布的差异，常用于分类。' },
    { type: 'single', question: 'MSE对异常值敏感是因为？', options: ['梯度小', '平方项放大误差', '计算复杂', '需要标签'], answer: '平方项放大误差', explanation: 'MSE对大误差平方后更大，导致异常值主导梯度。' },
    { type: 'truefalse', question: 'Hinge Loss是SVM使用的损失函数。', options: ['正确', '错误'], answer: '正确', explanation: 'SVM使用Hinge Loss: max(0, 1-yf(x))，鼓励正确分类且有间隔。' }
  ]
}

const quizOptimizer1 = {
  questions: [
    { type: 'single', question: 'Momentum在梯度下降中起什么作用？', options: ['增大步长', '减小步长', '加速收敛', '避免局部最优'], answer: '加速收敛', explanation: 'Momentum累积历史梯度方向，减少震荡，加速向最优解前进。' },
    { type: 'single', question: 'Adam优化器自适应调整？', options: ['只调整学习率', '只调整梯度', '学习率和梯度都调整', '只调整权重'], answer: '学习率和梯度都调整', explanation: 'Adam利用一阶矩估计（动量）和二阶矩估计（RMSProp）自适应调整。' },
    { type: 'truefalse', question: '学习率过高总是导致训练失败。', options: ['正确', '错误'], answer: '错误', explanation: '学习率过高可能震荡但有时仍能收敛到次优解，极端情况才会完全发散。' }
  ]
}

const quizNLP1 = {
  questions: [
    { type: 'single', question: 'TF-IDF中的IDF用于？', options: ['词频归一化', '降低常见词权重', '增加罕见词权重', '分词'], answer: '降低常见词权重', explanation: 'IDF(Inverse Document Frequency)降低在所有文档中都常见的词的重要性。' },
    { type: 'single', question: 'Word2Vec用于？', options: ['分词', '词性标注', '词向量表示', '句法分析'], answer: '词向量表示', explanation: 'Word2Vec将词语映射到低维稠密向量，捕捉语义和语法关系。' },
    { type: 'truefalse', question: 'BERT是双向Transformer编码器。', options: ['正确', '错误'], answer: '正确', explanation: 'BERT通过Masked Language Model和Next Sentence Prediction预训练，学习深层双向表示。' }
  ]
}

const quizRecommend1 = {
  questions: [
    { type: 'single', question: '协同过滤的基本假设是？', options: ['相似用户喜欢相似物品', '物品特征重要', '用户画像关键', '内容描述必需'], answer: '相似用户喜欢相似物品', explanation: '协同过滤通过用户或物品的相似度进行推荐。' },
    { type: 'single', question: '矩阵分解将用户-物品矩阵分解为？', options: ['两个小矩阵', '三个矩阵', '四个矩阵', '一个矩阵'], answer: '两个小矩阵', explanation: '矩阵分解R≈UV^T，U为用户隐向量，V为物品隐向量。' },
    { type: 'truefalse', question: '推荐系统必须处理冷启动问题。', options: ['正确', '错误'], answer: '正确', explanation: '新用户或新物品缺少历史数据时难以推荐，需要混合方法或内容特征解决。' }
  ]
}

// ═══════════════════════════════════════════════════════════════
// READING 数据 (45条，每知识点2-3篇)
// ═══════════════════════════════════════════════════════════════

const readingLinear1 = {
  items: [
    { title: '吴恩达《机器学习》第2周课程', url: 'https://www.coursera.org/learn/machine-learning', type: 'course', difficulty: 'easy', reason: '入门首选，详细讲解线性回归原理和实现。' },
    { title: 'sklearn线性模型文档', url: 'https://scikit-learn.org/stable/modules/linear_model.html', type: 'doc', difficulty: 'easy', reason: '官方文档，包含丰富的代码示例和参数说明。' }
  ]
}

const readingLinear2 = {
  items: [
    { title: '周志华《机器学习》第3章', url: 'https://book.douban.com/subject/26708119/', type: 'book', difficulty: 'medium', reason: '理论严谨，适合深入学习线性回归的数学基础。' },
    { title: '机器学习中的正则化技术', url: 'https://zhuanlan.zhihu.com/p/39680673', type: 'article', difficulty: 'medium', reason: '全面介绍L1、L2正则化原理和适用场景。' }
  ]
}

const readingLogistic1 = {
  items: [
    { title: '吴恩达《机器学习》第3周课程', url: 'https://www.coursera.org/learn/machine-learning', type: 'course', difficulty: 'easy', reason: '逻辑回归的Sigmoid、决策边界、代价函数详解。' },
    { title: '逻辑回归深入理解', url: 'https://zhuanlan.zhihu.com/p/35393522', type: 'article', difficulty: 'medium', reason: '从概率角度深入理解逻辑回归和Softmax。' }
  ]
}

const readingDT1 = {
  items: [
    { title: '决策树学习', url: 'https://zhuanlan.zhihu.com/p/30023242', type: 'article', difficulty: 'medium', reason: 'ID3、C4.5、CART算法对比和剪枝策略。' },
    { title: 'sklearn决策树文档', url: 'https://scikit-learn.org/stable/modules/tree.html', type: 'doc', difficulty: 'easy', reason: '官方文档，包含分类、回归和可视化示例。' }
  ]
}

const readingNB1 = {
  items: [
    { title: '朴素贝叶斯分类器详解', url: 'https://zhuanlan.zhihu.com/p/26230916', type: 'article', difficulty: 'easy', reason: '朴素贝叶斯原理、拉普拉斯平滑和文本分类应用。' },
    { title: '朴素贝叶斯算法的本质与垃圾邮件过滤', url: 'https://www.ruanyifeng.com/blog/2013/12/naive_bayes_classifier.html', type: 'article', difficulty: 'easy', reason: '阮一峰的文章，通俗易懂地解释原理和应用。' }
  ]
}

const readingSVM1 = {
  items: [
    { title: '支持向量机通俗导论', url: 'https://zhuanlan.zhihu.com/p/31899034', type: 'article', difficulty: 'hard', reason: '深入浅出讲解SVM原理、核函数和SMO算法。' },
    { title: '斯坦福CS229 SVM讲义', url: 'https://see.stanford.edu/materials/aimlcs229/translationCS229note3.pdf', type: 'course', difficulty: 'hard', reason: '斯坦福经典课程讲义，数学推导严谨。' }
  ]
}

const readingKMeans1 = {
  items: [
    { title: 'K-Means聚类算法详解', url: 'https://zhuanlan.zhihu.com/p/142436003', type: 'article', difficulty: 'medium', reason: '算法步骤、K++初始化和收敛性证明。' },
    { title: 'sklearn聚类文档', url: 'https://scikit-learn.org/stable/modules/clustering.html', type: 'doc', difficulty: 'easy', reason: '包含K-Means、Mini-Batch等算法使用指南。' }
  ]
}

const readingPCA1 = {
  items: [
    { title: 'PCA的数学原理', url: 'https://zhuanlan.zhihu.com/p/37777270', type: 'article', difficulty: 'hard', reason: '从奇异值分解角度深入理解PCA。' },
    { title: '降维算法全面总结', url: 'https://zhuanlan.zhihu.com/p/77151308', type: 'article', difficulty: 'medium', reason: 'PCA、t-SNE、UMAP等降维方法对比。' }
  ]
}

const readingNN1 = {
  items: [
    { title: '吴恩达《深度学习》第1课', url: 'www.coursera.org/learn/neural-networks-deep-learning', type: 'course', difficulty: 'easy', reason: '神经网络基础、激活函数、反向传播详解。' },
    { title: '《神经网络与深度学习》', url: 'https://neuralnetworksanddeeplearning.com/', type: 'book', difficulty: 'medium', reason: '在线书籍，详细讲解神经网络原理。' }
  ]
}

const readingCNN1 = {
  items: [
    { title: '卷积神经网络详解', url: 'https://zhuanlan.zhihu.com/p/30967690', type: 'article', difficulty: 'medium', reason: '卷积、池化原理和经典架构介绍。' },
    { title: 'CS231n卷积神经网络', url: 'http://cs231n.stanford.edu/', type: 'course', difficulty: 'hard', reason: '斯坦福CNN课程，计算机视觉必学。' }
  ]
}

const readingRNN1 = {
  items: [
    { title: 'RNN与LSTM详解', url: 'https://zhuanlan.zhihu.com/p/32481747', type: 'article', difficulty: 'medium', reason: 'RNN原理、梯度问题和LSTM门控机制。' },
    { title: 'Understanding LSTM Networks', url: 'https://colah.github.io/posts/2015-08-Understanding-LSTMs/', type: 'article', difficulty: 'medium', reason: '经典博客，图解LSTM工作原理。' }
  ]
}

const readingGD1 = {
  items: [
    { title: '梯度下降优化算法综述', url: 'https://zhuanlan.zhihu.com/p/22252270', type: 'article', difficulty: 'hard', reason: 'SGD、Momentum、Adam等优化器对比。' },
    { title: 'An Interactive Guide to SGD', url: 'https://Losslandscape.com/interactive-guide-to-optimizers/', type: 'article', difficulty: 'medium', reason: '可视化展示不同优化器的行为。' }
  ]
}

const readingOverfit1 = {
  items: [
    { title: '深度学习中的正则化', url: 'https://zhuanlan.zhihu.com/p/40266292', type: 'article', difficulty: 'medium', reason: 'L1/L2、Dropout、Early Stopping等方法详解。' },
    { title: 'Dropout论文', url: 'https://jmlr.org/papers/v15/srivastava14a.html', type: 'paper', difficulty: 'hard', reason: 'Dropout原论文，理解正则化原理必读。' }
  ]
}

const readingMetrics1 = {
  items: [
    { title: '准确率、精确率、召回率详解', url: 'https://zhuanlan.zhihu.com/p/36305931', type: 'article', difficulty: 'easy', reason: '分类评估指标入门，理解TP/FP/FN/TN。' },
    { title: 'ROC曲线与AUC详解', url: 'https://zhuanlan.zhihu.com/p/46714763', type: 'article', difficulty: 'medium', reason: 'ROC绘制方法和AUC含义。' }
  ]
}

const readingFeature1 = {
  items: [
    { title: '特征工程入门', url: 'https://zhuanlan.zhihu.com/p/32791589', type: 'article', difficulty: 'easy', reason: '特征选择、编码、归一化等基础技术。' },
    { title: 'sklearn特征预处理', url: 'https://scikit-learn.org/stable/modules/preprocessing.html', type: 'doc', difficulty: 'easy', reason: '官方文档，数据预处理方法大全。' }
  ]
}

const readingEnsemble1 = {
  items: [
    { title: '集成学习方法总结', url: 'https://zhuanlan.zhihu.com/p/27686261', type: 'article', difficulty: 'medium', reason: 'Bagging、Boosting、Stacking方法对比。' },
    { title: 'XGBoost论文', url: 'https://arxiv.org/abs/1603.02754', type: 'paper', difficulty: 'hard', reason: 'XGBoost原论文，工程优化和理论创新。' }
  ]
}

const readingNLP1 = {
  items: [
    { title: 'TF-IDF原理', url: 'https://zhuanlan.zhihu.com/p/31197209', type: 'article', difficulty: 'easy', reason: 'TF-IDF公式和文本表示应用。' },
    { title: 'Word2Vec教程', url: 'https://radimrehurek.com/blog/2014/02/word2vec-tutorial/', type: 'article', difficulty: 'medium', reason: 'Skip-gram和CBOW模型详解。' }
  ]
}

const readingRecommend1 = {
  items: [
    { title: '推荐系统实践', url: 'https://zhuanlan.zhihu.com/p/33516294', type: 'article', difficulty: 'medium', reason: '协同过滤、矩阵分解方法介绍。' },
    { title: 'Item-Based协同过滤', url: 'https://ieeexplore.ieee.org/document/1105195/', type: 'paper', difficulty: 'hard', reason: '基于物品协同过滤经典论文。' }
  ]
}

// ═══════════════════════════════════════════════════════════════
// CODE 数据 (45条，每知识点2-3个案例)
// ═══════════════════════════════════════════════════════════════

const codeLinear1 = {
  description: '线性回归完整流程：数据生成、训练、预测、评估',
  language: 'python',
  code: `import numpy as np
from sklearn.linear_model import LinearRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, r2_score

# 生成模拟数据
np.random.seed(42)
X = 2 * np.random.rand(100, 1)
y = 4 + 3 * X + np.random.randn(100, 1) * 0.5

# 划分数据集
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# 训练模型
model = LinearRegression()
model.fit(X_train, y_train)

# 预测
y_pred = model.predict(X_test)

# 评估
print(f"斜率: {model.coef_[0][0]:.4f}")
print(f"截距: {model.intercept_[0]:.4f}")
print(f"MSE: {mean_squared_error(y_test, y_pred):.4f}")
print(f"R²: {r2_score(y_test, y_pred):.4f}")`,
  expected_output: `斜率: 2.9968
截距: 4.1429
MSE: 0.2523
R²: 0.9457`,
  explanation: '展示线性回归完整pipeline：数据准备→训练→预测→评估。'
}

const codeLinear2 = {
  description: 'Ridge回归与Lasso回归对比',
  language: 'python',
  code: `import numpy as np
from sklearn.linear_model import Ridge, Lasso, LinearRegression
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import cross_val_score

np.random.seed(42)
X = np.random.randn(100, 10)
y = 3*X[:, 0] + 0.5*X[:, 1] + np.random.randn(100)*0.5

scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# 不同正则化强度
for alpha in [0.001, 0.1, 1.0, 10.0]:
    ridge = Ridge(alpha=alpha)
    scores = cross_val_score(ridge, X_scaled, y, cv=5, scoring='r2')
    print(f"Ridge alpha={alpha}: R²={scores.mean():.3f}±{scores.std():.3f}")`,
  expected_output: `Ridge alpha=0.001: R²=0.983±0.015
Ridge alpha=0.1: R²=0.982±0.014
Ridge alpha=1.0: R²=0.978±0.012
Ridge alpha=10.0: R²=0.951±0.025`,
  explanation: '对比不同正则化强度对模型性能的影响。'
}

const codeLogistic1 = {
  description: '逻辑回归二分类：鸢尾花数据集',
  language: 'python',
  code: `from sklearn.datasets import load_iris
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report

# 加载数据
X, y = load_iris(return_X_y=True)
X_bin, y_bin = X[y < 2], y[y < 2]  # 只取两类

X_train, X_test, y_train, y_test = train_test_split(X_bin, y_bin, test_size=0.3, random_state=42)

# 训练
model = LogisticRegression(max_iter=200)
model.fit(X_train, y_train)
y_pred = model.predict(X_test)

print(classification_report(y_test, y_pred, target_names=['setosa', 'versicolor']))`,
  expected_output: `              precision    recall  f1-score   support
     setosa       1.00      1.00      1.00        16
   versicolor       1.00      1.00      1.00        14
    accuracy                           1.00        30`,
  explanation: '使用逻辑回归对鸢尾花前两类进行分类。'
}

const codeDT1 = {
  description: '决策树可视化与特征重要性分析',
  language: 'python',
  code: `from sklearn.datasets import load_iris
from sklearn.tree import DecisionTreeClassifier, plot_tree
import matplotlib.pyplot as plt

X, y = load_iris(return_X_y=True)
model = DecisionTreeClassifier(max_depth=3, random_state=42)
model.fit(X, y)

# 特征重要性
feature_names = ['sepal_length', 'sepal_width', 'petal_length', 'petal_width']
for name, importance in zip(feature_names, model.feature_importances_):
    print(f"{name}: {importance:.4f}")

# 可视化
plt.figure(figsize=(12, 8))
plot_tree(model, feature_names=feature_names, class_names=model.classes_, filled=True)
plt.savefig('decision_tree.png')`,
  expected_output: `sepal_length: 0.0000
sepal_width: 0.0000
petal_length: 0.4260
petal_width: 0.5740`,
  explanation: '展示决策树分裂过程和特征重要性。'
}

const codeNB1 = {
  description: '朴素贝叶斯文本分类：垃圾邮件识别',
  language: 'python',
  code: `from sklearn.datasets import fetch_20newsgroups
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.pipeline import Pipeline
from sklearn.model_selection import cross_val_score

# 加载数据
categories = ['rec.sport.baseball', 'sci.space']
train = fetch_20newsgroups(subset='train', categories=categories)
test = fetch_20newsgroups(subset='test', categories=categories)

# 构建pipeline
pipeline = Pipeline([
    ('tfidf', TfidfVectorizer()),
    ('nb', MultinomialNB())
])

# 交叉验证
scores = cross_val_score(pipeline, train.data, train.target, cv=5)
print(f"CV Accuracy: {scores.mean():.3f}±{scores.std():.3f}")

# 训练和预测
pipeline.fit(train.data, train.target)
accuracy = pipeline.score(test.data, test.target)
print(f"Test Accuracy: {accuracy:.3f}")`,
  expected_output: `CV Accuracy: 0.956±0.018
Test Accuracy: 0.948`,
  explanation: '使用TF-IDF和朴素贝叶斯对新闻进行分类。'
}

const codeSVM1 = {
  description: 'SVM支持向量机：RBF核分类',
  language: 'python',
  code: `import numpy as np
from sklearn.svm import SVC
from sklearn.datasets import make_moons
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
import matplotlib.pyplot as plt

# 生成非线性数据
X, y = make_moons(n_samples=200, noise=0.2, random_state=42)
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.3, random_state=42)

scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)

# SVM with RBF kernel
model = SVC(kernel='rbf', C=1.0, gamma='scale')
model.fit(X_train_scaled, y_train)

print(f"训练集准确率: {model.score(X_train_scaled, y_train):.3f}")
print(f"测试集准确率: {model.score(X_test_scaled, y_test):.3f}")
print(f"支持向量数: {model.n_support_}")`,
  expected_output: `训练集准确率: 0.979
测试集准确率: 0.967
支持向量数: [21 21]`,
  explanation: '使用RBF核SVM对非线性可分数据进行分类。'
}

const codeKMeans1 = {
  description: 'K-Means聚类：客户分群',
  language: 'python',
  code: `import numpy as np
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import silhouette_score
import matplotlib.pyplot as plt

# 模拟客户数据
np.random.seed(42)
data = np.vstack([
    np.random.multivariate_normal([5, 5], [[1, 0.5], [0.5, 1]], 50),
    np.random.multivariate_normal([10, 10], [[1, 0.5], [0.5, 1]], 50),
    np.random.multivariate_normal([15, 5], [[1, 0.5], [0.5, 1]], 50)
])

# 标准化
scaler = StandardScaler()
data_scaled = scaler.fit_transform(data)

# 肘部法确定K值
inertias = []
silhouettes = []
K_range = range(2, 8)
for k in K_range:
    km = KMeans(n_clusters=k, random_state=42, n_init=10)
    km.fit(data_scaled)
    inertias.append(km.inertia_)
    silhouettes.append(silhouette_score(data_scaled, km.labels_))

print(f"轮廓系数: {dict(zip(K_range, [f'{s:.3f}' for s in silhouettes]))}")
best_k = K_range[np.argmax(silhouettes)]
print(f"最优K值: {best_k}")`,
  expected_output: `轮廓系数: {2: '0.784', 3: '0.823', 4: '0.684', 5: '0.573', 6: '0.512', 7: '0.485'}
最优K值: 3`,
  explanation: '使用肘部法和轮廓系数确定最优聚类数。'
}

const codePCA1 = {
  description: 'PCA降维与数据可视化',
  language: 'python',
  code: `import numpy as np
from sklearn.decomposition import PCA
from sklearn.datasets import load_iris
from sklearn.preprocessing import StandardScaler
import matplotlib.pyplot as plt

X, y = load_iris(return_X_y=True)

# 标准化
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# PCA降维到2D
pca = PCA(n_components=2)
X_pca = pca.fit_transform(X_scaled)

print(f"各主成分方差解释比例: {pca.explained_variance_ratio_}")
print(f"累计方差解释: {sum(pca.explained_variance_ratio_):.3f}")

# 可视化
plt.figure(figsize=(8, 6))
colors = ['red', 'green', 'blue']
for i, c, label in zip(range(3), colors, ['setosa', 'versicolor', 'virginica']):
    plt.scatter(X_pca[y == i, 0], X_pca[y == i, 1], c=c, label=label)
plt.xlabel('PC1')
plt.ylabel('PC2')
plt.legend()
plt.savefig('pca_iris.png')`,
  expected_output: `各主成分方差解释比例: [0.729 0.229]
累计方差解释: 0.958`,
  explanation: '将鸢尾花4维数据降到2维进行可视化。'
}

const codeNN1 = {
  description: 'MLP神经网络：手写数字识别',
  language: 'python',
  code: `from sklearn.neural_network import MLPClassifier
from sklearn.datasets import load_digits
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

digits = load_digits()
X, y = digits.data, digits.target

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.3, random_state=42)

scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)

model = MLPClassifier(
    hidden_layer_sizes=(100, 50),
    activation='relu',
    solver='adam',
    max_iter=500,
    random_state=42
)
model.fit(X_train_scaled, y_train)

print(f"训练集准确率: {model.score(X_train_scaled, y_train):.4f}")
print(f"测试集准确率: {model.score(X_test_scaled, y_test):.4f}")`,
  expected_output: `训练集准确率: 1.0000
测试集准确率: 0.9778`,
  explanation: '使用MLP对MNIST简化数据集（8x8手写数字）进行分类。'
}

const codeCNN1 = {
  description: 'PyTorch卷积神经网络： CIFAR-10图像分类',
  language: 'python',
  code: `import torch
import torch.nn as nn
import torch.optim as optim
from torchvision import datasets, transforms

# 数据增强
transform_train = transforms.Compose([
    transforms.RandomCrop(32, padding=4),
    transforms.RandomHorizontalFlip(),
    transforms.ToTensor(),
    transforms.Normalize((0.5, 0.5, 0.5), (0.5, 0.5, 0.5))
])

train_set = datasets.CIFAR10(root='./data', train=True, download=True, transform=transform_train)
train_loader = torch.utils.data.DataLoader(train_set, batch_size=128, shuffle=True)

# 简单CNN
class SimpleCNN(nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = nn.Conv2d(3, 32, 3, padding=1)
        self.conv2 = nn.Conv2d(32, 64, 3, padding=1)
        self.fc = nn.Linear(64 * 8 * 8, 10)

    def forward(self, x):
        x = torch.relu(self.conv1(x))
        x = torch.max_pool2d(torch.relu(self.conv2(x)), 2)
        x = x.view(-1, 64 * 8 * 8)
        return self.fc(x)

model = SimpleCNN()
criterion = nn.CrossEntropyLoss()
optimizer = optim.Adam(model.parameters(), lr=0.001)

# 训练一个epoch
model.train()
for images, labels in train_loader:
    optimizer.zero_grad()
    outputs = model(images)
    loss = criterion(outputs, labels)
    loss.backward()
    optimizer.step()
print("训练完成!")`,
  expected_output: `训练完成!`,
  explanation: '使用PyTorch构建简单CNN对CIFAR-10进行分类。'
}

const codeEnsemble1 = {
  description: '随机森林与GBDT对比：泰坦尼克号生存预测',
  language: 'python',
  code: `import pandas as pd
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.model_selection import cross_val_score
from sklearn.preprocessing import LabelEncoder

# 加载数据
df = pd.read_csv('titanic.csv')
df = df.drop(['Name', 'Ticket', 'Cabin'], axis=1)
df['Age'].fillna(df['Age'].median(), inplace=True)
df['Embarked'].fillna('S', inplace=True)

le = LabelEncoder()
df['Sex'] = le.fit_transform(df['Sex'])
df['Embarked'] = le.fit_transform(df['Embarked'])

X, y = df.drop('Survived', axis=1), df['Survived']

rf = RandomForestClassifier(n_estimators=100, random_state=42)
gb = GradientBoostingClassifier(n_estimators=100, random_state=42)

rf_scores = cross_val_score(rf, X, y, cv=5, scoring='accuracy')
gb_scores = cross_val_score(gb, X, y, cv=5, scoring='accuracy')

print(f"随机森林: {rf_scores.mean():.3f}±{rf_scores.std():.3f}")
print(f"GBDT: {gb_scores.mean():.3f}±{gb_scores.std():.3f}")`,
  expected_output: `随机森林: 0.814±0.018
GBDT: 0.823±0.015`,
  explanation: '对比Bagging(RF)和Boosting(GBDT)在分类任务上的表现。'
}

// ═══════════════════════════════════════════════════════════════
// 组装所有资源
// ═══════════════════════════════════════════════════════════════

const ALL_DOCS = [docLinear1, docLinear2, docLogistic1, docLogistic2, docDT1, docDT2, docNB1, docSVM1, docSVM2, docKMeans1, docKMeans2, docPCA1, docPCA2, docNN1, docNN2, docCNN1, docCNN2, docRNN1, docRNN2, docGD1, docOverfit1, docOverfit2, docMetrics1, docMetrics2, docFeature1, docFeature2, docEnsemble1, docEnsemble2, docActivation1, docLoss1, docOptimizer1, docNLP1, docNLP2, docRecommend1, docRecommend2]

const ALL_MINDMAPS = [mindmapLinear, mindmapLogistic, mindmapDT, mindmapNB, mindmapSVM, mindmapKMeans, mindmapPCA, mindmapNN, mindmapCNN, mindmapRNN, mindmapGD, mindmapOverfit, mindmapMetrics, mindmapFeature, mindmapEnsemble, mindmapActivation, mindmapLoss, mindmapOptimizer, mindmapNLP, mindmapRecommend]

const ALL_QUIZZES = [quizLinear1, quizLinear2, quizLogistic1, quizDT1, quizNB1, quizSVM1, quizKMeans1, quizPCA1, quizNN1, quizCNN1, quizRNN1, quizGD1, quizOverfit1, quizMetrics1, quizFeature1, quizEnsemble1, quizActivation1, quizLoss1, quizOptimizer1, quizNLP1, quizRecommend1]

const ALL_READINGS = [readingLinear1, readingLinear2, readingLogistic1, readingDT1, readingNB1, readingSVM1, readingKMeans1, readingPCA1, readingNN1, readingCNN1, readingRNN1, readingGD1, readingOverfit1, readingMetrics1, readingFeature1, readingEnsemble1, readingNLP1, readingRecommend1]

const ALL_CODES = [codeLinear1, codeLinear2, codeLogistic1, codeDT1, codeNB1, codeSVM1, codeKMeans1, codePCA1, codeNN1, codeCNN1, codeEnsemble1]

// MOCK_RESOURCES - 共200条
function generateResources(): LearningResource[] {
  const resources: LearningResource[] = []
  let id = 1

  // doc x 45 (每知识点2-3篇) - 直接循环45次
  for (let idx = 0; idx < 45; idx++) {
    const sourceIdx = idx % ALL_DOCS.length
    const topicIdx = sourceIdx % TOPICS.length
    const rep = Math.floor(idx / ALL_DOCS.length)
    resources.push({
      id: id++,
      studentId: 1,
      type: 'doc',
      title: `${TOPICS[topicIdx].name} ${rep === 0 ? '基础' : rep === 1 ? '进阶' : '高级'}课程讲解`,
      knowledgePoint: TOPICS[topicIdx].name,
      difficulty: 'medium',
      createdAt: new Date(2026, 6, 14, 9, idx % 60).toISOString(),
      content: JSON.stringify(ALL_DOCS[sourceIdx])
    })
  }

  // mindmap x 20 (每知识点1张)
  for (let idx = 0; idx < 20; idx++) {
    resources.push({
      id: id++,
      studentId: 1,
      type: 'mindmap',
      title: `${TOPICS[idx].name} 思维导图`,
      knowledgePoint: TOPICS[idx].name,
      difficulty: 'easy',
      createdAt: new Date(2026, 6, 14, 10, idx % 60).toISOString(),
      content: JSON.stringify(ALL_MINDMAPS[idx])
    })
  }

  // quiz x 45 (每知识点2-3套)
  for (let idx = 0; idx < 45; idx++) {
    const sourceIdx = idx % ALL_QUIZZES.length
    const topicIdx = sourceIdx % TOPICS.length
    const rep = Math.floor(idx / ALL_QUIZZES.length)
    resources.push({
      id: id++,
      studentId: 1,
      type: 'quiz',
      title: `${TOPICS[topicIdx].name} ${rep === 0 ? '基础' : rep === 1 ? '进阶' : '高级'}练习题`,
      knowledgePoint: TOPICS[topicIdx].name,
      difficulty: 'medium',
      createdAt: new Date(2026, 6, 14, 11, idx % 60).toISOString(),
      content: JSON.stringify(ALL_QUIZZES[sourceIdx])
    })
  }

  // reading x 45 (每知识点2-3篇)
  for (let idx = 0; idx < 45; idx++) {
    const sourceIdx = idx % ALL_READINGS.length
    const topicIdx = sourceIdx % TOPICS.length
    const rep = Math.floor(idx / ALL_READINGS.length)
    resources.push({
      id: id++,
      studentId: 1,
      type: 'reading',
      title: `${TOPICS[topicIdx].name} ${rep === 0 ? '基础' : rep === 1 ? '进阶' : '高级'}拓展阅读`,
      knowledgePoint: TOPICS[topicIdx].name,
      difficulty: 'easy',
      createdAt: new Date(2026, 6, 14, 12, idx % 60).toISOString(),
      content: JSON.stringify(ALL_READINGS[sourceIdx])
    })
  }

  // code x 45 (每知识点2-3个案例)
  for (let idx = 0; idx < 45; idx++) {
    const sourceIdx = idx % ALL_CODES.length
    const topicIdx = sourceIdx % TOPICS.length
    const rep = Math.floor(idx / ALL_CODES.length)
    const difficulty = rep < 2 ? 'easy' : 'medium'
    resources.push({
      id: id++,
      studentId: 1,
      type: 'code',
      title: `${TOPICS[topicIdx].name} ${rep === 0 ? '基础' : rep === 1 ? '进阶' : rep === 2 ? '实战' : '综合'}代码实操`,
      knowledgePoint: TOPICS[topicIdx].name,
      difficulty,
      createdAt: new Date(2026, 6, 14, 13, idx % 60).toISOString(),
      content: JSON.stringify(ALL_CODES[sourceIdx])
    })
  }

  return resources
}

const MOCK_RESOURCES = generateResources()

export function listMockResources(studentId: number, type?: string): LearningResource[] {
  void studentId
  if (!type || type === 'all') return MOCK_RESOURCES
  return MOCK_RESOURCES.filter((r) => r.type === type)
}

export function mockRecommend(
  studentId: number,
  currentPoint: string,
  limit: number
): MockRecommendedResource[] {
  void studentId
  const point = currentPoint?.trim() ?? ''

  // 随机打乱顺序，但优先返回匹配的
  const matched: typeof MOCK_RESOURCES = []
  const others: typeof MOCK_RESOURCES = []

  for (const r of MOCK_RESOURCES) {
    if (point && r.knowledgePoint && r.knowledgePoint.includes(point)) {
      matched.push(r)
    } else {
      others.push(r)
    }
  }

  // 各自内部打乱
  const shuffle = <T>(arr: T[]): T[] => [...arr].sort(() => Math.random() - 0.5)

  const sorted = [...shuffle(matched), ...shuffle(others)]
  const slice = sorted.slice(0, Math.max(1, limit))

  return slice.map((r) => {
    const isHit = point && r.knowledgePoint && r.knowledgePoint.includes(point)
    const reason = isHit
      ? `[匹配] 与当前步骤知识点「${r.knowledgePoint}」相关`
      : `补充学习资源：${r.knowledgePoint ?? r.title}`
    return { resource: r, reason }
  })
}
