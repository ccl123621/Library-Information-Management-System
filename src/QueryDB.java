import java.sql.*;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库查询与操作工具类
 * 负责处理书籍和日志的 CRUD 操作
 */
public class QueryDB {
    private static final String URL = "jdbc:sqlite:kindlebooks.db";
    // 复用正则逻辑，用于编辑时自动更新类型
    private static final Pattern SUBNAME = Pattern.compile("\\.(pdf|mobi|epub|azw3|html|txt)$");

    static {
        // 确保驱动加载
        try { Class.forName("org.sqlite.JDBC"); } catch (ClassNotFoundException e) { e.printStackTrace(); }
    }

    // ================= 书籍管理相关方法 =================

    /**
     * 获取所有书籍数据
     * @return Vector<Vector<Object>> 包含 id, name, kind 的二维向量，直接用于 TableModel
     */
    public static Vector<Vector<Object>> getAllBooks() {
        // 确保表存在，防止第一次运行报错
        SetUpDB.initTables();

        Vector<Vector<Object>> data = new Vector<>();
        String sql = "SELECT id, name, kind FROM books ORDER BY id DESC"; // 倒序排列，新书在前
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getString("name"));
                row.add(rs.getString("kind"));
                data.add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return data;
    }

    /**
     * 根据书名进行模糊搜索
     * @param keyword 搜索关键字
     * @return 符合条件的数据集
     */
    public static Vector<Vector<Object>> searchBooks(String keyword) {
        Vector<Vector<Object>> data = new Vector<>();
        // SQLite 使用 || 进行字符串拼接
        String sql = "SELECT id, name, kind FROM books WHERE name LIKE '%' || ? || '%'";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, keyword);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("name"));
                    row.add(rs.getString("kind"));
                    data.add(row);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return data;
    }

    /**
     * 更新书籍信息
     * @param id 书籍ID
     * @param newName 新的书名
     * @return boolean 更新是否成功
     */
    public static boolean updateBook(int id, String newName) {
        // 自动根据新名字提取后缀
        String newKind = "unknown";
        Matcher m = SUBNAME.matcher(newName);
        if (m.find()) { newKind = m.group(1); }

        String sql = "UPDATE books SET name = ?, kind = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setString(2, newKind);
            pstmt.setInt(3, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * 删除单条书籍记录
     * @param id 书籍ID
     * @return boolean 删除是否成功
     */
    public static boolean deleteBook(int id) {
        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * 清空整个书籍表
     */
    public static void clearAllBooks() {
        String sql = "DELETE FROM books";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            // 可选：重置自增ID
            stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name='books'");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ================= 日志管理相关方法 (新增功能) =================

    /**
     * 记录操作日志
     * @param time 操作时间
     * @param action 操作类型（如 Import, Add, Delete）
     * @param details 详情
     */
    public static void addLog(String time, String action, String details) {
        // 确保表存在
        SetUpDB.initTables();

        String sql = "INSERT INTO logs(log_time, action, details) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, time);
            pstmt.setString(2, action);
            pstmt.setString(3, details);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /**
     * 获取所有日志记录
     * @return 用于表格显示的日志数据
     */
    public static Vector<Vector<Object>> getAllLogs() {
        SetUpDB.initTables();
        Vector<Vector<Object>> data = new Vector<>();
        String sql = "SELECT id, log_time, action, details FROM logs ORDER BY id DESC";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getString("log_time"));
                row.add(rs.getString("action"));
                row.add(rs.getString("details"));
                data.add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return data;
    }

    /**
     * 删除单条日志
     * @param id 日志ID
     */
    public static void deleteLog(int id) {
        String sql = "DELETE FROM logs WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /**
     * 清空所有日志
     */
    public static void clearLogs() {
        String sql = "DELETE FROM logs";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) { e.printStackTrace(); }
    }
}