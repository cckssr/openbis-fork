import argparse
import os
import requests
import time


def get_mime_type(import_path):
    import_type = None
    if import_path.endswith(".zip"):
        return "application/zip"
    if import_path.endswith(".eln"):
        return "application/zip"
    if import_path.endswith(".json"):
        return "application/ld+json"
    if import_type is None:
        raise Exception(f"unknown type for format {import_path}")


if __name__ == "__main__":

    parser = argparse.ArgumentParser(
        prog="ProgramName",
        description="What the program does",
        epilog="Text at the bottom of help",
    )

    parser.add_argument("-u", "--url", required=True)  # option that takes a value
    parser.add_argument("-m", "--maxcalls", type=int)  # option that takes a value
    parser.add_argument(
        "-p", "--path", type=str, required=True
    )  # option that takes a value

    args = vars(parser.parse_args())
    base_url = args["url"]
    count = 0
    max_calls = None
    if "maxcalls" in args and args["maxcalls"]:
        max_calls = int(args["maxcalls"])

    url = f"{base_url}/import"
    import_path: str = args["path"]

    import_type = get_mime_type(import_path)

    headers = {
        "Accept": "application/json",
        "Content-Type": import_type,
        "api-key": os.environ["OPENBIS_KEY"],
    }
    with open(import_path, "rb") as in_file:
        contents = in_file.read()

    response = requests.post(url, data=contents, headers=headers, verify=False)
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

        url = f"{base_url}/status/{job_id}"
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "api-key": os.environ["OPENBIS_KEY"],
            "jobId": job_id,
        }

        response = requests.get(url, headers=headers, verify=False)
        content_type = response.headers["Content-Type"]

        response_json = response.json()
        if response_json["status"] == "COMPLETED":
            done = True

        if response_json["status"] == "FAILED":
            print(response_json["errors"])
            raise Exception("Something failed")
        if not done:
            time.sleep(4)
            output = f"Call {count} of {max_calls}" if max_calls else f"Call {count}"
            print(output)
