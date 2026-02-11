import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库初始化与数据导入工具类
 */
public class SetUpDB {
    // 匹配以 ./ 或 / 开头的行（通常是 tree 命令生成的路径）
    private static final Pattern TITLE = Pattern.compile("^[./].*");
    // 匹配常见电子书后缀
    private static final Pattern SUBNAME = Pattern.compile("\\.(pdf|mobi|epub|azw3|html|txt)$");
    // 数据库连接地址
    private static final String DB_URL = "jdbc:sqlite:kindlebooks.db";

    static {
        // 静态代码块：加载驱动（只需执行一次）
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化数据库表结构（如果不存在则创建）
     * 包含 books 表和 logs 表
     */
    public static void initTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // 1. 创建书籍表
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS books (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name VARCHAR(256), " +
                    "kind VARCHAR(16))");

            // 2. 创建日志表 (修复功能缺失)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "log_time TEXT, " +
                    "action TEXT, " +
                    "details TEXT)");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从文本文件批量导入书籍数据到数据库
     * @param filename 包含文件索引的文本文件路径
     * @throws IOException 文件读取异常
     */
    public static void importBooksFromFile(String filename) throws IOException {
        // 确保表存在
        initTables();

        String title = "";
        Vector<String> fullNames = new Vector<>();
        Vector<String> kinds = new Vector<>();

        // 1. 读取文件并解析
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filename), StandardCharsets.UTF_8)) {
            String line = br.readLine();
            while (line != null) {
                // 如果是目录行，更新当前目录上下文
                if (TITLE.matcher(line).find()) {
                    title = line;
                }
                // 如果是文件行，提取后缀并组合完整路径
                Matcher m = SUBNAME.matcher(line);
                if (m.find()) {
                    kinds.add(m.group(1)); // 后缀名
                    fullNames.add(title + line); // 完整路径
                }
                line = br.readLine();
            }
        }

        System.out.println("解析到书籍数量: " + fullNames.size());

        // 2. 批量插入数据库
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false); // 开启事务

            String sql = "INSERT INTO books(name, kind) VALUES(?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < fullNames.size(); i++) {
                    stmt.setString(1, fullNames.get(i));
                    stmt.setString(2, kinds.get(i));
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit(); // 提交事务
                System.out.println("数据批量添加成功");
            } catch (SQLException e) {
                conn.rollback(); // 发生错误回滚
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据输入的完整书名（或路径）添加单条记录（自动识别类型）
     * @param fullBookName 输入的字符串，例如 "kindle书库/入门指南.azw3"
     * @return boolean 是否添加成功
     */
    public static boolean addSingleBook(String fullBookName) {
        if (fullBookName == null || fullBookName.trim().isEmpty()) {
            return false;
        }

        // 自动提取后缀名
        String kind = "unknown";
        Matcher m = SUBNAME.matcher(fullBookName);
        if (m.find()) {
            kind = m.group(1);
        }

        return addBookWithExplicitType(fullBookName, kind);
    }

    /**
     * 添加带有明确类型的书籍记录
     * @param name 完整的书名或路径
     * @param kind 书籍的类型（后缀名）
     * @return 是否添加成功
     */
    public static boolean addBookWithExplicitType(String name, String kind) {
        initTables(); // 确保表存在

        if (name == null || name.trim().isEmpty() || kind == null || kind.trim().isEmpty()) {
            return false;
        }

        String sql = "INSERT INTO books(name, kind) VALUES(?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name.trim());
            pstmt.setString(2, kind.trim());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}