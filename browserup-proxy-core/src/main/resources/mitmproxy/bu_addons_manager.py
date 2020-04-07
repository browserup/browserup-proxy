import falcon
import _thread

from mitmproxy import ctx

from wsgiref.simple_server import make_server

initialized = False


def running():
    if not initialized and is_script_loader_initialized():
        resources = get_resources()
        _thread.start_new_thread(start_falcon, tuple([resources]))


def is_script_loader_initialized():
    script_loader = ctx.master.addons.get("scriptloader")

    for custom_addon in script_loader.addons:
        if len(custom_addon.addons) == 0:
            return False

    return True


def get_resources():
    script_loader = ctx.master.addons.get("scriptloader")
    resources = []

    for custom_addon in script_loader.addons:
        custom_loaded_addon = custom_addon.addons[0]
        if hasattr(custom_loaded_addon, "addons"):
            for ca in custom_loaded_addon.addons:
                if hasattr(ca, "get_resource"):
                    resources.append(getattr(ca, "get_resource")())
        if hasattr(custom_loaded_addon, "get_resource"):
            resources.append(getattr(custom_loaded_addon, "get_resource")())

    return resources


def start_falcon(resources):
    app = falcon.API()
    for resource in resources:
        app.add_route("/" + resource.addon_path() + "/{method_name}", resource)

    with make_server('', 8088, app) as httpd:
        print('Serving on port 8088...')
        httpd.serve_forever()


def done():
    print("done")
