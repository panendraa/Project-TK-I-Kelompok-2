import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;

public class MainApp extends JFrame {

    private JTable productTable;
    private DefaultTableModel tableModel;

    public MainApp() {
        setTitle("SimpleStore - Manajemen Produk");
        setSize(1000, 600); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Tabel untuk menampilkan data produk (Read) ---
        String[] columnNames = {"ID", "Nama Produk", "Harga", "Kategori"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) return Double.class; 
                return Object.class;
            }
        };
        productTable = new JTable(tableModel);
        
        DecimalFormat currencyFormat = new DecimalFormat("#,##0");
        productTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Double) {
                    value = currencyFormat.format(value);
                }
                setHorizontalAlignment(SwingConstants.RIGHT); 
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        add(new JScrollPane(productTable), BorderLayout.CENTER);

        // Panel untuk tombol-tombol 
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        // Tombol Tambah Produk
        JButton btnTambah = new JButton("Tambah Produk");
        btnTambah.addActionListener(e -> {
            FormProduk form = new FormProduk(MainApp.this, true, null);
            form.setVisible(true);
        });
        buttonPanel.add(btnTambah);

        // Tombol Edit Produk
        JButton btnEdit = new JButton("Edit Produk");
        btnEdit.addActionListener(e -> {
            int selectedRow = productTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(MainApp.this, "Pilih produk!");
                return;
            }
            int id = (int) tableModel.getValueAt(selectedRow, 0);
            String nama = (String) tableModel.getValueAt(selectedRow, 1);
            double harga = (double) tableModel.getValueAt(selectedRow, 2);
            Produk produk = new Produk(id, nama, harga);

            FormProduk form = new FormProduk(MainApp.this, true, produk);
            form.setVisible(true);
        });
        buttonPanel.add(btnEdit);

        // Tombol Hapus Produk
        JButton btnHapus = new JButton("Hapus Produk");
        btnHapus.addActionListener(e -> {
            int selectedRow = productTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(MainApp.this, "Pilih produk!");
                return;
            }
            int id = (int) tableModel.getValueAt(selectedRow, 0);
            String nama = (String) tableModel.getValueAt(selectedRow, 1);
            int confirm = JOptionPane.showConfirmDialog(MainApp.this,
                "Yakin ingin menghapus produk: " + nama + "?", 
                "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                deleteProduk(id);
            }
        });
        buttonPanel.add(btnHapus);

        // Tombol Kelola Kategori
        JButton btnKategori = new JButton("Kelola Kategori");
        btnKategori.addActionListener(e -> {
            FormKategori formKategori = new FormKategori(MainApp.this, true);
            formKategori.setVisible(true);
        });
        buttonPanel.add(btnKategori);

        // Tombol Atur Relasi
        JButton btnRelasi = new JButton("Atur Kategori Produk");
        btnRelasi.addActionListener(e -> {
            int selectedRow = productTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(MainApp.this, "Pilih produk!");
                return;
            }
            int idProduk = (int) tableModel.getValueAt(selectedRow, 0);
            String namaProduk = (String) tableModel.getValueAt(selectedRow, 1);

            FormRelasi formRelasi = new FormRelasi(MainApp.this, true, idProduk, namaProduk);
            formRelasi.setVisible(true);
        });
        buttonPanel.add(btnRelasi);

        add(buttonPanel, BorderLayout.SOUTH);
        loadProduk();
    }

     // Memuat data produk dengan list kategori
    public void loadProduk() { 
        tableModel.setRowCount(0); 
        
        // Query untuk mengambil nama kategori dan menggabungkannya
        String sql = "SELECT p.id_produk, p.nama_produk, p.harga, " +
                     "GROUP_CONCAT(k.nama_kategori SEPARATOR ', ') AS list_kategori " +
                     "FROM tabel_produk p " +
                     "LEFT JOIN tabel_produk_kategori pk ON p.id_produk = pk.id_produk_fk " +
                     "LEFT JOIN tabel_kategori k ON pk.id_kategori_fk = k.id_kategori " +
                     "GROUP BY p.id_produk " + 
                     "ORDER BY p.id_produk";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id_produk");
                String nama = rs.getString("nama_produk");
                double harga = rs.getDouble("harga");
                
                String kategori = rs.getString("list_kategori");
                if (kategori == null) kategori = "-"; 
                
                // Masukkan 4 kolom data: ID, Nama, Harga, Kategori
                tableModel.addRow(new Object[]{id, nama, harga, kategori});
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat data produk: " + e.getMessage());
        }
    }
