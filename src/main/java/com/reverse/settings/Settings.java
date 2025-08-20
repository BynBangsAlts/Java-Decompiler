package com.reverse.settings;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.reverse.settings.Theme;
import com.reverse.settings.Theme.Mode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

public class Settings extends JDialog {
    private final JComboBox<Mode> theme;
    private final JComboBox<String> font;
    private final JSlider size;
    private final JCheckBox lineNumbers;
    private final JCheckBox zen;

    public Settings(Window owner) {
        super(owner, "Settings", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        var panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(16,16,16,16));
        var form = new JPanel(new GridBagLayout());
        var gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0; gc.insets = new Insets(4,0,4,12); gc.anchor = GridBagConstraints.LINE_END;
        JLabel themeL = new JLabel("Appearance:");
        JLabel fontL  = new JLabel("Editor font:");
        JLabel sizeL  = new JLabel("Font size:");
        JLabel lnL    = new JLabel("Line numbers:");
        JLabel zenL   = new JLabel("Dark mode:");
        theme = new JComboBox<>(Mode.values());
        theme.setSelectedItem(Theme.getMode());
        String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        Arrays.sort(families, String.CASE_INSENSITIVE_ORDER);
        font = new JComboBox<>(families);
        font.setEditable(false);
        font.setSelectedItem(Theme.editorFont());
        size = new JSlider(11, 22, Theme.editorSize());
        size.setPaintTicks(true);
        size.setMajorTickSpacing(2);
        size.setMinorTickSpacing(1);
        size.setPaintLabels(true);
        lineNumbers = new JCheckBox("Show numbers on the left", Theme.showLineNumbers());
        zen = new JCheckBox("Extra room", Theme.zenMode());
        form.add(themeL, gc); gc.gridx = 1; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(theme, gc);

        // row 2
        gc.gridx = 0; gc.gridy++; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
        form.add(fontL, gc); gc.gridx = 1; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(font, gc);

        // row 3
        gc.gridx = 0; gc.gridy++; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
        form.add(sizeL, gc); gc.gridx = 1; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(size, gc);

        // row 4
        gc.gridx = 0; gc.gridy++; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
        form.add(lnL, gc); gc.gridx = 1; gc.weightx = 1;
        form.add(lineNumbers, gc);

        // row 5
        gc.gridx = 0; gc.gridy++; gc.weightx = 0;
        form.add(zenL, gc); gc.gridx = 1; gc.weightx = 1;
        form.add(zen, gc);

        panel.add(form, BorderLayout.CENTER);
        var buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        var cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        var apply = new JButton("Apply");
        apply.addActionListener(e -> { savePrefs(false); });
        var save = new JButton("Save & Close");
        save.addActionListener(e -> { savePrefs(true); });
        buttons.add(cancel);
        buttons.add(apply);
        buttons.add(save);
        panel.add(buttons, BorderLayout.SOUTH);
        setContentPane(panel);
        setSize(520, 360);
        setLocationRelativeTo(owner);
    }

    private void savePrefs(boolean close) {
        Theme.setMode((Mode) theme.getSelectedItem());
        Theme.setEditorFont((String) font.getSelectedItem());
        Theme.setEditorSize(size.getValue());
        Theme.setShowLineNumbers(lineNumbers.isSelected());
        Theme.setZenMode(zen.isSelected());
        Theme.refreshAll();
        if (close) dispose();
    }
}
