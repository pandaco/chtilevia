#!/usr/bin/env python3
"""Génère tests/fixtures/ à partir d'un instantané brut des flux Ilévia.

Usage : python3 -I tests/make_fixtures.py <dossier_brut> <dossier_fixtures>

<dossier_brut> contient : le GTFS extrait (*.txt), rt-midi.pb, rt-minuit.pb,
station_information.json et station_status.json. On n'en garde qu'un sous-ensemble
(lignes 10, tram et M1 ; 3 jours de calendrier ; quelques stations V'Lille) pour des
fixtures légères et stables.
"""
import csv
import json
import os
import sys
import zipfile

ROUTES = {"10", "71", "ME1"}
DATES = {"20261010", "20261011", "20261012"}
STATIONS = ("rihour", "gare", "sainte h")
RT_FILES = ("rt-midi.pb", "rt-minuit.pb")


def read(path):
    with open(path, newline="", encoding="utf-8-sig") as f:
        r = csv.DictReader(f)
        return r.fieldnames, list(r)


def write(zf, name, fields, rows):
    path = f"/tmp/chti-fixture-{os.getpid()}-{name}"
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields, lineterminator="\n")
        w.writeheader()
        w.writerows(rows)
    zf.write(path, name)
    os.remove(path)


def stream_filter(path, keep):
    with open(path, newline="", encoding="utf-8-sig") as f:
        r = csv.DictReader(f)
        return r.fieldnames, [row for row in r if keep(row)]


# --- protobuf : recopie les entités GTFS-RT des seules lignes conservées ---

def varint(b, i):
    r = s = 0
    while True:
        x = b[i]
        i += 1
        r |= (x & 0x7F) << s
        s += 7
        if x < 128:
            return r, i


def enc_varint(n):
    out = bytearray()
    while True:
        x = n & 0x7F
        n >>= 7
        out.append(x | (0x80 if n else 0))
        if not n:
            return bytes(out)


def fields(b):
    i = 0
    while i < len(b):
        start = i
        key, i = varint(b, i)
        f, w = key >> 3, key & 7
        if w == 0:
            v, i = varint(b, i)
        elif w == 2:
            n, i = varint(b, i)
            v, i = b[i:i + n], i + n
        elif w == 1:
            v, i = b[i:i + 8], i + 8
        elif w == 5:
            v, i = b[i:i + 4], i + 4
        else:
            raise ValueError(w)
        yield f, v, b[start:i]


def entity_route(ent):
    for f, v, _ in fields(ent):
        if f == 3:  # trip_update
            for f2, v2, _ in fields(v):
                if f2 == 1:  # trip
                    for f3, v3, _ in fields(v2):
                        if f3 == 5:
                            return v3.decode()
    return None


def filter_rt(src, dst):
    out = bytearray()
    for f, v, raw in fields(open(src, "rb").read()):
        if f != 2 or entity_route(v) in ROUTES:
            out += raw
    open(dst, "wb").write(bytes(out))


def main(raw, out):
    os.makedirs(out, exist_ok=True)

    rf, routes = read(f"{raw}/routes.txt")
    routes = [r for r in routes if r["route_id"] in ROUTES]
    tf, trips = read(f"{raw}/trips.txt")
    cf, cal = read(f"{raw}/calendar_dates.txt")
    cal = [c for c in cal if c["date"] in DATES]
    services = {c["service_id"] for c in cal}
    trips = [t for t in trips if t["route_id"] in ROUTES and t["service_id"] in services]
    trip_ids = {t["trip_id"] for t in trips}
    cal = [c for c in cal if c["service_id"] in {t["service_id"] for t in trips}]
    stf, stop_times = stream_filter(f"{raw}/stop_times.txt", lambda r: r["trip_id"] in trip_ids)
    used = {r["stop_id"] for r in stop_times}
    sf, stops = read(f"{raw}/stops.txt")
    parents = {s["parent_station"] for s in stops if s["stop_id"] in used}
    stops = [s for s in stops if s["stop_id"] in used or s["stop_id"] in parents]

    with zipfile.ZipFile(f"{out}/gtfs.zip", "w", zipfile.ZIP_DEFLATED) as zf:
        write(zf, "routes.txt", rf, routes)
        write(zf, "trips.txt", tf, trips)
        write(zf, "calendar_dates.txt", cf, cal)
        write(zf, "stop_times.txt", stf, stop_times)
        write(zf, "stops.txt", sf, stops)

    for name in RT_FILES:
        filter_rt(f"{raw}/{name}", f"{out}/{name}")

    info = json.load(open(f"{raw}/station_information.json"))
    status = json.load(open(f"{raw}/station_status.json"))
    keep = {s["station_id"] for s in info["data"]["stations"]
            if any(k in s["name"].lower() for k in STATIONS)}
    # Une station hors service, pour couvrir ce cas d'affichage
    keep |= {next(s["station_id"] for s in status["data"]["stations"] if not s["is_renting"])}
    info["data"]["stations"] = [s for s in info["data"]["stations"] if s["station_id"] in keep]
    status["data"]["stations"] = [s for s in status["data"]["stations"] if s["station_id"] in keep]
    for name, data in (("station_information.json", info), ("station_status.json", status)):
        with open(f"{out}/{name}", "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main(*sys.argv[1:3])
