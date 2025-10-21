
package xyz.blackdev.wgetapi;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.builder.ActivateType;

public class WgetAPI {
    public static void main(String[] args) throws java.io.IOException {
        CraftsNet.create()
                .withArgs(args)
                .withAddonSystem(ActivateType.DISABLED)
                .withWebServer(8080)
                .build();
        System.out.println("API up on http://localhost:8080");
    }
}
