import subprocess
from urllib.request import urlopen

BUF_SIZE = 4096  # bytes


def check_output(cmd: str):
    proc = subprocess.Popen(cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    try:
        while True:
            if proc.poll() is not None:
                break
            output = proc.stdout.readline()
            if output:
                print(output.decode('utf-8').strip())
    except KeyboardInterrupt:
        proc.terminate()
        raise

def download_file(uri: str, path: str, is_binary: bool = True) -> None:
    mode = 'wb' if is_binary else 'w'
    resp = urlopen(uri)
    with open(path, mode) as f:
        while True:
            buf = resp.read(BUF_SIZE)
            if not buf:
                break
            f.write(buf)