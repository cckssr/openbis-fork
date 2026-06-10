#!/bin/bash

# Exit immediately if a pipeline returns a non-zero status.
# (We handle specific command failures manually, so we leave standard set -e out 
# to control the flow during stop/status loops).


## Obtain current script
BASE=$(cd -- "$(dirname -- "$(readlink -f -- "$0")")" &>/dev/null && pwd)

# --- Configuration ---
# Define your scripts in the exact order they should START.
SERVICES=(
    "$BASE/as-service.sh"
    "$BASE/dss-service.sh"
    "$BASE/afs-service.sh"
    "$BASE/roc-service.sh"
)

# --- Helper Functions ---
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# --- Core Actions ---

do_start() {
    log "Starting all services..."
    for script in "${SERVICES[@]}"; do
        log "Executing: $script start"
        if ! "$script" start; then
            log "ERROR: $script failed to start. Aborting master start sequence."
            return 1
        fi
    done
    log "All services started successfully."
    return 0
}

do_stop() {
    log "Stopping all services in reverse order..."
    local exit_code=0
    
    # Loop through the array backwards
    for (( i=${#SERVICES[@]}-1; i>=0; i-- )); do
        local script="${SERVICES[$i]}"
        log "Executing: $script stop"
        if ! "$script" stop; then
            log "WARNING: $script failed to stop cleanly."
            exit_code=1  # Keep going to try and stop other services, but record the error
        fi
    done
    
    if [ $exit_code -eq 0 ]; then
        log "All services stopped successfully."
    else
        log "Component service stop sequences reported errors."
    fi
    return $exit_code
}

do_status() {
    log "Checking status of all services..."
    local global_status=0

    for script in "${SERVICES[@]}"; do
        # systemd contract expects 0 for running, non-zero for stopped/failed
        "$script" status
        local status_code=$?
        
        if [ $status_code -ne 0 ]; then
            log "ALERT: $script is NOT running perfectly (Exit code: $status_code)."
            global_status=$status_code # Capture the failure code
        else
            log "OK: $script is running."
        fi
    done

    if [ $global_status -eq 0 ]; then
        log "SUCCESS: All services are healthy."
    else
        log "CRITICAL: One or more services are down or degraded."
    fi
    
    return $global_status
}

# --- Main Entry Point ---
ACTION="$1"

case "$ACTION" in
    start)
        do_start
        exit $?
        ;;
    stop)
        do_stop
        exit $?
        ;;
    restart)
        # Standard systemd contract implementation for restart
        do_stop
        sleep 2
        do_start
        exit $?
        ;;
    status)
        do_status
        exit $?
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 2 # systemd dynamic exit code for invalid arguments
        ;;
esac