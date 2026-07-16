package uz.academixai.interfaces.web.psychologist;

/** Deviation, judgment call — POST /psychologist/watchlist/{studentId}'s body isn't spec'd. */
public record AddToWatchlistRequest(String reason) {}
