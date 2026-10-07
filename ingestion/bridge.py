import asyncio
import json
import os
import time
from pathlib import Path
from urllib.parse import quote
from aiohttp import web, ClientSession, ClientTimeout
from asyncua import Server, Client

MAPPING = json.loads(Path(__file__).with_name("mapping.json").read_text())
DATA = os.environ["EDGEX_DATA"]
METADATA = os.environ["EDGEX_METADATA"]
DEVICE = os.environ["EDGEX_DEVICE"]
PROFILE = "iiot-observation-v1"
FIELDS = {"eventId", "runId", "lineId", "station", "deviceId", "workpieceId", "batchId",
          "productType", "type", "deviceSequence", "originalTimeMillis", "receivedTimeMillis",
          "quality", "mappingConfigurationVersion", "formatVersion", "bufferOccupancy", "attributes"}


async def call(session, method, url, **kwargs):
    async with session.request(method, url, **kwargs) as response:
        body = await response.text()
        if response.status >= 400:
            raise RuntimeError(f"EdgeX {response.status}: {body}")
        result = json.loads(body) if body else {}
        entries = result if isinstance(result, list) else [result]
        if any(entry.get("statusCode", 200) >= 400 for entry in entries):
            raise RuntimeError(f"EdgeX rejected request: {body}")
        return result


async def provision(session):
    profile_url = METADATA + "/api/v3/deviceprofile/name/" + PROFILE
    async with session.get(profile_url) as response:
        if response.status == 404:
            await call(session, "POST", METADATA + "/api/v3/deviceprofile", json=[{
                "apiVersion": "v3", "profile": {"name": PROFILE,
                    "deviceResources": [{"name": "observation", "properties": {
                        "valueType": "String", "readWrite": "R", "units": "milliseconds-since-run-start"}}]}}])
        elif response.status != 200:
            raise RuntimeError("EdgeX profile service unavailable")
    for device in MAPPING["devices"].values():
        async with session.get(METADATA + "/api/v3/device/name/" + quote(device, safe="")) as response:
            if response.status == 404:
                await call(session, "POST", METADATA + "/api/v3/device", json=[{
                    "apiVersion": "v3", "device": {"name": device, "profileName": PROFILE,
                        "serviceName": "device-rest", "adminState": "UNLOCKED", "operatingState": "UP",
                        "protocols": {"other": {}}}}])
            elif response.status != 200:
                raise RuntimeError("EdgeX device service unavailable")


def validate(event):
    if set(event) != FIELDS or event.get("attributes") != {"sourceEventId": event.get("eventId")}:
        raise ValueError("Unsupported observable fields or attributes")
    station = str(event["station"])
    if station not in MAPPING["devices"] or event["deviceId"] != MAPPING["devices"][station]:
        raise ValueError("Unknown device or station mapping")
    for key in ("lineId", "mappingConfigurationVersion", "formatVersion"):
        if event[key] != MAPPING[key]:
            raise ValueError("Unsupported mapping or format version")
    if event["deviceSequence"] < 1 or event["originalTimeMillis"] < 0:
        raise ValueError("Invalid source sequence or time")


async def stored_event(session, device, event_id):
    deadline = time.monotonic() + 15
    while time.monotonic() < deadline:
        result = await call(session, "GET", DATA + "/api/v3/event/device/name/" + quote(device, safe="") + "?limit=1000")
        for event in result.get("events", []):
            for reading in event.get("readings", []):
                if reading.get("resourceName") == "observation":
                    mapped = json.loads(reading["value"])
                    if mapped["eventId"] == event_id:
                        return event["id"], mapped
        await asyncio.sleep(0.05)
    raise RuntimeError("EdgeX did not persist the observation before the deadline")


async def send_device(session, device, mapped):
    deadline = time.monotonic() + 10
    url = DEVICE + "/api/v3/resource/" + quote(device, safe="") + "/observation"
    while True:
        async with session.post(url, data=json.dumps(mapped), headers={"Content-Type": "text/plain"}) as response:
            body = await response.text()
            if response.status == 200:
                return
            if response.status != 404 or time.monotonic() >= deadline:
                raise RuntimeError(f"EdgeX device {response.status}: {body}")
        await asyncio.sleep(0.1)


async def publish(request):
    events = await request.json()
    try:
        for event in events:
            validate(event)
    except (ValueError, KeyError, TypeError) as error:
        raise web.HTTPBadRequest(text=str(error))
    receipts = []
    async with request.app["lock"]:
        async with Client("opc.tcp://127.0.0.1:4840/iiot/") as client:
            async with ClientSession(timeout=ClientTimeout(total=20)) as session:
                await provision(session)
                for event in events:
                    node = request.app["nodes"][event["station"]]
                    started = time.monotonic_ns()
                    await node.write_value(json.dumps(event, separators=(",", ":")))
                    value = await client.get_node(node.nodeid).read_data_value()
                    mapped = json.loads(value.Value.Value)
                    mapped["quality"] = "GOOD" if value.StatusCode.is_good() else "BAD"
                    mapped["receivedTimeMillis"] = mapped["originalTimeMillis"] + (time.monotonic_ns() - started) // 1_000_000
                    device = mapped["deviceId"]
                    await send_device(session, device, mapped)
                    edge_id, persisted = await stored_event(session, device, mapped["eventId"])
                    receipts.append({"event": persisted, "edgexEventId": edge_id, "protocol": "opc.tcp"})
    return web.json_response(receipts)


async def health(request):
    try:
        async with ClientSession(timeout=ClientTimeout(total=3)) as session:
            for url in (DATA, METADATA, DEVICE):
                async with session.get(url + "/api/v3/ping") as response:
                    if response.status != 200:
                        raise RuntimeError(f"EdgeX unavailable: {url}")
        async with Client("opc.tcp://127.0.0.1:4840/iiot/") as client:
            await client.get_node(request.app["nodes"][1].nodeid).read_value()
        return web.json_response({"status": "UP", "mapping": MAPPING})
    except Exception as error:
        return web.json_response({"status": "DOWN", "reason": str(error)}, status=503)


async def server_context(app):
    server = Server()
    await server.init()
    server.set_endpoint("opc.tcp://0.0.0.0:4840/iiot/")
    index = await server.register_namespace("urn:iiot:observations:v1")
    app["nodes"] = {}
    for station in range(1, 5):
        app["nodes"][station] = await server.nodes.objects.add_variable(index, f"Station{station}", "{}")
    app["lock"] = asyncio.Lock()
    async with server:
        yield


app = web.Application()
app.cleanup_ctx.append(server_context)
app.router.add_post("/publish", publish)
app.router.add_get("/health", health)
web.run_app(app, port=8084)
