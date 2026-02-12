# 图书管理系统

## 项目分析
这是一个 **Kindle 书籍管理系统**，使用 Java Swing 开发的桌面应用程序。

### 项目结构
- **BookManager.java** - 主界面类，包含完整的 GUI 界面
- **QueryDB.java** - 数据库操作类，处理书籍和日志的 CRUD 操作
- **SetUpDB.java** - 数据库初始化和数据导入类
- **lib/** - 依赖库，包含 SQLite JDBC 驱动
- **kindlebooks.db** - SQLite 数据库文件
- **kindlebooks_index.txt** - 书籍索引文件

### 主要功能
1. **书籍管理** - 添加、删除、搜索、编辑书籍（完整的 CRUD 操作）
2. **批量导入** - 从文本文件导入书籍索引
3. **日志系统** - 记录所有操作历史
4. **数据持久化** - 使用 SQLite 数据库存储

### 技术栈
- Java Swing (GUI)
- SQLite (数据库)
- JDBC (数据库连接)