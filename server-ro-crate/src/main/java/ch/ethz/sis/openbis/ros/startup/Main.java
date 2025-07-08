package ch.ethz.sis.openbis.ros.startup;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

public class Main implements QuarkusApplication
{
    public static void main(String[] args)
    {
        Quarkus.run(Main.class);
    }

    @Override
    public int run(String... args) throws Exception
    {
        System.out.println(">> Quarkus app running. Press Ctrl+C to exit.");
        Quarkus.waitForExit();
        return 0;
    }
}
