package hev.htproxy;

/**
 * JNI contract supplied by the pinned HevSocks5Tunnel Android native build.
 * Native library license and attribution are recorded in THIRD_PARTY_NOTICES.md.
 */
public final class TProxyService {
    private TProxyService() {}

    public static native boolean TProxyStartService(String configPath, int tunFd);
    public static native boolean TProxyStopService();
    public static native boolean TProxyIsRunning();
    public static native long[] TProxyGetStats();

    static {
        System.loadLibrary("hev-socks5-tunnel");
    }
}
