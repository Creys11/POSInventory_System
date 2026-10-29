import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class MistySwirlPOS extends JFrame {


    private final Color pink = new Color(222, 105, 137);
    private final Color lightPink = new Color(255, 235, 241);
    private final Color darkPink = new Color(116, 63, 82);


    private final String role;
    private final Map<String, Integer> cart = new LinkedHashMap<>();
    private final Map<String, Integer> prices = new LinkedHashMap<>();
    private final Map<String, Integer> stocks = new LinkedHashMap<>();
    private final Map<String, JLabel> stockLabels = new LinkedHashMap<>();


    private final JPanel orderItems;
    private final JLabel totalLabel;
    private JPanel productsPanel;
    private JLabel menuTitleLabel;


    public MistySwirlPOS(String role) {
        this.role = role;


        setTitle("Misty Swirl - POS and Inventory System");
        setSize(1200, 750);
        setMinimumSize(new Dimension(950, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());


        // LOAD MENU, PRICES AND STOCK FROM THE DATABASE
        try {
            DBConnection.loadProducts(prices, stocks);
        } catch (SQLException ex) {
            showDbError(null, "Could not load the menu from the database.", ex);
        }


        // HEADER
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(pink);
        header.setBorder(new EmptyBorder(18, 24, 18, 24));


        JLabel brand = new JLabel(
                "<html><b>MISTY SWIRL</b><br>"
                + "POS AND INVENTORY SYSTEM</html>");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.add(brand, BorderLayout.WEST);


        JLabel userLabel = new JLabel(role + " ACCOUNT");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));


        JButton logoutButton = new JButton("Log Out");
        styleButton(logoutButton, Color.WHITE, darkPink);
        logoutButton.addActionListener(e -> {
            dispose();
            showLogin();
        });


        JPanel userPanel = new JPanel(new FlowLayout());
        userPanel.setOpaque(false);
        userPanel.add(userLabel);
        userPanel.add(logoutButton);
        header.add(userPanel, BorderLayout.EAST);


        add(header, BorderLayout.NORTH);


        // SIDEBAR: navigation and separate menu-category buttons
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(lightPink);
        sidebar.setBorder(new EmptyBorder(12, 10, 12, 10));


        addMenuButton(sidebar, "All Products", () -> filterProducts("All Products"));
        addMenuButton(sidebar, "Completed Orders", this::showCompletedOrders);
        addMenuButton(sidebar, "Inventory", this::showInventory);


        JLabel categoryHeading = new JLabel(" MENU CATEGORIES");
        categoryHeading.setFont(new Font("Segoe UI", Font.BOLD, 12));
        categoryHeading.setForeground(darkPink);
        categoryHeading.setBorder(new EmptyBorder(14, 5, 5, 5));
        categoryHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(categoryHeading);


        String[] categories = {
            "Frozen Yogurt",
            "Sundae Flavors",
            "Best Seller",
            "Sauces",
            "Add Ons",
            "Misty Premium",
            "Soda Floats",
            "Ice Cream Cone",
            "Belgian Cone",
            "Misty Premium Coffee Float Series",
            "Pricing for Coffee Float Series",
            "Oreo Float Series",
            "Snack Mini Donuts",
            "Nachos",
            "Other Sundaes"
        };


        for (String category : categories) {
            addMenuButton(sidebar, category, () -> filterProducts(category));
        }


        JScrollPane sidebarScroll = new JScrollPane(sidebar);
        sidebarScroll.setPreferredSize(new Dimension(225, 0));
        sidebarScroll.setBorder(BorderFactory.createEmptyBorder());
        sidebarScroll.getVerticalScrollBar().setUnitIncrement(16);
        sidebarScroll.getViewport().setBackground(lightPink);
        add(sidebarScroll, BorderLayout.WEST);


        // POS-style bottom utility panel
        JPanel utilityPanel = new JPanel(new BorderLayout());
        utilityPanel.setBackground(new Color(255, 248, 250));
        utilityPanel.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(238, 190, 207)),
                new EmptyBorder(8, 16, 8, 16)));
        JLabel utilityLabel = new JLabel("Misty Swirl POS  |  " + role + " MODE");
        utilityLabel.setForeground(darkPink);
        utilityLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        utilityPanel.add(utilityLabel, BorderLayout.WEST);
        JButton settingsButton = new JButton("⚙ Settings");
        styleButton(settingsButton, Color.WHITE, darkPink);
        settingsButton.addActionListener(e -> {
            if ("ADMIN".equals(role)) showAdminSettings();
            else JOptionPane.showMessageDialog(this, "Settings are available to the admin only.",
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
        });
        utilityPanel.add(settingsButton, BorderLayout.EAST);
        add(utilityPanel, BorderLayout.SOUTH);


        // PRODUCT MENU
        JPanel center = new JPanel(new BorderLayout(10, 15));
        center.setBackground(Color.WHITE);
        center.setBorder(new EmptyBorder(24, 22, 24, 22));


        menuTitleLabel = new JLabel("Our Ice Cream Menu");
        menuTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        menuTitleLabel.setForeground(new Color(82, 45, 61));
        center.add(menuTitleLabel, BorderLayout.NORTH);


        // Product cards are refreshed when a category button is selected.
        productsPanel = new JPanel(new GridLayout(0, 3, 12, 12));
        productsPanel.setBackground(Color.WHITE);
        showProductsInCategory("All Products");


        JScrollPane productScroll = new JScrollPane(productsPanel);
        productScroll.setBorder(BorderFactory.createEmptyBorder());
        productScroll.getVerticalScrollBar().setUnitIncrement(16);
        productScroll.getViewport().setBackground(Color.WHITE);
        center.add(productScroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);


        // MY ORDER PANEL
        JPanel orderPanel = new JPanel(new BorderLayout(8, 12));
        orderPanel.setBackground(new Color(255, 248, 250));
        orderPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 1, 0, 0, pink),
                new EmptyBorder(22, 18, 22, 18)));
        orderPanel.setPreferredSize(new Dimension(310, 0));


        JLabel orderTitle = new JLabel("My Order");
        orderTitle.setFont(new Font("Segoe UI", Font.BOLD, 23));
        orderTitle.setForeground(new Color(82, 45, 61));
        orderPanel.add(orderTitle, BorderLayout.NORTH);


        orderItems = new JPanel();
        orderItems.setLayout(
                new BoxLayout(orderItems, BoxLayout.Y_AXIS));
        orderItems.setBackground(new Color(255, 248, 250));


        JScrollPane orderScroll = new JScrollPane(orderItems);
        orderScroll.setBorder(BorderFactory.createEmptyBorder());
        orderScroll.getViewport().setBackground(
                new Color(255, 248, 250));
        orderPanel.add(orderScroll, BorderLayout.CENTER);


        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(new Color(255, 248, 250));


        totalLabel = new JLabel("Total: ₱0.00");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 21));
        totalLabel.setForeground(pink);


        JButton clearButton = new JButton("Clear Order");
        styleButton(clearButton, Color.WHITE, darkPink);
        clearButton.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 40));


        clearButton.addActionListener(e -> {
            cart.clear();
            refreshOrder();
        });


        JButton checkoutButton = new JButton("Checkout");
        styleButton(checkoutButton, pink, Color.WHITE);
        checkoutButton.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 42));
        checkoutButton.addActionListener(e -> checkout());


        clearButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkoutButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);


        bottom.add(new JSeparator());
        bottom.add(Box.createVerticalStrut(12));
        bottom.add(totalLabel);
        bottom.add(Box.createVerticalStrut(12));
        bottom.add(clearButton);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(checkoutButton);


        orderPanel.add(bottom, BorderLayout.SOUTH);
        add(orderPanel, BorderLayout.EAST);


        refreshOrder();
        setVisible(true);
    }


    // BUTTON DESIGN
    private void styleButton(JButton button,
                             Color background,
                             Color foreground) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new CompoundBorder(
                new LineBorder(background.equals(Color.WHITE)
                        ? new Color(238, 205, 216) : background,
                        1, true),
                new EmptyBorder(9, 14, 9, 14)));
    }


    // SIDEBAR BUTTONS
    private void addMenuButton(JPanel panel,
                               String text,
                               Runnable action) {
        JButton button = new JButton(text);


        styleButton(button, Color.WHITE, darkPink);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(new CompoundBorder(
                new LineBorder(new Color(245, 210, 222), 1, true),
                new EmptyBorder(11, 14, 11, 10)));


        button.addActionListener(e -> action.run());
        panel.add(button);
    }


    // FILTER THE PRODUCT MENU BY THE SELECTED SIDEBAR CATEGORY
    private void filterProducts(String category) {
        showProductsInCategory(category);
    }


    private void showProductsInCategory(String category) {
        if (productsPanel == null) return;


        productsPanel.removeAll();
        for (String name : prices.keySet()) {
            if (matchesCategory(name, category)) {
                productsPanel.add(createProductCard(name, iconForProduct(name)));
            }
        }


        if (productsPanel.getComponentCount() == 0) {
            productsPanel.add(new JLabel("No products found in this category."));
        }


        menuTitleLabel.setText("All Products".equals(category)
                ? "Our Ice Cream Menu" : category);
        productsPanel.revalidate();
        productsPanel.repaint();
    }


    private boolean matchesCategory(String name, String category) {
        if ("All Products".equals(category)) return true;


        switch (category) {
            case "Frozen Yogurt":
                return name.startsWith("Frozen Yogurt -");
            case "Sundae Flavors":
                return name.startsWith("Sundae -");
            case "Best Seller":
                return name.startsWith("Yogurt Overload Sundae");
            case "Sauces":
                return name.startsWith("Sauce -");
            case "Add Ons":
                return name.startsWith("Add-on -");
            case "Misty Premium":
                return name.startsWith("Misty Premium -");
            case "Soda Floats":
                return name.startsWith("Soda Float -");
            case "Ice Cream Cone":
                return name.startsWith("Ice Cream Cone -");
            case "Belgian Cone":
                return name.startsWith("Belgian Cone -");
            case "Misty Premium Coffee Float Series":
            case "Pricing for Coffee Float Series":
                return name.startsWith("Coffee Float Series -");
            case "Oreo Float Series":
                return name.startsWith("Oreo Float Series -");
            case "Snack Mini Donuts":
                return name.startsWith("Mini Donuts -");
            case "Nachos":
                return name.startsWith("Nachos -");
            case "Other Sundaes":
                return name.startsWith("Other Sundae -");
            default:
                return false;
        }
    }


    // PICK AN EMOJI FOR EACH PRODUCT CATEGORY
    private String iconForProduct(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("yogurt")) return "🍦";
        if (lower.contains("sundae")) return "🍨";
        if (lower.contains("sauce")) return "🍯";
        if (lower.contains("add-on")) return "🍪";
        if (lower.contains("cone")) return "🍦";
        if (lower.contains("coffee")) return "☕";
        if (lower.contains("oreo")) return "🥤";
        if (lower.contains("float") || lower.contains("soda")) return "🥤";
        if (lower.contains("donut")) return "🍩";
        if (lower.contains("nachos")) return "🧀";
        return "🍨";
    }


    // PRODUCT CARDS
    private JPanel createProductCard(String name, String icon) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(255, 250, 252));
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(244, 211, 222), 1, true),
                new EmptyBorder(16, 10, 16, 10)));


        JLabel image = new JLabel(icon);
        image.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 38));
        image.setAlignmentX(Component.CENTER_ALIGNMENT);


        JLabel nameLabel = new JLabel("<html><div style='text-align:center;width:170px;'>"
                + name + "</div></html>");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        JLabel priceLabel =
                new JLabel("₱" + prices.get(name) + ".00");
        priceLabel.setForeground(pink);
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        JLabel stockLabel =
                new JLabel("Stock: " + stocks.get(name));
        stockLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        stockLabels.put(name, stockLabel);


        JButton addButton = new JButton("Add to Order");
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        styleButton(addButton, pink, Color.WHITE);
        addButton.setMaximumSize(new Dimension(170, 38));


        addButton.addActionListener(e -> {
            int qty = cart.getOrDefault(name, 0);


            if (qty >= stocks.get(name)) {
                JOptionPane.showMessageDialog(this,
                        "Not enough stock available!");
                return;
            }


            cart.put(name, qty + 1);
            refreshOrder();
        });


        card.add(image);
        card.add(Box.createVerticalStrut(6));
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(priceLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(stockLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(addButton);


        return card;
    }


    // REFRESH CART AND TOTAL
    private void refreshOrder() {
        orderItems.removeAll();
        double total = 0;


        if (cart.isEmpty()) {
            JLabel emptyLabel = new JLabel("Your order is empty.");
            emptyLabel.setBorder(new EmptyBorder(10, 5, 10, 5));
            orderItems.add(emptyLabel);
        }


        for (String name : cart.keySet()) {
            int qty = cart.get(name);
            int subtotal = prices.get(name) * qty;
            total += subtotal;


            JPanel item = new JPanel(new BorderLayout(5, 5));
            item.setBackground(Color.WHITE);
            item.setBorder(new CompoundBorder(
                    new LineBorder(new Color(245, 220, 229)),
                    new EmptyBorder(8, 5, 8, 5)));


            JLabel details = new JLabel(
                    "<html><b>" + name + "</b><br>"
                    + "₱" + prices.get(name) + " x " + qty
                    + "<br>Subtotal: ₱" + subtotal
                    + ".00</html>");


            JPanel buttons =
                    new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttons.setBackground(Color.WHITE);


            JButton minus = new JButton("-");
            JButton plus = new JButton("+");


            styleButton(minus, lightPink, darkPink);
            styleButton(plus, pink, Color.WHITE);


            minus.setPreferredSize(new Dimension(42, 30));
            plus.setPreferredSize(new Dimension(42, 30));


            minus.addActionListener(e -> {
                if (cart.get(name) <= 1) {
                    cart.remove(name);
                } else {
                    cart.put(name, cart.get(name) - 1);
                }


                refreshOrder();
            });


            plus.addActionListener(e -> {
                if (cart.get(name) < stocks.get(name)) {
                    cart.put(name, cart.get(name) + 1);
                    refreshOrder();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Maximum available stock reached.");
                }
            });


            buttons.add(minus);
            buttons.add(plus);


            item.add(details, BorderLayout.CENTER);
            item.add(buttons, BorderLayout.SOUTH);


            orderItems.add(item);
            orderItems.add(Box.createVerticalStrut(5));
        }


        totalLabel.setText(String.format("Total: ₱%.2f", total));
        orderItems.revalidate();
        orderItems.repaint();
    }


    // ADMIN-ONLY ACCOUNT SETTINGS
    private void showAdminSettings() {
        if (!"ADMIN".equals(role)) {
            JOptionPane.showMessageDialog(this, "Admin access only.",
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }


        String[] options = {"Change Admin Login", "Manage Staff Login", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "Choose which account to manage.", "Admin Settings",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);


        if (choice == 0) {
            changeAccountCredentials(true);
        } else if (choice == 1) {
            changeAccountCredentials(false);
        }
    }


    private void changeAccountCredentials(boolean adminAccount) {
        if (!"ADMIN".equals(role)) {
            JOptionPane.showMessageDialog(this, "Admin access only.",
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }


        JTextField usernameField = new JTextField(
                safeUsername(adminAccount ? "ADMIN" : "STAFF"));
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmField = new JPasswordField();


        Object[] fields = {
            "New username:", usernameField,
            "New password:", passwordField,
            "Confirm password:", confirmField
        };


        int result = JOptionPane.showConfirmDialog(this, fields,
                adminAccount ? "Change Admin Login" : "Manage Staff Login",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);


        if (result != JOptionPane.OK_OPTION) return;


        String newUsername = usernameField.getText().trim();
        String newPassword = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmField.getPassword());


        if (newUsername.isEmpty() || newPassword.trim().isEmpty()
                || confirmPassword.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Username and password cannot be empty.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }


        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                    "The passwords do not match.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }


        String otherUsername = safeUsername(adminAccount ? "STAFF" : "ADMIN");
        if (newUsername.equals(otherUsername)) {
            JOptionPane.showMessageDialog(this,
                    "Admin and staff usernames must be different.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }


        try {
            DBConnection.updateCredentials(adminAccount ? "ADMIN" : "STAFF",
                    newUsername, newPassword);
        } catch (SQLException ex) {
            showDbError(this, "Could not update the login in the database.", ex);
            return;
        }


        JOptionPane.showMessageDialog(this,
                (adminAccount ? "Admin" : "Staff")
                + " login credentials updated successfully. "
                + "The new credentials will be used for the next login.",
                "Settings Updated", JOptionPane.INFORMATION_MESSAGE);
    }


    // INVENTORY: ADMIN can edit stock; STAFF can only view it.
    private void showInventory() {
        boolean adminCanEdit = "ADMIN".equals(role);


        String[] columns = {"Product", "Price (₱)", "Stock"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Only the Stock column can be edited, and only by ADMIN.
                return adminCanEdit && column == 2;
            }


            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? String.class : Integer.class;
            }
        };


        for (String name : prices.keySet()) {
            model.addRow(new Object[]{name, prices.get(name), stocks.get(name)});
        }


        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(lightPink);
        table.getTableHeader().setForeground(darkPink);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(270);
        table.getColumnModel().getColumn(1).setPreferredWidth(85);
        table.getColumnModel().getColumn(2).setPreferredWidth(75);


        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(520, 420));


        if (!adminCanEdit) {
            JOptionPane.showMessageDialog(this, scrollPane,
                    "Inventory - Staff View (Read Only)", JOptionPane.INFORMATION_MESSAGE);
            return;
        }


        JButton saveButton = new JButton("Save Stock Changes");
        styleButton(saveButton, pink, Color.WHITE);
        JButton cancelButton = new JButton("Cancel");
        styleButton(cancelButton, Color.WHITE, darkPink);


        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(saveButton);


        JDialog dialog = new JDialog(this, "Admin - Edit Inventory", true);
        dialog.setLayout(new BorderLayout(8, 8));
        JLabel instruction = new JLabel(
                "  Edit the Stock column, then click Save Stock Changes.");
        instruction.setBorder(new EmptyBorder(10, 5, 2, 5));
        dialog.add(instruction, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(560, 480));
        dialog.setLocationRelativeTo(this);


        saveButton.addActionListener(e -> {
            if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
                return;
            }


            // Validate all stock values before applying any changes.
            for (int row = 0; row < model.getRowCount(); row++) {
                Object value = model.getValueAt(row, 2);
                int stockValue;
                try {
                    stockValue = Integer.parseInt(String.valueOf(value).trim());
                    if (stockValue < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog,
                            "Stock must be a whole number (0 or greater) for: "
                            + model.getValueAt(row, 0),
                            "Invalid Stock", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }


            Map<String, Integer> newStocks = new LinkedHashMap<>();
            for (int row = 0; row < model.getRowCount(); row++) {
                String productName = String.valueOf(model.getValueAt(row, 0));
                int stockValue = Integer.parseInt(String.valueOf(model.getValueAt(row, 2)).trim());
                newStocks.put(productName, stockValue);
            }

            try {
                DBConnection.updateStocks(newStocks);
            } catch (SQLException ex) {
                showDbError(dialog, "Could not save the stock changes to the database.", ex);
                return;
            }
            stocks.putAll(newStocks);


            refreshStockLabels();


            JOptionPane.showMessageDialog(dialog,
                    "Inventory stock updated successfully.",
                    "Inventory Saved", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });


        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }


    // Update stock quantities shown on the product cards.
    private void refreshStockLabels() {
        for (Map.Entry<String, JLabel> entry : stockLabels.entrySet()) {
            entry.getValue().setText("Stock: " + stocks.get(entry.getKey()));
        }
    }


    // VIEW COMPLETED ORDERS HISTORY
    private void showCompletedOrders() {
        final List<DBConnection.OrderRecord> orderHistory = fetchOrders();


        JDialog dialog = new JDialog(this, "Completed Orders History", true);
        dialog.setSize(750, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));


        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(pink);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));


        JLabel titleLabel = new JLabel("MISTY SWIRL - COMPLETED ORDERS HISTORY");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel, BorderLayout.WEST);


        dialog.add(header, BorderLayout.NORTH);


        String[] columns = {"Order ID", "Date & Time", "Processed By", "Payment Method", "Items Summary", "Total Paid"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };


        double grandTotal = 0;
        for (DBConnection.OrderRecord order : orderHistory) {
            StringBuilder itemsSummary = new StringBuilder();
            for (Map.Entry<String, Integer> entry : order.items.entrySet()) {
                if (itemsSummary.length() > 0) itemsSummary.append(", ");
                itemsSummary.append(entry.getKey()).append(" (x").append(entry.getValue()).append(")");
            }


            model.addRow(new Object[]{
                order.orderId,
                order.timestamp,
                order.role,
                order.paymentMethod,
                itemsSummary.toString(),
                String.format("₱%.2f", order.total)
            });
            grandTotal += order.total;
        }


        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(lightPink);
        table.getTableHeader().setForeground(darkPink);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));


        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new EmptyBorder(10, 15, 10, 15));
        dialog.add(scrollPane, BorderLayout.CENTER);


        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(new EmptyBorder(10, 20, 15, 20));
        footer.setBackground(new Color(255, 248, 250));


        JLabel summaryLabel = new JLabel(String.format("Total Revenue: ₱%.2f  |  Total Orders: %d", grandTotal, orderHistory.size()));
        summaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        summaryLabel.setForeground(darkPink);


        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);


        JButton viewDetailsBtn = new JButton("View Selected Order");
        styleButton(viewDetailsBtn, lightPink, darkPink);
        viewDetailsBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "Please select an order to view receipt details.");
                return;
            }


            DBConnection.OrderRecord selectedOrder = orderHistory.get(selectedRow);
            StringBuilder receiptDetail = new StringBuilder();
            receiptDetail.append("ORDER RECEIPT: ").append(selectedOrder.orderId).append("\n");
            receiptDetail.append("Date: ").append(selectedOrder.timestamp).append("\n");
            receiptDetail.append("Handled By: ").append(selectedOrder.role).append("\n");
            receiptDetail.append("Payment: ").append(selectedOrder.paymentMethod).append("\n\n");
            receiptDetail.append("Items Purchased:\n");


            for (Map.Entry<String, Integer> item : selectedOrder.items.entrySet()) {
                int itemPrice = selectedOrder.unitPrices.getOrDefault(item.getKey(), 0);
                int subtotal = itemPrice * item.getValue();
                receiptDetail.append(" - ").append(item.getKey())
                             .append(" x ").append(item.getValue())
                             .append(" = ₱").append(subtotal).append(".00\n");
            }


            receiptDetail.append("\nTOTAL PAID: ₱").append(String.format("%.2f", selectedOrder.total));


            JOptionPane.showMessageDialog(dialog, receiptDetail.toString(), "Order Details", JOptionPane.INFORMATION_MESSAGE);
        });


        JButton closeBtn = new JButton("Close");
        styleButton(closeBtn, pink, Color.WHITE);
        closeBtn.addActionListener(e -> dialog.dispose());


        actionPanel.add(viewDetailsBtn);
        actionPanel.add(closeBtn);


        footer.add(summaryLabel, BorderLayout.WEST);
        footer.add(actionPanel, BorderLayout.EAST);


        dialog.add(footer, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }


    // CHECKOUT: supports Cash and E-Cash payment methods
    private void checkout() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add an item to your order first.");
            return;
        }


        double total = 0;
        StringBuilder receipt = new StringBuilder("MISTY SWIRL RECEIPT\n\n");
        for (String name : cart.keySet()) {
            int qty = cart.get(name);
            int subtotal = prices.get(name) * qty;
            receipt.append(name).append(" x ").append(qty)
                   .append(" = ₱").append(subtotal).append(".00\n");
            total += subtotal;
        }
        receipt.append(String.format("\nTOTAL: ₱%.2f", total));


        JPanel paymentPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        paymentPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        JLabel totalText = new JLabel(String.format("Amount Due: ₱%.2f", total));
        totalText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JRadioButton cashButton = new JRadioButton("Cash", true);
        JRadioButton eCashButton = new JRadioButton("E-Cash (GCash / Maya / other e-wallet)");
        ButtonGroup paymentGroup = new ButtonGroup();
        paymentGroup.add(cashButton);
        paymentGroup.add(eCashButton);
        JTextField cashReceivedField = new JTextField();
        JTextField referenceField = new JTextField();
        JPanel cashPanel = new JPanel(new BorderLayout(8, 4));
        cashPanel.add(new JLabel("Cash received (₱):"), BorderLayout.WEST);
        cashPanel.add(cashReceivedField, BorderLayout.CENTER);
        JPanel referencePanel = new JPanel(new BorderLayout(8, 4));
        referencePanel.add(new JLabel("E-Cash reference no. (optional):"), BorderLayout.WEST);
        referencePanel.add(referenceField, BorderLayout.CENTER);
        paymentPanel.add(totalText);
        paymentPanel.add(cashButton);
        paymentPanel.add(cashPanel);
        paymentPanel.add(eCashButton);
        paymentPanel.add(referencePanel);


        cashButton.addActionListener(e -> {
            cashReceivedField.setEnabled(true);
            referenceField.setEnabled(false);
        });
        eCashButton.addActionListener(e -> {
            cashReceivedField.setEnabled(false);
            referenceField.setEnabled(true);
        });
        referenceField.setEnabled(false);


        int choice = JOptionPane.showConfirmDialog(this, paymentPanel,
                "Checkout and Payment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;


        String paymentMethod;
        double change = 0;
        if (cashButton.isSelected()) {
            double cashReceived;
            try {
                cashReceived = Double.parseDouble(cashReceivedField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid cash amount received.",
                        "Invalid Payment", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (cashReceived < total) {
                JOptionPane.showMessageDialog(this, String.format(
                        "Insufficient cash. Amount due is ₱%.2f.", total),
                        "Invalid Payment", JOptionPane.ERROR_MESSAGE);
                return;
            }
            change = cashReceived - total;
            paymentMethod = "Cash";
        } else {
            paymentMethod = "E-Cash";
        }


        int confirm = JOptionPane.showConfirmDialog(this,
                receipt + "\nPayment Method: " + paymentMethod
                + (cashButton.isSelected()
                    ? String.format("\nCash Received: ₱%.2f\nChange: ₱%.2f",
                        Double.parseDouble(cashReceivedField.getText().trim()), change)
                    : (referenceField.getText().trim().isEmpty() ? "" :
                        "\nReference: " + referenceField.getText().trim()))
                + "\n\nConfirm payment and complete this order?",
                "Confirm Order", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;


        // SAVE THE ORDER, ITS ITEMS AND THE STOCK DEDUCTION IN ONE TRANSACTION
        String orderId;
        try {
            orderId = DBConnection.saveOrder(role, paymentMethod, total, cart, prices);
        } catch (SQLException ex) {
            showDbError(this, "The order could not be saved. No stock was deducted.", ex);
            return;
        }

        try {
            DBConnection.loadStocks(stocks);
        } catch (SQLException ex) {
            for (String name : cart.keySet()) {
                stocks.put(name, stocks.get(name) - cart.get(name));
            }
        }
        refreshStockLabels();
        cart.clear();
        refreshOrder();


        JOptionPane.showMessageDialog(this,
                "Order " + orderId + " completed successfully!\nPayment: " + paymentMethod
                + (cashButton.isSelected() ? String.format("\nChange: ₱%.2f", change) : ""),
                "Payment Successful", JOptionPane.INFORMATION_MESSAGE);
    }


    // SINGLE LOGIN LAYOUT: role selector and credentials on one screen
    private static void showLogin() {
        JFrame login = new JFrame("Misty Swirl - Login");
        login.setSize(470, 500);
        login.setMinimumSize(new Dimension(430, 460));
        login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        login.setLocationRelativeTo(null);
        login.setLayout(new BorderLayout());


        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 5));
        heading.setBackground(new Color(222, 105, 137));
        heading.setBorder(new EmptyBorder(28, 12, 26, 12));
        JLabel brand = new JLabel("MISTY SWIRL", SwingConstants.CENTER);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 30));
        brand.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("POS AND INVENTORY SYSTEM", SwingConstants.CENTER);
        subtitle.setForeground(Color.WHITE);
        heading.add(brand);
        heading.add(subtitle);
        login.add(heading, BorderLayout.NORTH);


        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(255, 235, 241));
        form.setBorder(new EmptyBorder(24, 42, 24, 42));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 5, 7, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0; c.gridy = 0;
        form.add(new JLabel("Login as:"), c);
        JComboBox<String> roleChoice = new JComboBox<>(new String[]{"ADMIN", "STAFF"});
        roleChoice.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        c.gridy = 1;
        form.add(roleChoice, c);
        c.gridy = 2;
        form.add(new JLabel("Username:"), c);
        JTextField usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        c.gridy = 3;
        form.add(usernameField, c);
        c.gridy = 4;
        form.add(new JLabel("Password:"), c);
        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        c.gridy = 5;
        form.add(passwordField, c);
        JButton loginButton = new JButton("LOG IN");
        styleLoginButton(loginButton, new Color(222, 105, 137), Color.WHITE);
        c.gridy = 6;
        c.insets = new Insets(18, 5, 5, 5);
        form.add(loginButton, c);
        login.add(form, BorderLayout.CENTER);


        Runnable doLogin = () -> {
            String selectedRole = String.valueOf(roleChoice.getSelectedItem());
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            boolean valid;
            try {
                valid = DBConnection.validateLogin(selectedRole, username, password);
            } catch (SQLException ex) {
                showDbError(login, "Could not reach the database.", ex);
                return;
            }
            if (valid) {
                login.dispose();
                new MistySwirlPOS(selectedRole);
            } else {
                JOptionPane.showMessageDialog(login, "Invalid username or password!",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
                passwordField.requestFocusInWindow();
            }
        };
        loginButton.addActionListener(e -> doLogin.run());
        passwordField.addActionListener(e -> doLogin.run());
        login.setVisible(true);
    }


    private static void styleLoginButton(JButton button,
                                         Color background,
                                         Color foreground) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new CompoundBorder(
                new LineBorder(new Color(238, 190, 207), 1, true),
                new EmptyBorder(12, 15, 12, 15)));
    }


    // DATABASE HELPERS
    private static void showDbError(Component parent, String message, SQLException ex) {
        JOptionPane.showMessageDialog(parent,
                message + "\n\nDetails: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }


    private static String safeUsername(String role) {
        try {
            return DBConnection.getUsername(role);
        } catch (SQLException ex) {
            return "";
        }
    }


    private List<DBConnection.OrderRecord> fetchOrders() {
        try {
            return DBConnection.loadOrders();
        } catch (SQLException ex) {
            showDbError(this, "Could not load the order history.", ex);
            return new java.util.ArrayList<>();
        }
    }


    // MAIN METHOD
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }


        UIManager.put("OptionPane.background",
                new Color(255, 248, 250));
        UIManager.put("Panel.background",
                new Color(255, 248, 250));


        // Check the database connection before showing the login window.
        try (java.sql.Connection con = DBConnection.getConnection()) {
            // connected
        } catch (SQLException ex) {
            showDbError(null, "Cannot connect to the database.\n"
                    + "Start MySQL in the XAMPP Control Panel, import misty_swirl_pos.sql "
                    + "in phpMyAdmin, and make sure the MySQL JDBC driver is added to the project.", ex);
            System.exit(1);
        }

        SwingUtilities.invokeLater(MistySwirlPOS::showLogin);
    }
}