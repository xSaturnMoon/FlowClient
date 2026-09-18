package com.flowclient.mods.schematics;

import java.awt.FileDialog;
import java.io.IOException;
import java.nio.file.Path;

public final class SchematicImporter {
    private SchematicImporter() {}

    public static boolean importFromDialog() {
        FileDialog dialog = new FileDialog((java.awt.Frame) null, "Select Schematic", FileDialog.LOAD);
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".schem")
                    || lower.endsWith(".schematic")
                    || lower.endsWith(".litematic")
                    || lower.endsWith(".nbt");
        });
        dialog.setVisible(true);
        String file = dialog.getFile();
        String directory = dialog.getDirectory();
        if (file == null || directory == null) {
            return false;
        }

        try {
            SchematicRepository.importFile(Path.of(directory, file));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
