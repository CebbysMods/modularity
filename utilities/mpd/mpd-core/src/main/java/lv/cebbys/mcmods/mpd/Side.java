package lv.cebbys.mcmods.mpd;

/** Physical side: the client (which may also run an integrated server) or a dedicated server. */
public enum Side {
    CLIENT, SERVER,
    /** Only a loader specific source can tell; loader independent sources report this. */
    UNKNOWN
}
