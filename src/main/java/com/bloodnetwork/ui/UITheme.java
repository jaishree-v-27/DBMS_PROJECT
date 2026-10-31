package com.bloodnetwork.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Modern Clinical Design System for the Blood Donor Network Swing Application.
 * Implements a cohesive hospital SaaS visual hierarchy:
 * - Deep medical red (#B71C1C) and slate dark (#0F172A)
 * - Crisp white cards (#FFFFFF) and soft slate background (#F8FAFC)
 * - Modern typography, subtle hover transitions, and hospital status indicators.
 */
public class UITheme {

    // Primary Brand & Healthcare Palette
    public static final Color PRIMARY_RED     = new Color(183, 28, 28);    // #B71C1C
    public static final Color PRIMARY_DARK    = new Color(127, 0, 0);      // #7F0000
    public static final Color PRIMARY_HOVER   = new Color(153, 27, 27);    // #991B1B
    public static final Color ACCENT_RED      = new Color(211, 47, 47);    // #D32F2F
    public static final Color SELECTION_RED   = new Color(254, 226, 226);  // #FEE2E2
    public static final Color SELECTION_TEXT  = new Color(153, 27, 27);    // #991B1B

    // Neutral & Background Colors
    public static final Color NAVY_DARK       = new Color(15, 23, 42);     // #0F172A
    public static final Color SLATE_DARK      = new Color(30, 41, 59);     // #1E293B
    public static final Color SLATE_MUTED     = new Color(100, 116, 139);  // #64748B
    public static final Color LIGHT_BG        = new Color(248, 250, 252);  // #F8FAFC
    public static final Color BG_HOVER_LIGHT  = new Color(241, 245, 249);  // #F1F5F9
    public static final Color BORDER_COLOR    = new Color(226, 232, 240);  // #E2E8F0
    public static final Color BORDER_FOCUS    = new Color(183, 28, 28);    // #B71C1C
    public static final Color WHITE           = Color.WHITE;
    public static final Color TEXT_DARK       = new Color(15, 23, 42);     // High-contrast slate
    public static final Color TEXT_MUTED      = new Color(100, 116, 139);  // Muted slate

    // Clinical Status & Notification Colors
    public static final Color SUCCESS_GREEN   = new Color(22, 163, 74);    // #16A34A
    public static final Color SUCCESS_BG      = new Color(220, 252, 231);  // #DCFCE7
    public static final Color WARNING_AMBER   = new Color(217, 119, 6);    // #D97706
    public static final Color WARNING_BG      = new Color(254, 243, 199);  // #FEF3C7
    public static final Color DANGER_RED      = new Color(220, 38, 38);    // #DC2626
    public static final Color DANGER_HOVER    = new Color(185, 28, 28);    // #B91C1C
    public static final Color DANGER_BG       = new Color(254, 226, 226);  // #FEE2E2
    public static final Color INFO_BLUE       = new Color(37, 99, 235);    // #2563EB
    public static final Color INFO_BG         = new Color(219, 234, 254);  // #DBEAFE
    public static final Color WARNING_RED_BG  = new Color(255, 205, 210);  // Legacy compat

    // Typography (Segoe UI font hierarchy)
    public static final Font FONT_DISPLAY     = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_HEADER      = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font FONT_TITLE       = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY        = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD   = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL       = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BADGE       = new Font("Segoe UI", Font.BOLD, 11);

    private UITheme() {}

    /**
     * Creates a consistent hospital top header banner with branding.
     */
    public static JPanel createHeaderBanner(String title, String subtitle) {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(PRIMARY_RED);
        banner.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JLabel lblIcon = new JLabel("🩸");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        leftPanel.add(lblIcon);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_HEADER);
        lblTitle.setForeground(WHITE);
        leftPanel.add(lblTitle);

        banner.add(leftPanel, BorderLayout.WEST);

        if (subtitle != null && !subtitle.isEmpty()) {
            JLabel lblSub = new JLabel(subtitle);
            lblSub.setFont(FONT_BODY);
            lblSub.setForeground(new Color(255, 235, 238));
            banner.add(lblSub, BorderLayout.EAST);
        }

        return banner;
    }

    /**
     * Styles an action button with primary medical red and smooth hover transition.
     */
    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(PRIMARY_RED);
        btn.setForeground(WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(PRIMARY_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(PRIMARY_RED);
                }
            }
        });

        return btn;
    }

    /**
     * Styles a secondary outline button with subtle hover feedback.
     */
    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(WHITE);
        btn.setForeground(SLATE_DARK);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(BG_HOVER_LIGHT);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(WHITE);
                }
            }
        });

        return btn;
    }

    /**
     * Styles a danger action button (e.g., delete, cancel) with smooth red hover.
     */
    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(DANGER_RED);
        btn.setForeground(WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(DANGER_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(DANGER_RED);
                }
            }
        });

        return btn;
    }

    /**
     * Styles text input fields with clean padding and interactive focus state.
     */
    public static void styleTextField(JTextField tf) {
        tf.setFont(FONT_BODY);
        tf.setBackground(WHITE);
        tf.setForeground(TEXT_DARK);
        Border idleBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        );
        Border focusBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_FOCUS, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        );
        tf.setBorder(idleBorder);

        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tf.setBorder(focusBorder);
            }

            @Override
            public void focusLost(FocusEvent e) {
                tf.setBorder(idleBorder);
            }
        });
    }

    /**
     * Creates a styled card container panel with clean borders and padding.
     */
    public static JPanel createCardPanel() {
        JPanel card = new JPanel();
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        return card;
    }

    /**
     * Creates a healthcare KPI metric card displaying an icon, label, and highlighted value.
     */
    public static JPanel createKpiCard(String icon, String label, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(10, 6));
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        top.add(lblIcon, BorderLayout.WEST);

        JLabel lblTitle = new JLabel(label.toUpperCase());
        lblTitle.setFont(FONT_BADGE);
        lblTitle.setForeground(SLATE_MUTED);
        top.add(lblTitle, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(FONT_DISPLAY);
        lblVal.setForeground(accentColor != null ? accentColor : TEXT_DARK);
        card.add(lblVal, BorderLayout.CENTER);

        return card;
    }

    /**
     * Applies modern hospital SaaS table styling:
     * - 30px row height
     * - Sleek slate header
     * - Alternating row backgrounds (#FFFFFF and #F8FAFC)
     * - Soft crimson selection highlight (#FEE2E2)
     */
    public static void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(FONT_BODY);
        table.setShowGrid(true);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER_COLOR);
        table.setSelectionBackground(SELECTION_RED);
        table.setSelectionForeground(SELECTION_TEXT);

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BODY_BOLD);
        header.setBackground(BG_HOVER_LIGHT);
        header.setForeground(SLATE_DARK);
        header.setPreferredSize(new Dimension(header.getWidth(), 34));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        // Alternating row renderer (installed only if default renderer is active)
        DefaultTableCellRenderer alternatingRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? WHITE : LIGHT_BG);
                    c.setForeground(TEXT_DARK);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            if (table.getColumnModel().getColumn(i).getCellRenderer() == null
                    || table.getColumnModel().getColumn(i).getCellRenderer().getClass() == DefaultTableCellRenderer.class) {
                table.getColumnModel().getColumn(i).setCellRenderer(alternatingRenderer);
            }
        }
    }
}
