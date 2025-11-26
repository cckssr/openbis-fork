package ch.ethz.sis.openbis.systemtests.environment;

public interface Server<C>
{
    void configure(C configuration);

    void start();

    void stop();

    C getConfiguration();

    StringBuffer getLogs();
}
