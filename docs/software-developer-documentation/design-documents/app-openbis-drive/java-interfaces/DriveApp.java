package ch.openbis.drive;

public class DriveApp {

    /**
     * ./drive-app help
     * <p>
     * ./drive-app start
     * ./drive-app stop
     * ./drive-app status
     * <p>
     * Prints config on the standard output, in one line, fields separated by tabs
     * ./drive-app config
     * ./drive-app config -startAtLogin=true|false -language=eng syncInterval=120
     * <p>
     * Prints jobs on the standard output, one per line, fields separated by tabs
     * ./drive-app jobs
     * ./drive-app jobs add -type='Bidirectional|Upload|Download' -dir='./dir-a/dir-b' ...
     * ./drive-app jobs remove -dir='./dir-a/dir-b'
     * ./drive-app jobs start -dir='./dir-a/dir-b'
     * ./drive-app jobs stop -dir='./dir-a/dir-b'
     * <p>
     * Prints notifications on the standard output, one per line, fields separated by tabs
     * ./drive-app notifications -limit=100
     * <p>
     * Prints events on the standard output, one per line, fields separated by tabs
     * ./drive-app events -limit=100
     */
    public static void main(String[] args) {

    }
}
