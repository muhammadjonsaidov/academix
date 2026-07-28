package uz.academixai.infrastructure.antivirus;

import jakarta.annotation.PostConstruct;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §5.2 / backend TDD §"Virus Scan" — ClamAV scanning for uploaded files.
 *
 * <p>Speaks clamd's {@code INSTREAM} protocol directly over TCP rather than pulling in a client
 * library: the protocol is a length-prefixed chunk stream and a one-line reply, so a dependency
 * would be more version surface than code saved (and this project has repeatedly been burned by
 * dependencies that don't resolve — see CLAUDE.md).
 *
 * <p><b>Disabled by default, and says so at startup.</b> The official ClamAV image is ~1GB and
 * spends minutes loading signature databases on first boot, so defaulting this ON would make the
 * documented local stack much heavier and would hard-fail every upload on any existing deployment
 * the moment this shipped. Instead: {@code ACADEMIX_CLAMAV_ENABLED=true} turns it on, {@code
 * infra/docker-compose.yml} carries the service under the {@code antivirus} profile, and {@link
 * #warnIfDisabled()} logs a WARN on every boot while it's off — a security control that quietly
 * does nothing is exactly the failure mode this codebase keeps rediscovering, so this one announces
 * itself.
 *
 * <p><b>Fails closed when enabled.</b> If clamd is unreachable or misbehaves while scanning is
 * switched on, the upload is rejected rather than waved through — an operator who deliberately
 * enabled scanning wants the scan, and silently degrading to "no scanning" would defeat the point.
 * This deliberately differs from the AI pipeline's graceful degradation: a missing AI grade is a
 * postponed convenience, an unscanned upload is a permanent hole.
 */
@Component
public class ClamAvScanner {

  private static final Logger log = LoggerFactory.getLogger(ClamAvScanner.class);

  /** clamd caps a single INSTREAM chunk; 8KB is well under any default StreamMaxLength. */
  private static final int CHUNK_SIZE = 8192;

  @Value("${academix.clamav.enabled:false}")
  private boolean enabled;

  @Value("${academix.clamav.host:localhost}")
  private String host;

  @Value("${academix.clamav.port:3310}")
  private int port;

  @Value("${academix.clamav.timeout-ms:30000}")
  private int timeoutMs;

  @PostConstruct
  void warnIfDisabled() {
    if (!enabled) {
      log.warn(
          "ClamAV upload scanning is DISABLED (academix.clamav.enabled=false). Uploaded files are"
              + " stored unscanned — academix_tz.md §5.2 requires scanning in production. Set"
              + " ACADEMIX_CLAMAV_ENABLED=true and start the clamav service"
              + " (docker-compose --profile antivirus up -d).");
    } else {
      log.info("ClamAV upload scanning enabled — clamd at {}:{}", host, port);
    }
  }

  /**
   * Scans an uploaded file, throwing {@code ERR_INVALID_FILE} if it is infected or cannot be
   * scanned. No-op when disabled.
   *
   * <p>Uses {@code ERR_INVALID_FILE} rather than a new {@code ERR_VIRUS_*} code because the backend
   * TDD's error table defines no virus-specific code, and this codebase's rule is to use the
   * documented codes rather than invent alternatives. The message distinguishes the two cases for
   * the user even though the code is shared.
   */
  public void scan(MultipartFile file) {
    if (!enabled || file == null || file.isEmpty()) {
      return;
    }
    String result;
    try (InputStream content = file.getInputStream()) {
      result = scanStream(content);
    } catch (IOException e) {
      log.error("ClamAV scan failed for upload '{}'", file.getOriginalFilename(), e);
      throw unscannable();
    }

    if (result.contains("FOUND")) {
      // Deliberately logged at WARN with the signature name: this is a real security event and
      // the one case where an operator needs to know what was uploaded and by which request.
      log.warn("ClamAV rejected upload '{}': {}", file.getOriginalFilename(), result);
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_INVALID_FILE",
          "Fayl xavfsizlik tekshiruvidan o'tmadi.",
          "Boshqa fayl yuklang yoki administratorga murojaat qiling.");
    }
    if (!result.contains("OK")) {
      log.error("Unexpected clamd reply for upload '{}': {}", file.getOriginalFilename(), result);
      throw unscannable();
    }
  }

  private String scanStream(InputStream content) throws IOException {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), timeoutMs);
      socket.setSoTimeout(timeoutMs);

      try (OutputStream rawOut = socket.getOutputStream();
          DataOutputStream out = new DataOutputStream(rawOut);
          InputStream in = socket.getInputStream()) {

        out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
        out.flush();

        byte[] buffer = new byte[CHUNK_SIZE];
        int read;
        while ((read = content.read(buffer)) != -1) {
          if (read > 0) {
            out.writeInt(read); // 4-byte big-endian chunk length, per the INSTREAM protocol
            out.write(buffer, 0, read);
          }
        }
        out.writeInt(0); // zero-length chunk terminates the stream
        out.flush();

        return readReply(in);
      }
    }
  }

  /** clamd replies with a single NUL-terminated line, e.g. {@code stream: OK\0}. */
  private String readReply(InputStream in) throws IOException {
    StringBuilder reply = new StringBuilder();
    int b;
    while ((b = in.read()) != -1 && b != 0) {
      reply.append((char) b);
    }
    return reply.toString().trim();
  }

  private ApiException unscannable() {
    return new ApiException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "ERR_INVALID_FILE",
        "Faylni xavfsizlik tekshiruvidan o'tkazib bo'lmadi. Keyinroq urinib ko'ring.",
        "Antivirus xizmati ishlamayapti — administratorga murojaat qiling.");
  }
}
