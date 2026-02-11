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

    args = vars( parser.parse_args())
    base_url = args['url']
    count = 0
    max_calls = None
    if 'maxcalls' in args and args['maxcalls']:
        max_calls = int(args['maxcalls'])

    url = f'{base_url}/export'
    identifiers = ['20250808093031564-89', '20250808093031564-90']
    headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'api-key': os.environ['OPENBIS_KEY'],
        'Export': 'application/ld+json'
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

        url = f'{base_url}/status'
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
            with open('/tmp/out.json', 'wb') as out_file:
                out_file.write(response.content)
                sys.exit(0)
        response_json = response.json()
        if response_json['status'] == 'FAILED':
            print(response_json["errors"])
            raise Exception("Something failed")

        time.sleep(2.0)

        output = f"Call {count} of {max_calls}" if max_calls else f"Call {count}"

        print(output)


