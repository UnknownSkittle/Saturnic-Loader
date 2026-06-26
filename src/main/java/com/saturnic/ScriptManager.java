package com.saturnic;

import org.mozilla.javascript.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ScriptManager {

    private final Path saturnicDir;
    private final Context cx;
    private final Scriptable scope;

    public ScriptManager(Path saturnicDir) {
        this.saturnicDir = saturnicDir;

        cx = Context.enter();
        cx.setOptimizationLevel(-1);
        scope = cx.initStandardObjects();

        ScriptAPI api = new ScriptAPI(this);
        ScriptableObject.putProperty(scope, "api", Context.javaToJS(api, scope));
    }

    public void loadClient() {
        try {
            if (!Files.exists(saturnicDir)) Files.createDirectories(saturnicDir);

            // 1. Load dev folder
            Path devDir = saturnicDir.resolve("dev");
            if (Files.exists(devDir)) {
                try (Stream<Path> versions = Files.list(devDir)) {
                    Optional<Path> first = versions.filter(Files::isDirectory).findFirst();
                    if (first.isPresent()) {
                        System.out.println("[SaturnicLoader] Loading dev client: " + first.get());
                        loadFolderClient(first.get());
                        return;
                    }
                }
            }

            // 2. Load .strnc file
            try (Stream<Path> files = Files.list(saturnicDir)) {
                Optional<Path> strnc = files
                        .filter(p -> p.toString().endsWith(".strnc"))
                        .findFirst();

                if (strnc.isPresent()) {
                    System.out.println("[SaturnicLoader] Loading packaged client: " + strnc.get());
                    loadZipClient(strnc.get());
                    return;
                }
            }

            System.out.println("[SaturnicLoader] No client found.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFolderClient(Path folder) throws IOException {
        Files.walk(folder)
                .filter(p -> p.toString().endsWith(".js"))
                .forEach(this::loadScript);
    }

    private void loadZipClient(Path zipPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".js")) {
                    String code = new String(zis.readAllBytes());
                    cx.evaluateString(scope, code, entry.getName(), 1, null);
                }
            }
        }
    }

    private void loadScript(Path path) {
        try {
            String code = Files.readString(path);
            cx.evaluateString(scope, code, path.getFileName().toString(), 1, null);
        } catch (Exception e) {
            System.out.println("[SaturnicLoader] Script error: " + e.getMessage());
        }
    }

    public void onTick() {
        Object tickFunc = ScriptableObject.getProperty(scope, "onTick");
        if (tickFunc instanceof Function fn) {
            fn.call(cx, scope, scope, new Object[]{});
        }
    }
}
