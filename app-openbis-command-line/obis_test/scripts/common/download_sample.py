#!/usr/bin/env python3
import os, sys
from pybis import Openbis

OBIS_URL  = os.getenv("OBIS_URL",  "https://localhost:8443")
OBIS_USER = os.getenv("OBIS_USER", "admin")
OBIS_PASS = os.getenv("OBIS_PASS", "changeit")
# Accept either a full identifier or a permId from the env:
SAMPLE_INPUT = os.getenv("OBIS_SAMPLE_ID", "").strip()

ob = Openbis(
    OBIS_URL,
    verify_certificates=False,
    allow_http_but_do_not_use_this_in_production_and_only_within_safe_networks=True
)
ob.login(OBIS_USER, OBIS_PASS)

try:
    if not SAMPLE_INPUT:
        sys.exit("No OBIS_SAMPLE_ID provided. Pass sample permId or identifier.")

    # Resolve to a sample object first (works with identifier or permId)
    sample = ob.get_sample(SAMPLE_INPUT)
    if not sample:
        sys.exit(f"No such sample: {SAMPLE_INPUT}")

    # Fetch some datasets (server paging only; no server-side sort)
    ds_list = ob.get_datasets(sample=sample, count=100)  # pass object, not string

    if not ds_list:
        sys.exit("No datasets found for the sample.")

    # Sort client-side by registrationDate and pick newest
    latest = sorted(ds_list, key=lambda d: d.registrationDate, reverse=True)[0]
    print(latest.permId)

finally:
    try: ob.logout()
    except Exception: pass
