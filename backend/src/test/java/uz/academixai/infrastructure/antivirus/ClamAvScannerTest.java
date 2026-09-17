package uz.academixai.infrastructure.antivirus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import uz.academixai.shared.error.ApiException;

/**
 * Exercises {@link ClamAvScanner} against a stub clamd speaking the real INSTREAM wire protocol,
 * because the protocol framing (NUL-terminated command, 4-byte big-endian chunk lengths,
 * zero-length terminator, NUL-terminated reply) is the part most likely to be wrong and cannot be
 * verified by compiling. The stub asserts what the scanner actually put on the wire rather than
 * just replying.
 */
class ClamAvScannerTest {

  private ServerSocket server;
  private ExecutorService executor;
  private ClamAvScanner scanner;

  @BeforeEach
  void setUp() throws IOException {
    server = new ServerSocket(0); // ephemeral port
    executor = Executors.newSingleThreadExecutor();
    scanner = new ClamAvScanner();
    ReflectionTestUtils.setField(scanner, "enabled", true);
    ReflectionTestUtils.setField(scanner, "host", "127.0.0.1");
    ReflectionTestUtils.setField(scanner, "port", server.getLocalPort());
    ReflectionTestUtils.setField(scanner, "timeoutMs", 5000);
  }

  @AfterEach
  void tearDown() throws IOException {
    executor.shutdownNow();
    server.close();
  }

  /** Accepts one connection, drains a full INSTREAM request, replies, returns the payload sent. */
  private Future<byte[]> stubClamd(String reply) {
    return executor.submit(
        () -> {
          try (Socket socket = server.accept();
              InputStream rawIn = socket.getInputStream();
              DataInputStream in = new DataInputStream(rawIn);
              OutputStream out = socket.getOutputStream()) {

            byte[] command = new byte[10]; // "zINSTREAM\0"
            in.readFully(command);
            assertThat(new String(command, StandardCharsets.US_ASCII)).isEqualTo("zINSTREAM\0");

            ByteArrayOutputStream received = new ByteArrayOutputStream();
            while (true) {
              int length = in.readInt();
              if (length == 0) {
                break; // zero-length chunk terminates the stream
              }
              byte[] chunk = new byte[length];
              in.readFully(chunk);
              received.write(chunk);
            }
            out.write((reply + "\0").getBytes(StandardCharsets.US_ASCII));
            out.flush();
            return received.toByteArray();
          }
        });
  }

  @Test
  void sendsFileBytesVerbatimAndAcceptsCleanFile() throws Exception {
    byte[] payload = "clean homework photo bytes".getBytes(StandardCharsets.UTF_8);
    Future<byte[]> received = stubClamd("stream: OK");

    scanner.scan(new MockMultipartFile("image", "hw.png", "image/png", payload));

    // The scanner must deliver the file byte-for-byte — a framing bug would corrupt this.
    assertThat(received.get()).isEqualTo(payload);
  }

  @Test
  void rejectsInfectedFile() {
    stubClamd("stream: Eicar-Test-Signature FOUND");

    assertThatThrownBy(
            () ->
                scanner.scan(
                    new MockMultipartFile("image", "bad.png", "image/png", "x".getBytes())))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("xavfsizlik");
  }

  @Test
  void failsClosedWhenClamdIsUnreachable() throws IOException {
    server.close(); // nothing listening — the enabled-but-broken case

    assertThatThrownBy(
            () ->
                scanner.scan(new MockMultipartFile("image", "hw.png", "image/png", "x".getBytes())))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void doesNothingWhenDisabled() {
    ReflectionTestUtils.setField(scanner, "enabled", false);
    // No stub is listening: if the scanner tried to connect at all this would throw.
    assertThatCode(
            () ->
                scanner.scan(new MockMultipartFile("image", "hw.png", "image/png", "x".getBytes())))
        .doesNotThrowAnyException();
  }

  @Test
  void skipsEmptyFile() {
    assertThatCode(
            () -> scanner.scan(new MockMultipartFile("image", "hw.png", "image/png", new byte[0])))
        .doesNotThrowAnyException();
  }
}
