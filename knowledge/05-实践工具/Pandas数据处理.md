# Pandas数据处理

## 概念介绍

Pandas是Python数据分析的核心库，提供了DataFrame和Series两种数据结构，专门处理表格型数据（类似Excel）。在机器学习项目中，Pandas用于数据加载、清洗、探索性分析和特征工程，是数据预处理的主力工具。

## 核心原理

### 两种核心数据结构

- **Series**：一维带标签数组（类似一列）
- **DataFrame**：二维带标签表格（类似Excel表），有行索引和列索引

### 数据处理流程

```
读取数据 → 查看概况 → 处理缺失值 → 特征工程 → 数据转换 → 输出
```

## 代码实现

```python
import pandas as pd
import numpy as np

# ========== 创建DataFrame ==========
df = pd.DataFrame({
    '姓名': ['张三', '李四', '王五', '赵六'],
    '年龄': [20, 21, 19, 22],
    '成绩': [85, 92, 78, 95],
    '性别': ['男', '女', '男', '女']
})

print(df.head())           # 查看前5行
print(df.info())           # 查看数据类型和缺失值
print(df.describe())       # 查看统计摘要

# ========== 数据读写 ==========
# df = pd.read_csv('data.csv')      # 读取CSV
# df = pd.read_excel('data.xlsx')   # 读取Excel
# df.to_csv('output.csv', index=False)  # 保存CSV

# ========== 索引和选择 ==========
print(df['年龄'])             # 选择一列
print(df[['姓名', '成绩']])    # 选择多列
print(df[df['成绩'] > 80])    # 条件筛选
print(df.iloc[0:2])           # 按行号切片
print(df.loc[0:2, ['姓名','成绩']])  # 按标签切片

# ========== 缺失值处理 ==========
df2 = df.copy()
df2.loc[1, '成绩'] = np.nan
print(df2.isnull().sum())          # 统计缺失值
df2['成绩'].fillna(df2['成绩'].mean(), inplace=True)  # 均值填充
df2.dropna(inplace=True)           # 删除含缺失值的行

# ========== 分组聚合 ==========
print(df.groupby('性别')['成绩'].mean())   # 按性别分组求平均成绩

# ========== 特征工程 ==========
# 独热编码（One-Hot Encoding）
df_encoded = pd.get_dummies(df, columns=['性别'])
print(df_encoded)

# 添加新特征
df['是否优秀'] = (df['成绩'] >= 90).astype(int)
print(df)
```

## 适用场景

- 数据加载和清洗（CSV、Excel、数据库）
- 探索性数据分析（EDA）
- 特征工程（缺失值处理、编码、特征构造）
- 数据可视化（配合Matplotlib）

## 常见易错点

1. **SettingWithCopyWarning**：在切片上赋值会报警告，用.copy()或.loc
2. **索引混乱**：重置索引用df.reset_index(drop=True)
3. **数据类型不对**：用df.dtypes查看，用pd.to_numeric()转换
4. **链式索引**：df[df['a']>5]['b'] = 1 会报警告，改用df.loc

## 练习题

1. 读取一个CSV文件，统计每列的缺失值数量
2. 对分类特征做独热编码
3. 按某个特征分组，计算每组的统计指标

## 推荐阅读

- Pandas官方10分钟入门：https://pandas.pydata.org/docs/user_guide/10min.html
- 《利用Python进行数据分析》第5-8章
