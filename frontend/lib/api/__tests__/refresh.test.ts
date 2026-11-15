import { afterEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import { refreshAccessToken } from "@/lib/api/refresh";

vi.mock("axios", () => ({ default: { post: vi.fn() } }));

describe("refreshAccessToken", () => {
  afterEach(() => vi.mocked(axios.post).mockReset());

  it("shares one request between concurrent callers (the backend rotates the refresh cookie)", async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: { accessToken: "fresh" } });

    const tokens = await Promise.all([refreshAccessToken(), refreshAccessToken(), refreshAccessToken()]);

    expect(tokens).toEqual(["fresh", "fresh", "fresh"]);
    expect(axios.post).toHaveBeenCalledTimes(1);
  });

  it("starts a new request once the previous one has settled", async () => {
    vi.mocked(axios.post)
      .mockResolvedValueOnce({ data: { accessToken: "first" } })
      .mockResolvedValueOnce({ data: { accessToken: "second" } });

    expect(await refreshAccessToken()).toBe("first");
    expect(await refreshAccessToken()).toBe("second");
    expect(axios.post).toHaveBeenCalledTimes(2);
  });

  it("lets every waiting caller see a failure, then allows a retry", async () => {
    vi.mocked(axios.post).mockRejectedValueOnce(new Error("401"));

    const results = await Promise.allSettled([refreshAccessToken(), refreshAccessToken()]);

    expect(results.map((r) => r.status)).toEqual(["rejected", "rejected"]);
    expect(axios.post).toHaveBeenCalledTimes(1);
  });
});
