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
 * <p>每种类型 2 条，共 10 条。覆盖 doc / mindmap / quiz / reading / code。
 * content 字段是 JSON 字符串，与后端 {@code LearningResourceMapper} 序列化方式一致。
 *
 * <p>使用方式：{@link listMockResources}（前端不依赖后端即可展示完整 5 种资源类型）。
 */

// ────────────────────────── doc ──────────────────────────
const docLinear = {
  markdown: `# 线性回归原理与应用

## 一、概念介绍

线性回归（Linear Regression）是机器学习中最基础的有监督学习算法之一，用于预测一个**连续型目标变量**。
它假设自变量与因变量之间存在线性关系，通过学习一条直线（或超平面）来拟合数据。

> 例如：根据房屋面积预测房价、根据学习时长预测考试成绩，都可以使用线性回归建模。

## 二、核心原理

### 2.1 数学推导

预测函数：

$$\\hat{y} = w_1 x_1 + w_2 x_2 + \\dots + w_n x_n + b = \\mathbf{w}^\\top \\mathbf{x} + b$$

损失函数（均方误差 MSE）：

$$L(\\mathbf{w}, b) = \\frac{1}{m} \\sum_{i=1}^{m} \\left( \\hat{y}^{(i)} - y^{(i)} \\right)^2$$

通过**最小二乘法**或**梯度下降法**求解最优参数：

- 最小二乘法（闭式解）：$\\mathbf{w} = (X^\\top X)^{-1} X^\\top y$
- 梯度下降（迭代解）：$\\mathbf{w} \\leftarrow \\mathbf{w} - \\alpha \\frac{\\partial L}{\\partial \\mathbf{w}}$

### 2.2 算法流程

1. 准备数据，划分训练集 / 验证集 / 测试集
2. 选择损失函数（MSE / RMSE / MAE）
3. 初始化参数（$\\mathbf{w}$ 随机，$b = 0$）
4. 迭代优化：前向传播计算 $\\hat{y}$ → 计算损失 → 反向传播更新参数
5. 评估：$R^2$、MSE、MAE 等指标

## 三、代码示例

\`\`\`python
import numpy as np
from sklearn.linear_model import LinearRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, r2_score

X = np.array([[1], [2], [3], [4], [5], [6], [7], [8]])
y = np.array([2, 4, 5, 4, 5, 7, 8, 9])

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.25, random_state=42)
model = LinearRegression().fit(X_train, y_train)
y_pred = model.predict(X_test)
print(f"R²: {r2_score(y_test, y_pred):.2f}")
\`\`\`

## 四、适用场景

✅ **适合**：
- 自变量与因变量近似线性关系
- 特征数量适中（< 1万）
- 需要可解释的模型（系数即特征重要性）

❌ **不适合**：
- 特征与目标存在非线性关系
- 数据存在严重多重共线性

## 五、常见易错点

1. **未做特征归一化**：导致梯度下降收敛慢
2. **忽略多重共线性**：系数符号与业务直觉相反
3. **训练集 / 测试集未随机划分**：评估指标虚高
4. **将分类变量直接喂入**：应先 one-hot 编码`,
  summary: '线性回归的数学原理、求解方法、代码实现与适用场景，是入门机器学习的第一站。'
}

const docLogistic = {
  markdown: `# 逻辑回归：从回归到分类

## 一、概念介绍

逻辑回归（Logistic Regression）虽然名字里带"回归"，但它实际上是一个**分类算法**。
核心思想：将线性回归的输出通过 **Sigmoid 函数** 映射到 (0, 1) 区间，表示样本属于某一类的概率。

> 适用于：垃圾邮件识别、疾病预测、用户点击率预估等二分类场景。

## 二、Sigmoid 函数

$$\\sigma(z) = \\frac{1}{1 + e^{-z}}$$

将任意实数映射到 (0, 1)，并具备良好的数学性质：$\\sigma'(z) = \\sigma(z)(1-\\sigma(z))$。

## 三、损失函数：交叉熵

$$L(\\mathbf{w}, b) = -\\frac{1}{m} \\sum_{i=1}^{m} \\left[ y^{(i)} \\log \\hat{y}^{(i)} + (1-y^{(i)}) \\log (1-\\hat{y}^{(i)}) \\right]$$

## 四、决策边界

当 $\\hat{y} > 0.5$ 判为正类（1），否则判为负类（0）。通过调整阈值可以调节精确率与召回率。

## 五、多分类扩展

- **One-vs-Rest**：训练 K 个二分类器
- **Softmax 回归**：直接推广到多分类

## 六、易错点

1. 直接用 MSE 训练 → 损失函数非凸，易陷入局部最优
2. 数据未做标准化 → 收敛慢
3. 类别极不平衡 → 应配合 class_weight 或过采样`,
  summary: '逻辑回归的核心是 Sigmoid + 交叉熵，是从线性回归迈向分类的桥梁。'
}

// ────────────────────────── mindmap ──────────────────────────
const mindmapSupervised = {
  tree: {
    name: '监督学习',
    children: [
      {
        name: '分类',
        children: [
          { name: '逻辑回归', children: [{ name: 'Sigmoid' }, { name: '交叉熵' }, { name: '正则化' }] },
          { name: '决策树', children: [{ name: '信息增益' }, { name: '基尼系数' }, { name: '剪枝' }] },
          { name: 'SVM', children: [{ name: '最大间隔' }, { name: '核函数' }, { name: '软间隔' }] },
          { name: '朴素贝叶斯' },
          { name: 'KNN' }
        ]
      },
      {
        name: '回归',
        children: [
          { name: '线性回归', children: [{ name: '最小二乘' }, { name: '梯度下降' }, { name: '多项式回归' }] },
          { name: 'Ridge / Lasso' },
          { name: '决策树回归' }
        ]
      },
      {
        name: '集成学习',
        children: [
          { name: 'Bagging', children: [{ name: '随机森林' }] },
          { name: 'Boosting', children: [{ name: 'AdaBoost' }, { name: 'GBDT' }, { name: 'XGBoost' }] }
        ]
      }
    ]
  }
}

const mindmapMl = {
  tree: {
    name: '机器学习',
    children: [
      {
        name: '基础概念',
        children: [{ name: '数据集划分' }, { name: '评估指标' }, { name: '偏差与方差' }]
      },
      {
        name: '经典算法',
        children: [
          { name: '监督学习', children: [{ name: '线性回归' }, { name: '逻辑回归' }, { name: '决策树' }, { name: 'SVM' }] },
          { name: '无监督学习', children: [{ name: 'K-Means' }, { name: 'PCA' }] }
        ]
      },
      {
        name: '深度学习',
        children: [{ name: '神经网络' }, { name: 'CNN' }, { name: 'RNN' }, { name: 'Transformer' }]
      },
      {
        name: '实战工具',
        children: [{ name: 'NumPy' }, { name: 'Pandas' }, { name: 'Scikit-learn' }, { name: 'PyTorch' }]
      }
    ]
  }
}

// ────────────────────────── quiz ──────────────────────────
const quizLinear = {
  questions: [
    {
      type: 'single',
      question: '线性回归最常用的损失函数是？',
      options: ['交叉熵', '均方误差（MSE）', 'Hinge Loss', 'KL 散度'],
      answer: '均方误差（MSE）',
      explanation: 'MSE 对异常值敏感、可导、且对应最小二乘的闭式解，是回归任务的首选。'
    },
    {
      type: 'multiple',
      question: '下列哪些方法可以缓解过拟合？（多选）',
      options: ['增加训练数据', '降低模型复杂度', 'L1/L2 正则化', '学习率调大'],
      answer: ['增加训练数据', '降低模型复杂度', 'L1/L2 正则化'],
      explanation: '增加数据、降低复杂度、正则化都能缓解过拟合；调大学习率反而容易发散。'
    },
    {
      type: 'truefalse',
      question: '线性回归要求自变量与因变量严格线性相关，否则模型一定无效。',
      options: ['正确', '错误'],
      answer: '错误',
      explanation: '线性回归只要求线性假设，对非线性数据可通过多项式特征或换模型解决。'
    },
    {
      type: 'short',
      question: '简述最小二乘法与梯度下降各自的优缺点。',
      answer: '最小二乘法闭式解直接最优，但矩阵求逆 O(n³) 代价大；梯度下降可处理大规模数据，但需要调学习率和迭代次数。',
      explanation: '数据规模小时用正规方程，大数据用梯度下降或随机梯度下降。'
    }
  ]
}

const quizDecisionTree = {
  questions: [
    {
      type: 'single',
      question: '决策树划分时常用的指标不包括？',
      options: ['信息增益', '基尼系数', '方差缩减', '余弦相似度'],
      answer: '余弦相似度',
      explanation: '余弦相似度用于衡量向量夹角，不用于决策树划分。'
    },
    {
      type: 'multiple',
      question: '下列属于决策树剪枝策略的有？（多选）',
      options: ['预剪枝（限制深度）', '后剪枝（CCP）', 'L1 正则化', '设置 min_samples_leaf'],
      answer: ['预剪枝（限制深度）', '后剪枝（CCP）', '设置 min_samples_leaf'],
      explanation: '剪枝通过限制树的复杂度来防止过拟合。'
    },
    {
      type: 'truefalse',
      question: 'ID3 决策树使用基尼系数作为划分指标。',
      options: ['正确', '错误'],
      answer: '错误',
      explanation: 'ID3 使用信息增益，C4.5 使用信息增益率，CART 使用基尼系数。'
    }
  ]
}

// ────────────────────────── reading ──────────────────────────
const readingLinear = {
  items: [
    {
      title: '吴恩达《机器学习》课程 - 第 1-2 周',
      url: 'https://www.coursera.org/learn/machine-learning',
      type: 'course',
      difficulty: 'easy',
      reason: '入门首选：从线性回归讲到梯度下降，配合编程练习建立直觉。'
    },
    {
      title: '周志华《机器学习》（西瓜书）第 3 章',
      url: 'https://cs.nju.edu.cn/zhouzh/zhouzh.files/publication/ML%20%20book/%E6%9C%BA%E5%99%A8%E5%AD%A6%E4%B9%A0%E5%9B%9B%E6%9C%88%E8%8B%B1%E6%96%87%E6%9C%88%E5%88%8A%E7%89%88.pdf',
      type: 'book',
      difficulty: 'medium',
      reason: '公式推导严谨，覆盖线性回归、Logistic 回归与正则化。'
    },
    {
      title: 'Scikit-learn 官方文档 - Linear Models',
      url: 'https://scikit-learn.org/stable/modules/linear_model.html',
      type: 'doc',
      difficulty: 'easy',
      reason: '查阅 LinearRegression / Ridge / Lasso 的 API 与示例代码。'
    },
    {
      title: '李宏毅《机器学习》线性回归章节',
      url: 'http://speech.ee.ntu.edu.tw/~tlkagk/courses_ML20.html',
      type: 'course',
      difficulty: 'easy',
      reason: '中文讲解，亲和力强，包含梯度下降的直观可视化。'
    }
  ]
}

const readingDecisionTree = {
  items: [
    {
      title: '西瓜书 第 4 章 - 决策树',
      url: 'https://www.wzkaixin.com/book/xiyou/decision-tree.html',
      type: 'book',
      difficulty: 'medium',
      reason: '系统讲解信息增益、增益率、基尼系数与剪枝。'
    },
    {
      title: 'Random Forest 经典论文 - Breiman 2001',
      url: 'https://link.springer.com/article/10.1023/A:1010933404324',
      type: 'paper',
      difficulty: 'hard',
      reason: '集成学习的奠基论文，了解 Bagging 思想必读。'
    },
    {
      title: 'Kaggle Learn - Intro to Machine Learning',
      url: 'https://www.kaggle.com/learn/intro-to-machine-learning',
      type: 'course',
      difficulty: 'easy',
      reason: '通过决策树与随机森林的实例练习，快速上手建模流程。'
    },
    {
      title: 'XGBoost 论文 - Chen & Guestrin 2016',
      url: 'https://arxiv.org/abs/1603.02754',
      type: 'paper',
      difficulty: 'hard',
      reason: '理解 Boosting 思想与工程优化，工业界高频使用的算法。'
    }
  ]
}

// ────────────────────────── code ──────────────────────────
const codeLinear = {
  description: '使用 Scikit-learn 实现线性回归拟合一维数据，并可视化预测结果。',
  dataset: '随机生成的 100 个样本（y = 4 + 3x + 噪声）',
  language: 'python',
  code: `import numpy as np
from sklearn.linear_model import LinearRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, r2_score
import matplotlib.pyplot as plt

# 1. 准备数据
np.random.seed(42)
X = 2 * np.random.rand(100, 1)
y = 4 + 3 * X.squeeze() + np.random.randn(100) * 0.5

# 2. 划分训练集 / 测试集
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# 3. 训练模型
model = LinearRegression()
model.fit(X_train, y_train)

# 4. 评估
y_pred = model.predict(X_test)
print(f"权重 w = {model.coef_[0]:.4f}")
print(f"偏置 b = {model.intercept_:.4f}")
print(f"R² = {r2_score(y_test, y_pred):.4f}")
print(f"MSE = {mean_squared_error(y_test, y_pred):.4f}")

# 5. 可视化
plt.scatter(X_test, y_test, color='blue', label='真实值')
plt.plot(X_test, y_pred, color='red', linewidth=2, label='预测值')
plt.xlabel('x')
plt.ylabel('y')
plt.legend()
plt.title('Linear Regression Demo')
plt.show()`,
  expected_output: `权重 w = 2.9968
偏置 b = 4.1429
R² = 0.9457
MSE = 0.2523`,
  explanation:
    '① 准备数据：用 np.random 生成带噪声的线性数据；② 划分训练/测试集避免评估偏差；' +
    '③ fit() 内部通过最小二乘法求解最优 w 和 b；④ 用 R²、MSE 评估模型；' +
    '⑤ 最后用 matplotlib 把测试集的预测结果可视化出来，直观判断拟合效果。'
}

const codeDecisionTree = {
  description: '使用 Scikit-learn 的 DecisionTreeClassifier 在鸢尾花数据集上做多分类。',
  dataset: 'sklearn.datasets.load_iris()',
  language: 'python',
  code: `from sklearn.datasets import load_iris
from sklearn.tree import DecisionTreeClassifier, export_text
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report

# 1. 加载数据
iris = load_iris()
X, y = iris.data, iris.target

# 2. 划分数据集
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.3, random_state=42, stratify=y
)

# 3. 训练决策树（限制最大深度防止过拟合）
clf = DecisionTreeClassifier(max_depth=3, random_state=42)
clf.fit(X_train, y_train)

# 4. 输出树结构
print(export_text(clf, feature_names=iris.feature_names))

# 5. 在测试集上评估
y_pred = clf.predict(X_test)
print(classification_report(y_test, y_pred, target_names=iris.target_names))`,
  expected_output: `|--- petal width (cm) <= 0.75
|   |--- class: 0
|--- petal width (cm) >  0.75
|   |--- petal length (cm) <= 4.95
|   |   |--- class: 1
|   |--- petal length (cm) >  4.95
|   |   |--- class: 2

              precision    recall  f1-score   support

      setosa       1.00      1.00      1.00        15
  versicolor       0.94      0.94      0.94        16
   virginica       0.93      0.93      0.93        14

    accuracy                           0.96        45`,
  explanation:
    '① 鸢尾花数据集是经典的多分类案例，3 个类别各 50 个样本；' +
    '② stratify=y 保证训练/测试集中类别比例一致；' +
    '③ max_depth=3 限制树的复杂度，是预剪枝的常见做法；' +
    '④ export_text 可以把树的结构以文本形式打印，便于解释模型；' +
    '⑤ classification_report 同时输出 precision / recall / f1，多分类任务必备。'
}

// ────────────────────────── 组装 10 条 LearningResource ──────────────────────────

const MOCK_RESOURCES: LearningResource[] = [
  {
    id: 1,
    studentId: 1,
    type: 'doc',
    title: '线性回归 课程讲解',
    knowledgePoint: '线性回归',
    difficulty: 'medium',
    createdAt: '2026-07-14T09:00:00',
    content: JSON.stringify(docLinear)
  },
  {
    id: 2,
    studentId: 1,
    type: 'doc',
    title: '逻辑回归 课程讲解',
    knowledgePoint: '逻辑回归',
    difficulty: 'medium',
    createdAt: '2026-07-14T09:10:00',
    content: JSON.stringify(docLogistic)
  },
  {
    id: 3,
    studentId: 1,
    type: 'mindmap',
    title: '监督学习 思维导图',
    knowledgePoint: '监督学习',
    difficulty: 'easy',
    createdAt: '2026-07-14T09:20:00',
    content: JSON.stringify(mindmapSupervised)
  },
  {
    id: 4,
    studentId: 1,
    type: 'mindmap',
    title: '机器学习 知识体系导图',
    knowledgePoint: '机器学习',
    difficulty: 'easy',
    createdAt: '2026-07-14T09:30:00',
    content: JSON.stringify(mindmapMl)
  },
  {
    id: 5,
    studentId: 1,
    type: 'quiz',
    title: '线性回归 练习题',
    knowledgePoint: '线性回归',
    difficulty: 'medium',
    createdAt: '2026-07-14T09:40:00',
    content: JSON.stringify(quizLinear)
  },
  {
    id: 6,
    studentId: 1,
    type: 'quiz',
    title: '决策树 练习题',
    knowledgePoint: '决策树',
    difficulty: 'medium',
    createdAt: '2026-07-14T09:50:00',
    content: JSON.stringify(quizDecisionTree)
  },
  {
    id: 7,
    studentId: 1,
    type: 'reading',
    title: '线性回归 拓展阅读',
    knowledgePoint: '线性回归',
    difficulty: 'easy',
    createdAt: '2026-07-14T10:00:00',
    content: JSON.stringify(readingLinear)
  },
  {
    id: 8,
    studentId: 1,
    type: 'reading',
    title: '决策树 拓展阅读',
    knowledgePoint: '决策树',
    difficulty: 'medium',
    createdAt: '2026-07-14T10:10:00',
    content: JSON.stringify(readingDecisionTree)
  },
  {
    id: 9,
    studentId: 1,
    type: 'code',
    title: '线性回归 代码实操',
    knowledgePoint: '线性回归',
    difficulty: 'medium',
    createdAt: '2026-07-14T10:20:00',
    content: JSON.stringify(codeLinear)
  },
  {
    id: 10,
    studentId: 1,
    type: 'code',
    title: '决策树 代码实操',
    knowledgePoint: '决策树',
    difficulty: 'medium',
    createdAt: '2026-07-14T10:30:00',
    content: JSON.stringify(codeDecisionTree)
  }
]

/**
 * 返回 mock 资源列表。可选按 type 过滤，与真实接口语义一致。
 */
export function listMockResources(studentId: number, type?: string): LearningResource[] {
  // 让不同学生看到同样内容即可（mock 阶段不区分）
  void studentId
  if (!type || type === 'all') return MOCK_RESOURCES
  return MOCK_RESOURCES.filter((r) => r.type === type)
}

/**
 * 把 mock 资源包装成 RecommendedResource，供学习路径页 fallback 使用。
 *
 * <p>当前路径知识点（currentPoint）若能在某条资源的 knowledgePoint 里命中，
 * 就把这条资源提到最前，并给出一个面向路径上下文的推荐理由；其余资源给出
 * 基于弱项 / 兴趣的兜底理由。覆盖 5 种类型，保证"下面有内容"。
 */
export function mockRecommend(
  studentId: number,
  currentPoint: string,
  limit: number
): MockRecommendedResource[] {
  void studentId
  const point = currentPoint?.trim() ?? ''
  const sorted = [...MOCK_RESOURCES].sort((a, b) => {
    const aHit = point && a.knowledgePoint && a.knowledgePoint.includes(point) ? 1 : 0
    const bHit = point && b.knowledgePoint && b.knowledgePoint.includes(point) ? 1 : 0
    return bHit - aHit
  })
  const slice = sorted.slice(0, Math.max(1, limit))
  return slice.map((r, idx) => {
    const isHit = point && r.knowledgePoint && r.knowledgePoint.includes(point)
    const reason = isHit
      ? `📍 与当前步骤知识点「${r.knowledgePoint}」匹配，推荐优先学习`
      : idx === 0
        ? `基于画像推荐的入门资源：${r.knowledgePoint ?? r.title}`
        : `补充学习资源（${r.knowledgePoint ?? r.title}），扩展知识面`
    return { resource: r, reason }
  })
}