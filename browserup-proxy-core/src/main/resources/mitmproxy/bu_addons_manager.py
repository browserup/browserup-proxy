import falcon

from examples.complex.har_dump import HarDumpAddonResource
from wsgiref.simple_server import make_server

addons_manager = {}

def load(l):
    global addons_manager
    addons_manager = l.master.addons


def configure(updated):
    start_falcon()


async def start_falcon():
    app = falcon.API()
    app.add_route('/har_addon/{method_name}', HarDumpAddonResource())

    with make_server('', 8088, app) as httpd:
        print('Serving on port 8088...')
        httpd.serve_forever()

def done():
    print("done")

