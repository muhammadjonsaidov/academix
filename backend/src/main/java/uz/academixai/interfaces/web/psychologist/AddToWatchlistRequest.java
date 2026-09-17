package uz.academixai.interfaces.web.psychologist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Deviation, judgment call — POST /psychologist/watchlist/{studentId}'s body isn't spec'd. */
public record AddToWatchlistRequest(@NotBlank @Size(max = 500) String reason) {}
