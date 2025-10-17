package andreidodu.nocconvert.util;

import javax.swing.*;
import java.io.File;
import java.util.Optional;

public class FileSystemSupportGuiUtil {
    public Optional<String> selectDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select a directory");


        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            return Optional.of(selectedDir.getAbsolutePath());
        }

        return Optional.empty();
    }

}
