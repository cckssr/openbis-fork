import json
import os
import sys
import time

import requests

if __name__ == '__main__':

    url = 'http://localhost:8086/openbis/open-api/ro-crate/export'
    identifiers = ['20260119123615519-39']
    headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'api-key': os.environ['OPENBIS_KEY'],
        'Export': 'application/zip'
    }

    response = requests.post(url, json.dumps(identifiers), headers=headers, verify=False)
    if response.status_code != 202:
        raise Exception(f"This should have been accepted, {response.status_code}")
    response_obj = response.json()
    job_id = response_obj["jobId"]

    done = False
    while not done:
        url = 'http://localhost:8086/openbis/open-api/ro-crate/status'
        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'api-key': os.environ['OPENBIS_KEY'],
            'jobId': job_id
        }


        response = requests.get(url, headers=headers, verify=False)
        content_type = response.headers['Content-Type']
        if content_type != 'application/json':
            done = True
            with open('/tmp/out.zip', 'wb') as out_file:
                out_file.write(response.content)
                sys.exit(0)
        response_json = response.json()
        if response_json['status'] == 'FAILED':
            print(response_json["errors"])
            raise Exception("Something failed")

        time.sleep(2.0)

        print("lol")


