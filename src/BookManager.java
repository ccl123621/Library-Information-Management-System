import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * 主界面类 (Swing GUI)
 * 包含书籍管理面板和系统日志面板
 */
public class BookManager extends JFrame {

    // 书籍表格组件
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private final String[] columnNames = {"ID", "书名 (Title)", "类型 (Type)"};

    // 日志表格组件
    private JTable logTable;
    private DefaultTableModel logModel;
    private final String[] logColumns = {"ID", "时间 (Time)", "操作 (Action)", "详情 (Details)"};

    public BookManager() {
        initUI();
        // 程序启动时自动加载数据
        refreshTableData(null);
        refreshLogs(); // 预加载日志
    }

    /**
     * 初始化主界面 UI 布局
     */
    private void initUI() {
        setTitle("Kindle Book Manager / Kindle 书籍管理");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null); // 屏幕居中

        // 1. 顶部选项卡
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("  书籍管理 (Books)  ", createBookManagerPanel());
        tabbedPane.addTab("  系统日志 (Logs)  ", createLogPanel());

        add(tabbedPane);
    }

    /**
     * 创建书籍管理主面板
     */
    private JPanel createBookManagerPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // --- 工具栏 ---
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JButton btnImport = new JButton("导入索引文件");
        JButton btnAdd = new JButton("添加书籍");
        JButton btnEdit = new JButton("编辑书籍");
        JButton btnDelete = new JButton("删除书籍");
        JButton btnClear = new JButton("清空数据库");
        JButton btnRefresh = new JButton("刷新");

        // 绑定事件
        btnImport.addActionListener(e -> importAction());
        btnAdd.addActionListener(e -> addAction());
        btnEdit.addActionListener(e -> editAction());
        btnDelete.addActionListener(e -> deleteAction());
        btnClear.addActionListener(e -> clearAction());
        btnRefresh.addActionListener(e -> refreshTableData(null));

        toolbar.add(btnImport);
        toolbar.add(btnAdd);
        toolbar.add(btnEdit);
        toolbar.add(btnDelete);
        toolbar.add(btnClear);
        toolbar.add(btnRefresh);

        mainPanel.add(toolbar, BorderLayout.NORTH);

        // --- 内容分割区 ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(900);
        splitPane.setResizeWeight(0.8);

        // 左侧：数据表格
        tableModel = new DefaultTableModel(null, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; } // 禁止双击编辑
        };
        bookTable = new JTable(tableModel);
        bookTable.setRowHeight(25);
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(bookTable);

        // --- 右键菜单配置 ---
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem editMenuItem = new JMenuItem("修改书名");
        JMenuItem deleteMenuItem = new JMenuItem("删除书籍");

        editMenuItem.addActionListener(e -> editAction());
        deleteMenuItem.addActionListener(e -> deleteAction());

        popupMenu.add(editMenuItem);
        popupMenu.add(deleteMenuItem);

        // 鼠标监听器：处理右键点击选中行
        bookTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) { handleRightClick(e); }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) { handleRightClick(e); }

            private void handleRightClick(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = bookTable.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        bookTable.setRowSelectionInterval(row, row);
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
        splitPane.setLeftComponent(scrollPane);

        // 右侧：查询面板
        splitPane.setRightComponent(createSearchPanel());

        mainPanel.add(splitPane, BorderLayout.CENTER);
        return mainPanel;
    }

    /**
     * 创建右侧搜索面板
     */
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("数据查询"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("书名关键字:"), gbc);

        gbc.gridy = 1;
        searchField = new JTextField(15);
        panel.add(searchField, gbc);

        gbc.gridy = 2;
        JButton btnSearch = new JButton("搜索");
        btnSearch.addActionListener(e -> {
            String keyword = searchField.getText().trim();
            refreshTableData(keyword);
            appendLog("Search", "关键字: " + keyword); // 记录搜索日志
        });
        panel.add(btnSearch, gbc);

        // 占位填充
        gbc.gridy = 3; gbc.weighty = 1.0;
        panel.add(new JLabel(""), gbc);

        return panel;
    }

    // ================= 业务逻辑方法 =================

    /**
     * 异步刷新表格数据
     * @param keyword 搜索关键字，若为 null 则查询所有
     */
    private void refreshTableData(String keyword) {
        new SwingWorker<Vector<Vector<Object>>, Void>() {
            @Override
            protected Vector<Vector<Object>> doInBackground() {
                if (keyword == null || keyword.isEmpty()) {
                    return QueryDB.getAllBooks();
                } else {
                    return QueryDB.searchBooks(keyword);
                }
            }
            @Override
            protected void done() {
                try {
                    tableModel.setDataVector(get(), new Vector<>(java.util.Arrays.asList(columnNames)));
                    // 重置列宽
                    bookTable.getColumnModel().getColumn(0).setPreferredWidth(60);
                    bookTable.getColumnModel().getColumn(1).setPreferredWidth(600);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    /**
     * 导入文件操作
     */
    private void importAction() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("文本索引文件 (*.txt)", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // 调用修改后的方法（不会清空旧数据）
                    SetUpDB.importBooksFromFile(file.getAbsolutePath());
                    return null;
                }
                @Override
                protected void done() {
                    JOptionPane.showMessageDialog(null, "索引导入完成！");
                    refreshTableData(null);
                    appendLog("Import", "导入文件: " + file.getName());
                }
            }.execute();
        }
    }

    /**
     * 添加单本书籍操作
     */
    private void addAction() {
        JDialog dialog = new JDialog(this, "添加书籍", true);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("书名 (Path/Title):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField titleField = new JTextField(30);
        formPanel.add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("类型 (Type):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField typeField = new JTextField(30);
        formPanel.add(typeField, gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancel = new JButton("取消");
        JButton btnOK = new JButton("确定");

        btnCancel.addActionListener(e -> dialog.dispose());

        btnOK.addActionListener(e -> {
            String name = titleField.getText();
            String kind = typeField.getText();

            if (name == null || name.trim().isEmpty() || kind == null || kind.trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "书名和类型不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            btnOK.setEnabled(false);

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return SetUpDB.addBookWithExplicitType(name, kind);
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(dialog, "书籍添加成功！");
                            dialog.dispose();
                            refreshTableData(null);
                            appendLog("Add Book", "添加书籍: " + name);
                        } else {
                            JOptionPane.showMessageDialog(dialog, "添加失败。", "错误", JOptionPane.ERROR_MESSAGE);
                            btnOK.setEnabled(true);
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }.execute();
        });

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnOK);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * 编辑书籍操作
     */
    private void editAction() {
        int row = bookTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先选择一行！");
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        String oldName = (String) tableModel.getValueAt(row, 1);

        String newName = JOptionPane.showInputDialog(this, "修改书名:", oldName);
        if (newName != null && !newName.trim().isEmpty() && !newName.equals(oldName)) {
            if (QueryDB.updateBook(id, newName)) {
                refreshTableData(null);
                appendLog("Edit Book", "ID: " + id + " 旧名: " + oldName + " -> 新名: " + newName);
            }
        }
    }

    /**
     * 删除书籍操作
     */
    private void deleteAction() {
        int row = bookTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先选择一行！");
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);

        if (JOptionPane.showConfirmDialog(this, "确定删除该记录？\n" + name) == JOptionPane.YES_OPTION) {
            if (QueryDB.deleteBook(id)) {
                refreshTableData(null);
                appendLog("Delete Book", "删除ID: " + id + " 书名: " + name);
            }
        }
    }

    /**
     * 清空数据库操作
     */
    private void clearAction() {
        if (JOptionPane.showConfirmDialog(this, "警告：确定清空所有数据？此操作不可恢复！", "警告", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            QueryDB.clearAllBooks();
            refreshTableData(null);
            appendLog("Clear DB", "清空所有书籍数据");
        }
    }

    // ================= 日志系统实现 =================

    /**
     * 创建日志面板
     */
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton btnRefreshLog = new JButton("刷新日志");
        JButton btnDeleteLog = new JButton("删除选中日志");
        JButton btnClearLog = new JButton("清空所有日志");

        toolbar.add(btnRefreshLog);
        toolbar.add(btnDeleteLog);
        toolbar.add(btnClearLog);
        panel.add(toolbar, BorderLayout.NORTH);

        logModel = new DefaultTableModel(null, logColumns) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        logTable = new JTable(logModel);
        logTable.setRowHeight(25);

        logTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(500);

        JScrollPane scrollPane = new JScrollPane(logTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 按钮逻辑
        btnRefreshLog.addActionListener(e -> refreshLogs());

        btnDeleteLog.addActionListener(e -> {
            int row = logTable.getSelectedRow();
            if (row != -1) {
                int id = (int) logModel.getValueAt(row, 0);
                QueryDB.deleteLog(id);
                refreshLogs();
            }
        });

        btnClearLog.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "确定清空所有日志？") == JOptionPane.YES_OPTION) {
                QueryDB.clearLogs();
                refreshLogs();
            }
        });

        return panel;
    }

    /**
     * 写入日志并自动刷新显示
     */
    private void appendLog(String action, String details) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                QueryDB.addLog(time, action, details);
                return null;
            }
            @Override
            protected void done() {
                refreshLogs();
            }
        }.execute();
    }

    /**
     * 刷新日志表格数据
     */
    private void refreshLogs() {
        new SwingWorker<Vector<Vector<Object>>, Void>() {
            @Override
            protected Vector<Vector<Object>> doInBackground() {
                return QueryDB.getAllLogs();
            }
            @Override
            protected void done() {
                try {
                    logModel.setDataVector(get(), new Vector<>(java.util.Arrays.asList(logColumns)));
                    // 刷新数据后列宽会重置，需重新设置
                    logTable.getColumnModel().getColumn(0).setPreferredWidth(50);
                    logTable.getColumnModel().getColumn(1).setPreferredWidth(150);
                    logTable.getColumnModel().getColumn(2).setPreferredWidth(100);
                    logTable.getColumnModel().getColumn(3).setPreferredWidth(500);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }.execute();
    }

    public static void main(String[] args) {
        try {
            // 设置系统原生风格 (Windows下会更好看)
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { e.printStackTrace(); }

        // 在安全的EDT线程中启动窗口
        SwingUtilities.invokeLater(() -> new BookManager().setVisible(true));
    }
}