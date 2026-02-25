import argparse
import json
import os
import sys
import time

import requests



if __name__ == '__main__':

    parser = argparse.ArgumentParser(
        prog='ProgramName',
        description='What the program does',
        epilog='Text at the bottom of help')

    parser.add_argument('-u', '--url', required=True)  # option that takes a value
    parser.add_argument('-m', '--maxcalls', type=int)  # option that takes a value
    parser.add_argument('-i', '--identifier', type=str, required=True, action='append')# option that takes a value
    parser.add_argument('-o', '--output', type=str, required=True)# option that takes a value

    args = vars( parser.parse_args())
    base_url = args['url']
    count = 0
    max_calls = None
    if 'maxcalls' in args and args['maxcalls']:
        max_calls = int(args['maxcalls'])

    url = f'{base_url}/export'
    identifiers = args['identifier']
    export_path: str = args['output']

    export_type = None
    if export_path.endswith(".zip"):
        export_type = "application/zip"
    if export_path.endswith(".json"):
        export_type = "application/ld+json"
    if export_type is None:
        raise Exception(f"unknown type for format {export_path}")


    headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'api-key': os.environ['OPENBIS_KEY'],
        'Export': export_type,
        'openbis.with-levels-above': 'true',
        'openbis.import-compatible': 'true'
    }

    response = requests.post(url, json.dumps(identifiers), headers=headers, verify=False)
    if response.status_code != 202:
        raise Exception(f"This should have been accepted, {response.status_code}")
    response_obj = response.json()
    job_id = response_obj["jobId"]

    done = False
    count = 0
    while not done:
        count = count + 1
        if max_calls and count > max_calls:
            raise Exception("Too many attempts")

        url = f'{base_url}/status/{job_id}'
        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'api-key': os.environ['OPENBIS_KEY'],
            'jobId': job_id
        }


        response = requests.get(url, headers=headers, verify=False)
        content_type = response.headers['Content-Type']

        response_json = response.json()
        if response_json['status'] == 'COMPLETED':
            done = True

        if response_json['status'] == 'FAILED':
            print(response_json["errors"])
            raise Exception("Something failed")
        if not done:
            time.sleep(20.0)
            output = f"Call {count} of {max_calls}" if max_calls else f"Call {count}"
            print(output)
    url = f'{base_url}/download'
    headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'api-key': os.environ['OPENBIS_KEY'],
        'jobId': job_id
    }
    response = requests.get(url, headers=headers, verify=False)
    content_type = response.headers.get('Content-Type')
    with open('/tmp/out.zip', 'wb') as out_file:
        out_file.write(response.content)
        sys.exit(0)