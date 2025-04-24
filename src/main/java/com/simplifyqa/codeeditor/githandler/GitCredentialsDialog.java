package com.simplifyqa.codeeditor.githandler;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Paths;
import java.util.Properties;

public class GitCredentialsDialog extends JDialog {
    private boolean confirmed = false;
    private JTextField gitUrlField;
    private JTextField gitUsernameField;
    private JPasswordField gitTokenField;
    private JButton okButton;

    public GitCredentialsDialog(Frame parent, Properties props) {
        super(parent, "Enter Git Credentials", true);
        setIconImage(new ImageIcon(Paths.get(".theia","48-48.png").toAbsolutePath().toString()).getImage()); // Set custom logo
        setAlwaysOnTop(true);

        gitUrlField = new JTextField(30) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.height = 25;
                return size;
            }
        };
        gitUrlField.setBorder(null);

        gitUsernameField = new JTextField(30) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.height = 25;
                return size;
            }
        };
        gitUsernameField.setBorder(null);

        gitTokenField = new JPasswordField(30) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.height = 25;
                return size;
            }
        };
        gitTokenField.setBorder(null);

        gitUrlField.setText(props.getProperty("git.url", ""));
        gitUsernameField.setText(props.getProperty("git.username", ""));
        gitTokenField.setText(props.getProperty("git.token", ""));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Git URL:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(gitUrlField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Git Username:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(gitUsernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Git Token:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        panel.add(gitTokenField, gbc);


        JButton toggleTokenVisibilityButton = new JButton("👁️");
        toggleTokenVisibilityButton.setPreferredSize(new Dimension(60, 30));
        toggleTokenVisibilityButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        toggleTokenVisibilityButton.setBackground(Color.WHITE);
        toggleTokenVisibilityButton.setBorderPainted(false);
        toggleTokenVisibilityButton.setContentAreaFilled(false);
        toggleTokenVisibilityButton.setFocusPainted(false);

        addHoverEffect(toggleTokenVisibilityButton);

        toggleTokenVisibilityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gitTokenField.getEchoChar() == '\u2022') {
                    gitTokenField.setEchoChar((char) 0);
                } else {
                    gitTokenField.setEchoChar('\u2022');
                }
            }
        });

        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(26, -10, 10, 5);
        panel.add(toggleTokenVisibilityButton, gbc);

        okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        customizeButton(okButton);
        customizeButton(cancelButton);

        okButton.setEnabled(false);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                checkFields();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkFields();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkFields();
            }

            private void checkFields() {
                boolean allFieldsFilled = !gitUrlField.getText().trim().isEmpty() &&
                        !gitUsernameField.getText().trim().isEmpty() &&
                        !new String(gitTokenField.getPassword()).trim().isEmpty();
                okButton.setEnabled(allFieldsFilled);
            }
        };

        gitUrlField.getDocument().addDocumentListener(documentListener);
        gitUsernameField.getDocument().addDocumentListener(documentListener);
        gitTokenField.getDocument().addDocumentListener(documentListener);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private void customizeButton(JButton button) {
        button.setPreferredSize(new Dimension(80, 30));
        button.setBackground(Color.WHITE);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        addHoverEffect(button);
    }

    private void addHoverEffect(JButton button) {
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(135, 206, 250));
                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
                button.setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    public boolean isConfirmed() {
        return confirmed;
    }


    public String getGitUrl() {
        return gitUrlField.getText().trim();
    }

    public String getGitUsername() {
        return gitUsernameField.getText().trim();
    }

    public String getGitToken() {
        return new String(gitTokenField.getPassword()).trim();
    }
}