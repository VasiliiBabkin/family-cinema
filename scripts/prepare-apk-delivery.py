#!/usr/bin/env python3
"""Verify a local APK and prepare a named, checksummed delivery bundle.
No credentials, Drive links or binary data are written to Git. Upload uses the
connected Drive tool; its metadata must be checked against this manifest.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


def main():
    parser = argparse.ArgumentParser(description="Подготовка проверенного APK к доставке")
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--version", default="0.2.0")
    parser.add_argument("--target", choices=["universal", "mobile", "tv"], default="universal")
    parser.add_argument("--variant", choices=["debug", "release"], default="debug")
    parser.add_argument("--output", type=Path, default=Path("artifacts/delivery"))
    parser.add_argument("--build-tools", type=Path)
    parser.add_argument("--verify-roundtrip", type=Path)
    parser.add_argument("--drive-file-id")
    parser.add_argument("--drive-folder-id")
    parser.add_argument("--drive-url")
    args = parser.parse_args()
    if not args.apk.is_file() or args.apk.stat().st_size == 0:
        parser.error("APK отсутствует или пуст")
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    tools = args.build_tools or (Path(sdk) / "build-tools" / "34.0.0" if sdk else None)
    if tools is None:
        parser.error("Укажите --build-tools или ANDROID_HOME")
    subprocess.run([str(tools / "apksigner"), "verify", "--verbose", str(args.apk)], check=True)
    badging = subprocess.check_output([str(tools / "aapt"), "dump", "badging", str(args.apk)], text=True)
    if f"versionName='{args.version}'" not in badging:
        parser.error("Версия APK не совпадает с указанной")
    args.output.mkdir(parents=True, exist_ok=True)
    filename = f"family-cinema-v{args.version}-{args.target}-{args.variant}.apk"
    destination = args.output / filename
    with args.apk.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    manifest = {"version": args.version, "target": args.target, "variant": args.variant,
                "file": filename, "size_bytes": args.apk.stat().st_size, "sha256": digest,
                "signature": "verified", "drive": {"status": "BLOCKED", "reason": "Требуется согласованная папка и подтверждённая загрузка через Drive"}}
    if args.verify_roundtrip:
        from urllib.parse import urlparse
        if not (args.drive_file_id and args.drive_folder_id and args.drive_url):
            parser.error("Для readback нужны подтверждённые Drive file ID, folder ID и URL")
        url = urlparse(args.drive_url)
        if url.scheme != "https" or url.hostname != "drive.google.com" or f"/file/d/{args.drive_file_id}" not in url.path:
            parser.error("URL не соответствует подтверждённому Drive file ID")
        if not args.verify_roundtrip.is_file():
            parser.error("Скачанный обратно APK отсутствует")
        with args.verify_roundtrip.open("rb") as stream:
            remote_digest = hashlib.file_digest(stream, "sha256").hexdigest()
        if remote_digest != digest or args.verify_roundtrip.stat().st_size != args.apk.stat().st_size:
            parser.error("Размер или SHA-256 скачанного APK не совпадает с оригиналом")
        manifest["drive"] = {
            "status": "PASS", "file_id": args.drive_file_id, "folder_id": args.drive_folder_id,
            "url": args.drive_url, "roundtrip_verified": True, "downloaded_sha256": remote_digest,
            "size_bytes": args.verify_roundtrip.stat().st_size,
        }
    if destination.resolve() != args.apk.resolve():
        shutil.copy2(args.apk, destination)
    (args.output / "delivery.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+"\n")
    (args.output / "SHA256SUMS.txt").write_text(f"{digest}  {filename}\n")
    # Private Drive identifiers stay in the ignored receipt, not public build logs.
    print(json.dumps({k: v for k, v in manifest.items() if k != "drive"} | {"drive_status": manifest["drive"]["status"]}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
