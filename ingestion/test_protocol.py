import json
import os
import unittest
import urllib.request
import uuid

BRIDGE = os.environ.get("IIOT_BRIDGE", "http://127.0.0.1:18084")
DATA = os.environ.get("IIOT_EDGEX_DATA", "http://127.0.0.1:15980")


def request(url, payload=None):
    body = None if payload is None else json.dumps(payload).encode()
    req = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as response:
        return json.load(response)


class ProtocolTest(unittest.TestCase):
    def test_actual_opcua_and_edgex_preserve_observable_facts(self):
        run = "protocol-" + uuid.uuid4().hex
        event = dict(eventId=run + ":event:1", runId=run, lineId="line-a", station=2,
                     deviceId="line-a:station-2", workpieceId=run + ":wp:1", batchId="batch-a",
                     productType="A", type="OPERATION_STARTED", deviceSequence=1,
                     originalTimeMillis=1234, receivedTimeMillis=1234, quality="GOOD",
                     mappingConfigurationVersion="mapping-v1", formatVersion="format-v1",
                     bufferOccupancy=None, attributes={"sourceEventId": run + ":event:1"})
        result = request(BRIDGE + "/publish", [event])
        self.assertEqual(1, len(result))
        receipt = result[0]
        self.assertEqual("opc.tcp", receipt["protocol"])
        stored = request(DATA + "/api/v3/event/id/" + receipt["edgexEventId"])["event"]
        reading = stored["readings"][0]
        self.assertEqual("line-a:station-2", stored["deviceName"])
        self.assertEqual("String", reading["valueType"])
        mapped = json.loads(reading["value"])
        for field in event:
            if field != "receivedTimeMillis":
                self.assertEqual(event[field], mapped[field], field)
        self.assertGreaterEqual(mapped["receivedTimeMillis"], event["originalTimeMillis"])
        self.assertFalse({"faultTruth", "faultStation", "futureEvent"} & mapped["attributes"].keys())


if __name__ == "__main__":
    unittest.main()
