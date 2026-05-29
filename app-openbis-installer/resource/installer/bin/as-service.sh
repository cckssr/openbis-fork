#!/bin/bash

# Name of the script/service (optional, good for logging)
SERVICE_NAME="Application Server Service"

# Function to display usage instructions
usage() {
    echo "Usage: $0 {start|stop|restart|status|log}"
    exit 1
}

# Check if exactly one argument is passed
if [ $# -ne 1 ]; then
    usage
fi

## Obtain current script
BASE=$(cd -- "$(dirname -- "$(readlink -f -- "$0")")" &>/dev/null && pwd)

# Action based on the argument provided
case "$1" in
    start)
        echo "Starting $SERVICE_NAME..."
        # Add your start command logic here
        $BASE/../servers/openBIS-server/jetty/bin/startup.sh
        ;;

    stop)
        echo "Stopping $SERVICE_NAME..."
        # Add your stop command logic here
        $BASE/../servers/openBIS-server/jetty/bin/shutdown.sh
        ;;

    restart)
        echo "Stopping $SERVICE_NAME..."
        # Add your stop command logic here
        $BASE/../servers/openBIS-server/jetty/bin/shutdown.sh
        sleep 2
        echo "Starting $SERVICE_NAME..."
        # Add your start command logic here
        $BASE/../servers/openBIS-server/jetty/bin/startup.sh
      ;;

    status)
        echo "Checking status of $SERVICE_NAME..."
        # Add your status checking logic here
        # Example: check if a process is running
        PID_FILE="$BASE/../servers/openBIS-server/jetty/openbis.pid"
        # 1. Check if the PID file exists
        if [ -f "$PID_FILE" ]; then
            # Read the PID from the file
            PID=$(cat "$PID_FILE")

            # 2. Check if that specific PID is actually running in the system
            if kill -0 "$PID" 2>/dev/null; then
                echo "Status: Running (PID: $PID)"
                exit 0
            else
                echo "Status: CRASHED (PID file exists, but process $PID is dead)"
                exit 1
            fi
        else
            # 3. PID file doesn't exist. Let's double-check the process table
            # just in case it was started without a PID file.
            if pgrep -x "$PROCESS_NAME" > /dev/null; then
                echo "Status: Running (Warning: Process is up, but PID file is missing)"
                exit 0
            else
                echo "Status: Stopped (Cleanly inactive)"
                exit 3
            fi
        fi
        ;;

    log)
        echo "Displaying logs for $SERVICE_NAME..."
        # Add your log viewing logic here
        # Example: tail -f /var/log/myservice.log
        less $BASE/../servers/openBIS-server/jetty/logs/openbis.log
        ;;

    *)
        # Catch-all for any invalid arguments
        echo "Error: Invalid command '$1'"
        usage
        ;;
esac

exit 0