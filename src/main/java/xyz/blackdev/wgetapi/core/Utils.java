
package xyz.blackdev.wgetapi.core;

import java.io.*;
import java.nio.file.*;
import java.util.List;

public class Utils {
    public static void ensureDir(Path p) throws IOException {
        if (!Files.exists(p)) Files.createDirectories(p);
    }
    public static int sh(List<String> cmd, Path workdir, Appendable out) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workdir != null) pb.directory(workdir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        return p.waitFor();
    }
    public static String shellQuote(String s) {
        if (s == null || s.isEmpty()) return "''";
        return "'" + s.replace("'", "'\"'\"'") + "'";
    }
}
