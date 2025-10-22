package xyz.blackdev.wgetapi.winget;

import de.craftsblock.craftsnet.api.http.Exchange;
import de.craftsblock.craftsnet.api.http.RequestHandler;
import de.craftsblock.craftsnet.api.http.annotations.Route;
import de.craftsblock.craftsnet.api.transformers.annotations.Transformer;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegister;

import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@AutoRegister
public class WingetRoutes implements RequestHandler {

    private static WingetIndex index;

    static {
        try {
            var data = Paths.get("cloud-data");
            index = new WingetIndex(data);

            Thread t = new Thread(() -> {
                try {
                    index.syncAndIndexIfDue();
                } catch (Throwable ignored) {}
                while (true) {
                    try {
                        index.syncAndIndexIfDue();
                        Thread.sleep(15 * 60 * 1000);
                    } catch (Throwable ignored) {}
                }
            }, "winget-sync");
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String json(Object o) {
        try { return new com.google.gson.Gson().toJson(o); }
        catch (Throwable t) { return "[]"; }
    }

    private List<WingetIndex.Pkg> applyFilters(List<WingetIndex.Pkg> input) {
        return input.stream()
                .filter(p -> p.name != null && !p.name.isBlank())
                .filter(p -> p.publisher != null && !p.publisher.isBlank())
                .filter(p -> p.id != null && !p.id.isBlank())
                .filter(p -> p.version != null && !p.version.isBlank())
                .collect(Collectors.toList());
    }

    @Route("/winget/all")
    public String all(Exchange ex) {
        return json(applyFilters(index.all()));
    }

    @Route("/winget/all/{limit}")
    @Transformer(parameter = "limit", transformer = WingetLimitSanitizer.class)
    public String allLimit(Exchange ex, int limit) {
        List<WingetIndex.Pkg> list = applyFilters(index.all());
        if (list.size() <= limit) return json(list);
        return json(list.subList(0, limit));
    }

    @Route("/winget/search/{q}")
    public String search(Exchange ex, String q) {
        String s = q.toLowerCase(Locale.ROOT);
        return json(
                applyFilters(index.search(s))
        );
    }
}