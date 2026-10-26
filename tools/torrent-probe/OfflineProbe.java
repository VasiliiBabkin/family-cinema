import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.swig.settings_pack;
import java.io.File;
import java.nio.file.Files;

/** Licensing/lifecycle feasibility probe only. Not included in the Android APK. */
public final class OfflineProbe {
    public static void main(String[] args) throws Exception {
        File ownedVideo = new File(args[0]);
        if (!ownedVideo.isFile() || ownedVideo.length() > 2 * 1024 * 1024) throw new IllegalArgumentException("Owned fixture exceeds probe budget");
        TorrentBuilder.Result generated = new TorrentBuilder().path(ownedVideo).creator("Family Cinema offline feasibility probe").setPrivate(true).generate();
        byte[] metadata = generated.entry().bencode();
        TorrentInfo info = new TorrentInfo(metadata);
        if (!info.isValid() || info.totalSize() != ownedVideo.length() || info.numFiles() != 1 || !info.trackers().isEmpty()) throw new AssertionError("Incorrect owned torrent metadata");
        Files.write(new File(args[1], "owned-fixture.torrent").toPath(), metadata);
        SettingsPack settings = new SettingsPack().listenInterfaces("127.0.0.1:0").connectionsLimit(2).activeDownloads(1).activeSeeds(0).downloadRateLimit(65536).uploadRateLimit(1024);
        settings.setEnableDht(false);
        settings.setEnableLsd(false);
        settings.setBoolean(settings_pack.bool_types.enable_upnp.swigValue(), false);
        settings.setBoolean(settings_pack.bool_types.enable_natpmp.swigValue(), false);
        SessionManager session = new SessionManager(false);
        try {
            session.start(new SessionParams(settings));
            if (!session.isRunning()) throw new AssertionError("Native session did not start");
            session.pause();
            if (!session.isPaused()) throw new AssertionError("Pause failed");
            System.out.println("PASS: owned metadata " + info.numFiles() + " file, " + info.totalSize() + " bytes, " + info.numPieces() + " pieces; native session start/pause");
            System.out.println("No download added; DHT/LSD/UPnP/NAT-PMP disabled; loopback listen only.");
        } finally {
            session.stop();
        }
        if (session.isRunning()) throw new AssertionError("Native resources not stopped");
        System.out.println("PASS: native session stopped. Peer transfer, Android lifecycle and ARM64 runtime NOT TESTED.");
    }
}
