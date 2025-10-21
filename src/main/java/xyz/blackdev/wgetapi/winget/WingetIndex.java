
package xyz.blackdev.wgetapi.winget;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import static xyz.blackdev.wgetapi.core.Utils.*;

public class WingetIndex {
    public static class Pkg {
        public String id;
        public String name;
        public String publisher;
        public String version;
        public String manifestPath;
    }

    private final Path repoDir;
    private final Path cacheFile;
    private final Path lastSyncFile;
    private volatile List<Pkg> cache = new ArrayList<>();
    private final AtomicBoolean indexing = new AtomicBoolean(false);

    public WingetIndex(Path dataDir) throws java.io.IOException {
        this.repoDir = dataDir.resolve("winget-pkgs");
        this.cacheFile = dataDir.resolve("winget-index.json");
        this.lastSyncFile = dataDir.resolve("winget-last-sync.txt");
        if (Files.exists(cacheFile)) {
            try {
                String j = Files.readString(cacheFile, StandardCharsets.UTF_8);
                com.google.gson.reflect.TypeToken<java.util.List<Pkg>> tt = new com.google.gson.reflect.TypeToken<java.util.List<Pkg>>(){};
                java.util.List<Pkg> list = new com.google.gson.Gson().fromJson(j, tt.getType());
                if (list != null) cache = list;
            } catch (Throwable ignored) {}
        }
    }

    public synchronized List<Pkg> all() { return cache; }

    public synchronized List<Pkg> search(String q) {
        if (q == null || q.isBlank()) return cache;
        String s = q.toLowerCase(java.util.Locale.ROOT);
        return cache.stream().filter(p ->
                (p.id != null && p.id.toLowerCase(java.util.Locale.ROOT).contains(s)) ||
                (p.name != null && p.name.toLowerCase(java.util.Locale.ROOT).contains(s)) ||
                (p.publisher != null && p.publisher.toLowerCase(java.util.Locale.ROOT).contains(s))
        ).collect(Collectors.toList());
    }

    public void syncAndIndexIfDue() throws Exception {
        if (indexing.get()) return;
        indexing.set(true);
        try {
            Instant last = Instant.EPOCH;
            if (Files.exists(lastSyncFile)) {
                try { last = Instant.ofEpochMilli(Long.parseLong(Files.readString(lastSyncFile).trim())); } catch (Exception ignored) {}
            }
            if (Duration.between(last, Instant.now()).toMinutes() < 60 && !cache.isEmpty()) return;

            if (!Files.exists(repoDir)) {
                StringBuilder out = new StringBuilder();
                int code = sh(java.util.List.of("bash","-lc",
                        "git clone --depth=1 https://github.com/microsoft/winget-pkgs " + shellQuote(repoDir.toString())),
                        null, out);
                if (code != 0) { Files.writeString(lastSyncFile, Long.toString(System.currentTimeMillis()), StandardCharsets.UTF_8); return; }
            } else {
                StringBuilder out = new StringBuilder();
                sh(java.util.List.of("bash","-lc",
                        "cd " + shellQuote(repoDir.toString()) + " && git fetch --depth=1 origin master && git reset --hard origin/master"),
                        null, out);
            }

            Path manifests = repoDir.resolve("manifests");
            List<Pkg> list = new ArrayList<>();
            if (Files.exists(manifests)) {
                Yaml yaml = new Yaml();
                Files.walk(manifests).forEach(p -> {
                    String n = p.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                    if (n.endsWith(".yaml") || n.endsWith(".yml")) {
                        try (InputStream in = Files.newInputStream(p)) {
                            Object obj = yaml.load(in);
                            if (!(obj instanceof java.util.Map)) return;
                            java.util.Map<?,?> m = (java.util.Map<?,?>) obj;
                            Pkg pkg = new Pkg();
                            Object id = m.get("PackageIdentifier");
                            Object name = m.get("PackageName");
                            Object pub = m.get("Publisher");
                            Object ver = m.get("PackageVersion");
                            if (id == null && name == null) return;
                            pkg.id = id == null ? null : id.toString();
                            pkg.name = name == null ? null : name.toString();
                            pkg.publisher = pub == null ? null : pub.toString();
                            pkg.version = ver == null ? null : ver.toString();
                            pkg.manifestPath = manifests.relativize(p).toString();
                            list.add(pkg);
                        } catch (Exception ignore) {}
                    }
                });
            }
            synchronized (this) { cache = list; }
            try { Files.writeString(cacheFile, new com.google.gson.Gson().toJson(list), StandardCharsets.UTF_8); } catch (Throwable ignored) {}
            Files.writeString(lastSyncFile, Long.toString(System.currentTimeMillis()), StandardCharsets.UTF_8);
        } finally {
            indexing.set(false);
        }
    }
}
