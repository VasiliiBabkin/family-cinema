# Offline JNI feasibility probe

The APK does not include a torrent engine. This independent probe uses FrostWire jlibtorrent **2.0.12.9** (Java wrapper MIT; native libtorrent BSD-3-Clause and bundled dependency notices apply). No provider/GPL app code is copied.

Download the Java jar and the matching Linux x86_64 native jar from the project's versioned [release](https://github.com/frostwire/frostwire-jlibtorrent/releases/tag/release%2F2.0.12.9) into a directory outside Git. Compile OfflineProbe.java with the Java jar on the classpath; run with both jars and the directory containing OfflineProbe.class on the classpath. Arguments: path to the owned CC0 fixture, existing output directory. JDK 21 was used.

The probe creates real torrent metadata through the SDK, checks its file count/size, starts and pauses a native session, and stops it in finally. No download is added. DHT, LSD, UPnP and NAT-PMP are disabled; listen is loopback-only. It verifies native load and basic lifecycle on Linux, **not** Android ARM64 playback, peer transfer, seek, sequential scheduling or bounded P2P cache. See QA and dependency decisions for remaining gates.

Verified jar SHA-256:

- Java: `c96316e640397249ecfbbe56c355afef472190b8d47d51a94a03e47eb60c219a`
- Linux native: `588f70db3f2131f7c9a8133cba7883319c29a5434f2ad092ff4e1fb51dc2f088`

Binary jars, extracted libraries and generated .torrent files are excluded from Git. Only the probe source and actual result are retained.
