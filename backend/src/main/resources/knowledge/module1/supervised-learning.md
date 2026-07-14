# 监督学习

## 概念介绍

监督学习（Supervised Learning）是机器学习最常见也最成熟的范式。所谓"监督"，就是给计算机"带答案"的数据进行学习：每个样本都有输入 x 和对应的"正确答案" y，模型的任务是学到从 x 到 y 的映射。就像老师带着学生做有标准答案的练习题，每做一题老师都会告诉他对错。

监督学习的两大主任务：
- **分类**：y 是离散类别（猫/狗、是/否、垃圾/正常）。
- **回归**：y 是连续数值（房价、温度、销量）。

## 核心原理

### 基本流程

```
收集有标签数据 → 划分训练/验证/测试集 → 选择模型 → 训练 → 评估 → 部署
```

### 常用算法

| 类别 | 典型算法 | 适用场景 |
|-----|---------|---------|
| 线性模型 | 线性回归、逻辑回归 | 特征近似线性 |
| 树模型 | 决策树、随机森林、XGBoost | 表格数据、特征混合 |
| 支持向量机 | SVM | 小样本、高维 |
| K 近邻 | KNN | 分类边界不规则 |
| 朴素贝叶斯 | GaussianNB / MultinomialNB | 文本分类、垃圾邮件 |
| 神经网络 | MLP、CNN、RNN | 复杂模式（图像/文本/语音） |

每个算法的原理详见 module2~module4。

### 数据要求

- 数据需有标签：分类用 0/1/2... 整数，回归用实数。
- 类别平衡：分类问题建议各类样本数大致相当，否则需重采样或加权。
- 特征工程：处理缺失值、归一化、编码、构造新特征。

## 代码实现

```python
from sklearn.datasets import load_iris, fetch_california_housing
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LogisticRegression, LinearRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, r2_score

# ========== 分类示例 ==========
X_cls, y_cls = load_iris(return_X_y=True)
X_train, X_test, y_train, y_test = train_test_split(X_cls, y_cls, test_size=0.2, random_state=42, stratify=y_cls)

clf = RandomForestClassifier(n_estimators=100, random_state=42)
clf.fit(X_train, y_train)
print(f"[分类] 测试集准确率: {accuracy_score(y_test, clf.predict(X_test)):.4f}")

# ========== 回归示例 ==========
X_reg, y_reg = fetch_california_housing(return_X_y=True)
X_train, X_test, y_train, y_test = train_test_split(X_reg, y_reg, test_size=0.2, random_state=42)

reg = LinearRegression()
reg.fit(X_train, y_train)
print(f"[回归] 测试集 R²: {r2_score(y_test, reg.predict(X_test)):.4f}")
```

## 适用场景

- 数据有标签的预测任务（绝大多数业务问题）
- 需要量化预测置信度的场景（监督模型可输出概率）
- 需要可解释性的场景（线性模型、决策树天然可解释）

## 常见易错点

1. **混淆分类与回归**：逻辑回归是分类（虽然叫"回归"），其输出是 0/1 概率。
2. **标签错了还训练**：训练数据标注错误，再好的模型也学不到正确规律。
3. **类别不平衡不处理**：99% 负类 + 1% 正类，直接训练全预测负类准确率就 99%，但毫无价值。
4. **新数据和训练数据分布不一致**：模型在生产环境崩盘。

## 练习题

1. **选择题**：以下哪个是监督学习任务？（A）把客户分成3类 （B）预测明天的天气温度 （C）新闻按主题聚合 （D）发现异常交易
   - 答案：B。预测温度需要标签（历史的温度数据），是回归任务。

2. **简答题**：监督学习和无监督学习的本质区别是什么？
   - 答案：监督学习的数据有标签（正确答案），模型学习 (x → y) 的映射；无监督学习的数据无标签，模型自己发现数据中的结构（如分组、降维）。

3. **编程题**：分别用一个分类器和一个回归器对 Iris 和 California Housing 数据集训练，比较结果。

## 推荐阅读

- 周志华《机器学习》（西瓜书）第1章
- 李航《统计学习方法》第1章
- Andrew Ng《机器学习》课程第1-2周
- Scikit-learn 监督学习文档：https://scikit-learn.org/stable/supervised_learning.html
