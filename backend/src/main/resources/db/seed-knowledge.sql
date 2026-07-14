-- =====================================================================
-- 知识点元数据初始化数据（机器学习课程）
-- 对应 doc/05-机器学习知识库规划.md
-- =====================================================================

USE Random;

-- 模块1：基础概念
INSERT INTO knowledge_point (module, name, description, content_path, difficulty) VALUES
    (1, '机器学习定义',     'ML定义、与传统编程的区别',                       'module1/ml-definition.md',         'easy'),
    (1, '监督学习',         '分类与回归、标注数据',                           'module1/supervised-learning.md',   'easy'),
    (1, '无监督学习',       '聚类、降维、无标注数据',                         'module1/unsupervised-learning.md', 'easy'),
    (1, '数据集划分',       '训练集/验证集/测试集、划分比例',                 'module1/data-split.md',            'easy'),
    (1, '模型评估指标',     '准确率/精确率/召回率/F1/AUC',                    'module1/evaluation-metrics.md',    'medium'),
    (1, '偏差与方差',       'Bias-Variance Tradeoff',                         'module1/bias-variance.md',         'medium'),
    (1, '过拟合与欠拟合',   '表现、原因、解决方法',                           'module1/overfitting.md',           'medium');

-- 模块2：经典算法
INSERT INTO knowledge_point (module, name, description, content_path, difficulty) VALUES
    (2, '线性回归',         '最小二乘法、梯度下降',                           'module2/linear-regression.md',     'medium'),
    (2, '逻辑回归',         'Sigmoid函数、交叉熵损失',                        'module2/logistic-regression.md',   'medium'),
    (2, '决策树',           '信息增益、基尼系数、剪枝',                       'module2/decision-tree.md',         'medium'),
    (2, '随机森林',         'Bagging思想、特征随机选择',                      'module2/random-forest.md',         'medium'),
    (2, 'SVM',              '最大间隔、核函数、软间隔',                       'module2/svm.md',                   'hard'),
    (2, 'KNN',              '距离度量、K值选择',                              'module2/knn.md',                   'easy'),
    (2, '朴素贝叶斯',       '贝叶斯定理、条件独立假设',                       'module2/naive-bayes.md',           'medium'),
    (2, '集成学习',         'Bagging vs Boosting、XGBoost',                   'module2/ensemble-learning.md',     'hard');

-- 模块3：无监督学习
INSERT INTO knowledge_point (module, name, description, content_path, difficulty) VALUES
    (3, 'K-Means',          '聚类流程、K值选择(肘部法则)',                    'module3/k-means.md',               'medium'),
    (3, '层次聚类',         '凝聚/分裂、树状图',                              'module3/hierarchical-clustering.md', 'medium'),
    (3, 'PCA',              '主成分分析、方差解释率',                         'module3/pca.md',                   'medium'),
    (3, '降维可视化',       't-SNE、UMAP概念',                                'module3/dim-reduction.md',         'hard');

-- 模块4：深度学习基础
INSERT INTO knowledge_point (module, name, description, content_path, difficulty) VALUES
    (4, '神经网络基础',     '感知机、激活函数(Sigmoid/ReLU)、反向传播',         'module4/neural-network.md',       'hard'),
    (4, 'CNN',              '卷积层、池化层、经典结构',                        'module4/cnn.md',                   'hard'),
    (4, 'RNN/LSTM',         '序列建模、遗忘门/输入门',                         'module4/rnn-lstm.md',              'hard'),
    (4, 'Transformer',      'Self-Attention、位置编码',                        'module4/transformer.md',           'hard');

-- 模块5：实践工具
INSERT INTO knowledge_point (module, name, description, content_path, difficulty) VALUES
    (5, 'NumPy',            '数组操作、矩阵运算、广播机制',                    'module5/numpy.md',                 'easy'),
    (5, 'Pandas',           'DataFrame操作、数据清洗、特征工程',               'module5/pandas.md',                'easy'),
    (5, 'Scikit-learn',     'Pipeline、GridSearchCV、cross_val_score',         'module5/sklearn.md',               'medium'),
    (5, 'PyTorch',          'Tensor、Dataset、DataLoader、训练循环',           'module5/pytorch.md',               'hard');

-- 模块6：项目实战
INSERT INTO knowledge_point (module, name, description, content_path, difficulty) VALUES
    (6, '房价预测',         '回归：线性回归+特征工程+评估',                    'module6/house-price.md',           'medium'),
    (6, '手写数字识别',     '分类：CNN+PyTorch',                               'module6/mnist.md',                 'hard'),
    (6, '客户分群',         '聚类：K-Means+PCA+可视化',                        'module6/customer-segmentation.md', 'medium'),
    (6, '情感分析',         'NLP：TF-IDF+逻辑回归/Transformer',                'module6/sentiment-analysis.md',    'hard');