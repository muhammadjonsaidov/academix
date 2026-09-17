package uz.academixai.interfaces.web.psychologist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Deviation, judgment call — POST /psychologist/watchlist/{studentId}'s body isn't spec'd. */
public record AddToWatchlistRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 500, message = "ko'pi bilan 500 belgi")
        String reason) {}
